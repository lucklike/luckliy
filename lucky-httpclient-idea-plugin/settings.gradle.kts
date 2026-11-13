pluginManagement {
    repositories {
        // 国内镜像优先，末尾保留官方仓库兜底
        // 说明：Gradle Plugin Portal 在国内访问很慢，阿里云 gradle-plugin 仓库镜像了它的 Maven 布局
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "lucky-httpclient-idea-plugin"
