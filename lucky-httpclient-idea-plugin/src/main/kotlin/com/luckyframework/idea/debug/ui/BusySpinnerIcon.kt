package com.luckyframework.idea.debug.ui

import com.intellij.ui.JBColor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import javax.swing.Icon
import javax.swing.Timer

/**
 * 执行按钮的忙碌转圈图标。
 *
 * 用 Swing 定时器在 EDT 上推进弧线角度，只重绘绘制过该图标的按钮，
 * 不额外占用线程、也不刷新面板其它区域。尺寸与执行图标一致，
 * 切换时按钮宽高不变，地址栏不会抖动。
 */
internal class BusySpinnerIcon(
    private val iconSize: Int,
    private val color: Color = JBColor.foreground()
) : Icon {

    private var angle = 0

    /** 绘制过该图标的组件；定时器只重绘它，不触发全局刷新。 */
    private var target: Component? = null

    private val timer = Timer(FRAME_DELAY_MILLIS) {
        angle = (angle + ANGLE_STEP) % FULL_CIRCLE
        target?.repaint()
    }

    override fun getIconWidth(): Int = iconSize

    override fun getIconHeight(): Int = iconSize

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        target = c
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = color
            g2.stroke = BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            // 两端各留半个线宽，圆角才不会被图标边界裁掉
            val inset = STROKE_WIDTH / 2f + 1f
            val diameter = iconSize - inset * 2
            g2.draw(
                Arc2D.Float(
                    x + inset,
                    y + inset,
                    diameter,
                    diameter,
                    angle.toFloat(),
                    ARC_DEGREES.toFloat(),
                    Arc2D.OPEN
                )
            )
        } finally {
            g2.dispose()
        }
    }

    fun start() {
        if (!timer.isRunning) {
            timer.start()
        }
    }

    fun stop() {
        timer.stop()
        angle = 0
        target?.repaint()
    }

    fun isRunning(): Boolean = timer.isRunning

    private companion object {
        const val FULL_CIRCLE = 360

        /** 每帧转过的角度：12 帧一圈，转速接近平台的进度图标。 */
        const val ANGLE_STEP = 30
        const val FRAME_DELAY_MILLIS = 70

        /** 弧长：留出缺口，转动方向清晰可辨。 */
        const val ARC_DEGREES = 300

        /** 线宽按图标逻辑像素计，高 DPI 下由绘制变换统一缩放。 */
        const val STROKE_WIDTH = 1.6f
    }
}
