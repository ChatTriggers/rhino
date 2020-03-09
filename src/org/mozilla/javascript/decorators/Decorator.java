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

    // Used to associate a class member's name
    public static final Object NAME_KEY = new Object();

    // Used to associate a class member's value
    public static final Object VALUE_KEY = new Object();

    // Used to associate whether or not a user-defined
    // decorator includes an @initialize decorator
    public static final Object HAS_INITIALIZE = new Object();

    public static void init(Scriptable scope) {
        WrapDecorator.init(scope);
        RegisterDecorator.init(scope);
        NumericTemplateDecorator.init(scope);
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return consume(args[0], (int) args[1], (DecoratorType) args[2], cx, scope, thisObj, (Object[]) args[3]);
    }

    public abstract Object consume(Object target, int descriptor, DecoratorType decoratorType, Context cx, Scriptable scope, Scriptable thisObj, Object[] args);
}
