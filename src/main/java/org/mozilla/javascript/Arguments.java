/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.NativeArrayIterator.ARRAY_ITERATOR_TYPE;

/**
 * This class implements the "arguments" object.
 * <p>
 * See ECMA 10.1.8
 *
 * @author Norris Boyd
 * @see org.mozilla.javascript.NativeCall
 */
final class Arguments extends IdScriptableObject {
    private static final long serialVersionUID = 4275508002492040609L;

    private static final String FTAG = "Arguments";

    public Arguments(NativeCall activation) {
        this.activation = activation;

        Scriptable parent = activation.getParentScope();
        setParentScope(parent);
        setPrototype(ScriptableObject.getObjectPrototype(parent));

        Object[] origArgs = activation.callArgs;

        if (activation.function.hasRest() && activation.function.getParamCount() <= origArgs.length) {
            Object restObj = origArgs[origArgs.length - 1];

            if (!(restObj instanceof NativeArray))
                throw Kit.codeBug();

            NativeArray rest = (NativeArray) restObj;
            Object[] restItems = rest.toArray();
            args = new Object[origArgs.length - 1 + restItems.length];
            System.arraycopy(origArgs, 0, args, 0, origArgs.length - 1);
            System.arraycopy(restItems, 0, args, origArgs.length - 1, restItems.length);
        } else {
            args = origArgs;
        }

        lengthObj = args.length;

        NativeFunction f = activation.function;
        calleeObj = f;

        int version = f.getLanguageVersion();

        defineProperty(SymbolKey.ITERATOR, iteratorMethod, ScriptableObject.NOT_ENUMERABLE);
    }

    @Override
    public String getClassName() {
        return FTAG;
    }

    private Object arg(int index) {
        if (index < 0 || args.length <= index) return NOT_FOUND;
        return args[index];
    }

    // the following helper methods assume that 0 < index < args.length

    private void putIntoActivation(int index, Object value) {
        String argName = activation.function.getParamOrVarName(index);
        activation.put(argName, activation, value);
    }

    private Object getFromActivation(int index) {
        String argName = activation.function.getParamOrVarName(index);
        return activation.get(argName, activation);
    }

    private void replaceArg(int index, Object value) {
        if (activation.syncArgumentsObj) {
            activation.putRaw(activation.function.getParamOrVarName(index), activation, value);
        }

        synchronized (this) {
            if (args == activation.callArgs) {
                args = args.clone();
            }
            args[index] = value;
        }
    }

    private void removeArg(int index) {
        synchronized (this) {
            if (args[index] != NOT_FOUND) {
                if (args == activation.callArgs) {
                    args = args.clone();
                }
                args[index] = NOT_FOUND;
            }
        }
    }

    // end helpers

    @Override
    public boolean has(int index, Scriptable start) {
        if (arg(index) != NOT_FOUND) {
            return true;
        }
        return super.has(index, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        final Object value = arg(index);
        if (value == NOT_FOUND) {
            return super.get(index, start);
        }
        if (sharedWithActivation(index)) {
            return getFromActivation(index);
        }
        return value;
    }

    private boolean sharedWithActivation(int index) {
        // Sharing turns out to break the arguments object. Perhaps
        // there is a more elegant way to handle argument reassigning, but
        // never sharing seems to work.
        return false;
//        Context cx = Context.getContext();
//        if (cx.isStrictMode()) {
//            return false;
//        }
//        NativeFunction f = activation.function;
//        int definedCount = f.getParamAndVarCount();
//        if (index < definedCount) {
//            // Check if argument is not hidden by later argument with the same
//            // name as hidden arguments are not shared with activation
//            if (index < definedCount - 1) {
//                String argName = f.getParamOrVarName(index);
//                for (int i = index + 1; i < definedCount; i++) {
//                    if (argName.equals(f.getParamOrVarName(i))) {
//                        return false;
//                    }
//                }
//            }
//            return true;
//        }
//        return false;
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (arg(index) == NOT_FOUND) {
            super.put(index, start, value);
        } else {
            replaceArg(index, value);
        }
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        super.put(name, start, value);
    }

    @Override
    public void delete(int index) {
        if (0 <= index && index < args.length) {
            removeArg(index);
        }
        super.delete(index);
    }

// #string_id_map#

    private static final int
            Id_callee = 1,
            Id_length = 2,
            MAX_INSTANCE_ID = Id_length;

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
// #generated# Last update: 2020-03-03 10:45:13 CST
        L0: { id = 0; String X = null; int c;
            if (s.length()==6) {
                c=s.charAt(0);
                if (c=='c') { X="callee";id=Id_callee; }
                else if (c=='l') { X="length";id=Id_length; }
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        Context cx = Context.getContext();
        if (cx.isStrictMode()) {
            if (id == Id_callee) {
                return super.findInstanceIdInfo(s);
            }
        }


        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
            case Id_callee:
                attr = calleeAttr;
                break;
            case Id_length:
                attr = lengthAttr;
                break;
            default:
                throw new IllegalStateException();
        }
        return instanceIdInfo(attr, id);
    }

// #/string_id_map#

    @Override
    protected String getInstanceIdName(int id) {
        switch (id) {
            case Id_callee:
                return "callee";
            case Id_length:
                return "length";
        }
        return null;
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        switch (id) {
            case Id_callee:
                return calleeObj;
            case Id_length:
                return lengthObj;
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value) {
        switch (id) {
            case Id_callee:
                calleeObj = value;
                return;
            case Id_length:
                lengthObj = value;
                return;
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr) {
        switch (id) {
            case Id_callee:
                calleeAttr = attr;
                return;
            case Id_length:
                lengthAttr = attr;
                return;
        }
        super.setInstanceIdAttributes(id, attr);
    }

    @Override
    public Object[] getIds(boolean getNonEnumerable, boolean getSymbols) {
        Object[] ids = super.getIds(getNonEnumerable, getSymbols);
        if (args.length != 0) {
            boolean[] present = new boolean[args.length];
            int extraCount = args.length;
            for (int i = 0; i != ids.length; ++i) {
                Object id = ids[i];
                if (id instanceof Integer) {
                    int index = (Integer) id;
                    if (0 <= index && index < args.length) {
                        if (!present[index]) {
                            present[index] = true;
                            extraCount--;
                        }
                    }
                }
            }
            if (!getNonEnumerable) { // avoid adding args which were redefined to non-enumerable
                for (int i = 0; i < present.length; i++) {
                    if (!present[i] && super.has(i, this)) {
                        present[i] = true;
                        extraCount--;
                    }
                }
            }
            if (extraCount != 0) {
                Object[] tmp = new Object[extraCount + ids.length];
                System.arraycopy(ids, 0, tmp, extraCount, ids.length);
                ids = tmp;
                int offset = 0;
                for (int i = 0; i != args.length; ++i) {
                    if (!present[i]) {
                        ids[offset] = Integer.valueOf(i);
                        ++offset;
                    }
                }
                if (offset != extraCount) Kit.codeBug();
            }
        }
        return ids;
    }

    @Override
    public ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        if (id instanceof Scriptable) {
            return super.getOwnPropertyDescriptor(cx, id);
        }
        double d = ScriptRuntime.toNumber(id);
        int index = (int) d;
        if (d != index) {
            return super.getOwnPropertyDescriptor(cx, id);
        }
        Object value = arg(index);
        if (value == NOT_FOUND) {
            return super.getOwnPropertyDescriptor(cx, id);
        }
        if (sharedWithActivation(index)) {
            value = getFromActivation(index);
        }
        if (super.has(index, this)) { // the descriptor has been redefined
            ScriptableObject desc = super.getOwnPropertyDescriptor(cx, id);
            desc.put("value", desc, value);
            return desc;
        }
        Scriptable scope = getParentScope();
        if (scope == null) scope = this;
        return buildDataDescriptor(scope, value, EMPTY);
    }

    @Override
    protected void defineOwnProperty(Context cx, Object id,
                                     ScriptableObject desc,
                                     boolean checkValid) {
        super.defineOwnProperty(cx, id, desc, checkValid);

        double d = ScriptRuntime.toNumber(id);
        int index = (int) d;
        if (d != index) return;

        Object value = arg(index);
        if (value == NOT_FOUND) return;

        if (isAccessorDescriptor(desc)) {
            removeArg(index);
            return;
        }

        Object newValue = getProperty(desc, "value");
        if (newValue == NOT_FOUND) return;

        replaceArg(index, newValue);

        if (isFalse(getProperty(desc, "writable"))) {
            removeArg(index);
        }
    }

    // ECMAScript2015
    // 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
    //   9. Perform DefinePropertyOrThrow(obj, "callee", PropertyDescriptor {[[Get]]: %ThrowTypeError%,
    //      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
    void defineAttributesForStrictMode() {
        Context cx = Context.getContext();
        if (!cx.isStrictMode()) {
            return;
        }
        setGetterOrSetter("callee", 0, new ThrowTypeError("callee"), true);
        setGetterOrSetter("callee", 0, new ThrowTypeError("callee"), false);
        calleeObj = null;
    }

    private static BaseFunction iteratorMethod = new BaseFunction() {
        private static final long serialVersionUID = 4239122318596177391L;

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj,
                           Object[] args) {
            // TODO : call %ArrayProto_values%
            // 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
            //  1. Perform DefinePropertyOrThrow(obj, @@iterator, PropertyDescriptor {[[Value]]:%ArrayProto_values%,
            //     [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true}).
            return new NativeArrayIterator(scope, thisObj, ARRAY_ITERATOR_TYPE.KEYS);
        }
    };

    private static class ThrowTypeError extends BaseFunction {
        private static final long serialVersionUID = -744615873947395749L;
        private String propertyName;

        ThrowTypeError(String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            throw ScriptRuntime.typeError1("msg.arguments.not.access.strict", propertyName);
        }
    }

    // Fields to hold the callee and length properties,
    // where NOT_FOUND value tags deleted properties.
    private Object calleeObj;
    private Object lengthObj;

    private int calleeAttr = NOT_ENUMERABLE;
    private int lengthAttr = NOT_ENUMERABLE;

    private NativeCall activation;

    // Initially args holds activation.getOriginalArgs(), but any modification
    // of its elements triggers creation of a copy. If its element holds NOT_FOUND,
    // it indicates deleted index, in which case super class is queried.
    private Object[] args;
}
