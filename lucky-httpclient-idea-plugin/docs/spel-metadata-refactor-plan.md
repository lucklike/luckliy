# SpEL 元数据重构：实施方案与问题清单（v5 逐属性标注 + 组件识别 + 元素标记）

> 状态：方案定稿（D1–D12 已确认）  
> 取代范围：v2 的 `@Spel` 单注解 + 字段继承；v3 的 `@SpelAttributes` 类级注解  
> 关联文档：[设计文档](spel-metadata-annotations.md)、[使用指南](spel-metadata-annotations-usage.md)  
> 核心变化：**没有任何类级注解，也没有属性名清单。支持 SpEL 的属性逐条标 `@SpelAttribute`；分隔符、阶段来源、符号名来源分别标在 `@SpelSeparator`/`@SpelStage`/`@SpelName` 元素上，不再写 `separatorAttribute`/`stageAttribute`/`nameAttribute`；组件身份与类型用 `@LuckyComponent(type = ...)` 显式声明（类上、方法上或元注解，只给 IDEA 插件读），展示名从 Javadoc 读取。**
>
> 本文件只描述方案与问题，不含代码改动。
>
> **实现现状（已落地）**：插件不读取任何预编译 JSON 清单，也不存在生成器。`spel/metadata.json`、`spel/annotations.json`、`spel/builtin-symbols.json` 与 `tools/generate-*.js` 已全部删除；注解属性语义、内置变量、默认注册函数、组件清单均在 IDE 运行时从 PSI/索引解析。文中出现的「索引 / JSON / 快照 / 生成器」仅作历史方案记录，不代表当前实现。

---

## 0. 一句话结论

**方案可行。** v4 把「哪些属性支持 SpEL」从字符串白名单改成元素上的注解，代价是每个属性多写一行注解，收益是属性改名不会静默失效、判定规则从两条降为一条、审计从「校验清单」变成「检查一致性」。v5 在其上补组件身份与显式类型，并用 `@SpelSeparator`/`@SpelStage`/`@SpelName` 把分隔符、阶段来源、符号名来源全部元素化，删除最后一处属性名引用，只多四个注解、一个枚举和一张图标映射表。总工作量 16–21 人天，与 v3 持平。

---

## 1. 为什么不要类级注解

三代模型的问题演进：

| 版本 | 模型 | 核心问题 |
|---|---|---|
| v2 | 单个 `@Spel`，类级 + 字段级继承 | 7 级优先级 + 3×5 字段继承表，使用者要推演「哪个字段从哪来」 |
| v3 | `@SpelAttributes`（类级白名单）+ `@SpelAttribute`（属性级） | 类级 `value()` 是第二份属性名清单，属性改名时静默失配 |
| v4 | 只有 `@SpelAttribute` | 无 |

v3 的故障模式：

```java
// v3
@SpelAttributes(value = {"url", "func"}, stage = Lifecycle.REQUEST_INIT, result = String.class)
public @interface HttpRequest {
    String url() default "";   // 改成 uri 后，白名单里的 "url" 不会变
    String func() default "";
}
```

| 操作 | v3 的后果 |
|---|---|
| `url` 改名为 `uri` | 白名单仍是 `"url"`，该属性静默失去 SpEL 支持 |
| 新增 `String path()` | 忘记加进 `value()`，静默不生效 |
| 删除属性 | 白名单留下死名字，只能靠源码审计发现 |
| 复制注解做新注解 | 白名单跟着复制，容易出现对不上的名字 |

这些都是「两份数据不同步」，而且失败是静默的。v4 把元数据写在元素上，属性改名时注解跟着方法走或一起报错，不存在「清单没更新」这个状态。

### 1.1 三代对比

| 项目 | v2 | v3 | v4 |
|---|---:|---:|---:|
| 注解数量 | 3 | 4 | 3 |
| 类级声明处 | 68 | 68 | 0 |
| 属性级声明处 | ~60 | ~60 | ~137 |
| 维护的属性名清单 | 68 份 | 68 份 | 0 |
| 属性改名后 | 注解跟着走（无白名单） | 静默失配 | 注解跟着走 |
| 判定规则 | 7 级优先级 + 继承表 | 2 条 | 1 条 |
| 字段继承 | 有 | 无 | 无 |
| 工作量（人天） | 9–12 | 16–21 | 16–21 |

v2 也没有白名单，但有继承；v4 是「无继承 + 无白名单」的组合。

---

## 2. 新注解模型

### 2.1 七个注解

| 注解 | 目标 | 作用 |
|---|---|---|
| `@SpelAttribute` | `METHOD`、`FIELD` | 单个属性的 SpEL 语义 |
| `@SpelSeparator` | `METHOD`、`FIELD` | 标记哪个元素是分隔符（`KEY_VALUE`） |
| `@SpelStage` | `METHOD`、`FIELD` | 标记哪个元素提供生效阶段（`Lifecycle`） |
| `@SpelName` | `METHOD`、`FIELD` | 标记参数注解中哪个元素提供变量名（`String`） |
| `@SpelSymbol` | 字段、参数、注解类型 | SpEL 符号（函数不标注，由 `@SPELImport` 与 `addSpringElFunctionClass` 识别） |
| `@SpelConfig` | 类型 | 配置模型前缀/别名/命名 |
| `@LuckyComponent` | 类型、注解类型、方法 | 类/接口/方法的组件身份与类型（只给 IDEA 插件读） |

### 2.2 `@SpelAttribute` 与三个元素标记注解

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

要点：

- 字段集合与 v3 相比，删掉了全部属性名引用：`separatorAttribute`、`stageAttribute`、以及 `@SpelSymbol.nameAttribute`。这三处都是第二份属性名数据，属性改名时不同步。
- 没有 `value()`：属性名就是注解所在的方法名/字段名。
- 没有 `all`/`disabled`/`only`：标了就是参与，不参与就不标。
- 没有 `INHERIT`：`kind`/`stage` 的默认值就是最终值。
- 最简写法 `@SpelAttribute` 一行即可：`TEMPLATE` + `METHOD` + 不限制类型。

**`@SpelSeparator`**

```java
public @interface SpELImport {

    @SpelAttribute(kind = SpelKind.KEY_VALUE, stage = Lifecycle.METHOD, valueSpel = false)
    String[] rootLit() default {};

    @SpelSeparator
    String separator() default "=";
}
```

- 无属性：只回答「这个元素是分隔符」，不携带任何值；
- 同一注解最多一个（A11 error）、只能标 `String` 元素（A12 error）、不需要再标 `@SpelAttribute`（同标 A13 warning）；
- 解析优先级：`@SpelAttribute.separator` 字面量非空 > 同注解内 `@SpelSeparator` 元素（含 `@AliasFor` 声明端）> 不拆分；
- 不进元素元数据表，运行期在同注解内按 `@SpelSeparator` 定位，不落盘；
- 包位置：`com.luckyframework.httpclient.proxy.spel.meta`，与 `@SpelAttribute` 同包；
- 用户自定义注解同样适用，不需要维护任何名字表。

**`@SpelStage`**

```java
public @interface Hook {

    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String enable() default "";

    @SpelStage
    Lifecycle lifecycle() default Lifecycle.NON;
}
```

- 无属性：只回答「这个元素提供生效阶段」，不携带其他信息；
- 它是**注解级的默认阶段来源**：同注解内所有 `@SpelAttribute` 元素，只要 `stage` 没被显式书写，都用这个元素的值；`@Callback`/`@Val`/`@Pack` 通过 `@AliasFor` 指向 `@Hook`，不重复标注；
- 只能标 `Lifecycle` 元素（A6 error）；同一注解最多一个（A14 error）；与 `@SpelAttribute` 同标由 A2 拦下；所在注解无 `@SpelAttribute` → A18 warning；
- 解析优先级：`stage` 显式书写 > 同注解内 `@SpelStage` 元素（含 `@AliasFor` 声明端）> `METHOD`；
- 不进元素元数据表，运行期在同注解内按 `@SpelStage` 定位，不落盘；
- 与 v3 类级注解的区别：`@SpelStage` 没有属性值、不点名任何属性，只标记「运行期实参所在元素」；`kind`/`result` 这类声明期常量仍逐属性写死。可复用的判定标准见设计文档 6.5.2；
- 运行期依据：`HookGroup` 按 `hookAnn.lifecycle()` 分组后，执行到该阶段才解析 `enable`。

**`@SpelName`**

```java
@SpelSymbol(kind = SpelSymbolKind.PARAM)
public @interface MyParam {

    @SpelName
    String name();
}
```

- 无属性：只回答「哪个元素是变量名」；只在 `@SpelSymbol` 标在注解类型（`ANNOTATION_TYPE`）上时生效；
- 只能标 `String` 元素（A16 error）；同一注解最多一个（A15 error）；所在注解类型无 `@SpelSymbol` → A17 warning；
- 解析优先级：`@SpelSymbol.name` 显式 > `@SpelName` 元素值 > `value` 元素 > 无名称（不回退参数名）；
- 运行期在 `@SpelSymbol` 注解类型内单元素查找，不落盘；
- 与 `@SpelAttribute` 同标合法（名字元素本身可以是 SpEL），不报审计。

### 2.3 `@LuckyComponent` 与 `LuckyComponentType`

```java
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuckyComponent {

    LuckyComponentType type() default LuckyComponentType.AUTO;
}

public enum LuckyComponentType {
    AUTO, API, CONFIG, FUNCTION, SUPPORT
}
```

- **只给 IDEA 插件用**：框架运行期不读取，删除后不改变任何运行期行为；插件用它做文件标记（图标）、组件聚合（清单）、搜索与调试；
- 包位置：`@LuckyComponent`/`LuckyComponentType` 都在 `com.luckyframework.httpclient.proxy.spel.meta`，与 `@SpelConfig` 同包；框架身份注解（`proxy.annotations`/`configapi`/`spel`）按需 import；
- 回答「这个类是不是 Lucky 组件、是哪一类」，不参与 SpEL，不进 `spelAnnotations`；
- 三个识别入口：类/接口上直接标注（`source = class`）、类上注解的元注解链（`source = meta`）、类中方法上（直接标或方法注解链，`source = method`）；任意一处命中即为组件；
- 类型优先级：类直接标注 > 类元注解链 > 方法（含方法注解链）> `AUTO`/未标注走 `ComponentTypeResolver` 结构推断，推不出为 `OTHER`（仅推断侧，不进枚举）；命中多个不同具体类型时取优先级最高的值并报 A9；
- 不继承（无 `@Inherited`，子类不受父类标注影响）、不参与 Spring 注册、不是 `@Component` 的替代；
- 展示名不在注解里：取 Javadoc 第一段首句，读不到用简单类名；
- 框架已在 5 个身份注解上标注：`@HttpRequest`/`@ServerAddressMeta`/`@ApiConfig` → `API`，`@SpelConfig` → `CONFIG`，`@Namespace` → `FUNCTION`；`SUPPORT` 无身份注解，只走结构推断；
- 组件清单由运行期扫描 `@LuckyComponent` 得到（`class`/`title`/`type`/`declared`/`source`，`source = method` 时含 `methods`），不产出任何清单文件。

### 2.4 枚举调整

| 枚举 | v2 | v5 |
|---|---|---|
| `SpelKind` | 含 `INHERIT` | 无 `INHERIT`，默认 `TEMPLATE` |
| `Lifecycle` | 16 值 + `INHERIT` | 16 值，不新增值 |
| `SpelConstraint` | 8 值 | 不变 |
| `SpelSymbolKind` | 8 值 | 不变 |
| `SpelNaming` | 4 值 | 不变 |
| `LuckyComponentType` | 无（不存在） | 新增 5 值：`AUTO`/`API`/`CONFIG`/`FUNCTION`/`SUPPORT`；`OTHER` 只在插件侧 |

`Lifecycle` 不新增值：`@SpelSymbol.until` 的“无上界”复用现有 `NON`（`NON` 不对应真实阶段，作为上界等价于无上界）；`INHERIT` 没有注解需要书写，不进。`UNKNOWN` 保留在插件侧 `SpelLifecycle`。

### 2.5 解析规则（一条 + 五个辅助步骤 + 组件判定）

**元素上有 `@SpelAttribute` → 参与；没有 → 不参与。**

辅助步骤只决定「去哪找这条注解」与「分隔符/阶段/名称是谁」，不改变判定：

1. **`@AliasFor`**：先解析 effective attribute，再去**声明端**找 `@SpelAttribute`；两端都标触发 A1。
2. **元注解链**：元素既没有注解也没有别名时，沿元注解链找同名元素上的 `@SpelAttribute`（组合注解兜底），命中后输出 A8 warning。
3. **分隔符**：`KEY_VALUE` 元素先看 `@SpelAttribute.separator` 字面量；为空时在同注解内找 `@SpelSeparator` 元素（含 `@AliasFor` 声明端）；都没有则不拆分。字面量优先于元素，两者都写不报错。
4. **阶段来源**：`stage` 显式书写时用它；否则看同注解内的 `@SpelStage` 元素；都没有用 `METHOD`。
5. **名称来源**：`@SpelSymbol.name` 显式时用它；否则看同注解内的 `@SpelName` 元素；都没有回退 `value` 元素，仍无则无名称。

分隔符、阶段、名称的查找范围都是「同一注解」，不跨元注解链：`@SpELImport`、`@Hook`、`MyParam` 都是声明端，组合注解不重复标注。

组件判定独立于上面五步：

1. 类/接口上直接标了 `@LuckyComponent`（非 `AUTO`）→ 用该值（`source = class`）；
2. 类上某注解的元注解链上有（非 `AUTO`）→ 取最近一层（`source = meta`）；
3. 方法上直接标或方法注解的元注解链上有（非 `AUTO`）→ 取最近一层（`source = method`），方法记入 `methods`；
4. 命中值是 `AUTO` 或完全未标注 → `ComponentTypeResolver` 结构推断（`source = infer`），推不出：有标记为 `OTHER`，无标记则不是组件。

同一个类命中多个不同的具体类型（不论层级）时取优先级最高的值，并报 A9 warning。

展示名统一取 Javadoc 第一段首句，读不到用简单类名，与是否标注 `@LuckyComponent` 无关。

---

## 3. 可行性评估

### 3.1 关键论据：失败模式都是安全降级

| 漏写字段 | 实际效果 | 后果方向 |
|---|---|---|
| `stage` | `METHOD`，不参与阶段过滤 | 少提示，**不误报** |
| `result`/`typeExpression` | 都为空时不限制类型 | 少检查，**不误报** |
| `constraint` | `ANY` | 同上 |
| `separator` | 空串且无 `@SpelSeparator` 元素，无法拆分 | 功能不生效，**不误报** |
| `@SpelStage` 漏标 | 全部`@SpelAttribute`按 `METHOD`，不参与阶段过滤 | 少提示，**不误报** |
| `@SpelName` 漏标 | 回退 `value` 元素；仍无则无名称 | 少识别，**不误报** |

缺省统一指向「少做检查」而不是「做错检查」，所以不需要继承兜底，用源码审计保证声明完整即可。

### 3.2 声明量影响：处数 +7%，字符 +24%

| 项目 | v3 | v4 | 差异 |
|---|---:|---:|---|
| 类级声明处 | 68 | 0 | −68 |
| 属性级声明处 | ~60 | ~137 | +77 |
| 总声明处 | ~128 | ~137 | +7% |
| 总字符数 | ~10.9k | ~13.5k | +24% |
| 维护的字符串清单 | 68 份 | 0 | −68 |

字符增量主要来自「每个属性都要写 `stage`/`result`」。用最简写法（不写参数）可把增量压到 10% 以内；只有需要阶段过滤或类型检查的属性才写参数。

### 3.3 心智负担对比

| 需要理解的概念 | v2 | v3 | v4 |
|---|---|---|---|
| 优先级层级 | 7 级 | 2 条 | 1 条 |
| 字段继承表 | 3×5 | 无 | 无 |
| `INHERIT` 哨兵值 | 2 个 | 0 | 0 |
| 类级 vs 属性级谁生效 | 需推演 | 需记住两条规则 | 不存在 |
| 多组声明与跨组互斥 | 无 | 需要 | 无 |
| 属性名清单维护 | 无 | 68 份 | 无 |

### 3.4 实现难度对比

| 插件侧逻辑 | v3 | v4 |
|---|---|---|
| 元数据解析 | 二选一分支 + 组匹配 | 单分支 |
| 缓存键 | 注解 + 属性 + 组 | 注解 + 属性 |
| 阶段推导 | `stage` / `stageAttribute` / 默认 | `stage` / `@SpelStage` / 默认 |
| 阶段匹配复杂度 | 需要按组顺序遍历 | O(1) 查元素 |
| 审计 | 白名单存在性 + 跨组冲突 + 一致性 | 只有一致性 |

v4 少实现了组匹配、白名单校验、跨组冲突检测；多出来的是 77 处注解。

### 3.5 可行性结论

| 维度 | 判断 |
|---|---|
| 技术上可实现 | 是。注解元素支持 `METHOD`/`FIELD` 目标，覆盖全部 137 个属性 |
| 表达能力是否够用 | 是。每条声明自包含，不需要「一组共享语义」抽象 |
| 是否有无法表达的个例 | 无。v3 里靠组表达的场景，v4 逐条写即可 |
| 组件识别是否可实现 | 是。`@LuckyComponent` 的 `TYPE`/`ANNOTATION_TYPE`/`METHOD` 三个目标覆盖「类上直接标、元注解继承、方法标记」三种入口；一个 `LuckyComponentType` 枚举 + 按 `type` 一维查表的图标映射；检测器与解析器共用同一份优先级规则 |
| 分隔符是否可实现 | 是。`@SpelSeparator` 无属性、只需 `METHOD`/`FIELD` 目标；解析只多一步「同注解内找元素」，不跨元注解链，不需要名字表 |
| 阶段与名称来源是否可实现 | 是。`@SpelStage` 只需 `Lifecycle` 类型校验，`@SpelName` 只需 `String` 类型校验；两者都是同注解内单元素查找，与分隔符共用同一套 PSI 缓存 |
| 主要代价 | 同类属性要重复写 `stage`/`result`；漏标只能靠 A4 warning 发现 |
| 结论 | **推荐采用** |

---

## 4. 整体实施方案

### 4.1 阶段划分

| 阶段 | 目标 | 产出 | 估算 |
|---|---|---|---:|
| P0 | 注解可用、运行时可解析、插件可读 | `meta` 包（7 注解 + 5 枚举）、7 个核心注解逐属性标注、运行时扫描（`@SpelSymbol`/`addSpringElFunctionClass`/`@LuckyComponent`）、组件识别与四类图标、分隔符/阶段/名称解析 | 6–7 人天 |
| P1 | 符号、参数注入、Hook、配置模型 | `@SpelSymbol`、`@Param` 白名单、函数引用、`@SpelConfig` | 6–7 人天 |
| P2 | Javadoc 增强 | 示例、`@since`、`@deprecated`、悬停文档 | 2–3 人天 |
| 回归 | 全量回归 + 文档 + 发布 | 全量用例 + 新增用例、打包校验 | 2–3 人天 |
| **合计** | | | **16–21 人天** |

### 4.2 PR 拆分

| PR | 内容 | 依赖 | 完成判据 |
|---|---|---|---|
| PR0 | 删除 `spel/metadata.json`、`spel/annotations.json`、`spel/builtin-symbols.json` 与 `tools/generate-*.js`，同步清理加载器、JSON 依赖用例与文档口径 | 无 | 仓库无 JSON 资产，全量用例通过 |
| PR1 | `meta` 包：`@SpelAttribute`、`@SpelSeparator`、`@SpelStage`、`@SpelName`、`@SpelSymbol`、`@SpelConfig`、`@LuckyComponent`、`SpelKind`、`SpelConstraint`、`SpelSymbolKind`、`SpelNaming`、`LuckyComponentType`；`Lifecycle` 不改 | 无 | 编译通过 + 注解单测 |
| PR2 | 迁移 7 个核心注解（逐属性标注）：`HttpRequest`、`ServerAddress`、`Retryable`、`ConvertMetaType`、`Binary`、`SpELImport`、`Hook` | PR1 | 逐属性语义可在 IDE 内解析，无清单残留 |
| PR3 | 运行时扫描：`@SpelSymbol` 变量 + `importCommonFunction` 函数 + `@LuckyComponent` 组件；A1–A19 审计改为源码检查 | PR1、PR2 | 运行时符号与注解语义可查询 |
| PR4 | 插件读取端：PSI 解析（`SpelMetaAnnotationReader`/`SpelAnnotationResolver`）、单分支解析、别名/元注解链、数组对齐、`FUNCTION` 解析、按需 PSI、缓存、组件类型识别与 5 套图标 | PR3 | 全量用例全绿 |
| PR5 | 参数注入：`@Param` 全限定名白名单、`@Var` 系列、函数引用检查（33 个属性名） | PR4 | 注入与函数引用用例通过 |
| PR6 | Hook：`@SpelStage` 阶段来源、`@Callback` 参数注入、`@Val` 字段 SpEL、`@Pack` 排除 | PR4、PR5 | Hook 用例通过 |
| PR7 | `@SpelSymbol`：内置变量、用户自定义参数注解（含 `@SpelName` 解析）；函数不标注，按 `@SPELImport`/`addSpringElFunctionClass` 识别 | PR4 | 73 变量 / 151 函数可补全 |
| PR8 | `@SpelConfig` + 补齐 30+ 个注解（含 33 个函数引用属性） | PR4 | 配置与覆盖率清零 |
| PR9 | Javadoc 增强：示例、版本、弃用、悬停 | PR4 | 悬停用例通过 |

### 4.3 运行期数据模型（无预编译清单）

**不存在索引文件。** `spel/metadata.json`、`spel/annotations.json`、`spel/builtin-symbols.json` 与 `tools/generate-*.js` 已全部删除，插件不再有「生成 → 打包 → 加载」链路；全部元数据在 IDE 内按需从 PSI 读取，结果只存在内存缓存中：

| 数据 | 运行期来源 | 读取方式 | 失效方式 |
|---|---|---|---|
| 注解属性语义 | 元素上的 `@SpelAttribute` | 沿 `@AliasFor`（含跨注解）与元注解链回溯，读注解实参 | `PsiModificationTracker` 变化即重算 |
| 分隔符 / 阶段来源 / 名称来源 | 同注解内标了 `@SpelSeparator`/`@SpelStage`/`@SpelName` 的元素 | 同注解内单元素查找，不跨元注解链 | 同上 |
| 内置变量 | 全工程 `@SpelSymbol` 标注字段（含用户自定义） | `AnnotatedElementsSearch` + `computeConstantValue` | 同上 |
| 默认注册函数 | `HttpClientProxyObjectFactory#importCommonFunction()` 的 `addSpringElFunctionClass(...)` 实参 | 读方法体；方法体不可读时回退 `DEFAULT_FUNCTION_CLASSES` 常量清单 | 同上 |
| 组件清单 | `@LuckyComponent` 类 / 方法 / 元注解链 | 与 2.5 节组件判定同一套优先级 | 同上 |
| 描述与展示名 | Javadoc 首句 / `@param` / `@return` | 运行时读取并归一化（`{@code}` 等标签转纯文本） | 随 PSI 缓存 |

约束：

- 不存在的序列化字段：`schemaVersion`、`frameworkVersion`、`knownAnnotations`、`explicit`、`components` 段、`separatorElement`/`stageElement`/`nameElement`，它们只属于历史方案。
- 不存在构建期生成与版本对齐步骤：框架改注解、加变量、加函数类后，IDE 重新索引即生效，不需要重新生成清单或重新打包插件。
- dumb mode 下返回空快照不阻塞编辑；框架依赖缺失时符号表退化为「仅参数 + `@SpELImport` 导入」，不读任何本地清单兜底。
- 图标映射仍写死在插件侧（`API` 绿、`CONFIG` 蓝、`FUNCTION` 橙、`SUPPORT` 紫、`OTHER` 灰），不进入任何数据文件。

### 4.4 源码审计规则（PSI 检查）

| 编号 | 规则 | 级别 |
|---|---|---|
| A1 | 同一 effective attribute 在多处标了 `@SpelAttribute`（声明端与别名端重复） | error |
| A2 | `@SpelAttribute` 标在非 `String`/`String[]`/`Object`/`Object[]` 元素上 | error |
| A3 | `@SpelAttribute` 与 `@SpelSymbol` 同时标在同一元素 | warning |
| A4 | 同一注解内多个 `@SpelAttribute`，其中一个显式写了 `stage`/`result` 而其他没写 | warning |
| A5 | `constraint` 长度与 `result` 不匹配，或 `typeExpression` 中存在无法解析的类型表达式 | error |
| A6 | `@SpelStage` 标在非 `Lifecycle` 元素上 | error |
| A7 | 运行期扫描不到框架包内的注解（依赖未配置或索引未建立） | error |
| A8 | 元素没标注解，但通过元注解链同名匹配拿到了声明 | warning |
| A9 | 同一个类命中多个**不同的具体**组件类型（类上多个身份注解，或类标记与方法标记不一致） | warning（按优先级取第一个，列出全部候选） |
| A10 | `@LuckyComponent(type = AUTO)` 且推断为 `OTHER`（显式标了却分不出类型） | warning |
| A11 | 同一注解内多个元素标了 `@SpelSeparator` | error（无法确定用哪个） |
| A12 | `@SpelSeparator` 标在非 `String` 元素上 | error |
| A13 | `@SpelSeparator` 元素同时标了 `@SpelAttribute` | warning（分隔符通常不求值） |
| A14 | 同一注解内多个元素标了 `@SpelStage` | error（无法确定用哪个） |
| A15 | 同一注解内多个元素标了 `@SpelName` | error（无法确定用哪个） |
| A16 | `@SpelName` 标在非 `String` 元素上 | error |
| A17 | `@SpelName` 所在注解类型没有标 `@SpelSymbol` | warning（标了也没人读） |
| A18 | `@SpelStage` 所在注解内没有任何 `@SpelAttribute` | warning（标了但无人消费） |
| A19 | 同一原始类型同时出现在 `result` 与 `typeExpression` | warning（宽候选会让泛型候选失效） |

A3/A4/A8/A9/A10/A13/A17/A18/A19 用 warning 是保留「故意不过滤」「有意复用」「暂不分类」「分隔符同时支持 SpEL」「先声明后接入」的合法写法；报告逐条列出，由框架作者确认后写进变更记录。

与 v3 相比删除的规则：C1（组为空）、C2（白名单属性名不存在）、C9（跨组重复）——这三条都是白名单机制特有的，v4 不存在；C3 升级为 A1，C4/C5 合并为 A4，C6/C7/C8 变为 A5/A6/A7。

---

## 5. 问题清单

### 5.1 已决策

| ID | 问题 | 结论 | 状态 |
|---|---|---|---|
| D1 | 两个注解的命名 | `@SpelAttributes`（类级）/ `@SpelAttribute`（属性级） | 已被 D8 取代 |
| D2 | 是否保留 `all` + `disabled` | 全部删除，只保留 `value()` 白名单 | 已被 D8 取代 |
| D3 | 属性级 `stage` 默认值 | `METHOD`（不写就不过滤）+ 审计 | ✅ 沿用 |
| D4 | `@SpelConfig` 的 `stage` / `valueSpel` | 删除；配置字段逐个标 `@SpelAttribute` 并显式写 `stage` | ✅ 已确认 |
| D5 | 类级与属性级重复声明 | 属性级整条替换 + 报 error（按 effective attribute 判定） | 已收敛为 A1 |
| D6 | `@SpelAttribute.condition` 是否改名 | 保留；文档明确它与 `kind=CONDITION` 的区别 | ✅ 已确认 |
| D7 | `@SpelAttributes` 是否需要多组 | 支持 `@Repeatable`，跨组属性名互斥 | 已被 D8 取代 |
| D8 | 是否保留类级 `@SpelAttributes` | **删除**。只保留属性级 `@SpelAttribute`，逐条标注；不做任何属性名字符串白名单 | ✅ 已确认 |
| D9 | 如何识别「类文件是 Lucky 组件」及分类 | 新增 `@LuckyComponent(type = LuckyComponentType)`（`TYPE`/`ANNOTATION_TYPE`/`METHOD` 目标）；插件检测类上、类上元注解链、方法上三处，任一命中即为组件；类型按类 > 方法、直接 > 元注解取第一个；`AUTO` 才走结构推断；只给 IDEA 插件读，不参与 SpEL、不继承；展示名从 Javadoc 读，读不到用类名 | ✅ 已确认 |
| D10 | `separatorAttribute` 的属性名引用如何删除 | 新增 `@SpelSeparator`（`METHOD`/`FIELD` 目标，无属性），标在分隔符元素上；同一注解最多一个（A11）、只能 `String`（A12）、与 `@SpelAttribute` 同标 warning（A13）；字面量 `separator` 优先；不进元素元数据表，运行期在同注解内查找 | ✅ 已确认 |
| D11 | `stageAttribute` 的属性名引用如何删除 | 新增 `@SpelStage`（无属性），标在 `Lifecycle` 元素上，作为整个注解的默认阶段来源；显式 `stage` 优先；同一注解最多一个（A14）、类型错（A6）、无消费者 warning（A18）；运行期在同注解内查找 | ✅ 已确认 |
| D12 | `@SpelSymbol.nameAttribute` 的属性名引用如何删除 | 新增 `@SpelName`（无属性），标在参数注解的 `String` 元素上；`@SpelSymbol.name` 显式优先，无 `@SpelName` 时回退 `value`；同一注解多个（A15）、非 `String`（A16）、无 `@SpelSymbol` 宿主 warning（A17）；运行期在同注解内查找 | ✅ 已确认 |

D8 的理由：`value()` 白名单是第二份属性名数据，属性改名时不会同步，且失败是静默的。删除后判定规则降为一条，审计从「校验白名单」变成「检查一致性」。

D9 的理由：纯结构推断覆盖不到「没有任何 Lucky 注解但属于组件体系」的类（`ObjectGenerate` 实现、配置引用的 POJO、常量库），也会误判（仅 import 了 `@Param` 的工具类）；显式类型让文件级识别与图标分类都有确定答案，不需要用户记推断规则；展示名放在 Javadoc 里，不再多一个名称属性。另一个用法是作为元注解标在框架身份注解上（`@HttpRequest`/`@ServerAddressMeta`/`@ApiConfig`/`@SpelConfig`/`@Namespace` 已标），一次标注、所有使用处继承，避免在每个 API 接口上重复；方法级标记则服务「只在入口方法上标」的场景。

D10 的理由：`separatorAttribute = "separator"` 与 `@SpelAttributes#value` 是同一种故障——第二份属性名数据，属性改名时不同步且失败是静默的。`@SpelSeparator` 无属性，注解跟着元素走；分隔符元素只作识别标记，不参与 SpEL，因此不污染 `elements`，也不会让 `KEY_VALUE` 元素多出求值语义。

D11/D12 的理由：`stageAttribute` 与 `nameAttribute` 同属第二份属性名数据，D10 阶段曾以「目标元素可由类型唯一确定」为由暂缓，D11/D12 一并收口。统一后规则只有一套：信息在哪个元素上，就在那个元素上标一个无属性注解；插件在同注解内做单元素查找，不维护任何名字表。代价是 `@Hook#lifecycle`、`MyParam#name` 各多一行注解。

### 5.2 高风险

| ID | 问题 | 影响 | 应对 |
|---|---|---|---|
| R1 | 属性漏标 `@SpelAttribute` | 该属性静默不参与 SpEL | A4 warning；漏标即运行期不参与，补全/检查用例可直接观察到缺失 |
| R2 | 显式漏写 `result`，类型检查退化 | 不再提示结果类型不兼容 | A4 warning；5 个数值属性（`retryCount`/`waitMillis`/`maxWaitMillis`/`minWaitMillis`/`multiplier`）单列验收 |
| R3 | 把「元注解链回溯」误当成继承删掉 | `@Get`/`@Post` 等 9 个组合注解失效 | PR4 单列 3 个用例：组合注解、二级组合注解、`@AliasFor` |
| R4 | 把 `@AliasFor` effective attribute 误删 | `@Get#value` → `@HttpRequest#url` 语义丢失 | 复用现有 `SpelAnnotationResolver`（229 行，已实现多级别名链） |
| R5 | `SpelKind.INHERIT` / `Lifecycle.INHERIT` 删除波及插件 | `SpelLifecycle` 出现在 23 个插件文件、110 处；`SpelValueKind` 在 8 个文件、37 处 | 先做枚举映射表再改实现；`LifecycleTable.rank()` 的 `when` 同步补分支 |
| R6 | 运行期扫描不到框架注解 | 未迁移注解从「降级 UNKNOWN」变成「跳过」，误报增加 | 依赖缺失/索引未就绪时降级为空符号表；A7 审计兜底 |
| R7 | 两个 `@Param` 同名 | `generalapi.ai.tool.Param` 被误当注入点 | 按全限定名白名单匹配 |
| R8 | 运行时扫描结果不完整（框架依赖未配置、索引未建立、方法体不可读） | 补全与检查缺符号，表现为「什么都没提示」 | dumb mode 返回空快照不阻塞；`runCatching` 容错；方法体不可读回退 `DEFAULT_FUNCTION_CLASSES`；文档提示先配置框架依赖 |

### 5.3 中低风险

| ID | 问题 | 影响 | 应对 |
|---|---|---|---|
| R9 | 自定义 Javadoc tag（`@spelType`/`@spelStage`/`@sample`/`@key`）未注册到 `maven-javadoc-plugin` | 发布 javadoc 静默丢失 | 给 `lucky-httpclient/pom.xml` 补 `<tag>` |
| R10 | 现有 Javadoc 偏简略 | 悬停体验退化 | PR0 定规范：第一段一句话 + `@param` + `<pre>` 示例 |
| R11 | `result` 候选中含 `String` 导致检查恒真 | `@ConvertMetaType#type` 的 5 候选实际等于不检查 | 按「候选集合」校验，补全取公共成员 |
| R12 | `parseExpression` 统计口径不一致 | 文档写 284，实测 344 处 | 统一口径写进 PR0 基线 |
| R13 | 补全 P95 延迟无基线 | 无法验收 | PR0 建立基线 |
| R14 | 插件 `SpelLifecycle` 与框架 `Lifecycle` 双枚举 | 长期需要映射维护 | 映射集中到 `LifecycleMapper`，未知值降级 `UNKNOWN` |
| R15 | 用户项目自定义注解需要同一套规则 | 两处实现容易分叉 | 解析规则只实现一份（`SpelMetaAnnotationReader` + `SpelAnnotationResolver`），框架与用户注解走同一条路径 |
| R16 | 声明量增加 24% | 迁移工作量上升 | 最简写法（不写参数）+ IDE 注解模板 + PR2/PR9 分批 |
| R17 | 同一注解内多个属性的 `stage`/`result` 不一致（A4 只能提示） | 同类属性行为不一致 | A4 报告逐条 review；变更记录写明理由 |
| R18 | 元注解链同名匹配误复用（A8） | 用户注解的 `name` 碰巧与框架注解同名 | A8 warning；只在元素既无注解也无别名时启用；回溯深度上限 16，`java.lang.annotation.*` 排除 |
| R19 | 显式类型与结构不匹配（标了 `API` 但没有 `@HttpRequest`） | 图标分类可能名不副实 | 声明优先、不静默覆盖；运行期保留 `declared` 供排查 |
| R20 | 文件图标在 IDEA 2026.3 延迟显示 | 标注了也没立即出现四叶草 | 沿用 `iconProvider` + `fileIconProvider` 双 EP；不依赖单 EP 的延迟行为 |
| R21 | `@LuckyComponent` 被误当成 Spring `@Component` | 用户以为会注册 Bean | 文档与 Javadoc 明确：不参与容器，只供工具识别 |
| R22 | 展示名依赖 Javadoc 首句，改注释会改列表展示 | 组件名不稳定 | 首句规则固定（截止句号/换行），PR0 定 Javadoc 规范；不新增自定义名称 tag |
| R23 | 5 套图标资源命名不一致或色值偏差 | 同一类型在不同视图颜色不同 | 资源集中 `icons/`，`type → 资源`只写一份映射，PR4 截图比对 |
| R24 | `@SpelSeparator` 漏标或标错元素 | `KEY_VALUE` 不拆分，补全退化为整串 | `@SpelAttribute.separator` 字面量兜底（字面量优先）；A11/A12 校验唯一性与类型 |
| R25 | `@SpelStage` 漏标 | `enable` 等属性按 `METHOD`，阶段过滤变宽松（不误报，但不提示阶段缺失） | A18 提示注解内无消费者；模板新增注解时带上 `@SpelStage` |
| R26 | `@SpelName` 漏标 | 元素不叫 `value` 时变量名读不到 | A17 提示无 `@SpelSymbol` 宿主；回退 `value` 兜底常规写法 |
| R27 | 注解元素（注解属性）上误标 `@LuckyComponent` | 被误当成组件方法 | 方法识别只对普通类生效，注解类型按注解规则处理；审计提示并忽略 |

v3 的 R16（PSI 多组截断）随 D8 一并消失：`@SpelAttribute` 不可重复，`findAnnotation` 单条读取足够。

### 5.4 已确认（前序评估结论，v5 继续沿用）

| ID | 结论 |
|---|---|
| C-01 | 框架注解全量集合由运行期扫描得到，不随标注与否变化，也不落盘 |
| C-02 | `@Param` 必须按全限定名匹配，排除 `generalapi.ai.tool.Param` |
| C-03 | 不保留任何预编译 JSON 清单：注解、变量、函数、组件全部运行期解析；框架还没有外部用户，不做兼容层 |
| C-04 | 框架 `Lifecycle` 不新增值；`UNKNOWN` 只存在插件侧 |
| C-05 | `@SpelSymbol.until` 的默认 `NON` 按“无上界”解释，与 `@Hook#lifecycle` 的“不执行”分开处理 |
| C-06 | 函数引用不新增注解，统一用 `kind=FUNCTION` |
| C-07 | Hook 阶段用 `@SpelStage` 标在 `lifecycle` 元素上 |
| C-08 | 组合注解与 `@AliasFor` 属于「注解层回溯」，不是属性继承，必须保留 |

### 5.5 决策带来的额外约束

1. **D8 删除类级注解**：68 个注解的 `value()` 白名单全部删除，改为逐属性标注；`@Mock`、`@RespConvert` 等属性多的注解声明会变长，但不引入新规则。
2. **D4 `@SpelConfig` 去隐式**：232 个配置字段中只标 30–50 个支持 SpEL 的，每个显式写 `stage = Lifecycle.CONFIG_API_INIT`。
3. **D3 默认 `METHOD`**：漏写 `stage` 的合法解读是「不过滤」，A4 只提示不阻断。
4. **别名唯一声明**：同一 effective attribute 只标一处；`@AliasFor` 两端都标触发 A1。
5. **`@SpelAttribute` 不可重复**：一个元素最多一条；重复书写是编译错误，不需要运行时去重。
6. **D9 组件身份独立**：`@LuckyComponent` 只给 IDEA 插件读，不进 `spelAnnotations`，不写 `stage`/`result`/`kind`；可以标在类上、方法上或作为元注解（5 个框架身份注解已标）；`type` 只决定图标，不决定 SpEL 行为；展示名只读 Javadoc。
7. **D10 分隔符元素化**：`@SpelSeparator` 不进 `spelAnnotations.elements`，只影响同注解内 `KEY_VALUE` 元素的拆分；字面量 `separator` 与元素同时存在时字面量优先，不报错；分隔符查找不跨元注解链。
8. **D11 阶段元素化**：`@SpelStage` 不进 `elements`，只影响同注解内未显式书写 `stage` 的 `@SpelAttribute`；显式 `stage` 仍优先；阶段查找不跨元注解链，`@Callback`/`@Val`/`@Pack` 靠 `@AliasFor` 指向 `@Hook` 声明端。
9. **D12 名称元素化**：`@SpelName` 不进 `variables`，只在 `@SpelSymbol` 标在注解类型时被读取；`@SpelSymbol.name` 显式优先，无 `@SpelName` 时回退 `value`，不回退参数名。

---

## 6. 迁移映射表（v2 → v5）

| 注解 | v2 写法 | v5 写法 |
|---|---|---|
| `@HttpRequest` | `@Spel(only={"url"}, stage=REQUEST_INIT, result=String.class)`<br>`@Spel(kind=FUNCTION, result=String.class) String func()` | `@SpelAttribute(stage=REQUEST_INIT, result=String.class) String url()`<br>`@SpelAttribute(kind=FUNCTION, stage=REQUEST_INIT, result=String.class) String func()` |
| `@RetryMeta` | `@Spel(only={"name"}, stage=RETRY, result=String.class)`<br>`@Spel(kind=CONDITION, result=boolean.class) String enable()`<br>`@Spel(result=Integer.class) String retryCount()` | `@SpelAttribute(stage=RETRY, result=String.class) String name()`<br>`@SpelAttribute(kind=CONDITION, stage=RETRY, result=boolean.class) String enable()`<br>`@SpelAttribute(stage=RETRY, result=Integer.class) String retryCount()` |
| `@Retryable` | `@Spel(result=Long.class) String waitMillis()` 等 | `@SpelAttribute(stage=RETRY, result=Long.class) String waitMillis()` 等 |
| `@ConvertMetaType` | `@Spel(only={"type","respContentType"}, stage=RESPONSE, ...)` | `@SpelAttribute(stage=RESPONSE, result={...}) String type()`<br>`@SpelAttribute(stage=RESPONSE, result=String.class) String respContentType()` |
| `@ServerAddress` | `@Spel(only={"url","path"}, stage=REQUEST_INIT, result=String.class)`<br>`@Spel(kind=FUNCTION, result=String.class) String urlFunc()` | `@SpelAttribute(stage=REQUEST_INIT, result=String.class) String url()`<br>`@SpelAttribute(kind=FUNCTION, stage=REQUEST_INIT, result=String.class) String urlFunc()` |
| `@Hook` | `@Spel(only={"enable"}, kind=CONDITION, stageAttribute="lifecycle", result=boolean.class)` | `@SpelAttribute(kind=CONDITION, result=boolean.class) String enable()`<br>`@SpelStage Lifecycle lifecycle() default Lifecycle.NON` |
| `@SpELImport` | `@Spel(only={...}, kind=KEY_VALUE, stage=METHOD, separator="=")` | `@SpelAttribute(kind=KEY_VALUE, stage=METHOD, valueSpel=false) String[] rootLit()` 等 4 条<br>`@SpelSeparator String separator()` |
| `@ChunkUpload` | 类型级 `@Spel(only={8 个属性}, ...)` + 5 个属性级覆盖 | 8 条 `@SpelAttribute`，各自显式 `stage=REQUEST_INIT` |
| 配置字段 | `@Spel(result=Integer.class) private String maxCount;` | `@SpelAttribute(stage=CONFIG_API_INIT, result=Integer.class) private String maxCount;` |
| 自定义参数注解 | `@SpelSymbol(nameAttribute="name", kind=PARAM) @interface MyParam { String name(); }` | `@SpelSymbol(kind=PARAM) @interface MyParam { @SpelName String name(); }` |
| 组件类 | 无（不存在） | `@LuckyComponent(type = API) public interface UserApi`；框架身份注解标一次元注解即可；展示名写在 Javadoc 首句 |

注意：

- `@ConvertMetaType#type` 不再需要「从白名单里移除」这一步；
- `@ChunkUpload#value` 和 `@HttpRequest#value` 都是别名端，只标声明端 `file`/`url`，否则触发 A1；
- `@Callback`/`@Val`/`@Pack` 的 `enable`/`lifecycle` 走 `@AliasFor` 指向 `@Hook`，不重复标注；`@Hook#lifecycle` 只标 `@SpelStage`。
- `@SpELImport#separator` 只标 `@SpelSeparator`，不再在 `@SpelAttribute` 里写元素名。
- 自定义参数注解的 `name` 元素只标 `@SpelName`，不再写 `@SpelSymbol.nameAttribute`；元素名仍是 `value` 时可以都不标。
- 组件识别入口：类上直接标 > 类上元注解链 > 方法（含方法注解链）；`@HttpRequest`/`@ServerAddressMeta`/`@ApiConfig`/`@SpelConfig`/`@Namespace` 已作为元注解标好，普通 API 接口不需要再标。

---

## 7. 验收清单

模型：

- [ ] 每个参与 SpEL 的属性，都是因为元素上有 `@SpelAttribute`；不存在任何字符串白名单。
- [ ] 删掉一条 `@SpelAttribute`，该属性立即不参与；加上立即参与。
- [ ] 把 `@HttpRequest#url` 改名为 `uri` 后，注解跟着方法走，仍参与 SpEL，不存在「清单未更新导致静默失效」。
- [ ] 没有任何解析路径会从其他声明读取字段填充当前注解（无继承）。
- [ ] `@HttpRequest#func` 显式写 `stage = REQUEST_INIT`；删掉该字段后断言退化为 `METHOD`。
- [ ] `@Get`/`@Post` 等组合注解通过 `@AliasFor` 拿到 `@HttpRequest` 的声明。
- [ ] `@AliasFor` 两端都标触发 A1；只标声明端正常工作。
- [ ] `SpelKind`/`Lifecycle` 中不存在 `INHERIT`。
- [ ] `@SpelAttribute` 无 `all`/`disabled`/`only`/`value`/`separatorAttribute`/`stageAttribute`/分组属性。
- [ ] `@SpelConfig` 无 `stage`、无 `valueSpel`。
- [ ] `@SpelSeparator` 无属性，只能标在 `String` 元素上；同一注解内最多一个。
- [ ] `@SpelStage` 无属性，只能标在 `Lifecycle` 元素上；同一注解内最多一个；所在注解无 `@SpelAttribute` 触发 A18。
- [ ] `@SpelName` 无属性，只能标在 `String` 元素上；同一注解内最多一个；所在注解无 `@SpelSymbol` 触发 A17。
- [ ] 把 `@Hook#lifecycle` 改名为 `stage` 后，`@SpelStage` 跟着方法走，`enable` 的阶段仍跟随。
- [ ] 把 `@MyParam#name` 改名为 `key` 后，`@SpelName` 跟着方法走，变量名仍正确。
- [ ] 把 `@SpELImport#separator` 改名后，`@SpelSeparator` 跟着方法走，`KEY_VALUE` 拆分仍生效。
- [ ] `@SpelAttribute.separator` 字面量与 `@SpelSeparator` 元素同时存在时，字面量优先。
- [ ] `@LuckyComponent` 只有 `type()`，无 `value`/`name`/`kind`/`stage`；框架运行期不读取。
- [ ] `@Target` 为 `TYPE`/`ANNOTATION_TYPE`/`METHOD`；类上、类上元注解链、方法上三处任一命中即为组件。
- [ ] `LuckyComponentType` 只含 `AUTO`/`API`/`CONFIG`/`FUNCTION`/`SUPPORT`，`OTHER` 只在插件侧。
- [ ] `type = API/CONFIG/FUNCTION/SUPPORT` 时直接使用声明值，不做推断（`source = class`）。
- [ ] `@HttpRequest`/`@ServerAddressMeta`/`@ApiConfig`/`@SpelConfig`/`@Namespace` 已作为元注解标好；`@Get` 接口与 `@Get` 方法的识别结果为 `API`（`source = meta`）。
- [ ] 方法上标注、类上无标注时类仍为组件（`source = method`），方法记入 `methods`。
- [ ] 同一个类命中多个不同的具体 `type` 时触发 A9，取优先级最高的值；不继承（子类不受影响）、不注册 Bean。
- [ ] `AUTO` 与未标注类仍能得到推断结果，`AUTO` 推不出时触发 A10。

数据：

- [ ] 仓库中不存在 `spel/metadata.json`、`spel/annotations.json`、`spel/builtin-symbols.json` 与 `tools/generate-*.js`，插件运行期与测试期都不读取任何 JSON 清单。
- [ ] 注解属性语义来自元素上的 `@SpelAttribute`；不存在 `schemaVersion`、`knownAnnotations`、`explicit` 等序列化字段。
- [ ] 分隔符/阶段/名称来源由 `@SpelSeparator`/`@SpelStage`/`@SpelName` 在同注解内定位，不存在记录这些元素名的文件。
- [ ] 内置变量来自全工程 `@SpelSymbol` 字段扫描，新增字段后无需任何生成步骤即可补全。
- [ ] 默认注册函数来自 `importCommonFunction()` 的 `addSpringElFunctionClass(...)`；新增函数类后无需重新生成即可补全。
- [ ] 组件清单来自 `@LuckyComponent` 的运行时扫描（`class`/`title`/`type`/`declared`/`source`，`source = method` 时含 `methods`）。
- [ ] 两个 `@Param` 按全限定名区分，`generalapi.ai.tool.Param` 不产生任何 SpEL 行为。
- [ ] 框架 `Lifecycle` 未新增枚举值；`@SpelSymbol.until` 用 `NON` 表示无上界；`UNKNOWN` 只在插件侧。
- [ ] 不存在任何 JSON 回退路径：扫描不到框架依赖时降级为空符号表，而不是读清单兜底。
- [ ] 审计 A1–A19 全部有对应用例；A3/A4/A8/A9/A10/A13/A17/A18/A19 只出 warning，不阻断构建。

功能：

- [ ] 多结果类型按 `result ∪ typeExpression` 的「任一候选通过」检查；`constraint` 长度错误与 `typeExpression` 解析失败触发 A5。
- [ ] 无结果约束时（仅 `constraint=BINARY`）能正常校验。
- [ ] `urlFunc` 支持字面量函数名和 `#{...}` 动态函数名。
- [ ] 函数参数带注入注解时按表达式注入，不带时按类型注入，都不满足时提示。
- [ ] `@Hook#enable` 阶段跟随 `lifecycle`；把 `@SpelStage` 移到另一个 `Lifecycle` 元素后行为同步变化。
- [ ] `@Val` 字段值按 SpEL 解析，`@Pack` 不解析。
- [ ] 配置 key 按 `naming` 生成。
- [ ] `API`/`CONFIG`/`FUNCTION`/`SUPPORT` 显示四套不同图标与颜色；`AUTO` 推不出时显示通用图标；类上、方法上、元注解上全部取消后图标消失。
- [ ] 组件展示名取 Javadoc 首句，无注释时用类名。
- [ ] 组件清单能按 `type` 分组、按 `source` 标注识别入口，并对 `AUTO`/未标注类给出推断结果。
- [ ] `@SpelSeparator` 元素改名/删除时，A11/A12 能报出具体注解与元素。
- [ ] `@SpelStage`/`@SpelName` 元素改名/删除时，A6/A14–A18 能报出具体注解与元素。
- [ ] Javadoc 缺失时降级不报错。
- [ ] 补全 P95 延迟不超过迁移前 10%。
- [ ] 全量插件用例全绿，`buildPlugin` 通过。

---

## 8. 既有文档同步结果

三篇文档已统一为 v5（7 注解、逐属性标注、无白名单、无继承、元素标记、组件显式身份与类型）：

| 文件 | 变更 | 结果 |
|---|---|---|
| `spel-metadata-annotations.md` | 第 2 章信息源加 `@SpelStage`/`@SpelName`、列表改「7 个注解」；第 4 章注解总览加两行；第 6 章改「`@SpelAttribute` 与元素标记注解」、删 `stageAttribute`、6.5 拆三个子节；6.5.2 补「为何可做注解级默认」与判定标准；第 8 章 `@SpelSymbol` 删 `nameAttribute`；12.5 审计 A6 改写 + A14–A18；13.1 加 `stageElement`/`nameElement`；14 对比表「字符串属性引用」降为 0；15/16/17 同步 | 1345 → 1451 行 |
| `spel-metadata-annotations-usage.md` | 1.1 改七注解；1.2 加阶段/名称规则；3.1 用法七/八改为元素标记；3.2 用法五改 `@SpelName`；5.3 改动量重算（50 文件 / 约 2285 行、16–21 人天）；5.7/5.9/6/7/8 同步 | 816 → 833 行 |
| 本文 | 标题与状态改 D1–D12；2.1 七注解；2.2 删 `stageAttribute` 并加 `@SpelStage`/`@SpelName` 两节；2.5 五个辅助步骤；3.x/4.x 可行性与工作量；4.3 运行期数据模型；4.4 A14–A18；5.1 D11/D12、5.3 R25/R26、5.5 第 8–9 条；6 迁移表；7 验收 | 545 → 640 行 |

全文已确认无旧语法残留：`@SpelAttributes`、`typeLevel`、`@Repeatable`、`C1`–`C9` 只出现在「已删除说明」或「v3 对比列」；`separatorAttribute`/`stageAttribute`/`nameAttribute` 只出现在「D10–D12 / 已删除说明 / 验收断言 / v4 对比列」。

### 8.1 本轮对齐：`@LuckyComponent` 是 IDEA 插件专用标记

三篇文档又按同一口径对齐了一次：**`@LuckyComponent` 只给 IDEA 插件读，用于文件标记、聚合、搜索、调试；插件检测类上、类上元注解链、类上方法三处，任一处命中即为组件。**

| 文件 | 本轮变更 | 结果 |
|---|---|---|
| `spel-metadata-annotations.md` | 第 2/3/4/5.6 章加「插件专用 + 类/方法/元注解三入口」；第 10 章整节重写（识别入口、四级优先级、元注解继承表、`source`/`methods` 运行期字段）；12.5 A9 改为类型冲突告警；13.1/14/15/16/17 同步 | 已同步 |
| `spel-metadata-annotations-usage.md` | 1.1/1.2 加标注位置；3.5 重写为「标注位置与六种用法」（新增元注解与方法级两个用法）；5.2/5.7/5.9/6/8 同步 | 已同步 |
| 本文 | 2.1/2.3/2.5 改三入口与优先级；3.5/4.3/4.4 同步；5.1 D9、5.3 R27、5.5 第 6 条；6/7 验收 | 已同步 |

本轮同时确认：`@LuckyComponent` 的 `@Target` 为 `TYPE`/`ANNOTATION_TYPE`/`METHOD`；框架已在 `@HttpRequest`、`@ServerAddressMeta`、`@ApiConfig`、`@SpelConfig`、`@Namespace` 五个身份注解上标好，`SUPPORT` 只走结构推断；文档不再出现「不传播」的旧描述。

### 8.2 本轮收口：预编译 JSON 全部删除，改为运行期解析

| 项 | 变更 |
|---|---|
| 资产 | `spel/metadata.json`、`spel/annotations.json`、`spel/builtin-symbols.json`、`tools/generate-*.js` 全部删除 |
| 加载器 | `AnnotationMetadataLoader`、`AnnotationTable`、`BuiltinSymbols`、`FrameworkVersionDetector` 删除 |
| 读取端 | `SpelMetaAnnotationReader`（注解 PSI 直读）+ `SpelAnnotationResolver`（别名/元注解链）+ `SpelRuntimeSymbols`（`@SpelSymbol` 变量、`addSpringElFunctionClass` 函数、`@LuckyComponent` 组件） |
| 符号合并 | `SpelContextBuilder` 仍按「参数 > 方法级导入 > 类级导入 > 运行期符号 > 全局」分层，其中不再有 JSON 数据源 |
| 测试 | `AnnotationMetadataGoldenTest`、`BuiltinSymbolGoldenTest` 删除；桩改为真实框架声明形态（含跨注解 `@AliasFor`） |
| 兼容 | 不保留 JSON 回退路径，框架尚无外部用户 |
| 验证 | 仓库内无 JSON 清单与生成工具；全量插件用例通过 |
