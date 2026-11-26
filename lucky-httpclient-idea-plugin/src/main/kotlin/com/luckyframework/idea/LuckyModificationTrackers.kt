package com.luckyframework.idea

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.PsiModificationTracker

/**
 * 按内容来源区分的项目级修改跟踪器。
 *
 * 平台没有提供“代码块外修改”计数器：`PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT`
 * 与废弃的 `getJavaStructureModificationTracker()` 都等价于全局计数（任意 PSI 修改）。
 * 插件自己维护的扫描快照因此按内容来源拆分：
 *
 * - [structure]：Java 声明结构（类/成员/注解/参数/文件增删），方法体编辑与
 *   类型文本变化都不递增；
 * - [signature]：[structure] 叠加声明位置的类型文本，逐字面量的 SpEL
 *   求值上下文绑定这一级；
 * - [java]：任意 Java PSI 修改，只用于确实需要跟随每一次编辑的界面刷新；
 * - [configFiles]：YAML / properties / 纯文本配置文件；
 * - 项目根/依赖模型变化由 `ProjectRootModificationTracker` 单独跟踪。
 *
 * 项目级全量扫描必须绑定 [structure]：绑定 [java] 会让每一次击键都触发全项目
 * 重扫，绑定 [signature] 会让每次改参数/返回类型都触发重扫，daemon 高亮排队
 * 时间随之被拉长。
 */
internal object LuckyModificationTrackers {

    /**
     * Java 声明结构修改计数。
     *
     * 快照只关心“有哪些类、成员、注解、参数”，类型文本与方法体编辑不影响
     * 结果，因此不能用 [java]：它对每个字符都递增。
     */
    fun structure(project: Project): ModificationTracker = LuckyStructureTracker.getInstance(project)

    /**
     * 声明结构 + 类型签名计数。
     *
     * 只供逐字面量的 SpEL 求值上下文缓存使用：改参数/返回类型后上下文里的
     * 类型信息必须刷新，但符号表、组件清单与模块依赖判定都不受影响，不能
     * 因此重启全项目扫描。
     */
    fun signature(project: Project): ModificationTracker =
        LuckyStructureTracker.getInstance(project).signature()

    /** 任意 Java 文件修改（含方法体）。 */
    fun java(project: Project): ModificationTracker =
        PsiModificationTracker.getInstance(project).forLanguage(JavaLanguage.INSTANCE)

    /** 配置文件（YAML / properties / 纯文本）修改。 */
    fun configFiles(project: Project): ModificationTracker =
        PsiModificationTracker.getInstance(project).forLanguages(::isConfigLanguage)

    private fun isConfigLanguage(language: Language): Boolean {
        val id = language.id
        return id.equals("yaml", true) ||
            id.equals("properties", true) ||
            id == "TEXT"
    }

    /** 项目根/依赖模型版本。 */
    fun roots(project: Project): Long =
        ProjectRootModificationTracker.getInstance(project).modificationCount
}
