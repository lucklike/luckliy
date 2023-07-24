package com.luckyframework.httpclient.proxy;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.StringResultConvert;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.proxy.annotations.FileParam;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.HttpConfiguration;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.QueryParam;
import com.luckyframework.serializable.JsonSerializationScheme;
import com.luckyframework.serializable.SerializationSchemeFactory;
import com.luckyframework.serializable.SerializationTypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/22 15:17
 */
public class HttpProxyApiTestDemo {

    public static void main(String[] args) {
        HttpRequestProxyObjectFactory proxyObjectFactory = new HttpRequestProxyObjectFactory(new JdkHttpExecutor());
        HttpTest httpTest = proxyObjectFactory.getCglibProxyObject(HttpTest.class);
//        List<Map<String, Object>> weather = httpTest.weather("恩施");
//        System.out.println(weather);
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("city", "武汉");
        queryMap.put("y", "23");
        System.out.println(httpTest.voidWeather2(queryMap));
//
//        LuckyHttpApi luckyApi = proxyObjectFactory.getCglibProxyObject(LuckyHttpApi.class);
//        System.out.println(luckyApi.getUser());
//        System.out.println(luckyApi.getUsers());
//        Map<String, Object> userMap = new HashMap<>(4);
//        userMap.put("id", 12324);
//        userMap.put("userName", "罗雁珠");
//        userMap.put("password", "768990");
//        userMap.put("price", 999999.999);
//        luckyApi.putUser(userMap);
//        FilePojo fp = new FilePojo("FilePojo", "https://ts1.cn.mm.bing.net/th/id/R-C.2e332fe1fca6eb694147ef3a6a930fff?rik=Db66UJWIliu3NA&riu=http%3a%2f%2fimg.redocn.com%2f200903%2f2%2f556518_1235964346JSzk.jpg&ehk=U%2bl5n5ntAhgny2J3dwOvm%2b5w5KoM4brhwzxXIfN8lBk%3d&risl=&pid=ImgRaw&r=0");
//        luckyApi.fileUpload(fp);
////        luckyApi.fileUpload("lucky-http-client", "https://ts1.cn.mm.bing.net/th/id/R-C.823270fc68b9c58f0d9b3feb92b7b172?rik=aubbEBMSC86e%2bw&riu=http%3a%2f%2fimg95.699pic.com%2fphoto%2f50038%2f1181.jpg_wh860.jpg&ehk=iQboj4JMLLfDitOL7VJtSktED0AE%2f7Fyxfik0GTJkyQ%3d&risl=&pid=ImgRaw&r=0");
//        httpTest.voidWeather("恩施", (i, hm, inf) -> {
//            System.out.println(i);
//            InputStream inputStream = null;
//            try {
//                inputStream = inf.getInputStream();
//                StringWriter sw = new StringWriter();
//                FileCopyUtils.copy(new BufferedReader(new InputStreamReader(inputStream, hm.getContentType().getCharset())), sw);
//                System.out.println(sw);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//        });

    }
}

@HttpConfiguration(
    path = "#{T(com.luckyframework.httpclient.proxy.HttpTest).getUrl()}",
    requestProcessor = HttpTest.MyRequestProcessor.class,
    stringResultConvert = HttpTest.StringConvert1.class
)
interface HttpTest {

    @Get("https://cn.bing.com/search")
    String baidu(String q);


    @Get("/{version}/weather/weatherInfo")
    List<Map<String, Object>> weather(String city);

    @Get("/{version}/weather/weatherInfo")
    void voidWeather(String city, ResponseProcessor processor);

    @Get("/{version}/weather/weatherInfo")
    List<Map<String, Object>> voidWeather2(Map<String, Object> queryParam);

    static String getUrl(){
       return "https://restapi.amap.com";
    }

    class MyRequestProcessor extends SpELRequestProcessor {

        @Override
        public void process(Request request) {
            super.process(request);
            request.addQueryParameter("key", "833152601d928116dd12555fe949c214");
            request.addPathParameter("version", "v3");
            request.addPathParameter("version2", "v32");
            request.addHeader("X-F", "fu");
            request.addHeader("X-K", "kang");
            System.out.println(request);
        }
    }

    class StringConvert1 implements StringResultConvert {

        @Override
        public <T> T convert(String stringResult, Type resultType) throws Exception {
            JsonSerializationScheme jsonScheme = SerializationSchemeFactory.getJsonScheme();
            Map<String, Object> map
                    = jsonScheme.deserialization(stringResult, new SerializationTypeToken<Map<String, Object>>() {
            });
            return (T) ConversionUtils.conversion(map.get("lives"), resultType);
        }
    }

}

@HttpConfiguration("http://localhost:8080/")
interface LuckyHttpApi {

    @Post("/user")
    Map<String, Object> getUser();

    @Post("/users")
    String getUsers();

    @Post("/fileUpload")
    void fileUpload(String msg, @FileParam("file") String filePath);

    @Post("/fileUpload")
    void fileUpload(FilePojo filePojo);

    @Post("putUser")
    void putUser(@JsonBody Map<String, Object> user);

}

class FilePojo {
    @QueryParam("msg")
    private final String message;

    @FileParam("file")
    private final String resourcePath;

    public String getMessage() {
        return message;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public FilePojo(String message, String resourcePath) {
        this.message = message;
        this.resourcePath = resourcePath;
    }
}