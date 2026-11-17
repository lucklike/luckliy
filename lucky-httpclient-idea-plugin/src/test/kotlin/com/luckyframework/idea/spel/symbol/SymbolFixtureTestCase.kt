package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.metadata.SpelFrameworkStubs
import com.luckyframework.idea.spel.metadata.SpelLifecycle
import com.luckyframework.idea.spel.metadata.SpelMetaStubs

/**
 * 符号与上下文测试的共享夹具。
 *
 * 用源码桩模拟框架注解与工具类，测试不依赖真实 lucky-httpclient jar。
 */
abstract class SymbolFixtureTestCase : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        STUBS.forEach { (path, text) -> myFixture.addFileToProject(path, text) }
    }

    protected fun configure(text: String) {
        myFixture.configureByText("Api.java", text)
    }

    protected fun literalOf(value: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiLiteralExpression::class.java)
            .first { it.value == value }

    protected fun methodNamed(name: String): PsiMethod =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java).first { it.name == name }

    protected fun methodAnnotation(methodName: String, fqName: String): PsiAnnotation =
        methodNamed(methodName).modifierList!!.annotations.first { it.qualifiedName == fqName }

    protected fun newBuilder(
        builtin: List<SpelSymbol> = emptyList(),
        global: List<SpelSymbol> = emptyList()
    ): SpelContextBuilder = SpelContextBuilder(
        builtinSource = fixedSource(builtin),
        globalSource = fixedSource(global)
    )

    protected fun fixedSource(symbols: List<SpelSymbol>): SpelSymbolSource = object : SpelSymbolSource {
        override fun symbols(): List<SpelSymbol> = symbols
    }

    protected fun symbol(
        name: String,
        kind: SpelSymbolKind,
        origin: SpelSymbolOrigin = SpelSymbolOrigin.BUILTIN,
        type: String? = null
    ): SpelSymbol = SpelSymbol(
        name = name,
        kind = kind,
        type = type,
        origin = origin,
        lifecycle = SpelLifecycle.REQUEST_INIT
    )

    companion object {
        val STUBS: Map<String, String> = SpelMetaStubs.STUBS + SpelRuntimeStubs.STUBS +
            SpelFrameworkStubs.STUBS + mapOf(
            "com/luckyframework/reflect/Param.java" to """
package com.luckyframework.reflect;

public @interface Param {
    String value() default "";
    String def() default "null";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/DynamicParam.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.reflect.Param;
import org.springframework.core.annotation.AliasFor;

@Param
public @interface DynamicParam {

    @AliasFor(annotation = Param.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = Param.class, attribute = "value")
    String name() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/StandardObjectParam.java" to """
package com.luckyframework.httpclient.proxy.annotations;

@DynamicParam
public @interface StandardObjectParam {
}
""",
            "com/luckyframework/httpclient/proxy/annotations/Query.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

@StandardObjectParam
public @interface Query {

    @AliasFor(annotation = DynamicParam.class, attribute = "name")
    String value() default "";
}
""",
            "com/luckyframework/httpclient/proxy/spel/Namespace.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface Namespace {
    String value() default "";
}
""",
            "com/luckyframework/httpclient/proxy/spel/FunctionAlias.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface FunctionAlias {
    String value();
}
""",
            "com/luckyframework/httpclient/proxy/spel/FunctionFilter.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface FunctionFilter {
}
""",
            "com/luckyframework/reflect/Combination.java" to """
package com.luckyframework.reflect;

import java.lang.annotation.Annotation;

public @interface Combination {
    Class<? extends Annotation>[] value();
}
""",
            "com/luckyframework/httpclient/proxy/annotations/ObjectGenerate.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

public @interface ObjectGenerate {

    @AliasFor("clazz")
    Class<?> value() default Void.class;

    @AliasFor("value")
    Class<?> clazz() default Void.class;

    String msg() default "";
}
""",
            "com/luckyframework/httpclient/proxy/convert/ResultSelectionResponseConvert.java" to """
package com.luckyframework.httpclient.proxy.convert;

public class ResultSelectionResponseConvert {
}
""",
            "com/luckyframework/httpclient/proxy/annotations/ConvertMetaType.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface ConvertMetaType {
    Class<?> value() default Object.class;

    @SpelAttribute(result = {java.lang.reflect.Type.class, Class.class,
            org.springframework.core.ResolvableType.class, String.class})
    String type() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = {java.lang.reflect.Type.class, Class.class,
            org.springframework.core.ResolvableType.class, String.class})
    String typeFunc() default "";

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String respContentType() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String respContentTypeFunc() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/ResultConvertMeta.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

@ConvertMetaType
public @interface ResultConvertMeta {

    ObjectGenerate convert();

    @AliasFor(annotation = ConvertMetaType.class, attribute = "value")
    Class<?> metaType() default Object.class;

    @AliasFor(annotation = ConvertMetaType.class, attribute = "type")
    String metaTypeExpr() default "";

    @AliasFor(annotation = ConvertMetaType.class, attribute = "typeFunc")
    String metaTypeFunc() default "";

    @AliasFor(annotation = ConvertMetaType.class, attribute = "respContentType")
    String respContentType() default "";

    @AliasFor(annotation = ConvertMetaType.class, attribute = "respContentTypeFunc")
    String respContentTypeFunc() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/RespConvert.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.convert.ResultSelectionResponseConvert;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

@Combination(ResultConvertMeta.class)
@ResultConvertMeta(convert = @ObjectGenerate(ResultSelectionResponseConvert.class))
public @interface RespConvert {

    @AliasFor("result")
    String value() default "";

    @SpelAttribute(stage = Lifecycle.RESPONSE)
    String result() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.RESPONSE)
    String resultFunc() default "";

    @AliasFor(annotation = ResultConvertMeta.class, attribute = "metaType")
    Class<?> metaType() default Object.class;

    @AliasFor(annotation = ResultConvertMeta.class, attribute = "metaTypeExpr")
    String metaTypeExpr() default "";

    @AliasFor(annotation = ResultConvertMeta.class, attribute = "metaTypeFunc")
    String metaTypeFunc() default "";

    @AliasFor(annotation = ResultConvertMeta.class, attribute = "respContentType")
    String respContentType() default "";

    @AliasFor(annotation = ResultConvertMeta.class, attribute = "respContentTypeFunc")
    String respContentTypeFunc() default "";
}
""",
            "com/luckyframework/httpclient/proxy/handle/ResultHandler.java" to """
package com.luckyframework.httpclient.proxy.handle;

public interface ResultHandler<T> {
    void handleResult(Object result);
}
""",
            "com/luckyframework/httpclient/proxy/annotations/ResultHandlerMeta.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.handle.ResultHandler;

public @interface ResultHandlerMeta {
    Class<? extends ResultHandler> handlerClass() default ResultHandler.class;
    ObjectGenerate handler() default @ObjectGenerate(ResultHandler.class);
}
""",
            "com/luckyframework/httpclient/proxy/annotations/RetryMeta.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface RetryMeta {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.RETRY)
    String enable() default "true";

    @SpelAttribute(result = String.class, stage = Lifecycle.RETRY)
    String name() default "";

    @SpelAttribute(result = int.class, stage = Lifecycle.RETRY)
    String retryCount() default "0";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/Retryable.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

public @interface Retryable {

    @AliasFor(annotation = RetryMeta.class, attribute = "enable")
    String enable() default "true";

    String retryCount() default "3";

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.RETRY)
    String condition() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/Condition.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface Condition {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String assertion();

    @SpelAttribute
    String result() default "";

    @SpelAttribute(result = {Throwable.class, String.class})
    String exception() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/StaticQuery.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSeparator;

public @interface StaticQuery {

    @SpelAttribute(kind = SpelKind.KEY_VALUE, result = String.class)
    String value() default "";

    @SpelSeparator
    String separator() default "=";

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.REQUEST_INIT)
    String condition() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/AutoRedirect.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface AutoRedirect {

    @SpelAttribute(result = String.class, stage = Lifecycle.RESPONSE_INIT)
    String location() default "";

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.RESPONSE_INIT)
    String condition() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/PrintLog.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface PrintLog {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.RESPONSE)
    String respCondition() default "";

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.REQUEST)
    String reqCondition() default "";
}
""",
            "com/luckyframework/httpclient/proxy/spel/hook/Hook.java" to """
package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.httpclient.proxy.spel.meta.SpelStage;

public @interface Hook {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelStage
    Lifecycle lifecycle() default Lifecycle.NON;
}
""",
            "com/luckyframework/httpclient/proxy/annotations/ExceptionHandleMeta.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

public @interface ExceptionHandleMeta {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.THROWABLE)
    String condition() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = boolean.class, stage = Lifecycle.THROWABLE)
    String conditionFunc() default "";
}
""",
            "com/luckyframework/httpclient/proxy/handle/ExceptionFallback.java" to """
package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import org.springframework.core.annotation.AliasFor;

public @interface ExceptionFallback {

    @AliasFor(annotation = ExceptionHandleMeta.class, attribute = "condition")
    String condition() default "";
}
""",
            "com/example/MyUtils.java" to """
package com.example;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.Namespace;

@Namespace("crypto")
public class MyUtils {

    public static String md5(String text) {
        return text;
    }

    @FunctionAlias("sha")
    public static String sha256(String text) {
        return text;
    }

    @FunctionFilter
    public static String filtered(String text) {
        return text;
    }

    private static String hidden() {
        return "";
    }

    public String instanceMethod() {
        return "";
    }
}
"""
        )
    }
}
