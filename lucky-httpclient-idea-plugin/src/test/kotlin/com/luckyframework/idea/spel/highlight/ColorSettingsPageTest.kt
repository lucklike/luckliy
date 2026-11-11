package com.luckyframework.idea.spel.highlight

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 8.1 颜色设置页描述符与示例代码。
 */
class ColorSettingsPageTest : BasePlatformTestCase() {

    fun testDescriptorsAndDemoText() {
        val page = SpelColorSettingsPage()

        assertEquals(12, page.attributeDescriptors.size)
        assertTrue(page.displayName.isNotBlank())
        assertTrue(page.demoText.isNotBlank())
        assertTrue(page.demoText.contains("#{"))
        assertNotNull(page.highlighter)
        assertTrue(page.colorDescriptors.isEmpty())
    }
}
