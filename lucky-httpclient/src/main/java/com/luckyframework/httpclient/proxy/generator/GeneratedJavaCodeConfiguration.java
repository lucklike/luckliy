package com.luckyframework.httpclient.proxy.generator;

public class GeneratedJavaCodeConfiguration {
    /**
     * SpEL表达式，用于启用/禁用代码生成。
     * 仅当此表达式计算结果为true时，才会执行代码生成。
     */
    private Boolean enable = false;

    /**
     * SpEL表达式，用于判断是否覆盖已存在的文件。
     */
    private Boolean cover = false;

    /**
     * SpEL表达式，用于从响应中提取数据源以生成实体类。
     * 如果未指定，将使用整个响应体作为数据源。
     */
    private String extractExpression = "";

    /**
     * 必需。Java文件保存的目录路径。
     * 支持SpEL表达式。
     */
    private String savePath = "#{get_source_root_path($class$)}";

    /**
     * 生成的Java类的包名。
     * 支持SpEL表达式。
     */
    private String packageName = "#{get_def_package_name($class$)}";

    /**
     * 必需。主生成的Java类的类名。
     * 支持SpEL表达式。
     */
    private String className = "#{get_def_class_name($mc$)}";

    /**
     * 是否使用Lombok注解（如 @Data、@Builder）。
     */
    private boolean useLombok = true;

    /**
     * 是否使用Swagger注解（如 @ApiModel、@ApiModelProperty）。
     */
    private boolean useSwagger = false;

    /**
     * 嵌套类的处理策略。
     */
    private NestedClassStrategy nestedClassStrategy = NestedClassStrategy.INNER_CLASS;

    /**
     * 从JSON字段名生成Java字段名的命名策略。
     */
    private FieldNamingStrategy fieldNamingStrategy = FieldNamingStrategy.ORIGINAL;

    /**
     * 需要支持的序列化框架（通过注解方式）。
     */
    private SerializationFramework serializationFramework = SerializationFramework.NONE;

    // ========== Getter and Setter ==========

    /**
     * 获取代码生成启用条件的SpEL表达式。
     * 仅当此表达式计算结果为true时，才会执行代码生成。
     *
     * @return 启用条件表达式
     */
    public Boolean getEnable() {
        return enable;
    }

    /**
     * 设置代码生成启用条件的SpEL表达式。
     * 仅当此表达式计算结果为true时，才会执行代码生成。
     *
     * @param enable 启用条件表达式
     */
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    /**
     * 获取文件覆盖条件的SpEL表达式。
     * 当表达式计算结果为true时，将覆盖已存在的文件。
     *
     * @return 覆盖条件表达式
     */
    public Boolean getCover() {
        return cover;
    }

    /**
     * 设置文件覆盖条件的SpEL表达式。
     * 当表达式计算结果为true时，将覆盖已存在的文件。
     *
     * @param cover 覆盖条件表达式
     */
    public void setCover(Boolean cover) {
        this.cover = cover;
    }

    /**
     * 获取数据源提取的SpEL表达式。
     * 用于从响应中提取数据源以生成实体类。
     * 如果未指定，将使用整个响应体作为数据源。
     *
     * @return 提取表达式
     */
    public String getExtractExpression() {
        return extractExpression;
    }

    /**
     * 设置数据源提取的SpEL表达式。
     * 用于从响应中提取数据源以生成实体类。
     * 如果未指定，将使用整个响应体作为数据源。
     *
     * @param extractExpression 提取表达式
     */
    public void setExtractExpression(String extractExpression) {
        this.extractExpression = extractExpression;
    }

    /**
     * 获取Java文件的保存目录路径。
     * 支持SpEL表达式动态计算。
     *
     * @return 保存路径
     */
    public String getSavePath() {
        return savePath;
    }

    /**
     * 设置Java文件的保存目录路径。
     * 支持SpEL表达式动态计算。
     *
     * @param savePath 保存路径
     */
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    /**
     * 获取生成的Java类的包名。
     * 支持SpEL表达式动态计算。
     *
     * @return 包名
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * 设置生成的Java类的包名。
     * 支持SpEL表达式动态计算。
     *
     * @param packageName 包名
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * 获取主生成的Java类的类名。
     * 支持SpEL表达式动态计算。
     *
     * @return 类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置主生成的Java类的类名。
     * 支持SpEL表达式动态计算。
     *
     * @param className 类名
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取是否使用Lombok注解。
     * 如果为true，生成的类将添加 @Data、@Builder 等Lombok注解。
     *
     * @return true表示使用Lombok，false表示不使用
     */
    public boolean isUseLombok() {
        return useLombok;
    }

    /**
     * 设置是否使用Lombok注解。
     * 如果为true，生成的类将添加 @Data、@Builder 等Lombok注解。
     *
     * @param useLombok true表示使用Lombok，false表示不使用
     */
    public void setUseLombok(boolean useLombok) {
        this.useLombok = useLombok;
    }

    /**
     * 获取是否使用Swagger注解。
     * 如果为true，生成的类将添加 @ApiModel、@ApiModelProperty 等Swagger注解。
     *
     * @return true表示使用Swagger，false表示不使用
     */
    public boolean isUseSwagger() {
        return useSwagger;
    }

    /**
     * 设置是否使用Swagger注解。
     * 如果为true，生成的类将添加 @ApiModel、@ApiModelProperty 等Swagger注解。
     *
     * @param useSwagger true表示使用Swagger，false表示不使用
     */
    public void setUseSwagger(boolean useSwagger) {
        this.useSwagger = useSwagger;
    }

    /**
     * 获取嵌套类的处理策略。
     *
     * @return 嵌套类策略枚举值
     */
    public NestedClassStrategy getNestedClassStrategy() {
        return nestedClassStrategy;
    }

    /**
     * 设置嵌套类的处理策略。
     *
     * @param nestedClassStrategy 嵌套类策略枚举值
     */
    public void setNestedClassStrategy(NestedClassStrategy nestedClassStrategy) {
        this.nestedClassStrategy = nestedClassStrategy;
    }

    /**
     * 获取从JSON字段名生成Java字段名的命名策略。
     *
     * @return 字段命名策略枚举值
     */
    public FieldNamingStrategy getFieldNamingStrategy() {
        return fieldNamingStrategy;
    }

    /**
     * 设置从JSON字段名生成Java字段名的命名策略。
     *
     * @param fieldNamingStrategy 字段命名策略枚举值
     */
    public void setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
        this.fieldNamingStrategy = fieldNamingStrategy;
    }

    /**
     * 获取需要支持的序列化框架。
     * 生成的类将添加对应框架的序列化注解。
     *
     * @return 序列化框架枚举值
     */
    public SerializationFramework getSerializationFramework() {
        return serializationFramework;
    }

    /**
     * 设置需要支持的序列化框架。
     * 生成的类将添加对应框架的序列化注解。
     *
     * @param serializationFramework 序列化框架枚举值
     */
    public void setSerializationFramework(SerializationFramework serializationFramework) {
        this.serializationFramework = serializationFramework;
    }
}