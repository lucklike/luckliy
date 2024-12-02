package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ModifiedVerifier;
import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;

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

        private static final Set<String> INTERNAL_FUNCTION_NAME = ClassStaticElement.create(CommonFunctions.class).getAllStaticMethods().keySet();
        private static final ErrVarModifiedVerifier INSTANCE = new ErrVarModifiedVerifier();

        private ErrVarModifiedVerifier() {
        }

        @Override
        public boolean can(String element) {
            return !INTERNAL_FUNCTION_NAME.contains(element);
        }
    }
}
