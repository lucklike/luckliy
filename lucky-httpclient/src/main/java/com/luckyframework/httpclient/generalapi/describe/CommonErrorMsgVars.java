package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.spel.var.ClassRootLiteral;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用错误信息变量，需要配合{@link DescribeFunction}一起使用
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/16 07:02
 */
public class CommonErrorMsgVars {

    /**
     * 异常提示信息
     */
    @ClassRootLiteral
    private static final Map<String, Object> $err = new HashMap<String, Object>() {{
        // 请求方法
        String method = "#{$reqMethod$}";
        // URL
        String path = "#{$urlPath$}";
        // 接口名称
        String name = "#{#hasText($api.name) ? $api.name : $method$.getName()}";
        // 开发者信息
        String dev = "#{#nonText($api.author) ? '' : ' ### developer: ' + (#hasText($api.contactWay) ? #str('{}/{}', $api.author, $api.contactWay) : $api.author) + ' ###'}";

        // HTTP状态码
        String status = "#{$status$}";
        // HTTP异常提示信息
        String statusErrMsg = "#{#hasText(_statusErrMsg_) ? ', ' + _statusErrMsg_: ''}";

        String errCode = "#{__code__}";
        String errMsg = "#{#hasText(_msg_) ? ', ' + _msg_ : ''}";

        // HTTP状态码异常时的提示信息 -> Http Status Error! [GET]<用户注册>(/user/error): ['4001', error test] ##Developer: fukang/17363312985##
        String httpStatusErrTemp = "Http Status Error! [{}]<{}>({}): {}{}{}";

        // 接口响应码异常时的提示信息 -> Response Code Error! [GET]<用户注册>(/user/error): ['4001', error test] ##Developer: fukang/17363312985##
        String codeErrTemp = "Response Code Error! [{}]<{}>({}): {}{}{}";


        /*
         * 可配置项：
         * _statusErrMsg_ HTTP状态码异常时，用于获取异常提示信息的表达式
         */
        put("status", StringUtils.format(httpStatusErrTemp, method, name, path, status, statusErrMsg, dev));

        /*
         * 可选配置项：
         * _msg_                获取异常提示信息的表达式
         *
         * 必要配置：
         * __code__             获取Code码的表达式
         */
        put("code", StringUtils.format(codeErrTemp, method, name, path, errCode, errMsg, dev));
    }};

    /**
     * 断言信息
     */
    @ClassRootLiteral
    private static final Map<String, Object> $assert = new HashMap<String, Object>() {{
        /*
         * 可配置选项：
         * _statusExp_       状态表达式
         * _normalStatus_    正常状态码，配数字或者集合
         */
        put("status", "#{#hasText(_statusExp_) ? _statusExp_ : (#nonEmpty(_normalStatus_) ? '#{#nonIn(_normalStatus_, $status$)}' : '#{$status$ != 200}')}");

        /*
         * 必要配置：
         * __respCodeAssertExp__        判断Code码是否正常的表达式
         */
        put("code", "#{__respCodeAssertExp__}");
    }};
}
