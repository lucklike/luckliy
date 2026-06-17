package com.luckyframework.httpclient.proxy.generator;

import com.luckyframework.common.Bool;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 生成的Java代码工具类
 * 提供从HTTP响应生成Java实体类的核心功能
 */
public class GeneratedJavaCodeUtils {

    private static final Logger log = LoggerFactory.getLogger(GeneratedJavaCodeUtils.class);

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

        // 校验必需参数
        validateRequiredParameters(mc, config);

        // 通过SpEL解析参数
        String savePath = mc.parseExpression(config.getSavePath(), String.class);
        String packageName = mc.parseExpression(config.getPackageName(), String.class);
        String className = mc.parseExpression(config.getClassName(), String.class);

        // 记录生成开始日志
        log.info("========== Starting Java Entity Class Generation ==========");
        log.info("API Method: {}", mc.getCurrentAnnotatedElement().toGenericString());
        log.info("Generation Configuration:");
        log.info("  - Save Path: {}", savePath);
        log.info("  - Package Name: {}", packageName);
        log.info("  - Class Name: {}", className);
        log.info("  - Use Lombok: {}", config.isUseLombok());
        log.info("  - Use Swagger: {}", config.isUseSwagger());
        log.info("  - Nested Class Strategy: {}", config.getNestedClassStrategy());
        log.info("  - Field Naming Strategy: {}", config.getFieldNamingStrategy());
        log.info("  - Serialization Framework: {}", config.getSerializationFramework());

        // 提取数据源
        Object entity = extractDataSource(mc, response, config);
        if (entity == null) {
            log.warn("Data source is null, cannot generate Java entity classes");
            return;
        }

        log.info("Data source type: {}", entity.getClass().getSimpleName());

        // 生成Java代码
        EntityGeneratorFromMap generator = new EntityGeneratorFromMap(packageName, config.isUseLombok(), config.isUseSwagger(), config.getNestedClassStrategy(), config.getFieldNamingStrategy(), config.getSerializationFramework());

        List<JavaCode> javaCodeList = generateJavaCodeList(generator, entity, className);

        if (ContainerUtils.isEmptyCollection(javaCodeList)) {
            throw new LuckyRuntimeException(String.format("Failed to generate Java entity classes for method: %s. Data source type: %s", mc.getCurrentAnnotatedElement(), entity.getClass().getName()));
        }

        // 将文件写入磁盘
        writeJavaFiles(javaCodeList, savePath, mc, config);

        // 记录生成完成日志
        log.info("========== Java Entity Class Generation Completed ==========");
        log.info("Total files generated: {}", javaCodeList.size());
        log.info("Save directory: {}", new File(savePath).getAbsolutePath());
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
            log.info("Creating directory: {}", saveDir.getAbsolutePath());
            boolean created = saveDir.mkdirs();
            if (!created) {
                throw new LuckyRuntimeException(String.format("Failed to create directory: %s", saveDir.getAbsolutePath()));
            }
        }

        log.info("Writing {} Java file(s) to directory: {}", javaCodeList.size(), saveDir.getAbsolutePath());

        int writtenCount = 0;
        int skippedCount = 0;

        for (JavaCode javaCode : javaCodeList) {
            File targetFile = new File(saveDir, javaCode.getRelativePath());

            // 检查文件是否应被覆盖
            if (targetFile.exists() && !Objects.equals(Boolean.TRUE, config.getCover())) {
                log.debug("Skipping existing file: {}", targetFile.getAbsolutePath());
                skippedCount++;
                continue;
            }

            try {
                javaCode.writeToDirectory(savePath);
                log.info("Generated: {}", targetFile.getAbsolutePath());
                writtenCount++;
            } catch (IOException e) {
                log.error("Failed to write file: {}", targetFile.getAbsolutePath(), e);
                throw new LuckyRuntimeException(String.format("Failed to write Java file: %s", javaCode.getName()), e);
            }
        }

        log.info("File writing completed - Written: {}, Skipped: {}, Total: {}", writtenCount, skippedCount, javaCodeList.size());
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
                Object firstItem = list.get(0);
                if (firstItem instanceof Map) {
                    log.debug("Generating from List<Map> data structure, list size: {}", list.size());
                    log.debug("Using first element as template for code generation");
                    javaCodeList = generator.generateJavaCodeList((Map<String, Object>) firstItem, className);
                } else {
                    log.warn("Cannot generate from List with non-Map elements. Element type: {}", firstItem != null ? firstItem.getClass().getSimpleName() : "null");
                }
            } else {
                log.warn("Cannot generate from empty List data source");
            }
        } else {
            log.warn("Unsupported data source type: {}. Supported types: Map or List<Map>", entity.getClass().getName());
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