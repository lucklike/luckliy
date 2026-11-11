package com.luckyframework.idea.spel.evaluator

/**
 * 受控求值白名单（design D11，spec 15.2）。
 *
 * 只允许纯计算类型：`java.lang`（排除 Runtime/System/Thread/Class）、
 * `java.util`、`java.time`、`java.math`、`java.text` 与 `luckyframework`。
 * 文件、网络、进程、反射与脚本相关的类型和方法一律拒绝。
 *
 * 这是「开发者主动触发」场景下的防护网，不是对抗恶意表达式的安全边界；
 * 真正的兜底是超时（见 `SpelEvaluator`）。
 */
object EvaluatorPolicy {

    /** 允许的包前缀；`java.lang` 下的危险类由 [DENIED_TYPE_PREFIXES] 覆盖。 */
    private val ALLOWED_TYPE_PREFIXES = listOf(
        "java.lang.",
        "java.util.",
        "java.time.",
        "java.math.",
        "java.text.",
        "com.luckyframework."
    )

    /** 危险类型前缀，优先级高于白名单。 */
    private val DENIED_TYPE_PREFIXES = listOf(
        "java.io.",
        "java.nio.file.",
        "java.net.",
        "java.lang.reflect.",
        "java.lang.invoke.",
        "javax.script.",
        "java.lang.Class",
        "java.lang.ClassLoader",
        "java.lang.Process",
        "java.lang.Runtime",
        "java.lang.System",
        "java.lang.Thread"
    )

    /** 任意类上都不允许调用的方法名。 */
    private val DENIED_METHOD_NAMES = setOf(
        "exec",
        "exit",
        "halt",
        "load",
        "loadLibrary",
        "setAccessible",
        "newInstance",
        "invoke"
    )

    /** 这些类型上的任何方法都拒绝，即使类型本身通过了前缀检查。 */
    private val DENIED_METHOD_CLASSES = listOf(
        "java.io.",
        "java.nio.file.",
        "java.net.",
        "java.lang.reflect.",
        "java.lang.invoke.",
        "javax.script.",
        "java.lang.Class",
        "java.lang.ClassLoader",
        "java.lang.Process",
        "java.lang.Runtime",
        "java.lang.System",
        "java.lang.Thread"
    )

    fun isTypeAllowed(qualifiedName: String): Boolean {
        val name = qualifiedName.trim()
        if (name.isEmpty()) {
            return false
        }
        if (DENIED_TYPE_PREFIXES.any { name.startsWith(it) }) {
            return false
        }
        return ALLOWED_TYPE_PREFIXES.any { name.startsWith(it) }
    }

    fun isMethodAllowed(declaringClass: String, methodName: String): Boolean {
        if (methodName in DENIED_METHOD_NAMES) {
            return false
        }
        return DENIED_METHOD_CLASSES.none { declaringClass.startsWith(it) }
    }

    /** 类型被拒绝时的用户提示，按访问面归类为文件/网络/进程/反射。 */
    fun typeDenialMessage(qualifiedName: String): String = when {
        qualifiedName.startsWith("java.io.") || qualifiedName.startsWith("java.nio.file.") ->
            "文件访问被禁止"

        qualifiedName.startsWith("java.net.") -> "网络访问被禁止"
        qualifiedName.startsWith("java.lang.Runtime") ||
            qualifiedName.startsWith("java.lang.Process") -> "进程操作被禁止"

        qualifiedName.startsWith("java.lang.reflect.") ||
            qualifiedName.startsWith("java.lang.invoke.") -> "反射操作被禁止"

        else -> "类型 $qualifiedName 不在求值白名单中"
    }

    /** 方法被拒绝时的用户提示。 */
    fun methodDenialMessage(declaringClass: String, methodName: String): String = when {
        declaringClass.startsWith("java.io.") || declaringClass.startsWith("java.nio.file.") ->
            "文件访问被禁止"

        declaringClass.startsWith("java.net.") -> "网络访问被禁止"
        declaringClass.startsWith("java.lang.Runtime") ||
            declaringClass.startsWith("java.lang.Process") -> "进程操作被禁止"

        declaringClass.startsWith("java.lang.reflect.") ||
            declaringClass.startsWith("java.lang.invoke.") -> "反射操作被禁止"

        else -> "方法 $declaringClass.$methodName 不在求值白名单中"
    }
}
