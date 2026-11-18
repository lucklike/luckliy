package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StringPattern
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.documentation.docCommentOf
import com.luckyframework.idea.spel.documentation.summaryOf
import com.luckyframework.idea.spel.environment.SpringPlaceholderParser
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.reference.SpelNavigation
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelContextBuilder
import com.luckyframework.idea.spel.symbol.SpelImportParser
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin
import javax.swing.Icon

/**
 * SpEL 补全：变量、Root 变量、函数、命名空间、类型与成员。
 *
 * 候选文本与来源来自 [SpelContextBuilder] 的统一符号视图；成员与类型补全
 * 按需解析 PSI 类型，无法推断时降级为通用成员并标注「类型未知」（design D7）。
 */
class SpelCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val analyzer = SpelCompletionAnalyzer()
    private val contextBuilder = SpelContextBuilder()
    private val importParser = SpelImportParser()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val literal = PsiTreeUtil.getParentOfType(
            parameters.position,
            PsiLiteralExpression::class.java,
            false
        ) ?: return
        if (!isApplicable(literal, parameters.originalFile)) {
            return
        }
        val offsetInLiteral = parameters.offset - literal.textRange.startOffset
        // `${...}` 内部由环境变量键补全接管，不能再混入 SpEL 变量/成员候选
        if (SpringPlaceholderParser.placeholderContaining(literal.text, offsetInLiteral) != null) {
            return
        }
        // 函数名属性整段不含 `#{}`：内容不是 SpEL 表达式，直接按函数名补全
        if (!literal.text.contains("#{")) {
            addFunctionNameCandidates(literal, offsetInLiteral, result)
            return
        }
        val site = analyzer.analyze(
            literal.text,
            offsetInLiteral
        ) ?: return
        // 重启条件必须与「候选集合何时变化」一致，否则要么列表滞后、要么候选缺失：
        // - 候选集合与输入无关：VARIABLE/MEMBER/ELEMENT 模式全量加入，字母、数字、
        //   `_`、`$` 的连续输入只影响平台过滤与排序（排序键实时读取编辑器，见
        //   SpelOrderWeigher），不需要重算，避免快速输入时列表停留在旧前缀。
        // - 候选集合依赖输入：ROOT 模式在输入为空时收敛 `$0`/`name::type` 等参数
        //   简写，出现第一个字符后才加入；`.`/`[`/`#`/`@`/`(`/`,`/运算符等非标识符
        //   字符会切换补全来源。这些情况必须重算。
        // - 类型与 Bean 候选的插入文本、截断数量本身取决于输入前缀，仍按任意前缀重启。
        when (site.mode) {
            SpelCompletionMode.TYPE, SpelCompletionMode.BEAN ->
                result.restartCompletionOnAnyPrefixChange()

            else -> result.restartCompletionOnPrefixChange(FILTER_ONLY_PREFIX)
        }
        val symbols = contextBuilder.build(literal)
        // 未标注 @SpelAttribute 的属性运行期不求值：包括类型与 Bean 在内一律不补全
        if (!symbols.known) {
            return
        }
        addCandidates(site, symbols, literal, importedPackages(literal), result, parameters.editor)
    }

    /**
     * 通用候选填充：Java 字符串字面量与 YAML 标量共用。
     *
     * [element] 用于取项目、类型解析，并作为虚拟声明的锚点；
     * [packages] 是当前上下文中导入的包；
     * [editor] 用于读取最新输入生成排序键（列表弹出后输入变化不会重新回调提供器）。
     */
    internal fun addCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        element: PsiElement,
        packages: List<String>,
        result: CompletionResultSet,
        editor: Editor? = null
    ) {
        // 排序完全由排序键控制：平台默认 sorter 会混入匹配度、历史统计等权重，
        // 无法保证「前缀命中在前、仅包含在后、同组字母序」的确定性顺序。
        val ordered = result.withRelevanceSorter(spelSorter(currentWordProvider(editor)))
        // 函数/方法候选携带虚拟声明：选中后右侧文档区按默认样式展示签名、参数与返回值
        val navigation = SpelNavigation(element.project, context, anchor = element)
        when (site.mode) {
            SpelCompletionMode.VARIABLE -> addInvocationCandidates(site, context, navigation, ordered)
            SpelCompletionMode.ROOT -> addRootCandidates(site, context, navigation, ordered)
            SpelCompletionMode.MEMBER ->
                addMemberCandidates(site, context, element.project, navigation, ordered)

            SpelCompletionMode.ELEMENT ->
                addElementCandidates(site, context, element.project, navigation, ordered)

            SpelCompletionMode.TYPE ->
                addTypeCandidates(site, element.project, navigation, packages, ordered)

            SpelCompletionMode.BEAN -> addBeanCandidates(site, element.project, navigation, ordered)
        }
    }

    /**
     * `#` 之后的普通变量、变量表函数与命名空间。
     *
     * Root 变量（方法参数、`@SpELImport(root = ...)`）运行期只能用裸名称引用，
     * 不出现在 `#` 候选里；未注册的内置函数只支持裸 `name(...)` 调用，同样不列出。
     */
    private fun addInvocationCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val target = spelTarget(result, "#" + site.word)
        // `#this` 是 SpEL 内置对象，不在符号表：选择/投影方括号内单独列出，
        // 图标与普通变量区分，提示它指向当前元素而不是可重命名变量
        if (site.elementScopes.isNotEmpty()) {
            target.addElement(thisElement())
        }
        val functions = ArrayList<SpelSymbol>()
        // 候选全量加入，由平台按当前前缀过滤；字母输入不重启补全，
        // 避免「输入太快时列表停留在旧前缀结果」的滞后
        for (symbol in context.available()) {
            when (symbol.kind) {
                SpelSymbolKind.VARIABLE -> target.addElement(variableElement(symbol, "#", navigation))
                // `#name(...)` 只适用于变量表里有平铺 Method 的函数：
                // 注册函数与 @SpELImport 导入函数可用；带命名空间的只能 `#ns.name(...)`
                SpelSymbolKind.FUNCTION ->
                    if (symbol.namespace == null && symbol.isCompletable()) {
                        functions += symbol
                    }

                SpelSymbolKind.NAMESPACE -> target.addElement(namespaceElement(symbol, navigation))
                else -> Unit
            }
        }
        addFunctionCandidates(
            target,
            functions.distinctBy { it.name to it.signature },
            "#",
            navigation
        )
    }

    /**
     * 函数名属性的补全（`@SpelAttribute(kind = FUNCTION)` 的属性值）。
     *
     * 运行期该值按平铺名从变量表取 `Method`（[SpelContext.functionNames]），
     * 因此只列出平铺注册的函数；插入的是裸函数名，不带 `#`，也不补 `()`。
     */
    private fun addFunctionNameCandidates(
        literal: PsiLiteralExpression,
        offsetInLiteral: Int,
        result: CompletionResultSet
    ) {
        // 候选集合与输入无关，字母输入不重启补全，避免列表停留在旧前缀结果
        result.restartCompletionOnPrefixChange(FILTER_ONLY_PREFIX)
        val symbols = contextBuilder.build(literal)
        // 未标注 @SpelAttribute 的属性运行期不求值，一律不补全
        if (!symbols.known) {
            return
        }
        val word = literal.text.substring(0, offsetInLiteral).takeLastWhile {
            Character.isJavaIdentifierPart(it)
        }
        val navigation = SpelNavigation(literal.project, symbols, anchor = literal)
        addFunctionCandidates(
            spelTarget(result, word),
            symbols.availableFunctionNames().distinctBy { it.name to it.signature },
            "",
            navigation,
            functionNameHandler
        )
    }

    /** 同名重载用签名区分 lookup，插入时再简化为函数名。 */
    private fun addFunctionCandidates(
        target: CompletionResultSet,
        functions: List<SpelSymbol>,
        prefix: String,
        navigation: SpelNavigation,
        insertHandler: InsertHandler<LookupElement> = functionCallHandler
    ) {
        val overloaded = functions.groupBy { it.name }.filterValues { it.size > 1 }.keys
        for (function in functions) {
            val lookup = if (function.name in overloaded) {
                prefix + function.name + signatureSuffix(function)
            } else {
                prefix + function.name
            }
            target.addElement(functionElement(function, lookup, navigation, insertHandler))
        }
    }

    private fun signatureSuffix(symbol: SpelSymbol): String {
        val signature = symbol.signature ?: return ""
        val index = signature.indexOf('(')
        return if (index >= 0) signature.substring(index) else ""
    }

    /**
     * 未注册的内置函数（如 `BankCardGenerator` 的方法）运行期不可用，
     * 需通过 `@SpELImport` 导入后才参与补全；导入来的函数不受影响。
     */
    private fun SpelSymbol.isCompletable(): Boolean =
        kind != SpelSymbolKind.FUNCTION || origin != SpelSymbolOrigin.BUILTIN || registered

    /**
     * Root 变量（裸名称）与函数（平铺调用）。
     *
     * 方法参数、`@SpELImport(root = ...)` 与内置 Root 变量都只能用裸名称引用，
     * 插入时不带 `#`；`@SpELImport(var = ...)` 的普通变量只能写在 `#` 之后，
     * 这里仍列出但插入时自动补 `#`，避免用户必须先知道正确写法。
     * 函数只支持平铺调用（如 `#{str_ne(a, b)}`），因此 Root 位置列出全部函数，
     * 包括内置函数；内置函数在 `#` 位置不出现。
     */
    private fun addRootCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val target = spelTarget(result, site.word)
        val seen = HashSet<String>()
        val functions = ArrayList<SpelSymbol>()
        for (symbol in context.available()) {
            when (symbol.kind) {
                SpelSymbolKind.ROOT_VARIABLE -> {
                    // `name::type`/`$n::type` 是参数专用的组合写法
                    if (symbol.name.contains("::") && symbol.origin != SpelSymbolOrigin.PARAMETER) {
                        continue
                    }
                    // 空输入时收敛 `$name`/`::type` 等变体，避免 `#{` 初始列表被淹没；
                    // 输入后一律按「包含输入字符」参与过滤，保证这些写法能被搜到
                    if (site.word.isEmpty() && symbol.origin == SpelSymbolOrigin.PARAMETER &&
                        symbol.isShorthandName()
                    ) {
                        continue
                    }
                    if (!seen.add(symbol.name)) {
                        continue
                    }
                    target.addElement(rootVariableElement(symbol, navigation))
                }

                SpelSymbolKind.VARIABLE -> {
                    if (symbol.name.contains('$')) {
                        continue
                    }
                    // 普通变量只能写在 `#` 之后：候选按变量名过滤，插入时补 `#`
                    if (!seen.add("#" + symbol.name)) {
                        continue
                    }
                    target.addElement(hashVariableElement(symbol, navigation))
                }

                SpelSymbolKind.FUNCTION -> if (symbol.isCompletable()) functions += symbol
                else -> Unit
            }
        }
        // 平铺函数不带前缀，插入时补 `()`；同名重载用签名区分
        addFunctionCandidates(
            target,
            functions.distinctBy { it.name to it.signature },
            "",
            navigation
        )
    }

    /** Root 变量候选：插入裸名称；方法参数使用参数图标与图层。 */
    private fun rootVariableElement(symbol: SpelSymbol, navigation: SpelNavigation): LookupElement {
        if (symbol.origin == SpelSymbolOrigin.PARAMETER) {
            return parameterElement(symbol, navigation)
        }
        val declaration = navigation.describe(symbol)
        val element = LookupElementBuilder.create(declaration, symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(AllIcons.Nodes.Variable)
            .withPsiElement(declaration)
        return ordered(element, visibleName(symbol), layerKey(symbol))
    }

    /** 参数候选：名称 + 类型；Root 位置插入时不带 `#`。 */
    private fun parameterElement(symbol: SpelSymbol, navigation: SpelNavigation): LookupElement {
        val declaration = navigation.describe(symbol)
        val element = LookupElementBuilder.create(declaration, symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(AllIcons.Nodes.Parameter)
            .withPsiElement(declaration)
        return ordered(element, visibleName(symbol), layerKey(symbol))
    }

    /** 参数的 `$name`/`$n`/`$$n`/`name::type`/`$n::type`/`$pc$n`/`$pc$name` 变体，区别于裸参数名。 */
    private fun SpelSymbol.isShorthandName(): Boolean =
        name.contains('$') || name.contains("::")

    /** 普通变量在 Root 位置补全时插入 `#name`。 */
    private fun hashVariableElement(symbol: SpelSymbol, navigation: SpelNavigation): LookupElement {
        val declaration = navigation.describe(symbol)
        val element = LookupElementBuilder.create(declaration, symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(AllIcons.Nodes.Variable)
            .withPsiElement(declaration)
            .withInsertHandler { context, _ ->
                context.document.insertString(context.startOffset, "#")
                context.editor.caretModel.moveToOffset(context.tailOffset + 1)
            }
        return ordered(element, visibleName(symbol), layerKey(symbol))
    }

    /** `.` 之后的成员：命名空间函数或按接收者类型列出的字段与方法。 */
    private fun addMemberCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        project: Project,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val qualifier = site.qualifier ?: return
        val namespaceName = qualifier.removePrefix("#")
        val namespace = context.ofKind(SpelSymbolKind.NAMESPACE).firstOrNull { it.name == namespaceName }
        if (namespace != null && qualifier.startsWith("#")) {
            val functions = context.ofKind(SpelSymbolKind.FUNCTION)
                .filter { it.namespace == namespace.name && it.isCompletable() }
            if (functions.isNotEmpty()) {
                val target = spelTarget(result, site.word)
                addFunctionCandidates(target, functions, "", navigation)
                return
            }
        }

        val resolver = SpelTypeResolver(project, context)
        val elementType = SpelElementContext.elementType(site.elementScopes, resolver)
        // 数组的 `length` 是 SpEL 特判属性，与成员检查的放行规则保持一致
        if (resolver.resolvePsiType(qualifier, elementType) is PsiArrayType) {
            addArrayMembers(site, result)
            return
        }
        val owner = resolver.resolve(qualifier, elementType)
        if (owner == null) {
            addUnknownMembers(site, result)
            return
        }
        addClassMembers(site, owner, navigation, result)
    }

    /**
     * 选择/投影方括号内的 `#this`：指向当前元素对象。
     *
     * SpEL 内置对象不在符号表；用独立图标避免与普通变量/参数混淆。
     */
    private fun thisElement(): LookupElement {
        val element = LookupElementBuilder.create("#this")
            .withTailText("  过滤/投影中的当前元素", true)
            .withTypeText("当前元素", true)
            .withIcon(AllIcons.Nodes.Parameter)
        return ordered(element, "this")
    }

    /** 数组的 `length`：SpEL 对数组属性有特判，补全与检查需一致放行。 */
    private fun addArrayMembers(site: SpelCompletionSite, result: CompletionResultSet) {
        val element = LookupElementBuilder.create("length")
            .withTypeText("int", true)
            .withIcon(AllIcons.Nodes.Field)
        spelTarget(result, site.word).addElement(ordered(element, "length"))
    }

    /**
     * 选择/投影方括号内的裸标识符与裸调用：按当前元素类型补全成员，
     * 并同时列出全局函数。
     *
     * 运行期 `users.?[str_eq(#this.address, '3')]` 的裸调用先查函数表，
     * 未命中才按元素方法解析（`length()`）；补全必须同时给出两类候选，
     * 否则内置函数无法被发现。
     */
    private fun addElementCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        project: Project,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val owner = SpelElementContext.elementClass(site.elementScopes, SpelTypeResolver(project, context))
        if (owner == null) {
            addUnknownMembers(site, result)
        } else {
            addClassMembers(site, owner, navigation, result)
        }
        addElementFunctionCandidates(site, context, navigation, result)
    }

    /**
     * 元素上下文中的全局函数候选。
     *
     * 只列可直接平铺调用的函数（与 [addRootCandidates] 一致）：命名空间函数运行期
     * 同样可以从函数表平铺解析，`#ns.fn()` 只是额外写法，因此不按命名空间过滤。
     */
    private fun addElementFunctionCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val functions = context.ofKind(SpelSymbolKind.FUNCTION)
            .filter { it.isCompletable() }
            .distinctBy { it.name to it.signature }
        if (functions.isEmpty()) {
            return
        }
        addFunctionCandidates(spelTarget(result, site.word), functions, "", navigation)
    }

    private fun addClassMembers(
        site: SpelCompletionSite,
        owner: PsiClass,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val target = spelTarget(result, site.word)
        val seenFields = HashSet<String>()
        for (field in owner.allFields) {
            val name = field.name
            // SpEL 通过反射读取字段，private 字段（Lombok 实体）也应参与补全
            if (!seenFields.add(name)) {
                continue
            }
            val declaration = navigation.fieldDeclaration(field)
            val element = LookupElementBuilder.create(declaration, name)
                .withTailText(descriptionTail(docCommentOf(field)), true)
                .withTypeText(field.type.presentableText, true)
                .withIcon(AllIcons.Nodes.Field)
                .withPsiElement(declaration)
            target.addElement(ordered(element, name))
        }

        val seenMethods = HashSet<String>()
        for (method in owner.allMethods) {
            if (method.isConstructor || !method.hasModifierProperty(PsiModifier.PUBLIC)) {
                continue
            }
            // getXxx/isXxx 按 JavaBean 属性名补全（`meta.fileId` 对应 `getFileId()`）
            val propertyName = propertyNameOf(method)
            if (propertyName != null && seenFields.add(propertyName)) {
                val declaration = navigation.methodDeclaration(method)
                val element = LookupElementBuilder.create(declaration, propertyName)
                    .withTailText(descriptionTail(docCommentOf(method)), true)
                    .withTypeText(method.returnType?.presentableText ?: "?", true)
                    .withIcon(AllIcons.Nodes.Property)
                    .withPsiElement(declaration)
                target.addElement(ordered(element, propertyName))
            }
            val signature = signatureOf(method)
            if (!seenMethods.add(signature)) {
                continue
            }
            val declaration = navigation.methodDeclaration(method)
            val element = LookupElementBuilder.create(declaration, method.name)
                .withPresentableText(methodPresentation(method))
                .withTypeText(method.returnType?.presentableText ?: "void", true)
                .withIcon(AllIcons.Nodes.Method)
                .withInsertHandler(functionCallHandler)
                .withPsiElement(declaration)
            target.addElement(ordered(element, method.name))
        }
    }

    /** JavaBean 访问器名还原为属性名：`getFileId` → `fileId`，`isOk` → `ok`。 */
    private fun propertyNameOf(method: PsiMethod): String? {
        if (method.parameterList.parametersCount != 0) {
            return null
        }
        val name = method.name
        val raw = when {
            name.startsWith("get") && name.length > 3 -> name.substring(3)
            name.startsWith("is") && name.length > 2 -> name.substring(2)
            else -> return null
        }
        if (raw == "Class") {
            return null
        }
        return raw.replaceFirstChar { it.lowercaseChar() }
    }

    /** 类型未知时的通用成员，保证不出现空补全列表。 */
    private fun addUnknownMembers(site: SpelCompletionSite, result: CompletionResultSet) {
        val target = spelTarget(result, site.word)
        for (name in UNKNOWN_MEMBERS) {
            val element = LookupElementBuilder.create(name)
                .withPresentableText("$name()")
                .withTypeText("类型未知", true)
                .withIcon(AllIcons.Nodes.Method)
                .withInsertHandler(functionCallHandler)
            target.addElement(ordered(element, name))
        }
    }

    /** `T(`、`new`、`instanceof` 之后的类型：`@SPELImport(pack)` 导入的包优先，其次项目类路径。 */
    private fun addTypeCandidates(
        site: SpelCompletionSite,
        project: Project,
        navigation: SpelNavigation,
        packages: List<String>,
        result: CompletionResultSet
    ) {
        // instanceof 的右操作数必须是 T(...)：替换掉已输入的限定路径，插入完整类型引用
        val target = if (site.wrapInTypeReference) {
            result.withPrefixMatcher(SpelPrefixMatcher(site.word, site.typePrefix + site.word))
        } else {
            spelTarget(result, site.word)
        }
        val facade = JavaPsiFacade.getInstance(project)
        val seen = HashSet<String>()

        fun addClass(psiClass: PsiClass) {
            if (seen.size >= MAX_TYPE_CANDIDATES) {
                return
            }
            val simpleName = psiClass.name ?: return
            val qualifiedName = psiClass.qualifiedName ?: return
            val insertText = if (site.wrapInTypeReference) {
                if (site.typePrefix.isNotEmpty() && !qualifiedName.startsWith(site.typePrefix)) {
                    return
                }
                "T(" + typeReferenceName(qualifiedName) + ")"
            } else {
                typeInsertName(qualifiedName, site.typePrefix) ?: return
            }
            // 已输入包前缀时按「待插入的限定名」过滤（`T(java.ut` 命中 `java.util.*`），
            // 否则仍按简单类名过滤；instanceof 候选展示不含 T(...) 的类名
            val visibleName = if (site.wrapInTypeReference || site.typePrefix.isEmpty()) {
                simpleName
            } else {
                insertText
            }
            if (!matchesWord(visibleName, site.word)) {
                return
            }
            if (!seen.add(qualifiedName)) {
                return
            }
            target.addElement(
                typeElement(navigation, psiClass, simpleName, qualifiedName, insertText, visibleName)
            )
        }

        for (packageName in packages) {
            val psiPackage = facade.findPackage(packageName) ?: continue
            for (psiClass in psiPackage.classes) {
                addClass(psiClass)
            }
        }

        val scope = GlobalSearchScope.allScope(project)
        val cache = PsiShortNamesCache.getInstance(project)
        // 短名缓存的遍历顺序是哈希序，而共用词干的类型（如各种 RequestXxx）
        // 数量很容易超过上限：若按原序处理，精确同名的 Request 会被截断。
        // 这里按「完全相同 > 前缀命中 > 仅包含输入」分桶，保证高相关类型先入列表。
        val exact = ArrayList<String>()
        val prefixed = ArrayList<String>()
        val contained = ArrayList<String>()
        for (shortName in cache.allClassNames) {
            if (!matchesWord(shortName, site.word)) {
                continue
            }
            when {
                site.word.isEmpty() || shortName.equals(site.word, true) -> exact += shortName
                shortName.startsWith(site.word, true) -> prefixed += shortName
                else -> contained += shortName
            }
        }

        outer@ for (bucket in listOf(exact, prefixed, contained)) {
            for (shortName in bucket) {
                if (seen.size >= MAX_TYPE_CANDIDATES) {
                    break@outer
                }
                val classes = runCatching {
                    cache.getClassesByName(shortName, scope)
                }.getOrElse { PsiClass.EMPTY_ARRAY }
                // 同名类全部列出（右侧限定名区分），框架类优先：
                // 短名缓存的首个同名类可能是无关依赖（如 okhttp 的 Request）。
                classes.sortedByDescending {
                    it.qualifiedName?.startsWith(LUCKY_FRAMEWORK_PREFIX) == true
                }.forEach { addClass(it) }
            }
        }
    }

    /**
     * `@` 之后的 Spring Bean。
     *
     * 与跳转（[SpelNavigation.beans]）保持同一启发式：Bean 名由声明类名首字母小写
     * 反推。这里只列项目源码中的具体类，避免依赖库的大量同名类淹没用户 Bean；
     * 接口、抽象类、枚举与注解不可实例化，不参与候选。
     */
    private fun addBeanCandidates(
        site: SpelCompletionSite,
        project: Project,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val target = spelTarget(result, site.word)
        val cache = PsiShortNamesCache.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val seen = HashSet<String>()
        // 与类型候选一致：按「完全相同 > 前缀命中 > 仅包含」分桶，
        // 避免短名缓存的哈希序让精确候选被截断。
        val exact = ArrayList<String>()
        val prefixed = ArrayList<String>()
        val contained = ArrayList<String>()
        for (shortName in cache.allClassNames) {
            val beanName = shortName.replaceFirstChar { it.lowercaseChar() }
            if (!matchesWord(beanName, site.word)) {
                continue
            }
            when {
                site.word.isEmpty() || beanName.equals(site.word, true) -> exact += shortName
                beanName.startsWith(site.word, true) -> prefixed += shortName
                else -> contained += shortName
            }
        }

        outer@ for (bucket in listOf(exact, prefixed, contained)) {
            for (shortName in bucket.sorted()) {
                if (seen.size >= MAX_BEAN_CANDIDATES) {
                    break@outer
                }
                val classes = runCatching {
                    cache.getClassesByName(shortName, scope)
                }.getOrElse { PsiClass.EMPTY_ARRAY }
                for (psiClass in classes) {
                    if (!isBeanCandidate(psiClass)) {
                        continue
                    }
                    val beanName = psiClass.name?.replaceFirstChar { it.lowercaseChar() } ?: continue
                    if (!seen.add(beanName)) {
                        continue
                    }
                    val qualifiedName = psiClass.qualifiedName ?: continue
                    target.addElement(beanElement(navigation, psiClass, beanName, qualifiedName))
                }
            }
        }
    }

    /** 可实例化的具体类才能作为 Bean 声明类。 */
    private fun isBeanCandidate(psiClass: PsiClass): Boolean =
        !psiClass.isInterface &&
            !psiClass.isAnnotationType &&
            !psiClass.isEnum &&
            !psiClass.hasModifierProperty(PsiModifier.ABSTRACT)

    private fun beanElement(
        navigation: SpelNavigation,
        psiClass: PsiClass,
        beanName: String,
        qualifiedName: String
    ): LookupElement {
        // 候选携带虚拟声明：选中后右侧文档由声明上的真实 [PsiClass] 生成，
        // 与编辑器内的类型文档一致；补全阶段不解析类注释以保持响应速度。
        val declaration = navigation.beanDeclaration(psiClass, beanName, withDocumentation = false)
        val element = LookupElementBuilder.create(declaration, beanName)
            .withPresentableText("@$beanName")
            .withTypeText(qualifiedName, true)
            .withIcon(AllIcons.Nodes.Class)
            .withPsiElement(declaration)
        return ordered(element, beanName)
    }

    private fun importedPackages(literal: PsiLiteralExpression): List<String> {
        val owners = LinkedHashSet<PsiModifierListOwner>()
        PsiTreeUtil.getParentOfType(literal, PsiMethod::class.java, true)?.let { owners += it }
        PsiTreeUtil.getParentOfType(literal, PsiClass::class.java, true)?.let { owners += it }
        return owners.flatMap { owner ->
            importParser.findImportAnnotations(owner).flatMap { importParser.parsePackages(it) }
        }.distinct()
    }

    /**
     * 类型候选：主文本为简单类名，右侧显示限定名；插入时按 SpEL 规则补全包名。
     *
     * SpEL 的 `T()` 与 `new` 只有 `java.lang` 包内的类可以省略包名，其余必须写全限定名，
     * 因此不能像 Java 代码那样依赖 import 使用短名。lookup 文本用插入名而不是
     * 简单名：平台按 `lookupString` 对候选去重，同名类（不同包）会互相覆盖。
     *
     * 已输入包前缀（`T(java.util.Arr`）时插入名相对前缀计算，替换当前单词后
     * 不会重复已输入的包名；`instanceof` 位置插入完整的 `T(类型)`，替换范围
     * 由 [SpelCompletionSite.wrapInTypeReference] 分支扩展为整段类型路径。
     */
    private fun typeElement(
        navigation: SpelNavigation,
        psiClass: PsiClass,
        simpleName: String,
        qualifiedName: String,
        insertText: String,
        sortName: String
    ): LookupElement {
        // 候选携带虚拟声明：选中后右侧文档由声明上的真实 [PsiClass] 生成，
        // 与编辑器内的类型文档一致；补全阶段不解析类注释以保持响应速度。
        val declaration = navigation.typeDeclaration(psiClass, withDocumentation = false)
        val element = LookupElementBuilder.create(declaration, insertText)
            .withPresentableText(simpleName)
            .withTypeText(qualifiedName, true)
            .withIcon(AllIcons.Nodes.Class)
            .withPsiElement(declaration)
        return ordered(element, sortName)
    }

    /**
     * 类型位置实际要插入的文本。
     *
     * 未输入前缀时按 SpEL 规则（`java.lang` 用短名，其余全限定名）；
     * 已输入前缀时只插入剩余部分，前缀不匹配的类直接排除。
     */
    private fun typeInsertName(qualifiedName: String, typePrefix: String): String? {
        if (typePrefix.isEmpty()) {
            return typeReferenceName(qualifiedName)
        }
        if (!qualifiedName.startsWith(typePrefix)) {
            return null
        }
        return qualifiedName.removePrefix(typePrefix).takeIf { it.isNotEmpty() }
    }

    /** `T()` 中的插入名称：`java.lang` 的顶层类用短名，其余用全限定名。 */
    private fun typeReferenceName(qualifiedName: String): String {
        val packageName = qualifiedName.substringBeforeLast('.', "")
        return if (packageName == "java.lang") qualifiedName.substringAfterLast('.') else qualifiedName
    }

    private fun variableElement(
        symbol: SpelSymbol,
        prefix: String,
        navigation: SpelNavigation
    ): LookupElement {
        val declaration = navigation.describe(symbol)
        val element = LookupElementBuilder.create(declaration, prefix + symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(variableIcon(symbol))
            .withPsiElement(declaration)
        return ordered(element, visibleName(symbol), layerKey(symbol))
    }

    /** 变量类候选的图标：方法参数用参数图标，其余变量用变量图标。 */
    private fun variableIcon(symbol: SpelSymbol): Icon =
        if (symbol.origin == SpelSymbolOrigin.PARAMETER) AllIcons.Nodes.Parameter
        else AllIcons.Nodes.Variable

    private fun functionElement(
        symbol: SpelSymbol,
        lookup: String,
        navigation: SpelNavigation,
        insertHandler: InsertHandler<LookupElement> = functionCallHandler
    ): LookupElement {
        // 主文本展示签名（含参数类型），返回类型靠右，与 IDEA 默认方法提示框一致
        val declaration = navigation.describe(symbol)
        val element = LookupElementBuilder.create(declaration, lookup)
            .withPresentableText(functionPresentation(symbol))
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(AllIcons.Nodes.Function)
            .withInsertHandler(insertHandler)
            .withPsiElement(declaration)
        return ordered(element, visibleName(symbol), layerKey(symbol))
    }

    private fun namespaceElement(symbol: SpelSymbol, navigation: SpelNavigation): LookupElement {
        val declaration = navigation.describe(symbol)
        val element = LookupElementBuilder.create(declaration, "#" + symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "未知", true)
            .withIcon(AllIcons.Nodes.Class)
            .withPsiElement(declaration)
        return ordered(element, visibleName(symbol), layerKey(symbol))
    }

    /** 变量类候选的说明：单行摘要，超长截断为省略号；无说明时返回空串。 */
    private fun descriptionTail(text: String?): String {
        val summary = summaryOf(text, MAX_DESCRIPTION_LENGTH) ?: return ""
        return "  $summary"
    }

    /** 函数主文本：签名（含参数类型），无签名时退化为 `name()`。 */
    private fun functionPresentation(symbol: SpelSymbol): String =
        symbol.signature ?: "${symbol.name}()"

    /** 方法主文本：`name(参数类型 参数名, ...)`，与 Java 补全默认样式一致。 */
    private fun methodPresentation(method: PsiMethod): String {
        val parameters = method.parameterList.parameters.joinToString(", ") {
            "${it.type.presentableText} ${it.name}"
        }
        return "${method.name}($parameters)"
    }

    private fun signatureOf(method: PsiMethod): String {
        val parameters = method.parameterList.parameters.joinToString(", ") { it.type.presentableText }
        return "${method.name}($parameters)"
    }

    /**
     * 输入字符后只保留名字包含该片段的候选（不区分大小写）。
     *
     * 过滤依据是列表里展示的名字：`@FunctionAlias` 函数展示为可输入别名
     * （如 `__std_exception_handler__`），因此输入 `_` 时它归入前缀命中组，
     * 而不会以原方法名 `exceptionHandler` 的形式出现。
     */
    private fun matchesWord(name: String, word: String): Boolean =
        word.isEmpty() || name.contains(word, true)

    /** 列表中展示的名字：函数取签名里的方法名，其余取符号名。 */
    private fun visibleName(symbol: SpelSymbol): String {
        if (symbol.kind != SpelSymbolKind.FUNCTION) {
            return symbol.name
        }
        return symbol.signature?.substringBefore('(')?.takeIf { it.isNotEmpty() } ?: symbol.name
    }

    /**
     * 结果集按「包含输入」过滤。
     *
     * 默认前缀匹配器只接受前缀命中，会把「仅包含输入」的候选直接丢弃；
     * 这里换成 [SpelPrefixMatcher]，两组候选都进入列表，先后顺序由排序键决定。
     */
    private fun spelTarget(result: CompletionResultSet, prefix: String): CompletionResultSet =
        result.withPrefixMatcher(SpelPrefixMatcher(SpelPrefixMatcher.wordOf(prefix), prefix))

    /**
     * 候选排序键。
     *
     * 输入为空时按「作用域分层 + 名称」（就近作用域优先）；
     * 输入非空时按用户规则：以输入开头的（组 0）在前，仅包含输入的（组 1）在后，
     * 同组内按不区分大小写的字母序排列。
     */
    private fun ordered(
        element: LookupElement,
        name: String,
        layer: String = LAYER_BUILTIN
    ): LookupElement = SpelOrderedElement(element, name, layer)

    /**
     * 候选分层键：方法参数 > `@SpELImport` 导入符号 > 内置符号 > 框架内部变量。
     *
     * 内部变量指 `_index_`、`_$value$_`、`__$Val$__` 以及 `__$default$__`
     * 这类 `ValueContext`/`InternalVarName` 声明，日常书写几乎不会手写，沉底。
     */
    private fun layerKey(symbol: SpelSymbol): String = when {
        symbol.origin == SpelSymbolOrigin.PARAMETER -> LAYER_PARAMETER
        symbol.origin in IMPORT_ORIGINS -> LAYER_IMPORT
        symbol.origin == SpelSymbolOrigin.BUILTIN && symbol.isInternalName() ->
            LAYER_INTERNAL + internalRank(symbol.name)
        else -> LAYER_BUILTIN
    }

    /** 函数不参与内部变量判定，避免误伤以下划线开头的自定义函数。 */
    private fun SpelSymbol.isInternalName(): Boolean =
        kind != SpelSymbolKind.FUNCTION && name.startsWith("_")

    /**
     * 内部变量的语义顺序：索引 → 名称 → 类型 → 值 → 值来源 → 变量容器。
     *
     * 只在输入为空（`#{` 刚弹出）时生效；未列出的内部变量排在其后。
     */
    private fun internalRank(name: String): String {
        val index = INTERNAL_VARIABLE_ORDER.indexOf(name)
        return if (index < 0) "99" else index.toString().padStart(2, '0')
    }

    /** 补全的前提：位于 `#{` 字面量内或函数名属性值中，且模块已激活。 */
    internal fun isApplicable(literal: PsiLiteralExpression): Boolean =
        isSpelSite(literal) &&
            LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)

    /**
     * 补全副本的激活判断：`position` 属于编辑器副本，模块按 [originalFile] 查找。
     */
    internal fun isApplicable(literal: PsiLiteralExpression, originalFile: PsiFile): Boolean {
        if (!isSpelSite(literal)) {
            return false
        }
        if (DumbService.isDumb(literal.project)) {
            return false
        }
        return LuckyHttpClientPresence.getInstance(literal.project)
            .isEnabledForCopy(literal, originalFile)
    }

    /** 补全位置：`#{` 片段内，或 `kind = FUNCTION` 属性值（整段就是函数名）。 */
    private fun isSpelSite(literal: PsiLiteralExpression): Boolean =
        literal.text.contains("#{") || SpelAnnotationResolver().isFunctionNameAttribute(literal)

    private companion object {
        /**
         * 「只做过滤」的前缀：至少两个字符的纯标识符片段。
         *
         * 平台用 `restartCompletionOnPrefixChange` 的 pattern 匹配
         * 「注册时的光标到当前光标」的文本，命中才重启。长度 1 也要重启：
         * 空输入与有输入时的候选集合不同（参数简写在输入非空后才加入）。
         * 字符串重载是精确相等匹配而非正则，必须用 [StringPattern] 模式。
         */
        val FILTER_ONLY_PREFIX: StringPattern =
            PlatformPatterns.string().matches("(?![A-Za-z0-9_\\$]{2,}$)[\\s\\S]*")

        val UNKNOWN_MEMBERS = listOf("toString", "getClass", "equals", "hashCode")
        const val MAX_TYPE_CANDIDATES = 200
        const val MAX_BEAN_CANDIDATES = 200

        private val IMPORT_ORIGINS = setOf(
            SpelSymbolOrigin.METHOD_IMPORT,
            SpelSymbolOrigin.CLASS_IMPORT,
            SpelSymbolOrigin.CLASS_STATIC
        )

        /** 分层键前缀（字符串排序，小者在前）：参数 > 导入 > 内置 > 内部变量。 */
        const val LAYER_PARAMETER = "00"
        const val LAYER_IMPORT = "01"
        const val LAYER_BUILTIN = "02"
        const val LAYER_INTERNAL = "03"

        /** `ValueContext` 内部变量的语义顺序。 */
        val INTERNAL_VARIABLE_ORDER = listOf(
            "_index_",
            "_name_",
            "_type_",
            "_value_",
            "_\$value\$_",
            "__\$Val\$__"
        )

        /** 列表内说明的长度上限，超出部分以省略号结尾。 */
        const val MAX_DESCRIPTION_LENGTH = 80

        val functionCallHandler = InsertHandler<LookupElement> { context, _ ->
            val document = context.document
            val start = context.startOffset
            var end = context.tailOffset
            val inserted = document.text.substring(start, end)
            val name = inserted.substringBefore('(')
            if (name != inserted) {
                document.replaceString(start, end, name)
                end = start + name.length
            }
            if (end <= document.textLength - 1 && document.charsSequence[end] == '(') {
                context.editor.caretModel.moveToOffset(end + 1)
            } else {
                document.insertString(end, "()")
                context.editor.caretModel.moveToOffset(end + 1)
            }
        }

        /**
         * 函数名属性的插入：只保留函数名。
         *
         * 重载候选用 `name(参数)` 区分 lookups，插入后整段替换为裸函数名；
         * 该属性值是函数名而不是调用，不能补 `()`。
         */
        val functionNameHandler = InsertHandler<LookupElement> { context, _ ->
            val document = context.document
            val start = context.startOffset
            val end = context.tailOffset
            val inserted = document.text.substring(start, end)
            val name = inserted.substringBefore('(')
            if (name != inserted) {
                document.replaceString(start, end, name)
            }
        }
    }
}
