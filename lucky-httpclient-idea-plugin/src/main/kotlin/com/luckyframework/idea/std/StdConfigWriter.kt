package com.luckyframework.idea.std

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * 标准客户端配置的写入器。
 *
 * YAML 是可选依赖：主包不能引用 YAML PSI，写入能力通过扩展点下沉到
 * `yaml-spel-support.xml`。未安装 YAML 插件时扩展点为空，「生成配置」
 * 入口自动隐藏，Java 侧不会因类加载失败。
 */
interface StdConfigWriter {

    /** 项目内可写入的配置文件，`application*`/`bootstrap*` 排在最前。 */
    fun candidates(project: Project): List<PsiFile>

    /** 项目内没有可用配置文件时新建 `application.yml`；无法创建时返回 null。 */
    fun createDefaultFile(project: Project): PsiFile?

    /**
     * 在文件中写入 `standard-client-configs.<configId>` 的最小化配置。
     *
     * 已存在的键不重复创建；返回是否发生了写入。
     */
    fun write(file: PsiFile, configId: String): Boolean

    companion object {
        val EP_NAME: ExtensionPointName<StdConfigWriter> =
            ExtensionPointName.create("com.luckyframework.idea.httpclient.stdConfigWriter")

        fun instance(): StdConfigWriter? = EP_NAME.extensionList.firstOrNull()
    }
}
