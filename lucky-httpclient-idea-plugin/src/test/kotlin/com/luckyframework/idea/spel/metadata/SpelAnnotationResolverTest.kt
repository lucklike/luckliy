package com.luckyframework.idea.spel.metadata

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SpelAnnotationResolverTest : BasePlatformTestCase() {

    private val resolver = SpelAnnotationResolver()

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(ALIAS_FOR_PATH, ALIAS_FOR_STUB)
        myFixture.addFileToProject(HTTP_REQUEST_PATH, HTTP_REQUEST_STUB)
        myFixture.addFileToProject(GET_PATH, GET_STUB)
        myFixture.addFileToProject(POST_PATH, POST_STUB)
        myFixture.addFileToProject(SSE_LISTENER_PATH, SSE_LISTENER_STUB)
        myFixture.addFileToProject(SSE_PATH, SSE_STUB)
        myFixture.addFileToProject(MY_API_PATH, MY_API_STUB)
        myFixture.addFileToProject(PLAIN_PATH, PLAIN_STUB)
        myFixture.addFileToProject(CYCLE_A_PATH, CYCLE_A_STUB)
        myFixture.addFileToProject(CYCLE_B_PATH, CYCLE_B_STUB)
    }

    fun testGetValueResolvesToHttpRequestUrl() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("http://#{#host}/a"))!!

        assertEquals("com.luckyframework.httpclient.proxy.annotations.Get", resolved.declaredAnnotationFqName)
        assertEquals("value", resolved.declaredAttribute)
        assertEquals("com.luckyframework.httpclient.proxy.annotations.HttpRequest", resolved.effectiveAnnotationFqName)
        assertEquals("url", resolved.effectiveAttribute)
        assertEquals(SpelValueKind.TEMPLATE, resolved.kind)
        assertEquals(SpelLifecycle.REQUEST_INIT, resolved.lifecycle)
        assertTrue(resolved.isAlias)
    }

    fun testPostValueResolvesToHttpRequestUrl() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("http://x"))!!

        assertEquals("com.luckyframework.httpclient.proxy.annotations.Post", resolved.declaredAnnotationFqName)
        assertEquals("com.luckyframework.httpclient.proxy.annotations.HttpRequest", resolved.effectiveAnnotationFqName)
        assertEquals("url", resolved.effectiveAttribute)
    }

    fun testSseExpressionResolvesThroughMetaAnnotation() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("#{@userListener}"))!!

        assertEquals("com.luckyframework.httpclient.proxy.sse.Sse", resolved.declaredAnnotationFqName)
        assertEquals("com.luckyframework.httpclient.proxy.sse.SseListener", resolved.effectiveAnnotationFqName)
        assertEquals("expression", resolved.effectiveAttribute)
        assertEquals(SpelValueKind.TEMPLATE, resolved.kind)
    }

    fun testDirectAnnotationAttribute() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("http://y"))!!

        assertEquals("com.luckyframework.httpclient.proxy.annotations.HttpRequest", resolved.effectiveAnnotationFqName)
        assertEquals("value", resolved.effectiveAttribute)
        assertFalse(resolved.isAlias)
    }

    fun testCustomCompositeAnnotationResolvesSameNameMetaAttribute() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("http://z"))!!

        assertEquals("com.example.MyApi", resolved.declaredAnnotationFqName)
        assertEquals("com.luckyframework.httpclient.proxy.annotations.HttpRequest", resolved.effectiveAnnotationFqName)
        assertEquals("url", resolved.effectiveAttribute)
    }

    fun testUnregisteredAttributeDegradesToUnknown() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("x"))!!

        assertEquals(SpelValueKind.UNKNOWN, resolved.kind)
        assertEquals(SpelLifecycle.METHOD, resolved.lifecycle)
    }

    fun testPlainAnnotationIsNotAKnownContext() {
        configureFixture()

        assertNull(resolver.resolveLiteral(literalOf("plain")))
        assertNull(resolver.resolveLiteral(literalOf("#{outside}")))
    }

    fun testMetaAnnotationCycleTerminates() {
        configureFixture()

        assertNull(resolver.resolveLiteral(literalOf("a")))
    }

    fun testCollectReturnsWrittenAttributes() {
        configureFixture()

        val annotation = methodNamed("get").modifierList!!.annotations
            .first { it.qualifiedName == "com.luckyframework.httpclient.proxy.annotations.Get" }
        val collected = resolver.collect(annotation)

        assertEquals(1, collected.size)
        assertEquals(SpelValueKind.TEMPLATE, collected[0].kind)
        assertEquals("url", collected[0].effectiveAttribute)
    }

    fun testMethodLevelAnnotationsAreSortedOut() {
        configureFixture()

        val plain = annotationOf("plain", "com.example.Plain")
        assertFalse(resolver.isKnown(plain))
        assertTrue(resolver.isKnown(annotationOf("get", "com.luckyframework.httpclient.proxy.annotations.Get")))
    }

    private fun configureFixture() {
        myFixture.configureByText("Api.java", FIXTURE)
    }

    private fun literalOf(value: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiLiteralExpression::class.java)
            .first { it.value == value }

    private fun methodNamed(name: String): PsiMethod =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java).first { it.name == name }

    private fun annotationOf(methodName: String, fqName: String): PsiAnnotation =
        methodNamed(methodName).modifierList!!.annotations.first { it.qualifiedName == fqName }

    companion object {
        private const val ALIAS_FOR_PATH = "org/springframework/core/annotation/AliasFor.java"
        private const val HTTP_REQUEST_PATH = "com/luckyframework/httpclient/proxy/annotations/HttpRequest.java"
        private const val GET_PATH = "com/luckyframework/httpclient/proxy/annotations/Get.java"
        private const val POST_PATH = "com/luckyframework/httpclient/proxy/annotations/Post.java"
        private const val SSE_LISTENER_PATH = "com/luckyframework/httpclient/proxy/sse/SseListener.java"
        private const val SSE_PATH = "com/luckyframework/httpclient/proxy/sse/Sse.java"
        private const val MY_API_PATH = "com/example/MyApi.java"
        private const val PLAIN_PATH = "com/example/Plain.java"
        private const val CYCLE_A_PATH = "com/example/CycleA.java"
        private const val CYCLE_B_PATH = "com/example/CycleB.java"

        private const val ALIAS_FOR_STUB = """
package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AliasFor {
    String value() default "";
    Class<? extends Annotation> annotation() default Annotation.class;
    String attribute() default "";
    boolean isDefault() default false;
}
"""

        private const val HTTP_REQUEST_STUB = """
package com.luckyframework.httpclient.proxy.annotations;

public @interface HttpRequest {
    String value() default "";
    String url() default "";
    String func() default "";
}
"""

        private const val GET_STUB = """
package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

@HttpRequest
public @interface Get {
    @AliasFor(annotation = HttpRequest.class, attribute = "url")
    String value() default "";

    @AliasFor(annotation = HttpRequest.class, attribute = "func")
    String func() default "";
}
"""

        private const val POST_STUB = """
package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

@HttpRequest
public @interface Post {
    @AliasFor(annotation = HttpRequest.class, attribute = "url")
    String value() default "";
}
"""

        private const val SSE_LISTENER_STUB = """
package com.luckyframework.httpclient.proxy.sse;

public @interface SseListener {
    String expression() default "";
}
"""

        private const val SSE_STUB = """
package com.luckyframework.httpclient.proxy.sse;

import org.springframework.core.annotation.AliasFor;

@SseListener
public @interface Sse {
    @AliasFor(annotation = SseListener.class, attribute = "expression")
    String expression() default "";

    String async() default "";
}
"""

        private const val MY_API_STUB = """
package com.example;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

@HttpRequest
public @interface MyApi {
    String url() default "";
}
"""

        private const val PLAIN_STUB = """
package com.example;

public @interface Plain {
    String value() default "";
}
"""

        private const val CYCLE_A_STUB = """
package com.example;

@CycleB
public @interface CycleA {
    String value() default "";
}
"""

        private const val CYCLE_B_STUB = """
package com.example;

@CycleA
public @interface CycleB {
    String value() default "";
}
"""

        private const val FIXTURE = """
package p;

import com.example.CycleA;
import com.example.MyApi;
import com.example.Plain;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.sse.Sse;

class Api {

    @Get("http://#{#host}/a")
    void get(String host) {
        String outside = "#{outside}";
    }

    @Post(value = "http://x")
    void post() {
    }

    @HttpRequest(value = "http://y")
    void direct() {
    }

    @Sse(expression = "#{@userListener}")
    void sse() {
    }

    @Sse(async = "x")
    void sseAsync() {
    }

    @MyApi(url = "http://z")
    void custom() {
    }

    @Plain("plain")
    void plain() {
    }

    @CycleA("a")
    void cycle() {
    }
}
"""
    }
}
