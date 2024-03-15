package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.BasicAuth;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Basic Auth配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 12:34
 */
public class BasicAuthStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        BasicAuth basicAuth = context.toAnnotation(BasicAuth.class);
        // 获取注解信息
        String username = context.parseExpression(basicAuth.username());
        String password = context.parseExpression(basicAuth.password());
        String header = context.parseExpression(basicAuth.header());
        Charset charset = Charset.forName(context.parseExpression(basicAuth.charset()));

        return Collections.singletonList(new ParamInfo(header, base64(username, password, charset)));
    }


    // base64编码
    private String base64(String username, String password, Charset charset) {
        String auth = username + ":" + password;
        byte[] encodeAuth = Base64.getEncoder().encode(auth.getBytes());
        return  "Basic " + new String(encodeAuth, charset);
    }
}
