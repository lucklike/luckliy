package com.luckyframework.idea.std.yaml

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * 在 YAML 键上注册标准客户端配置引用（YAML 插件存在时加载）。
 *
 * 简单键在 YAML PSI 中不是独立的 `YAMLScalar` 节点，叶子直接挂在
 * [YAMLKeyValue] 下，因此必须在 [YAMLKeyValue] 上注册才能拿到普通键。
 * 与现有 SpEL 引用提供者并存：SpEL 只处理含 `#{` 的值标量，两者不重叠。
 */
class StdYamlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java),
            StdYamlReferenceProvider()
        )
    }
}
