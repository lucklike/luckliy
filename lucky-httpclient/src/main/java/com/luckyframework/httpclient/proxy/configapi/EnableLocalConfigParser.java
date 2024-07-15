package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.proxy.configapi.Source.LOCAL_FILE;


/**
 * 无注解化配置注解-提供从本地文件中获取请求配置的功能
 * <pre>
 *   {@code
 *      该注解使用{@link SpELImport}默认导入了{@link EncoderUtils }工具类中的如下方法：
 *      1.base64(String)              -> base64编码函数                    ->   #{#base64('abcdefg')}
 *      2.basicAuth(String, String)   -> basicAuth编码函数                 ->   #{#basicAuth('username', 'password‘)}
 *      3.url(String)                 -> URLEncoder编码(UTF-8)            ->   #{#url('string')}
 *      4.urlCharset(String, String)  -> URLEncoder编码(自定义编码方式)      ->   #{#urlCharset('string', 'UTF-8')}
 *      5.json(Object)                -> JSON序列化函数                     ->   #{#json(object)}
 *      6.xml(Object)                 -> XML序列化函数                      ->   #{#xml(object)}
 *      7.java(Object)                -> Java对象序列化函数                  ->   #{#java(object)}
 *      8.form(Object)                -> form表单序列化函数                  ->   #{#form(object)}
 *      9.protobuf(Object)            -> protobuf序列化函数                 ->   #{#protobuf(object)}
 *      10.md5(Object)                -> md5加密函数，英文小写                ->   #{#md5('abcdefg')}
 *      11.MD5(Object)                -> md5加密函数，英文大写                ->   #{#MD5('abcdefg')}
 *      12.hmacSha256(String, String) -> hmac-sha256算法签名                ->   #{#hmacSha256('sasas', 'Hello world')}
 *
 *      #某个被@EnableConfigurationParser注解标注的Java接口
 *      顶层的key需要与@EnableConfigurationParser注解的prefix属性值一致，如果注解没有配置prefix，则key使用接口的全类名
 *      io.github.lucklike.httpclient.EnvTestApi:
 *        #公共配置可以写在此处
 *        ........
 *        url: ~
 *        method: ~
 *        ........
 *
 *        #接口中对应方法的方法名
 *        httpTest:
 *          #指定请求的URL
 *          url: http://localhost:8080/envApi/{version}/test
 *          #指定当前请求为SSE请求
 *          sse: http://localhost:8080/envApi/sse
 *          #指定请求的HTTP方法
 *          method: POST
 *          #指定请求是否异步，仅对void方法生效
 *          async: true/false
 *          #指定执行改异步请求的线程池名称
 *          async-executor: async-pool-name
 *          #指定连接超时时间
 *          connect-timeout: 10000
 *          #指定读超时时间
 *          read-timeout: 15000
 *          #指定写超时时间
 *          write-timeout: 15000
 *
 *          #使用类型指定执行当前请求的HTTP执行器
 *          http-executor: JDK/HTTP_CLIENT/OK_HTTP
 *
 *          #使用自定义的HTTP执行器
 *          http-executor-config:
 *            #模式一：指定Spring容器中Bean的名称
 *            bean-name: myHttpExecutor
 *
 *            #模式二：使用Class+Scope方式指定
 *            class-name: io.github.lucklike.springboothttp.api.MyHttpExecutor
 *            scope: SINGLETON/PROTOTYPE/METHOD/CLASS/METHOD_CONTEXT
 *
 *          #在SpEL运行时环境中声明变量和函数
 *          spring-el-import:
 *            #声明Root变量，可以直接通过变量名引用
 *            root:
 *              key: value
 *              key2: "#{key}/test"
 *            #声明普通变量，需要通过#变量名引用
 *            val:
 *              var: value
 *              var2: "#{#var}/test"
 *            #导入函数集合，此处导入的类中的静态方法都会被导入到SpEL运行时环境中，使用'#方法名(参数)'的方式进行调用
 *            fun:
 *              - com.luckyframework.httpclient.proxy.configapi.EncoderUtils
 *              - com.luckyframework.httpclient.MyUtils
 *            #导入包，调用其中的类的静态方法或者实例化时则可以省略包名，例如：#{new ArrayList()}、#{T(Arrays).toString()}
 *            pack:
 *              - java.util
 *              - com.luckyframework.httpclient
 *
 *          #指定请求头参数
 *          header:
 *            X-USER-TOKEN: ugy3i978yhiuh7y76t709-0u87y78g76
 *            X-USER-ID: 10000
 *            X-USER-NAME: lucklike
 *
 *          #指定Query参数
 *          query:
 *            _time: 1719985901194
 *            type: "#{p0}"     #取去参数列表的第一个参数值
 *            name: "#{name}"   #取去参数列表中名称为name的参数
 *            array:
 *              - array-test-1
 *              - array-test-2
 *
 *          #指定Form表单参数
 *          form:
 *            id: 1234
 *            userName: Jack
 *            email: jack@qq.com
 *            password: "#{#SM4('${privateKey}')}" #使用SpEL运行时环境中的SM4函数对环境中的${privateKey}进行解密
 *
 *          #指定路径参数，用于填充URL中的{}占位符
 *          path:
 *            version: v4 #URL中的{version}部分最终将会被替换为v4
 *            name: value
 *
 *          #使用multipart/form-data格式的文本参数
 *          multi-data:
 *            name: lucy
 *            sex: 女
 *            age: 25
 *
 *          #使用multipart/form-data格式的文件参数
 *          multi-file:
 *            photo: file:D:/user/image/photo.jpg               #可以是本地文件
 *            idCard-1: http://localhost:8888/idCard/lucky.png  #也可以是网路上的文件
 *            idCard-2: "#{p1}"                                 #取参数列表中的第二个参数来得到文件
 *
 *          #配置代理
 *          proxy:
 *            type: SOCKS         #代理类型，目前支持SOCKS和HTTP两种类型，默认值为HTTP
 *            ip: 192.168.0.111   #代理服务器IP，必填参数，不填整体将不生效
 *            port: 8080          #代理服务器端口，必填参数，不填整体将不生效
 *            username: username  #用户名
 *            password: password  #密码
 *
 *          #用来指定请求体参数
 *          body:
 *            #模式一：自定义请求体格式
 *            mime-type: text/plain
 *            charset: UTF-8
 *            data: >
 *              文本数据，可以是任何形式的文本
 *              111111,222222,333333,444
 *              AAAAAA,BBBBBB,CCCCCC,DDD
 *
 *            #模式二：使用JSON格式请求体
 *            json: >
 *              {
 *                 "id": #{id},
 *                 "username": "#{name}",
 *                 "password": "PA$$W0RD",
 *                 "email": "#{name}_#{id}@qq.com",
 *                 "age": 27
 *              }
 *
 *            json:
 *              - id: "#{id}"
 *                username: "#{name}"
 *                password: PA$$W0RD
 *                email: "#{name}_#{id}@qq.com"
 *                age: 27
 *              - id: "#{id2}"
 *                username: "#{name2}"
 *                password: #{pwd}
 *                email: "#{name}_#{id}@qq.com"
 *                age: 18
 *
 *            #模式三：使用XML格式请求体
 *            xml: >
 *              <User>
 *                <id>#{id}</id>
 *                <username>#{name}</username>
 *                <password>PA$$W0RD</password>
 *                <email>#{name}_#{id}@qq.com</email>
 *                <age>27</age>
 *              </User>
 *
 *            #模式四：使用form-url格式的请求体
 *            form: >
 *              id=#{id}&
 *              username=#{name}&
 *              password=PA$$W0RD&
 *              email=#{name}_#{id}@qq.com&
 *              age=27
 *
 *            #模式五：使用二进制请求体
 *            file: file:D:/user/image/photo.jpg
 *
 *            #模式六：使用Java序列化请求体
 *            java: #{#java(p0)} #使用java函数将参数列表中的第一个参数进行序列化
 *
 *            #模式七：使用Google Protobuf格式的请求体
 *            protobuf: #{#protobuf(p0)}  #使用protobuf函数将参数列表中的第一个参数转化为Google Protobuf格式的请求体
 *
 *          #配置拦截器
 *          interceptor:
 *            #模式一：指定Spring容器中Bean的名称
 *            - bean-name: fanYiGouTokenInterceptor
 *              priority: 1 #优先级，数值越小优先级越高
 *
 *            #模式二：使用Class+Scope方式指定
 *            - class-name: io.github.lucklike.springboothttp.api.FanYiGouApi$TokenInterceptor
 *              scope: SINGLETON/PROTOTYPE/METHOD/CLASS/METHOD_CONTEXT
 *              priority: 2 #优先级，数值越小优先级越高
 *
 *          #配置响应转换器，其中result和exception至少要写一个
 *          resp-convert:
 *            result: "#{$body$.data}"      #响应转换表达式
 *            meta-type: java.lang.Object   #响应转化元数据类型，整个原始响应体对应的Java数据类型
 *            exception: "出异常了老铁，http-status: #{$status$}" #异常表达式，这里可以是一个字符串，可以以是一个异常实例
 *
 *            #条件表达式，其中result和exception至少要写一个
 *            condition:
 *              - assertion: "#{$status$ == 403}" #断言表达式，如果此处返回true则会执行下面的result表达式或者exception表达式
 *                exception: "认证失败，http-status: #{$status$}" #异常表达式，这里可以是一个字符串，可以以是一个异常实例
 *
 *              - assertion: "#{$status$ == 200}"
 *                result: "#{$body$.data}"
 *
 *          #配置SSE请求的监听器
 *          sse-listener:
 *            #模式一：指定Spring容器中Bean的名称
 *            bean-name: myEventListener
 *
 *            #模式二：使用Class+Scope方式指定
 *            class-name: io.github.lucklike.springboothttp.api.sse.MyEventListener
 *            scope: SINGLETON/PROTOTYPE/METHOD/CLASS/METHOD_CONTEXT
 *
 *   }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 21:06
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpELImport(fun = {EncoderUtils.class})
@EnableConfigurationParser(sourceType = LOCAL_FILE)
@Combination({EnableConfigurationParser.class})
public @interface EnableLocalConfigParser {

    @AliasFor(annotation = EnableConfigurationParser.class, attribute = "source")
    String value() default "classpath:/api/#{$class$.getSimpleName()}.yml";

    /**
     * 定义配置前缀，用于唯一定位到一段配置
     */
    String prefix() default "";

    /**
     * 配置源信息，本地文件位置
     * <pre>
     *     classpath:/api/EnvApi.yml
     *     file:/usr/local/config/api/EnvApi.yml
     * </pre>
     */
    /**
     * 配置源信息
     */
    @AliasFor(annotation = EnableConfigurationParser.class, attribute = "source")
    String source() default "classpath:/api/#{$class$.getSimpleName()}.yml";

}
