package org.mozilla.javascript;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class NativePromise extends IdScriptableObject {
    private static final String PROMISE_TAG = "Promise";

    private CompletableFuture<Object> _future;
    private List<CompletableFuture<Object>> _futures;

    private PromiseState state = PromiseState.PENDING;

    enum PromiseState {
        PENDING,
        FULFILLED,
        REJECTED
    }

    static void init(Scriptable scope, boolean sealed) {
        NativePromise obj = new NativePromise();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    private NativePromise() {
    }

    @Override
    public String getClassName() {
        return PROMISE_TAG;
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        addIdFunctionProperty(ctor, PROMISE_TAG, ConstructorId_all, "all", 1);
        addIdFunctionProperty(ctor, PROMISE_TAG, ConstructorId_allSettled, "allSettled", 1);
        addIdFunctionProperty(ctor, PROMISE_TAG, ConstructorId_any, "any", 1);
        addIdFunctionProperty(ctor, PROMISE_TAG, ConstructorId_race, "race", 1);
        addIdFunctionProperty(ctor, PROMISE_TAG, ConstructorId_resolve, "resolve", 1);
        addIdFunctionProperty(ctor, PROMISE_TAG, ConstructorId_reject, "reject", 1);

        addCtorSpecies(ctor);

        super.fillConstructorProperties(ctor);
    }

    @Override
    protected void initPrototypeId(int id) {
        switch (id) {
            case SymbolId_toStringTag:
                initPrototypeValue(
                    SymbolId_toStringTag,
                    SymbolKey.TO_STRING_TAG,
                    getClassName(),
                    NOT_ENUMERABLE | NOT_WRITABLE
                );
                return;
            // fallthrough
        }

        String s;
        int arity;

        switch (id) {
            case Id_constructor:
                s = "constructor";
                arity = 1;
                break;
            case Id_then:
                s = "then";
                arity = 2;
                break;
            case Id_catch:
                s = "catch";
                arity = 1;
                break;
            case Id_finally:
                s = "finally";
                arity = 1;
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }

        initPrototypeMethod(PROMISE_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(PROMISE_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        switch (f.methodId()) {
            case Id_constructor:
                return js_construct(scope, args);
            case Id_then:
                return js_then(cx, scope, thisObj, args);
            case Id_catch:
                return js_catch(cx, scope, thisObj, args);
            case Id_finally:
                return js_finally(cx, scope, thisObj, args);
            case ConstructorId_all:
            case ConstructorId_allSettled:
            case ConstructorId_any:
            case ConstructorId_race:
            case ConstructorId_reject:
            case ConstructorId_resolve:
            default:
                throw new IllegalArgumentException(String.valueOf(f.methodId()));
        }
    }

    private NativePromise js_construct(Scriptable scope, Object[] args) {
        NativePromise promise = new NativePromise();

        if (args.length > 0 && args[0] instanceof CompletableFuture) {
            promise._future = (CompletableFuture<Object>) args[0];

            if (args.length > 1) {
                promise._futures = (List<CompletableFuture<Object>>) args[1];
            } else {
                promise._futures = Collections.emptyList();
            }
        } else {
            if (args.length == 0 || !(args[0] instanceof Function)) {
                throw ScriptRuntime.typeError0("msg.promise.missing.resolver");
            }

            Function resolver = (Function) args[0];


            promise._future = CompletableFuture.supplyAsync(() -> {
                synchronized (promise) {
                    try {
                        Context newCx = Context.enter();

                        AtomicReference<PromiseState> state = new AtomicReference<>(PromiseState.PENDING);
                        AtomicReference<Object> result = new AtomicReference<>(null);

                        resolver.call(newCx, scope, null, new Object[]{
                            BaseFunction.wrap((_cx, _scope, _thisObj, _args) -> {
                                state.set(PromiseState.FULFILLED);
                                result.set(_args.length > 0 ? _args[0] : Undefined.instance);
                                return null;
                            }),
                            BaseFunction.wrap((_cx, _scope, _thisObj, _args) -> {
                                state.set(PromiseState.REJECTED);
                                result.set(_args.length > 0 ? _args[0] : Undefined.instance);
                                return null;
                            })
                        });

                        while (state.get() == PromiseState.PENDING) ;

                        if (state.get() == PromiseState.REJECTED) {
                            throw new JavaScriptException(result.get());
                        }

                        Scriptable obj = new NativeObject();
                        ScriptableObject.putProperty(obj, "result", result.get());
                        return obj;
                    } finally {
                        Context.exit();
                    }
                }
            });
        }

        return promise;
    }

    private Object js_finally(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!(thisObj instanceof NativePromise)) {
            throw Kit.codeBug("Expected Promise.then thisObj to be a NativePromise");
        }

        NativePromise thisPromise = (NativePromise) thisObj;

        Object onFinally = args.length > 0 ? args[0] : null;

        return constructPromise(cx, scope, thisObj, new Object[]{
            thisPromise._future.handle((success, error) -> {
                synchronized (thisPromise) {
                    try {
                        Context newCx = Context.enter();

                        if (onFinally instanceof Callable) {
                            ((Callable) onFinally).call(newCx, scope, thisObj, new Object[0]);
                        }

                        return Undefined.instance;
                    } finally {
                        Context.exit();
                    }
                }
            })
        });
    }

    private Object js_catch(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return js_then(cx, scope, thisObj, new Object[]{ null, args.length > 0 ? args[0] : null });
    }

    private Object js_then(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!(thisObj instanceof NativePromise)) {
            throw Kit.codeBug("Expected Promise.then thisObj to be a NativePromise");
        }

        NativePromise thisPromise = (NativePromise) thisObj;

        Object onFulfillment = args.length > 0 ? args[0] : null;
        Object onRejection = args.length > 1 ? args[1] : null;

        return constructPromise(cx, scope, thisObj, new Object[]{
            thisPromise._future.handle((success, error) -> {
                synchronized (thisPromise) {
                    try {
                        Context newCx = Context.enter();

                        if (success == null && error == null && thisPromise._futures != null) {
                            success = newCx.newObject(scope);
                            ScriptableObject.putProperty(
                                (Scriptable) success,
                                "result",
                                thisPromise._futures.stream().map(
                                    future -> {
                                        try {
                                            return ScriptableObject.getProperty(
                                                ScriptableObject.ensureScriptable(future.get()),
                                                "result"
                                            );
                                        } catch (InterruptedException | ExecutionException e) {
                                            throw new WrappedException(e);
                                        }
                                    }
                                ).collect(Collectors.toList())
                            );
                        }

                        Function reject = BaseFunction.wrap((_cx, _scope, _thisObj, _args) -> {
                            Object reason = _args[0];

                            if (onRejection instanceof Function) {
                                try {
                                    while (reason instanceof Throwable) {
                                        if (reason instanceof WrappedException) {
                                            reason = ((WrappedException) reason).getWrappedException();
                                        } else if (reason instanceof CompletionException) {
                                            reason = ((CompletionException) reason).getCause();
                                        } else if (reason instanceof JavaScriptException) {
                                            reason = ((JavaScriptException) reason).getValue();
                                        }
                                    }

                                    Scriptable obj = _cx.newObject(scope);
                                    ScriptableObject.putProperty(
                                        obj,
                                        "result",
                                        ((Function) onRejection).call(_cx, _scope, _thisObj, new Object[]{ reason })
                                    );

                                    return obj;
                                } catch (Exception e) {
                                    throw new CompletionException(e);
                                }
                            }

                            throw new JavaScriptException(reason);
                        });

                        if (success != null) {
                            if (onFulfillment instanceof Function) {
                                try {
                                    Object value = success instanceof Scriptable
                                        ? ScriptableObject.getProperty((Scriptable) success, "result")
                                        : Undefined.instance;

                                    Scriptable obj = newCx.newObject(scope);

                                    Object arg = value instanceof NativePromise
                                        ? ScriptableObject.getProperty(ScriptableObject.ensureScriptable(
                                        ((NativePromise) value)._future.get()
                                    ), "result")
                                        : value;

                                    Object newVal = ((Function) onFulfillment).call(
                                        newCx, scope, thisObj, new Object[]{ arg }
                                    );

                                    ScriptableObject.putProperty(obj, "result", newVal);

                                    return obj;
                                } catch (Exception e) {
                                    // TODO: return this?
                                    reject.call(newCx, scope, thisObj, new Object[]{ e });
                                }
                            }

                            return success;
                        } else if (error != null) {
                            try {
                                return reject.call(newCx, scope, thisObj, new Object[]{ error });
                            } catch (Exception e) {
                                throw new CompletionException(e);
                            }
                        }

                        throw Kit.codeBug("Unexpected end of Promise then handler");
                    } finally {
                        Context.exit();
                    }
                }
            })
        });
    }

    private Object constructPromise(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        BaseFunction species = getSpecies(thisObj);

        if (species == null) {
            throw Kit.codeBug();
        }

        return species.construct(cx, scope, args);
    }

    @Override
    protected int findPrototypeId(Symbol key) {
        if (SymbolKey.TO_STRING_TAG.equals(key)) {
            return SymbolId_toStringTag;
        }

        return 0;
    }

    // #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2020-02-11 08:35:44 CST
        L0:
        {
            id = 0;
            String X = null;
            L:
            switch (s.length()) {
                case 4:
                    X = "then";
                    id = Id_then;
                    break L;
                case 5:
                    X = "catch";
                    id = Id_catch;
                    break L;
                case 7:
                    X = "finally";
                    id = Id_finally;
                    break L;
                case 11:
                    X = "constructor";
                    id = Id_constructor;
                    break L;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
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
        Id_then = 2,
        Id_catch = 3,
        Id_finally = 4,

    SymbolId_toStringTag = 5,

    MAX_PROTOTYPE_ID = SymbolId_toStringTag;

// #/string_id_map#
}
