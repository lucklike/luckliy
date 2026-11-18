package com.luckyframework.idea.std.yaml

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.luckyframework.idea.std.StdConfigWriter
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * `standard-client-configs.<configId>` 的 YAML 写入实现（YAML 插件存在时加载）。
 *
 * 生成的是框架要求的最小化配置：`url` 与 `service` 至少配置一个，这里写入
 * `url` 占位值并附注释。写入采用「文本插入 + 文档提交」：只在该层级末尾
 * 追加文本，不重排用户已有内容，缩进从同层兄弟节点推断。
 */
class YamlStdConfigWriter : StdConfigWriter {

    override fun candidates(project: Project): List<PsiFile> {
        val scope = GlobalSearchScope.projectScope(project)
        val manager = PsiManager.getInstance(project)
        return FileTypeIndex.getFiles(YAMLFileType.YML, scope)
            .asSequence()
            .filter { it.isValid && YamlStdConfigSource.inScope(it) }
            .mapNotNull { manager.findFile(it) }
            .sortedWith(compareBy({ priority(it.name) }, { it.virtualFile?.path.orEmpty() }))
            .toList()
    }

    override fun createDefaultFile(project: Project): PsiFile? {
        var created: PsiFile? = null
        WriteCommandAction.runWriteCommandAction(project, "创建 application.yml", null, {
            val directory = resourcesDirectory(project) ?: projectDirectory(project)
            if (directory != null) {
                val existing = directory.findFile("application.yml")
                    ?: directory.findFile("application.yaml")
                created = existing ?: directory.createFile("application.yml")
            }
        })
        return created
    }

    override fun write(file: PsiFile, configId: String): Boolean {
        val yamlFile = file as? YAMLFile ?: return false
        if (configId.isBlank()) {
            return false
        }
        val project = file.project
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return false
        val plan = planOf(yamlFile, document.text, configId) ?: return false
        WriteCommandAction.runWriteCommandAction(project, "生成 StdHttpClient 配置", null, {
            document.insertString(plan.offset, plan.text)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        })
        return true
    }

    /**
     * 计算插入位置与文本。
     *
     * 逐层查找 `lucky` → `http-client` → `standard-client-configs`：
     * 缺失的层从该位置补齐剩余结构；已存在的层继续下探。`configId` 已存在
     * 或层级值与预期结构不符时返回 null（不覆盖用户配置）。
     */
    private fun planOf(file: YAMLFile, text: String, configId: String): Plan? {
        val eol = if (text.contains("\r\n")) "\r\n" else "\n"
        val documents = file.documents
        if (documents.size > 1) {
            return null
        }
        val root = documents.firstOrNull()?.topLevelValue as? YAMLMapping
        if (root == null) {
            if (documents.any { it.topLevelValue != null }) {
                return null
            }
            val offset = text.length
            val prefix = if (offset == 0 || text.endsWith("\n")) "" else eol
            return Plan(offset, prefix + blockText(KEY_PATH, configId, 0, DEFAULT_STEP, eol))
        }
        var mapping: YAMLMapping = root
        var parent: YAMLKeyValue? = null
        for ((index, key) in KEY_PATH.withIndex()) {
            val keyValue = mapping.getKeyValueByKey(key)
            if (keyValue == null) {
                val step = stepOf(mapping, parent)
                return appendPlan(mapping, parent, KEY_PATH.drop(index), configId, step, text, eol)
            }
            val value = keyValue.value
            if (value == null || value is YAMLScalar && value.text.isBlank()) {
                val indent = indentOf(keyValue.key ?: keyValue) + DEFAULT_STEP
                val block = blockText(KEY_PATH.drop(index + 1), configId, indent, DEFAULT_STEP, eol)
                return Plan(keyValue.textRange.endOffset, eol + block)
            }
            mapping = value as? YAMLMapping ?: return null
            parent = keyValue
        }
        if (mapping.getKeyValueByKey(configId) != null) {
            return null
        }
        return appendPlan(mapping, parent, emptyList(), configId, stepOf(mapping, parent), text, eol)
    }

    /** 在某层 mapping 的末尾追加剩余结构的文本。 */
    private fun appendPlan(
        mapping: YAMLMapping,
        parent: YAMLKeyValue?,
        keys: List<String>,
        configId: String,
        step: Int,
        text: String,
        eol: String
    ): Plan {
        val last = mapping.keyValues.lastOrNull()
        if (last != null) {
            val indent = indentOf(last.key ?: last)
            return Plan(last.textRange.endOffset, eol + blockText(keys, configId, indent, step, eol))
        }
        if (parent == null) {
            return Plan(text.length, blockText(keys, configId, 0, step, eol))
        }
        val indent = indentOf(parent.key ?: parent) + step
        return Plan(parent.textRange.endOffset, eol + blockText(keys, configId, indent, step, eol))
    }

    /**
     * 生成剩余层级 + configId + url 的文本块。
     *
     * [keys] 是缺失的固定段（不含 configId），最后一层是 configId。
     */
    private fun blockText(
        keys: List<String>,
        configId: String,
        indent: Int,
        step: Int,
        eol: String
    ): String {
        val builder = StringBuilder()
        var column = indent
        for (key in keys) {
            builder.append(pad(column)).append(key).append(':').append(eol)
            column += step
        }
        builder.append(pad(column)).append(quoteIfNeeded(configId)).append(':').append(eol)
        column += step
        builder.append(pad(column)).append("# 服务地址，与 service 二选一（至少配置一个）").append(eol)
        builder.append(pad(column)).append("url: https://example.com")
        return builder.toString()
    }

    private fun stepOf(mapping: YAMLMapping, parent: YAMLKeyValue?): Int {
        val child = mapping.keyValues.firstOrNull() ?: return DEFAULT_STEP
        val parentIndent = parent?.let { indentOf(it.key ?: it) } ?: 0
        val step = indentOf(child.key ?: child) - parentIndent
        return if (step in 1..MAX_STEP) step else DEFAULT_STEP
    }

    /** 元素所在行的行首缩进（以空格数计）。 */
    private fun indentOf(element: PsiElement): Int {
        val text = element.containingFile.text
        val start = element.textRange.startOffset
        var index = start - 1
        while (index >= 0 && text[index] != '\n') {
            index--
        }
        return start - index - 1
    }

    private fun pad(column: Int): String = " ".repeat(column)

    private fun quoteIfNeeded(key: String): String =
        if (SAFE_KEY.matches(key)) key else "'" + key.replace("'", "''") + "'"

    private fun priority(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower == "application.yml" || lower == "application.yaml" -> 0
            lower.startsWith("application") -> 1
            lower.startsWith("bootstrap") -> 2
            else -> 3
        }
    }

    private fun resourcesDirectory(project: Project): PsiDirectory? {
        val manager = PsiManager.getInstance(project)
        for (root in ProjectRootManager.getInstance(project).contentRoots) {
            val resources = root.findFileByRelativePath("src/main/resources") ?: continue
            if (!resources.isDirectory) {
                continue
            }
            val directory = manager.findDirectory(resources)
            if (directory != null) {
                return directory
            }
        }
        return null
    }

    private fun projectDirectory(project: Project): PsiDirectory? {
        val basePath = project.basePath ?: return null
        val base = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return null
        return PsiManager.getInstance(project).findDirectory(base)
    }

    /** 插入位置与待插入文本。 */
    private class Plan(val offset: Int, val text: String)

    private companion object {
        val KEY_PATH = listOf("lucky", "http-client", "standard-client-configs")
        val SAFE_KEY = Regex("[A-Za-z0-9_.-]+")
        const val DEFAULT_STEP = 2
        const val MAX_STEP = 8
    }
}
