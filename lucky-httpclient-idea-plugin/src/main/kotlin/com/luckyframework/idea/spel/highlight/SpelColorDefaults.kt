package com.luckyframework.idea.spel.highlight

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 插件内置的 SpEL 配色默认值。
 *
 * `colorSettings/Default.xml` 与 `Darcula.xml` 只在颜色方案创建时由平台合并，
 * 用户改色后仅删除覆盖项会回落到平台 fallback 而不是插件默认色，因此「还原到初始化配置」
 * 必须重新读取资源并写回当前方案。
 */
object SpelColorDefaults {

    /** 把当前主题对应的默认配色写回 [scheme]。 */
    fun applyTo(scheme: EditorColorsScheme) {
        load()?.forEach { (key, attributes) -> scheme.setAttributes(key, attributes) }
    }

    /** 浅色主题用 Default.xml，深色主题用 Darcula.xml。 */
    private fun load(): Map<TextAttributesKey, TextAttributes>? {
        val resource = if (JBColor.isBright()) DEFAULT_RESOURCE else DARCULA_RESOURCE
        val stream = SpelColorDefaults::class.java.getResourceAsStream(resource) ?: return null
        return stream.use {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
            val keys = SpelTextAttributes.all().associateBy { key -> key.externalName }
            val options = document.documentElement.getElementsByTagName("option")
            val result = LinkedHashMap<TextAttributesKey, TextAttributes>()
            for (index in 0 until options.length) {
                val option = options.item(index) as? Element ?: continue
                val key = keys[option.getAttribute("name")] ?: continue
                val value = option.getElementsByTagName("value").item(0) as? Element ?: continue
                result[key] = attributesOf(value)
            }
            result
        }
    }

    private fun attributesOf(value: Element): TextAttributes {
        val attributes = TextAttributes()
        val properties = value.getElementsByTagName("option")
        for (index in 0 until properties.length) {
            val property = properties.item(index) as? Element ?: continue
            val text = property.getAttribute("value")
            when (property.getAttribute("name")) {
                "FOREGROUND" -> ColorUtil.fromHex(text)?.let { attributes.foregroundColor = it }
                "BACKGROUND" -> ColorUtil.fromHex(text)?.let { attributes.backgroundColor = it }
                "EFFECT_COLOR" -> ColorUtil.fromHex(text)?.let { attributes.effectColor = it }
                "ERROR_STRIPE_COLOR" -> ColorUtil.fromHex(text)?.let { attributes.errorStripeColor = it }
                "FONT_TYPE" -> text.toIntOrNull()?.let { attributes.fontType = it }
                "EFFECT_TYPE" -> text.toIntOrNull()?.let {
                    EffectType.values().getOrNull(it)?.let { type -> attributes.effectType = type }
                }
            }
        }
        return attributes
    }

    private const val DEFAULT_RESOURCE = "/colorSettings/Default.xml"
    private const val DARCULA_RESOURCE = "/colorSettings/Darcula.xml"
}
