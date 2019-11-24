/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.generator;

import org.mozilla.javascript.*;

/**
 * This class implements generator objects. See
 * http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Generators
 *
 * @author Norris Boyd
 */
public final class NativeGenerator extends IdScriptableObject {
    private static final long serialVersionUID = 1645892441041347273L;

    private static final Object GENERATOR_TAG = "Generator";

    public static NativeGenerator init(ScriptableObject scope, boolean sealed) {
        // Generator
        // Can't use "NativeGenerator().exportAsJSClass" since we don't want
        // to define "Generator" as a constructor in the top-level scope.

        NativeGenerator prototype = new NativeGenerator();
        if (scope != null) {
            prototype.setParentScope(scope);
            prototype.setPrototype(getObjectPrototype(scope));
        }
        prototype.activatePrototypeMap(MAX_PROTOTYPE_ID);
        if (sealed) {
            prototype.sealObject();
        }

        // Need to access Generator prototype when constructing
        // Generator instances, but don't have a generator constructor
        // to use to find the prototype. Use the "associateValue"
        // approach instead.
        if (scope != null) {
            scope.associateValue(GENERATOR_TAG, prototype);
        }

        return prototype;
    }

    /**
     * Only for constructing the prototype object.
     */
    private NativeGenerator() {
    }

    public NativeGenerator(Scriptable scope, NativeFunction function,
                           Object savedState) {
        this.function = function;
        this.savedState = savedState;
        // Set parent and prototype properties. Since we don't have a
        // "Generator" constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        NativeGenerator prototype = (NativeGenerator)
                ScriptableObject.getTopScopeValue(top, GENERATOR_TAG);
        this.setPrototype(prototype);
    }

    public static final int GENERATOR_SEND = 0,
            GENERATOR_THROW = 1,
            GENERATOR_CLOSE = 2;

    @Override
    public String getClassName() {
        return "Generator";
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_iterator) {
            initPrototypeMethod(GENERATOR_TAG, id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
            return;
        }

        String s;
        int arity;
        switch (id) {
            case Id_next:
                arity = 1;
                s = "next";
                break;
            case Id_return:
                arity = 1;
                s = "return";
                break;
            case Id_throw:
                arity = 0;
                s = "throw";
                break;
            case Id___iterator__:
                arity = 1;
                s = "__iterator__";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(GENERATOR_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args) {
        if (!f.hasTag(GENERATOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        if (!(thisObj instanceof NativeGenerator))
            throw incompatibleCallError(f);

        NativeGenerator generator = (NativeGenerator) thisObj;

        switch (id) {
            case Id_next: {
                generator.firstTime = false;
                Object arg = args.length > 0 ? args[0] : Undefined.instance;
                return generator.resume(cx, scope, GENERATOR_SEND, arg);
            }

            case Id_return: {
                Object arg = args.length > 0 ? args[0] : Undefined.instance;
                return generator.resume(cx, scope, GENERATOR_CLOSE, arg);
            }

            case Id_throw: {
                return generator.resume(cx, scope, GENERATOR_THROW,
                        args.length > 0 ? args[0] : Undefined.instance);
            }

            case SymbolId_iterator:
                return new NativeGeneratorIterator(scope, (NativeGenerator) thisObj);

            case Id___iterator__:
                return thisObj;

            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    Object resume(Context cx, Scriptable scope, int operation, Object value) {
        if (savedState == null) {
            throw new JavaScriptException(value, lineSource, lineNumber);
        }

        if (operation == GENERATOR_CLOSE) {
            done = true;
        }

        try {
            synchronized (this) {
                // generator execution is necessarily single-threaded and
                // non-reentrant.
                // See https://bugzilla.mozilla.org/show_bug.cgi?id=349263
                if (locked)
                    throw ScriptRuntime.typeError0("msg.already.exec.gen");
                locked = true;
            }
            Object ret = function.resumeGenerator(cx, scope, operation, savedState, value);

            if (ret instanceof NativeObject && ScriptableObject.hasProperty((Scriptable) ret, "done")) {
                done = (Boolean) ScriptableObject.getProperty((Scriptable) ret, "done");
            }

            return ret;
        } catch (GeneratorClosedException e) {
            // On closing a generator in the compile path, the generator
            // throws a special exception. This ensures execution of all pending
            // finalizers and will not get caught by user code.
            return Undefined.instance;
        } catch (RhinoException e) {
            lineNumber = e.lineNumber();
            lineSource = e.lineSource();
            savedState = null;
            throw e;
        } finally {
            synchronized (this) {
                locked = false;
            }
        }
    }

    @Override
    protected int findPrototypeId(Symbol key) {
        if (SymbolKey.ITERATOR.equals(key)) {
            return SymbolId_iterator;
        }

        return 0;
    }

    // #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2019-11-24 16:11:10 CST
        L0: { id = 0; String X = null;
            L: switch (s.length()) {
            case 4: X="next";id=Id_next; break L;
            case 5: X="throw";id=Id_throw; break L;
            case 6: X="return";id=Id_return; break L;
            case 12: X="__iterator__";id=Id___iterator__; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
            Id_next = 1,
            Id_return = 2,
            Id_throw = 3,
            Id___iterator__ = 4,
            SymbolId_iterator = 5,
            MAX_PROTOTYPE_ID = 5;

    // #/string_id_map#
    private NativeFunction function;
    private Object savedState;
    private String lineSource;
    private int lineNumber;
    private boolean firstTime = true;
    private boolean locked;
    boolean done = false;

    public static class GeneratorClosedException extends RuntimeException {
        private static final long serialVersionUID = 2561315658662379681L;
    }
}
