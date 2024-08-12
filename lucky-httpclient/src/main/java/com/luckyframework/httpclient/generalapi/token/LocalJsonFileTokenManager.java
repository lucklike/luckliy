package com.luckyframework.httpclient.generalapi.token;

import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * 基于本地Json文件存储的Token管理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/18 16:48
 */
public abstract class LocalJsonFileTokenManager<T> extends TokenManager<T> {

    private static final Logger log = LoggerFactory.getLogger(LocalJsonFileTokenManager.class);

    @Override
    protected final void saveToken(T token) {
        try {
            File file = ResourceUtils.getFile(getResourceLocation());
            FileUtils.createSaveFolder(file.getParentFile());
            Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
            FileCopyUtils.copy(JSON_SCHEME.serialization(token), writer);
        } catch (Exception e) {
            throw new LuckyRuntimeException("Failed to save token", e).printException(log);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T getCachedToken() {
        try {
            File file = ResourceUtils.getFile(getResourceLocation());
            if (file.exists()) {
                return (T) JSON_SCHEME.deserialization(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8), getTokenClass());
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get cached token", e);
            return null;
        }
    }

    /**
     * 获取存储Token数据的JSON文件路径
     *
     * @return 存储Token数据的JSON文件路径
     */
    protected abstract String getResourceLocation();

    /**
     * 获取Token对象的类型
     *
     * @return Token对象的类型
     */
    protected abstract Class<T> getTokenClass();
}
