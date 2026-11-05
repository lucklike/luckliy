## Why

现有 `generalapi.openai` 包此前是半成品：只有 `@Tool/@Param/@ParamDesc/@Required` 四个注解和一个未完成的 `ToolRegister`（生成的函数配置没有存入注册表、参数 JSON Schema 不会生成、全仓没有任何消费方），既不能支撑工具调用，也没有会话数据模型。抽象层次放反了——只有注解（入口糖），没有数据模型（地基），因此决定推倒从 0 建立正确的层次：先有统一数据模型（ToolSpec / 会话模型），注解只是模型的入口糖之一。准备性改动已完成：旧包 `generalapi.openai` 已删除，空包 `generalapi.ai` 已建好，本 change 在 `generalapi.ai` 上从零实现全部能力。

业务上缺少对接 AI 厂商的「工具调用」能力：

1. 各厂商 Function Calling 报文格式不一致（OpenAI 兼容簇、Anthropic、Gemini 三个阵营在工具声明、tool_calls 响应结构、结果回填消息、流式增量协议上均有差异），业务方每接一家都要手写一遍适配与回环逻辑；
2. MCP 已成为工具生态的事实标准，开源 MCP server 绝大多数只提供 stdio 传输，本框架无任何支持；
3. lucky-httpclient 已具备注解驱动 API、ProxyPlant 插件机制、SSE 流式、Mock、Token 缓存等基建，补齐此项能力的边际成本低，且能让「声明一个接口 + 一个注解」即用 AI 工具调用，形成相对 Spring AI / langchain4j 的差异化（零重依赖、注解风格一致）。

约束：Java 8 硬性版本限制（官方 MCP Java SDK 要求 Java 17，不可引入）；所有验证必须离线可跑，不依赖真实 API key。

## What Changes

- **新建 `generalapi.ai` 包**（旧包 `generalapi.openai` 已删除、包骨架已建好，本 change 在该包内从 0 开发）：统一术语为 Tool（`@Tool` / ToolSpec / ToolCall / ToolRegistry / ToolExecutor），不再使用 Function 作为领域名词（Gemini 的 functionDeclarations 仅是方言层的翻译目标）。
- **统一会话模型**：新增 `ChatRequest / ChatMessage / ContentPart / ChatResponse / Usage` 等 POJO，以 OpenAI chat/completions 兼容格式为内部规范形状，messages 可被程序读写（支撑回环），替代现在让人手拼 JSON 请求体的用法。
- **工具声明与注册中心**：新增 `ToolRegistry`（ToolSpec + ToolExecutor 的注册表）与 `ToolSchemaGenerator`（从 `@Tool` 方法反射生成 JSON Schema：参数类型映射、`@Param` 嵌套对象展开、`@Required` 收集、枚举；配合已开启的 `-parameters` 编译参数取参数名）。ToolSpec 是一等公民，支持注解生成、编程式注册、MCP tools/list 三种来源。
- **厂商方言层**：新增 `VendorAdapter` 接口与三个实现——OpenAIDialect（覆盖 OpenAI/DeepSeek/通义/Kimi/智谱/豆包/Ollama 兼容簇，仅换 base-url）、AnthropicDialect（Claude）、GeminiDialect；负责工具声明序列化、tool_calls 解析、结果回填、流式增量拼接四类翻译，厂商能力差异经 `capabilities()` 协商（是否支持并行调用/strict schema/流式工具调用），不在业务代码里散落 if-else。
- **工具调用回环引擎**：新增 `FunctionCallingRunner`：发请求 → 发现 tool_calls → 经 ToolRegistry 找到执行器本地执行（含异常回填策略：工具报错时将错误信息回填给模型自纠）→ 追加消息重放，直至模型停止调用；带最大轮次保护。通过 ProxyPlant 插件 + `@EnableToolCall` 注解挂到现有注解 API 上，复用 HTTP/SSE/重试/日志脱敏/Mock。
- **流式支持**：基于现有 `@Sse` + StandardEventListener 实现 tool_calls 增量拼接状态机（OpenAI 按 index 拼接 arguments、Anthropic 拼接 input_json_delta），拼接逻辑为可离线测试的纯函数（输入 chunk 序列 → 输出完整 ToolCall）。
- **MCP 客户端（第一阶段只做 tools）**：自研薄层 JSON-RPC 2.0（消息面仅 initialize / tools/list / tools/call / ping / 通知），传输层两条：stdio（ProcessBuilder 拉起子进程）与 Streamable HTTP（复用本模块 POST + SSE 能力）；新增 `McpToolBridge` 把远端 MCP server 的 tools/list 直接转为 ToolSpec 注册进 ToolRegistry，`tools/call` 桥接为 ToolExecutor——对回环引擎完全透明。
- **不引入新第三方依赖**：不使用 Spring AI / langchain4j / 官方 MCP SDK，全部基于 Java 8 + 本仓既有工具类（ConfigurationMap 等）实现。

不在本次范围（后续独立 change 交付）：MCP server 导出（把本框架 @Tool 方法暴露为 MCP server）、MCP resources/prompts/elicitation 等高阶特性、并行工具调用与 strict schema 细节、RAG/embedding 等对话之外的 AI 能力。

## Capabilities

### New Capabilities

- `chat-session-model`：与厂商无关的对话会话数据形状（请求 / 消息 / 内容分片 / 响应 / token 用量），可被程序读写与双向 JSON 序列化，是回环引擎唯一依赖的地基。
- `tool-declaration`：以注解或编程方式声明工具（名称、描述、参数 JSON Schema），并把声明与执行器集中注册；Schema 生成规则（类型映射、嵌套展开、必填收集、枚举）为对外契约。
- `vendor-dialect`：把统一模型翻译为 OpenAI 兼容簇 / Anthropic / Gemini 三家报文，并把三家响应翻译回统一模型；角色位置差异、必填项差异、工具调用标识补齐、能力标志协商均在此能力内。
- `tool-call-loop`：「发请求 → 工具调用 → 本地执行 → 回填 → 重放」的有界回环，含注解 API 接入方式、最大轮次保护、工具失败/未知工具/非法参数的回填策略。
- `streaming-tool-call`：流式（SSE）增量 chunk 拼接为完整文本与完整工具调用的语义，含拼接时机、异常终止的确定性、流式回环的连续性。
- `mcp-client`：MCP 协议 tools 能力的客户端——初始化握手、tools/list 转为本地工具声明、tools/call 桥接执行、stdio 与 Streamable HTTP 两条传输、通知与配对、凭据脱敏。

### Modified Capabilities

无。仓库 `openspec/specs/` 目前为空（`openspec list --specs` 无任何已登记能力），本次不修改既有能力规格。旧包 `generalapi.openai` 的半成品注解与 `ToolRegister` 属直接删除，没有对外承诺的行为需要写 REMOVED 增量。

## Impact

- **代码**：全部落在 `lucky-httpclient` 模块 `com.luckyframework.httpclient.generalapi.ai` 包（当前为空包骨架）内新增子包；测试放同包 `src/test/java`，外部响应以 JSON 样本文件做离线回放。除删除旧 `generalapi.openai` 包（已完成）外不改动既有包结构。
- **依赖**：不新增任何第三方依赖（不引入 Spring AI / langchain4j / 官方 MCP Java SDK），JSON-RPC 2.0、JSON Schema 生成、流式拼接均基于 Java 8 + 本仓既有构件（`ConfigurationMap`、`StandardEventListener`、`ProxyPlant`、Mock/重试/日志脱敏插件）自研或复用；Java 8 版本上限是硬约束。
- **API**：新增用户可见入口——`@Tool`/`@Param` 系列注解、`@EnableToolCall` 注解与工具注册 API、会话模型 POJO、MCP server 配置项；对既有注解 API 是纯增量，未标注新注解的方法行为不变。
- **兼容性**：会话模型以 OpenAI chat/completions 兼容形状为内部规范，OpenAI 兼容簇（DeepSeek/通义/Kimi/智谱/豆包/Ollama）仅换 base-url 即可复用；老代码没有对 `generalapi.openai` 的引用（全仓无消费方），删除无破坏面。
- **验证**：所有场景断言可在离线环境跑通，不依赖真实 API key 与公网；MCP stdio 传输的验证由本地子进程（或伪造 server 的可执行入口）驱动。
