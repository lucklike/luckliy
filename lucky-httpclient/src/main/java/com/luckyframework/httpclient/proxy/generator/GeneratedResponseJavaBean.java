package com.luckyframework.httpclient.proxy.generator;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.hook.AsyncHook;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

/**
 * Annotation for automatically generating Java entity classes from HTTP response data.
 * The entity classes will be generated during the response lifecycle phase.
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/6/15
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpELImport(GeneratedResponseJavaBean.GeneratedJavaCodeFunctionAndCallback.class)
public @interface GeneratedResponseJavaBean {

    /**
     * SpEL expression to enable/disable code generation.
     * The code generation will be executed only when this expression evaluates to true.
     *
     * @return enable condition expression
     */
    String enable() default "";

    /**
     * SpEL expression to determine whether to overwrite existing files.
     *
     * @return overwrite condition expression
     */
    String cover() default "false";

    /**
     * SpEL expression to extract data source from response for entity generation.
     * If not specified, the entire response body will be used as the data source.
     *
     * @return extraction expression
     */
    String extractExpression() default "";

    /**
     * Required. The directory path where Java files will be saved.
     * Supports SpEL expressions.
     *
     * @return save path
     */
    String savePath() default "";

    /**
     * The package name for generated Java classes.
     * Supports SpEL expressions.
     *
     * @return package name
     */
    String packageName() default "#{$class$.getPackage().getName() + '.resp'}";

    /**
     * Required. The class name for the main generated Java class.
     * Supports SpEL expressions.
     *
     * @return class name
     */
    String className() default "";

    /**
     * Whether to use Lombok annotations (e.g., @Data, @Builder).
     *
     * @return true if using Lombok, false otherwise
     */
    boolean useLombok() default true;

    /**
     * Whether to use Swagger annotations (e.g., @ApiModel, @ApiModelProperty).
     *
     * @return true if using Swagger, false otherwise
     */
    boolean useSwagger() default false;

    /**
     * Strategy for handling nested classes.
     *
     * @return nested class strategy
     */
    NestedClassStrategy nestedClassStrategy() default NestedClassStrategy.INNER_CLASS;

    /**
     * Strategy for naming Java fields from JSON field names.
     *
     * @return field naming strategy
     */
    FieldNamingStrategy fieldNamingStrategy() default FieldNamingStrategy.ORIGINAL;

    /**
     * Serialization framework to support with annotations.
     *
     * @return serialization framework
     */
    SerializationFramework serializationFramework() default SerializationFramework.NONE;

    /**
     * Callback class that handles the Java code generation logic.
     */
    class GeneratedJavaCodeFunctionAndCallback {

        private static final Logger log = LoggerFactory.getLogger(GeneratedJavaCodeFunctionAndCallback.class);

        @AsyncHook
        @Callback(lifecycle = Lifecycle.RESPONSE, errorInterrupt = false)
        public static void generatedJavaCode(MethodContext mc, Response response) throws IOException {
            GeneratedResponseJavaBean ann = mc.getSameAnnotationCombined(GeneratedResponseJavaBean.class);

            // Check if generation is enabled
            String enable = ann.enable();
            if (StringUtils.hasText(enable) && !mc.parseExpression(enable, boolean.class)) {
                log.debug("Java code generation is disabled by enable expression: {}", enable);
                return;
            }

            // Validate required parameters
            validateRequiredParameters(mc, ann);

            // Resolve parameters with SpEL
            String savePath = mc.parseExpression(ann.savePath(), String.class);
            String packageName = mc.parseExpression(ann.packageName(), String.class);
            String className = mc.parseExpression(ann.className(), String.class);

            // Log generation start
            log.info("========== Starting Java Entity Class Generation ==========");
            log.info("API Method: {}", mc.getCurrentAnnotatedElement().toGenericString());
            log.info("Generation Configuration:");
            log.info("  - Save Path: {}", savePath);
            log.info("  - Package Name: {}", packageName);
            log.info("  - Class Name: {}", className);
            log.info("  - Use Lombok: {}", ann.useLombok());
            log.info("  - Use Swagger: {}", ann.useSwagger());
            log.info("  - Nested Class Strategy: {}", ann.nestedClassStrategy());
            log.info("  - Field Naming Strategy: {}", ann.fieldNamingStrategy());
            log.info("  - Serialization Framework: {}", ann.serializationFramework());

            // Extract data source
            Object entity = extractDataSource(mc, response, ann);
            if (entity == null) {
                log.warn("Data source is null, cannot generate Java entity classes");
                return;
            }

            log.info("Data source type: {}", entity.getClass().getSimpleName());

            // Generate Java code
            EntityGeneratorFromMap generator = new EntityGeneratorFromMap(
                    packageName,
                    ann.useLombok(),
                    ann.useSwagger(),
                    ann.nestedClassStrategy(),
                    ann.fieldNamingStrategy(),
                    ann.serializationFramework()
            );

            List<JavaCode> javaCodeList = generateJavaCodeList(mc, generator, entity, className);

            if (ContainerUtils.isEmptyCollection(javaCodeList)) {
                throw new LuckyRuntimeException(
                        String.format("Failed to generate Java entity classes for method: %s. Data source type: %s",
                                mc.getCurrentAnnotatedElement(), entity.getClass().getName())
                );
            }

            // Write files to disk
            writeJavaFiles(javaCodeList, savePath, mc, ann);

            // Log generation completion
            log.info("========== Java Entity Class Generation Completed ==========");
            log.info("Total files generated: {}", javaCodeList.size());
            log.info("Save directory: {}", new File(savePath).getAbsolutePath());
        }

        /**
         * Validate that all required parameters are provided.
         *
         * @param mc  method context
         * @param ann annotation instance
         * @throws IllegalArgumentException if any required parameter is missing
         */
        private static void validateRequiredParameters(MethodContext mc, GeneratedResponseJavaBean ann) {
            String savePath = ann.savePath();
            if (!StringUtils.hasText(savePath)) {
                throw new IllegalArgumentException(
                        String.format("@GeneratedResponseJavaBean.savePath is required but empty. Method: %s",
                                mc.getSignature())
                );
            }

            String packageName = ann.packageName();
            if (!StringUtils.hasText(packageName)) {
                throw new IllegalArgumentException(
                        String.format("@GeneratedResponseJavaBean.packageName is required but empty. Method: %s",
                                mc.getSignature())
                );
            }

            String className = ann.className();
            if (!StringUtils.hasText(className)) {
                throw new IllegalArgumentException(
                        String.format("@GeneratedResponseJavaBean.className is required but empty. Method: %s",
                                mc.getSignature())
                );
            }
        }

        /**
         * Extract data source from response for entity generation.
         *
         * @param mc      method context
         * @param response HTTP response
         * @param ann     annotation instance
         * @return extracted data source object
         */
        private static Object extractDataSource(MethodContext mc, Response response, GeneratedResponseJavaBean ann) {
            String extractExpression = ann.extractExpression();
            if (StringUtils.hasText(extractExpression)) {
                log.debug("Extracting data source using expression: {}", extractExpression);
                return mc.parseExpression(extractExpression);
            } else {
                log.debug("No extract expression provided, using full response body as data source");
                return response.getEntity(Object.class);
            }
        }

        /**
         * Generate Java code list from the data source.
         *
         * @param mc        method context
         * @param generator entity generator
         * @param entity    data source entity
         * @param className base class name
         * @return list of JavaCode objects
         */
        private static List<JavaCode> generateJavaCodeList(MethodContext mc, EntityGeneratorFromMap generator,
                                                           Object entity, String className) {
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
                        log.warn("Cannot generate from List with non-Map elements. Element type: {}",
                                firstItem != null ? firstItem.getClass().getSimpleName() : "null");
                    }
                } else {
                    log.warn("Cannot generate from empty List data source");
                }
            } else {
                log.warn("Unsupported data source type: {}. Supported types: Map or List<Map>",
                        entity.getClass().getName());
            }

            return javaCodeList;
        }

        /**
         * Write generated Java files to disk.
         *
         * @param javaCodeList list of JavaCode objects
         * @param savePath     target directory path
         * @param mc           method context
         * @param ann          annotation instance
         * @throws IOException if file writing fails
         */
        private static void writeJavaFiles(List<JavaCode> javaCodeList, String savePath,
                                           MethodContext mc, GeneratedResponseJavaBean ann) throws IOException {
            File saveDir = new File(savePath);
            if (!saveDir.exists()) {
                log.info("Creating directory: {}", saveDir.getAbsolutePath());
                boolean created = saveDir.mkdirs();
                if (!created) {
                    throw new LuckyRuntimeException(
                            String.format("Failed to create directory: %s", saveDir.getAbsolutePath())
                    );
                }
            }

            log.info("Writing {} Java file(s) to directory: {}", javaCodeList.size(), saveDir.getAbsolutePath());

            int writtenCount = 0;
            int skippedCount = 0;

            for (JavaCode javaCode : javaCodeList) {
                File targetFile = new File(saveDir, javaCode.getRelativePath());

                // Check if file should be overwritten
                if (targetFile.exists() && !shouldOverwrite(ann, mc, targetFile)) {
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
                    throw new LuckyRuntimeException(
                            String.format("Failed to write Java file: %s", javaCode.getName()), e
                    );
                }
            }

            log.info("File writing completed - Written: {}, Skipped: {}, Total: {}",
                    writtenCount, skippedCount, javaCodeList.size());
        }

        /**
         * Determine whether to overwrite an existing file.
         *
         * @param ann        annotation instance
         * @param mc         method context
         * @param targetFile target file to check
         * @return true if should overwrite, false otherwise
         */
        private static boolean shouldOverwrite(GeneratedResponseJavaBean ann, MethodContext mc, File targetFile) {
            String coverExpr = ann.cover();
            if (StringUtils.hasText(coverExpr)) {
                try {
                    // Add targetFile to context for expression evaluation
                    mc.getContextVar().addVariable("targetFile", targetFile);
                    boolean shouldCover = mc.parseExpression(coverExpr, boolean.class);
                    mc.getContextVar().removeVariable("targetFile");
                    log.debug("Overwrite decision for {}: {} (expression: {})",
                            targetFile.getName(), shouldCover, coverExpr);
                    return shouldCover;
                } catch (Exception e) {
                    log.warn("Failed to parse cover expression: {}, using default (overwrite=false)",
                            coverExpr, e);
                    return false;
                }
            }
            return false; // Default: do not overwrite
        }
    }

}