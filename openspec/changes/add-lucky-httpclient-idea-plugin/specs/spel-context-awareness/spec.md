## Purpose

定义插件如何从源码位置推导 SpEL 表达式的边界、所属上下文与可用符号范围，为高亮、补全、跳转与检查提供一致的语义基础。

## ADDED Requirements

### Requirement: 表达式边界识别
插件 SHALL 识别字符串字面量中以 `#{` 开始、以匹配的 `}` 结束的 SpEL 片段；同一字符串中的多个片段 MUST 分别识别。识别 MUST 正确处理片段内的嵌套花括号、单引号/双引号字符串与转义字符，MUST NOT 把字符串内部的 `}` 误判为表达式结束。

#### Scenario: 字符串模板中的多个片段
- **WHEN** 字符串为 `"X-TIME: #{#time()}; X-ID: #{#id}"`
- **THEN** 两个 `#{...}` 片段分别被识别，`#time()` 与 `#id` 各自获得能力

#### Scenario: 表达式内字符串包含右花括号
- **WHEN** 表达式为 `#{ {'a': '}'} }`
- **THEN** 表达式边界为最外层 `#{...}`，字符串中的 `}` 不结束表达式

#### Scenario: 未闭合表达式
- **WHEN** 字符串为 `"#{foo"` 且没有匹配的 `}`
- **THEN** 该片段被识别为未闭合表达式，并产生基础语法错误提示

### Requirement: 特殊前缀识别
插件 SHALL 识别 `@if(#{...}):`、`@max(n):` 与反引号嵌套标记 `` ``#{...}`` ``；`@if` 与 `@max` 前缀本身 MUST NOT 被当作 SpEL 语法元素高亮。

#### Scenario: @if 条件表达式
- **WHEN** 字符串为 `"@if(#{#enabled}): X-Trace: 1"`
- **THEN** `#enabled` 被识别为条件表达式的一部分，`@if(...):` 前缀不被当作变量或函数

#### Scenario: 反引号嵌套
- **WHEN** 字符串由双反引号包裹，内容为 `#{#{#key}}`
- **THEN** 内层 `#{#key}` 被识别为嵌套表达式，反引号标记不被当作 SpEL 语法

### Requirement: 上下文推导
插件 SHALL 根据表达式所在的注解、方法、类推导可用符号范围：方法参数及其命名形式（参数名、`$参数名`、`$n`、`$$n`、`参数名::类型`、`$n::类型`）、`@Param` 重命名、类级与方法级 `@SpELImport`、内置变量与内置函数、全局变量。方法参数上的显式命名 MUST 优先于编译期参数名；方法级导入 MUST 优先于类级同名声明。

#### Scenario: 方法参数变量
- **WHEN** 方法签名为 `void query(@Param("keyword") String q, int page)`，注解中使用 `#{q}` 与 `#{page}`
- **THEN** `q` 与 `page` 均被识别为可用变量，`q` 对应参数声明位置

#### Scenario: 参数简写形式
- **WHEN** 方法签名为 `void query(String keyword, int page)`，表达式中使用 `#{#keyword}`、`#{$keyword}`、`#{$1}`、`#{$1::String}` 与 `#{$2}`
- **THEN** 上述引用均被识别为指向对应方法参数的可用变量

#### Scenario: 类级与方法级导入叠加
- **WHEN** 类上声明 `@SpELImport(root = "baseUrl=#{...}")`，方法上声明 `@SpELImport(var = "token=#{...}")`
- **THEN** 方法内表达式可同时使用 Root 变量 `baseUrl` 与普通变量 `#token`

### Requirement: 变量与函数分类
插件 SHALL 区分以下符号类别并在高亮、补全与跳转中保持一致：普通变量（`#name`）、Root 变量（`name`）、函数（`#func()`）、命名空间函数（`#ns.func()`）、类型引用（`T(...)`）、Bean 引用（`@name`）。

#### Scenario: Root 变量与普通变量
- **WHEN** 表达式同时使用 `#{user.name}` 与 `#{#token}`
- **THEN** `user` 被识别为 Root 变量，`#token` 被识别为普通变量，两者不混淆

#### Scenario: 命名空间函数
- **WHEN** 表达式为 `#{#crypto.md5('a')}`
- **THEN** `crypto` 被识别为命名空间，`md5` 被识别为该命名空间下的函数

#### Scenario: 项目内注册的函数类
- **WHEN** 项目通过 `addSpringElFunctionClass(Utils.class)` 注册无命名空间的函数类 `Utils`，表达式为 `#{#sum(1, 2)}`
- **THEN** `sum` 被识别为可用函数；若同一类以命名空间 `util` 注册，则只有 `#{#util.sum(1, 2)}` 被识别为可用

### Requirement: 生命周期可见性
插件 SHALL 根据表达式所在注解的语义阶段判定变量可见性，至少区分请求阶段、响应阶段、异常阶段、重试阶段与结果阶段；在当前阶段不可见的变量 MUST 被标记为「阶段不可用」，而 MUST NOT 被当作未解析变量。

#### Scenario: 重试阶段使用响应变量
- **WHEN** 表达式位于 `@Retryable` 的 `condition` 属性中并引用 `$status$`
- **THEN** `$status$` 被视为可用，不产生未解析告警

#### Scenario: 请求阶段使用响应变量
- **WHEN** 表达式位于 URL 定义注解中并引用 `$body$`
- **THEN** 该引用被标记为当前阶段不可用，并给出响应阶段才可用的提示

### Requirement: 未知上下文降级
当表达式所在位置无法归属到 `lucky-httpclient` 注解或已知配置时，插件 SHALL 仅提供通用语法高亮，MUST NOT 产生未解析符号的告警或错误。

#### Scenario: 普通业务字符串
- **WHEN** 普通 Java 方法体中的字符串包含 `#{notASpelVariable}`
- **THEN** 仅按 SpEL 语法高亮，MUST NOT 报未解析变量
