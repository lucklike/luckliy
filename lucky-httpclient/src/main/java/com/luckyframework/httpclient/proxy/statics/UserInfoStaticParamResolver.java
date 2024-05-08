package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.annotations.StaticUserInfo;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * UserInfo配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 12:34
 */
public class UserInfoStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        StaticUserInfo userInfoAnn = context.toAnnotation(StaticUserInfo.class);
        // 获取UserInfo
        String userInfo = context.parseExpression(userInfoAnn.value());
        return Collections.singletonList(new ParamInfo("userInfo", userInfo));
    }
}
