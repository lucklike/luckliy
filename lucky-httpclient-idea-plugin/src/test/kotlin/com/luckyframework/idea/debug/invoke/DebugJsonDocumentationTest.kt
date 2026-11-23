package com.luckyframework.idea.debug.invoke

import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DebugJsonDocumentationTest : BasePlatformTestCase() {

    fun `test index follows json template paths`() {
        val docs = DebugJsonDocumentation.index(listOf(requestParameter()))
        assertEquals(
            setOf(
                "request",
                "request.city",
                "request.location",
                "request.location.lat",
                "request.days",
                "request.days.date"
            ),
            docs.keys
        )
        assertEquals("城市名", docs["request.city"]?.description)
    }

    fun `test key offset resolves parameter documentation`() {
        val parameters = listOf(requestParameter())
        val json = DebugArgumentCodec.sampleJson(parameters)
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText("lucky-arguments.json", JsonFileType.INSTANCE, json)
        DebugJsonDocumentation.attach(file, parameters)

        val keyOffset = json.indexOf("\"lat\"") + 2
        assertEquals("lat", DebugJsonDocumentation.parameterAt(file, keyOffset)?.name)
        assertEquals("request.location.lat", jsonPathAt(file, keyOffset))

        // 悬停在值上不弹出字段说明
        val valueOffset = json.indexOf("0", json.indexOf("\"lat\""))
        assertNull(DebugJsonDocumentation.parameterAt(file, valueOffset))
    }

    fun `test documentation html escapes description`() {
        val parameter = parameter(
            "city",
            "java.lang.String",
            DebugParameterKind.STRING,
            description = "城市 <主城区> & 周边"
        )
        val html = DebugJsonDocumentation.htmlOf(parameter)
        assertTrue(html.contains("city"))
        assertTrue(html.contains("String"))
        assertTrue(html.contains("&lt;主城区&gt;"))
        assertTrue(html.contains("&amp;"))
    }

    /** 与悬停文档同一套路径规则，单独取出来做断言便于定位失败原因。 */
    private fun jsonPathAt(file: PsiFile, offset: Int): String? {
        val element = file.findElementAt(offset) ?: return null
        val property = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java, false) ?: return null
        return DebugJsonDocumentation.pathOf(property)
    }

    private fun requestParameter(): DebugParameter = parameter(
        "request",
        "demo.WeatherRequest",
        DebugParameterKind.OBJECT,
        description = "天气查询请求",
        fields = listOf(
            parameter("city", "java.lang.String", DebugParameterKind.STRING, description = "城市名"),
            parameter(
                "location",
                "demo.Location",
                DebugParameterKind.OBJECT,
                fields = listOf(parameter("lat", "double", DebugParameterKind.DOUBLE))
            ),
            parameter(
                "days",
                "java.util.List",
                DebugParameterKind.LIST,
                element = parameter(
                    "element",
                    "demo.Day",
                    DebugParameterKind.OBJECT,
                    fields = listOf(parameter("date", "java.lang.String", DebugParameterKind.STRING))
                )
            )
        )
    )

    private fun parameter(
        name: String,
        typeName: String,
        kind: DebugParameterKind,
        description: String? = null,
        fields: List<DebugParameter> = emptyList(),
        element: DebugParameter? = null
    ): DebugParameter = DebugParameter(
        index = 0,
        name = name,
        typeName = typeName,
        kind = kind,
        description = description,
        fields = fields,
        element = element
    )
}
