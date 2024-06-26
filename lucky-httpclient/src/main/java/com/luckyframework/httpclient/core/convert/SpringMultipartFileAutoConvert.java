package com.luckyframework.httpclient.core.convert;

import com.luckyframework.httpclient.core.meta.Response;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Type;

public class SpringMultipartFileAutoConvert implements Response.AutoConvert {
    
    @Override
    public boolean can(Response resp, Type type) {
        return type == MultipartFile.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Response resp, Type type) {
        return (T) new SpringMultipartFile(resp.getMultipartFile());
    }
}
