package com.luckyframework.idea.spel.environment

/**
 * `${key}` 补全的排序：以输入开头（组 1）在前，仅包含输入（组 2）在后，
 * 同组内按不区分大小写的字母序。
 */
class SpringPlaceholderCompletionOrderTest : SpringEnvironmentFixtureTestCase() {

    fun testPrefixMatchesComeBeforeContainsMatches() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            """
sel.alpha=1
my.sel.beta=2
alpha.sel.gamma=3
unrelated=4
""".trimIndent()
        )
        myFixture.configureByText(
            "DemoApi.java",
            """
@io.github.lucklike.httpclient.std.StdHttpClient(path = "${'$'}{sel<caret>}")
interface DemoApi {
}
"""
        )

        myFixture.completeBasic()

        val matched = myFixture.lookupElementStrings.orEmpty()
            .filter { it.contains("sel", ignoreCase = true) }
        assertEquals(listOf("sel.alpha", "alpha.sel.gamma", "my.sel.beta"), matched)
    }

    fun testAlphabeticalOrderInsideGroup() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            """
zeta.sel=1
alpha.sel=2
middle.sel=3
""".trimIndent()
        )
        myFixture.configureByText(
            "DemoApi.java",
            """
@io.github.lucklike.httpclient.std.StdHttpClient(path = "${'$'}{sel<caret>}")
interface DemoApi {
}
"""
        )

        myFixture.completeBasic()

        val matched = myFixture.lookupElementStrings.orEmpty()
            .filter { it.contains("sel", ignoreCase = true) }
        assertEquals(listOf("alpha.sel", "middle.sel", "zeta.sel"), matched)
    }
}
