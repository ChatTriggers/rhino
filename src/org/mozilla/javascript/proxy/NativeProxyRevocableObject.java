package org.mozilla.javascript.proxy;

import org.mozilla.javascript.*;

public class NativeProxyRevocableObject extends BaseFunction {
    private static final String PROXY_TAG = "Proxy";

    private final NativeProxy proxy;
    private final Function revoke;

    NativeProxyRevocableObject(NativeProxy proxy, Function revoke) {
        this.proxy = proxy;
        this.revoke = revoke;
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == Id_revoke) {
            initPrototypeMethod(PROXY_TAG, Id_revoke, "revoke", null, 0);
        } else if (id == Id_proxy) {
            initPrototypeMethod(PROXY_TAG, Id_revoke, "revoke", null, proxy.getArity());
        }
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(PROXY_TAG)) {
            super.execIdCall(f, cx, scope, thisObj, args);
        }

        int id = f.methodId();

        switch (id) {
            case Id_proxy:
                return proxy;
            case Id_revoke:
                return revoke;
            default:
                return super.execIdCall(f, cx, scope, thisObj, args);
        }
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2019-11-21 23:30:39 CST
        L0:
        {
            id = 0;
            String X = null;
            int s_length = s.length();
            if (s_length == 5) {
                X = "proxy";
                id = Id_proxy;
            } else if (s_length == 6) {
                X = "revoke";
                id = Id_revoke;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
            Id_revoke = 1,
            Id_proxy = 2;
// #/string_id_map#

}
