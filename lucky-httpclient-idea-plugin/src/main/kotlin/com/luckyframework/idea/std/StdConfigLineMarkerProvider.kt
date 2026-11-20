package com.luckyframework.idea.std

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.util.Function
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.util.function.Supplier

/**
 * `@StdHttpClient` 接口/方法的配置行标记。
 *
 * 接口行：存在配置时导航到全部 `standard-client-configs.<configId>` 节点；
 * 不存在时显示缺失态（受设置页开关控制），点击图标可生成最小化配置。
 * 方法行只在存在 `method-configs.<apiId>` 时显示，避免在大量无方法级配置的接口上
 * 产生噪音。
 *
 * 关联关系来自 `@StdHttpClient` 索引，必须走 [collectSlowLineMarkers]（后台线程）：
 * 快速通道在 EDT 上执行，那里不能访问索引。
 */
class StdConfigLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val project = elements.firstOrNull()?.project ?: return
        val index = StdHttpClientIndex.getInstance(project)
        for (element in elements) {
            val identifier = element as? PsiIdentifier ?: continue
            val marker = when (val owner = identifier.parent) {
                is PsiClass -> classMarker(owner, identifier, index)
                is PsiMethod -> methodMarker(owner, identifier, index)
                else -> null
            }
            marker?.let { result.add(it) }
        }
    }

    private fun classMarker(
        psiClass: PsiClass,
        identifier: PsiIdentifier,
        index: StdHttpClientIndex
    ): LineMarkerInfo<*>? {
        val entry = index.entryOf(psiClass) ?: return null
        val targets = StdConfigNavigation.configTargets(entry)
        if (targets.isNotEmpty()) {
            return NavigationGutterIconBuilder.create(LuckyIcons.Config)
                .setTargets(targets)
                .setTooltipText(StdConfigNavigation.configTooltip(entry))
                .createLineMarkerInfo(identifier)
        }
        if (!LuckyHttpClientSettings.getInstance().stdConfigMissingHint) {
            return null
        }
        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            LuckyIcons.ConfigMissing,
            Function<PsiIdentifier, String> { StdConfigNavigation.missingTooltip(entry) },
            GutterIconNavigationHandler<PsiIdentifier> { _, _ ->
                val project = psiClass.project
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                StdConfigGenerator.chooseTarget(project, editor, entry.configId)
            },
            GutterIconRenderer.Alignment.LEFT,
            // 新 API 的必填参数：屏幕阅读器使用的可访问名称，与悬浮提示保持一致
            Supplier { StdConfigNavigation.missingTooltip(entry) }
        )
    }

    private fun methodMarker(
        method: PsiMethod,
        identifier: PsiIdentifier,
        index: StdHttpClientIndex
    ): LineMarkerInfo<*>? {
        val targets = index.methodNodesOf(method)
        if (targets.isEmpty()) {
            return null
        }
        val entry = index.entryOf(method.containingClass ?: return null) ?: return null
        val apiId = index.apiIdOf(method) ?: method.name
        return NavigationGutterIconBuilder.create(LuckyIcons.Config)
            .setTargets(targets)
            .setTooltipText(StdConfigNavigation.methodTooltip(entry.configId, apiId))
            .createLineMarkerInfo(identifier)
    }
}
