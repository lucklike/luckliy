package com.luckyframework.common;

import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加解密工具
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/8 17:31
 */
public class EncryptionUtils {

    public static String base64Encode(String sourceText, Charset charset) {
        return base64Encode(sourceText.getBytes(charset), charset);
    }

    public static String base64Encode(InputStream source, Charset charset) throws IOException {
        return base64Encode(FileCopyUtils.copyToByteArray(source), charset);
    }

    public static String base64Encode(File source, Charset charset) throws IOException {
        return base64Encode(FileCopyUtils.copyToByteArray(source), charset);
    }

    public static String base64Encode(Resource source, Charset charset) throws IOException {
        return base64Encode(source.getInputStream(), charset);
    }

    public static String base64Encode(Reader source, Charset charset) throws IOException {
        return base64Encode(FileCopyUtils.copyToString(source), charset);
    }

    public static String base64Encode(byte[] source, Charset charset) {
        return new String(Base64.getEncoder().encode(source), charset);
    }

    public static String base64Decode(String encryptedText, Charset charset) {
        byte[] decode = Base64.getDecoder().decode(encryptedText.getBytes(charset));
        return new String(decode, charset);
    }

    public static String base64UFT8Encode(String sourceText) {
        return base64Encode(sourceText, StandardCharsets.UTF_8);
    }

    public static String base64UFT8Decode(String encryptedText) {
        return base64Decode(encryptedText, StandardCharsets.UTF_8);
    }

    public static String templateEncode(String template, String sourceTxt) {
        return new TemplateEncryption(template).encode(sourceTxt);
    }

    public static String templateDecode(String template, String encryptedText) {
        return new TemplateEncryption(template).decode(encryptedText);
    }

    public static String piEncode(String sourceTxt) {
        return TemplateEncryption.PI_TEMP_ENC.encode(sourceTxt);
    }

    public static String piDecode(String encryptedText) {
        return TemplateEncryption.PI_TEMP_ENC.decode(encryptedText);
    }

    static class TemplateEncryption {

        public final static TemplateEncryption PI_TEMP_ENC = new TemplateEncryption(String.valueOf(Math.PI));
        private final char[] base;

        public TemplateEncryption(String template) {
            base = base64UFT8Encode(template).toCharArray();
        }

        public String encode(String sourceText) {
            char[] sourceTxtChars = sourceText.toCharArray();
            char[] resultTxtChars = new char[sourceTxtChars.length];
            for (int i = 0; i < sourceTxtChars.length; i++) {
                int baseIndex = i % base.length;
                resultTxtChars[i] = (char) (sourceTxtChars[i] + base[baseIndex]);
            }
            return new String(resultTxtChars);
        }

        public String decode(String encryptedText) {
            char[] txtChars = encryptedText.toCharArray();
            char[] resultChars = new char[txtChars.length];
            for (int i = 0; i < txtChars.length; i++) {
                int baseIndex = i % base.length;
                resultChars[i] = (char) (txtChars[i] - base[baseIndex]);
            }
            return new String(resultChars);
        }
    }
}
