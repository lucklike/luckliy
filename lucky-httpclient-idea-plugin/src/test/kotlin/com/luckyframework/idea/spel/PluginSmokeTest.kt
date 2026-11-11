package com.luckyframework.idea.spel

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PluginSmokeTest : BasePlatformTestCase() {

    fun testPlatformFixtureIsAvailable() {
        val file = myFixture.configureByText("Demo.java", "class Demo {}")
        assertEquals("Demo.java", file.name)
        assertNotNull(myFixture.project)
    }
}
