package com.luckyframework.idea.std

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod

/**
 * 一个 `@StdHttpClient` 接口在某个 PSI 版本下的关联信息。
 *
 * [configId] 是运行期实际使用的 ID：`@StdHttpClient#configId` → `@ApiConfig#value`
 * → 接口简单类名。[configNodes] 是配置文件中匹配到该 ID 的节点，可能为空，
 * 也可能有多个（多个配置文件同时声明）。
 */
class StdClientEntry(
    val clientClass: PsiClass,
    val configId: String,
    val methods: List<StdMethodEntry>,
    val configNodes: List<StdConfigNode>
)

/** 接口方法与运行期 apiId 的对应关系；apiId 默认是方法名。 */
class StdMethodEntry(
    val method: PsiMethod,
    val apiId: String
)

/**
 * 配置文件中的一个标准客户端配置节点。
 *
 * [keyValue] 是 `standard-client-configs.<configId>` 这个键值对；
 * [methodConfigs] 是 `method-configs.<apiId>` 子节点，key 为去引号后的 apiId；
 * [baseUrl] 是节点下 `url` 属性的原始文本（未配置时为 null）；
 * [methodPaths] 是各 apiId 配置的 `path` 属性，供侧边栏展示方法路径。
 *
 * 这里只保存平台 [PsiElement]，不引用 YAML PSI 类型：YAML 支持是可选依赖，
 * 未安装 YAML 插件时主包（Java 侧）不能因为类加载而失败。
 */
class StdConfigNode(
    val keyValue: PsiElement,
    val configId: String,
    val methodConfigs: Map<String, PsiElement>,
    val containingFile: PsiFile,
    val baseUrl: String? = null,
    val methodPaths: Map<String, String> = emptyMap()
)

/**
 * `@StdHttpClient` 索引快照。
 *
 * 快照整体随 `PsiModificationTracker` 失效，因此可以直接持有 PSI 元素；
 * 索引未激活（插件关闭、dumb 模式、项目未依赖 starter）时返回 [EMPTY]。
 */
class StdClientIndexSnapshot(
    val active: Boolean,
    val entries: List<StdClientEntry>,
    val byClass: Map<PsiClass, StdClientEntry>,
    val byConfigId: Map<String, List<StdClientEntry>>,
    val configNodesByConfigId: Map<String, List<StdConfigNode>>,
    val apiIdByMethod: Map<PsiMethod, String>
) {

    companion object {
        val EMPTY = StdClientIndexSnapshot(
            active = false,
            entries = emptyList(),
            byClass = emptyMap(),
            byConfigId = emptyMap(),
            configNodesByConfigId = emptyMap(),
            apiIdByMethod = emptyMap()
        )
    }
}
