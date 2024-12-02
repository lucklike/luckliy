package com.luckyframework.common;

import com.luckyframework.exception.CtrlMapValueModifiedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class VerifierCtrlMap<K, V> extends AbstractCtrlMap<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(VerifierCtrlMap.class);

    private final ModifiedVerifier<K> errVerifier;

    private final ModifiedVerifier<K> ignoreVerifier;


    public VerifierCtrlMap(Map<K, V> delegate, ModifiedVerifier<K> errVerifier, ModifiedVerifier<K> ignoreVerifier) {
        super(delegate);
        this.errVerifier = errVerifier;
        this.ignoreVerifier = ignoreVerifier;
    }

    @Override
    protected boolean canItBeModified(K k) {
        if (!existenceOrNot(k)) {
            return true;
        }

        if (!errVerifier.can(k)) {
            throw new CtrlMapValueModifiedException("Unable to modify a protected Key: '{}'", k);
        }

        boolean can = ignoreVerifier.can(k);

        if (!can && logger.isDebugEnabled()) {
            logger.debug("Ignore this change to Key: '{}'", k);
        }

        return can;
    }

    protected boolean existenceOrNot(K key) {
        return delegate.containsKey(key);
    }
}
