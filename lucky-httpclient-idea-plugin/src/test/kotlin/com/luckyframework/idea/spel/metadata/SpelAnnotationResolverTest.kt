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
        SpelMetaStubs.STUBS.forEach { (path, text) -> myFixture.addFileToProject(path, text) }
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
        myFixture.addFileToProject(CONVERT_META_TYPE_PATH, CONVERT_META_TYPE_STUB)
        myFixture.addFileToProject(COMBINATION_PATH, COMBINATION_STUB)
        myFixture.addFileToProject(OBJECT_GENERATE_PATH, OBJECT_GENERATE_STUB)
        myFixture.addFileToProject(SELECT_CONVERT_PATH, SELECT_CONVERT_STUB)
        myFixture.addFileToProject(RESULT_CONVERT_META_PATH, RESULT_CONVERT_META_STUB)
        myFixture.addFileToProject(RESP_CONVERT_PATH, RESP_CONVERT_STUB)
        myFixture.addFileToProject(MY_CONVERT_PATH, MY_CONVERT_STUB)
    }

    fun testGetValueResolvesToHttpRequestUrl() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("http://#{#host}/a"))!!

        assertEquals("com.luckyframework.httpclient.proxy.annotations.Get", resolved.declaredAnnotationFqName)
        assertEquals("value", resolved.declaredAttribute)
        assertEquals("com.luckyframework.httpclient.proxy.annotations.HttpRequest", resolved.effectiveAnnotationFqName)
        assertEquals("url", resolved.effectiveAttribute)
        assertEquals(SpelValueKind.TEMPLATE, resolved.kind)
        assertEquals(SpelLifecycle.METHOD, resolved.lifecycle)
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
        assertEquals("url", resolved.effectiveAttribute)
        assertEquals("value", resolved.declaredAttribute)
        assertTrue(resolved.isAlias)
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

    /** `@RespConvert(value = ...)` 的 `value` 是 `result` 的注解内别名。 */
    fun testAnnotationInternalAliasResolvesWithinSameAnnotation() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("#{resp}"))!!

        assertEquals(RESP_CONVERT, resolved.declaredAnnotationFqName)
        assertEquals("value", resolved.declaredAttribute)
        assertEquals(RESP_CONVERT, resolved.effectiveAnnotationFqName)
        assertEquals("result", resolved.effectiveAttribute)
        assertEquals(SpelValueKind.TEMPLATE, resolved.kind)
        assertEquals(SpelLifecycle.RESPONSE, resolved.lifecycle)
        assertTrue(resolved.isAlias)
    }

    /** `@RespConvert#respContentTypeFunc` 的别名链最终落到 `@ConvertMetaType` 上声明的元注解。 */
    fun testAliasChainCrossesMetaAnnotation() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("getContentType"))!!

        assertEquals(RESP_CONVERT, resolved.declaredAnnotationFqName)
        assertEquals("respContentTypeFunc", resolved.declaredAttribute)
        assertEquals(CONVERT_META_TYPE, resolved.effectiveAnnotationFqName)
        assertEquals("respContentTypeFunc", resolved.effectiveAttribute)
        assertEquals(SpelValueKind.FUNCTION, resolved.kind)
        assertEquals(SpelLifecycle.METHOD, resolved.lifecycle)
    }

    /** 组合注解自身的属性没有 `@SpelAttribute` 时，不因为出现在注解上而获得 SpEL 语义。 */
    fun testCompositeAttributeWithoutSpelAttributeDegradesToUnknown() {
        configureFixture()

        val annotation = annotationOf("respMeta", RESP_CONVERT)
        val resolved = resolver.resolve(annotation, "metaType")!!

        assertEquals(RESP_CONVERT, resolved.effectiveAnnotationFqName)
        assertEquals("metaType", resolved.effectiveAttribute)
        assertEquals(SpelValueKind.UNKNOWN, resolved.kind)
    }

    /**
     * 用户自定义注解不登记在元数据表中，通过元注解同名属性接入框架语义：
     * `@MyConvert#metaTypeExpr` → `@ResultConvertMeta#metaTypeExpr` → `@ConvertMetaType#type`。
     */
    fun testCustomCompositeAnnotationResolvesMetaAttribute() {
        configureFixture()

        val resolved = resolver.resolveLiteral(literalOf("#{meta}"))!!

        assertEquals(MY_CONVERT, resolved.declaredAnnotationFqName)
        assertEquals("metaTypeExpr", resolved.declaredAttribute)
        assertEquals(CONVERT_META_TYPE, resolved.effectiveAnnotationFqName)
        assertEquals("type", resolved.effectiveAttribute)
        assertEquals(SpelValueKind.TEMPLATE, resolved.kind)
        assertEquals(SpelLifecycle.METHOD, resolved.lifecycle)
    }

    /**
     * 用户自定义注解未登记在任何离线清单中：插件在运行期直接读取注解声明上的
     * `@SpelAttribute` / `@SpelStage` / `@SpelSeparator` 得到同样的语义。
     */
    fun testCustomAnnotationIsReadAtRuntime() {
        myFixture.addFileToProject(PAYLOAD_PATH, PAYLOAD_STUB)
        myFixture.addFileToProject(MY_TEMPLATE_PATH, MY_TEMPLATE_STUB)
        myFixture.configureByText("CustomApi.java", CUSTOM_FIXTURE)

        val annotation = methodNamed("call").modifierList!!.annotations
            .first { it.qualifiedName == MY_TEMPLATE }

        val value = resolver.resolve(annotation, "value")!!
        assertEquals(SpelValueKind.TEMPLATE, value.kind)
        assertEquals(listOf("com.example.Payload"), value.metadata.result)
        // 未显式声明 stage，读取 @SpelStage 元素在当前使用处的值
        assertEquals(SpelLifecycle.REQUEST, value.lifecycle)

        val headers = resolver.resolve(annotation, "headers")!!
        assertEquals(SpelValueKind.KEY_VALUE, headers.kind)
        // @SpelAttribute 的显式 separator 优先于 @SpelSeparator 元素
        assertEquals("=", headers.separator)

        // @SpelSeparator 元素本身不参与 SpEL：注解已识别，但该属性降级为 UNKNOWN
        val separator = resolver.resolve(annotation, "separator")!!
        assertEquals(SpelValueKind.UNKNOWN, separator.kind)
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
        private const val CONVERT_META_TYPE_PATH =
            "com/luckyframework/httpclient/proxy/annotations/ConvertMetaType.java"
        private const val COMBINATION_PATH = "com/luckyframework/reflect/Combination.java"
        private const val OBJECT_GENERATE_PATH =
            "com/luckyframework/httpclient/proxy/annotations/ObjectGenerate.java"
        private const val SELECT_CONVERT_PATH =
            "com/luckyframework/httpclient/proxy/convert/ResultSelectionResponseConvert.java"
        private const val RESULT_CONVERT_META_PATH =
            "com/luckyframework/httpclient/proxy/annotations/ResultConvertMeta.java"
        private const val RESP_CONVERT_PATH =
            "com/luckyframework/httpclient/proxy/annotations/RespConvert.java"
        private const val MY_CONVERT_PATH = "com/example/MyConvert.java"

        private const val RESP_CONVERT = "com.luckyframework.httpclient.proxy.annotations.RespConvert"
        private const val MY_CONVERT = "com.example.MyConvert"
        private const val RESULT_CONVERT_META =
            "com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta"

        private const val MY_TEMPLATE_PATH = "com/example/MyTemplate.java"
        private const val PAYLOAD_PATH = "com/example/Payload.java"
        private const val MY_TEMPLATE = "com.example.MyTemplate"

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

        private const val CONVERT_META_TYPE = "com.luckyframework.httpclient.proxy.annotations.ConvertMetaType"

        private const val HTTP_REQUEST_STUB = """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

public @interface HttpRequest {

    @AliasFor("url")
    String value() default "";

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String url() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
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

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;

public @interface SseListener {

    @SpelAttribute
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

        private const val CONVERT_META_TYPE_STUB = """
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
"""

        private const val COMBINATION_STUB = """
package com.luckyframework.reflect;

import java.lang.annotation.Annotation;

public @interface Combination {
    Class<? extends Annotation>[] value();
}
"""

        private const val OBJECT_GENERATE_STUB = """
package com.luckyframework.httpclient.proxy.annotations;

public @interface ObjectGenerate {
    Class<?> value() default Void.class;
    Class<?> clazz() default Void.class;
    String msg() default "";
}
"""

        private const val SELECT_CONVERT_STUB = """
package com.luckyframework.httpclient.proxy.convert;

public class ResultSelectionResponseConvert {
}
"""

        private const val RESULT_CONVERT_META_STUB = """
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
"""

        private const val RESP_CONVERT_STUB = """
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
"""

        private const val MY_CONVERT_STUB = """
package com.example;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;

@ResultConvertMeta(convert = @ObjectGenerate)
public @interface MyConvert {
    String metaTypeExpr() default "";
}
"""

        private const val PAYLOAD_STUB = """
package com.example;

public class Payload {
}
"""

        private const val MY_TEMPLATE_STUB = """
package com.example;

import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSeparator;
import com.luckyframework.httpclient.proxy.spel.meta.SpelStage;

public @interface MyTemplate {

    @SpelAttribute(kind = SpelKind.TEMPLATE, result = Payload.class)
    String value() default "";

    @SpelAttribute(kind = SpelKind.KEY_VALUE, separator = "=")
    String headers() default "";

    @SpelSeparator
    String separator() default "&";

    @SpelStage
    String lifecycle() default "METHOD";
}
"""

        private const val CUSTOM_FIXTURE = """
package p;

import com.example.MyTemplate;

class CustomApi {

    @MyTemplate(value = "user", headers = "a=1", separator = "&", lifecycle = "REQUEST")
    void call() {
    }
}
"""

        private const val FIXTURE = """
package p;

import com.example.CycleA;
import com.example.MyApi;
import com.example.MyConvert;
import com.example.Plain;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
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

    @MyConvert(metaTypeExpr = "#{meta}")
    void customMeta() {
    }

    @Plain("plain")
    void plain() {
    }

    @CycleA("a")
    void cycle() {
    }

    @RespConvert(value = "#{resp}")
    void respValue() {
    }

    @RespConvert(respContentTypeFunc = "getContentType")
    void respFunc() {
    }

    @RespConvert(metaType = String.class)
    void respMeta() {
    }
}
"""
    }
}
