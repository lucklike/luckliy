package com.luckyframework.idea.spel.spring

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import javax.swing.Icon

/**
 * Spring 插件 Bean 模型的访问入口。
 *
 * 本接口与 [SpringBeanDefinition] 不引用任何 Spring 类型，社区版可以安全
 * 加载；直接调用 Spring API 的实现类 [SpringBeanModelAccessImpl] 只在
 * Spring 插件存在时由平台服务实例化（见 plugin.xml 的 applicationService）。
 */
internal interface SpringBeanModelAccess {

    /**
     * 模块的全部 Spring Bean；null 表示模块没有 Spring 模型。
     *
     * 空列表表示存在 Spring 模型，但其中确实没有可识别的 Bean。
     */
    fun beans(module: Module): List<SpringBeanDefinition>?

    /**
     * Spring 插件对 Bean 声明使用的图标：`@Bean` 方法、组件类、抽象 Bean
     * 各不相同；[beanClass] 为空时退回通用 Bean 图标。
     */
    fun iconOf(beanClass: PsiClass?, declaration: PsiElement?): Icon?
}
