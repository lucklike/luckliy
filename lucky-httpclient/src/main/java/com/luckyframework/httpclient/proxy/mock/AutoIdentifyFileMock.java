package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.function.CommonFunctions;
import com.luckyframework.httpclient.proxy.function.ResourceFunctions;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.spel.SimpleSpelBean;
import org.springframework.core.io.Resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * 自动识别指定目录下的特定文件来进行Mock
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Mock(enable = "#{enable_mock($mc$)}", mockFunc = "#{mock_result($mc$)}")
@SpELImport(AutoIdentifyFileMock.MockFunction.class)
public @interface AutoIdentifyFileMock {

    // 总开关
    String mainSwitch() default "true";

    // 通用mock文件资源路径表达式
    String mockFile() default "classpath:mock/#{$class$.getSimpleName()}/#{$method$.getName()}.json";


    class MockFunction {

        @FunctionAlias("enable_mock")
        public static boolean enableMock(MethodContext mc) {
            AutoIdentifyFileMock ann = mc.getMergedAnnotationCheckParent(AutoIdentifyFileMock.class);
            boolean mainSwitch = mc.parseExpression(ann.mainSwitch(), boolean.class);
            if (!mainSwitch) {
                return false;
            }

            String mockFilePath = mc.parseExpression(ann.mockFile(), String.class);
            Resource mockResource = ResourceFunctions.resource(mockFilePath);
            return mockResource.exists();
        }

        @FunctionAlias("mock_result")
        public static Response mockResult(MethodContext mc) {
            AutoIdentifyFileMock ann = mc.getMergedAnnotationCheckParent(AutoIdentifyFileMock.class);

            String mockFilePath = mc.parseExpression(ann.mockFile(), String.class);
            SimpleSpelBean<?> spelBean = Resources.resourceAsSpelBean(mockFilePath);

            MockResponse mockResponse = MockResponse.create();

            // status
            mockResponse.status(spelBean.getInt("status"));

            // header
            List<String> headers = spelBean.getStringList("headers");
            if (ContainerUtils.isNotEmptyCollection(headers)) {
                for (String header : headers) {

                }
            }

            return mockResponse;
        }
    }

}
