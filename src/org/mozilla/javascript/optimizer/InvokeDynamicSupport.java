package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.*;

import java.lang.invoke.*;
import java.util.Arrays;

public class InvokeDynamicSupport {
    private static final MethodHandle INIT_CALL_WITH_TEMPLATE;
    private static final MethodHandle INIT_LOOKUP_NAME;
    private static final MethodHandle ARRAY_EQUALS;
    private static final MethodHandle OBJECT_EQUALS;
    private static final MethodHandle SLOT_GET_VALUE;
    private static final MethodHandle SCRIPTABLE_GET;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            INIT_CALL_WITH_TEMPLATE = lookup.findStatic(
                InvokeDynamicSupport.class,
                "callWithTemplateLiteral",
                MethodType.methodType(
                    Object.class,
                    MutableCallSite.class,
                    Object[].class,
                    int.class,
                    Object[].class,
                    Object.class,
                    Context.class,
                    Scriptable.class,
                    Scriptable.class
                )
            );

            INIT_LOOKUP_NAME = lookup.findStatic(
                InvokeDynamicSupport.class,
                "lookupName",
                MethodType.methodType(
                    Object.class,
                    MutableCallSite.class,
                    Context.class,
                    Scriptable.class,
                    String.class
                )
            );

            // ====== UTILS ======

            ARRAY_EQUALS = lookup.findStatic(
                Arrays.class,
                "equals",
                MethodType.methodType(boolean.class, Object[].class, Object[].class)
            );

            OBJECT_EQUALS = lookup.findVirtual(
                Object.class,
                "equals",
                MethodType.methodType(boolean.class, Object.class)
            );

            SLOT_GET_VALUE = lookup.findVirtual(
                ScriptableObject.Slot.class,
                "getValue",
                MethodType.methodType(Object.class, Scriptable.class)
            );

            SCRIPTABLE_GET = lookup.findVirtual(
                Scriptable.class,
                "get",
                MethodType.methodType(Object.class, String.class, Scriptable.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite bootstrapLookupName(MethodHandles.Lookup lookup, String name, MethodType type) {
        MutableCallSite callSite = new MutableCallSite(type);

        // The first call will be to callWithTemplateLiteral in this class.
        MethodHandle check = INIT_LOOKUP_NAME.bindTo(callSite);

        callSite.setTarget(check.asType(type));
        return callSite;
    }

    public static Object lookupName(MutableCallSite callSite, Context cx, Scriptable scope, String name) {
        ScriptablePair pair = lookupNameOwner(cx, scope, name);
        Scriptable owner = pair.getFirst();
        Scriptable start = pair.getSecond();

        if (owner == null) {
            String msg = ScriptRuntime.getMessage1("msg.is.not.defined", name);
            RuntimeException notFoundError = ScriptRuntime.constructError("ReferenceError", msg);

            ScriptRuntime.name(cx, scope, name);
            lookupNameOwner(cx, scope, name);

            MethodHandle exception = MethodHandles.throwException(Object.class, RuntimeException.class);
            exception = exception.bindTo(notFoundError);
            constructLookupHandle(exception, scope, callSite);

            throw notFoundError;
        }

        if (owner instanceof ScriptableObject) {
            ScriptableObject.Slot targetSlot = ((ScriptableObject) owner).getSlot(name);

            MethodHandle getValue = MethodHandles.insertArguments(SLOT_GET_VALUE, 0, targetSlot, start);
            constructLookupHandle(getValue, scope, callSite);

            return targetSlot.getValue(start);
        }

        MethodHandle getValue = MethodHandles.insertArguments(SCRIPTABLE_GET, 0, owner, name, start);
        constructLookupHandle(getValue, scope, callSite);

        return owner.get(name, start);
    }

    private static ScriptablePair lookupNameOwner(Context cx, Scriptable scope, String name) {
        Scriptable parent = scope.getParentScope();
        Scriptable owner;
        Scriptable start;

        if (parent == null) {
            owner = ScriptableObject.getBase(
                cx.useDynamicScope ? ScriptRuntime.checkDynamicScope(cx.topCallScope, scope) : scope,
                name
            );
            start = scope;
            return new ScriptablePair(owner, start);
        }

        for (; ; ) {
            if (scope instanceof NativeWith) {
                Scriptable withObj = scope.getPrototype();
                owner = ScriptableObject.getBase(withObj, name);
                start = withObj;
                if (owner != null) {
                    break;
                }
            } else if (scope instanceof NativeCall) {
                // NativeCall does not prototype chain and Scriptable.get
                // can be called directly.
                if (scope.has(name, scope)) {
                    owner = scope;
                    start = scope;
                    break;
                }
            } else {
                // Can happen if Rhino embedding decided that nested
                // scopes are useful for what ever reasons.
                owner = ScriptableObject.getBase(scope, name);
                start = scope;

                if (owner != null) {
                    break;
                }
            }
            scope = parent;
            parent = parent.getParentScope();
            if (parent == null) {
                owner = ScriptableObject.getBase(
                    cx.useDynamicScope ? ScriptRuntime.checkDynamicScope(cx.topCallScope, scope) : scope,
                    name
                );
                start = scope;
                break;
            }
        }

        return new ScriptablePair(owner, start);
    }

    private static void constructLookupHandle(MethodHandle getValue, Scriptable scope, MutableCallSite callSite) {
        getValue = MethodHandles.dropArguments(getValue, 0, Context.class, Scriptable.class, String.class);
        MethodHandle comparison = MethodHandles.dropArguments(MethodHandles.dropArguments(
            OBJECT_EQUALS.bindTo(scope),
            0,
            Context.class
        ), 2, String.class);
        getValue = MethodHandles.guardWithTest(
            MethodHandles.explicitCastArguments(comparison, getValue.type().changeReturnType(boolean.class)),
            getValue,
            INIT_LOOKUP_NAME.bindTo(callSite)
        );
        callSite.setTarget(getValue);
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
            constant,
            0,
            Object[].class,
            int.class,
            Object[].class,
            Object.class,
            Context.class,
            Scriptable.class,
            Scriptable.class
        );

        MethodHandle guarded = MethodHandles.guardWithTest(
            MethodHandles.insertArguments(ARRAY_EQUALS, 0, new Object[]{args}),
            constant,
            INIT_CALL_WITH_TEMPLATE.bindTo(callSite)
        );

        callSite.setTarget(guarded);
        return value;
    }
}
