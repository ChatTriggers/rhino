package org.mozilla.javascript;

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
            case Id_construct:
                return js_construct(args, cx, scope);
            case Id_defineProperty:
                return js_defineProperty(args, cx);
            case Id_deleteProperty:
                return js_deleteProperty(args, cx, scope);
            case Id_get:
                return js_get(args);
            case Id_getOwnPropertyDescriptor:
                return js_getOwnPropertyDescriptor(args, cx, scope);
            case Id_getPrototypeOf:
                return js_getPrototypeOf(args, cx, scope);
            case Id_has:
                return js_has(args, cx);
            case Id_isExtensible:
                return js_isExtensible(args, cx, scope);
            case Id_ownKeys:
                return js_ownKeys(args, cx, scope);
            case Id_preventExtensions:
                return js_preventExtensions(args);
            case Id_set:
                return js_set(args);
            case Id_setPrototypeOf:
                return js_setPrototypeOf(args, cx, scope);
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

    private Object js_construct(Object[] args, Context cx, Scriptable scope) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.not.obj", args.length < 1 ? Undefined.instance : args[0]);
        } else if (args.length < 2 || !(args[1] instanceof NativeArray)) {
            throw ScriptRuntime.typeError("Expected argumentsList to be an array");
        }

        Function target = (Function) args[0];
        NativeArray argumentsList = (NativeArray) args[1];
        Function newTarget = null;

        if (args.length > 2) {
            newTarget = (Function) args[2];
        }

        Scriptable val = ScriptRuntime.newObject(target, cx, newTarget, argumentsList.toArray());

        if (newTarget != null) {
            Scriptable obj = newTarget.construct(cx, scope, new Object[]{});
            val.setPrototype(obj.getPrototype());
        }

        return val;
    }

    private boolean js_defineProperty(Object[] args, Context cx) {
        ScriptableObject target = ensureScriptableObject(args.length > 0 ? args[0] : Undefined.instance);
        Object propKey = args[1];
        ScriptableObject desc = ensureScriptableObject(args.length > 2 ? args[2] : Undefined.instance);

        try {
            target.defineOwnProperty(cx, propKey, desc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean js_deleteProperty(Object[] args, Context cx, Scriptable scope) {
        ScriptableObject target = ensureScriptableObject(args.length > 0 ? args[0] : Undefined.instance);
        Object propKey = args[1];

        try {
            Object res = ScriptRuntime.delete(target, propKey, cx, scope, true);

            if (res instanceof Boolean) {
                return (boolean) res;
            } else {
                throw Kit.codeBug();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private Object js_get(Object[] args) {
        if (args.length < 1 || !(args[0] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.not.obj", args.length < 1 ? Undefined.instance : args[0]);
        }

        Scriptable target = ensureScriptable(args[0]);
        Object key = args.length > 1 ? args[1] : Undefined.instance;

        if (key instanceof String) {
            return ScriptableObject.getProperty(target, (String) key);
        } else if (key instanceof Integer) {
            return ScriptableObject.getProperty(target, key);
        } else if (key instanceof Double) {
            return ScriptableObject.getProperty(target, ((Double) key).intValue());
        } else {
            throw Kit.codeBug();
        }
    }

    private Object js_getOwnPropertyDescriptor(Object[] args, Context cx, Scriptable scope) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        // TODO(norris): There's a deeper issue here if
        // arg instanceof Scriptable. Should we create a new
        // interface to admit the new ECMAScript 5 operations?
        Scriptable s = NativeObject.getCompatibleObject(cx, scope, arg);
        ScriptableObject obj = ensureScriptableObject(s);
        Object nameArg = args.length < 2 ? Undefined.instance : args[1];
        Scriptable desc = obj.getOwnPropertyDescriptor(cx, nameArg);
        return desc == null ? Undefined.instance : desc;
    }

    private Object js_getPrototypeOf(Object[] args, Context cx, Scriptable scope) {
        if (args.length < 1) return null;
        return NativeObject.getCompatibleObject(cx, scope, args[0]).getPrototype();
    }

    private boolean js_has(Object[] args, Context cx) {
        if (args.length < 2) Kit.codeBug(); // TODO: Error msg

        return ScriptRuntime.in(args[1], args[0], cx);
    }

    private boolean js_isExtensible(Object[] args, Context cx, Scriptable scope) {
        if (args.length < 1 || !(args[0] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.not.obj", args.length < 1 ? Undefined.instance : args[0]);
        }

        ScriptableObject obj = (ScriptableObject) args[0];
        return obj.isExtensible();
    }

    private Object js_ownKeys(Object[] args, Context cx, Scriptable scope) {
        if (args.length < 1 || !(args[0] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.not.obj", args.length < 1 ? Undefined.instance : args[0]);
        }

        ScriptableObject obj = ensureScriptableObject(NativeObject.getCompatibleObject(cx, scope, args[0]));
        Object[] ownKeys = obj.getIds(true, true);

        for (int i = 0; i < ownKeys.length; i++) {
            Object key = ownKeys[i];

            if (!(key instanceof Symbol)) {
                ownKeys[i] = ScriptRuntime.toString(ownKeys[i]);
            }
        }
        return cx.newArray(scope, ownKeys);
    }

    private Object js_preventExtensions(Object[] args) {
        if (args.length < 1 || !(args[0] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.not.obj", args.length < 1 ? Undefined.instance : args[0]);
        }

        try {
            ScriptableObject obj = ensureScriptableObject(args[0]);
            obj.preventExtensions();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean js_set(Object[] args) {
        if (args.length < 1 || !(args[0] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.not.obj", args.length < 1 ? Undefined.instance : args[0]);
        }

        Scriptable target = args.length > 0 ? ensureScriptable(args[0]) : null;
        Object key = args.length > 1 ? args[1] : Undefined.instance;
        Object value = args.length > 2 ? args[2] : Undefined.instance;

        try {
            if (key instanceof String) {
                ScriptableObject.putProperty(target, (String) key, value);
            } else if (key instanceof Integer) {
                ScriptableObject.putProperty(target, (Integer) key, value);
            } else if (key instanceof Double) {
                ScriptableObject.putProperty(target, ((Double) key).intValue(), value);
            } else if (key instanceof Undefined) {
                ScriptableObject.putProperty(target, "undefined", value);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Object js_setPrototypeOf(Object[] args, Context cx, Scriptable scope) {
        if (args.length < 2 || !(args[1] == null || args[1] instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(args.length < 2 ? Undefined.instance : args[1]));
        }

        Object target = args[0];
        ScriptableObject key = (ScriptableObject) args[1];

        if (key instanceof Symbol) {
            throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(target));
        }

        if (!(target instanceof ScriptableObject)) {
            return target;
        }

        ScriptableObject scriptTarget = (ScriptableObject) target;
        if (!scriptTarget.isExtensible()) {
            return false;
        }

        Scriptable prototypeProto = key;
        while (prototypeProto != null) {
            if (prototypeProto == scriptTarget) {
                return false;
            }
            prototypeProto = prototypeProto.getPrototype();
        }

        scriptTarget.setPrototype(key);
        return true;
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2019-11-19 22:24:21 CST
        L0:
        {
            id = 0;
            String X = null;
            int c;
            L:
            switch (s.length()) {
                case 3:
                    c = s.charAt(0);
                    if (c == 'g') {
                        if (s.charAt(2) == 't' && s.charAt(1) == 'e') {
                            id = Id_get;
                            break L0;
                        }
                    } else if (c == 'h') {
                        if (s.charAt(2) == 's' && s.charAt(1) == 'a') {
                            id = Id_has;
                            break L0;
                        }
                    } else if (c == 's') {
                        if (s.charAt(2) == 't' && s.charAt(1) == 'e') {
                            id = Id_set;
                            break L0;
                        }
                    }
                    break L;
                case 5:
                    X = "apply";
                    id = Id_apply;
                    break L;
                case 7:
                    X = "ownKeys";
                    id = Id_ownKeys;
                    break L;
                case 9:
                    X = "construct";
                    id = Id_construct;
                    break L;
                case 12:
                    X = "isExtensible";
                    id = Id_isExtensible;
                    break L;
                case 14:
                    c = s.charAt(0);
                    if (c == 'd') {
                        c = s.charAt(2);
                        if (c == 'f') {
                            X = "defineProperty";
                            id = Id_defineProperty;
                        } else if (c == 'l') {
                            X = "deleteProperty";
                            id = Id_deleteProperty;
                        }
                    } else if (c == 'g') {
                        X = "getPrototypeOf";
                        id = Id_getPrototypeOf;
                    } else if (c == 's') {
                        X = "setPrototypeOf";
                        id = Id_setPrototypeOf;
                    }
                    break L;
                case 17:
                    X = "preventExtensions";
                    id = Id_preventExtensions;
                    break L;
                case 24:
                    X = "getOwnPropertyDescriptor";
                    id = Id_getOwnPropertyDescriptor;
                    break L;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
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
