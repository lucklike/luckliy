package com.luckyframework.httpclient.core.ssl;

import com.luckyframework.conversion.TargetField;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

/**
 * 构建证书{@link KeyStore}的关键信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/4 11:09
 */
public class KeyStoreInfo {

    /**
     * 证书算法
     * <pre>
     *     默认值会更具使用的场景不同而不同
     *     1.作为KeyStore时的默认值为：{@link KeyManagerFactory#getDefaultAlgorithm()}
     *     2.作为TrustStore时的默认值为：{@link TrustManagerFactory#getDefaultAlgorithm()}
     * </pre>
     */
    private String algorithm;

    /**
     * cert秘钥
     */
    @TargetField("cert-password")
    private String certPassword;

    /**
     * KeyStore类型，默认值：{@link  KeyStore#getDefaultType()}
     */
    @TargetField("key-store-type")
    private String keyStoreType;

    /**
     * KeyStore公钥文件地址
     */
    @TargetField("key-store-file")
    private String keyStoreFile;

    /**
     * KeyStore秘钥
     */
    @TargetField("key-store-password")
    private String keyStorePassword;

    /**
     * KeyStore
     */
    private KeyStore _keyStore;

    /**
     * 获取证书算法
     * <pre>
     *     默认值会更具使用的场景不同而不同
     *     1.作为KeyStore时的默认值为：{@link KeyManagerFactory#getDefaultAlgorithm()}
     *     2.作为TrustStore时的默认值为：{@link TrustManagerFactory#getDefaultAlgorithm()}
     * </pre>
     *
     * @return 证书算法
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * 设置证书算法
     * <pre>
     *     默认值会更具使用的场景不同而不同
     *     1.作为KeyStore时的默认值为：{@link KeyManagerFactory#getDefaultAlgorithm()}
     *     2.作为TrustStore时的默认值为：{@link TrustManagerFactory#getDefaultAlgorithm()}
     * </pre>
     *
     * @param algorithm 证书算法
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * 获取cert秘钥
     *
     * @return cert秘钥
     */
    public String getCertPassword() {
        return certPassword;
    }

    /**
     * 设置cert秘钥
     *
     * @param certPassword cert秘钥
     */
    public void setCertPassword(String certPassword) {
        this.certPassword = certPassword;
    }

    /**
     * 获取KeyStore类型，默认值：{@link  KeyStore#getDefaultType()}
     *
     * @return KeyStore类型
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }

    /**
     * 设置KeyStore类型，默认值：{@link  KeyStore#getDefaultType()}
     *
     * @param keyStoreType KeyStore类型
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    /**
     * 获取KeyStore公钥文件地址
     *
     * @return KeyStore公钥文件地址
     */
    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    /**
     * 设置KeyStore公钥文件地址
     *
     * @param keyStoreFile KeyStore公钥文件地址
     */
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    /**
     * 获取KeyStore秘钥
     *
     * @return KeyStore秘钥
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * 设置KeyStore秘钥
     *
     * @param keyStorePassword KeyStore秘钥
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * 获取KeyStore实例
     *
     * @return KeyStore实例
     */
    public synchronized KeyStore getKeyStore() {
        if (_keyStore == null) {
            _keyStore = SSLUtils.createKeyStore(this);
        }
        return _keyStore;
    }
}
