# <center> 🍀lucky-httpclient


## 基本功能

---

### # 安装与使用
- [安装与使用](./docs/simple/annotation-api/01.安装与使用.md)
- [声明HTTP接口](./docs/simple/annotation-api/02.声明HTTP接口.md)
- [定义请求方法](./docs/simple/annotation-api/03.定义请求方法.md)
- [URL的组成](./docs/simple/annotation-api/04.Http协议URL的组成.md)

### # 简单参数设置
- [URL设置](./docs/simple/annotation-api/05.整体URL设置.md)
- [提取公共域名](./docs/simple/annotation-api/06.提取根路径.md)
- [Query参数设置](./docs/simple/annotation-api/07.Query参数设置.md)
- [Header参数设置](./docs/simple/annotation-api/08.Header参数设置.md)
- [Cookie参数设置](./docs/simple/annotation-api/09.Cookie参数设置.md)
- [Ref锚点参数设置](./docs/simple/annotation-api/10.Ref锚点参数设置.md)
- [UserInfo参数设置](./docs/simple/annotation-api/11.UserInfo参数设置.md)
- [简单身份认证](./docs/simple/annotation-api/12.简单身份认证（Basic%20Authentication）.md)

### # 请求体参数设置
- [Form请求体](./docs/simple/annotation-api/13.Body-Form参数设置.md)
- [MultipartFormData请求体](./docs/simple/annotation-api/14.Body-MultipartFormData参数设置（支持文件上传）.md)
- [JSON请求体](./docs/simple/annotation-api/15.Body-Json参数设置.md)
- [XML请求体](./docs/simple/annotation-api/16.Body-Xml参数设置.md)
- [Protobuf请求体](./docs/simple/annotation-api/17.Body-Protobuf参数设置.md)
- [JDK序列化请求体](./docs/simple/annotation-api/18.Body-JDK序列化参数设置.md)
- [二进制请求体](./docs/simple/annotation-api/19.Body-Binary二进制参数设置.md)
- [自定义格式请求体](./docs/simple/annotation-api/20.Body-自定义格式参数设置.md)

### # 异步请求
- [异步模型](./docs/simple/annotation-api/26.0.异步模型.md)
- [线程池配置](./docs/simple/annotation-api/26.1.线程池配置.md)
- [异步请求](./docs/simple/annotation-api/26.异步请求.md)


### # 文件上传
- [multipart/form-data](./docs/simple/annotation-api/14.Body-MultipartFormData参数设置（支持文件上传）.md)
- [application/octet-stream](./docs/simple/annotation-api/19.Body-Binary二进制参数设置.md)
- [异步分片上传](./docs/simple/annotation-api/35.分片文件上传.md)  

### # 文件下载
- [常规下载](./docs/simple/annotation-api/33.常规文件下载.md)
- [异步分片下载](./docs/simple/annotation-api/34.分片文件下载.md)

### # 内容解压缩
- [介绍](./docs/simple/annotation-api/24.1.内容压缩-介绍.md)
- [Gzip解压缩](./docs/simple/annotation-api/24.2.Gzip解压缩.md)
- [Deflate解压缩](./docs/simple/annotation-api/24.3.Deflate解压缩.md)
- [Brotli解压缩](./docs/simple/annotation-api/24.4.Brotli解压缩.md)
- [Zstandard解压缩](./docs/simple/annotation-api/24.5.Zstandard解压缩.md)
- [自定义格式扩展接口](./docs/simple/annotation-api/24.6.自定义解压缩格式扩展.md)

### # 响应API与数据类型转换
- [Response对象](./docs/simple/annotation-api/32.0.Response对象.md)
- [Http接口方法默认支持的返回值类型](./docs/simple/annotation-api/32.1.代理接口返回值处理逻辑.md)
- [自动转换器（Response.AutoConvert）](./docs/simple/annotation-api/32.2.自动转换器-AutoConvert.md)
- [注解转换器（ResponseConvert）](./docs/simple/annotation-api/32.3.注解转换器-ResponseConvert.md)

### # SSE
- [介绍与使用说明](./docs/simple/annotation-api/36.1.SSE介绍与使用说明.md)
- [标准SSE数据格式 -- text/event-stream](./docs/simple/annotation-api/36.2标准SSE数据格式.md)
- [非标SSE数据格式 -- application/x-ndjson](./docs/simple/annotation-api/37.SSE-application-x-ndjson.md)

### # HTTPS
- [介绍](./docs/simple/annotation-api/28.1.HTTPS介绍.md)
- [单向认证](./docs/simple/annotation-api/28.2.HTTPS-单向认证.md)
- [简单双向认证](./docs/simple/annotation-api/28.3.HTTPS-简单双向认证.md)
- [自定义SSL认证](./docs/simple/annotation-api/28.4.HTTPS-自定义验证.md)

### # Mock
- [使用说明](./docs/simple/annotation-api/34.1.常规接口Mock.md)

### # 拦截器与插件
- [拦截器（Interceptor）](docs/simple/annotation-api/38.1拦截器.md)
- [插件（Plugin）](docs/simple/annotation-api/38.2.插件.md)


### # 其他功能
- [超时时间设置](./docs/simple/annotation-api/21.接口超时时间设置.md)
- [代理设置](./docs/simple/annotation-api/22.代理设置.md)
- [自动重定向](./docs/simple/annotation-api/23.自动重定向.md)
- [重试机制](./docs/simple/annotation-api/25.重试机制.md)
- [Cookie管理器](./docs/simple/annotation-api/27.Cookie管理器.md)
- [统一异常处理](./docs/simple/annotation-api/29.统一异常处理.md)
- [日志管理](./docs/simple/annotation-api/30.日志管理.md)
- [执行器设置](./docs/simple/annotation-api/31.HTTP执行设置.md)

## 高级功能

---

### # 架构原理

- [架构](./docs/principle/00.架构.md)
- [执行上下文-Context](./docs/principle/01.执行上下文-Context.md)
- [SpEL](./docs/principle/02.SpEL功能.md)

### # 扩展接口

- [原理篇](./docs/extend/00.元注解与扩展接口.md)
- [URL生成器](./docs/extend/01.URL生成器)
- [静态参数设置](./docs/extend/02.静态参数设置器.md)
- [动态参数设置](./docs/extend/03.动态参数设置器.md)
- [SSL](./docs/extend/04.SSL.md)
- [异常处理机制](./docs/extend/05.异常处理器.md)
- [拦截器机制](./docs/extend/06.拦截器.md)
- [异步执行机制](./docs/extend/07.异步执行器.md)
- [重试机制](./docs/extend/08.重试机制.md)
- [Mock原理](./docs/extend/09.Mock原理.md)
- [响应转换机制](./docs/extend/10.响应结果转换器)
- [Wrapper机制](./docs/extend/11.Wrapper机制)
- [插件机制](./docs/extend/12.插件机制)
- [AI工具调用与MCP](./docs/extend/13.AI工具调用与MCP.md)