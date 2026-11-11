## Purpose

定义在 IDE 内对 SpEL 表达式进行离线预览与受控求值的行为，帮助用户在不发起真实请求的情况下验证表达式结果。

## ADDED Requirements

### Requirement: 无外部依赖表达式求值
插件 SHALL 支持对不依赖请求或响应变量、且不执行外部 I/O 的表达式进行离线求值，并显示结果类型与结果值。

#### Scenario: 算术表达式
- **WHEN** 用户对 `#{1 + 2}` 执行「求值 SpEL」
- **THEN** 显示结果为 `3`（整数类型）

#### Scenario: 内置纯函数
- **WHEN** 用户对 `#{#time()}` 执行求值
- **THEN** 显示函数返回结果，且不发起网络请求

### Requirement: 模拟变量求值
插件 SHALL 允许用户为变量提供模拟值后求值；未提供值的变量 MUST 被视为不可用，并在结果中明确指出变量名。

#### Scenario: 提供模拟变量
- **WHEN** 用户为 `#name` 提供模拟值 `"abc"`，对 `#{#name + '!'}` 求值
- **THEN** 显示结果为 `"abc!"`

#### Scenario: 缺少模拟变量
- **WHEN** 用户对 `#{#missing + 1}` 求值且未提供 `#missing` 的值
- **THEN** 结果显示「变量 #missing 未提供」，MUST NOT 以空值继续求值

### Requirement: 求值失败提示
求值失败时，插件 SHALL 显示失败原因与可定位的错误位置，且 MUST NOT 修改源文件。

#### Scenario: 语法错误
- **WHEN** 用户对 `#{#fn(1,}` 执行求值
- **THEN** 显示语法错误及错误位置，源文件保持不变

### Requirement: 求值安全约束
求值 MUST 在受限环境中执行：MUST NOT 允许文件读写、网络访问、进程启动或反射修改运行时状态；表达式触发受限操作时 MUST 以明确的「操作被禁止」结果结束。

#### Scenario: 禁止文件访问
- **WHEN** 表达式尝试通过类型引用读取本地文件
- **THEN** 求值被拒绝，并显示「文件访问被禁止」
