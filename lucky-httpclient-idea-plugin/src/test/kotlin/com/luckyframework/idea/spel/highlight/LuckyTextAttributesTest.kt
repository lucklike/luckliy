package com.luckyframework.idea.spel.highlight

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 8.1 高亮属性键与颜色设置页。
 */
class LuckyTextAttributesTest : BasePlatformTestCase() {

    fun testSeventeenKeysWithDefaults() {
        val keys = SpelTextAttributes.all()

        assertEquals(17, keys.size)
        assertEquals(17, keys.map { it.externalName }.toSet().size)
        for (key in keys) {
            assertNotNull("缺少默认颜色: ${key.externalName}", key.fallbackAttributeKey)
        }
    }
}
