package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.Resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

/**
 * 无注解化配置注解-提供从本地文件中获取请求配置的功能<br/>
 * 某个被@EnableConfigurationParser注解标注的Java接口<br/>
 * 顶层的key需要与@EnableConfigurationParser注解的prefix属性值一致，如果注解没有配置prefix，则key使用接口的全类名<br/>
 *
 * <b>内置函数：</b><br/>
 * 该注解使用{@link SpELImport}默认导入了{@link CommonFunctions }工具类中的如下方法：
 *
 * <table>
 *     <tr>
 *         <th>函数签名</th>
 *         <th>函数描述</th>
 *         <th>示例</th>
 *     </tr>
 *     <tr>
 *         <td>{@link String} base64({@link Object})</td>
 *         <td>base64编码函数</td>
 *         <td>#{#base64('abcdefg')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} _base64(Object)</td>
 *         <td>base64解码函数</td>
 *         <td>#{#_base64('YWJjZGVmZw==')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} basicAuth(String, String)</td>
 *         <td>basicAuth编码函数</td>
 *         <td>#{#basicAuth('username', 'password‘)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} url(String, String...)</td>
 *         <td>URLEncoder编码</td>
 *         <td>#{#url('hello world!')} 或者 #{#url('hello world!', 'UTF-8')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} _url(String, String...)</td>
 *         <td>URLEncoder解码</td>
 *         <td>#{#_url('a23cb5') 或者 #{#_url('a23cb5', 'UTF-8')</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} json(Object)</td>
 *         <td>JSON序列化函数</td>
 *         <td>#{#json(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} xml(Object)</td>
 *         <td>XML序列化函数</td>
 *         <td>#{#xml(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} java(Object)</td>
 *         <td>Java对象序列化函数</td>
 *         <td>#{#java(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} form(Object)</td>
 *         <td>form表单序列化函数</td>
 *         <td>#{#form(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} protobuf(Object)</td>
 *         <td>protobuf序列化函数</td>
 *         <td>#{#protobuf(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} md5(Object)</td>
 *         <td>md5加密函数，英文小写</td>
 *         <td>#{#md5('abcdefg')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} MD5(Object)</td>
 *         <td>md5加密函数，英文大写</td>
 *         <td>#{#MD5('abcdefg')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} sha256(String, String)</td>
 *         <td>hmac-sha256算法签名</td>
 *         <td>#{#sha256('sasas', 'Hello world')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} uuid()</td>
 *         <td>生成UUID函数，英文小写</td>
 *         <td>#{#uuid()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} UUID()</td>
 *         <td>生成UUID函数，英文大写</td>
 *         <td>#{#UUID()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} nanoid(int...)</td>
 *         <td>生成nanoid函数</td>
 *         <td>#{#nanoid()} 或者 #{#nanoid(4)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Integer int} random(int, int)</td>
 *         <td>生成指定范围内的随机数</td>
 *         <td>#{#random(1, 100)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Integer int} randomMax(int)</td>
 *         <td>生成指定范围内的随机数，最大值为指定值</td>
 *         <td> #{#randomMax(100)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Long} time()</td>
 *         <td>获取当前时间毫秒(13位时间戳)</td>
 *         <td>#{#time()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Long} timeSec()</td>
 *         <td>获取当前时间秒(10位时间戳)</td>
 *         <td>#{#timeSec()}</td>
 *     </tr>
 *     <tr>
 *         <td{@link Date} >date()</td>
 *         <td>获取当前日期({@link Date })</td>
 *         <td>#{#date()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} formatDate(Date, String)</td>
 *         <td>时间格式化</td>
 *         <td>#{#formatDate(#date(), 'yyyy-MM-dd HH:mm:ss')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMddHHmmssDate(Date)</td>
 *         <td>时间格式化(yyyy-MM-dd HH:mm:ss)</td>
 *         <td>#{#yyyyMMddHHmmssDate(#date())}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMddDate(Date)</td>
 *         <td>时间格式化(yyyyMMdd)</td>
 *         <td>#{#yyyyMMddDate(#date())}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} format(String)</td>
 *         <td>格式化当前时间</td>
 *         <td>#{#format('yyyy-MM-dd HH:mm:ss')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMddHHmmss()</td>
 *         <td>格式化当前时间(yyyy-MM-dd HH:mm:ss)</td>
 *         <td>#{#yyyyMMddHHmmss()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMdd()</td>
 *         <td>格式化当前时间(yyyyMMdd)</td>
 *         <td>#{#yyyyMMdd()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Resource} resource(String)</td>
 *         <td>资源加载函数，返回{@link Resource }对象</td>
 *         <td>#{#resource('classpath:application.yml')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Resource Resource[]}resources(String...)</td>
 *         <td>资源加载函数，返回{@link Resource }数组对象</td>
 *         <td>#{#resources('classpath:application.yml', 'classpath:application.properties')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} read(Object, String...)</td>
 *         <td>获取文件对象内容的函数</td>
 *         <td>#{#read('classpath:test.json') 或者 #{#read(#resource('http://lucklike.io/test.xml'))}}</td>
 *     </tr>
 * </table>
 *
 * <b>配置内容：</b><br/>
 * <pre>
 *   {@code
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
 *          #重定向配置
 *          redirect:
 *            #开启自动重定向功能，默认：false
 *            enable: true
 *            #需要重定向的状态码，默认：301, 302, 303, 304, 307, 308
 *            status: [301, 302, 303, 304, 307, 308]
 *            #允许最大重定向次数，默认：5
 *            max-count: 5
 *            #重定向条件表达式
 *            condition: "#{$status$ == 301}"
 *            #获取重定向地址的表达式，默认：#{$respHeader$.Location}
 *            location: "#{$respHeader$.Location}"
 *            #重定向拦截器的优先级，默认：100
 *            priority: 100
 *
 *          #日志配置
 *          logger:
 *            #是否开启日志功能，默认关闭
 *            enable: true
 *            #是否打印请求日志：默认开启（仅在enable为true时生效）
 *            enable-req-log: true
 *            #是否打印响应日志：默认开启（仅在enable为true时生效）
 *            enable-resp-log: true
 *            #是否开启打印注解信息功能，默认关闭
 *            enable-annotation-log: true
 *            #是否开启打印参数信息功能，默认关闭
 *            enable-args-log: true
 *            #是否强制打印响应体信息
 *            force-print-body: false
 *            #日志打印拦截器的优先级，默认2147483647
 *            priority: 2147483647
 *            #MimeType为这些类型时，将打印响应体日志（覆盖默认值）
 *            #默认值：
 *            #application/json
 *            #application/xml
 *            #application/x-java-serialized-object
 *            #text/xml
 *            #text/plain
 *            #text/html
 *            set-allow-mime-types:
 *              - application/json
 *              - application/xml
 *              - application/x-java-serialized-object
 *              - text/xml
 *              - text/plain
 *              - text/html
 *            #MimeType为这些类型时，将打印响应体日志（在默认值的基础上新增）
 *            #默认值：
 *            #application/json
 *            #application/xml
 *            #application/x-java-serialized-object
 *            #text/xml
 *            #text/plain
 *            #text/html
 *            add-allow-mime-types:
 *              - text/plain
 *              - text/html
 *            #响应体超过该值时，将不会打印响应体日志，值小于等于0时表示没有限制,单位：字节默认值：-1
 *            body-max-length: 100
 *            #打印请求日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
 *            req-log-condition: "#{$status$ != 200}"
 *            #打印响应日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
 *            resp-log-condition: "#{$status$ != 200}"
 *
 *          retry:
 *            #是否开启重试的开关
 *            enable: true
 *            #任务名称，默认值为{@link MethodContext#getSimpleSignature}
 *            task-name: bilibili-index
 *            #最大重试次数，默认3次
 *            max-count: 5
 *            #重试等待时间，单位：毫秒，默认值1000
 *            wait-millis: 2000
 *            #下一次等待时间与上一次等待时间的比值，默认值：0
 *            multiplier: 2
 *            #最大等待时间，单位：毫秒，默认值：10000
 *            max-wait-millis: 20000
 *            #最小等待时间，单位：毫秒，默认值：500
 *            min-wait-millis: 600
 *            #需要重试的异常列表，默认值为Exception
 *            exception:
 *              - java.lang.Exception
 *            #不需要重试的异常列表，默认值为null
 *            exclude:
 *              - java.lang.RuntimeException
 *            #需要进行重试的HTTP状态码，默认值为null
 *            exception-status: [404, 405, 406, 500]
 *            #不需要进行重试的HTTP状态码，默认值为null
 *            normal-status: [202, 301]
 *            #决定是否需要重试的表达式
 *            expression: "#{$status$ != 200}"
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
 *            #声明Root字面量，不会进行SpEL解析
 *            root-lit:
 *              key1: value1
 *              key2: value2
 *            #声明普通字面量，不会进行SpEL解析
 *            var-lit:
 *              var1: value1
 *              var2: value2
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
 *          #禁用拦截器配置,在此处配置的拦截器逻辑将不会执行，注意：此处配置的是Interceptor接口实现类中uniqueIdentification()方法的返回值
 *          interceptor-prohibit:
 *            - Interceptor#uniqueIdentification()
 *            - com.luckyframework.httpclient.proxy.interceptor.PrintLogInterceptor
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
 *          #是否禁止使用响应转换器，如果禁止则直接将响应体转化为方法返回值
 *          convert-prohibit: false
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
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 21:06
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@InterceptorRegister(
        intercept = @ObjectGenerate(clazz = ConfigurationApiFunctionalSupport.class, scope = Scope.CLASS),
        priority = PriorityConstant.CONFIG_API_PRIORITY
)
@ResultConvert(
        convert = @ObjectGenerate(clazz = ConfigurationApiFunctionalSupport.class, scope = Scope.CLASS))
@StaticParam(
        resolver = @ObjectGenerate(clazz = ConfigurationApiFunctionalSupport.class, scope = Scope.CLASS),
        setter = @ObjectGenerate(ConfigApiParameterSetter.class)
)
@HttpRequest
@SpELImport(fun = {CommonFunctions.class})
@Combination({StaticParam.class, InterceptorRegister.class, SpELImport.class})
public @interface EnableConfigurationParser {

    /**
     * 定义配置前缀，用于唯一定位到一个具体的配置，不配置时会默认使用当前接口的全类名
     */
    String prefix() default "";

    /**
     * 配置源信息，用于指定一个配置源信息，例如一个本地文件
     */
    String source() default "";

    /**
     * 配置源类型，用于唯一确定一个{@link ConfigurationSource}
     * @see ConfigurationApiFunctionalSupport
     */
    String sourceType() default "";

    /**
     * 拦截器优先级，数值越高优先级越低
     */
    @AliasFor(annotation = InterceptorRegister.class, attribute = "priority")
    int priority() default PriorityConstant.CONFIG_API_PRIORITY;
}
