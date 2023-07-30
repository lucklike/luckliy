package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StopWatch;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseConvert;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.*;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 04:41
 */
public class V2TestDemo {

    private static final Logger log = LoggerFactory.getLogger(V2TestDemo.class);

    public static void main(String[] args) throws IOException {
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addPathParameter("version", "v3");
        factory.setRequestAfterProcessor(System.out::println);

        GaoDeApi gaoDeApi = factory.getJdkProxyObject(GaoDeApi.class);
//        System.out.println(gaoDeApi.weatherStr("武汉"));
//        System.out.println(gaoDeApi.weatherParamUrlStr("/weatherInfo", "武汉"));
//        System.out.println(gaoDeApi.weatherLivesMap("广东"));
//        Map<String, Object> map = gaoDeApi.weatherMap("恩施");

        HttpClientProxyObjectFactory luckyFactory = new HttpClientProxyObjectFactory();
        luckyFactory.setRequestAfterProcessor(System.out::println);
        LuckyApi luckyApi = luckyFactory.getCglibProxyObject(LuckyApi.class);
        String imagePath = "https://ts1.cn.mm.bing.net/th/id/R-C.4f8a31cc3b8599f047b075ffcc111891?rik=9CsauFlSl8kIeA&riu=http%3a%2f%2fwww.quazero.com%2fuploads%2fallimg%2f140305%2f1-140305222328.jpg&ehk=dANwIADtWaagt3jED8hmKAr9RdY2uP1PrDOJ5404TSY%3d&risl=&pid=ImgRaw&r=0";

        StopWatch sw = new StopWatch();
        sw.start("fileUpdate");
//        luckyApi.fileUpload("lucky client", imagePath);
        FilePojo fp = new FilePojo("File Pojo Message", imagePath);

        luckyApi.fileUpload(fp, new SaveResultResponseProcessor(){
            @Override
            protected void responseProcess(Response response) {
                log.info("response processed...");
            }
        });
        sw.stopLast();

        sw.start("putUser");
        Map<String, Object> userMap = new HashMap<>(4);
        userMap.put("id", 12324);
        userMap.put("userName", "罗雁珠");
        userMap.put("password", "768990");
        userMap.put("price", 999999.999);
        luckyApi.putUser(userMap);
        sw.stopLast();

        sw.start("getUser");
        System.out.println(luckyApi.getUser());
        sw.stopLast();

        sw.start("getFile");
        MultipartFile file = luckyApi.getFile("lucky file getter test", "msg2 hello");
        file.setFileName("test");
        file.copyToFolder("/Users/fukang/Lucky/Luckliy/lucky-httpclient/src/main/resources");
        sw.stopLast();

        sw.stopWatch();
        System.out.println(sw.prettyPrintMillis());
//        System.out.println(map);
    }
}

@DomainName("https://restapi.amap.com/{version}/weather/")
@RequestConf(
        commonQueryParams = {
            @KV(name = "key", value = "833152601d928116dd12555fe949c214")
        })
interface GaoDeApi {

    @Get("weatherInfo")
    String weatherStr(String city);

    @Get
    String weatherParamUrlStr(@Url String api, String city);

    @Get("weatherInfo")
    @ResponseConf(GaoDeResponseConvert.class)
    Map<String, Object> weatherLivesMap(String city);

    @Get("weatherInfo")
    Map<String, Object> weatherMap(String city);

    class GaoDeResponseConvert implements ResponseConvert {

        @Override
        public <T> T convert(Response response, Type resultType) throws Exception {
            Map<String, Object> resultMap = response.getEntity(new SerializationTypeToken<Map<String, Object>>() {});
            return ConversionUtils.conversion(((List) resultMap.get("lives")).get(0), resultType);
        }
    }
}

@Async
@SpELDomainName("#{T(com.luckyframework.httpclient.proxy.LuckyApi).getLuckyApiDomainName()}")
interface LuckyApi {

    @Post("fileUpload")
    void fileUpload(@RequestParam("msg") String message, @ResourceParam("file") String filePath);

    @Post("fileUpload")
    void fileUpload(FilePojo filePojo, ResponseProcessor responseProcessor);

    @Post("putUser")
    void putUser(@JsonBody Map<String, Object> user);

    @Post("user")
    User getUser();

    @Get("getFile")
    @URLEncoderQuery
    MultipartFile getFile(String msg, String msg2);

    static String getLuckyApiDomainName() {
        return "http://localhost:8080";
    }
}

class FilePojo {

    @RequestParam("msg")
    private final String message;

    @ResourceParam("file")
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
 class User {

    private Integer id;
    private String userName;
    private String password;
    private Double price;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public static User defaultUser(){
        User user = new User();
        user.setId(1);
        user.setUserName("Jack 付康");
        user.setPassword("PA$$W0RD");
        user.setPrice(112.8);
        return user;
    }


    public static List<User> defaultUserList() {
        List<User> list = new ArrayList<>();
        list.add(defaultUser());
        User user = new User();
        user.setId(1);
        user.setUserName("Lucy 谢弗里");
        user.setPassword("HAhahaPA$$W0RD");
        user.setPrice(1454.9);
        list.add(user);
        return list;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", price=" + price +
                '}';
    }
}
