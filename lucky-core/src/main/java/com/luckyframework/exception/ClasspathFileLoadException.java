package com.luckyframework.exception;

/**
 * ClassPath文件加载异常
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/24 下午9:14
 */
public class ClasspathFileLoadException extends RuntimeException{

    public ClasspathFileLoadException(String filePath, Throwable e){
        super(String.format("加载\"classpath:%s\"时出现错误,文件不存在或者没有访问的权限！",filePath),e);
    }
}