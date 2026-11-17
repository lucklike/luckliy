package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.documentation.docCommentOf
import com.luckyframework.idea.spel.documentation.summaryOf
import com.luckyframework.idea.spel.metadata.LuckyComponentMetadata
import com.luckyframework.idea.spel.metadata.SpelLifecycle
import java.util.concurrent.ConcurrentHashMap

/**
 * 在 IDE 运行期直接从框架类发现 SpEL 符号。
 *
 * 数据来源全部是注解与 Java 声明，不读取任何离线清单：
 *
 * - 变量：`@SpelSymbol` 标注的字段常量，常量值即运行期变量名；
 * - 组件：`@LuckyComponent` 标注的类/方法，以及使用带该元注解的注解的类；
 * - 函数：`HttpClientProxyObjectFactory` 初始化的 `addSpringElFunctionClass`
 *   调用所指的函数类，按 public static 方法展开。
 *
 * 框架内置符号与用户扩展符号走同一条路径：用户在任意依赖模块中标注这些
 * 元注解后立即生效，无需生成或更新任何 JSON。
 *
 * 扫描按项目缓存，`PsiModificationTracker` 变化后自动失效；dumb 模式下
 * 索引未就绪，直接返回空结果，避免阻塞索引构建。
 */
object SpelRuntimeSymbols : ProjectSpelSymbolSource {

    override fun symbols(project: Project): List<SpelSymbol> =
        variables(project) + functions(project)

    /** `@SpelSymbol` 标注的字段常量声明的变量。 */
    fun variables(project: Project): List<SpelSymbol> = snapshot(project)?.variables ?: emptyList()

    /** 框架默认注册（`addSpringElFunctionClass`）的函数。 */
    fun functions(project: Project): List<SpelSymbol> = snapshot(project)?.functions ?: emptyList()

    /** `@LuckyComponent` 声明的组件清单。 */
    fun components(project: Project): List<LuckyComponentMetadata> =
        snapshot(project)?.components ?: emptyList()

    private fun snapshot(project: Project): Snapshot? {
        if (DumbService.isDumb(project)) {
            return null
        }
        val stamp = PsiModificationTracker.getInstance(project).modificationCount
        val cached = CACHE[project]
        if (cached != null && cached.stamp == stamp) {
            return cached
        }
        val snapshot = Snapshot(stamp, project)
        CACHE[project] = snapshot
        return snapshot
    }

    /**
     * 一个 PSI 版本下的扫描结果。三个列表分别惰性计算，补全只访问变量时
     * 不会为函数扫描付出代价；同一次 PSI 版本内重复访问直接复用。
     */
    private class Snapshot(val stamp: Long, private val project: Project) {

        val variables: List<SpelSymbol> by lazy { scanning { scanVariables(project) } }

        val functions: List<SpelSymbol> by lazy { scanning { scanFunctions(project) } }

        val components: List<LuckyComponentMetadata> by lazy { scanning { scanComponents(project) } }
    }

    /** 索引未就绪或 PSI 结构异常时按空结果处理，不能影响编辑体验。 */
    private inline fun <T> scanning(action: () -> List<T>): List<T> =
        runCatching(action).getOrElse { emptyList() }

    // ---------------------------------------------------------------------------------------------
    // 变量：@SpelSymbol 字段常量
    // ---------------------------------------------------------------------------------------------

    private fun scanVariables(project: Project): List<SpelSymbol> {
        val annotation = findClass(project, SPEL_SYMBOL) ?: return emptyList()
        return searchElements(annotation, project, PsiField::class.java).mapNotNull { field ->
            if (field !is PsiField) {
                return@mapNotNull null
            }
            symbolOf(field)
        }
    }

    private fun symbolOf(field: PsiField): SpelSymbol? {
        val annotation = annotationOf(field, SPEL_SYMBOL) ?: return null
        val explicitName = stringValue(annotation, "name")
        val name = explicitName?.takeIf { it.isNotBlank() }
            ?: constantString(field)?.takeIf { it.isNotBlank() }
            ?: return null
        val kind = variableKind(enumName(annotation, "kind"), name, field) ?: return null
        // 类型取值：显式表达式 → 显式 class 字面量 → 字段声明类型。
        // 显式声明的类型原样使用（包括 `Object`）；`type` 必须用
        // `findDeclaredAttributeValue`（只取显式声明），`findAttributeValue`
        // 会把注解默认值 `Object.class` 当成用户声明。未声明时只有声明类型
        // 本身没有信息量（`Object`/`Class<?>`）才按“未知”处理，避免把
        // `Class` 的 `getName` 等方法当成变量成员展示。
        val type = stringValue(annotation, "typeExpression")
            ?.takeIf { it.isNotBlank() }
            ?.let(::simplifyTypeText)
            ?: classLiteralName(annotation.findDeclaredAttributeValue("type"))
                ?.let(::simplifyTypeText)
            ?: field.type.presentableText.takeUnless(::isUninformativeType)
        return SpelSymbol(
            name = name,
            kind = kind,
            type = type,
            origin = SpelSymbolOrigin.BUILTIN,
            declaration = field,
            lifecycle = lifecycleOf(enumName(annotation, "from")),
            description = descriptionOf(field, name)
        )
    }

    /**
     * 变量描述：Javadoc 首段。
     *
     * 框架习惯在描述末尾用括号重复变量名（`响应状态码（{@code $status$}）`），
     * 变量名已经展示在补全项上，括号说明不再进入描述。
     */
    private fun descriptionOf(field: PsiField, name: String): String? {
        val text = docCommentOf(field) ?: return null
        val withoutNameClause = text.replace(nameClause(name), "").trim()
        return summaryOf(withoutNameClause.ifEmpty { text }, DESCRIPTION_LIMIT)
    }

    private fun nameClause(name: String): Regex =
        Regex("""\s*[（(]\s*${Regex.escape(name)}\s*[）)]\s*$""")

    /**
     * `@SpelSymbol#kind` 与插件符号类别的映射。
     *
     * `AUTO` 按框架规则推导：`$...$` 形式是根变量；
     * `InternalRootVarName` 中的常量整体作为根变量注册
     * （`RootVarCtrlMap` 按声明类收集保护名，不逐一声明 `kind`）；
     * 其余是普通变量。`FUNCTION` 只描述函数名常量，不作为变量参与补全。
     */
    private fun variableKind(kind: String?, name: String, field: PsiField): SpelSymbolKind? = when (kind) {
        "ROOT", "INTERNAL" -> SpelSymbolKind.ROOT_VARIABLE
        "VAR" -> SpelSymbolKind.VARIABLE
        "FUNCTION", "BEAN", "ENV", "PARAM" -> null
        else -> if (isInternalName(name) || declaredInRootNameHolder(field)) {
            SpelSymbolKind.ROOT_VARIABLE
        } else {
            SpelSymbolKind.VARIABLE
        }
    }

    /** `$...$` 命名的框架保留变量以裸名称注册为 Root 变量。 */
    private fun isInternalName(name: String): Boolean =
        name.length > 2 && name.startsWith('$') && name.endsWith('$')

    /** 无信息类型：只知道声明类型是对象/类容器时，无法据此推断成员。 */
    private fun isUninformativeType(text: String): Boolean =
        text == "Object" || text == "Class" || text.startsWith("Class<")

    /** `InternalRootVarName` 的常量全部作为 Root 变量保护名，与 `@SpelSymbol#kind` 无关。 */
    private fun declaredInRootNameHolder(field: PsiField): Boolean =
        field.containingClass?.qualifiedName == ROOT_VAR_NAME_HOLDER

    /**
     * 字段的字符串常量值。
     *
     * 常量字段优先走 `computeConstantValue()`；目标 JDK 尚未解析时该 API 返回
     * null，此时直接读取源码中的初始化表达式。
     */
    private fun constantString(field: PsiField): String? {
        (field.computeConstantValue() as? String)?.let { return it }
        return (field.initializer as? PsiLiteralExpression)?.value as? String
    }

    private fun lifecycleOf(name: String?): SpelLifecycle {
        if (name.isNullOrBlank()) {
            return SpelLifecycle.METHOD
        }
        return SpelLifecycle.values().firstOrNull { it.name == name } ?: SpelLifecycle.METHOD
    }

    // ---------------------------------------------------------------------------------------------
    // 组件：@LuckyComponent
    // ---------------------------------------------------------------------------------------------

    private fun scanComponents(project: Project): List<LuckyComponentMetadata> {
        val annotation = findClass(project, LUCKY_COMPONENT) ?: return emptyList()
        val result = LinkedHashMap<String, LuckyComponentMetadata>()

        val carriers = ArrayDeque<PsiClass>()
        val visited = HashSet<String>()

        // 直接标注的类/接口；注解声明作为元注解载体继续向下传播
        for (psiClass in searchClasses(annotation, project)) {
            putComponent(result, psiClass, typeOf(annotationOf(psiClass, LUCKY_COMPONENT)), "class", true)
            if (psiClass.isAnnotationType && visited.add(psiClass.qualifiedName ?: psiClass.name ?: "")) {
                carriers += psiClass
            }
        }
        // 方法上直接标注：方法所在类是组件
        for (method in searchElements(annotation, project, PsiMethod::class.java)) {
            if (method is PsiMethod) {
                method.containingClass?.let {
                    putComponent(result, it, typeOf(annotationOf(method, LUCKY_COMPONENT)), "method", true)
                }
            }
        }
        // 元注解传播：使用带 @LuckyComponent 的注解的类/接口/方法继承组件身份
        while (carriers.isNotEmpty()) {
            val carrier = carriers.removeFirst()
            val type = typeOf(annotationOf(carrier, LUCKY_COMPONENT))
            for (user in searchClasses(carrier, project)) {
                putComponent(result, user, type, "annotation", false)
                if (user.isAnnotationType && visited.add(user.qualifiedName ?: user.name ?: "")) {
                    carriers += user
                }
            }
            for (method in searchElements(carrier, project, PsiMethod::class.java)) {
                if (method is PsiMethod) {
                    method.containingClass?.let { putComponent(result, it, type, "annotation", false) }
                }
            }
        }
        return result.values.toList()
    }

    /** 直接标注优先：同一类先出现的来源不被后续继承覆盖。 */
    private fun putComponent(
        target: MutableMap<String, LuckyComponentMetadata>,
        psiClass: PsiClass,
        type: String,
        source: String,
        declared: Boolean
    ) {
        val className = psiClass.qualifiedName ?: return
        val existing = target[className]
        if (existing != null && (existing.declared || !declared)) {
            return
        }
        target[className] = LuckyComponentMetadata(
            className = className,
            title = summaryOf(docCommentOf(psiClass), DESCRIPTION_LIMIT) ?: psiClass.name ?: className,
            type = type,
            declared = declared,
            source = source
        )
    }

    private fun typeOf(annotation: PsiAnnotation?): String {
        val name = annotation?.let { enumName(it, "type") } ?: return AUTO
        return name.takeIf { it.isNotBlank() } ?: AUTO
    }

    // ---------------------------------------------------------------------------------------------
    // 函数：HttpClientProxyObjectFactory 默认注册的函数类
    // ---------------------------------------------------------------------------------------------

    private fun scanFunctions(project: Project): List<SpelSymbol> {
        val registered = registeredFunctionClasses(project)
        if (registered.isEmpty()) {
            return emptyList()
        }
        val result = LinkedHashMap<String, SpelSymbol>()
        val expander = ImportedClassSymbols()
        for ((className, namespace) in registered) {
            val owner = findClass(project, className) ?: continue
            for (symbol in expander.symbolsOf(owner, SpelSymbolOrigin.BUILTIN)) {
                if (symbol.kind == SpelSymbolKind.NAMESPACE && namespace != null) {
                    continue
                }
                val effective = if (symbol.kind == SpelSymbolKind.FUNCTION && namespace != null) {
                    symbol.copy(namespace = namespace)
                } else {
                    symbol
                }
                result.putIfAbsent(functionKey(effective), effective.copy(registered = true))
            }
            if (namespace != null) {
                result.putIfAbsent(
                    functionKey(namespace, owner.qualifiedName),
                    SpelSymbol(
                        name = namespace,
                        kind = SpelSymbolKind.NAMESPACE,
                        type = owner.qualifiedName,
                        origin = SpelSymbolOrigin.BUILTIN,
                        declaration = owner,
                        owner = owner.qualifiedName
                    )
                )
            }
        }
        return result.values.toList()
    }

    private fun functionKey(symbol: SpelSymbol): String =
        functionKey(symbol.name, symbol.owner) + "#" + (symbol.signature ?: "")

    private fun functionKey(name: String, owner: String?): String =
        "$name@${owner ?: ""}"

    /**
     * `HttpClientProxyObjectFactory` 初始化时注册的函数类。
     *
     * 源码模块可以从 `importCommonFunction()` 的方法体读到精确的
     * `addSpringElFunctionClass(X.class)` 调用（含显式命名空间）；依赖 jar
     * 中方法体不可见，回退到框架固定的默认注册入口清单。
     */
    private fun registeredFunctionClasses(project: Project): Map<String, String?> {
        val factory = findClass(project, HTTP_CLIENT_PROXY_OBJECT_FACTORY)
        if (factory != null) {
            val fromSource = LinkedHashMap<String, String?>()
            for (call in PsiTreeUtil.collectElementsOfType(factory, PsiMethodCallExpression::class.java)) {
                val method = call.resolveMethod() ?: continue
                if (method.name != ADD_SPRING_EL_FUNCTION_CLASS) {
                    continue
                }
                val arguments = call.argumentList.expressions
                val classArgument = arguments.lastOrNull() as? PsiClassObjectAccessExpression ?: continue
                val owner = classType(classArgument)?.resolve()?.qualifiedName ?: continue
                val namespace = if (arguments.size >= 2) {
                    stringValue(arguments[0])
                } else {
                    null
                }
                fromSource[owner] = namespace
            }
            if (fromSource.isNotEmpty()) {
                return fromSource
            }
        }
        return DEFAULT_FUNCTION_CLASSES.associateWith { null }
    }

    private fun classType(value: PsiClassObjectAccessExpression): PsiClassType? =
        value.operand.type as? PsiClassType

    // ---------------------------------------------------------------------------------------------
    // PSI 工具
    // ---------------------------------------------------------------------------------------------

    private fun findClass(project: Project, fqName: String): PsiClass? = runCatching {
        JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project))
    }.getOrNull()

    private fun searchClasses(annotation: PsiClass, project: Project): List<PsiClass> = runCatching {
        AnnotatedElementsSearch.searchPsiClasses(annotation, GlobalSearchScope.allScope(project)).findAll()
            .toList()
    }.getOrElse { emptyList() }

    private fun searchElements(
        annotation: PsiClass,
        project: Project,
        elementType: Class<out PsiModifierListOwner>
    ): List<PsiModifierListOwner> = runCatching {
        AnnotatedElementsSearch.searchElements(annotation, GlobalSearchScope.allScope(project), elementType)
            .findAll()
            .toList()
    }.getOrElse { emptyList() }

    /** 注解按全限定名解析；类尚未解析时按简单名退化匹配。 */
    private fun annotationOf(owner: PsiModifierListOwner, fqName: String): PsiAnnotation? {
        owner.getAnnotation(fqName)?.let { return it }
        return owner.modifierList?.annotations?.firstOrNull {
            it.qualifiedName == fqName ||
                (it.qualifiedName == null && it.nameReferenceElement?.referenceName == fqName.substringAfterLast('.'))
        }
    }

    private fun enumName(annotation: PsiAnnotation, name: String): String? {
        val value = annotation.findAttributeValue(name) ?: return null
        val text = when (value) {
            is PsiReferenceExpression -> value.text
            is PsiLiteralExpression -> value.value as? String
            else -> null
        } ?: return null
        return text.substringAfterLast('.').trim('"').takeIf { it.isNotBlank() }
    }

    private fun stringValue(annotation: PsiAnnotation, name: String): String? =
        stringValue(annotation.findAttributeValue(name))

    private fun stringValue(value: PsiAnnotationMemberValue?): String? =
        (value as? PsiLiteralExpression)?.value as? String

    private fun classLiteralName(value: PsiAnnotationMemberValue?): String? {
        val type = (value as? PsiClassObjectAccessExpression)?.operand?.type ?: return null
        return qualifiedTypeName(type)
    }

    private fun qualifiedTypeName(type: PsiType): String = when (type) {
        is PsiArrayType -> qualifiedTypeName(type.componentType) + "[]"
        is PsiClassType -> type.resolve()?.qualifiedName ?: type.canonicalText
        else -> type.canonicalText
    }

    /** 类型文本去掉包名：`java.util.Map<java.lang.String, java.lang.Object>` → `Map<String, Object>`。 */
    private fun simplifyTypeText(text: String): String =
        QUALIFIED_TYPE.replace(text) { it.value.substringAfterLast('.') }

    private val CACHE = ConcurrentHashMap<Project, Snapshot>()

    private const val SPEL_SYMBOL = "com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol"
    private const val LUCKY_COMPONENT = "com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent"
    private const val HTTP_CLIENT_PROXY_OBJECT_FACTORY =
        "com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory"
    private const val ROOT_VAR_NAME_HOLDER =
        "com.luckyframework.httpclient.proxy.spel.InternalRootVarName"
    private const val ADD_SPRING_EL_FUNCTION_CLASS = "addSpringElFunctionClass"
    private const val AUTO = "AUTO"
    private const val DESCRIPTION_LIMIT = 80

    /** 框架固定注册的函数类；只在依赖 jar 无法读取方法体时使用。 */
    private val DEFAULT_FUNCTION_CLASSES = listOf(
        "com.luckyframework.httpclient.proxy.function.CommonFunctions",
        "com.luckyframework.httpclient.proxy.function.SerializationFunctions",
        "com.luckyframework.httpclient.proxy.function.ResourceFunctions",
        "com.luckyframework.httpclient.proxy.function.RandomFunctions",
        "com.luckyframework.httpclient.proxy.function.DigestFunctions",
        "com.luckyframework.httpclient.proxy.function.MacFunctions",
        "com.luckyframework.httpclient.proxy.function.CipherFunctions"
    )

    private val QUALIFIED_TYPE = Regex("""[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+""")
}
