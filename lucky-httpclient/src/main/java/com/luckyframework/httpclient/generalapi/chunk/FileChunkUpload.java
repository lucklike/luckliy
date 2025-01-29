package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.httpclient.proxy.plugin.Plugin;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件分片上传注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/28 13:07
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Plugin(pluginClass = FileChunkUpload.UploadPlugin.class)
public @interface FileChunkUpload {

    String check() default "";



    /**
     * 负责文件上传的插件
     */
    class UploadPlugin implements ProxyPlugin {
        @Override
        public Object decorate(ProxyDecorator decorator) throws Throwable {
            return null;
        }
    }
}
