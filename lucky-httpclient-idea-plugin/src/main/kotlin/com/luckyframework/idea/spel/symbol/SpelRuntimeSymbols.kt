package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil
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
 *   框架 jar 中的组合注解（如 `@Get`）另按公开注解清单直接收集（见
 *   [scanComponentRoots]），不依赖注解索引能否向上追溯到元注解；
 * - 函数：`HttpClientProxyObjectFactory` 初始化的 `addSpringElFunctionClass`
 *   调用所指的函数类，按 public static 方法展开。
 *
 * 框架内置符号与用户扩展符号走同一条路径：用户在任意依赖模块中标注这些
 * 元注解后立即生效，无需生成或更新任何 JSON。
 *
 * 扫描按项目缓存，`PsiModificationTracker` 或项目根/依赖模型变化后自动失效；
 * dumb 模式下索引未就绪，直接返回空结果，避免阻塞索引构建。
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

    /**
     * 最近一次成功扫描到的组件清单；未预热或 dumb 模式下返回 null。
     *
     * 补全列表等 EDT 路径只能读取这里：它们不能像 [components] 那样主动触发
     * 注解扫描（`AnnotatedElementsSearch` 是索引查询，EDT 上会触发 Slow operations
     * 断言）。
     *
     * 这里刻意不校验 PSI 修改计数：触发补全的按键本身就是一次 PSI 修改，
     * 按版本号校验会让清单在每次输入后立即失效，补全图标随之在默认图标与
     * 品牌图标之间反复横跳。清单与 [Snapshot] 生命周期解耦，只保留最近一次
     * 成功扫描的结果；它可能落后于当前 PSI 版本，但组件身份极少变化，
     * 下一次 [components] 重算会覆盖它。
     */
    fun cachedComponents(project: Project): List<LuckyComponentMetadata>? {
        if (DumbService.isDumb(project)) {
            return null
        }
        return latestComponents[project]
    }

    /**
     * 丢弃项目的扫描快照，下一次访问重新扫描。
     *
     * 项目模型（依赖导入、模块根变化）或索引状态变化时调用：此时
     * [PsiModificationTracker] 的计数不一定变化，仅靠它无法让"依赖未就绪时
     * 缓存的空结果"失效。
     */
    fun invalidate(project: Project) {
        CACHE.remove(project)
    }

    private fun snapshot(project: Project): Snapshot? {
        if (DumbService.isDumb(project)) {
            return null
        }
        val stamp = PsiModificationTracker.getInstance(project).modificationCount
        val rootsStamp = ProjectRootModificationTracker.getInstance(project).modificationCount
        val cached = CACHE[project]
        if (cached != null && cached.stamp == stamp && cached.rootsStamp == rootsStamp) {
            return cached
        }
        val snapshot = Snapshot(stamp, rootsStamp, project)
        CACHE[project] = snapshot
        return snapshot
    }

    /**
     * 一个 PSI 版本下的扫描结果。三个列表分别惰性计算，补全只访问变量时
     * 不会为函数扫描付出代价；同一次 PSI 版本内重复访问直接复用。
     */
    private class Snapshot(val stamp: Long, val rootsStamp: Long, private val project: Project) {

        val variables: List<SpelSymbol> by lazy { scanning { scanVariables(project) } }

        val functions: List<SpelSymbol> by lazy { scanning { scanFunctions(project) } }

        private val componentsLazy = lazy {
            val result = runCatching { scanComponents(project) }
            // 只保留非空结果：依赖未就绪时的空扫描不能抹掉上一次的有效清单，
            // 否则补全图标会随着每次重扫在两种状态之间抖动
            result.getOrNull()?.takeIf { it.isNotEmpty() }?.let { latestComponents[project] = it }
            result.getOrElse { emptyList() }
        }

        val components: List<LuckyComponentMetadata> get() = componentsLazy.value
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
        val annotation = findClass(project, LUCKY_COMPONENT)
        val result = LinkedHashMap<String, LuckyComponentMetadata>()
        if (annotation == null) {
            // 新元注解暂不可解析时仍按根注解清单收集 legacy 组件，
            // 避免首次打开项目时只看到 @StdHttpClient 分组。
            scanComponentRoots(project, result)
            return result.values.toList()
        }

        val carriers = ArrayDeque<PsiClass>()
        val visited = HashSet<String>()

        // 直接标注的类/接口；注解声明作为元注解载体继续向下传播
        for (psiClass in searchClasses(annotation, project)) {
            val declared = annotationOf(psiClass, LUCKY_COMPONENT)
            putComponent(
                result, psiClass, typeOf(declared), "class", true,
                groupOf(declared), priorityOf(declared)
            )
            if (psiClass.isAnnotationType && visited.add(psiClass.qualifiedName ?: psiClass.name ?: "")) {
                carriers += psiClass
            }
        }
        // 方法上直接标注：方法所在类是组件
        for (method in searchElements(annotation, project, PsiMethod::class.java)) {
            if (method is PsiMethod) {
                val declared = annotationOf(method, LUCKY_COMPONENT)
                method.containingClass?.let {
                    putComponent(
                        result, it, typeOf(declared), "method", true,
                        groupOf(declared), priorityOf(declared)
                    )
                }
            }
        }
        // 元注解传播：使用带 @LuckyComponent 的注解的类/接口/方法继承组件身份与分组
        while (carriers.isNotEmpty()) {
            val carrier = carriers.removeFirst()
            val declared = annotationOf(carrier, LUCKY_COMPONENT)
            val type = typeOf(declared)
            val group = groupOf(declared)
            val priority = priorityOf(declared)
            for (user in searchClasses(carrier, project)) {
                putComponent(result, user, type, "annotation", false, group, priority)
                if (user.isAnnotationType && visited.add(user.qualifiedName ?: user.name ?: "")) {
                    carriers += user
                }
            }
            for (method in searchElements(carrier, project, PsiMethod::class.java)) {
                if (method is PsiMethod) {
                    method.containingClass?.let { putComponent(result, it, type, "annotation", false, group, priority) }
                }
            }
        }
        // jar 中的组合注解链不保证能被索引向上追溯，按固定注解清单兜底
        scanComponentRoots(project, result)
        return result.values.toList()
    }

    /**
     * 按框架公开的组件注解直接收集使用处的类型。
     *
     * [`@Get`] 一类组合注解在声明处标注 `@HttpRequest`，`@HttpRequest` 再标注
     * `@LuckyComponent`；依赖 jar 中的注解声明不能保证被注解索引检索到，
     * 只靠元注解 BFS 会漏掉 legacy `@HttpClient` 接口。这里改为直接搜索注解
     * 使用点（项目源码），两种情况都能覆盖。
     */
    private fun scanComponentRoots(
        project: Project,
        result: MutableMap<String, LuckyComponentMetadata>
    ) {
        for (root in COMPONENT_ROOTS) {
            val annotation = findClass(project, root.annotation) ?: continue
            // 根注解声明自身的 group/priority；只有注解链语义与根类型一致时才采用，
            // 避免 legacy AUTO 注解被元注解链上的 API 声明覆盖
            val attributes = componentAttributesOf(annotation)?.takeIf { it.type == root.type }
            for (psiClass in searchClasses(annotation, project)) {
                putComponent(
                    result, psiClass, root.type, "root", false,
                    attributes?.group, attributes?.priority
                )
            }
            for (method in searchElements(annotation, project, PsiMethod::class.java)) {
                if (method is PsiMethod) {
                    method.containingClass?.let {
                        putComponent(
                            result, it, root.type, "root", false,
                            attributes?.group, attributes?.priority
                        )
                    }
                }
            }
        }
    }

    /** 一个组件根注解：[type] 是该注解使用处的组件类型。 */
    private class ComponentRoot(val annotation: String, val type: String)

    /** 注解声明上的组件属性，沿元注解链取最近一层。 */
    private class ComponentAttributes(val type: String, val group: String?, val priority: Double?)

    /**
     * 注解声明自身的 `@LuckyComponent` 属性。
     *
     * 按元注解链逐层向外查找，最近一层优先：[Get] 取 [HttpRequest] 上的声明，
     * [HttpRequest] 取自身声明；旧版本 jar 中不存在的属性返回 null。
     */
    private fun componentAttributesOf(annotation: PsiClass): ComponentAttributes? {
        var level: List<PsiClass> = listOf(annotation)
        val visited = HashSet<String>()
        for (depth in 0..MAX_META_DEPTH) {
            val found = level.asSequence()
                .filter { cls -> (cls.qualifiedName ?: cls.name)?.let(visited::add) == true }
                .mapNotNull { cls ->
                    annotationOf(cls, LUCKY_COMPONENT)?.let {
                        ComponentAttributes(typeOf(it), groupOf(it), priorityOf(it))
                    }
                }
                .firstOrNull()
            if (found != null) {
                return found
            }
            level = level.flatMap { cls ->
                cls.modifierList?.annotations?.mapNotNull { it.resolveAnnotationType() }.orEmpty()
            }
            if (level.isEmpty()) {
                return null
            }
        }
        return null
    }

    /** 直接标注优先：同一类先出现的来源不被后续继承覆盖。 */
    private fun putComponent(
        target: MutableMap<String, LuckyComponentMetadata>,
        psiClass: PsiClass,
        type: String,
        source: String,
        declared: Boolean,
        group: String? = null,
        priority: Double? = null
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
            source = source,
            group = group,
            priority = priority
        )
    }

    private fun typeOf(annotation: PsiAnnotation?): String {
        val name = annotation?.let { enumName(it, "type") } ?: return AUTO
        return name.takeIf { it.isNotBlank() } ?: AUTO
    }

    /** `@LuckyComponent#group`；旧版本注解没有该属性时返回 null。 */
    private fun groupOf(annotation: PsiAnnotation?): String? =
        annotation?.let { runCatching { stringValue(it, "group") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }

    /** `@LuckyComponent#priority`；旧版本注解没有该属性时返回 null。 */
    private fun priorityOf(annotation: PsiAnnotation?): Double? {
        val annotation = annotation ?: return null
        val value = runCatching { annotation.findAttributeValue("priority") }.getOrNull() ?: return null
        return constantDouble(value)
    }

    /** 数值注解属性：常量求值优先，`Double.MAX_VALUE` 一类引用再按文本兜底。 */
    private fun constantDouble(value: PsiAnnotationMemberValue): Double? {
        val evaluated = runCatching {
            JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value)
        }.getOrNull()
        if (evaluated is Number) {
            return evaluated.toDouble()
        }
        val text = value.text.trim()
            .removeSuffix("d").removeSuffix("D").removeSuffix("f").removeSuffix("F")
        text.toDoubleOrNull()?.let { return it }
        return when (text.substringAfterLast('.')) {
            "MAX_VALUE" -> Double.MAX_VALUE
            "MIN_VALUE" -> Double.MIN_VALUE
            else -> null
        }
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
        for (entry in registered) {
            val owner = findClass(project, entry.owner) ?: continue
            for (symbol in expander.symbolsOf(owner, SpelSymbolOrigin.BUILTIN)) {
                if (symbol.kind == SpelSymbolKind.NAMESPACE && entry.namespace != null) {
                    continue
                }
                val effective = if (symbol.kind == SpelSymbolKind.FUNCTION && entry.namespace != null) {
                    symbol.copy(namespace = entry.namespace)
                } else {
                    symbol
                }
                // 工厂构造即注册，从 CLASS 阶段起可用（Context 初始化早于任何请求）
                result.putIfAbsent(
                    functionKey(effective),
                    effective.copy(registered = true, lifecycle = SpelLifecycle.CLASS)
                )
            }
            if (entry.namespace != null) {
                result.putIfAbsent(
                    functionKey(entry.namespace, owner.qualifiedName),
                    SpelSymbol(
                        name = entry.namespace,
                        kind = SpelSymbolKind.NAMESPACE,
                        type = owner.qualifiedName,
                        origin = SpelSymbolOrigin.BUILTIN,
                        declaration = owner,
                        owner = owner.qualifiedName,
                        lifecycle = SpelLifecycle.CLASS
                    )
                )
            }
        }
        return result.values.toList()
    }

    private fun functionKey(symbol: SpelSymbol): String =
        (symbol.namespace ?: "") + "::" + functionKey(symbol.name, symbol.owner) +
            "#" + (symbol.signature ?: "")

    private fun functionKey(name: String, owner: String?): String =
        "$name@${owner ?: ""}"

    /**
     * 注册的函数类清单。
     *
     * 三路来源取并集，按「类 + 命名空间」去重：
     *
     * - 框架默认类：工厂构造函数必然执行 `importCommonFunction()`，始终可用；
     * - factory 源码中的 `addSpringElFunctionClass(X.class)` 调用（含显式命名空间）；
     * - 项目源码中任意位置的注册调用（见 [projectRegistrations]）。
     *
     * 依赖 jar 中方法体不可见，前两项会退化，项目调用与默认清单保证结果不丢。
     */
    private fun registeredFunctionClasses(project: Project): List<RegisteredFunctionClass> {
        val result = LinkedHashMap<Pair<String, String?>, RegisteredFunctionClass>()

        fun add(className: String?, namespace: String?) {
            if (className.isNullOrBlank()) {
                return
            }
            val effective = namespace?.takeIf { it.isNotBlank() }
            result.putIfAbsent(className to effective, RegisteredFunctionClass(className, effective))
        }

        DEFAULT_FUNCTION_CLASSES.forEach { add(it, null) }

        val factory = findClass(project, HTTP_CLIENT_PROXY_OBJECT_FACTORY)
        if (factory != null) {
            for (call in PsiTreeUtil.collectElementsOfType(factory, PsiMethodCallExpression::class.java)) {
                val method = call.resolveMethod() ?: continue
                if (method.name != ADD_SPRING_EL_FUNCTION_CLASS) {
                    continue
                }
                val registration = registrationOf(call) ?: continue
                add(registration.owner, registration.namespace)
            }
        }

        // 用户在自己的配置类/工厂子类中注册的函数类（jar 场景下的主要来源）
        projectRegistrations(project).forEach { add(it.owner, it.namespace) }

        return result.values.toList()
    }

    /**
     * 项目源码中所有 `addSpringElFunctionClass` 调用。
     *
     * 只认可解析到 [HTTP_CLIENT_PROXY_OBJECT_FACTORY]（含子类）的调用：字符串、
     * 注释与方法声明处无法构成调用表达式，同名方法也会因归属校验被过滤。
     * 按词搜索只取项目源码（不含依赖 jar），成本由命中该词的文件数决定。
     */
    private fun projectRegistrations(project: Project): List<RegisteredFunctionClass> {
        val result = ArrayList<RegisteredFunctionClass>()
        val processor = object : TextOccurenceProcessor {
            override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
                registrationCallOf(element)?.let { result += it }
                return true
            }
        }
        // 搜索失败（索引异常、项目关闭中）时不影响默认清单与 factory 源码结果
        runCatching {
            PsiSearchHelper.getInstance(project).processElementsWithWord(
                processor,
                GlobalSearchScope.projectScope(project),
                ADD_SPRING_EL_FUNCTION_CLASS,
                UsageSearchContext.IN_CODE,
                true
            )
        }
        return result
    }

    /** 词命中元素向上还原为注册调用；声明、字符串与注释返回 null。 */
    private fun registrationCallOf(element: PsiElement): RegisteredFunctionClass? {
        val call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression::class.java, false)
            ?: return null
        if (call.methodExpression.referenceName != ADD_SPRING_EL_FUNCTION_CLASS) {
            return null
        }
        // Class<?> 与 ClassStaticElement 重载同时可见时 resolveMethod 可能因歧义返回 null，
        // 用 multiResolve 兜底；只要求任一候选来自工厂类（含子类）。
        val owner = call.resolveMethod()?.containingClass
        if (owner == null || !isFactory(owner)) {
            val resolved = call.methodExpression.multiResolve(false)
                .asSequence()
                .mapNotNull { it.element as? PsiMethod }
                .firstOrNull {
                    it.name == ADD_SPRING_EL_FUNCTION_CLASS &&
                        it.containingClass?.let(::isFactory) == true
                }
            if (resolved == null) {
                return null
            }
        }
        return registrationOf(call)
    }

    /** 注册方法必须来自工厂类或其子类，避免同名方法误命中。 */
    private fun isFactory(psiClass: PsiClass): Boolean =
        psiClass.qualifiedName == HTTP_CLIENT_PROXY_OBJECT_FACTORY ||
            InheritanceUtil.isInheritor(psiClass, HTTP_CLIENT_PROXY_OBJECT_FACTORY)

    /** 一个注册的函数类；[namespace] 为显式命名空间，null 表示由类上 `@Namespace` 决定。 */
    private data class RegisteredFunctionClass(val owner: String, val namespace: String?)

    /** 读取 `addSpringElFunctionClass(X.class)` / `addSpringElFunctionClass("ns", X.class)` 的实参。 */
    private fun registrationOf(call: PsiMethodCallExpression): RegisteredFunctionClass? {
        val arguments = call.argumentList.expressions
        val classArgument = arguments.lastOrNull() as? PsiClassObjectAccessExpression ?: return null
        val owner = classType(classArgument)?.resolve()?.qualifiedName ?: return null
        val namespace = if (arguments.size >= 2) namespaceValue(arguments[0]) else null
        return RegisteredFunctionClass(owner, namespace)
    }

    /**
     * 命名空间实参：字符串字面量或编译期常量（如 `private static final String NS = "util"`）。
     * 求值失败或为空白时视为未指定，由函数类上的 `@Namespace` 决定。
     */
    private fun namespaceValue(expression: PsiExpression): String? {
        val helper = JavaPsiFacade.getInstance(expression.project).constantEvaluationHelper
        (helper.computeConstantExpression(expression) as? String)?.let {
            return it.takeIf(String::isNotBlank)
        }
        // 常量求值依赖字段类型解析；索引/类路径不完整时回退到字段初始化器
        val field = (expression as? PsiReferenceExpression)?.resolve() as? PsiField ?: return null
        val initializer = field.initializer ?: return null
        val value = (helper.computeConstantExpression(initializer) as? String)
            ?: (initializer as? PsiLiteralExpression)?.value as? String
        return value?.takeIf { it.isNotBlank() }
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

    /** 最近一次成功扫描到的组件清单，供 EDT 路径读取，见 [cachedComponents]。 */
    private val latestComponents = ConcurrentHashMap<Project, List<LuckyComponentMetadata>>()

    private const val SPEL_SYMBOL = "com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol"
    private const val LUCKY_COMPONENT = "com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent"
    private const val HTTP_CLIENT_PROXY_OBJECT_FACTORY =
        "com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory"
    private const val ROOT_VAR_NAME_HOLDER =
        "com.luckyframework.httpclient.proxy.spel.InternalRootVarName"
    private const val ADD_SPRING_EL_FUNCTION_CLASS = "addSpringElFunctionClass"
    private const val AUTO = "AUTO"
    private const val API = "API"
    private const val DATABASE = "DATABASE"
    private const val FUNCTION = "FUNCTION"
    private const val CONFIG = "CONFIG"
    private const val DESCRIPTION_LIMIT = 80

    /** 根注解向元注解链查找组件属性的最大层数。 */
    private const val MAX_META_DEPTH = 4

    /**
     * 框架公开的组件注解：按使用处直接收集，作为元注解 BFS 的兜底。
     *
     * 前 11 项是 3.x 的请求/接口注解（方法级使用，所在接口即 API 组件）；
     * `DBClient` 是数据库客户端；中间四项兼容 starter 2.x 的 legacy 注解；
     * `SpelConfig` 是配置模型。
     */
    private val COMPONENT_ROOTS = listOf(
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.HttpRequest", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Get", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Post", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Put", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Delete", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Head", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Options", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Trace", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Connect", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.annotations.Patch", API),
        ComponentRoot("com.luckyframework.httpclient.proxy.configapi.ApiConfig", API),
        ComponentRoot("io.github.lucklike.httpclient.dbclient.annotation.DBClient", DATABASE),
        ComponentRoot("io.github.lucklike.httpclient.discovery.HttpClient", API),
        ComponentRoot("io.github.lucklike.httpclient.annotation.HttpClientComponent", API),
        ComponentRoot("io.github.lucklike.httpclient.annotation.LuckyComponent", AUTO),
        ComponentRoot("io.github.lucklike.httpclient.annotation.SpELFunction", FUNCTION),
        ComponentRoot("com.luckyframework.httpclient.proxy.spel.meta.SpelConfig", CONFIG)
    )

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
