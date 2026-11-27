import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.luckyframework"
version = "0.1.0"

repositories {
    // 国内镜像优先（Maven Central 直连约 150KB/s，阿里云约 2.7MB/s），官方仓库兜底
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
    intellijPlatform {
        // 不走阿里云：JetBrains 的 cache-redirector 在国内实测更快（约 7MB/s），
        // 且 IntelliJ Platform 制品本身也没有国内镜像
        defaultRepositories()
    }
}

// 调试 launcher 的编译期依赖：只参与编译，不进插件产物。
// 运行期由用户项目 classpath 提供 Spring/Jackson，
// 因此 launcher 用 Java 8 字节码编译，兼容用户项目的 JDK 与 Boot 版本。
// 注意不要命名为 launcherCompileOnly：sourceSets.create("launcher") 会自动生成同名配置。
val debugLauncherCompileOnly: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        // Spring Bean 模型 API 仅参与编译：运行时通过 optional 依赖接入，
        // 社区版不会加载直接引用这些 API 的实现类。
        // 使用 241 分支中可用的版本，兼容 sinceBuild=241。
        //
        // 不引入 com.intellij.spring.boot：它的 contributor 在 241~243 没有
        // processAllKeys，且 MetaConfigKey 来自 Ultimate 平台的 microservices
        // 插件（不在 marketplace 发布），社区版编译 classpath 无法获得这些类型，
        // Spring Boot 桥接只能在运行时反射探测。
        plugin("com.intellij.spring:241.15989.150")
        // IntelliJ Platform 2024.1 起 Java PSI 位于独立捆绑插件，需要显式声明
        bundledPlugin("com.intellij.java")
        // Properties PSI（配置键反向跳转与重命名）为可选能力：仅在安装了
        // Properties 插件（IDEA 自带并默认启用）时加载对应扩展
        bundledPlugin("com.intellij.properties")
        // YAML 支持为可选能力：仅在安装了 YAML 插件时加载配置侧扩展
        bundledPlugin("org.jetbrains.plugins.yaml")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
    // 仅测试用：验证受控求值对真实 spring-expression 的反射调用。
    // 插件产物不打包 Spring，运行期从用户项目 classpath 加载。
    testImplementation("org.springframework:spring-expression:6.2.7")

    // launcher 只需要 @Configuration/@Import 与 ImportSelector，版本取 Java 8 兼容的 5.3.x；
    // 二进制兼容 Spring 6，运行期以用户 classpath 上的版本为准
    debugLauncherCompileOnly("org.springframework:spring-context:5.3.39")
    debugLauncherCompileOnly("com.fasterxml.jackson.core:jackson-databind:2.13.5")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// launcher 源码独立于插件源码，避免其 Java 8 字节码与插件的 17 字节码相互影响
sourceSets {
    create("launcher") {
        java.srcDir("launcher/src/main/java")
        compileClasspath += debugLauncherCompileOnly
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // IntelliJ Platform 2024.1 运行在 JBR 17 上，字节码必须保持 17
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.withType<JavaCompile>().configureEach {
    if (name != "compileLauncherJava") {
        options.release.set(17)
    }
    options.encoding = "UTF-8"
}

// launcher 运行在用户项目的 JDK 上，字节码必须保持 Java 8
tasks.named<JavaCompile>("compileLauncherJava") {
    options.release.set(8)
}

// 只打包 launcher 自己的类；Spring/Jackson 由用户 classpath 提供。
// 只取 classesDirs：sourceSet 的 resources 输出会被 IntelliJ Platform 插件
// 写入 plugin.xml，不能带进子进程的 classpath
val launcherJar by tasks.registering(Jar::class) {
    archiveFileName.set("lucky-debug-launcher.jar")
    destinationDirectory.set(layout.buildDirectory.dir("launcher"))
    from(sourceSets["launcher"].output.classesDirs)
}

// 以资源形式随插件分发，运行期解压到临时目录后加入子进程 classpath
tasks.named<ProcessResources>("processResources") {
    dependsOn(launcherJar)
    from(launcherJar) {
        into("launcher")
    }
}

intellijPlatform {
    instrumentCode.set(false)
    buildSearchableOptions.set(false)

    pluginConfiguration {
        id = "com.luckyframework.idea.httpclient"
        name = "Lucky HttpClient"
        version = "0.1.0"
        ideaVersion {
            sinceBuild = "241"
            // 设计明确不设上限，避免平台每年升级都要发版
            untilBuild = provider { null }
        }
    }
}

tasks.test {
    useJUnit()
    systemProperty("file.encoding", "UTF-8")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// ===== 完整版（Ultimate）沙箱 =====
// 直接使用本机安装的 IntelliJ IDEA 启动一个隔离沙箱，插件由 prepareSandbox 同步进去，
// 不再需要在真实 IDE 里反复安装/卸载插件。
//
// 用法：
//   ./gradlew runIdeFull
//   ./gradlew runIdeFull -PidePath="D:/.../IntelliJ IDEA 2026.3" -PideProject="D:/Lucklike/ldemo"
//
// 说明：
//   - 每次启动前会自动编译并同步插件，改完代码只需关掉沙箱 IDE 重新执行 runIdeFull；
//   - 编译/测试平台仍是 IC 2024.1，不受本机 IDE 版本影响；插件字节码为 17，可直接运行在 2026.3 上；
//   - 只同步本插件，不带 Spring 241（完整版自带 Spring 插件，避免版本冲突）；
//   - 沙箱配置存放在 .intellijPlatform/sandbox-full，首次启动会复用本机 IDEA 的许可证文件。
val idePath = providers.gradleProperty("idePath")
    .orElse(providers.environmentVariable("IDEA_HOME"))
    .orElse("D:/Application/IDEA-2026.3/IntelliJ IDEA 2026.3")
val ideProject = providers.gradleProperty("ideProject")
val ultimateSandbox = layout.projectDirectory.dir(".intellijPlatform/sandbox-full")
val prepareSandboxTask = tasks.named<PrepareSandboxTask>("prepareSandbox")

val syncUltimateSandbox by tasks.registering(Sync::class) {
    group = "intellij platform"
    description = "将当前插件同步到完整版 IDEA 沙箱"
    dependsOn(prepareSandboxTask)
    from(prepareSandboxTask.map { File(it.sandboxPluginsDirectory.get().asFile, it.pluginName.get()) }) {
        into(prepareSandboxTask.map { it.pluginName.get() })
    }
    into(ultimateSandbox.dir("config/plugins"))
}

val prepareUltimateLicense by tasks.registering {
    group = "intellij platform"
    description = "复用本机 IDEA 的许可证，避免沙箱每次启动都要重新登录"
    outputs.upToDateWhen { false }
    doLast {
        val ideHome = File(idePath.get())
        val dataDirectoryName = runCatching {
            (JsonSlurper().parse(File(ideHome, "product-info.json")) as Map<*, *>)["dataDirectoryName"]
        }.getOrNull() as? String ?: return@doLast
        val source = when {
            Os.isFamily(Os.FAMILY_WINDOWS) ->
                File(System.getenv("APPDATA") ?: return@doLast, "JetBrains/$dataDirectoryName")
            Os.isFamily(Os.FAMILY_MAC) ->
                File(System.getProperty("user.home"), "Library/Application Support/JetBrains/$dataDirectoryName")
            else ->
                File(System.getProperty("user.home"), ".config/JetBrains/$dataDirectoryName")
        }
        val target = ultimateSandbox.dir("config").asFile
        for (name in listOf("idea.key", "options/other.xml")) {
            val from = File(source, name)
            val to = File(target, name)
            if (from.isFile && !to.exists()) {
                to.parentFile.mkdirs()
                from.copyTo(to)
            }
        }
    }
}

val runIdeFull by tasks.registering(Exec::class) {
    group = "intellij platform"
    description = "在本机安装的完整版 IDEA 沙箱中启动插件（默认 2026.3）"
    dependsOn(syncUltimateSandbox, prepareUltimateLicense)
    isIgnoreExitValue = true
    doFirst {
        val ideHome = File(idePath.get())
        val executable = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            File(ideHome, "bin/idea64.exe")
        } else {
            File(ideHome, "bin/idea.sh")
        }
        if (!executable.isFile) {
            throw GradleException(
                "找不到 IDEA 启动器: ${executable.absolutePath}，" +
                    "请通过 gradle.properties 的 idePath 或 -PidePath=<IDE 安装目录> 指定"
            )
        }
        val args = mutableListOf(
            "-Didea.config.path=${ultimateSandbox.dir("config").asFile.absolutePath}",
            // 自定义 config 路径时必须一并显式声明插件目录，否则平台会提示
            // “please set 'idea.plugins.path' property, too” 并去推断旧布局
            "-Didea.plugins.path=${ultimateSandbox.dir("config/plugins").asFile.absolutePath}",
            "-Didea.system.path=${ultimateSandbox.dir("system").asFile.absolutePath}",
            "-Didea.log.path=${ultimateSandbox.dir("log").asFile.absolutePath}",
            // 沙箱的 WARNING 及以上日志同步输出到启动它的命令行窗口；idea.log 照常落盘
            "-Didea.log.console=true",
            "-Didea.is.internal=true",
            "-Didea.auto.reload.plugins=true",
            "-Didea.ignore.disabled.plugins=true",
            // 沙箱插件由 Gradle 同步而来，平台无法识别安装来源，会弹出
            // “Cannot update plugin” 通知；开发沙箱不需要插件更新来源功能
            "-Dplatform.enable.plugin.update.source.feature=false"
        )
        ideProject.orNull?.takeIf { it.isNotBlank() }?.let { args += it }
        commandLine(listOf(executable.absolutePath) + args)
    }
}
