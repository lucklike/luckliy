import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
