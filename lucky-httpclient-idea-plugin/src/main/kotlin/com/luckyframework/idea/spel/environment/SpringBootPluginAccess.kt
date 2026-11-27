package com.luckyframework.idea.spel.environment

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

/**
 * 可选 Spring Boot 插件的类加载入口。
 *
 * 插件在 plugin.xml 中声明了 `com.intellij.spring.boot` 的可选依赖：
 * Ultimate 有该插件时，本插件的 classloader 能看到它的类；社区版没有时
 * 依赖被跳过，这里返回 null。所有 Spring Boot 类型只通过反射使用，
 * 主 classpath 不出现这些类型。
 */
internal object SpringBootPluginAccess {

    private const val SPRING_BOOT_PLUGIN_ID = "com.intellij.spring.boot"

    private val springBootPluginClassLoader: ClassLoader? by lazy {
        runCatching {
            val descriptor = PluginManagerCore.getPlugin(PluginId.getId(SPRING_BOOT_PLUGIN_ID))
                ?: return@runCatching null
            descriptor.javaClass.methods
                .firstOrNull { it.name == "getPluginClassLoader" && it.parameterCount == 0 }
                ?.invoke(descriptor) as? ClassLoader
        }.getOrNull()
    }

    fun loadClass(name: String): Class<*>? {
        val loaders = listOfNotNull(
            SpringBootPluginAccess::class.java.classLoader,
            springBootPluginClassLoader
        ).distinct()
        for (loader in loaders) {
            val loaded = runCatching { Class.forName(name, false, loader) }.getOrNull()
            if (loaded != null) {
                return loaded
            }
        }
        return null
    }
}
