package com.luckyframework.idea.navigator

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.util.Function
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.util.function.Supplier

/**
 * Lucky 组件接口方法行的小飞机调试图标。
 *
 * 与侧边栏方法行的规则一致：图标恒显示，不依赖调试容器状态；点击图标
 * 打开该方法的调试页，容器未启动时由调试页引导启动。
 *
 * 判断依赖组件扫描，必须走 [collectSlowLineMarkers]（后台线程）；
 * 快速通道在 EDT 上执行，那里不能访问注解索引。
 */
class LuckyDebugLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val project = elements.firstOrNull()?.project ?: return
        if (!LuckyHttpClientSettings.getInstance().isEnabled || DumbService.isDumb(project)) {
            return
        }
        for (element in elements) {
            val identifier = element as? PsiIdentifier ?: continue
            val method = identifier.parent as? PsiMethod ?: continue
            if (!LuckyComponentKinds.isDebugApiMethod(project, method)) {
                continue
            }
            result.add(markerOf(project, identifier, method))
        }
    }

    private fun markerOf(
        project: Project,
        identifier: PsiIdentifier,
        method: PsiMethod
    ): LineMarkerInfo<*> {
        val tooltip = "Debug Method"
        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            LuckyIcons.Send,
            Function<PsiIdentifier, String> { tooltip },
            GutterIconNavigationHandler<PsiIdentifier> { _, _ ->
                LuckyDebugOpener.getInstance(project).open(method)
            },
            GutterIconRenderer.Alignment.LEFT,
            // 新 API 的必填参数：屏幕阅读器使用的可访问名称，与悬浮提示保持一致
            Supplier { tooltip }
        )
    }
}
