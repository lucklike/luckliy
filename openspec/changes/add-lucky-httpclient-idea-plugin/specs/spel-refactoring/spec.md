## Purpose

定义 SpEL 符号的查找用法、重命名与表达式提取能力，使变量与函数的变更能够安全传播到所有引用位置。

## ADDED Requirements

### Requirement: 查找变量与函数用法
插件 SHALL 支持对可解析的 SpEL 变量与函数执行 Find Usages，结果 MUST 包含跨文件的 `#{}` 引用，并按文件分组。

#### Scenario: 查找变量用法
- **WHEN** 用户对方法参数 `userId` 执行 Find Usages
- **THEN** 结果包含所有引用 `#{#userId}` 的位置（含其他文件）

#### Scenario: 查找函数用法
- **WHEN** 用户对函数 `time` 执行 Find Usages
- **THEN** 结果包含所有 `#{#time()}` 引用位置

### Requirement: 重命名变量
重命名 `@SpELImport` 变量或方法参数时，插件 SHALL 同步更新所有可解析的 SpEL 引用；重命名前 MUST 提供受影响位置预览，冲突时 MUST 中止并提示。

#### Scenario: 重命名导入变量
- **WHEN** 用户把 `@SpELImport(var = "token=...")` 中的 `token` 重命名为 `accessToken`
- **THEN** 所有 `#{#token}` 引用更新为 `#{#accessToken}`

#### Scenario: 重命名参数
- **WHEN** 用户重命名方法参数 `userId`
- **THEN** 引用该参数的所有 SpEL 表达式同步更新

### Requirement: 重命名函数
重命名被导入的静态方法时，插件 SHALL 同步更新函数引用；若方法声明了别名，MUST 按别名更新引用而不是方法名。

#### Scenario: 别名函数重命名
- **WHEN** 方法声明别名 `md5`，用户重命名该别名
- **THEN** 所有 `#{#md5(...)}` 引用同步更新

### Requirement: 提取表达式为变量
插件 SHALL 支持把选中的 SpEL 片段提取为 `@SpELImport` 变量，并在原位置替换为变量引用；提取 MUST 保留原表达式语义。

#### Scenario: 提取函数调用
- **WHEN** 用户选中 `#{#time()}` 中的 `#time()` 并执行「提取为 SpEL 变量」
- **THEN** 在合适作用域插入 `@SpELImport(var = "t=#{#time()}")`，原位置变为 `#{#t}`
