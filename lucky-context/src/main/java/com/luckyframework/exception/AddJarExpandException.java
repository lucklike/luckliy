package com.luckyframework.exception;

/**
 * @author fk
 * @version 1.0
 * @date 2020/12/30 0030 15:55
 */
public class AddJarExpandException extends RuntimeException{

    public AddJarExpandException(String url){
        super("添加Jar扩展失败！错误的jarPath :"+url);
    }
}
