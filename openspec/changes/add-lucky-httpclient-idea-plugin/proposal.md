## Why

我想为lucky-httpclient这个项目写一个IDEA插件，用于方便开发者进行开发

## What Changes

1. 插件项目代码写在D:\Lucklike\luckliy\lucky-httpclient-idea-plugin这个目录中
2. 此插件项目为独立的项目，不要塞进当前 Maven 多模块
3. 插件通过「项目依赖中存在 lucky-httpclient」自动激活，不影响其他项目
4. 目标 IDE 2024.1+（sinceBuild 241），开发用 JDK 21，社区版即可运行；不强依赖 Spring 插件
5. 主要功能如下：
   1. 感知lucky-httpclient中的SpEL上下文，在用户编写SpEL表达式时给出相应的的提示和补全
   2. SpEL表达式高亮，基础语法错误提示、悬停文档、类型推导 + 成员补全
   3. SpEL表达式中的变量需要支持跳转，使用Ctrl+鼠标右键可以调整到SpEL变量或函数的声明位置
   4. 你也可以补充一些其他好用的功能
      


## Capabilities

### New Capabilities

- `plugin-activation`：插件在依赖 lucky-httpclient 的项目中自动启用，在其他项目中保持零干扰；覆盖 IDE 版本兼容与全局开关。
- `spel-context-awareness`：识别 `#{}` 表达式边界、所在注解/方法/类上下文、变量分类与生命周期可见性。
- `spel-expression-highlighting`：SpEL 语法元素高亮、基础语法错误提示与颜色配置。
- `spel-expression-completion`：变量、Root 变量、函数、命名空间函数、类型与成员补全。
- `spel-symbol-navigation`：从变量、函数、类型与 Bean 引用跳转到声明位置。
- `spel-hover-documentation`：悬停显示变量来源、函数签名与内置符号说明。
- `spel-diagnostics-quickfix`：未解析符号、`@SpELImport` 格式、生命周期误用的检查与快速修复。
- `spel-refactoring`：查找 SpEL 引用、重命名变量/函数、把表达式提取为变量。
- `spel-yaml-configuration-support`：YAML 配置 API 中的 SpEL 高亮、补全与跳转。
- `spel-expression-evaluation`：在受控环境中离线预览/求值 SpEL 表达式。

### Modified Capabilities

无。

## Impact

- 新增独立插件项目，位于仓库根目录的 `lucky-httpclient-idea-plugin`，不纳入根 Maven 多模块。
- 插件仅依赖 IntelliJ Platform 的 Java 模块，不要求 Spring 插件；目标 IDE 2024.1+（sinceBuild 241）。
- 插件只读取项目依赖与源码中的 `lucky-httpclient` 注解/符号，不修改框架运行期行为。
- 可选框架侧改动：为注解增加 `@SpELSupport` 元数据以精确标识 SpEL 属性与生命周期，不改变现有运行期逻辑。
