## Purpose

定义 SpEL 相关的静态检查与快速修复，使常见错误在编码阶段被发现并可一键修正，同时允许用户关闭可能误报的检查。

## ADDED Requirements

### Requirement: 未解析变量与函数检查
插件 SHALL 对可确定上下文中的未解析变量与未解析函数产生警告；检查 MUST 可配置严重级别，并 MUST 支持完全关闭。

#### Scenario: 未解析变量警告
- **WHEN** 在已知上下文的方法注解中使用 `#{#notDefined}`
- **THEN** 编辑器显示未解析变量的警告标记

#### Scenario: 关闭检查
- **WHEN** 用户将未解析检查级别设为「不提示」
- **THEN** 未解析变量不再产生警告标记

### Requirement: 导入声明格式检查
插件 SHALL 校验 `@SpELImport` 的 `root`/`var`/`rootLit`/`varLit` 声明：缺少分隔符、空变量名、同一作用域重复声明 MUST 产生错误；覆盖框架内置变量 MUST 产生警告。

#### Scenario: 缺少分隔符
- **WHEN** 注解声明为 `@SpELImport(var = "token")`
- **THEN** 编辑器在 `"token"` 处显示缺少分隔符的错误

#### Scenario: 覆盖内置变量
- **WHEN** 注解声明为 `@SpELImport(root = "$req$=#{...}")`
- **THEN** 编辑器显示「覆盖内置变量」的警告

### Requirement: 生命周期误用检查
当变量在表达式所属阶段不可用时，插件 SHALL 产生警告，并说明该变量的可用阶段。

#### Scenario: 请求阶段引用响应变量
- **WHEN** URL 定义注解中引用 `$body$`
- **THEN** 编辑器显示「$body$ 仅在响应阶段可用」的警告

### Requirement: 快速修复：创建变量声明
对未解析变量，插件 SHALL 提供快速修复，在合适的作用域（类或方法）插入 `@SpELImport` 声明，并保留用户输入的变量名。

#### Scenario: 创建普通变量
- **WHEN** 用户对未解析的 `#token` 选择「创建 SpEL 变量声明」
- **THEN** 在当前方法上插入 `@SpELImport(var = "token=...")`，且原表达式保持不变

### Requirement: 快速修复：导入函数类与包裹表达式
插件 SHALL 提供「导入函数类」快速修复，用于把光标处静态方法所属的类加入 `@SpELImport`；SHALL 提供「包裹为 SpEL 表达式」与「取消 SpEL 包裹」操作。

#### Scenario: 包裹表达式
- **WHEN** 用户选中字符串中的 `#time()` 并执行「包裹为 SpEL 表达式」
- **THEN** 文本变为 `#{#time()}`

#### Scenario: 导入函数类
- **WHEN** 用户对未解析的 `#{#md5('a')}` 选择「导入函数类」
- **THEN** 在合适作用域插入对应的 `@SpELImport` 类导入，函数引用保持可解析

### Requirement: 误报抑制
插件 SHALL 支持对单条检查结果抑制，并 MUST 在源码中保留可识别的抑制标记；被抑制的结果 MUST NOT 再次显示。

#### Scenario: 抑制未解析变量
- **WHEN** 用户对某条未解析变量警告执行「抑制此提示」
- **THEN** 该位置不再显示该警告，且源码中出现可识别的抑制标记
