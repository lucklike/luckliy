package com.luckyframework.idea.navigator

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.metadata.SpelMetaStubs
import com.luckyframework.idea.std.StdHttpClientStubs

/**
 * 导航栏测试基类：提供组件、HTTP 注解与标准客户端的完整源码桩。
 *
 * 与 `SpelFrameworkStubs` 不同，这里的 `HttpRequest` 必须包含 `method`
 * 属性，否则 `@Get`/`@Post` 组合注解无法沿元注解链解析请求方法；
 * 组件与标准客户端的桩直接复用现有测试夹具，保证与生产扫描同源。
 */
abstract class NavigatorFixtureTestCase : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        STUBS.forEach { (path, text) -> myFixture.addFileToProject(path, text) }
    }

    /** 添加 `demo` 包下的 Java 源文件并解析出类。 */
    protected fun addClass(name: String, body: String): PsiClass {
        myFixture.addFileToProject("demo/$name.java", "package demo;\n\n$body")
        return findClass(name)
    }

    protected fun findClass(name: String): PsiClass =
        JavaPsiFacade.getInstance(project)
            .findClass("demo.$name", GlobalSearchScope.projectScope(project))
            ?: error("测试类 demo.$name 未解析")

    protected fun methodOf(psiClass: PsiClass, name: String): PsiMethod =
        psiClass.findMethodsByName(name, false).firstOrNull()
            ?: error("方法 $name 不存在")

    private companion object {

        private val STUBS: Map<String, String> = SpelMetaStubs.STUBS + StdHttpClientStubs.STUBS + mapOf(
            "org/springframework/core/annotation/AliasFor.java" to """
package org.springframework.core.annotation;

public @interface AliasFor {
    String value() default "";
    Class<?> annotation() default Annotation.class;
    String attribute() default "";
}
""",
            // legacy 组件注解：3.0.3 的 jar 中没有 @LuckyComponent 元注解，
            // 侧边栏依赖根注解清单收集它们的实现类
            "io/github/lucklike/httpclient/discovery/HttpClient.java" to """
package io.github.lucklike.httpclient.discovery;

public @interface HttpClient {
    String value() default "";
    String url() default "";
    String urlFunc() default "";
    String path() default "";
    String pathFunc() default "";
}
""",
            "io/github/lucklike/httpclient/annotation/SpELFunction.java" to """
package io.github.lucklike.httpclient.annotation;

public @interface SpELFunction {
    String value() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/HttpRequest.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.annotation.AliasFor;

// 与真实框架一致：HttpRequest 本身就是 API 类型的组件注解，
// url/func 标注 @SpelAttribute，导航栏据此建立 SpEL 引用与文档；
// group/priority 与源码一致，导航栏的分组名称和顺序都读取它们
@LuckyComponent(type = LuckyComponentType.API, group = "httpclient", priority = 1)
public @interface HttpRequest {

    @AliasFor("url")
    String value() default "";

    @AliasFor("value")
    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String url() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String func() default "";

    RequestMethod method() default RequestMethod.NON;
}
""",
            "com/luckyframework/httpclient/proxy/annotations/RequestMethod.java" to """
package com.luckyframework.httpclient.proxy.annotations;

public enum RequestMethod {
    NON, GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT, PATCH
}
""",
            "com/luckyframework/httpclient/proxy/annotations/Get.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

@HttpRequest(method = RequestMethod.GET)
public @interface Get {

    @AliasFor(annotation = HttpRequest.class, attribute = "url")
    String value() default "";

    @AliasFor(annotation = HttpRequest.class, attribute = "func")
    String func() default "";
}
""",
            "com/luckyframework/httpclient/proxy/annotations/Post.java" to """
package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

@HttpRequest(method = RequestMethod.POST)
public @interface Post {

    @AliasFor(annotation = HttpRequest.class, attribute = "url")
    String value() default "";

    @AliasFor(annotation = HttpRequest.class, attribute = "func")
    String func() default "";
}
""",
            // Spring starter 标记：${...} 配置解析与折叠依赖它激活
            "io/github/lucklike/httpclient/SpringSpELConvert.java" to """
package io.github.lucklike.httpclient;

public class SpringSpELConvert {
}
"""
        )
    }
}
