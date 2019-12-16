package org.mozilla.javascript.decorators;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class WrapDecorator implements Decorator {
    public static void init(Scriptable scope) {
        WrapDecorator wrap = new WrapDecorator();
        ScriptableObject.defineProperty(scope, "@wrap", wrap, ScriptableObject.NOT_ENUMERABLE);
    }

    @Override
    public Object consume(Object target, int descriptor, Object[] descriptorArgs, Object[] metadata, Context cx, Scriptable scope, Scriptable thisObj) {
        return ((Callable) descriptorArgs[0]).call(cx, scope, thisObj, new Object[]{ target });
    }
}
