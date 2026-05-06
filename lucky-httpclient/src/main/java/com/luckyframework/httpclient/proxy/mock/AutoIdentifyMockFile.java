package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.function.ResourceFunctions;
import com.luckyframework.httpclient.proxy.mock.config.MockConfigFunction;
import com.luckyframework.httpclient.proxy.mock.config.MockConfiguration;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;
import com.luckyframework.spel.SimpleSpelBean;
import org.springframework.core.io.Resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动识别指定目录下的特定文件来进行Mock，支持json/yml/properties格式文件
 *
 * <pre>
 *  eg:
 *  Class Api:
 * </pre>
 * <pre>
 *  {@code
 *      @HttpClient("http://localhost:8080")
 *      @AutoIdentifyFileMock
 *      public interface AutoIdentifyFileMockApi {
 *
 *          @Get("login")
 *          SpelBean<?> login();
 *
 *          @Post("logout")
 *          String logout(@JsonParam String token);
 *      }
 *  }
 * </pre>
 *
 * <pre>
 *   Mock_AutoIdentifyFileMockApi.yml:
 * </pre>
 *
 * <pre>
 *  {@code
 *  #总开关
 * enable: true
 * #方法级别的延时模拟，（单位：毫秒）
 * latency: 1000
 *
 * #各个方法的Mock配置
 * methods:
 *     #login方法的Mock数据
 *     login:
 *         #方法级别开关
 *         enable: false
 *         #条件匹配
 *         match:
 *           #条件1+结果1
 *           - when: "#{1==1}"
 *             latency: 1200
 *             status: 200
 *             headers:
 *               Server: nginx/1.18.0
 *               Date: Mon, 16 Mar 2026 06:46:14 GMT
 *               Content-Length: 157
 *             body:
 *               txt: qwqw
 *               file: classpath:deded/lll.txt
 *           # 条件2+结果2
 *           - when: "#{c == 3}"
 *             latency: 1300
 *             status: 404
 *             headers:
 *               Server: nginx/1.18.0
 *               Date: Mon, 16 Mar 2026 06:46:14 GMT
 *               Content-Length: 157
 *             body:
 *               txt: 404 Not Found
 *               file: classpath:deded/lll.txt
 *
 *         #延时模拟，（单位：毫秒）
 *         latency: 1000
 *         #状态码
 *         status: 200
 *         #响应头，Key和Value均支持SpEL表达式
 *         headers:
 *           Content-Type: application/json
 *           X-Random-Emial: #{random_email()};
 *         #响应体
 *         body:
 *           #文本格式响应体，支持SpEL表达式
 *           txt: |
 *             {
 *               "access_token": "e6c0991176784141583030b2af550655812729af8cd92598b5b99a9c0f89",
 *               "expire_time": 36000,
 *               "expires_in": "2026-01-08 21:06:04",
 *               "random_tel": "#{random_tel()}"
 *             }
 *           #文件类型的响应体
 *           file: classpath:test/mocak.pdf
 *
 *     #logout方法的Mock数据
 *     logout:
 *         headers:
 *           Content-Type: application/json
 *         body:
 *           txt: |
 *             {
 *                 "error": {
 *                     "error_no": "0",
 *                     "error_info": "",
 *                     "error_pathinfo": null
 *                 },
 *                 "data": {
 *                     "staff_no": "1163",
 *                     "user_id": "1163",
 *                     "user_name": "fukang7075",
 *                     "info": "【#{$0}】登出成功"
 *                 }
 *             }
 *  }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Mock(enable = "#{__enable_mock__($mc$)}", mockResp = "#{__mock_result__($mc$)}")
@SpELImport(AutoIdentifyMockFile.MockFunction.class)
public @interface AutoIdentifyMockFile {

    /**
     * Mock文件所在目录，支持SpEL表达式
     */
    String mockDir() default "classpath:mock-response";

    /**
     * Mock文件资源路径表达式，支持SpEL表达式
     */
    String mockFile() default "Mock_#{$class$.getSimpleName()}.yml";


    /**
     * Mock相关的函数
     */
    class MockFunction {

        public static final String MOCK_CONFIG = "$AutoIdentifyMockFile";

        /**
         * 初始化Mock配置，检查Mock配置是否存在，存在则加载
         *
         * @param cc 类上下文对象
         * @return Mock配置
         */
        @Callback(lifecycle = Lifecycle.CLASS, storeOrNot = true, storeName = MOCK_CONFIG)
        public static MockConfiguration initMockConfiguration(ClassContext cc) {
            AutoIdentifyMockFile ann = cc.getMergedAnnotationCheckParent(AutoIdentifyMockFile.class);

            // mock文件是否存在
            String mockFilePath = StringUtils.joinUrlPath(cc.parseExpression(ann.mockDir(), String.class), cc.parseExpression(ann.mockFile(), String.class));
            Resource mockResource = ResourceFunctions.resource(mockFilePath);
            if (!mockResource.exists() || !mockResource.isFile()) {
                return null;
            }

            SimpleSpelBean<?> mockBean = Resources.resourceAsSpelBean(mockFilePath);
            try {
                return mockBean.beanBind(MockConfiguration.class);
            } catch (Exception e) {
                return null;
            }

        }

        @FunctionAlias("__enable_mock__")
        public static boolean enableMock(MethodContext mc) {
            MockConfiguration mockConfig = mc.getRootVar(MOCK_CONFIG, MockConfiguration.class);
            return MockConfigFunction.mockEnable(mc, mockConfig);
        }

        @FunctionAlias("__mock_result__")
        public static Response mockResult(MethodContext mc) throws Exception {
            // 将Mock配置转化为MockResponse对象
            MockConfiguration mockConfig = mc.getRootVar(MOCK_CONFIG, MockConfiguration.class);
            MockResponse mockResponse = MockConfigFunction.mockResult(mc, mockConfig);

            // 设置特殊Mock响应头
            AutoIdentifyMockFile ann = mc.getMergedAnnotationCheckParent(AutoIdentifyMockFile.class);

            String mockFilePath = StringUtils.joinUrlPath(mc.parseExpression(ann.mockDir(), String.class), mc.parseExpression(ann.mockFile(), String.class));
            mockResponse.header("Mock-Annotation", "@AutoIdentifyMockFile");
            mockResponse.header("Mock-File", mockFilePath);
            mockResponse.header("Mock-File-Config", MockConfigFunction.getApiName(mc));

            //return
            return mockResponse;
        }

    }
}
