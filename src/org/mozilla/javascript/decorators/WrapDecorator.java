package org.mozilla.javascript.decorators;

import org.mozilla.javascript.*;

public class WrapDecorator extends Decorator {
    public static void init(Scriptable scope) {
        WrapDecorator wrap = new WrapDecorator();
        ScriptableObject.defineProperty(scope, "@wrap", wrap, ScriptableObject.NOT_ENUMERABLE);
    }

    @Override
    public Object consume(Object target, int descriptor, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if ((descriptor & PREINIT) == 0) return target;
        if (args.length == 0 || !(args[0] instanceof Callable)) return target;

        return ((Callable) args[0]).call(cx, scope, thisObj, new Object[]{ target });
    }
}
