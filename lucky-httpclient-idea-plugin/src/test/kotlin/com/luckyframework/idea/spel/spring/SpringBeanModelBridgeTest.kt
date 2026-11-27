package com.luckyframework.idea.spel.spring

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Spring Bean 桥接的服务注册冒烟测试。
 *
 * 测试沙箱通过 Gradle 的 `plugin("com.intellij.spring:...")` 加载 Spring 插件：
 * 此时平台服务必须能实例化直接 API 实现；没有插件时桥接必须保持不可用，
 * 避免社区版加载静态引用 `com.intellij.spring.*` 的类。
 */
class SpringBeanModelBridgeTest : BasePlatformTestCase() {

    fun testBridgeAvailabilityMatchesSpringPlugin() {
        val installed = PluginManagerCore.getPlugin(PluginId.getId("com.intellij.spring")) != null
        assertTrue("测试沙箱应通过 Gradle plugin 依赖加载 Spring 插件", installed)
        assertTrue("Spring 插件存在时必须能实例化直接 API 实现", SpringBeanModelBridge.isAvailable)
    }

    fun testBeansOfNonSpringModuleDoesNotThrow() {
        val file = myFixture.configureByText("Plain.java", "class Plain {}")
        // 测试模块不是 Spring 模块：允许 null（无模型）或空列表（模型无 Bean）
        SpringBeanModelBridge.beans(file)
    }
}
