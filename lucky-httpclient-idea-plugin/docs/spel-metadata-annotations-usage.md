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
| **值形态（模板/键值/声明/条件/字面量/函数引用）** | `@Spel.kind` | **是** |
| **属性值是函数名（函数引用）** | `@Spel.kind=FUNCTION` | **是** |
| **函数/Hook 回调参数如何注入** | `@Param`/`@Var` 系列（框架已有注解），或按参数类型注入 | 否（插件内置识别） |
| **Hook 的阶段来源** | `@Spel.stageAttribute` + `@Hook#lifecycle` | **是（仅 `stageAttribute`）** |
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

`@Hook`、`@Callback`、`@Val`、`@Pack` 以及 `@Var`/`@Rar`/`@Env`/`@Bean` 不需要新的元数据注解类型：`@Hook`/`@Callback` 用 `@Spel.stageAttribute` 指向 `lifecycle`；`@Val` 字段值是 SpEL，阶段同样跟随 `lifecycle`，由插件内置识别；`@Pack` 字段不参与 SpEL；`@Var` 系列由插件内置注入规则识别。

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

### 现在：一个类型级声明 + 少量属性级覆盖

```java
@Spel(only = {"name"}, stage = Lifecycle.RETRY, result = String.class)
public @interface Retryable {

    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "true";

    @Spel(result = Integer.class)
    String retryCount() default "3";

    @Spel(result = Long.class)
    String waitMillis() default "1000";

    @Spel(result = Long.class)
    String maxWaitMillis() default "10000";

    @Spel(result = Long.class)
    String minWaitMillis() default "500";

    @Spel(result = Double.class)
    String multiplier() default "0";

    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    String condition() default "#{($status$ >= 500 and $status$ < 600) or {408, 429}.contains($status$)}";
}
```

说明：

- `name` 是 String 模板，由类型级 `only` 声明；
- `retryCount`/`waitMillis`/`maxWaitMillis`/`minWaitMillis`/`multiplier` 在框架中分别按 `int`/`long`/`long`/`long`/`double` 解析，用属性级 `result` 覆盖类型的 `String`；
- `enable`/`condition` 结果必须是布尔，属性级覆盖；
- 属性说明、默认值、参数说明、示例全部来自 Javadoc 和 `default`，不需要写；
- `enable`/`retryCount` 的声明端在 `@RetryMeta`，实际标注写在那里，`@Retryable` 上只保留 `@AliasFor` 别名（见设计文档 8.2）。

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

    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String func() default "";

    RequestMethod method() default RequestMethod.NON;
}
```

- `only` 里的属性默认是模板，结果 String；
- `value` 是 `url` 的别名，按 effective attribute 继承；
- `func` 是 URL 生成函数引用，用属性级 `kind=FUNCTION` 覆盖类型级默认；
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

    @AliasFor("file")
    String value() default "";

    @Spel(result = {java.io.File.class, String.class})
    @AliasFor("value")
    String file() default "";

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

这 4 个数值属性通过 `parseLongExpression`/`parseIntExpression` 间接求值，容易被漏检；现在只要一个类型级 `@Spel` + 4 个结果类型覆盖。`file` 的表达式结果可以是 `File` 或文件路径字符串，所以给了两个候选类型；`value` 是 `file` 的 `@AliasFor` 别名，按 effective attribute 继承。

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

    @Spel(separatorAttribute = "separator")
    String[] var() default {};

    @Spel(valueSpel = false, separatorAttribute = "separator")
    String[] rootLit() default {};

    @Spel(valueSpel = false, separatorAttribute = "separator")
    String[] varLit() default {};

    String separator() default "=";
}
```

- `root`/`var` 的 value 参与求值；
- `rootLit`/`varLit` 的 value 不参与求值，仅作为字面量；
- `value`/`ifs` 是 Class 导入，`pack` 是包名列表，都不参与 SpEL；
- 分隔符来自同注解的 `separator` 属性，用 `separatorAttribute` 指向。

#### 用法七：泛型结果

```java
@Spel(
    only = {"type"},
    result = java.util.List.class,
    typeExpression = "java.util.List<com.example.Item>"
)
public @interface MyListApi { ... }
```

`result` 写原始类型 `List.class`，`typeExpression` 补泛型参数；不写 `result` 只表示不限制类型。只有 `Class<?>` 无法表达时才用 `typeExpression`。

#### 用法八：多结果类型

有些属性允许返回多种类型，用 `result` 数组表达“任一类型通过”：

```java
@Spel(
    stage = Lifecycle.RESPONSE,
    result = {
        java.lang.reflect.Type.class,
        Class.class,
        org.springframework.core.ResolvableType.class,
        com.luckyframework.serializable.SerializationTypeToken.class,
        String.class
    },
    constraint = {
        SpelConstraint.TYPE,
        SpelConstraint.TYPE,
        SpelConstraint.TYPE,
        SpelConstraint.TYPE,
        SpelConstraint.STRING
    }
)
String type() default "";
```

规则：

- `result = {}`：不限制类型；
- `result = {}` 且 `constraint` 非空时，`constraint` 作为独立约束生效（如 `@Binary#file` 的 `BINARY`）；
- 多个元素：结果匹配任一候选即通过，补全只展示所有候选的公共成员；
- `typeExpression`/`constraint` 空数组或长度 1 时广播到所有候选；长度大于 1 时必须与 `result` 等长，否则报元数据声明错误；
- 单元素仍可简写为 `result = String.class`，与 `result = {String.class}` 等价，Java 注解数组属性本身支持这种简写。

数组对齐只影响静态检查，不改变运行时行为：运行时仍由框架的 `toType`/转换器处理。

#### 用法九：函数引用（`kind = FUNCTION`）

`@ServerAddress#urlFunc`/`pathFunc` 这类属性的值不是模板，而是函数名：

```java
@Spel(only = {"url", "path"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface ServerAddress {

    @AliasFor("url")
    String value() default "";

    @AliasFor("value")
    String url() default "";

    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String urlFunc() default "";

    String path() default "";

    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String pathFunc() default "";
}
```

框架的执行顺序是：

1. `url` 非空时先按 SpEL 模板求值；
2. `url` 为空或求值结果为空串时，`urlFunc` 先按 SpEL 表达式求值，结果必须是 String 函数名；
3. 按函数名查找函数并执行，返回值作为 URL。

所以插件按值的形态分别处理：

| `urlFunc` 的值 | 插件行为 |
|---|---|
| `"getUrl"` | 按函数名补全、校验、跳转 |
| `"#{#funcName}"` | 先按 SpEL 补全/检查，结果类型为 String；静态结果是函数名时再校验函数 |

检查项：

- 函数必须存在：名称来自 `@FunctionAlias` 或方法名，命名空间来自 `@Namespace`；
- 函数返回值兼容 `@Spel.result`（这里是 String）；
- 函数参数带 `@Param`/`@Var` 系列时按表达式注入（见 3.2 用法六），不带注解时由框架按类型注入（见 3.2 用法七）；两者都不满足时提示参数可能为 null；
- `url` 与 `urlFunc` 同时配置时，提示 `url` 优先。

示例函数：

```java
@Namespace("api")
public final class ApiFunctions {

    /**
     * 根据配置生成服务地址。
     */
    @FunctionAlias("getUrl")
    public static String getUrl(@Var("baseUrl") String baseUrl) { ... }
}
```

`@Var("baseUrl")` 在框架执行函数时自动注入 `#{#baseUrl}`，用户不需要在注解里传参。

同类属性还有 `@HttpRequest#func`、`@Destroy#func`、`@Wrapper#fun`、`@Get`/`@Post` 等 9 个组合注解的 `func`（`@HttpRequest#func` 的别名）、`@ServerAddress#urlFunc`/`pathFunc`、`@ConvertMetaType#typeFunc`/`respContentTypeFunc`、`@Retryable#conditionFunc`、`@ExceptionHandle#handleFunc`、`@ExceptionHandleMeta#conditionFunc`、`@ExceptionReturn#handleFunc`/`conditionFunc`（别名）、`@ExceptionFallback#conditionFunc`（别名）、`@RespConvert#resultFunc`/`metaTypeFunc`/`respContentTypeFunc`（后两个为别名）、`@ResultConvertMeta#metaTypeFunc`/`respContentTypeFunc`、`@HttpExec#execFunc`、`@Mock#enableFunc`（别名）/`mockFunc`、`@MockMeta#enableFunc`、`@MockPlugin#implFunc`。源码共 33 个属性名，其中 18 个是 `@AliasFor` 别名、15 个是声明端；当前 `annotations.json` 只收录 12 个属性名（9 个声明端），且都没有标成 `FUNCTION`，迁移时以源码为准。它们统一用 `kind=FUNCTION` 处理。

#### `@Spel` 属性速查

| 属性 | 何时写 | 例子 |
|---|---|---|
| `disabled` | 排除类型级 `all/only` 中的属性 | `@Spel(disabled=true)` |
| `kind` | 非模板形态；`FUNCTION` 表示属性值是函数名 | `@Spel(kind=CONDITION)`、`@Spel(kind=FUNCTION)` |
| `stage` | 与类型级默认不同 | `@Spel(stage=RETRY)` |
| `stageAttribute` | 阶段来自同注解的另一个属性 | `@Spel(stageAttribute="lifecycle")` |
| `result` | 结果不是 String；数组表示任一类型通过 | `@Spel(result=Integer.class)`、`@Spel(result={Type.class, String.class})` |
| `typeExpression` | 泛型/复杂类型；按索引与 `result` 对齐，长度 1 广播 | `@Spel(typeExpression="List<Item>")` |
| `constraint` | 二进制/类型元数据；对齐规则同 `typeExpression` | `@Spel(constraint=BINARY)` |
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
- 变量名：`@Param#value` 必填；为空时框架抛 `@Param annotation has no value.`，不会回退参数名；
- 变量类型：参数类型；
- 同时生成 `userId`（最终值）、`$userId`（原始值）、`$0`（最终值）、`$$0`（原始值）、`userId::type`/`$0::type`（参数类型）、`$pc$userId`/`$pc$0`（`ParameterContext`）。

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

#### 用法六：参数注入符号（`@Param`/`@Var` 系列）

函数引用和 Hook 回调方法的参数由框架在调用时自动注入，插件不需要用户传参，但需要知道每个参数对应哪个变量。规则内置在插件里，不需要写 `@SpelSymbol`：

| 注解 | 注入表达式 | 变量名 | 类型 |
|---|---|---|---|
| `@Param` | `value` 原样作为 SpEL | `value` 必填 | 参数类型 |
| `@Var(NORMAL)` | `#{#name}` | `value` 或参数名 | 参数类型 |
| `@Var(ROOT)` / `@Rar` | `#{name}` | `value` 或参数名 | 参数类型 |
| `@Var(ENVIRONMENT)` / `@Env` | `${name}` | `value` 或参数名 | 参数类型 |
| `@Var(BEAN)` / `@Bean` | `#{@name}` | `value` 或参数名 | 参数类型 |

```java
@FunctionAlias("getUrl")
public static String getUrl(
        @Var("baseUrl") String baseUrl,
        @Env("api.timeout") int timeout,
        @Bean("signService") SignService signService) { ... }
```

等价于：

```text
baseUrl     -> #{#baseUrl}
timeout     -> ${api.timeout}
signService -> #{@signService}
```

插件行为：

- 在 `@Param`/`@Var` 的 `value` 位置提供变量补全；
- 检查变量在当前 `Lifecycle` 阶段是否存在；
- 检查注入表达式结果与参数类型是否兼容；
- `@Param` 没有 `value` 时报错（框架会抛 `@Param annotation has no value.`），不能按参数名回退；
- `@Rar`/`@Env`/`@Bean` 用 `@Var(type=...)` 元注解固定变量类型，`value` 通过 `@AliasFor(annotation=Var.class)` 继承，按 merged annotation 解析。

#### 用法七：无注解参数按类型注入

函数引用和 Hook 回调方法的参数可以完全不带注解，`Context#getMethodParamObject` 会按参数类型注入：

| 参数类型 | 注入内容 |
|---|---|
| `MethodContext` / `MethodMetaContext` / `ClassContext` | 对应的上下文对象 |
| `Method` / `Class` | 当前方法 / 当前接口类 |
| 当前接口实例类型 | 代理对象本身（`$_THIS_$`） |
| `Request` / `Response` | 当前请求 / 响应对象 |
| `Throwable` / `HttpClientProxyObjectFactory` / `TaskResult` / `ApiDescribe` / `AsyncTaskExecutor` 的子类 | 对应上下文对象 |

都不匹配时回退 `parameterInstance` 扩展函数，仍取不到时该参数为 `null`。例如框架内置的分片上传函数：

```java
@FunctionAlias("__chunk_upload__")
public static Object upload(MethodContext context) { ... }
```

插件行为：

- 这类参数不需要注入注解，不要报“参数无法自动注入”；
- 只有既无注入注解、类型又不在上表范围时，才提示该参数可能为 `null`。

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
    private String expression;

    /**
     * 重试函数名。
     */
    @Spel(kind = SpelKind.FUNCTION, result = boolean.class)
    private String funcName;

    /**
     * 最大重试次数。
     */
    private Integer maxCount;

    /**
     * 是否开启重试。
     * 不支持 SpEL，不标 @Spel。
     */
    private Boolean enable;
}
```

插件读取：

- key：`expression` → `expression`；`funcName` → `func-name`；`maxCount` → `max-count`；
- `expression` 是条件表达式，`funcName` 是函数引用（函数返回值必须是 boolean）；
- 类型：字段类型；
- 说明：Javadoc；
- 默认阶段：`@SpelConfig.stage`；
- `enable`/`maxCount` 没标 `@Spel`，按普通配置处理。

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
public class MyConfig { ... }
```

---

### 3.4 `@Hook` / `@Callback`

`@Hook` 是框架的生命周期钩子。它不新增元数据注解，只需要把阶段来源指向 `lifecycle` 属性：

```java
@Spel(only = {"enable"}, kind = SpelKind.CONDITION,
      stageAttribute = "lifecycle", result = boolean.class)
public @interface Hook {

    String enable() default "";

    Lifecycle lifecycle() default Lifecycle.NON;

    // hookHandle/hookHandleClass/order/errorInterrupt 不支持 SpEL
}
```

- `enable` 是条件表达式，结果必须是布尔；
- `enable` 的阶段不是固定值，而是跟随同注解的 `lifecycle` 属性，用 `stageAttribute="lifecycle"` 表达；
- `lifecycle` 本身不是 SpEL，且不能是 `INHERIT`/`NEVER`/`UNKNOWN`，插件直接报错；
- `@Callback`/`@Val`/`@Pack` 的 `enable`/`lifecycle` 通过 `@AliasFor` 继承 `@Hook`，按 effective attribute 解析；
- `@Val` 是静态字段，String 字段值会作为 SpEL 解析，结果按 `name`（空时用字段名）存入变量，阶段跟随 `lifecycle`；
- `@Pack` 是静态字段，值是包名或类（支持 `String`/`String[]`/`Collection<String>`、`Class`/`Class[]`/`Collection<Class>`），不解析 SpEL；
- `@Callback`/`@Val`/`@Pack` 的 `lifecycle` 没有默认值，必须显式指定；`@Callback#storeName` 只有 `storeOrNot=true` 时生效，且在 `literal=false`（默认）时本身也按 SpEL 解析。

`@Callback` 方法参数同样支持自动注入：

```java
@Callback(lifecycle = Lifecycle.REQUEST_INIT,
          storeOrNot = true, storeName = "authToken")
public static String loadToken(@Env("auth.token") String token) { ... }
```

插件行为：

- 在 `enable` 中补全当前 `lifecycle` 阶段的变量；
- 检查 `enable` 表达式结果为布尔；
- 检查 `@Callback` 方法参数：无注入注解且类型不在按类型注入范围内时提示；
- 在 Hook 方法/字段上悬停显示 Javadoc。

---

## 4. 七个真实场景

### 4.1 场景一：框架新增一个支持 SpEL 的注解

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

插件补全 `#orderId`，并将运行时生成的 `$orderId`、`$0`、`$pc$orderId` 等别名一并纳入补全。

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

### 4.7 场景七：函数引用属性

```java
@Spel(only = {"url", "path"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface ServerAddress {

    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String urlFunc() default "";
}
```

使用：

```java
@ServerAddress(urlFunc = "getUrl")
@Get("/users")
List<User> users();
```

```java
@Namespace("api")
public final class ApiFunctions {

    @FunctionAlias("getUrl")
    public static String getUrl(@Var("baseUrl") String baseUrl) { ... }
}
```

插件行为：

- `"getUrl"` 作为函数名补全/跳转；
- 校验 `getUrl` 返回 String；
- `baseUrl` 通过 `@Var` 自动注入 `#{#baseUrl}`，不要求用户传参；
- 若 `url` 同时配置，提示 `url` 优先。

---

## 5. 改动量评估

### 5.1 统计基线

| 指标 | 数量 | 说明 |
|---|---:|---|
| `annotations.json` 注解 | 68 | 当前 JSON 基线；源码中还有一批含 SpEL 属性的注解未收录或只收录部分属性：`@ChunkUpload`、`@Describe`、`@Destroy`、`@ExceptionHandle`/`@ExceptionReturn`/`@ExceptionFallback`、`@Async`/`@AsyncExecutor`/`@AsyncHook`、`@Timeout`、`@TextEventStream`、`@MultiFile`、`@HttpProxy`/`@SocksProxy`、`@PrintRequestLog`/`@PrintResponseLog`、`@PropertiesJava`/`@PropertiesJson`、`@RecordReplay`、`@OAuth2ClientCredentialsGrant`/`@OAuth2TokenExchange` 等；另有 `@Mock#getEnable`、`@Condition#getAssertion` 是源码已不存在的旧属性，迁移时删除 |
| `annotations.json` 属性 | 162 | 当前 JSON 的元数据总量 |
| `TEMPLATE` 属性 | 102 | 类型级 `only`/`all` 批量声明 |
| `IF_CONDITION` 属性 | 31 | 属性级 `kind=CONDITION` |
| `NONE` 属性 | 25 | 不写，或 `disabled=true`；其中 11 个是函数引用属性（10 个 `*Func` + `@HttpRequest#func`），迁入 `FUNCTION` |
| `FUNCTION` 属性 | 0（源码 15） | JSON 中还没有 `kind=FUNCTION`；源码 33 个属性名、其中 18 个别名、15 个声明端，当前 JSON 收录了 12 个属性名但都未标成 `FUNCTION` |
| `KEY_VALUE` 属性 | 4 | 属性级 `kind=KEY_VALUE`；`DECLARATION` 目前无使用者 |
| `parseExpression` 调用点 | 284 | 不修改，只做一致性校验 |
| `@FunctionAlias` | 132 | 只给需要覆盖的函数写 `@SpelSymbol` |
| 内置函数索引 | 151 | `builtin-symbols.json` 中的函数总数（其中 132 个带 `@FunctionAlias`） |
| 内置变量常量 | 73 | 3 个常量类合计 73 个；当前索引收录 68 个 |
| 配置 API 非静态字段 | 232 | `configapi` 包中的非静态私有字段；只标支持 SpEL 的 30–50 个 |

### 5.2 声明量对比

| 项目 | v1（15 注解） | v2（3 注解） |
|---|---:|---:|
| 新增注解 | 15 | 3 |
| 注解属性总数 | ~150 | ~25 |
| 类型级 `@Spel` | 无 | 68 个（当前 JSON 基线），补齐源码后约 96 个，每个一行 |
| 属性级覆盖 | ~162 个 `@SpelValue` | ~60 个 `@Spel` |
| 符号声明 | ~100 个 `@SpelVariable` | ~100 个 `@SpelSymbol` |
| 配置声明 | ~50 个 `@SpelConfigProperty` | ~10 个 `@SpelConfig` + ~50 个 `@Spel` |
| 文档/示例/版本 | 全部写注解 | Javadoc 读取 |
| 函数引用/参数注入/Hook 新增注解 | 无 | 0（复用 `@Spel`/`@Var` 系列） |
| 框架侧声明处 | ~500 | ~250–300 |

### 5.3 P0 改动清单（核心三注解）

| 项目 | 文件数 | 新增/修改行数 | 说明 |
|---|---:|---:|---|
| 新增 httpclient `meta` 包 | 7 | ~470 | 3 个注解 + 4 个枚举；数组属性、`stageAttribute`、`FUNCTION` |
| 扩展 `Lifecycle` | 1 | ~10 | 新增 `INHERIT`/`NEVER`/`UNKNOWN` + 运行期过滤 |
| 核心注解迁移 | 7 | ~240 | 6 个核心注解 + `@Hook` 阶段来源；`@Callback` 通过 `@AliasFor` 继承 |
| 生成器改造 | 1 | ~380 | 注解 + Javadoc → 索引；扫描 `annotations`/`generalapi`/`proxy/mock`/`proxy/handle`/`proxy/destroy`；函数签名/参数注入索引；一致性校验 |
| 插件读取端 | 5 | ~680 | 索引加载、注解解析、数组对齐、`FUNCTION` 解析、Javadoc 解析、缓存 |
| 测试夹具 | 10 | ~400 | 注解解析、别名、阶段、多结果类型、函数引用、Javadoc、回退 |
| **合计** | **31** | **~2180** | **4–5 人天** |

### 5.4 P1 改动清单（符号、参数注入与 Hook）

| 项目 | 文件数 | 新增/修改行数 | 说明 |
|---|---:|---:|---|
| 参数注入规则 | 1 | ~180 | `@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 的表达式映射、补全、类型检查（不标注） |
| 内置变量 | 3 | ~150 | 73 个常量加 `@SpelSymbol` |
| 函数 | 2 | ~100 | 20–30 个公共函数加 `@SpelSymbol` |
| 函数引用检查 | 1 | ~200 | 函数存在性、返回值、参数注入（注解注入 + 按类型注入）、动态函数名 |
| Hook 支持 | 2 | ~180 | `stageAttribute` 跟随、`@Callback` 参数注入、`@Val` 字段 SpEL、`@Pack` 排除 |
| 其余 SpEL 注解 | 28 | ~450 | `@ChunkUpload`、`@Describe`、`@Destroy`、`@ExceptionHandle`/`@ExceptionReturn`/`@ExceptionFallback`、`@Async`/`@AsyncExecutor`/`@AsyncHook`、`@Timeout`、`@TextEventStream`、`@MultiFile`、`@HttpProxy`/`@SocksProxy`、`@PrintRequestLog`/`@PrintResponseLog`、`@PropertiesJava`/`@PropertiesJson`、`@RecordReplay`、`@OAuth2ClientCredentialsGrant`/`@OAuth2TokenExchange`、`@Mock`/`@MockMeta`/`@MockPlugin`、`@Wrapper`、`@HttpExec`、`@RespConvert`/`@ResultConvertMeta`；补齐 `@Destroy#func`、`@MockPlugin#implFunc`、`@Mock#mockFunc`/`enableFunc`、`@Wrapper#fun`、`@HttpExec#execFunc`、`@RespConvert#respContentTypeFunc` 等函数引用属性 |
| 配置模型 | 10 | ~150 | `@SpelConfig` + 30–50 个字段 `@Spel` |
| 插件扩展 | 4 | ~360 | 符号/函数/注入/配置索引与缓存 |
| **合计** | **51** | **~1770** | **4–5 人天** |

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
| P0 | 4–5 | 三注解 + 7 个核心注解改造 + 数组/FUNCTION + 索引 + 插件读取 |
| P1 | 4–5 | 参数注入、函数引用、Hook、变量/函数/配置、未收录注解补齐 |
| P2 | 1–2 | Javadoc 增强 |
| **合计** | **9–12** | 不含 UI 大改和完整测试矩阵 |

相比 v1 的 13–19 人天，减少约 40%。

### 5.7 改动量最大的五个点

1. **插件读取端**：索引加载、注解解析、数组对齐、`FUNCTION` 解析、缓存，约 680 行。
2. **生成器**：注解 + Javadoc → 索引，还要生成函数签名与参数注入索引，约 380 行。
3. **函数引用与参数注入**：函数存在性/返回值/参数注入检查，以及 `@Param`/`@Var` 系列的表达式映射，约 380 行。
4. **Javadoc 解析器**：需要读取 `PsiDocComment` 的第一段、`@param`、`@return`、`@since`、`@deprecated`、`<pre>`，约 200–300 行，是 v2 新增的核心工作量。
5. **68 个类型级 `@Spel`（当前 JSON 基线，补齐源码后约 96 个）+ 60 个属性级覆盖 + 73 个内置变量**：每个一行，名称/类型/说明从常量/Javadoc 读取。

### 5.8 不会改的地方

- 284 个 `parseExpression` 调用点：不改，只在 CI 做一致性校验。
- 运行时行为：不改；注解不参与求值。
- 现有注解的 `@Retention`/`@Target`：不改。
- 用户业务代码：不改。
- 插件的语法解析、高亮、补全框架：不改；新增的是函数引用与参数注入两类检查。

### 5.9 风险与回退

| 风险 | 应对 |
|---|---|
| Javadoc 格式不一致 | 只读取结构化 tag（`@param`/`@return`/`@since`/`@deprecated`/`<pre>`），不猜自然语言 |
| Javadoc 缺失 | 降级为“无说明”，不影响高亮/补全/检查 |
| 注解与调用点不一致 | 生成器做“调用点 → 声明”一致性校验，CI 失败 |
| 用户自定义注解无法解析 | 未标注时回退到 `annotations.json` |
| `@Param` 内置规则与框架语义漂移 | 规则集中到单一解析器，配契约测试；`@Param` 变更时同步 |
| `@Var` 系列注入语义漂移 | `@AliasFor` 按 effective annotation 解析，共用一份规则表 |
| 按类型注入范围变化 | 类型清单集中到单一解析器，与 `ContextParameterInstanceGetter` 同步 |
| 函数重载或同名函数 | 按参数注入注解 + 参数类型消歧；无法消歧时降级为只校验函数存在 |
| 动态函数名（`#{...}`） | 只做语法/类型检查，不做跳转；静态可求值时再校验函数 |
| Hook 阶段值非法 | `INHERIT`/`NEVER`/`UNKNOWN` 不被注册为 Hook 阶段，插件报错 |
| 性能下降 | 构建期索引 + `PsiClass` 缓存 + 基准测试 |
| 枚举新增值 | 未知值降级 `UNKNOWN`，插件不崩溃 |

---

## 6. 验收清单

- [ ] 每个 SpEL 注解至少有一个类型级 `@Spel`（`only` 或 `all`）。
- [ ] 每个非 String 结果属性有属性级 `result`/`constraint` 覆盖。
- [ ] `result` 多元素时“任一候选通过”生效，数组长度不匹配能被报告。
- [ ] `@ServerAddress#urlFunc` 的字面量函数名可补全/跳转，`#{...}` 动态函数名只做语法/类型检查。
- [ ] 函数参数既无 `@Param`/`@Var` 系列注解、类型又不在按类型注入范围内时给出提示。
- [ ] `@Var`/`@Rar`/`@Env`/`@Bean` 的注入表达式与 effective attribute 解析正确。
- [ ] `@Hook#enable` 的阶段跟随 `lifecycle`，`@Callback` 回调参数可注入，`@Val` 字段值按 SpEL 解析、`@Pack` 字段不解析。
- [ ] `@Spel(disabled=true)` 能正确排除属性。
- [ ] `@AliasFor` 别名继承 effective attribute 的声明。
- [ ] `Lifecycle` 新增的 `INHERIT`/`NEVER`/`UNKNOWN` 不会被注册为 Hook 阶段。
- [ ] 73 个内置变量的名称/类型/阶段能从常量、字段类型、Javadoc 读取；当前索引只收录 68 个，缺 `$_PARAM_CONTEXT_$` 和 `OrdinaryVarName` 的 4 个常量。
- [ ] 151 个内置函数（其中 132 个带 `@FunctionAlias`）不需要逐个写注解；只给需要覆盖的函数写 `@SpelSymbol`。
- [ ] 配置 key 能按 `naming` 从字段名生成。
- [ ] Javadoc 缺失时插件降级，不报错。
- [ ] 补全 P95 延迟不超过迁移前 10%。
- [ ] 用户自定义注解/参数注解示例工程通过。

---

## 7. 建议的实施顺序（按 PR 拆分）

| PR | 内容 | 依赖 |
|---|---|---|
| PR1 | 在 `lucky-httpclient` 新增 `meta` 包：3 个注解 + 4 个枚举 + 扩展 `Lifecycle` + 数组/FUNCTION/`stageAttribute` + 单元测试 | 无 |
| PR2 | 迁移 7 个核心注解：`HttpRequest`、`Retryable`、`ConvertMetaType`、`Binary`、`SpELImport`、`ServerAddress`、`Hook`；同批补齐 `@ChunkUpload`、`@ExceptionHandle`/`@ExceptionReturn`、`@Mock` 函数引用属性、`@Wrapper#fun`、`@MockPlugin#implFunc`；`@Callback` 通过 `@AliasFor` 继承，不单独标注 | PR1 |
| PR3 | 生成器：注解 + Javadoc → 索引 + 函数签名/参数注入索引 + 一致性校验 | PR1、PR2 |
| PR4 | 插件：读取索引 + 多结果类型对齐 + `FUNCTION` 解析 + 按需 PSI + 缓存 + 回退 | PR3 |
| PR5 | 参数注入：`@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 补全与类型检查；函数引用检查 | PR4 |
| PR6 | `@Hook`/`@Callback`：`stageAttribute` 跟随、回调参数注入、`@Val` 字段 SpEL、`@Pack` 排除 | PR4、PR5 |
| PR7 | `@SpelSymbol`：用户自定义参数注解、内置变量、公共函数 | PR4 |
| PR8 | `@SpelConfig`：配置模型 + YAML 映射 | PR4 |
| PR9 | Javadoc 增强：示例、`@since`、`@deprecated`、悬停文档 | PR4 |

---

## 8. 结论

v2 的接入成本可以压到**一个类型级 `@Spel` + 少量属性级覆盖**：

```java
@Spel(only = {"url"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface HttpRequest { ... }
```

其余信息全部交给 Javadoc 和 Java 签名。函数引用和 Hook 也不需要新注解：前者用 `kind=FUNCTION`，后者用 `stageAttribute="lifecycle"`。真正的工作量不在“写注解”，而在：

1. Javadoc 解析器；
2. 插件读取端与缓存；
3. 函数引用/参数注入检查；
4. 注解声明与调用点的一致性校验。

这四块合计约 **1300–1600 行、4–6 人天**；加上注解迁移和测试，完整 P0+P1+P2 约 **9–12 人天**。
