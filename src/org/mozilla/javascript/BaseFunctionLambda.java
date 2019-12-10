package org.mozilla.javascript;

@FunctionalInterface
public interface BaseFunctionLambda {
    Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args);
}
