## 1. 工程骨架与构建

- [x] 1.1 在 `lucky-httpclient-idea-plugin` 创建独立 Gradle 工程（`settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`.gitignore`），用本机 Gradle 8.13（`D:\JAVA_EVN\gradle-8.13-bin\gradle-8.13\bin\gradle.bat`）生成 Wrapper 供他人使用（验证：`gradle --version` 显示 Gradle 8.13 + JVM 21.0.12；`gradlew.bat --version` 通过；本机日常构建统一用 `gradle` 命令，避免重复下载发行版）
- [x] 1.2 固定构建 JDK：`gradle.properties` 配置 `org.gradle.java.home=D:\jdk21`，toolchain 使用 Java 21（验证：`gradle -q javaToolchains` 列出 21.0.12，构建不读取 `JAVA_HOME=D:\jdk`）
- [x] 1.3 接入 IntelliJ Platform Gradle Plugin 2.x，目标 `intellijIdeaCommunity("2024.1")`、`sinceBuild=241`、不设 `untilBuild`，只启用 Java 模块（验证：`gradle verifyPluginProjectConfiguration` 通过；首次构建需联网下载 2024.1 平台产物（约 1.2–1.6 GB，缓存在 Gradle 目录），本机无需安装 2024.1，缓存后离线可用）
- [x] 1.4 创建最小 `plugin.xml` 与平台测试夹具（`BasePlatformTestCase` 基类、`src/test/testData`）（验证：`PluginSmokeTest` 通过，`gradle buildPlugin` 在 `build/distributions/` 产出 zip）

## 2. 最小垂直骨架

- [x] 2.1 实现 `SpelFragment`/`SpelToken` 与最小 `SpelScanner`（仅单层 `#{}`），建立「字符串转义偏移 → PSI 文本偏移」映射（验证：`SpelScannerTest` 断言 `"a#{#x}b"` 得到 1 个片段、范围 2..5，并覆盖 `\"`/`\\` 偏移映射）
- [x] 2.2 注册 `Annotator`，对含 `#{` 的 `PsiLiteralExpression` 加定界符高亮（验证：fixture 断言 `HighlightInfo` 覆盖 `#{` 与 `}`）
- [x] 2.3 注册 `CompletionContributor`，在 `#{#` 后返回固定候选 `#demo`（验证：`lookupElementStrings` 含 `#demo`）
- [x] 2.4 注册 `PsiReferenceContributor`，让 `#{#demo}` 解析到字符串字面量内的伪声明，并把三个扩展点汇总进 `plugin.xml`（验证：`resolve()` 非空且 `isSoft=true`，`gradle buildPlugin` 仍通过）

## 3. 依赖感知激活与全局开关

- [x] 3.1 实现 `LuckyHttpClientPresence`：`JavaPsiFacade.findClass("com.luckyframework.httpclient.proxy.annotations.HttpRequest", moduleScope)` 判定依赖，并按 `Module` 缓存（验证：`PresenceServiceTest` 中带桩注解的模块返回 true，空模块返回 false）
- [x] 3.2 用 `ProjectActivity` 初始化激活状态，监听 `ModuleRootListener` 与 `PsiModificationTracker` 刷新缓存（验证：fixture 动态加入依赖库后结果翻转）
- [x] 3.3 在 Annotator/Completion/Reference/Inspection 入口统一调用 `isEnabledFor(element)`（验证：不依赖模块的 fixture 中 `HighlightInfo` 为空、lookup 为空、`resolve()` 为 null）
- [x] 3.4 实现 `LuckyHttpClientSettings`（`PersistentStateComponent`）与设置页（`SearchableConfigurable`），默认启用；关闭后立即失效（验证：`SettingsTest` 切换后 `isEnabled()` 为 false、`Annotator` 不再产出，且文件 PSI 文本不变）
- [x] 3.5 审查 PSI 写入点，确认只存在于 QuickFix 与重构（验证：全仓检索 `replace|addAfter|delete` 得到写入清单，与 design「插件只读项目依赖与源码」逐条比对，Annotator/Completion 中无写入调用）

## 4. SpEL 扫描器与语法结构

- [x] 4.1 支持同一字符串内多个片段（验证：`"X-TIME: #{#time()}; X-ID: #{#id}"` 得到 2 个片段且偏移正确）
- [x] 4.2 支持嵌套花括号、单/双引号字符串与转义（验证：`#{ {'a': '}'} }` 的片段边界为最外层 `#{...}`）
- [x] 4.3 未闭合表达式、未闭合字符串、括号不配平统一产出 `SpelProblem`（验证：`#{foo`、`#{'abc}`、`#{#fn(1, 2}` 三类 problem 的类型与位置断言）
- [x] 4.4 识别 `@if(...):`、`@max(n):` 与双反引号嵌套，前缀不进入 token 流（验证：`"@if(#{#enabled}): X"` 无 `@if` token；`` ``#{#{#key}}`` `` 得到内外两层片段）
- [x] 4.5 token 分类：变量/Root/函数/命名空间/类型/Bean/字符串/数字/运算符/关键字/属性访问（验证：`SpelTokenTest` 每类至少一条断言）

## 5. 注解元数据与组合注解解析

- [x] 5.1 定义 `spel-annotations.json` 结构与表版本字段，实现 `AnnotationMetadataLoader`（验证：`AnnotationMetadataLoaderTest` 加载内置表并断言 `HttpRequest#url` 为 `TEMPLATE`）
- [x] 5.2 编写生成脚本扫描 `lucky-httpclient/src/main/java` 全部包（含 `generalapi`）的 `parseExpression(` 调用点与注解定义（验证：输出含 `HttpRequest#url`、`ServerAddress#url/path`、`Retryable#condition`、`SpELImport#var/root`）
- [x] 5.3 人工确认高频属性的值种类（`TEMPLATE`/`KEY_VALUE`/`IF_CONDITION`/`NONE`）与生命周期并落库，未确认项保留 `UNKNOWN`（验证：`AnnotationMetadataGoldenTest` 抽查 6 个高频注解属性）
- [x] 5.4 组合注解递归 + `@AliasFor` 展开 + 循环检测；未登记属性降级 `UNKNOWN`（验证：`@Get`/`@Post`/`@Sse` 与自造组合注解 fixture 均解析出 `url`；自定义注解上的 `#{...}` 不产生未解析告警）

## 6. 符号模型与上下文推导

- [x] 6.1 定义 `SpelSymbol` 与 `SpelContext`（名称/种类/类型/来源/声明/生命周期/可见性/范围）（验证：`SpelSymbolTest` 覆盖 6 种 kind 与 5 层来源）
- [x] 6.2 方法参数六种命名注册（`name`/`$name`/`$n`/`$$n`/`name::type`/`$n::type`）与 `@Param` 覆盖（验证：`ParameterSymbolTest` 断言 3 个参数产出 18 个符号，且 `@Param("keyword")` 覆盖编译期名）
- [x] 6.3 解析 `@SpELImport` 的 `root`/`var`/`rootLit`/`varLit`/`value`/`ifs`/`pack` 与 `separator`，`varLit` 按普通变量处理（验证：`SpelImportParserTest` 覆盖默认 `=`、自定义分隔符、缺分隔符三种情况，并断言 `varLit` 种类为 NORMAL）
- [x] 6.4 符号优先级合并：方法参数 > 方法级导入 > 类级导入 > 内置 > 全局（验证：`SymbolPrecedenceTest` 对同名符号断言最终来源）
- [x] 6.5 生命周期表与阶段过滤（验证：`LifecycleTableTest` 断言 `Retryable#condition`→`RETRY`、`$body$` 从 `RESPONSE_INIT` 起可用、`$err$`→`THROWABLE`；URL 注解引用 `$body$` 得到「阶段不可用」而非未解析）
- [x] 6.6 未知上下文降级（验证：普通方法体中 `#{notASpelVariable}` 只有语法高亮，无未解析告警）

## 7. 内置符号表

- [x] 7.1 编写生成脚本读取 `InternalRootVarName`/`InternalVarName`/`function` 包，产出 `builtin-symbols.json`（名称/类型/阶段/说明/函数签名）（验证：条目数分别 ≥40、≥17、≥148）
- [x] 7.2 插件加载内置表并接入符号解析（验证：`BuiltinSymbolTest` 断言 `$status$`、`$body$`、`time`、`md5` 可解析；`BuiltinSymbolGoldenTest` 与源码常量比对一致）
- [x] 7.3 表版本与框架版本检测（验证：未知版本返回「未验证版本」提示且不阻断解析）

## 8. 高亮与语法错误

- [x] 8.1 定义 `TextAttributesKey`、`Default.xml` 与 `ColorSettingsPage`（变量/函数/Root/类型/Bean/字符串/数字/运算符/关键字/定界符）（验证：`LuckyTextAttributesTest` 断言 10 个 key 有默认值，`ColorSettingsPageTest` 断言描述符与示例代码非空）
- [x] 8.2 `Annotator` 按 token 分类型产出高亮（验证：`#{#time() + #offset}` 分别命中函数/变量/运算符样式）
- [x] 8.3 基础语法错误以 `HighlightSeverity.ERROR` 呈现且范围可定位（验证：未闭合 `#{`、括号不配平、字符串未闭合三条 fixture）
- [x] 8.4 高亮范围与性能（验证：`"hello world"`、`"a=1&b=2"` 无 `HighlightInfo`；500 片段 fixture 首次高亮 < 1s）

## 9. 补全

- [x] 9.1 `SpelCompletionContributor` 骨架与 `elementPattern`（验证：不含 `#{` 的字符串无补全结果）
- [x] 9.2 变量补全：参数六种命名、`@SpELImport`、内置、全局，带类型与来源 tail 文本（验证：`#userId` 的 tail 含「方法参数」；`#{#$` 下包含 `$keyword`、`$1`、`$$1`、`$1::String`）
- [x] 9.3 Root 变量补全且插入文本不带 `#`（验证：`user` 候选插入结果为 `user`）
- [x] 9.4 函数与命名空间补全（验证：`#ti` → `time` 含签名与返回类型；重载 fixture 列出 2 个候选；`#crypto.` 列出命名空间类静态方法）
- [x] 9.5 类型补全 `T(`（验证：`@SpELImport(pack="java.util")` 下 `T(ArrayL` → `ArrayList`）
- [x] 9.6 成员补全（验证：已知类型列出 public 字段与方法；未知类型列出通用成员并标注「类型未知」，不出现空列表）
- [x] 9.7 插入格式：函数补 `()` 光标居中、变量不重复 `#`、前缀精确匹配优先（验证：`InsertHandlerTest` 断言编辑后文本与光标位置）
- [x] 9.8 dumb mode 与无依赖模块下安全退出（验证：模拟 `DumbService` 后补全结果为空且无异常）

## 10. 引用、跳转与查找用法

- [x] 10.1 `SpelReference`（继承 `PsiReferenceBase`）与 `PsiReferenceContributor` 注册（验证：`ReferenceFixtureTest` 断言引用范围与 `isSoft=true`）
- [x] 10.2 变量跳转：参数（`$name`/`$n`/`$$n`/`::type`）、`@SpELImport` 键、内置变量、全局声明（验证：4 条 `resolve()` fixture 各自定位正确）
- [x] 10.3 内置变量在无库源码时解析到虚拟声明（验证：`resolve()` 非空且 `NavigationItem` 指向内置说明）
- [x] 10.4 函数跳转：`@FunctionAlias` 别名匹配与重载多目标（验证：`PsiPolyVariantReference` 返回 2 个目标）
- [x] 10.5 命名空间、`T(...)` 与 Bean 跳转（验证：`#crypto.md5`、`T(java.util.ArrayList)`、可解析 Bean 三条 fixture；不可解析 Bean 无目标且无弹窗）
- [x] 10.6 未解析引用不阻塞编辑（验证：输入与保存后文本不变，无未处理异常日志）
- [x] 10.7 Find Usages 跨文件按文件分组（验证：多文件 fixture 中参数、`@SpELImport` 变量、函数各找到 3 处引用）

## 11. 悬停文档

- [x] 11.1 `DocumentationProvider` 注册与变量文档（名称/来源/类型/作用域/是否 Root）（验证：`#userId` 文档含「方法参数」「String」；`user` 文档含「Root 变量」）
- [x] 11.2 函数文档（签名/返回类型/所属类，别名同时显示原方法名）（验证：`#time()` 与 `#md5('a')` 两条断言）
- [x] 11.3 内置变量文档（含义/类型/可用阶段）（验证：`$body$` 文档含「响应阶段可用」）
- [x] 11.4 未解析符号文档（验证：`#notDefined` 文档含「未解析」且非空）

## 12. 检查与快速修复

- [x] 12.1 `UnresolvedSpelSymbolInspection`（默认 warning，可配置严重级别与关闭）（验证：`InspectionFixtureTest` 通过，`plugin.xml` 中 `<localInspection>` 可加载）
- [x] 12.2 `SpelImportFormatInspection`：缺分隔符/空变量名/重复声明报 error，覆盖内置变量报 warning（验证：4 条 fixture）
- [x] 12.3 生命周期误用检查（验证：URL 注解引用 `$body$` 报「仅在响应阶段可用」）
- [x] 12.4 QuickFix：创建变量声明（方法/类作用域，保留变量名）（验证：`#token` 修复后插入 `@SpELImport(var = "token=...")`，原表达式不变）
- [x] 12.5 QuickFix：导入函数类（验证：`#md5('a')` 修复后出现类导入且引用可解析）
- [x] 12.6 QuickFix：包裹与取消包裹 SpEL（验证：选中 `#time()` 后变为 `#{#time()}`，反向操作还原）
- [x] 12.7 抑制单条提示并保留可识别标记（验证：抑制后该 `HighlightInfo` 消失，源码含抑制标记）
- [x] 12.8 `inspectionDescriptions` 描述资源（验证：`gradle verifyPluginProjectConfiguration` 通过，描述文件可加载）

## 13. 重构

- [x] 13.1 重命名 `@SpELImport` 变量并同步所有引用（验证：`RenameFixtureTest` 断言跨文件 `#{#token}` → `#{#accessToken}`）
- [x] 13.2 重命名方法参数与函数别名（验证：参数改名后 `#{#userId}` 同步；`md5` 改名后所有 `#md5(...)` 更新）
- [x] 13.3 提取表达式为变量（验证：插入 `@SpELImport(var = "t=#{#time()}")`，原位置变为 `#{#t}`）
- [x] 13.4 重命名预览与冲突中止（验证：冲突 fixture 中止且文件内容不变）

## 14. YAML 配置支持

- [x] 14.1 以可选依赖接入 YAML 插件（`plugin.xml` optional depends + Gradle optional 依赖）（验证：`verifyPluginProjectConfiguration` 通过，未安装 YAML 插件时 Java 侧测试全绿）
- [x] 14.2 YAML 字符串值识别 `#{`，排除注释、键名与非字符串值（验证：`url: "http://#{#host}/api"` 有片段；`# 示例：#{#host}` 注释无片段）
- [x] 14.3 复用符号、补全与跳转到 YAML，并保证配置作用域隔离（验证：声明 `token` 后可补全可跳转；另一文件补全不含 `#token`）
- [x] 14.4 `SpELImportConf` 键名 camelCase/kebab-case 兼容（验证：`rootLit` 与 `root-lit` 两种写法均被识别）

## 15. 受控求值

- [x] 15.1 反射加载项目 classpath 中的 `spring-expression`（不打包），缺失时给出提示（验证：`EvaluatorClassLoaderTest` 加载成功，空 classpath 返回提示）
- [x] 15.2 白名单 `TypeLocator` 与黑名单 `MethodResolver`（验证：`EvaluatorSecurityTest` 允许 `java.util.List`，拒绝文件、网络、进程调用）
- [x] 15.3 结果展示与模拟变量（验证：`#{1 + 2}` 返回 3；为 `#name` 提供 `"abc"` 后 `#{#name + '!'}` 返回 `"abc!"`；缺 `#missing` 时显示「变量 #missing 未提供」）
- [x] 15.4 超时、后台执行与失败不改源文件（验证：死循环表达式 3 秒内返回超时，执行前后文件文本一致）

## 16. 集成验证与发布准备

- [ ] 16.1 全量离线测试（验证：`gradle test` 全绿，10 个能力均至少有一条断言）
- [ ] 16.2 平台兼容校验（验证：`gradle verifyPlugin` 对 2024.1 无兼容性错误；首次会下载 2024.1 IDE 发行包，同样缓存后离线可用）
- [ ] 16.3 打包与安装（验证：`buildPlugin` 产出 zip，沙盒 `runIde` 中 Install from Disk 安装成功）
- [ ] 16.4 双沙盒冒烟：依赖项目触发 10 项能力，无依赖项目零干扰（验证：手工核对清单与截图记录）
- [ ] 16.5 README（安装、功能清单、已知限制、版本兼容、构建命令）（验证：文档存在且其中命令可复制执行）
- [ ] 16.6 根仓库零影响（验证：根 `pom.xml` 与 11 个 `lucky-*` 模块 `git diff` 为空，`git status` 仅新增插件目录）
