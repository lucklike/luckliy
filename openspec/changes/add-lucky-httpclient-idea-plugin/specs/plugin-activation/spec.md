## Purpose

定义插件在何种项目与 IDE 环境下启用、如何隔离未使用 lucky-httpclient 的项目，以及用户可用的全局开关；这是所有 SpEL 编辑器能力的前置契约。

## ADDED Requirements

### Requirement: 依赖感知激活
插件 SHALL 仅在当前项目能够解析到 `lucky-httpclient` 依赖（源码模块或二进制依赖）时启用 SpEL 编辑器能力；无法解析时 MUST 保持全部能力关闭。

#### Scenario: 依赖存在时启用
- **WHEN** 项目包含 `com.luckyframework:lucky-httpclient` 依赖或对应源码模块
- **THEN** 打开 Java 文件时，注解字符串中的 `#{}` 表达式获得高亮、补全与跳转能力

#### Scenario: 依赖缺失时零干扰
- **WHEN** 项目不包含 `lucky-httpclient` 依赖
- **THEN** `#{}` 字符串不产生任何高亮、补全、检查或跳转行为

### Requirement: 多模块项目按模块隔离
多模块项目中，插件 SHALL 以模块是否依赖 `lucky-httpclient` 判定该模块的 SpEL 能力；未依赖模块中的 `#{}` 字符串 MUST NOT 被处理。

#### Scenario: 混合模块
- **WHEN** 同一项目的 A 模块依赖 `lucky-httpclient`，B 模块不依赖
- **THEN** A 模块中的 `#{}` 获得插件能力，B 模块中的 `#{}` 不获得任何插件能力

### Requirement: IDE 版本兼容
插件 MUST 在 2024.1 及以上版本的 IntelliJ IDEA 社区版运行，且 MUST NOT 要求安装 Spring 或 Spring Boot 插件。

#### Scenario: 社区版无 Spring 插件
- **WHEN** 用户在未安装 Spring 插件的 IDEA 社区版中打开依赖 `lucky-httpclient` 的项目
- **THEN** 高亮、补全与跳转功能可用，MUST NOT 出现「缺少依赖插件」的启动错误

### Requirement: 全局开关
插件 SHALL 提供全局启用/禁用开关；禁用后 MUST 立即停止所有 SpEL 编辑器行为，且 MUST NOT 修改用户源码。

#### Scenario: 禁用插件能力
- **WHEN** 用户关闭插件全局开关
- **THEN** 已打开文件中的 SpEL 高亮、补全、检查与跳转立即失效，源码内容保持不变

### Requirement: 不修改用户源码
除用户主动触发的重构或快速修复外，插件 MUST NOT 修改项目源码或构建配置。

#### Scenario: 被动分析
- **WHEN** 用户仅打开并浏览含 SpEL 的 Java 文件
- **THEN** 文件内容、依赖与构建配置保持不变
