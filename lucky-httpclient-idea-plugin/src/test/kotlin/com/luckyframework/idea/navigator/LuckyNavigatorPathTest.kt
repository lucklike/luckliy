package com.luckyframework.idea.navigator

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.luckyframework.idea.spel.highlight.SpelTextAttributes

/**
 * 侧边栏 path 富文本：
 *
 * - `#{...}` 按 SpEL 词法着色，悬停文档与点击跳转沿编辑器同一条解析链路；
 * - `${...}` 的键、默认值与定界符分别着色，配置值可解析时生成折叠组；
 * - `func` 直接展示函数名，函数名整段着色并可跳转到函数声明；
 * - URL 与 `func` 同时存在时只展示 URL（运行期优先）；
 * - `${...}` 的样式由 colorSettings 的 additionalTextAttributes 注册。
 */
class LuckyNavigatorPathTest : NavigatorFixtureTestCase() {

    fun testSpelPathCarriesHighlightDocumentationAndNavigation() {
        val psiClass = addClass(
            "SpelApi",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;

public interface SpelApi {

    @Get("/users/#{userId}")
    String getUser(String userId);
}
"""
        )
        val path = pathOf(psiClass, "getUser")

        assertEquals("/users/#{userId}", path.text)
        assertSegmentsMatchText(path)
        assertEquals(SpelTextAttributes.DELIMITER, segmentOf(path, "#{").attributesKey)

        val variable = segmentOf(path, "userId")
        assertEquals(SpelTextAttributes.ROOT_VARIABLE, variable.attributesKey)
        assertNotNull("SpEL 变量缺少悬停文档", variable.documentation)
        assertEquals("userId", (variable.navigation as? PsiParameter)?.name)

        assertEquals(SpelTextAttributes.DELIMITER, path.segments.last().attributesKey)
    }

    fun testConfiguredPlaceholderFoldsValue() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        val psiClass = addClass(
            "PlaceholderApi",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;

public interface PlaceholderApi {

    @Get("http://${'$'}{api.host}/users")
    String list();
}
"""
        )
        val path = pathOf(psiClass, "list")

        // path 文本保留原表达式，配置值只作为折叠预览
        assertEquals("http://\${api.host}/users", path.text)
        assertSegmentsMatchText(path)

        val key = segmentOf(path, "api.host")
        assertEquals(SpelTextAttributes.SPRING_PLACEHOLDER, key.attributesKey)
        assertEquals("example.com", key.fold?.text)
        assertTrue(
            "配置键的悬停文档应包含解析值: ${key.documentation}",
            key.documentation?.contains("example.com") == true
        )
        assertNotNull("配置键缺少跳转目标", key.navigation)

        assertEquals(SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER, segmentOf(path, "\${").attributesKey)
        assertEquals(SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER, segmentOf(path, "}").attributesKey)
    }

    fun testFuncIsShownWithNameAndNavigates() {
        addClass(
            "UrlFunctions",
            """
import io.github.lucklike.httpclient.annotation.SpELFunction;

@SpELFunction
public final class UrlFunctions {

    public static String myUrl() {
        return "/dynamic";
    }
}
"""
        )
        // 函数符号沿运行期注册链路：工厂初始化时调用 addSpringElFunctionClass
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/HttpClientProxyObjectFactory.java",
            """
package com.luckyframework.httpclient.proxy;

import demo.UrlFunctions;

public class HttpClientProxyObjectFactory {

    public void addSpringElFunctionClass(Class<?> clazz) {
    }

    public void init() {
        addSpringElFunctionClass(UrlFunctions.class);
    }
}
"""
        )
        val psiClass = addClass(
            "FuncApi",
            """
import io.github.lucklike.httpclient.discovery.HttpClient;
import com.luckyframework.httpclient.proxy.annotations.Get;

@HttpClient
public interface FuncApi {

    @Get(func = "myUrl")
    String fetch();
}
"""
        )
        val path = pathOf(psiClass, "fetch")

        assertEquals("myUrl", path.text)
        assertSegmentsMatchText(path)

        val function = path.segments.single()
        assertEquals("myUrl", function.text)
        assertEquals(SpelTextAttributes.FUNCTION, function.attributesKey)
        assertNotNull("函数名缺少悬停文档", function.documentation)
        assertEquals("myUrl", (function.navigation as? PsiMethod)?.name)

        // 侧边栏行内文字与 path 文本保持一致
        val api = apiItemOf("FuncApi", "fetch")
        assertEquals("myUrl", api.subtitle)
        assertEquals(path.text, api.path?.text)
    }

    fun testFuncPlaceholderFoldsConfigurationValue() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "dynamic.url=/from-config\n"
        )
        val psiClass = addClass(
            "PlaceholderFuncApi",
            """
import com.luckyframework.httpclient.proxy.annotations.Get;

public interface PlaceholderFuncApi {

    @Get(func = "${'$'}{dynamic.url}")
    String fetch();
}
"""
        )
        val path = pathOf(psiClass, "fetch")

        assertEquals("\${dynamic.url}", path.text)
        assertSegmentsMatchText(path)

        val key = segmentOf(path, "dynamic.url")
        assertEquals(SpelTextAttributes.SPRING_PLACEHOLDER, key.attributesKey)
        assertEquals("/from-config", key.fold?.text)
        assertEquals(SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER, segmentOf(path, "\${").attributesKey)
    }

    fun testUrlTakesPrecedenceOverFunc() {
        val psiClass = addClass(
            "BothApi",
            """
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

public interface BothApi {

    @HttpRequest(url = "/from-url", func = "myUrl")
    String both();
}
"""
        )
        val path = pathOf(psiClass, "both")

        assertEquals("/from-url", path.text)
        assertTrue(
            "URL 优先时不应出现函数名: ${path.segments.map { it.text }}",
            path.segments.none { it.text == "myUrl" }
        )
    }

    fun testBaseUrlPartsAreConcatenatedWithSingleSlash() {
        fun concat(url: String, path: String): String? =
            LuckyNavigatorPath.concatOf(
                LuckyNavigatorPath.plainOf(url),
                LuckyNavigatorPath.plainOf(path)
            )?.text

        assertEquals(
            "http://localhost:8080/idea/",
            concat("http://localhost:8080", "idea/")
        )
        assertEquals(
            "http://localhost:8080/idea/",
            concat("http://localhost:8080/", "/idea/")
        )
        assertEquals(
            "http://localhost:8080/idea/",
            concat("http://localhost:8080", "/idea/")
        )
        assertEquals(
            "http://localhost:8080/idea/",
            concat("http://localhost:8080/", "idea/")
        )
        // 任一侧缺失时保持另一侧原样
        assertEquals("http://localhost:8080", LuckyNavigatorPath.concatOf(
            LuckyNavigatorPath.plainOf("http://localhost:8080"), null
        )?.text)
        assertEquals("idea/", LuckyNavigatorPath.concatOf(
            null, LuckyNavigatorPath.plainOf("idea/")
        )?.text)
    }

    fun testMixedPlaceholderAndSpelSegments() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "idea.test.version=v9\n"
        )
        val psiClass = addClass(
            "MixedApi",
            """
import com.luckyframework.httpclient.proxy.annotations.Post;

public interface MixedApi {

    @Post("path/${'$'}{idea.test.version: 'v1'}/#{${'$'}0}")
    String call(String id);
}
"""
        )
        val path = pathOf(psiClass, "call")

        assertEquals("/path/\${idea.test.version: 'v1'}/#{\$0}", path.text)
        assertSegmentsMatchText(path)

        // 配置键存在时折叠值是配置值，不是默认值
        val key = segmentOf(path, "idea.test.version")
        assertEquals(SpelTextAttributes.SPRING_PLACEHOLDER, key.attributesKey)
        assertEquals("v9", key.fold?.text)
        assertTrue(
            "默认值片段应保留约定样式: ${path.segments.map { it.text to it.attributesKey }}",
            path.segments.any {
                it.text.trim() == "'v1'" && it.attributesKey == SpelTextAttributes.SPRING_PLACEHOLDER_DEFAULT
            }
        )
        // `$0` 是第 0 个参数的最终值，属于 Root 变量空间
        assertEquals(SpelTextAttributes.ROOT_VARIABLE, segmentOf(path, "\$0").attributesKey)
    }

    /** 约定样式：键名米色 + 灰色下划线，默认值与定界符不强调；深浅两套配色同步注册。 */
    fun testPlaceholderAttributesKeepAgreedStyle() {
        assertEquals("e5dad4", colorOption("Darcula.xml", "LUCKY_SPRING_PLACEHOLDER", "FOREGROUND"))
        assertEquals("7b7976", colorOption("Darcula.xml", "LUCKY_SPRING_PLACEHOLDER", "EFFECT_COLOR"))
        assertEquals("1", colorOption("Darcula.xml", "LUCKY_SPRING_PLACEHOLDER", "EFFECT_TYPE"))
        assertEquals("6aab73", colorOption("Darcula.xml", "LUCKY_SPRING_PLACEHOLDER_DEFAULT", "FOREGROUND"))
        assertNull(colorOption("Darcula.xml", "LUCKY_SPRING_PLACEHOLDER_DEFAULT", "EFFECT_TYPE"))
        assertEquals("cc7832", colorOption("Darcula.xml", "LUCKY_SPRING_PLACEHOLDER_DELIMITER", "FOREGROUND"))

        // 浅色主题使用同色系变体，下划线两套一致
        assertEquals("6e5d54", colorOption("Default.xml", "LUCKY_SPRING_PLACEHOLDER", "FOREGROUND"))
        assertEquals("7b7976", colorOption("Default.xml", "LUCKY_SPRING_PLACEHOLDER", "EFFECT_COLOR"))
        assertEquals("067d17", colorOption("Default.xml", "LUCKY_SPRING_PLACEHOLDER_DEFAULT", "FOREGROUND"))
    }

    private fun pathOf(psiClass: PsiClass, methodName: String): LuckyPathPresentation {
        val method = methodOf(psiClass, methodName)
        return LuckyNavigatorPath.presentationOf(
            LuckyHttpAnnotations.urlSourceOf(method),
            LuckyHttpAnnotations.funcSourceOf(method)
        ) ?: error("方法 $methodName 未解析出 path")
    }

    private fun apiItemOf(component: String, methodName: String): LuckyItemNode.Api =
        LuckyNavigatorModelBuilder().build(project)
            .projects
            .flatMap { it.groups }
            .flatMap { it.components }
            .single { it.title == component }
            .items
            .filterIsInstance<LuckyItemNode.Api>()
            .single { it.name == methodName }

    private fun segmentOf(path: LuckyPathPresentation, text: String): LuckyPathSegment =
        path.segments.firstOrNull { it.text == text }
            ?: error("path 中不存在片段 [$text]: ${path.segments.map { it.text }}")

    /** 片段按顺序拼接必须与纯文本一致，折叠只在渲染层生效。 */
    private fun assertSegmentsMatchText(path: LuckyPathPresentation) {
        assertEquals(path.text, path.segments.joinToString("") { it.text })
    }

    /** 读取配色 XML 中某个 TextAttributesKey 的指定选项值（不存在时为 null）。 */
    private fun colorOption(file: String, key: String, option: String): String? {
        val xml = javaClass.getResource("/colorSettings/$file")?.readText()
            ?: error("缺少配色文件 $file")
        val block = Regex("""<option name="$key">(.*?)</option>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)
            ?: return null
        return Regex("""<option name="$option" value="([^"]*)" />""").find(block)?.groupValues?.get(1)
    }
}