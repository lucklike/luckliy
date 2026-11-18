package com.luckyframework.idea.spel

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 品牌图标资源必须能被平台加载。
 *
 * SVG 解析失败（格式非法、路径写错）只会在运行期抛异常，用测试提前暴露。
 */
class LuckyIconsTest : BasePlatformTestCase() {

    fun testComponentIconIsNormalizedTo16() {
        assertEquals(16, LuckyIcons.Component.iconWidth)
        assertEquals(16, LuckyIcons.Component.iconHeight)
    }

    fun testLogoIconLoads() {
        assertEquals(16, LuckyIcons.Logo.iconWidth)
        assertEquals(16, LuckyIcons.Logo.iconHeight)
    }
}
