package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.logging.FontUtil;
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
        String url = "#{$url$}";

        // 接口名称
        String apiName = "#{#nonText($api.name) ? $method$.getName() : $api.name}";
        // 开发者信息
        String dev = "#{#nonText($api.author) ? '！' : (#nonText($api.contactWay) ? '，请联系接口维护人员：' + $api.author + '。' : '，请联系接口维护人员：' + $api.author + '/' + $api.contactWay + '。')}";
        // HTTP状态码
        String status = "status = #{$status$}";
        // HTTP状态码对应的错误描述信息
        String statusErrMsg = "#{#nonText($statusErrMsg) ? '' : ', msg = ' + $statusErrMsg}";

        // $err.statusErr -> 【XXX】<status = 404，msg = xxx> 接口响应码异常，请联系接口维护人员：付康/17363312985。 [GET] -> http://www.baidu.com
        String statusErr = StringUtils.format("{}接口响应码异常{} [{}] {}",
                StringUtils.format("{}{} ", FontUtil.getWhiteStr("【" + apiName + "】"), FontUtil.getRedStr("<" + status + statusErrMsg + ">")),
                dev,
                method,
                url
        );
        put("statusErr", statusErr);
    }};
}
