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

            NativeProxyRevocableObject obj = new NativeProxyRevocableObject(proxy, revoke);
            
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
