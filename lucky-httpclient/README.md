# <center> lucky-httpclient

##  🍀 简介  

---

`lucky-httpclient`是一个简单易用的HTTP客户端工具，提供了`编程式`和`注解式`两种编码方式，支持`异步调用`与`响应结果选择`，并提供了丰富的扩展机制，开发者可以根据自己的需求来定制和扩展

## ⚙️ 安装

---

🪶 Maven  
在项目的`pom.xml`的`dependencies`中加入以下内容:
```xml
    <dependency>
        <groupId>io.github.lucklike</groupId>
        <artifactId>lucky-httpclient</artifactId>
        <version>2.0.0</version>
    </dependency>
```

🐘 Gradle

```groovy
    implementation group: 'io.github.lucklike', name: 'lucky-httpclient', version: '2.0.0'
```

---

## 📃 开发文档

## **一. 编程式开发**

---

编程式开发中主要会涉及到以下几个组件：
1. [Request](./src/main/java/com/luckyframework/httpclient/core/Request.java)  
    请求信息，用于封装http请求信息如：`url`、`method`、`headers`、`query`、`form`、 `body`、`file`等。

2. [Response](./src/main/java/com/luckyframework/httpclient/core/Response.java)  
    响应信息，用于封装HTTP响应信息如：响应状态码、响应头、响应体等

3. [HttpExecutor](./src/main/java/com/luckyframework/httpclient/core/executor/HttpExecutor.java)    
    HTTP请求执行器，用于发起请求和返回响应结果，内置以下了三种实现：  
    - `JdkHttpExecutor`： 基于Jdk`HttpURLConnection`实现。  
    - `HttpClientExecutor`： 基于`Apache HttpClient`实现，使用该实现需要导入`Apache HttpClient`相关的依赖。  
    - `OkHttpExecutor`： 基于`OkHttp3`实现，使用该实现需要导入`OkHttp3`相关的依赖。

4. [ResponseProcessor](./src/main/java/com/luckyframework/httpclient/core/ResponseProcessor.java)  
    响应处理器，通过该接口可以获取`原始响应流`，做大文件下载时可以使用该接口进行`流式处理`。

5. [SaveResultResponseProcessor](./src/main/java/com/luckyframework/httpclient/core/impl/SaveResultResponseProcessor.java)  
    `ResponseProcessor`接口的一个重要实现类，用于将原始响应数据转化为`Response`对象

**开发流程如下：**
1. 创建一个用于执行http请求的执行器`HttpExecutor`
2. 创建一个具体的请求对象`Request`
3. 使用执行器的`execute()`方法执行请求，并得到一个响应`Response`
4. 根据业务需求处理响应结果

### 👀 代码示例

---

1️⃣ **【 `GET` 获取百度首页】**


```java
    // 1.创建一个用于执行http请求的请求执行器
    HttpExecutor httpExecutor = new JdkHttpExecutor();

    // 2.创建一个GET请求
    Request req = Request.post("https://www.baidu.com");

    // 3.执行请求返回一个响应
    Response response = httpExecutor.execute(req);
    
    // 4.将相应结果转化为UTF8编码的String类型并打印
    System.out.println(response.getStringResult(StandardCharsets.UTF_8));
```

2️⃣ **【 `POST` 表单提交】**

```java
    // 1.创建一个用于执行http请求的请求执行器
    HttpExecutor httpExecutor = new JdkHttpExecutor();

    // 2.创建一个POST表单提交请求
    Request req = Request.post("http://127.0.0.1:8080/addUser")
        .addFormParameter("name", "Jack")
        .addFormParameter("sex", "男")
        .addFormParameter("age", "22")
        .addFormParameter("vip", true);

    // 3.执行请求返回一个响应
    Response response = httpExecutor.execute(req);
    System.out.println(response.getStringResult());
```

3️⃣ **【文件下载 -- `内存byte模式`】**

```java
    // 图片地址
    String filePath = "https://ts1.cn.mm.bing.net/th/id/R-C.b49dbddffaa692d75988e0c5882dacca?rik=r6IIYs2muimY7A&riu=http%3a%2f%2fwww.quazero.com%2fuploads%2fallimg%2f140529%2f1-140529145A4.jpg&ehk=Co9XURYRCjJXUTzFG0Mw6lD7olzDKceEgv3slEC8kvQ%3d&risl=&pid=ImgRaw&r=0";
    HttpExecutor httpExecutor = new JdkHttpExecutor();
    Request req = Request.get(filePath);
    Response response = httpExecutor.execute(req);
    
    // 使用Response的getMultipartFile方法获取一个MultipartFile对象
    MultipartFile file = response.getMultipartFile();
    // 将图片保存在D盘
    file.copyToFolder("D:/");
```

4️⃣ **【大文件下载 -- `流式下载`】**

```java
    // 系统镜像地址
    String fileUrl = "https://mirrors.sohu.com/centos/8/isos/x86_64/CentOS-8.5.2111-x86_64-dvd1.iso";
    HttpExecutor httpExecutor = new JdkHttpExecutor();
    Request req = Request.get(fileUrl);
    
    // 利用ResponseProcessor接口获取原始响应流后进行流式处理
    httpExecutor.execute(req, new ResponseProcessor() {
        @Override
        public void process(ResponseMetaData responseMeta) {
            try {
                String savePath = StringUtils.format("D:/{}", responseMeta.getDownloadFilename());
                OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(savePath)));
                FileCopyUtils.copy(responseMeta.getInputStream(), out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    });
```

5️⃣ **【 `POST` 文件上传】**

```java
    Request request = Request.post("http://127.0.0.1:8080/file/upload")
            // 添加本地文件（File）
            .addFiles("file", new File("D:/github-poject/luckliy_v4/LUCKY_V4_TEST/springboot-test/pom.xml"))
            // 添加InputStream
            .addInputStream("file2", "HELP.md", Files.newInputStream(Paths.get("D:/github-poject/luckliy_v4/LUCKY_V4_TEST/springboot-test/HELP.md")))
            // 添加Resource
            .addResources("file3", "classpath:application.properties", "https://ts1.cn.mm.bing.net/th/id/R-C.jpeg");

    HttpExecutor httpExecutor = new JdkHttpExecutor();
    httpExecutor.execute(request);
```

6️⃣ **【Restful请求】**  

```java
    // 使用Map封装参数，也可以使用实体类来封装参数
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("id", 123);
    userMap.put("name", "Test User");
    userMap.put("age", 22);
    userMap.put("email", "test@example.com");

    // 使用setJsonBody()方法设置JSON格式的请求体参数
    Request request = Request.put("http://127.0.0.1:8080/putUser")
                             .setJsonBody(userMap);

    HttpExecutor httpExecutor = new JdkHttpExecutor();
    Response response = httpExecutor.execute(request);
    
    // 当明确返回值为JSON格式的字符串时，可以使用jsonStrToEntity()方法将返回会结果直接反序列化为实体对象
    User entity = response.jsonStrToEntity(User.class);

    // 如果不确定返回值格式时可以使用getEntity()方法来反序列化结果，但是这种方法只支持JSON和XML格式的响应格式
    User entity2 = response.getEntity(User.class);
    System.out.println(entity);
```

7️⃣  **【配置代理】**

```java
    Request request = Request.post("url")
              // 代理无需账号密码，可以直接这样设置
             .setProxy("127.0.0.1", 9080)
              // 如果需要自定其他类型代理或更多的项目，可以这样设置
             .setProxy(new Proxy(Proxy.Type.HTTP,
                     new InetSocketAddress(host, port));
```

 8️⃣ **【超时设置】**
 
```java
    Request request = Request.post("url")
            // 设置连接超时时间
            .setConnectTimeout(2000)
            // 设置读超时时间
            .setReadTimeout(2000)
            // 设置写超时时间
            .setWriterTimeout(2000);
```
9️⃣ **【请求头设置】**

```java
    Request request = Request.post("url")
            // 添加请求头
            .addHeader("token", "2eefefergrthytu6u565kjgjn--")
            // 设置请求头
            .setHeader("Content-Type", "application/json")
            // 简单验证（basicAuth方法）
            .setAuthorization("user", "password")
            // 添加一个Cookie信息
            .addCookie("c1", "fk-7075");
```

🔟 **【使用HttpExecutor.xxxForXxx()方法简化调用流程】**

```java
    HttpExecutor httpExecutor = new JdkHttpExecutor();

    // 使用ForString()方法直接获取String类型结果
    String stringResult = httpExecutor.getForString("https://api.oioweb.cn/api/qq/info?qq={}", 2809110992L);

    // 使用ForEntity()方法直接将响应结果反序列化为实体
    Map map = httpExecutor.getForEntity("https://api.oioweb.cn/api/qq/info?qq={}", Map.class, 2809110992L);

    // 使用ForMultipartFile方法获取响应体中的文件
    MultipartFile multipartFile = httpExecutor.getForMultipartFile("https://ts1.cn.mm.bing.net/th/id/R-C.jpeg");
    
```

## **二. 注解开发**

---
`注解开发`是在`编程式开发`的基础上做了一层封装，进一步的简化了开发。注解开发模式下我们只需要`声明一个接口`，然后使用`特定的注解`进行相关的描述即可,lucky-httpclient底层会使用`动态代理`机制帮我们生成代理对象，通过代理对象便可以完成所有的http请求。

🍋 **使用`HttpClientProxyObjectFactory`生成Http接口的代理对象以及配置重要的请求参数**
- [HttpClientProxyObjectFactor中重要的方法](./src/main/java/com/luckyframework/httpclient/proxy/HttpClientProxyObjectFactory.java)

  | 重要方法                                                                                              | 方法注释                                                          |
  |---------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
  | `getCglibProxyObject(Class<T> interfaceClass)`                                                    | 使用`Cglib代理`生成`代理对象`并返回                                        |
  | `getJdkProxyObject(Class<T> interfaceClass)`                                                      | 使用`Jdk代理`生成`代理对象`并返回                                          |
  | `addExpressionParam(String name, Object value)`                                                   | `[static]`添加一个`SpEL表达式`参数，该参数可以在支持SpEL表达式的注解中直接使用`例如: #{key}` |
  | `setSpELConverter(SpELConvert spELConverter)`                                                     | `[static]`设置一个用于解析`SpEL表达式`的解析器                               |
  | `setExecutor(Executor executor)`                                                                  | 设置一个`用于执行异步请求`的`线程池`                                          |
  | `setExecutorSupplier(Supplier<Executor> executorSupplier)`                                        | 设置一个`用于执行异步请求`的`线程池`的`Supplier`对象，用于延迟创建                      |
  | `setExecutorSupplier(Supplier<Executor> executorSupplier)`                                        | 设置一个`用于执行异步请求`的`线程池`的`Supplier`对象，用于延迟创建                      |
  | `setHttpExecutor(HttpExecutor httpExecutor)`                                                      | 设置用于`执行HTTP请求`的`请求执行器`                                        |
  | `setExceptionHandle(HttpExceptionHandle exceptionHandle)`                                         | 设置用于处理异常的`异常处理器`                                              |
  | `setObjectCreator(ObjectCreator objectCreator)`                                                   | 设置用于创建组件对象的`对象创建器`                                            |
  | `addRequestAfterProcessors(RequestAfterProcessor... requestAfterProcessors)`                      | 设置`请求处理器`，在之`请求执行之前`会执行该接口实例的方法                               |
  | `addResponseAfterProcessors(ResponseAfterProcessor... responseAfterProcessors)`                   | 设置`响应处理器`，在之`响应返回之后`执行该接口实例的方法                                |
  | `setConnectionTimeout(int connectionTimeout)`                                                     | 设置`连接超时时间 `                                                   |
  | `setReadTimeout(int readTimeout)`                                                                 | 设置`读超时时间 `                                                    |
  | `setWriteTimeout(int writeTimeout)`                                                               | 设置`写超时时间 `                                                    |
  | `setHeaders(ConfigurationMap headerMap)`                                                          | 设置公共的`请求头`参数                                                  |
  | `setProxyClassHeaders(Class<?> proxyClass, Map<String, Object> proxyClassHeaders)`                | 为代理类`proxyClass`设置`专用的`公共`请求头`参数                              |
  | `setPathParameters(ConfigurationMap pathMap)`                                                     | 设置公共的`路径`参数                                                   |
  | `setProxyClassPathParameters(Class<?> proxyClass, Map<String, Object> proxyClassPathParameters)`  | 为代理类`proxyClass`设置`专用的`公共`路径`参数                               |
  | `setQueryParameters(ConfigurationMap queryMap)`                                                   | 设置公共的`URL`参数                                                  |
  | `setProxyClassQueryParameter(Class<?> proxyClass, Map<String, Object> proxyClassQueryParameters)` | 为代理类`proxyClass`设置`专用的`公共`URL`参数                              |
  | `setFormParameters(ConfigurationMap formMap)`                                                     | 设置公共的`表单`参数                                                   |
  | `setProxyClassFormParameter(Class<?> proxyClass, Map<String, Object> proxyClassFormParameters)`   | 为代理类`proxyClass`设置`专用的`公共`表单`参数                               |


```java
    // 设置SpEL表达式参数
    HttpClientProxyObjectFactory.addExpressionParam("baiduUrl", "http://www.baidu.com");
    HttpClientProxyObjectFactory.addExpressionParam("googleUrl", "http://www.google.com");
    // 设置SpEl表达式转换器
    HttpClientProxyObjectFactory.setSpELConverter(new SpELConvert());

    HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
    // 设置连接超时时间
    factory.setConnectionTimeout(2000);
    // 设置读超时时间
    factory.setReadTimeout(2000);
    // 设置写超时时间
    factory.setWriteTimeout(2000);
    // 设置HTTP执行器为Okhttp请求执行器
    factory.setHttpExecutor(new OkHttpExecutor());
    // 设置用于异步执行HTTP任务的线程池
    factory.setExecutor(Executors.newFixedThreadPool(10));
    // 设置异常处理器
    factory.setExceptionHandle(new DefaultHttpExceptionHandle());
    // 添加请求处理器
    factory.addRequestAfterProcessors(new PrintLogProcessor());
    // 添加响应处理器
    factory.addResponseAfterProcessors(new PrintLogProcessor());

    // 添加公共请求头参数
    ConfigurationMap headers = new ConfigurationMap();
    headers.put("X-TOKEN", "dscsdvfdgerggegrherh");
    headers.put("X-SESSION-ID", "SDSDSDSDSDXSSX");
    factory.setHeaders(headers);

    // 设置百度API专用的请求头
    ConfigurationMap baiduHeaders = new ConfigurationMap();
    baiduHeaders.put("BAIDU-USER", "nnig656464");
    baiduHeaders.put("BAIDU-TEST", "test-vi");
    factory.setProxyClassHeaders(BaiduApi.class, baiduHeaders);

    // 基于JDK实现的代理对象
    BaiduApi jdkBaiduApi = factory.getJdkProxyObject(BaiduApi.class);

    // 基于Cglib实现的代理对象
    BaiduApi cglibBaiduApi = factory.getCglibProxyObject(BaiduApi.class);
```
  
---

🍓 **使用`@HttpRequest`系注解将接口方法标记为HTTP请求方法（支持SpEL表达式）**

---

`@HttpRequest`系注解有：  

| 注解         | 请求方法      |
|------------|-----------|
| `@Get `    | GET请求     |
| `@Post`    | POST请求    |
| `@Delete`  | DELETE请求  |
| `@Put`     | PUT请求     |
| `@Head`    | HEAD请求    |
| `@Patch`   | PATCH请求   |
| `@Connect` | CONNECT请求 |
| `@Options` | OPTIONS请求 |
| `@Trace`   | TRACE请求   |

```java
import com.luckyframework.httpclient.proxy.annotations.Delete;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;

public interface JSXSApi {

    /*
        使用HttpClientProxyObjectFactory.addExpressionParam("baiduUrl", "https://www.baidu.com")方法设置了表达式参数后，
        便可以在SpEL表达式中使用配置的key直接拿到value
     */
    @Get("#{baiduUrl}")
    String baidu();

    // 删除ID为1的book
    @Delete("http://localhost:8080/book/delete/1")
    void deleteBook();

    // 新增一个book
    @Post("http://localhost:8080/book/insert")
    void addBook(Book book);
}

```

  
🍊 **使用`@DomainName`注解提取域名（支持SpEL表达式）**

---

开发中建议将`同一个域名`或者`同一域名中某个特定的模块`下的Http接口组织到`同一个Java接口`，这样便可以使用 **`@DomainName`** 注解来提取公共域名，方便统一管理。例如：上面的接口加上 **`@DomainName`** 注解之后便可以简化为如下代码：

```java
package com.springboot.testdemo.springboottest.api;
import com.luckyframework.httpclient.proxy.annotations.Delete;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;

// 直接配置域名
@DomainName("http://localhost:8080/book/")

/*
    使用HttpClientProxyObjectFactory.addExpressionParam("JSXS", "http://localhost:8080/book/")方法设置了表达式参数后，
    便可以在SpEL表达式中使用配置的key直接拿到value
 */
@DomainName("#{JSXS}")

// 使用SpEL表达式获取域名
@DomainName("#{T(com.springboot.testdemo.springboottest.api.JSXSApi).getDomainName()}")
public interface JSXSApi {

    // 获取百度首页
    @Get("https://www.baidu.com")
    String baidu();

    // 删除ID为1的book
    @Delete("/delete/1")
    void deleteBook();

    // 新增一个book
    @Post("/insert")
    void addBook(Book book);
    
    static String getDomainName() {
        return "http://localhost:8080/book/";
    }
}
```

🍎 **使用`@DynamicParam`系列注解动态的设置请求参数**

---

| 注解                  | 对应请求参数                          | 对应Request方法         |
|---------------------|---------------------------------|---------------------|
| `@Url`              | 动态设置URL                         | setUrlTemplate()    |
| `@QueryParam`       | 动态设置URL参数                       | addQueryParameter() |
| `@PathParam`        | 动态设置填充URL占位符的参数                 | addPathParameter()  |
| `@URLEncoderQuery`  | 动态设置URL参数（自动UrlEncoder编码）       | addQueryParameter() |
| `@URLEncoderPath`   | 动态设置填充URL占位符的参数（自动UrlEncoder编码） | addPathParameter()  |
| `@FormParam`        | 动态设置表单参数                        | addFormParameter()  |
| `@HeaderParam`      | 动态设置请求头参数                       | addHeader()         |
| `@CookieParam`      | 动态设置设置Cookie信息                  | addCookie()         |
| `@ResourceParam`    | 动态设置文件参数                        | addResources()      |
| `@InputStreamParam` | 动态设置文件参数(InputStream方式)         | addHttpFiles()      |
| `@BodyParam`        | 动态设置请求体参数                       | setBody()           |
| `@JsonBody`         | 动态设置JSON格式的请求体参数（自动序列化为JSON字符串） | setBody()           |
| `@XmlBody`          | 动态设置XML格式的请求体参数（自动序列化为XML字符串）   | setBody()           |

<font color='red'>注：</font>遇到下面这些`特殊类型`时`@DynamicParam`注解不会生效：
1. 当方法参数为`ResponseProcessor`类型时，当得到结果时会执行该参数的`process方法`。
2. 当方法参数为`File`、`Resource`、`MultipartFile`、`HttpFile`类型或者为`这些类型的数组`或`集合`时，会使用`addHttpFiles()`进行参数设置。
3. 当方法参数为`BodyObject`类型时，会使用`setBody()`方法进行参数设置。

**_如果方法或者方法参数上没有标注任何`@DynamicParam`注解时，则默认使用`addQueryParameter()`方法进行参数设置。_**
`@DynamicParam`注解的具体用法：

```java


import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.QueryParam;
import com.luckyframework.httpclient.proxy.annotations.Url;
import com.luckyframework.io.MultipartFile;

@DomainName("http://localhost:8080/users")
public interface UserApi {

    /*
        没有任何注解时，默认方法参数为URL参数
        GET http://localhost:8080/users/getById?id=id_value 
     */
    @Get("/getById")
    User getUserById(Integer id);

    /*
         @QueryParam注解标注的参数将设置为Url参数(query参数)
         GET http://localhost:8080/users/getById?id=number
     */
    @Get("/getById")
    User getUserById2(@QueryParam("id") Integer number);

    /*
        @PathParam注解标注的参数将设置为填充Url占位符'{}'的参数
        GET http://localhost:8080/users/get/num_value
     */
    @Get("/get/{id}")
    User getUser(@PathParam("id") Integer num);

    /*
        @HeaderParam注解标注的参数将设置为Header参数
        @CookieParam注解标注的参数将设置为Cookie参数
        
        DELETE  http://localhost:8080/users/cookieHeader
        token: token_value
        Cookie: sessionId=sessionId_value; userId=userId_value
     */
    @Delete("cookieHeader")
    void cookieHeader(@HeaderParam("token") String c, @CookieParam("sessionId") String h, @CookieParam("userId") String u);

    /*
        @FormParam注解表示表单提交，lucky底层会将展开User的所有属性来形成表单内容
        POST http://localhost:8080/users/get/insertByForm
        Content-Type: application/x-www-form-urlencoded
        
        id=id_value&
        name=name_value&
        sex=sex_value&
        age=age_value&
        email=email_value
     */

    @Post("insertByForm")
    void insertUser(@FormParam User user);

    /*
        @JsonBody注解标注的参数会被序列化为JSON格式字符串
        POST http://localhost:8080/users/get/insertByJson
        Content-Type: application/json;
        
        {
            "id": "id_value",
            "name": "name_value",
            "age": "age_value",
            "sex": "sex_value",
            "email": "email_value",
        }    
     */
    @Post("insertByJson")
    void insertByJson(@JsonBody User user);

    /*
        文件上传，File、Resource、MultipartFile、HttpFile这四种类型或者这些类型的数组或集合会自动的当做文件参数来处理
        POST http://localhost:8080/users/fileUpload
        Content-Type: multipart/form-data; boundary=LuckyBoundary
        
        --LuckyBoundary
        Content-Disposition: form-data; name="msg"
        Content-Type: text/plain
        
        msg_value
        --LuckyBoundary
        Content-Disposition: form-data; name="files"; filename="test.jpg"
        Content-Type: image/jpeg
        
        < D:/test/test.jpg
        --LuckyBoundary
        Content-Disposition: form-data; name="files"; filename="data.json"
        Content-Type: application/json
        
        < D:/json/data.json
     */
    @Post("fileUpload")
    void fileUpload(File[] files, @FormParam String msg);

    /*
        使用@ResourceParam注解来实现文件上传，lucky底层会将@ResourceParam注解标注的方法参数转化为Resource[]后进行文件参数处理
        这里支持String、String[]、Collection<String>等类型的参数转换，字符串内容为Spring的资源路径表达式,请参考ResourceLoader.getResource()
        例如：
        
        1. file:D:/test.jpg
        2. classpath:static/text.txt
        3. http://localhost:8080/files/test.jpg
        ...
     */
    @Post("fileUpload")
    void fileUpload(@ResourceParam String[] files, @FormParam String msg);

    /*
        使用@Url注解来实现动态Url切换的功能
        eg: 
        imageUrl="http://localhost:8080/files/test.jpg"
        GET http://localhost:8080/files/test.jpg
        
        imageUrl="http://localhost:8084/user/application.yml"
        GET http://localhost:8084/user/application.yml
     */
    @Get
    MultipartFile getImage(@Url String imageUrl);
}
```

🍒 **使用`@StaticParam`系列注解设置静态请求参数**

---

| 注解                | 对应请求参数       | 示例                                                                                                               | 支持`SpEL`表达式 |
|-------------------|--------------|------------------------------------------------------------------------------------------------------------------|:-----------:|
| `@BasicAuth`      | `简单身份认证`注解   | `@BasicAuth(username = "admin", password = "#{password}")`                                                       |      ✅      | 
| `@StaticHeader`   | 设置`请求头`参数    | `@StaticHeader({"SESSION-ID=HUUYGBKJHNOIJJPO", "TOKEN=#{token}"})`                                               |      ✅      | 
| `@StaticQuery`    | 设置`URL`参数    | `@StaticQuery({"appKey=#{appKey}", "version=v1.0.0"})`                                                           |      ✅      | 
| `@StaticForm`     | 设置`表单`参数     | `@StaticForm({"username=#{username}", "age=20", "sex=男"})`                                                       |      ✅      |
| `@StaticResource` | 设置`资源`参数     | `@StaticResource({"file1=#{file1Path}", "file2=classpath:statis/*.jpg", "file3=http://www.baidu.com/G-rc.jpg"})` |      ✅      | 
| `@StaticPath`     | 设置`路径`参数     | `@StaticPath({"api=#{api}", "fileName=test.jpg"})`                                                               |      ✅      | 
| `@StaticCookie`   | 设置`Cookie`参数 | `@StaticCookie({"sessionId=FE@GYGn56rnioIIHIH", "user-info=#{userInfo}"})`                                       |      ✅      | 
| `@Proxy`          | 设置`代理`       | `@Proxy(ip="127.0.0.1", port=#{port})`                                                                           |      ✅      | 
| `@Timeout`        | 设置`超时时间`参数   | `@Timeout(connectionTimeout = 2000, readTimeout = 2000, writeTimeout=#{writeTimeout})`                           |      ❌      | 

代码示例：  
```java
package com.springboot.testdemo.springboottest.api;

import com.luckyframework.httpclient.proxy.annotations.Delete;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.StaticCookie;
import com.luckyframework.httpclient.proxy.annotations.StaticForm;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.annotations.StaticPath;
import com.luckyframework.httpclient.proxy.annotations.StaticQuery;
import com.luckyframework.httpclient.proxy.annotations.StaticResource;
import com.springboot.testdemo.springboottest.beans.User;

/**
 * 使用@StaticParam系列注解静态的设置请求参数
 */
@DomainName("http://localhost:8080/users")
public interface User2Api {
    
    /*
        使用@StaticQuery注解静态的设置URL参数
        GET http://localhost:8080/users/getById?id=666
     */
    @Get("/getById")
    @StaticQuery("id=666")
    User getUserById();
  
    /*
        使用@StaticPath注解静态的设置URL占位符'{}'参数   
        GET http://localhost:8080/users/get/999
     */
    @Get("/get/{id}")
    @StaticPath("id=999")
    User getUser();
  
    /*
        使用@StaticForm注解静态的设置表单参数
        HttpClientProxyObjectFactory.addExpressionParam("user", "JackFu")
        
        POST http://localhost:8080/users/get/insertByForm
        Content-Type: application/x-www-form-urlencoded
        
        id=888&
        name=JackFu&
        sex=男&
        age=32&
        email=JackFu@qq.com    
     */
    @Post("insertByForm")
    @StaticForm({"id=888", "name=#{user}", "sex=男", "age=32", "email=#{user}@qq.com"})
    void insertUser();
  
    /*
        使用@StaticResource住额吉静态的设置资源参数
        POST http://localhost:8080/users/fileUpload
        Content-Type: multipart/form-data; boundary=LuckyBoundary
        
        --LuckyBoundary
        Content-Disposition: form-data; name="msg"
        Content-Type: text/plain
        
        @StaticForm + @StaticResource fileUpload
        --LuckyBoundary
        Content-Disposition: form-data; name="files"; filename="test.jpg"
        Content-Type: text/plain
        
        < D:/test/application.properties
        --LuckyBoundary
        Content-Disposition: form-data; name="files"; filename="data.json"
        Content-Type: text/plain
        
        < D:/test/jndi.properties
     */
    @Post("fileUpload")
    @StaticForm("msg=@StaticForm + @StaticResource fileUpload")
    @StaticResource({"files=classpath*:*.properties"})
    void fileUpload();
  
  
    /*
        使用@StaticCookie注解设置静态Cookie参数
        使用@StaticHeader注解设置静态请求头参数
          
        DELETE  http://localhost:8080/users/cookieHeader
        token: TOKEN-FK-7075
        Cookie: userId=FK7075; sessionId=SESSION_ID-HUIHOIO23465VHJBHBNLKJP
     */
    @Delete("cookieHeader")
    @StaticCookie({"userId=FK7075", "sessionId=SESSION_ID-HUIHOIO23465VHJBHBNLKJP"})
    @StaticHeader("token=TOKEN-FK-7075")
    void cookieHeader();

    /*
        使用@Timeout注解设置超时时间
        使用@BasicAuth注解设置简单权限认证信息
        使用@Proxy注解设置代理服务器信息
        GET http://www.baidu.com/users/get/589, PROXY: HTTP @ /127.0.0.1:8080
        Authorization: Basic Rks3MDc1OlBBJCRXMFJE
        
     */
    @Get("http://www.baidu.com/users/get/589")
    @Timeout(connectionTimeout = 10000, readTimeout = 1000)
    @Proxy(ip = "127.0.0.1", port = "8080")
    @BasicAuth(username = "FK7075", password = "PA$$W0RD")
    User getUserByBasicAuth();

}

```

🍑 **使用`ResponseProcessor`接口获取原始数据流**  

---

一般模式下lucky会将HTTP调用的结果以`byte[]`的形式保存在内存中，后续再做转换与返回，当遇到`大文件下载`或者`返回结果很大`时这种方案显然是不适用的，基于这个问题的解决方案就是`ResponseProcessor`，通过`ResponseProcessor`
接口可以获取到原始的数据输入流，便可以使用流式操作来避免内存被撑爆的问题。在`注解开发`模式下只需要在接口方法中定义好`ResponseProcessor`参数即可，lucky会在HTTP请求结束后自动找到`参数列表中`的`第一个ResponseProcessor`
参数来进行回调。示例代码如下：

```java
/**
 * 在注解开发模式下声明一个用于下载CentOS系统镜像的HTTP接口方法
 */
public interface LargeFileDownload {

  // 大文件下载场景，使用ResponseProcessor接口流式处理返回结果
  @Get("https://mirrors.sohu.com/centos/8/isos/x86_64/CentOS-8.5.2111-x86_64-dvd1.iso")
  void getSaveFile(ResponseProcessor processor);
}

// 生成代理对象并调用下载CentOS系统镜像的方法
public class Test {
  public static void main(String[] args) {
    HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
    LargeFileDownload api = factory.getJdkProxyObject(LargeFileDownload.class);

    api.getSaveFile(rmd -> {
        try {
          String savePath = StringUtils.format("D:/test/files/{}", rmd.getDownloadFilename());
          OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(savePath)));
          FileCopyUtils.copy(rmd.getInputStream(), out);
        }catch (IOException e) {
          throw new RuntimeException(e);
        }
    });
  }
}

```

🍉  **异步请求的声明**  

---

1. 对于`void方法`可以使用`@Async`注解将其标记为一个异步方法。在`接口上使用@Async注解`，则接口`中所有的void方法`都讲会使用`异步方式`来调用
2. 对于`非void方法`，如果需要异步返回则只需要将返回值用`Future`包裹即可，lucky会自动识别类型并发起异步调用。

```java
import com.luckyframework.httpclient.proxy.annotations.Async;

// 在接口上使用@Async注解，则接口中所有的void方法都讲会使用异步方式来调用
@Async
@DomainName("#{userModel}")
public interface UserApi {
    
  /*
      对于返回值为Future<?>类型的接口方法，lucky会默认采用异步调用的方式来进行请求
   */
  @Get("/get/{id}")
  Future<User> getUser(@PathParam Integer id);

  /*
      异步添加用户
      对于void方法，如果想使用异步调用，则必须使用@Async标注
   */
  @Async
  @Put("insertByJson")
  void insertByJson(@JsonBody User user);

  /*
      异步文件上传
      对于void方法，如果想使用异步调用，则必须使用@Async标注
   */
  @Async
  @Post("fileUpload")
  void fileUpload(File[] files, @FormParam String msg);

  /*
      异步文件下载
      大文件下载场景，可以使用@Async注解 + ResponseProcessor接口的方式进行异步流式处理
   */
  @Async
  @Get("https://mirrors.sohu.com/centos/8/isos/x86_64/CentOS-8.5.2111-x86_64-dvd1.iso")
  void largeFileDownload(ResponseProcessor processor);

}
```

🍇  **使用`@ResponseConvert`系列注解对响应结果进行转换**  

---
注：如果接口上配置了`@ResponseConvert`系列注解，那么注解中配置的转化器会对接口中所有的HTTP方法生效，如果某个HTTP方法并不想使用接口上配置的转换器逻辑时便可以使用`@ConvertProhibition`
注解来禁止  

目前`@ResponseConvert`系列注解只有一个：`@ResultSelect`  
可以使用`@ResultSelect`注解的`value`属性来对响应结果进行选取，如果取不到值但又想赋予默认值，则可以使用`defaultValue`来设置默认值，该属性支持`SpEL`表达式  
具体用法为：
```text
    value:
    取值表达式@resp.${key}，请参照{@link ConfigurationMap#getProperty(String)}的用法，
    其中'@resp'为固定的前缀，表示整合响应结果。
    从数组中取值使用下标：@resp.array[0].user或@resp[1].user.password
    从对象中取值：@resp.object.user或@resp.user.password
    
    defaultValue:
    配置默认值，支持SpEL表达式，当value取值表达式中指定的值不存在时，便会使用该默认值返回
```


> 以高德的天气API为例：

```java

@DomainName("#{gaoDeApi}")
public interface GaoDeApi {
   
    @Get("/v3/weather/weatherInfo?city=荆州")
    Object queryWeather();
}
```

- 不使用`@ResultSelect`注解时的返回结构为：

```json
{
    "status": "1",
    "count": "2",
    "info": "OK",
    "infocode": "10000",
    "lives": [
        {
            "province": "湖北",
            "city": "荆州区",
            "adcode": "421003",
            "weather": "阴",
            "temperature": "23",
            "winddirection": "北",
            "windpower": "≤3",
            "humidity": "87",
            "reporttime": "2023-09-11 23:31:32",
            "temperature_float": "23.0",
            "humidity_float": "87.0"
        },
        {
            "province": "湖北",
            "city": "荆州市",
            "adcode": "421000",
            "weather": "中雨",
            "temperature": "23",
            "winddirection": "西",
            "windpower": "≤3",
            "humidity": "87",
            "reporttime": "2023-09-11 23:31:31",
            "temperature_float": "23.0",
            "humidity_float": "87.0"
        }
    ]
}
```

- 如果只需要获取`lives`数组部分的数据，只需要在原来的接口方法上加上`@ResultSelect("@resp.lives")`即可：
```java
@DomainName("#{gaoDeApi}")
public interface GaoDeApi {
    @ResultSelect(key="@resp.lives", defaultValue="#{new java.util.ArrayList()}")
    @Get("/v3/weather/weatherInfo?city=荆州")
    Object queryWeather();
}
```
此时的返回结果为：  
```json
[
    {
        "province": "湖北",
        "city": "荆州区",
        "adcode": "421003",
        "weather": "阴",
        "temperature": "23",
        "winddirection": "北",
        "windpower": "≤3",
        "humidity": "87",
        "reporttime": "2023-09-11 23:31:32",
        "temperature_float": "23.0",
        "humidity_float": "87.0"
    },
    {
        "province": "湖北",
        "city": "荆州市",
        "adcode": "421000",
        "weather": "中雨",
        "temperature": "23",
        "winddirection": "西",
        "windpower": "≤3",
        "humidity": "87",
        "reporttime": "2023-09-11 23:31:31",
        "temperature_float": "23.0",
        "humidity_float": "87.0"
    }
]
```

同理，如果只需要`lives`数组的`第一个元素`则加上`@ResultSelect("@resp.lives[0]")`


🥝 **使用`@ExceptionHandle`注解配置异常处理器**  

---

编写自己的异常处理类，将class设置给`@ExceptionHandle`注解的`value`属性上即可生效  

- 编写异常处理类
```java
package com.springboot.testdemo.springboottest.api;

import com.luckyframework.common.Console;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.HttpExceptionHandle;

public class MyExceptionHandle implements HttpExceptionHandle {

    @Override
    public void exceptionHandler(Request request, Exception exception) {
        Console.printlnMulberry("出异常啦老铁！-> {}", exception);
    }
}

```
- 使用`@ExceptionHandle`注解标注HTTP方法并设置异常处理类

```java
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;

@DomainName("#{gaoDeApi}")
public interface GaoDeApi {
    
    /*
        出现异常时将会打印：
        出异常啦老铁！-> com.luckyframework.httpclient.exception.ResponseProcessException: A value for '@resp.lives.不存在的值' does not exist in the response body, and the default value configuration is not checked
     */
    @ExceptionHandle(MyExceptionHandle.class)
    @ResultSelect(key = "@resp.lives.不存在的值")
    @Get("/v3/weather/weatherInfo?city=荆州")
    Object queryWeather();
}

```

🍈 **使用`@RequestAfterHandle`和`@ResponseAfterHandle`来配置`多个`请求处理器和响应处理器**

---

- `@RequestAfterHandle`中配置的请求处理器会在请求封装完成后和请求执行之前被调用，多个请求处理器的优先级由`requestPriority`属性值决定，数值越小优先级越高。
- `@ResponseAfterHandle`中配置的响应处理器会在请求执行完成得到响应结果之后被调用，多个请求处理器的优先级由`responsePriority`属性值决定，数值越小优先级越高。

框架中已经封装好的`@RequestAfterHandle`和`@ResponseAfterHandle`注解有：

1. `@PrintRequestLog`注解： 功能是在控制台中打印请求信息。
2. `@PrintResponseLog`注解: 功能是在控制台中打印响应信息。
3. `@PrintLog`注解: 功能是在控制台中打印请求信息和响应信息
4. `@RequestConditional`注解： 功能是对请求实例进行条件判断，条件满足则继续执行，否则直接异常中断。
5. `@ResponseConditional`注解：功能是对响应实例进行条件判断，条件满足则继续执行，否则直接异常中断。
6. `@HttpConditional`注解：功能是对请求和响应实例进行条件判断，条件满足则继续执行，否则直接异常中断。





