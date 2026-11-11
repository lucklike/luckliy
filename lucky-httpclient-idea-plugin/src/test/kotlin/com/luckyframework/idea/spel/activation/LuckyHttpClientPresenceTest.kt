package com.luckyframework.idea.spel.activation

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.LuckyFixtureTestCase

class LuckyHttpClientPresenceTest : BasePlatformTestCase() {

    fun testModuleWithoutDependencyIsDisabled() {
        myFixture.configureByText("Demo.java", "class Demo {}")
        val module = requireNotNull(ModuleUtilCore.findModuleForPsiElement(myFixture.file))

        assertFalse(LuckyHttpClientPresence.getInstance(project).isEnabledFor(module))
    }

    fun testDependencyAddedLaterFlipsResult() {
        myFixture.configureByText("Demo.java", "class Demo {}")
        val module = requireNotNull(ModuleUtilCore.findModuleForPsiElement(myFixture.file))
        val presence = LuckyHttpClientPresence.getInstance(project)

        assertFalse(presence.isEnabledFor(module))

        myFixture.addFileToProject(LuckyFixtureTestCase.HTTP_REQUEST_STUB_PATH, LuckyFixtureTestCase.HTTP_REQUEST_STUB)

        assertTrue(presence.isEnabledFor(module))
    }
}
