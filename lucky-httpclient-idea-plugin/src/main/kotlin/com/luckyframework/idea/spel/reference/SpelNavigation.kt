package com.luckyframework.idea.spel.reference

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.documentation.SpelDocumentation
import com.luckyframework.idea.spel.documentation.docCommentOf
import com.luckyframework.idea.spel.documentation.summaryOf
import com.luckyframework.idea.spel.documentation.withMethodDetails
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

    fun types(qualifiedName: String): List<SpelDeclaration> {
        val psiClass = typeResolver.findClass(qualifiedName) ?: return emptyList()
        return listOf(typeDeclaration(psiClass))
    }

    fun beans(name: String): List<SpelDeclaration> {
        val simpleName = name.replaceFirstChar { it.uppercaseChar() }
        if (simpleName.isEmpty()) {
            return emptyList()
        }
        val classes: Array<PsiClass> = runCatching {
            PsiShortNamesCache.getInstance(project)
                .getClassesByName(simpleName, GlobalSearchScope.allScope(project))
        }.getOrElse { PsiClass.EMPTY_ARRAY }
        return classes.mapNotNull { psiClass ->
            val declared = psiClass.name ?: return@mapNotNull null
            if (declared.replaceFirstChar { it.lowercaseChar() } != name) {
                return@mapNotNull null
            }
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
     */
    fun members(qualifier: String, name: String): List<SpelDeclaration> {
        val namespace = context.ofKind(SpelSymbolKind.NAMESPACE)
            .firstOrNull { it.name == qualifier.removePrefix("#") }
        if (namespace != null) {
            return functions(name, namespace.name)
        }
        val owner = typeResolver.resolve(qualifier) ?: return emptyList()
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
        for (method in owner.allMethods) {
            if (!method.isConstructor &&
                method.name == name &&
                method.hasModifierProperty(PsiModifier.PUBLIC)
            ) {
                result += methodDeclaration(method)
            }
        }
        return result.distinctBy { it.target }
    }

    /** 符号 → 虚拟声明；补全候选复用本方法，保证补全预览与悬停文档同源。 */
    internal fun describe(symbol: SpelSymbol): SpelDeclaration {
        val target = symbol.declaration ?: builtinTarget(symbol)
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
