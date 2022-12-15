package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.*;
import com.luckyframework.httpclient.core.impl.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.exception.NotFindRequestException;
import com.luckyframework.io.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 基本的基于JDK的Http客户端实现的执行器
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 3:08 下午
 */
public class JdkHttpExecutor implements HttpExecutor {

    private final String end = "\r\n";
    private final String twoHyphens = "--";
    private final String boundary = "*****";


    @Override
    public void execute(Request request, ResponseProcessor processor) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(request.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connectionConfigSetting(connection,request);
            connectionHeaderSetting(connection,request.getHeaderManager());
            connectionParamsSetting(connection,request);
            connection.connect();
            int code = connection.getResponseCode();
            HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                String name = entry.getKey();
                if(name==null) continue;
                List<String> valueList = entry.getValue();
                for (String value : valueList) {
                    httpHeaderManager.putHeader(name,value);
                }
            }
            processor.process(code,httpHeaderManager,connection.getInputStream());
        }finally {
            if(connection != null){
                connection.disconnect();
            }
        }
    }


    /***
     * 连接设置
     * @param connection HTTP连接
     */
    protected void connectionConfigSetting(HttpURLConnection connection,Request request) {
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setConnectTimeout(Request.DEF_CONNECTION_TIME_OUT);
        connection.setReadTimeout(Request.DEF_READ_TIME_OUT);
        Integer connectTimeout = request.getConnectTimeout();
        Integer socketTimeout = request.getReadTimeout();
        if(connectTimeout != null){
            connection.setConnectTimeout(connectTimeout);
        }
        if(socketTimeout != null){
            connection.setReadTimeout(socketTimeout);
        }

    }

    /**
     * 请求头设置
     * @param connection Http连接
     * @param requestHeader 请求头
     */
    protected void connectionHeaderSetting(HttpURLConnection connection, HttpHeaderManager requestHeader) {
        Map<String, List<Header>> headerMap = requestHeader.getHeaderMap();
        for (Map.Entry<String, List<Header>> entry : headerMap.entrySet()) {
            String name = entry.getKey();
            List<Header> valueList = entry.getValue();
            for (Header header : valueList) {
                Object headerValue = header.getValue();
                if(headerValue != null){
                    connection.setRequestProperty(name,headerValue.toString());
                }
            }
        }
    }

    /**
     * 请求参数设置以及请求类型确认
     * @param connection Http连接
     * @param request 请求信息
     * @throws IOException
     */
    protected void connectionParamsSetting(HttpURLConnection connection, Request request) throws IOException {
        switch (request.getRequestMethod()){
            case GET        :  getSetting(request,connection) ;    break;
            case POST       :  postSetting(request,connection);    break;
            case DELETE     :  deleteSetting(request,connection);  break;
            case PUT        :  putSetting(request,connection);     break;
            case OPTIONS    :  optionsSetting(request,connection); break;
            case HEAD       :  headSetting(request,connection);    break;
            case TRACE      :  traceSetting(request,connection);   break;
            default         : throw new NotFindRequestException("Jdk HttpURLConnection does not support requests of type ['"+request.getRequestMethod()+"'].");
        }
    }

    /**
     * 设置为[GET]请求，并添加请求参数
     * @param request 请求信息
     * @param connection Http连接
     * @throws ProtocolException
     */
    private void getSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("GET");
    }

    /**
     * 设置为[POST]请求，并添加请求参数
     * @param request 请求信息
     * @param connection Http连接
     * @throws IOException
     */
    private void postSetting(Request request, HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("POST");
        setRequestParameters(connection, request);
    }

    /**
     * 设置为[DELETE]请求，并添加请求参数
     * @param request 请求信息
     * @param connection Http连接
     * @throws ProtocolException
     */
    private void deleteSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("DELETE");

    }

    /**
     * 设置为[PUT]请求，并添加请求参数
     * @param request 请求信息
     * @param connection Http连接
     * @throws IOException
     */
    private void putSetting(Request request, HttpURLConnection connection) throws IOException{
        connection.setRequestMethod("PUT");
        setRequestParameters(connection, request);
    }

    /**
     * 设置为[OPTIONS]请求，并添加请求参数
     * @param request 请求信息
     * @param connection Http连接
     * @throws ProtocolException
     */
    private void optionsSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("OPTIONS");
    }

    /**
     * 设置为[HEAD]请求，并添加请求参数
     * @param request 请求信息
     * @param connection Http连接
     * @throws ProtocolException
     */
    private void headSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("HEAD");
    }

    /**
     * 设置为[TRACE]请求，并添加请求参数
     * @param request 请求信息
     * @param connection Http连接
     * @throws ProtocolException
     */
    private void traceSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("TRACE");
    }

    /**
     * 设置具体的请求参数
     * @param connection Http连接
     * @param request 请求信息
     * @throws IOException
     */
    private void setRequestParameters( HttpURLConnection connection,Request request) throws IOException {
        RequestParameter requestParameter = request.getRequestParameter();
        BodyObject body = requestParameter.getBody();
        Map<String, Object> nameValuesMap = requestParameter.getRequestParameters();
        //如果设置了Body参数，则优先使用Body参数
        if(body!=null){
            connection.setRequestProperty(HttpHeaders.CONTENT_TYPE,body.getContentType().toString());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write(body.getBody());
            writer.flush();
            writer.close();
            return;
        }


        if(ContainerUtils.isEmptyMap(nameValuesMap)){
           return;
        }

        //如果Body参数为null，而表单参数不为null，则设置表单参数
        connection.setRequestProperty("Content-Type", "multipart/form-data;charset=utf-8;boundary=" + boundary);
        DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
        for (Map.Entry<String, Object> e : requestParameter.getRequestParameters().entrySet()) {
            Class<?> paramValueClass = e.getValue().getClass();
            //包装File类型的参数
            if (File.class == paramValueClass) {
                File file = (File) e.getValue();
                FileInputStream inputStream = new FileInputStream(file);
                writerData(ds,e.getKey(), file.getName(), inputStream);
            }
            //包装File[]类型的参数
            else if (File[].class == paramValueClass) {
                File[] files = (File[]) e.getValue();
                for (File file : files) {
                    FileInputStream inputStream = new FileInputStream(file);
                    writerData(ds,e.getKey(), file.getName(), inputStream);
                }
            }
            //包装MultipartFile类型的参数
            else if (MultipartFile.class == paramValueClass) {
                MultipartFile mf = (MultipartFile) e.getValue();
                writerData(ds,e.getKey(), mf.getFileName(), mf.getInputStream());
            }
            //包装MultipartFile[]类型的参数
            else if (MultipartFile[].class == paramValueClass) {
                MultipartFile[] mfs = (MultipartFile[]) e.getValue();
                for (MultipartFile mf : mfs) {
                    writerData(ds,e.getKey(), mf.getFileName(), mf.getInputStream());
                }
            }
            //其他类型将会被当做String类型的参数
            else {
                ds.writeBytes(twoHyphens + boundary + end);
                ds.writeBytes("Content-Disposition: form-data; " + "name=\""+e.getKey()+"\""+end+end);
                ds.write(e.getValue().toString().getBytes(StandardCharsets.UTF_8));
                ds.writeBytes(end);
            }
        }
        ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
        ds.flush();
        ds.close();

    }

    /**
     * 将具体的文件参数写入请求体中
     * @param ds 数据输出流
     * @param name 参数名
     * @param fileName 文件名
     * @param inputStream 文件的输入流
     * @throws IOException
     */
    private void writerData(DataOutputStream ds,String name,String fileName,InputStream inputStream) throws IOException {
        ds.writeBytes(twoHyphens + boundary + end);
        ds.writeBytes("Content-Disposition: form-data; " + "name=\""+name+"\"; filename=\"" + fileName +"\"" + end);
        ds.writeBytes(end);

        int bufferSize = 1024*4;
        byte[] buffer = new byte[bufferSize];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            ds.write(buffer, 0, length);
        }
        ds.writeBytes(end);
        inputStream.close();
    }
}
