package com.luckyframework.httpclient.proxy.spel;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数空间
 */
public class MethodSpaceConstant {

    /**
     * 公共函数空间
     */
    public final static String COMMON_FUNCTION_SPACE = "__Fun::Common::__";

    /**
     * 资源相关函数的函数空间
     */
    public final static String RESOURCE_FUNCTION_SPACE = "__Fun::Resource::__";

    /**
     * 编解码相关函数的函数空间
     */
    public final static String SERIALIZATION_FUNCTION_SPACE = "__Fun::Serialization::__";

    /**
     * 随机函数相关的函数空间
     */
    public final static String RANDOM_FUNCTION_SPACE = "__Fun::Random::__";

    /**
     * 消息摘要相关函数的函数空间
     */
    public final static String MESSAGE_DIGEST_FUNCTION_SPACE = "__Fun::MessageDigest::__";

    /**
     * 消息认证相关函数的函数空间
     */
    public final static String CRYPTO_MAC_FUNCTION_SPACE = "__Fun::Crypto:Mac::__";

    /**
     * 加解密相关函数的函数空间
     */
    public final static String CIPHER_FUNCTION_SPACE = "__Fun::Cipher::__";

    /**
     * 外部空间
     */
    public static final List<String> EXTERNAL_SPACES = new ArrayList<>();

    /**
     * 添加一个外部空间
     *
     * @param externalSpace 外部空间
     */
    public static void addExternalSpace(String externalSpace) {
        EXTERNAL_SPACES.add(externalSpace);
    }

    /**
     * 获取所有的函数空间
     *
     * @return 所有的函数空间
     */
    public static List<String> getSpaces() {
        List<String> internalVarName = InternalUtils.getInternalVarName(MethodSpaceConstant.class);
        internalVarName.addAll(EXTERNAL_SPACES);
        return internalVarName;
    }


}
