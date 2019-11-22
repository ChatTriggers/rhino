package org.mozilla.javascript.proxy;

import org.mozilla.javascript.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mozilla.javascript.ScriptableObject.*;

@SuppressWarnings("DuplicatedCode")
public class NativeProxy extends IdScriptableObject {
    private static final long serialVersionUID = -5164128569432516845L;
    private static final Object PROXY_TAG = "Proxy";

    private ScriptableObject target;
    private ScriptableObject handler;
    private List<Object> handlerIds;

    public static void init(Scriptable scope, boolean sealed) {
        NativeProxyCtor ctor = new NativeProxyCtor();

        ctor.setParentScope(scope);
        ctor.setPrototype(null);
        ctor.defineProperty("prototype", Undefined.instance, ScriptableObject.DONTENUM);

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

    @Override
    public String getClassName() { return (String) PROXY_TAG; }

    @Override
    protected void initPrototypeId(int id) {
        throw new IllegalArgumentException(String.valueOf(id));
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(PROXY_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        throw new IllegalArgumentException(String.valueOf(id));
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (!handlerIds.contains("get")) {
            return super.get(name, start);
        }

        Object get = handler.get("get");

        if (get == null) {
            return null;
        } else if (!(get instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", name);
        }

        Function fn = (Function) get;
        Object result = fn.call(Context.getContext(), this, start, new Object[]{ target, name, start });

        if (ScriptableObject.hasProperty(target, name)) {
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
        if (!handlerIds.contains("get")) {
            return super.get(key, start);
        }

        Object get = handler.get("get");

        if (get == null) {
            return null;
        } else if (!(get instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", ScriptRuntime.toString(key));
        }

        Function fn = (Function) get;
        Object result = fn.call(Context.getContext(), this, start, new Object[]{ target, key, start });

        if (ScriptableObject.hasProperty(target, key)) {
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
        if (!handlerIds.contains("set")) {
            super.put(name, start, value);
        } else {
            if (ScriptableObject.hasProperty(target, name)) {
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
                throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", name);
            }

            ((Function) setter).call(Context.getContext(), this, start, new Object[]{target, name, value, start});
        }

    }

    @Override
    public void put(Symbol key, Scriptable start, Object value) {
        if (!handlerIds.contains("set")) {
            super.put(key, start, value);
        } else {
            if (ScriptableObject.hasProperty(target, key)) {
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
                throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", ScriptRuntime.toString(key));
            }

            ((Function) setter).call(Context.getContext(), this, start, new Object[]{target, key, value, start});
        }

    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (!handlerIds.contains("has")) {
            return super.has(name, start);
        }

        Object handlerHas = handler.get("has");

        if (handlerHas == null) {
            return false;
        } else if (!(handlerHas instanceof Function)) {
            throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", ScriptRuntime.toString(name));
        }

        Function fn = (Function) handlerHas;
        Object _handlerResult = fn.call(Context.getContext(), this, this, new Object[]{ target, name });

        if (!(_handlerResult instanceof Boolean) && _handlerResult != Undefined.instance) {
            // TODO: error
            throw Kit.codeBug();
        }
        boolean handlerResult = _handlerResult != Undefined.instance && (boolean) _handlerResult;

        if (ScriptableObject.hasProperty(target, name)) {
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
        if (!handlerIds.contains("has")) {
            return super.has(key, start);
        } else {
            Object handlerHas = handler.get("has");

            if (handlerHas == null) {
                return false;
            } else if (!(handlerHas instanceof Function)) {
                throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", ScriptRuntime.toString(key));
            }

            Function fn = (Function) handlerHas;
            Object _handlerResult = fn.call(Context.getContext(), this, this, new Object[]{target, key});

            if (!(_handlerResult instanceof Boolean) && _handlerResult != Undefined.instance) {
                // TODO: error
                throw Kit.codeBug();
            }
            boolean handlerResult = _handlerResult != Undefined.instance && (boolean) _handlerResult;

            if (ScriptableObject.hasProperty(target, key)) {
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
        if (!handlerIds.contains("deleteProperty")) {
            super.delete(name);
        } else {
            Object deleteProperty = handler.get("deleteProperty");

            if (deleteProperty == null) {
                return;
            } else if (!(deleteProperty instanceof Function)) {
                throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", ScriptRuntime.toString(name));
            }

            Object _result = ((Function) deleteProperty).call(Context.getContext(), this, this, new Object[]{ target, name });

            if (!(_result instanceof Boolean) && _result != Undefined.instance) {
                // TODO: error
                throw Kit.codeBug();
            }
            boolean result = _result != Undefined.instance && (boolean) _result;

            if (result && ScriptableObject.hasProperty(target, name)) {
                int attributes = target.getAttributes(name);

                if ((attributes & PERMANENT) != 0) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.delete", name);
                }
            }
        }
    }

    @Override
    public void delete(Symbol key) {
        if (!handlerIds.contains("deleteProperty")) {
            super.delete(key);
        } else {
            Object deleteProperty = handler.get("deleteProperty");

            if (deleteProperty == null) {
                return;
            } else if (!(deleteProperty instanceof Function)) {
                throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", ScriptRuntime.toString(key));
            }

            Object _result = ((Function) deleteProperty).call(Context.getContext(), this, this, new Object[]{ target, key });

            if (!(_result instanceof Boolean) && _result != Undefined.instance) {
                // TODO: error
                throw Kit.codeBug();
            }
            boolean result = _result != Undefined.instance && (boolean) _result;

            if (result && ScriptableObject.hasProperty(target, key)) {
                int attributes = target.getAttributes(key);

                if ((attributes & PERMANENT) != 0) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.delete", key);
                }
            }
        }
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        if (!handlerIds.contains("getOwnPropertyDescriptor")) {
            return super.getOwnPropertyDescriptor(cx, id);
        } else {
            Object getOwnPropertyDescriptor = handler.get("getOwnPropertyDescriptor");

            if (getOwnPropertyDescriptor == null) {
                return null;
            } else if (!(getOwnPropertyDescriptor instanceof Function)) {
                throw ScriptRuntime.typeError1("msg.proxy.invalid.handler", ScriptRuntime.toString(id));
            }

            Object _result = ((Function) getOwnPropertyDescriptor).call(cx, this, this, new Object[]{ target, id });

            // Invariant 1
            if (_result != Undefined.instance && !(_result instanceof ScriptableObject)) {
                throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv1", ScriptRuntime.toString(id));
            }

            int targetDesc;
            boolean exists;

            if (id instanceof String) {
                exists = ScriptableObject.hasProperty(target, (String) id);
                targetDesc = exists ? target.getAttributes((String) id) : -1;
            } else if (ScriptRuntime.isSymbol(id)) {
                exists = ScriptableObject.hasProperty(target, (Symbol) id);
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

            Object _configurable = handlerDesc.get("configurable");
            boolean configurable = _configurable instanceof Boolean && (boolean) _configurable;

            // Invariant 5
            if (!configurable) {
                if (!exists) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv5.non.existant", ScriptRuntime.toString(id));
                } else if ((targetDesc & PERMANENT) == 0) {
                    throw ScriptRuntime.typeError1("msg.proxy.invariant.getdescriptor.inv5.existant", ScriptRuntime.toString(id));
                }
            }

            if (!ScriptableObject.hasProperty(handlerDesc, "configurable")) {
                ScriptableObject.putProperty(handlerDesc, "configurable", false);
            }
            if (!ScriptableObject.hasProperty(handlerDesc, "writable")) {
                ScriptableObject.putProperty(handlerDesc, "writable", false);
            }
            if (!ScriptableObject.hasProperty(handlerDesc, "enumerable")) {
                ScriptableObject.putProperty(handlerDesc, "enumerable", false);
            }

            return handlerDesc;
        }
    }

    @Override
    public void defineOwnProperty(Context cx, Object key, ScriptableObject desc) {
        if (!handlerIds.contains("defineProperty")) {
            super.defineOwnProperty(cx, key, desc);
        } else {
            
        }
    }
}
