# <center> lucky-httpclient

##  🍀 简介  

---

`lucky-httpclient`是一个简单易用的HTTP客户端工具，提供了`编程式`和`注解式`两种编码方式，并提供了丰富的扩展机制，开发者可以根据自己的需求来定制和扩展

## ⚙️ 安装

---

🪶 Maven  
在项目的`pom.xml`的`dependencies`中加入以下内容:
```xml
    <dependency>
        <groupId>io.github.lucklike</groupId>
        <artifactId>lucky-httpclient</artifactId>
        <version>1.0.2</version>
    </dependency>
```

🐘 Gradle

```groovy
    implementation group: 'io.github.lucklike', name: 'lucky-httpclient', version: '1.0.2'
```

---

## 📃 开发文档

## **一. 编程式开发**

---

编程式开发中主要会涉及到以下三个组件：
1. [Request](./src/main/java/com/luckyframework/httpclient/core/Request.java)  
    请求信息，用于封装http请求信息如：`url`、`method`、`headers`、`query`、`form`、 `body`、`file`等。

2. [Response](./src/main/java/com/luckyframework/httpclient/core/Response.java)
3. [HttpExecutor](./src/main/java/com/luckyframework/httpclient/core/executor/HttpExecutor.java)  

**开发流程如下：**
1. 创建一个用于执行http请求的执行器`HttpExecutor`
2. 创建一个具体的请求对象`Request`
3. 使用执行器的`execute()`方法执行请求，并得到一个响应`Response`
4. 根据业务需求处理响应结果

架构图：  
![架构图](./doc-resources/httpclient-d.jpg)

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
    public void process(int status, HttpHeaderManager header, InputStreamFactory factory) {
        try {
            OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get("D:/CentOS-8.5.2111-x86_64-dvd1.iso")));
            FileCopyUtils.copy(factory.getInputStream(), out);
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
            .addFiles("file", new File("/Users/fukang/github-poject/luckliy_v4/LUCKY_V4_TEST/springboot-test/pom.xml"))
            // 添加InputStream
            .addInputStream("file2", "HELP.md", Files.newInputStream(Paths.get("/Users/fukang/github-poject/luckliy_v4/LUCKY_V4_TEST/springboot-test/HELP.md")))
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
`注解开发`是在`编程式开发`的基础上又做了一层封装，又进一步的简化了开发。注解开发模式下我们只需要`声明一个接口`，然后使用`特定的注解`进行相关的描述即可，  
lucky-httpclient底层会使用`动态代理`机制帮我们生成代理对象，通过代理对象便可以完成所有的http请求。

- [HttpClientProxyObjectFactory](./src/main/java/com/luckyframework/httpclient/proxy/HttpClientProxyObjectFactory.java)

🍓 **使用`@HttpRequest`系注解标注请求的`method`和`url`**  
````java
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpRequest {

    /**
     * 定义http请求的Url信息
     */
    @AliasFor("url")
    String value() default "";

    /**
     * 定义http请求的Url信息
     */
    @AliasFor("value")
    String url() default "";

    /**
     * 定义http请求的Method
     */
    RequestMethod method();

}
````

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

    // 获取百度首页
    @Get("https://www.baidu.com")
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

开发中建议将`同一个域名`或者`同一域名中某个特定的模块`下的Http接口组织到`同一个Java接口`，这样便可以使用 **`@DomainName`** 注解来提取公共域名，方便统一管理。例如：上面的接口加上 **`@DomainName`** 注解  
之后便可以简化为如下代码：

```java
package com.springboot.testdemo.springboottest.api;
import com.luckyframework.httpclient.proxy.annotations.Delete;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;

// 直接配置域名
@DomainName("http://localhost:8080/book/")

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
🍎 **使用`@DynamicParam`系列注解对方法或者方法参数进行标注**

| 注解                  | 请求参数                          | Request方法           |
|---------------------|-------------------------------|---------------------|
| `@Url`              | 设置URL                         | setUrlTemplate()    |
| `@QueryParam`       | 设置URL参数                       | addQueryParameter() |
| `@PathParam`        | 填充URL占位符的参数                   | addPathParameter()  |
| `@URLEncoderQuery`  | 设置URL参数（自动UrlEncoder编码）       | addQueryParameter() |
| `@URLEncoderPath`   | 填充URL占位符的参数（自动UrlEncoder编码）   | addPathParameter()  |
| `@FormParam`        | 表单参数                          | addFormParameter()  |
| `@HeaderParam`      | 请求头参数                         | addHeader()         |
| `@CookieParam`      | 设置Cookie信息                    | addCookie()         |
| `@ResourceParam`    | 设置文件参数                        | addResources()      |
| `@InputStreamParam` | 设置文件参数(InputStream方式)         | addHttpFiles()      |
| `@BodyParam`        | 设置请求体参数                       | setBody()           |
| `@JsonBody`         | 设置JSON格式的请求体参数（自动序列化为JSON字符串） | setBody()           |
| `@XmlBody`          | 设置XML格式的请求体参数（自动序列化为XML字符串）   | setBody()           |

<font color='red'>注：</font>：遇到下面这些特殊类型时`@DynamicParam`注解不会生效：
1. 当方法参数为`ResponseProcessor`类型时，不做任何设置。
2. 当方法参数为`File`、`Resource`、`MultipartFile`、`HttpFile`类型或者为`这些类型的数组`或`集合`时，会使用`addHttpFiles()`进行参数设置。
3. 当方法参数为`BodyObject`类型时，会使用`setBody()`方法进行参数设置。

**_如果方法或者方法参数上没有标注任何`@DynamicParam`注解时，则默认使用`addQueryParameter()`方法进行参数设置。_**
```java

```

