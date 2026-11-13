package com.luckyframework.idea.spel.completion

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.metadata.SpelLifecycle
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind

/**
 * 选择/投影表达式的结果类型与 `#this` 元素上下文（Spring 5.3 语义）。
 */
class SpelTypeResolverTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        addJdkStubs()
        myFixture.addFileToProject(
            "com/example/User.java",
            """
            package com.example;

            public class User {

                private String name;

                private int age;

                public String getName() {
                    return name;
                }

                public Integer getAge() {
                    return age;
                }

                public String getNickName() {
                    return name;
                }

                public boolean isActive() {
                    return true;
                }
            }
            """.trimIndent()
        )
    }

    /** MockJdk 不含 java.lang/java.util 类，补齐选择/投影推导用到的类型。 */
    private fun addJdkStubs() {
        myFixture.addFileToProject(
            "java/lang/String.java",
            """
            package java.lang;

            public final class String {

                public int length() {
                    return 0;
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "java/lang/Integer.java",
            """
            package java.lang;

            public final class Integer {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "java/lang/Iterable.java",
            """
            package java.lang;

            public interface Iterable<T> {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "java/util/Collection.java",
            """
            package java.util;

            public interface Collection<E> extends Iterable<E> {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "java/util/List.java",
            """
            package java.util;

            public interface List<E> extends Collection<E> {

                E get(int index);

                int size();
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "java/util/Map.java",
            """
            package java.util;

            public interface Map<K, V> {

                V get(Object key);

                interface Entry<K, V> {

                    K getKey();

                    V getValue();
                }
            }
            """.trimIndent()
        )
    }

    /** 嵌套选择/投影的容器类型：`Group.members` 是 `List<User>`。 */
    private fun addGroupStub() {
        myFixture.addFileToProject(
            "com/example/Group.java",
            """
            package com.example;

            import java.util.List;

            public class Group {

                private List<User> members;

                public List<User> getMembers() {
                    return members;
                }
            }
            """.trimIndent()
        )
    }

    fun testListSelectionAndProjection() {
        val resolver = resolverWith("users" to listOf("com.example.User"))

        assertEquals("List<User>", textOf(resolver, "#users.?[age > 1]"))
        assertEquals("User", textOf(resolver, "#users.^[age > 1]"))
        assertEquals("User", textOf(resolver, "#users.$[age > 1]"))
        assertEquals("List<String>", textOf(resolver, "#users.![name]"))
    }

    fun testSafeNavigationSelectionAndProjection() {
        val resolver = resolverWith("users" to listOf("com.example.User"))

        assertEquals("List<User>", textOf(resolver, "#users?.?[age > 1]"))
        assertEquals("List<String>", textOf(resolver, "#users?.![name]"))
        assertEquals("List<String>", textOf(resolver, "#users?.?[age > 1].![name]"))
    }

    fun testMapSelectionAndProjection() {
        val resolver = resolverWith(
            "scores" to mapOf("java.lang.String", "java.lang.Integer")
        )

        assertEquals("Map<String, Integer>", textOf(resolver, "#scores.?[value > 1]"))
        assertEquals("Map<String, Integer>", textOf(resolver, "#scores.^[value > 1]"))
        assertEquals("Map<String, Integer>", textOf(resolver, "#scores.$[value > 1]"))
        assertEquals("List<Integer>", textOf(resolver, "#scores.![value]"))
        assertEquals("List<String>", textOf(resolver, "#scores.![key]"))
    }

    fun testArraySelectionAndProjection() {
        val resolver = resolverWith("items" to arrayOf("com.example.User"))

        assertEquals("User[]", textOf(resolver, "#items.?[age > 1]"))
        assertEquals("User", textOf(resolver, "#items.^[age > 1]"))
        assertEquals("User", textOf(resolver, "#items.$[age > 1]"))
        assertEquals("String[]", textOf(resolver, "#items.![name]"))
    }

    fun testThisElementContext() {
        val resolver = SpelTypeResolver(project, SpelContext.unknown())
        val user = classType("com.example.User")

        assertEquals("User", textOf(resolver, "#this", user))
        assertEquals("String", textOf(resolver, "#this.name", user))
        assertEquals("String", textOf(resolver, "name", user))
        assertEquals("Integer", textOf(resolver, "getAge()", user))
    }

    /** 元素上下文中的裸标识符是元素成员，不回退到 Root 变量。 */
    fun testBareIdentifierDoesNotFallBackToRootVariable() {
        val resolver = SpelTypeResolver(project, SpelContext.unknown())
        val user = classType("com.example.User")

        assertNull(textOf(resolver, "nationality", user))
        assertNull(textOf(resolver, "missing()", user))
    }

    /** 属性写法找不到字段时回退到 getter。 */
    fun testPropertyFallsBackToGetter() {
        val resolver = SpelTypeResolver(project, SpelContext.unknown())
        val user = classType("com.example.User")

        assertEquals("String", textOf(resolver, "nickName", user))
        assertEquals("boolean", textOf(resolver, "active", user))
    }

    /** 投影内部的 `#this` 与裸标识符使用集合元素类型。 */
    fun testProjectionUsesElementContext() {
        val resolver = resolverWith("users" to listOf("com.example.User"))

        assertEquals("List<String>", textOf(resolver, "#users.![#this.name]"))
        assertEquals("List<String>", textOf(resolver, "#users.![getName()]"))
    }

    /** 嵌套选择/投影逐层推导：`List<User>` 的 `tags` 为 `List<Tag>`。 */
    fun testNestedSelectionKeepsElementContext() {
        addGroupStub()
        myFixture.addFileToProject(
            "com/example/Tagged.java",
            """
            package com.example;

            import java.util.List;

            public class Tagged {

                private List<String> tags;

                public List<String> getTags() {
                    return tags;
                }
            }
            """.trimIndent()
        )
        val resolver = resolverWith("group" to classType("com.example.Group"))

        assertEquals("List<User>", textOf(resolver, "#group.members.?[age > 1]"))
        assertEquals("List<User>", textOf(resolver, "#group.members.![#this]"))
        assertEquals(
            "List<String>",
            textOf(resolverWith("tagged" to classType("com.example.Tagged")), "#tagged.tags.?[length() > 1]")
        )
        assertEquals(
            "List<Integer>",
            textOf(resolverWith("tagged" to classType("com.example.Tagged")), "#tagged.tags.![length()]")
        )
    }

    /** 标记前有空白（`. ?[`/`. ![`）时仍按选择/投影表达式推导。 */
    fun testSpaceBeforeMarkerIsStillSelection() {
        val resolver = resolverWith("users" to listOf("com.example.User"))

        assertEquals("List<User>", textOf(resolver, "#users. ?[age > 1]"))
        assertEquals("User", textOf(resolver, "#users. ^[age > 1]"))
        assertEquals("User", textOf(resolver, "#users. $[age > 1]"))
        assertEquals("List<String>", textOf(resolver, "#users. ![name]"))
        assertEquals("List<User>", textOf(resolver, "#users. ?[age > 1]. ![#this]"))
    }

    /** 嵌套投影结果是两层集合；内层标记前的空白同样成立。 */
    fun testNestedProjectionKeepsTwoLevels() {
        addGroupStub()
        val resolver = resolverWith("groups" to listOf("com.example.Group"))

        assertEquals("List<List<String>>", textOf(resolver, "#groups.![members.![name]]"))
        assertEquals("List<List<String>>", textOf(resolver, "#groups. ![members. ![name]]"))
    }

    /** `$body$[0]` 是下标访问，不是 `$[` 选择表达式。 */
    fun testResponseVariableIndexIsNotSelection() {
        val resolver = resolverWith("\$body\$" to listOf("com.example.User"))

        assertEquals("User", textOf(resolver, "\$body\$[0]"))
    }

    /** 布尔字面量可直接推导为 boolean，供过滤条件检查放行。 */
    fun testBooleanLiteralType() {
        val resolver = SpelTypeResolver(project, SpelContext.unknown())

        assertEquals("boolean", textOf(resolver, "true"))
        assertEquals("boolean", textOf(resolver, "false"))
    }

    /** 只有类型文本的符号保留数组维度，`length` 推导为 int。 */
    fun testSymbolTypeTextKeepsArrayDimensions() {
        val context = SpelContext(
            known = true,
            stage = SpelLifecycle.REQUEST_INIT,
            attribute = null,
            symbols = listOf(
                SpelSymbol(name = "names", kind = SpelSymbolKind.VARIABLE, type = "String[]"),
                SpelSymbol(name = "bytes", kind = SpelSymbolKind.VARIABLE, type = "byte[]")
            )
        )
        val resolver = SpelTypeResolver(project, context)

        assertEquals("String[]", textOf(resolver, "#names"))
        assertEquals("byte[]", textOf(resolver, "#bytes"))
        assertEquals("int", textOf(resolver, "#names.length"))
        assertEquals("int", textOf(resolver, "#bytes.length"))
    }

    private fun resolverWith(vararg symbols: Pair<String, PsiType>): SpelTypeResolver =
        SpelTypeResolver(project, contextOf(*symbols))

    private fun contextOf(vararg symbols: Pair<String, PsiType>): SpelContext =
        SpelContext(
            known = true,
            stage = SpelLifecycle.REQUEST_INIT,
            attribute = null,
            symbols = symbols.map { (name, type) ->
                SpelSymbol(
                    name = name,
                    kind = SpelSymbolKind.VARIABLE,
                    resolvedType = type
                )
            }
        )

    private fun textOf(
        resolver: SpelTypeResolver,
        expression: String,
        elementType: PsiType? = null
    ): String? = resolver.resolvePsiType(expression, elementType)?.presentableText

    private val factory: PsiElementFactory
        get() = JavaPsiFacade.getElementFactory(project)

    private fun classType(qualifiedName: String): PsiType =
        factory.createType(psiClass(qualifiedName))

    private fun listOf(element: String): PsiType =
        factory.createType(psiClass("java.util.List"), classType(element))

    private fun mapOf(key: String, value: String): PsiType =
        factory.createType(
            psiClass("java.util.Map"),
            classType(key),
            classType(value)
        )

    private fun arrayOf(component: String): PsiType =
        PsiArrayType(classType(component))

    private fun psiClass(qualifiedName: String): PsiClass =
        JavaPsiFacade.getInstance(project)
            .findClass(qualifiedName, GlobalSearchScope.allScope(project))
            ?: error("未找到测试类: $qualifiedName")
}
