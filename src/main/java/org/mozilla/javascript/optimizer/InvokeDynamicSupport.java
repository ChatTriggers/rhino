package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import java.lang.invoke.*;
import java.util.Arrays;

public class InvokeDynamicSupport {
    public static CallSite bootstrapGetObjectProp(MethodHandles.Lookup lookup, String name, MethodType type) {
        MutableCallSite callSite = new MutableCallSite(type);

        // The first call will be to getObjectProp in this class.
        MethodHandle check = INIT_GET_OBJ_PROP.bindTo(callSite);

        callSite.setTarget(check.asType(type));
        return callSite;
    }

    public static Object getObjectProp(MutableCallSite callSite, Object value, String property, Context cx, Scriptable scope, boolean isPrivate) throws Throwable {
        callSite.setTarget(REAL_GET_OBJ_PROP);
        return REAL_GET_OBJ_PROP.invoke(value, property, cx, scope, isPrivate);
    }

    public static CallSite bootstrapCallWithTemplateLiteral(MethodHandles.Lookup lookup, String name, MethodType type) {
        MutableCallSite callSite = new MutableCallSite(type);

        // The first call will be to callWithTemplateLiteral in this class.
        MethodHandle check = INIT_CALL_WITH_TEMPLATE.bindTo(callSite);

        callSite.setTarget(check.asType(type));
        return callSite;
    }

    public static Object callWithTemplateLiteral(
        MutableCallSite callSite, Object[] args, int boundary, Object[] rawStrings,
        Object target, Context cx, Scriptable scope, Scriptable thisObj
    ) {
        Object value = ScriptRuntime.callWithTemplateLiteral(args, boundary, rawStrings, target, cx, scope, thisObj);

        MethodHandle constant = MethodHandles.constant(Object.class, value);
        constant = MethodHandles.dropArguments(
            constant, 0, Object[].class, int.class, Object[].class, Object.class, Context.class, Scriptable.class, Scriptable.class
        );

        MethodHandle guarded = MethodHandles.guardWithTest(
                MethodHandles.insertArguments(ARRAY_EQUALS, 0, new Object[] { args }),
                constant,
                INIT_CALL_WITH_TEMPLATE.bindTo(callSite)
        );

        callSite.setTarget(guarded);
        return value;
    }

    private static final MethodHandle INIT_GET_OBJ_PROP;
    private static final MethodHandle REAL_GET_OBJ_PROP;

    private static final MethodHandle INIT_CALL_WITH_TEMPLATE;
    private static final MethodHandle REAL_CALL_WITH_TEMPLATE;

    private static final MethodHandle ARRAY_EQUALS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            REAL_GET_OBJ_PROP = lookup.findStatic(
                    ScriptRuntime.class,
                    "getObjectProp",
                    MethodType.methodType(Object.class, Object.class, String.class, Context.class, Scriptable.class, boolean.class)
            );

            INIT_GET_OBJ_PROP = lookup.findStatic(
                    InvokeDynamicSupport.class,
                    "getObjectProp",
                    MethodType.methodType(Object.class, MutableCallSite.class, Object.class, String.class, Context.class,  Scriptable.class, boolean.class)
            );

            REAL_CALL_WITH_TEMPLATE = lookup.findStatic(
                    ScriptRuntime.class,
                    "callWithTemplateLiteral",
                    MethodType.methodType(Object.class, Object[].class, int.class, Object[].class, Object.class, Context.class, Scriptable.class, Scriptable.class)
            );

            INIT_CALL_WITH_TEMPLATE = lookup.findStatic(
                    InvokeDynamicSupport.class,
                    "callWithTemplateLiteral",
                    MethodType.methodType(Object.class, MutableCallSite.class, Object[].class, int.class, Object[].class, Object.class, Context.class, Scriptable.class, Scriptable.class)
            );

            // ====== UTILS ======

            ARRAY_EQUALS = lookup.findStatic(
                    Arrays.class,
                    "equals",
                    MethodType.methodType(boolean.class, Object[].class, Object[].class)
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
