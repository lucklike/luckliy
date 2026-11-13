package com.luckyframework.idea.spel.lexer

import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelProblem
import com.luckyframework.idea.spel.model.SpelProblemKind

/**
 * 按 Spring 官方 SpEL 语法做递归下降校验。
 *
 * 语法来源：`InternalSpelExpressionParser` 中的规则注释，覆盖
 * `expression`、`logicalOrExpression`、`logicalAndExpression`、
 * `relationalExpression`、`sumExpression`、`productExpression`、
 * `powerExpression`、`unaryExpression`、`primaryExpression`、`node`、
 * `dottedNode`、`startNode`、内联 List/Map、构造器、类型引用与集合投影/选择。
 *
 * 设计取舍：
 * - 只报告第一个语法错误，避免在错误文本上继续推导出连锁误报；
 * - 扫描阶段已发现结构问题（未闭合、括号不配平、未闭合字符串）的片段
 *   不再参与语法分析，防止重复报告；
 * - 函数尾逗号（`f(1,)`）仍然视为错误，与既有诊断保持一致；
 * - 不依赖 spring-expression，未知字符降级为普通 token 后由语法规则判定。
 */
class SpelSyntaxParser(
    private val lexer: SpelSyntaxLexer = SpelSyntaxLexer()
) {

    /**
     * 返回片段内容中的语法错误，最多一个；扫描阶段已有问题的片段返回空。
     */
    fun problemsOf(literalText: String, fragment: SpelFragment): List<SpelProblem> {
        if (!fragment.closed || fragment.problems.isNotEmpty()) {
            return emptyList()
        }
        val tokens = lexer.lex(literalText, fragment)
        if (tokens.isEmpty()) {
            return listOf(
                SpelProblem(
                    SpelProblemKind.SYNTAX_ERROR,
                    fragment.textRange,
                    "SpEL 表达式不能为空"
                )
            )
        }
        return try {
            Parser(tokens, fragment.expressionRange.endOffset).parse()
            emptyList()
        } catch (failure: SyntaxFailure) {
            listOf(SpelProblem(failure.kind, failure.range, failure.message))
        }
    }

    fun hasProblems(literalText: String, fragment: SpelFragment): Boolean =
        problemsOf(literalText, fragment).isNotEmpty()

    private class SyntaxFailure(
        val kind: SpelProblemKind,
        val range: TextRange,
        override val message: String
    ) : RuntimeException(message)

    /**
     * 官方语法的递归下降实现。
     *
     * 每个方法对应一条官方规则；遇到不可能的 token 组合时抛出
     * [SyntaxFailure]，由 [problemsOf] 转换为可展示的问题。
     */
    private class Parser(
        private val tokens: List<SpelSyntaxToken>,
        private val endOffset: Int
    ) {

        private var index = 0
        private var depth = 0

        fun parse() {
            expression()
            val extra = peek()
            if (extra != null) {
                fail(
                    SpelProblemKind.SYNTAX_ERROR,
                    extra.range,
                    "多余的 '${extra.text}'，一个 SpEL 片段只能包含一个表达式"
                )
            }
        }

        // expression : logicalOrExpression
        //   ( (ASSIGN logicalOrExpression) | (ELVIS expression)
        //   | (QMARK expression COLON expression) )? ;
        private fun expression(): Unit = nested {
            logicalOr()
            val token = peek() ?: return@nested
            when {
                token.type == SpelSyntaxTokenType.OPERATOR && token.text == "=" -> {
                    advance()
                    logicalOr()
                }

                token.type == SpelSyntaxTokenType.ELVIS -> {
                    advance()
                    expression()
                }

                token.type == SpelSyntaxTokenType.QUESTION -> {
                    advance()
                    expression()
                    expect(SpelSyntaxTokenType.COLON, "三目运算符缺少 ':'")
                    expression()
                }
            }
        }

        // logicalOrExpression : logicalAndExpression (OR logicalAndExpression)* ;
        private fun logicalOr() {
            logicalAnd()
            while (isOperator("||") || isWord("or")) {
                advance()
                logicalAnd()
            }
        }

        // logicalAndExpression : relationalExpression (AND relationalExpression)* ;
        private fun logicalAnd() {
            relational()
            while (isOperator("&&") || isWord("and")) {
                advance()
                relational()
            }
        }

        // relationalExpression : sumExpression (relationalOperator sumExpression)? ;
        // instanceof 的右操作数在官方语法中是类型引用 T(...)，裸类名不合法。
        private fun relational() {
            sum()
            val token = peek() ?: return
            if (token.type == SpelSyntaxTokenType.IDENTIFIER && token.text.equals("instanceof", true)) {
                advance()
                if (peek()?.type != SpelSyntaxTokenType.IDENTIFIER || peek()?.text != "T" ||
                    peek(1)?.type != SpelSyntaxTokenType.LPAREN
                ) {
                    fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "instanceof 右侧必须是 T(类型)")
                }
                typeReference()
                return
            }
            val relational = (token.type == SpelSyntaxTokenType.OPERATOR && token.text in RELATIONAL_OPERATORS) ||
                (token.type == SpelSyntaxTokenType.IDENTIFIER && token.text.lowercase() in RELATIONAL_WORDS)
            if (relational) {
                advance()
                sum()
            }
        }

        // sumExpression : productExpression ((PLUS | MINUS) productExpression)* ;
        private fun sum() {
            product()
            while (isOperator("+", "-")) {
                advance()
                product()
            }
        }

        // productExpression : powerExpression ((STAR | DIV | MOD) powerExpression)* ;
        private fun product() {
            power()
            while (isOperator("*", "/", "%") || isWord("div", "mod")) {
                advance()
                power()
            }
        }

        // powerExpression : unaryExpression (POWER unaryExpression)? (INC | DEC)? ;
        private fun power() {
            unary()
            if (isOperator("^", "**")) {
                advance()
                unary()
            }
            if (isOperator("++", "--")) {
                advance()
            }
        }

        // unaryExpression : (NOT | PLUS | MINUS | INC | DEC) unaryExpression | primaryExpression ;
        private fun unary() {
            if (isOperator("!", "+", "-", "++", "--") || isWord("not")) {
                advance()
                unary()
            } else {
                primary()
            }
        }

        // primaryExpression : startNode (node)* ;
        private fun primary(): Unit = nested {
            startNode()
            while (true) {
                when (peek()?.type) {
                    SpelSyntaxTokenType.DOT,
                    SpelSyntaxTokenType.SAFE_NAVI -> {
                        val access = advance()!!
                        dottedNode(access)
                    }

                    SpelSyntaxTokenType.LBRACKET -> indexer()

                    else -> return@nested
                }
            }
        }

        // startNode : parenExpr | literal | type | methodOrProperty | functionOrVar
        //   | projection | selection | indexer | constructor | beanRef | inlineListOrMap ;
        private fun startNode() {
            val token = peek()
                ?: fail(SpelProblemKind.SYNTAX_ERROR, TextRange(endOffset, endOffset), "表达式不完整，缺少操作数")
            when (token.type) {
                SpelSyntaxTokenType.NUMBER,
                SpelSyntaxTokenType.STRING -> advance()

                SpelSyntaxTokenType.IDENTIFIER -> when {
                    token.text.equals("true", true) ||
                        token.text.equals("false", true) ||
                        token.text.equals("null", true) -> advance()

                    token.text == "new" -> constructor()

                    token.text == "T" && peek(1)?.type == SpelSyntaxTokenType.LPAREN -> typeReference()

                    else -> {
                        advance()
                        if (peek()?.type == SpelSyntaxTokenType.LPAREN) {
                            arguments()
                        }
                    }
                }

                SpelSyntaxTokenType.VARIABLE -> {
                    if (token.text.length <= 1) {
                        fail(SpelProblemKind.SYNTAX_ERROR, token.range, "'#' 后缺少变量或函数名")
                    }
                    advance()
                    if (peek()?.type == SpelSyntaxTokenType.LPAREN) {
                        arguments()
                    }
                }

                SpelSyntaxTokenType.BEAN -> {
                    if (token.text.length <= 1) {
                        fail(SpelProblemKind.SYNTAX_ERROR, token.range, "'@' 后缺少 Bean 名称")
                    }
                    advance()
                }

                SpelSyntaxTokenType.LPAREN -> {
                    advance()
                    expression()
                    expect(SpelSyntaxTokenType.RPAREN, "缺少 ')'")
                }

                SpelSyntaxTokenType.LBRACKET -> indexer()

                SpelSyntaxTokenType.PROJECTION,
                SpelSyntaxTokenType.SELECTION,
                SpelSyntaxTokenType.SELECT_FIRST,
                SpelSyntaxTokenType.SELECT_LAST -> selectionLike()

                SpelSyntaxTokenType.LCURLY -> inlineListOrMap()

                else -> fail(
                    SpelProblemKind.SYNTAX_ERROR,
                    token.range,
                    "表达式缺少操作数：无法解析 '${token.text}'"
                )
            }
        }

        // node : (DOT dottedNode) | (SAFE_NAVI dottedNode) | indexer ;
        private fun dottedNode(access: SpelSyntaxToken) {
            val token = peek()
            when {
                token == null -> fail(
                    SpelProblemKind.MISSING_MEMBER_NAME,
                    TextRange(endOffset, endOffset),
                    "'${access.text}' 后缺少属性或方法名"
                )

                token.type == SpelSyntaxTokenType.IDENTIFIER -> {
                    advance()
                    if (peek()?.type == SpelSyntaxTokenType.LPAREN) {
                        arguments()
                    }
                }

                token.type == SpelSyntaxTokenType.VARIABLE && token.text.length > 1 -> {
                    advance()
                    if (peek()?.type == SpelSyntaxTokenType.LPAREN) {
                        arguments()
                    }
                }

                token.type == SpelSyntaxTokenType.LBRACKET -> indexer()

                token.type == SpelSyntaxTokenType.PROJECTION ||
                    token.type == SpelSyntaxTokenType.SELECTION ||
                    token.type == SpelSyntaxTokenType.SELECT_FIRST ||
                    token.type == SpelSyntaxTokenType.SELECT_LAST -> selectionLike()

                else -> fail(
                    SpelProblemKind.MISSING_MEMBER_NAME,
                    token.range,
                    "'${access.text}' 后缺少属性或方法名"
                )
            }
        }

        // indexer : LSQUARE expression RSQUARE ;
        private fun indexer() {
            expect(SpelSyntaxTokenType.LBRACKET, "缺少 '['")
            if (peek()?.type == SpelSyntaxTokenType.RBRACKET) {
                fail(SpelProblemKind.SYNTAX_ERROR, peek()!!.range, "索引访问缺少下标表达式")
            }
            expression()
            expect(SpelSyntaxTokenType.RBRACKET, "索引访问缺少 ']'")
        }

        // projection/selection : PROJECT/SELECT expression RSQUARE ;
        private fun selectionLike() {
            val token = advance() ?: return
            if (peek() == null || peek()?.type == SpelSyntaxTokenType.RBRACKET) {
                fail(SpelProblemKind.SYNTAX_ERROR, token.range, "'${token.text}' 后缺少表达式")
            }
            expression()
            expect(SpelSyntaxTokenType.RBRACKET, "'${token.text}' 缺少 ']'")
        }

        // methodArgs : LPAREN (argument (COMMA argument)* COMMA?)? RPAREN ;
        // 与既有诊断保持一致：尾逗号视为错误。
        private fun arguments() {
            expect(SpelSyntaxTokenType.LPAREN, "缺少 '('")
            if (peek()?.type == SpelSyntaxTokenType.RPAREN) {
                advance()
                return
            }
            while (true) {
                val token = peek()
                    ?: fail(SpelProblemKind.SYNTAX_ERROR, TextRange(endOffset, endOffset), "参数列表缺少 ')'")
                when (token.type) {
                    SpelSyntaxTokenType.COMMA ->
                        fail(SpelProblemKind.EMPTY_ARGUMENT, token.range, "函数参数不能为空")

                    SpelSyntaxTokenType.RPAREN ->
                        fail(SpelProblemKind.EMPTY_ARGUMENT, token.range, "多余的逗号，最后一个参数不能为空")

                    else -> expression()
                }
                when (peek()?.type) {
                    SpelSyntaxTokenType.COMMA -> {
                        advance()
                        when (peek()?.type) {
                            SpelSyntaxTokenType.COMMA ->
                                fail(SpelProblemKind.EMPTY_ARGUMENT, peek()!!.range, "多余的逗号，参数之间缺少表达式")

                            SpelSyntaxTokenType.RPAREN ->
                                fail(SpelProblemKind.EMPTY_ARGUMENT, peek()!!.range, "多余的逗号，最后一个参数不能为空")

                            null -> fail(
                                SpelProblemKind.SYNTAX_ERROR,
                                TextRange(endOffset, endOffset),
                                "参数列表缺少 ')'"
                            )

                            else -> Unit
                        }
                    }

                    SpelSyntaxTokenType.RPAREN -> {
                        advance()
                        return
                    }

                    null -> fail(
                        SpelProblemKind.SYNTAX_ERROR,
                        TextRange(endOffset, endOffset),
                        "参数列表缺少 ')'"
                    )

                    else -> fail(SpelProblemKind.SYNTAX_ERROR, peek()!!.range, "参数之间缺少 ','")
                }
            }
        }

        // list = LCURLY (element (COMMA element)*) RCURLY
        // map  = LCURLY (key ':' value (COMMA key ':' value)*) RCURLY
        private fun inlineListOrMap() {
            advance() // LCURLY
            if (peek()?.type == SpelSyntaxTokenType.RCURLY) {
                advance()
                return
            }
            if (peek()?.type == SpelSyntaxTokenType.COLON) {
                advance()
                expect(SpelSyntaxTokenType.RCURLY, "内联 Map 的 ':' 后缺少 '}'")
                return
            }
            expression()
            when (peek()?.type) {
                SpelSyntaxTokenType.RCURLY -> advance()

                SpelSyntaxTokenType.COLON -> {
                    advance()
                    if (peek()?.type == SpelSyntaxTokenType.RCURLY || peek() == null) {
                        fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "':' 后缺少表达式")
                    }
                    expression()
                    while (peek()?.type == SpelSyntaxTokenType.COMMA) {
                        advance()
                        when (peek()?.type) {
                            SpelSyntaxTokenType.RCURLY, null ->
                                fail(SpelProblemKind.EMPTY_ARGUMENT, rangeOf(peek()), "多余的逗号，最后一个元素不能为空")

                            else -> expression()
                        }
                        expect(SpelSyntaxTokenType.COLON, "内联 Map 的键缺少 ':'")
                        if (peek()?.type == SpelSyntaxTokenType.RCURLY || peek()?.type == SpelSyntaxTokenType.COMMA ||
                            peek() == null
                        ) {
                            fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "':' 后缺少表达式")
                        }
                        expression()
                    }
                    expect(SpelSyntaxTokenType.RCURLY, "内联 Map 缺少 '}'")
                }

                SpelSyntaxTokenType.COMMA -> {
                    advance()
                    if (peek()?.type == SpelSyntaxTokenType.RCURLY || peek() == null) {
                        fail(SpelProblemKind.EMPTY_ARGUMENT, rangeOf(peek()), "多余的逗号，最后一个元素不能为空")
                    }
                    expression()
                    if (peek()?.type == SpelSyntaxTokenType.COLON) {
                        fail(SpelProblemKind.SYNTAX_ERROR, peek()!!.range, "内联 List 与 Map 不能混用")
                    }
                    while (peek()?.type == SpelSyntaxTokenType.COMMA) {
                        advance()
                        when (peek()?.type) {
                            SpelSyntaxTokenType.RCURLY, null ->
                                fail(SpelProblemKind.EMPTY_ARGUMENT, rangeOf(peek()), "多余的逗号，最后一个元素不能为空")

                            SpelSyntaxTokenType.COLON ->
                                fail(SpelProblemKind.SYNTAX_ERROR, peek()!!.range, "内联 List 与 Map 不能混用")

                            else -> {
                                expression()
                                if (peek()?.type == SpelSyntaxTokenType.COLON) {
                                    fail(SpelProblemKind.SYNTAX_ERROR, peek()!!.range, "内联 List 与 Map 不能混用")
                                }
                            }
                        }
                    }
                    expect(SpelSyntaxTokenType.RCURLY, "内联 List 缺少 '}'")
                }

                null -> fail(SpelProblemKind.SYNTAX_ERROR, TextRange(endOffset, endOffset), "内联 List/Map 缺少 '}'")

                else -> fail(
                    SpelProblemKind.SYNTAX_ERROR,
                    peek()!!.range,
                    "内联 List/Map 的元素之间缺少 ','"
                )
            }
        }

        // constructor : 'new' qualifiedId (ctorArgs | arrayDimensions) ;
        private fun constructor() {
            advance() // new
            if (peek()?.type != SpelSyntaxTokenType.IDENTIFIER) {
                fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "new 后缺少类名")
            }
            qualifiedId()
            if (peek()?.type == SpelSyntaxTokenType.LBRACKET) {
                while (peek()?.type == SpelSyntaxTokenType.LBRACKET) {
                    advance()
                    if (peek()?.type != SpelSyntaxTokenType.RBRACKET) {
                        expression()
                    }
                    expect(SpelSyntaxTokenType.RBRACKET, "数组维度缺少 ']'")
                }
                if (peek()?.type == SpelSyntaxTokenType.LCURLY) {
                    inlineListOrMap()
                }
            } else {
                if (peek()?.type != SpelSyntaxTokenType.LPAREN) {
                    fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "构造器调用缺少参数列表")
                }
                arguments()
            }
        }

        // type : 'T' LPAREN qualifiedId (LSQUARE RSQUARE)* RPAREN ;
        private fun typeReference() {
            advance() // T
            expect(SpelSyntaxTokenType.LPAREN, "T() 缺少 '('")
            if (peek()?.type != SpelSyntaxTokenType.IDENTIFIER) {
                fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "T() 中缺少类型名")
            }
            qualifiedId()
            while (peek()?.type == SpelSyntaxTokenType.LBRACKET) {
                advance()
                expect(SpelSyntaxTokenType.RBRACKET, "数组维度缺少 ']'")
            }
            expect(SpelSyntaxTokenType.RPAREN, "T() 缺少 ')'")
        }

        private fun qualifiedId() {
            if (peek()?.type != SpelSyntaxTokenType.IDENTIFIER) {
                fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "缺少类名")
            }
            advance()
            while (peek()?.type == SpelSyntaxTokenType.DOT && peek(1)?.type == SpelSyntaxTokenType.IDENTIFIER) {
                advance()
                advance()
            }
        }

        private fun expect(type: SpelSyntaxTokenType, message: String): SpelSyntaxToken {
            val token = peek()
            if (token == null || token.type != type) {
                fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(token), message)
            }
            return advance()!!
        }

        private fun peek(offset: Int = 0): SpelSyntaxToken? = tokens.getOrNull(index + offset)

        private fun advance(): SpelSyntaxToken? = tokens.getOrNull(index++)

        private fun isOperator(vararg texts: String): Boolean {
            val token = peek() ?: return false
            return token.type == SpelSyntaxTokenType.OPERATOR && token.text in texts
        }

        private fun isWord(vararg texts: String): Boolean {
            val token = peek() ?: return false
            return token.type == SpelSyntaxTokenType.IDENTIFIER && token.text.lowercase() in texts
        }

        private fun rangeOf(token: SpelSyntaxToken?): TextRange =
            token?.range ?: TextRange(endOffset, endOffset)

        private fun <T> nested(block: () -> T): T {
            if (depth >= MAX_DEPTH) {
                fail(SpelProblemKind.SYNTAX_ERROR, rangeOf(peek()), "SpEL 表达式嵌套层级过深")
            }
            depth++
            try {
                return block()
            } finally {
                depth--
            }
        }

        private fun fail(kind: SpelProblemKind, range: TextRange, message: String): Nothing =
            throw SyntaxFailure(kind, range, message)

        private companion object {
            const val MAX_DEPTH = 128

            val RELATIONAL_OPERATORS = setOf("==", "!=", "<", "<=", ">", ">=")

            val RELATIONAL_WORDS = setOf(
                "eq", "ne", "lt", "le", "gt", "ge", "between", "matches"
            )
        }
    }
}
