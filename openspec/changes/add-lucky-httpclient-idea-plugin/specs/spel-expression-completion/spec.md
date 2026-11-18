## Purpose

定义在 SpEL 片段内根据当前上下文提供变量、函数、类型与成员补全的行为，使用户无需记忆框架内置符号与导入关系。

## ADDED Requirements

### Requirement: 变量补全
在 `#` 之后，插件 SHALL 提供当前上下文可见的普通变量：方法参数（含参数名、`$参数名`、`$n`、`$$n`、`参数名::类型`、`$n::类型` 等命名形式与 `@Param` 重命名）、`@SpELImport` 的 `var`/`varLit`、内置变量与全局变量。每个候选项 MUST 显示名称与类型（可推断时），并 MUST 标注来源。

#### Scenario: 方法参数补全
- **WHEN** 用户在注解字符串中输入 `#{#` 且方法参数为 `String userId`
- **THEN** 补全列表包含 `#userId`，类型显示为 `String`，来源标注为方法参数

#### Scenario: 参数简写补全
- **WHEN** 方法签名为 `void query(String keyword, int page)`，用户输入 `#{#$`
- **THEN** 补全列表包含 `$keyword`、`$1`、`$$1` 与 `$1::String` 等候选，并标注各自对应的参数

#### Scenario: 导入变量补全
- **WHEN** 类上声明 `@SpELImport(var = "token=#{...}")`，用户在方法内输入 `#{#to`
- **THEN** 补全列表包含 `#token`，来源标注为 `@SpELImport`

### Requirement: Root 变量补全
在表达式起始位置，插件 SHALL 提供当前上下文可见的 Root 变量（`@SpELImport` 的 `root`/`rootLit` 与内置 Root 变量）；Root 变量候选 MUST NOT 自动添加 `#` 前缀。

#### Scenario: Root 变量候选
- **WHEN** 类上声明 `@SpELImport(root = "user=#{...}")`，用户在表达式起始输入 `u`
- **THEN** 补全列表包含 `user`，插入文本为 `user` 而非 `#user`

### Requirement: 函数补全
在 `#` 之后且光标处于可调用位置时，插件 SHALL 提供当前上下文可见的函数：已在变量表平铺注册的内置函数、`@SpELImport` 导入类的 public static 方法，以及通过 `addSpringElFunctionClass` 注册的无命名空间函数。候选项 MUST 显示函数签名与返回类型，同名重载 MUST 全部列出。带命名空间的函数 MUST NOT 出现在 `#` 位置，只能从 `#ns.` 进入补全。

#### Scenario: 内置函数补全
- **WHEN** 用户在表达式中输入 `#{#ti`
- **THEN** 补全列表包含内置函数 `time`，并显示其签名与返回类型

#### Scenario: 导入类函数补全
- **WHEN** 方法上声明 `@SpELImport(MyUtils.class)`，用户输入 `#{#md5`
- **THEN** 补全列表包含 `MyUtils` 的 public static 方法 `md5`

#### Scenario: 带命名空间的函数不出现在 `#` 位置
- **WHEN** 某导入类声明命名空间 `crypto`，用户在 `#{#` 之后请求补全
- **THEN** 补全列表不包含该类的函数；用户输入 `#{#crypto.` 后才列出这些函数

### Requirement: 命名空间函数补全
在 `#ns.` 之后，插件 SHALL 提供该命名空间下的函数；命名空间来源为带命名空间声明的导入类。

#### Scenario: 命名空间函数
- **WHEN** 某导入类声明命名空间 `crypto`，用户输入 `#{#crypto.`
- **THEN** 补全列表包含该类的 public static 方法

### Requirement: 类型补全
在 `T(` 之后，插件 SHALL 提供可由 `@SpELImport(pack)` 导入包简化的类型，以及项目类路径中可解析的类型。

#### Scenario: 导入包后的类型
- **WHEN** 类上声明 `@SpELImport(pack = "java.util")`，用户输入 `#{T(ArrayL`
- **THEN** 补全列表包含 `ArrayList`

### Requirement: 成员补全
在 `.` 之后，插件 SHALL 基于前一个表达式的静态类型提供字段与方法候选；类型无法推断时 MUST 降级为通用成员候选，并明确标注「类型未知」。

#### Scenario: 已知类型的成员
- **WHEN** 方法参数为 `User user` 且用户输入 `#{#user.`
- **THEN** 补全列表包含 `User` 的 public 字段与 public 方法

#### Scenario: 未知类型的降级
- **WHEN** 变量类型无法静态推断，用户输入 `#{#payload.`
- **THEN** 补全列表包含通用成员，并标注「类型未知」，MUST NOT 显示空的补全列表

### Requirement: 补全过滤与插入格式
补全 SHALL 按输入前缀过滤并优先精确匹配；变量插入 MUST 避免重复 `#`，函数插入 MUST 补齐 `()` 并把光标置于括号内，类型插入 MUST 按导入包规则使用简单名或全限定名。

#### Scenario: 函数插入
- **WHEN** 用户选择函数 `md5` 并确认
- **THEN** 插入结果为 `md5()`，光标位于括号内
