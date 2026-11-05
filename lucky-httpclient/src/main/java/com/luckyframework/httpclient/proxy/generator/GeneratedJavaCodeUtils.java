package com.luckyframework.httpclient.proxy.generator;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生成的Java代码工具类
 * 提供从HTTP响应生成Java实体类的核心功能
 */
public class GeneratedJavaCodeUtils {

    private static final Logger log = LoggerFactory.getLogger(GeneratedJavaCodeUtils.class);

    /**
     * 已回显过的告警标识。本功能挂在每次响应上，配置类问题若每次响应都告警会刷屏，
     * 因此同一标识只在首次出现时以warn输出。标识基数等于API方法数乘以少数几种原因，不会无界增长
     */
    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    /**
     * 根据响应数据生成Java实体类代码
     *
     * @param mc       方法上下文，用于SpEL表达式解析
     * @param response HTTP响应对象，包含响应体数据
     * @param config   代码生成配置
     * @throws IOException 文件写入失败时抛出
     */
    public static void generatedJavaCode(MethodContext mc, Response response, GeneratedJavaCodeConfiguration config) throws IOException {
        // 检查代码生成是否启用
        if (!Objects.equals(Boolean.TRUE, config.getEnable())) {
            log.debug("Java code generation is disabled by enable expression: {}", config.getEnable());
            return;
        }

        // 返回值类型白名单：配置了但未命中时不生成，避免为已定义好的强类型返回值生成冗余实体类
        if (!isGenerateTypeMatched(mc, config.getGenerateTypes())) {
            return;
        }

        // 校验必需参数
        validateRequiredParameters(mc, config);

        // 通过SpEL解析参数
        String savePath = mc.parseExpression(config.getSavePath(), String.class);
        String packageName = mc.parseExpression(config.getPackageName(), String.class);
        String className = mc.parseExpression(config.getClassName(), String.class);

        // 该钩子挂在每次响应上，正常路径一律用debug回显，避免每次请求刷出十余行日志
        if (log.isDebugEnabled()) {
            log.debug("Starting Java entity generation: method={}, savePath={}, packageName={}, className={}, "
                            + "useLombok={}, useSwagger={}, nestedClassStrategy={}, fieldNamingStrategy={}, serializationFramework={}",
                    mc.getCurrentAnnotatedElement().toGenericString(), savePath, packageName, className,
                    config.isUseLombok(), config.isUseSwagger(), config.getNestedClassStrategy(),
                    config.getFieldNamingStrategy(), config.getSerializationFramework());
        }

        warnIfSerializationDependencyMissing(config.getSerializationFramework());

        // 提取数据源
        Object entity = extractDataSource(mc, response, config);
        if (entity == null) {
            warnOnce("null-source-" + mc.getSignature(), "Data source is null, cannot generate Java entity classes for method: {}", mc.getSignature());
            return;
        }

        log.debug("Data source expression: {}, type: {}", config.getExtractExpression(), entity.getClass().getSimpleName());

        // 生成Java代码
        EntityGeneratorFromMap generator = new EntityGeneratorFromMap(packageName, config.isUseLombok(), config.isUseSwagger(), config.getNestedClassStrategy(), config.getFieldNamingStrategy(), config.getSerializationFramework());
        generator.setSourceDescription(resolveSourceDescription(mc));
        generator.setExtendClass(resolveSuperType(mc, config.getExtendClass()));
        generator.setImplementInterfaces(resolveImplementInterfaces(mc, config.getImplementInterfaces()));
        generator.setSerializable(config.isSerializable());

        List<JavaCode> javaCodeList = generateJavaCodeList(generator, entity, className);

        if (ContainerUtils.isEmptyCollection(javaCodeList)) {
            throw new LuckyRuntimeException(String.format("Failed to generate Java entity classes for method: %s. Data source type: %s", mc.getCurrentAnnotatedElement(), entity.getClass().getName()));
        }

        // 落盘前自检：把注定编译失败的文件拦在源码目录之外
        if (config.isValidate() && GeneratedJavaCodeValidator.isAvailable()) {
            GeneratedJavaCodeValidator.ValidationResult validationResult = GeneratedJavaCodeValidator.validate(javaCodeList);
            if (!validationResult.isPassed()) {
                warnOnce("validate-fail-" + className,
                        "Generated Java code [{}] failed to compile, so nothing was written to disk. "
                                + "Fix the issue or turn off validate to write it anyway. Errors: {}",
                        className, validationResult.errorSummary());
                return;
            }
        }

        // 将文件写入磁盘
        writeJavaFiles(javaCodeList, savePath, mc, config);

        // 配套测试骨架：它额外依赖JUnit与具体序列化库，出问题时只丢弃测试，不影响已通过的实体文件
        if (config.isGenerateExampleTest()) {
            writeExampleTest(mc, response, config, packageName, className, javaCodeList);
        }

        log.debug("Java entity generation finished for method: {}", mc.getSignature());
    }

    /**
     * 将生成的Java代码文件写入磁盘
     *
     * @param javaCodeList Java代码对象列表
     * @param savePath     目标目录路径
     * @param mc           方法上下文
     * @param config       代码生成配置
     * @throws IOException 文件写入失败时抛出
     */
    private static void writeJavaFiles(List<JavaCode> javaCodeList, String savePath, MethodContext mc, GeneratedJavaCodeConfiguration config) throws IOException {
        File saveDir = new File(savePath);
        if (!saveDir.exists()) {
            log.debug("Creating directory: {}", saveDir.getAbsolutePath());
            boolean created = saveDir.mkdirs();
            if (!created) {
                throw new LuckyRuntimeException(String.format("Failed to create directory: %s", saveDir.getAbsolutePath()));
            }
        }

        boolean cover = Objects.equals(Boolean.TRUE, config.getCover());
        int writtenCount = 0;
        int skippedCount = 0;

        for (JavaCode javaCode : javaCodeList) {
            File targetFile = new File(saveDir, javaCode.getRelativePath());

            // 文件已存在且未开启覆盖，属于「已经生成过」的正常状态；
            // 逐个打日志会把控制台刷满，这里只记debug，汇总交给下方一行
            if (targetFile.exists() && !cover) {
                log.debug("Skipping existing file: {}", targetFile.getAbsolutePath());
                skippedCount++;
                continue;
            }

            try {
                javaCode.writeToDirectory(savePath);
                log.debug("Generated: {}", targetFile.getAbsolutePath());
                writtenCount++;
            } catch (IOException e) {
                log.error("Failed to write file: {}", targetFile.getAbsolutePath(), e);
                throw new LuckyRuntimeException(String.format("Failed to write Java file: %s", javaCode.getName()), e);
            }
        }

        // 本次确实写入了文件才占一条info；全部跳过时不产生磁盘变更，不该刷屏
        if (writtenCount > 0) {
            log.info("Wrote {} Java file(s) ({} already existed and skipped) to: {}", writtenCount, skippedCount, saveDir.getAbsolutePath());
        } else {
            log.debug("Nothing written: all {} Java file(s) already exist under {} and cover is disabled", skippedCount, saveDir.getAbsolutePath());
        }
    }

    /**
     * 根据数据源生成Java代码列表
     *
     * @param generator 实体生成器
     * @param entity    数据源实体
     * @param className 基础类名
     * @return Java代码对象列表
     */
    private static List<JavaCode> generateJavaCodeList(EntityGeneratorFromMap generator, Object entity, String className) {
        List<JavaCode> javaCodeList = null;

        if (entity instanceof Map) {
            log.debug("Generating from Map data structure");
            javaCodeList = generator.generateJavaCodeList((Map<String, Object>) entity, className);
        } else if (entity instanceof List) {
            List<?> list = (List<?>) entity;
            if (ContainerUtils.isNotEmptyCollection(list)) {
                // 合并列表中所有Map元素建模：只按第一个元素生成会把后续元素独有的字段全部丢掉
                log.debug("Generating from List<Map> data structure, list size: {}", list.size());
                javaCodeList = generator.generateJavaCodeList(list, className);
            } else {
                log.debug("Cannot generate from empty List data source");
            }
        } else {
            warnOnce("unsupported-type-" + entity.getClass().getName(),
                    "Unsupported data source type: {}. Supported types: Map or List<Map>", entity.getClass().getName());
        }

        return javaCodeList;
    }

    /**
     * 从响应中提取用于实体生成的数据源
     *
     * @param mc       方法上下文
     * @param response HTTP响应
     * @param config   代码生成配置
     * @return 提取的数据源对象
     */
    private static Object extractDataSource(MethodContext mc, Response response, GeneratedJavaCodeConfiguration config) {
        String extractExpression = config.getExtractExpression();
        if (StringUtils.hasText(extractExpression)) {
            log.debug("Extracting data source using expression: {}", extractExpression);
            return mc.parseExpression(extractExpression);
        } else {
            log.debug("No extract expression provided, using full response body as data source");
            return response.getEntity(Object.class);
        }
    }

    /**
     * 生成并写入配套的报文反序列化测试骨架
     *
     * @param mc           方法上下文
     * @param response     HTTP响应，用于取报文原文本作为样本
     * @param config       代码生成配置
     * @param packageName  实体包名，测试类与实体同包以便直接引用
     * @param className    实体主类名
     * @param javaCodeList 已生成的实体代码，自检时需与测试类一起编译
     */
    private static void writeExampleTest(MethodContext mc, Response response, GeneratedJavaCodeConfiguration config,
                                         String packageName, String className, List<JavaCode> javaCodeList) {
        String rawBody;
        try {
            rawBody = response.getStringResult();
        } catch (Exception e) {
            log.debug("Cannot read the raw response body, skip example test generation: {}", e.getMessage());
            return;
        }
        if (rawBody == null || rawBody.trim().isEmpty()) {
            log.debug("Response body is empty, skip example test generation");
            return;
        }

        boolean xmlBody = false;
        try {
            xmlBody = response.isXmlBody();
        } catch (Exception e) {
            log.debug("Cannot detect the response content type, treat it as JSON: {}", e.getMessage());
        }

        JavaCode testCode = ExampleTestGenerator.generate(packageName, className, rawBody,
                config.getSerializationFramework(), xmlBody);

        // 测试类引用了刚生成的实体，单独编译会因为实体不在classpath上报错，必须把实体一起交给编译器
        if (config.isValidate() && GeneratedJavaCodeValidator.isAvailable()) {
            List<JavaCode> compileUnit = new ArrayList<>(javaCodeList);
            compileUnit.add(testCode);
            GeneratedJavaCodeValidator.ValidationResult validationResult = GeneratedJavaCodeValidator.validate(compileUnit);
            if (!validationResult.isPassed()) {
                warnOnce("example-test-validate-fail-" + className,
                        "The generated example test for [{}] failed to compile, so only the entity classes were kept. Errors: {}",
                        className, validationResult.errorSummary());
                return;
            }
        }

        try {
            String testSavePath = mc.parseExpression(config.getTestSavePath(), String.class);
            testCode.writeToDirectory(testSavePath);
            log.info("Wrote example test: {}", new File(testSavePath, testCode.getRelativePath()).getAbsolutePath());
        } catch (Exception e) {
            // 测试目录定位失败（如运行在jar环境里找不到工程根）不影响实体生成结果
            log.warn("Failed to write the example test for [{}]: {}", className, e.getMessage());
        }
    }

    /**
     * 同一标识只在首次出现时以warn输出，后续降为debug，避免每次响应重复刷屏
     *
     * @param key    告警去重标识
     * @param format 日志模板
     * @param args   模板参数
     */
    private static void warnOnce(String key, String format, Object... args) {
        if (WARNED_KEYS.add(key)) {
            log.warn(format, args);
        } else {
            log.debug(format, args);
        }
    }

    /**
     * 选了某个序列化框架但工程里没引对应依赖时提前提示。
     * 这种组合下生成的代码必然编译失败，直接点明依赖坐标比让用户猜报错有用
     *
     * @param framework 配置的序列化框架
     */
    private static void warnIfSerializationDependencyMissing(SerializationFramework framework) {
        if (framework == null || framework.isDependencyPresent()) {
            return;
        }
        warnOnce("missing-dependency-" + framework.name(),
                "serializationFramework is set to {} but [{}] is not on the classpath. "
                        + "Generated code will not compile until the dependency is added: {}",
                framework, framework.getProbeClassName(), framework.getDependencyCoordinate());
    }

    /**
     * 构建写入生成类Javadoc的来源说明
     *
     * @param mc 方法上下文
     * @return 来源说明
     */
    private static String resolveSourceDescription(MethodContext mc) {
        return "API method " + mc.getCurrentAnnotatedElement().toGenericString();
    }

    /**
     * 解析主类要继承的父类配置
     *
     * @param mc          方法上下文
     * @param configured  配置的父类名（可以是SpEL表达式）
     * @return 解析后的父类名，未配置时为空串
     */
    private static String resolveSuperType(MethodContext mc, String configured) {
        if (!StringUtils.hasText(configured)) {
            return "";
        }
        String resolved = mc.parseExpression(configured, String.class);
        return resolved == null ? "" : resolved.trim();
    }

    /**
     * 解析主类要实现的接口配置，逐项执行SpEL表达式并丢弃空项
     *
     * @param mc         方法上下文
     * @param configured 配置的接口名数组（每项可以是SpEL表达式）
     * @return 解析后的接口名数组
     */
    private static String[] resolveImplementInterfaces(MethodContext mc, String[] configured) {
        if (configured == null || configured.length == 0) {
            return new String[0];
        }
        List<String> resolvedList = new ArrayList<>();
        for (String itf : configured) {
            if (!StringUtils.hasText(itf)) {
                continue;
            }
            String resolved = mc.parseExpression(itf, String.class);
            if (StringUtils.hasText(resolved)) {
                resolvedList.add(resolved.trim());
            }
        }
        return resolvedList.toArray(new String[0]);
    }

    /**
     * 判断方法返回值类型是否命中配置的白名单。
     * 未配置白名单（空数组）时不做类型限制，直接判定为命中；
     * 命中规则为配置类型是返回值类型本身或其父类/接口，只比较最外层类型，不递归检查泛型参数
     *
     * @param mc            方法上下文
     * @param generateTypes 配置的返回值类型白名单
     * @return true表示命中，可以继续执行生成逻辑
     */
    private static boolean isGenerateTypeMatched(MethodContext mc, Class<?>[] generateTypes) {
        if (generateTypes == null || generateTypes.length == 0) {
            log.debug("No generateTypes configured, type filter is disabled for method: {}", mc.getSignature());
            return true;
        }

        ResolvableType resultResolvableType = mc.getResultResolvableType();
        Class<?> resultType = resultResolvableType == null ? Object.class : resultResolvableType.toClass();
        for (Class<?> candidate : generateTypes) {
            if (candidate != null && isAssignableResultType(candidate, resultType)) {
                return true;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Result type [{}] of method [{}] does not match the configured generateTypes {}, skip Java entity generation.",
                    resultType.getTypeName(), mc.getSignature(), Arrays.toString(generateTypes));
        }
        return false;
    }

    /**
     * 判断返回值类型是否满足配置的允许类型，void 与 Void 视为同一类型
     *
     * @param configured 配置的允许类型
     * @param actual     方法实际的返回值类型
     * @return true表示满足匹配条件
     */
    private static boolean isAssignableResultType(Class<?> configured, Class<?> actual) {
        if (configured == Void.class || configured == void.class) {
            return actual == Void.class || actual == void.class;
        }
        return configured.isAssignableFrom(actual);
    }

    /**
     * 校验所有必需参数是否已提供
     *
     * @param mc     方法上下文
     * @param config 代码生成配置
     * @throws IllegalArgumentException 如果有任何必需参数缺失
     */
    private static void validateRequiredParameters(MethodContext mc, GeneratedJavaCodeConfiguration config) {
        String savePath = config.getSavePath();
        if (!StringUtils.hasText(savePath)) {
            throw new IllegalArgumentException(String.format("savePath is required but empty. Method: %s", mc.getSignature()));
        }

        String packageName = config.getPackageName();
        if (!StringUtils.hasText(packageName)) {
            throw new IllegalArgumentException(String.format("packageName is required but empty. Method: %s", mc.getSignature()));
        }

        String className = config.getClassName();
        if (!StringUtils.hasText(className)) {
            throw new IllegalArgumentException(String.format("className is required but empty. Method: %s", mc.getSignature()));
        }
    }

}