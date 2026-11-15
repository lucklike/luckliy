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
| `DECLARATION` | 框架自定义声明语法，如 `@SpELImport#root` |
| `CONDITION` | 结果必须是布尔 |
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

    Class<?> result() default Object.class;

    String typeExpression() default "";

    SpelConstraint constraint() default SpelConstraint.ANY;

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
| `result` | `Class<?>` | `Object.class` | 期望结果类型；`Object.class` 表示不限制。 |
| `typeExpression` | `String` | `""` | 泛型/复杂类型表达式，如 `java.util.List<com.x.Item>`；`result` 无法表达时使用。 |
| `constraint` | `SpelConstraint` | `ANY` | 粗约束；用于二进制、类型元数据等无法用单个 `Class` 表达的场景。 |
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
    @Spel(disabled = true)
    String func() default "";

    @Spel(disabled = true)
    RequestMethod method() default RequestMethod.NON;
}
```

- `all=true`：所有 `String`/`String[]` 属性默认支持 SpEL。
- `func` 是字符串但不想支持，用 `disabled=true` 排除一次。
- `method` 是枚举，本来就不在 `all` 范围，写 `disabled` 只是显式说明。

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
String func() default "";
```

**优先级**：属性级 `disabled=true` > 属性级 `@Spel` > 类型级 `@Spel` > 默认值。`only` 与 `all` 同时存在时以 `only` 为准；`all` 只影响 `String`/`String[]` 属性。

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

- 字段常量：读取常量值。例如 `$_STATUS_$ = "$status$"` → `$status$`。
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
- 变量名：`value()`，空时用参数名；
- 变量类型：参数类型；
- 生成 `$name`、`$index`、`$$index`、`name::type` 等别名。

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
    private String condition;

    @Spel(result = Integer.class)
    private String maxCount;

    // 不支持 SpEL 的字段不标
    private Boolean enable;
}
```

- key 从字段名 + `naming` 生成：`maxCount` → `max-count`。
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

    @Spel(disabled = true)
    String func() default "";

    RequestMethod method() default RequestMethod.NON;
}
```

`value` 是 `url` 的别名，插件按 effective attribute 继承；`method` 是枚举，不在 `only` 列表，无需 `disabled`（示例保留是为了显式说明）。

### 8.2 `@Retryable`

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

### 8.3 `@ConvertMetaType`

```java
@Spel(only = {"type", "respContentType"}, stage = Lifecycle.RESPONSE, result = String.class)
public @interface ConvertMetaType {

    Class<?> value() default Object.class;

    @Spel(constraint = SpelConstraint.TYPE)
    String type() default "";

    String typeFunc() default "";

    String respContentType() default "";

    String respContentTypeFunc() default "";
}
```

`type` 的结果支持 `Type`/`Class`/`ResolvableType`/`SerializationTypeToken`/`String`，用 `constraint=TYPE` 表达；插件内置这 5 种形态的检查规则，不需要 `alternatives`。`typeFunc`/`respContentTypeFunc` 是函数名，不参与 SpEL，不在 `only` 列表即可；现有 JSON 把 `respContentTypeFunc` 误标为 `TEMPLATE`，迁移时一并修正。

### 8.4 `@Binary`

```java
@Spel(only = {"condition", "name", "file", "filename", "contentType"},
      stage = Lifecycle.REQUEST_INIT,
      result = String.class)
public @interface Binary {

    @Spel(kind = SpelKind.CONDITION, result = boolean.class)
    String condition() default "";

    String name();

    @Spel(constraint = SpelConstraint.BINARY)
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

这 4 个属性之前因为 `parseLongExpression`/`parseIntExpression` 间接求值而漏检；现在一个类型级 `@Spel` + 4 个结果类型覆盖即可。

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

    @Spel(valueSpel = false, separatorAttribute = "separator")
    String[] rootLit() default {};

    @Spel(valueSpel = false, separatorAttribute = "separator")
    String[] varLit() default {};

    @Spel(disabled = true)
    String separator() default "=";
}
```

`rootLit`/`varLit` 的“不解析”语义用 `valueSpel=false` 表达，不再靠插件特判。

---

## 9. 解析优先级

1. 属性级 `@Spel(disabled=true)`：直接 `NONE`。
2. 属性级 `@Spel`：覆盖 kind/stage/result/separator。
3. 类型级 `@Spel`：`only`/`all` 列表 + 默认值。
4. 元注解链：组合注解按 `@AliasFor` 找 effective attribute。
5. Javadoc/Java 签名：名称、类型、说明、示例。
6. 插件默认：未知阶段、未知类型降级。

配置字段额外遵循：字段级 `@Spel` > `@SpelConfig.stage`/`valueSpel` > 默认值。字段没有 `@Spel` 时，按 `@SpelConfig.valueSpel` 决定是否识别；为 `false` 时整个配置模型默认不识别 SpEL。

**别名规则**：`@Get#value` → `@HttpRequest#url` 时，以 effective attribute 的 `@Spel` 为准；声明端只补充文档，不改变语义。

---

## 10. 性能架构

### 10.1 构建期索引

构建时从注解 + Javadoc 生成 `META-INF/lucky-spel-metadata.json`：

- `@Spel` 声明：注解 → 属性 → kind/stage/result/separator；
- `@SpelSymbol` 声明：符号名/类型/阶段；
- `@SpelConfig` 声明：prefix/naming/字段列表；
- Javadoc 提取：说明、`@param`、`@return`、`@since`、`@deprecated`、示例。

索引是构建产物，不是手写 JSON。Javadoc 只在构建期解析一次，运行期插件读索引。

### 10.2 用户注解

- 用户项目中的 `@Spel`/`@SpelSymbol`/`@SpelConfig` 按 `PsiClass` 按需解析；
- 按 `PsiModificationTracker.modificationCount` 缓存；
- 不扫描全项目注解；
- Javadoc 解析结果缓存到 `PsiClass`/`PsiMethod`。

### 10.3 性能热点

- Bean 补全不再用 `PsiShortNamesCache.allClassNames` 扫全项目；用 Spring 注解/`@SpelSymbol` 索引。
- 函数补全不再每次构建全量列表；索引 + 按需解析。
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
| 预估工作量 | 13–19 人天 | 6–9 人天 |

---

## 12. 落地顺序

### P0：核心三注解

- 在 `lucky-httpclient` 新增 `com.luckyframework.httpclient.proxy.spel.meta` 包：3 个注解 + 4 个枚举。
- 扩展 `com.luckyframework.httpclient.proxy.spel.hook.Lifecycle`：新增 `INHERIT`、`NEVER`、`UNKNOWN`，运行期忽略。
- 迁移 6 个核心注解：`HttpRequest`、`ServerAddress`、`Retryable`、`ConvertMetaType`、`Binary`、`SpELImport`。
- 生成器：读注解 + Javadoc，生成索引。
- 插件：读取索引 + 按需 PSI + 缓存。

### P1：符号与配置

- `@SpelSymbol` 接入内置变量、公共函数、用户自定义参数注解。
- 内置 `@Param` 由插件硬编码识别，不标注 `@SpelSymbol`。
- `@SpelConfig` 接入配置模型。

### P2：Javadoc 增强

- 解析 Javadoc 示例、`@since`、`@deprecated`、`@param`/`@return`。
- 悬停文档、示例运行、弃用提示。

---

## 13. 验收清单

- [ ] 每个 SpEL 注解至少有一个类型级 `@Spel`（`only` 或 `all`）。
- [ ] 每个非 `String` 结果属性有属性级 `result`/`constraint` 覆盖。
- [ ] `@Spel(disabled=true)` 能正确排除属性。
- [ ] `@AliasFor` 别名继承 effective attribute 的声明。
- [ ] `Lifecycle` 新增的 `INHERIT`/`NEVER`/`UNKNOWN` 不会被注册为 Hook 阶段。
- [ ] 73 个内置变量的名称/类型/阶段能从常量、字段类型、Javadoc 读取。
- [ ] 132 个函数不需要逐个写注解；只给需要覆盖的函数写 `@SpelSymbol`。
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

其余交给 Javadoc 和 Java 签名。
