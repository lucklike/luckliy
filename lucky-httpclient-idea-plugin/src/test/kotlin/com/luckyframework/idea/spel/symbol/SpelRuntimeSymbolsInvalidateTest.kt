package com.luckyframework.idea.spel.symbol

import com.luckyframework.idea.spel.LuckyFixtureTestCase

/**
 * 组件扫描快照的失效入口。
 *
 * 首次打开项目时依赖/索引未就绪会先扫描出空组件清单；依赖变化后必须能
 * 主动丢弃旧快照，否则图标一直使用默认图标。
 */
class SpelRuntimeSymbolsInvalidateTest : LuckyFixtureTestCase() {

    fun testInvalidateReScansComponentsAfterNewComponentAppears() {
        assertFalse(
            "新组件在添加前不应出现",
            SpelRuntimeSymbols.components(project).any { it.className == NEW_COMPONENT }
        )

        myFixture.addFileToProject(
            "com/example/NewComponentApi.java",
            """
            package com.example;

            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface NewComponentApi {
            }
            """.trimIndent()
        )

        SpelRuntimeSymbols.invalidate(project)

        assertTrue(
            "失效后应重新扫描并发现新增组件",
            SpelRuntimeSymbols.components(project).any { it.className == NEW_COMPONENT }
        )
    }

    fun testInvalidateDropsCachedSnapshot() {
        myFixture.addFileToProject(
            "com/example/StableComponentApi.java",
            """
            package com.example;

            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface StableComponentApi {
            }
            """.trimIndent()
        )

        val before = SpelRuntimeSymbols.components(project)
        assertTrue("组件应能被扫描到", before.any { it.className == STABLE_COMPONENT })

        SpelRuntimeSymbols.invalidate(project)
        val after = SpelRuntimeSymbols.components(project)

        assertNotSame("失效后应重建快照而不是复用旧结果", before, after)
        assertEquals(
            "重新扫描的组件集合不应变化",
            before.map { it.className }.toSet(),
            after.map { it.className }.toSet()
        )
    }

    companion object {
        private const val NEW_COMPONENT = "com.example.NewComponentApi"
        private const val STABLE_COMPONENT = "com.example.StableComponentApi"
    }
}
