package org.mozilla.javascript.proxy;

import org.mozilla.javascript.*;

public class NativeProxyCtor extends BaseFunction {
    private static final String PROXY_TAG = "Proxy";

    public NativeProxyCtor() {
        addIdFunctionProperty(this, PROXY_TAG, ConstructorId_revocable, "revocable", 2);
    }

    @Override
    public String getFunctionName() {
        return PROXY_TAG;
    }

    @Override
    public int getLength() {
        return 2;
    }

    @Override
    public int getArity() {
        return 2;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        throw ScriptRuntime.typeError0("msg.proxy.no.new");
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(PROXY_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        int id = f.methodId();

        if (id == ConstructorId_revocable) {
            if (args.length != 2) {
                throw ScriptRuntime.typeError0("msg.proxy.revocable.two.args");
            } else if (!(args[0] instanceof ScriptableObject) || !(args[1] instanceof ScriptableObject)) {
                throw ScriptRuntime.typeError0("msg.proxy.revocable.arg.types");
            }

            NativeProxy proxy = new NativeProxy((ScriptableObject) args[0], (ScriptableObject) args[1]);

            Function revoke = new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    proxy.revoke();
                    return null;
                }
            };

            NativeObject obj = new NativeObject() {
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
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==5) { X="proxy";id=Id_proxy; }
            else if (s_length==6) { X="revoke";id=Id_revoke; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
                    return id;
                }

                private static final int
                        Id_revoke = 1,
                        Id_proxy = 2;
// #/string_id_map#

            };
            
            ScriptableObject.putProperty(obj, "proxy", proxy);
            ScriptableObject.putProperty(obj, "revoke", revoke);

            return obj;
        }

        throw Kit.codeBug();
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        if (args.length == 0) {
            throw ScriptRuntime.typeError("Proxy constructor requires a target object");
        } else if (!(args[0] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.not.obj", ScriptRuntime.toString(args[0]));
        } else if (args.length < 2 || !(args[1] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.not.obj", args.length > 2 ? ScriptRuntime.toString(args[1]) : "undefined");
        }

        return new NativeProxy((ScriptableObject) args[0], (ScriptableObject) args[1]);
    }

    @Override
    protected String getInstanceIdName(int id) {
        return super.getInstanceIdName(id);
    }

    private static final int
            ConstructorId_revocable = -1;
}
