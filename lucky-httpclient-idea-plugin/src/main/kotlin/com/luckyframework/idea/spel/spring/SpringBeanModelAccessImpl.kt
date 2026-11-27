package com.luckyframework.idea.spel.spring

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.spring.SpringApiIcons
import com.intellij.spring.SpringManager
import com.intellij.spring.model.SpringBeanPointer
import javax.swing.Icon

/**
 * Spring Bean 模型的直接 API 实现（可选 Ultimate 能力）。
 *
 * 本类静态引用 `com.intellij.spring.*`，只由平台服务在 Spring 插件存在时
 * 加载：社区版没有该插件，[SpringBeanModelBridge] 不会请求本服务，
 * 因此不会触发 [NoClassDefFoundError]。
 *
 * 数据来自官方模型：`@Bean` 方法名、`@Bean("name")`/`@Component("name")`
 * 自定义名、别名集合与 XML Bean 都能直接识别，类型与声明 PSI 也是模型
 * 计算好的结果，不再通过反射逐项读取。
 */
internal class SpringBeanModelAccessImpl : SpringBeanModelAccess {

    override fun beans(module: Module): List<SpringBeanDefinition>? {
        val model = SpringManager.getInstance(module.project).getCombinedModel(module)
            ?: return null
        val pointers = model.allCommonBeans
        val result = ArrayList<SpringBeanDefinition>(pointers.size)
        for (pointer in pointers) {
            definitionOf(pointer)?.let { result += it }
        }
        return result
    }

    private fun definitionOf(pointer: SpringBeanPointer<*>): SpringBeanDefinition? {
        val name = pointer.name ?: return null
        if (name.isBlank()) {
            return null
        }
        val aliases = pointer.aliases
            .filter { it.isNotBlank() && it != name }
        val beanClass = pointer.beanClass
        val declaration = pointer.psiElement
        return SpringBeanDefinition(
            name = name,
            aliases = aliases,
            typeText = beanClass?.qualifiedName ?: beanClass?.name,
            beanClass = beanClass,
            declaration = declaration,
            fileName = declaration?.containingFile?.name,
            icon = iconOf(beanClass, declaration)
        )
    }

    /** 与 IDEA 原生 Spring 补全一致：@Bean 方法、组件类、抽象 Bean 使用不同图标。 */
    override fun iconOf(beanClass: PsiClass?, declaration: PsiElement?): Icon = when {
        declaration is PsiMethod -> SpringApiIcons.FactoryMethodBean
        beanClass == null -> SpringApiIcons.SpringBean
        beanClass.isInterface || beanClass.hasModifierProperty(PsiModifier.ABSTRACT) ->
            SpringApiIcons.AbstractBean
        else -> SpringApiIcons.SpringJavaBean
    }
}
