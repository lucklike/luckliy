package com.luckyframework.idea.spel.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.service

/**
 * 插件全局开关。
 *
 * 默认启用；关闭后所有 SpEL 编辑器能力（高亮、补全、跳转、检查）立即失效，
 * 但不修改任何源码。
 */
@State(name = "LuckyHttpClientSpelSettings", storages = [Storage("lucky-httpclient-spel.xml")])
@Service(Service.Level.APP)
class LuckyHttpClientSettings : PersistentStateComponent<LuckyHttpClientSettings.State> {

    data class State(var enabled: Boolean = true)

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

    companion object {
        fun getInstance(): LuckyHttpClientSettings = service()
    }
}
