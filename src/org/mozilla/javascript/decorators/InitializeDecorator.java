package org.mozilla.javascript.decorators;

import org.mozilla.javascript.*;

public class InitializeDecorator extends Decorator {
    public static void init(Scriptable scope) {
        InitializeDecorator initialize = new InitializeDecorator();
        ScriptableObject.defineProperty(scope, "@initialize", initialize, ScriptableObject.NOT_ENUMERABLE);
    }

    @Override
    public Object consume(Object target, int descriptor, DecoratorType decoratorType, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if ((descriptor & STATIC) != 0) {
            // The proposal is not clear what this behavior should be,
            // so I have chosen to disallow it. The whole point of
            // @initialize is to have field/method-related code
            // run in a class's constructor, which makes very little
            // sense in a static context
            throw ScriptRuntime.typeError("@initialize is not allowed on static class members");
        }

        if (decoratorType != DecoratorType.INITIALIZE) return target;

        if (args.length == 0 || !(args[0] instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.object.not.callable", ScriptRuntime.toString(args[0]));
        }

        Callable cb = (Callable) args[0];
        ScriptableObject sTarget = ScriptableObject.ensureScriptableObject(target);

        Object key = Undefined.instance;
        Object value = Undefined.instance;

        if ((descriptor & PUBLIC) != 0 && (descriptor & CLASS) == 0) {
            // public field/method
            key = sTarget.hasAssociatedValue(NAME_KEY) ? sTarget.getAssociatedValue(NAME_KEY) : Undefined.instance;

            if ((descriptor & METHOD) == 0) {
                // public field
                value = sTarget.hasAssociatedValue(VALUE_KEY) ? sTarget.getAssociatedValue(VALUE_KEY) : Undefined.instance;
            }
        }

        Object result = cb.call(cx, scope, thisObj, new Object[]{ target, key, value });

        // Ensure result is undefined
        if (!Undefined.isUndefined(result)) {
            throw ScriptRuntime.typeError("The function provided to @initialize must return undefined");
        }

        return target;
    }
}
