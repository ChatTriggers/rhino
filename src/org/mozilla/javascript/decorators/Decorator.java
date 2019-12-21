package org.mozilla.javascript.decorators;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public abstract class Decorator extends BaseFunction {
    public static int CLASS   = 1 << 0;
    public static int METHOD  = 1 << 1;
    public static int FIELD   = 1 << 2;
    public static int PUBLIC  = 1 << 3;
    public static int PRIVATE = 1 << 4;
    public static int STATIC  = 1 << 5;
    public static int PREINIT = 1 << 6;

    public static final Object NAME_KEY = new Object();

    public static void init(Scriptable scope) {
        WrapDecorator.init(scope);
        RegisterDecorator.init(scope);
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object res = consume(args[0], (int) args[1], cx, scope, thisObj, (Object[]) args[2]);
        return res;
    }

    public abstract Object consume(Object target, int descriptor, Context cx, Scriptable scope, Scriptable thisObj, Object[] args);
}
