package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Namespace;

import static com.luckyframework.common.EncryptionUtils.templateDecode;
import static com.luckyframework.common.EncryptionUtils.templateEncode;
import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.CIPHER_FUNCTION_SPACE;

/**
 * 加解密相关的函数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/4 03:25
 */

@Namespace(CIPHER_FUNCTION_SPACE)
public class CipherFunctions {

    /**
     * 模板算法加密
     *
     * @param sourceTxt 原字符串
     * @param template  模板字符串
     * @return 加密后的字符串
     */
    @FunctionAlias("temp_encode")
    public static String tempEncode(String sourceTxt, String template) {
        return templateEncode(template, sourceTxt);
    }

    /**
     * 模板算法解密
     *
     * @param encodeTxt 加密后的字符
     * @param template  模板字符串
     * @return 解密后的字符
     */
    @FunctionAlias("temp_decode")
    public static String tempDecode(String encodeTxt, String template) {
        return templateDecode(template, encodeTxt);
    }

}
