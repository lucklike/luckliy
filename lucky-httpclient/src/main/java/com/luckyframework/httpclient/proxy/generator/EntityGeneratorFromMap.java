package com.luckyframework.httpclient.proxy.generator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 根据Map结构生成Java实体类的工具类
 * 支持嵌套Map和List<Map>结构
 * 支持多种序列化框架注解
 */
public class EntityGeneratorFromMap {

    // 基本类型的包装类映射
    private static final Map<Class<?>, String> WRAPPER_TYPE_MAP = new HashMap<>();
    // 基本类型映射
    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>();

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

        PRIMITIVE_TYPES.addAll(Arrays.asList("int", "long", "double", "boolean", "byte", "short", "float", "char"));
    }

    private String packageName = "com.example.entity";
    private String indent = "    ";
    private boolean useLombok = true;
    private boolean useSwagger = true;
    private NestedClassStrategy nestedStrategy = NestedClassStrategy.INNER_CLASS;
    private FieldNamingStrategy fieldNamingStrategy = FieldNamingStrategy.ORIGINAL;
    private SerializationFramework serializationFramework = SerializationFramework.NONE;

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
        Map<String, String> allClasses = new LinkedHashMap<>();
        Map<String, Set<String>> classDependencies = new HashMap<>();
        Map<String, Set<String>> classImports = new HashMap<>();

        generateEntityRecursive(map, className, allClasses, classDependencies, classImports, null);

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
            String innerClassResult = buildInnerClassResult(className, map, allClasses, classImports);
            result.add(JavaCode.of(packageName, className, innerClassResult));
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
        Map<String, String> allClasses = new LinkedHashMap<>();
        Map<String, Set<String>> classDependencies = new HashMap<>();
        Map<String, Set<String>> classImports = new HashMap<>();

        generateEntityRecursive(map, className, allClasses, classDependencies, classImports, null);

        // 根据策略返回结果
        if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
            return buildSeparateClassResult(allClasses);
        } else {
            return buildInnerClassResult(className, map, allClasses, classImports);
        }
    }



    /**
     * 递归生成实体类及所有嵌套类
     */
    private void generateEntityRecursive(Map<String, Object> map, String className,
                                         Map<String, String> allClasses,
                                         Map<String, Set<String>> classDependencies,
                                         Map<String, Set<String>> classImports,
                                         String parentClassName) {
        if (allClasses.containsKey(className)) {
            return;
        }

        StringBuilder code = new StringBuilder();

        // 分析字段类型，并收集嵌套类和导入信息
        Map<String, FieldInfo> fields = analyzeFields(map, className, allClasses, classDependencies, classImports, parentClassName);

        // 收集该类需要的导入
        Set<String> imports = collectImports(map, className, classDependencies.get(className));
        classImports.put(className, imports);

        // 生成包名和导入（独立类需要，内部类在主类中统一处理）
        if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
            code.append("package ").append(packageName).append(";\n\n");
            for (String imp : imports) {
                code.append(imp).append("\n");
            }
            if (!imports.isEmpty()) {
                code.append("\n");
            }
        }

        // 生成类注解（内部类需要先添加缩进）
        if (nestedStrategy == NestedClassStrategy.INNER_CLASS && parentClassName != null) {
            String classAnnotations = generateClassAnnotations(className);
            String[] annotationLines = classAnnotations.split("\n");
            for (String line : annotationLines) {
                code.append("    ").append(line).append("\n");
            }
            code.append("    public static class ").append(className).append(" {\n\n");
        } else {
            code.append(generateClassAnnotations(className));
            code.append("public class ").append(className).append(" {\n\n");
        }

        // 生成字段定义
        for (FieldInfo field : fields.values()) {
            code.append(generateFieldDefinition(field, nestedStrategy == NestedClassStrategy.INNER_CLASS && parentClassName != null ? 2 : 1));
        }

        // 如果不使用Lombok，生成getter/setter和toString
        if (!useLombok) {
            code.append("\n");
            for (FieldInfo field : fields.values()) {
                code.append(generateGetterSetter(field, nestedStrategy == NestedClassStrategy.INNER_CLASS && parentClassName != null ? 2 : 1));
            }
            code.append(generateToString(className, fields.values(), nestedStrategy == NestedClassStrategy.INNER_CLASS && parentClassName != null ? 2 : 1));
        }

        code.append("}\n");

        allClasses.put(className, code.toString());
    }

    /**
     * 分析字段类型
     */
    private Map<String, FieldInfo> analyzeFields(Map<String, Object> map,
                                                 String currentClassName,
                                                 Map<String, String> allClasses,
                                                 Map<String, Set<String>> classDependencies,
                                                 Map<String, Set<String>> classImports,
                                                 String parentClassName) {
        Map<String, FieldInfo> fields = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            // 根据命名策略决定字段名
            String fieldName = (fieldNamingStrategy == FieldNamingStrategy.CAMEL_CASE)
                    ? toCamelCase(entry.getKey())
                    : entry.getKey();

            Object value = entry.getValue();
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.name = fieldName;
            fieldInfo.originalName = entry.getKey();

            if (value == null) {
                // null值，使用Object类型，并添加注释
                fieldInfo.type = "Object";
                fieldInfo.comment = "// TODO: 请根据实际业务确定该字段的类型";
                fieldInfo.importNeeded = false;
            } else if (value instanceof Map) {
                Map<?, ?> mapValue = (Map<?, ?>) value;
                if (mapValue.isEmpty()) {
                    // 空Map，使用Map<String, Object>类型
                    fieldInfo.type = "Map<String, Object>";
                    fieldInfo.comment = "// TODO: 请根据实际业务确定该Map的具体结构";
                    fieldInfo.importNeeded = true;
                } else {
                    // 嵌套Map
                    String innerClassName = capitalizeFirst(toCamelCase(entry.getKey())) + "Info";
                    fieldInfo.type = innerClassName;
                    fieldInfo.isNestedClass = true;

                    // 记录依赖关系
                    if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
                        classDependencies.computeIfAbsent(currentClassName, k -> new HashSet<>()).add(innerClassName);
                    }

                    // 递归生成嵌套类
                    generateEntityRecursive((Map<String, Object>) mapValue, innerClassName, allClasses, classDependencies, classImports, currentClassName);
                }
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    // 空List，使用List<Object>类型
                    fieldInfo.type = "List<Object>";
                    fieldInfo.comment = "// TODO: 请根据实际业务确定该List的具体类型";
                    fieldInfo.importNeeded = true;
                } else if (list.get(0) instanceof Map) {
                    // List<Map> 类型
                    String innerClassName = capitalizeFirst(toCamelCase(entry.getKey())) + "Item";
                    fieldInfo.type = "List<" + innerClassName + ">";
                    fieldInfo.isNestedClass = true;

                    // 记录依赖关系
                    if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS) {
                        classDependencies.computeIfAbsent(currentClassName, k -> new HashSet<>()).add(innerClassName);
                    }

                    // 递归生成嵌套类
                    generateEntityRecursive((Map<String, Object>) list.get(0), innerClassName, allClasses, classDependencies, classImports, currentClassName);
                } else {
                    // 简单List类型
                    fieldInfo.type = determineListType(list);
                }
                fieldInfo.importNeeded = true;
            } else {
                fieldInfo.type = determineSimpleType(value.getClass());
                fieldInfo.importNeeded = needImport(fieldInfo.type);
            }

            fields.put(fieldName, fieldInfo);
        }

        return fields;
    }

    /**
     * 收集导入信息
     */
    private Set<String> collectImports(Map<String, Object> map, String className, Set<String> dependencies) {
        Set<String> importSet = new HashSet<>();

        // 检查是否需要导入日期类型
        if (hasType(map, Date.class)) {
            importSet.add("import java.util.Date;");
        }
        if (hasType(map, BigDecimal.class)) {
            importSet.add("import java.math.BigDecimal;");
        }

        // 检查是否需要导入List和Map
        if (hasListType(map) || hasMapType(map)) {
            importSet.add("import java.util.List;");
            importSet.add("import java.util.Map;");
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

        // 独立类策略需要导入依赖的其他类
        if (nestedStrategy == NestedClassStrategy.SEPARATE_CLASS && dependencies != null && !dependencies.isEmpty()) {
            for (String dependency : dependencies) {
                importSet.add("import " + packageName + "." + dependency + ";");
            }
        }

        return importSet;
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
    private String buildInnerClassResult(String mainClassName, Map<String, Object> originalMap,
                                         Map<String, String> allClasses,
                                         Map<String, Set<String>> classImports) {
        StringBuilder result = new StringBuilder();

        // 添加包名
        result.append("package ").append(packageName).append(";\n\n");

        // 添加导入（使用主类的导入）
        Set<String> mainImports = classImports.get(mainClassName);
        if (mainImports != null && !mainImports.isEmpty()) {
            for (String imp : mainImports) {
                result.append(imp).append("\n");
            }
            result.append("\n");
        }

        // 生成类级别的序列化注解
        result.append(generateClassSerializationAnnotations(mainClassName));

        // 生成主类注解
        result.append(generateClassAnnotations(mainClassName));

        // 生成主类定义
        result.append("public class ").append(mainClassName).append(" {\n\n");

        // 添加主类的字段
        String mainClassCode = allClasses.get(mainClassName);
        if (mainClassCode != null) {
            String fieldsPart = extractFieldsFromClassCode(mainClassCode);
            result.append(fieldsPart);
        }

        // 添加所有内部类（在底部）
        for (Map.Entry<String, String> entry : allClasses.entrySet()) {
            if (!entry.getKey().equals(mainClassName)) {
                String innerClassCode = entry.getValue();
                // 提取内部类代码，并调整缩进
                String formattedInnerClass = formatInnerClass(innerClassCode);
                result.append(formattedInnerClass);
            }
        }

        result.append("}\n");

        return result.toString();
    }

    /**
     * 从类代码中提取字段部分
     */
    private String extractFieldsFromClassCode(String classCode) {
        StringBuilder fields = new StringBuilder();
        String[] lines = classCode.split("\n");
        boolean inFields = false;
        int braceCount = 0;

        for (String line : lines) {
            if (line.contains("public class ") && line.contains("{")) {
                inFields = true;
                braceCount++;
                continue;
            }

            if (inFields) {
                // 统计大括号
                for (char c : line.toCharArray()) {
                    if (c == '{') braceCount++;
                    if (c == '}') braceCount--;
                }

                // 如果遇到内部类的开始，停止提取
                if (line.contains("public static class")) {
                    break;
                }

                // 如果还没结束，添加该行
                if (braceCount > 0) {
                    fields.append(line).append("\n");
                }
            }
        }

        return fields.toString();
    }

    /**
     * 格式化内部类代码（调整缩进）
     */
    private String formatInnerClass(String classCode) {
        StringBuilder formatted = new StringBuilder();
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

        // 确保内部类之间有空行
        if (formatted.length() > 0 && !formatted.toString().endsWith("\n\n")) {
            formatted.append("\n");
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
            result.append("// 文件 ").append(index++).append(": ").append(entry.getKey()).append(".java\n");
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

        // 添加注释（如果有）
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

        code.append("\n").append(indentStr).append("    ").append("+ '}';\n");
        code.append(indentStr).append("}\n\n");

        return code.toString();
    }

    /**
     * 生成类注解
     */
    private String generateClassAnnotations(String className) {
        StringBuilder annotations = new StringBuilder();

        if (useLombok) {
            annotations.append("@Data\n");
            annotations.append("@Builder\n");
            annotations.append("@NoArgsConstructor\n");
            annotations.append("@AllArgsConstructor\n");
        }

        if (useSwagger) {
            annotations.append("@ApiModel(description = \"").append(className).append("实体类\")\n");
        }

        return annotations.toString();
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
     * 判断简单类型
     */
    private String determineSimpleType(Class<?> clazz) {
        if (WRAPPER_TYPE_MAP.containsKey(clazz)) {
            return WRAPPER_TYPE_MAP.get(clazz);
        }
        return clazz.getSimpleName();
    }

    /**
     * 判断List类型
     */
    private String determineListType(List<?> list) {
        if (list.isEmpty()) {
            return "List<Object>";
        }

        Object firstElement = list.get(0);
        if (firstElement instanceof Map) {
            return "List<Object>";
        } else {
            String elementType = determineSimpleType(firstElement.getClass());
            return "List<" + elementType + ">";
        }
    }

    /**
     * 检查Map中是否包含指定类型
     */
    private boolean hasType(Map<String, Object> map, Class<?> targetType) {
        for (Object value : map.values()) {
            if (value != null && targetType.isAssignableFrom(value.getClass())) {
                return true;
            }
            if (value instanceof Map && hasType((Map<String, Object>) value, targetType)) {
                return true;
            }
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map && hasType((Map<String, Object>) item, targetType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查是否包含List类型
     */
    private boolean hasListType(Map<String, Object> map) {
        for (Object value : map.values()) {
            if (value instanceof List) {
                return true;
            }
            if (value instanceof Map && hasListType((Map<String, Object>) value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否包含Map类型
     */
    private boolean hasMapType(Map<String, Object> map) {
        for (Object value : map.values()) {
            if (value instanceof Map) {
                return true;
            }
            if (value instanceof Map && hasMapType((Map<String, Object>) value)) {
                return true;
            }
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map && hasMapType((Map<String, Object>) item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断是否需要导入
     */
    private boolean needImport(String type) {
        return !PRIMITIVE_TYPES.contains(type) &&
                !type.startsWith("List<") &&
                !"String".equals(type) &&
                !"Object".equals(type);
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
                    result.append(Character.toLowerCase(c));
                }
            }
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
        boolean isNestedClass = false;
        boolean importNeeded = false;
        String nestedClassCode;
    }
}