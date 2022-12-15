package com.luckyframework.common;


import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.serializable.SerializationSchemeFactory;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.util.FileCopyUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 从classpath或者工作目录下获取资源
 * @author fk7075
 * @version 1.0.0
 * @date 2020/10/3 12:34 上午
 */
@SuppressWarnings("all")
public abstract class Resources {

    /** 当前工作目录绝对路径*/
    private static final String WORKING_DIRECTORY;
    /** 当前工作目录文件*/
    private static final File WORKING_DIRECTORY_FILE;

    static {
        String userDir = System.getProperty("user.dir");
        WORKING_DIRECTORY = userDir.endsWith(File.separator) ? userDir : userDir + File.separator;
        WORKING_DIRECTORY_FILE = new File(WORKING_DIRECTORY);
    }

    //region working directory methods


    //--------------------------------------------------------------------------
    //           Methods based on working directory -- user.dir
    //--------------------------------------------------------------------------

    /**
     * 获取工作目录的绝对路径
     * @return 工作目录的绝对路径
     */
    public static String getWorkingDirectoryPath(){
        return Resources.WORKING_DIRECTORY;
    }

    /**
     * 获取工作目录
     * @return 工作目录
     */
    public static File getWorkingDirectory(){
        return Resources.WORKING_DIRECTORY_FILE;
    }

    /**
     * 获取工作目录中制定文件或文件夹的绝对路径
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹的路径
     * @return 工作目录中文件或文件夹的绝对路径
     */
    public static String getWorkingDirectoryFilePath(String filePath){
        filePath = filePath.replaceAll("/", File.separator);
        filePath = filePath.startsWith(File.separator) ? filePath : File.separator + filePath;
        return WORKING_DIRECTORY + filePath;
    }

    /**
     * 判断工作目录中的某个文件或者文件夹是否存在
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹的路径
     * @return 文件或者文件夹是否存在
     */
    public static boolean workingDirectoryFileExists(String filePath){
        return getWorkingDirectoryFile(filePath).exists();
    }

    /**
     * 在工作目录中创建一个文件
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件路径
     * @return 是否创建成功
     * @throws IOException 文件创建失败时会抛出IO异常
     */
    public boolean createFileInWorkingDirectory(String filePath) throws IOException {
        File file = getWorkingDirectoryFile(filePath);
        if(!file.exists()){
            File parentFile = file.getParentFile();
            if(!parentFile.exists()){
                boolean mkdirs = parentFile.mkdirs();
                if (!mkdirs){
                    return false;
                }
            }
            return file.createNewFile();
        }
        return true;
    }

    /**
     * 在工作目录中创建一个文件夹
     * <br/>
     * <li>
     * <b>注意：folderPath参数中文件分隔符只能使用 "/" <b/></li>
     * @param folderPath 文件夹路径
     * @return 是否创建成功
     */
    public boolean createFolderInWorkingDirectory(String folderPath){
        File folder = getWorkingDirectoryFile(folderPath);
        if(folder.exists()){
            return true;
        }
        return folder.mkdirs();
    }

    /**
     * 获取工作目录中的某个文件或文件夹
     * <br/>
     * <li>
     * <b>注意：folderPath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @return 工作目录中的文件或文件夹
     */
    public static File getWorkingDirectoryFile(String filePath){
        return Paths.get(getWorkingDirectoryFilePath(filePath)).toFile();
    }


    /**
     * 获取工作目录中的某个文件或文件夹的{@link InputStream}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @return 工作目录中的文件或文件夹的InputStream
     */
    public static InputStream getWorkingDirectoryInputStream(String filePath){
        try {
            return new BufferedInputStream(new FileInputStream(getWorkingDirectoryFile(filePath)));
        }catch (FileNotFoundException e){
            throw new LuckyIOException(e);
        }

    }

    // region working directory Reader

    /**
     * 获取工作目录中的某个文件或文件夹的{@link Reader}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @param charsetName 字符集名称
     * @return 工作目录中的文件或文件夹的Reader
     * @throws UnsupportedEncodingException 当输入了错误的字符集名称时会触发该异常
     */
    public static Reader getWorkingDirectoryReader(String filePath, String charsetName) throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(getWorkingDirectoryInputStream(filePath),charsetName));
    }

    /**
     * 获取工作目录中的某个文件或文件夹的{@link Reader}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @param charset 字符集
     * @return  工作目录中的文件或文件夹的Reader
     */
    public static Reader getWorkingDirectoryReader(String filePath, Charset charset){
        return new BufferedReader(new InputStreamReader(getWorkingDirectoryInputStream(filePath), charset));
    }

    /**
     * 使用"UTF-8"编码方式获取工作目录中的某个文件或文件夹的{@link Reader}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @return 工作目录中的文件或文件夹的Reader
     */
    public static Reader getWorkingDirectoryReader(String filePath) {
        return getWorkingDirectoryReader(filePath, StandardCharsets.UTF_8);
    }

    // endregion

    /**
     * 获取工作目录中的某个文件或文件夹的{@link OutputStream}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @return 工作目录中的文件或文件夹的OutputStream
     */
    public static OutputStream getWorkingDirectoryOutputStream(String filePath) {
        try {
            return new BufferedOutputStream(new FileOutputStream(getWorkingDirectoryFile(filePath)));
        } catch (FileNotFoundException e) {
            throw new LuckyIOException(e);
        }
    }

    /**
     * 获取工作目录中的某个文件或文件夹的{@link Writer}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @param charsetName 字符集名称
     * @throws UnsupportedEncodingException 当输入了错误的字符集名称时会触发该异常
     * @return 工作目录中的文件或文件夹的Writer
     */
    public static Writer getWorkingDirectoryWriter(String filePath, String charsetName) throws UnsupportedEncodingException {
        return new BufferedWriter(new OutputStreamWriter(getWorkingDirectoryOutputStream(filePath),charsetName));
    }

    /**
     * 获取工作目录中的某个文件或文件夹的{@link Writer}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @param charset 字符集
     * @return 工作目录中的文件或文件夹的Writer
     */
    public static Writer getWorkingDirectoryWriter(String filePath, Charset charset) {
        return new BufferedWriter(new OutputStreamWriter(getWorkingDirectoryOutputStream(filePath),charset));
    }

    /**
     * 使用"UTF-8"的编码方式获取工作目录中的某个文件或文件夹的{@link Writer}
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @return 工作目录中的文件或文件夹的Writer
     */
    public static Writer getWorkingDirectoryWriter(String filePath){
        return getWorkingDirectoryWriter(filePath,StandardCharsets.UTF_8);
    }

    /**
     * 获取工作目录中某个文本文件的内容
     * <br/>
     * <li>
     * <b>注意：txtFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param txtFilePath 文件或文件夹路径
     * @param charset 字符集
     * @throws IOException 获取或解析文件出错时会抛出该异常
     * @return 文本文件的内容
     */
    public static String getWorkingDirectoryFileContent(String txtFilePath, Charset charset) throws IOException {
        return FileCopyUtils.copyToString(getWorkingDirectoryReader(txtFilePath, charset));
    }

    /**
     * 获取工作目录中某个文本文件的内容
     * <br/>
     * <li>
     * <b>注意：txtFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param txtFilePath 文件或文件夹路径
     * @param charsetName 字符集名称
     * @throws IOException 获取或解析文件出错或者给定的字符集错误时会抛出该异常
     * @return 文本文件的内容
     */
    public static String getWorkingDirectoryFileContent(String txtFilePath, String charsetName) throws IOException {
        return FileCopyUtils.copyToString(getWorkingDirectoryReader(txtFilePath, charsetName));
    }

    /**
     * 使用"UTF-8"的编码方式获取工作目录中某个文本文件的内容
     * <br/>
     * <li>
     * <b>注意：txtFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param txtFilePath 文件或文件夹路径
     * @throws IOException 获取或解析文件出错时会抛出该异常
     * @return 文本文件的内容
     */
    public static String getWorkingDirectoryFileContent(String txtFilePath) throws IOException {
        return FileCopyUtils.copyToString(getWorkingDirectoryReader(txtFilePath, StandardCharsets.UTF_8));
    }

    /**
     * 将工作目录中的某个文件转化为byte数组
     * <br/>
     * <li>
     * <b>注意：filePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param filePath 文件或文件夹路径
     * @throws IOException 获取或解析文件出错时会抛出该异常
     * @return byte数组
     */
    public static byte[] getWorkingDirectoryFileByte(String filePath) throws IOException {
        return FileCopyUtils.copyToByteArray(getWorkingDirectoryInputStream(filePath));
    }

    /* JSON FILE */

    /**
     * 将工作目录中的某个Json文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：jsonFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param jsonFilePath Json文件的路径
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromWorkingDirectoryJson(String jsonFilePath, Class<T> typeOf){
        return fromJsonReader(getWorkingDirectoryReader(jsonFilePath), typeOf);
    }

    /**
     * 将工作目录中的某个Json文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：jsonFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param jsonFilePath Json文件的路径
     * @param typeToken  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromWorkingDirectoryJson(String jsonFilePath, SerializationTypeToken<T> typeToken){
        return fromJsonReader(getWorkingDirectoryReader(jsonFilePath), typeToken);
    }

    /* YAML FILE */

    /**
     * 将工作目录中的某个Yaml文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：yamlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param yamlFilePath Yaml文件的路径
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromWorkingDirectoryYaml(String yamlFilePath, Class<T> typeOf){
        return fromYamlReader(getWorkingDirectoryReader(yamlFilePath), typeOf);
    }

    /* XML FILE  */

    /**
     * 将工作目录中的某个Xml文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：xmlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param xmlFilePath Xml文件的路径
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromWorkingDirectoryXmlReader(String xmlFilePath, Class<T> typeOf){
        return fromXmlReader(getWorkingDirectoryReader(xmlFilePath), typeOf);
    }

    /**
     * 将工作目录中的某个Xml文件转化为{@link Properties}对象
     * <br/>
     * <li>
     * <b>注意：xmlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param xmlFilenPath xml文件的路径
     * @return Properties对象
     */
    public static Properties workingDirectoryXmlToProperts(String xmlFilenPath){
        return xmlInputStreamToProperts(getWorkingDirectoryInputStream(xmlFilenPath));
    }

    /**
     * 将工作目录中的某个Xml文件转化为{@link ConfigurationMap}对象
     * <br/>
     * <li>
     * <b>注意：xmlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param xmlFilenPath xml文件的路径
     * @return ConfigurationMap对象
     */
    public static ConfigurationMap workingDirectoryXmlToConfigMap(String xmlFilenPath){
        return xmlInputStreamToConfigMap(getWorkingDirectoryInputStream(xmlFilenPath));
    }


    /* PROPERTIES FILE  */

    /**
     * 将工作目录中的某个Properties文件转化为{@link Properties}对象
     * <br/>
     * <li>
     * <b>注意：propertiesFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param propertiesFilePath Properties文件的路径
     * @return 转化后的Properties对象
     */
    public static Properties getWorkingDirectoryProperties(String propertiesFilePath){
        return getPropertiesReader(getWorkingDirectoryReader(propertiesFilePath));
    }

    /**
     * 将工作目录中的某个Properties文件转化为{@link ConfigurationMap}对象
     * <br/>
     * <li>
     * <b>注意：propertiesFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param propertiesFilePath Properties文件的路径
     * @return 转化后的ConfigurationMap对象
     */
    public static ConfigurationMap workingDirectoryPropertiesToConfigMap(String propertiesFilePath){
        return getConfigMapReader(getWorkingDirectoryReader(propertiesFilePath));
    }

    //endregion



    //--------------------------------------------------------------------------
    //                  Methods based on classpath
    //--------------------------------------------------------------------------


    /**
     * 判断classpath中是否存在某个文件或文件夹
     * @param classpath 文件或文件夹名称
     * @return classpath中是否存在某个文件或文件夹
     */
    public static boolean classPathFileExists(String classpath) {
        InputStream in = getClassPathInputStream(classpath);
        if(in == null){
            return false;
        }
        try {
            in.close();
        }catch (Exception ignore){}
        return true;
    }

    /**
     * 获取classpath下的一个{@link InputStream}
     * @param classpath 文件路径
     * @return InputStream
     */
    public static InputStream getClassPathInputStream(String classpath){
        return Resources.class.getResourceAsStream(classpath);
    }

    /**
     * 获取classpath下的一个{@link Reader}
     * @param classpath 文件路径
     * @param charset 字符集
     * @return Reader
     */
    public static Reader getClassPathReader(String classpath, Charset charset){
        return new BufferedReader(new InputStreamReader(getClassPathInputStream(classpath),charset));
    }

    /**
     * 获取classpath下的一个{@link Reader}
     * @param classpath 文件路径
     * @param charsetName 字符集名称
     * @throws UnsupportedEncodingException 字符集名称错误时会抛出该异常
     * @return Reader
     */
    public static Reader getClassPathReader(String classpath, String charsetName) throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(getClassPathInputStream(classpath),charsetName));
    }

    /**
     * 使用"UTF-8"的编码方式获取classpath下的一个{@link Reader}
     * @param classpath 文件路径
     * @param charset 字符集
     * @return Reader
     */
    public static Reader getClassPathReader(String classpath){
        return getClassPathReader(classpath,StandardCharsets.UTF_8);
    }

    /**
     * 获取classpath中某个文本文件的内容
     * <br/>
     * <li>
     * <b>注意：classpath参数中文件分隔符只能使用 "/" <b/></li>
     * @param classpath 文件或文件夹路径
     * @param charset 字符集
     * @throws IOException 获取或解析文件出错时会抛出该异常
     * @return 文本文件的内容
     */
    public static String getClassPathFileContent(String classpath, Charset charset) throws IOException {
        return FileCopyUtils.copyToString(getClassPathReader(classpath, charset));
    }

    /**
     * 获取classpath中某个文本文件的内容
     * <br/>
     * <li>
     * <b>注意：classpath参数中文件分隔符只能使用 "/" <b/></li>
     * @param classpath 文件或文件夹路径
     * @param charsetName 字符集名称
     * @throws IOException 获取或解析文件出错或者给定的字符集错误时会抛出该异常
     * @return 文本文件的内容
     */
    public static String getClassPathFileContent(String classpath, String charsetName) throws IOException {
        return FileCopyUtils.copyToString(getClassPathReader(classpath, charsetName));
    }

    /**
     * 使用"UTF-8"的编码方式获取classpath中某个文本文件的内容
     * <br/>
     * <li>
     * <b>注意：classpath参数中文件分隔符只能使用 "/" <b/></li>
     * @param classpath 文件或文件夹路径
     * @throws IOException 获取或解析文件出错时会抛出该异常
     * @return 文本文件的内容
     */
    public static String getClassPathFileContent(String classpath) throws IOException {
        return FileCopyUtils.copyToString(getClassPathReader(classpath, StandardCharsets.UTF_8));
    }

    /**
     * 将classpath中的某个文件转化为byte数组
     * <br/>
     * <li>
     * <b>注意：classpath参数中文件分隔符只能使用 "/" <b/></li>
     * @param classpath 文件或文件夹路径
     * @throws IOException 获取或解析文件出错时会抛出该异常
     * @return byte数组
     */
    public static byte[] getClassPathFileByte(String classpath) throws IOException {
        return FileCopyUtils.copyToByteArray(getClassPathInputStream(classpath));
    }

    /* classpath Json File */

    /**
     * 将classpath中的某个Json文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：jsonFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param jsonFilePath Json文件的路径
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromClassPathJson(String jsonFilePath, Class<T> typeOf){
        return fromJsonReader(getClassPathReader(jsonFilePath), typeOf);
    }

    /**
     * 将classpath的某个Json文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：jsonFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param jsonFilePath Json文件的路径
     * @param typeToken  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromClassPathJson(String jsonFilePath, SerializationTypeToken<T> typeToken){
        return fromJsonReader(getClassPathReader(jsonFilePath), typeToken);
    }

    /* classpath Yaml File */

    /**
     * 将classpath中的某个Yaml文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：yamlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param yamlFilePath Yaml文件的路径
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromClassPathYaml(String yamlFilePath, Class<T> typeOf){
        return fromYamlReader(getClassPathReader(yamlFilePath), typeOf);
    }

    /* classpath Xml File */

    /**
     * 将classpath中的某个Xml文件转化为Java对象
     * <br/>
     * <li>
     * <b>注意：xmlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param xmlFilePath Xml文件的路径
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromClassPathXmlReader(String xmlFilePath, Class<T> typeOf){
        return fromXmlReader(getClassPathReader(xmlFilePath), typeOf);
    }

    /**
     * 将classpath中的某个Xml文件转化为{@link Properties}对象
     * <br/>
     * <li>
     * <b>注意：xmlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param xmlFilenPath xml文件的路径
     * @return Properties对象
     */
    public static Properties classPathXmlToProperts(String xmlFilenPath){
        return xmlInputStreamToProperts(getClassPathInputStream(xmlFilenPath));
    }

    /**
     * 将classpath中的某个Xml文件转化为{@link ConfigurationMap}对象
     * <br/>
     * <li>
     * <b>注意：xmlFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param xmlFilenPath xml文件的路径
     * @return ConfigurationMap对象
     */
    public static ConfigurationMap classPathXmlToConfigMap(String xmlFilenPath){
        return xmlInputStreamToConfigMap(getClassPathInputStream(xmlFilenPath));
    }

    /* classpath Properties File */

    /**
     * 将classpath中的某个Properties文件转化为{@link Properties}对象
     * <br/>
     * <li>
     * <b>注意：propertiesFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param propertiesFilePath Properties文件的路径
     * @return 转化后的Properties对象
     */
    public static Properties getClassPathProperties(String propertiesFilePath){
        return getPropertiesReader(getClassPathReader(propertiesFilePath));
    }

    /**
     * 将classpath中的某个Properties文件转化为{@link ConfigurationMap}对象
     * <br/>
     * <li>
     * <b>注意：propertiesFilePath参数中文件分隔符只能使用 "/" <b/></li>
     * @param propertiesFilePath Properties文件的路径
     * @return 转化后的ConfigurationMap对象
     */
    public static ConfigurationMap classPathPropertiesToConfigMap(String propertiesFilePath){
        return getConfigMapReader(getClassPathReader(propertiesFilePath));
    }


    //region JSON File

    /**
     * 将某个Json文件转化为Java对象
     * @param jsonReader Json文件的Reader
     * @param typeOf  Java对象的Class
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromJsonReader(Reader jsonReader, Class<T> typeOf){
        try {
            return (T) SerializationSchemeFactory.getJsonScheme().deserialization(jsonReader, typeOf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将某个Json文件转化为Java对象
     * @param jsonReader Json文件的Reader
     * @param typeToken  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromJsonReader(Reader jsonReader, SerializationTypeToken<T> typeToken){
        try {
            return (T) SerializationSchemeFactory.getJsonScheme().deserialization(jsonReader, typeToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //endregion

    //region YAML File

    /**
     * 将工作目录中的某个Yaml文件转化为Java对象
     * @param yamlReader Yaml文件的Reader
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromYamlReader(Reader yamlReader, Class<T> typeOf){
        return new Yaml().loadAs(yamlReader, typeOf);
    }


    //endregion

    //region XML File

    /**
     * 将工作目录中的某个Xml文件转化为Java对象
     * @param xmlReader Xml文件的Reader
     * @param typeOf  Java对象的类型token
     * @return 转化后的Java对象
     * @param <T> 泛型类型
     */
    public static <T> T fromXmlReader(Reader xmlReader, Class<T> typeOf){
        try {
            return (T) SerializationSchemeFactory.getXmlScheme().deserialization(xmlReader,typeOf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将工作目录中的某个Xml文件转化为{@link Properties}对象
     * @param xmlInputStream xml文件的InputStream
     * @return Properties对象
     */
    public static Properties xmlInputStreamToProperts(InputStream xmlInputStream){
        try {
            Properties properties = new Properties();
            properties.loadFromXML(xmlInputStream);
            return properties;
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 将工作目录中的某个Xml文件转化为{@link ConfigurationMap}对象
     * @param xmlInputStream xml文件的InputStream
     * @return ConfigurationMap对象
     */
    public static ConfigurationMap xmlInputStreamToConfigMap(InputStream xmlInputStream){
        return ConfigurationMap.create(xmlInputStreamToProperts(xmlInputStream));
    }

    //endregion

    //region PROPERTIES File

    /**
     * 将工作目录中的某个Properties文件转化为{@link Properties}对象
     * @param propertiesReader Properties文件的Reader
     * @return 转化后的Properties对象
     */
    public static Properties getPropertiesReader(Reader propertiesReader){
        try {
            Properties props = new Properties();
            props.load(propertiesReader);
            return props;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将工作目录中的某个Properties文件转化为{@link ConfigurationMap}对象
     * @param propertiesReader Properties文件的Reader
     * @return 转化后的ConfigurationMap对象
     */
    public static ConfigurationMap getConfigMapReader(Reader propertiesReader){
        return ConfigurationMap.create(getPropertiesReader(propertiesReader));
    }

    //endregion


}
