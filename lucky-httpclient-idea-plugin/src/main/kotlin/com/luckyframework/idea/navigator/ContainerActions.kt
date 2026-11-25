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
 * 左键点击电源按钮执行的动作。
 *
 * 电源按钮是容器栏唯一的主操作入口：未启动时启动，启动中（含重启中）、
 * 运行中、失败或熔断时点击都关闭容器；正在停止时不再响应，避免重复触发。
 */
internal fun containerPowerAction(state: DebugContainerService.State?): ContainerAction? = when (state) {
    null, DebugContainerService.State.STOPPED -> ContainerAction.START

    DebugContainerService.State.STARTING,
    DebugContainerService.State.RESTARTING,
    DebugContainerService.State.READY,
    DebugContainerService.State.FAILED,
    DebugContainerService.State.SUSPENDED -> ContainerAction.STOP

    DebugContainerService.State.STOPPING -> null
}

/**
 * 电源按钮右键菜单：按状态给出可用操作，顺序与容器栏展示一致。
 *
 * 未启动只能启动；失败先看日志再重新启动；启动中只能停止与看日志；
 * 运行中提供停止、日志与重启；正在停止时只保留日志。
 */
internal fun containerContextMenuActions(state: DebugContainerService.State?): List<ContainerAction> =
    when (state) {
        null,
        DebugContainerService.State.STOPPED -> listOf(ContainerAction.START)

        DebugContainerService.State.FAILED,
        DebugContainerService.State.SUSPENDED ->
            listOf(ContainerAction.LOG, ContainerAction.START)

        DebugContainerService.State.STARTING,
        DebugContainerService.State.RESTARTING ->
            listOf(ContainerAction.STOP, ContainerAction.LOG)

        DebugContainerService.State.READY ->
            listOf(ContainerAction.STOP, ContainerAction.LOG, ContainerAction.RESTART)

        DebugContainerService.State.STOPPING -> listOf(ContainerAction.LOG)
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

/** 未启动/已停止的角标描边色：与平台 Module 图标角标描边一致。 */
internal fun containerBadgeColor(state: DebugContainerService.State?): Color =
    containerStateColor(state) ?: IDLE_COLOR

/**
 * 容器栏电源按钮的颜色。
 *
 * 停止态用中性灰，启动中/重启中/停止中为黄色，运行中为绿色，失败或熔断为红色；
 * 其余（树行项目图标）沿用 [containerStateColor]。
 */
internal fun containerPowerColor(state: DebugContainerService.State?): Color =
    containerStateColor(state) ?: STOPPED_COLOR

private val READY_COLOR = JBColor(Color(0x2C8A46), Color(0x3FA85B))
private val FAILED_COLOR = JBColor(Color(0xB33B33), Color(0xCC4C43))
private val BUSY_COLOR = JBColor(Color(0xB97A0C), Color(0xD39422))
private val IDLE_COLOR = JBColor(Color(0x3574F0), Color(0x548AF7))
private val STOPPED_COLOR = JBColor(Color(0x9AA0AB), Color(0x6C707E))
