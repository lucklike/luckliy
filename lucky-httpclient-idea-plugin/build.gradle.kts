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

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        // IntelliJ Platform 2024.1 起 Java PSI 位于独立捆绑插件，需要显式声明
        bundledPlugin("com.intellij.java")
        // YAML 支持为可选能力：仅在安装了 YAML 插件时加载配置侧扩展
        bundledPlugin("org.jetbrains.plugins.yaml")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
    // 仅测试用：验证受控求值对真实 spring-expression 的反射调用。
    // 插件产物不打包 Spring，运行期从用户项目 classpath 加载。
    testImplementation("org.springframework:spring-expression:6.2.7")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
    options.release.set(17)
    options.encoding = "UTF-8"
}

intellijPlatform {
    instrumentCode.set(false)
    buildSearchableOptions.set(false)

    pluginConfiguration {
        id = "com.luckyframework.httpclient.spel"
        name = "Lucky HttpClient SpEL"
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
