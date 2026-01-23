package com.luckyframework.io;

import com.luckyframework.common.StringUtils;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

/**
 * zip: xxxxxx  !/zippath
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
            return String.format("Zip resource [%s]__[%s]", zipResource.getDescription(), zipFilePath);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            File file = zipResource.getFile();
            ZipFile zipFile = new ZipFile(file);
            return zipFile.getInputStream(zipFile.getEntry(StringUtils.joinUrlPath(StringUtils.stripFilenameExtension(file.getName()), zipFilePath)));

        }
    }
}
