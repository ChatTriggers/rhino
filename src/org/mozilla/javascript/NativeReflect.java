package org.mozilla.javascript;

import jdk.nashorn.internal.runtime.ScriptObject;

public class NativeReflect extends IdScriptableObject {
    private static final Object REFLECT_TAG = "Reflect";

    static void init(Scriptable scope, boolean sealed) {
        NativeReflect obj = new NativeReflect();
        obj.activatePrototypeMap(LAST_METHOD_ID);
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed)
            obj.sealObject();
        ScriptableObject.defineProperty(scope, "Reflect", obj, ScriptableObject.DONTENUM);
    }

    @Override
    public String getClassName() {
        return (String) REFLECT_TAG;
    }

    @Override
    protected void initPrototypeId(int id) {
        String name;
        int arity;

        switch (id) {
            case Id_apply:
                arity = 3;
                name = "apply";
                break;
            case Id_construct:
                arity = 3;
                name = "construct";
                break;
            case Id_defineProperty:
                arity = 3;
                name = "defineProperty";
                break;
            case Id_deleteProperty:
                arity = 2;
                name = "deleteProperty";
                break;
            case Id_get:
                arity = 3;
                name = "get";
                break;
            case Id_getOwnPropertyDescriptor:
                arity = 2;
                name = "getOwnPropertyDescriptor";
                break;
            case Id_getPrototypeOf:
                arity = 1;
                name = "getPrototypeOf";
                break;
            case Id_has:
                arity = 2;
                name = "has";
                break;
            case Id_isExtensible:
                arity = 1;
                name = "isExtensible";
                break;
            case Id_ownKeys:
                arity = 1;
                name = "ownKeys";
                break;
            case Id_preventExtensions:
                arity = 1;
                name = "preventExtensions";
                break;
            case Id_set:
                arity = 4;
                name = "set";
                break;
            case Id_setPrototypeOf:
                arity = 2;
                name = "setPrototypeOf";
                break;
            default:
                throw new IllegalStateException(String.valueOf(id));
        }

        initPrototypeMethod(REFLECT_TAG, id, name, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(REFLECT_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        switch (f.methodId()) {
            case Id_apply:
                return js_apply(args, cx, scope);
            // case Id_construct:
            //     // FIXME
            //     return js_construct(args, cx, scope);
            // case Id_defineProperty:
            //     return js_defineProperty(args, cx, scope);
            default:
                return null;
        }
    }

    private Object js_apply(Object[] args, Context cx, Scriptable scope) {
        if (args.length > 0 && !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeError4("msg.reflect.bad.args", "target", "Reflect.apply", "function", args[0].getClass().getSimpleName());
        } else if (args.length > 2 && !(args[2] instanceof NativeArray)) {
            throw ScriptRuntime.typeError4("msg.reflect.bad.args", "argumentsList", "Reflect.apply", "array", args[2].getClass().getSimpleName());
        }

        Function target = (Function) args[0];
        Scriptable thisObj = args[1] == Undefined.instance ? null : ScriptRuntime.toObject(cx, scope, args[1]);
        NativeArray targetArgs = (NativeArray) args[2];

        return target.call(cx, scope, thisObj, targetArgs.toArray());
    }

    // private Object js_construct(Object[] args, Context cx, Scriptable scope) {
    //     if (args.length > 0 && !(args[0] instanceof Function)) {
    //         throw ScriptRuntime.typeError4("msg.reflect.bad.args", "target", "Reflect.construct", "function", args[0].getClass().getSimpleName());
    //     } else if (args.length > 1 && !(args[1] instanceof NativeArray)) {
    //         throw ScriptRuntime.typeError4("msg.reflect.bad.args", "argumentsList", "Reflect.construct", "array", args[2].getClass().getSimpleName());
    //     }
    //
    //     Function target = (Function) args[0];
    //     NativeArray targetArgs = (NativeArray) args[1];
    //     Scriptable newTarget = null;
    //
    //     if (args.length > 2) {
    //         if (args[2] instanceof Scriptable) {
    //             newTarget = (Scriptable) args[2];
    //         } else {
    //             throw ScriptRuntime.typeError4("msg.reflect.bad.args", "newTarget", "Reflect.construct", "function", args[0].getClass().getSimpleName());
    //         }
    //     }
    //
    //     Scriptable obj = target.construct(cx, scope, targetArgs.toArray());
    //
    //     if (newTarget != null) {
    //         obj.setParentScope(getParentScope());
    //         obj.setPrototype(newTarget.getPrototype());
    //     }
    //
    //     return obj;
    // }

    // private boolean js_defineProperty(Object[] args, Context cx, Scriptable scope) {
    //     if (args.length > 0 && !(args[0] instanceof ScriptableObject)) {
    //         throw ScriptRuntime.typeError4("msg.reflect.bad.args", "target", "Reflect.construct", "function", args[0].getClass().getSimpleName());
    //     }
    //
    //     ScriptableObject target = (ScriptableObject) args[0];
    //     Object propKey = args[1];
    //     ScriptableObject attributes = (ScriptableObject) args[2];
    //
    //     try {
    //         target.defineOwnProperty(cx, scope, attributes);
    //         return true;
    //     } catch (Exception e) {
    //         return false;
    //     }
    // }


// #string_id_map#


    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2019-11-19 22:24:21 CST
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 3: c=s.charAt(0);
                if (c=='g') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_get; break L0;} }
                else if (c=='h') { if (s.charAt(2)=='s' && s.charAt(1)=='a') {id=Id_has; break L0;} }
                else if (c=='s') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_set; break L0;} }
                break L;
            case 5: X="apply";id=Id_apply; break L;
            case 7: X="ownKeys";id=Id_ownKeys; break L;
            case 9: X="construct";id=Id_construct; break L;
            case 12: X="isExtensible";id=Id_isExtensible; break L;
            case 14: c=s.charAt(0);
                if (c=='d') {
                    c=s.charAt(2);
                    if (c=='f') { X="defineProperty";id=Id_defineProperty; }
                    else if (c=='l') { X="deleteProperty";id=Id_deleteProperty; }
                }
                else if (c=='g') { X="getPrototypeOf";id=Id_getPrototypeOf; }
                else if (c=='s') { X="setPrototypeOf";id=Id_setPrototypeOf; }
                break L;
            case 17: X="preventExtensions";id=Id_preventExtensions; break L;
            case 24: X="getOwnPropertyDescriptor";id=Id_getOwnPropertyDescriptor; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_apply = 1,
        Id_construct = 2,
        Id_defineProperty = 3,
        Id_deleteProperty = 4,
        Id_get = 5,
        Id_getOwnPropertyDescriptor = 6,
        Id_getPrototypeOf = 7,
        Id_has = 8,
        Id_isExtensible = 9,
        Id_ownKeys = 10,
        Id_preventExtensions = 11,
        Id_set = 12,
        Id_setPrototypeOf = 13,
        LAST_METHOD_ID = Id_setPrototypeOf;

// #/string_id_map#
}
