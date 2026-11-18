package com.luckyframework.idea.navigator

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiMethod

/**
 * [LuckyNavigatorModelBuilder] 的分组与条目规则：
 *
 * - `@StdHttpClient` 接口单独成组，组件灰字是类级 baseUrl；
 * - `@LuckyComponent` 按声明类型分组，`AUTO` 按结构推导；
 * - 接口方法项带 HTTP 方法与路径，函数项带签名/返回类型；
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

        // 未声明 HTTP 注解的接口方法仍然展示，路径用占位符
        val plain = apis.single { it.name == "plain" }
        assertNull(plain.httpMethod)
        assertEquals("-", plain.subtitle)
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

    fun testEmptyProjectReturnsEmptyModel() {
        val model = LuckyNavigatorModelBuilder().build(project)
        assertTrue(model.projects.isEmpty())
    }
}
