package com.luckyframework.httpclient.proxy;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseConvert;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.BasicAuth;
import com.luckyframework.httpclient.proxy.annotations.CookieParam;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.HeaderParam;
import com.luckyframework.httpclient.proxy.annotations.InputStreamParam;
import com.luckyframework.httpclient.proxy.annotations.Proxy;
import com.luckyframework.httpclient.proxy.annotations.StaticForm;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.StaticPath;
import com.luckyframework.httpclient.proxy.annotations.StaticQuery;
import com.luckyframework.httpclient.proxy.annotations.FormParam;
import com.luckyframework.httpclient.proxy.annotations.ResourceParam;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticResource;
import com.luckyframework.httpclient.proxy.annotations.Timeout;
import com.luckyframework.httpclient.proxy.annotations.URLEncoderQuery;
import com.luckyframework.httpclient.proxy.annotations.Url;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
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
//        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
//        factory.setHttpExecutor(new OkHttpExecutor());
//        factory.setRequestAfterProcessor(r -> log.info(r.toString()));
//
//        GaoDeApi gaoDeApi = factory.getJdkProxyObject(GaoDeApi.class);
//        System.out.println(gaoDeApi.weatherStr("武汉"));

//        System.out.println(gaoDeApi.weatherParamUrlStr("/weatherInfo", "武汉"));
//        System.out.println(gaoDeApi.weatherLivesMap("广东"));
//        System.out.println(gaoDeApi.weatherMap("恩施"));

//        HttpClientProxyObjectFactory luckyFactory = new HttpClientProxyObjectFactory();
//        luckyFactory.setHttpExecutor(new OkHttpExecutor());
//        luckyFactory.setRequestAfterProcessor(r -> log.info(r.toString()));
//
//        LuckyApi luckyApi = luckyFactory.getCglibProxyObject(LuckyApi.class);
////        String imagePath = "https://ts1.cn.mm.bing.net/th/id/R-C.38b3920de3bbb1a8709cebdd9a5232db?rik=9qF1hmbI8E5S2A&riu=http%3a%2f%2fimage.qianye88.com%2fpic%2f250ca79ac5bc699d70515a175d50d84c&ehk=4ByzzCUqAMcxjNRcOHJuoTOhOvBeAAjMJVUKPY9qblg%3d&risl=&pid=ImgRaw&r=0";
//        File imageFile = new File("/Users/fukang/Pictures/IMG_0452.JPG");
//        List<File> files = new ArrayList<>();
//        files.add(imageFile);
//
//        StopWatch sw = new StopWatch();
//        sw.start("fileUpload");
//        luckyApi.fileUpload("文件上传测试", files);
//        sw.stopLast();

//        sw.start("fileUpdate");
////        luckyApi.fileUpload("lucky client", imagePath);
//        FilePojo fp = new FilePojo("File Pojo Message", imagePath);
//
//        luckyApi.fileUpload(fp, new SaveResultResponseProcessor(){
//            @Override
//            protected void responseProcess(Response response) {
//                log.info("response processed...");
//            }
//        });
//        sw.stopLast();

//        sw.start("putUser");
//        Map<String, Object> userMap = new HashMap<>(4);
//        userMap.put("id", 12324);
//        userMap.put("userName", "罗艳玲");
//        userMap.put("password", "768990");
//        userMap.put("price", 999999.999);
//        luckyApi.putUser(userMap);
//        sw.stopLast();
//
//        sw.start("getUser");
//        System.out.println(luckyApi.getUser());
//        sw.stopLast();
//
//        sw.start("getFile");
//        MultipartFile file = luckyApi.getFile("lucky file getter test", "msg2 hello");
//        file.setFileName("test");
//        file.copyToFolder("/Users/fukang/Lucky/Luckliy/lucky-httpclient/src/main/resources");
//        sw.stopLast();

//        sw.stopWatch();
//        System.out.println(sw.prettyPrintMillis());
//        System.out.println(map);

//        HttpExecutor httpExecutor = new JdkHttpExecutor();
//        Request request = Request.post("http://www.baidu.com/user")
//                .setProxy("127.0.0.1", 8080)
//                .setConnectTimeout(100)
//                .setReadTimeout(1)
//                .addCookie("user", "book")
//                .addCookie("password", "pesdsd")
//                .addHeader("TOKEN", "123456")
//                .setHeader("TOKEN", "1234567");
//        System.out.println(httpExecutor.getString(request));
//
//        MultipartFile multipartFile = httpExecutor.getForMultipartFile("http://localhost:8080/getFile?msg={0}&msg2={1}", "Hello", "Word");
//        System.out.println(multipartFile);
//
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        HttpClientProxyObjectFactory.setSpELConverter(
                new SpELConvert().importPackage(LuckyApi.class)
        );
        factory.setRequestAfterProcessor(r -> log.info(r.toString()));
        factory.setHttpExecutor(new JdkHttpExecutor());
        LuckyApi luckyApi = factory.getJdkProxyObject(LuckyApi.class);
//        System.out.println(luckyApi.getFile());
        InputStream inputStream = ConversionUtils.conversion(LuckyApi.uploadFilePath2(), InputStream.class);
        luckyApi.fileUpload2(LuckyApi.uploadFilePath2());
//        luckyApi.fileUpload("static resource test");
//        System.out.println(luckyApi.getUser("Admin", "PA$$W0RD", "TOKEN-7075768976"));
//        System.out.println(luckyApi.getFile("嘿嘿", "哈哈"));
//        User user = new User();
//        user.setId(10086);
//        user.setUserName("罗艳玲");
//        user.setPassword("768976");
//        user.setPrice(9999.999D);
//        luckyApi.putUser(user);


    }
}

@StaticPath({"version=v3"})
@StaticQuery({"key=833152601d928116dd12555fe949c214"})
@DomainName("https://restapi.amap.com/{version}/weather/")
interface GaoDeApi {

    @Get("weatherInfo")
    String weatherStr(String city);

    @Get
    String weatherParamUrlStr(@Url String api, String city);

    @Get("weatherInfo")
    @ResultConvert(GaoDeResponseConvert.class)
    Map<String, Object> weatherLivesMap(String city);

    @Get("weatherInfo")
    Map<String, Object> weatherMap(String city);

    class GaoDeResponseConvert implements ResponseConvert {

        @Override
        public <T> T convert(Response response, Type resultType) throws Exception {
            Map<String, Object> resultMap = response.getEntity(new SerializationTypeToken<Map<String, Object>>() {
            });
            return ConversionUtils.conversion(((List) resultMap.get("lives")).get(0), resultType);
        }
    }
}

@Async
@StaticHeader({"TOKEN=123456", "TEST-HEADER=中文测试"})
@BasicAuth(username = "admin", password = "123456")
@Proxy(ip = "#{T(LuckyApi).ipAddress()}", port = "#{T(LuckyApi).getPort()}")
@DomainName("#{T(LuckyApi).getLuckyApiDomainName()}")
interface LuckyApi {

    @Post("fileUpload")
    @StaticForm({"msg=3306"})
    void fileUpload2(@InputStreamParam(name = "file", filename = "gougou.jpeg") String inputStream);

    @Post("fileUpload")
    void fileUpload(@FormParam("msg") String message, @ResourceParam("file") String filePath);

    @Post("fileUpload")
    void fileUpload(@FormParam String msg, List<File> file);

    @Post("fileUpload")
    @StaticResource({"file=https://ts1.cn.mm.bing.net/th/id/R-C.0a5dd0a60d9457a9aa9d1b6b990a75e2?rik=NOoaRlopuKZstA&riu=http%3a%2f%2fseopic.699pic.com%2fphoto%2f50051%2f9950.jpg_wh1200.jpg&ehk=tESE5KqtlaOOss8Jb6UhUBX9F8qHCS6aUSnvUC6ut3Q%3d&risl=&pid=ImgRaw&r=0"})
    void fileUpload(@FormParam String msg);

    //    @Async
    @Post("fileUpload")
    @StaticForm({"msg=#{T(LuckyApi).uploadMsg('树先生')}"})
    @StaticResource({"file=#{T(LuckyApi).uploadFilePath()}"})
    void fileUpload();

    @Post("fileUpload")
    void fileUpload(FilePojo filePojo, ResponseProcessor responseProcessor);

    @Post("putUser")
    void putUser(@JsonBody Map<String, Object> user);

    @Post("putUser")
    void putUser(@JsonBody User user);

    @Post("user")
    User getUser(@CookieParam String user, @CookieParam String password, @HeaderParam("TOKEN") String token);

    @Get("getFile")
    @URLEncoderQuery
    MultipartFile getFile(String msg, String msg2);

    @Get("getFile")
    @StaticQuery(value = {"msg=hello word", "msg = 我爱我的祖国"}, urlEncode = true)
    MultipartFile getFile();

    static String getLuckyApiDomainName() {
        return "http://localhost:8081";
    }

    static Resource uploadFilePath() {
        String path = "https://pic1.zhimg.com/v2-254c56a96610c4f50cb02f0ada316c29_r.jpg?source=1940ef5c";
        return ConversionUtils.conversion(path, Resource.class);
    }

    static String uploadFilePath2() {
        return "https://ts1.cn.mm.bing.net/th/id/R-C.7859264e8e662049d23f5679b4347a39?rik=xiUUPm8WSQ2t4Q&riu=http%3a%2f%2fpic.bizhi360.com%2fbbpic%2f20%2f5920.jpg&ehk=g47Yu2qiW009cMIJju2Ye2ddaa3mA9UvGSsg%2beXnaa4%3d&risl=&pid=ImgRaw&r=0";
    }

    static String ipAddress() {
        return "127.0.0.1";
    }

    static int getPort() {
        return 8080;
    }

    static String urlEncode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    static String uploadMsg(String name) {
        return "Hello " + name;
    }
}

class FilePojo {

    @FormParam("msg")
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

    public static User defaultUser() {
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
