# SpEL 元数据注解设计（精简版）

> 状态：设计提案（v2，取代此前的 15 注解方案）  
> 适用范围：`lucky-httpclient`、`lucky-httpclient-idea-plugin`  
> 核心原则：**能从 Java 注释/Java 签名准确读取的信息，一律不写注解。**

---

## 1. 为什么精简

上一版设计了 15 个注解，把变量说明、函数说明、示例、默认值、优先级、分组等都放进了注解。结果是框架开发者为了接入一个注解，要写大量“文档性注解”，而这些信息本来就在 Javadoc 和 Java 签名里。

v2 把信息源分成四层：

| 信息 | 来源 | 是否写注解 |
|---|---|---|
| 属性说明、变量说明、函数说明 | Javadoc | 否 |
| 函数入参、返回值、泛型、重载、可变参数 | Java 方法签名 | 否 |
| 注解属性默认值 | 注解元素的 `default` | 否 |
| `@since`、`@deprecated`、`@see`、示例 | Javadoc | 否 |
| 函数别名、命名空间 | `@FunctionAlias`、`@Namespace` | 否 |
| Bean 名称/类型 | Spring `@Component`、`@Bean`、类名 | 否 |
| **哪些属性支持 SpEL** | 注解 | **是** |
| **值形态（模板/键值/声明/条件/字面量）** | 注解 | **是** |
| **表达式期望结果类型** | 注解 | **是** |
| **生命周期阶段** | 注解 | **是** |
| **分隔符、key/value 是否求值** | 注解 | **是** |
| **字段/方法/参数是否暴露为 SpEL 符号** | 注解 | **是** |

最终只保留 **3 个注解**：

- `@Spel`：声明 SpEL 值契约。
- `@SpelSymbol`：声明 SpEL 符号（变量、函数、参数、Bean）。
- `@SpelConfig`：声明配置模型。

---

## 2. 设计原则

1. **Javadoc 优先。** 说明、参数、返回值、示例、版本、弃用信息全部从 Javadoc 读取；注解不重复。
2. **签名优先。** 函数名、参数类型、返回类型、泛型、重载、可变参数从 Java 签名读取。
3. **默认值不写。** 注解元素的 `default` 直接由 PSI 读取，不在元注解里再写一遍。
4. **注解只写机器语义。** 只声明“是否支持 SpEL、怎么解析、结果是什么、何时生效”这些 Javadoc 无法可靠表达的事实。
5. **类型级默认 + 属性级覆盖。** 一个注解通常只写一个类型级 `@Spel`，特殊属性才覆盖。
6. **未知值降级。** 枚举新增值、无法解析的 Javadoc 一律降级为 `UNKNOWN`，插件不崩溃。

---

## 3. 注解总览

三个注解直接放在 `lucky-httpclient` 的 `com.luckyframework.httpclient.proxy.spel.meta` 包，不新增 Maven 模块；`Lifecycle` 保持在 `com.luckyframework.httpclient.proxy.spel.hook` 包，只扩展三个元数据值。

| 注解 | 目标 | 作用 | 典型写法 |
|---|---|---|---|
| `@Spel` | 注解类型、注解元素方法、配置字段 | 声明哪些属性支持 SpEL、值形态、结果类型、阶段、分隔符 | 每个 SpEL 注解 1 个类型级 + 少量属性级 |
| `@SpelSymbol` | 字段、方法、参数、注解类型 | 声明一个字段/方法/参数/参数注解暴露为 SpEL 符号 | 内置变量、公共函数、参数注解 |
| `@SpelConfig` | 类型 | 声明配置模型前缀、命名策略、默认阶段 | 配置 POJO |

---

## 4. 枚举

### 4.1 `SpelKind`

| 值 | 含义 |
|---|---|
| `INHERIT` | 继承类型级默认；没有则按 `TEMPLATE` |
| `TEMPLATE` | 字符串模板，可包含多个 `#{...}`，结果 String |
| `KEY_VALUE` | 按分隔符拆成 key/value，分别决定是否求值 |
| `DECLARATION` | 框架自定义声明语法；当前 `@SpELImport` 归入 `KEY_VALUE`，`DECLARATION` 保留给其他声明语法 |
| `CONDITION` | 结果必须是布尔 |
| `FUNCTION` | 函数引用：值可以是函数名字面量，或求值结果为函数名的 SpEL 表达式 |
| `LITERAL` | 不解析，仅作为字面量保存 |

### 4.2 `Lifecycle`（复用并扩展）

直接复用 `lucky-httpclient` 现有的运行期枚举 `com.luckyframework.httpclient.proxy.spel.hook.Lifecycle`，不再新增 `SpelStage`。现有值保持不变，只增加三个元数据专用值：

| 值 | 用途 | 运行期行为 |
|---|---|---|
| `INHERIT` | 继承类型级默认 | 忽略，不注册为 Hook 阶段 |
| `NEVER` | `until` 无上界 | 忽略，不注册为 Hook 阶段 |
| `UNKNOWN` | 未知值降级 | 忽略，不注册为 Hook 阶段 |

现有运行期值：`CLASS`、`METHOD_META`、`METHOD`、`REQUEST_INIT`、`CONFIG_API_INIT`、`CONFIG_API_METHOD_INIT`、`CONFIG_API_INIT_COMPLETE`、`REQUEST`、`INVOKE_WRAPPER_METHOD`、`RETRY`、`RESPONSE_INIT`、`RESPONSE`、`METHOD_RESULT`、`THROWABLE`、`DESTROY`、`NON`。

`NON` 仍然是“不执行”，只用于 `@Hook` 默认值；不要用 `NON` 表示继承或无上界。新增的三个值只在元数据解析阶段使用，运行期调度器直接忽略。

### 4.3 `SpelConstraint`

`ANY`、`BOOLEAN`、`NUMBER`、`INTEGER`、`STRING`、`BINARY`、`TYPE`、`CLASS`

用于 `Class<?>` 无法精确表达的粗约束：

- `BINARY`：`byte[]`、`InputStream`、`File` 等二进制形态。
- `TYPE`：`Type`、`Class`、`ResolvableType`、`SerializationTypeToken`、`String` 等类型元数据。
- `NUMBER`/`INTEGER`：数值范围。

### 4.4 `SpelSymbolKind`

`AUTO`、`ROOT`、`VAR`、`BEAN`、`ENV`、`INTERNAL`、`FUNCTION`、`PARAM`

`AUTO` 的推导规则：

| 位置 | 推导 |
|---|---|
| 字段常量 | 常量值以 `$` 开头且以 `$` 结尾 → `INTERNAL`；否则 `VAR` |
| 方法 | 有 `@FunctionAlias` 或位于 `@Namespace` 类 → `FUNCTION` |
| 参数 | → `PARAM` |
| Spring `@Component`/`@Bean` | → `BEAN` |
| 注解类型 | 由 `@SpelSymbol` 的 `kind` 显式指定，默认 `PARAM` |

### 4.5 `SpelNaming`

`AS_IS`、`CAMEL_CASE`、`KEBAB_CASE`、`SNAKE_CASE`

---

## 5. `@Spel`

```java
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Spel {

    boolean disabled() default false;

    SpelKind kind() default SpelKind.INHERIT;

    Lifecycle stage() default Lifecycle.INHERIT;

    String stageAttribute() default "";

    Class<?>[] result() default {};

    String[] typeExpression() default {};

    SpelConstraint[] constraint() default {};

    String separator() default "";

    String separatorAttribute() default "";

    boolean keySpel() default false;

    boolean valueSpel() default true;

    String[] only() default {};

    boolean all() default false;

    String condition() default "";
}
```

| 属性 | 类型 | 默认 | 含义与用途 |
|---|---|---|---|
| `disabled` | `boolean` | `false` | 显式关闭。类型级 `all=true` 时，用它排除个别属性。 |
| `kind` | `SpelKind` | `INHERIT` | 值形态；属性级覆盖类型级默认。 |
| `stage` | `Lifecycle` | `INHERIT` | 生命周期；属性级覆盖类型级默认，最终回退 `METHOD`（最宽松，避免漏报）。 |
| `stageAttribute` | `String` | `""` | 阶段来自同注解的另一个属性，如 `@Hook#enable` 用 `stageAttribute="lifecycle"`。属性级 `stage` 优先于它。 |
| `result` | `Class<?>[]` | `{}` | 期望结果类型；空数组表示不限制，多个元素表示“任一类型通过”。 |
| `typeExpression` | `String[]` | `{}` | 泛型/复杂类型表达式；与 `result` 按索引对齐，长度 1 时广播，长度 0 表示无泛型。 |
| `constraint` | `SpelConstraint[]` | `{}` | 粗约束；空数组表示 `ANY`，长度 1 时广播，否则与 `result` 等长。 |
| `separator` | `String` | `""` | `KEY_VALUE`/`DECLARATION` 的分隔符。 |
| `separatorAttribute` | `String` | `""` | 分隔符来自同注解的哪个属性，如 `@SpELImport#separator`。 |
| `keySpel` | `boolean` | `false` | key 是否参与求值。 |
| `valueSpel` | `boolean` | `true` | value 是否参与求值；`rootLit`/`varLit` 用 `false`。 |
| `only` | `String[]` | `{}` | 仅类型级使用：列出支持 SpEL 的属性名。 |
| `all` | `boolean` | `false` | 仅类型级使用：所有 `String`/`String[]` 属性默认支持。与 `only` 二选一。 |
| `condition` | `String` | `""` | 可选的静态生效条件，如 `enable != false`。只支持属性引用、字面量、比较、逻辑、括号。 |

### 5.1 使用规则

**类型级默认（推荐）**：

```java
@Spel(all = true, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface HttpRequest {
    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String func() default "";

    RequestMethod method() default RequestMethod.NON;
}
```

- `all=true`：所有 `String`/`String[]` 属性默认支持 SpEL。
- `func` 是函数引用，用属性级 `kind=FUNCTION` 覆盖类型级的模板语义。
- `method` 是枚举，本来就不在 `all` 范围，不需要标注。

**精确列表（更安全）**：

```java
@Spel(
    only = {"url"},
    stage = Lifecycle.REQUEST_INIT,
    result = String.class
)
public @interface HttpRequest { ... }
```

`only` 适合属性不多、或存在多个 String 属性但只有部分支持 SpEL 的注解。`all` 适合属性多且语义统一的注解。

**属性级覆盖**：

```java
@Spel(kind = SpelKind.CONDITION, result = boolean.class)
String enable() default "true";
```

**属性级关闭**：

```java
@Spel(disabled = true)
String internalFlag() default "";
```

**优先级**：属性级 `disabled=true` > 属性级 `@Spel` > 类型级 `@Spel` > 默认值。`only` 与 `all` 同时存在时以 `only` 为准；`all` 只影响 `String`/`String[]` 属性。

### 5.2 多结果类型

`result`/`typeExpression`/`constraint` 都是数组，用于一个属性可能返回多种类型的场景。典型例子是 `@ConvertMetaType#type`：

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

契约：

- `result` 空数组：不限制类型。
- `result` 多元素：表达式结果匹配任一候选即通过；补全按候选类型的公共成员提供。
- `typeExpression` 空数组：所有候选都无泛型；长度 1：广播到所有候选；长度等于 `result`：按索引对齐；其他长度：元数据声明错误。
- `constraint` 空数组：全部 `ANY`；长度 1：广播；长度等于 `result`：按索引对齐；其他长度：元数据声明错误。
- `result` 为空且 `constraint` 非空时，`constraint` 作为独立约束生效，例如 `@Binary#file` 的 `constraint=BINARY`。
- 数组对齐只影响静态检查，不改变运行时类型转换；运行时仍由框架的 `toType`/转换器处理。

### 5.3 函数引用（`kind=FUNCTION`）

`kind=FUNCTION` 表示属性值不是普通模板，而是“函数引用”。典型例子是 `@ServerAddress#urlFunc`/`pathFunc`：

```java
@Spel(only = {"url", "path"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface ServerAddress {

    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String urlFunc() default "";

    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String pathFunc() default "";
}
```

框架的实际顺序是：

1. `url` 非空时，先按 SpEL 模板求值；
2. `url` 为空或结果校验失败时，`urlFunc` 先按 SpEL 表达式求值，结果必须是 String 函数名；
3. 用函数名查找函数并执行，函数返回值作为 URL。

因此插件需要区分两种写法：

| `urlFunc` 的值 | 插件行为 |
|---|---|
| `"getUrl"` | 直接按函数名补全/校验/跳转 |
| `"#{#funcName}"` | 先按 SpEL 表达式补全/检查，结果类型 String；静态结果若是函数名，再校验函数 |

函数引用的检查项：

- 函数必须存在：名称来自 `@FunctionAlias` 或方法名，命名空间来自 `@Namespace`；
- 函数返回值必须兼容 `@Spel.result`（这里是 String）；
- 参数带 `@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 时按表达式注入，插件检查表达式结果与参数类型兼容；
- 参数不带注入注解时由 `Context#getMethodParamObject` 按类型注入：`MethodContext`、`MethodMetaContext`、`ClassContext`、`Method`、`Class`、当前接口实例、`Request`、`Response`，以及 `Throwable`/`HttpClientProxyObjectFactory`/`TaskResult`/`ApiDescribe`/`AsyncTaskExecutor` 的子类；再回退 `parameterInstance` 扩展函数，仍取不到时该参数为 null；
- 只有既无注入注解、类型又不在按类型注入范围时才提示——例如 `@ChunkUpload.ChunkUploadFunction.upload(MethodContext context)` 是无注解的合法写法；
- `url` 和 `urlFunc` 同时配置时，提示 `url` 优先；
- 函数名是字面量时提供跳转/悬停；是动态表达式时只做语法/类型检查。

函数引用不新增注解：函数名、命名空间、参数、返回值全部从现有注解和 Java 签名读取。

同类属性还有 `@HttpRequest#func`、`@Destroy#func`、`@Wrapper#fun`、`@Get`/`@Post` 等 9 个组合注解的 `func`（`@HttpRequest#func` 的别名）、`@ServerAddress#urlFunc`/`pathFunc`、`@ConvertMetaType#typeFunc`/`respContentTypeFunc`、`@Retryable#conditionFunc`、`@ExceptionHandle#handleFunc`、`@ExceptionHandleMeta#conditionFunc`、`@ExceptionReturn#handleFunc`/`conditionFunc`（别名）、`@ExceptionFallback#conditionFunc`（别名）、`@RespConvert#resultFunc`/`metaTypeFunc`/`respContentTypeFunc`（后两个为别名）、`@ResultConvertMeta#metaTypeFunc`/`respContentTypeFunc`、`@HttpExec#execFunc`、`@Mock#enableFunc`（别名）/`mockFunc`、`@MockMeta#enableFunc`、`@MockPlugin#implFunc`。源码共 33 个属性名，其中 18 个是 `@AliasFor` 别名、15 个是声明端；当前 `annotations.json` 只收录 12 个属性名（9 个声明端），且都没有标成 `FUNCTION`，迁移时以源码为准。它们统一按 `kind=FUNCTION` 处理，`@Spel.result` 写函数返回值类型：URL/内容类型类为 `String`，条件函数为 `boolean`，处理器函数按框架期望类型。

---

## 6. `@SpelSymbol`

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelSymbol {

    String name() default "";

    SpelSymbolKind kind() default SpelSymbolKind.AUTO;

    Class<?> type() default Object.class;

    String typeExpression() default "";

    Lifecycle from() default Lifecycle.INHERIT;

    Lifecycle until() default Lifecycle.NEVER;

    String nameAttribute() default "value";
}
```

| 属性 | 类型 | 默认 | 含义与用途 |
|---|---|---|---|
| `name` | `String` | `""` | 符号名。空串自动推导：字段常量值、方法名/`@FunctionAlias`、参数名、注解属性值。 |
| `kind` | `SpelSymbolKind` | `AUTO` | 符号类型；`AUTO` 按位置推导。 |
| `type` | `Class<?>` | `Object.class` | 变量类型/函数返回类型。`Object.class` 表示从字段类型/方法返回类型读取。 |
| `typeExpression` | `String` | `""` | 泛型类型覆盖。 |
| `from` | `Lifecycle` | `INHERIT` | 从哪个阶段可见；`INHERIT` 时从 Javadoc `@spelStage` 读取，最终回退 `METHOD`（最宽松，避免漏报）。 |
| `until` | `Lifecycle` | `NEVER` | 到哪个阶段为止；`NEVER` 表示无上界。 |
| `nameAttribute` | `String` | `"value"` | 仅用于注解类型：参数注解的哪个属性提供变量名。 |

### 6.1 自动推导规则

**变量名**：

- 字段常量：读取常量值。例如 `$_RESPONSE_STATUS_$ = "$status$"` → `$status$`。
- 方法：`@FunctionAlias` 优先，否则方法名。
- 参数：参数名；内置 `@Param` 由插件硬编码识别 `@Param#value`，不需要标注。
- 注解类型：仅用户自定义参数注解；`nameAttribute` 指向的属性值。

**变量类型**：

- 字段：字段类型。若字段是 `String` 常量但代表其他类型，显式写 `type=Integer.class`。
- 方法：返回类型，含泛型。
- 参数：参数类型。
- 注解类型：参数类型，或 `typeExpression`。

**说明、参数说明、返回值、示例、版本、弃用**：全部从 Javadoc 读取，不写注解。

### 6.2 示例

**内置变量**：

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

- 名称：`"$status$"`；
- 类型：Javadoc `@spelType` 或字段类型；这里是 `Integer`；
- 阶段：Javadoc `@spelStage`；这里是 `RESPONSE`；
- 说明：Javadoc 第一段；
- 示例：Javadoc `@sample`。

如果不想用自定义 Javadoc tag，可以显式写注解：

```java
@SpelSymbol(type = Integer.class, from = Lifecycle.RESPONSE)
public static final String $_RESPONSE_STATUS_$ = "$status$";
```

**函数**：

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
- 说明：Javadoc。

**内置参数注解 `@Param`**：

`@Param` 在 `lucky-core` 中，`@SpelSymbol` 在 `lucky-httpclient` 中，core 不能反向依赖 httpclient，因此 `@Param` 不标注，由插件内置识别：

- 注解全限定名：`com.luckyframework.reflect.Param`；
- 变量名：`value()` 必填；为空时框架抛 `@Param annotation has no value.`，不会回退参数名；
- 变量类型：参数类型；
- 生成 `name`/`$name`（最终值/原始值）、`$index`/`$$index`（按位置的最终值/原始值）、`name::type`/`$index::type`（参数类型）、`$pc$name`/`$pc$index`（`ParameterContext`）等别名。

**用户自定义参数注解**：

```java
@SpelSymbol(nameAttribute = "name", kind = SpelSymbolKind.PARAM)
public @interface MyParam {
    String name();
}
```

**Bean**：

```java
@Component("userService")
public class UserService { ... }
```

插件从 Spring 注解读取 Bean 名和类型，不需要 `@SpelSymbol`。只有需要覆盖名称/阶段时才加：

```java
@SpelSymbol(name = "users", from = Lifecycle.REQUEST_INIT)
@Component("userService")
public class UserService { ... }
```

### 6.3 参数注入符号（`@Param` / `@Var` 系列）

`@Param`、`@Var`、`@Rar`、`@Env`、`@Bean` 是框架已有的参数注入注解，不需要再标 `@SpelSymbol`。插件内置识别规则：

| 注解 | 注入表达式 | 变量名 | 类型 |
|---|---|---|---|
| `@Param` | `value` 原样作为 SpEL | `value` 必填 | 参数类型 |
| `@Var(NORMAL)` | `#{#name}` | `value` 或参数名 | 参数类型 |
| `@Var(ROOT)` / `@Rar` | `#{name}` | `value` 或参数名 | 参数类型 |
| `@Var(ENVIRONMENT)` / `@Env` | `${name}` | `value` 或参数名 | 参数类型 |
| `@Var(BEAN)` / `@Bean` | `#{@name}` | `value` 或参数名 | 参数类型 |

`@Rar`/`@Env`/`@Bean` 以 `@Var(type = ...)` 元注解固定变量类型（`ROOT`/`ENVIRONMENT`/`BEAN`），`value` 通过 `@AliasFor(annotation = Var.class, attribute = "value")` 继承，插件按 merged annotation 读取。

使用位置：

- 函数方法参数（`urlFunc`/`pathFunc` 指向的函数）；
- `@Hook`/`@Callback` 回调方法参数；
- 其他由 `Context#autoInjectParamExecuteMethod` 执行的方法。

不使用注入注解时，`Context#getMethodParamObject` 会按参数类型注入 `MethodContext`、`MethodMetaContext`、`ClassContext`、`Method`、`Class`、当前接口实例、`Request`、`Response`，以及 `Throwable`/`HttpClientProxyObjectFactory`/`TaskResult`/`ApiDescribe`/`AsyncTaskExecutor` 的子类；再回退 `parameterInstance` 扩展函数，仍取不到时为 null。

插件行为：

- 函数引用补全时，自动注入的参数不需要用户传参；
- 无注入注解且类型不在按类型注入范围内时，提示该参数可能为 null；
- 在 `@Param#value`/`@Var#value` 的值位置提供变量补全；
- 检查变量名在当前阶段是否存在；
- `@Param#value` 为空时报错（框架会抛 `@Param annotation has no value.`），不能按参数名回退。

### 6.4 Hook 机制

`@Hook` 是生命周期钩子，插件需要把 `lifecycle` 作为阶段来源，把 `enable` 作为条件表达式，把回调方法参数作为注入点：

```java
@Spel(only = {"enable"}, kind = SpelKind.CONDITION,
      stageAttribute = "lifecycle", result = boolean.class)
public @interface Hook {

    String enable() default "";

    Lifecycle lifecycle() default Lifecycle.NON;

    // hookHandle/hookHandleClass/order/errorInterrupt 不支持 SpEL
}
```

规则：

- `enable` 的 `stage` 跟随 `lifecycle` 属性，使用 `stageAttribute="lifecycle"`；
- `lifecycle` 不能是 `INHERIT`/`NEVER`/`UNKNOWN`，插件报错；
- `@Callback`/`@Val`/`@Pack` 的 `enable`/`lifecycle` 通过 `@AliasFor` 继承 `@Hook`，插件按 effective attribute 解析；
- `@Callback` 方法参数支持 `@Param`/`@Var` 系列自动注入，也支持不带注解的按类型注入（见 6.3）；
- `@Val` 是静态字段，但字段值不是普通字面量：`String` 字段会由 `VarHookHandler` 当作 SpEL 表达式解析，结果按 `name`（空时用字段名）存入变量，阶段跟随 `lifecycle`；插件要对字段值做 SpEL 高亮/补全/结果类型检查；
- `@Pack` 是静态字段，值是包名或类（支持 `String`/`String[]`/`Collection<String>`、`Class`/`Class[]`/`Collection<Class>`），不解析 SpEL；
- `@Callback`/`@Val`/`@Pack` 的 `lifecycle` 没有默认值，必须显式指定；`@Callback#storeName` 只有 `storeOrNot=true` 时才生效，且 `storeName`/`@Val#name` 在 `literal=false` 时本身也按 SpEL 解析；
- Hook 方法内补全的变量集合由 `lifecycle` 决定，例如 `REQUEST` 阶段提供请求变量，`RESPONSE` 阶段提供响应变量；
- Hook 方法/字段的 Javadoc 说明直接读取。

`@Hook` 的 `lifecycle` 是 `Lifecycle` 枚举，不是字符串；插件从注解值读取，不需要额外元数据。

---

## 7. `@SpelConfig`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelConfig {

    String prefix();

    String[] aliases() default {};

    SpelNaming naming() default SpelNaming.AS_IS;

    Lifecycle stage() default Lifecycle.CONFIG_API_INIT;

    boolean valueSpel() default true;
}
```

| 属性 | 类型 | 默认 | 含义与用途 |
|---|---|---|---|
| `prefix` | `String` | 必填 | 配置前缀。 |
| `aliases` | `String[]` | `{}` | 其他可用前缀。 |
| `naming` | `SpelNaming` | `AS_IS` | 字段名 → key 的命名策略。 |
| `stage` | `Lifecycle` | `CONFIG_API_INIT` | 配置字段默认阶段。 |
| `valueSpel` | `boolean` | `true` | 字段值默认是否支持 SpEL；字段级 `@Spel` 覆盖。 |

字段级：

```java
@SpelConfig(prefix = "lucky.api", naming = SpelNaming.KEBAB_CASE)
public class RetryConf {

    @Spel(result = boolean.class, kind = SpelKind.CONDITION)
    private String expression;

    @Spel(kind = SpelKind.FUNCTION, result = boolean.class)
    private String funcName;

    private Integer maxCount;

    // 不支持 SpEL 的字段不标
    private Boolean enable;
}
```

- key 从字段名 + `naming` 生成：`maxCount` → `max-count`。
- `expression` 是条件表达式，`funcName` 是函数引用；`maxCount`/`enable` 不支持 SpEL，不标 `@Spel`。
- 字段说明从 Javadoc 读取。
- 字段类型从 Java 类型读取。
- 显式 key 可以在 Javadoc 写 `@key retry-count`，或后续再加 `key` 属性；先保持最简。

---

## 8. 真实注解改造示例

### 8.1 `@HttpRequest`

```java
@Spel(only = {"url"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
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

`value` 是 `url` 的别名，插件按 effective attribute 继承；`func` 是 URL 生成函数引用，支持字面量函数名和 `#{...}` 动态函数名；`method` 是枚举，不在 `only` 列表，无需 `disabled`。

### 8.2 `@Retryable`

```java
// 声明端：@Spel 写在 @RetryMeta 上
@Spel(only = {"name"}, stage = Lifecycle.RETRY, result = String.class)
public @interface RetryMeta {

    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "true";

    @Spel(result = Integer.class)
    String retryCount() default "0";

    ObjectGenerate beforeRetry();

    ObjectGenerate decider();
}

// 组合注解：只保留别名和自有的数值/条件属性
@RetryMeta(beforeRetry = @ObjectGenerate(BackoffWaitingBeforeRetryContext.class),
           decider = @ObjectGenerate(HttpExceptionRetryDeciderContext.class))
public @interface Retryable {

    @AliasFor(annotation = RetryMeta.class, attribute = "enable")
    String enable() default "true";

    @AliasFor(annotation = RetryMeta.class, attribute = "name")
    String name() default "[#{T(Thread).currentThread().getName()}][#{$req$.getUniqueId()}][#{$api$.name}]";

    @AliasFor(annotation = RetryMeta.class, attribute = "retryCount")
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

    @Spel(kind = SpelKind.FUNCTION, result = boolean.class)
    String conditionFunc() default "";
}
```

`enable`/`name`/`retryCount` 通过 `@AliasFor` 指向 `@RetryMeta`，属性级 `@Spel` 必须写在声明端（`@RetryMeta`）才生效，`@Retryable` 上只保留别名。`retryCount`/`waitMillis`/`maxWaitMillis`/`minWaitMillis`/`multiplier` 在框架中分别按 `int`/`long`/`long`/`long`/`double` 解析（`MethodContext#getRetryActuator`、`BackoffWaitingBeforeRetryContext#doBeforeRetry`），不能沿用类型级的 `result = String.class`，必须属性级覆盖；`conditionFunc` 是函数引用，函数返回值必须是 boolean。

### 8.3 `@ConvertMetaType`

```java
@Spel(only = {"type", "respContentType"}, stage = Lifecycle.RESPONSE, result = String.class)
public @interface ConvertMetaType {

    Class<?> value() default Object.class;

    @Spel(result = {Type.class, Class.class, ResolvableType.class,
                    SerializationTypeToken.class, String.class})
    String type() default "";

    @Spel(
        kind = SpelKind.FUNCTION,
        result = {Type.class, Class.class, ResolvableType.class,
                  SerializationTypeToken.class, String.class},
        constraint = {SpelConstraint.TYPE, SpelConstraint.TYPE, SpelConstraint.TYPE,
                      SpelConstraint.TYPE, SpelConstraint.STRING}
    )
    String typeFunc() default "";

    String respContentType() default "";

    @Spel(kind = SpelKind.FUNCTION, result = String.class)
    String respContentTypeFunc() default "";
}
```

`type` 的 5 种候选类型见 5.2；`typeFunc` 的函数返回值同样必须是这 5 种类型之一，框架在 `Context#toType` 里统一转换；`respContentTypeFunc` 的返回值固定为 `String`。两者都支持字面量函数名和 `#{...}` 动态函数名。示例中的 `Type`/`ResolvableType`/`SerializationTypeToken` 需 import 对应类型。现有 JSON 把 `typeFunc` 标成了 `NONE`、把 `respContentTypeFunc` 误标为 `TEMPLATE` 且 lifecycle 为 `UNKNOWN`，迁移时一并修正为 `FUNCTION`。

### 8.4 `@Binary`

```java
@Spel(only = {"condition", "name", "file", "filename", "contentType"},
      stage = Lifecycle.REQUEST_INIT,
      result = String.class)
public @interface Binary {

    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    String condition() default "";

    String name();

    @Spel(result = {}, constraint = SpelConstraint.BINARY)
    String file();

    String filename();

    String contentType() default "";
}
```

### 8.5 `@ChunkUpload`

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

这 4 个数值属性因为 `parseLongExpression`/`parseIntExpression` 间接求值而容易漏检；现在一个类型级 `@Spel` + 4 个结果类型覆盖即可。`file` 的表达式结果可以是 `File` 或文件路径字符串，所以给了两个候选类型；`value` 是 `file` 的 `@AliasFor` 别名，按 effective attribute 继承。

### 8.6 `@SpELImport`

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

`rootLit`/`varLit` 的“不解析”语义用 `valueSpel=false` 表达，不再靠插件特判；`value`/`ifs` 是 Class 导入，`pack` 是包名列表，都不参与 SpEL。

### 8.7 `@ServerAddress`

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

- `value`/`url` 是模板；
- `urlFunc`/`pathFunc` 是函数引用；
- 函数示例：

```java
@FunctionAlias("getUrl")
public static String getUrl(@Var("baseUrl") String baseUrl) { ... }
```

插件检查：函数必须存在、返回值兼容 `String`、参数 `baseUrl` 可自动注入（`@Var(NORMAL)` → `#{#baseUrl}`）。

### 8.8 `@Hook`

```java
@Spel(only = {"enable"}, kind = SpelKind.CONDITION,
      stageAttribute = "lifecycle", result = boolean.class)
public @interface Hook {

    String enable() default "";

    Lifecycle lifecycle() default Lifecycle.NON;

    // hookHandle/hookHandleClass/order/errorInterrupt 不支持 SpEL
}
```

`enable` 的阶段跟随 `lifecycle`，插件在对应阶段提供变量补全；`lifecycle` 不是 SpEL，且不能是 `INHERIT`/`NEVER`/`UNKNOWN`。

### 8.9 `@Callback` 参数注入

```java
@Callback(lifecycle = Lifecycle.REQUEST_INIT,
          storeOrNot = true, storeName = "authToken")
public static String loadToken(@Env("auth.token") String token) { ... }
```

`@Callback` 的 `enable`/`lifecycle` 通过 `@AliasFor` 继承 `@Hook`；`lifecycle` 没有默认值，必须显式指定。`storeOrNot=true` 时才会把返回值存到 `storeName`，省略 `storeName` 时默认存到 `$方法名`；`storeName` 在 `literal=false`（默认）时本身也按 SpEL 解析。方法参数 `token` 通过 `@Env` 自动注入 `${auth.token}`，插件不需要用户为函数调用传参，并检查变量是否存在于当前阶段。

---

## 9. 解析优先级

1. 属性级 `@Spel(disabled=true)`：直接 `NONE`。
2. 属性级 `@Spel`：覆盖 kind/stage/result/separator。
3. `stageAttribute`：属性级 `stage` 为空时，读同注解的属性值（如 `@Hook#lifecycle`）；值再为空或 `INHERIT` 时进入下一级。
4. 类型级 `@Spel`：`only`/`all` 列表 + 默认值。
5. 元注解链：组合注解按 `@AliasFor` 找 effective attribute。
6. Javadoc/Java 签名：名称、类型、说明、示例。
7. 插件默认：未知阶段、未知类型降级。

配置字段额外遵循：字段级 `@Spel` > `@SpelConfig.stage`/`valueSpel` > 默认值。字段没有 `@Spel` 时，按 `@SpelConfig.valueSpel` 决定是否识别；为 `false` 时整个配置模型默认不识别 SpEL。

**数组对齐**：`result` 为空表示不限制；`typeExpression`/`constraint` 为空表示广播；长度 1 表示广播；长度大于 1 时必须与 `result` 等长，否则按元数据声明错误处理。

**函数引用**：`kind=FUNCTION` 时先解析函数名/表达式，再按 Java 签名读取返回值与参数；参数注入先看 `@Param`/`@Var` 系列的内置规则，无注解时再按 `Context#getMethodParamObject` 的类型注入规则处理。

**别名规则**：`@Get#value` → `@HttpRequest#url` 时，以 effective attribute 的 `@Spel` 为准；声明端只补充文档，不改变语义。`@Callback`/`@Val`/`@Pack` 的 `enable`/`lifecycle` 通过 `@AliasFor` 继承 `@Hook`，同样按 effective attribute 解析。

---

## 10. 性能架构

### 10.1 构建期索引

构建时从注解 + Javadoc 生成 `META-INF/lucky-spel-metadata.json`：

- `@Spel` 声明：注解 → 属性 → kind/stage/result/separator；
- `@SpelSymbol` 声明：符号名/类型/阶段；
- `@SpelConfig` 声明：prefix/naming/字段列表；
- 函数索引：函数名/命名空间/返回值/参数类型/参数注入注解或按类型注入；
- Hook 索引：`@Hook`/`@Callback` 的阶段、enable、回调方法；
- 参数注入索引：`@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 的引用形式，以及按类型注入的类型清单；
- Javadoc 提取：说明、`@param`、`@return`、`@since`、`@deprecated`、示例。

索引是构建产物，不是手写 JSON。Javadoc 只在构建期解析一次，运行期插件读索引。

### 10.2 用户注解

- 用户项目中的 `@Spel`/`@SpelSymbol`/`@SpelConfig` 按 `PsiClass` 按需解析；
- 按 `PsiModificationTracker.modificationCount` 缓存；
- 不扫描全项目注解；
- Javadoc 解析结果缓存到 `PsiClass`/`PsiMethod`；
- 函数方法的参数注解按 `PsiMethod` 缓存；
- `@Param`/`@Var` 系列按 `PsiParameter` 缓存。

### 10.3 性能热点

- Bean 补全不再用 `PsiShortNamesCache.allClassNames` 扫全项目；用 Spring 注解/`@SpelSymbol` 索引。
- 函数补全按函数名/命名空间 O(1) 查索引，不遍历全项目静态方法。
- Hook 补全按 `Lifecycle` 阶段过滤，不每次全量扫描。
- 注解属性元数据按 `PsiClass` 缓存为不可变 Map，查表 O(1)。

---

## 11. 与 v1 的对比

| 项目 | v1（15 注解） | v2（3 注解） |
|---|---:|---:|
| 新增注解 | 15 | 3 |
| 注解属性总数 | ~150 | ~25 |
| 框架侧声明处 | ~500 | ~250–300 |
| 文档性字段 | 全部写注解 | Javadoc 读取 |
| 函数签名 | 注解重复 | Java 签名读取 |
| 默认值 | 注解重复 | `default` 读取 |
| 预估工作量 | 13–19 人天 | 9–12 人天 |

---

## 12. 落地顺序

### P0：核心三注解

- 在 `lucky-httpclient` 新增 `com.luckyframework.httpclient.proxy.spel.meta` 包：3 个注解 + 4 个枚举。
- `@Spel` 支持 `result`/`typeExpression`/`constraint` 数组、`kind=FUNCTION`、`stageAttribute`。
- 扩展 `com.luckyframework.httpclient.proxy.spel.hook.Lifecycle`：新增 `INHERIT`、`NEVER`、`UNKNOWN`，运行期忽略。
- 迁移 7 个核心注解：`HttpRequest`、`ServerAddress`、`Retryable`、`ConvertMetaType`、`Binary`、`SpELImport`、`Hook`；`@Callback` 通过 `@AliasFor` 继承，不单独标注。
- 生成器：读注解 + Javadoc，生成索引；扫描范围要覆盖 `annotations`、`generalapi`、`proxy/mock`、`proxy/handle`、`proxy/destroy`，并识别 `parseLongExpression`/`parseIntExpression` 这类间接求值。
- 插件：读取索引 + 按需 PSI + 缓存。

### P1：符号、参数注入与 Hook

- `@SpelSymbol` 接入内置变量、公共函数、用户自定义参数注解。
- 内置 `@Param` 由插件硬编码识别，不标注 `@SpelSymbol`。
- `@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 参数注入规则与补全。
- `@ServerAddress#urlFunc`/`pathFunc` 函数引用的补全、跳转、返回值/参数检查。
- `@Hook`/`@Callback` 的阶段跟随、`enable` 条件、回调方法参数注入。
- `@SpelConfig` 接入配置模型。
- 补齐当前 `annotations.json` 未覆盖的注解与属性：`@ChunkUpload`、`@Describe`、`@Destroy`、`@ExceptionHandle`/`@ExceptionReturn`/`@ExceptionFallback`、`@Async`/`@AsyncExecutor`/`@AsyncHook`、`@Timeout`、`@TextEventStream`、`@MultiFile`、`@HttpProxy`/`@SocksProxy`、`@PrintRequestLog`/`@PrintResponseLog`、`@PropertiesJava`/`@PropertiesJson`、`@RecordReplay`、`@OAuth2ClientCredentialsGrant`/`@OAuth2TokenExchange`、`@Mock`/`@MockMeta`/`@MockPlugin`、`@Wrapper`、`@HttpExec`、`@RespConvert`/`@ResultConvertMeta`，以及 `@Destroy#func`、`@MockPlugin#implFunc`、`@Mock#mockFunc`/`enableFunc`、`@Wrapper#fun`、`@HttpExec#execFunc`、`@RespConvert#respContentTypeFunc` 等函数引用属性；同时删除源码已不存在的旧属性 `@Mock#getEnable`、`@Condition#getAssertion`。

### P2：Javadoc 增强

- 解析 Javadoc 示例、`@since`、`@deprecated`、`@param`/`@return`。
- 悬停文档、示例运行、弃用提示。

---

## 13. 验收清单

- [ ] 每个 SpEL 注解至少有一个类型级 `@Spel`（`only` 或 `all`）。
- [ ] 每个非 `String` 结果属性有属性级 `result`/`constraint` 覆盖。
- [ ] 多结果类型按“任一候选通过”检查，数组长度错误能被报告。
- [ ] `@ConvertMetaType#type` 的 5 种候选类型任一通过。
- [ ] `@ServerAddress#urlFunc` 支持字面量函数名和 `#{...}` 动态函数名两种写法。
- [ ] 函数参数带 `@Param`/`@Var` 系列时按表达式注入，不带注解时按类型注入，两者都不满足时能给出提示。
- [ ] `@Var`/`@Rar`/`@Env`/`@Bean` 的引用形式正确。
- [ ] `@Hook#enable` 的阶段跟随 `lifecycle`。
- [ ] `Lifecycle` 新增的 `INHERIT`/`NEVER`/`UNKNOWN` 不会被注册为 Hook 阶段，也不能用于 `@Hook#lifecycle`。
- [ ] `@Spel(disabled=true)` 能正确排除属性。
- [ ] `@AliasFor` 别名继承 effective attribute 的声明。
- [ ] 73 个内置变量的名称/类型/阶段能从常量、字段类型、Javadoc 读取；当前 `builtin-symbols.json` 只收录 68 个，缺 `$_PARAM_CONTEXT_$` 和 `OrdinaryVarName` 的 4 个常量。
- [ ] 151 个内置函数（其中 132 个带 `@FunctionAlias`）不需要逐个写注解；只给需要覆盖的函数写 `@SpelSymbol`。
- [ ] `@Val` 字段值按 SpEL 解析，`@Pack` 字段值不解析；`@Callback`/`@Val`/`@Pack` 的 `lifecycle` 必填，`@Callback#storeName` 只在 `storeOrNot=true` 时生效。
- [ ] 配置 key 能按 `naming` 从字段名生成。
- [ ] Javadoc 缺失时插件降级，不报错。
- [ ] 补全 P95 延迟不超过迁移前 10%。

---

## 14. 结论

v2 的核心不是“少写几个注解”，而是**把信息放回它本来该在的地方**：

- 说明、参数、返回值、示例、版本 → Javadoc；
- 名称、类型、泛型、重载、默认值 → Java 签名/注解元素默认值；
- 只有“哪些属性能用 SpEL、结果是什么、何时执行、怎么拆分”才写注解。

这样框架开发者接入一个注解通常只需要：

```java
@Spel(only = {"url"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
```

其余交给 Javadoc 和 Java 签名。函数名属性用 `kind=FUNCTION`，Hook 用 `stageAttribute="lifecycle"`，都不需要新增注解类型。
