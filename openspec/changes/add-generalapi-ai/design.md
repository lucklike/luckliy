# Design: add-generalapi-ai

## Context

动机与范围见 [proposal.md](file:///D:/Lucklike/luckliy/openspec/changes/add-generalapi-ai/proposal.md) 的 Why / What Changes，行为契约见本 change 的 6 份 delta spec；本文只解释 HOW。

现状中与本设计相关的既有事实：

- `lucky-httpclient` 的注解 API 由 `HttpRequestProxy.methodProxy()` 驱动：一次调用创建一个 `MethodContext`，插件链与真实执行共享它，`doMethodProxy()` 在 `finally` 里调用 `mc.destroy()`。因此**同一个 `ExecuteMeta`/`ProxyDecorator` 不能安全地重复 `proceed()`**——第二次执行时上下文已销毁（`RepeatableReadStreamDestroyHandle` 等已回收资源，`wrapResult` 的包装类型也会被重复处理）。
- 插件机制：`ProxyPlugin.decorate(ProxyDecorator)` + `match(ExecuteMeta)`，通过 `@Plugin`/`@Plugins` 元注解或 `HttpClientProxyObjectFactory#addPlugin` 注册；这是本框架横切能力的唯一入口，回环要挂在这里。
- 流式：`@Sse` → `SseResponseConvert` 逐行把 SSE 文本喂给 `EventListener`，`StandardEventListener` 已处理「尾帧无空行」的 flush；方法返回值固定为 `null`，结果通过监听器输出。
- 序列化：`JSON_SCHEME`（默认 `JacksonSerializationScheme`）已全局配置 `FAIL_ON_UNKNOWN_PROPERTIES=false` 与 `Include.NON_NULL`，正好满足 `chat-session-model` 的两条契约；`ConfigurationMap` 支持 `addProperty("a.b.c", v)` 点号路径构造嵌套 JSON，旧 `ToolRegister` 就是用它的。
- 既有同类先例：`ChunkUploadApi` + `ChunkUploadExecutor` 采用「使用者实现协议接口、框架在多次调用中编排」的模式，每次调用都是全新的代理方法调用；测试侧有 `TestHttpServer`（JDK `com.sun.net.httpserver`，带路径命中计数、SSE 截断/挂起场景）与 `@Mock`（`xxx$Mock` 静态方法可拿到 `Request`）可用。
- 被删的旧包只留下 `@Tool(name/desc/type)`、`@Param(name/desc/type/enumerate/ignore/required)`、`@Required`（`@Param(required=true)` 的组合注解）这套注解语义，新实现沿用它们的属性命名，不沿用「注解即全部」的层次。
- 硬约束：Java 8（无 record/var/`Stream.toList`，官方 MCP SDK 需 17 不可用）、零新增第三方依赖、验证必须离线。

## Goals / Non-Goals

**Goals:**

- 分层顺序正确：先数据模型（`ChatRequest`/`ChatResponse`/`ToolSpec`），注解与 MCP 只是模型的三种入口之一。
- 厂商差异只出现在方言层，业务代码与回环引擎里没有 `if (vendor == ...)`。
- 回环、流式拼接、JSON Schema 生成三块逻辑都是可用纯函数方式离线穷举断言的。
- 公开 API 面积可控：对外只有「注解 + 会话模型 POJO + 注册表入口 + MCP 配置入口」。

**Non-Goals（设计层边界，比 proposal 的「不在本次范围」更具体）:**

- 不改造 `HttpClientProxyObjectFactory`/`MethodContext`/`ExecuteMeta` 等内核公共类的语义（需要改动即视为越界，须先单独确认）。
- 不为工具调用引入新的线程模型：工具在调用线程上同步执行；异步回环沿用既有 `@Async`/`@Sse async`。
- 不做对话历史的持久化、摘要、窗口裁剪（上下文长度治理留给后续 change）。
- 不做 `FunctionCallingRunner` 之外的第二种回环实现（如响应式 `Flux` 版回环）。

## Decisions

### D1 包结构与命名

`generalapi.ai` 下按层分五个子包，全部新代码落在此内：

| 子包 | 内容 |
|---|---|
| `ai.chat` | `ChatRequest`/`ChatMessage`/`ContentPart`/`ChatResponse`/`Usage`/`ChatRole`/`FinishReason` |
| `ai.tool` | `@Tool`/`@Param`/`@Required`、`ToolSpec`、`ToolExecutor`、`ToolRegistry`、`ToolSchemaGenerator`、`ToolResult` |
| `ai.vendor` | `VendorAdapter` + `OpenAIDialect`/`AnthropicDialect`/`GeminiDialect`、三套内置会话 API 接口、`Capabilities` |
| `ai.loop` | `@EnableToolCall`（组合 `@Plugin`）、`ToolCallProxyPlugin`、`FunctionCallingRunner`、`ToolCallFailurePolicy` |
| `ai.mcp` | `JsonRpc*`、`McpTransport`（`StdioTransport`/`StreamableHttpTransport`）、`McpClient`、`McpToolBridge` |

`Function` 一词只允许出现在方言层内部（Gemini 的 `functionDeclarations`/`functionCall`），模型与引擎层一律 Tool。测试夹具放同包 `support` 子目录。

### D2 请求侧用「POJO + 各家 Map」，响应侧用「各家 raw → 统一模型」

三家在字段名、system 位置、工具调用承载结构上差异如下（报文样例见 `specs/vendor-dialect/spec.md`）：

| 差异点 | OpenAI 兼容簇 | Anthropic | Gemini |
|---|---|---|---|
| 工具声明位置 | `tools[].function.parameters` | `tools[].input_schema` | `tools[0].functionDeclarations[].parameters`（类型大写） |
| system | `messages[].role=system` | 顶层 `system` 字符串 | 顶层 `systemInstruction` |
| 工具调用 | `assistant.tool_calls[]` | `assistant.content[].tool_use` | `model.content[].functionCall` |
| 工具结果 | `role=tool` + `tool_call_id` | `user.content[].tool_result` + `is_error` | `user.content[].functionResponse`（按 name 配对） |
| 调用标识 | 报文自带 `id` | 报文自带 `id` | 报文无 `id` |

- **请求**：`ChatRequest` 的字段命名直接对齐 OpenAI，OpenAI 簇方言 = 原样 `JSON_SCHEME.serialization(chatRequest)`；Anthropic/Gemini 方言用 `ConfigurationMap` 组装报文（`addProperty("systemInstruction.parts", ...)` 这类点号路径），不引入第二套请求 DTO。
  理由：请求结构由我们自己决定，Map 式组装最省事且断言直观；若改成「各家一套 DTO + Mapper」，类数量翻倍而行为不变。
  备选（已否决）：全链路只用 `ConfigurationMap`（业务侧要手拼 JSON，正是本次要消灭的用法）；只用 POJO 多态（要为 `tool_use`/`tool_result` 造继承体系，序列化形状反而难控）。
- **响应**：三家原始报文一律先解析成 `Map`（`Response.getMapResult()`），再由各家方言的解析函数**显式取字段**构建 `ChatResponse`，不依赖 POJO 字段绑定的隐式规则。
  理由：响应是外部可控输入，显式取值才能让「缺失 usage」「未知字段」「Gemini 无 id」这些契约有确定的落点；也避免使用者替换 `JSON_SCHEME`（如换 Gson）后行为漂移。
- 多模态分片：`ContentPart` 用统一描述（text/image + url 或 base64），三家各自翻译成 `image_url`/`{type:image,source:{...}}`/`{inlineData:{...}}`。工作量集中在方言层一次做完，避免留一个「只有 OpenAI 簇能用图片」的半洞。
- `ChatRequest`/`ChatMessage` 各留一个 `Map<String,Object> extra` 承载厂商专属字段：请求侧 Map 组装时 `putAll`，OpenAI 簇直连序列化路径用 Jackson `@JsonAnyGetter/@JsonAnySetter`（jackson-databind 已在 compile 依赖链上）。

### D3 `ToolSchemaGenerator` 输出 `ConfigurationMap`，注册期一次生成

- 输出 `{"type":"object","properties":{...},"required":[...]}` 的 Map 树，不建 Schema DTO 类层次（理由同 D2）。属性顺序按参数声明顺序（`LinkedHashMap`），`enum` 按常量声明顺序。
- 类型映射表在生成器里以「Class → 类型名」静态 Map 表达；`Optional<T>` 按 `T` 处理且不进 `required`；未覆盖类型直接抛异常，符合 spec 的「注册期失败」。
- 递归展开用 `IdentityHashMap<Class, Integer>` 做访问栈记录，遇到重复 Class 即抛异常并在消息里输出引用链；深度上限取常量 4，超限同序报错。理由：栈溢出比报错难诊断得多。
- 参数名来源优先 `Parameter#getName()`（编译已开 `-parameters`），显式 `@Param(name=...)` 覆盖；识别到 `arg\d+` 形态即视为不可得并按 spec 抛异常。
- 生成结果按 `Method` 缓存，注册一次复用（方言层每轮都要用，反射不能进循环）。

### D4 `ToolRegistry`：全局默认实例 + 可多实例

- `ToolSpec{name, description, ConfigurationMap parameters}` + `ToolExecutor`（`@FunctionalInterface Object execute(Map<String,Object> args) throws Throwable`，形态参照既有 `FunExecutor`）成对登记。
- 三种来源同一入口：`register(Object toolHolder)`（扫 `@Tool`）、`register(ToolSpec, ToolExecutor)`（编程式）、`McpToolBridge.registerAll(...)`（远端）。
- 提供 `ToolRegistry.getDefault()` 静态默认实例，同时允许业务自建实例并显式传给 `@EnableToolCall`（SpEL 表达式取 bean/实例），避免绑定 IoC 容器——`lucky-httpclient` 必须能脱离 `lucky-context` 使用。
- 冲突不静默：同名再登记抛 `ToolRegistryException`；提供带 `overwrite` 参数的重载。子集用 `registry.select(names)` 返回快照，不修改原表（满足「子集生效」与「入参不被就地修改」两条契约）。

### D5 方言选择 = 选 API 接口，不是运行时枚举分支

`VendorAdapter` 承担四类翻译，接口面收敛为四个方法：

```
Object writeBody(ChatRequest request);      // 统一模型 → 各家请求体
ChatResponse parseResponse(Map raw);        // 各家响应 → 统一模型
Capabilities capabilities();               // 能力标志（未知标志一律 false）
StreamAccumulator newAccumulator();         // 流式增量拼接器（见 D7）
```

业务侧通过三套内置会话 API 接口选方言（每套一个薄接口：URL 路径 + 鉴权头 + 返回 `ChatResponse` 的转换器），接口上的方法加 `@EnableToolCall` 即开启回环；Gemini 的 `models/{model}:generateContent` 路径用 `@Path` 取模型名。Anthropic 的 `anthropic-version` 头与默认 `max_tokens` 写在 Anthropic 接口的默认注解里。
理由：URL、鉴权、路径本来就是注解 API 的表达内容，用「选接口」代替「配枚举 + 框架内 switch」，与仓库既有 `generalapi.oauth2`/`upload` 的用法一致，也让 spec 的「换 base-url 即可复用兼容簇」退化为「同一个接口换 `@ServerAddress`」（旧文档里的 `@DomainName` 是本注解的旧称，代码中不存在该类型）。
备选（已否决）：单个接口 + `vendor: openai|anthropic|gemini` 配置项，由框架改 URL/头/请求体——需要框架介入 URL 生成，等于在框架里造一层隐式分支，可读性更差。
Gemini 类型名大写由方言里一个「递归改 Map 树中 `type` 值」的纯函数完成，不改 `ToolSchemaGenerator` 的输出。

### D6 回环每轮都是「重新调用代理方法」，用请求对象内的轮次标记防重入

- `@EnableToolCall` 是组合 `@Plugin` 的元注解，`ToolCallProxyPlugin.decorate()` 命中后**不再 `proceed()`**，而是把参数里的 `ChatRequest` 交给 `FunctionCallingRunner`；runner 负责发每一轮请求并返回最终结果。
- 每一轮都重新调用同一个代理方法（`meta.getProxy()` + 同一 `Method` + 新的参数数组），从而拿到全新的 `MethodContext`：注解配置（base-url、鉴权头、重试、日志、Mock、转换器）自动复用，插件自身不需要重新实现任何 HTTP 逻辑。
- 防重入标记放在 `ChatRequest` 的内部控制字段上（`transient int loopRound`，序列化时排除），而不是 `ThreadLocal`：嵌套调用会新建 `MethodContext`，跨线程（`@Async`/流式异步）时 `ThreadLocal` 会丢失；而 `ChatRequest` 作为方法参数一定被带到下一层的同一个调用里。插件读到 `loopRound > 0` 就直接 `proceed()` 走正常单次请求。
- runner 每轮拿 `ChatRequest` 的副本（`copy()`：消息列表新建 `ArrayList`，其余字段浅复制）追加消息，满足 `tool-call-loop` 的「调用方请求对象不被就地修改」。
- 轮次上限默认 5（可配置），超限抛异常时带上「已完成轮次 + 最后一轮工具名与参数」；工具结果顺序严格按响应中的调用顺序逐条回填，保证 Anthropic/Gemini 的按 `id`/`name` 配对不断裂。
- 失败统一收敛成 `ToolResult.failure(String text)`：执行异常、未知工具、参数解析失败、必填缺失、无执行实现五类都在引擎内生成失败结果并回填（默认策略），`@EnableToolCall` 提供 `failurePolicy = ABORT` 开关改为直接上抛。三类校验错误在调用执行器前判断（JSON 合法性 → 按 schema 取值/转换 → 必填检查），复用 `lucky-core` 的 `ConversionUtils` 做单值类型转换。
  备选（已否决）：让插件内循环 `decorator.proceed()`——撞上 `mc.destroy()` 语义；改内核提供「可重入 context」——越出 Non-Goals，属于底层公共类改动。

### D7 流式：拼接器是无状态入参的纯函数，监听器只做装饰

- `StreamAccumulator` 的契约是 `feed(chunk) → 事件`（文本增量 / 完整工具调用 / 协议异常），输入为「已解析出的单帧 Map」序列，OpenAI 簇按 `delta.tool_calls[].index` 分桶、Anthropic 按 `content_block_index` 分桶；单帧结构非法或流未给出结束信号即抛协议异常（含工具名与已拼接前缀）。因为是纯数据变换，可以直接用固定帧列表做单测，不需要真实 SSE。
- `ToolCallStreamListener` 装饰业务方的 `EventListener`：透传 `onOpen`/文本增量/`onError`/`onCompleted`，只把「完整工具调用」截留下来执行，然后以同一 listener 再发起下一轮流式调用。业务只看到一个连续流，`onCompleted` 只在最后一轮触发。
- 复用 `StandardEventListener` 已有的尾帧 flush，不改 SSE 基建。
- 流式的轮次上限、失败回填与阻塞式共用同一份 runner 逻辑，只是「发一轮」的实现从「一次代理方法调用」换成「一次流式代理方法调用 + 等待聚合结果」。

### D8 MCP：一层 JSON-RPC + 两种 Transport + 一次桥接

- `McpClient` 只实现 initialize / tools/list / tools/call / ping / 通知五个消息面，其余方法一律按「未知通知容忍、未知响应报错」处理。
- `JsonRpcIdGenerator`（`AtomicInteger`）+ `Map<Integer, Future>` 做请求-响应配对；读循环放一个后台线程，`Future.get(timeout)` 提供超时；`close()` 里 `process.destroyForcibly()` 兜底，避免子进程残留。
- `Transport` 接口两个实现：`StdioTransport`（`ProcessBuilder` + 换行分隔 JSON 帧）与 `StreamableHttpTransport`。后者用注解 API 定义一个内部 `McpHttpApi`（`@Post` + `@JsonBody`，URL/头来自配置），从而复用 HTTP 栈的重试、超时、日志脱敏与 Mock；`Mcp-Session-Id` 由客户端持有并在每次请求头注入，收到 404 时重握手一次再重发。
  备选（已否决）：直接用 `HttpURLConnection` 自写一层——丢掉既有基建，且日志脱敏要重做。
- `McpToolBridge` 把 `tools/list` 的 `inputSchema` 原样放进 `ToolSpec.parameters`（缺失补空 object schema），`tools/call` 包成 `ToolExecutor`：`content[]` 的 text 分片按序换行连接，`isError=true` 与 JSON-RPC `error` 都转成 `ToolResult.failure`，非 text 分片输出可识别占位文本。对回环引擎完全透明——引擎只看到一条普通 `ToolSpec`。
- 重名策略：默认抛冲突异常，另提供 `prefix(serverName)` 注册选项，远端 `tools/call` 仍用原始名。

### D9 配置与凭据

厂商与 MCP 的连接信息一律走配置文件 + 注解表达（`@ServerAddress`、`@Header`、`@EnableToolCall` 的 SpEL 项），沿用 `EnableConfigurationParser` 的读取路径风格；不新增硬编码常量，也不新增私有配置格式。密钥日志输出复用 `proxy.logging` 的按名脱敏能力，MCP HTTP 头与厂商 `Authorization` 都走同一条脱敏链路。

### D10 离线验证策略（同时是 tasks 的断言点）

| 场景 | 手段 |
|---|---|
| 报文形状 | golden-file：`src/test/resources/ai/{vendor}/*.json`，方言输出与样本逐字段比对 |
| 回环轮次与回填 | `@Mock` 的 `xxx$Mock` 静态方法（能拿到 `Request`）按当前请求体消息数选样本，断言「发了几轮、第二轮消息多了哪两条」 |
| 流式拼接 | 纯函数直接喂帧列表；端到端用 `SseMock.resource(...)` 回放录制的 SSE 文本 |
| 「是否真发请求/请求次数」 | 既有 `TestHttpServer`（带路径命中计数、SSE 截断/挂起） |
| MCP stdio | 测试类自带一个 `main` 假 server，用 `java -cp target/test-classes` 拉起，进程退出与超时路径可断言 |
| 不访问公网 | 上述全部在离线环境跑；`mvn -o -pl lucky-httpclient test` |

### D11 垂直切片顺序（供 tasks 阶段直接引用）

1. 切片一（walking skeleton）：`chat` 模型 + `OpenAIDialect` + 手写一条 `ToolSpec` + 阻塞回环 + `@Mock` 回放，端到端跑通「接口 + `@EnableToolCall` + 一个 `@Tool` 方法」。
2. 切片二：`ToolSchemaGenerator` 与注解注册入口（此时切片一的样板工具可改为注解生成，回归同一断言）。
3. 切片三：Anthropic/Gemini 方言 + 能力标志 + 响应归一化补齐标识。
4. 切片四：流式拼接与流式回环。
5. 切片五：MCP 客户端与桥接。

每个切片都有独立可判定的离线断言，禁止先铺完全部模型再联调。

## Risks / Trade-offs

- [报文细节与真机不符（Gemini `functionResponse` 的 role、Anthropic 必填项、流式 index 语义等）] → 先以 golden-file 固化断言，spec 已把可变项写成「样例即契约」；真机若不符，改动只落在单个方言类，模型层与引擎层不动。
- [每轮重新调用代理方法带来重复的注解解析与参数装配开销] → 相对一次网络往返可忽略；插件与类型解析在框架内已有 `Method` 级缓存，不在本轮优化。
- [`ChatRequest` 内部控制字段泄漏进请求体] → 字段 `transient` 且序列化排除，单测断言发出的请求体不含该键。
- [使用者替换 `JSON_SCHEME`（如改 Gson）后 null 省略/未知字段行为漂移] → OpenAI 簇以外的请求体由 `ConfigurationMap` 组装（不依赖 POJO 序列化配置），响应侧显式取 Map 字段；文档注明「OpenAI 簇形状与 `NON_NULL`/`ignore-unknown` 前提绑定」。
- [MCP 子进程泄漏或读循环卡死] → 超时 + `destroyForcibly` + `close()` 幂等，测试断言进程已回收；无响应场景必须走超时异常而非永久等待。
- [并行工具调用被误认为已支持] → 三家 `capabilities().parallelToolCalls` 一律上报 false，引擎按串行执行并全部回填，OpenAI 簇的 `parallel_tool_calls` 字段本轮不写入报文。
- [工具执行是同步的，长耗时工具会阻塞调用线程（且超时未定义）] → 本轮沿用调用线程执行；超时/取消列为 Open Question，不悄悄引入线程池语义。
- [公开 API 面积偏大（注解 + POJO + 注册表 + MCP 配置）形成长期兼容负担] → 对外只暴露 D1 表中标为入口的类型；`JsonRpc*`、`StreamAccumulator`、各方言内部转换器尽量包内可见，跨包需要的用 `@apiNote 内部API` 标注。
- [`@Param` 与 `com.luckyframework.reflect.Param`、`@Required` 与校验注解同名易混淆] → 新注解只放 `ai.tool` 包，javadoc 首行写明「AI 工具参数描述注解，非校验注解」，测试夹具统一按全限定名或明确 import 使用。

## Migration Plan

1. 纯增量落地：全部新代码在 `generalapi.ai` 下，旧 `generalapi.openai` 已删除且全仓无引用，无调用方需要改造。
2. 按 D11 切片顺序提交，每个切片自带测试；未启用的能力不注册全局插件，既有 API 行为零变化。
3. 回滚策略：删除 `ai` 包与其测试资源目录即可完全回退，不涉及数据库、配置文件字段或内核改动。
4. 归档：`openspec archive add-generalapi-ai` 后 6 份 delta 生成 `openspec/specs/<capability>/spec.md`，后续变更（MCP server 导出、并行调用等）走新 change 的 MODIFIED 增量。

## Open Questions

- 工具执行是否需要超时/取消策略（例如包装为 `Future` 并用 `@Timeout` 配置）——不影响现有 spec 断言，等有真实长任务需求再定。
- MCP `notifications/tools/list_changed` 到达时是否自动刷新注册表（本轮要求业务显式重新 `tools/list`）。
- `@Tool` 方法是否需要支持框架侧 SpEL 注入（如自动注入 bean、当前 `MethodContext`）——本轮只接模型给的参数。
- `@Tool` 方法自动扫描是否接入 `lucky-context` 的 bean 后处理器（本轮走显式注册，容器自动注册留后续 change）。
- Anthropic 默认 `max_tokens` 的具体取值、以及轮次上限默认值 5 是否需要按厂商区分——实现时先在配置里给出，不改变任何契约。
