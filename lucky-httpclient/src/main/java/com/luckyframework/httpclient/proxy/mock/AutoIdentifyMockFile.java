package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.function.ResourceFunctions;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.spel.SimpleSpelBean;
import org.springframework.core.io.Resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

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
 *      @UseAutoUrlDerivationInsurance
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
 *      #login方法的Mock数据
 *      login:
 *        #方法级别开关
 *        enable: false
 *        #状态码
 *        status: 200
 *        #响应头
 *        headers:
 *          Content-Type: application/json
 *        #响应体
 *        body:
 *          #文本格式响应体
 *          txt: |
 *            {
 *              "access_token": "e6c0991176784141583030b2af550655812729af8cd92598b5b99a9c0f89",
 *              "expire_time": 36000,
 *              "expires_in": "2026-01-08 21:06:04"
 *            }
 *          #文件类型的响应体
 *          file: classpath:test/mocak.pdf
 *
 *      #logout方法的Mock数据
 *      logout:
 *        headers:
 *          Content-Type: application/json
 *        body:
 *          txt: |
 *            {
 *                "error": {
 *                    "error_no": "0",
 *                    "error_info": "",
 *                    "error_pathinfo": null
 *                },
 *                "data": {
 *                    "staff_no": "1163",
 *                    "user_id": "1163",
 *                    "user_name": "付康",
 *                    "info": "【#{$0}】登出成功"
 *                }
 *            }
 *  }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Mock(enable = "#{enable_mock($mc$)}", mockResp = "#{mock_result($mc$)}")
@SpELImport(AutoIdentifyMockFile.MockFunction.class)
public @interface AutoIdentifyMockFile {

    /**
     * 总开关， 支持SpEL表达式
     */
    String mainSwitch() default "true";


    /**
     * Mock文件资源路径表达式，支持SpEL表达式，默认为
     */
    String mockFile() default "classpath:mock-response/Mock_#{$class$.getSimpleName()}.yml";


    /**
     * 文件中的Key
     */
    String mockKey() default "#{$method$.getName()}";


    class MockFunction {

        @FunctionAlias("enable_mock")
        public static boolean enableMock(MethodContext mc) {

            // 总开关
            AutoIdentifyMockFile ann = mc.getMergedAnnotationCheckParent(AutoIdentifyMockFile.class);
            boolean mainSwitch = mc.parseExpression(ann.mainSwitch(), boolean.class);
            if (!mainSwitch) {
                return false;
            }

            // mock文件是否存在
            String mockFilePath = mc.parseExpression(ann.mockFile(), String.class);
            Resource mockResource = ResourceFunctions.resource(mockFilePath);
            if (!mockResource.exists() || !mockResource.isFile()) {
                return false;
            }

            // 不是Map结构
            SimpleSpelBean<?> mockBean = Resources.resourceAsSpelBean(mockFilePath);
            if (!(mockBean.getBean() instanceof Map)) {
                return false;
            }

            // 存在Mock配置
            String mockKey = mc.parseExpression(ann.mockKey(), String.class);
            Object mockValue = mockBean.get(String.format("['%s']", mockKey));
            if (!(mockValue instanceof Map)) {
                return false;
            }

            // 方法级别开关
            Object enable = ((Map<?, ?>) mockValue).get("enable");
            return enable == null || mc.parseExpression(String.valueOf(enable), boolean.class);
        }

        @FunctionAlias("mock_result")
        public static Response mockResult(MethodContext mc) {
            AutoIdentifyMockFile ann = mc.getMergedAnnotationCheckParent(AutoIdentifyMockFile.class);

            String mockFilePath = mc.parseExpression(ann.mockFile(), String.class);
            String mockKeyConfig = mc.parseExpression(ann.mockKey(), String.class);
            String mockKey = String.format("['%s'].", mockKeyConfig);
            SimpleSpelBean<?> mockBean = Resources.resourceAsSpelBean(mockFilePath);

            MockResponse mockResponse = MockResponse.create();
            mockResponse.header("Mock-Annotation", "@AutoIdentifyMockFile");
            mockResponse.header("Mock-File", mockFilePath);
            mockResponse.header("Mock-File-Key", mockKeyConfig);

            // status
            String statusStr = mockBean.getString(mockKey + "status");
            if (StringUtils.hasText(statusStr)) {
                mockResponse.status(mc.parseExpression(statusStr, Integer.class));
            } else {
                mockResponse.status(200);
            }

            // header
            Map<String, Object> headers = mockBean.get(mockKey + "headers", new SerializationTypeToken<Map<String, Object>>() {
            });
            if (ContainerUtils.isNotEmptyMap(headers)) {
                headers.forEach(mockResponse::header);
            }

            // body

            // TXT
            String txtBody = mockBean.get(mockKey + "body?.txt", String.class);
            if (StringUtils.hasText(txtBody)) {
                mockResponse.body(mc.parseExpression(txtBody, String.class));
            }

            // FILE
            String fileBody = mockBean.get(mockKey + "body?.file", String.class);
            if (StringUtils.hasText(fileBody)) {
                mockResponse.body(ResourceFunctions.resourceAsStream(mc.parseExpression(fileBody, String.class)));
            }

            return mockResponse;
        }
    }

}
