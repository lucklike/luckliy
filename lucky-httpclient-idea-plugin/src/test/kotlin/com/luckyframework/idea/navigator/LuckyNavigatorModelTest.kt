package com.luckyframework.idea.navigator

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.luckyframework.idea.spel.highlight.SpelTextAttributes
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols

/**
 * [LuckyNavigatorModelBuilder] 的分组与条目规则：
 *
 * - `@StdHttpClient` 接口单独成组，组件灰字是类级 baseUrl；
 * - `@LuckyComponent` 按声明类型分组（含 DATABASE 数据库客户端），`AUTO` 按结构推导；
 * - 分组与组件之间按包名分层；
 * - 项目源码归属模块节点，模块按内容根目录嵌套，空模块不展示；
 * - 依赖 jar 中的组件先归入 External Libraries，再按 jar 分类；
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
        // 测试夹具中的源码属于项目内容根，必须归入模块节点而不是外部库
        assertEquals(LuckyProjectKind.MODULE, projectNode.kind)
        assertEquals(1, projectNode.componentCount)
        assertTrue(model.projects.none { it.kind == LuckyProjectKind.LIBRARY })
        val group = projectNode.groups.single { it.kind == LuckyGroupKind.STD_CLIENT }
        // 分组与组件之间插入包节点
        assertEquals(listOf("demo"), group.packages.map { it.name })
        val client = group.components.single()
        assertEquals("DemoClient", client.title)
        assertEquals("http://api.example.com", client.subtitle)
        // 组件行的 baseUrl 与 path 同样保留富文本片段
        assertEquals("http://api.example.com", client.baseUrl?.text)
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

        // 未声明 HTTP 注解的标准客户端方法仍会被代理，运行期默认 POST；
        // path 未声明时按地址栏习惯显示 /
        val plain = apis.single { it.name == "plain" }
        assertEquals("POST", plain.httpMethod)
        assertEquals("/", plain.subtitle)
        assertNull(plain.signature)
    }

    /** 组件 baseUrl 与 path 共用富文本：`${'$'}{...}` 保留键片段与折叠值。 */
    fun testComponentBaseUrlKeepsRichSegments() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        addClass(
            "RichBaseUrlClient",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

@HttpRequest("${'$'}{api.host}")
public interface RichBaseUrlClient {

    @Get("/users")
    String list();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val component = model.projects.flatMap { it.groups }.flatMap { it.components }
            .single { it.title == "RichBaseUrlClient" }
        assertEquals("\${api.host}", component.subtitle)
        val baseUrl = component.baseUrl ?: error("组件缺少 baseUrl")
        assertEquals("\${api.host}", baseUrl.text)
        val key = baseUrl.segments.single { it.text == "api.host" }
        assertEquals(SpelTextAttributes.SPRING_PLACEHOLDER, key.attributesKey)
        assertEquals("example.com", key.fold?.text)
    }

    /** legacy `@HttpClient` 接口既没有 `@LuckyComponent` 元注解，也没有请求方法注解。 */
    fun testLegacyHttpClientIsCollectedByRootAnnotation() {
        addClass(
            "LegacyClient",
            """
import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient(value = "http://legacy.example.com", path = "api/")
public interface LegacyClient {

    String legacy();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }.single { it.kind == LuckyGroupKind.API }
        val component = group.components.single { it.title == "LegacyClient" }
        // legacy `@HttpClient` 不在 `@HttpRequest` 元注解链上，baseUrl 仍需识别；
        // 同时声明的 value 与 path 按运行期规则拼接
        assertEquals("http://legacy.example.com/api/", component.subtitle)
        assertEquals("http://legacy.example.com/api/", component.baseUrl?.text)
    }

    /** `@StdHttpClient` 的 `path` 参与 baseUrl 拼接；没有注解 URL 与 YAML 时单独展示。 */
    fun testStdHttpClientPathContributesToBaseUrl() {
        addClass(
            "StdPathClient",
            """
import io.github.lucklike.httpclient.std.StdHttpClient;
import com.luckyframework.httpclient.proxy.annotations.Get;

@StdHttpClient(path = "v1/")
public interface StdPathClient {

    @Get("/users")
    String list();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val component = model.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.STD_CLIENT }
            .components
            .single { it.title == "StdPathClient" }
        // `@StdHttpClient` 的元注解链上含 `@HttpRequest`，path 不能被 HTTP 分支吞掉
        assertEquals("v1/", component.subtitle)
        assertEquals("v1/", component.baseUrl?.text)
    }

    /** `@HttpClient#pathFunc` 静态无法执行，按函数名拼接到 baseUrl。 */
    fun testLegacyHttpClientPathFuncContributesToBaseUrl() {
        addClass(
            "PathFuncClient",
            """
import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient(value = "http://legacy.example.com", pathFunc = "testPath")
public interface PathFuncClient {

    String legacy();

    static String testPath() {
        return "/aTest";
    }
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val component = model.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.API }
            .components
            .single { it.title == "PathFuncClient" }
        assertEquals("http://legacy.example.com/testPath", component.subtitle)
        assertEquals("http://legacy.example.com/testPath", component.baseUrl?.text)
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

    /** `@DBClient` 的 `@LuckyComponent(type = DATABASE)` 应归入 database 分组。 */
    fun testDbClientAnnotationUsesDatabaseGroup() {
        myFixture.addFileToProject(
            "io/github/lucklike/httpclient/dbclient/annotation/DBClient.java",
            """
package io.github.lucklike.httpclient.dbclient.annotation;

import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@LuckyComponent(type = LuckyComponentType.DATABASE)
public @interface DBClient {}
"""
        )
        addClass(
            "DbUserApi",
            """
import io.github.lucklike.httpclient.dbclient.annotation.DBClient;

@DBClient
public interface DbUserApi {

    String findById(String id);
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.DATABASE }
        val component = group.components.single { it.title == "DbUserApi" }
        assertEquals(listOf("findById"), component.items.map { it.name })
    }

    /** HTTP 方法未写 path 时显示 `/`；非 HTTP 方法显示完整签名，重载不再无法区分。 */
    fun testMethodRowsShowSlashAndFullSignature() {
        addClass(
            "SignatureApi",
            """
import io.github.lucklike.httpclient.discovery.HttpClient;
import com.luckyframework.httpclient.proxy.annotations.Get;

@HttpClient
public interface SignatureApi {

    @Get
    String ping();

    String find(String id);

    String find(String id, int version);

    void save(String value, int version);
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.API }
        val component = group.components.single { it.title == "SignatureApi" }
        val items = component.items.filterIsInstance<LuckyItemNode.Api>()

        val ping = items.single { it.name == "ping" }
        assertEquals("GET", ping.httpMethod)
        assertEquals("/", ping.subtitle)
        assertNull(ping.signature)
        // HTTP 方法固定排在非 HTTP 方法前面（组内保持声明顺序）
        assertEquals("ping", items.first().name)

        val findById = items.single { it.signature == "find(String): String" }
        assertNull(findById.httpMethod)
        assertNull(findById.subtitle)
        assertEquals("find(String): String", findById.signature)

        val findByIdWithVersion = items.single { it.signature == "find(String, int): String" }
        assertEquals("find(String, int): String", findByIdWithVersion.signature)

        val save = items.single { it.signature == "save(String, int): void" }
        assertEquals("save(String, int): void", save.signature)
    }

    /** 新元注解暂不可解析时，legacy `@HttpClient` 仍应通过根注解清单被收集。 */
    fun testLegacyClientCollectedWithoutLuckyComponentMetaAnnotation() {
        val metaAnnotationFile = myFixture.findFileInTempDir(
            "com/luckyframework/httpclient/proxy/spel/meta/LuckyComponent.java"
        )
        assertNotNull(metaAnnotationFile)
        WriteCommandAction.runWriteCommandAction(project) { metaAnnotationFile!!.delete(this) }
        assertNull(
            JavaPsiFacade.getInstance(project)
                .findClass(
                    "com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent",
                    GlobalSearchScope.allScope(project)
                )
        )
        SpelRuntimeSymbols.invalidate(project)

        addClass(
            "LegacyClientWithoutMeta",
            """
import io.github.lucklike.httpclient.discovery.HttpClient;

@HttpClient
public interface LegacyClientWithoutMeta {

    String call();
}
"""
        )
        val model = LuckyNavigatorModelBuilder().build(project)

        val group = model.projects.flatMap { it.groups }
            .single { it.kind == LuckyGroupKind.API }
        assertTrue(group.components.any { it.title == "LegacyClientWithoutMeta" })
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
        assertEquals("std", LuckyGroupKind.STD_CLIENT.title)
        assertEquals("httpclient", LuckyGroupKind.API.title)
        assertEquals("database", LuckyGroupKind.DATABASE.title)
        assertEquals("function", LuckyGroupKind.FUNCTION.title)
        assertEquals("config", LuckyGroupKind.CONFIG.title)
        assertEquals("support", LuckyGroupKind.SUPPORT.title)
        assertEquals("other", LuckyGroupKind.OTHER.title)
    }

    /** 固定分组顺序：std 在 httpclient 之前，database 紧随 httpclient。 */
    fun testGroupOrderPutsStdFirstAndDatabaseAfterHttp() {
        assertEquals(
            listOf(
                LuckyGroupKind.STD_CLIENT,
                LuckyGroupKind.API,
                LuckyGroupKind.DATABASE
            ),
            LuckyGroupKind.values().take(3).toList()
        )
    }

    fun testEmptyProjectReturnsEmptyModel() {
        val model = LuckyNavigatorModelBuilder().build(project)
        assertTrue(model.projects.isEmpty())
    }

    /** 模块按内容根目录嵌套：子模块归属路径最深的祖先模块。 */
    fun testModuleParentsFollowContentRootNesting() {
        assertEquals(
            mapOf(
                "auth-server" to "ldemo",
                "service" to "ldemo"
            ),
            moduleParentsOf(
                listOf(
                    LuckyModuleLocation("ldemo", "D:/lucklike/ldemo"),
                    LuckyModuleLocation("auth-server", "D:/lucklike/ldemo/auth-server"),
                    LuckyModuleLocation("service", "D:/lucklike/ldemo/service"),
                    LuckyModuleLocation("other", "D:/lucklike/other")
                )
            )
        )
    }

    /** 搜索命中子模块时保留父模块，清空搜索后层级不会丢失。 */
    fun testFilterKeepsParentModuleWithMatchingChild() {
        val psiClass = addClass("NestedApi", "public class NestedApi {}")
        val component = LuckyComponentNode(
            title = "NestedApi",
            qualifiedName = "demo.NestedApi",
            subtitle = null,
            psiClass = psiClass,
            items = emptyList()
        )
        val child = LuckyProjectNode(
            "auth-server",
            listOf(
                LuckyGroupNode(
                    LuckyGroupKind.API,
                    listOf(LuckyPackageNode("demo", listOf(component)))
                )
            ),
            LuckyProjectKind.MODULE
        )
        val parent = LuckyProjectNode(
            "ldemo",
            emptyList(),
            LuckyProjectKind.MODULE,
            listOf(child)
        )

        val filtered = LuckyNavigatorFilter.filter(
            LuckyNavigatorModel(listOf(parent)),
            "NestedApi"
        )

        val root = filtered.projects.single()
        assertEquals("ldemo", root.name)
        assertEquals("auth-server", root.children.single().name)
        assertEquals(1, root.componentCount)
    }

    /** 搜索 jar 名时保留 External Libraries 与 jar 层级。 */
    fun testFilterByLibraryNameKeepsJarLevel() {
        val psiClass = addClass("JarApi", "public class JarApi {}")
        val component = LuckyComponentNode(
            title = "JarApi",
            qualifiedName = "demo.JarApi",
            subtitle = null,
            psiClass = psiClass,
            items = emptyList()
        )
        val jar = LuckyProjectNode(
            "Maven: com.example:demo-lib:1.0",
            listOf(
                LuckyGroupNode(
                    LuckyGroupKind.API,
                    listOf(LuckyPackageNode("demo", listOf(component)))
                )
            ),
            LuckyProjectKind.JAR
        )
        val libraries = LuckyProjectNode(
            LuckyNavigatorModelBuilder.EXTERNAL_LIBRARIES,
            emptyList(),
            LuckyProjectKind.LIBRARY,
            listOf(jar)
        )

        val filtered = LuckyNavigatorFilter.filter(
            LuckyNavigatorModel(listOf(libraries)),
            "demo-lib"
        )

        val root = filtered.projects.single()
        assertEquals(LuckyProjectKind.LIBRARY, root.kind)
        assertEquals("Maven: com.example:demo-lib:1.0", root.children.single().name)
    }

    /** 包名默认展示；关闭后只隐藏包名副标题，baseUrl 等副标题仍保留。 */
    fun testPackageSubtitleVisibility() {
        val psiClass = addClass("PackageOnly", "public class PackageOnly {}")
        val component = LuckyComponentNode(
            title = "PackageOnly",
            qualifiedName = "demo.PackageOnly",
            subtitle = "demo",
            psiClass = psiClass,
            items = emptyList()
        )

        assertTrue(isPackageSubtitle(component))
        assertFalse(isPackageSubtitle(component.copy(subtitle = "http://api.example.com")))
        assertTrue(LuckyHttpClientSettings().isShowPackageNames)
    }
}
