## Purpose

定义从 SpEL 引用跳转到声明位置的行为，使变量、函数、类型与命名空间的来源可追溯，降低跨文件使用 SpEL 时的定位成本。

## ADDED Requirements

### Requirement: 变量跳转
对 `#name` 形式的变量引用（包括参数名、`$参数名`、`$n`、`$$n`、`参数名::类型`、`$n::类型`），插件 SHALL 在用户触发跳转（Ctrl+Click 或 Go to Declaration）时定位到声明位置：方法参数、`@SpELImport` 的变量声明、`@Param` 命名声明、内置变量常量或全局变量声明。

#### Scenario: 跳到方法参数
- **WHEN** 方法参数为 `String userId`，表达式引用 `#{#userId}`
- **THEN** 跳转定位到 `userId` 参数声明

#### Scenario: 跳到 @SpELImport 变量
- **WHEN** 类上声明 `@SpELImport(var = "token=#{...}")`，表达式引用 `#{#token}`
- **THEN** 跳转定位到 `"token=#{...}"` 字符串中的 `token` 部分

#### Scenario: 跳到参数简写
- **WHEN** 表达式引用 `#{$1}` 且方法第一个参数为 `String keyword`
- **THEN** 跳转定位到 `keyword` 参数声明

#### Scenario: 跳到内置变量
- **WHEN** 表达式引用内置变量 `#{$status$}`
- **THEN** 跳转定位到该内置变量的常量声明；库源码不可用时定位到内置符号说明页

### Requirement: 函数跳转
对 `#func()` 形式的函数引用，插件 SHALL 定位到对应的 public static 方法；存在 `@FunctionAlias` 别名时 MUST 按别名匹配；同名重载 MUST 提供多目标选择。

#### Scenario: 普通函数
- **WHEN** 表达式引用 `#{#time()}`
- **THEN** 跳转定位到内置函数 `time` 的方法声明

#### Scenario: 别名函数
- **WHEN** 方法声明了别名 `md5`，表达式引用 `#{#md5()}`
- **THEN** 跳转定位到声明该别名的方法

### Requirement: 命名空间与类型跳转
对 `#ns.func()`，插件 SHALL 定位到命名空间类中的方法；对 `T(...)`，插件 SHALL 定位到类型声明。

#### Scenario: 命名空间函数跳转
- **WHEN** 表达式引用 `#{#crypto.md5('a')}`
- **THEN** 跳转定位到 `crypto` 命名空间类中的 `md5` 方法

#### Scenario: 类型跳转
- **WHEN** 表达式引用 `#{T(java.util.ArrayList)}`
- **THEN** 跳转定位到 `ArrayList` 类声明

### Requirement: Bean 引用跳转
对 `@beanName` 形式的 Bean 引用，插件 SHALL 在能解析 Spring Bean 定义时定位到 Bean 声明；无法解析时 MUST NOT 产生错误弹窗。

#### Scenario: 可解析 Bean
- **WHEN** 项目中存在名为 `userMapper` 的 Spring Bean，表达式引用 `#{@userMapper}`
- **THEN** 跳转定位到该 Bean 的声明位置

#### Scenario: 不可解析 Bean
- **WHEN** 表达式引用 `#{@unknownBean}` 且项目无法解析该 Bean
- **THEN** 跳转不产生目标且不弹出错误对话框

### Requirement: 未解析引用不阻塞编辑
对未解析的 SpEL 引用，插件 MUST 保持编辑器可正常输入与保存，MUST NOT 抛出未处理异常或显示模态错误对话框。

#### Scenario: 未解析变量
- **WHEN** 用户对未解析的 `#notDefined` 触发跳转
- **THEN** 编辑器不跳转、不修改文件，且不出现模态错误
