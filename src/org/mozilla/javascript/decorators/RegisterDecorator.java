package org.mozilla.javascript.decorators;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class RegisterDecorator implements Decorator {
    public static void init(Scriptable scope) {
        RegisterDecorator register = new RegisterDecorator();
        ScriptableObject.defineProperty(scope, "@register", register, ScriptableObject.NOT_ENUMERABLE);
    }

    @Override
    public Object consume(Context cx, Scriptable scope, Scriptable thisObj, Object obj, int descriptor, Object[] descriptorArgs, Object[] metadata) {
        Object[] args;

        if ((descriptor & CLASS) != 0 || (descriptor & PRIVATE) != 0) {
            args = new Object[]{ obj };
        } else {
            args = new Object[]{ obj, metadata[0] };
        }

        return ((Callable) descriptorArgs[0]).call(cx, scope, thisObj, args);
    }
}
