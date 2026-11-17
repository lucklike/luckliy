# SpEL 元数据注解使用指南（v5，逐属性标注 + 组件识别 + 元素标记）

> 配套文档：`spel-metadata-annotations.md`（设计）、`spel-metadata-refactor-plan.md`（实施方案与问题清单）
> 适用对象：框架开发者（给注解加 SpEL 支持）、插件维护者（读取元数据）
> 实现现状：**插件不读取任何预编译 JSON 清单**。`spel/metadata.json`、`spel/annotations.json`、`spel/builtin-symbols.json` 及生成工具已全部删除；注解语义、`@SpelSymbol` 变量、`@LuckyComponent` 组件与默认注册函数都在 IDE 运行期从 PSI/索引解析，用户扩展的注解与框架内置注解走同一条路径。本文提到 JSON 索引的地方只作历史方案记录。
> 核心变化：**没有类级注解，没有属性名清单。支持 SpEL 的属性逐条标 `@SpelAttribute`；分隔符、阶段来源、符号名来源分别标在 `@SpelSeparator`/`@SpelStage`/`@SpelName` 元素上；组件身份与类型用 `@LuckyComponent(type = ...)` 显式声明（类上、方法上或作为元注解，只给 IDEA 插件读）。**

---

## 1. 使用模型

### 1.1 七个注解的分工

| 注解 | 标在哪 | 回答什么问题 |
|---|---|---|
| `@SpelAttribute` | 注解元素方法、配置类字段 | 这个属性能不能用 SpEL、怎么解析、结果是什么、什么时候生效 |
| `@SpelSeparator` | 分隔符元素（`String`） | 这个注解里哪个属性是 `KEY_VALUE` 的分隔符 |
| `@SpelStage` | 阶段来源元素（`Lifecycle`） | 这个注解里哪个属性提供生效阶段 |
| `@SpelName` | 符号名来源元素（`String`） | 这个参数注解里哪个属性是变量名 |
| `@SpelSymbol` | 字段、参数、参数注解类型 | 这个字段/参数是不是一个可以在 SpEL 里引用的符号 |
| `@SpelConfig` | 配置类 | 这个配置类的前缀、别名、命名策略是什么 |
| `@LuckyComponent` | 类、接口、方法、注解类型 | 这个文件是不是 lucky-httpclient 组件、是哪一类组件（只给 IDEA 插件读） |

没有类级“公共属性描述”；没有 `all`、没有 `disabled`、没有分组、没有继承，也不存在 `separatorAttribute`/`stageAttribute`/`nameAttribute` 这种属性名引用。

### 1.2 一句话规则

> **标了 `@SpelAttribute` 就参与 SpEL，没标就不参与。**
> **`KEY_VALUE` 的分隔符，标 `@SpelSeparator` 的那个元素就是。**
> **阶段来自哪个元素，标 `@SpelStage` 的那个就是；变量名来自哪个元素，标 `@SpelName` 的那个就是。**
> **标了 `@LuckyComponent`（类上、方法上或元注解链上）就是组件；`type` 写具体类型就用声明的，写 `AUTO` 或不写才看结构推断。**

不需要记住“类级优先还是属性级优先”，因为只有一层。不需要记住“多组之间谁覆盖谁”，因为不存在多组。

### 1.3 判定过程

插件/生成器对每个注解元素按顺序做三件事：

1. 元素上有没有 `@SpelAttribute`？有 → 用这条声明。
2. 没有的话，这个元素是不是别的元素的 `@AliasFor` 别名？是 → 去声明端找 `@SpelAttribute`。
3. 还没有的话，沿元注解链找同名元素上的 `@SpelAttribute`（框架组合注解兜底）。

三步都没有 → 不参与 SpEL。

第 3 步只用于“组合注解忘了写 `@AliasFor`”的框架内部场景，并且生成器会输出 warning（A8），由框架作者确认这是有意复用，而不是名字碰巧相同。

### 1.4 别名与组合注解

同一份语义只允许写一次。`@AliasFor` 的两端属于同一个 effective attribute，**只在声明端标注**：

```java
public @interface HttpRequest {

    @AliasFor("url")
    String value() default "";          // 别名端：不标

    @SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
    @AliasFor("value")
    String url() default "";            // 声明端：标

    // ...
}
```

两端都标会触发 A1（error），因为同一 effective attribute 出现了两条声明。

组合注解同理：`@Get#value` 是 `@HttpRequest#url` 的别名，声明只写在 `@HttpRequest#url` 上，`@Get` 不重复标注。插件解析 `@Get` 时按 `@AliasFor` 追到声明端。

---

## 2. 快速开始

给一个已有注解加 SpEL 支持的完整流程：

```java
// 改前
public @interface ServerAddress {
    String value() default "";
    String url() default "";
    String urlFunc() default "";
    String path() default "";
    String pathFunc() default "";
}
```

```java
// 改后：逐个属性标注
public @interface ServerAddress {

    @AliasFor("url")
    String value() default "";

    @SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
    @AliasFor("value")
    String url() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.REQUEST_INIT, result = String.class)
    String urlFunc() default "";

    @SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
    String path() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.REQUEST_INIT, result = String.class)
    String pathFunc() default "";
}
```

三步：

1. 找出这个注解里所有“框架会把值当 SpEL 处理”的属性；
2. 每个属性加一条 `@SpelAttribute`，写清楚阶段和结果类型；
3. 别名只标一次，`@AliasFor` 两端不要都标。

不需要在类上写任何东西，不需要维护 `{"url", "urlFunc", ...}` 这样的列表。

---

## 3. 详细用法

### 3.1 `@SpelAttribute` 与元素标记注解的九种用法

#### 用法一：最简（模板 + 不过滤阶段 + 不限制类型）

```java
@SpelAttribute
String description() default "";
```

默认值 `TEMPLATE` + `METHOD` + 不限制类型。适合“能写 SpEL 就行，不需要额外约束”的属性。

代价是插件不会在阶段不对时提示“这个阶段没有这个变量”——这是有意的：少提示，不误报。

#### 用法二：指定阶段和结果类型

```java
@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
String url() default "";
```

含义：只在 `REQUEST_INIT` 阶段求值，表达式结果应当是 `String`。

这是最常见的写法。

#### 用法三：条件属性

```java
@SpelAttribute(kind = SpelKind.CONDITION,
               stage = Lifecycle.REQUEST_INIT, result = boolean.class)
String condition() default "";
```

结果必须是 `boolean`/`Boolean`。插件会在条件位置提供变量补全，并在结果类型不是布尔时报错。

#### 用法四：函数引用

```java
@SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.REQUEST_INIT, result = String.class)
String urlFunc() default "";
```

值有两种写法：

- `"getUrl"`：字面量函数名 → 插件直接补全/校验/跳转函数；
- `"#{#funcName}"`：动态函数名 → 插件按 SpEL 表达式检查，结果类型必须是 `String`，若静态求值得到函数名再校验函数是否存在。

函数本身不需要额外注解：名称来自 `@FunctionAlias` 或方法名，命名空间来自 `@Namespace`，参数和返回值来自 Java 签名。

#### 用法五：多结果类型

```java
@SpelAttribute(
    stage = Lifecycle.RESPONSE,
    result = {
        java.lang.reflect.Type.class,
        Class.class,
        org.springframework.core.ResolvableType.class,
        com.luckyframework.serializable.SerializationTypeToken.class,
        String.class
    },
    constraint = {
        SpelConstraint.TYPE, SpelConstraint.TYPE,
        SpelConstraint.TYPE, SpelConstraint.TYPE, SpelConstraint.STRING
    }
)
String type() default "";
```

`result` 是“任一候选通过”的语义，`constraint` 与它按索引对齐。`typeExpression` 也是候选来源（用于 `Class` 无法完整表达的泛型类型），与 `result` 取并集，例如 `"java.util.Collection<java.io.File>"`；两者都为空才表示不限制类型。只写 `constraint` 不写 `result` 也可以，例如二进制形态：

```java
@SpelAttribute(stage = Lifecycle.REQUEST_INIT, constraint = SpelConstraint.BINARY)
String file();
```

#### 用法六：不解析 value（字面量）

```java
@SpelAttribute(kind = SpelKind.KEY_VALUE, stage = Lifecycle.METHOD, valueSpel = false)
String[] rootLit() default {};
```

`valueSpel = false` 表示 value 部分不作为 SpEL 求值，只当字面量。key 部分默认也不求值（`keySpel = false`）。

#### 用法七：分隔符标在元素上

```java
@SpelAttribute(kind = SpelKind.KEY_VALUE, stage = Lifecycle.METHOD)
String[] root() default {};

@SpelSeparator
String separator() default "=";
```

`@SpelSeparator` 只做一件事：告诉插件「这个元素的值是分隔符」。插件读取它的默认值/使用值来拆分 `KEY_VALUE`，不需要任何属性名。把 `separator` 改成 `delimiter`，注解跟着方法走，不会断。

也可以继续用字面量：`@SpelAttribute(..., separator = "=")`。两者同时存在时字面量优先。同一注解最多一个 `@SpelSeparator`（A11），只能标在 `String` 元素上（A12）。

#### 用法八：阶段来源标在元素上

```java
public @interface Hook {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelStage
    Lifecycle lifecycle() default Lifecycle.NON;
}
```

`@SpelStage` 标记的元素提供整个注解的默认阶段：同注解内所有 `@SpelAttribute` 元素，只要 `stage` 没有显式书写，都跟随它。所以 `@Hook#enable` 的生效阶段就是 `lifecycle` 的值。

元素类型必须是 `Lifecycle`（A6 error），同一注解最多一个（A14 error），所在注解至少要有一个 `@SpelAttribute`（否则 A18 warning）。`@Callback`/`@Val`/`@Pack` 的 `enable`/`lifecycle` 通过 `@AliasFor` 指向 `@Hook`，不重复标注。

它**不是类级注解回潮**：`@SpelStage` 没有属性值、不点名任何属性，只标记「运行期实参所在元素」；`kind`/`result` 这类声明期常量仍逐属性写死。判定标准见设计文档 6.5.2。

#### 用法九：可选的静态生效条件

```java
@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class,
               condition = "enable != false")
String url() default "";
```

只支持属性引用、字面量、比较、逻辑运算和括号。用于“这个属性只在另一个开关为真时才生效”的场景。复杂条件请用 Javadoc 说明，不要塞进 `condition`。

### 3.2 `@SpelSymbol` 的五种用法

> **函数不使用 `@SpelSymbol`。** 函数是否可用及其生命周期只由两条路径决定：`@SpELImport`（类上标注 → `CLASS` 阶段；方法上标注 → `METHOD_META` 阶段）与 `HttpClientProxyObjectFactory#addSpringElFunctionClass(Class<?>)`（工厂构造时注册，从 `CLASS` 阶段起可用）。函数名、命名空间、参数与返回值由 Java 签名和 Javadoc 读取。

#### 用法一：内置变量（最简）

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

名称取常量值 `"$status$"`；类型优先 Javadoc `@spelType`，否则字段类型；阶段优先注解 `from`，否则 Javadoc `@spelStage`，否则 `METHOD`。

如果不想用自定义 Javadoc tag：

```java
@SpelSymbol(type = Integer.class, from = Lifecycle.RESPONSE)
public static final String $_RESPONSE_STATUS_$ = "$status$";
```

#### 用法二：泛型类型

```java
@SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>",
            from = Lifecycle.RESPONSE_INIT)
public static final String $_RESPONSE_HEADER_$ = "$respHeader$";
```

字段是 `String` 常量、真实类型是泛型时才需要 `typeExpression`；`Class` 能完整表达的类型直接写 `type`。

#### 用法三：内置参数注解（不需要标注）

`@Param` 在 `lucky-core`，`@SpelSymbol` 在 `lucky-httpclient`，core 不能反向依赖 httpclient，所以 `@Param` 不标注，由插件内置识别：

- 全限定名 `com.luckyframework.reflect.Param`；
- 变量名取 `value()`，必填；为空时框架抛 `@Param annotation has no value.`；
- 变量类型取参数类型；
- 自动生成 `name`/`$name`/`$index`/`$$index`/`name::type`/`$index::type`/`$pc$name`/`$pc$index` 等别名。

`@Var`/`@Rar`/`@Env`/`@Bean` 同理，由插件内置识别，不需要标注。

**必须按全限定名识别**：`com.luckyframework.httpclient.generalapi.ai.tool.Param` 是 AI 工具调用的参数描述，不参与 SpEL，必须排除。

#### 用法四：用户自定义参数注解

```java
@SpelSymbol(kind = SpelSymbolKind.PARAM)
public @interface MyParam {

    @SpelName
    String name();
}
```

`@SpelName` 指定哪个元素提供变量名：不标时回退到 `value` 元素（保持旧默认行为），所以只有元素不叫 `value` 才需要标。元素必须是 `String`（A16 error），同一注解最多一个（A15 error），所在注解必须标 `@SpelSymbol`（否则 A17 warning）。

#### 用法五：Spring Bean 覆盖

```java
@SpelSymbol(name = "users", from = Lifecycle.REQUEST_INIT)
@Component("userService")
public class UserService { ... }
```

默认从 Spring 注解读 Bean 名和类型，不需要 `@SpelSymbol`；只有需要改名字或改可见阶段时才加。

### 3.3 `@SpelConfig` 的三种用法

#### 用法一：最简

```java
@SpelConfig(prefix = "lucky.api")
public class ApiConf {

    @SpelAttribute(stage = Lifecycle.CONFIG_API_INIT, result = String.class)
    private String baseUrl;
}
```

#### 用法二：命名策略

```java
@SpelConfig(prefix = "lucky.api", naming = SpelNaming.KEBAB_CASE)
public class RetryConf {

    @SpelAttribute(stage = Lifecycle.CONFIG_API_INIT, kind = SpelKind.CONDITION, result = boolean.class)
    private String expression;

    private Integer maxCount;   // key = max-count，不支持 SpEL，不标
}
```

#### 用法三：多前缀

```java
@SpelConfig(prefix = "lucky.http", aliases = {"lucky.api", "http"})
public class HttpConf { ... }
```

注意：

- `@SpelConfig` **没有** `stage`，需要阶段的字段必须自己写 `@SpelAttribute(stage = ...)`；
- `@SpelConfig` **没有** `valueSpel`，字段不支持 SpEL 就不标，不存在“整个模型默认识别”的隐式规则；
- 字段说明从 Javadoc 读取。

### 3.4 Hook 与 `@Callback`

```java
public @interface Hook {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelStage
    Lifecycle lifecycle() default Lifecycle.NON;
}
```

```java
@Callback(lifecycle = Lifecycle.REQUEST_INIT, storeOrNot = true, storeName = "authToken")
public static String loadToken(@Env("auth.token") String token) { ... }
```

规则：

- `@Callback` 的 `enable`/`lifecycle` 通过 `@AliasFor` 指向 `@Hook`，不重复标注；`enable` 的阶段由 `@Hook#lifecycle` 上的 `@SpelStage` 提供；
- `lifecycle` 没有默认值，必须显式指定；`NON` 表示不执行，插件侧不认识的枚举值降级为 `UNKNOWN`；
- `storeName` 只在 `storeOrNot = true` 时生效；省略时默认存到 `$方法名`，且默认按 SpEL 解析；
- 回调方法参数带 `@Param`/`@Var` 系列时按表达式注入；不带注解时按类型注入（`MethodContext`、`Request`、`Response`、`Throwable` 等），两者都不满足时提示可能为 null；
- `@Val` 字段值按 SpEL 解析，`@Pack` 字段值不解析。

### 3.5 `@LuckyComponent` 的标注位置与用法

```java
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuckyComponent {
    LuckyComponentType type() default LuckyComponentType.AUTO;
}
```

它回答两件事：这个类是不是 lucky-httpclient 组件、是哪一类组件。

**只给 IDEA 插件用**：框架运行期从不读取它，删掉不改变任何行为。插件用它做四件事——文件标记（图标）、组件聚合（按类型出清单）、搜索（按类型/展示名定位）、调试（从组件跳到相关 SpEL 元数据）。它不参与 SpEL，也不是 `@Component` 的替代。包位置：`com.luckyframework.httpclient.proxy.spel.meta`（`LuckyComponentType` 同包）。

**插件怎么判断一个类是不是组件**：类上、类上注解的元注解链、类的方法上，三处任意一处有 `@LuckyComponent` 就是；`type` 也按这个顺序取第一个非 `AUTO` 的值。

```text
1. 类/接口上直接标注                    → source = class
2. 类上某个注解的元注解链上有            → source = meta
3. 方法上（直接标或方法注解链上有）      → source = method（方法记为组件方法）
4. 都没有                                → 结构推断（source = infer），推不出则不是组件
```

同一个类命中多个不同的具体类型时取优先级最高的值，并报 A9 warning。

| `type` | 含义 | 图标与颜色 |
|---|---|---|
| `AUTO` | 默认值，按结构推断；推不出为 `OTHER` | 按推断结果 |
| `API` | HTTP API 接口 | 绿 `</>` |
| `CONFIG` | 配置模型 | 蓝齿轮 |
| `FUNCTION` | SpEL 函数库 | 橙 `f(x)` |
| `SUPPORT` | 支撑类 | 紫扳手 |

`OTHER` 只作为推断兜底，写不进注解。

#### 用法一：标注 API 接口

```java
/**
 * 用户中心 API
 */
@LuckyComponent(type = LuckyComponentType.API)
@HttpRequest(url = "https://api.example.com")
public interface UserApi {

    @Get("/users/{id}")
    User getUser(@Param("id") String id);
}
```

有 `@HttpRequest` 的接口本来就能被推断为 API；显式写 `API` 的作用是固定类型，图标与颜色不随推断规则变化。

#### 用法二：标在框架注解上（元注解）

```java
@LuckyComponent(type = LuckyComponentType.API)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpRequest { ... }
```

框架已经这样标了 5 个身份注解：`@HttpRequest`/`@ServerAddressMeta`/`@ApiConfig` → `API`，`@SpelConfig` → `CONFIG`，`@Namespace` → `FUNCTION`。所以标了 `@Get` 的接口、以及接口里标了 `@Get` 的方法，都会经 `@Get` → `@HttpRequest` 的元注解链被识别为 `API` 组件，使用处不需要再标。

#### 用法三：标注支撑类

```java
/**
 * 用户对象生成器
 */
@LuckyComponent(type = LuckyComponentType.SUPPORT)
public class UserGenerate implements ObjectGenerate {

    @Override
    public Object generate(GenerateContext context) { ... }
}
```

`ObjectGenerate` 实现、自定义 `ObjectFactory`、Hook 处理器、Mock 实现都能标；它们身上没有 `@HttpRequest`，推断不出 `API`。

#### 用法四：标注配置类与 POJO

```java
/**
 * 通用返回结构
 */
@LuckyComponent(type = LuckyComponentType.CONFIG)
public class ApiResult<T> {
    private int code;
    private String message;
    private T data;
}
```

被 `@SpelConfig` 嵌套引用的 POJO、被 `@SpelSymbol` 引用的常量库也归到 `CONFIG`。

#### 用法五：函数库与 `AUTO`

```java
/**
 * 通用字符串函数
 */
@LuckyComponent(type = LuckyComponentType.FUNCTION)
@Namespace("str")
public class StringFunctions {
    ...
}
```

不想现在分类时只写 `@LuckyComponent`（默认 `AUTO`）：能推断就用推断结果，推不出给通用图标并报 A10 warning。第三方或生成代码无法改注解时也是同样的兜底。

#### 用法六：只在方法上标

```java
public interface OrderApi {

    @LuckyComponent(type = LuckyComponentType.API)
    @Get("/order/{id}")
    Order getOrder(@Param("id") String id);
}
```

接口类上没有任何组件标记，插件仍会把它识别为 `API` 组件，并把 `getOrder` 记为组件方法（`source = method`）。适合只在入口方法上打标记、不想在类上重复声明的场景。

展示名不需要写在注解里：

- 取 Javadoc 第一段首句（截止到 `。`、`.` 或换行）；
- 没有注释时显示简单类名。

四条规则：

- **元注解可继承**：标在注解类型上会传给使用处（如 `@Get` → `@HttpRequest`），`source = meta`；
- **不继承类**：子类不会因为父类标了就成为组件（无 `@Inherited`）；
- **不参与 SpEL**：不进 `spelAnnotations`，不影响补全与检查；
- **不参与 Spring**：不是 `@Component` 的替代，不触发 Bean 注册。

---

## 4. 七个真实场景

### 场景一：给 `@HttpRequest` 加 SpEL

```java
@AliasFor("url")
String value() default "";

@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
@AliasFor("value")
String url() default "";

@SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.REQUEST_INIT, result = String.class)
String func() default "";
```

要点：`url` 是声明端，`value` 是别名；`stage` 必须显式写，没有类级默认。

### 场景二：`@Retryable` 的数字属性

```java
@SpelAttribute(stage = Lifecycle.RETRY, result = Long.class)
String waitMillis() default "1000";

@SpelAttribute(stage = Lifecycle.RETRY, result = Long.class)
String maxWaitMillis() default "10000";
```

框架用 `parseLongExpression` 求值，结果必须是 `Long`。`retryCount` 是 `Integer`，`multiplier` 是 `Double`。

### 场景三：`@ChunkUpload` 的 8 个属性

```java
@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = {java.io.File.class, String.class})
@AliasFor("value")
String file() default "";

@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
String fileId() default "";

@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = Long.class)
String chunkSize() default "";
```

`file` 的表达式结果可以是 `File` 或路径字符串，所以给两个候选类型；`value` 是它的别名，只在 `file` 上标注。

### 场景四：`@Binary` 的二进制属性

```java
@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
String name();

@SpelAttribute(stage = Lifecycle.REQUEST_INIT, constraint = SpelConstraint.BINARY)
String file();
```

`file` 不写 `result`，只写 `constraint = BINARY`，表示结果形态是二进制（`byte[]`/`InputStream`/`File`），不绑定具体类。

### 场景五：`@SpELImport` 的字面量属性

```java
@SpelAttribute(kind = SpelKind.KEY_VALUE, stage = Lifecycle.METHOD, valueSpel = false)
String[] rootLit() default {};

@SpelSeparator
String separator() default "=";
```

`root`/`var` 的 value 参与求值，`rootLit`/`varLit` 不参与。分隔符由 `@SpelSeparator` 标在 `separator` 元素上，`@SpelAttribute` 里不再写 `separatorAttribute = "separator"`。`value`/`ifs` 是 Class 导入、`pack` 是包名列表，都不标。

### 场景六：`@ServerAddress` 的函数引用

```java
@SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.REQUEST_INIT, result = String.class)
String urlFunc() default "";
```

```java
@FunctionAlias("getUrl")
public static String getUrl(@Var("baseUrl") String baseUrl) { ... }
```

插件检查：函数存在、返回值是 `String`、参数 `baseUrl` 能自动注入。

### 场景七：`@Hook` 的阶段跟随

```java
@SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
String enable() default "";

@SpelStage
Lifecycle lifecycle() default Lifecycle.NON;
```

`enable` 的阶段跟随 `lifecycle`，不在对应阶段的变量补全会按那个阶段过滤。

---

## 5. 改动量评估

### 5.1 统计基线

| 指标 | 数值 |
|---|---:|
| 框架注解总数 | 68 |
| 注解属性总数 | 162 |
| 参与 SpEL 的属性 | ~137（TEMPLATE 102、IF_CONDITION 31、KEY_VALUE 4） |
| `@FunctionAlias` 函数 | 132 / 151 |
| 内置变量常量 | 73 |
| 内置函数 | 151 |
| 配置 API 非静态字段 | 232 |
| 现有插件用例 | 623 |

### 5.2 声明量对比

| 项目 | v3（类级 + 属性级） | v4（逐属性） |
|---|---:|---:|
| 类级声明 | 68 | 0 |
| 属性级声明 | ~60 | ~137 |
| 总声明处 | ~128 | ~137 |
| 总字符数 | 约 10.9k | 约 13.5k（+24%） |
| 维护的属性名清单 | 68 份 | 0 |

声明处只多 7%，字符数多约 24%，主要来自“每个属性都要写 `stage`/`result`”。用最简写法 `@SpelAttribute`（不写参数）可以把增量压到 10% 以内。

`@LuckyComponent` 是可选标注，只影响插件侧的文件标记、聚合、搜索与调试，不计入 SpEL 声明量；框架已有的身份注解（`@HttpRequest` 等）已作为元注解标好，用户项目通常不需要再写。

### 5.3 P0 改动量

| 项目 | 文件数 | 预估行数 |
|---|---:|---:|
| `meta` 包 + `@LuckyComponent`：7 注解 + 5 枚举 | 12 | 405 |
| 7 个核心注解逐属性标注 | 10 | 300 |
| 生成器（注解 + Javadoc → 索引 + `ComponentTypeResolver` + `separatorElement`/`stageElement`/`nameElement` + A1–A19） | 7 | 450 |
| 插件读取端（索引 + PSI + 5 套组件图标 + 分隔符/阶段/名称解析） | 9 | 650 |
| 测试 | 12 | 480 |
| **合计** | **50** | **约 2285** |

人天：6–7。

### 5.4 P1 改动量

| 项目 | 文件数 | 预估行数 |
|---|---:|---:|
| `@SpelSymbol` 接入（变量/参数注解） | 10 | 360 |
| 参数注入识别（`@Param`/`@Var` 系列） | 6 | 300 |
| 函数引用（33 个属性名） | 8 | 350 |
| Hook/`@Callback` 阶段跟随 | 6 | 280 |
| `@SpelConfig` | 5 | 220 |
| 补齐 30+ 个注解 | 20 | 400 |
| 测试 | 16 | 480 |
| **合计** | **71** | **约 2390** |

人天：6–7。

### 5.5 P2 改动量

| 项目 | 预估行数 |
|---|---:|
| Javadoc 示例/`@since`/`@deprecated` | 250 |
| 悬停文档增强 | 180 |
| 弃用提示 | 120 |
| **合计** | **约 550** |

人天：2–3。

### 5.6 汇总

| 阶段 | 人天 |
|---|---:|
| P0 | 6–7 |
| P1 | 6–7 |
| P2 | 2–3 |
| 回归 + 发版 | 2–3 |
| **合计** | **16–21** |

与 v3 的 16–21 持平：少实现了组匹配和白名单审计，多了约 70 处逐属性标注；比只做分隔符的版本多约 1 人天，花在两个元素标记注解的解析与审计上。

### 5.7 七点

1. **没有类级注解。** 属性多且语义相同的注解（如 `@ChunkUpload` 8 个属性）要写 8 行；这是为了属性改名不静默失效。
2. **没有优先级规则。** 只有一条判定，新人不需要理解“类级 vs 属性级”。
3. **审计从“白名单校验”变成“一致性提示”**：A1/A2/A5/A6/A7/A11/A12/A14/A15/A16 是 error，A3/A4/A8/A9/A10/A13/A17/A18 是 warning。
4. **`knownAnnotations` 必须全量。** 否则未迁移的注解会从 `UNKNOWN` 降级变成“跳过”，误报增加。
5. **预编译清单已全部退出。** `metadata.json`/`annotations.json`/`builtin-symbols.json` 与生成工具已删除，插件只认源码里的注解与 Java 声明，不留 JSON 兜底。
6. **组件身份、类型与 SpEL 元数据分开。** `@LuckyComponent` 只给 IDEA 插件读，不进 `spelAnnotations`，不影响补全与检查，只影响文件标记、组件聚合、搜索与调试；`type` 只决定图标，不决定 SpEL 行为。
7. **分隔符、阶段来源、符号名来源都是元素，不是名字。** `@SpelSeparator`/`@SpelStage`/`@SpelName` 标在哪个元素上，哪个元素就提供这份信息；不存在 `separatorAttribute`/`stageAttribute`/`nameAttribute`。

### 5.8 不改的地方

- 插件现有的用例结构；
- 现有注解的 Javadoc；
- `@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 的框架实现；
- `Lifecycle` 的 16 个运行期值。

### 5.9 风险表

| 风险 | 影响 | 缓解 |
|---|---|---|
| 逐属性标注漏标 | 该属性静默不参与 | A4 warning + 运行时按 `@SpelAttribute` 实际声明判定 |
| 属性改名 | 注解跟着走，不会静默失效 | 无（这是 v4 的收益） |
| `@AliasFor` 两端都标 | A1 error | 审计 |
| 标在非 String 元素上 | A2 error | 审计 |
| 同一注解内 stage/result 不一致 | 行为不一致 | A4 warning |
| `@SpelSeparator` 漏标或标错元素 | `KEY_VALUE` 不拆分，补全退化为整串 | `@SpelAttribute.separator` 字面量兜底；A11/A12 校验唯一性与类型 |
| `@SpelStage` 漏标 | `enable` 等条件属性按 `METHOD` 处理，阶段过滤变宽松（不误报，但不提示阶段缺失） | A18 提示注解内无消费者；模板新增注解时带上 `@SpelStage` |
| `@SpelName` 漏标 | 元素不叫 `value` 时变量名读不到 | A17 提示所在注解无 `@SpelSymbol`；回退 `value` 兜底常规写法 |
| 元注解链同名误匹配 | 意外复用声明 | A8 warning；只对框架注解生效 |
| 未迁移注解被“跳过” | 未解析检查误报增加 | `knownAnnotations` 全量 |
| 声明量增加 | 迁移工作量上升 | 最简写法 + IDE 模板 |
| 显式类型与结构不匹配（标了 `API` 但没有 `@HttpRequest`） | 图标可能名不副实 | 声明优先、不静默覆盖；评审时看 `declared` 与 `type` 是否一致 |
| 同一类命中多个不同的具体 `type` | 图标类型不确定 | A9 warning，按类 > 方法、直接 > 元注解的顺序取第一个 |
| `AUTO` 推不出类型 | 只能显示通用图标 | A10 warning 提示补类型 |
| 图标资源缺失或命名不一致 | 图标空白 | 统一资源目录 + 缺失降级为 `luckyComponent.svg` |
| 文件图标在 IDEA 2026.3 延迟显示 | 图标不立即出现 | 沿用 `iconProvider` + `fileIconProvider` 双 EP |

### 5.10 预编译 JSON 清单已删除

插件侧不再存在 `annotations.json`、`builtin-symbols.json`、`metadata.json`，也不再有生成器或加载器：

| 数据 | 运行期来源 |
|---|---|
| 注解属性语义 | 注解声明上的 `@SpelAttribute` 等元注解（含 `@AliasFor`/元注解链） |
| 内置变量 | `@SpelSymbol` 标注的字段常量（如 `InternalRootVarName`） |
| 默认注册函数 | `HttpClientProxyObjectFactory#importCommonFunction` 中的 `addSpringElFunctionClass` 调用 |
| 组件清单 | `@LuckyComponent` 标注的类/方法及其元注解链 |
| 描述、参数、返回值 | Javadoc 与 Java 签名 |

扫描结果按 `PsiModificationTracker` 缓存，dumb 模式下返回空结果；用户在自己模块里添加同样的注解即可立即生效，不需要重新生成任何文件。

---

## 6. 验收清单

模型：

- [ ] 每个参与 SpEL 的属性都是因为元素上有 `@SpelAttribute`，不存在字符串白名单。
- [ ] 删掉一条 `@SpelAttribute`，该属性立即不参与；加上立即参与。
- [ ] 注解属性改名后，注解跟着方法走，仍参与 SpEL，不存在“清单未更新导致静默失效”。
- [ ] 同一 effective attribute 只在声明端标注一次；两端都标触发 A1。
- [ ] `@SpelAttribute` 标在非 `String`/`String[]`/`Object`/`Object[]` 元素上触发 A2。
- [ ] `@SpelSymbol` 的 `@Target` 为 `FIELD`/`PARAMETER`/`ANNOTATION_TYPE`，不含 `METHOD`；函数识别不依赖 `@SpelSymbol`，只由 `@SPELImport` 与 `addSpringElFunctionClass` 决定。
- [ ] `@SpelAttribute` 与 `@SpelSymbol` 同时标在同一元素触发 A3。
- [ ] 同一注解内多个 `@SpelAttribute` 的 `stage`/`result` 不一致触发 A4。
- [ ] `constraint` 长度不匹配或 `typeExpression` 无法解析触发 A5；同一原始类型两处重复触发 A19。
- [ ] `@SpelStage` 指向非 `Lifecycle` 元素触发 A6；同一注解内多个触发 A14。
- [ ] 元注解链同名匹配拿到声明时触发 A8，并列出元素名。
- [ ] `SpelKind` 无 `INHERIT`；`Lifecycle` 无 `INHERIT`；`@SpelAttribute` 无 `all`/`disabled`/`only`/`separatorAttribute`/`stageAttribute`/分组属性。
- [ ] `@SpelConfig` 无 `stage`、无 `valueSpel`。
- [ ] `@SpelSeparator` 无属性；同一注解内最多一个，只能标在 `String` 元素上（A11/A12）。
- [ ] 把 `@SpELImport#separator` 改名为 `delimiter` 后，`@SpelSeparator` 跟着方法走，`KEY_VALUE` 拆分仍生效。
- [ ] `@SpelStage` 无属性；同一注解内最多一个，只能标在 `Lifecycle` 元素上；所在注解无 `@SpelAttribute` 触发 A18。
- [ ] 把 `@Hook#lifecycle` 改名为 `stage` 后，`@SpelStage` 跟着方法走，`enable` 的阶段仍跟随。
- [ ] `@SpelName` 无属性；同一注解内最多一个（A15），只能标在 `String` 元素上（A16）；所在注解无 `@SpelSymbol` 触发 A17。
- [ ] 把 `@MyParam#name` 改名为 `key` 后，`@SpelName` 跟着方法走，变量名仍正确。
- [ ] `@LuckyComponent` 只有 `type()`，不含 `value`/`name`/`kind`/`stage` 等 SpEL 语义，框架运行期不读取。
- [ ] `@LuckyComponent` 的 `@Target` 为 `TYPE`/`ANNOTATION_TYPE`/`METHOD`；类上、元注解链上、方法上三处任一命中即为组件。
- [ ] `LuckyComponentType` 只含 `AUTO`/`API`/`CONFIG`/`FUNCTION`/`SUPPORT`，`OTHER` 只在插件与索引侧。
- [ ] `type = API/CONFIG/FUNCTION/SUPPORT` 时直接使用声明值，不做推断，`source = class`。
- [ ] 接口标 `@Get` 时经 `@HttpRequest` 的元注解链被识别为 `API` 组件（`source = meta`）。
- [ ] 类上无标记、仅某个方法命中时，类仍为组件（`source = method`），方法记入 `methods`。
- [ ] 同一个类命中多个不同的具体 `type` 时触发 A9；子类不受父类标注影响（无 `@Inherited`）；不参与 Spring 注册。
- [ ] `@LuckyComponent(type = AUTO)` 且推不出类型时触发 A10。

数据：

- [ ] 注解语义只来自 `@SpelAttribute` 声明，没有类级元素表，也没有 `typeLevel`。
- [ ] 声明端与使用端信息都可从 PSI 直接读出，不需要离线快照。
- [ ] 框架注解与非 SpEL 注解按 PSI 是否携带 SpEL 元注解区分，未携带者不参与 SpEL，也不误报未解析。
- [ ] `@SpelSeparator`/`@SpelStage` 只在声明模型中作为元素来源，不被当作普通属性符号输出。
- [ ] `@SpelName` 只在 `@SpelSymbol` 注解类型的变量名推导中生效，不作为普通符号输出。
- [ ] 组件清单来自 `@LuckyComponent`：`className`/`title`/`type`/`declared`/`source` 字段齐备，`source = method` 的类仍被识别。
- [ ] 两个 `@Param` 按全限定名区分，`generalapi.ai.tool.Param` 不产生任何 SpEL 行为。
- [ ] 框架 `Lifecycle` 未新增枚举值；`@SpelSymbol.until` 用 `NON` 表示无上界；`UNKNOWN` 只在插件侧。
- [ ] `annotations.json`/`builtin-symbols.json`/`metadata.json` 不存在，插件运行期只从 PSI/索引读取元数据。

功能：

- [ ] 多结果类型按 `result ∪ typeExpression` 的“任一候选通过”检查。
- [ ] `urlFunc` 支持字面量函数名和 `#{...}` 动态函数名。
- [ ] 函数参数带注入注解时按表达式注入，不带时按类型注入，都不满足时提示。
- [ ] `@Hook#enable` 阶段跟随 `lifecycle`；把 `lifecycle` 换成另一个 `Lifecycle` 元素并把 `@SpelStage` 移过去，行为同步变化。
- [ ] `@Val` 字段值按 SpEL 解析，`@Pack` 不解析。
- [ ] 配置 key 按 `naming` 生成。
- [ ] `KEY_VALUE` 属性能按 `@SpelSeparator` 元素的值拆分；字面量 `separator` 优先。
- [ ] `@SpelSeparator` 元素被改名/删除时，A11/A12 报出具体注解与元素。
- [ ] `@SpelStage`/`@SpelName` 元素被改名/删除时，A6/A14–A18 报出具体注解与元素。
- [ ] `API`/`CONFIG`/`FUNCTION`/`SUPPORT` 四类组件显示四套不同图标与颜色；`AUTO` 推不出时显示通用图标。
- [ ] 取消 `@LuckyComponent`（类上、方法上、元注解上全部取消）后图标消失。
- [ ] 组件展示名取 Javadoc 首句，无注释时用类名。
- [ ] 组件清单能按 `type` 分组、按 `source` 标注识别入口，并对 `AUTO`/未标注类给出推断结果。
- [ ] Javadoc 缺失时降级不报错。
- [ ] 补全 P95 延迟不超过迁移前 10%。
- [ ] 623 条用例全绿，`buildPlugin` 通过。

---

## 7. PR 拆分

| PR | 范围 | 依赖 |
|---|---|---|
| PR0 | 删除 `annotations.json`/`builtin-symbols.json`/`metadata.json` 与生成工具 | 无 |
| PR1 | `meta` 包：7 注解 + 5 枚举（`Lifecycle` 不改） | 无 |
| PR2 | 7 个核心注解逐属性标注 | PR1 |
| PR3 | 运行时扫描：`@SpelSymbol` 变量 + `addSpringElFunctionClass` 函数 + `@LuckyComponent` 组件 + A1–A19 审计 | PR1、PR2 |
| PR4 | 插件读取端切运行时解析（元注解 + `@SpelSymbol` + `addSpringElFunctionClass` + 组件识别 + 四类图标映射） | PR3 |
| PR5 | 参数注入（`@Param`/`@Var` 系列）+ 函数引用（33 个属性名） | PR4 |
| PR6 | Hook/`@Callback` 阶段跟随 + 参数注入 | PR4、PR5 |
| PR7 | `@SpelSymbol` 接入（变量/参数注解，含 `@SpelName`） | PR4 |
| PR8 | `@SpelConfig` + 补齐 30+ 个注解 | PR4 |
| PR9 | Javadoc 增强（示例/`@since`/`@deprecated`） | PR4 |

每个 PR 独立跑 `buildPlugin`；PR3 起的 diff 报告是合并门槛。

---

## 8. 结论

v5 把“哪些属性支持 SpEL”这件事从**字符串清单**变成**元素上的注解**：

- 属性改名不会静默失效；
- 分隔符、阶段来源、符号名来源也不再是 `separatorAttribute`/`stageAttribute`/`nameAttribute`，而是对应元素上的 `@SpelSeparator`/`@SpelStage`/`@SpelName`；
- 不需要记住两条规则、多组、优先级；
- 审计从“校验清单里的名字对不对”变成“检查注解之间是否一致”。

同时把“这个类是不是 Lucky 组件、是哪一类”从**结构猜测**变成**显式标记**：`@LuckyComponent(type = ...)` 可以标在类上、方法上，也可以作为元注解标在框架注解上（`@HttpRequest`/`@ServerAddressMeta`/`@ApiConfig`/`@SpelConfig`/`@Namespace` 已经标好），插件据此做文件标记、聚合、搜索与调试；它只给 IDEA 插件读，不进入 SpEL 元数据；组件名从 Javadoc 读，读不到用类名。

代价是每个属性多写一行注解。用最简写法（`@SpelAttribute` 不带参数）可以把字符增量压到 10% 以内，而唯一必须显式写的只有“我需要阶段过滤或类型检查”的那些属性。
