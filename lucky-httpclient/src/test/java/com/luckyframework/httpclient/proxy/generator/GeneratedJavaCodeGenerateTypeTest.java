package com.luckyframework.httpclient.proxy.generator;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.async.Model;
import com.luckyframework.httpclient.proxy.generator.support.GeneratedJavaCodeTestApi;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.httpclient.proxy.typeparser.support.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link GeneratedResponseJavaBean#generateTypes()} 返回值类型白名单测试
 * <pre>
 *     验证点：
 *       A.返回值类型命中白名单时正常生成实体类
 *       B.返回值类型未命中白名单时不生成实体类
 *       C.未配置白名单时不做类型限制，正常生成实体类
 * </pre>
 * 测试手段：注入同步执行的AsyncTaskExecutor，让RESPONSE阶段的生成回调在代理方法返回前完成，
 * 从而可以确定性地断言目标文件是否存在，无需轮询等待异步任务
 */
public class GeneratedJavaCodeGenerateTypeTest {

    private static final String API_PATH = "/generator/generate-type";
    private static final String OUT_DIR = "target/generated-generate-type-test";
    private static final String PACKAGE_PATH = "generated/generatetype/";
    private static final String JSON_BODY = "{\"id\":1,\"name\":\"alice\"}";

    private TestHttpServer server;
    private GeneratedJavaCodeTestApi api;

    @Before
    public void setUp() throws Exception {
        deleteRecursively(new File(OUT_DIR));

        server = new TestHttpServer();
        server.registerJson(API_PATH);
        server.queueResponse(API_PATH, JSON_BODY);

        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        // 生成回调默认异步执行，这里换成同步执行器，保证代理方法返回时回调已执行完毕
        factory.setAsyncExecutor(command -> command.run());
        factory.setAsyncModel(Model.JAVA_THREAD);
        api = factory.getProxyObject(GeneratedJavaCodeTestApi.class);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.close();
        }
        deleteRecursively(new File(OUT_DIR));
    }

    /**
     * WHEN 方法返回值类型命中generateTypes配置；THEN 正常生成实体类文件
     */
    @Test
    public void shouldGenerateWhenResultTypeMatched() {
        Map<String, Object> result = api.mapResult(TestUtils.request(server, API_PATH));

        assertNotNull(result);
        assertTrue("返回值类型命中白名单时应该生成实体类", generatedFile("MapResultResponse").isFile());
    }

    /**
     * WHEN 方法返回值类型未命中generateTypes配置；THEN 不生成实体类文件
     */
    @Test
    public void shouldNotGenerateWhenResultTypeNotMatched() {
        String result = api.stringResult(TestUtils.request(server, API_PATH));

        assertNotNull(result);
        assertFalse("返回值类型未命中白名单时不应生成实体类", generatedFile("StringResultResponse").exists());
    }

    /**
     * WHEN 未配置generateTypes；THEN 不做类型限制，正常生成实体类文件
     */
    @Test
    public void shouldGenerateWhenGenerateTypesAbsent() {
        Map<String, Object> result = api.mapResultWithoutTypes(TestUtils.request(server, API_PATH));

        assertNotNull(result);
        assertTrue("未配置返回值类型白名单时不应限制类型，应该生成实体类", generatedFile("MapResultWithoutTypesResponse").isFile());
    }

    private File generatedFile(String className) {
        return new File(OUT_DIR, PACKAGE_PATH + className + ".java");
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }
}
