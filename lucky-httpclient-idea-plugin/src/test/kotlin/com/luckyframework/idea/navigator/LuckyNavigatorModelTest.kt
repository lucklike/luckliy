package com.luckyframework.idea.navigator

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiMethod

/**
 * [LuckyNavigatorModelBuilder] 的分组与条目规则：
 *
 * - `@StdHttpClient` 接口单独成组，组件灰字是类级 baseUrl；
 * - `@LuckyComponent` 按声明类型分组，`AUTO` 按结构推导；
 * - 分组与组件之间按包名分层；
 * - 接口方法项带 HTTP 方法与路径，函数项带签名/返回类型；
 * - 标准客户端未配置请求方法时按运行期规则展示默认 POST；
 * - 所有条目都指向可导航的源码 PSI 方法。
 */
class LuckyNavigatorModelTest : NavigatorFixtureTestCase() {

    fun testStdClientGroupWithApiItems() {
        addClass(
            "DemoClient",
            """
import io.github.lucklike.httpclient.std.StdHttpClient;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Post;

@StdHttpClient
@HttpRequest("http://api.example.com")
public interface DemoClient {

    @Get("/users/{id}")
    String getUser(String id);

    @Post("/users")
    String createUser(String body);

    String plain();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val projectNode = model.projects.single { node ->
            node.groups.any { it.kind == LuckyGroupKind.STD_CLIENT }
        }
        val group = projectNode.groups.single { it.kind == LuckyGroupKind.STD_CLIENT }
        // 分组与组件之间插入包节点
        assertEquals(listOf("demo"), group.packages.map { it.name })
        val client = group.components.single()
        assertEquals("DemoClient", client.title)
        assertEquals("http://api.example.com", client.subtitle)
        assertEquals(3, client.items.size)

        val apis = client.items.filterIsInstance<LuckyItemNode.Api>()
        val get = apis.single { it.name == "getUser" }
        assertEquals("GET", get.httpMethod)
        assertEquals("/users/{id}", get.subtitle)
        assertTrue((get.element as NavigatablePsiElement).canNavigate())
        assertEquals("getUser", (get.element as PsiMethod).name)

        val post = apis.single { it.name == "createUser" }
        assertEquals("POST", post.httpMethod)
        assertEquals("/users", post.subtitle)

        // 未声明 HTTP 注解的标准客户端方法仍会被代理，运行期默认 POST，路径用占位符
        val plain = apis.single { it.name == "plain" }
        assertEquals("POST", plain.httpMethod)
        assertEquals("-", plain.subtitle)
    }

    /** legacy `@HttpClient` 接口既没有 `@LuckyComponent` 元注解，也没有请求方法注解。 */
    fun testLegacyHttpClientIsCollectedByRootAnnotation() {
        addClass(
            "LegacyClient",
            """
import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient("http://legacy.example.com")
public interface LegacyClient {

    String legacy();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }.single { it.kind == LuckyGroupKind.API }
        assertTrue(group.components.any { it.title == "LegacyClient" })
    }

    /** legacy `@SpELFunction` 类没有注册调用，只能按根注解清单收集。 */
    fun testLegacySpelFunctionIsCollectedByRootAnnotation() {
        addClass(
            "LegacyFunctions",
            """
import io.github.lucklike.httpclient.annotation.SpELFunction;

@SpELFunction
public final class LegacyFunctions {

    public static String hello(String name) {
        return "hello " + name;
    }
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }.single { it.kind == LuckyGroupKind.FUNCTION }
        val component = group.components.single { it.title == "LegacyFunctions" }
        assertEquals(listOf("hello"), component.items.map { it.name })
    }

    fun testFilterKeepsMatchedMethodsOnly() {
        val model = demoClientModel()

        val filtered = LuckyNavigatorFilter.filter(model, "getUser")
        val client = filtered.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.STD_CLIENT }
            .components
            .single()

        assertEquals(listOf("getUser"), client.items.map { it.name })
    }

    fun testFilterByComponentNameKeepsAllMethods() {
        val filtered = LuckyNavigatorFilter.filter(demoClientModel(), "democlient")
        val client = filtered.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.STD_CLIENT }
            .components
            .single()

        assertEquals(3, client.items.size)
    }

    fun testFilterByHttpMethodMatchesApis() {
        val filtered = LuckyNavigatorFilter.filter(demoClientModel(), "post")
        val names = filtered.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.STD_CLIENT }
            .components
            .single()
            .items
            .map { it.name }

        assertEquals(setOf("createUser", "plain"), names.toSet())
    }

    fun testFilterWithoutMatchReturnsEmptyModel() {
        assertTrue(LuckyNavigatorFilter.filter(demoClientModel(), "no-such-api").projects.isEmpty())
    }

    private fun demoClientModel(): LuckyNavigatorModel {
        addClass(
            "DemoClient",
            """
import io.github.lucklike.httpclient.std.StdHttpClient;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;

@StdHttpClient
public interface DemoClient {

    @Get("/users/{id}")
    String getUser(String id);

    @Post("/users")
    String createUser(String body);

    String plain();
}
"""
        )
        return LuckyNavigatorModelBuilder().build(project)
    }

    fun testApiGroupIsNotShownWhenStdClientWins() {
        // @Get 的元注解链会在真实框架中把接口标记为 API 组件，
        // 同一接口必须只出现在 StdHttpClient 分组里
        addClass(
            "DuplicatedClient",
            """
import io.github.lucklike.httpclient.std.StdHttpClient;
import com.luckyframework.httpclient.proxy.annotations.Get;

@StdHttpClient
public interface DuplicatedClient {

    @Get("/dup")
    String dup();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val components = model.projects
            .flatMap { it.groups }
            .filter { it.kind == LuckyGroupKind.STD_CLIENT || it.kind == LuckyGroupKind.API }
            .flatMap { it.components }
        assertEquals(1, components.count { it.title == "DuplicatedClient" })
        assertTrue(
            model.projects.flatMap { it.groups }
                .single { it.kind == LuckyGroupKind.STD_CLIENT }
                .components
                .any { it.title == "DuplicatedClient" }
        )
    }

    fun testFunctionComponentUsesPublicStaticMethods() {
        addClass(
            "MathFunctions",
            """
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;

@LuckyComponent(type = LuckyComponentType.FUNCTION)
public final class MathFunctions {

    public static int add(int a, int b) {
        return a + b;
    }

    public static String greet(String name) {
        return "hi " + name;
    }

    private static int hidden() {
        return 0;
    }
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.FUNCTION }
        val component = group.components.single { it.title == "MathFunctions" }
        // 包名作为非 HTTP 组件的灰色副标题
        assertEquals("demo", component.subtitle)
        assertEquals(listOf("add", "greet"), component.items.map { it.name })

        val add = component.items.first() as LuckyItemNode.Function
        assertEquals("int", add.subtitle)
        assertTrue((add.element as NavigatablePsiElement).canNavigate())
        assertEquals("add", (add.element as PsiMethod).name)
    }

    fun testAutoComponentInfersApiGroup() {
        addClass(
            "AutoApi",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

@LuckyComponent
public interface AutoApi {

    @Get("/auto")
    String fetch();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.API }
        val component = group.components.single { it.title == "AutoApi" }
        val item = component.items.single() as LuckyItemNode.Api
        assertEquals("GET", item.httpMethod)
        assertEquals("/auto", item.subtitle)
    }

    /** 分组标题使用组件类型枚举名，去掉中文描述。 */
    fun testGroupTitlesUseKindNames() {
        assertEquals("STD HTTP", LuckyGroupKind.STD_CLIENT.title)
        assertEquals("HTTP", LuckyGroupKind.API.title)
        assertEquals("FUNCTION", LuckyGroupKind.FUNCTION.title)
        assertEquals("CONFIG", LuckyGroupKind.CONFIG.title)
        assertEquals("SUPPORT", LuckyGroupKind.SUPPORT.title)
        assertEquals("OTHER", LuckyGroupKind.OTHER.title)
    }

    /** 固定分组顺序：HTTP 在 STD HTTP 之前。 */
    fun testGroupOrderPutsHttpBeforeStdHttp() {
        assertEquals(
            listOf(LuckyGroupKind.API, LuckyGroupKind.STD_CLIENT),
            LuckyGroupKind.values().take(2).toList()
        )
    }

    fun testEmptyProjectReturnsEmptyModel() {
        val model = LuckyNavigatorModelBuilder().build(project)
        assertTrue(model.projects.isEmpty())
    }
}
