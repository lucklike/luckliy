package com.luckyframework.idea.spel.highlight

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 8.1 高亮属性键与颜色设置页。
 */
class LuckyTextAttributesTest : BasePlatformTestCase() {

    fun testTwelveKeysWithDefaults() {
        val keys = SpelTextAttributes.all()

        assertEquals(12, keys.size)
        assertEquals(12, keys.map { it.externalName }.toSet().size)
        for (key in keys) {
            assertNotNull("缺少默认颜色: ${key.externalName}", key.fallbackAttributeKey)
        }
    }
}
