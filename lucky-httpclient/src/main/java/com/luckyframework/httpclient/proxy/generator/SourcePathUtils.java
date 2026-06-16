package com.luckyframework.httpclient.proxy.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for locating Java source file paths in Maven/Gradle projects.
 * Works by detecting project structure from compiled class location in IDE environment.
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/6/15
 */
public class SourcePathUtils {

    private static final Logger log = LoggerFactory.getLogger(SourcePathUtils.class);

    /**
     * Mapping from compiled output directory patterns to source root directory patterns
     */
    private static final Map<String, String> SOURCE_ROOT_MAPPINGS = new HashMap<>();

    static {
        // Maven
        SOURCE_ROOT_MAPPINGS.put("target/classes", "src/main/java");
        SOURCE_ROOT_MAPPINGS.put("target/test-classes", "src/test/java");

        // Gradle
        SOURCE_ROOT_MAPPINGS.put("build/classes/java/main", "src/main/java");
        SOURCE_ROOT_MAPPINGS.put("build/classes/java/test", "src/test/java");

        // IntelliJ IDEA
        SOURCE_ROOT_MAPPINGS.put("out/production/", "src/main/java");
        SOURCE_ROOT_MAPPINGS.put("out/test/", "src/test/java");

        // Eclipse
        SOURCE_ROOT_MAPPINGS.put("bin/main", "src/main/java");
        SOURCE_ROOT_MAPPINGS.put("bin/test", "src/test/java");
    }

    /**
     * Get the Java source root directory for a given class.
     * For Maven: src/main/java or src/test/java
     * For Gradle: src/main/java or src/test/java
     *
     * @param clazz the class
     * @return source root directory absolute path
     * @throws IllegalStateException if cannot locate source root
     */
    public static String getSourceRootPath(Class<?> clazz) throws UnsupportedEncodingException {
        return getSourceRootPath(clazz, false);
    }

    /**
     * Get the Java source root directory for a given class.
     *
     * @param clazz            the class
     * @param createIfNotExists create directory if not exists
     * @return source root directory absolute path
     * @throws IllegalStateException if cannot locate source root
     */
    public static String getSourceRootPath(Class<?> clazz, boolean createIfNotExists) throws UnsupportedEncodingException {
        log.debug("Resolving source root for class: {}", clazz.getName());

        // 1. Get compiled class location
        String classPath = getCompiledClassPath(clazz);
        log.debug("Compiled class path: {}", classPath);

        // 2. Find project root by locating pom.xml or build.gradle
        File projectRoot = findProjectRoot(new File(classPath));
        log.debug("Project root: {}", projectRoot.getAbsolutePath());

        // 3. Determine source directory based on class path and project root
        String sourceDir = determineSourceDirectory(classPath, projectRoot);
        log.debug("Source root: {}", sourceDir);

        // 4. Create directory if needed
        if (createIfNotExists) {
            File dir = new File(sourceDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    log.debug("Created source root directory: {}", sourceDir);
                }
            }
        }

        return sourceDir;
    }

    /**
     * Get the full source file path for a given class
     *
     * @param clazz the class
     * @return source file absolute path
     */
    public static String getSourceFilePath(Class<?> clazz) throws UnsupportedEncodingException {
        String sourceRoot = getSourceRootPath(clazz);
        String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
        String fileName = clazz.getSimpleName() + ".java";
        return sourceRoot + File.separator + packagePath + File.separator + fileName;
    }

    /**
     * Get the full source file path for a given class with custom class name
     *
     * @param clazz     the class
     * @param className custom class name for generated file
     * @return source file absolute path
     */
    public static String getSourceFilePath(Class<?> clazz, String className) throws UnsupportedEncodingException {
        String sourceRoot = getSourceRootPath(clazz);
        String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
        return sourceRoot + File.separator + packagePath + File.separator + className + ".java";
    }

    /**
     * Get compiled class file path
     */
    private static String getCompiledClassPath(Class<?> clazz) throws UnsupportedEncodingException {
        String className = clazz.getSimpleName();
        URL resource = clazz.getResource(className + ".class");

        if (resource == null) {
            throw new IllegalStateException(
                    String.format("Cannot locate class file for: %s", clazz.getName())
            );
        }

        String path = URLDecoder.decode(resource.getPath(), "utf-8");

        // Handle Windows path (remove leading slash)
        if (path.startsWith("/") && File.separatorChar == '\\') {
            path = path.substring(1);
        }

        // Remove any query parameters
        int queryIndex = path.indexOf('?');
        if (queryIndex > 0) {
            path = path.substring(0, queryIndex);
        }

        return path;
    }

    /**
     * Find project root by locating pom.xml or build.gradle
     */
    private static File findProjectRoot(File startFile) {
        File current = startFile;

        while (current != null) {
            File parent = current.getParentFile();
            if (parent == null) {
                break;
            }

            // Check for Maven or Gradle project markers
            File pomFile = new File(parent, "pom.xml");
            File gradleFile = new File(parent, "build.gradle");
            File gradleKtsFile = new File(parent, "build.gradle.kts");

            if (pomFile.exists() || gradleFile.exists() || gradleKtsFile.exists()) {
                log.debug("Found project marker in: {}", parent.getAbsolutePath());
                return parent;
            }

            current = parent;
        }

        throw new IllegalStateException(
                String.format("Cannot find project root (no pom.xml or build.gradle found). " +
                        "Start path: %s", startFile.getAbsolutePath())
        );
    }

    /**
     * Determine source directory based on class path and project root
     */
    private static String determineSourceDirectory(String classPath, File projectRoot) {
        String relativePath = getRelativePath(classPath, projectRoot);
        log.debug("Relative path from project root: {}", relativePath);

        // Try to match against known output directory patterns
        for (Map.Entry<String, String> mapping : SOURCE_ROOT_MAPPINGS.entrySet()) {
            String outputPattern = mapping.getKey();
            String sourcePattern = mapping.getValue();

            if (relativePath.contains(outputPattern)) {
                String sourceDir = projectRoot.getAbsolutePath() + File.separator + sourcePattern;
                log.debug("Matched pattern '{}' -> source root: {}", outputPattern, sourceDir);
                return sourceDir;
            }
        }

        // Fallback: try to guess from package path
        log.warn("No matching pattern found for relative path: {}, using fallback", relativePath);
        String fallbackSourceDir = projectRoot.getAbsolutePath() + File.separator + "src/main/java".replace("/", File.separator);
        log.debug("Using fallback source root: {}", fallbackSourceDir);
        return fallbackSourceDir;
    }

    /**
     * Get relative path from project root
     */
    private static String getRelativePath(String fullPath, File projectRoot) {
        String rootPath = projectRoot.getAbsolutePath();
        String normalizedFullPath = fullPath.replace('\\', '/');
        String normalizedRootPath = rootPath.replace('\\', '/');

        if (normalizedFullPath.startsWith(normalizedRootPath)) {
            String relative = normalizedFullPath.substring(normalizedRootPath.length());
            // Remove leading slash
            if (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            return relative;
        }

        return fullPath;
    }

    /**
     * Check if currently running in IDE (not from JAR)
     *
     * @param clazz the class to check
     * @return true if running in IDE, false if running from JAR
     */
    public static boolean isRunningInIde(Class<?> clazz) {
        try {
            String path = getCompiledClassPath(clazz);
            // In JAR, path contains ".jar!" or starts with "jar:"
            boolean isJar = path.contains(".jar!") || path.startsWith("jar:");
            log.debug("Class path: {}, isJar: {}", path, isJar);
            return !isJar;
        } catch (Exception e) {
            log.warn("Failed to check IDE environment: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the compiled output directory for a given class
     *
     * @param clazz the class
     * @return compiled output directory
     */
    public static String getCompiledOutputDir(Class<?> clazz) throws UnsupportedEncodingException {
        String classPath = getCompiledClassPath(clazz);
        File classFile = new File(classPath);
        File parentDir = classFile.getParentFile();

        // Navigate up to find the root of compiled classes
        while (parentDir != null) {
            String path = parentDir.getAbsolutePath().replace('\\', '/');
            boolean isOutputRoot = false;

            for (String pattern : SOURCE_ROOT_MAPPINGS.keySet()) {
                if (path.endsWith(pattern)) {
                    isOutputRoot = true;
                    break;
                }
            }

            if (isOutputRoot) {
                return parentDir.getAbsolutePath();
            }

            parentDir = parentDir.getParentFile();
        }

        return classFile.getParentFile().getAbsolutePath();
    }

    /**
     * Get the source root directory with custom source path pattern
     *
     * @param clazz            the class
     * @param customSourcePath custom source path relative to project root (e.g., "src/main/kotlin")
     * @return source root directory absolute path
     */
    public static String getSourceRootPathWithCustomPath(Class<?> clazz, String customSourcePath) throws UnsupportedEncodingException {
        String classPath = getCompiledClassPath(clazz);
        File projectRoot = findProjectRoot(new File(classPath));
        return projectRoot.getAbsolutePath() + File.separator + customSourcePath;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println(getSourceRootPath(GeneratedResponseJavaBean.class));
    }
}