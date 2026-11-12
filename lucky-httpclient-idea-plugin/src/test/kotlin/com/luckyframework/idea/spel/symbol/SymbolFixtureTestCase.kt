package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.metadata.SpelLifecycle

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
        val STUBS: Map<String, String> = mapOf(
            "com/luckyframework/reflect/Param.java" to """
package com.luckyframework.reflect;

public @interface Param {
    String value() default "";
    String def() default "null";
}
""",
            "org/springframework/core/annotation/AliasFor.java" to """
package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

public @interface AliasFor {
    String value() default "";
    Class<? extends Annotation> annotation() default Annotation.class;
    String attribute() default "";
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
            "com/luckyframework/httpclient/proxy/spel/SpELImport.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface SpELImport {
    Class<?>[] value() default {};
    If[] ifs() default {};
    String[] root() default {};
    String[] var() default {};
    String[] rootLit() default {};
    String[] varLit() default {};
    String[] pack() default {};
    String separator() default "=";
}
""",
            "com/luckyframework/httpclient/proxy/spel/SpELImports.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface SpELImports {
    SpELImport[] value();
}
""",
            "com/luckyframework/httpclient/proxy/spel/If.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface If {
    String condition();
    Class<?>[] classes();
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
            "com/luckyframework/httpclient/proxy/annotations/HttpRequest.java" to """
package com.luckyframework.httpclient.proxy.annotations;

public @interface HttpRequest {
    String value() default "";
    String url() default "";
    String func() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/Retryable.java" to """
package com.luckyframework.httpclient.proxy.annotations;

public @interface Retryable {
    String enable() default "true";
    String retryCount() default "3";
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
