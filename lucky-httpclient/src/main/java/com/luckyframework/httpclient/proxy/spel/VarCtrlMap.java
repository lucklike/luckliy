package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ModifiedVerifier;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.HashSet;
import java.util.Set;

public class VarCtrlMap extends ContextCtrlMap {

    public VarCtrlMap() {
        this(null);
    }

    public VarCtrlMap(Context context) {
        super(context, ErrVarModifiedVerifier.INSTANCE, k -> !ProhibitCoverEnum.isMatch(k));
    }

    @Override
    protected boolean contextHasKey(Context context, String key) {
        if (context == null) {
            return false;
        }
        if (context.getContextVar().getVar().containsKey(key)) {
            return true;
        }
        if (context instanceof MethodContext && ((MethodContext) context).getMetaContext().getContextVar().getVar().containsKey(key)) {
            return true;
        }
        return context instanceof ClassContext && context.getHttpProxyFactory().getGlobalSpELVar().getVar().containsKey(key);
    }

    static class ErrVarModifiedVerifier implements ModifiedVerifier<String> {

        private static final ErrVarModifiedVerifier INSTANCE = new ErrVarModifiedVerifier();

        private static final Set<String> PROTECTED_PARAM_NAME;
        static {
            PROTECTED_PARAM_NAME = new HashSet<>();
            PROTECTED_PARAM_NAME.addAll(InternalUtils.getInternalVarName(InternalVarName.class));
            PROTECTED_PARAM_NAME.addAll(InternalUtils.getInternalVarName(MethodSpaceConstant.class));
        }

        private ErrVarModifiedVerifier() {
        }

        @Override
        public boolean can(String element) {
            return !PROTECTED_PARAM_NAME.contains(element);
        }
    }
}
