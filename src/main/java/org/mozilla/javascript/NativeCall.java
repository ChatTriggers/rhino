/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the activation object.
 * <p>
 * See ECMA 10.1.6
 *
 * @author Norris Boyd
 * @see org.mozilla.javascript.Arguments
 */
public final class NativeCall extends IdScriptableObject {
    private static final long serialVersionUID = -7471457301304454454L;

    private static final Object CALL_TAG = "Call";

    static void init(Scriptable scope, boolean sealed) {
        NativeCall obj = new NativeCall();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    NativeCall() {
    }

    NativeCall(NativeFunction function, Scriptable scope, Object[] callArgs,
               Object[] effectiveArgs, boolean isArrow, boolean isStrict, boolean syncArgumentsObj) {
        this.function = function;

        setParentScope(scope);
        // leave prototype null

        this.syncArgumentsObj = !isStrict && syncArgumentsObj;
        this.callArgs = (callArgs == null) ? ScriptRuntime.emptyArgs : callArgs;
        this.effectiveArgs = (effectiveArgs == null) ? ScriptRuntime.emptyArgs : effectiveArgs;
        this.isStrict = isStrict;

        // initialize values of arguments
        int paramAndVarCount = function.getParamAndVarCount();
        int paramCount = function.getParamCount();
        if (paramAndVarCount != 0) {
            for (int i = 0; i < paramCount; ++i) {
                String name = function.getParamOrVarName(i);
                Object val = i < this.effectiveArgs.length ? this.effectiveArgs[i] : Undefined.instance;
                defineProperty(name, val, NOT_CONFIGURABLE);
            }
        }

        // initialize "arguments" property but only if it was not overridden by
        // the parameter with the same name
        if (!super.has("arguments", this) && !isArrow) {
            arguments = new Arguments(this);
            defineProperty("arguments", arguments, NOT_CONFIGURABLE);
        }

        if (paramAndVarCount != 0) {
            for (int i = paramCount; i < paramAndVarCount; ++i) {
                String name = function.getParamOrVarName(i);
                if (!super.has(name, this)) {
                    if (function.getParamOrVarConst(i)) {
                        defineProperty(name, Undefined.instance, CONST);
                    } else if (!(function instanceof InterpretedFunction)
                        || ((InterpretedFunction) function).hasFunctionNamed(name)) {
                        defineProperty(name, Undefined.instance, NOT_CONFIGURABLE);
                    }
                }
            }
        }
    }

    @Override
    public String getClassName() {
        return "Call";
    }

    @Override
    protected int findPrototypeId(String s) {
        return s.equals("constructor") ? Id_constructor : 0;
    }

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        if (id == Id_constructor) {
            arity = 1;
            s = "constructor";
        } else {
            throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(CALL_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(CALL_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        if (id == Id_constructor) {
            if (thisObj != null) {
                throw Context.reportRuntimeError1("msg.only.from.new", "Call");
            }
            ScriptRuntime.checkDeprecated(cx, "Call");
            NativeCall result = new NativeCall();
            result.setPrototype(getObjectPrototype(scope));
            return result;
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    /**
     * Bypasses the arguments object synchronization. This is called
     * from the activation to achieve two-way synchronization. If the
     * other method was called, it would cause a stack overflow.
     */
    public void putRaw(String name, Scriptable start, Object value) {
        super.put(name, start, value);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (this.syncArgumentsObj && arguments != null) {
            int index = -1;

            for (int i = 0; i < function.getParamCount() && index == -1; i++) {
                if (function.getParamOrVarName(i).equals(name)) {
                    index = i;
                }
            }

            if (index != -1) {
                ScriptableObject.putProperty(arguments, index, value);
            }
        }

        super.put(name, start, value);
    }

    public void defineAttributesForArguments() {
        if (arguments != null) {
            arguments.defineAttributesForStrictMode();
        }
    }

    private static final int
            Id_constructor = 1,
            MAX_PROTOTYPE_ID = 1;

    NativeFunction function;
    Object[] callArgs;
    Object[] effectiveArgs;
    boolean isStrict;
    boolean syncArgumentsObj;
    private Arguments arguments;

    transient NativeCall parentActivationCall;
}

