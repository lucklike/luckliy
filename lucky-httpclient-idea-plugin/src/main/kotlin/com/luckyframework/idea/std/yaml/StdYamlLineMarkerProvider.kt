package com.luckyframework.idea.std.yaml

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.std.StdConfigNavigation
import com.luckyframework.idea.std.StdHttpClientIndex
import java.util.function.Supplier
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * 配置文件侧的 `@StdHttpClient` 行标记（YAML 插件存在时加载）。
 *
 * - `standard-client-configs.<configId>`：有接口时导航回接口，没有时显示缺失提示；
 * - `method-configs.<apiId>`：有同名方法时导航回方法，没有时显示缺失提示。
 *
 * 缺失提示与接口行的空配置提示共用设置页开关；关联功能未激活（插件关闭、dumb 模式、
 * 项目未依赖 starter）时两种标记都不显示。缺失提示只有图标与悬浮文案，不提供导航。
 *
 * 行标记只挂在键的首个叶子上：普通键是直接挂在 [YAMLKeyValue] 下的叶子，
 * 带引号的键包在 [YAMLScalar] 里，按起始偏移判断可同时覆盖两种形态，
 * 也保证同一行只出一个图标。
 */
class StdYamlLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val keyValue = keyValueOf(element) ?: return null
        val marker = markerOf(keyValue) ?: return null
        if (!marker.missing) {
            return NavigationGutterIconBuilder.create(LuckyIcons.Config)
                .setTargets(marker.targets)
                .setTooltipText(marker.tooltip)
                .createLineMarkerInfo(element)
        }
        if (!LuckyHttpClientSettings.getInstance().stdConfigMissingHint) {
            return null
        }
        return LineMarkerInfo(
            element,
            element.textRange,
            LuckyIcons.ConfigMissing,
            Function<PsiElement, String> { marker.tooltip },
            null,
            GutterIconRenderer.Alignment.LEFT,
            // 新 API 的必填参数：屏幕阅读器使用的可访问名称，与悬浮提示保持一致
            Supplier { marker.tooltip }
        )
    }

    /**
     * 解析键对应的行标记；不是标准配置键或关联功能未激活时返回 null。
     *
     * [StdYamlMarker.targets] 为空表示配置键没有匹配到任何接口/方法。
     */
    fun markerOf(keyValue: YAMLKeyValue): StdYamlMarker? {
        val virtualFile = keyValue.containingFile.virtualFile ?: return null
        if (!YamlStdConfigSource.inScope(virtualFile)) {
            return null
        }
        val segments = StdConfigLocator.segmentsOf(keyValue)
        val configId: String
        val apiId: String?
        when {
            segments.size == 4 && StdConfigLocator.isStdConfigPath(segments) -> {
                configId = segments[3].trim()
                apiId = null
            }
            segments.size == 6 &&
                StdConfigLocator.isStdConfigPath(segments) &&
                StdConfigLocator.isMethodConfigs(segments[4]) -> {
                configId = segments[3].trim()
                apiId = segments[5].trim()
            }
            else -> return null
        }
        if (configId.isEmpty() || apiId?.isEmpty() == true) {
            return null
        }
        val snapshot = StdHttpClientIndex.getInstance(keyValue.project).snapshot()
        if (!snapshot.active) {
            return null
        }
        val entries = snapshot.byConfigId[configId].orEmpty()
        if (apiId == null) {
            return if (entries.isEmpty()) {
                StdYamlMarker(emptyList(), StdConfigNavigation.missingClientTooltip(configId))
            } else {
                StdYamlMarker(
                    StdConfigNavigation.clientTargets(entries),
                    StdConfigNavigation.clientTooltip(entries)
                )
            }
        }
        val targets = StdConfigNavigation.clientMethodTargets(entries, apiId)
        return if (targets.isEmpty()) {
            StdYamlMarker(
                emptyList(),
                StdConfigNavigation.missingClientMethodTooltip(configId, apiId)
            )
        } else {
            StdYamlMarker(targets, StdConfigNavigation.clientMethodTooltip(entries, apiId))
        }
    }

    /**
     * 键的首个叶子：通过键值对与叶子的起始偏移是否一致，排除值里的叶子
     * 以及键的后续叶子；显式键（`? key`）没有键节点，直接跳过。
     */
    private fun keyValueOf(element: PsiElement): YAMLKeyValue? {
        val parent = element.parent
        val keyValue = when {
            parent is YAMLKeyValue -> parent
            parent is YAMLScalar && parent.parent is YAMLKeyValue ->
                parent.parent as YAMLKeyValue
            else -> return null
        }
        return keyValue.takeIf {
            it.key != null && it.textRange.startOffset == element.textRange.startOffset
        }
    }
}

/**
 * 配置文件侧行标记的解析结果。
 *
 * [targets] 为空表示配置键未匹配到任何接口/方法，由调用方渲染为缺失提示。
 */
class StdYamlMarker(
    val targets: List<PsiElement>,
    val tooltip: String
) {
    val missing: Boolean get() = targets.isEmpty()
}
