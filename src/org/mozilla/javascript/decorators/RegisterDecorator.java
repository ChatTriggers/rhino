package org.mozilla.javascript.decorators;

import org.mozilla.javascript.*;

public class RegisterDecorator extends Decorator {
    public static void init(Scriptable scope) {
        RegisterDecorator register = new RegisterDecorator();
        ScriptableObject.defineProperty(scope, "@register", register, ScriptableObject.NOT_ENUMERABLE);
    }

    @Override
    public Object consume(Object target, int descriptor, DecoratorType decoratorType, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (decoratorType != DecoratorType.REGISTER) return Undefined.instance;

        Object[] callArgs;
        Object realTarget = target;

        if ((descriptor & STATIC) == 0) {
            realTarget = ScriptableObject.getProperty(ScriptableObject.ensureScriptable(target), "prototype");
        }

        if ((descriptor & CLASS) != 0 || (descriptor & PRIVATE) != 0) {
            callArgs = new Object[]{ realTarget };
        } else {
            callArgs = new Object[]{ realTarget, ((ScriptableObject) target).getAssociatedValue(NAME_KEY) };
        }

        Object result = ((Callable) args[0]).call(cx, scope, thisObj, callArgs);

        // Ensure result is undefined
        if (!Undefined.isUndefined(result)) {
            throw ScriptRuntime.typeError("The function provided to @register must return undefined");
        }

        return Undefined.instance;
    }
}
