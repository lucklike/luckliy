package com.luckyframework.idea.navigator

/**
 * [LuckyHttpAnnotations] 的解析规则：
 *
 * - `@Get`/`@Post` 沿元注解链映射为请求方法；
 * - `@HttpRequest` 的 `method` 默认 `NON` 视为未指定；
 * - URL 取使用处显式书写的 `url`/`value`，支持编译期常量；
 * - 非 HTTP 注解的 `value` 不会被误读。
 */
class LuckyHttpAnnotationsTest : NavigatorFixtureTestCase() {

    fun testCombinationAnnotationMapsMethodAndUrl() {
        val psiClass = addClass(
            "GetClient",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;

public interface GetClient {

    @Get("/users/{id}")
    String getUser(String id);
}
"""
        )
        val method = methodOf(psiClass, "getUser")
        assertEquals("GET", LuckyHttpAnnotations.httpMethodOf(method))
        assertEquals("/users/{id}", LuckyHttpAnnotations.urlOf(method))
    }

    fun testPostAnnotation() {
        val psiClass = addClass(
            "PostClient",
            """
import com.luckyframework.httpclient.proxy.annotations.Post;

public interface PostClient {

    @Post("/users")
    String createUser(String body);
}
"""
        )
        val method = methodOf(psiClass, "createUser")
        assertEquals("POST", LuckyHttpAnnotations.httpMethodOf(method))
        assertEquals("/users", LuckyHttpAnnotations.urlOf(method))
    }

    fun testExplicitHttpRequestAttributes() {
        val psiClass = addClass(
            "RawClient",
            """
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.RequestMethod;

public interface RawClient {

    @HttpRequest(method = RequestMethod.PUT, url = "/items/#{id}")
    String update(String id);
}
"""
        )
        val method = methodOf(psiClass, "update")
        assertEquals("PUT", LuckyHttpAnnotations.httpMethodOf(method))
        assertEquals("/items/#{id}", LuckyHttpAnnotations.urlOf(method))
    }

    fun testDefaultRequestMethodIsTreatedAsUnset() {
        val psiClass = addClass(
            "DefaultClient",
            """
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

@HttpRequest("http://api.example.com")
public interface DefaultClient {

    @HttpRequest
    String call();
}
"""
        )
        assertNull(LuckyHttpAnnotations.httpMethodOf(methodOf(psiClass, "call")))
        // 类级 value 通过 @AliasFor 映射为 url
        assertEquals("http://api.example.com", LuckyHttpAnnotations.urlOf(psiClass))
    }

    fun testConstantUrlIsResolved() {
        val psiClass = addClass(
            "ConstantClient",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;

public interface ConstantClient {

    String PATH = "/constants/list";

    @Get(PATH)
    String list();
}
"""
        )
        val method = methodOf(psiClass, "list")
        assertEquals("GET", LuckyHttpAnnotations.httpMethodOf(method))
        assertEquals("/constants/list", LuckyHttpAnnotations.urlOf(method))
    }

    fun testNonHttpAnnotationValueIsIgnored() {
        val psiClass = addClass(
            "SuppressedClient",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;

public interface SuppressedClient {

    @SuppressWarnings("unused")
    @Get("/kept")
    String fetch();
}
"""
        )
        val method = methodOf(psiClass, "fetch")
        assertEquals("GET", LuckyHttpAnnotations.httpMethodOf(method))
        // @SuppressWarnings("unused") 的 value 不是 URL，不能被当成路径
        assertEquals("/kept", LuckyHttpAnnotations.urlOf(method))
    }

    fun testUnknownAnnotationWithoutHttpMeta() {
        val annotationType = addClass(
            "MyApi",
            """
public @interface MyApi {
    String value();
}
"""
        )
        assertNull(LuckyHttpAnnotations.httpMethodOf(annotationType))
        assertNull(LuckyHttpAnnotations.urlOf(annotationType))
    }
}
