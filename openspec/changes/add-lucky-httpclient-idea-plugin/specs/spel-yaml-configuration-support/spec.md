## Purpose

定义对 lucky-httpclient 配置 API 的 YAML 文件中 SpEL 表达式的支持，使配置式接口与注解式接口获得一致的编辑体验。

## ADDED Requirements

### Requirement: YAML 表达式识别
插件 SHALL 识别配置文件中包含 `#{}` 的字符串值，并将其视为 SpEL 片段；YAML 的注释、键名与非字符串值 MUST NOT 被识别为表达式。

#### Scenario: 字符串值中的表达式
- **WHEN** 配置为 `url: "http://#{#host}/api"`
- **THEN** `#host` 被识别为 SpEL 变量引用

#### Scenario: 注释中的表达式
- **WHEN** 配置为 `# 示例：#{#host}`
- **THEN** 注释内容不被识别为 SpEL 表达式

### Requirement: YAML 补全与跳转
在配置文件表达式中，插件 SHALL 提供与 Java 注解一致的变量与函数补全及跳转；变量来源 MUST 包含配置中的 SpEL 导入声明与框架内置符号。

#### Scenario: 配置导入变量
- **WHEN** 配置中声明导入变量 `token`，用户在另一处输入 `#{#to`
- **THEN** 补全列表包含 `#token`，并可跳转到导入声明

### Requirement: 配置上下文隔离
不同配置文件的导入声明 MUST 在其所属配置作用域内生效，MUST NOT 跨配置文件泄漏变量或函数。

#### Scenario: 两个配置文件
- **WHEN** 配置文件 A 声明变量 `token`，配置文件 B 未声明
- **THEN** B 中的表达式补全不包含 `#token`
