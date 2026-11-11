package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
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
        val site = analyzer.analyze(
            literal.text,
            parameters.offset - literal.textRange.startOffset
        ) ?: return
        val symbols = contextBuilder.build(literal, true)
        addCandidates(site, symbols, literal, importedPackages(literal), result)
    }

    /**
     * 通用候选填充：Java 字符串字面量与 YAML 标量共用。
     *
     * [element] 用于取项目、类型解析，并作为虚拟声明的锚点；
     * [packages] 是当前上下文中导入的包。
     */
    internal fun addCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        element: PsiElement,
        packages: List<String>,
        result: CompletionResultSet
    ) {
        // 函数/方法候选携带虚拟声明：选中后右侧文档区按默认样式展示签名、参数与返回值
        val navigation = SpelNavigation(element.project, context, anchor = element)
        when (site.mode) {
            SpelCompletionMode.VARIABLE -> addInvocationCandidates(site, context, navigation, result)
            SpelCompletionMode.ROOT -> addRootCandidates(site, context, navigation, result)
            SpelCompletionMode.MEMBER ->
                addMemberCandidates(site, context, element.project, navigation, result)

            SpelCompletionMode.TYPE ->
                addTypeCandidates(site, element.project, packages, result)
        }
    }

    /** `#` 之后的变量、函数与命名空间；`$` 开头时只提供参数简写。 */
    private fun addInvocationCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        if (site.word.startsWith("$")) {
            val target = result.withPrefixMatcher(site.word)
            for (symbol in context.available()) {
                if (symbol.kind != SpelSymbolKind.VARIABLE || !symbol.name.startsWith("$")) {
                    continue
                }
                target.addElement(variableElement(symbol, "", site.word))
            }
            return
        }

        val target = result.withPrefixMatcher("#" + site.word)
        val functions = ArrayList<SpelSymbol>()
        for (symbol in context.available()) {
            when (symbol.kind) {
                SpelSymbolKind.VARIABLE -> target.addElement(variableElement(symbol, "#", site.word))
                // 命名空间函数同样支持平铺调用（MethodSpaceMethodResolver 会从变量表查找）
                SpelSymbolKind.FUNCTION -> if (symbol.isCompletable()) functions += symbol
                SpelSymbolKind.NAMESPACE -> target.addElement(namespaceElement(symbol, site.word))
                else -> Unit
            }
        }
        addFunctionCandidates(
            target,
            functions.distinctBy { it.name to it.signature },
            "#",
            site.word,
            navigation
        )
    }

    /** 同名重载用签名区分 lookup，插入时再简化为函数名。 */
    private fun addFunctionCandidates(
        target: CompletionResultSet,
        functions: List<SpelSymbol>,
        prefix: String,
        word: String,
        navigation: SpelNavigation
    ) {
        val overloaded = functions.groupBy { it.name }.filterValues { it.size > 1 }.keys
        for (function in functions) {
            val lookup = if (function.name in overloaded) {
                prefix + function.name + signatureSuffix(function)
            } else {
                prefix + function.name
            }
            target.addElement(functionElement(function, lookup, word, navigation))
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
     * Root 变量插入时不带 `#`。
     *
     * 方法参数同时支持 `#name` 与裸 `name`（框架将其作为 Root 变量注入，
     * 如 `#{meta.fileId}`），因此这里也列参数原名；`$` 简写与 `::type`
     * 变体只在 `#` 前缀模式出现。普通 `@SpELImport(var = ...)` 变量必须
     * 带 `#`，这里仍列出但插入时自动补前缀，避免用户必须知道 `#` 写法。
     *
     * 函数同样支持平铺调用（如 `#{str_ne(a, b)}`，运行期从变量表查找），
     * 因此 Root 位置也列内置函数与 `@SpELImport` 导入的函数。
     */
    private fun addRootCandidates(
        site: SpelCompletionSite,
        context: SpelContext,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val target = result.withPrefixMatcher(site.word)
        val seen = HashSet<String>()
        val functions = ArrayList<SpelSymbol>()
        for (symbol in context.available()) {
            if (symbol.name.contains("::")) {
                continue
            }
            when (symbol.kind) {
                SpelSymbolKind.ROOT_VARIABLE -> {
                    if (!seen.add(symbol.name)) {
                        continue
                    }
                    // 响应变量（`$body$` 等）也列入初始候选：`#{` 弹出 lookup 后
                    // 输入 `$` 只会做前缀过滤，不会重新计算候选。
                    val element = LookupElementBuilder.create(symbol.name)
                        .withTailText(descriptionTail(symbol.description), true)
                        .withTypeText(symbol.type ?: "Object", true)
                        .withIcon(AllIcons.Nodes.Variable)
                    target.addElement(prioritize(element, symbol.name, site.word))
                }

                SpelSymbolKind.VARIABLE -> {
                    // 参数的 `$name`/`$n` 简写只在 `#` 前缀后提供，Root 位置不列
                    if (symbol.name.contains('$')) {
                        continue
                    }
                    if (symbol.origin == SpelSymbolOrigin.PARAMETER) {
                        if (!seen.add(symbol.name)) {
                            continue
                        }
                        val element = LookupElementBuilder.create(symbol.name)
                            .withTailText(descriptionTail(symbol.description), true)
                            .withTypeText(symbol.type ?: "Object", true)
                            .withIcon(AllIcons.Nodes.Parameter)
                        target.addElement(prioritize(element, symbol.name, site.word))
                    } else {
                        // 非 Root 变量：候选按变量名过滤，插入时补 `#`
                        if (!seen.add("#" + symbol.name)) {
                            continue
                        }
                        target.addElement(hashVariableElement(symbol, site.word))
                    }
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
            site.word,
            navigation
        )
    }

    /** 普通变量在 Root 位置补全时插入 `#name`。 */
    private fun hashVariableElement(symbol: SpelSymbol, word: String): LookupElement {
        val element = LookupElementBuilder.create(symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(AllIcons.Nodes.Variable)
            .withInsertHandler { context, _ ->
                context.document.insertString(context.startOffset, "#")
                context.editor.caretModel.moveToOffset(context.tailOffset + 1)
            }
        return prioritize(element, symbol.name, word)
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
                val target = result.withPrefixMatcher(site.word)
                addFunctionCandidates(target, functions, "", site.word, navigation)
                return
            }
        }

        val owner = SpelTypeResolver(project, context).resolve(qualifier)
        if (owner == null) {
            addUnknownMembers(site, result)
            return
        }
        addClassMembers(site, owner, navigation, result)
    }

    private fun addClassMembers(
        site: SpelCompletionSite,
        owner: PsiClass,
        navigation: SpelNavigation,
        result: CompletionResultSet
    ) {
        val target = result.withPrefixMatcher(site.word)
        val seenFields = HashSet<String>()
        for (field in owner.allFields) {
            val name = field.name
            // SpEL 通过反射读取字段，private 字段（Lombok 实体）也应参与补全
            if (!name.startsWith(site.word, true)) {
                continue
            }
            if (!seenFields.add(name)) {
                continue
            }
            val element = LookupElementBuilder.create(name)
                .withTailText(descriptionTail(docCommentOf(field)), true)
                .withTypeText(field.type.presentableText, true)
                .withIcon(AllIcons.Nodes.Field)
            target.addElement(prioritize(element, name, site.word))
        }

        val seenMethods = HashSet<String>()
        for (method in owner.allMethods) {
            if (method.isConstructor || !method.hasModifierProperty(PsiModifier.PUBLIC)) {
                continue
            }
            // getXxx/isXxx 按 JavaBean 属性名补全（`meta.fileId` 对应 `getFileId()`）
            val propertyName = propertyNameOf(method)
            if (propertyName != null &&
                propertyName.startsWith(site.word, true) &&
                seenFields.add(propertyName)
            ) {
                val element = LookupElementBuilder.create(propertyName)
                    .withTailText(descriptionTail(docCommentOf(method)), true)
                    .withTypeText(method.returnType?.presentableText ?: "?", true)
                    .withIcon(AllIcons.Nodes.Property)
                target.addElement(prioritize(element, propertyName, site.word))
            }
            if (!method.name.startsWith(site.word, true)) {
                continue
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
            target.addElement(prioritize(element, method.name, site.word))
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
        val target = result.withPrefixMatcher(site.word)
        for (name in UNKNOWN_MEMBERS) {
            val element = LookupElementBuilder.create(name)
                .withPresentableText("$name()")
                .withTypeText("类型未知", true)
                .withIcon(AllIcons.Nodes.Method)
                .withInsertHandler(functionCallHandler)
            target.addElement(prioritize(element, name, site.word))
        }
    }

    /** `T(` 之后的类型：`@SpELImport(pack)` 导入的包优先，其次项目类路径。 */
    private fun addTypeCandidates(
        site: SpelCompletionSite,
        project: Project,
        packages: List<String>,
        result: CompletionResultSet
    ) {
        val target = result.withPrefixMatcher(site.word)
        val facade = JavaPsiFacade.getInstance(project)
        val seen = HashSet<String>()

        for (packageName in packages) {
            val psiPackage = facade.findPackage(packageName) ?: continue
            for (psiClass in psiPackage.classes) {
                val simpleName = psiClass.name ?: continue
                if (!simpleName.startsWith(site.word, true)) {
                    continue
                }
                val qualifiedName = psiClass.qualifiedName ?: simpleName
                if (!seen.add(qualifiedName)) {
                    continue
                }
                target.addElement(typeElement(simpleName, qualifiedName, site.word))
            }
        }

        val scope = GlobalSearchScope.allScope(project)
        val cache = PsiShortNamesCache.getInstance(project)
        for (shortName in cache.allClassNames) {
            if (seen.size >= MAX_TYPE_CANDIDATES) {
                break
            }
            if (!shortName.startsWith(site.word, true)) {
                continue
            }
            val psiClass = cache.getClassesByName(shortName, scope).firstOrNull() ?: continue
            val qualifiedName = psiClass.qualifiedName ?: shortName
            if (!seen.add(qualifiedName)) {
                continue
            }
            target.addElement(typeElement(shortName, qualifiedName, site.word))
        }
    }

    private fun importedPackages(literal: PsiLiteralExpression): List<String> {
        val owners = LinkedHashSet<PsiModifierListOwner>()
        PsiTreeUtil.getParentOfType(literal, PsiMethod::class.java, true)?.let { owners += it }
        PsiTreeUtil.getParentOfType(literal, PsiClass::class.java, true)?.let { owners += it }
        return owners.flatMap { owner ->
            importParser.findImportAnnotations(owner).flatMap { importParser.parsePackages(it) }
        }.distinct()
    }

    /** 类型候选：主文本为简单类名，右侧显示限定名，与 IDEA 的类补全一致。 */
    private fun typeElement(
        simpleName: String,
        qualifiedName: String,
        word: String
    ): LookupElement = prioritize(
        LookupElementBuilder.create(simpleName)
            .withTypeText(qualifiedName, true)
            .withIcon(AllIcons.Nodes.Class),
        simpleName,
        word
    )

    private fun variableElement(
        symbol: SpelSymbol,
        prefix: String,
        word: String
    ): LookupElement {
        val element = LookupElementBuilder.create(prefix + symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(variableIcon(symbol))
        return prioritize(element, symbol.name, word)
    }

    /** 变量类候选的图标：方法参数用参数图标，其余变量用变量图标。 */
    private fun variableIcon(symbol: SpelSymbol): Icon =
        if (symbol.origin == SpelSymbolOrigin.PARAMETER) AllIcons.Nodes.Parameter
        else AllIcons.Nodes.Variable

    private fun functionElement(
        symbol: SpelSymbol,
        lookup: String,
        word: String,
        navigation: SpelNavigation
    ): LookupElement {
        // 主文本展示签名（含参数类型），返回类型靠右，与 IDEA 默认方法提示框一致
        val declaration = navigation.describe(symbol)
        val element = LookupElementBuilder.create(declaration, lookup)
            .withPresentableText(functionPresentation(symbol))
            .withTypeText(symbol.type ?: "Object", true)
            .withIcon(AllIcons.Nodes.Function)
            .withInsertHandler(functionCallHandler)
            .withPsiElement(declaration)
        return prioritize(element, symbol.name, word)
    }

    private fun namespaceElement(symbol: SpelSymbol, word: String): LookupElement {
        val element = LookupElementBuilder.create("#" + symbol.name)
            .withTailText(descriptionTail(symbol.description), true)
            .withTypeText(symbol.type ?: "未知", true)
            .withIcon(AllIcons.Nodes.Class)
        return prioritize(element, symbol.name, word)
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

    /** 前缀精确匹配优先，其次前缀匹配。 */
    private fun prioritize(element: LookupElement, name: String, word: String): LookupElement {
        val priority = when {
            word.isEmpty() -> 0.0
            name == word -> 1000.0
            name.equals(word, true) -> 900.0
            name.startsWith(word, true) -> 100.0
            else -> 0.0
        }
        return PrioritizedLookupElement.withPriority(element, priority)
    }

    /** 补全的前提：位于 `#{` 字面量内且模块已激活（dumb mode 下直接退出）。 */
    internal fun isApplicable(literal: PsiLiteralExpression): Boolean =
        literal.text.contains("#{") &&
            LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)

    /**
     * 补全副本的激活判断：`position` 属于编辑器副本，模块按 [originalFile] 查找。
     */
    internal fun isApplicable(literal: PsiLiteralExpression, originalFile: PsiFile): Boolean {
        if (!literal.text.contains("#{")) {
            return false
        }
        if (DumbService.isDumb(literal.project)) {
            return false
        }
        return LuckyHttpClientPresence.getInstance(literal.project)
            .isEnabledForCopy(literal, originalFile)
    }

    private companion object {
        val UNKNOWN_MEMBERS = listOf("toString", "getClass", "equals", "hashCode")
        const val MAX_TYPE_CANDIDATES = 200

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
    }
}
