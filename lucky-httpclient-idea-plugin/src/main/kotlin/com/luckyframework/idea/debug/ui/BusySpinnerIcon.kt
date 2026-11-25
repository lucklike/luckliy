package com.luckyframework.idea.debug.ui

import com.intellij.icons.AllIcons
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.Timer

/**
 * 执行按钮的忙碌转圈图标。
 *
 * 帧图与导航栏项目行的过渡态动画同源（平台 `AllIcons.Process.Step_*`），
 * 用 Swing 定时器在 EDT 上推进帧，只重绘绘制过该图标的按钮，
 * 不额外占用线程、也不刷新面板其它区域。尺寸与执行图标一致，
 * 切换时按钮宽高不变，地址栏不会抖动。
 */
internal class BusySpinnerIcon(
    private val iconSize: Int
) : Icon {

    private var frame = 0

    /** 绘制过该图标的组件；定时器只重绘它，不触发全局刷新。 */
    private var target: Component? = null

    private val timer = Timer(FRAME_DELAY_MILLIS) {
        frame = (frame + 1) % FRAMES.size
        target?.repaint()
    }

    override fun getIconWidth(): Int = iconSize

    override fun getIconHeight(): Int = iconSize

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        target = c
        val current = FRAMES[frame % FRAMES.size]
        // 平台帧图标按逻辑像素绘制，居中放进与执行图标一致的区域内
        current.paintIcon(
            c,
            g,
            x + (iconSize - current.iconWidth) / 2,
            y + (iconSize - current.iconHeight) / 2
        )
    }

    fun start() {
        if (!timer.isRunning) {
            timer.start()
        }
    }

    fun stop() {
        timer.stop()
        frame = 0
        target?.repaint()
    }

    fun isRunning(): Boolean = timer.isRunning

    private companion object {

        /** 与导航栏项目行一致的 8 帧平台进度图标。 */
        private val FRAMES = arrayOf(
            AllIcons.Process.Step_1,
            AllIcons.Process.Step_2,
            AllIcons.Process.Step_3,
            AllIcons.Process.Step_4,
            AllIcons.Process.Step_5,
            AllIcons.Process.Step_6,
            AllIcons.Process.Step_7,
            AllIcons.Process.Step_8
        )

        /** 帧间隔与导航栏行尾转圈一致。 */
        private const val FRAME_DELAY_MILLIS = 90
    }
}
