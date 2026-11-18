package com.luckyframework.idea.std

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.metadata.SpelFrameworkStubs
import com.luckyframework.idea.spel.metadata.SpelMetaStubs
import com.luckyframework.idea.std.yaml.YamlStdConfigSource
import com.luckyframework.idea.std.yaml.YamlStdConfigWriter

/**
 * P1 测试基类：加入 `@StdHttpClient`、`@ApiConfig`、`@Api` 与 `@AliasFor` 桩。
 *
 * 同时加载 SpEL 元注解桩，保证 `HttpRequest` 等测试桩可以被正常解析；
 * 不需要引入真实框架 jar。
 */
abstract class StdFixtureTestCase : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        registerStdExtensions()
        (SpelMetaStubs.STUBS + SpelFrameworkStubs.STUBS + StdHttpClientStubs.STUBS)
            .forEach { (path, text) -> myFixture.addFileToProject(path, text) }
    }

    /**
     * 测试容器不会加载插件的 `META-INF/plugin.xml`，这里手工注册 P1 依赖的
     * 配置节点来源与写入器，让索引与生成配置走与生产环境一致的链路。
     *
     * 平台 2024.1 的 `maskAll` 会把被 mask 的扩展点登记进只读集合，
     * 同一 EP 再次 mask 会直接抛错，因此整个测试 JVM 只注册一次，
     * 由 Application 兜底释放。
     */
    private fun registerStdExtensions() {
        if (extensionsRegistered) {
            return
        }
        extensionsRegistered = true
        val application = ApplicationManager.getApplication()
        application.runWriteAction {
            val area = application.extensionArea
            if (area.getExtensionPointIfRegistered<Any>(StdConfigSource.EP_NAME.name) == null) {
                area.registerExtensionPoint(
                    StdConfigSource.EP_NAME.name,
                    StdConfigSource::class.java.name,
                    ExtensionPoint.Kind.INTERFACE,
                    false
                )
            }
            ExtensionTestUtil.maskExtensions(
                StdConfigSource.EP_NAME,
                listOf<StdConfigSource>(YamlStdConfigSource()),
                application
            )
            if (area.getExtensionPointIfRegistered<Any>(StdConfigWriter.EP_NAME.name) == null) {
                area.registerExtensionPoint(
                    StdConfigWriter.EP_NAME.name,
                    StdConfigWriter::class.java.name,
                    ExtensionPoint.Kind.INTERFACE,
                    false
                )
            }
            ExtensionTestUtil.maskExtensions(
                StdConfigWriter.EP_NAME,
                listOf<StdConfigWriter>(YamlStdConfigWriter()),
                application
            )
        }
    }

    private companion object {
        @Volatile
        private var extensionsRegistered = false
    }
}
