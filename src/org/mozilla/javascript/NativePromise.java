package org.mozilla.javascript;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class NativePromise extends IdScriptableObject {
    private static final String PROMISE_TAG = "Promise";

    private PromiseState state = PromiseState.PENDING;
    private CompletableFuture<?> future = null;
    private List<CompletableFuture<?>> futures = null;

    enum PromiseState {
        PENDING,
        FULFILLED,
        REJECTED
    }

    static void init(Scriptable scope, boolean sealed) {
        NativePromise obj = new NativePromise();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    public String getClassName() {
        return PROMISE_TAG;
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        addCtorSpecies(ctor);
    }

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;

        switch (id) {
            case ConstructorId_all:
                s = "all";
                arity = 1;
                break;
            case ConstructorId_allSettled:
                s = "allSettled";
                arity = 1;
                break;
            case ConstructorId_any:
                s = "any";
                arity = 1;
                break;
            case ConstructorId_race:
                s = "race";
                arity = 1;
                break;
            case ConstructorId_reject:
                s = "reject";
                arity = 1;
                break;
            case ConstructorId_resolve:
                s = "resolve";
                arity = 1;
                break;
            case Id_constructor:
                s = "constructor";
                arity = 1;
                break;
            case Id_catch:
                s = "catch";
                arity = 1;
                break;
            case Id_finally:
                s = "finally";
                arity = 1;
                break;
            case Id_then:
                s = "then";
                arity = 2;
                break;
            default:
                throw Kit.codeBug();
        }

        initPrototypeMethod(PROMISE_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(PROMISE_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        switch (f.methodId()) {
            case ConstructorId_all:
                return js_all(cx, scope, thisObj, args);
            case ConstructorId_allSettled:
                return js_allSettled(cx, scope, thisObj, args);
            case ConstructorId_any:
                return js_any(cx, scope, thisObj, args);
            case ConstructorId_race:
                return js_race(cx, scope, thisObj, args);
            case ConstructorId_reject:
                return js_reject(cx, scope, thisObj, args);
            case ConstructorId_resolve:
                return js_resolve(cx, scope, thisObj, args);
            case Id_constructor:
                return js_construct(cx, scope, thisObj, args);
            case Id_catch:
                return js_catch(cx, scope, thisObj, args);
            case Id_finally:
                return js_finally(cx, scope, thisObj, args);
            case Id_then:
                return js_then(cx, scope, thisObj, args);
        }

        return null;
    }

    private NativePromise ensureCorrectProto(Scriptable thisObj, String methodName) {
        if (!(thisObj instanceof NativePromise) || ((NativePromise) thisObj).future == null || getSpecies(thisObj) == null) {
            throw ScriptRuntime.typeError1("msg.incompat.call", methodName);
        }

        return ((NativePromise) thisObj);
    }

    private Object js_construct(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg0 = args.length > 0 ? args[0] : null;
        Object arg1 = args.length > 1 ? args[1] : null;
        NativePromise promise = new NativePromise();

        if (arg0 instanceof CompletableFuture) {
            promise.future = (CompletableFuture) arg0;

            if (arg1 instanceof List) {
                promise.futures = (List<CompletableFuture<?>>) arg1;
            }

            return promise;
        } else if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.isnt.function", ScriptRuntime.toString(arg0));
        }

        Callable resolver = (Callable) arg0;

        promise.future = CompletableFuture.supplyAsync((Supplier<Object>) () -> {
            synchronized (promise) {
                try {
                    Context.enter();

                    final PromiseState[] status = {PromiseState.PENDING};
                    final Object[] result = {null};

                    resolver.call(Context.getContext(), scope, this, new Object[]{
                            new BaseFunction() {
                                @Override
                                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                    status[0] = PromiseState.FULFILLED;
                                    result[0] = args.length > 0 ? args[0] : null;
                                    return null;
                                }
                            },
                            new BaseFunction() {
                                @Override
                                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                    status[0] = PromiseState.REJECTED;
                                    result[0] = args.length > 0 ? args[0] : null;
                                    return null;
                                }
                            }
                    });

                    while (status[0] == PromiseState.PENDING) { }

                    if (status[0] == PromiseState.FULFILLED) {
                        NativeObject obj = new NativeObject();
                        ScriptableObject.putProperty(obj, "result", result[0]);
                        return obj;
                    }

                    throw ScriptRuntime.throwCustomError(cx, scope, "Error", ScriptRuntime.toString(result[0]));
                } finally {
                    Context.exit();
                }
            }
        });

        return promise;
    }

    private Object js_catch(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ensureCorrectProto(thisObj, "catch");
        return js_then(cx, scope, thisObj, new Object[]{ null, args.length > 0 ? args[0] : null });
    }

    private Object js_finally(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return null;
    }

    private Object js_then(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativePromise promise = ensureCorrectProto(thisObj, "then");
        Callable onFulfillment = null;
        Callable onRejection = null;

        if (args.length > 0 && args[0] instanceof Callable) {
            onFulfillment = (Callable) args[0];
        }

        if (args.length > 1 && args[1] instanceof Callable) {
            onRejection = (Callable) args[1];
        }

        final Callable finalOnFulfillment = onFulfillment;
        Callable finalOnRejection = onRejection;
        CompletableFuture future = promise.future.handle((success, error) -> {
            try {
                Context.enter();
                Object ret;

                if (success == null && error == null && promise.futures != null) {
                    ret = new NativeObject();
                    ScriptableObject.putProperty((Scriptable) ret, "result", promise.futures.stream().map(f -> {
                        try {
                            Object get = f.get();
                            if (!(get instanceof Scriptable)) {
                                throw Kit.codeBug();
                            }

                            return ScriptableObject.getProperty((Scriptable) get, "result");
                        } catch (Exception e) {
                            throw Kit.codeBug(e.getMessage());
                        }
                    }));
                }

                if (success != null) {
                    if (finalOnFulfillment != null) {
                        try {
                            Object value = success instanceof Scriptable ? ScriptableObject.getProperty((Scriptable) success, "result") : Undefined.instance;
                            NativeObject obj = new NativeObject();

                            if (value instanceof NativePromise) {
                                Scriptable futureResult = (Scriptable) ((NativePromise) value).future.get();
                                Object[] newArgs = new Object[]{ScriptableObject.getProperty(futureResult, "result")};
                                ScriptableObject.putProperty(obj, "result", finalOnFulfillment.call(cx, scope, thisObj, newArgs));
                            } else {
                                ScriptableObject.putProperty(obj, "result", finalOnFulfillment.call(cx, scope, thisObj, new Object[]{value}));
                            }

                            return obj;
                        } catch (JavaScriptException e) {
                            thenRejectHelper(cx, scope, thisObj, e.getValue(), finalOnRejection);
                        } catch (Exception e) {
                            throw Kit.codeBug(e.getMessage());
                        }
                    }

                    return success;
                } else if (error != null) {
                    Object reason = ((JavaScriptException) error.getCause()).getValue();
                    return thenRejectHelper(cx, scope, thisObj, reason, finalOnRejection);
                }

                return Undefined.instance;
            } finally {
                Context.exit();
            }
        });

        return constructPromise(cx, scope, thisObj, new Object[]{ future });
    }

    private Object thenRejectHelper(Context cx, Scriptable scope, Scriptable thisObj, Object reason, Object onRejection) {
        if (onRejection instanceof Callable) {
            try {
                NativeObject obj = new NativeObject();
                ScriptableObject.putProperty(obj, "result", ((Callable) onRejection).call(cx, scope, thisObj, new Object[]{ reason }));
            } catch (Exception e) {
                throw ScriptRuntime.throwError(Context.getContext(), scope, e.getMessage());
            }
        }

        throw new JavaScriptException(reason, null, 0);
    }

    private Object js_all(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return null;
    }

    private Object js_allSettled(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return null;
    }

    private Object js_any(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return null;
    }

    private Object js_race(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return null;
    }

    private Object js_reject(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return constructPromise(cx, scope, thisObj, new Object[]{
                new BaseFunction() {
                    @Override
                    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] innerArgs) {
                        ((Callable) innerArgs[1]).call(cx, scope, thisObj, args);
                        return Undefined.instance;
                    }
                }
        });
    }

    private Object js_resolve(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg0 = args.length > 0 ? args[0] : null;

        if (arg0 instanceof NativePromise) {
            return arg0;
        } else if ((arg0 instanceof Scriptable) && ScriptableObject.hasProperty((Scriptable) arg0, "then")) {
            Object then = ScriptableObject.getProperty((Scriptable) arg0, "then");

            if (then instanceof Callable) {
                return constructPromise(cx, scope, thisObj, new Object[]{
                        new BaseFunction() {
                            @Override
                            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                                BaseFunction fulfill = (BaseFunction) args[0];
                                BaseFunction reject = (BaseFunction) args[1];

                                try {
                                    Object result = ((Callable) then).call(cx, scope, thisObj, new Object[]{ fulfill, reject });
                                    NativeObject obj = new NativeObject();
                                    ScriptableObject.putProperty(obj, "result", result);
                                    return obj;
                                } catch (Exception e) {
                                    throw ScriptRuntime.throwCustomError(cx, scope, "Error", "TODO");
                                }
                            }
                        }
                });
            }
        }

        return constructPromise(cx, scope, thisObj, new Object[]{ CompletableFuture.completedFuture(arg0) });
    }

    private Object constructPromise(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        BaseFunction species = getSpecies(thisObj);

        if (species == null) {
            throw Kit.codeBug();
        }

        return species.construct(cx, scope, args);
    }

    private static BaseFunction getSpecies(Scriptable obj) {
        if (obj instanceof NativeFunction) {
            // If obj is an instance of a class, we need
            // to access the constructor property of the
            // base class, not the instance
            obj = obj.getPrototype();
        }

        if (ScriptableObject.hasProperty(obj, "constructor")) {
            // If the constructor is a getter, we want to return null
            // and make sure to NOT access the constructor, as a
            // get constructor does not influence Symbol.species
            if (obj instanceof ScriptableObject) {
                ScriptableObject ctorDesc = ((ScriptableObject) obj).getOwnPropertyDescriptor(Context.getContext(), "constructor");

                if (ctorDesc != null && ScriptableObject.hasProperty(ctorDesc, "get") && !Undefined.isUndefined(ScriptableObject.getProperty(ctorDesc, "get"))) {
                    return null;
                }
            }

            Object ctorObj = ScriptableObject.getProperty(obj, "constructor");

            if (Undefined.isUndefined(ctorObj)) {
                return null;
            }

            Scriptable ctor = ScriptableObject.ensureScriptable(ctorObj);

            if (ScriptableObject.hasProperty(ctor, SymbolKey.SPECIES)) {
                Object species = ScriptableObject.getProperty(ctor, SymbolKey.SPECIES);

                if (Undefined.isUndefined(species) || species == null) {
                    return null;
                }

                if (!(species instanceof BaseFunction)) {
                    // TODO: Error
                    throw Kit.codeBug();
                }

                return (BaseFunction) species;
            }
        }

        return null;
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        if (typeHint == ScriptRuntime.StringClass) {
            return "[object Promise]";
        }
        return super.getDefaultValue(typeHint);
    }

    // #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2019-12-09 19:31:36 CST
        L0: { id = 0; String X = null;
            L: switch (s.length()) {
            case 4: X="then";id=Id_then; break L;
            case 5: X="catch";id=Id_catch; break L;
            case 7: X="finally";id=Id_finally; break L;
            case 11: X="constructor";id=Id_constructor; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
            ConstructorId_all = -1,
            ConstructorId_allSettled = -2,
            ConstructorId_any = -3,
            ConstructorId_race = -4,
            ConstructorId_reject = -5,
            ConstructorId_resolve = -6,

            Id_constructor = 1,
            Id_catch = 2,
            Id_finally = 3,
            Id_then = 4,

            MAX_PROTOTYPE_ID = Id_then;
// #/string_id_map#
}
