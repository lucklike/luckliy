package com.luckyframework.idea.spel.inspection

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.util.InheritanceUtil
import com.luckyframework.idea.spel.completion.SpelElementContext
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelExpressionText
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelSyntaxParser
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.symbol.SpelContext

/**
 * 成员访问问题的类别，决定检查的默认严重级别。
 */
enum class SpelMemberProblemKind {
    /** 接收者类型明确，类型中不存在该成员，运行期一定失败 */
    MEMBER_NOT_FOUND,

    /** 接收者类型是 `Object`，无法静态校验成员，只能提示 */
    MEMBER_UNVERIFIABLE
}

/**
 * 一个成员访问问题；[range] 相对所在字符串字面量的源码文本。
 */
data class SpelMemberProblem(
    val kind: SpelMemberProblemKind,
    val range: TextRange,
    val message: String
)

/**
 * `.`（含安全导航 `?.`）之后的成员检查。
 *
 * 接收者类型能静态解析时，按真实类校验成员是否存在：
 * - 方法调用要求类（含继承体系）中存在同名方法；
 * - 属性访问要求存在同名字段，或 `getXxx`/`isXxx` 形式的 getter
 *   （与成员导航、补全的可见成员规则一致）。
 *
 * 选择/投影方括号内的 `#this`、裸标识符与裸函数按当前元素类型校验
 * （`users.?[age > 1]`、`.![name.length()]`），元素类型无法推导时保持静默。
 *
 * 保守放行的场景：接收者是 `Map`（成员是动态 key）、泛型类型变量、
 * 数组、集合的 `size`/`length` 属性（SpEL 对数组 `length` 有特判），
 * 以及类型完全无法推导的表达式。接收者类型明确是 `Object` 时只给警告。
 */
class SpelMemberAccessChecker(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val syntax: SpelSyntaxParser = SpelSyntaxParser()
) {

    fun check(literalText: String, context: SpelContext, project: Project): List<SpelMemberProblem> {
        val problems = ArrayList<SpelMemberProblem>()
        for (fragment in scanner.scanLiteral(literalText)) {
            collect(literalText, fragment, context, project, problems)
        }
        return problems
    }

    private fun collect(
        literalText: String,
        fragment: SpelFragment,
        context: SpelContext,
        project: Project,
        problems: MutableList<SpelMemberProblem>
    ) {
        // 结构或语法不完整的片段交给语法高亮，避免在错误文本上叠加成员告警
        if (fragment.problems.isEmpty() && syntax.problemsOf(literalText, fragment).isEmpty()) {
            checkFragment(literalText, fragment, context, project, problems)
        }
        fragment.children.forEach { collect(literalText, it, context, project, problems) }
    }

    private fun checkFragment(
        literalText: String,
        fragment: SpelFragment,
        context: SpelContext,
        project: Project,
        problems: MutableList<SpelMemberProblem>
    ) {
        val nested = fragment.children.map { it.textRange }
        val tokens = tokenizer.tokenize(literalText, fragment)
            .filter { token -> nested.none { it.contains(token.range) } }
        if (tokens.isEmpty()) {
            return
        }
        val typeResolver = SpelTypeResolver(project, context)
        for (index in tokens.indices) {
            checkMember(literalText, fragment, tokens, index, context, typeResolver, problems)
        }
    }

    private fun checkMember(
        literalText: String,
        fragment: SpelFragment,
        tokens: List<SpelToken>,
        index: Int,
        context: SpelContext,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelMemberProblem>
    ) {
        val token = tokens[index]
        val elementType = elementTypeAt(literalText, fragment, token.range.startOffset, typeResolver)
        when (token.type) {
            SpelTokenType.IDENTIFIER ->
                checkMemberAccess(literalText, fragment, tokens, index, elementType, context, typeResolver, problems)

            SpelTokenType.ROOT_VARIABLE ->
                checkElementMember(tokens, index, elementType, context, typeResolver, problems)

            // `name.length()` 的 `length` 是成员方法，不按元素裸函数校验
            SpelTokenType.FUNCTION ->
                if (isMemberAccess(tokens.getOrNull(index - 1))) {
                    checkMemberAccess(literalText, fragment, tokens, index, elementType, context, typeResolver, problems)
                } else {
                    checkElementMember(tokens, index, elementType, context, typeResolver, problems)
                }

            else -> Unit
        }
    }

    /** 指针位置所在的选择/投影元素类型；不在元素上下文中时返回 null。 */
    private fun elementTypeAt(
        literalText: String,
        fragment: SpelFragment,
        offset: Int,
        typeResolver: SpelTypeResolver
    ): PsiType? {
        val scopes = SpelElementScopes.enclosing(literalText, fragment.expressionRange.startOffset, offset)
        if (scopes.isEmpty()) {
            return null
        }
        return SpelElementContext.elementType(scopes, typeResolver)
    }

    private fun checkMemberAccess(
        literalText: String,
        fragment: SpelFragment,
        tokens: List<SpelToken>,
        index: Int,
        elementType: PsiType?,
        context: SpelContext,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelMemberProblem>
    ) {
        val token = tokens[index]
        val access = tokens.getOrNull(index - 1) ?: return
        if (!isMemberAccess(access)) {
            return
        }
        // `#ns.fn(...)` 是命名空间函数，交给函数调用检查；是否命名空间由符号表确认，
        // `#user.name`/`#this.name` 这类变量形式继续按接收者类型校验
        val qualifier = tokens.getOrNull(index - 2)
        if (qualifier != null && qualifier.type == SpelTokenType.NAMESPACE &&
            context.namespaces(qualifier.text.removePrefix("#")).isNotEmpty()
        ) {
            return
        }
        val expressionStart = fragment.expressionRange.startOffset
        val accessOffset = access.range.startOffset
        if (accessOffset <= expressionStart) {
            return
        }
        val prefix = literalText.substring(expressionStart, accessOffset)
        val receiverStart = expressionStart + SpelExpressionText.receiverStart(prefix)
        val receiver = literalText.substring(receiverStart, accessOffset).trim()
        if (receiver.isEmpty()) {
            return
        }
        val name = token.text
        val methodCall = tokens.getOrNull(index + 1)?.type == SpelTokenType.LPAREN
        // 数组的 `length` 是 SpEL 特判的伪字段，`clone` 等来自 Object：都能访问
        val receiverType = typeResolver.resolvePsiType(receiver, elementType)
        if (receiverType is PsiArrayType &&
            ((!methodCall && name == "length") || name in OBJECT_MEMBER_NAMES)
        ) {
            return
        }
        val owner = typeResolver.resolve(receiver, elementType)
        if (owner == null) {
            checkUnknownReceiver(context, receiver, name, methodCall, token, problems)
            return
        }
        // 泛型类型变量的成员取决于运行期实参，无法静态校验
        if (owner is PsiTypeParameter) {
            return
        }
        if (isJavaLangObject(owner)) {
            if (!hasMember(owner, name, methodCall)) {
                problems += SpelMemberProblem(
                    SpelMemberProblemKind.MEMBER_UNVERIFIABLE,
                    token.range,
                    "接收者类型为 Object，无法校验成员 '$name'"
                )
            }
            return
        }
        // Map 的成员名是动态 key（`#{$query$.foo}`），不能按类成员判定
        if (isInheritor(owner, "java.util.Map", typeResolver)) {
            return
        }
        // 集合的 `size`/`length` 是 SpEL 常见写法，数组另有 `length` 特判，统一放行
        if (!methodCall && isInheritor(owner, "java.util.Collection", typeResolver) &&
            name in COLLECTION_PROPERTIES
        ) {
            return
        }
        if (hasMember(owner, name, methodCall)) {
            return
        }
        problems += SpelMemberProblem(
            SpelMemberProblemKind.MEMBER_NOT_FOUND,
            token.range,
            messageOf(owner, name, methodCall)
        )
    }

    /**
     * 选择/投影方括号内的裸标识符与裸函数：按元素类型的成员校验。
     *
     * 裸函数也可能是全局/命名空间函数（`users.?[str_eq(#this.name, 'a')]`）：
     * 运行期 `MethodSpaceMethodResolver` 在元素方法未命中时会继续从变量表
     * 查找 static Method，因此函数表命中时同样放行。
     *
     * `#fn()`/`#var`/`$body$` 仍走全局符号解析；元素类型无法推导、
     * Map 元素（动态 key）与泛型类型变量保守放行。
     */
    private fun checkElementMember(
        tokens: List<SpelToken>,
        index: Int,
        elementType: PsiType?,
        context: SpelContext,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelMemberProblem>
    ) {
        if (elementType == null) {
            return
        }
        val token = tokens[index]
        if (token.text.startsWith("#") || token.text.startsWith("$")) {
            return
        }
        // `name::type` 的 type 后缀属于 Root 变量组合，不是元素成员
        if (isTypeSuffix(tokens, index)) {
            return
        }
        val owner = (elementType as? PsiClassType)?.resolve() ?: return
        if (owner is PsiTypeParameter || isJavaLangObject(owner)) {
            return
        }
        if (isInheritor(owner, "java.util.Map", typeResolver)) {
            return
        }
        val methodCall = token.type == SpelTokenType.FUNCTION ||
            tokens.getOrNull(index + 1)?.type == SpelTokenType.LPAREN
        if (!methodCall && isInheritor(owner, "java.util.Collection", typeResolver) &&
            token.text in COLLECTION_PROPERTIES
        ) {
            return
        }
        if (hasMember(owner, token.text, methodCall)) {
            return
        }
        // 裸调用在元素方法未命中时仍可能是全局函数，与运行期语义一致
        if (methodCall && context.functions(token.text, null).isNotEmpty()) {
            return
        }
        problems += SpelMemberProblem(
            SpelMemberProblemKind.MEMBER_NOT_FOUND,
            token.range,
            messageOf(owner, token.text, methodCall)
        )
    }

    /** `::` 之后的 `type` 是前一个 `name::type` 组合的一部分。 */
    private fun isTypeSuffix(tokens: List<SpelToken>, index: Int): Boolean {
        val previous = tokens.getOrNull(index - 1) ?: return false
        return previous.type == SpelTokenType.OPERATOR && previous.text == "::"
    }

    /**
     * 接收者类型无法解析时的降级处理。
     *
     * 只有符号表明确声明为 `Object` 时才给出警告（如未配置 `@ConvertMetaType`
     * 的 `$body$`），其余未知类型（未解析变量、动态表达式）保持静默，
     * 避免在类型推导能力之外误报。
     */
    private fun checkUnknownReceiver(
        context: SpelContext,
        receiver: String,
        name: String,
        methodCall: Boolean,
        token: SpelToken,
        problems: MutableList<SpelMemberProblem>
    ) {
        val declared = declaredTypeOf(context, receiver) ?: return
        if (!isObjectTypeText(declared) || name in OBJECT_MEMBER_NAMES) {
            return
        }
        problems += SpelMemberProblem(
            SpelMemberProblemKind.MEMBER_UNVERIFIABLE,
            token.range,
            "接收者类型为 Object，无法校验成员 '$name'"
        )
    }

    /**
     * 接收者在符号表中的声明类型文本：上下文推导类型优先于符号表类型。
     *
     * 按书写形式取符号：`#name` 只查普通变量，裸 `name` 只查 Root 变量。
     * 形式写错时返回 null，由未解析检查报告书写形式错误，避免连锁误报。
     */
    private fun declaredTypeOf(context: SpelContext, receiver: String): String? {
        val hashed = receiver.startsWith("#")
        val name = receiver.removePrefix("#").removeSuffix("()").trim()
        if (!SYMBOL_NAME.matches(name)) {
            return null
        }
        val symbols = if (hashed) context.variables(name) else context.rootVariables(name)
        val symbol = symbols.firstOrNull() ?: return null
        return symbol.resolvedType?.presentableText ?: symbol.type
    }

    private fun isObjectTypeText(type: String): Boolean {
        val simple = type.substringBefore('<').trim().removeSuffix("[]").trim()
        return simple == "Object" || simple == "java.lang.Object"
    }

    /** 方法要求同名方法；属性要求同名字段或零参 getter。 */
    private fun hasMember(owner: PsiClass, name: String, methodCall: Boolean): Boolean {
        if (methodCall) {
            return owner.findMethodsByName(name, true).isNotEmpty()
        }
        if (owner.findFieldByName(name, true) != null) {
            return true
        }
        return GETTER_PREFIXES.any { prefix ->
            val getter = prefix + name.replaceFirstChar { it.uppercaseChar() }
            owner.findMethodsByName(getter, true).any { it.parameterList.parametersCount == 0 }
        }
    }

    private fun messageOf(owner: PsiClass, name: String, methodCall: Boolean): String {
        val label = owner.name ?: owner.qualifiedName ?: "类型"
        return if (methodCall) {
            "类 '$label' 中不存在方法 '$name'"
        } else {
            "类 '$label' 中不存在属性 '$name'"
        }
    }

    private fun isJavaLangObject(owner: PsiClass): Boolean =
        owner.qualifiedName == "java.lang.Object"

    private fun isInheritor(owner: PsiClass, typeName: String, typeResolver: SpelTypeResolver): Boolean {
        val target = typeResolver.findClass(typeName) ?: return false
        return InheritanceUtil.isInheritorOrSelf(owner, target, true)
    }

    /** 成员访问符：`.` 或安全导航 `?.`。 */
    private fun isMemberAccess(token: SpelToken?): Boolean =
        token != null && (token.type == SpelTokenType.DOT ||
            (token.type == SpelTokenType.OPERATOR && token.text == "?."))

    private companion object {
        /** 符号名：变量、Root 变量与 `name::type` 组合。 */
        val SYMBOL_NAME = Regex("""[A-Za-z_$][A-Za-z0-9_$]*(?:::type)?""")

        val GETTER_PREFIXES = listOf("get", "is")

        /** 集合上按属性书写的常用成员，SpEL 可能接受，保守放行。 */
        val COLLECTION_PROPERTIES = setOf("size", "length")

        /** `Object` 自身可访问的成员：类型无法确定时访问这些成员不会失败。 */
        val OBJECT_MEMBER_NAMES = setOf(
            "class", "equals", "hashCode", "toString",
            "getClass", "clone", "finalize", "wait", "notify", "notifyAll"
        )
    }
}
