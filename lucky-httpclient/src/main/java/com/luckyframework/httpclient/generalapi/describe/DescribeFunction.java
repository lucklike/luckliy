package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.logging.FontUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/14 23:27
 */
public class DescribeFunction {

    private static final Map<String, Object> $api = new HashMap<String, Object>() {{

        // 接口描述信息
        put("id", "#{#describe($mc$).id}");
        put("name", "#{#describe($mc$).name}");
        put("version", "#{#describe($mc$).version}");
        put("author", "#{#describe($mc$).author}");
        put("updateTime", "#{#describe($mc$).updateTime}");
        put("contactWay", "#{#describe($mc$).contactWay}");

        // 错误信息
        Map<String, Object> errorMsgMap = new HashMap<>();
        put("error", errorMsgMap);

        // 请求方法
        String method = "#{$reqMethod$}";
        // URL
        String url = "#{$url$}";

        // 接口名称
        String apiName = "#{$api.name == '' ? $method$.getName() : $api.name}";
        // 开发者信息
        String dev = "#{$api.author == '' ? '！' : ($api.contactWay == '' ? '，请联系接口维护人员：' + $api.author : '，请联系接口维护人员：' + $api.author + '/' + $api.contactWay )}";
        // HTTP状态码
        String status = "status = #{$status$}";
        // HTTP状态码对应的错误描述信息
        String statusErrMsg = "#{$statusErrMsg == null ? '' : ', msg = ' + $statusErrMsg}";

        // $api.error.statusErr -> 【XXX】<status = 404，msg = xxx> 接口响应码异常，请联系接口维护人员：付康/17363312985 [GET] -> http://www.baidu.com
        String statusErr = StringUtils.format("{}接口响应码异常{} [{}] {}",
                StringUtils.format("{}{} ", FontUtil.getWhiteStr("【" + apiName + "】"), FontUtil.getRedStr("<" + status + statusErrMsg + "> ")),
                dev,
                method,
                url
        );
        errorMsgMap.put("statusErr", statusErr);


    }};

    /**
     * 获取接口描述信息实体类
     *
     * @param context 方法上下文
     * @return 接口描述信息实体类
     */
    public static DescribeEntity describe(MethodContext context) {
        return DescribeEntity.of(context.getSameAnnotationCombined(Describe.class));
    }

    /**
     * 匹配接口ID，如果匹配返回true，否则返回false
     *
     * @param context 方法上下文
     * @param apiId   目标ID
     * @return 是否匹配
     */
    public static boolean matchId(MethodContext context, String apiId) {
        Describe describeAnn = context.getMergedAnnotation(Describe.class);
        if (describeAnn == null || !StringUtils.hasText(describeAnn.id())) {
            return false;
        }
        return Objects.equals(describeAnn.id(), apiId);
    }

    /**
     * {@link #matchId(MethodContext, String)}方法的简写方法
     * <pre>
     *     eg:
     *     ``#{#$matchId('FUN-TOKEN')}``
     * </pre>
     *
     * @param apiId 目标ID
     * @return 调用matchId方法的表达式
     */
    public static String $matchId(String apiId) {
        return StringUtils.format("#{#matchId($mc$, '{}')}", apiId);
    }


    public static class DescribeEntity {

        public static final DescribeEntity EMPTY = new DescribeEntity("", "", "", "", "", "");

        /**
         * 接口唯一ID
         */
        private final String id;

        /**
         * 接口名称
         */
        private final String name;

        /**
         * 接口版本号
         */
        private final String version;

        /**
         * 接口作者
         */
        private final String author;

        /**
         * 修改时间
         */
        private final String updateTime;

        /**
         * 维护人员联系方式
         */
        private final String contactWay;

        private DescribeEntity(String id, String name, String version, String author, String updateTime, String contactWay) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.author = author;
            this.updateTime = updateTime;
            this.contactWay = contactWay;
        }

        public static DescribeEntity of(Describe describe) {
            if (describe == null) {
                return EMPTY;
            }
            return new DescribeEntity(describe.id(), describe.name(), describe.version(), describe.author(), describe.updateTime(), describe.contactWay());
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getAuthor() {
            return author;
        }

        public String getUpdateTime() {
            return updateTime;
        }

        public String getContactWay() {
            return contactWay;
        }
    }
}
