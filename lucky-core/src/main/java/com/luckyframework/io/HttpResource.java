package com.luckyframework.io;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.KeyCaseSensitivityMap;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 可以重命名的资源
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 07:49
 */
public class HttpResource extends UrlResource {

    private Map<String, List<String>> headerMap;

    public HttpResource(URL url) {
        super(url);
    }

    public HttpResource(UrlResource urlResource){
        this(urlResource.getURL());
    }

    @Override
    public String getFilename() {
        String filename = super.getFilename();
        if (getContentType() == null) {
            return filename;
        }
        String fileType = getFileType().toLowerCase();
        int i = filename.lastIndexOf(".");
        if (i == -1) {
            return filename + fileType;
        }
        return filename.substring(0, i) + fileType;
    }

    @Override
    protected void customizeConnection(HttpURLConnection con) throws IOException {
        headerMap = new KeyCaseSensitivityMap<>(con.getHeaderFields());
    }

    public String getContentType() {
        List<String> contentTypeList = headerMap.get("content-type");
        return ContainerUtils.isEmptyCollection(contentTypeList) ? null : contentTypeList.get(0);
    }

    public List<String> getHeader(String name) {
        return headerMap.get(name);
    }

    private String getFileType(){
        for (String content : getContentType().split(";")) {
            int i = content.indexOf("/");
            if(i != -1) {
                return "."+content.substring(i + 1);
            }
        }
        return "";
    }


}
