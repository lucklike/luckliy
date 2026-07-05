package com.luckyframework.httpclient.proxy.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Java code file representation with package and class information.
 * Provides methods to write Java source files to various locations.
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/6/15
 */
public class JavaCode {
    private final String packageName;
    private final String name;
    private final String content;

    public JavaCode(String packageName, String name, String content) {
        this.packageName = packageName;
        this.name = name;
        this.content = content;
    }

    public static JavaCode of(String packageName, String name, String content) {
        return new JavaCode(packageName, name, content);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    /**
     * Write Java code to the specified file.
     *
     * @param file target file
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void writerToFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        // Ensure parent directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Use try-with-resources to ensure the file is properly closed
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            writer.write(content);
            writer.flush();
        }
    }

    /**
     * Write Java code to the specified file path.
     *
     * @param path target file path
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void writeToPath(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        writerToFile(new File(path));
    }

    /**
     * Write Java code to the specified directory, automatically creating the directory structure based on the package name.
     *
     * @param baseDir base directory
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void writeToDirectory(File baseDir) throws IOException {
        if (baseDir == null) {
            throw new IllegalArgumentException("Base directory cannot be null");
        }

        // Create directory structure based on package name
        String packagePath = packageName.replace('.', File.separatorChar);
        File targetDir = new File(baseDir, packagePath);

        // Ensure directory exists
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // Target file
        File targetFile = new File(targetDir, name + ".java");
        writerToFile(targetFile);
    }

    /**
     * Write Java code to the current working directory, automatically creating the directory structure based on the package name.
     *
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void writeToCurrentDirectory() throws IOException {
        writeToDirectory(new File(System.getProperty("user.dir")));
    }

    /**
     * Write Java code to the specified directory path, automatically creating the directory structure based on the package name.
     *
     * @param baseDirPath base directory path
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void writeToDirectory(String baseDirPath) throws IOException {
        if (baseDirPath == null || baseDirPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Base directory path cannot be null or empty");
        }
        writeToDirectory(new File(baseDirPath));
    }

    /**
     * Get the suggested file path based on package name and class name.
     *
     * @param baseDir base directory
     * @return suggested file path
     */
    public File getSuggestedFile(File baseDir) {
        String packagePath = packageName.replace('.', File.separatorChar);
        return new File(baseDir, packagePath + File.separator + name + ".java");
    }

    /**
     * Get the suggested file path relative to the current working directory.
     *
     * @return suggested file path
     */
    public File getSuggestedFile() {
        return getSuggestedFile(new File(System.getProperty("user.dir")));
    }

    /**
     * Get the relative path based on package name and class name.
     * For example: packageName="com.example.dto", name="UserInfo"
     * Returns: com/example/dto/UserInfo.java (using system file separator)
     *
     * @return relative path string
     */
    public String getRelativePath() {
        return getRelativePath(File.separatorChar);
    }

    /**
     * Get the relative path based on package name and class name with specified separator character.
     * For example: packageName="com.example.dto", name="UserInfo", separator='/'
     * Returns: com/example/dto/UserInfo.java
     *
     * @param separator path separator character
     * @return relative path string
     */
    public String getRelativePath(char separator) {
        String packagePath = packageName.replace('.', separator);
        return packagePath + separator + name + ".java";
    }

    /**
     * Get the relative path based on package name and class name with specified separator string.
     * For example: packageName="com.example.dto", name="UserInfo", separator="/"
     * Returns: com/example/dto/UserInfo.java
     *
     * @param separator path separator string
     * @return relative path string
     */
    public String getRelativePath(String separator) {
        String packagePath = packageName.replace(".", separator);
        return packagePath + separator + name + ".java";
    }

    /**
     * Get the Unix-style relative path (using "/" as separator).
     * This is useful for logging and cross-platform path display.
     * For example: packageName="com.example.dto", name="UserInfo"
     * Returns: com/example/dto/UserInfo.java
     *
     * @return Unix-style relative path
     */
    public String getUnixStyleRelativePath() {
        return getRelativePath('/');
    }

    /**
     * Get the target file object relative to the base directory.
     *
     * @param baseDir base directory
     * @return target file object
     */
    public File getTargetFile(File baseDir) {
        return new File(baseDir, getRelativePath());
    }

    /**
     * Get the target file object relative to the base directory path.
     *
     * @param baseDirPath base directory path
     * @return target file object
     */
    public File getTargetFile(String baseDirPath) {
        return getTargetFile(new File(baseDirPath));
    }
}