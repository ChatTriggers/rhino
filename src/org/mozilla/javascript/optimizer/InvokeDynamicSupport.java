package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.*;

import java.lang.invoke.*;
import java.util.Arrays;
import java.util.Objects;

public class InvokeDynamicSupport {
    public static CallSite bootstrapGetObjectProp(MethodHandles.Lookup lookup, String name, MethodType type) {
        MutableCallSite callSite = new MutableCallSite(type);

        // The first call will be to getObjectProp in this class.
        MethodHandle check = INIT_GET_OBJ_PROP.bindTo(callSite);

        callSite.setTarget(check.asType(type));
        return callSite;
    }

    public static Object getObjectProp(MutableCallSite callSite, Object value, String property, Context cx, Scriptable scope) throws Throwable {
        callSite.setTarget(REAL_GET_OBJ_PROP);
        return REAL_GET_OBJ_PROP.invoke(value, property, cx, scope);
    }

    public static CallSite bootstrapCallWithTemplateLiteral(MethodHandles.Lookup lookup, String name, MethodType type) {
        MutableCallSite callSite = new MutableCallSite(type);

        // The first call will be to callWithTemplateLiteral in this class.
        MethodHandle check = INIT_CALL_WITH_TEMPLATE.bindTo(callSite);

        callSite.setTarget(check.asType(type));
        return callSite;
    }

    public static Object callWithTemplateLiteral(MutableCallSite callSite, Object[] args, int boundary, Object target, Context cx, Scriptable scope, Scriptable thisObj) {
        Object value = ScriptRuntime.callWithTemplateLiteral(args, boundary, target, cx, scope, thisObj);

        MethodHandle constant = MethodHandles.constant(Object.class, value);
        constant = MethodHandles.dropArguments(constant, 0, Object[].class, int.class, Object.class, Context.class, Scriptable.class, Scriptable.class);

        MethodHandle guarded = MethodHandles.guardWithTest(
                MethodHandles.insertArguments(ARRAY_EQUALS, 0, new Object[] { args }),
                constant,
                INIT_CALL_WITH_TEMPLATE.bindTo(callSite)
        );

        callSite.setTarget(guarded);
        return value;
    }

    public static CallSite bootstrapCallProp0(MethodHandles.Lookup lookup, String name, MethodType type) {
        MutableCallSite callSite = new MutableCallSite(type);

        // The first call will be to callWithTemplateLiteral in this class.
        MethodHandle check = INIT_CALL_PROP0.bindTo(callSite);

        callSite.setTarget(check.asType(type));
        return callSite;
    }

    public static Object callProp0(MutableCallSite callSite, Object obj, String property, Context cx, Scriptable scope) {
        if (obj instanceof ScriptableObject) {
            ScriptableObject realObj = ((ScriptableObject) obj);
            SwitchPoint switchPoint = realObj.getSwitchPoint();

            if (!switchPoint.hasBeenInvalidated()) {
                MethodHandle fallback = INIT_CALL_PROP0.bindTo(callSite);

                Callable func = ScriptRuntime.getPropFunctionAndThis(obj, property, cx, scope);
                ScriptRuntime.lastStoredScriptable(cx); // Discard
                MethodHandle actualCall = CALLABLE_CALL.bindTo(func);
                actualCall = MethodHandles.insertArguments(actualCall, 3, new Object[]{ScriptRuntime.emptyArgs});
                actualCall = MethodHandles.insertArguments(actualCall, 2, realObj);
                actualCall = MethodHandles.dropArguments(actualCall, 0, Object.class, String.class);

                MethodHandle switchPointGuarded = switchPoint.guardWithTest(actualCall, fallback);

                MethodHandle eqCheck = MethodHandles.insertArguments(OBJ_EQUALS, 1, obj);
                MethodHandle equalityGuarded = MethodHandles.guardWithTest(eqCheck, switchPointGuarded, fallback);

                callSite.setTarget(equalityGuarded);
                return func.call(cx, scope, realObj, ScriptRuntime.emptyArgs);
            }
        }

        callSite.setTarget(REAL_CALL_PROP0);
        return OptRuntime.callProp0(obj, property, cx, scope);
    }

    private static final MethodHandle INIT_GET_OBJ_PROP;
    private static final MethodHandle REAL_GET_OBJ_PROP;

    private static final MethodHandle INIT_CALL_WITH_TEMPLATE;

    private static final MethodHandle INIT_CALL_PROP0;
    private static final MethodHandle REAL_CALL_PROP0;
    private static final MethodHandle CALLABLE_CALL;

    private static final MethodHandle ARRAY_EQUALS;
    private static final MethodHandle OBJ_EQUALS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            INIT_GET_OBJ_PROP = lookup.findStatic(
                    InvokeDynamicSupport.class,
                    "getObjectProp",
                    MethodType.methodType(Object.class, MutableCallSite.class, Object.class, String.class, Context.class,  Scriptable.class)
            );

            REAL_GET_OBJ_PROP = lookup.findStatic(
                    ScriptRuntime.class,
                    "getObjectProp",
                    MethodType.methodType(Object.class, Object.class, String.class, Context.class, Scriptable.class)
            );

            INIT_CALL_WITH_TEMPLATE = lookup.findStatic(
                    InvokeDynamicSupport.class,
                    "callWithTemplateLiteral",
                    MethodType.methodType(Object.class, MutableCallSite.class, Object[].class, int.class, Object.class, Context.class, Scriptable.class, Scriptable.class)
            );

            INIT_CALL_PROP0 = lookup.findStatic(
                    InvokeDynamicSupport.class,
                    "callProp0",
                    MethodType.methodType(Object.class, MutableCallSite.class, Object.class, String.class, Context.class,  Scriptable.class)
            );

            REAL_CALL_PROP0 = lookup.findStatic(
                    OptRuntime.class,
                    "callProp0",
                    MethodType.methodType(Object.class, Object.class, String.class, Context.class,  Scriptable.class)
            );

            // ====== UTILS ======

            ARRAY_EQUALS = lookup.findStatic(
                    Arrays.class,
                    "equals",
                    MethodType.methodType(boolean.class, Object[].class, Object[].class)
            );

            OBJ_EQUALS = lookup.findStatic(
                    Objects.class,
                    "equals",
                MethodType.methodType(boolean.class, Object.class, Object.class)
            );

            CALLABLE_CALL = lookup.findVirtual(
                    Callable.class,
                    "call",
                    MethodType.methodType(Object.class, Context.class, Scriptable.class, Scriptable.class, Object[].class)
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
