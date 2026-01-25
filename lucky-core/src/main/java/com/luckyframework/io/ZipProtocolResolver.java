package com.luckyframework.io;

import com.luckyframework.common.StringUtils;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * zip: xxxxxx  !/zippath
 * <pre>
 * eg:
 * jar:file:/Users/fukang/Lucky/lucky-httpclient-example/lucky-client/src/main/resources/lucky-client-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes/application.yml
 * zip:classpath:param-temp/param-temp.zip!/SpeechCreate.json
 * </pre>
 */
public class ZipProtocolResolver implements ProtocolResolver {


    @Nullable
    @Override
    public Resource resolve(String location, @NonNull ResourceLoader resourceLoader) {
        String ZIP_PROTOCOL = "zip:", PROTOCOL_SUFFIX = "!/";

        // 不是以zip:开头的不处理
        if (!location.startsWith(ZIP_PROTOCOL)) {
            return null;
        }

        int i = location.indexOf(PROTOCOL_SUFFIX);

        // 不存在!/的不处理
        if (i == -1) {
            return null;
        }
        String sourceLocation = location.substring(ZIP_PROTOCOL.length(), i);
        String zipFilePath = location.substring(i + PROTOCOL_SUFFIX.length());

        return new ZipResource(resourceLoader.getResource(sourceLocation), zipFilePath);
    }

    static class ZipResource extends AbstractResource {
        private final Resource zipResource;
        private final String zipFilePath;

        ZipResource(Resource zipResource, String zipFilePath) {
            this.zipResource = zipResource;
            this.zipFilePath = zipFilePath;
        }

        @Override
        public String getDescription() {
            return String.format("Zip resource [%s]@[%s]", zipResource.getDescription(), zipFilePath);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            ZipFile zipFile = new ZipFile(zipResource.getFile());
            ZipEntry zipEntry = zipFile.getEntry(getZipEntryPath());
            if (zipEntry == null) {
                throw new FileNotFoundException(getDescription());
            }
            InputStream zipFileInputStream = zipFile.getInputStream(zipEntry);
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return zipFileInputStream.read();
                }

                @Override
                public void close() throws IOException {
                    zipFileInputStream.close();
                    zipFile.close();
                }
            };

        }


        /**
         * 获取 Zip 内部文件的访问路径
         *
         * @return Zip 内部文件的访问路径
         * @throws IOException 可能会出现 IO 异常
         */
        private String getZipEntryPath() throws IOException {
            File file = zipResource.getFile();
            String ext = StringUtils.getFilenameExtension(file.getName());
            if ("zip".equalsIgnoreCase(ext)) {
                return StringUtils.joinUrlPath(StringUtils.stripFilenameExtension(file.getName()), zipFilePath);
            }
            return zipFilePath;
        }
    }
}
