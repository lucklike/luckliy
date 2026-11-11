package com.luckyframework.idea.spel.symbol

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * 7.2 内置符号表与框架源码常量比对（Golden）。
 */
class BuiltinSymbolGoldenTest : BasePlatformTestCase() {

    private val frameworkSpelDir = File(
        "../lucky-httpclient/src/main/java/com/luckyframework/httpclient/proxy/spel"
    )

    fun testRootVariablesMatchFrameworkConstants() {
        assertConstantsMatch("InternalRootVarName.java", BuiltinSymbolsLoader.load().rootVariables)
    }

    fun testVariablesMatchFrameworkConstants() {
        assertConstantsMatch("InternalVarName.java", BuiltinSymbolsLoader.load().variables)
    }

    fun testRegisteredFunctionOwnersMatchRuntimeImports() {
        val table = BuiltinSymbolsLoader.load()
        val registeredOwners = table.functions.filter { it.registered }.map { it.owner }.toSet()

        assertEquals(
            setOf(
                "com.luckyframework.httpclient.proxy.function.CommonFunctions",
                "com.luckyframework.httpclient.proxy.function.SerializationFunctions",
                "com.luckyframework.httpclient.proxy.function.ResourceFunctions",
                "com.luckyframework.httpclient.proxy.function.RandomFunctions",
                "com.luckyframework.httpclient.proxy.function.DigestFunctions",
                "com.luckyframework.httpclient.proxy.function.MacFunctions",
                "com.luckyframework.httpclient.proxy.function.CipherFunctions"
            ),
            registeredOwners
        )
        // 未在运行期注册的类保留在表中，供 @SpELImport 场景参考
        assertTrue(table.functions.any { !it.registered })
    }

    fun testKeyFunctionsMatchFrameworkSource() {
        val functions = BuiltinSymbolsLoader.load().functions

        val time = functions.first { it.name == "time" }
        assertEquals("time", time.method)
        assertEquals("CommonFunctions", time.owner.substringAfterLast('.'))
        assertEquals("long", time.returnType)

        val md5 = functions.first { it.name == "md5" }
        assertEquals("md5", md5.method)
        assertEquals("DigestFunctions", md5.owner.substringAfterLast('.'))
        assertEquals("byte[]", md5.returnType)

        val print = functions.first { it.name == "print" }
        assertEquals("print", print.method)
        assertEquals("CommonFunctions", print.owner.substringAfterLast('.'))

        // 所有函数都用别名或方法名作为 SpEL 名称，且带签名
        assertTrue(functions.all { it.name.isNotBlank() && it.signature.startsWith(it.name + "(") })
        assertTrue(functions.all { it.returnType.isNotBlank() })
    }

    private fun assertConstantsMatch(fileName: String, variables: List<BuiltinVariable>) {
        val file = File(frameworkSpelDir, fileName)
        assertTrue("找不到框架源码: ${file.absolutePath}", file.exists())

        val expected = LinkedHashMap<String, String>()
        val pattern = Regex("""public\s+(?:static\s+final|final\s+static)\s+String\s+([\w$]+)\s*=\s*"([^"]*)"\s*;""")
        for (match in pattern.findAll(file.readText())) {
            expected[match.groupValues[1]] = match.groupValues[2]
        }

        assertFalse(expected.isEmpty())
        assertEquals(expected.size, variables.size)
        for (variable in variables) {
            assertEquals(
                "常量名不一致: ${variable.name}",
                variable.name,
                expected[variable.constant]
            )
        }
    }
}
