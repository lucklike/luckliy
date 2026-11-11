package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

/**
 * 从项目依赖的 jar 文件名识别 lucky-httpclient 版本（design D13）。
 *
 * 源码模块没有 jar 版本信息，返回 null，由 [BuiltinSymbolsTable.versionMessage]
 * 给出「未验证版本」提示，但不阻断解析。
 */
object FrameworkVersionDetector {

    private const val HTTP_REQUEST = "com.luckyframework.httpclient.proxy.annotations.HttpRequest"

    private val JAR_VERSION = Regex("""lucky-httpclient-(\d+(?:\.\d+)+)\.jar""", RegexOption.IGNORE_CASE)

    fun detect(project: Project): String? {
        val facade = JavaPsiFacade.getInstance(project)
        val psiClass = facade.findClass(HTTP_REQUEST, GlobalSearchScope.allScope(project)) ?: return null
        val file = psiClass.containingFile?.virtualFile ?: return null
        if (!file.name.endsWith(".jar", ignoreCase = true)) {
            return null
        }
        return parseVersion(file.name)
    }

    fun parseVersion(fileName: String?): String? =
        fileName?.let { JAR_VERSION.find(it)?.groupValues?.get(1) }
}
