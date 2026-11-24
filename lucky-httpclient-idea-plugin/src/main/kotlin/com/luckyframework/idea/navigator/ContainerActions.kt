package com.luckyframework.idea.navigator

import com.intellij.ui.JBColor
import com.luckyframework.idea.debug.container.DebugContainerService
import java.awt.Color

/** 项目行右侧的容器动作；由容器状态决定当前行展示哪些图标。 */
internal enum class ContainerAction(val tooltip: String) {
    START("Start Container"),
    RESTART("Restart Container"),
    STOP("Stop Container"),
    LOG("View Container Logs")
}

/**
 * 项目行应展示的容器动作。
 *
 * 未创建容器、已停止时只提供启动；运行中提供重启与停止；启动失败或自动重启熔断时
 * 在启动之后追加查看日志，方便定位失败原因；启动/停止过渡期间不展示动作，避免重复触发。
 */
internal fun containerActionsFor(state: DebugContainerService.State?): List<ContainerAction> = when {
    containerBusy(state) -> emptyList()

    state == DebugContainerService.State.READY ->
        listOf(ContainerAction.RESTART, ContainerAction.STOP)

    state == DebugContainerService.State.FAILED ||
        state == DebugContainerService.State.SUSPENDED ->
        listOf(ContainerAction.START, ContainerAction.LOG)

    else -> listOf(ContainerAction.START)
}

/** 容器是否处于启动/重启/停止过渡中；过渡期间行尾展示转圈动画。 */
internal fun containerBusy(state: DebugContainerService.State?): Boolean = when (state) {
    DebugContainerService.State.STARTING,
    DebugContainerService.State.RESTARTING,
    DebugContainerService.State.STOPPING -> true

    else -> false
}

/**
 * 容器栏三个按钮的可用性。
 *
 * 与树行按状态隐藏动作不同，容器栏始终展示启动/重启/停止三个按钮，
 * 用禁用态表达当前不可执行的操作：过渡中全部禁用；运行中只允许重启与停止；
 * 停止/失败/熔断状态下只允许启动，重启仍可用于从失败状态重新拉起容器。
 */
internal fun containerStartEnabled(state: DebugContainerService.State?): Boolean = when (state) {
    null,
    DebugContainerService.State.STOPPED,
    DebugContainerService.State.FAILED,
    DebugContainerService.State.SUSPENDED -> true

    else -> false
}

internal fun containerRestartEnabled(state: DebugContainerService.State?): Boolean = when (state) {
    DebugContainerService.State.READY,
    DebugContainerService.State.FAILED,
    DebugContainerService.State.SUSPENDED -> true

    else -> false
}

internal fun containerStopEnabled(state: DebugContainerService.State?): Boolean =
    state == DebugContainerService.State.READY

/** 容器状态文案；界面文案保持英文。 */
internal fun containerStateLabel(state: DebugContainerService.State?): String = when (state) {
    null, DebugContainerService.State.STOPPED -> "Stopped"
    DebugContainerService.State.STARTING -> "Starting..."
    DebugContainerService.State.READY -> "Running"
    DebugContainerService.State.FAILED -> "Failed"
    DebugContainerService.State.STOPPING -> "Stopping..."
    DebugContainerService.State.RESTARTING -> "Restarting..."
    DebugContainerService.State.SUSPENDED -> "Suspended"
}

/** 项目图标上的容器状态点颜色；未启动或未创建容器时返回 null，不绘制状态点。 */
internal fun containerStateColor(state: DebugContainerService.State?): Color? = when (state) {
    DebugContainerService.State.READY -> READY_COLOR

    DebugContainerService.State.FAILED,
    DebugContainerService.State.SUSPENDED -> FAILED_COLOR

    DebugContainerService.State.STARTING,
    DebugContainerService.State.RESTARTING,
    DebugContainerService.State.STOPPING -> BUSY_COLOR

    else -> null
}

private val READY_COLOR = JBColor(Color(0x2C8A46), Color(0x3FA85B))
private val FAILED_COLOR = JBColor(Color(0xB33B33), Color(0xCC4C43))
private val BUSY_COLOR = JBColor(Color(0xB97A0C), Color(0xD39422))
