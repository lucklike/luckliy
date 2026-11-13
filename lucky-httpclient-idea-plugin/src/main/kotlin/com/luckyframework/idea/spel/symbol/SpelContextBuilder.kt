package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.luckyframework.idea.spel.metadata.ParamNameResolver
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 从字符串字面量推导 SpEL 上下文（design D5）。
 *
 * 符号层按优先级叠加，同名符号由更近的来源胜出：
 * 方法参数 > 方法级 `@SpELImport` > 类级 `@SpELImport` > 内置符号 > 全局变量。
 *
 * 内置与全局来源通过 [SpelSymbolSource] 注入：内置符号来自
 * [BuiltinSymbolsLoader] 生成的表；全局变量只纳入可静态解析的配置部分。
 */
class SpelContextBuilder(
    private val resolver: SpelAnnotationResolver = SpelAnnotationResolver(),
    private val paramNameResolver: ParamNameResolver = ParamNameResolver(),
    private val importParser: SpelImportParser = SpelImportParser(),
    private val importedClasses: ImportedClassSymbols = ImportedClassSymbols(),
    private val builtinSource: SpelSymbolSource = BuiltinSymbolsLoader.source(),
    private val globalSource: SpelSymbolSource = EmptySpelSymbolSource
) {

    fun build(literal: PsiLiteralExpression): SpelContext = build(literal, false)

    /**
     * 构建字面量的符号上下文。
     *
     * [lenient] 为 true 时，即使字面量不在已知注解属性中也照常返回符号层
     * （补全用；`known` 保持 false，阶段不参与过滤）。
     */
    fun build(literal: PsiLiteralExpression, lenient: Boolean): SpelContext {
        val attribute = resolver.resolveLiteral(literal)
        if (attribute == null && !lenient) {
            return SpelContext.unknown()
        }
        val stage = attribute?.lifecycle ?: SpelLifecycle.UNKNOWN
        val method = PsiTreeUtil.getParentOfType(literal, PsiMethod::class.java, true)
        val containingClass = PsiTreeUtil.getParentOfType(literal, PsiClass::class.java, true)

        val parameters = method?.let { parameterSymbols(it) } ?: emptyList()
        val methodLayer = method?.let { ownerLayer(it, SpelSymbolOrigin.METHOD_IMPORT) } ?: ImportedSymbols()
        val classLayer = containingClass?.let { classLayer(it) } ?: ImportedSymbols()
        val builtin = builtinSource.symbols().map {
            contextualize(it.copy(lifecycle = LifecycleTable.availableFrom(it.name)), method, containingClass)
        }
        val global = globalSource.symbols()

        val merged = merge(parameters, methodLayer.symbols, classLayer.symbols, builtin, global)
        return SpelContext(
            known = attribute != null,
            stage = stage,
            attribute = attribute,
            symbols = applyVisibility(merged, stage),
            disabledSymbols = (methodLayer.disabled + classLayer.disabled)
                .distinctBy { it.kind to it.name }
        )
    }

    /**
     * 运行期类型由所在位置决定的内置变量：
     *
     * - `$this$` 是当前 API 的代理对象（`ClassContext.getProxyObject()`），静态类型即所在类；
     * - `$result$` 是 `MethodContext.handleResultAndReturn` 的入参，按
     *   `getResultResolvableType()` 推导（结果处理器泛型 / 包装类型拆包）；
     * - `$body$`/`$jsonBody$`/`$xmlBody$` 是 `CommonFunctions.sta` 基于
     *   `getConvertMetaType(context).getMetaType()` 转换的结果，按 `@ConvertMetaType` 推导。
     *
     * 声明改为当前类/当前方法后，类型解析与成员补全直接命中真实 PSI，
     * 符号表里的 `Object` 只在没有所在类/方法时兜底。
     *
     * 跳转不落到推导类型的类声明：内置变量的声明在框架内部，正常不跳转；
     * `$result$`/`$body$` 能推导出类型时，跳转到**类型在源码中的书写位置**
     * （[SpelSymbol.jumpTarget]）：`$body$` 指向注解中的类型属性值
     * （如 `@ConvertMetaType(Item.class)`），`$result$` 指向返回类型中的类型引用
     * 或处理器注解里的类字面量。类型不可知时保留原声明。
     */
    private fun contextualize(symbol: SpelSymbol, method: PsiMethod?, containingClass: PsiClass?): SpelSymbol =
        when (symbol.name) {
            THIS_VARIABLE -> containingClass
                ?.let { symbol.copy(type = it.name ?: symbol.type, declaration = it) }
                ?: symbol

            RESULT_VARIABLE -> method
                ?.let { resultSymbol(symbol, it, containingClass) }
                ?: symbol

            BODY_VARIABLE, JSON_BODY_VARIABLE, XML_BODY_VARIABLE -> method
                ?.let { convertMetaType(it, containingClass) }
                ?.let { resolution ->
                    symbol.copy(
                        type = resolution.type.presentableText,
                        resolvedType = resolution.type,
                        jumpTarget = resolution.target
                    )
                }
                ?: symbol

            else -> symbol
        }

    /**
     * `$result$` 的类型按 `MethodContext.getResultResolvableType()` 推导；
     * 跳转定位到类型在源码中的书写位置（返回类型引用或处理器注解中的类字面量），
     * 类型不可知时仍指向方法。
     */
    private fun resultSymbol(symbol: SpelSymbol, method: PsiMethod, containingClass: PsiClass?): SpelSymbol {
        val resolution = resultType(method, containingClass)
            ?: return symbol.copy(declaration = method)
        return symbol.copy(
            type = resolution.type.presentableText,
            declaration = method,
            resolvedType = resolution.type,
            jumpTarget = resolution.target
        )
    }

    /**
     * 在类型元素中定位推导出的类型引用：`Optional<Item>` 找拆包后的 `Item`，
     * 非拆包时根元素即目标。基本类型与无法定位时返回 null，跳转回退原声明。
     */
    private fun typeElementOf(root: PsiTypeElement?, target: PsiType): PsiTypeElement? {
        if (root == null) {
            return null
        }
        if (root.type == target) {
            return root
        }
        return PsiTreeUtil.collectElementsOfType(root, PsiTypeElement::class.java)
            .firstOrNull { it.type == target }
    }

    /**
     * 对齐 `MethodContext.getResultResolvableType()`：
     *
     * - void 方法配置了结果处理器时，结果是 `ResultHandler<T>` 的 `T`
     *   （`handleResult` 会先剥掉处理器上的 `Optional`）；
     * - 其余按默认 `PackTypeParser` 顺序拆包：`Future`、`SpelBean`、`SimpleSpelBean`、
     *   `FlatBean`、`Optional`、`Outcome` 都取第一个泛型实参；
     * - void 方法没有结果，不推导。
     */
    private fun resultType(method: PsiMethod, containingClass: PsiClass?): TypeResolution? {
        val returnType = method.returnType ?: return null
        if (isVoid(returnType)) {
            val handler = resultHandlerType(method, containingClass) ?: return null
            val type = unwrapOptional(handler.type, method) ?: return null
            return TypeResolution(type, handler.target)
        }
        val type = unwrapPackType(returnType, method) ?: return null
        return TypeResolution(type, typeElementOf(method.returnTypeElement, type))
    }

    /**
     * 结果处理器上的结果类型：参数列表中的 `ResultHandler<T>` 优先，
     * 其次是 `@ResultHandlerMeta` 声明的处理器（对齐 `getResultHandlerHolder()`）。
     * 方法没有声明时继续沿所在类的继承体系查找。
     */
    private fun resultHandlerType(method: PsiMethod, containingClass: PsiClass?): TypeResolution? {
        method.parameterList.parameters.forEach { parameter ->
            if (isAssignableTo(parameter.type, RESULT_HANDLER)) {
                val type = genericArgument(parameter.type, 0) ?: return null
                return TypeResolution(type, typeElementOf(parameter.typeElement, type) ?: parameter.typeElement)
            }
        }
        resultHandlerMetaValues(method)?.let { return resolveResultHandlerType(it) }
        containingClass?.let { psiClass ->
            for (owner in classHierarchy(psiClass)) {
                resultHandlerMetaValues(owner)?.let { return resolveResultHandlerType(it) }
            }
        }
        return null
    }

    /**
     * 对齐 `generateObject(handler(), handlerClass(), ResultHandler.class)`：
     * `handler()` 有效时优先，其次 `handlerClass()`；两者都停留在基类 `ResultHandler` 时不推导。
     * 跳转目标是处理器类字面量（`X.class`）的书写位置。
     */
    private fun resolveResultHandlerType(values: ResultHandlerMetaValues): TypeResolution? {
        values.handler?.let { generate ->
            // handler() 只配置了 msg 等无法静态推导的信息时，运行期类型不可知
            val generateClass = objectGenerateClass(generate) ?: return null
            if (generateClass.qualifiedName != RESULT_HANDLER) {
                val type = resultHandlerGenericArgument(generateClass) ?: return null
                return TypeResolution(type, objectGenerateTarget(generate))
            }
        }
        val handlerClass = (values.handlerClass as? PsiClassType)?.resolve() ?: return null
        if (handlerClass.qualifiedName == RESULT_HANDLER) {
            return null
        }
        val type = resultHandlerGenericArgument(handlerClass) ?: return null
        return TypeResolution(type, values.handlerClassElement)
    }

    /** `@ObjectGenerate` 的 `value`/`clazz` 互为别名，取显式书写的那个。 */
    private fun objectGenerateClass(generate: PsiAnnotation): PsiClass? {
        for (pair in generate.parameterList.attributes) {
            val attribute = pair.name ?: DEFAULT_ATTRIBUTE
            if (attribute != "value" && attribute != "clazz") {
                continue
            }
            (classValue(pair.value) as? PsiClassType)?.resolve()?.let { return it }
        }
        return null
    }

    /** `@ObjectGenerate` 中类字面量（`X.class`）的位置，供 `$result$` 跳转到处理器声明处。 */
    private fun objectGenerateTarget(generate: PsiAnnotation): PsiElement? {
        for (pair in generate.parameterList.attributes) {
            val attribute = pair.name ?: DEFAULT_ATTRIBUTE
            if (attribute != "value" && attribute != "clazz") {
                continue
            }
            (pair.value as? PsiClassObjectAccessExpression)?.let { return it }
        }
        return null
    }

    /**
     * 读取对象上 `@ResultHandlerMeta` 的合并结果：直接标注优先；
     * 否则看组合注解上的属性值（显式书写或声明处默认值），别名图既包含
     * `@AliasFor` 的多级别名链，也包含元注解同名属性形成的隐式别名。
     *
     * 显式属性与声明默认值分开收集，避免元注解默认值 `ResultHandler`
     * 按注解顺序覆盖用户真正写的处理器类型。
     */
    private fun resultHandlerMetaValues(owner: PsiModifierListOwner): ResultHandlerMetaValues? {
        val annotations = owner.modifierList?.annotations ?: return null
        annotations.firstOrNull { it.qualifiedName == RESULT_HANDLER_META }?.let { annotation ->
            return resultHandlerMetaValuesOf(annotation)
        }
        var explicit: ResultHandlerMetaValues? = null
        var fallback: ResultHandlerMetaValues? = null
        for (annotation in annotations) {
            val declaration = annotation.resolveAnnotationType() ?: continue
            explicit = mergeResultHandlerMetaValues(explicit, aliasedResultHandlerMetaValues(annotation, declaration))
            fallback = mergeResultHandlerMetaValues(fallback, declaredResultHandlerMetaValues(declaration, HashSet(), 0))
        }
        return mergeResultHandlerMetaValues(fallback, explicit)
    }

    private fun resultHandlerMetaValuesOf(annotation: PsiAnnotation): ResultHandlerMetaValues {
        val handlerClassValue = annotation.findAttributeValue("handlerClass")
        return ResultHandlerMetaValues(
            handlerClass = classValue(handlerClassValue),
            handlerClassElement = handlerClassValue as? PsiClassObjectAccessExpression,
            handler = annotation.findAttributeValue("handler") as? PsiAnnotation
        )
    }

    /** [winner] 覆盖 [base] 的非空字段，[base] 只补充空缺。 */
    private fun mergeResultHandlerMetaValues(
        base: ResultHandlerMetaValues?,
        winner: ResultHandlerMetaValues?
    ): ResultHandlerMetaValues? {
        if (base == null) {
            return winner
        }
        if (winner == null) {
            return base
        }
        return ResultHandlerMetaValues(
            handlerClass = winner.handlerClass ?: base.handlerClass,
            handlerClassElement = if (winner.handlerClass != null) winner.handlerClassElement else base.handlerClassElement,
            handler = winner.handler ?: base.handler
        )
    }

    private fun aliasedResultHandlerMetaValues(
        annotation: PsiAnnotation,
        declaration: PsiClass
    ): ResultHandlerMetaValues? {
        var handlerClass: PsiType? = null
        var handlerClassElement: PsiElement? = null
        var handler: PsiAnnotation? = null
        var found = false
        for (pair in annotation.parameterList.attributes) {
            val attribute = pair.name ?: DEFAULT_ATTRIBUTE
            when (resolveResultHandlerAttribute(declaration, attribute, HashSet(), 0)) {
                "handlerClass" -> {
                    handlerClass = classValue(pair.value)
                    handlerClassElement = pair.value as? PsiClassObjectAccessExpression
                    found = true
                }

                "handler" -> {
                    handler = pair.value as? PsiAnnotation
                    found = true
                }
            }
        }
        return if (found) ResultHandlerMetaValues(handlerClass, handlerClassElement, handler) else null
    }

    /**
     * 沿别名图找到 `@ResultHandlerMeta` 上的目标属性，找不到返回 null。运行期 Spring 的
     * 合并语义包含三类边：
     *
     * - `@AliasFor` 注解内别名；
     * - `@AliasFor` 跨注解别名（`annotation = X.class`），目标不是 `@ResultHandlerMeta` 时继续传递；
     * - 元注解同名属性形成的隐式别名。
     */
    private fun resolveResultHandlerAttribute(
        declaration: PsiClass,
        attribute: String,
        visited: MutableSet<String>,
        depth: Int
    ): String? {
        if (depth > ALIAS_MAX_DEPTH) {
            return null
        }
        val key = "${declaration.qualifiedName}#$attribute"
        if (!visited.add(key)) {
            return null
        }
        val method = declaration.findMethodsByName(attribute, false).firstOrNull() ?: return null
        // 到达锚点：属性名即 @ResultHandlerMeta 的语义属性
        if (declaration.qualifiedName == RESULT_HANDLER_META) {
            return attribute
        }
        findAliasFor(method)?.let { alias ->
            val targetAnnotation = (classValue(alias.findAttributeValue("annotation")) as? PsiClassType)
                ?.resolve()
                ?.takeUnless { it.qualifiedName == JAVA_LANG_ANNOTATION }
            val targetAttribute = aliasText(alias, "attribute")?.takeIf { it.isNotBlank() }
                ?: aliasText(alias, "value")?.takeIf { it.isNotBlank() }
                ?: attribute
            return if (targetAnnotation == null) {
                resolveResultHandlerAttribute(declaration, targetAttribute, visited, depth + 1)
            } else {
                resolveResultHandlerAttribute(targetAnnotation, targetAttribute, visited, depth + 1)
            }
        }
        // 隐式别名：元注解中存在同名属性时继续向上查找
        for (meta in declaration.modifierList?.annotations ?: emptyArray()) {
            val metaFqName = meta.qualifiedName ?: continue
            if (metaFqName.startsWith("java.lang.annotation.")) {
                continue
            }
            val metaDeclaration = meta.resolveAnnotationType() ?: continue
            val target = resolveResultHandlerAttribute(metaDeclaration, attribute, visited, depth + 1)
            if (target != null) {
                return target
            }
        }
        return null
    }

    /**
     * 读取注解声明处的元注解值：沿元注解链递归展开，每一层用其上显式书写的
     * 属性值覆盖更深层的默认值。运行期 Spring 的隐式别名会让同名属性沿元注解链传递，
     * 因此 `@MyHandlerMeta` 声明处的 `@ResultHandlerMeta(handlerClass = X.class)` 对使用方可见。
     */
    private fun declaredResultHandlerMetaValues(
        declaration: PsiClass,
        visited: MutableSet<String>,
        depth: Int
    ): ResultHandlerMetaValues? {
        if (depth > ALIAS_MAX_DEPTH) {
            return null
        }
        val fqName = declaration.qualifiedName ?: return null
        if (!visited.add(fqName)) {
            return null
        }
        var values: ResultHandlerMetaValues? = null
        for (annotation in declaration.modifierList?.annotations ?: return values) {
            val annotationFqName = annotation.qualifiedName ?: continue
            if (annotationFqName == RESULT_HANDLER_META) {
                values = mergeResultHandlerMetaValues(values, resultHandlerMetaValuesOf(annotation))
                continue
            }
            if (annotationFqName.startsWith("java.lang.annotation.")) {
                continue
            }
            val metaDeclaration = annotation.resolveAnnotationType() ?: continue
            values = mergeResultHandlerMetaValues(
                values,
                declaredResultHandlerMetaValues(metaDeclaration, visited, depth + 1)
            )
            values = mergeResultHandlerMetaValues(values, aliasedResultHandlerMetaValues(annotation, metaDeclaration))
        }
        return values
    }
    private fun resultHandlerGenericArgument(handlerClass: PsiClass): PsiType? {
        val project = handlerClass.project
        val resultHandler = JavaPsiFacade.getInstance(project)
            .findClass(RESULT_HANDLER, GlobalSearchScope.allScope(project)) ?: return null
        val handlerType = JavaPsiFacade.getElementFactory(project).createType(handlerClass)
        val argument = PsiUtil.substituteTypeParameter(handlerType, resultHandler, 0, false) ?: return null
        return argument.takeUnless { isObject(it) }
    }

    /** 处理器类型本身是 `Optional<T>` 时，运行期传给处理函数的是 `T`。 */
    private fun unwrapOptional(type: PsiType, context: PsiElement): PsiType? {
        if (!isAssignableTo(type, OPTIONAL)) {
            return type
        }
        return genericArgument(type, 0) ?: objectType(context)
    }

    /** 包装类型拆包：无泛型时按 `SingleGenericPackTypeParser` 回退 `Object`。 */
    private fun unwrapPackType(type: PsiType, context: PsiElement): PsiType? {
        val names = hierarchyNames(type)
        if (names.none { it in PACK_TYPE_NAMES }) {
            return type
        }
        return genericArgument(type, 0) ?: objectType(context)
    }

    /**
     * `$body$`/`$jsonBody$`/`$xmlBody$` 的静态类型，对齐 `Context.getConvertMetaType()`：
     * 优先 `typeFunc()`（结果由函数运行期决定，静态无法确定），其次 `type()` 中可静态解析的
     * `T(X)`/字符串常量，最后 `value()`；注解按「方法 → 类（含继承）」的顺序查找。
     * 无注解时运行期使用 `ConvertMetaData.DEFAULT`（`Object`），保持内置表类型不变。
     */
    private fun convertMetaType(method: PsiMethod, containingClass: PsiClass?): TypeResolution? {
        metaTypeValues(method)?.let { return resolveMetaType(it, method) }
        containingClass?.let { psiClass ->
            for (owner in classHierarchy(psiClass)) {
                metaTypeValues(owner)?.let { return resolveMetaType(it, method) }
            }
        }
        return null
    }

    /** 类型与它在注解中的书写位置（类字面量或类型字符串），供 `$body$` 跳转。 */
    private fun resolveMetaType(values: MetaTypeValues, context: PsiElement): TypeResolution? {
        // 函数的结果只有运行期才知道
        if (!values.typeFuncText.isNullOrBlank()) {
            return null
        }
        if (!values.typeText.isNullOrBlank()) {
            val type = staticTypeReference(values.typeText, context) ?: return null
            return TypeResolution(type, values.typeElement)
        }
        return values.value?.let { TypeResolution(it, values.valueElement) }
    }

    /**
     * 读取对象上 `@ConvertMetaType` 的合并结果：直接标注优先；
     * 否则看组合注解上的属性值（显式书写或声明处默认值），别名图既包含
     * `@AliasFor` 的多级别名链（如 `@RespConvert#metaType` → `@ResultConvertMeta#metaType`
     * → `@ConvertMetaType#value`），也包含元注解同名属性形成的隐式别名
     * （如用户自定义 `@MyConvert#metaType` → `@ResultConvertMeta#metaType`）。
     *
     * 显式属性与声明默认值分开收集：`@ResultConvertMeta` 的 `@ConvertMetaType` 会用默认值
     * `Object` 填充所有字段，如果与显式值混在一起，会按注解顺序覆盖掉用户真正写的类型。
     */
    private fun metaTypeValues(owner: PsiModifierListOwner): MetaTypeValues? {
        val annotations = owner.modifierList?.annotations ?: return null
        annotations.firstOrNull { it.qualifiedName == CONVERT_META_TYPE }?.let { annotation ->
            return metaTypeValuesOf(annotation)
        }
        var explicit: MetaTypeValues? = null
        var fallback: MetaTypeValues? = null
        for (annotation in annotations) {
            val declaration = annotation.resolveAnnotationType() ?: continue
            explicit = mergeMetaTypeValues(explicit, aliasedMetaTypeValues(annotation, declaration))
            fallback = mergeMetaTypeValues(fallback, declaredMetaTypeValues(declaration, HashSet(), 0))
        }
        return mergeMetaTypeValues(fallback, explicit)
    }

    private fun metaTypeValuesOf(annotation: PsiAnnotation): MetaTypeValues {
        val value = annotation.findAttributeValue("value")
        val type = annotation.findAttributeValue("type")
        return MetaTypeValues(
            value = classValue(value),
            valueElement = value as? PsiClassObjectAccessExpression,
            typeText = textValue(type),
            typeElement = type,
            typeFuncText = textValue(annotation.findAttributeValue("typeFunc"))
        )
    }

    /** [winner] 覆盖 [base] 的非空字段，[base] 只补充空缺。 */
    private fun mergeMetaTypeValues(base: MetaTypeValues?, winner: MetaTypeValues?): MetaTypeValues? {
        if (base == null) {
            return winner
        }
        if (winner == null) {
            return base
        }
        return MetaTypeValues(
            value = winner.value ?: base.value,
            valueElement = if (winner.value != null) winner.valueElement else base.valueElement,
            typeText = winner.typeText?.takeIf { it.isNotBlank() } ?: base.typeText,
            typeElement = if (!winner.typeText.isNullOrBlank()) winner.typeElement else base.typeElement,
            typeFuncText = winner.typeFuncText?.takeIf { it.isNotBlank() } ?: base.typeFuncText
        )
    }

    private fun aliasedMetaTypeValues(annotation: PsiAnnotation, declaration: PsiClass): MetaTypeValues? {
        var value: PsiType? = null
        var valueElement: PsiElement? = null
        var typeText: String? = null
        var typeElement: PsiElement? = null
        var typeFuncText: String? = null
        var found = false
        for (pair in annotation.parameterList.attributes) {
            val attribute = pair.name ?: DEFAULT_ATTRIBUTE
            when (resolveMetaTypeAttribute(declaration, attribute, HashSet(), 0)) {
                "value" -> {
                    value = classValue(pair.value)
                    valueElement = pair.value as? PsiClassObjectAccessExpression
                    found = true
                }

                "type" -> {
                    typeText = textValue(pair.value)
                    typeElement = pair.value
                    found = true
                }

                "typeFunc" -> {
                    typeFuncText = textValue(pair.value)
                    found = true
                }
            }
        }
        return if (found) MetaTypeValues(value, valueElement, typeText, typeElement, typeFuncText) else null
    }

    /**
     * 沿别名图找到 `@ConvertMetaType` 上的目标属性，找不到返回 null。运行期 Spring 的合并语义
     * 包含三类边：
     *
     * - `@AliasFor` 注解内别名（`annotation` 缺省，如 `@AliasFor("result")`）；
     * - `@AliasFor` 跨注解别名（`annotation = X.class`），目标不是 `@ConvertMetaType` 时继续传递；
     * - 元注解同名属性形成的隐式别名（`@MyConvert#metaType` → `@ResultConvertMeta#metaType`）。
     */
    private fun resolveMetaTypeAttribute(
        declaration: PsiClass,
        attribute: String,
        visited: MutableSet<String>,
        depth: Int
    ): String? {
        if (depth > ALIAS_MAX_DEPTH) {
            return null
        }
        val key = "${declaration.qualifiedName}#$attribute"
        if (!visited.add(key)) {
            return null
        }
        val method = declaration.findMethodsByName(attribute, false).firstOrNull() ?: return null
        // 到达锚点：属性名即 @ConvertMetaType 的语义属性
        if (declaration.qualifiedName == CONVERT_META_TYPE) {
            return attribute
        }
        findAliasFor(method)?.let { alias ->
            val targetAnnotation = (classValue(alias.findAttributeValue("annotation")) as? PsiClassType)
                ?.resolve()
                ?.takeUnless { it.qualifiedName == JAVA_LANG_ANNOTATION }
            val targetAttribute = aliasText(alias, "attribute")?.takeIf { it.isNotBlank() }
                ?: aliasText(alias, "value")?.takeIf { it.isNotBlank() }
                ?: attribute
            return if (targetAnnotation == null) {
                resolveMetaTypeAttribute(declaration, targetAttribute, visited, depth + 1)
            } else {
                resolveMetaTypeAttribute(targetAnnotation, targetAttribute, visited, depth + 1)
            }
        }
        // 隐式别名：元注解中存在同名属性时继续向上查找
        for (meta in declaration.modifierList?.annotations ?: emptyArray()) {
            val metaFqName = meta.qualifiedName ?: continue
            if (metaFqName.startsWith("java.lang.annotation.")) {
                continue
            }
            val metaDeclaration = meta.resolveAnnotationType() ?: continue
            val target = resolveMetaTypeAttribute(metaDeclaration, attribute, visited, depth + 1)
            if (target != null) {
                return target
            }
        }
        return null
    }

    private fun aliasText(alias: PsiAnnotation, name: String): String? =
        runCatching { textValue(alias.findAttributeValue(name)) }.getOrNull()

    private fun findAliasFor(owner: PsiModifierListOwner): PsiAnnotation? {
        owner.getAnnotation(ALIAS_FOR)?.let { return it }
        // spring-core 不在 classpath 时 FQ 名无法解析，退化为按简单名匹配
        return owner.modifierList?.annotations?.firstOrNull {
            it.qualifiedName == ALIAS_FOR || it.nameReferenceElement?.referenceName == "AliasFor"
        }
    }

    /**
     * 读取注解声明处的元类型默认值：沿元注解链递归展开，每一层用其上显式书写的
     * 属性值覆盖更深层的默认值。运行期 Spring 的隐式别名会让同名属性沿元注解链传递，
     * 因此 `@MyConvert` 声明处的 `@ResultConvertMeta(metaType = X.class)` 对使用方可见。
     */
    private fun declaredMetaTypeValues(
        declaration: PsiClass,
        visited: MutableSet<String>,
        depth: Int
    ): MetaTypeValues? {
        if (depth > ALIAS_MAX_DEPTH) {
            return null
        }
        val fqName = declaration.qualifiedName ?: return null
        if (!visited.add(fqName)) {
            return null
        }
        var values: MetaTypeValues? = null
        for (annotation in declaration.modifierList?.annotations ?: return values) {
            val annotationFqName = annotation.qualifiedName ?: continue
            if (annotationFqName == CONVERT_META_TYPE) {
                values = mergeMetaTypeValues(values, metaTypeValuesOf(annotation))
                continue
            }
            if (annotationFqName.startsWith("java.lang.annotation.")) {
                continue
            }
            val metaDeclaration = annotation.resolveAnnotationType() ?: continue
            values = mergeMetaTypeValues(values, declaredMetaTypeValues(metaDeclaration, visited, depth + 1))
            values = mergeMetaTypeValues(values, aliasedMetaTypeValues(annotation, metaDeclaration))
        }
        return values
    }

    /** 解析 `type()` 中可静态确定的类型：`T(X)` 或字符串常量形式的类名。 */
    private fun staticTypeReference(expression: String, context: PsiElement): PsiType? {
        val text = expression.trim()
        val typeName = when {
            text.length > 3 && text.startsWith("T(") && text.endsWith(")") ->
                text.substring(2, text.length - 1).trim()

            text.length > 2 && isQuoted(text) -> text.substring(1, text.length - 1).trim()
            else -> return null
        }
        if (typeName.isEmpty()) {
            return null
        }
        return findClass(context, typeName)
            ?.let { JavaPsiFacade.getElementFactory(context.project).createType(it) }
    }

    private fun isQuoted(text: String): Boolean =
        (text.startsWith("'") && text.endsWith("'")) ||
            (text.startsWith("\"") && text.endsWith("\""))

    private fun findClass(context: PsiElement, typeName: String): PsiClass? {
        val project = context.project
        val scope = GlobalSearchScope.allScope(project)
        JavaPsiFacade.getInstance(project).findClass(typeName, scope)?.let { return it }
        if (typeName.contains('.')) {
            return null
        }
        val candidates = runCatching {
            PsiShortNamesCache.getInstance(project).getClassesByName(typeName, scope)
        }.getOrElse { PsiClass.EMPTY_ARRAY }
        return candidates.firstOrNull { it.qualifiedName?.startsWith(LUCKY_PACKAGE_PREFIX) == true }
            ?: candidates.firstOrNull()
    }

    /** 类型的继承体系简单名（含自身）；PSI 无法解析时退化为类型文本中的简单名。 */
    private fun hierarchyNames(type: PsiType): Set<String> {
        val classType = type as? PsiClassType ?: return emptySet()
        val names = LinkedHashSet<String>()
        val visited = HashSet<String>()
        fun visit(current: PsiClass?) {
            if (current == null) {
                return
            }
            val key = current.qualifiedName ?: current.name ?: return
            if (!visited.add(key)) {
                return
            }
            current.name?.let { names += it }
            visit(current.superClass)
            current.interfaces.forEach { visit(it) }
        }
        visit(classType.resolve())
        if (names.isEmpty()) {
            simpleTypeName(classType.presentableText)?.let { names += it }
        }
        return names
    }

    private fun isAssignableTo(type: PsiType, target: String): Boolean =
        target.substringAfterLast('.') in hierarchyNames(type)

    private fun isVoid(type: PsiType): Boolean = type == PsiType.VOID || type.presentableText == "void"

    /**
     * 第一个泛型实参；通配符按 `ResolvableType.resolve()` 的语义取上界，
     * 未绑定类型变量无法静态解析。
     */
    private fun genericArgument(type: PsiType, index: Int): PsiType? {
        val classType = type as? PsiClassType ?: return null
        val parameter = classType.parameters.getOrNull(index) ?: return null
        return when {
            parameter is PsiWildcardType -> parameter.extendsBound
            (parameter as? PsiClassType)?.resolve() is PsiTypeParameter -> null
            else -> parameter
        }
    }

    private fun isObject(type: PsiType): Boolean =
        (type as? PsiClassType)?.resolve()?.qualifiedName == "java.lang.Object"

    private fun objectType(context: PsiElement): PsiType? = runCatching {
        PsiType.getJavaLangObject(context.manager, GlobalSearchScope.allScope(context.project))
    }.getOrNull()

    private fun classValue(value: Any?): PsiType? =
        (value as? PsiClassObjectAccessExpression)?.operand?.type

    private fun textValue(value: Any?): String? = (value as? PsiLiteralExpression)?.value as? String

    private fun simpleTypeName(type: String): String? {
        var name = type.substringBefore('<').trim()
        while (name.endsWith("[]")) {
            name = name.removeSuffix("[]").trim()
        }
        return name.substringAfterLast('.').takeIf { it.isNotEmpty() }
    }

    private data class ResultHandlerMetaValues(
        val handlerClass: PsiType?,
        val handlerClassElement: PsiElement?,
        val handler: PsiAnnotation?
    )

    private data class MetaTypeValues(
        val value: PsiType?,
        val valueElement: PsiElement?,
        val typeText: String?,
        val typeElement: PsiElement?,
        val typeFuncText: String?
    )

    /** 推导出的类型及其在源码中的书写位置（注解类字面量、类型字符串或返回类型引用）。 */
    private data class TypeResolution(val type: PsiType, val target: PsiElement?)

    private fun ownerLayer(owner: PsiModifierListOwner, origin: SpelSymbolOrigin): ImportedSymbols =
        ImportedSymbols(importParser.symbols(owner, origin)) + importedClasses.collect(owner, origin)

    /**
     * 类层符号：类上的 `@SpELImport` 声明，加上当前类（含父类、父接口）
     * 声明的 public static 方法。
     *
     * 后者对应运行期 `ClassContext.loadClassSpELFun`：它通过
     * `ClassStaticElement` 把继承体系里所有 public static 方法注册成
     * SpEL 函数，因此 `#{chunk_success(...)}` 这类调用可以平铺使用。
     * `allMethods` 已包含父类与父接口的方法，且统一按当前类的
     * `@Namespace` 归组，与 `ClassStaticElement.create(clazz)` 一致。
     */
    private fun classLayer(psiClass: PsiClass): ImportedSymbols {
        var result = ImportedSymbols()
        for (owner in classHierarchy(psiClass)) {
            result = result + ownerLayer(owner, SpelSymbolOrigin.CLASS_IMPORT)
        }
        return result + ImportedSymbols(
            importedClasses.symbolsOf(psiClass, SpelSymbolOrigin.CLASS_STATIC)
        )
    }

    /**
     * 方法参数八种命名：`name`、`$name`、`$n`、`$$n`、`name::type`、`$n::type`
     * 以及参数上下文对象 `$pc$n`、`$pc$name`。
     *
     * 与运行期 `ParameterContext.initContext` 一致，`n` 从 0 开始。
     * 所有形式都注册在 Root 变量空间（`METHOD_CONTEXT_ARGS_SPACE`），
     * 只能使用裸名称引用，写成 `#name` 在运行期解析不到。
     * 参数名不是直接取方法签名里的名字：运行期由
     * `ParameterUtils.getParamName` 取**合并后的** `@Param#value`，
     * 它可能来自参数上的组合注解经 `@AliasFor` 链传递的值
     * （例如 `@Query("req")` 注入的是 `req`）。具体语义见 [ParamNameResolver]。
     * 描述按运行期语义区分最终值/原始值/类型/上下文。
     */
    private fun parameterSymbols(method: PsiMethod): List<SpelSymbol> {
        val result = ArrayList<SpelSymbol>()
        method.parameterList.parameters.forEachIndexed { index, parameter ->
            val declaredName = parameter.name ?: return@forEachIndexed
            // 注入名来自 `@Param` 系列注解时，跳转定位到注解中书写名称的位置；
            // 没有注解（或注解未写值）时仍跳参数声明上的名字
            val resolvedName = paramNameResolver.resolveName(parameter)
            val effectiveName = resolvedName?.name ?: declaredName
            val type = parameter.type.presentableText
            val jumpTarget = resolvedName?.declaration
            val sourceRange = when {
                // 字符串字面量跳过起始引号，光标落在名称首字符前
                jumpTarget is PsiLiteralExpression -> PARAM_NAME_RANGE
                jumpTarget != null -> TextRange(0, 0)
                else -> parameter.nameIdentifier?.textRange
                    ?.shiftLeft(parameter.textRange.startOffset)
            }
            val names = listOf(
                effectiveName to "参数 ${effectiveName} 的最终值",
                "\$$effectiveName" to "参数 ${effectiveName} 的原始值",
                "\$$index" to "第 $index 个参数的最终值",
                "\$\$$index" to "第 $index 个参数的原始值",
                "$effectiveName::type" to "参数 ${effectiveName} 的类型",
                "\$$index::type" to "第 $index 个参数的类型",
                "$PARAM_CONTEXT_PREFIX$index" to "第 $index 个参数的参数上下文",
                "$PARAM_CONTEXT_PREFIX$effectiveName" to "参数 ${effectiveName} 的参数上下文"
            )
            for ((name, description) in names) {
                result += SpelSymbol(
                    name = name,
                    kind = SpelSymbolKind.ROOT_VARIABLE,
                    // `::type` 的值是参数的类型对象（ResolvableType），不是参数值类型；
                    // `$pc$n`/`$pc$name` 的值是 `ParameterContext` 本身
                    type = when {
                        name.endsWith(TYPE_SUFFIX) -> TYPE_INFO_TYPE
                        name.startsWith(PARAM_CONTEXT_PREFIX) -> PARAM_CONTEXT_TYPE
                        else -> type
                    },
                    origin = SpelSymbolOrigin.PARAMETER,
                    declaration = parameter,
                    lifecycle = SpelLifecycle.REQUEST_INIT,
                    sourceRange = sourceRange,
                    description = description,
                    jumpTarget = jumpTarget
                )
            }
        }
        return result
    }

    private fun classHierarchy(psiClass: PsiClass): List<PsiClass> {
        val result = ArrayList<PsiClass>()
        val visited = HashSet<String>()
        fun visit(current: PsiClass?) {
            if (current == null) {
                return
            }
            val key = current.qualifiedName ?: current.name ?: return
            if (!visited.add(key)) {
                return
            }
            result += current
            visit(current.superClass)
            current.interfaces.forEach { visit(it) }
        }
        visit(psiClass)
        return result
    }

    /**
     * 按「就近优先」合并：同名同类同命名空间只保留第一个。
     *
     * 函数按签名区分，保证同名重载全部保留（spec 要求重载补全全部列出）。
     */
    private fun merge(vararg layers: List<SpelSymbol>): List<SpelSymbol> {
        val seen = HashSet<SymbolKey>()
        val result = ArrayList<SpelSymbol>()
        for (layer in layers) {
            for (symbol in layer) {
                val signature = if (symbol.kind == SpelSymbolKind.FUNCTION) symbol.signature else null
                if (seen.add(SymbolKey(symbol.kind, symbol.namespace, symbol.name, signature))) {
                    result += symbol
                }
            }
        }
        return result
    }

    private fun applyVisibility(symbols: List<SpelSymbol>, stage: SpelLifecycle): List<SpelSymbol> {
        if (!LifecycleTable.isStageFilterable(stage)) {
            return symbols
        }
        return symbols.map { symbol ->
            if (LifecycleTable.isAvailable(stage, symbol.lifecycle)) {
                symbol
            } else {
                symbol.copy(visibility = SpelVisibility.STAGE_UNAVAILABLE)
            }
        }
    }

    private data class SymbolKey(
        val kind: SpelSymbolKind,
        val namespace: String?,
        val name: String,
        val signature: String?
    )

    companion object {
        /** 参数类型引用的后缀；`name::type`/`$n::type` 的值来自 `ParameterContext.getType()`。 */
        private const val TYPE_SUFFIX = "::type"
        private const val TYPE_INFO_TYPE = "ResolvableType"

        /** 注解字符串字面量中名称的相对范围：跳过起始引号。 */
        private val PARAM_NAME_RANGE = TextRange(1, 1)

        /** 参数上下文对象的前缀：`InternalRootVarName.$_PARAM_CONTEXT_$`。 */
        private const val PARAM_CONTEXT_PREFIX = "\$pc\$"
        private const val PARAM_CONTEXT_TYPE = "ParameterContext"

        /** 运行期类型随所在位置变化的内置变量。 */
        private const val THIS_VARIABLE = "\$this\$"
        private const val RESULT_VARIABLE = "\$result\$"
        private const val BODY_VARIABLE = "\$body\$"
        private const val JSON_BODY_VARIABLE = "\$jsonBody\$"
        private const val XML_BODY_VARIABLE = "\$xmlBody\$"

        /** 结果类型按默认 PackTypeParser 拆包的包装类型简单名。 */
        private val PACK_TYPE_NAMES = setOf(
            "Future",
            "SpelBean",
            "SimpleSpelBean",
            "FlatBean",
            "Optional",
            "Outcome"
        )

        private const val OPTIONAL = "java.util.Optional"
        private const val RESULT_HANDLER = "com.luckyframework.httpclient.proxy.handle.ResultHandler"
        private const val RESULT_HANDLER_META = "com.luckyframework.httpclient.proxy.annotations.ResultHandlerMeta"
        private const val CONVERT_META_TYPE = "com.luckyframework.httpclient.proxy.annotations.ConvertMetaType"
        private const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
        private const val JAVA_LANG_ANNOTATION = "java.lang.annotation.Annotation"
        private const val ALIAS_MAX_DEPTH = 16
        private const val DEFAULT_ATTRIBUTE = "value"
        private const val LUCKY_PACKAGE_PREFIX = "com.luckyframework."
    }
}
