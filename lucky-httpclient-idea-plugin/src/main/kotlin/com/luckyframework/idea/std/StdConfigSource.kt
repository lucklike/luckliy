package com.luckyframework.idea.std

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * 标准客户端配置节点来源。
 *
 * YAML 是可选依赖：主包不能直接引用 YAML PSI 类型，否则未安装 YAML 插件时
 * Java 侧行标记会因类加载失败。实现由 `yaml-spel-support.xml` 注册，
 * 未安装 YAML 插件时扩展点为空，索引只保留 Java 侧的客户端信息。
 */
interface StdConfigSource {

    fun nodes(project: Project): Map<String, List<StdConfigNode>>

    companion object {
        val EP_NAME: ExtensionPointName<StdConfigSource> =
            ExtensionPointName.create("com.luckyframework.idea.httpclient.stdConfigSource")
    }
}
