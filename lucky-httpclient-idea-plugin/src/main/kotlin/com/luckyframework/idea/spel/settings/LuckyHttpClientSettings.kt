package com.luckyframework.idea.spel.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.service

/**
 * 插件全局设置。
 *
 * 默认启用；关闭后所有 SpEL 编辑器能力（高亮、补全、跳转、检查）立即失效，
 * 但不修改任何源码。
 *
 * `@LuckyComponent` 组件类图标可单独开关，并支持指定本地 SVG/PNG/JPG 文件；
 * 未指定文件时使用插件内置图标。
 *
 * `@StdHttpClient` 配置关联支持限定 YAML 文件范围，并可关闭缺失关联提示
 *（接口找不到配置、配置键找不到接口/方法）。
 *
 * 侧边导航栏可关闭包名展示：关闭后不再渲染包节点与包名副标题。
 *
 * 接口调试容器默认以非 Web 方式启动，不占用端口；需要 Servlet 上下文时
 * 可打开 [isDebugWebServerEnabled] 让容器启动内嵌 Web 服务器。
 *
 * 调试面板头部支持两种展示维度：默认方法维度（方法图标 + 方法全路径），
 * 可切换为 URL 维度（HTTP 方法徽标 + 完整地址），由 [isDebugHeaderShowsMethodPath] 控制。
 */
@State(name = "LuckyHttpClientSpelSettings", storages = [Storage("lucky-httpclient-spel.xml")])
@Service(Service.Level.APP)
class LuckyHttpClientSettings : PersistentStateComponent<LuckyHttpClientSettings.State> {

    data class State(
        var enabled: Boolean = true,
        var componentIconEnabled: Boolean = true,
        var componentIconPath: String = "",
        var stdConfigFileScope: String = "ALL_YAML",
        var stdConfigMissingHint: Boolean = true,
        var environmentPreviewEnabled: Boolean = true,
        var showPackageNames: Boolean = true,
        var debugWebServerEnabled: Boolean = false,
        var debugHeaderShowsMethodPath: Boolean = true
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var isEnabled: Boolean
        get() = state.enabled
        set(value) {
            state.enabled = value
        }

    var isComponentIconEnabled: Boolean
        get() = state.componentIconEnabled
        set(value) {
            state.componentIconEnabled = value
        }

    var componentIconPath: String
        get() = state.componentIconPath
        set(value) {
            state.componentIconPath = value
        }

    /** `@StdHttpClient` 关联的 YAML 文件范围：[SCOPE_ALL_YAML] 或 [SCOPE_APPLICATION_ONLY]。 */
    var stdConfigFileScope: String
        get() = state.stdConfigFileScope
        set(value) {
            state.stdConfigFileScope = value
        }

    /** 未找到关联时是否在接口行与配置键行显示提示。 */
    var stdConfigMissingHint: Boolean
        get() = state.stdConfigMissingHint
        set(value) {
            state.stdConfigMissingHint = value
        }

    /** 是否把 `${...}` 的解析值折叠显示在编辑器中。 */
    var isEnvironmentPreviewEnabled: Boolean
        get() = state.environmentPreviewEnabled
        set(value) {
            state.environmentPreviewEnabled = value
        }

    /** 导航栏是否展示包名：关闭后隐藏包节点与包名副标题。 */
    var isShowPackageNames: Boolean
        get() = state.showPackageNames
        set(value) {
            state.showPackageNames = value
        }

    /** 调试容器是否启动内嵌 Web 服务器；默认关闭，以非 Web 方式运行 Spring。 */
    var isDebugWebServerEnabled: Boolean
        get() = state.debugWebServerEnabled
        set(value) {
            state.debugWebServerEnabled = value
        }

    /**
     * 调试面板头部是否展示方法维度：默认 true（方法图标 + 方法全路径），
     * 关闭后为 URL 维度（HTTP 方法徽标 + 完整地址）。
     */
    var isDebugHeaderShowsMethodPath: Boolean
        get() = state.debugHeaderShowsMethodPath
        set(value) {
            state.debugHeaderShowsMethodPath = value
        }

    companion object {
        const val SCOPE_ALL_YAML = "ALL_YAML"
        const val SCOPE_APPLICATION_ONLY = "APPLICATION_ONLY"

        fun getInstance(): LuckyHttpClientSettings = service()
    }
}
