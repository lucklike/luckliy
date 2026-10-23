# Tasks: add-generalapi-ai

按 design.md 的 D11 垂直切片顺序组织：先打通切片一的 walking skeleton（1.x），再逐层补全。每个任务自带离线验证方式（测试类名或断言点），全部命令为 `set JAVA_HOME=D:\jdk` 后执行 `mvn -o -pl lucky-httpclient ...`。

Open Questions 的取舍已在任务里写成明确默认值，不改变任何契约：Anthropic 缺省 `max_tokens` 取可配置默认 1024（未配置默认值时按 spec 提前失败）；回环轮次上限默认 5 且三家统一；MCP `notifications/tools/list_changed` 本轮只容忍不自动刷新注册表；`@Tool` 方法不做框架侧 SpEL 注入、不接容器自动扫描。

## 1. 切片一：会话模型 + OpenAI 方言 + 阻塞回环（walking skeleton）

- [x] 1.1 建 `generalapi.ai` 的五个子包 `chat`/`tool`/`vendor`/`loop`/`mcp` 与测试侧 `support` 夹具目录、golden-file 目录 `src/test/resources/ai/{openai,anthropic,gemini}`，验证：`mvn -o -pl lucky-httpclient test-compile` 通过且目录齐备
- [x] 1.2 定义 `ChatRole`、`FinishReason`、`ContentPart`（text/image，image 支持 url 与 base64 两种来源，模型层不出现任何厂商字段名）、`Usage`（统一字段：输入/输出/总 token），验证：新增 `chat/ChatSessionModelTest` 对四类做「构造 → 序列化 → 回读」逐字段断言
- [x] 1.3 `ChatMessage`/`ChatRequest`/`ChatResponse`：字段命名对齐 OpenAI chat/completions；`ChatRequest` 含 model、messages、temperature、maxTokens、stop、stream、tools、toolChoice，并留 `Map<String,Object> extra` 扩展容器与 `transient int loopRound`（序列化排除）；`ChatMessage` 同样留 `extra`，验证：`ChatSessionModelTest` 断言「null 字段省略」「未知字段忽略不抛异常」「`extra` 里的键出现在 JSON」「发出的 JSON 不含 `loopRound` 键」四条
- [x] 1.4 `ChatRequest` 的消息读写入口（追加消息、追加工具结果消息）与 `copy()`（messages 新建 `ArrayList`，其余字段浅复制），验证：`ChatSessionModelTest#copyIsolation` 断言 copy 后原对象条数与内容不变、messages 列表实例不同
- [x] 1.5 `ToolSpec`（name/description/`ConfigurationMap parameters`）+ `ToolExecutor`（`@FunctionalInterface Object execute(Map<String,Object>) throws Throwable`，形态参照既有 `FunExecutor`）+ `ToolResult`（success/failure 两态）+ `ToolRegistry`（`getDefault()`、`register(spec, executor)`、`register(spec)` 仅声明、`select(names)` 返回快照、按名查询未命中返回明确「未找到」），验证：`tool/ToolRegistryTest` 覆盖「同名冲突抛 `ToolRegistryException` 且首次登记不变」「overwrite 重载生效」「子集快照不含未选工具且原表未被修改」「列举顺序稳定」四条断言
- [x] 1.6 `VendorAdapter` 接口四方法（`writeBody`/`parseResponse`/`capabilities`/`newAccumulator`）与 `Capabilities`（工具调用、流式工具调用、并行工具调用、schema 严格模式四个标志；未知标志名返回不支持而非抛异常），验证：`vendor/CapabilitiesTest` 断言未知标志返回 false 且不抛异常
- [x] 1.7 `OpenAIDialect.writeBody`：工具声明包一层 `{"type":"function"}` 且 schema 落 `function.parameters`；本轮不写入 `parallel_tool_calls` 字段；`extra` 合并进请求体，验证：`vendor/OpenAIDialectTest` 与 `src/test/resources/ai/openai/request-with-tools.json` 逐字段比对（键顺序不参与断言）
- [x] 1.8 `OpenAIDialect.parseResponse(Map)`：显式取字段（不用 POJO 隐式绑定）构建 `ChatResponse`——`choices[0].message.content` 与 `tool_calls[]`（id/function.name/arguments 文本原样保留）、`finish_reason` 归一、`usage` 缺失时得到空用量而非异常，验证：`vendor/OpenAIDialectTest` 用三个录制响应样本断言纯文本、含 2 个工具调用、缺 usage 三种归一结果
- [x] 1.9 内置 OpenAI 兼容会话 API 接口：`@Post` + 路径 `/chat/completions` + base-url 与鉴权头走 `@ServerAddress`/`@Header` 的配置项（不硬编码），响应转换器产出 `ChatResponse`，验证：`vendor/OpenAIApiTest` 用既有 `TestHttpServer` 断言 HTTP 方法、路径、`Authorization` 头取自配置文件键，且返回对象为 `ChatResponse`
- [x] 1.10 `FunctionCallingRunner` 阻塞回环最小版 + `@EnableToolCall`（组合 `@Plugin` 的元注解）+ `ToolCallProxyPlugin`：命中后不 `proceed()`，`loopRound > 0` 时直接 `proceed()` 走单次请求；每轮经 `ExecuteMeta#getProxy()` + 同一 `Method` + 新参数数组重新调用代理方法；串行执行工具、按响应顺序回填、`loopRound` 自增；轮次上限默认 5 且可配置，验证：`loop/FunctionCallingRunnerTest` 用 `@Mock`（`xxx$Mock` 静态方法拿到 `Request`，按当前消息数选响应样本）断言「共发 2 次请求」「第 2 次请求体比第 1 次多出 `assistant.tool_calls` 与 `role=tool` 两条消息」「返回第 2 轮文本」
- [x] 1.11 切片一端到端验收（跨 1.2–1.10）：以编程式登记一条 `ToolSpec` + 执行器，会话接口方法标 `@EnableToolCall`，跑通「接口 + 注解 + 一轮工具调用 → 最终文本」，验证：`loop/WalkingSkeletonTest` 通过 `mvn -o -pl lucky-httpclient -Dtest=WalkingSkeletonTest test`，测试全程不发起外部网络请求

## 2. 切片二：注解声明与 JSON Schema 生成

- [x] 2.1 在 `ai.tool` 重建 `@Tool`/`@Param`/`@ParamDesc`/`@Required` 四注解，属性命名沿用被删旧包（name/desc/type/enumerate/ignore/required），javadoc 首行写明「AI 工具参数描述注解，非校验注解」，验证：模块编译通过，且测试夹具中 import 与 `com.luckyframework.reflect.Param`、`lucky-validator` 的 `@Required` 不产生歧义（IDE/编译无二义告警）
- [x] 2.2 `ToolSchemaGenerator` 的类型映射表（Class → 类型名静态 Map）：string→`string`、整数族与 `BigInteger`→`integer`、浮点族与 `BigDecimal`→`number`、布尔→`boolean`、枚举→`string` + 按常量声明顺序的 `enum`、数组与集合→`array` + 元素类型递归决定的 `items`、`Date`/时间类型→`string`、`Optional<T>` 按 `T` 处理且不进 `required`；不可映射类型抛异常含类型全名；输出为 `ConfigurationMap` 的 Map 树且 `properties` 按参数声明顺序（`LinkedHashMap`），验证：`tool/ToolSchemaGeneratorTest` 逐类型断言 + `InputStream` 参数注册抛异常且不生成部分 schema
- [x] 2.3 参数名与描述来源：优先 `Parameter#getName()`（编译已开 `-parameters`），`@Param(name=...)` 显式覆盖；反射结果为 `arg\d+` 形态视为不可得并抛异常（消息含类名、方法名、参数位置）；工具描述缺失时注册失败并指出具体方法；工具名默认取方法名、`@Tool(name=...)` 可覆盖，验证：`ToolSchemaGeneratorTest` 覆盖 spec 的三场景——`get_weather(String city, int days)` 得到 golden schema、显式命名后 `properties` 与 `required` 均用新名、合成参数名场景抛错
- [x] 2.4 POJO 参数递归展开：字段进 `properties`、字段描述取字段注解、必填按层归属分别进入根 `required` 与该对象 schema 的 `required`；`IdentityHashMap` 访问栈检测自引用/相互引用并抛含引用链的异常，深度上限常量 4 超限同序报错；生成结果按 `Method` 缓存，验证：`ToolSchemaGeneratorTest#nestedObject` 比对 spec 样例（`query` 层 `["city"]`、根层 `["query"]`），`#selfReference` 与 `#depthLimit` 断言异常消息含类型名与链
- [x] 2.5 `ToolRegistry.register(Object toolHolder)`：扫描 `@Tool` 方法生成声明并把该方法绑定为执行器（反射调用，参数按 schema 解析后传入），验证：`tool/AnnotationToolRegistrationTest` 断言注解生成的声明与 2.3 的 golden schema 一致，且用计数 holder 断言「多次列举/多轮调用不重复反射」（注册期命中缓存）
- [x] 2.6 执行结果序列化规则：字符串与基本类型取其文本、对象/集合/Map 走 JSON 文本；仅有声明无执行实现时工具仍可被列举并出现在报文里，被调用时返回「无执行实现」的失败 `ToolResult` 而非未捕获异常，验证：`AnnotationToolRegistrationTest` 断言 `{"temp":12,"weather":"晴"}` 形式、字符串原样不包 JSON、无执行实现场景得到失败结果
- [x] 2.7 回归合并：把 1.11 的编程式工具改为 `@Tool` 注解生成，验证：`WalkingSkeletonTest` 断言原样通过（证明注解与手写两条来源在注册表内是同一数据结构）

## 3. 切片三：Anthropic / Gemini 方言与能力协商

- [x] 3.1 `AnthropicDialect.writeBody`：`system` 提为顶层字符串、工具 schema 落 `tools[].input_schema`、assistant 的工具调用写作 `content[].tool_use`、结果写作 `user` 角色消息内 `content[].tool_result`（带 `tool_use_id`，失败时 `is_error: true`）；请求 MUST 带 `anthropic-version: 2023-06-01` 头；`max_tokens` 缺省填可配置默认值（默认 1024），默认值也未配置时在发出请求前抛指明缺失字段的异常，验证：`vendor/AnthropicDialectTest` 与 `ai/anthropic/*.json` 逐字段比对；`#maxTokensFailFast` 断言异常且 `TestHttpServer` 路径命中数为 0
- [x] 3.2 `AnthropicDialect.parseResponse(Map)`：`content[]` 中 `text` 分片归一为文本、`tool_use` 分片归一为工具调用（`id` 原样保留为调用标识）、`stop_reason` 归一为 `FinishReason`、`usage.input_tokens`/`output_tokens` 归一到统一 `Usage` 字段，验证：`AnthropicDialectTest` 用录制响应样本断言文本+2 个 tool_use+用量三项，且第二轮回填能与 `id` 一一对应
- [x] 3.3 `GeminiDialect.writeBody`：`systemInstruction` 顶层、工具落 `tools[0].functionDeclarations[].parameters`、类型名由方言内「递归改 Map 树中 `type` 值为大写」的纯函数转换（不改 `ToolSchemaGenerator` 输出）、角色映射 user/model、工具结果以 `user` 角色消息内 `functionResponse`（`name` 为工具名，本次固定 role 为 `user`）；目标地址 `models/{model}:generateContent` 用 `@Path` 取模型名，验证：`vendor/GeminiDialectTest` 比对 golden 并断言出现 `OBJECT`/`STRING` 大写、`functionDeclarations` 位置正确、URL 含模型标识
- [x] 3.4 `GeminiDialect.parseResponse(Map)`：`candidates[0].content.parts[].functionCall` 归一为工具调用并补齐调用标识（报文无 id 时生成同一次解析内唯一且稳定的标识，供按 name 配对与统一模型使用）、`finishReason` 归一、`usageMetadata` 三家命名归一，验证：`GeminiDialectTest#synthesizeCallId` 断言标识非空唯一，`#usageNormalized` 断言 prompt/candidates/total 三个计数映射到统一字段
- [x] 3.5 三家 `capabilities()` 取值与实际实现一致：工具调用与流式工具调用三家均支持，并行工具调用与 schema 严格模式三家均上报不支持；上层只依据标志集合决策，验证：`vendor/VendorCapabilitiesTest` 断言四标志取值与未知标志返回不支持；并检查 `generalapi/ai/loop` 包源码不出现厂商名字面量（无 `if (vendor == ...)` 分支）
- [x] 3.6 兼容簇复用验证：同一份会话请求与工具声明，分别以 OpenAI 与 DeepSeek 的 base-url/鉴权配置发起，验证：`vendor/OpenAICompatibleClusterTest` 断言两次请求体 JSON 除 `model` 外完全一致、目标地址与鉴权头按各自配置生成（`@Mock` 捕获 `Request` 比对），且未新增任何兼容簇分支代码

## 4. 切片四：回环语义补全（轮次、失败回填、隔离）

- [x] 4.1 最大轮次保护：默认 5 可配置覆盖；达上限而模型仍要求调用时停止重放并抛可诊断异常，文本含已达成轮次数与最后一轮待执行的工具名及参数，验证：`loop/MaxRoundsTest#hitsLimit` 用「每轮都要求调用工具」的样本配上限 3，断言共发 3 次请求、异常文本含「轮次上限 3」与工具名/参数、`TestHttpServer` 命中数为 3（第 4 次未发出）
- [x] 4.2 提高上限后收敛：同批样本前 4 轮为工具调用、第 5 轮为文本，验证：`MaxRoundsTest#raisesLimit` 配上限 8 断言方法返回第 5 轮文本、命中数为 5
- [x] 4.3 五类失败统一生成 `ToolResult.failure` 并回填后继续回环：执行异常（内容含工具名与错误摘要）、未知工具（含「未知工具 <name>」）、参数文本非法 JSON、必填缺失、有声明无执行实现；校验顺序为「JSON 合法性 → 按 schema 取值并用 `ConversionUtils` 做单值转换 → 必填检查」，三类校验失败在调用执行器前判定，验证：`loop/ToolFailureBackfillTest` 五个场景各断言第 2 轮请求中工具结果消息内容关键词，必填缺失场景用计数 holder 断言执行方法未被调用
- [x] 4.4 同轮多调用的完整性：结果条数与调用条数一一对应、顺序严格按响应中的调用顺序逐条回填（部分失败也两条俱全），验证：`ToolFailureBackfillTest#partialFailure` 断言第 2 轮含 2 条工具结果消息且 `tool_call_id` 与第 1 轮 2 个调用一一对应
- [x] 4.5 `ToolCallFailurePolicy` 两态：默认 CONTINUE（业务侧无异常）、`@EnableToolCall(failurePolicy = ABORT)` 时首个工具异常即上抛且不再发后续请求，验证：`ToolFailureBackfillTest#aborts` 断言业务方收到该异常（或包装异常）且 `TestHttpServer` 命中数为 1
- [x] 4.6 隔离性两条：回环只在 `ChatRequest` 副本上追加消息；未标 `@EnableToolCall` 的方法行为完全不变（请求体不注入 `tools`，响应中出现 tool_calls 时不执行工具、不重放、原样返回），验证：`loop/IsolationTest` 两用例——入参 2 条消息在返回后仍 2 条且第二请求实例不同；未标注方法的请求体无 `tools` 键且命中数为 1

## 5. 切片五：流式增量拼接与流式回环

- [x] 5.1 `StreamAccumulator` 契约（`feed(单帧 Map) → 事件`：文本增量 / 完整工具调用 / 协议异常）与 `OpenAIStreamAccumulator`：按 `choices[0].delta.tool_calls[].index` 分桶，`id` 与 `function.name` 取该 index 首帧，`function.arguments` 增量按到达顺序连接，文本增量与工具增量互不干扰，验证：`vendor/OpenAIStreamAccumulatorTest` 用固定帧列表（spec 样例）断言产出的参数文本为合法 JSON 且与 golden 一致，同帧含 `delta.content` 时文本事件照常透出
- [x] 5.2 `AnthropicStreamAccumulator`：按 `content_block_delta` 的 `index` 分桶拼接 `delta.partial_json`，仅在 `content_block_stop` 时产出完整工具调用（半成品期间 MUST NOT 对外可见），`message_stop` 给出流结束信号，验证：`vendor/AnthropicStreamAccumulatorTest` 断言拼接过程中无 ToolCall 事件、stop 后产出一个完整调用，且 `id`/name 取自 `content_block_start`
- [x] 5.3 `GeminiStreamAccumulator`：`candidates[].content.parts[].functionCall` 单帧完整即产出、多帧文本与调用混合时按到达顺序透传，保证三家 `streamToolCall` 标志名副其实，验证：`vendor/GeminiStreamAccumulatorTest` 用录制帧列表断言产出一个完整调用且文本事件顺序不变
- [x] 5.4 拼接的确定性与异常终止：同一帧序列重复拼接（每次新 accumulator 实例）结果逐字段一致；单帧结构非法或流未给出结束信号时抛协议异常，消息含工具名与已拼接的参数前缀，验证：`vendor/StreamAccumulatorDeterminismTest` 两组断言（一致性比对 + 非法帧/截断流异常消息内容）
- [x] 5.5 `ToolCallStreamListener` 装饰业务方 `EventListener`：透传 `onOpen`/文本增量/`onError`，截留完整工具调用后本地执行并以同一 listener 发起下一轮流式调用，业务只看到一个连续流、`onCompleted` 只在最后一轮触发；复用 `StandardEventListener` 的尾帧 flush 不改 SSE 基建，验证：`loop/ToolCallStreamListenerTest` 用 `SseMock.resource(...)` 回放两轮流式文本，断言文本事件跨轮连续、`onCompleted` 计数为 1、工具被执行 1 次
- [x] 5.6 流式与阻塞共用同一份 runner 语义（轮次上限、失败回填、请求副本），仅「发一轮」的实现换为流式调用 + 等待聚合结果，验证：`loop/StreamLoopRoundTest` 断言流式下同样触发超限异常与失败回填，`mvn -o -pl lucky-httpclient -Dtest=*Stream* test` 全绿

## 6. 切片六：MCP 客户端与工具桥接

- [x] 6.1 JSON-RPC 2.0 消息面（request/response/notification/error 四类，仅 initialize/tools/list/tools/call/ping/通知）+ `JsonRpcIdGenerator`（`AtomicInteger`）+ `Map<Integer, Future>` 请求-响应配对 + `Future.get(timeout)` 超时，验证：`mcp/JsonRpcPairingTest` 用内存假 transport 断言乱序响应按 id 各自命中、无 id 的通知不参与配对且不被当作响应
- [x] 6.2 `McpTransport` 接口与 `StdioTransport`：`ProcessBuilder` 按配置命令拉起子进程、消息以换行分隔 JSON 帧（不依赖空白缩进）、读循环在后台线程、`close()` 幂等并 `process.destroyForcibly()` 兜底，验证：`mcp/StdioTransportTest` 用测试自带 `support/FakeMcpServer#main`（`java -cp target/test-classes` 拉起）断言一帧一对象的收发；超时用例断言得到超时异常且 `process.isAlive()` 为 false（进程无残留）
- [x] 6.3 `McpClient` 初始化握手：消息顺序 `initialize`（协议版本 + clientInfo + capabilities）→ 响应 → `notifications/initialized`（无 id）；握手前请求工具清单抛「会话未初始化」且不发请求；server 返回版本不一致时采用 server 版本沿用；`capabilities` 不含 `tools` 时不注册任何远端工具并给出可诊断说明，验证：`mcp/McpHandshakeTest` 四场景（帧序列取自录制文件，用假 transport 断言发出顺序与内容）
- [x] 6.4 `tools/list` 分页与 `McpToolBridge` 注册：`nextCursor` 存在时带 `params.cursor` 续拉直至取完；每个远端工具转 `ToolSpec`（name/description 原样、`inputSchema` 原样保留、缺失补 `{"type":"object","properties":{}}`）并连同执行桥接登记；与本地工具重名默认抛冲突，`prefix(serverName)` 选项下注册名带前缀而远端 `tools/call` 仍用原始名，验证：`mcp/McpToolBridgeTest` 断言单页 2 工具字段逐项一致、分页发出 2 次 `tools/list` 且第二次 cursor 为 `c1`、冲突异常含工具名、前缀选项后两名字不同
- [x] 6.5 `tools/call` 桥接为 `ToolExecutor`：`params.name` 用远端原始名、`params.arguments` 为按声明 schema 解析的参数对象；`result.content` 中 text 分片按序换行连接为结果内容、`isError: true` 转失败结果交回环回填（不抛给业务方）、JSON-RPC `error` 转含错误码与说明的失败结果、非 text 分片输出可识别占位说明不静默丢弃，验证：`McpToolBridgeTest` 四场景（成功文本、`isError`、`-32601` 错误响应、两个 text 分片连接顺序）
- [x] 6.6 `StreamableHttpTransport`：以内部注解 API 接口（`@Post` + `@JsonBody`，地址与头来自配置）复用 HTTP 栈的重试/超时/日志脱敏/Mock；请求头含 `Content-Type: application/json` 与 `Accept: application/json, text/event-stream`；响应为单 JSON 直接取、为 SSE 时按帧解析取与该请求 id 匹配的消息；握手响应的 `Mcp-Session-Id` 由客户端持有并在后续请求头注入；HTTP 404 时重握手一次并重发该请求，再失败即报错不无限重试，验证：`mcp/StreamableHttpTransportTest` 三场景（会话头保持、SSE 帧内结果取出、404 重连）且 404 用例断言服务端命中数为 2
- [x] 6.7 通知容忍与 `ping`：server 主动通知与未知 method 的通知只忽略不报错、不打断配对（结果前先到 `notifications/message` 仍正确配对，含 `notifications/tools/list_changed` 本轮只容忍不自动刷新）；`ping` 收到 `result: {}` 视为成功、无响应时在超时后得到失败结果，验证：`mcp/McpNotificationPingTest` 两场景
- [x] 6.8 MCP 配置来源与凭据保护：命令与参数、HTTP 地址与请求头全部来自配置文件键或编程式配置（无硬编码常量、无新私有配置格式）；`Authorization` 等凭据复用 `proxy.logging` 的按名脱敏，验证：`mcp/McpConfigMaskingTest` 断言调试日志中被脱敏键的令牌值不出现明文、其余请求信息仍可读；`mcp/McpOfflineTest` 断言本能力全部行为在无公网环境下由本地子进程与录制帧完成

## 7. 收尾与合规检查

- [x] 7.1 越界检查：确认未修改 `HttpClientProxyObjectFactory`/`MethodContext`/`ExecuteMeta` 等内核公共类语义、未新增任何第三方依赖，验证：`git diff --name-only` 中 `lucky-httpclient/pom.xml` 与各模块 pom 无改动，`proxy/` 内核包（`proxy/context`、`proxy/plugin`、`proxy/HttpRequestProxy` 等）无文件出现在 diff 里
- [x] 7.2 公开 API 面积收敛：对外入口仅「`@Tool` 系列注解 + `@EnableToolCall` + 会话模型 POJO + `ToolRegistry` + 三套内置会话 API 接口 + MCP 配置入口」；`JsonRpc*`、`StreamAccumulator` 实现、方言内部转换器改为包内可见或 javadoc 标注内部 API，验证：逐项核对 D1 表格并出具入口清单；`mvn -o -pl lucky-httpclient javadoc:javadoc` 无「公开类型缺注释」告警
- [x] 7.3 需求覆盖矩阵：6 份 delta spec 的 37 条需求（共 89 个场景）逐条对应到至少一个测试类/断言点，验证：在 PR 描述或本文档附注列出「需求 → 测试类#方法」映射表，无需求落在空白；`openspec validate add-generalapi-ai --strict` 仍通过
- [x] 7.4 全量离线回归：`mvn -o -pl lucky-httpclient test` 全绿，验证：测试过程零公网访问（无真实 API key 常量、无外网域名），MCP stdio 与所有回放场景在断网环境可重复执行
- [x] 7.5 使用文档：在 `lucky-httpclient/docs/extend/` 追加一篇「AI 工具调用与 MCP」，覆盖注解 API 用法、三家方言通过选接口切换、注册表三种来源、回环与流式行为、MCP 配置项与离线自测方式，验证：文档中的接口与注解片段与 `WalkingSkeletonTest`/`AnnotationToolRegistrationTest` 夹具一致，可原样编译
