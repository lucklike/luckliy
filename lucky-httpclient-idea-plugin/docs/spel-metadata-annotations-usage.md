# SpEL 元数据注解使用指南与改动量评估（精简版）

> 状态：设计提案（v2，取代此前的 15 注解方案）  
> 关联文档：[SpEL 元数据注解设计（精简版）](spel-metadata-annotations.md)  
> 核心原则：**能从 Java 注释/Java 签名准确读取的信息，一律不写注解。**

---

## 1. 使用模型

### 1.1 信息从哪里来

| 信息 | 来源 | 是否写注解 |
|---|---|---|
| 属性说明、变量说明、函数说明 | Javadoc | 否 |
| 函数入参、返回值、泛型、重载、可变参数 | Java 方法签名 | 否 |
| 注解属性默认值 | 注解元素的 `default` | 否 |
| `@since`、`@deprecated`、`@see`、示例 | Javadoc | 否 |
| 函数别名、命名空间 | `@FunctionAlias`、`@Namespace` | 否 |
| Bean 名称/类型 | Spring `@Component`、`@Bean`、类名 | 否 |
| **哪些属性支持 SpEL** | `@Spel` | **是** |
| **值形态（模板/键值/声明/条件/字面量）** | `@Spel.kind` | **是** |
| **表达式期望结果类型** | `@Spel.result`/`constraint` | **是** |
| **生命周期阶段** | `@Spel.stage` | **是** |
| **分隔符、key/value 是否求值** | `@Spel` | **是** |
| **字段/方法/参数是否暴露为符号** | `@SpelSymbol` | **是** |

### 1.2 三个注解

| 注解 | 用在哪里 | 解决什么 |
|---|---|---|
| `@Spel` | 注解类型、注解元素方法、配置字段 | 哪些属性支持 SpEL、怎么解析、结果是什么、何时执行 |
| `@SpelSymbol` | 字段、方法、参数、注解类型 | 这个字段/方法/参数/参数注解是否暴露为 SpEL 符号 |
| `@SpelConfig` | 配置 POJO 类 | 配置前缀、命名策略、默认阶段 |

三个注解放在 `lucky-httpclient` 的 `com.luckyframework.httpclient.proxy.spel.meta` 包；`Lifecycle` 复用 `com.luckyframework.httpclient.proxy.spel.hook` 的现有枚举，只增加 `INHERIT`/`NEVER`/`UNKNOWN` 三个元数据值。

### 1.3 两条规则

1. **能读注释就不写注解。** 说明、参数、返回值、示例、版本、弃用信息全部放 Javadoc。
2. **能类型级默认就不逐属性写。** 一个注解通常只写一个类型级 `@Spel`，只有特殊属性才覆盖。

---

## 2. 快速开始：给 `@Retryable` 接入 SpEL

### 之前：需要逐属性写 15 个注解属性

```java
// v1 方案：每个属性都要写 @SpelValue，还要写 description/examples/since...
@SpelValue(kind = TEMPLATE, lifecycle = RETRY, result = @SpelType(String.class),
           description = "任务名称", examples = {...})
String name() default "...";
```

### 现在：一个类型级声明 + 两个特殊属性

```java
@Spel(
    only = {"name", "retryCount", "waitMillis", "maxWaitMillis", "minWaitMillis", "multiplier"},
    stage = Lifecycle.RETRY,
    result = String.class
)
public @interface Retryable {

    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "true";

    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    String condition() default "#{($status$ >= 500 and $status$ < 600) or {408, 429}.contains($status$)}";
}
```

说明：

- 6 个模板属性由类型级 `only` 一次性声明；
- `enable`/`condition` 结果必须是布尔，属性级覆盖；
- 属性说明、默认值、参数说明、示例全部来自 Javadoc 和 `default`，不需要写。

插件看到：

- `name`、`retryCount` 等属性中的 `#{...}` 被高亮；
- `enable`/`condition` 的结果按布尔检查；
- 补全 `$status$` 等变量；
- 悬停显示 Javadoc 中的说明。

---

## 3. 三个注解的详细用法

### 3.1 `@Spel`

#### 用法一：类型级 `only`，精确列出支持 SpEL 的属性

```java
@Spel(
    only = {"url"},
    stage = Lifecycle.REQUEST_INIT,
    result = String.class
)
public @interface HttpRequest {

    @AliasFor("url")
    String value() default "";

    @AliasFor("value")
    String url() default "";

    @Spel(disabled = true)
    String func() default "";

    RequestMethod method() default RequestMethod.NON;
}
```

- `only` 里的属性默认是模板，结果 String；
- `value` 是 `url` 的别名，按 effective attribute 继承；
- `func` 显式关闭，避免被误判；
- `method` 是枚举，不在 `only` 列表，自动不参与。

#### 用法二：类型级 `all=true`，所有 String 属性默认支持

```java
@Spel(
    all = true,
    stage = Lifecycle.REQUEST_INIT,
    result = String.class
)
public @interface MyApi {

    String path() default "";

    String description() default "";

    @Spel(disabled = true)
    String internalFlag() default "";
}
```

- `path`、`description` 自动支持 SpEL；
- `internalFlag` 是字符串但不想支持，用 `disabled=true` 排除一次；
- 适合属性多、语义统一的注解。

#### 用法三：属性级覆盖结果类型

```java
@Spel(
    only = {"value", "file", "fileId", "fileName",
            "chunkSize", "maxConcurrency", "maxRetryCount", "retryBackoffMillis"},
    stage = Lifecycle.REQUEST_INIT,
    result = String.class
)
public @interface ChunkUpload {

    @Spel(result = Long.class)
    String chunkSize() default "";

    @Spel(result = Integer.class)
    String maxConcurrency() default "";

    @Spel(result = Integer.class)
    String maxRetryCount() default "";

    @Spel(result = Long.class)
    String retryBackoffMillis() default "";
}
```

这 4 个属性之前因为通过 `parseLongExpression`/`parseIntExpression` 间接求值而漏检；现在只要一个类型级 `@Spel` + 4 个结果类型覆盖。

#### 用法四：条件属性

```java
@Spel(kind = SpelKind.CONDITION, result = boolean.class)
String condition() default "";
```

`kind=CONDITION` 表示结果必须是布尔。插件据此检查 `#{...}` 的结果类型。

#### 用法五：静态生效条件

```java
@Spel(
    kind = SpelKind.CONDITION,
    result = boolean.class,
    condition = "enable != false"
)
String condition() default "...";
```

`condition` 只支持属性引用、字面量、比较、逻辑、括号；不支持函数调用/Bean 访问。`enable=false` 时插件不再对 `condition` 做未解析符号检查。

#### 用法六：键值/声明形态

```java
@Spel(
    only = {"root", "var", "rootLit", "varLit"},
    kind = SpelKind.KEY_VALUE,
    stage = Lifecycle.METHOD,
    separator = "="
)
public @interface SpELImport {

    @Spel(separatorAttribute = "separator")
    String[] root() default {};

    @Spel(valueSpel = false, separatorAttribute = "separator")
    String[] rootLit() default {};

    @Spel(valueSpel = false, separatorAttribute = "separator")
    String[] varLit() default {};

    @Spel(disabled = true)
    String separator() default "=";
}
```

- `root`/`var` 的 value 参与求值；
- `rootLit`/`varLit` 的 value 不参与求值，仅作为字面量；
- 分隔符来自同注解的 `separator` 属性，用 `separatorAttribute` 指向。

#### 用法七：泛型结果

```java
@Spel(
    only = {"type"},
    result = Object.class,
    typeExpression = "java.util.List<com.example.Item>"
)
public @interface MyListApi { ... }
```

`result=Object.class` 表示不限制；`typeExpression` 用于泛型/复杂类型。只有 `Class<?>` 无法表达时才用。

#### `@Spel` 属性速查

| 属性 | 何时写 | 例子 |
|---|---|---|
| `disabled` | 排除类型级 `all/only` 中的属性 | `@Spel(disabled=true)` |
| `kind` | 非模板形态 | `@Spel(kind=CONDITION)` |
| `stage` | 与类型级默认不同 | `@Spel(stage=RETRY)` |
| `result` | 结果不是 String | `@Spel(result=Integer.class)` |
| `typeExpression` | 泛型/复杂类型 | `@Spel(typeExpression="List<Item>")` |
| `constraint` | 二进制/类型元数据 | `@Spel(constraint=BINARY)` |
| `separator` | KEY_VALUE/DECLARATION | `@Spel(separator="=")` |
| `separatorAttribute` | 分隔符来自另一属性 | `@Spel(separatorAttribute="separator")` |
| `keySpel` | key 也要求值 | `@Spel(keySpel=true)` |
| `valueSpel` | value 不要求值 | `@Spel(valueSpel=false)` |
| `only` | 类型级，精确列出 | `@Spel(only={"url"})` |
| `all` | 类型级，所有 String 属性 | `@Spel(all=true)` |
| `condition` | 静态生效条件 | `@Spel(condition="enable != false")` |

---

### 3.2 `@SpelSymbol`

#### 用法一：内置变量（信息全从 Java 常量 + Javadoc 读取）

```java
public final class InternalRootVarName {

    /**
     * HTTP 响应状态码。
     *
     * @spelType java.lang.Integer
     * @spelStage RESPONSE
     * @sample 200
     */
    @SpelSymbol
    public static final String $_RESPONSE_STATUS_$ = "$status$";
}
```

插件读取：

- 名称：常量值 `"$status$"`；
- 类型：Javadoc `@spelType`，或字段类型；
- 阶段：Javadoc `@spelStage`，或默认阶段；
- 说明：Javadoc 第一段；
- 示例：Javadoc `@sample`。

如果不想用自定义 Javadoc tag：

```java
@SpelSymbol(type = Integer.class, from = Lifecycle.RESPONSE)
public static final String $_RESPONSE_STATUS_$ = "$status$";
```

#### 用法二：函数（签名/Javadoc 全自动）

```java
@Namespace("crypto")
public final class CryptoFunctions {

    /**
     * 计算 MD5。
     *
     * @param input 输入字符串
     * @return 32 位小写 MD5
     */
    @SpelSymbol
    @FunctionAlias("md5")
    public static String md5(String input) { ... }
}
```

插件读取：

- 名称：`@FunctionAlias("md5")`；
- 命名空间：`@Namespace("crypto")`；
- 入参：`String input`；
- 返回值：`String`；
- 说明/参数/返回值：Javadoc。

**不需要**写 `@SpelSymbol(name="md5", type=String.class, description="...")`。

#### 用法三：内置 `@Param`（插件内置识别）

`@Param` 在 `lucky-core`，`@SpelSymbol` 在 `lucky-httpclient`，core 不能反向依赖 httpclient，所以 `@Param` 不标注，由插件硬编码识别：

- 注解全限定名：`com.luckyframework.reflect.Param`；
- 变量名：`@Param#value`，空时用参数名；
- 变量类型：参数类型；
- 同时生成 `$userId`、`$0`、`$$0`、`userId::type`、`$0::type`、`$pc$userId`、`$pc$0`。

使用：

```java
@Get("/users/#{$userId$}")
String getUser(@Param("userId") String id);
```

#### 用法四：用户自定义参数注解

```java
@SpelSymbol(nameAttribute = "name", kind = SpelSymbolKind.PARAM)
public @interface MyParam {
    String name();
}
```

插件读取 `nameAttribute` 指向的 `name` 属性作为变量名，参数类型作为变量类型。

#### 用法五：Bean（优先用 Spring 注解，必要时覆盖）

```java
@Component("userService")
public class UserService { ... }
```

插件从 Spring 注解读取 Bean 名和类型，不需要 `@SpelSymbol`。只有需要覆盖名称/阶段时：

```java
@SpelSymbol(name = "users", from = Lifecycle.REQUEST_INIT)
@Component("userService")
public class UserService { ... }
```

#### `@SpelSymbol` 属性速查

| 属性 | 何时写 | 例子 |
|---|---|---|
| `name` | 名称无法从常量/方法/参数推导 | `@SpelSymbol(name="users")` |
| `kind` | 符号类型无法自动推导 | `@SpelSymbol(kind=BEAN)` |
| `type` | 字段/方法类型不能代表变量类型 | `@SpelSymbol(type=Integer.class)` |
| `typeExpression` | 泛型类型 | `@SpelSymbol(typeExpression="List<Item>")` |
| `from` | 阶段无法从 Javadoc 读取 | `@SpelSymbol(from=RESPONSE)` |
| `until` | 有明确结束阶段 | `@SpelSymbol(until=DESTROY)` |
| `nameAttribute` | 仅用户自定义参数注解，变量名来自哪个属性 | `@SpelSymbol(nameAttribute="name")` |

---

### 3.3 `@SpelConfig`

#### 用法一：配置模型

```java
@SpelConfig(prefix = "lucky.api", naming = SpelNaming.KEBAB_CASE)
public class RetryConf {

    /**
     * 重试条件表达式。
     */
    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    private String condition;

    /**
     * 最大重试次数。
     */
    @Spel(result = Integer.class)
    private String maxCount;

    /**
     * 是否开启重试。
     * 不支持 SpEL，不标 @Spel。
     */
    private Boolean enable;
}
```

插件读取：

- key：`condition` → `condition`；`maxCount` → `max-count`；
- 类型：字段类型；
- 说明：Javadoc；
- 默认阶段：`@SpelConfig.stage`；
- `enable` 没标 `@Spel`，按普通布尔配置处理。

#### 用法二：显式 key

如果字段名不能直接映射，先在 Javadoc 写 `@key`：

```java
/**
 * 最大重试次数。
 *
 * @key retry-count
 */
@Spel(result = Integer.class)
private String maxCount;
```

如果后续发现 Javadoc tag 不够，再给 `@Spel` 加一个 `key` 属性；v2 先保持最简。

#### 用法三：配置别名

```java
@SpelConfig(
    prefix = "lucky.api",
    aliases = {"lucky.http-client"},
    naming = SpelNaming.KEBAB_CASE
)
public class ConfigApi { ... }
```

---

## 4. 六个真实场景

### 4.1 场景一：框架新增一个支持 SpEL 的注解

```java
@Spel(
    only = {"path", "description"},
    stage = Lifecycle.REQUEST_INIT,
    result = String.class
)
public @interface MyApi {

    String path() default "";

    String description() default "";

    @Spel(disabled = true)
    String internalFlag() default "";
}
```

使用：

```java
@MyApi(path = "/users/#{$id$}", description = "查询 #{#userId}")
String query(@Param("id") String id, @Param("userId") String userId);
```

插件自动识别，不需要改插件代码。

### 4.2 场景二：用户自定义组合注解

```java
@Retryable
@Get("/users/#{$id$}")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GetUser {
}
```

插件按元注解链找到 `@Get`/`@HttpRequest`/`@Retryable` 上的 `@Spel`，自动继承 `url`、`condition` 等语义。用户不需要重复声明 SpEL 支持。

### 4.3 场景三：用户自定义参数注解

```java
@SpelSymbol(nameAttribute = "name", kind = SpelSymbolKind.PARAM)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyParam {
    String name();
}
```

使用：

```java
@Get("/orders/#{$orderId$}")
String getOrder(@MyParam(name = "orderId") String id);
```

插件补全 `#orderId`，并生成 `$orderId`、`$0`、`$pc$orderId` 等别名。

### 4.4 场景四：新增内置变量

```java
/**
 * 请求唯一 ID。
 *
 * @spelType java.lang.String
 * @spelStage REQUEST_INIT
 * @sample req-001
 */
@SpelSymbol
public static final String $_REQUEST_ID_$ = "$requestId$";
```

插件读取常量值、Javadoc 类型/阶段/示例，不需要写 `@SpelVariable(name=..., type=..., from=..., description=...)`。

### 4.5 场景五：新增函数

```java
@Namespace("crypto")
public final class CryptoFunctions {

    /**
     * 计算 MD5。
     *
     * @param input 输入字符串
     * @return 32 位小写 MD5
     */
    @SpelSymbol
    @FunctionAlias("md5")
    public static String md5(String input) { ... }
}
```

插件从 `@FunctionAlias` 读名称、`@Namespace` 读命名空间、方法签名读参数/返回值、Javadoc 读说明。不需要重复写。

### 4.6 场景六：新增 YAML 配置项

```java
@SpelConfig(prefix = "lucky.custom", naming = SpelNaming.KEBAB_CASE)
public class CustomConf {

    /**
     * 服务地址。
     */
    @Spel(result = String.class)
    private String endpoint;

    /**
     * 超时毫秒数。
     */
    @Spel(result = Integer.class)
    private String timeout;
}
```

YAML：

```yaml
lucky:
  custom:
    endpoint: "https://#{#host}/api"
    timeout: 3000
```

插件对 `endpoint` 做 SpEL 高亮/补全，对 `timeout` 做整数结果检查。

---

## 5. 改动量评估

### 5.1 统计基线

| 指标 | 数量 | 说明 |
|---|---:|---|
| `annotations.json` 注解 | 68 | 实际参与 SpEL 的注解 |
| `annotations.json` 属性 | 162 | 需要迁移的元数据总量 |
| `TEMPLATE` 属性 | 102 | 类型级 `only`/`all` 批量声明 |
| `IF_CONDITION` 属性 | 31 | 属性级 `kind=CONDITION` |
| `NONE` 属性 | 25 | 不写，或 `disabled=true` |
| `KEY_VALUE` 属性 | 4 | 属性级 `kind=KEY_VALUE`/`DECLARATION` |
| `parseExpression` 调用点 | 284 | 不修改，只做一致性校验 |
| `@FunctionAlias` | 132 | 只给需要覆盖的函数写 `@SpelSymbol` |
| 内置变量常量 | 73 | 每个加 `@SpelSymbol`，其余从常量/Javadoc 读取 |
| 配置 API 非静态字段 | 约 234 | 只标支持 SpEL 的 30–50 个 |

### 5.2 声明量对比

| 项目 | v1（15 注解） | v2（3 注解） |
|---|---:|---:|
| 新增注解 | 15 | 3 |
| 注解属性总数 | ~150 | ~25 |
| 类型级 `@Spel` | 无 | 68 个，每个一行 |
| 属性级覆盖 | ~162 个 `@SpelValue` | ~60 个 `@Spel` |
| 符号声明 | ~100 个 `@SpelVariable` | ~100 个 `@SpelSymbol` |
| 配置声明 | ~50 个 `@SpelConfigProperty` | ~5 个 `@SpelConfig` + ~50 个 `@Spel` |
| 文档/示例/版本 | 全部写注解 | Javadoc 读取 |
| 框架侧声明处 | ~500 | ~250–300 |

### 5.3 P0 改动清单（核心三注解）

| 项目 | 文件数 | 新增/修改行数 | 说明 |
|---|---:|---:|---|
| 新增 httpclient `meta` 包 | 7 | ~450 | 3 个注解 + 4 个枚举 |
| 扩展 `Lifecycle` | 1 | ~10 | 新增 `INHERIT`/`NEVER`/`UNKNOWN` + 运行期过滤 |
| 核心注解迁移 | 6 | ~200 | `HttpRequest`、`ServerAddress`、`Retryable`、`ConvertMetaType`、`Binary`、`SpELImport` |
| 生成器改造 | 1 | ~300 | 注解 + Javadoc → 索引；一致性校验 |
| 插件读取端 | 4 | ~500 | 索引加载、`@Spel`/`@SpelSymbol` 解析、Javadoc 解析、缓存 |
| 测试夹具 | 8 | ~300 | 注解解析、别名、阶段、Javadoc、回退 |
| **合计** | **27** | **~1760** | **3–4 人天** |

### 5.4 P1 改动清单（符号与配置）

| 项目 | 文件数 | 新增/修改行数 | 说明 |
|---|---:|---:|---|
| 参数注解 | 3 | ~20 | `@Var`、`@Bean`、`@Env` 加 `@SpelSymbol`；内置 `@Param` 由插件硬编码识别 |
| 内置变量 | 3 | ~150 | 73 个常量加 `@SpelSymbol` |
| 函数 | 2 | ~100 | 20–30 个公共函数加 `@SpelSymbol` |
| 配置模型 | 10 | ~150 | `@SpelConfig` + 30–50 个字段 `@Spel` |
| 插件扩展 | 4 | ~300 | 符号/配置索引与缓存 |
| **合计** | **22** | **~720** | **2–3 人天** |

### 5.5 P2 改动清单（Javadoc 增强）

| 项目 | 文件数 | 新增/修改行数 | 说明 |
|---|---:|---:|---|
| Javadoc 示例解析 | 3 | ~200 | `<pre>`/`@sample`、`@since`、`@deprecated` |
| 悬停/补全展示 | 3 | ~200 | 说明、参数、返回值、示例 |
| 测试 | 4 | ~150 | Javadoc 缺失、格式异常、降级 |
| **合计** | **10** | **~550** | **1–2 人天** |

### 5.6 工作量汇总

| 阶段 | 人天 | 产出 |
|---|---:|---|
| P0 | 3–4 | 三注解 + 6 个核心注解 + 索引 + 插件读取 |
| P1 | 2–3 | 参数/变量/函数/配置 |
| P2 | 1–2 | Javadoc 增强 |
| **合计** | **6–9** | 不含 UI 大改和完整测试矩阵 |

相比 v1 的 13–19 人天，减少约 50%。

### 5.7 改动量最大的五个点

1. **Javadoc 解析器**：需要读取 `PsiDocComment` 的第一段、`@param`、`@return`、`@since`、`@deprecated`、`<pre>`；约 200–300 行，是 v2 新增的核心工作量。
2. **插件读取端**：索引加载、`@Spel`/`@SpelSymbol` 解析、缓存，约 500 行。
3. **68 个类型级 `@Spel`**：每个注解一行，约 68 处。
4. **60 个属性级覆盖**：31 个 CONDITION + 4 个 KEY_VALUE + 约 25 个结果类型覆盖。
5. **73 个内置变量**：每个加 `@SpelSymbol`，但名称/类型/说明从常量/Javadoc 读取，不重复写。

### 5.8 不会改的地方

- 284 个 `parseExpression` 调用点：不改，只在 CI 做一致性校验。
- 运行时行为：不改；注解不参与求值。
- 现有注解的 `@Retention`/`@Target`：不改。
- 用户业务代码：不改。
- 插件的高亮/补全/检查主体：不改，只换元数据来源。

### 5.9 风险与回退

| 风险 | 应对 |
|---|---|
| Javadoc 格式不一致 | 只读取结构化 tag（`@param`/`@return`/`@since`/`@deprecated`/`<pre>`），不猜自然语言 |
| Javadoc 缺失 | 降级为“无说明”，不影响高亮/补全/检查 |
| 注解与调用点不一致 | 生成器做“调用点 → 声明”一致性校验，CI 失败 |
| 用户自定义注解无法解析 | 未标注时回退到 `annotations.json` |
| `@Param` 内置规则与框架语义漂移 | 规则集中到单一解析器，配契约测试；`@Param` 变更时同步 |
| 性能下降 | 构建期索引 + `PsiClass` 缓存 + 基准测试 |
| 枚举新增值 | 未知值降级 `UNKNOWN`，插件不崩溃 |

---

## 6. 验收清单

- [ ] 每个 SpEL 注解至少有一个类型级 `@Spel`（`only` 或 `all`）。
- [ ] 每个非 String 结果属性有属性级 `result`/`constraint` 覆盖。
- [ ] `@Spel(disabled=true)` 能正确排除属性。
- [ ] `@AliasFor` 别名继承 effective attribute 的声明。
- [ ] `Lifecycle` 新增的 `INHERIT`/`NEVER`/`UNKNOWN` 不会被注册为 Hook 阶段。
- [ ] 73 个内置变量的名称/类型/阶段能从常量、字段类型、Javadoc 读取。
- [ ] 132 个函数不需要逐个写注解；只给需要覆盖的函数写 `@SpelSymbol`。
- [ ] 配置 key 能按 `naming` 从字段名生成。
- [ ] Javadoc 缺失时插件降级，不报错。
- [ ] 补全 P95 延迟不超过迁移前 10%。
- [ ] 用户自定义注解/参数注解示例工程通过。

---

## 7. 建议的实施顺序（按 PR 拆分）

| PR | 内容 | 依赖 |
|---|---|---|
| PR1 | 在 `lucky-httpclient` 新增 `meta` 包：3 个注解 + 4 个枚举 + 扩展 `Lifecycle` + 单元测试 | 无 |
| PR2 | 迁移 6 个核心注解：`HttpRequest`、`Retryable`、`ConvertMetaType`、`Binary`、`SpELImport`、`ServerAddress` | PR1 |
| PR3 | 生成器：注解 + Javadoc → 索引 + 一致性校验 | PR1、PR2 |
| PR4 | 插件：读取索引 + 按需 PSI + 缓存 + 回退 | PR3 |
| PR5 | `@SpelSymbol`：用户自定义参数注解、内置变量、公共函数；`@Param` 内置识别 | PR4 |
| PR6 | `@SpelConfig`：配置模型 + YAML 映射 | PR4 |
| PR7 | Javadoc 增强：示例、`@since`、`@deprecated`、悬停文档 | PR4 |

---

## 8. 结论

v2 的接入成本可以压到**一个类型级 `@Spel` + 少量属性级覆盖**：

```java
@Spel(only = {"url"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface HttpRequest { ... }
```

其余信息全部交给 Javadoc 和 Java 签名。真正的工作量不在“写注解”，而在：

1. Javadoc 解析器；
2. 插件读取端与缓存；
3. 注解声明与调用点的一致性校验。

这三块合计约 **1000–1200 行、3–5 人天**；加上注解迁移和测试，完整 P0+P1+P2 约 **6–9 人天**。
