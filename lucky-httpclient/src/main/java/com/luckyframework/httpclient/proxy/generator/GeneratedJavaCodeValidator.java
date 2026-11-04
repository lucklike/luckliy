package com.luckyframework.httpclient.proxy.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 生成代码的落盘前自检器
 * 用JDK自带的javac把待写入的源码在内存里编译一遍，提前拦住语法错误、缺import、
 * 注解属性缺失等问题，避免把注定编译失败的文件写进用户源码目录
 */
public class GeneratedJavaCodeValidator {

    private static final Logger log = LoggerFactory.getLogger(GeneratedJavaCodeValidator.class);

    /**
     * 单次自检最多回显的错误条数，避免刷屏
     */
    private static final int MAX_REPORT_ERRORS = 8;

    private GeneratedJavaCodeValidator() {
    }

    /**
     * 当前运行环境是否具备编译能力（JRE部署下为false，此时应跳过自检）
     *
     * @return true表示可以执行内存编译自检
     */
    public static boolean isAvailable() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    /**
     * 校验一组Java代码能否编译通过
     *
     * @param codes 待校验的代码集合，同一个包内的多个类需一起传入，否则互相引用会被误判
     * @return 校验结果，通过时{@link ValidationResult#isPassed()}为true
     */
    public static ValidationResult validate(List<JavaCode> codes) {
        ValidationResult result = new ValidationResult();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            result.setSkipped(true);
            return result;
        }
        if (codes == null || codes.isEmpty()) {
            result.setSkipped(true);
            return result;
        }

        StandardJavaFileManager fileManager = null;
        try {
            fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
            List<JavaFileObject> sources = new ArrayList<>(codes.size());
            for (JavaCode code : codes) {
                sources.add(new InMemorySource(code));
            }

            File outputDir = createTempOutputDir();
            List<String> options = new ArrayList<>();
            // 禁用注解处理：Lombok等框架的注解处理器会在自检阶段生成额外代码，干扰对生成产物本身的判断
            options.add("-proc:none");
            options.add("-encoding");
            options.add("UTF-8");
            options.add("-d");
            options.add(outputDir.getAbsolutePath());
            String classpath = System.getProperty("java.class.path");
            if (classpath != null && !classpath.trim().isEmpty()) {
                options.add("-classpath");
                options.add(classpath);
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            boolean executed = compiler.getTask(null, fileManager, diagnostics, options, null, sources).call();
            if (executed) {
                return result;
            }

            List<String> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                    continue;
                }
                String source = diagnostic.getSource() == null ? "?" : diagnostic.getSource().getName();
                errors.add(source + ":" + diagnostic.getLineNumber() + " " + diagnostic.getMessage(null));
            }
            result.setErrors(errors);
            return result;
        } catch (IOException e) {
            // 自检器自身出错不应阻断生成流程，降级为跳过
            log.debug("Java code self-check skipped due to error: {}", e.getMessage());
            result.setSkipped(true);
            return result;
        } finally {
            closeQuietly(fileManager);
        }
    }

    /**
     * 创建存放自检产物（.class）的临时目录，用完即由JVM退出时清理
     */
    private static File createTempOutputDir() throws IOException {
        File base = new File(System.getProperty("java.io.tmpdir"));
        File dir = new File(base, "lucky-generated-check-" + System.nanoTime());
        if (!dir.mkdirs()) {
            throw new IOException("Failed to create temporary directory: " + dir.getAbsolutePath());
        }
        dir.deleteOnExit();
        return dir;
    }

    private static void closeQuietly(StandardJavaFileManager fileManager) {
        if (fileManager == null) {
            return;
        }
        try {
            fileManager.close();
        } catch (IOException e) {
            log.trace("Failed to close java file manager", e);
        }
    }

    /**
     * 以内存字符串形式提供给javac的源码
     */
    private static class InMemorySource extends SimpleJavaFileObject {

        private final String content;

        InMemorySource(JavaCode code) {
            super(URI.create("file:///" + code.getPackageName().replace('.', '/') + "/" + code.getName() + ".java"),
                    Kind.SOURCE);
            this.content = code.getContent();
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

    /**
     * 自检结果
     */
    public static class ValidationResult {

        private boolean skipped;

        private List<String> errors = Collections.emptyList();

        public boolean isSkipped() {
            return skipped;
        }

        void setSkipped(boolean skipped) {
            this.skipped = skipped;
        }

        public boolean isPassed() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        void setErrors(List<String> errors) {
            this.errors = errors == null ? Collections.emptyList() : errors;
        }

        /**
         * 输出带条数上限的错误摘要，用于日志回显
         *
         * @return 多行错误摘要文本
         */
        public String errorSummary() {
            if (isPassed()) {
                return "";
            }
            List<String> shown = errors.size() > MAX_REPORT_ERRORS
                    ? new ArrayList<>(errors.subList(0, MAX_REPORT_ERRORS))
                    : errors;
            StringBuilder sb = new StringBuilder();
            for (String error : shown) {
                sb.append("\n  - ").append(error);
            }
            if (errors.size() > shown.size()) {
                sb.append("\n  - ... and ").append(errors.size() - shown.size()).append(" more similar error(s)");
            }
            return sb.toString();
        }
    }
}
