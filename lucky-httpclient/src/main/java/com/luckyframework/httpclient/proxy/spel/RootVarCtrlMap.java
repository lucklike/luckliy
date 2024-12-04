package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ModifiedVerifier;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.Set;

public class RootVarCtrlMap extends ContextCtrlMap {

    public RootVarCtrlMap() {
        this(null);
    }

    public RootVarCtrlMap(Context context) {
        super(context, ErrRootVarModifiedVerifier.INSTANCE, k -> !ProhibitCoverEnum.isMatch(k));
    }

    @Override
    protected boolean contextHasKey(Context context, String key) {
        if (context == null) {
            return false;
        }
        if (context.getContextVar().getRoot().containsKey(key)) {
            return true;
        }
        if (context instanceof MethodContext && ((MethodContext) context).getMetaContext().getContextVar().getRoot().containsKey(key)) {
            return true;
        }
        return context instanceof ClassContext && context.getHttpProxyFactory().getGlobalSpELVar().getRoot().containsKey(key);
    }

    static class ErrRootVarModifiedVerifier implements ModifiedVerifier<String> {
        private static final Set<String> INTERNAL_PARAM_NAME = InternalRootVarName.getAllInternalRootVarName();
        private static final ErrRootVarModifiedVerifier INSTANCE = new ErrRootVarModifiedVerifier();

        private ErrRootVarModifiedVerifier() {
        }

        @Override
        public boolean can(String element) {
            return !INTERNAL_PARAM_NAME.contains(element);
        }
    }
}
