package com.luckyframework.idea.debug.invoke

import com.luckyframework.idea.debug.container.ContainerLauncher
import com.luckyframework.idea.debug.container.ContainerStartException
import com.luckyframework.idea.debug.container.DebugContainerService
import com.luckyframework.idea.debug.container.DebugLogBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.io.File

/**
 * 真实容器链路验证：DebugCallExecutor + 表单参数 + 子进程 launcher。
 *
 * 依赖一个已解压的 Spring Boot 项目（classes/lib/noann）与 launcher jar，
 * 默认跳过；设置以下环境变量后由 `gradlew test --tests '*DebugCallE2ETest*'` 执行：
 *
 * - LUCKY_DEBUG_E2E=1
 * - LUCKY_DEBUG_E2E_ROOT=<项目 classes/lib/noann 所在目录>
 * - LUCKY_DEBUG_E2E_LAUNCHER=<lucky-debug-launcher.jar>
 * - LUCKY_DEBUG_E2E_JAVA=<java.exe>（可选）
 * - LUCKY_DEBUG_E2E_MAIN=<主类>（可选）
 */
class DebugCallE2ETest {

    @Test
    fun `invokes real container without the web server`() {
        runRealContainer(webServer = false)
    }

    @Test
    fun `invokes real container with the web server`() {
        runRealContainer(webServer = true)
    }

    @Test
    fun `fails fast when the main class does not exist`() {
        Assume.assumeTrue(
            "set LUCKY_DEBUG_E2E=1 to run the real container test",
            System.getenv("LUCKY_DEBUG_E2E") == "1"
        )
        val root = File(requireEnv("LUCKY_DEBUG_E2E_ROOT"))
        val launcherJar = File(requireEnv("LUCKY_DEBUG_E2E_LAUNCHER"))
        val javaExecutable = System.getenv("LUCKY_DEBUG_E2E_JAVA") ?: "java"
        val classpath = listOf(
            File(root, "noann").absolutePath,
            File(root, "classes").absolutePath,
            File(root, "lib/*").absolutePath,
            launcherJar.absolutePath
        )

        val startedAt = System.currentTimeMillis()
        val error = runCatching {
            ContainerLauncher().launch(
                ContainerLauncher.Spec(
                    javaExecutable = javaExecutable,
                    classpath = classpath,
                    mainClass = "org.example.DoesNotExistApplication",
                    launcherJar = launcherJar.toPath(),
                    programArgs = DebugContainerService.programArgsFor(webServer = false),
                    jvmArgs = listOf("--add-exports=java.base/sun.net=ALL-UNNAMED"),
                    workingDirectory = root,
                    readyTimeoutMillis = 30_000
                ),
                DebugLogBuffer()
            ).use { }
        }.exceptionOrNull()
        val elapsedMs = System.currentTimeMillis() - startedAt

        println("e2e missing main class: elapsedMs=$elapsedMs, error=${error?.message}")
        assertTrue("expected a startup failure but was $error", error is ContainerStartException)
        assertTrue("startup failure took ${elapsedMs}ms", elapsedMs < 30_000)
        val message = error?.message.orEmpty()
        assertTrue(
            "expected an actionable missing-class message but was: $message",
            message.contains("找不到类") || message.contains("Build")
        )
    }

    private fun runRealContainer(webServer: Boolean) {
        Assume.assumeTrue(
            "set LUCKY_DEBUG_E2E=1 to run the real container test",
            System.getenv("LUCKY_DEBUG_E2E") == "1"
        )
        val root = File(requireEnv("LUCKY_DEBUG_E2E_ROOT"))
        val launcherJar = File(requireEnv("LUCKY_DEBUG_E2E_LAUNCHER"))
        val javaExecutable = System.getenv("LUCKY_DEBUG_E2E_JAVA") ?: "java"
        val mainClass = System.getenv("LUCKY_DEBUG_E2E_MAIN")
            ?: "org.example.NoAnnotationApplication"

        val classpath = listOf(
            File(root, "noann").absolutePath,
            File(root, "classes").absolutePath,
            File(root, "lib/*").absolutePath,
            launcherJar.absolutePath
        )
        val launcher = ContainerLauncher()
        launcher.launch(
            ContainerLauncher.Spec(
                javaExecutable = javaExecutable,
                classpath = classpath,
                mainClass = mainClass,
                launcherJar = launcherJar.toPath(),
                programArgs = DebugContainerService.programArgsFor(webServer),
                // demo 项目的 RetryConfiguration 直接访问 sun.net，JDK 17 需要显式导出。
                jvmArgs = listOf("--add-exports=java.base/sun.net=ALL-UNNAMED"),
                workingDirectory = root
            ),
            DebugLogBuffer()
        ).use { started ->
            val executor = DebugCallExecutor { request, timeout ->
                started.client.request(request, timeout)
            }
            val target = DebugTarget(
                moduleName = "e2e",
                className = "org.example.api.cache.RedisCacheTestApi",
                methodName = "getToken",
                parameters = listOf(
                    DebugParameter(0, "token", "java.lang.String", DebugParameterKind.STRING)
                ),
                signature = "getToken(java.lang.String)"
            )

            val outcome = executor.invoke(target, mapOf("token" to "u1"))

            assertTrue("expected success but was $outcome", outcome is DebugCallOutcome.Success)
            outcome as DebugCallOutcome.Success
            println(
                "e2e outcome: kind=${outcome.kind}, returnType=${outcome.returnType}, " +
                    "elapsedMs=${outcome.elapsedMs}"
            )
            println("e2e body: ${outcome.body.replace('\n', ' ')}")
            assertEquals("json", outcome.kind)
            assertEquals("org.example.api.cache.Token", outcome.returnType)
            assertTrue(outcome.body.contains("\"token\""))
            assertTrue(outcome.body.contains("\"expire\""))
            assertTrue(outcome.elapsedMs >= 0)

            val logs = started.logs.snapshot()
            val tomcatLines = logs.filter { it.contains("Tomcat", ignoreCase = true) }
            println("e2e webServer=$webServer, tomcatLogLines=${tomcatLines.size}")
            if (webServer) {
                assertTrue("expected Tomcat startup logs but was $logs", tomcatLines.isNotEmpty())
            } else {
                assertTrue("expected no Tomcat startup logs but was $tomcatLines", tomcatLines.isEmpty())
            }
        }
    }

    private fun requireEnv(name: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("missing environment variable $name")
}
