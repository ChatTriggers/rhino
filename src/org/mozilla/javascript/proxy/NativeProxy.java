package org.mozilla.javascript.proxy;

import org.mozilla.javascript.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mozilla.javascript.ScriptableObject.*;

@SuppressWarnings("DuplicatedCode")
public class NativeProxy extends IdScriptableObject implements Function {
    private static final long serialVersionUID = -5164128569432516845L;

    private ScriptableObject target;
    private ScriptableObject handler;
    private List<Object> handlerIds;
    private boolean revoked;

    public static void init(Scriptable scope, boolean sealed) {
        NativeProxyCtor ctor = new NativeProxyCtor();

        ctor.setParentScope(scope);
        ctor.setPrototype(getObjectPrototype(scope));
        ScriptableObject.deleteProperty(ctor, "prototype");

        if (sealed) {
            ctor.sealObject();
        }

        defineProperty(scope, "Proxy", ctor, ScriptableObject.DONTENUM);
    }

    NativeProxy(ScriptableObject target, ScriptableObject handler) {
        this.target = target;
        this.handler = handler;
        this.handlerIds = handler == null ? Collections.emptyList() : Arrays.asList(handler.getIds());
    }

    public ScriptableObject getTarget() {
        return target;
    }

    void revoke() {
        this.revoked = true;
    }

    private void ensureNotRevoked() {
        if (revoked) {
            throw ScriptRuntime.typeError0("msg.proxy.revocable.illegal.operation");
        }
    }

    @Override
    public String getClassName() {
        return target.getClassName();
    }

    @Override
    protected void initPrototypeId(int id) {
        throw new IllegalArgumentException(String.valueOf(id));
    }



    @Override
    public Object get(String name, Scriptable start) {
        ensureNotRevoked();

        if (!handlerIds.contains("get")) {
            return target.get(name, target);
        }

        Object get = handler.get("get");

        if (get == null) {
            return null;
        } else if (!(get instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "get");
        }

        Function fn = (Function) get;
        Object result = fn.call(Context.getContext(), this, start, new Object[]{ target, name, start });

        if (hasProperty(target, name)) {
            int attributes = target.getAttributes(name);

            if ((attributes & PERMANENT) != 0 && (attributes & READONLY) != 0) {
                Object targetRes = target.get(name);

                if (result != targetRes) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.get", name);
                }
            }
        }

        return result;

    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        ensureNotRevoked();

        if (!handlerIds.contains("get")) {
            return target.get(key, target);
        }

        Object get = handler.get("get");

        if (get == null) {
            return null;
        } else if (!(get instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "get");
        }

        Function fn = (Function) get;
        Object result = fn.call(Context.getContext(), this, start, new Object[]{ target, key, start });

        if (hasProperty(target, key)) {
            int attributes = target.getAttributes(key);

            if ((attributes & PERMANENT) != 0 && (attributes & READONLY) != 0) {
                Object targetRes = target.get(key);

                if (result != targetRes) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.get", key);
                }
            }
        }

        return result;

    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        ensureNotRevoked();

        if (!handlerIds.contains("set")) {
            target.put(name, target, value);
            return;
        }

        if (hasProperty(target, name)) {
            int attributes = target.getAttributes(name);

            Object getter = target.getGetterOrSetter(name, 0, false);

            if ((attributes & PERMANENT) != 0 && (getter != null || getter != Undefined.instance) && ScriptableObject.hasProperty(target, "set")) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.set.accessor", name);
            } else if ((attributes & PERMANENT) != 0 && (attributes & READONLY) != 0) {
                Object oldVal = target.get(name);

                if (oldVal != value) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.set.property", name);
                }
            }
        }

        Object setter = handler.get("set");

        if (setter == null) {
            return;
        } else if (!(setter instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "set");
        }

        ((Function) setter).call(Context.getContext(), this, start, new Object[]{target, name, value, start});
    }

    @Override
    public void put(Symbol key, Scriptable start, Object value) {
        ensureNotRevoked();

        if (!handlerIds.contains("set")) {
            target.put(key, target, value);
        }

        if (hasProperty(target, key)) {
            int attributes = target.getAttributes(key);

            Object getter = target.getGetterOrSetter(key, 0, false);

            if ((attributes & PERMANENT) != 0 && (getter != null || getter != Undefined.instance) && ScriptableObject.hasProperty(target, "set")) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.set.accessor", key);
            } else if ((attributes & PERMANENT) != 0 && (attributes & READONLY) != 0) {
                Object oldVal = target.get(key);

                if (oldVal != value) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.set.property", key);
                }
            }
        }

        Object setter = handler.get("set");

        if (setter == null) {
            return;
        } else if (!(setter instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "put");
        }

        ((Function) setter).call(Context.getContext(), this, start, new Object[]{target, key, value, start});
    }

    @Override
    public boolean has(String name, Scriptable start) {
        ensureNotRevoked();

        if (!handlerIds.contains("has")) {
            return target.has(name, target);
        }

        Object handlerHas = handler.get("has");

        if (handlerHas == null) {
            return false;
        } else if (!(handlerHas instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "has");
        }

        Function fn = (Function) handlerHas;
        Object _handlerResult = fn.call(Context.getContext(), this, this, new Object[]{ target, name });

        if (!(_handlerResult instanceof Boolean) && _handlerResult != Undefined.instance) {
            // TODO: error
            throw Kit.codeBug();
        }
        boolean handlerResult = _handlerResult != Undefined.instance && (boolean) _handlerResult;

        if (hasProperty(target, name)) {
            int attributes = target.getAttributes(name);

            if ((attributes & PERMANENT) != 0 && !handlerResult) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.has.non.configurable", name);
            } else if (!target.isExtensible() && !handlerResult) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.has.non.extensible", name);
            }
        }

        return handlerResult;

    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        ensureNotRevoked();

        if (!handlerIds.contains("has")) {
            return target.has(key, target);
        } else {
            Object handlerHas = handler.get("has");

            if (handlerHas == null) {
                return false;
            } else if (!(handlerHas instanceof Function)) {
                throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "has");
            }

            Function fn = (Function) handlerHas;
            Object _handlerResult = fn.call(Context.getContext(), this, this, new Object[]{target, key});

            if (!(_handlerResult instanceof Boolean) && _handlerResult != Undefined.instance) {
                // TODO: error
                throw Kit.codeBug();
            }
            boolean handlerResult = _handlerResult != Undefined.instance && (boolean) _handlerResult;

            if (hasProperty(target, key)) {
                int attributes = target.getAttributes(key);

                if ((attributes & PERMANENT) != 0 && !handlerResult) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.has.non.configurable", key);
                } else if (!target.isExtensible() && !handlerResult) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.has.non.extensible", key);
                }
            }

            return handlerResult;
        }

    }

    @Override
    public void delete(String name) {
        ensureNotRevoked();

        if (!handlerIds.contains("deleteProperty")) {
            target.delete(name);
            return;
        }

        Object deleteProperty = handler.get("deleteProperty");

        if (deleteProperty == null) {
            return;
        } else if (!(deleteProperty instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "deleteProperty");
        }

        Object _result = ((Function) deleteProperty).call(Context.getContext(), this, this, new Object[]{ target, name });

        if (!(_result instanceof Boolean) && _result != Undefined.instance) {
            // TODO: error
            throw Kit.codeBug();
        }
        boolean result = _result != Undefined.instance && (boolean) _result;

        if (result && hasProperty(target, name)) {
            int attributes = target.getAttributes(name);

            if ((attributes & PERMANENT) != 0) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.delete", name);
            }
        }
    }

    @Override
    public void delete(Symbol key) {
        ensureNotRevoked();

        if (!handlerIds.contains("deleteProperty")) {
            target.delete(key);
            return;
        }


        Object deleteProperty = handler.get("deleteProperty");

        if (deleteProperty == null) {
            return;
        } else if (!(deleteProperty instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "deleteProperty");
        }

        Object _result = ((Function) deleteProperty).call(Context.getContext(), this, this, new Object[]{ target, key });

        if (!(_result instanceof Boolean) && _result != Undefined.instance) {
            // TODO: error
            throw Kit.codeBug();
        }
        boolean result = _result != Undefined.instance && (boolean) _result;

        if (result && hasProperty(target, key)) {
            int attributes = target.getAttributes(key);

            if ((attributes & PERMANENT) != 0) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.delete", key);
            }
        }
    }

    @Override
    public ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        ensureNotRevoked();

        if (!handlerIds.contains("getOwnPropertyDescriptor")) {
            return target.getOwnPropertyDescriptor(cx, id);
        }

        Object getOwnPropertyDescriptor = handler.get("getOwnPropertyDescriptor");

        if (getOwnPropertyDescriptor == null) {
            return null;
        } else if (!(getOwnPropertyDescriptor instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "getOwnPropertyDescriptor");
        }

        Object _result = ((Function) getOwnPropertyDescriptor).call(cx, this, this, new Object[]{ target, id });

        // Invariant 1
        if (_result != Undefined.instance && !(_result instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv1", ScriptRuntime.toString(id));
        }

        int targetDesc;
        boolean exists;

        if (id instanceof String) {
            exists = hasProperty(target, (String) id);
            targetDesc = exists ? target.getAttributes((String) id) : -1;
        } else if (ScriptRuntime.isSymbol(id)) {
            exists = hasProperty(target, (Symbol) id);
            targetDesc = exists ? target.getAttributes((Symbol) id) : -1;
        } else {
            throw Kit.codeBug();
        }

        // invariant 2
        if (_result == Undefined.instance && exists && (targetDesc & PERMANENT) != 0) {
            throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv2", ScriptRuntime.toString(id));
        }

        // invariant 3
        if (_result == Undefined.instance && exists && !target.isExtensible()) {
            throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv3", ScriptRuntime.toString(id));
        }

        if (_result == Undefined.instance) {
            return null;
        }

        // Invariant 4
        if (!exists && !target.isExtensible()) {
            throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv4", ScriptRuntime.toString(id));
        }

        ScriptableObject handlerDesc = (ScriptableObject) _result;
        boolean configurable = isConfigurable(handlerDesc);

        // Invariant 5
        if (!configurable) {
            if (!exists) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv5.non.existant", ScriptRuntime.toString(id));
            } else if ((targetDesc & PERMANENT) == 0) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv5.existant", ScriptRuntime.toString(id));
            }
        }

        if (!hasProperty(handlerDesc, "configurable")) {
            putProperty(handlerDesc, "configurable", false);
        }
        if (!hasProperty(handlerDesc, "writable")) {
            putProperty(handlerDesc, "writable", false);
        }
        if (!hasProperty(handlerDesc, "enumerable")) {
            putProperty(handlerDesc, "enumerable", false);
        }

        return handlerDesc;
    }

    @Override
    public void defineOwnProperty(Context cx, Object id, ScriptableObject desc) {
        ensureNotRevoked();

        if (!handlerIds.contains("defineProperty")) {
            target.defineOwnProperty(cx, id, desc);
            return;
        }

        Object defineProperty = handler.get("defineProperty");

        if (defineProperty == null) {
            return;
        } else if (!(defineProperty instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "defineOwnProperty");
        }

        Object result = ((Function) defineProperty).call(cx, this, this, new Object[]{ target, id, desc });

        if (result instanceof Boolean && !((boolean) result)) {
            throw ScriptRuntime.typeError1("msg.proxy.invariant.defineprop.returned.false", ScriptRuntime.toString(id));
        }

        // Invariant 1
        if (!target.isExtensible()) {
            throw ScriptRuntime.typeError1("msg.proxy.invariant.defineprop.not.extensible", ScriptRuntime.toString(id));
        }

        // Invariant 2
        boolean isConfigurable = isConfigurable(desc);

        if (!isConfigurable) {
            int attributes = getAttributes(target, id);

            if (attributes != -1 && (attributes & PERMANENT) == 0) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.defineprop.non.configurable", ScriptRuntime.toString(id));
            }
        }
    }

    @Override
    public Scriptable getPrototype() {
        ensureNotRevoked();

        if (!handlerIds.contains("getPrototypeOf")) {
            return target.getPrototype();
        }

        Object getPrototypeOf = handler.get("getPrototypeOf");

        if (getPrototypeOf == null) {
            return null;
        } else if (!(getPrototypeOf instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "getPrototypeOf");
        }

        Object result = ((Function) getPrototypeOf).call(Context.getContext(), this, this, new Object[]{ target });

        if (result != null && !(result instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError0("msg.proxy.invariant.getproto.invalid.return");
        } else if (!target.isExtensible() && result != target.getPrototype()) {
            throw ScriptRuntime.typeError0("msg.proxy.invariant.getproto.non.extensible");
        }

        return (ScriptableObject) result;
    }

    @Override
    public void setPrototype(Scriptable m) {
        ensureNotRevoked();

        if (!handlerIds.contains("setPrototypeOf")) {
            target.setPrototype(m);
            return;
        }

        Object setPrototypeOf = handler.get("setPrototypeOf");

        if (setPrototypeOf == null) {
            return;
        } else if (!(setPrototypeOf instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "setPrototypeOf");
        }

        Object result = ((Function) setPrototypeOf).call(Context.getContext(), this, this, new Object[]{ target, m });

        if (result instanceof Boolean && (boolean) result && !target.isExtensible() && m != target.getPrototype()) {
            throw ScriptRuntime.typeError0("msg.proxy.invariant.setproto.invalid.parameter");
        }
    }

    @Override
    public boolean isExtensible() {
        ensureNotRevoked();

        if (!handlerIds.contains("isExtensible")) {
            return target.isExtensible();
        }

        Object isExtensible = handler.get("isExtensible");

        if (isExtensible == null) {
            return false;
        } else if (!(isExtensible instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "isExtensible");
        }

        Object result = ((Function) isExtensible).call(Context.getContext(), this, this, new Object[]{ target });

        if (!(result instanceof Boolean) || ((boolean) result) != target.isExtensible()) {
            throw ScriptRuntime.typeError0("msg.proxy.invariant.isextensible");
        }

        return (boolean) result;
    }

    @Override
    public void preventExtensions() {
        ensureNotRevoked();

        if (!handlerIds.contains("preventExtensions")) {
            target.preventExtensions();
        }

        Object preventExtensions = handler.get("preventExtensions");

        if (preventExtensions == null) {
            return;
        } else if (!(preventExtensions instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "preventExtensions");
        }

        Object result = ((Function) preventExtensions).call(Context.getContext(), this, this, new Object[]{ target });

        if (result instanceof Boolean && (boolean) result && this.isExtensible()) {
            throw ScriptRuntime.typeError0("msg.proxy.invariant.prevent.extensible");
        }
    }

    @Override
    public Object[] getIds() {
        ensureNotRevoked();

        if (!handlerIds.contains("ownKeys")) {
            return target.getIds();
        }

        Object ownKeys = handler.get("ownKeys");

        if (!(ownKeys instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "setPrototypeOf");
        }

        Object result = ((Function) ownKeys).call(Context.getContext(), this, this, new Object[]{ target });

        if (!(result instanceof NativeArray)) {
            throw ScriptRuntime.typeError0("msg.proxy.invariant.ownkeys.invalid.array");
        }

        Object[] resultIds = ((NativeArray) result).toArray();
        boolean extensible = target.isExtensible();
        List<Object> targetIds = Arrays.asList(target.getIds());

        for (Object resultId : resultIds) {
            if (!(resultId instanceof String) && !ScriptRuntime.isSymbol(resultId)) {
                throw ScriptRuntime.typeError0("msg.proxy.invariant.ownkeys.invalid.array");
            }

            if (!extensible && !targetIds.contains(resultId)) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.ownkeys.invalid.element", ScriptRuntime.toString(resultId));
            }

            targetIds.remove(resultId);
        }

        for (Object targetId : targetIds) {
            int attributes = getAttributes(target, targetId);

            if (attributes != -1 && (attributes & PERMANENT) != 0) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.ownkeys.skip.prop", ScriptRuntime.toString(targetId));
            }
        }

        return resultIds;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ensureNotRevoked();

        if (!handlerIds.contains("apply")) {
            if (target instanceof Callable)
                return ((Callable) target).call(cx, scope, target, args);
            throw ScriptRuntime.typeError0("msg.proxy.not.callable");
        }

        if (!(target instanceof Callable)) {
            throw ScriptRuntime.typeError0("msg.proxy.target.not.callable");
        }

        Object apply = handler.get("apply");

        if (!(apply instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "call");
        }

        return ((Function) apply).call(cx, scope, thisObj, new Object[]{ target, thisObj, cx.newArray(scope, args) });
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        ensureNotRevoked();

        if (!handlerIds.contains("construct")) {
            if (target instanceof BaseFunction)
                return ((BaseFunction) target).construct(cx, scope, args);
            throw ScriptRuntime.typeError0("msg.proxy.not.constructable");
        }

        if (!(target instanceof Function)) {
            throw ScriptRuntime.typeError0("msg.proxy.target.not.constructable");
        }

        Object construct = handler.get("construct");

        if (construct == null) {
            return null;
        } else if (!(construct instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", "preventExtensions");
        }

        Object result = ((Function) construct).call(cx, scope, this, new Object[]{ target, cx.newArray(scope, args), this });

        if (!(result instanceof ScriptableObject)) {
            throw ScriptRuntime.typeError0("msg.proxy.result.not.constructable");
        }

        return (ScriptableObject) result;
    }

    public int getArity() {
        ensureNotRevoked();

        if (handlerIds.contains("construct")) {
            return ((BaseFunction) handler.get("construct")).getArity();
        } else if (handlerIds.contains("apply")) {
            return ((BaseFunction) handler.get("apply")).getArity();
        } else if (target instanceof BaseFunction) {
            return ((BaseFunction) target).getArity();
        }

        throw ScriptRuntime.typeError0("msg.proxy.not.callable");
    }

    private static int getAttributes(ScriptableObject obj, Object id) {
        if (id instanceof String) {
            String s = (String) id;
            if (!hasProperty(obj, s)) return -1;
            return obj.getAttributes(s);
        } else if (ScriptRuntime.isSymbol(id)) {
            Symbol s = (Symbol) id;
            if (!hasProperty(obj, s)) return -1;
            return obj.getAttributes(s);
        } else {
            throw Kit.codeBug();
        }
    }

    private static boolean isConfigurable(ScriptableObject desc) {
        Object result = hasProperty(desc, "configurable") ? getProperty(desc, "configurable") : false;

        if (result instanceof Boolean)
            return (boolean) result;
        return false;
    }

}
