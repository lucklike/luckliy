package com.luckyframework.idea.spel.lexer

import com.luckyframework.idea.spel.model.SpelProblem
import junit.framework.TestCase

/**
 * 官方 SpEL 语法规则的覆盖测试。
 *
 * 合法用例对应 `expression` 到 `primaryExpression` 的各条规则；
 * 非法用例聚焦“结构完整但内容不合法”的片段——括号不配平、未闭合
 * 字符串等结构问题由 [SpelScanner] 负责，解析器不重复报告。
 */
class SpelSyntaxParserTest : TestCase() {

    private val scanner = SpelScanner()
    private val parser = SpelSyntaxParser()

    fun testArithmeticOperators() {
        assertValid("#{1 + 2 * 3 - 4 / 2 % 2}")
        assertValid("#{2 ^ 3}")
        assertValid("#{2 ** 3}")
        assertValid("#{-1 + +2}")
        assertValid("#{#a++ + ++#b}")
    }

    fun testLogicalAndRelationalOperators() {
        assertValid("#{!true and false or true}")
        assertValid("#{#a && #b || #c}")
        assertValid("#{#a > 1}")
        assertValid("#{#a eq 1 and #b ne 2}")
        assertValid("#{'abc' matches '[a-z]+'}")
        assertValid("#{1 between {1, 5}}")
        assertValid("#{#obj instanceof T(String)}")
    }

    fun testPropertyAndMethodAccess() {
        assertValid("#{#a.b.c}")
        assertValid("#{#a.b().c}")
        assertValid("#{#fn(1, 'a', true, null)}")
        assertValid("#{#ns.fn(1)}")
        assertValid("#{#list[0]}")
        assertValid("#{#list[0].name}")
        assertValid("#{#a.[0]}")
    }

    fun testCollectionProjectionAndSelection() {
        assertValid("#{#list.?[#this > 1]}")
        assertValid("#{#list.![#this.name]}")
        assertValid("#{#list.^[true]}")
        assertValid("#{#list.$[true]}")
        // 官方 startNode 也允许投影/选择作为起始节点
        assertValid("#{![#this.name]}")
        assertValid("#{?[#this > 1]}")
        // 安全导航后紧跟选择/投影标记
        assertValid("#{#list?.?[#this > 1]}")
        assertValid("#{#list?.![#this.name]}")
        assertValid("#{#list?.^[true]}")
        assertValid("#{#list?.\$[true]}")
    }

    fun testTernaryElvisAndSafeNavigation() {
        assertValid("#{#a ? 1 : 2}")
        assertValid("#{#a ?: 2}")
        assertValid("#{#a?.b}")
        // 用户局部变量与属性/下标赋值是 Spring 官方语法
        assertValid("#{#a = 1}")
        assertValid("#{#a.b = 1}")
    }

    /** `$name$` 根变量由框架注入且只读，表达式内赋值一定不生效。 */
    fun testRootVariableAssignmentIsProblem() {
        assertProblem("#{\$status\$ = 400}", "只读，不能赋值")
        assertProblem("#{\$body\$ = 'x'}", "只读，不能赋值")
        // 属性链路不是根变量赋值，运行期按 Spring 语义处理
        assertValid("#{\$req\$.url = 'x'}")
    }

    fun testTypeReferenceAndConstructor() {
        assertValid("#{T(java.lang.String)}")
        assertValid("#{T(int[])}")
        assertValid("#{new java.util.ArrayList()}")
        assertValid("#{new int[]{1, 2}}")
        assertValid("#{new int[3]}")
    }

    fun testInlineListAndMap() {
        assertValid("#{ {1, 2, 3} }")
        assertValid("#{ {'a': 1, 'b': 2} }")
        assertValid("#{{}}")
        assertValid("#{ {:} }")
    }

    fun testLiteralForms() {
        assertValid("#{'abc' + 1}")
        assertValid("#{'it''s'}")
        // 字符串字面量里的内层 `#{...}` 只是普通文本，整体是合法字面量
        assertValid("#{'#{Hello World}'}")
        assertValid("#{0x1F + 10L + 1.5e3 + 2f}")
    }

    fun testFrameworkVariableNames() {
        assertValid("#{add(\$req\$.getBaseUrl(), 4)}")
        assertValid("#{#root.method(#p0)}")
        assertValid("#{#time() + #offset}")
        assertValid("#{chunk_success(\$body\$)}")
    }

    /** 框架扩展：父表达式中的嵌套片段整体是一个操作数。 */
    fun testNestedFragmentActsAsOperand() {
        assertValid("#{#{#key}}")
        assertValid("#{#{#a} + 1}")
    }

    /** 用户反馈场景：`name::type` / `$n::type` 是参数类型组合，不是语法错误。 */
    fun testParameterTypeCombination() {
        assertValid("#{keyword::type}")
        assertValid("#{\$0::type}")
        assertValid("#{param::type + '-' + \$0::type}")
    }

    /** 用户反馈场景：函数调用后的顶层 `, p` 不是合法 SpEL。 */
    fun testSecondTopLevelExpressionIsProblem() {
        assertProblem("#{add(\$req\$.getBaseUrl(), 4), p}", "只能包含一个表达式")
    }

    fun testEmptyArguments() {
        assertProblem("#{#fn(, 1)}", "函数参数不能为空")
        assertProblem("#{#fn(1,, 2)}", "参数之间缺少表达式")
        assertProblem("#{#fn(1,)}", "最后一个参数不能为空")
        assertProblem("#{#fn(1 2)}", "参数之间缺少 ','")
    }

    fun testMissingOperands() {
        assertProblem("#{#fn(1 +)}", "缺少操作数")
        assertProblem("#{#a + }", "表达式不完整")
        assertProblem("#{.a}", "缺少操作数")
    }

    fun testMemberAccessProblems() {
        assertProblem("#{#a.}", "后缺少属性或方法名")
        assertProblem("#{#a?.}", "后缺少属性或方法名")
        assertProblem("#{#fn(request., 4)}", "后缺少属性或方法名")
    }

    fun testEmptyExpressionIsProblem() {
        assertProblem("#{}", "表达式不能为空")
    }

    fun testTernaryWithoutColon() {
        assertProblem("#{#a ? 1}", "三目运算符缺少 ':'")
    }

    fun testTypeAndConstructorProblems() {
        assertProblem("#{T()}", "T() 中缺少类型名")
        assertProblem("#{new Foo}", "构造器调用缺少参数列表")
    }

    /** SpEL 的 instanceof 右操作数必须是 T(类型)，裸类名不合法。 */
    fun testInstanceofRequiresTypeReference() {
        assertProblem("#{#obj instanceof Object}", "instanceof 右侧必须是 T(类型)")
        assertProblem("#{#obj instanceof}", "instanceof 右侧必须是 T(类型)")
    }

    fun testInlineCollectionProblems() {
        assertProblem("#{ {1, a: 2} }", "内联 List 与 Map 不能混用")
        assertProblem("#{ {a:} }", "':' 后缺少表达式")
    }

    fun testIndexerProblems() {
        assertProblem("#{#a[]}", "索引访问缺少下标表达式")
    }

    fun testExtraContentAfterExpression() {
        assertProblem("#{'a' 'b'}", "多余的")
        assertProblem("#{1 < 2 < 3}", "多余的")
        assertProblem("#{#fn(1) 2}", "多余的")
        assertProblem("#{a::typeX}", "多余的")
    }

    fun testInvalidVariableAndBean() {
        assertProblem("#{#}", "'#' 后缺少变量或函数名")
        assertProblem("#{@}", "'@' 后缺少 Bean 名称")
    }

    fun testCollectionSelectionWithoutExpression() {
        assertProblem("#{#list.?[]}", "后缺少表达式")
    }

    /** 结构问题由 scanner 报告，解析器不再叠加语法错误。 */
    fun testScannerProblemsSuppressSyntaxParsing() {
        val literal = "\"#{#fn(1, 2}\""
        val fragment = scanner.scanLiteral(literal).single()

        assertTrue(fragment.problems.isNotEmpty())
        assertTrue(parser.problemsOf(literal, fragment).isEmpty())
    }

    private fun assertValid(expression: String) {
        assertTrue("$expression 不应报错", problemsOf(expression).isEmpty())
    }

    private fun assertProblem(expression: String, expected: String) {
        val problems = problemsOf(expression)
        assertTrue(
            "$expression 应报错「$expected」，实际: ${problems.map { it.message }}",
            problems.any { it.message.contains(expected) }
        )
    }

    private fun problemsOf(expression: String): List<SpelProblem> {
        val literal = "\"$expression\""
        val fragment = scanner.scanLiteral(literal).single()
        return parser.problemsOf(literal, fragment)
    }
}
