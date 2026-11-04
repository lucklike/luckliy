package com.luckyframework.httpclient.proxy.generator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * 根据Map结构生成Java实体类的工具类
 * 支持嵌套Map和List<Map>结构
 * 支持多种序列化框架注解
 */
public class EntityGeneratorFromMap {

    // 基本类型的包装类映射
    private static final Map<Class<?>, String> WRAPPER_TYPE_MAP = new HashMap<>();
    // 类型简单名对应的全限定名，用于输出import
    private static final Map<String, String> TYPE_FQCNS = new HashMap<>();
    // 无需import的类型（java.lang 与基本类型）
    private static final Set<String> JAVA_LANG_TYPES = new HashSet<>();
    // Java关键字与保留字，报文字段名撞上时需避让
    private static final Set<String> JAVA_KEYWORDS = new HashSet<>();
    // 数值类型的提升顺序，rank越大越宽
    private static final Map<String, Integer> NUMERIC_RANK = new HashMap<>();
    // 结构指纹递归的最大深度，防止报文存在环形引用时无限递归
    private static final int MAX_FINGERPRINT_DEPTH = 8;

    static {
        WRAPPER_TYPE_MAP.put(String.class, "String");
        WRAPPER_TYPE_MAP.put(Integer.class, "Integer");
        WRAPPER_TYPE_MAP.put(int.class, "int");
        WRAPPER_TYPE_MAP.put(Long.class, "Long");
        WRAPPER_TYPE_MAP.put(long.class, "long");
        WRAPPER_TYPE_MAP.put(Double.class, "Double");
        WRAPPER_TYPE_MAP.put(double.class, "double");
        WRAPPER_TYPE_MAP.put(Boolean.class, "Boolean");
        WRAPPER_TYPE_MAP.put(boolean.class, "boolean");
        WRAPPER_TYPE_MAP.put(Date.class, "Date");
        WRAPPER_TYPE_MAP.put(BigDecimal.class, "BigDecimal");
        WRAPPER_TYPE_MAP.put(BigInteger.class, "BigInteger");

        TYPE_FQCNS.put("Date", "java.util.Date");
        TYPE_FQCNS.put("BigDecimal", "java.math.BigDecimal");
        TYPE_FQCNS.put("BigInteger", "java.math.BigInteger");
        TYPE_FQCNS.put("List", "java.util.List");
        TYPE_FQCNS.put("Map", "java.util.Map");

        JAVA_LANG_TYPES.addAll(Arrays.asList("String", "Integer", "Long", "Boolean", "Double", "Float",
                "Short", "Byte", "Character", "Object", "Number", "Iterable", "Runnable", "Thread",
                "int", "long", "double", "boolean", "float", "short", "byte", "char", "void"));

        JAVA_KEYWORDS.addAll(Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
                "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private", "protected", "public", "return",
                "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while", "true", "false", "null"));

        NUMERIC_RANK.put("Byte", 1);
        NUMERIC_RANK.put("Short", 2);
        NUMERIC_RANK.put("Integer", 3);
        NUMERIC_RANK.put("Long", 4);
        NUMERIC_RANK.put("BigInteger", 5);
        NUMERIC_RANK.put("Float", 6);
        NUMERIC_RANK.put("Double", 7);
        NUMERIC_RANK.put("BigDecimal", 8);
    }

    private String packageName = "com.example.entity";
    private String indent = "    ";
    private boolean useLombok = true;
    private boolean useSwagger = true;
    private NestedClassStrategy nestedStrategy = NestedClassStrategy.INNER_CLASS;
    private FieldNamingStrategy fieldNamingStrategy = FieldNamingStrategy.ORIGINAL;
    private SerializationFramework serializationFramework = SerializationFramework.NONE;

    /**
     * 主类需要继承的父类（全限定名或简单类名），为空表示不继承
     */
    private String extendClass = "";

    /**
     * 主类需要实现的接口列表（全限定名或简单类名）
     */
    private String[] implementInterfaces = new String[0];

    /**
     * 写入主类Javadoc的来源说明（如产生本次生成的接口方法）
     */
    private String sourceDescription = "";

    /**
     * 每个类的「成员代码块」文本，按类名缓存。
     * 内部类拼装时直接取用，避免从完整类源码里反向抽取成员（文本回抽对注释内容敏感，容易错切）
     */
    private final Map<String, String> memberCodes = new LinkedHashMap<>();

    /**
     * 已占用类名及其结构指纹。用于识别「同名但结构不同」的嵌套字段，
     * 以前提到同名就直接复用已有类，后一个结构的字段会被静默丢弃
     */
    private final Map<String, String> classShapes = new HashMap<>();

    /**
     * 是否让主类实现 java.io.Serializable并补 serialVersionUID
     */
    private boolean serializable = false;

    public void setSerializable(boolean serializable) {
        this.serializable = serializable;
    }

    public EntityGeneratorFromMap() {
    }

    public EntityGeneratorFromMap(String packageName) {
        this.packageName = packageName;
    }

    public EntityGeneratorFromMap(String packageName, boolean useLombok, boolean useSwagger) {
        this.packageName = packageName;
        this.useLombok = useLombok;
        this.useSwagger = useSwagger;
    }

    public EntityGeneratorFromMap(String packageName, boolean useLombok, boolean useSwagger,
                                  NestedClassStrategy nestedStrategy) {
        this.packageName = packageName;
        this.useLombok = useLombok;
        this.useSwagger = useSwagger;
        this.nestedStrategy = nestedStrategy;
    }

    public EntityGeneratorFromMap(String packageName, boolean useLombok, boolean useSwagger,
                                  NestedClassStrategy nestedStrategy,
                                  FieldNamingStrategy fieldNamingStrategy) {
        this.packageName = packageName;
        this.useLombok = useLombok;
        this.useSwagger = useSwagger;
        this.nestedStrategy = nestedStrategy;
        this.fieldNamingStrategy = fieldNamingStrategy;
    }

    public EntityGeneratorFromMap(String packageName, boolean useLombok, boolean useSwagger,
                                  NestedClassStrategy nestedStrategy,
                                  FieldNamingStrategy fieldNamingStrategy,
                                  SerializationFramework serializationFramework) {
        this.packageName = packageName;
        this.useLombok = useLombok;
        this.useSwagger = useSwagger;
        this.nestedStrategy = nestedStrategy;
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.serializationFramework = serializationFramework;
    }

    // 在 EntityGeneratorFromMap 类中，现有的 generateEntity 方法之后添加以下新方法

    /**
     * 设置主类需要继承的父类
     *
     * @param extendClass 父类全限定名或简单类名，空串表示不继承
     */
    public void setExtendClass(String extendClass) {
        this.extendClass = extendClass == null ? "" : extendClass;
    }

    /**
     * 设置主类需要实现的接口
     *
     * @param implementInterfaces 接口全限定名或简单类名数组
     */
    public void setImplementInterfaces(String[] implementInterfaces) {
        this.implementInterfaces = implementInterfaces == null ? new String[0] : implementInterfaces;
    }

    /**
     * 设置写入主类Javadoc的来源说明
     *
     * @param sourceDescription 来源说明文本
     */
    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription == null ? "" : sourceDescription;
    }

    /**
     * 根据Map生成实体类代码（包括所有嵌套类）- 返回JavaCode对象
     * @param map 数据Map
     * @param className 实体类名
     * @return JavaCode对象，SEPARATE_CLASS模式下返回第一个类（主类）的JavaCode，其他类需要通过generateJavaCodeList获取
     */
    public JavaCode generateJavaCode(Map<String, Object> map, String className) {
        List<JavaCode> javaCodes = generateJavaCodeList(map, className);
        return javaCodes.isEmpty() ? null : javaCodes.get(0);
    }

    /**
     * 根据Map生成实体类代码（包括所有嵌套类）- 返回JavaCode列表
     * @param map 数据Map
     * @param className 实体类名
     * @return JavaCode列表，每个元素代表一个独立的Java类
     */
    public List<JavaCode> generateJavaCodeList(Map<String, Object> map, String className) {
        return doGenerateJavaCodeList(singleSamples(map), className);
    }

    /**
     * 根据样本列表生成实体类代码：合并列表中所有Map元素的字段，
     * 不再只拿第一个元素建模（第一个元素缺少的字段会整块丢失）
     *
     * @param mapList   样本列表，非Map元素会被忽略
     * @param className 实体类名
     * @return JavaCode列表
     */
    public List<JavaCode> generateJavaCodeList(List<?> mapList, String className) {
        return doGenerateJavaCodeList(mergeElementMaps(mapList), className);
    }

    /**
     * 生成JavaCode列表的实际执行入口
     *
     * @param fieldSamples 字段名到样本值列表的映射
     * @param className    主类类名
     * @return JavaCode列表
     */
    private List<JavaCode> doGenerateJavaCodeList(Map<String, List<Object>> fieldSamples, String className) {
        Map<String, String> allClasses = new LinkedHashMap<>();
        Map<String, Set<String>> classDependencies = new HashMap<>();
        Map<String, Set<String>> classImports = new HashMap<>();
        memberCodes.clear();
        classShapes.clear();

        generateEntityRecursive(fieldSamples, className, allClasses, classDependencies, classImports, null);

        List<JavaCode> result = new ArrayList<>();

        if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
            // 独立类策略：每个类生成独立的JavaCode
            List<String> orderedClasses = orderClassesByDependency(allClasses.keySet(), classDependencies);
            for (String clsName : orderedClasses) {
                String classCode = allClasses.get(clsName);
                if (classCode != null) {
                    result.add(JavaCode.of(packageName, clsName, classCode));
                }
            }
        } else {
            // 内部类策略：只生成一个JavaCode，包含所有内部类
            result.add(JavaCode.of(packageName, className, buildInnerClassResult(className, allClasses, classImports)));
        }

        return result;
    }

    /**
     * 根据依赖关系对类进行排序（被依赖的类排在前面）
     */
    private List<String> orderClassesByDependency(Set<String> classNames, Map<String, Set<String>> classDependencies) {
        List<String> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String className : classNames) {
            if (!visited.contains(className)) {
                topologicalSort(className, classDependencies, visited, ordered);
            }
        }

        return ordered;
    }

    /**
     * 拓扑排序DFS实现
     */
    private void topologicalSort(String className, Map<String, Set<String>> classDependencies,
                                 Set<String> visited, List<String> ordered) {
        visited.add(className);

        Set<String> dependencies = classDependencies.get(className);
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (!visited.contains(dep)) {
                    topologicalSort(dep, classDependencies, visited, ordered);
                }
            }
        }

        ordered.add(className);
    }


    /**
     * 根据Map生成实体类代码（包括所有嵌套类）
     * @param map 数据Map
     * @param className 实体类名
     * @return 生成的Java代码，如果使用独立类策略，返回多个类用分隔符分隔
     */
    public String generateEntity(Map<String, Object> map, String className) {
        return doGenerateEntity(singleSamples(map), className);
    }

    /**
     * 根据样本列表生成实体类代码，合并所有Map元素的字段
     *
     * @param mapList   样本列表，非Map元素会被忽略
     * @param className 主类类名
     * @return 生成的Java代码
     */
    public String generateEntity(List<?> mapList, String className) {
        return doGenerateEntity(mergeElementMaps(mapList), className);
    }

    /**
     * 生成实体类代码的实际执行入口
     *
     * @param fieldSamples 字段名到样本值列表的映射
     * @param className    主类类名
     * @return 生成的Java代码
     */
    private String doGenerateEntity(Map<String, List<Object>> fieldSamples, String className) {
        Map<String, String> allClasses = new LinkedHashMap<>();
        Map<String, Set<String>> classDependencies = new HashMap<>();
        Map<String, Set<String>> classImports = new HashMap<>();
        memberCodes.clear();
        classShapes.clear();

        generateEntityRecursive(fieldSamples, className, allClasses, classDependencies, classImports, null);

        // 根据策略返回结果
        if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
            return buildSeparateClassResult(allClasses);
        } else {
            return buildInnerClassResult(className, allClasses, classImports);
        }
    }



    /**
     * 递归生成实体类及所有嵌套类
     *
     * @param fieldSamples    当前结构体的「字段名 - 样本值列表」映射
     * @param className       本次生成的类名（已消歧）
     * @param allClasses      已生成类源码缓存
     * @param classDependencies 类依赖缓存
     * @param classImports    类import缓存
     * @param parentClassName 父类类名，主类为null
     */
    private void generateEntityRecursive(Map<String, List<Object>> fieldSamples, String className,
                                         Map<String, String> allClasses,
                                         Map<String, Set<String>> classDependencies,
                                         Map<String, Set<String>> classImports,
                                         String parentClassName) {
        if (allClasses.containsKey(className)) {
            return;
        }

        // 主类无父级；内部类指 INNER_CLASS 策略下嵌套于主类之中的类
        boolean isMainClass = parentClassName == null;
        boolean isInnerClass = !isMainClass && nestedStrategy == NestedClassStrategy.INNER_CLASS;

        StringBuilder code = new StringBuilder();

        // 分析字段类型，并在遍历中顺带生成嵌套类
        Map<String, FieldInfo> fields = analyzeFields(fieldSamples, className, allClasses, classDependencies, classImports, parentClassName);

        // import 基于已确定的字段类型收集，不再回头扫描原始Map
        Set<String> imports = collectImports(fields.values(), isMainClass);
        classImports.put(className, imports);

        // 生成包名和导入（独立类需要，内部类在主类中统一处理）
        if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
            code.append("package ").append(packageName).append(";\n\n");
            appendImports(code, imports);
        }

        // 类体一律以「零缩进平铺」形式生成，缩进完全交由拼装阶段统一施加，
        // 避免以前「生成时缩进一次 + 拼装时又缩进一次」导致内部类多缩进一级
        code.append(generateClassJavadoc(className, isMainClass));
        code.append(generateClassAnnotations(className, isMainClass));
        code.append(isInnerClass ? "public static class " : "public class ")
                .append(className)
                .append(isMainClass ? buildExtendsAndImplements() : "")
                .append(" {\n");

        StringBuilder members = new StringBuilder();
        // 实现Serializable时补 serialVersionUID，避免序列化实体的默认值告警
        if (isMainClass && serializable) {
            members.append("private static final long serialVersionUID = 1L;\n\n");
        }
        for (FieldInfo field : fields.values()) {
            members.append(generateFieldDefinition(field, 1));
        }
        // 不使用Lombok时，生成getter/setter和toString
        members.append(generateAccessors(className, fields.values()));
        String memberCode = normalizeMemberBlock(members);
        code.append(memberCode);

        code.append("}\n");

        allClasses.put(className, code.toString());
        memberCodes.put(className, memberCode);
    }

    /**
     * 生成成员方法部分：不使用Lombok时输出getter/setter与toString
     *
     * @param className 当前类名
     * @param fields    字段集合
     * @return 成员方法代码，使用Lombok时为空串
     */
    private String generateAccessors(String className, Collection<FieldInfo> fields) {
        if (useLombok) {
            return "";
        }
        StringBuilder code = new StringBuilder();
        for (FieldInfo field : fields) {
            code.append(generateGetterSetter(field, 1));
        }
        code.append(generateToString(className, fields, 1));
        return code.toString();
    }

    /**
     * 规整成员代码块：连续空行压成一个，并去除首尾多余空行，保证类体上下边框一致
     *
     * @param members 原始成员代码
     * @return 规整后的成员代码，无成员时返回空串
     */
    private String normalizeMemberBlock(StringBuilder members) {
        if (members.length() == 0) {
            return "";
        }
        String[] lines = members.toString().split("\n", -1);
        StringBuilder sb = new StringBuilder();
        boolean lastBlank = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (!lastBlank && sb.length() > 0) {
                    sb.append('\n');
                }
                lastBlank = true;
            } else {
                sb.append(line).append('\n');
                lastBlank = false;
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.length() == 0 ? "" : sb.toString() + "\n";
    }

    /**
     * 分析字段类型。入参是「字段名 - 该字段出现过的所有样本值」，
     * 因此List多元素、同字段出现多种类型都能在此合并，不再只按第一个元素建模
     */
    private Map<String, FieldInfo> analyzeFields(Map<String, List<Object>> fieldSamples,
                                                 String currentClassName,
                                                 Map<String, String> allClasses,
                                                 Map<String, Set<String>> classDependencies,
                                                 Map<String, Set<String>> classImports,
                                                 String parentClassName) {
        Map<String, FieldInfo> fields = new LinkedHashMap<>();

        for (Map.Entry<String, List<Object>> entry : fieldSamples.entrySet()) {
            String rawKey = entry.getKey();
            List<Object> samples = entry.getValue();
            Object value = firstNonNull(samples);

            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.originalName = rawKey;
            // 报文key可能含中划线、空格，甚至直接是Java关键字，必须清洗成合法标识符
            fieldInfo.name = sanitizeFieldName(fieldNamingStrategy == FieldNamingStrategy.CAMEL_CASE
                    ? toCamelCase(rawKey) : rawKey);
            fieldInfo.sampleValue = renderSampleValue(value);

            if (value == null) {
                fieldInfo.type = "Object";
                fieldInfo.comment = "// TODO: this field is null in the payload sample, declare its real type according to your business";
            } else if (value instanceof Map) {
                analyzeStructureField(fieldInfo, samples, rawKey, "Info", false, currentClassName,
                        allClasses, classDependencies, classImports);
            } else if (value instanceof List) {
                analyzeListField(fieldInfo, samples, rawKey, currentClassName,
                        allClasses, classDependencies, classImports);
            } else {
                applySimpleType(fieldInfo, samples);
            }

            fields.put(fieldInfo.name, fieldInfo);
        }

        return fields;
    }

    /**
     * 处理结构体型（Map）字段：合并所有样本后递归生成嵌套结构类
     *
     * @param fieldInfo   待填充的字段信息
     * @param structures  结构体样本集合（Map本身或List的元素）
     * @param rawKey      报文中原始key，用于推导嵌套类名
     * @param suffix      嵌套类名后缀，Map结构用Info，List元素用Item
     * @param wrapAsList  是否把结果类型包成 List&lt;X&gt;
     */
    private void analyzeStructureField(FieldInfo fieldInfo, Collection<?> structures, String rawKey, String suffix,
                                       boolean wrapAsList, String currentClassName,
                                       Map<String, String> allClasses,
                                       Map<String, Set<String>> classDependencies,
                                       Map<String, Set<String>> classImports) {
        Map<String, List<Object>> nestedSamples = mergeElementMaps(structures);
        if (nestedSamples.isEmpty()) {
            // 结构信息不足，退化成通用容器类型
            fieldInfo.type = wrapAsList ? "List<Object>" : "Map<String, Object>";
            fieldInfo.comment = wrapAsList
                    ? "// TODO: declare the concrete element type of this List according to your business"
                    : "// TODO: declare the concrete structure of this Map according to your business";
            return;
        }
        String innerClassName = registerInnerClassName(
                sanitizeClassName(capitalizeFirst(toCamelCase(rawKey)) + suffix),
                nestedSamples, currentClassName, classDependencies);
        fieldInfo.type = wrapAsList ? "List<" + innerClassName + ">" : innerClassName;

        generateEntityRecursive(nestedSamples, innerClassName, allClasses, classDependencies, classImports, currentClassName);
    }

    /**
     * 处理List字段：元素全为结构体时生成嵌套类，否则合并元素类型
     */
    private void analyzeListField(FieldInfo fieldInfo, List<Object> samples, String rawKey,
                                  String currentClassName,
                                  Map<String, String> allClasses,
                                  Map<String, Set<String>> classDependencies,
                                  Map<String, Set<String>> classImports) {
        List<Object> elements = flattenSamples(samples);
        if (elements.isEmpty()) {
            fieldInfo.type = "List<Object>";
            fieldInfo.comment = "// TODO: declare the concrete element type of this List according to your business";
            return;
        }

        boolean hasMapElement = false;
        boolean hasSimpleElement = false;
        for (Object element : elements) {
            if (element instanceof Map) {
                hasMapElement = true;
            } else if (element != null) {
                hasSimpleElement = true;
            }
        }

        if (hasMapElement && !hasSimpleElement) {
            analyzeStructureField(fieldInfo, elements, rawKey, "Item", true, currentClassName,
                    allClasses, classDependencies, classImports);
        } else if (!hasMapElement) {
            MergedType merged = mergeType(elements);
            fieldInfo.type = "List<" + merged.type + ">";
            fieldInfo.sampleClass = merged.sampleClass;
            fieldInfo.comment = merged.warning;
        } else {
            fieldInfo.type = "List<Object>";
            fieldInfo.comment = "// TODO: element types of this List are inconsistent across payload samples, please confirm the real type";
        }
    }

    /**
     * 取样本中第一个非null值作为建模代象
     */
    private Object firstNonNull(List<Object> samples) {
        for (Object sample : samples) {
            if (sample != null) {
                return sample;
            }
        }
        return null;
    }

    /**
     * 把单一报文对象包装成「字段名 - 样本值列表」结构
     */
    private Map<String, List<Object>> singleSamples(Map<?, ?> map) {
        Map<String, List<Object>> samples = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            samples.put(String.valueOf(entry.getKey()), Collections.singletonList(entry.getValue()));
        }
        return samples;
    }

    /**
     * 合并多个Map样本，得到「字段名 - 所有出现值」的样本表。
     * 这是List多元素字段取并集的关键：只取第一个元素时，后续元素独有的字段会全部丢失
     */
    private Map<String, List<Object>> mergeElementMaps(Collection<?> maps) {
        Map<String, List<Object>> merged = new LinkedHashMap<>();
        for (Object item : maps) {
            if (!(item instanceof Map)) {
                continue;
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                String key = String.valueOf(entry.getKey());
                List<Object> bucket = merged.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    merged.put(key, bucket);
                }
                bucket.add(entry.getValue());
            }
        }
        return merged;
    }

    /**
     * 把多个List样本的元素平铺到一个列表里
     */
    private List<Object> flattenSamples(List<Object> samples) {
        List<Object> elements = new ArrayList<>();
        for (Object sample : samples) {
            if (sample instanceof List) {
                elements.addAll((List<?>) sample);
            } else if (sample != null) {
                elements.add(sample);
            }
        }
        return elements;
    }

    /**
     * 为嵌套结构确定一个不冲突的类名。
     * 不同层级出现同名字段但结构不同时，直接复用会让后一个结构被静默丢弃，
     * 因此冲突时先加父类名前缀，仍冲突再追加序号
     */
    private String registerInnerClassName(String baseName, Map<String, List<Object>> samples,
                                          String parentClassName, Map<String, Set<String>> classDependencies) {
        String fingerprint = structureFingerprint(samples, 0);
        String candidate = baseName;
        if (!nameAvailable(candidate, fingerprint)) {
            candidate = sanitizeClassName(parentClassName + baseName);
        }
        for (int i = 2; !nameAvailable(candidate, fingerprint); i++) {
            candidate = baseName + i;
        }
        classShapes.put(candidate, fingerprint);
        if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
            classDependencies.computeIfAbsent(parentClassName, k -> new HashSet<>()).add(candidate);
        }
        return candidate;
    }

    /**
     * 类名可占用：尚未被使用，或已被使用且结构完全一致（同构复用同一个类）
     */
    private boolean nameAvailable(String className, String fingerprint) {
        String existing = classShapes.get(className);
        return existing == null || existing.equals(fingerprint);
    }

    /**
     * 生成结构指纹，用于判定两处同名字段是否为同一结构
     */
    private String structureFingerprint(Map<String, List<Object>> samples, int depth) {
        if (depth >= MAX_FINGERPRINT_DEPTH) {
            return "...";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<Object>> entry : samples.entrySet()) {
            parts.add(entry.getKey() + "=" + valueFingerprint(firstNonNull(entry.getValue()), depth + 1));
        }
        Collections.sort(parts);
        return "{" + String.join(",", parts) + "}";
    }

    /**
     * 单个样本值的结构指纹
     */
    private String valueFingerprint(Object value, int depth) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map) {
            return depth >= MAX_FINGERPRINT_DEPTH
                    ? "map"
                    : structureFingerprint(mergeElementMaps(Collections.singletonList(value)), depth);
        }
        if (value instanceof List) {
            List<Object> elements = new ArrayList<>();
            elements.addAll((List<?>) value);
            if (depth >= MAX_FINGERPRINT_DEPTH) {
                return "[list]";
            }
            Map<String, List<Object>> elementSamples = mergeElementMaps(elements);
            return elementSamples.isEmpty()
                    ? "[" + valueFingerprint(firstNonNull(elements), depth + 1) + "]"
                    : "[" + structureFingerprint(elementSamples, depth + 1) + "]";
        }
        return simpleTypeNameOf(value);
    }

    /**
     * 单个样本值对应的Java类型简单名
     */
    private String simpleTypeNameOf(Object value) {
        Class<?> clazz = value.getClass();
        // 浮点统一用BigDecimal，避免金额等场景丢精度
        if (clazz == double.class || clazz == Double.class
                || clazz == float.class || clazz == Float.class) {
            return "BigDecimal";
        }
        String mapped = WRAPPER_TYPE_MAP.get(clazz);
        if (mapped != null) {
            return mapped;
        }
        return clazz.getSimpleName();
    }

    /**
     * 多样本类型合并结果
     */
    private static class MergedType {
        String type;
        String warning;
        Class<?> sampleClass;
    }

    /**
     * 合并多个样本得字段类型：数值按宽度提升，无法合并则降级为Object并给出提示
     */
    private MergedType mergeType(Collection<Object> values) {
        MergedType result = new MergedType();
        String current = null;
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (result.sampleClass == null) {
                result.sampleClass = value.getClass();
            }
            String typeName = simpleTypeNameOf(value);
            if (current == null || current.equals(typeName)) {
                current = typeName;
                continue;
            }
            String widened = widenNumeric(current, typeName);
            if (widened != null) {
                current = widened;
                // 类型提升后原始样本类已不再代表最终类型
                result.sampleClass = null;
            } else {
                current = "Object";
                result.sampleClass = null;
                result.warning = "// TODO: this field appears with multiple types in the payload samples (including " + typeName + "), please confirm the real type";
                break;
            }
        }
        result.type = current == null ? "Object" : current;
        return result;
    }

    /**
     * 数值类型提升：两者均为数值时返回更宽的类型，否则返回null表示不可提升
     */
    private String widenNumeric(String left, String right) {
        Integer leftRank = NUMERIC_RANK.get(left);
        Integer rightRank = NUMERIC_RANK.get(right);
        if (leftRank == null || rightRank == null) {
            return null;
        }
        // 任一侧是浮点则统一提到BigDecimal，避免精度问题
        if (leftRank >= 6 || rightRank >= 6) {
            return "BigDecimal";
        }
        return leftRank >= rightRank ? left : right;
    }

    /**
     * 简单类型字段：按所有样本合并出最终类型
     */
    private void applySimpleType(FieldInfo fieldInfo, Collection<Object> samples) {
        MergedType merged = mergeType(samples);
        fieldInfo.type = merged.type;
        fieldInfo.sampleClass = merged.sampleClass;
        fieldInfo.comment = merged.warning;
    }

    /**
     * 把报文key清洗为合法的Java字段名：
     * 非法字符替换为下划线，首字符为数字时加前缀，撞上关键字时加后缀；
     * 原始key由属性注释与序列化注解保留，不影响反序列化
     */
    private String sanitizeFieldName(String rawName) {
        String name = legalIdentifier(rawName, '_');
        if (name.isEmpty()) {
            return "field";
        }
        if (Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        if (JAVA_KEYWORDS.contains(name)) {
            name = name + "_";
        }
        return name;
    }

    /**
     * 把推导出的嵌套类名清洗为合法类名，全部非法时回退到占位名
     */
    private String sanitizeClassName(String rawName) {
        String name = legalIdentifier(rawName, '_');
        if (name.isEmpty()) {
            return "Inner";
        }
        if (Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        if (JAVA_KEYWORDS.contains(name)) {
            name = name + "_";
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * 只保留字母、数字与下划线，其余字符替换为指定字符
     */
    private String legalIdentifier(String source, char replaceChar) {
        if (source == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            } else {
                sb.append(replaceChar);
            }
        }
        return sb.toString();
    }

    /**
     * 基于已确定的字段类型收集import，不再回头扫描原始报文Map
     * （扫描式判断与最终生成的类型容易脱节，既会漏import也会产生无用import）
     *
     * @param fields      字段集合
     * @param isMainClass 是否主类，只有主类会继承/实现父类与接口
     * @return 按字典序排列的import集合
     */
    private Set<String> collectImports(Collection<FieldInfo> fields, boolean isMainClass) {
        // 用TreeSet保证import顺序稳定，使同一次响应重复生成得到完全一致的文件内容
        Set<String> importSet = new TreeSet<>();

        for (FieldInfo field : fields) {
            addTypeImports(importSet, field);
        }

        // 序列化框架注解导入
        addSerializationImports(importSet);

        if (useLombok) {
            importSet.add("import lombok.Data;");
            importSet.add("import lombok.Builder;");
            importSet.add("import lombok.NoArgsConstructor;");
            importSet.add("import lombok.AllArgsConstructor;");
        }

        if (useSwagger) {
            importSet.add("import io.swagger.annotations.ApiModel;");
            importSet.add("import io.swagger.annotations.ApiModelProperty;");
        }

        // 只有主类会继承/实现，父类与接口的import也只由主类收集
        if (isMainClass) {
            importSet.addAll(superTypeImports());
            // 继承父类且使用Lombok时，callSuper批注需要额外的两个import
            if (useLombok && hasExtendClass()) {
                importSet.add("import lombok.EqualsAndHashCode;");
                importSet.add("import lombok.ToString;");
            }
        }

        // 独立类策略下依赖类与当前类同包，本来就直接可见，无需生成冗余import

        return importSet;
    }

    /**
     * 按字段类型补充import，容器类型需递归处理泛型参数
     */
    private void addTypeImports(Set<String> importSet, FieldInfo field) {
        String type = field.type;
        if (type.startsWith("List<")) {
            importSet.add("import java.util.List;");
            addSingleTypeImport(importSet, field, genericInnerName(type));
        } else if (type.startsWith("Map<")) {
            importSet.add("import java.util.Map;");
        } else {
            addSingleTypeImport(importSet, field, type);
        }
    }

    /**
     * 为单个类型简单名补充import
     */
    private void addSingleTypeImport(Set<String> importSet, FieldInfo field, String typeName) {
        if (typeName == null || typeName.isEmpty() || JAVA_LANG_TYPES.contains(typeName)) {
            return;
        }
        // 本次生成的嵌套结构类与当前类同包，无需import
        if (classShapes.containsKey(typeName)) {
            return;
        }
        String known = TYPE_FQCNS.get(typeName);
        if (known != null) {
            importSet.add("import " + known + ";");
        } else if (field.sampleClass != null && !field.sampleClass.getName().startsWith("java.lang.")) {
            // 表内未收录的类型（如 LocalDateTime）按样本类的真实包名导入
            importSet.add("import " + field.sampleClass.getName() + ";");
        }
    }

    /**
     * 取 List&lt;X&gt; 中的X，非泛型类型返回自身
     */
    private String genericInnerName(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start < 0 || end <= start) {
            return type;
        }
        return type.substring(start + 1, end).trim();
    }

    /**
     * 添加序列化框架的导入
     */
    private void addSerializationImports(Set<String> importSet) {
        switch (serializationFramework) {
            case JACKSON:
                importSet.add("import com.fasterxml.jackson.annotation.JsonProperty;");
                importSet.add("import com.fasterxml.jackson.annotation.JsonIgnore;");
                importSet.add("import com.fasterxml.jackson.annotation.JsonFormat;");
                importSet.add("import com.fasterxml.jackson.annotation.JsonInclude;");
                break;
            case GSON:
                importSet.add("import com.google.gson.annotations.SerializedName;");
                importSet.add("import com.google.gson.annotations.Expose;");
                break;
            case FASTJSON:
                importSet.add("import com.alibaba.fastjson.annotation.JSONField;");
                break;
            case FASTJSON2:
                importSet.add("import com.alibaba.fastjson2.annotation.JSONField;");
                break;
            case JACKSON_XML:
                importSet.add("import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;");
                importSet.add("import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;");
                importSet.add("import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;");
                break;
            case XSTREAM:
                importSet.add("import com.thoughtworks.xstream.annotations.XStreamAlias;");
                importSet.add("import com.thoughtworks.xstream.annotations.XStreamAsAttribute;");
                break;
            case JAXB:
                importSet.add("import javax.xml.bind.annotation.XmlElement;");
                importSet.add("import javax.xml.bind.annotation.XmlRootElement;");
                importSet.add("import javax.xml.bind.annotation.XmlAttribute;");
                importSet.add("import javax.xml.bind.annotation.XmlAccessorType;");
                importSet.add("import javax.xml.bind.annotation.XmlAccessType;");
                break;
            default:
                break;
        }
    }

    /**
     * 生成类级别的序列化注解（修正版）
     */
    private String generateClassSerializationAnnotations(String className) {
        StringBuilder annotations = new StringBuilder();

        // 将类名转换为小驼峰形式作为根元素名称
        String rootName = toLowerCamelCase(className);

        switch (serializationFramework) {
            case JACKSON_XML:
                annotations.append("@JacksonXmlRootElement(localName = \"").append(rootName).append("\")\n");
                break;
            case XSTREAM:
                annotations.append("@XStreamAlias(\"").append(rootName).append("\")\n");
                break;
            case JAXB:
                annotations.append("@XmlRootElement(name = \"").append(rootName).append("\")\n");
                annotations.append("@XmlAccessorType(XmlAccessType.FIELD)\n");
                break;
            default:
                break;
        }

        return annotations.toString();
    }

    /**
     * 生成字段级别的序列化注解
     */
    private String generateFieldSerializationAnnotation(FieldInfo field, int indentLevel) {
        String indentStr = repeatString("    ", indentLevel);
        StringBuilder annotation = new StringBuilder();

        switch (serializationFramework) {
            case JACKSON:
                // 如果字段名和原始名不同，使用 @JsonProperty
                if (!field.name.equals(field.originalName)) {
                    annotation.append(indentStr).append("@JsonProperty(\"").append(field.originalName).append("\")\n");
                } else {
                    annotation.append(indentStr).append("@JsonProperty\n");
                }
                break;

            case GSON:
                if (!field.name.equals(field.originalName)) {
                    annotation.append(indentStr).append("@SerializedName(\"").append(field.originalName).append("\")\n");
                } else {
                    annotation.append(indentStr).append("@SerializedName\n");
                }
                break;

            case FASTJSON:
                annotation.append(indentStr).append("@JSONField(name = \"").append(field.originalName).append("\")\n");
                break;

            case FASTJSON2:
                annotation.append(indentStr).append("@JSONField(name = \"").append(field.originalName).append("\")\n");
                break;

            case JACKSON_XML:
                annotation.append(indentStr).append("@JacksonXmlProperty(localName = \"").append(field.originalName).append("\")\n");
                break;

            case XSTREAM:
                annotation.append(indentStr).append("@XStreamAlias(\"").append(field.originalName).append("\")\n");
                break;

            case JAXB:
                annotation.append(indentStr).append("@XmlElement(name = \"").append(field.originalName).append("\")\n");
                break;

            default:
                break;
        }

        return annotation.toString();
    }

    /**
     * 构建内部类结果（所有类在同一个文件中）
     */
    private String buildInnerClassResult(String mainClassName,
                                         Map<String, String> allClasses,
                                         Map<String, Set<String>> classImports) {
        StringBuilder result = new StringBuilder();

        // 添加包名
        result.append("package ").append(packageName).append(";\n\n");

        // 添加导入（所有类共用一个编译单元，import必须取全部类的并集）
        appendImports(result, mergeFileImports(classImports));

        // 生成类Javadoc
        result.append(generateClassJavadoc(mainClassName, true));

        // 生成类级别的序列化注解
        result.append(generateClassSerializationAnnotations(mainClassName));

        // 生成主类注解
        result.append(generateClassAnnotations(mainClassName, true));

        // 生成主类定义
        result.append("public class ").append(mainClassName).append(buildExtendsAndImplements()).append(" {\n");

        // 主类成员直接取生成阶段缓存的成员块，不再从整类源码里反向抽取
        String membersPart = memberCodes.get(mainClassName);
        if (membersPart != null) {
            result.append(membersPart);
        }

        // 添加所有内部类（在底部），缩进只在此处统一施加一层
        for (Map.Entry<String, String> entry : allClasses.entrySet()) {
            if (!entry.getKey().equals(mainClassName)) {
                boolean needLeadingBlank = membersPart != null && !membersPart.isEmpty();
                result.append(formatInnerClass(entry.getValue(), needLeadingBlank));
            }
        }

        result.append("}\n");

        return result.toString();
    }

    /**
     * 合并同一编译单元内所有类的import。
     * INNER_CLASS 策略下嵌套类与主类写在同一个文件里，只取主类的import会丢掉
     * 仅嵌套类用到的类型（如 List、BigDecimal），因此必须取并集
     */
    private Set<String> mergeFileImports(Map<String, Set<String>> classImports) {
        Set<String> merged = new TreeSet<>();
        for (Set<String> imports : classImports.values()) {
            if (imports != null) {
                merged.addAll(imports);
            }
        }
        return merged;
    }

    /**
     * 格式化内部类代码：整体加一层缩进，并在与前一个成员之间补一个空行
     *
     * @param classCode          平铺形式的内部类代码
     * @param needLeadingBlank   是否需要在前部补空行
     * @return 可嵌入主类的内部类代码
     */
    private String formatInnerClass(String classCode, boolean needLeadingBlank) {
        StringBuilder formatted = new StringBuilder();
        if (needLeadingBlank) {
            formatted.append("\n");
        }
        String[] lines = classCode.split("\n");

        for (String line : lines) {
            // 跳过package和import行
            if (line.trim().startsWith("package ") || line.trim().startsWith("import ")) {
                continue;
            }
            // 为每一行添加一个缩进
            if (!line.trim().isEmpty()) {
                formatted.append("    ").append(line).append("\n");
            } else {
                formatted.append("\n");
            }
        }

        return formatted.toString();
    }

    /**
     * 构建独立类结果（每个类单独一个文件，用分隔符分隔）
     */
    private String buildSeparateClassResult(Map<String, String> allClasses) {
        StringBuilder result = new StringBuilder();
        String separator = "\n" + repeatString("=", 80) + "\n";

        int index = 1;
        for (Map.Entry<String, String> entry : allClasses.entrySet()) {
            result.append("// File ").append(index++).append(": ").append(entry.getKey()).append(".java\n");
            result.append(entry.getValue());
            if (index <= allClasses.size()) {
                result.append(separator);
            }
        }

        return result.toString();
    }

    /**
     * 生成字段定义
     */
    private String generateFieldDefinition(FieldInfo field, int indentLevel) {
        StringBuilder fieldCode = new StringBuilder();
        String indentStr = repeatString("    ", indentLevel);

        // 属性Javadoc：报文字段名与示例值
        fieldCode.append(indentStr).append(generateFieldJavadoc(field));

        // 添加TODO注释（如果有）
        if (field.comment != null && !field.comment.isEmpty()) {
            fieldCode.append(indentStr).append(field.comment).append("\n");
        }

        // 序列化框架注解
        String serializationAnnotation = generateFieldSerializationAnnotation(field, indentLevel);
        if (!serializationAnnotation.isEmpty()) {
            fieldCode.append(serializationAnnotation);
        }

        // Swagger注解
        if (useSwagger) {
            fieldCode.append(indentStr).append("@ApiModelProperty(value = \"").append(field.originalName).append("\")\n");
        }

        // 字段定义
        fieldCode.append(indentStr).append("private ").append(field.type).append(" ").append(field.name).append(";\n\n");

        return fieldCode.toString();
    }

    /**
     * 生成getter/setter
     */
    private String generateGetterSetter(FieldInfo field, int indentLevel) {
        String indentStr = repeatString("    ", indentLevel);
        String upperField = capitalizeFirst(field.name);
        StringBuilder code = new StringBuilder();

        // Getter
        code.append(indentStr).append("public ").append(field.type).append(" get").append(upperField).append("() {\n");
        code.append(indentStr).append("    ").append("return ").append(field.name).append(";\n");
        code.append(indentStr).append("}\n\n");

        // Setter
        code.append(indentStr).append("public void set").append(upperField).append("(").append(field.type).append(" ").append(field.name).append(") {\n");
        code.append(indentStr).append("    ").append("this.").append(field.name).append(" = ").append(field.name).append(";\n");
        code.append(indentStr).append("}\n\n");

        return code.toString();
    }

    /**
     * 生成toString方法（仅在不使用Lombok时）
     */
    private String generateToString(String className, Collection<FieldInfo> fields, int indentLevel) {
        String indentStr = repeatString("    ", indentLevel);
        StringBuilder code = new StringBuilder();
        code.append(indentStr).append("@Override\n");
        code.append(indentStr).append("public String toString() {\n");
        code.append(indentStr).append("    ").append("return \"").append(className).append("{\"\n");

        boolean first = true;
        for (FieldInfo field : fields) {
            if (first) {
                code.append(indentStr).append("        ").append("+ \"").append(field.name).append("=\" + ").append(field.name);
                first = false;
            } else {
                code.append("\n").append(indentStr).append("        ").append("+ \", ").append(field.name).append("=\" + ").append(field.name);
            }
        }

        code.append("\n").append(indentStr).append("        ").append("+ '}';\n");
        code.append(indentStr).append("}\n\n");

        return code.toString();
    }

    /**
     * 生成类注解
     *
     * @param className   类名
     * @param isMainClass 是否为主类，只有主类会继承配置的父类
     * @return 类注解文本
     */
    private String generateClassAnnotations(String className, boolean isMainClass) {
        StringBuilder annotations = new StringBuilder();

        if (useLombok) {
            annotations.append("@Data\n");
            annotations.append("@Builder\n");
            annotations.append("@NoArgsConstructor\n");
            annotations.append("@AllArgsConstructor\n");
            // 继承父类时Lombok生成的equals/hashCode/toString默认不含父类字段，
            // 既会触发callSuper警告，又会让比较结果漏掉父类字段，所以显式声明callSuper
            if (isMainClass && hasExtendClass()) {
                annotations.append("@ToString(callSuper = true)\n");
                annotations.append("@EqualsAndHashCode(callSuper = true)\n");
            }
        }

        if (useSwagger) {
            annotations.append("@ApiModel(description = \"").append(className).append(" response entity\")\n");
        }

        return annotations.toString();
    }

    /**
     * 生成类级Javadoc
     *
     * @param className   类名
     * @param isMainClass 是否为主类（主类额外记录生成来源）
     * @return 类Javadoc文本
     */
    private String generateClassJavadoc(String className, boolean isMainClass) {
        StringBuilder doc = new StringBuilder();
        doc.append("/**\n");
        doc.append(" * ").append(className)
                .append(isMainClass ? " response payload entity." : " nested structure entity.")
                .append("\n");
        if (isMainClass && !sourceDescription.isEmpty()) {
            doc.append(" * <p>Automatically generated from HTTP response data, source: ")
                    .append(escapeDoc(sourceDescription))
                    .append(".</p>\n");
        }
        doc.append(" */\n");
        return doc.toString();
    }

    /**
     * 生成属性Javadoc：字段名与报文实际值不一致时标注报文字段，并给出示例值
     *
     * @param field 字段信息
     * @return 属性Javadoc文本
     */
    private String generateFieldJavadoc(FieldInfo field) {
        List<String> parts = new ArrayList<>();
        if (!field.name.equals(field.originalName)) {
            parts.add("payload field <code>" + escapeDoc(field.originalName) + "</code>");
        }
        if (field.sampleValue != null) {
            parts.add("sample value: " + escapeDoc(field.sampleValue));
        }
        if (parts.isEmpty()) {
            parts.add("type: " + field.type);
        }
        return "/** " + String.join(", ", parts) + " */\n";
    }

    /**
     * 将报文中的实际值渲染为适合写入注释的示例值文本
     *
     * @param value 字段值
     * @return 示例值文本
     */
    private String renderSampleValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Map) {
            Map<?, ?> mapValue = (Map<?, ?>) value;
            if (mapValue.isEmpty()) {
                return "{} (empty object)";
            }
            List<String> keys = new ArrayList<>();
            for (Object key : mapValue.keySet()) {
                keys.add(String.valueOf(key));
            }
            return "{" + String.join(", ", keys) + "}";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[] (empty list)";
            }
            Object first = list.get(0);
            String itemDesc = (first instanceof Map) ? "object element" : renderSampleValue(first);
            return "[" + itemDesc + ", ... " + list.size() + " items in total]";
        }
        return String.valueOf(value);
    }

    /**
     * 拼装主类的 extends / implements 部分
     *
     * @return 形如 “ extends BaseResp implements Serializable” 的片段，无配置时为空串
     */
    private String buildExtendsAndImplements() {
        StringBuilder sb = new StringBuilder();
        if (hasExtendClass()) {
            sb.append(" extends ").append(simpleTypeName(extendClass.trim()));
        }
        List<String> interfaces = new ArrayList<>();
        for (String itf : implementInterfaces) {
            if (itf != null && !itf.trim().isEmpty()) {
                interfaces.add(simpleTypeName(itf.trim()));
            }
        }
        // 便捷开关开启时自动补上Serializable，避免使用方手写全限定接口名
        if (serializable && !interfaces.contains("Serializable")) {
            interfaces.add("Serializable");
        }
        if (!interfaces.isEmpty()) {
            sb.append(" implements ").append(String.join(", ", interfaces));
        }
        return sb.toString();
    }

    /**
     * 是否配置了需要继承的父类
     */
    private boolean hasExtendClass() {
        return extendClass != null && !extendClass.trim().isEmpty();
    }

    /**
     * 收集父类与接口所需的import（仅全限定名需要，简单类名视为 java.lang 或同包可见）
     *
     * @return import语句集合，已按字典序排列
     */
    private Set<String> superTypeImports() {
        Set<String> imports = new TreeSet<>();
        addSuperTypeImport(imports, extendClass);
        for (String itf : implementInterfaces) {
            addSuperTypeImport(imports, itf);
        }
        if (serializable) {
            imports.add("import java.io.Serializable;");
        }
        return imports;
    }

    /**
     * 为一个父类/接口声明追加import
     */
    private void addSuperTypeImport(Set<String> imports, String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return;
        }
        String name = typeName.trim();
        // 全限定名才需要import；与本生成器同包的类直接可见，不记入 import
        if (name.lastIndexOf('.') > 0 && !name.startsWith(packageName + ".")) {
            imports.add("import " + name + ";");
        }
    }

    /**
     * 取全限定名的最后一段作为简单类名
     */
    private String simpleTypeName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot > 0 ? typeName.substring(lastDot + 1) : typeName;
    }

    /**
     * 按固定顺序输出import块，保证生成结果可复现
     */
    private void appendImports(StringBuilder code, Set<String> imports) {
        if (imports == null || imports.isEmpty()) {
            return;
        }
        for (String imp : imports) {
            code.append(imp).append("\n");
        }
        code.append("\n");
    }

    /**
     * 转义注释内容，避免提前闭合Javadoc
     */
    private String escapeDoc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("*/", "* /").replace("\r", " ").replace("\n", " ");
    }

    /**
     * Java 8 兼容的字符串重复方法
     */
    private String repeatString(String str, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 下划线转驼峰
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_' || c == '-') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    // 保留原大小写，不再把 userName 压平成 username
                    result.append(c);
                }
            }
        }

        // 字段名首字母统一小写，避免 OrderStatus 这种全名开头大写的字段名
        if (result.length() > 0) {
            result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
        }

        return result.toString();
    }

    /**
     * 首字母大写
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 转小驼峰（首字母小写）- 符合Java Bean规范
     * 例如：TestUser -> testUser, XMLUser -> xmlUser, URLData -> urlData
     */
    private String toLowerCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() == 1) {
            return str.toLowerCase();
        }
        // 处理特殊情况：如果前两个字母都是大写，保持原样（如 XMLUser -> XMLUser）
        if (Character.isUpperCase(str.charAt(0)) && Character.isUpperCase(str.charAt(1))) {
            return str;
        }
        // 首字母小写，其余保持原样
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 字段信息内部类
     */
    private static class FieldInfo {
        String name;
        String originalName;
        String type;
        String comment;
        String sampleValue;
        /**
         * 代表样本值的真实类型，用于推导无法预知包名的类型（如 LocalDateTime）的import
         */
        Class<?> sampleClass;
    }
}