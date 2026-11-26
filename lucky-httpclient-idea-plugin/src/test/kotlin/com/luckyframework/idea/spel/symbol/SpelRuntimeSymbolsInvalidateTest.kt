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

    /**
     * 方法体编辑不能丢弃快照。
     *
     * 快照失效会让下一次高亮在后台重扫全项目注解；按任意 Java PSI 修改计数
     * 失效时，每一次击键都会触发重扫并在完成后重启 daemon，这正是着色延迟的
     * 来源。这里锁住“只有声明结构变化才重建”的行为。
     */
    fun testBodyEditKeepsCachedSnapshot() {
        myFixture.configureByText(
            "BodyEditApi.java",
            """
            package com.example;

            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface BodyEditApi {
                default void run() {
                    int a = 1<caret>;
                }
            }
            """.trimIndent()
        )
        val before = SpelRuntimeSymbols.components(project)
        assertTrue("组件应能被扫描到", before.any { it.className == BODY_EDIT_COMPONENT })

        myFixture.type("2")

        assertSame(
            "方法体编辑不应让符号快照失效",
            before,
            SpelRuntimeSymbols.components(project)
        )
    }

    /**
     * 新增源码文件必须让快照失效，不依赖调用方显式 [SpelRuntimeSymbols.invalidate]。
     *
     * 依赖导入/模块根变化有单独的监听器，但用户在项目里新建组件接口是最常见的
     * 声明变化，结构计数必须能感知。
     */
    fun testNewFileInvalidatesSnapshotWithoutExplicitInvalidate() {
        assertFalse(
            "新组件在添加前不应出现",
            SpelRuntimeSymbols.components(project).any { it.className == ADDED_COMPONENT }
        )

        myFixture.addFileToProject(
            "com/example/AddedWithoutInvalidateApi.java",
            """
            package com.example;

            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface AddedWithoutInvalidateApi {
            }
            """.trimIndent()
        )

        assertTrue(
            "新增文件应自动让符号快照失效",
            SpelRuntimeSymbols.components(project).any { it.className == ADDED_COMPONENT }
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
        private const val BODY_EDIT_COMPONENT = "com.example.BodyEditApi"
        private const val ADDED_COMPONENT = "com.example.AddedWithoutInvalidateApi"
    }
}
