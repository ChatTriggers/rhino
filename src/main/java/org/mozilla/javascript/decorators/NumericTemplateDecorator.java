package org.mozilla.javascript.decorators;

import org.mozilla.javascript.*;

public class NumericTemplateDecorator extends Decorator {
    public static void init(Scriptable scope) {
        NumericTemplateDecorator numericTemplate = new NumericTemplateDecorator();
        ScriptableObject.defineProperty(scope, "@numericTemplate", numericTemplate, ScriptableObject.NOT_ENUMERABLE);
    }

    @Override
    public Object consume(Object target, int descriptor, DecoratorType decoratorType, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (decoratorType != DecoratorType.NUMERICTEMPLATE) {
            throw ScriptRuntime.typeError("@numericTemplate decorator expected numeric literal, got " + ScriptRuntime.toString(target));
        }

        if (args.length == 0 || !(args[0] instanceof Callable)) return target;

        Scriptable obj = ScriptRuntime.newObjectLiteral(
            new Object[]{ "number", "string" },
            new Object[]{ target, ScriptRuntime.toString(target) },
            null,
            cx,
            scope
        );

        return ((Callable) args[0]).call(cx, scope, thisObj, new Object[]{ obj });
    }
}
