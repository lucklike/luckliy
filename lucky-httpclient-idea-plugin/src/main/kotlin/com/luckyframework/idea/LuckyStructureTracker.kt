package com.luckyframework.idea

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReferenceParameterList
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypeParameterList
import java.util.concurrent.atomic.AtomicLong

/**
 * Java 声明结构与类型签名修改计数。
 *
 * 插件的项目级快照（SpEL 符号表、`@SpelConfig` 模型、`@StdHttpClient` 索引、
 * 模块的框架依赖判定等）只在类、成员、注解、参数和文件增删时才是脏的；
 * 方法体、表达式与注释的编辑不会改变它们的结果。平台的
 * `PsiModificationTracker` 对任意 PSI 修改都会递增（2024.1 的
 * `JAVA_STRUCTURE_MODIFICATION_COUNT` 等价于全局计数，2026.3 已移除），
 * 用它做失效键会让每一次击键都触发全项目重扫，daemon 高亮队列随之被反复
 * 清空。
 *
 * 这里按 PSI 事件自行统计两级计数：
 *
 * - [getModificationCount]：类、成员、注解、参数与文件增删。符号表的键集合
 *   只在这类变化时改变，全项目扫描与模块依赖判定绑定这一级；
 * - [signature]：在上面的基础上额外包含声明位置的类型文本。把参数或返回值
 *   从 `String` 改成 `Integer` 不改变符号表的键集合，但会改变 SpEL 求值上下文
 *   的类型信息，只有逐字面量的上下文缓存需要在这一级失效。
 *
 * 判定只做类型比较，运行在每次 PSI 事件上也不会有可感知成本。
 */
@Service(Service.Level.PROJECT)
internal class LuckyStructureTracker(private val project: Project) : PsiTreeChangeListener, ModificationTracker {

    /** 声明结构计数：类、成员、注解、参数与文件增删。 */
    private val structureCounter = AtomicLong()

    /** 类型签名计数：结构变化叠加声明位置的类型文本变化。 */
    private val signatureCounter = AtomicLong()

    /**
     * 签名计数器的稳定视图。
     *
     * `CachedValue` 按对象引用比较依赖，每次访问都新建 tracker 会让同一个
     * 缓存被平台判定为「依赖已变化」而反复失效，因此返回同一实例。
     */
    private val signatureTracker = object : ModificationTracker {
        override fun getModificationCount(): Long = signatureCounter.get()
    }

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(this, project)
    }

    override fun getModificationCount(): Long = structureCounter.get()

    /** 结构 + 类型变化的签名，供 SpEL 求值上下文缓存使用。 */
    fun signature(): ModificationTracker = signatureTracker

    override fun beforeChildAddition(event: PsiTreeChangeEvent) = Unit

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) = Unit

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) = Unit

    override fun beforeChildMovement(event: PsiTreeChangeEvent) = Unit

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) = Unit

    override fun beforePropertyChange(event: PsiTreeChangeEvent) = Unit

    override fun childAdded(event: PsiTreeChangeEvent) = bumpIfStructural(event)

    override fun childRemoved(event: PsiTreeChangeEvent) = bumpIfStructural(event)

    override fun childReplaced(event: PsiTreeChangeEvent) = bumpIfStructural(event)

    override fun childMoved(event: PsiTreeChangeEvent) = bumpIfStructural(event)

    override fun childrenChanged(event: PsiTreeChangeEvent) = bumpIfStructural(event)

    override fun propertyChanged(event: PsiTreeChangeEvent) {
        if (isStructuralProperty(event.propertyName)) {
            // 文件/目录改名与根变化同时影响符号表键集合与类型签名
            structureCounter.incrementAndGet()
            signatureCounter.incrementAndGet()
        }
    }

    private fun bumpIfStructural(event: PsiTreeChangeEvent) {
        if (isStructuralChange(event)) {
            // 结构变化必然改变类型签名：两个计数器一起递增，
            // 保证签名计数永不落后于结构计数
            structureCounter.incrementAndGet()
            signatureCounter.incrementAndGet()
            return
        }
        if (isDeclarationTypeChange(event) || isSignatureChange(event)) {
            // 只有签名变化：符号表键集合、组件清单与模块依赖判定都不受影响，
            // 只需要让逐字面量的 SpEL 上下文按新类型/新名字重建
            signatureCounter.incrementAndGet()
        }
    }

    /** 声明结构变化：影响符号表、组件清单与模块依赖判定。 */
    private fun isStructuralChange(event: PsiTreeChangeEvent): Boolean =
        isStructuralNodeChange(event) || isStructuralContainer(event.parent)

    /** 变化的节点本身是结构节点，与「仅类型文本变化」区分。 */
    private fun isStructuralNodeChange(event: PsiTreeChangeEvent): Boolean =
        isStructuralElement(event.child) ||
            isStructuralElement(event.newChild) ||
            isStructuralElement(event.oldChild) ||
            isAnnotationValueChange(event)

    /**
     * 参数内部的编辑：改名字、改注解或替换类型。
     *
     * 参数增删在 [isStructuralElement] 里已经算作结构变化（child 是
     * `PsiParameter`）；参数名与参数类型只进入逐字面量的求值上下文，
     * 不影响任何全项目快照。
     */
    private fun isSignatureChange(event: PsiTreeChangeEvent): Boolean = event.parent is PsiParameter

    /** 声明位置的类型文本变化：只影响 SpEL 求值上下文的类型信息。 */
    private fun isDeclarationTypeChange(event: PsiTreeChangeEvent): Boolean =
        isDeclarationTypeElement(event.child) ||
            isDeclarationTypeElement(event.newChild) ||
            isDeclarationTypeElement(event.oldChild) ||
            isDeclarationTypeContainer(event.parent)

    /**
     * 注解属性值的内部编辑。
     *
     * 修改 `@Rar("a")` 的字符串内容时，Java 词法节点在字面量内部替换，
     * `parent` 是 `PsiLiteralExpression` 而不是外层的 `PsiNameValuePair`，
     * 需要再向上看一层才能区分“注解值”与“方法体内的普通字面量”。
     */
    private fun isAnnotationValueChange(event: PsiTreeChangeEvent): Boolean {
        val parent = event.parent ?: return false
        val owner = if (parent is PsiLiteralExpression) parent.parent else parent
        return owner is PsiNameValuePair || owner is PsiArrayInitializerMemberValue
    }

    private fun isStructuralElement(element: PsiElement?): Boolean = when (element) {
        null -> false
        is PsiMember,
        is PsiAnnotation,
        is PsiModifierList,
        is PsiParameter,
        is PsiParameterList,
        is PsiTypeParameter,
        is PsiTypeParameterList,
        is PsiNameValuePair,
        is PsiFile,
        is PsiDirectory -> true
        else -> false
    }

    /**
     * 声明节点作为变化的直接容器时属于结构变化：成员增删、参数列表、
     * 注解属性值、修饰符列表都在这些节点上报告变化。
     *
     * 方法体与表达式编辑的容器是 `PsiCodeBlock`、`PsiExpression` 等，
     * 不会命中下面的类型。
     */
    private fun isStructuralContainer(element: PsiElement?): Boolean = when (element) {
        null -> false
        is PsiClass,
        is PsiMethod,
        is PsiField,
        is PsiAnnotation,
        is PsiNameValuePair,
        is PsiModifierList,
        is PsiParameterList,
        is PsiTypeParameterList -> true
        else -> false
    }

    /** 变化的节点本身就是声明位置的类型节点（整体替换类型）。 */
    private fun isDeclarationTypeElement(element: PsiElement?): Boolean =
        element is PsiTypeElement && isDeclarationType(element)

    /** 变化发生在声明类型节点的内部（编辑器逐字符替换的常见形态）。 */
    private fun isDeclarationTypeContainer(element: PsiElement?): Boolean = when (element) {
        is PsiTypeElement -> isDeclarationType(element)
        is PsiJavaCodeReferenceElement -> isDeclarationType(element.parent)
        is PsiReferenceParameterList -> isDeclarationType(element.parent)
        else -> false
    }

    /**
     * 类型是否位于声明位置：参数、返回值、字段与类型参数。
     *
     * 编辑器里把 `String` 改成 `Integer` 时，文本替换发生在类型引用元素
     * （[PsiJavaCodeReferenceElement]）内部，事件容器会逐层落到引用元素或
     * [PsiTypeElement] 上，不会替换整个类型节点，所以这两层都要判。
     *
     * 这类变化只递增 [signature]：符号表仍以名字为键，但 SpEL 求值上下文里
     * 的参数类型必须刷新。若让它递增结构计数，每次改类型都会触发三份全项目
     * 扫描并在签名比较后重启 daemon，高亮会晚数秒才出现。
     *
     * 方法体内的局部变量、类型转换、`new` 表达式与泛型实参不属于声明位置。
     */
    private fun isDeclarationType(element: PsiElement?): Boolean {
        val type = when (element) {
            is PsiTypeElement -> element
            is PsiJavaCodeReferenceElement -> element.parent as? PsiTypeElement
            is PsiReferenceParameterList -> element.parent as? PsiTypeElement
            else -> null
        } ?: return false
        val owner = type.parent
        return owner is PsiParameter || owner is PsiMethod ||
            owner is PsiField || owner is PsiTypeParameter
    }

    private fun isStructuralProperty(name: String?): Boolean = when (name) {
        PsiTreeChangeEvent.PROP_ROOTS,
        PsiTreeChangeEvent.PROP_FILE_NAME,
        PsiTreeChangeEvent.PROP_DIRECTORY_NAME,
        "name" -> true
        else -> false
    }

    companion object {
        fun getInstance(project: Project): LuckyStructureTracker = project.service()
    }
}
