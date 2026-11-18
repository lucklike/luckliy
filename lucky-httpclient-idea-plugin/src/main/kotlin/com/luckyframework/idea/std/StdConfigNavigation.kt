package com.luckyframework.idea.std

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

/**
 * 行标记与 Ctrl+Click 共用的目标收集与提示文案。
 *
 * 配置节点可能是多个文件中的同名 configId，统一返回列表，由平台提供多目标选择。
 * 反向（配置文件 → 接口/方法）同样返回列表：configId 可能被多个接口共用，
 * 重载方法共享同一个 apiId。
 */
object StdConfigNavigation {

    fun configTargets(entry: StdClientEntry): List<PsiElement> =
        entry.configNodes.map { it.keyValue }

    fun methodTargets(entry: StdClientEntry, apiId: String): List<PsiElement> =
        entry.configNodes.mapNotNull { it.methodConfigs[apiId] }

    /** 配置文件 → 接口：同一 configId 可能被多个接口共用。 */
    fun clientTargets(entries: List<StdClientEntry>): List<PsiElement> =
        entries.map { it.clientClass }

    /** 配置文件 → 接口方法：重载方法共享同一个 apiId，返回全部同名方法。 */
    fun clientMethodTargets(entries: List<StdClientEntry>, apiId: String): List<PsiElement> =
        entries.flatMap { entry -> entry.methods.filter { it.apiId == apiId }.map { it.method } }

    fun configTooltip(entry: StdClientEntry): String =
        "StdHttpClient 配置：lucky.http-client.standard-client-configs.${entry.configId}"

    fun missingTooltip(entry: StdClientEntry): String =
        "未找到配置：lucky.http-client.standard-client-configs.${entry.configId}（点击生成）"

    fun methodTooltip(configId: String, apiId: String): String =
        "方法级配置：lucky.http-client.standard-client-configs.$configId.method-configs.$apiId"

    fun clientTooltip(entries: List<StdClientEntry>): String =
        if (entries.size == 1) {
            "StdHttpClient 接口：${className(entries.single().clientClass)}"
        } else {
            "StdHttpClient 接口（${entries.size} 个）：" +
                entries.joinToString("、") { className(it.clientClass) }
        }

    /** 配置文件键未匹配到任何接口时的提示文案。 */
    fun missingClientTooltip(configId: String): String =
        "未找到接口：lucky.http-client.standard-client-configs.$configId"

    /** 方法配置键未匹配到任何接口方法时的提示文案。 */
    fun missingClientMethodTooltip(configId: String, apiId: String): String =
        "未找到接口方法：lucky.http-client.standard-client-configs.$configId.method-configs.$apiId"

    fun clientMethodTooltip(entries: List<StdClientEntry>, apiId: String): String {
        val owners = entries.flatMap { entry ->
            entry.methods.filter { it.apiId == apiId }
                .map { "${className(entry.clientClass)}#${it.method.name}" }
        }
        return if (owners.size == 1) {
            "StdHttpClient 方法：${owners.single()}"
        } else {
            "StdHttpClient 方法（${owners.size} 个）：${owners.joinToString("、")}"
        }
    }

    private fun className(psiClass: PsiClass): String =
        psiClass.qualifiedName ?: psiClass.name ?: "<anonymous>"
}
