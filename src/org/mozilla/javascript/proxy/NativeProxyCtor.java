package org.mozilla.javascript.proxy;

import org.mozilla.javascript.*;

public class NativeProxyCtor extends BaseFunction {
    @Override
    public String getFunctionName() {
        return "Proxy";
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
}
