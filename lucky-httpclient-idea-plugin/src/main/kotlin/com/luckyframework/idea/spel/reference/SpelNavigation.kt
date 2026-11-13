package com.luckyframework.idea.spel.reference

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.luckyframework.idea.spel.completion.SpelElementContext
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.documentation.SpelDocumentation
import com.luckyframework.idea.spel.documentation.docCommentOf
import com.luckyframework.idea.spel.documentation.summaryOf
import com.luckyframework.idea.spel.documentation.withMethodDetails
import com.luckyframework.idea.spel.lexer.SpelElementScope
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin

/**
 * 把符号解析为可导航的声明（design D8）。
 *
 * 变量与函数按符号层解析；成员与类型按需解析 PSI；Bean 采用
 * 「类名首字母大写后反推」的启发式，无法解析时返回空列表，不抛异常。
 */
class SpelNavigation(
    private val project: Project,
    private val context: SpelContext,
    private val typeResolver: SpelTypeResolver = SpelTypeResolver(project, context),
    private val anchor: PsiElement? = null
) {

    fun variables(name: String): List<SpelDeclaration> =
        context.variables(name).map { describe(it) }

    fun functions(name: String, namespace: String?): List<SpelDeclaration> =
        context.functions(name, namespace).map { describe(it) }

    fun namespaces(name: String): List<SpelDeclaration> =
        context.namespaces(name).map { describe(it) }

    /** 名称是否是符号表登记的命名空间：区分 `#ns.fn()` 与 `#var.member`。 */
    internal fun hasNamespace(name: String): Boolean = context.namespaces(name).isNotEmpty()

    /** 名称是否在变量表中；`#name.` 形式的命名空间回退使用。 */
    internal fun hasVariable(name: String): Boolean = context.variables(name).isNotEmpty()

    fun types(qualifiedName: String): List<SpelDeclaration> {
        // 类型引用是明确写出的类：短名 Object 也要按 java.lang.Object 解析
        val psiClass = typeResolver.findTypeReference(qualifiedName) ?: return emptyList()
        return listOf(typeDeclaration(psiClass))
    }

    fun beans(name: String): List<SpelDeclaration> {
        if (name.isBlank()) {
            return emptyList()
        }
        return typeResolver.resolveBeanClasses(name).map { psiClass ->
            SpelDeclaration(
                project = project,
                name = name,
                kindLabel = "Bean",
                typeLabel = psiClass.qualifiedName,
                description = "Spring Bean 声明",
                target = psiClass,
                anchor = anchor,
                documentation = SpelDocumentation(
                    name = name,
                    category = "Bean",
                    type = psiClass.qualifiedName,
                    description = "Spring Bean 声明"
                )
            )
        }
    }

    /**
     * `.` 之后的成员：命名空间函数优先，其次按接收者类型查找字段与属性/方法。
     *
     * SpEL 的属性访问通过 getter 或字段反射完成，因此 private 字段与
     * `getXxx`/`isXxx` 属性方法都算作可见成员（Lombok 实体依赖这一点）。
     * [arguments] 是调用实参文本，用于在多个方法重载中选中实际调用的那个；
     * [scopes] 是成员所在的选择/投影方括号，`#this.age` 与 `name.length()`
     * 的接收者按当前元素类型解析。
     */
    fun members(
        qualifier: String,
        name: String,
        arguments: List<String>? = null,
        scopes: List<SpelElementScope> = emptyList()
    ): List<SpelDeclaration> {
        val namespace = context.ofKind(SpelSymbolKind.NAMESPACE)
            .firstOrNull { it.name == qualifier.removePrefix("#") }
        if (namespace != null) {
            return functions(name, namespace.name)
        }
        val owner = ownerOf(qualifier, elementTypeOf(scopes)) ?: return emptyList()
        return membersOf(owner, name, arguments)
    }

    /**
     * 选择/投影方括号内的裸标识符与裸函数：按当前元素类型的成员解析。
     *
     * `users.?[age]` 的 `age`、`users.![name.length()]` 的 `length` 都走这里。
     */
    fun elementMembers(
        scopes: List<SpelElementScope>,
        name: String,
        arguments: List<String>? = null
    ): List<SpelDeclaration> {
        val owner = typeResolver.classOf(elementTypeOf(scopes)) ?: return emptyList()
        return membersOf(owner, name, arguments)
    }

    /** 按接收者类查找字段、属性与公共方法。 */
    private fun membersOf(
        owner: PsiClass,
        name: String,
        arguments: List<String>?
    ): List<SpelDeclaration> {
        val result = ArrayList<SpelDeclaration>()
        // 字段优先：字段声明带源码注释，getter 只作为取不到的补充，避免同名双目标
        val field = owner.allFields.firstOrNull { it.name == name }
        if (field != null) {
            result += fieldDeclaration(field)
        } else {
            for (method in owner.allMethods) {
                if (method.isConstructor || method.parameterList.parametersCount != 0) {
                    continue
                }
                if (method.name == getterName(name, "get") || method.name == getterName(name, "is")) {
                    result += methodDeclaration(method)
                }
            }
        }
        val overloads = owner.allMethods.filter {
            !it.isConstructor && it.name == name && it.hasModifierProperty(PsiModifier.PUBLIC)
        }
        for (method in selectOverloads(overloads, arguments)) {
            result += methodDeclaration(method)
        }
        return result.distinctBy { it.target }
    }

    /** 选择/投影方括号内当前元素类型；不在元素上下文中时返回 null。 */
    private fun elementTypeOf(scopes: List<SpelElementScope>): PsiType? =
        if (scopes.isEmpty()) null else SpelElementContext.elementType(scopes, typeResolver)

    /** 调用实参可定位时只保留选中的重载，其余情况返回全部候选。 */
    private fun selectOverloads(methods: List<PsiMethod>, arguments: List<String>?): List<PsiMethod> {
        if (arguments == null || methods.size <= 1) {
            return methods
        }
        val selected = typeResolver.selectMethod(methods.toTypedArray(), arguments) ?: return methods
        return listOf(selected)
    }

    /** 接收者类型：`@bean` 按「类名首字母小写」启发式反推，其余交给类型解析器。 */
    private fun ownerOf(qualifier: String, elementType: PsiType? = null): PsiClass? {
        val trimmed = qualifier.trim()
        if (trimmed.startsWith("@")) {
            return typeResolver.resolveBeanClass(trimmed.removePrefix("@"))
        }
        return typeResolver.resolve(trimmed, elementType)
    }

    /** 符号 → 虚拟声明；补全候选复用本方法，保证补全预览与悬停文档同源。 */
    internal fun describe(symbol: SpelSymbol): SpelDeclaration {
        // 跳转优先目标：`@Param` 名称的注解位置、`$result$`/`$body$` 推导类型的书写位置；
        // 两者都没有时回退到声明与内置 owner 类
        val target = symbol.jumpTarget ?: symbol.declaration ?: builtinTarget(symbol)
        return SpelDeclaration(
            project = project,
            name = symbol.name,
            kindLabel = kindLabel(symbol),
            typeLabel = if (symbol.kind == SpelSymbolKind.FUNCTION) {
                symbol.signature ?: symbol.type
            } else {
                symbol.type
            },
            description = abbreviate(symbol.description),
            target = target,
            sourceRange = symbol.sourceRange,
            documentation = SpelDocumentation.of(symbol, kindLabel(symbol), target as? PsiMethod),
            anchor = anchor
        )
    }

    /**
     * 内置符号在符号表里只有 `owner` 类名与 `methodName`，没有 PSI 声明。
     *
     * 按 `owner` 在项目依赖中定位真实类，函数再按签名选对应重载，
     * 使 Ctrl+Click 能跳到框架源码或依赖 jar 的反编译视图。
     * 类不在项目依赖中时返回 null，降级为虚拟声明。
     */
    private fun builtinTarget(symbol: SpelSymbol): PsiElement? {
        val owner = symbol.owner ?: return null
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(owner, GlobalSearchScope.allScope(project))
            ?: return null
        if (symbol.kind != SpelSymbolKind.FUNCTION) {
            return psiClass
        }
        val methods = psiClass.findMethodsByName(symbol.methodName ?: symbol.name, true)
        return selectOverload(methods, symbol.signature) ?: psiClass
    }

    /** 同名重载按 `signature` 中的参数类型简单名匹配；匹配不到时取第一个。 */
    private fun selectOverload(methods: Array<PsiMethod>, signature: String?): PsiMethod? {
        if (methods.size <= 1) {
            return methods.firstOrNull()
        }
        val types = signatureParameterTypes(signature) ?: return methods.firstOrNull()
        return methods.firstOrNull { method ->
            val parameters = method.parameterList.parameters
            parameters.size == types.size && parameters.zip(types).all { (parameter, type) ->
                val presentable = parameter.type.presentableText.removeSuffix("...")
                presentable == type.removeSuffix("...")
            }
        } ?: methods.firstOrNull()
    }

    /** `str_ne(Object, Object)` → `["Object", "Object"]`；签名缺失时返回 null。 */
    private fun signatureParameterTypes(signature: String?): List<String>? {
        val body = signature?.substringAfter('(', "")?.substringBeforeLast(')')
            ?: return null
        if (body.isBlank()) {
            return emptyList()
        }
        return body.split(',').map { it.trim() }
    }

    private fun typeDeclaration(psiClass: PsiClass): SpelDeclaration = SpelDeclaration(
        project = project,
        name = psiClass.name ?: psiClass.qualifiedName.orEmpty(),
        kindLabel = "类型",
        typeLabel = psiClass.qualifiedName,
        description = docCommentOf(psiClass),
        target = psiClass,
        anchor = anchor,
        documentation = SpelDocumentation(
            name = psiClass.name ?: psiClass.qualifiedName.orEmpty(),
            category = "类型",
            type = psiClass.qualifiedName,
            description = docCommentOf(psiClass)
        )
    )

    private fun fieldDeclaration(field: PsiField): SpelDeclaration = SpelDeclaration(
        project = project,
        name = field.name,
        kindLabel = "字段",
        typeLabel = field.type.presentableText,
        target = field,
        anchor = anchor,
        documentation = SpelDocumentation(
            name = field.name,
            category = "字段",
            type = field.type.presentableText,
            owner = field.containingClass?.qualifiedName,
            description = docCommentOf(field)
        )
    )

    internal fun methodDeclaration(method: PsiMethod): SpelDeclaration = SpelDeclaration(
        project = project,
        name = method.name,
        kindLabel = "方法",
        typeLabel = signatureOf(method),
        target = method,
        anchor = anchor,
        documentation = SpelDocumentation(
            name = method.name,
            category = "方法",
            signature = signatureOf(method),
            type = method.returnType?.presentableText,
            owner = method.containingClass?.qualifiedName,
            description = docCommentOf(method)
        ).withMethodDetails(method)
    )

    private fun kindLabel(symbol: SpelSymbol): String = when (symbol.kind) {
        SpelSymbolKind.VARIABLE -> when (symbol.origin) {
            SpelSymbolOrigin.PARAMETER -> "方法参数"
            SpelSymbolOrigin.METHOD_IMPORT, SpelSymbolOrigin.CLASS_IMPORT -> "@SpELImport 变量"
            SpelSymbolOrigin.BUILTIN -> "内置变量"
            SpelSymbolOrigin.GLOBAL -> "全局变量"
            SpelSymbolOrigin.CLASS_STATIC, SpelSymbolOrigin.UNKNOWN -> "变量"
        }

        SpelSymbolKind.ROOT_VARIABLE -> when (symbol.origin) {
            SpelSymbolOrigin.BUILTIN -> "内置变量"
            SpelSymbolOrigin.GLOBAL -> "全局变量"
            SpelSymbolOrigin.METHOD_IMPORT, SpelSymbolOrigin.CLASS_IMPORT -> "Root 变量 @SpELImport"
            else -> "Root 变量"
        }

        SpelSymbolKind.FUNCTION -> "函数"
        SpelSymbolKind.NAMESPACE -> "命名空间"
        SpelSymbolKind.TYPE -> "类型"
        SpelSymbolKind.BEAN -> "Bean"
    }

    private fun signatureOf(method: PsiMethod): String {
        val parameters = method.parameterList.parameters.joinToString(", ") { it.type.presentableText }
        return "${method.name}($parameters)"
    }

    /** JavaBean 属性名对应的访问器名：`fileId` → `getFileId` / `isFileId`。 */
    private fun getterName(name: String, prefix: String): String =
        prefix + name.replaceFirstChar { it.uppercaseChar() }

    /** 符号描述的单行摘要，超出长度上限时截断并补省略号。 */
    private fun abbreviate(text: String?): String? = summaryOf(text, DESCRIPTION_LIMIT)

    private companion object {
        /** 跳转声明中的描述长度上限，超出部分用省略号。 */
        const val DESCRIPTION_LIMIT = 80
    }
}
