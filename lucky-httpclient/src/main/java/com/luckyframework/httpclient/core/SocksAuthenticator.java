package com.luckyframework.httpclient.core;


import org.springframework.lang.NonNull;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Optional;

/**
 * Socks代理权限校验
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:48
 */
public class SocksAuthenticator extends Authenticator {

    private final ThreadLocal<PasswordAuthentication> passwordAuthThreadLocal = new ThreadLocal<>();

    private static final SocksAuthenticator INSTANCE;

    static {
        INSTANCE = new SocksAuthenticator();
        Authenticator.setDefault(INSTANCE);
    }


    public static SocksAuthenticator getInstance() {
        return INSTANCE;
    }

    public void setPasswordAuthenticator(@NonNull PasswordAuthentication passwordAuthentication) {
        passwordAuthThreadLocal.set(passwordAuthentication);
    }

    public void setPasswordAuthenticator(String username, String password) {
        setPasswordAuthenticator(
                new PasswordAuthentication(
                        username,
                        Optional.of(password).orElse("").toCharArray()
                )
        );
    }

    public void removePasswordAuthenticator() {
        passwordAuthThreadLocal.remove();
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return passwordAuthThreadLocal.get();
    }
}
