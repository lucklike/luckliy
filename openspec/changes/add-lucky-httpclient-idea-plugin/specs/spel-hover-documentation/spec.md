## Purpose

定义悬停 SpEL 符号时展示的说明信息，让用户在不离开编辑器的情况下理解变量来源、函数签名与内置符号用途。

## ADDED Requirements

### Requirement: 变量悬停文档
悬停普通变量或 Root 变量时，插件 SHALL 显示名称、来源、可推断的类型、作用域与是否为 Root 变量；来源至少区分方法参数、`@SpELImport`、内置变量与全局变量。

#### Scenario: 参数变量悬停
- **WHEN** 用户悬停 `#{#userId}`
- **THEN** 文档显示名称 `userId`、来源「方法参数」与类型 `String`

#### Scenario: Root 变量悬停
- **WHEN** 用户悬停 `#{user.name}` 中的 `user`
- **THEN** 文档显示来源「@SpELImport Root 变量」并标注「Root 变量」

### Requirement: 函数悬停文档
悬停函数时，插件 SHALL 显示方法签名、返回类型、所属类与可用说明；有别名时 MUST 同时显示别名与原方法名。

#### Scenario: 内置函数悬停
- **WHEN** 用户悬停 `#{#time()}`
- **THEN** 文档显示返回类型、所属工具类与函数说明

#### Scenario: 别名函数悬停
- **WHEN** 用户悬停 `#{#md5('a')}`
- **THEN** 文档同时显示别名 `md5` 与原方法名

### Requirement: 内置符号说明
对框架内置变量，插件 SHALL 显示其含义、类型与可用的生命周期阶段。

#### Scenario: 响应变量说明
- **WHEN** 用户悬停 `#{$body$}` 中的 `$body$`
- **THEN** 文档显示「响应体」含义、类型（可推断时）与「响应阶段可用」提示

### Requirement: 未解析符号提示
悬停未解析符号时，插件 SHALL 显示「未解析」状态及可能原因（如当前上下文不可见、未导入、拼写错误），MUST NOT 显示空白文档。

#### Scenario: 未解析变量悬停
- **WHEN** 用户悬停未解析的 `#notDefined`
- **THEN** 文档显示「未解析」及「请检查变量名或当前上下文是否可见」
