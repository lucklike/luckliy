# SpEL 元数据注解设计（v5，逐属性标注 + 组件识别）

> 状态：设计提案（v5，在 v4 基础上新增 `@LuckyComponent` 用于文件级组件识别与类型区分；SpEL 属性模型不变）  
> 适用范围：`lucky-httpclient`、`lucky-httpclient-idea-plugin`  
> 实现现状：**插件不读取任何预编译 JSON 清单，也不存在生成器。** `spel/metadata.json`、`spel/annotations.json`、`spel/builtin-symbols.json` 已删除，注解语义、`@SpelSymbol` 变量、`@LuckyComponent` 组件与默认注册函数均在 IDE 运行期从 PSI/索引解析（见 13.1）。文中出现的“索引/JSON”描述仅作历史方案记录。  
> 核心原则：**能从 Javadoc / Java 签名准确读取的信息，一律不写注解。**
>
> v4 相对 v3 的硬决策（v5 继续沿用）：
> 1. 删除 `@SpelAttributes`：不维护任何属性名字符串白名单；
> 2. 支持 SpEL 的属性**逐条**标 `@SpelAttribute`，注解直接跟着方法/字段走；
> 3. 判定规则从两条降为一条：**标了就是参与，没标就是不参与**。
>
> v5 相对 v4 的增量：
> 1. 新增 `@LuckyComponent`：**只给 IDEA 插件用的组件标记**。可以直接标在类/接口上，也可以作为元注解标在框架注解（`@HttpRequest`、`@ServerAddressMeta`、`@ApiConfig`、`@SpelConfig`、`@Namespace`）上，由使用处继承；
> 2. 插件判定一个类是不是组件，看**类上或类中任一方法上**是否有 `@LuckyComponent`（含元注解链）；它不参与 SpEL 元数据，不继承；用于文件标记、聚合、搜索、调试；
> 3. 组件类型用 `LuckyComponentType` 显式声明（`API`/`CONFIG`/`FUNCTION`/`SUPPORT`），`AUTO` 才走结构推断；
> 4. 组件展示名从 Javadoc 第一段首句读取，读不到用类名，注解不带任何名称属性；
> 5. `@SpelAttribute` 删除 `separatorAttribute`，分隔符改由 `@SpelSeparator` 标记在元素上；
> 6. `@SpelAttribute` 删除 `stageAttribute`、`@SpelSymbol` 删除 `nameAttribute`，阶段来源与符号名来源分别改由 `@SpelStage`、`@SpelName` 标记在元素上；至此不存在任何属性名引用。
>
> 历史沿革：v1（15 注解）→ v2（3 注解 + 字段继承）→ v3（4 注解，类级 + 属性级分离，无继承）→ v4（3 注解，逐属性标注）→ v5（7 注解，逐属性标注 + 组件识别 + 元素标记，无属性名引用）。

---

## 1. 为什么删除类级注解

v3 的 `@SpelAttributes(value = {"url", "path"})` 用字符串维护一份"本注解里哪些属性支持 SpEL"的清单。这份清单与 Java 源码里的元素名是**两套独立数据**：

```java
// v3 写法
@SpelAttributes(value = {"url", "func"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface HttpRequest {
    String url() default "";
    String func() default "";
}
```

问题在于：

| 场景 | v3 的后果 |
|---|---|
| 把 `url` 改名为 `uri` | `value()` 里的 `"url"` 不会自动更新，属性静默失去 SpEL 支持 |
| 新增一个 `String path()` | 必须记得同步往 `value()` 里加 `"path"`，否则静默不生效 |
| 删除一个属性 | `value()` 里留下死名字，只能靠生成器的属性名存在性审计发现 |
| 复制粘贴一个注解做新注解 | 属性名清单跟着复制，容易出现对不上的名字 |

这些都是"两份数据不同步"的典型故障，而且**失败方式是静默的**：不报错，只是插件不再识别。

v4 把元数据直接写在元素上：

```java
// v4 写法
public @interface HttpRequest {

    @SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
    String url() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.REQUEST_INIT, result = String.class)
    String func() default "";

    RequestMethod method() default RequestMethod.NON;
}
```

属性改名时，注解跟着方法一起移动或一起报错，不存在"清单里还留着旧名字"的状态。这是 v4 相对 v3 的核心收益。

### 1.1 代价

| 项目 | v3 | v4 |
|---|---|---|
| 注解数量 | 4 | 3 |
| 类级声明处 | 68 | 0 |
| 属性级声明处 | ~60 | ~137（原类级覆盖的属性全部转为逐条标注） |
| 总声明处 | ~128 | ~137 |
| 每处字符数 | 类级约 90–120，属性级约 60–90 | 约 45–100 |
| 属性改名后的行为 | 静默失配 | 注解散失/编译报错，不会静默 |
| 判定规则 | 2 条 | 1 条 |
| 需要维护的字符串清单 | 68 份 | 0 |

声明处数量增加约 7%，字符总量增加约 20–35%。换来的是**不存在第二份属性名清单**。

**v5 的 `@SpelSeparator`/`@SpelStage`/`@SpelName` 不是类级注解的回潮**：它们没有属性值，也不维护任何属性名清单，只把一个已经存在的元素标记为「这份信息的来源」。被删掉的是「类级预设常量 + 属性名清单」，不是「注解与注解之间有关联」；两者的分界见 6.5.2。

---

## 2. 信息源分层

| 信息 | 来源 | 是否写注解 |
|---|---|---|
| 属性说明、变量说明、函数说明 | Javadoc | 否 |
| 函数入参、返回值、泛型、重载、可变参数 | Java 方法签名 | 否 |
| 注解属性默认值 | 注解元素的 `default` | 否 |
| `@since`、`@deprecated`、`@see`、示例 | Javadoc | 否 |
| 函数别名、命名空间 | `@FunctionAlias`、`@Namespace` | 否 |
| Bean 名称/类型 | Spring `@Component`、`@Bean`、类名 | 否 |
| **某个属性是否支持 SpEL** | `@SpelAttribute` 是否存在 | **是** |
| **值形态（模板/键值/声明/条件/函数/字面量）** | `@SpelAttribute.kind` | **是** |
| **表达式期望结果类型** | `@SpelAttribute.result` / `constraint` | **是** |
| **生命周期阶段** | `@SpelAttribute.stage` / `@SpelStage` | **是** |
| **分隔符、key/value 是否求值** | `@SpelSeparator` / `@SpelAttribute.separator` / `keySpel` / `valueSpel` | **是** |
| **单属性生效条件** | `@SpelAttribute.condition` | **是** |
| **字段/参数是否暴露为 SpEL 符号** | `@SpelSymbol` | **是** |
| **参数注解的哪个元素提供变量名** | `@SpelName` | **是** |
| **配置模型前缀与命名策略** | `@SpelConfig` | **是** |
| **某类是否属于 Lucky 组件体系** | `@LuckyComponent`（类上、方法注解上或元注解链上） | **是** |
| **组件类型（API/配置/函数库/支撑）** | `@LuckyComponent.type`（同上，按优先级取值） | **是** |
| **组件展示名** | Javadoc 第一段首句（读不到用类名） | 否 |

最终是 **7 个注解**：

- `@SpelAttribute`：在注解元素或配置字段上声明单个属性的 SpEL 语义。
- `@SpelSeparator`：标记同注解内的哪个元素是 `KEY_VALUE` 的分隔符。
- `@SpelStage`：标记同注解内的哪个元素提供生效阶段（必须是 `Lifecycle` 类型）。
- `@SpelName`：标记参数注解中哪个元素提供变量名。
- `@SpelSymbol`：声明 SpEL 符号（变量、参数、参数注解、Bean 覆盖）。
- `@SpelConfig`：声明配置模型。
- `@LuckyComponent`：给 IDEA 插件用的组件标记，声明类/接口/方法的组件身份与类型，供插件做文件标记、聚合、搜索与调试；可直接标在类或方法上，也可作为元注解标在框架注解上。

---

## 3. 设计原则

1. **Javadoc 优先。** 说明、参数、返回值、示例、版本、弃用信息全部从 Javadoc 读取；注解不重复。
2. **签名优先。** 函数名、参数类型、返回类型、泛型、重载、可变参数从 Java 签名读取。
3. **默认值不写。** 注解元素的 `default` 直接由 PSI 读取，不在元注解里再写一遍。
4. **注解只写机器语义。** 只声明“是否支持 SpEL、怎么解析、结果是什么、何时生效”这些 Javadoc 无法可靠表达的事实。
5. **元数据跟着元素走。** 不在任何地方维护属性名（含分隔符、阶段来源、符号名来源）的字符串清单；属性改名不得导致 SpEL 支持、分隔符拆分或阶段/名称推导静默失效。
6. **没有继承，也没有优先级。** 每条 `@SpelAttribute` 是自包含的；多声明之间只有「别名指向同一元素」的合流，没有覆盖关系。元素标记（`@SpelSeparator`/`@SpelStage`/`@SpelName`）提供的是**取值来源**，不是声明覆盖。
7. **身份显式声明、元注解可继承。** 组件归属与组件类型都用 `@LuckyComponent` 表达：用户可以直接标在类或方法上，框架也可以把它作为元注解标在 `@HttpRequest` 这类注解上，由使用该注解的类/方法继承；类型按「类上直接标注 > 类上元注解链 > 方法（含方法注解的元注解链）> `AUTO` 推断」取值，推断结果不能反向覆盖声明值。
8. **未知值降级。** 枚举新增值、无法解析的 Javadoc 一律降级为 `UNKNOWN`，插件不崩溃。

---

## 4. 注解总览

六个 SpEL 元数据注解与 `@LuckyComponent` 都放在 `lucky-httpclient` 的 `com.luckyframework.httpclient.proxy.spel.meta` 包；`@LuckyComponent` 同时要作为元注解标在 `proxy.annotations`/`configapi`/`spel` 下的身份注解上，所以与元注解同包，不依赖 SpEL。不新增 Maven 模块；`Lifecycle` 保持在 `com.luckyframework.httpclient.proxy.spel.hook` 包，且不新增任何枚举值（`@SpelSymbol.until` 的“无上界”复用现有 `NON`）。

| 注解 | 目标 | 作用 | 典型写法 |
|---|---|---|---|
| `@SpelAttribute` | `METHOD`、`FIELD` | 声明单个属性的 SpEL 语义 | 每个支持 SpEL 的属性一条 |
| `@SpelSeparator` | `METHOD`、`FIELD` | 标记同注解内的分隔符元素 | `@SpelSeparator String separator() default "=";` |
| `@SpelStage` | `METHOD`、`FIELD` | 标记同注解内提供生效阶段的元素 | `@SpelStage Lifecycle lifecycle() default Lifecycle.NON;` |
| `@SpelName` | `METHOD`、`FIELD` | 标记参数注解中提供变量名的元素 | `@SpelName String name();` |
| `@SpelSymbol` | 字段、参数、注解类型 | 声明一个字段/参数/参数注解暴露为 SpEL 符号 | 内置变量、参数注解、Bean 覆盖 |
| `@SpelConfig` | 类型 | 声明配置模型前缀、别名、命名策略 | 配置 POJO |
| `@LuckyComponent` | 类型、注解类型、方法 | 声明组件身份与类型；可直接标类/接口/方法，也可作为元注解被使用处继承 | `@LuckyComponent(type = LuckyComponentType.API)` |

---

## 5. 枚举

### 5.1 `SpelKind`

| 值 | 含义 |
|---|---|
| `TEMPLATE` | 字符串模板，可包含多个 `#{...}`，结果 String。默认值。 |
| `KEY_VALUE` | 按分隔符拆成 key/value，分别决定是否求值 |
| `DECLARATION` | 框架自定义声明语法；当前 `@SpELImport` 归入 `KEY_VALUE`，`DECLARATION` 保留给其他声明语法 |
| `CONDITION` | 结果必须是布尔 |
| `FUNCTION` | 函数引用：值可以是函数名字面量，或求值结果为函数名的 SpEL 表达式 |
| `LITERAL` | 不解析，仅作为字面量保存 |

没有 `INHERIT`：`@SpelAttribute` 的默认值就是 `TEMPLATE`，不存在“继承类级默认”的第三态。

### 5.2 `Lifecycle`（复用，不扩展）

直接复用 `lucky-httpclient` 现有的运行期枚举 `com.luckyframework.httpclient.proxy.spel.hook.Lifecycle`，不新增任何值：

`CLASS`、`METHOD_META`、`METHOD`、`REQUEST_INIT`、`CONFIG_API_INIT`、`CONFIG_API_METHOD_INIT`、`CONFIG_API_INIT_COMPLETE`、`REQUEST`、`INVOKE_WRAPPER_METHOD`、`RETRY`、`RESPONSE_INIT`、`RESPONSE`、`METHOD_RESULT`、`THROWABLE`、`DESTROY`、`NON`。

**`NON` 的双重语义**：运行期它表示“不执行”，是 `@Hook#lifecycle` 的默认值；在 `@SpelSymbol.until` 里它表示“不设置阶段上界”——`NON` 不对应任何真实阶段，作为上界等价于没有上界。插件按属性区分这两处含义，不需要新增枚举值。

**为什么不需要新增值**：`until` 的默认值必须能在注解里写成编译期枚举常量，现有 `NON` 已经满足；`INHERIT` 没有任何注解需要书写——每条 `@SpelAttribute` 都是自包含的，阶段缺失统一用 `METHOD`（最宽松）表达。

**`UNKNOWN` 留在插件侧**：它是插件 `SpelLifecycle` 的降级值。框架 `Lifecycle` 将来新增常量时，插件把不认识的常量映射为 `UNKNOWN`，用于关闭该属性的未解析检查。框架枚举里没有 `UNKNOWN`，用户也就无法在 `@Hook#lifecycle` 里误写它。

**不需要额外的运行期隔离**：不新增枚举值，就不存在“新值被误注册为 Hook 阶段”的问题；`NON` 是框架已有的合法值，`@Hook#lifecycle = NON` 只表示不注册（不执行）。

### 5.3 `SpelConstraint`

`ANY`、`BOOLEAN`、`NUMBER`、`INTEGER`、`STRING`、`BINARY`、`TYPE`、`CLASS`

用于 `Class<?>` 无法精确表达的粗约束：

- `BINARY`：`byte[]`、`InputStream`、`File` 等二进制形态。
- `TYPE`：`Type`、`Class`、`ResolvableType`、`SerializationTypeToken`、`String` 等类型元数据。
- `NUMBER`/`INTEGER`：数值范围。

### 5.4 `SpelSymbolKind`

`AUTO`、`ROOT`、`VAR`、`BEAN`、`ENV`、`INTERNAL`、`FUNCTION`、`PARAM`

`AUTO` 的推导规则：

| 位置 | 推导 |
|---|---|
| 字段常量 | 常量值以 `$` 开头且以 `$` 结尾 → `INTERNAL`；否则 `VAR` |
| 参数 | → `PARAM` |
| Spring `@Component`/`@Bean` | → `BEAN` |
| 注解类型 | 由 `@SpelSymbol` 的 `kind` 显式指定，默认 `PARAM` |

方法不参与推导：函数不使用 `@SpelSymbol` 标注，`FUNCTION` 只用于描述函数名常量（见 §8.2）。

### 5.5 `SpelNaming`

`AS_IS`、`CAMEL_CASE`、`KEBAB_CASE`、`SNAKE_CASE`

### 5.6 `LuckyComponentType`

`AUTO`、`API`、`CONFIG`、`FUNCTION`、`SUPPORT`

| 值 | 含义 | 框架内的身份注解 / 典型类 |
|---|---|---|
| `AUTO` | 默认值，按结构推断 | 先标组件、后分类时 |
| `API` | HTTP API 接口 | `@HttpRequest`（覆盖 `@Get` 等组合注解与两个 `@Enable*` 注解）、`@ServerAddressMeta`、`@ApiConfig`；或结构上有 `@HttpRequest`/`@Get`/`@Post` 的接口 |
| `CONFIG` | 配置模型 | `@SpelConfig`；或结构上有 `@SpelConfig` 的类 |
| `FUNCTION` | SpEL 函数库 | `@Namespace`；或结构上的 `@Namespace` 类、静态 `@FunctionAlias` 方法所在类 |
| `SUPPORT` | 支撑类 | 没有身份注解，只靠结构推断：`ObjectGenerate`/`ObjectFactory`/`HttpClientProxyObjectFactory`/Hook 处理器/Mock 实现 |

`API`/`CONFIG`/`FUNCTION` 在框架内都有对应的身份注解（`@LuckyComponent` 作为元注解标在它们上面），类或方法上出现这些注解时，插件沿元注解链就能拿到 `type`，不需要用户再标；`SUPPORT` 只有结构推断一条路径。

`OTHER` 不在枚举里：它是插件与索引侧的推断兜底值（推断不出类型时使用），用户不能书写。这与 `UNKNOWN` 留在插件侧的做法一致。插件遇到不认识的未来枚举值时，同样按 `OTHER` 降级，不崩溃。

---

## 6. `@SpelAttribute` 与元素标记注解

```java
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelAttribute {

    SpelKind kind() default SpelKind.TEMPLATE;

    Lifecycle stage() default Lifecycle.METHOD;

    Class<?>[] result() default {};

    String[] typeExpression() default {};

    SpelConstraint[] constraint() default {};

    String separator() default "";

    boolean keySpel() default false;

    boolean valueSpel() default true;

    String condition() default "";
}

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelSeparator {
}

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelStage {
}

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelName {
}
```

| 属性 | 类型 | 默认 | 含义与用途 |
|---|---|---|---|
| `kind` | `SpelKind` | `TEMPLATE` | 值形态。 |
| `stage` | `Lifecycle` | `METHOD` | 生命周期阶段；`METHOD` 表示不参与阶段过滤（最宽松，避免漏报）。**显式书写**时优先于同注解的 `@SpelStage` 元素。 |
| `result` | `Class<?>[]` | `{}` | 结果类型候选，用于 `Class` 字面量能完整表达的类型（原始类型、数组、非参数化类型）；与 `typeExpression` 取并集，任一候选通过即匹配；两者都为空表示不限制类型。 |
| `typeExpression` | `String[]` | `{}` | 结果类型候选，用于 `Class` 字面量无法完整表达的泛型类型，如 `java.util.Collection<java.io.File>`；与 `result` 取并集，不做索引对齐；每项必须是可解析的完整类型名。 |
| `constraint` | `SpelConstraint[]` | `{}` | 粗约束；空数组表示 `ANY`，长度 1 时广播，否则与 `result` 等长。 |
| `separator` | `String` | `""` | 字面量分隔符；非空时优先于同注解的 `@SpelSeparator` 元素。 |
| `keySpel` | `boolean` | `false` | key 是否参与求值。 |
| `valueSpel` | `boolean` | `true` | value 是否参与求值；字面量属性用 `false`。 |
| `condition` | `String` | `""` | 可选的静态生效条件，如 `enable != false`。只支持属性引用、字面量、比较、逻辑、括号。 |

### 6.1 判定规则（只有一条）

**元素上有 `@SpelAttribute` → 参与 SpEL；没有 → 不参与。**

没有白名单、没有 `all`、没有 `disabled`、没有分组。要关闭某个属性，删掉注解即可；要开启，加上注解即可。

以下两种情况需要额外的解析步骤，但都不改变这条判定：

- **`@AliasFor` 双向别名**：同一 effective attribute 只在**声明端**标 `@SpelAttribute`，别名端不标。判定时先解析 effective attribute，再去声明端读取。
- **组合注解**：`@Get#value` 是 `@HttpRequest#url` 的别名时，按 effective attribute 找到 `@HttpRequest#url` 上的声明；`@Get` 自身不重复标注。

### 6.2 最简写法

如果某个属性是普通模板、不需要阶段过滤、不限制结果类型，一行就够：

```java
@SpelAttribute
String description() default "";
```

默认值 `TEMPLATE` + `METHOD` + 不限制类型正好覆盖大多数“可有可无”的场景。只有需要阶段过滤或类型检查时才写参数：

```java
@SpelAttribute(stage = Lifecycle.REQUEST_INIT, result = String.class)
String url() default "";
```

### 6.3 缺失字段的后果

v4 起靠「缺省即降级」保证安全，不靠继承兜底：

| 漏写字段 | 实际效果 | 后果方向 |
|---|---|---|
| `stage` | `METHOD`，不参与阶段过滤 | 少提示「该阶段不可用的变量」，**不误报** |
| `result`/`typeExpression` | 都为空时，不限制类型 | 少检查结果类型，**不误报** |
| `constraint` | 空数组，`ANY` | 同上 |
| `separator` | 空串且无 `@SpelSeparator` 元素，`KEY_VALUE` 无法拆分 | 该属性拆分不生效，**不误报** |
| `@SpelStage`（未标） | 全部 `@SpelAttribute` 用 `METHOD`，不参与阶段过滤 | 少提示，**不误报** |
| `@SpelName`（未标） | 回退 `value` 元素；仍无则无名称 | 少识别，**不误报** |

### 6.4 同一注解内的一致性提示

逐属性标注后，最容易出问题的是「同一个注解里 5 个属性都标了，其中 3 个写了 `stage`、2 个忘了」。没有类级声明作为参照，生成器无法判断这是不是漏写，因此采用启发式：

> 同一注解内多个 `@SpelAttribute`，若其中一个显式写了 `stage`（或 `result`）而其他没写，输出 warning，列出元素名供人工确认。

如果注解内标了 `@SpelStage`，未写 `stage` 的元素会跟随该元素的值，不属于漏写，不计入这条 warning。

这条只做提示，不阻断构建。

### 6.5 元素标记注解：`@SpelSeparator` / `@SpelStage` / `@SpelName`

三个注解都是「无属性的指针」：告诉插件「同注解内哪个元素承载这份信息」，从而把属性名从注解属性里拿出来。共同规则：

1. 都无属性；目标都是 `METHOD`/`FIELD`。
2. 同一注解内每种最多一个；多个是声明错误（插件无法决定用哪个）。
3. 都不参与 SpEL：不进 `spelAnnotations.elements`，只进 `knownAnnotations`；索引用注解级字段记录（`separatorElement`/`stageElement`/`nameElement`）。
4. 查找范围都是「同一注解」（含 `@AliasFor` 声明端），不跨元注解链；组合注解不重复标注。
5. 用户自定义注解同样适用，插件按同一套规则识别，不需要维护任何名字表。

#### 6.5.1 `@SpelSeparator`

`separatorAttribute = "separator"` 与已删除的 `@SpelAttributes#value` 是同一种故障：第二份属性名数据，属性改名时不同步。改成在分隔符元素上标注：

```java
public @interface SpELImport {

    @SpelAttribute(kind = SpelKind.KEY_VALUE, stage = Lifecycle.METHOD, valueSpel = false)
    String[] rootLit() default {};

    @SpelSeparator
    String separator() default "=";
}
```

- 只能标在 `String` 元素上（A12 error）；其他类型不能当分隔符。
- 标了 `@SpelSeparator` 的元素**不需要**再标 `@SpelAttribute`；两者同时标时给 warning（A13），因为分隔符本身通常不求值。
- 分隔符解析顺序：`@SpelAttribute.separator` 字面量非空 → 用它；否则找同注解内 `@SpelSeparator` 元素（含 `@AliasFor` 声明端），用它的值；都没有 → 不拆分（安全降级）。
- 索引：注解级字段 `separatorElement`。

#### 6.5.2 `@SpelStage`

`stageAttribute = "lifecycle"` 是同一种故障。阶段不是常量，而是同注解另一个元素的值：

```java
public @interface Hook {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelStage
    Lifecycle lifecycle() default Lifecycle.NON;
}
```

- 它是**注解级的默认阶段来源**：同注解内所有 `@SpelAttribute` 元素，只要 `stage` 没有被显式书写，都用这个元素的值。`@Callback`/`@Val`/`@Pack` 的 `enable` 通过 `@AliasFor` 指向 `@Hook#enable`，阶段自动跟随 `@Hook#lifecycle`，不需要逐条重复标注。
- 元素类型必须是 `Lifecycle`（A6 error）；多个 → A14 error。
- 与 `@SpelAttribute` 同标会被 A2 拦下（`Lifecycle` 不在 `String`/`String[]`/`Object`/`Object[]` 范围内），不需要额外规则。
- 所在注解内没有任何 `@SpelAttribute` → A18 warning（标了但无人消费）。
- 解析顺序：`stage` 显式书写 → `@SpelStage` 元素值 → `METHOD`。
- 索引：注解级字段 `stageElement`。

运行期依据：`HookGroup` 按 `hookAnn.lifecycle()` 分组，执行到该阶段时才解析 `enable`，所以 `enable` 的 SpEL 阶段就是 `lifecycle` 的值。

**为什么阶段可以做注解级默认，而属性级语义不能**

这是对「为什么删掉类级 `@SpelAttributes`」的必要回答。v3 的 `@SpelAttributes(value = {"url", "func"}, stage = REQUEST_INIT, result = String.class)` 同时做了两件事：

1. 用字符串点名「哪些属性有这个语义」；
2. 给它们预设 `stage`/`result`。

第 1 件是**第二份属性名数据**，属性改名时静默失配；第 2 件与属性级声明构成**层级覆盖**，必须引入优先级规则来判断「哪条生效、哪个字段从哪来」。`@SpelStage` 两件都不做：没有属性值、不点名任何属性，只给一个已存在的元素打标记。所以删掉的是「预设常量 + 覆盖」，不是「注解与注解有关联」。

可以复用的判定标准：

> 语义的取值如果**在运行期由另一个注解实参决定**，允许用元素标记间接引用；如果它是**声明期常量**，必须逐属性写死，不做任何级默认。

| 语义 | 取值来源 | 表达方式 |
|---|---|---|
| `stage` | 运行期由注解实参决定（`HookGroup` 按 `hookAnn.lifecycle()` 分组） | `@SpelStage` 元素标记 |
| `separator` | 运行期由注解实参决定（拆分时读 `separator()` 的返回值） | `@SpelSeparator` 元素标记 |
| 参数变量名 | 运行期由参数注解实参决定 | `@SpelName` 元素标记 |
| `kind`/`result`/`constraint`/`typeExpression`/`keySpel`/`valueSpel`/`condition` | 声明期常量，运行期不随其他元素变化 | 逐属性 `@SpelAttribute` |

`result` 这类字段不能做注解级默认，原因不是「写在类上不好看」，而是一旦允许「注解级预设 + 属性级覆盖」，就回到了 v2/v3 的层级合并：需要优先级、需要区分「显式写了默认值」与「没写」，漏写与故意不写无法区分。而 `stage` 没有这个问题：`stage` 字面量与 `@SpelStage` 是**互斥的两种取值方式**（写常量 or 引用实参），不是「默认值 + 覆盖」；`@SpelStage` 在注解内唯一（A14），也不存在多个动态来源的优先级。

失败代价也不同：白名单漏改是静默失配；`@SpelStage` 漏标是安全降级（全部按 `METHOD`，少提示不误报），另有 A6/A14/A18 三道构建期审计。

适用边界：`@SpelStage` 只适用于「同一注解内所有 SpEL 属性共用一个阶段来源」的注解。若某注解需要不同属性跟随不同来源，只能逐属性写 `stage` 或引入新的元素标记，不能再加第二个 `@SpelStage`（A14 会拦）——这保证「注解级默认」不会退化成「注解级覆盖」。

#### 6.5.3 `@SpelName`

`nameAttribute = "value"` 是同一种故障。参数注解里哪个元素是变量名，不应该写成字符串：

```java
@SpelSymbol(kind = SpelSymbolKind.PARAM)
public @interface MyParam {

    @SpelName
    String name();
}
```

- 只在 `@SpelSymbol` 标在注解类型上（`ANNOTATION_TYPE`）时有意义；元素类型必须是 `String`（A16 error）；多个 → A15 error。
- 所在注解类型没有 `@SpelSymbol` → A17 warning（标了也没人读）。
- 优先级：`@SpelSymbol.name` 显式 `→` `@SpelName` 元素值 `→` 回退到 `value` 元素（保持旧的默认行为）`→` 都没有则无名称，不回退参数名。
- 索引：`symbols.paramAnnotations` 中该注解条目的 `nameElement` 字段。

至此 `@SpelAttribute`、`@SpelSymbol` 中不存在任何属性名引用：`separatorAttribute`、`stageAttribute`、`nameAttribute` 全部删除，对比表中的「字符串属性引用」一栏降为 0；属性改名时注解跟着元素走，不会静默失效。

---

## 7. 多结果类型与函数引用

### 7.1 多结果类型

`result`/`typeExpression`/`constraint` 都是数组，用于一个属性可能返回多种类型的场景。`result` 与 `typeExpression` 是两个并列的候选来源，取并集；`constraint` 只对 `result` 候选做粗约束。

| 类型形态 | 写法 |
|---|---|
| `Class` 字面量能完整表达：原始类型、数组、非参数化类型 | `result`，如 `File.class`、`File[].class` |
| `Class` 字面量无法完整表达：带类型参数的泛型类型 | `typeExpression`，如 `"java.util.Collection<java.io.File>"` |

典型例子是 `@ConvertMetaType#type`：

```java
@SpelAttribute(
    stage = Lifecycle.METHOD,
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

- 候选集合 = `result` 的每一项 ∪ `typeExpression` 的每一项；表达式结果匹配任一候选即通过，补全按候选类型的公共成员提供。
- `result` 写 `Class` 字面量能完整表达的类型：原始类型、数组、非参数化类型。
- `typeExpression` 写 `Class` 字面量无法完整表达的泛型类型，必须使用可解析的完整类型名，例如 `"java.util.Collection<java.io.File>"`；解析失败属于元数据声明错误。
- 两者都为空：不限制类型，也是「类型由方法返回类型自动推导」的写法。
- 同一原始类型不建议同时出现在两处：宽候选（如 `Collection.class`）会让泛型候选（如 `Collection<File>`）失效，审计给 warning（A19）。
- `constraint` 空数组：全部 `ANY`；长度 1：广播；长度等于 `result`：按索引对齐；其他长度：元数据声明错误。
- `result` 为空且 `constraint` 非空时，`constraint` 作为独立约束生效；只关心「是二进制」而不关心具体类时可以写 `constraint=BINARY`（`@Binary#file` 最终选择了在 `result` 中列候选类型，见 §11.4）。
- 源码 `@ConvertMetaType#type` 只用了 `result`，`constraint` 是预留的粗约束能力，尚未在真实注解中使用。
- 以上并集与对齐规则只影响静态检查，不改变运行时类型转换；运行时仍由框架的 `toType`/转换器处理。

### 7.2 函数引用（`kind=FUNCTION`）

`kind=FUNCTION` 表示属性值不是普通模板，而是“函数引用”。典型例子是 `@ServerAddress#urlFunc`/`pathFunc`：

```java
public @interface ServerAddress {

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String url() default "";

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String path() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String urlFunc() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
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
- 函数返回值必须兼容 `@SpelAttribute.result`（这里是 String）；
- 参数带 `@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 时按表达式注入，插件检查表达式结果与参数类型兼容；
- 参数不带注入注解时由 `Context#getMethodParamObject` 按类型注入：`MethodContext`、`MethodMetaContext`、`ClassContext`、`Method`、`Class`、当前接口实例、`Request`、`Response`，以及 `Throwable`/`HttpClientProxyObjectFactory`/`TaskResult`/`ApiDescribe`/`AsyncTaskExecutor` 的子类；再回退 `parameterInstance` 扩展函数，仍取不到时该参数为 null；
- 只有既无注入注解、类型又不在按类型注入范围时才提示——例如 `@ChunkUpload.ChunkUploadFunction.upload(MethodContext context)` 是无注解的合法写法；
- `url` 和 `urlFunc` 同时配置时，提示 `url` 优先；
- 函数名是字面量时提供跳转/悬停；是动态表达式时只做语法/类型检查。

函数引用不新增注解：函数名、命名空间、参数、返回值全部从现有注解和 Java 签名读取。

同类属性还有 `@HttpRequest#func`、`@Destroy#func`、`@Wrapper#fun`、`@Get`/`@Post` 等 9 个组合注解的 `func`（`@HttpRequest#func` 的别名）、`@ServerAddress#urlFunc`/`pathFunc`、`@ConvertMetaType#typeFunc`/`respContentTypeFunc`、`@Retryable#conditionFunc`、`@ExceptionHandle#handleFunc`、`@ExceptionHandleMeta#conditionFunc`、`@ExceptionReturn#handleFunc`/`conditionFunc`（别名）、`@ExceptionFallback#conditionFunc`（别名）、`@RespConvert#resultFunc`/`metaTypeFunc`/`respContentTypeFunc`（后两个为别名）、`@ResultConvertMeta#metaTypeFunc`/`respContentTypeFunc`（别名）、`@HttpExec#execFunc`、`@Mock#enableFunc`（别名）/`mockFunc`、`@MockMeta#enableFunc`、`@MockPlugin#implFunc`。源码共 33 个属性名，其中 18 个是 `@AliasFor` 别名、15 个是声明端。

**15 个声明端已全部标 `kind=FUNCTION`**：

| 声明端 | `result` | `stage` |
|---|---|---|
| `@HttpRequest#func` | `String` | `METHOD` |
| `@Destroy#func` | 空（执行一段逻辑，不约束返回值） | `DESTROY` |
| `@Wrapper#fun` | 空（结果类型由所在方法返回类型推导） | 默认 `METHOD` |
| `@ServerAddress#urlFunc` | `String` | `METHOD` |
| `@ServerAddress#pathFunc` | `String` | `METHOD` |
| `@HttpExec#execFunc` | `HttpExecutor` | 默认 `METHOD` |
| `@ConvertMetaType#typeFunc` | `{Type, Class, ResolvableType, SerializationTypeToken, String}` | `METHOD` |
| `@ConvertMetaType#respContentTypeFunc` | `String` | `METHOD` |
| `@Retryable#conditionFunc` | `boolean` | `RETRY` |
| `@ExceptionHandle#handleFunc` | 空（返回值即方法返回值，动态） | `THROWABLE` |
| `@ExceptionHandleMeta#conditionFunc` | `boolean` | `THROWABLE` |
| `@RespConvert#resultFunc` | 空（结果类型由方法返回类型推导） | `RESPONSE` |
| `@Mock#mockFunc` | `Response` | 默认 `METHOD` |
| `@MockMeta#enableFunc` | `boolean` | 默认 `METHOD` |
| `@MockPlugin#implFunc` | `Object` | 默认 `METHOD` |

18 个别名端不标注（9 个 HTTP 方法注解的 `func`、`@ExceptionReturn#handleFunc`/`conditionFunc`、`@ExceptionFallback#conditionFunc`、`@RespConvert#metaTypeFunc`/`respContentTypeFunc`、`@ResultConvertMeta#metaTypeFunc`/`respContentTypeFunc`、`@Mock#enableFunc` 等），插件按 effective attribute 解析到上表声明端。

`result` 为空的函数引用表示返回值类型由使用位置推导，插件不做返回值静态约束；函数引用不使用 `constraint`。**`stage` 取值以源码为准**：默认值 `METHOD` 表示不做阶段过滤；§11 的示例已按源码实际标注校准，§7.1 的 `constraint` 只是规则演示（源码 `@ConvertMetaType#type` 未使用 `constraint`）。

`annotations.json` 只收录其中 12 个属性名（9 个声明端），且都没有标成 `FUNCTION`，仅作回归基线，迁移时以源码为准。

---

## 8. `@SpelSymbol`

> **本注解不声明函数。** 函数是否可用及其生命周期只由两条路径决定：`@SpELImport`（注解上的类导入）与 `HttpClientProxyObjectFactory#addSpringElFunctionClass(Class<?>)`（工厂级函数注册）。函数方法不标注 `@SpelSymbol`，识别细节见 §8.2。

```java
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelSymbol {

    String name() default "";

    SpelSymbolKind kind() default SpelSymbolKind.AUTO;

    Class<?> type() default Object.class;

    String typeExpression() default "";

    Lifecycle from() default Lifecycle.METHOD;

    Lifecycle until() default Lifecycle.NON;
}
```

| 属性 | 类型 | 默认 | 含义与用途 |
|---|---|---|---|
| `name` | `String` | `""` | 符号名。空串自动推导：字段常量值、参数名、注解属性值。 |
| `kind` | `SpelSymbolKind` | `AUTO` | 符号类型；`AUTO` 按位置推导。 |
| `type` | `Class<?>` | `Object.class` | 变量类型。`Object.class` 是哨兵值，表示从字段类型/参数类型读取；符号真实类型就是 `Object` 时改用 `typeExpression`。 |
| `typeExpression` | `String` | `""` | 泛型类型覆盖。 |
| `from` | `Lifecycle` | `METHOD` | 从哪个阶段可见。来源优先级见下，不是继承。 |
| `until` | `Lifecycle` | `NON` | 到哪个阶段为止；`NON` 表示不设置上界。 |

**`from` 的取值优先级**（线性三档，不是继承）：

1. 注解上显式写的 `from = ...`；
2. 否则读 Javadoc `@spelStage`；
3. 否则用注解默认值 `METHOD`（最宽松，不做阶段过滤）。

生成器通过 PSI 能区分「显式书写」与「使用默认值」，这三级可以精确实现。

### 8.1 自动推导规则

**变量名**：

- 字段常量：读取常量值。例如 `$_RESPONSE_STATUS_$ = "$status$"` → `$status$`。
- 参数：参数名；内置 `@Param` 由插件按全限定名白名单硬编码识别 `@Param#value`，不需要标注（白名单见 8.3）。
- 注解类型：仅用户自定义参数注解；`@SpelName` 指定元素的值（无 `@SpelName` 时回退 `value` 元素）。

**变量类型**：

- 字段：字段类型。若字段是 `String` 常量但代表其他类型，显式写 `type=Integer.class`。
- 参数：参数类型。
- 注解类型：参数类型，或 `typeExpression`。

**说明、参数说明、返回值、示例、版本、弃用**：全部从 Javadoc 读取，不写注解。

### 8.2 示例

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

**函数（不使用 `@SpelSymbol`）**：

函数不在 `@SpelSymbol` 上声明，插件只按下面两条路径收集函数，并据此判定函数的可用范围与生命周期：

| 路径 | 覆盖范围 | 生效时机 |
|---|---|---|
| `@SpELImport#value` / `@SpELImport#ifs` | 类上标注 → 该类及其继承链导入的函数类；方法上标注 → 该方法的函数类 | 类上：`CLASS` 阶段（`ClassContext#initContext` 调用 `useHook(CLASS)` 之前）；方法上：`METHOD_META` 阶段（`MethodMetaContext#initContext` 调用 `useHook(METHOD_META)` 之前） |
| `HttpClientProxyObjectFactory#addSpringElFunctionClass(Class<?>)`（另有 `(String namespace, Class<?>)`、`(ClassStaticElement)` 重载，以及单个方法的 `addSpringElFunction`) | 工厂级全局函数；框架构造函数中注册了 `CommonFunctions`、`SerializationFunctions`、`ResourceFunctions`、`RandomFunctions`、`DigestFunctions`、`MacFunctions`、`CipherFunctions` 七个类 | 工厂构造即注册，早于任何上下文，从 `CLASS` 阶段起可用 |

函数方法自身的名称、命名空间、参数、返回值、说明仍按 Java 签名与 Javadoc 读取：名称取 `@FunctionAlias` 或方法名，命名空间取 `@Namespace` 或 `addSpringElFunctionClass` 的 `namespace` 参数，被 `@FunctionFilter` 标注的方法不参与；同一类中同名方法必须用 `@FunctionAlias` 区分，否则注册时抛异常。`SpelSymbolKind.FUNCTION` 只用于描述函数名常量（例如 `__$PARAMETER_INSTANCE_FUNCTION$__`），不用于方法。

**内置参数注解 `@Param`**：

`@Param` 在 `lucky-core` 中，`@SpelSymbol` 在 `lucky-httpclient` 中，core 不能反向依赖 httpclient，因此 `@Param` 不标注，由插件内置识别：

- 注解全限定名：`com.luckyframework.reflect.Param`；
- 变量名：`value()` 必填；为空时框架抛 `@Param annotation has no value.`，不会回退参数名；
- 变量类型：参数类型；
- 生成 `name`/`$name`（最终值/原始值）、`$index`/`$$index`（按位置的最终值/原始值）、`name::type`/`$index::type`（参数类型）、`$pc$name`/`$pc$index`（`ParameterContext`）等别名。

**用户自定义参数注解**：

```java
@SpelSymbol(kind = SpelSymbolKind.PARAM)
public @interface MyParam {

    @SpelName
    String name();
}
```

**Bean**：插件从 Spring 注解读取 Bean 名和类型，不需要 `@SpelSymbol`。只有需要覆盖名称/阶段时才加：

```java
@SpelSymbol(name = "users", from = Lifecycle.REQUEST_INIT)
@Component("userService")
public class UserService { ... }
```

### 8.3 参数注入符号（`@Param` / `@Var` 系列）

`@Param`、`@Var`、`@Rar`、`@Env`、`@Bean` 是框架已有的参数注入注解，不需要再标 `@SpelSymbol`。

**识别必须按全限定名，不能按简单名。** 框架里存在两个 `@Param`：

| 全限定名 | 是否参与 SpEL | 处理 |
|---|---|---|
| `com.luckyframework.reflect.Param` | 是 | 按下面的内置规则识别 |
| `com.luckyframework.httpclient.generalapi.ai.tool.Param` | 否（AI 工具调用的参数描述） | 硬编码排除：不生成变量、不补全、不检查 |

按简单名 `Param` 匹配会把 AI 工具注解误当成 SpEL 注入点，产生错误补全和误报。白名单固定为：`com.luckyframework.reflect.Param`、`com.luckyframework.httpclient.proxy.spel.Var`、`com.luckyframework.httpclient.proxy.spel.Rar`、`com.luckyframework.httpclient.proxy.spel.Env`、`com.luckyframework.httpclient.proxy.spel.Bean`。

内置识别规则：

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

### 8.4 Hook 机制

`@Hook` 是生命周期钩子，插件需要把 `lifecycle` 作为阶段来源，把 `enable` 作为条件表达式，把回调方法参数作为注入点：

```java
public @interface Hook {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelStage
    Lifecycle lifecycle() default Lifecycle.NON;

    // hookHandle/hookHandleClass/order/errorInterrupt 不支持 SpEL
}
```

规则：

- `enable` 的阶段由 `@SpelStage` 标注的 `lifecycle` 元素提供；
- `lifecycle` 不能是插件侧的 `UNKNOWN`（不认识的枚举值降级，不报错）；`NON` 表示不执行，插件按无害处理；
- `@Callback`/`@Val`/`@Pack` 的 `enable`/`lifecycle` 通过 `@AliasFor` 指向 `@Hook`，插件按 effective attribute 解析；
- `@Callback` 方法参数支持 `@Param`/`@Var` 系列自动注入，也支持不带注解的按类型注入（见 8.3）；
- `@Val` 是静态字段，但字段值不是普通字面量：`String` 字段会由 `VarHookHandler` 当作 SpEL 表达式解析，结果按 `name`（空时用字段名）存入变量，阶段跟随 `lifecycle`；插件要对字段值做 SpEL 高亮/补全/结果类型检查；
- `@Pack` 是静态字段，值是包名或类（支持 `String`/`String[]`/`Collection<String>`、`Class`/`Class[]`/`Collection<Class>`），不解析 SpEL；
- `@Callback`/`@Val`/`@Pack` 的 `lifecycle` 没有默认值，必须显式指定；`@Callback#storeName` 只有 `storeOrNot=true` 时才生效，且 `storeName`/`@Val#name` 在 `literal=false` 时本身也按 SpEL 解析；
- Hook 方法内补全的变量集合由 `lifecycle` 决定，例如 `REQUEST` 阶段提供请求变量，`RESPONSE` 阶段提供响应变量；
- Hook 方法/字段的 Javadoc 说明直接读取。

`@Hook` 的 `lifecycle` 是 `Lifecycle` 枚举，不是字符串；插件从注解值读取，不需要额外元数据。

---

## 9. `@SpelConfig`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelConfig {

    String prefix();

    String[] aliases() default {};

    SpelNaming naming() default SpelNaming.AS_IS;
}
```

| 属性 | 类型 | 默认 | 含义与用途 |
|---|---|---|---|
| `prefix` | `String` | 必填 | 配置前缀。 |
| `aliases` | `String[]` | `{}` | 其他可用前缀。 |
| `naming` | `SpelNaming` | `AS_IS` | 字段名 → key 的命名策略。 |

**没有 `stage`、没有 `valueSpel`**：

- 没有 `valueSpel`：字段不支持 SpEL 就不标 `@SpelAttribute`，不存在「整个配置模型默认识别」的隐式规则；
- 没有 `stage`：`@SpelConfig` 不提供字段默认阶段，需要阶段的字段显式写 `@SpelAttribute(stage = ...)`。

配置字段写法：

```java
@SpelConfig(prefix = "lucky.api", naming = SpelNaming.KEBAB_CASE)
public class RetryConf {

    @SpelAttribute(stage = Lifecycle.CONFIG_API_INIT,
                   kind = SpelKind.CONDITION, result = boolean.class)
    private String expression;

    @SpelAttribute(stage = Lifecycle.CONFIG_API_INIT,
                   kind = SpelKind.FUNCTION, result = boolean.class)
    private String funcName;

    private Integer maxCount;

    // 不支持 SpEL 的字段不标
    private Boolean enable;
}
```

- key 从字段名 + `naming` 生成：`maxCount` → `max-count`；
- `expression` 是条件表达式，`funcName` 是函数引用；
- 字段说明从 Javadoc 读取，字段类型从 Java 类型读取；
- 显式 key 可以在 Javadoc 写 `@key retry-count`，或后续再加 `key` 属性；先保持最简。

---

## 10. `@LuckyComponent`（IDEA 插件专用组件标记）

```java
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuckyComponent {

    LuckyComponentType type() default LuckyComponentType.AUTO;
}
```

| 属性 | 类型 | 默认 | 含义与用途 |
|---|---|---|---|
| `type` | `LuckyComponentType` | `AUTO` | 组件类型，直接决定图标与颜色；`AUTO` 表示按结构推断。 |

包位置：`com.luckyframework.httpclient.proxy.spel.meta`（`@LuckyComponent`、`LuckyComponentType`、`@SpelConfig` 同包）；它作为元注解出现在 `proxy.annotations`/`configapi`/`spel` 下的身份注解上时按需 import，与 SpEL 元数据无关。

**这个注解只服务 IDEA 插件**：框架运行期从不读取它，删掉它不改变任何运行期行为。它的唯一职责是让插件知道「这个文件是 lucky-httpclient 框架的组件、属于哪一类」，从而支撑标记、聚合、搜索、调试四类能力。它不参与 SpEL 元数据：不是 `@SpelAttribute` 的宿主，不进 `spelAnnotations`，不能写 `stage`/`result`/`kind`。

### 10.1 它回答什么问题

> 这个类是不是用 lucky-httpclient 写的组件？是哪一类组件？

插件用它做五件事：

1. **标记**：按 `type` 给不同的四叶草图标与颜色（沿用 `iconProvider` + `fileIconProvider` 双 EP），Project 视图与编辑器标签保持一致；
2. **聚合**：一次遍历按 `type` 分组列出项目里的 Lucky 组件，生成组件清单；
3. **搜索**：按组件类型、展示名、标记来源定位文件，不必逐个打开看结构；
4. **调试**：从组件身份直接跳到相关 SpEL 元数据（`@SpelAttribute` 属性、`@SpelConfig` 字段、函数符号），解释「这个类为什么有/没有补全」；
5. **激活判定**：项目里出现 `@LuckyComponent`（含元注解链）时插件可以稳定激活，不依赖「主源集恰好有 `@HttpRequest`」这种脆弱条件。

### 10.2 插件怎么识别一个类是不是组件

判定入口有三个，任意一个命中即为组件：

```text
1. 类/接口上直接标了 @LuckyComponent                     → 组件，source = class
2. 类/接口上某个注解的元注解链上有 @LuckyComponent       → 组件，source = meta
3. 类中任一方法上直接标了 @LuckyComponent，或该方法上某个
   注解的元注解链上有 @LuckyComponent                     → 组件，source = method
```

`source = method` 时，命中的方法同时被记录为**组件方法**（索引字段 `methods`）；类上没有任何标记、只有某个方法命中时，整个类仍按方法上的 `type` 成为组件。

**类型判定优先级**（从高到低）：

```text
1. 类/接口上直接标注且非 AUTO → 用该值（source = class）
2. 类上注解的元注解链且非 AUTO → 取最近一层（source = meta）
3. 方法上（含方法注解的元注解链）且非 AUTO → 取最近一层（source = method）
4. 命中的值是 AUTO，或完全未标注 → 结构推断（source = infer）
5. 结构推断不出：有标记 → OTHER；无标记 → 不是组件
```

同一个类命中多个不同的具体 `type`（无论来自类上直接标注、元注解链还是方法）时，取优先级最高的值，并输出 A9 warning 列出全部候选。

结构推断表：

| 类结构 | 推断结果 |
|---|---|
| 接口上有 `@HttpRequest`/`@Get`/`@Post` 等 | `API` |
| 类上有 `@SpelConfig` | `CONFIG` |
| 类上有 `@Namespace`，或有静态 `@FunctionAlias` 方法 | `FUNCTION` |
| 实现 `ObjectGenerate`/`ObjectFactory`/`HttpClientProxyObjectFactory`/Hook 处理器/Mock 实现 | `SUPPORT` |
| 以上都不满足 | `OTHER`（有标记时）/ 不是组件（无标记时） |

为什么还保留 `AUTO`：显式类型是给「用户知道自己要什么」的场景；`AUTO` 服务两类情况——先标组件后补类型，以及第三方/生成代码无法改注解时。`AUTO` 推断为 `OTHER` 时审计报 A10，提示补类型。

推断覆盖不到的地方，正是显式类型的价值：

- **没有任何 Lucky 注解、但属于组件体系的类**：被 `@SpelConfig` 嵌套引用的 POJO、被 `@SpelSymbol` 引用的常量/枚举库、自定义异常、DTO；
- **推断会误判**：普通工具类只是 import 了 `@Param`，不等于它是组件；
- **推断不稳定**：框架新增组合注解时插件要跟着改推断规则，而 `type` 是显式契约，不用改。

所以两者分工固定为：

> **显式类型优先；`AUTO` 或未标注时才用结构推断；推断不出且未标注 → 不是组件。**

### 10.3 元注解继承与框架内的标注点

- `@Target({TYPE, ANNOTATION_TYPE, METHOD})`：可直接标在类/接口/枚举上，可标在类的方法上，也可标在注解声明上作为元注解；
- **元注解可继承**：标在注解声明上的 `@LuckyComponent` 会随注解的使用传给使用处的类与方法。例：`@HttpRequest` 标 `API` 后，所有带 `@Get`/`@Post`/… 的接口、以及这些接口内带 `@Get`/`@Post`/… 的方法都是 `API` 组件；
- **不继承**：不加 `@Inherited`，子类不会因为父类标了就成为组件；
- **不参与 Spring 容器**：它不是 `@Component` 的替代，不触发 Bean 注册；框架运行期也不读取它。

框架已在 5 个身份注解上标注：

| 身份注解 | 所在包 | `type` | 经元注解链继承的注解 |
|---|---|---|---|
| `@HttpRequest` | `proxy.annotations` | `API` | `@Get`/`@Post`/`@Put`/`@Delete`/`@Patch`/`@Head`/`@Options`/`@Trace`/`@Connect`、`@EnableConfigurationParser`、`@EnableResourceParser` |
| `@ServerAddressMeta` | `proxy.annotations` | `API` | `@ServerAddress` |
| `@ApiConfig` | `proxy.configapi` | `API` | 配置 API 接口 |
| `@SpelConfig` | `proxy.spel.meta` | `CONFIG` | 配置模型类 |
| `@Namespace` | `proxy.spel` | `FUNCTION` | 函数库类 |

`SUPPORT` 没有身份注解：支撑类的共同点只是实现了某个接口，用注解表达会引入大量无意义标注，因此保持结构推断。用户自定义组件仍可直接标 `@LuckyComponent(type = ...)`。

### 10.4 展示名与图标

**展示名**由 Javadoc 第一段首句决定，注解不再带 `value`/`name`：

```java
/**
 * 用户中心 API
 */
@LuckyComponent(type = LuckyComponentType.API)
public interface UserApi { ... }
```

- 取 Javadoc 第一段第一句（截止到 `。`、`.` 或换行），去行首 `*` 与空白；
- 为空（无注释或只有 tag）→ 用简单类名；
- 用于图标 tooltip、文档生成、组件清单；
- 不参与路由，不参与 SpEL 变量名。

**图标映射**：

| `type` | 图标资源 | 基色 | 徽标 |
|---|---|---|---|
| `API` | `icons/luckyApi.svg` | 绿 `#3FA34D` | `</>` |
| `CONFIG` | `icons/luckyConfig.svg` | 蓝 `#2F6FEB` | 齿轮 |
| `FUNCTION` | `icons/luckyFunction.svg` | 橙 `#E08A1E` | `f(x)` |
| `SUPPORT` | `icons/luckySupport.svg` | 紫 `#8A5CF6` | 扳手 |
| `OTHER`（推断兜底） | `icons/luckyComponent.svg` | 灰 `#8C8C8C` | 无 |

`AUTO` 不单独占一行：解析成具体类型后用对应图标，解析为 `OTHER` 时用灰色通用图标。

约束：

- 统一 16×16：四叶草轮廓相同，只换徽标与基色，保证同类功能图标视觉一致；
- Project 视图与编辑器标签用同一张图（双 EP 共享映射，避免两处颜色不一致）；
- 资源缺失时降级为 `luckyComponent.svg`，不抛异常；
- 暗色主题优先用 `xxx_dark.svg`，未提供时复用同一张。

### 10.5 索引与解析

索引新增一段：

```json
"components": [
  {
    "class": "com.example.UserApi",
    "title": "用户中心 API",
    "type": "API",
    "declared": "API",
    "source": "meta",
    "methods": []
  },
  {
    "class": "com.example.ApiResult",
    "title": "ApiResult",
    "type": "OTHER",
    "source": "infer"
  },
  {
    "class": "com.example.OrderApi",
    "title": "订单 API",
    "type": "API",
    "declared": "API",
    "source": "method",
    "methods": ["createOrder", "queryOrder"]
  }
]
```

| 字段 | 含义 |
|---|---|
| `class` | 全限定名 |
| `title` | Javadoc 首句；读不到用简单类名 |
| `type` | 最终类型 `API`/`CONFIG`/`FUNCTION`/`SUPPORT`/`OTHER`；`AUTO` 已解析成具体值或 `OTHER` |
| `declared` | 命中的原始声明值（`AUTO`/`API`/`CONFIG`/`FUNCTION`/`SUPPORT`）；省略表示未标注、类型完全来自推断 |
| `source` | 标记来源：`class`（类上直接标注）/`meta`（类上注解的元注解链）/`method`（方法上直接标注或方法注解的元注解链）/`infer`（纯结构推断） |
| `methods` | 组件方法名列表；`source = method` 时记录命中的方法，其余情况可为空 |

用户项目里的类不在框架索引里，按需用 PSI 解析，规则与生成器共用一份 `ComponentDetector` + `ComponentTypeResolver`。

`@LuckyComponent` 必须进 `knownAnnotations`（属于框架注解），但不进 `spelAnnotations`（不声明任何 SpEL 属性）。

### 10.6 与 SpEL 注解的关系

| 组合 | 结果 |
|---|---|
| 只有 `@SpelConfig` | 元注解链命中 `CONFIG`（`source = meta`），不需再标 `@LuckyComponent` |
| `@LuckyComponent(type = CONFIG)` + `@SpelConfig` | 合法；类上直接标注优先（`source = class`），配置语义不变 |
| `@LuckyComponent(type = SUPPORT)` 的普通 POJO | 合法；有图标，但没有 SpEL 元数据 |
| 方法上标 `@LuckyComponent` 而类上未标 | 合法；类被识别为组件（`source = method`），方法记入 `methods` |
| `@LuckyComponent` 标在注解元素（注解属性）上 | 不构成组件身份；插件忽略并提示（`METHOD` 目标是为组件方法准备的，不是为注解元素） |

---

## 11. 真实注解改造示例

### 11.1 `@HttpRequest`

```java
public @interface HttpRequest {

    @AliasFor("url")
    String value() default "";

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    @AliasFor("value")
    String url() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String func() default "";

    RequestMethod method() default RequestMethod.NON;
}
```

`value` 是 `url` 的别名，只在声明端 `url` 上标注；插件按 effective attribute 解析到 `url` 的声明。`func` 是函数引用。`method` 是枚举，不标。`stage` 显式写 `METHOD`（也是默认值，最宽松、不做阶段过滤），没有类级默认可继承；`func` 的结果类型由 `result = String.class` 约束。

### 11.2 `@Retryable`

```java
// 声明端：属性级都写在 @RetryMeta 上
public @interface RetryMeta {

    @SpelAttribute(result = String.class, stage = Lifecycle.RETRY)
    String name() default "[#{T(Thread).currentThread().getName()}][#{$req$.getUniqueId()}][#{$api$.name}]";

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.RETRY)
    String enable() default "true";

    @SpelAttribute(result = int.class, stage = Lifecycle.RETRY)
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
    String name() default "...";

    @AliasFor(annotation = RetryMeta.class, attribute = "retryCount")
    String retryCount() default "3";

    @SpelAttribute(result = long.class)
    String waitMillis() default "1000";

    @SpelAttribute(result = long.class)
    String maxWaitMillis() default "10000";

    @SpelAttribute(result = long.class)
    String minWaitMillis() default "500";

    @SpelAttribute(result = double.class)
    String multiplier() default "0";

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.RETRY)
    String condition() default "#{($status$ >= 500 and $status$ < 600) or {408, 429}.contains($status$)}";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = boolean.class, stage = Lifecycle.RETRY)
    String conditionFunc() default "";
}
```

`enable`/`name`/`retryCount` 通过 `@AliasFor` 指向 `@RetryMeta`，属性级声明必须写在声明端才生效，`@Retryable` 上只保留别名。`retryCount`/`waitMillis`/`maxWaitMillis`/`minWaitMillis`/`multiplier` 在框架中分别按 `int`/`long`/`long`/`long`/`double` 解析（`MethodContext#getRetryActuator`、`BackoffWaitingBeforeRetryContext#doBeforeRetry`），`result` 用基本类型 `int.class`/`long.class`/`double.class`；`waitMillis`/`maxWaitMillis`/`minWaitMillis`/`multiplier` 源码没有显式写 `stage`，按默认 `METHOD` 处理，其余五个属性显式标了 `RETRY`；`conditionFunc` 是函数引用，函数返回值必须是 boolean。

### 11.3 `@ConvertMetaType`

```java
public @interface ConvertMetaType {

    Class<?> value() default Object.class;

    @SpelAttribute(result = {Type.class, Class.class, ResolvableType.class,
                             SerializationTypeToken.class, String.class},
                   stage = Lifecycle.METHOD)
    String type() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION,
                   result = {Type.class, Class.class, ResolvableType.class,
                             SerializationTypeToken.class, String.class},
                   stage = Lifecycle.METHOD)
    String typeFunc() default "";

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String respContentType() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String respContentTypeFunc() default "";
}
```

`type` 的 5 种候选类型见 7.1；`typeFunc` 的函数返回值同样必须是这 5 种类型之一，框架在 `Context#toType` 里统一转换；`respContentTypeFunc` 的返回值固定为 `String`。示例中的 `Type`/`ResolvableType`/`SerializationTypeToken` 需 import 对应类型。源码没有使用 `constraint`（§7.1 的 `constraint` 只是规则演示），候选类型完全由 `result` 表达；四个属性的 `stage` 都是 `METHOD`（默认值，不做阶段过滤）。现有 JSON 把 `typeFunc` 标成了 `NONE`、把 `respContentTypeFunc` 误标为 `TEMPLATE` 且 lifecycle 为 `UNKNOWN`，迁移时一并修正为 `FUNCTION`。

### 11.4 `@Binary`

```java
public @interface Binary {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.REQUEST_INIT)
    String condition() default "";

    @SpelAttribute(result = String.class)
    String name();

    @SpelAttribute(result = {byte[].class, Byte[].class, ByteBuffer.class, Reader.class,
            InputStream.class, File.class, InputStreamSource.class})
    String file();

    @SpelAttribute(result = String.class)
    String filename();

    @SpelAttribute(result = String.class)
    String contentType() default "";
}
```

只有 `condition` 显式标了 `REQUEST_INIT`（用于决定是否注入该表单项），其余四个属性用默认 `METHOD`。`file` 没有用 `constraint`，而是把 `byte[]`/`Byte[]`/`ByteBuffer`/`Reader`/`InputStream`/`File`/`InputStreamSource` 七种候选写进 `result`，运行期再由 `HttpExecutor#isBinaryParam` 判定。

### 11.5 `@ChunkUpload`

```java
public @interface ChunkUpload {

    @AliasFor("file")
    String value() default "";

    @SpelAttribute(result = Object.class)
    @AliasFor("value")
    String file() default "";

    @SpelAttribute(result = String.class)
    String fileId() default "";

    @SpelAttribute(result = String.class)
    String fileName() default "";

    @SpelAttribute(result = long.class)
    String chunkSize() default "";

    @SpelAttribute(result = int.class)
    String maxConcurrency() default "";

    @SpelAttribute(result = int.class)
    String maxRetryCount() default "";

    @SpelAttribute(result = long.class)
    String retryBackoffMillis() default "";
}
```

这 4 个数值属性因为 `parseLongExpression`/`parseIntExpression` 间接求值而容易漏检，`result` 用小写基本类型 `long`/`int`；`file` 的注解 `result` 是 `Object`，运行期由 `ChunkUploadFunction#resolveFile` 判定必须是 `File` 或路径字符串；`value` 是 `file` 的 `@AliasFor` 别名，只在 `file` 上标注。七个属性都没有显式写 `stage`，按默认 `METHOD` 处理。

### 11.6 `@SpELImport`

```java
public @interface SpELImport {

    @SpelAttribute(kind = SpelKind.KEY_VALUE, keySpel = true)
    String[] root() default {};

    @SpelAttribute(kind = SpelKind.KEY_VALUE, keySpel = true)
    String[] var() default {};

    @SpelAttribute(kind = SpelKind.KEY_VALUE, valueSpel = false)
    String[] rootLit() default {};

    @SpelAttribute(kind = SpelKind.KEY_VALUE, valueSpel = false)
    String[] varLit() default {};

    @SpelSeparator
    String separator() default "=";
}
```

`root`/`var` 的 key 与 value 都参与求值（`keySpel=true`）；`rootLit`/`varLit` 的 value 不参与求值，仅作为字面量，用 `valueSpel=false` 表达；四个属性都没有显式写 `stage`，按默认 `METHOD` 处理；`value`/`ifs` 是 Class 导入，`pack` 是包名列表，都不标注；分隔符来自 `separator` 元素上的 `@SpelSeparator`，把 `separator` 改名为 `delimiter` 时注解跟着方法走，不会断。

### 11.7 `@ServerAddress`

```java
public @interface ServerAddress {

    @AliasFor("url")
    String value() default "";

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    @AliasFor("value")
    String url() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String urlFunc() default "";

    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String path() default "";

    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String pathFunc() default "";
}
```

- `value`/`url`、`path` 是模板；
- `urlFunc`/`pathFunc` 是函数引用；
- 函数示例：

```java
@FunctionAlias("getUrl")
public static String getUrl(@Var("baseUrl") String baseUrl) { ... }
```

插件检查：函数必须存在、返回值兼容 `String`、参数 `baseUrl` 可自动注入（`@Var(NORMAL)` → `#{#baseUrl}`）。

### 11.8 `@Hook`

```java
public @interface Hook {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelStage
    Lifecycle lifecycle() default Lifecycle.NON;

    // hookHandle/hookHandleClass/order/errorInterrupt 不支持 SpEL
}
```

`enable` 的阶段跟随 `lifecycle`，插件在对应阶段提供变量补全；`lifecycle` 不是 SpEL；`NON` 表示不执行，插件侧不认识的枚举值降级为 `UNKNOWN`。

### 11.9 `@Callback` 参数注入

```java
@Callback(lifecycle = Lifecycle.REQUEST_INIT,
          storeOrNot = true, storeName = "authToken")
public static String loadToken(@Env("auth.token") String token) { ... }
```

`@Callback` 的 `enable`/`lifecycle` 通过 `@AliasFor` 指向 `@Hook`，不重复标注；`lifecycle` 没有默认值，必须显式指定。`storeOrNot=true` 时才会把返回值存到 `storeName`，省略 `storeName` 时默认存到 `$方法名`；`storeName` 在 `literal=false`（默认）时本身也按 SpEL 解析。方法参数 `token` 通过 `@Env` 自动注入 `${auth.token}`，插件不需要用户为函数调用传参，并检查变量是否存在于当前阶段。

### 11.10 其余存量声明端补齐（A 类）

§11.1–§11.9 是核心示例。除此之外，插件扫描出 9 个注解、**22 个真实声明端属性**漏标，现已全部补齐；它们都是声明端，不涉及别名重复标注。`stage` 未显式书写的按默认值 `METHOD` 处理（最宽松，不做阶段过滤）。其中 `@Destroy#func` 同时属于函数引用，已列在 §7.2 的 15 个声明端表中，这里按注解分组再列一次，便于与源码对照，不是重复标注。

| 声明端 | `kind` | `result` | `stage` | 运行期依据 |
|---|---|---|---|---|
| `@SpELImport#root` | `KEY_VALUE`，`keySpel=true` | 空 | 默认 `METHOD` | `Context#importVarHandler` 对 key/value 都调 `analyticExpression(..., true)` |
| `@SpELImport#var` | `KEY_VALUE`，`keySpel=true` | 空 | 默认 `METHOD` | 同上 |
| `@SpELImport#rootLit` | `KEY_VALUE`，`valueSpel=false` | 空 | 默认 `METHOD` | value 走 `analyticExpression(..., false)` |
| `@SpELImport#varLit` | `KEY_VALUE`，`valueSpel=false` | 空 | 默认 `METHOD` | 同上 |
| `@If#condition` | `CONDITION` | `boolean` | 默认 `METHOD` | `Context#importFunHookHandler` 的 `parseExpression(condition, boolean.class)` |
| `@Destroy#value` | `TEMPLATE` | 空 | `DESTROY` | `DefaultDestroyHandle#destroy` 的 `parseExpression(value)` |
| `@Destroy#func` | `FUNCTION` | 空 | `DESTROY` | `getSpELFuncOrDefault(func, "$Destroy")` |
| `@DestroyMeta#enable` | `CONDITION` | `boolean` | `DESTROY` | `MethodContext#destroy` 的 `parseExpression(enable, boolean.class)` |
| `@ExceptionFallback#implExp` | `TEMPLATE` | 空 | `THROWABLE` | `parseExpression(fallbackExp, proxyClass)`，目标类型是当前接口，随注解位置变化 |
| `@EnableConfigurationParser#prefix` | `TEMPLATE` | `String` | 默认 `METHOD` | `ConfigurationApiFunctionalSupport` 的 `context.parseExpression(ann.prefix())` |
| `@EnableConfigurationParser#source` | `TEMPLATE` | `String` | 默认 `METHOD` | 同上，`parseExpression(ann.source())` |
| `@EnableResourceParser#prefix` | `TEMPLATE` | `String` | 默认 `METHOD` | 经 `@Combination` 合并到同一个 `prefix` 属性 |
| `@RepeatableReadStream#storeDir` | `TEMPLATE` | `String` | 默认 `METHOD` | `RepeatableReadStreamConvertUnpack` 的 `parseExpression(convertAnn.storeDir())` |
| `@GeneratedResponseJavaBean#enable` | `CONDITION` | `boolean` | `RESPONSE` | `convertToConfig` 解析，回调 `lifecycle = RESPONSE` |
| `@GeneratedResponseJavaBean#cover` | `CONDITION` | `boolean` | `RESPONSE` | 同上 |
| `@GeneratedResponseJavaBean#extractExpression` | `TEMPLATE` | 空 | `RESPONSE` | `GeneratedJavaCodeUtils` 解析，结果作为数据源，类型不固定 |
| `@GeneratedResponseJavaBean#savePath` | `TEMPLATE` | `String` | `RESPONSE` | `GeneratedJavaCodeUtils` 的 `parseExpression(..., String.class)` |
| `@GeneratedResponseJavaBean#packageName` | `TEMPLATE` | `String` | `RESPONSE` | 同上 |
| `@GeneratedResponseJavaBean#className` | `TEMPLATE` | `String` | `RESPONSE` | 同上 |
| `@GeneratedResponseJavaBean#testSavePath` | `TEMPLATE` | `String` | `RESPONSE` | 同上 |
| `@GeneratedResponseJavaBean#extendClass` | `TEMPLATE` | `String` | `RESPONSE` | `resolveSuperType` 按 String 解析 |
| `@GeneratedResponseJavaBean#implementInterfaces` | `TEMPLATE` | `String`（`String[]` 的元素类型） | `RESPONSE` | `resolveImplementInterfaces` 逐项按 String 解析 |

以下属性已确认**不需要标注**：

- `@Destroy#enable`：`@AliasFor` 到 `@DestroyMeta#enable`；
- `@ExceptionFallback#conditionFunc`：`@AliasFor` 到 `@ExceptionHandleMeta#conditionFunc`；
- `@EnableConfigurationParser#sourceType`：原样用作 `ConfigurationSource` 限定名，不求值；
- `@EnableResourceParser#value`/`source`/`priority`：别名端；
- `@RepeatableReadStream#value`：`StreamType` 枚举；
- `@GeneratedResponseJavaBean` 的 `allowedGenerateEntityMethodReturnTypes`/`useLombok`/`useSwagger`/`nestedClassStrategy`/`fieldNamingStrategy`/`serializationFramework`/`serializable`/`validate`/`generateExampleTest`：`Class`/布尔/枚举字面量。

### 11.11 边界项：`@ValueStore#name`

```java
@SpelAttribute(result = String.class)
String name() default "";
```

- `literal=false`（默认）时，`AbstractValueStoreHookHandler#getVarMap` 对存储名调用 `context.parseExpression(k)`，变量名可以是 SpEL；
- `literal=true` 时原样使用，不解析；Javadoc 已写明该差异，插件在 `literal=true` 时按字面量处理，不做表达式检查；
- `@Callback#storeName`、`@Val#name` 是 `@AliasFor` 别名，不重复标注；
- `result=String` 用于变量名结果类型检查与补全，不表示变量值类型。

---

## 12. 解析规则

### 12.1 判定

1. 元素（或配置字段）上有 `@SpelAttribute` → 参与 SpEL。
2. 没有 → 不参与。

### 12.2 别名与元注解链

- **`@AliasFor`**：先解析出 effective attribute，再去声明端找 `@SpelAttribute`。同一 effective attribute 只允许一处标注。
- **组合注解**：`@Get` → `@HttpRequest`，按 `@AliasFor` 解析到 `@HttpRequest` 的声明端；`@Get` 自身不重复标注。
- **元注解链兜底**：如果元素既没有 `@SpelAttribute`、也没有 `@AliasFor`，但所在注解的元注解上有同名元素且标了 `@SpelAttribute`，则沿用该声明。这是「注解归属回溯」，不是属性继承；生成器会把这种间接使用列进审计报告，供确认。
- 回溯深度上限 16，`java.lang.annotation.*` 不参与。

### 12.3 信息补充

在判定出「是否参与、用哪条声明」之后，名称、类型、说明、示例、版本仍然按下面的来源补充：

- 名称：常量值、`@FunctionAlias`、方法名、参数名、`@SpelName` 指向的元素值；
- 类型：字段类型、方法返回类型、参数类型、`typeExpression`；
- 说明/示例/版本：Javadoc。

这一步是「信息源优先级」，不是元数据继承。

### 12.4 插件默认

- 未知阶段、未知类型降级为 `UNKNOWN`；
- Javadoc 缺失降级为「无说明」；
- 索引版本低于插件要求时，按 `knownAnnotations` 降级为 `UNKNOWN`，不崩溃。

### 12.5 构建期审计规则

生成器在生成索引后额外输出一份审计报告：

| 编号 | 规则 | 级别 |
|---|---|---|
| A1 | 同一 effective attribute 在多处标了 `@SpelAttribute`（声明端与别名端重复） | error |
| A2 | `@SpelAttribute` 标在非 `String`/`String[]`/`Object`/`Object[]` 元素上 | error |
| A3 | `@SpelAttribute` 与 `@SpelSymbol` 同时标在同一元素 | warning |
| A4 | 同一注解内多个 `@SpelAttribute`，其中一个显式写了 `stage`/`result` 而其他没写 | warning |
| A5 | `constraint` 长度与 `result` 不匹配，或 `typeExpression` 中存在无法解析的类型表达式 | error |
| A6 | `@SpelStage` 标在非 `Lifecycle` 元素上 | error |
| A7 | `knownAnnotations` 缺失框架包内的注解 | error |
| A8 | 元素没有 `@SpelAttribute`，但通过元注解链同名匹配拿到了声明 | warning（列出元素，确认是否为有意复用） |
| A9 | 同一个类命中多个**不同的具体**组件类型（如类上 `@ApiConfig`→`API` 与 `@SpelConfig`→`CONFIG` 并存，或方法标记与类标记冲突） | warning（按优先级取第一个，列出全部候选供人工确认） |
| A10 | `@LuckyComponent(type = AUTO)` 且结构推断为 `OTHER` | warning（建议写具体类型） |
| A11 | 同一注解内多个元素标了 `@SpelSeparator` | error（无法确定用哪个） |
| A12 | `@SpelSeparator` 标在非 `String` 元素上 | error |
| A13 | `@SpelSeparator` 元素同时标了 `@SpelAttribute` | warning（分隔符通常不求值） |
| A14 | 同一注解内多个元素标了 `@SpelStage` | error（无法确定用哪个） |
| A15 | 同一注解内多个元素标了 `@SpelName` | error（无法确定用哪个） |
| A16 | `@SpelName` 标在非 `String` 元素上 | error |
| A17 | `@SpelName` 所在注解类型没有标 `@SpelSymbol` | warning（标了也没人读） |
| A18 | `@SpelStage` 所在注解内没有任何 `@SpelAttribute` 元素 | warning（标了但无人消费） |
| A19 | 同一原始类型同时出现在 `result` 与 `typeExpression` | warning（宽候选会让泛型候选失效） |

A4/A8/A13/A17/A18/A19 用 warning，是为保留「故意不过滤」「有意复用」「分隔符同时支持 SpEL」「先声明后接入」的合法写法；报告里逐条列出，由框架作者确认后写进变更记录。

---

## 13. 性能架构

### 13.1 运行期解析（取代构建期索引）

插件不再生成或读取 `META-INF/lucky-spel-metadata.json`，所有元数据在 IDE 运行期从 PSI 与索引读取：

- 注解语义：按 `PsiClass` 读取声明上的 `@SpelAttribute`/`@SpelSeparator`/`@SpelStage`/`@SpelName`，沿 `@AliasFor` 与元注解链解析；
- `@SpelSymbol` 变量：`AnnotatedElementsSearch` 搜索标注字段，常量值即变量名，类型/阶段/Javadoc 描述从声明读取；
- 默认注册函数：读取 `HttpClientProxyObjectFactory#importCommonFunction` 中的 `addSpringElFunctionClass(X.class)` 调用，按 `@FunctionAlias`/`@Namespace`/`@FunctionFilter` 展开；
- 组件：`@LuckyComponent` 标注的类/方法及其元注解链；
- 函数参数/返回值：Java 签名；说明/示例/`@since`：Javadoc。

扫描结果按 `PsiModificationTracker.modificationCount` 缓存，dumb 模式下返回空结果；用户模块中的自定义注解与框架内置注解走同一条路径，无需重新生成任何文件。

`knownAnnotations` 不再作为离线清单存在：一个注解是否属于 lucky 上下文，由它（或其元注解链）是否携带 SpEL 元注解实时判定；未携带者不参与 SpEL，也不执行未解析检查。

判定顺序固定为：

1. 注解声明（含别名/元注解链）上存在 `@SpelAttribute` → 使用声明的 kind/stage/result；
2. 属于框架上下文但当前元素未标注任一 SpEL 元注解 → 按 `UNKNOWN` 降级，不参与未解析检查；
3. 完全不属于 lucky 上下文 → 跳过。

### 13.2 用户注解

- 用户项目中的 `@SpelAttribute`/`@SpelSeparator`/`@SpelStage`/`@SpelName`/`@SpelSymbol`/`@SpelConfig` 按 `PsiClass` 按需解析；
- 按 `PsiModificationTracker.modificationCount` 缓存；
- 不扫描全项目注解；
- Javadoc 解析结果缓存到 `PsiClass`/`PsiMethod`；
- 函数方法的参数注解按 `PsiMethod` 缓存；
- `@Param`/`@Var` 系列按 `PsiParameter` 缓存。

### 13.3 性能热点

- Bean 补全不再用 `PsiShortNamesCache.allClassNames` 扫全项目；用 Spring 注解/`@SpelSymbol` 索引。
- 函数补全按函数名/命名空间 O(1) 查索引，不遍历全项目静态方法。
- Hook 补全按 `Lifecycle` 阶段过滤，不每次全量扫描。
- 注解属性元数据按 `PsiClass` 缓存为不可变 Map，查表 O(1)。

### 13.4 预编译 JSON 清单的最终定位

`annotations.json`、`builtin-symbols.json`、`metadata.json` 与 `tools/generate-*.js` **已全部删除**，不存在任何运行期或测试期的 JSON 数据源：

| 数据 | 运行期来源 |
|---|---|
| 注解属性语义 | 注解声明上的 `@SpelAttribute` 等元注解（含 `@AliasFor`/元注解链） |
| 内置变量 | `@SpelSymbol` 标注的字段常量 |
| 默认注册函数 | `HttpClientProxyObjectFactory#importCommonFunction` 的 `addSpringElFunctionClass` 调用 |
| 组件清单 | `@LuckyComponent` 标注的类/方法及其元注解链 |
| 说明、参数、返回值 | Javadoc 与 Java 签名 |

这样只有一份数据源（源码），不存在两套提取口径分叉的问题；框架还没有外部用户，不保留兼容层。

---

## 14. 与历史版本对比

| 项目 | v1（15 注解） | v2（3 注解 + 继承） | v3（4 注解，类级 + 属性级） | v4（3 注解，逐属性） | v5（7 注解 + 组件 + 元素标记） |
|---|---:|---:|---:|---:|---:|
| 新增注解 | 15 | 3 | 4 | 3 | 7 |
| 注解属性总数 | ~150 | ~25 | ~25 | ~14 | ~11 |
| 类级声明处 | ~500 | 68 | 68 | 0 | 0 |
| 属性级声明处 | ~500 | ~60 | ~60 | ~137 | ~137 |
| 需要维护的属性名清单 | 无 | 68 份 | 68 份 | 0 | 0 |
| 字符串属性引用 | 无 | 有 | 有 | `stageAttribute`/`separatorAttribute` | 无 |
| 属性改名后 | 注解跟着走 | 静默失配 | 静默失配 | 注解跟着走 | 注解跟着走 |
| 判定规则 | 无 | 7 级优先级 + 字段继承表 | 2 条 | 1 条 | 1 条 |
| 合并/覆盖规则 | 无 | 字段级继承 | 无 | 无 | 无 |
| 组件识别 | 无 | 无 | 无 | 无 | `@LuckyComponent(type)`：类/方法直接标注、元注解可继承，显式优先 + `AUTO` 推断兜底 |
| 预估工作量 | 13–19 人天 | 9–12 人天 | 16–21 人天 | 16–21 人天 | 16–21 人天 |

v4 与 v3 的工作量基本持平：v3 少写一些注解，但要多实现组匹配、白名单校验与跨组冲突审计；v4 多写约 70 处注解，但读取与审计逻辑更少。v5 多出 `@SpelSeparator`/`@SpelStage`/`@SpelName`/`@LuckyComponent` 四个元素/身份注解、一个枚举和一个 `ComponentTypeResolver`，同时删掉全部属性名引用，工作量增加约 1 人天。

---

## 15. 落地顺序

### P0：属性级注解 + 核心注解迁移

- 在 `lucky-httpclient` 新增 `com.luckyframework.httpclient.proxy.spel.meta` 包：`@SpelAttribute`、`@SpelSeparator`、`@SpelStage`、`@SpelName`、`@SpelSymbol`、`@SpelConfig`、`@LuckyComponent`、`SpelKind`、`SpelConstraint`、`SpelSymbolKind`、`SpelNaming`、`LuckyComponentType`。
- 复用 `com.luckyframework.httpclient.proxy.spel.hook.Lifecycle`，不新增枚举值：`@SpelSymbol.until` 的“无上界”使用现有 `NON`；`UNKNOWN` 不进框架枚举，留在插件侧 `SpelLifecycle`。
- 迁移 7 个核心注解：`HttpRequest`、`ServerAddress`、`Retryable`、`ConvertMetaType`、`Binary`、`SpELImport`、`Hook`；`@Callback` 通过 `@AliasFor` 指向 `@Hook`，不单独标注。
- 运行时扫描：读取注解 + Javadoc 生成内存中的元数据；输出属性语义、`components`、`@SpelSymbol` 变量与 `addSpringElFunctionClass` 函数；执行 12.5 的 A1–A19 审计。
- 插件：运行时解析 + 按需 PSI + 缓存；组件检测按「类上直接标注 → 类上元注解链 → 方法（含方法注解的元注解链）→ 结构推断」取 `type` 并记录 `source`；按 `type` 映射 5 套图标资源（`iconProvider` + `fileIconProvider`）。

### P1：符号、参数注入与 Hook

- `@SpelSymbol` 接入内置变量与用户自定义参数注解；函数不标注，由 `@SpELImport` 与 `addSpringElFunctionClass` 识别。
- 内置 `@Param` 由插件按全限定名白名单硬编码识别，不标注 `@SpelSymbol`；`generalapi.ai.tool.Param` 排除。
- `@Param`/`@Var`/`@Rar`/`@Env`/`@Bean` 参数注入规则与补全。
- `@ServerAddress#urlFunc`/`pathFunc` 函数引用的补全、跳转、返回值/参数检查。
- `@Hook`/`@Callback` 的阶段跟随、`enable` 条件、回调方法参数注入。
- `@SpelConfig` 接入配置模型（无 `stage`/`valueSpel`，字段逐个标 `@SpelAttribute`）。
- 补齐当前 `annotations.json` 未覆盖的注解与属性：`@ChunkUpload`、`@Describe`、`@Destroy`、`@ExceptionHandle`/`@ExceptionReturn`/`@ExceptionFallback`、`@Async`/`@AsyncExecutor`/`@AsyncHook`、`@Timeout`、`@TextEventStream`、`@MultiFile`、`@HttpProxy`/`@SocksProxy`、`@PrintRequestLog`/`@PrintResponseLog`、`@PropertiesJava`/`@PropertiesJson`、`@RecordReplay`、`@OAuth2ClientCredentialsGrant`/`@OAuth2TokenExchange`、`@Mock`/`@MockMeta`/`@MockPlugin`、`@Wrapper`、`@HttpExec`、`@RespConvert`/`@ResultConvertMeta`，以及 `@Destroy#func`、`@MockPlugin#implFunc`、`@Mock#mockFunc`/`enableFunc`、`@Wrapper#fun`、`@HttpExec#execFunc`、`@RespConvert#respContentTypeFunc` 等函数引用属性；同时删除源码已不存在的旧属性 `@Mock#getEnable`、`@Condition#getAssertion`。

### P2：Javadoc 增强

- 解析 Javadoc 示例、`@since`、`@deprecated`、`@param`/`@return`。
- 悬停文档、示例运行、弃用提示。

---

## 16. 验收清单

模型与规则：

- [ ] 每个参与 SpEL 的属性，都是因为元素上有 `@SpelAttribute`；不存在任何字符串白名单。
- [ ] 删除某个 `@SpelAttribute` 后，该属性立即不再参与 SpEL；加上后立即参与。
- [ ] 把 `@HttpRequest#url` 改名为 `uri` 后，注解跟着方法走，仍参与 SpEL；不存在“清单未更新导致静默失效”。
- [ ] 同一个 effective attribute 只在声明端标注一次；别名端重复标注触发 A1。
- [ ] `@SpelAttribute` 标在非 String 元素上触发 A2。
- [ ] 没有任何解析路径会从其他声明读取字段填充当前 `@SpelAttribute`（无继承）。
- [ ] `@HttpRequest#func` 显式写 `stage`（源码取 `METHOD`）；字段缺失时按默认值 `METHOD` 处理，显式写其他值（如 `REQUEST_INIT`）时按该值过滤。
- [ ] 同一注解内多个 `@SpelAttribute` 的 `stage`/`result` 不一致时，A4 输出 warning。
- [ ] `@Get`/`@Post` 等组合注解通过 `@AliasFor` 拿到 `@HttpRequest` 的声明。
- [ ] `SpelKind` 中不存在 `INHERIT`；`Lifecycle` 中不存在 `INHERIT`。
- [ ] `@SpelAttribute` 不含 `all`、`disabled`、`only`、`separatorAttribute`、`stageAttribute`、分组属性。
- [ ] `@SpelConfig` 不含 `stage`、`valueSpel`。
- [ ] `@SpelSeparator` 无属性，只能标在 `String` 元素上；同一注解内最多一个。
- [ ] `@SpelStage` 无属性，只能标在 `Lifecycle` 元素上；同一注解内最多一个。
- [ ] 把 `@SpelImport#separator` 改名为 `delimiter` 后，`@SpelSeparator` 跟着方法走，`KEY_VALUE` 拆分仍生效。
- [ ] 把 `@Hook#lifecycle` 改名为 `stage` 后，`@SpelStage` 跟着方法走，`enable` 的阶段仍跟随。
- [ ] `@SpelName` 无属性，只能标在 `String` 元素上；所在注解类型必须标 `@SpelSymbol`。
- [ ] 把 `@MyParam#name` 改名为 `key` 后，`@SpelName` 跟着方法走，变量名仍正确。
- [ ] `@SpelAttribute.separator` 与 `@SpelSeparator` 同时存在时，字面量优先。
- [ ] `@LuckyComponent` 只有 `type()`，不含 `value`/`name`/`kind`/`stage` 等 SpEL 语义。
- [ ] `@LuckyComponent` 的 `@Target` 为 `TYPE`/`ANNOTATION_TYPE`/`METHOD`，能直接标类、标方法，也能作为元注解。
- [ ] `LuckyComponentType` 只含 `AUTO`/`API`/`CONFIG`/`FUNCTION`/`SUPPORT`，`OTHER` 只在插件与索引侧。
- [ ] 类上直接标注 `type = API/CONFIG/FUNCTION/SUPPORT` 时直接使用声明值，不做推断，`source = class`。
- [ ] 接口标 `@Get`/`@Post` 时经 `@HttpRequest` 的元注解链被识别为 `API` 组件，`source = meta`。
- [ ] 类上无标记、仅某个方法标了 `@LuckyComponent`（或方法注解的元注解链上有）时，类仍被识别为组件，`source = method`，方法记入 `methods`。
- [ ] 子类不会因为父类标了 `@LuckyComponent` 而成为组件（无 `@Inherited`）。
- [ ] 同一个类命中多个不同的具体 `type` 时触发 A9，并采用优先级最高的值。
- [ ] `@LuckyComponent(type = AUTO)` 且推不出类型时触发 A10。
- [ ] 没有 `@LuckyComponent` 但有 `@HttpRequest`/`@SpelConfig`/`@Namespace` 的类，仍能被推断识别。

数据与兼容：

- [ ] 注解语义只来自 `@SpelAttribute` 声明，没有类级元素表，也没有 `typeLevel`。
- [ ] 声明端与使用端信息都可从 PSI 直接读出，不需要离线快照或生成器。
- [ ] `@SpelSeparator`/`@SpelStage` 只作为元素来源参与解析，不被当作普通属性符号输出。
- [ ] `@SpelName` 只在 `@SpelSymbol` 注解类型的变量名推导中生效，不作为普通符号输出。
- [ ] 组件清单来自 `@LuckyComponent`：`className`/`title`/`type`/`declared`/`source` 字段齐备，`source = method` 的类仍被识别。
- [ ] 注解是否属于 lucky 上下文由 PSI 实时判定，未带 SpEL 元注解者降级为不参与而不是误报。
- [ ] 两个 `@Param`（`com.luckyframework.reflect.Param` 与 `com.luckyframework.httpclient.generalapi.ai.tool.Param`）按全限定名区分，后者不产生任何 SpEL 行为。
- [ ] 框架 `Lifecycle` 未新增任何枚举值；`@SpelSymbol.until` 用 `NON` 表示无上界；`UNKNOWN` 只在插件侧。
- [ ] `annotations.json`/`builtin-symbols.json`/`metadata.json` 与生成工具不存在，插件运行期只从 PSI/索引读取元数据。

功能与性能：

- [ ] 多结果类型按 `result ∪ typeExpression` 的“任一候选通过”检查；`constraint` 长度错误与 `typeExpression` 解析失败能被报告。
- [ ] `@ServerAddress#urlFunc` 支持字面量函数名和 `#{...}` 动态函数名两种写法。
- [ ] 函数参数带 `@Param`/`@Var` 系列时按表达式注入，不带注解时按类型注入，两者都不满足时能给出提示。
- [ ] `@Hook#enable` 的阶段跟随 `lifecycle`。
- [ ] 73 个内置变量的名称/类型/阶段能从 `@SpelSymbol` 字段的常量值、字段类型与 Javadoc 读取，插件运行期不再依赖任何变量清单文件。
- [ ] 151 个内置函数不需要逐个写注解：内置函数由 `addSpringElFunctionClass` 注册（工厂构造即生效，从 `CLASS` 阶段可用），用户函数由 `@SPELImport` 导入（类上 `CLASS`、方法上 `METHOD_META`）；`@SpelSymbol` 不参与函数识别。
- [ ] `@Val` 字段值按 SpEL 解析，`@Pack` 字段值不解析；`@Callback`/`@Val`/`@Pack` 的 `lifecycle` 必填，`@Callback#storeName` 只在 `storeOrNot=true` 时生效。
- [ ] 配置 key 能按 `naming` 从字段名生成。
- [ ] `@SpelSeparator` 元素被改名/删除时，A11/A12 能报出具体注解与元素。
- [ ] `@SpelStage`/`@SpelName` 被改名/删除时，A6/A14–A18 能报出具体注解与元素。
- [ ] `@LuckyComponent` 类（含方法标注、元注解链命中）在 Project 视图和编辑器标签显示四叶草图标，且 `API`/`CONFIG`/`FUNCTION`/`SUPPORT` 图标与颜色各不相同；取消标注后图标消失。
- [ ] 图标资源缺失时降级为 `luckyComponent.svg`，不弹异常。
- [ ] 组件展示名取 Javadoc 第一段首句；无注释时显示简单类名。
- [ ] 组件扫描能列出项目内全部组件并按 `type` 分组，同时按 `source` 标注识别入口，并对 `AUTO`/未标注类给出推断结果。
- [ ] Javadoc 缺失时插件降级，不报错。
- [ ] 补全 P95 延迟不超过迁移前 10%。
- [ ] 现有 623 条插件用例全绿，`buildPlugin` 通过。

---

## 17. 结论

v5 的核心是**让元数据只有一个来源**：

- 说明、参数、返回值、示例、版本 → Javadoc；
- 名称、类型、泛型、重载、默认值 → Java 签名/注解元素默认值；
- “这个属性能用 SpEL 吗、怎么解析、结果是什么、何时生效” → 元素上的 `@SpelAttribute`；
- “分隔符是哪个属性” → 该元素上的 `@SpelSeparator`；
- “阶段来自哪个属性” → 该元素上的 `@SpelStage`；
- “参数注解的哪个元素是变量名” → 该元素上的 `@SpelName`；
- “哪些字段/参数是 SpEL 符号” → `@SpelSymbol`；
- “哪些函数可用、从哪个阶段可用” → `@SPELImport` 与 `HttpClientProxyObjectFactory#addSpringElFunctionClass(Class<?>)`，不是注解标记；
- “配置模型的前缀与命名” → `@SpelConfig`；
- “这个类是不是 Lucky 组件、是哪一类” → `@LuckyComponent(type = ...)`（类上、方法上或元注解链上；只给 IDEA 插件读，运行期不读）。

接入一个属性通常只需要：

```java
@SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
```

声明一个组件只需要：

```java
@LuckyComponent(type = LuckyComponentType.API)
public interface UserApi { ... }
```

框架内的注解只需在注解声明上标一次，使用处自动继承：

```java
@LuckyComponent(type = LuckyComponentType.API)
public @interface HttpRequest { ... }
```

组件名不需要写在注解里：Javadoc 第一段首句就是展示名，读不到就用类名。

不需要类级注解，不需要属性名清单，也不存在 `separatorAttribute`/`stageAttribute`/`nameAttribute` 这种字符串引用；属性改名时，注解跟着方法走，不会静默失效。
