package com.luckyframework.idea.debug.container

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

/**
 * 在主源集中定位 `@SpringBootApplication` 入口。
 *
 * 依赖模块的源码也在搜索范围内，多模块项目可能返回多个候选；
 * 排序时把当前模块的主类放前面，调用方默认取第一个。
 */
object BootMainClassLocator {

    const val ANNOTATION = "org.springframework.boot.autoconfigure.SpringBootApplication"

    sealed interface Result {
        data class Found(val mainClass: PsiClass) : Result
        data class Multiple(val candidates: List<PsiClass>) : Result
        data object NotFound : Result
        data object IndexNotReady : Result
    }

    /** 必须在读锁内调用。 */
    fun locate(project: Project, module: Module): Result {
        if (DumbService.isDumb(project)) {
            return Result.IndexNotReady
        }
        val annotation = JavaPsiFacade.getInstance(project)
            .findClass(ANNOTATION, GlobalSearchScope.allScope(project))
            ?: return Result.NotFound
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        val candidates = AnnotatedElementsSearch.searchPsiClasses(annotation, scope)
            .findAll()
            .asSequence()
            .filter { it.isValid && it.qualifiedName != null && hasMainMethod(it) }
            .sortedWith(
                compareBy<PsiClass> { if (ModuleUtilCore.findModuleForPsiElement(it) == module) 0 else 1 }
                    .thenBy { it.qualifiedName }
            )
            .distinctBy { it.qualifiedName }
            .toList()
        return when {
            candidates.isEmpty() -> Result.NotFound
            candidates.size == 1 -> Result.Found(candidates.first())
            else -> Result.Multiple(candidates)
        }
    }

    /** 把定位结果转成可展示的全限定名列表。 */
    fun qualifiedNames(result: Result): List<String> = when (result) {
        is Result.Found -> listOfNotNull(result.mainClass.qualifiedName)
        is Result.Multiple -> result.candidates.mapNotNull { it.qualifiedName }
        else -> emptyList()
    }

    private fun hasMainMethod(psiClass: PsiClass): Boolean =
        psiClass.findMethodsByName("main", false).any {
            it.hasModifierProperty(PsiModifier.PUBLIC) && it.hasModifierProperty(PsiModifier.STATIC)
        }
}
