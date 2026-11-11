package com.luckyframework.idea.spel.highlight

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor

class SpelColorDefaultsTest : BasePlatformTestCase() {

    fun testApplyRestoresBundledDefaults() {
        val scheme = EditorColorsSchemeImpl(null as EditorColorsScheme?)
        SpelColorDefaults.applyTo(scheme)

        // 浅色主题使用 Default.xml，深色主题使用 Darcula.xml
        val variable = if (JBColor.isBright()) "660e7a" else "c77dbb"
        assertEquals(
            ColorUtil.fromHex(variable),
            scheme.getAttributes(SpelTextAttributes.VARIABLE)?.foregroundColor
        )
        assertEquals(
            ColorUtil.fromHex("808080"),
            scheme.getAttributes(SpelTextAttributes.PARENTHESES)?.foregroundColor
        )
        assertNotNull(scheme.getAttributes(SpelTextAttributes.STRING))
    }
}
