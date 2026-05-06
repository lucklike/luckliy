package com.luckyframework.httpclient.generalapi.token;

import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;
import com.luckyframework.reflect.MethodUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.ResolvableType;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于实现 Token 缓存的代理插件
 * <pre>
 *     注：使用该插件的方法返回值类型必须要实现{@link TokenResult}接口
 * </pre>
 */
public final class TokenCacheProxyPlugin implements ProxyPlugin {

    /**
     * 缓存管理器缓存
     */
    private final Map<Method, TokenManager<?>> tokenManagerCache = new ConcurrentHashMap<>();

    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {
        MethodMetaContext mec = decorator.getMeta().getMetaContext();
        ResolvableType returnResolvableType = mec.getMethodConvertReturnResolvableType();

        // 方法返回值类型检查
        Class<?> resolveClass = returnResolvableType.toClass();
        if (!TokenResult.class.isAssignableFrom(resolveClass)) {
            throw new TokenCacheException("TokenCacheProxyPlugin decorate method ['{}'] return type is not 'com.luckyframework.httpclient.generalapi.token.TokenResult'", FontUtil.getYellowUnderline(MethodUtils.getLocation(mec.getCurrentAnnotatedElement())));
        }

        // 获取注解配置信息
        UseTokenManager tokenCacheAnn = mec.getMergedAnnotation(UseTokenManager.class);
        String cacheFilePath = mec.parseExpression(tokenCacheAnn.value(), String.class);

        // 根据是否配置了缓存文件路径来使用对应的缓存管理器
        return createTokenManager(decorator, returnResolvableType, cacheFilePath).getToken();
    }

    /**
     * 创建 Token 管理器
     * <pre>
     *     1.如果缓存中存在则使用缓存中的
     *     2.缓存中不存在
     *        a.配置了cacheFilePath，则构建{@link LocalFileTokenManager}
     *        b.未配置cacheFilePath，则构建{@link TMemoryTokenManager}
     *        c.将构建完成的完成的缓存管理器存入缓存
     * </pre>
     *
     * @param decorator            代理装饰器
     * @param returnResolvableType 方法返回类型
     * @param cacheFilePath        缓存文件路径
     * @return Token 管理器
     */
    private TokenManager<?> createTokenManager(ProxyDecorator decorator, ResolvableType returnResolvableType, String cacheFilePath) {
        Method method = decorator.getMeta().getMethod();
        TokenManager<?> tokenManager = tokenManagerCache.get(method);
        if (tokenManager == null) {
            tokenManager = StringUtils.hasText(cacheFilePath)
                    ? new LocalFileTokenManager(returnResolvableType.getType(), cacheFilePath)
                    : new TMemoryTokenManager();
            tokenManagerCache.put(method, tokenManager);
        }

        if (tokenManager instanceof ProxyDecoratorAware) {
            ((ProxyDecoratorAware) tokenManager).setDecorator(decorator);
        }
        return tokenManager;
    }


    /**
     * ProxyDecoratorAware
     */
    interface ProxyDecoratorAware {
        void setDecorator(ProxyDecorator decorator);
    }

    /**
     * 将 Token 信息缓存到内存的管理器
     */
    static class TMemoryTokenManager extends MemoryTokenManager<TokenResult> implements ProxyDecoratorAware {

        protected ProxyDecorator decorator;

        public void setDecorator(ProxyDecorator decorator) {
            this.decorator = decorator;
        }

        @Override
        protected TokenResult refreshToken(@Nullable TokenResult oldToken) {
            try {
                TokenResult tokenResult = (TokenResult) decorator.proceed();
                tokenResult.postProcess();
                return tokenResult;
            } catch (Throwable e) {
                throw new TokenCacheException(e, "TokenCacheProxyPlugin refreshToken error! decorate method is ['{}']", FontUtil.getYellowUnderline(MethodUtils.getLocation(decorator.getMeta().getMethod())));
            }
        }

        @Override
        protected boolean isExpires(TokenResult token) {
            return token.expires();
        }
    }

    /**
     * 将 Token 信息缓存到本地文件的管理器
     */
    static class LocalFileTokenManager extends JsonFileTokenManager<TokenResult> implements ProxyDecoratorAware {


        private final Type tokenType;
        private final String cacheFilePath;

        private ProxyDecorator decorator;

        public LocalFileTokenManager(Type tokenType, String cacheFilePath) {
            this.tokenType = tokenType;
            this.cacheFilePath = cacheFilePath;
        }

        public void setDecorator(ProxyDecorator decorator) {
            this.decorator = decorator;
        }

        @Override
        protected File getJsonFile() {
            return new File(cacheFilePath);
        }

        @Override
        protected TokenResult refreshToken(@Nullable TokenResult oldToken) {
            try {
                TokenResult tokenResult = (TokenResult) decorator.proceed();
                tokenResult.postProcess();
                return tokenResult;
            } catch (Throwable e) {
                throw new TokenCacheException(e, "TokenCacheProxyPlugin refreshToken error! decorate method is ['{}']", FontUtil.getYellowUnderline(MethodUtils.getLocation(decorator.getMeta().getMethod())));
            }
        }

        @Override
        protected boolean isExpires(TokenResult token) {
            return token.expires();
        }

        @Override
        protected Type getTokenType() {
            return tokenType;
        }
    }

}
