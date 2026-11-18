## Context

本设计服务于 `proposal.md` 中定义的能力，行为契约见 `specs/` 下 10 个能力的 `spec.md`。这里只记录影响实现方式的现状与约束。

### 运行期 SpEL 模型（代码事实）

- 表达式边界：`SpELConvert` 使用 Spring `TemplateParserContext`，默认前后缀为 `#{` / `}`；`isSpELExpression` 只判断字符串中是否同时存在起始与结束标记，因此一个字符串可以包含多个独立片段。
- 嵌套与条件：反引号 `` `` `` 包裹表示需要嵌套解析，`@max(n):` 限制嵌套次数；`@if(...):` 由 `Context.ifExpressionEvaluation` 以正则 `^@if\s*\([\S\s]*?\)\s*:` 识别。
- 变量容器：`SpELVariate` 分为普通变量（`VarCtrlMap`，含函数）与 Root 变量（`RootVarCtrlMap`）。`Context.getFinallyVar()` 从当前上下文向父链逐层 `replenishMerge`，最后合并全局变量；`MutableMap.get` 返回第一个命中的分层，因此「离表达式最近的上下文优先，全局变量兜底」。
- 方法参数：`ParameterContext.initContext` 为每个参数注册 `name`、`$name`、`$n`、`$$n`、`name::type`、`$n::type` 六类名字；参数名来自编译期 `-parameters`，可被 `@Param` 覆盖。
- 注解声明：`@SpELImport` 支持 `value`（导入类）、`ifs`（条件导入）、`root`/`var`/`rootLit`/`varLit`（变量声明）、`pack`（导包）、`separator`（默认 `=`）。`root`/`var` 的键和值都会执行 SpEL，`rootLit`/`varLit` 不执行。
- 参数注入：`@Var` 按 `VarType` 映射为 `#{name}`（ROOT）、`${name}`（ENVIRONMENT）、`#{@name}`（BEAN）、`#{#name}`（NORMAL）；`@Param` 的 `value` 本身是完整 SpEL 表达式。
- 内置符号：`InternalRootVarName` 约 40 个 Root 变量，`InternalVarName` 约 17 个内部变量，`function` 包约 148 个 public static 函数；`ClassStaticElement` 只收集 public static 方法，跳过 synthetic 与 `@FunctionFilter`，用 `@FunctionAlias` 改名，同名方法会抛异常。
- 生命周期：`Lifecycle` 共 16 个阶段；`DefaultSpELVarManager` 在 `REQUEST_INIT`/`RESPONSE_INIT`/`RESPONSE` 注册请求/响应变量；`@Retryable.condition` 在响应之后执行，因此可以读取 `$status$`。
- 组合注解：`@SpELImport`、`@Namespace`、`@FunctionAlias` 都可以出现在元注解上；`Context.handleSpELImport` 使用 `AnnotationUtils.getNestCombinationAnnotations` 递归查找。
- 已知偏差：`importVarHandler` 把 `varLit` 加入 Root 变量而非普通变量，疑似框架 bug；插件按 `specs/spel-context-awareness/spec.md` 的公开语义把 `varLit` 当作普通变量处理，不复制该偏差。

### 工程约束

- 框架：Java 8、Spring 5.3.29、Maven 多模块；插件代码放在仓库根目录的 `lucky-httpclient-idea-plugin`，独立构建，不进入根 `pom.xml`。
- 插件：JDK 21、IntelliJ IDEA 社区版 2024.1+（`sinceBuild=241`）、不要求 Spring/Spring Boot 插件。
- 插件只读项目依赖与源码；除用户主动触发的重构/快速修复外不修改文件。
- 当前框架没有 `@SpELSupport` 元注解，注解属性的 SpEL 语义分散在 `Context.parseExpression(...)` 的调用点。

## Goals / Non-Goals

**Goals:**

- 在 IDE 内静态还原足够的 SpEL 上下文，让高亮、补全、跳转、悬停文档、检查共用同一套符号视图。
- 对无法静态确定的部分采取保守降级：不误报、不阻塞编辑、不影响未使用 `lucky-httpclient` 的项目。
- 保持插件独立可构建、可安装、可卸载；不修改框架运行期行为，不要求框架先升级。
- 用可版本化、可测试的注解元数据表与内置符号表支撑后续能力扩展（YAML、重构、求值）。

**Non-Goals:**

- 不实现完整的 SpEL 语法、类型系统或求值语义；只覆盖 specs 需要的表达式结构。
- 不执行框架运行期逻辑来推断变量；不启动真实请求。
- 第一期不支持 Kotlin 字符串、非 `lucky-httpclient` 项目的 SpEL、`@SpELSupport` 框架改动。
- 求值工具不作为安全边界，只做开发者主动触发的本地预览。
- 不在本 change 内完成插件市场发布、签名与自动更新。

## Decisions

### D1. 独立 Gradle 插件工程

**选择**：在 `lucky-httpclient-idea-plugin` 建立独立 Gradle 项目，Kotlin 实现，JDK 21，使用 IntelliJ Platform Gradle Plugin 2.x；编译目标 IDE 为 `intellijIdeaCommunity("2024.1")`，`sinceBuild=241`（2024.1 的 build 号），`untilBuild` 不设上限。这与 proposal 的「目标 IDE 2024.1+」一致。如果要向下覆盖 2023.3，需要把编译目标整体降到 2023.3 并改用 `sinceBuild=233`，不能只改 `sinceBuild`。

**理由**：Gradle 是 IntelliJ 插件的标准构建方式；独立项目避免把 IntelliJ/插件依赖带进 Maven 多模块，也避免框架构建受插件版本影响。

**替代方案**：作为 Maven 子模块（会污染根构建、需要额外插件工具链，放弃）；独立 Maven 项目（IntelliJ 插件生态与测试夹具支持弱于 Gradle，放弃）。

### D2. 依赖感知激活按模块判定

**选择**：在项目打开后由 `ProjectActivity` 启动激活检查；用 `JavaPsiFacade.findClass("com.luckyframework.httpclient.proxy.annotations.HttpRequest", projectScope)` 判断依赖是否存在，并结合 `ModuleUtilCore.findModuleForPsiElement` 按模块缓存结果；`DumbService.isDumb()` 时降级为只做轻量高亮，等待 smart mode 后恢复补全/跳转。

**理由**：依赖可能是源码模块也可能是 jar，类存在性比 library 名称、`pom.xml` 文本更可靠；按模块判定满足 `specs/plugin-activation/spec.md` 的隔离要求。

**替代方案**：`plugin.xml` 的 `<depends>`（`lucky-httpclient` 不是 IDE 插件，不可行）；解析 `pom.xml`/Gradle 构建脚本（只覆盖部分项目，且解析成本高，放弃）；每次编辑时实时检查依赖（性能差，放弃）。

### D3. 自研轻量 SpEL 扫描器

**选择**：实现独立的 `SpelScanner` 与最小 `SpelExpression` 结构，负责识别 `#{}` 片段、嵌套花括号、字符串/转义、`@if(...):`、`@max(n):`、双反引号，并产出带偏移的 token 流；不依赖也不打包 `spring-expression`。

**理由**：插件类加载器与用户项目隔离，打包 Spring 会带来版本冲突与 `LinkageError` 风险；高亮、补全、跳转只需要词法与轻量结构，不需要完整求值；自研可精确控制错误恢复和字符串内偏移。

**替代方案**：打包 `spring-expression`（版本冲突风险高，放弃）；反射调用用户项目的 `SpELConvert`（需要初始化框架上下文，副作用大，放弃）；用 ANTLR/JavaCC 生成完整解析器（构建复杂、收益低，放弃）。

**边界**：扫描器只保证 specs 覆盖的语法；遇到未知语法时降级为普通 token，不产生错误标记。`@if` 的识别优先与框架正则保持一致，必要时用括号平衡增强；两者冲突时以「不误报」为优先。

### D4. 注解元数据表与组合注解解析

**选择**：插件内置 `spel-annotations.json`，记录「注解全限定名 → 属性名 → 值种类 + 生命周期 + 符号种类」。值种类至少包括：

- `TEMPLATE`：整个字符串是 SpEL 模板（如 `@HttpRequest#url`、`@ServerAddress#url/path`）；
- `KEY_VALUE`：按 `separator` 分割，键和值都可能包含 SpEL（如 `@SpELImport#root/var`）；
- `IF_CONDITION`：条件表达式（如 `@SpELImport#ifs.condition`、`@Retryable#condition`）；
- `NONE`：不是 SpEL（如 `@SpELImport#separator`、`@FunctionAlias#value`）。

表的候选由生成脚本扫描框架源码中的 `parseExpression(ann.xxx(), ...)` 调用点与注解定义得到，再人工确认生命周期；插件运行时只读 JSON，不反射框架类。组合注解通过递归遍历元注解并解析 `@AliasFor` 展开属性；递归带循环检测。

**理由**：框架没有 `@SpELSupport`，SpEL 语义分散在调用点，静态表是当前唯一可控、可测试的来源；把表放进插件仓库可以与框架版本解耦，也便于后续替换为元注解生成。

**替代方案**：立即在框架 20+ 个注解上增加 `@SpELSupport`（跨仓库改动、影响框架版本，作为后续可选项，不在本 change 实施）；运行时反射注解（插件无法加载框架运行期类，且无法判断属性语义，放弃）；按注解名硬编码（难维护、无法覆盖 `@AliasFor`，放弃）。

**已知偏差**：`varLit` 在运行期被错误加入 Root 变量；插件按公开语义把它作为普通变量，design 保留该记录，避免实现时照抄运行期行为。

### D5. 统一符号模型与来源分层

**选择**：所有能力共用 `SpelSymbol`：`name`、`kind`（变量/Root/函数/命名空间/类型/Bean）、`type`、`origin`（参数/注解导入/内置/全局/未知）、`declaration`、`lifecycle`、`visibility`、`sourceRange`。符号解析按以下优先级叠加：

1. 方法参数（含六种命名形式与 `@Param` 覆盖名）；
2. 方法级 `@SpELImport`（`root`/`var`/`rootLit`/`varLit`/`value`/`ifs`/`pack`）；
3. 类级 `@SpELImport`；
4. 框架内置变量与函数；
5. 全局变量（只纳入可静态解析的配置部分，无法确定时不进入符号表）。

**理由**：高亮、补全、跳转、文档、检查必须看到同一套符号，否则会出现「能补全但跳不过去」「高亮一种名字、检查另一种名字」的不一致；优先级与 `MutableMap.get` 的「最近上下文优先」一致。

**替代方案**：每个功能独立解析（重复实现、结果不一致，放弃）；只做文本匹配不做符号解析（无法支持类型、跳转和检查，放弃）。

**函数规则**：复刻 `ClassStaticElement` 的公开规则：只取 public static、跳过 synthetic、跳过 `@FunctionFilter`、按 `@FunctionAlias` 改名；同名冲突时补全去重并标注冲突，跳转提供多目标，检查给出冲突提示。

**函数注册来源**：注册函数按三路并集收集：框架固定注册的默认函数类（jar 无法读取方法体时仍有完整清单）、`HttpClientProxyObjectFactory` 源码中的调用、项目源码中任意 `addSpringElFunctionClass` 调用（含工厂子类）。只接受能解析到工厂类（含子类）的调用，字符串、注释与同名方法不参与；命名空间实参支持编译期字符串常量，空白或求值失败时回退到类上的 `@Namespace`。注册在工厂构造时完成，生命周期为 `CLASS`。

**函数书写形式**：无命名空间的注册函数与 `@SpELImport` 导入函数在变量表有平铺 `Method`，可写 `#fn(...)`；带命名空间的函数只能写 `#ns.fn(...)`；未注册的内置函数（如 `BankCardGenerator`）只能平铺 `fn(...)`。

**内置符号表**：从框架源码生成 JSON，包含变量名、类型、生命周期、说明，以及函数签名、返回类型、Javadoc 摘要；插件不加载框架类。用户项目只依赖 jar 时，PSI 仍可解析类成员；没有 Javadoc 时降级为签名。

### D6. 生命周期可见性映射

**选择**：以 `Lifecycle` 枚举为骨架，维护两张表：

- 「注解属性 → 阶段」：例如 URL 定义属性映射到 `REQUEST_INIT`，`@Retryable#condition` 映射到 `RETRY`（响应之后），`@AutoVerifyHttpStatus#condition/errMsg` 映射到 `RESPONSE`，`@RangeDownload#saveDir/filename` 映射到 `RESPONSE`；
- 「内置变量 → 可用阶段」：请求变量从 `REQUEST_INIT` 起可用，响应变量从 `RESPONSE_INIT`/`RESPONSE` 起可用，`$err$`/`$rootErr$` 在 `THROWABLE`，`$result$` 在 `METHOD_RESULT`。

未知注解属性默认按 `METHOD` 处理，且不参与生命周期过滤，只做语法/符号检查。

**理由**：运行期阶段由调用时机决定，静态分析只能近似；保守策略避免把合法表达式误报为「阶段不可用」。

**替代方案**：按注解类型粗粒度映射（例如把所有 `@Retryable` 属性都视为 RETRY，会误判其他属性，放弃）；不做生命周期检查（ specs 要求阶段误用提示，放弃）。

**与运行期对齐**：`@Retryable.condition` 的默认值已经使用 `$status$`，说明该属性在响应之后执行；插件必须以这个事实为准，而不是简单地把 `@Retryable` 归为请求阶段。

### D7. 类型推导与成员补全

**选择**：类型来源按可靠性排序：PSI 参数/字段/方法返回类型 → `@SpELImport` 值表达式的可推断结果（字面量、`T(...)`、函数返回类型）→ 内置变量类型表 → `Object`。类型不可推断时，成员补全返回通用成员（`toString`/`getClass`/`equals`/`hashCode`）并标注「类型未知」。

**理由**：完整 SpEL 类型系统成本高，MVP 只需要常见场景；降级策略满足 `specs/spel-expression-completion/spec.md` 的「不得显示空补全列表」。

**替代方案**：调用 Spring `Expression.getType()`（需要运行期上下文与变量值，放弃）；只做名称前缀匹配（成员补全不可用，放弃）。

**框架类映射**：`$req$` → `Request`、`$resp$` → `Response`、`$status$` → `Integer` 等由内置变量表维护；框架 jar 在项目 classpath 时用 PSI 解析成员，否则返回通用成员。

### D8. PSI 集成方式

**选择**：

- 高亮/语法错误：`Annotator` 处理 `PsiLiteralExpression`，配合 `TextAttributesKey` 与 `ColorSettingsPage`；
- 补全：`CompletionContributor` 通过 `elementPattern` 限定包含 `#{` 的字符串字面量；
- 跳转/查找用法：`PsiReferenceContributor` 注册 `SpelReference`（继承 `PsiReferenceBase`，`isSoft=true`），`resolve()` 返回 Java PSI 或虚拟内置符号声明；多目标实现 `PsiPolyVariantReference`；
- 检查：`LocalInspectionTool`，未解析/生命周期默认 warning，`@SpELImport` 格式默认 error；
- 快速修复：`LocalQuickFix` 插入 `@SpELImport`、导入函数类、包裹/取消包裹。

不为 SpEL 注册独立 `Language`/`ParserDefinition`，因为 SpEL 是 Java 字符串内容，不是独立文件。

**理由**：改动面小、与 Java PSI 共存、性能可控；避免为字符串内容创建伪语言文件与 `FileViewProvider`。

**替代方案**：自定义 `Language` + `ParserDefinition` + `FileViewProvider`（复杂、易与 Java 字符串 PSI 冲突，放弃）；只用 `Annotator`（无法支持跳转/引用/查找用法，放弃）。

### D9. 索引与缓存策略

**选择**：用 `AnnotatedElementsSearch` 建立项目级索引，覆盖 `@SpELImport`、`@Namespace`、`@FunctionAlias`、`@FunctionFilter`；类型查找用 `JavaPsiFacade.findClass`，类型补全用 `PsiShortNamesCache`；用 `CachedValuesManager` 缓存每个 `PsiLiteralExpression` 的 `SpelContext` 与符号表，依赖 `PsiModificationTracker.MODIFICATION_COUNT` 失效。

只处理文本包含 `#{` 的 `PsiLiteralExpression`；不在 `Annotator` 中做全项目扫描；补全/跳转在 `DumbService.isDumb()` 时禁用，smart mode 恢复。

**理由**：避免每次编辑全项目扫描；索引与缓存能支撑多文件符号；dumb mode 降级保证 IDE 启动期间可用。

**替代方案**：每次实时扫描所有注解（性能差，放弃）；自建持久化索引（复杂度高、维护成本大，放弃）。

**性能目标**：500 个 SpEL 片段的文件首次高亮不超过 1 秒；补全响应不超过 300 毫秒；PSI 读取放在后台 read action，不阻塞 EDT。

### D10. YAML 配置支持作为可选能力

**选择**：以可选依赖接入 YAML PSI；识别配置模型中 `SpELImportConf` 对应的节点（`root`/`val`/`rootLit`/`varLit`/`classes`/`pack`/`running`），键名兼容 camelCase 与 kebab-case；复用 Java 的符号模型、引用、补全与检查；未安装 YAML 插件时自动降级，不影响 Java 功能。

**理由**：配置 API 与注解 API 的 SpEL 语义一致；可选依赖避免强制安装 YAML 插件，符合「社区版即可运行」的约束。

**替代方案**：自己解析 YAML 文本（无法复用 YAML PSI、易错，放弃）；强制依赖 YAML 插件（增加安装门槛，放弃）。

**作用域**：导入声明只在同一 YAML 文件/同一 API 节点内生效，不跨文件泄漏；这与 `specs/spel-yaml-configuration-support/spec.md` 的隔离要求一致。

### D11. 受控求值

**选择**：求值功能通过项目 classpath 的 `URLClassLoader` 反射加载 `spring-expression`，用受限的 `StandardEvaluationContext` 执行；`TypeLocator` 采用白名单（`java.lang`、`java.util`、`java.time`、`java.math`、`luckyframework` 等），`MethodResolver` 过滤文件、网络、进程、反射修改类方法；求值在后台线程执行并设置超时（约 3 秒），失败只显示错误，不修改源文件。

**理由**：不打包 Spring，复用用户项目版本，避免类加载冲突；specs 要求禁止文件/网络/进程操作；求值是独立能力，失败不影响高亮/补全/跳转。

**替代方案**：打包 `spring-expression`（版本冲突、插件体积增大，放弃）；调用框架 `SpELConvert`（初始化上下文、可能触发副作用，放弃）；不做求值（specs 要求，放弃）。

**安全边界**：JDK 21 的 `SecurityManager` 已弃用，不能作为沙箱；只能靠类型/方法白名单与超时。设计上明确「求值是开发者主动触发的本地预览，不是对抗恶意表达式的安全边界」；如果后续需要更强隔离，再评估独立进程方案。

### D12. 测试与符号表校验

**选择**：

- 解析器/符号表：纯 Kotlin 单元测试，覆盖边界、嵌套、`@if`、`@max`、参数六种命名、`@SpELImport` 键值解析；
- PSI 集成：`BasePlatformTestCase`/`LightJavaCodeInsightFixtureTestCase` + `testData` fixture，覆盖补全、跳转、检查、快速修复；
- 符号表：生成脚本从框架源码产出 JSON，CI 比对生成结果；插件测试加载 JSON 校验关键内置符号；
- 求值：用 JDK 自带类验证白名单，用临时文件/网络地址验证拒绝路径；
- 性能：大文件 fixture 验证高亮/补全的时间目标。

**理由**：插件测试需要 PSI/索引，平台测试夹具可以离线运行；不依赖真实框架启动，符合仓库「所有验证可离线」的约定。

**替代方案**：启动真实项目做端到端测试（慢、不稳定，放弃）；只做纯单元测试（无法覆盖 PSI 集成，放弃）。

### D13. 版本与兼容策略

**选择**：插件从 `0.1.0` 起步；`sinceBuild=241`（2024.1），`untilBuild` 不设硬上限；通过依赖中 `lucky-httpclient` 的类存在性激活；内置符号表按框架 `3.0.x` 生成，检测到未知版本时显示「未验证版本」提示但不阻断功能。

**理由**：插件与框架独立发布，硬绑定版本会增加升级成本；类存在性激活对源码模块与 jar 都适用。

**替代方案**：与框架版本严格绑定（每次框架升级都要发插件，放弃）；不提示版本差异（用户难以判断符号表是否过期，放弃）。

## Risks / Trade-offs

- [注解属性表覆盖不全，部分 SpEL 属性没有高亮/补全] → 先覆盖高频注解；未知属性不参与未解析检查；生成脚本 + 测试保障；后续用 `@SpELSupport` 元注解替代人工表。
- [运行期动态注册的变量/函数无法静态还原，补全可能漏项] → 默认函数类与项目内 `addSpringElFunctionClass` 调用按三路并集识别；反射或外部配置注册无法静态还原时，未知上下文不报错；补全尽量全量；悬停/文档标注来源；检查默认 warning 且可关闭。
- [类型推导失败导致成员补全退化] → 返回通用成员并标注「类型未知」；不显示空列表；后续按使用频率补充内置类型表。
- [组合注解与 `@AliasFor` 解析复杂，容易漏掉元注解上的声明] → 递归解析 + 循环检测；用 `@Get`/`@Post`/`@Sse`/`@SpELImport` 组合注解 fixture 覆盖。
- [dumb mode 下索引不可用] → 高亮降级；补全/跳转/检查等待 smart mode；不阻塞 IDE 启动。
- [YAML 插件缺失或配置键名不一致] → 可选依赖；键名兼容 camelCase/kebab-case；无 YAML 插件时 Java 功能不受影响。
- [求值沙箱无法完全阻止恶意 SpEL] → 白名单 + 方法过滤 + 超时；明确不是安全边界；失败不影响其他功能。
- [框架 `varLit` 运行期偏差] → 按公开语义实现；design 与测试记录偏差；不与运行期 bug 对齐。
- [大文件/大量 SpEL 片段导致高亮变慢] → 只处理含 `#{` 的字符串；缓存解析结果；性能 fixture 设定阈值。
- [插件与框架版本漂移导致内置符号过期] → 符号表版本化；未知版本提示；CI 比对框架源码生成结果。
- [用户项目只有 jar、没有源码/Javadoc] → PSI 解析 jar 成员；文档降级为签名；内置符号表不依赖源码。

## Migration Plan

1. 在 `D:\Lucklike\luckliy\lucky-httpclient-idea-plugin` 建立独立 Gradle 项目；根 `pom.xml` 不新增模块。
2. 先交付可安装骨架：`plugin.xml`、激活检查、空的高亮/补全/跳转注册，验证 `runIde` 与 `buildPlugin`。
3. 按能力分阶段实现：上下文/符号模型 → 高亮 → 补全 → 跳转/文档 → 检查/快速修复 → 重构 → YAML → 求值。
4. 每个阶段用 `testData` fixture 与离线测试验证，不依赖真实 HTTP 服务。
5. 通过 IDE 的「Install Plugin from Disk」安装 `build/distributions/*.zip`；回滚时卸载插件并删除目录即可，不影响框架源码与 Maven 构建。
6. `@SpELSupport` 元注解作为后续独立 change，不在本次实施；如果后续引入，插件元数据表改为「优先读元注解，缺失时回退静态表」。

## Open Questions

- Kotlin 字符串中的 SpEL 是否在后续版本支持？当前 specs 只要求 Java 注解与 YAML，暂列 Non-Goal。
- 插件是否发布到 JetBrains Marketplace、是否需要签名与自动更新？不影响当前实现与任务拆分。
- 求值是否需要独立的进程级隔离？当前白名单 + 超时满足 specs；如果后续安全要求提高，再单独评估。
- 内置符号表是否需要覆盖 `generalapi` 的扩展注解（OAuth2、AI、分片上传等）？可按实际使用频率逐步扩展，不影响架构。
