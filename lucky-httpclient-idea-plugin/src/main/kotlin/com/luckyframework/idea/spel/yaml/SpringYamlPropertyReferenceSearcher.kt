package com.luckyframework.idea.spel.yaml

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.luckyframework.idea.spel.environment.SpringPlaceholderUsageSearch
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * YAML 配置键重命名时收集 Java 注解中的 `${key}` 用法。
 *
 * YAML 声名的 `name` 只是叶子键（`port`），直接拿它搜索会命中
 * `${port}` 而不是 `${server.port}`；这里先拼出完整点分路径再精确匹配。
 *
 * 在 `yaml-spel-support.xml` 中注册，主 jar 不依赖 YAML PSI。
 */
class SpringYamlPropertyReferenceSearcher :
    QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val target = queryParameters.elementToSearch as? YAMLKeyValue ?: return
        val key = SpringYamlKeyPaths.fullKeyOf(target) ?: return
        // 虚拟声明推导不出文件范围时平台会给出空 LocalSearchScope，降级到元素使用范围
        val requested = queryParameters.effectiveSearchScope
        val scope = if (requested is LocalSearchScope && requested.scope.isEmpty()) {
            target.useScope
        } else {
            requested
        }
        SpringPlaceholderUsageSearch.processReferences(target.project, key, scope) { reference ->
            consumer.process(reference)
        }
    }
}
