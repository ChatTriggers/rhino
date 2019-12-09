/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.*;
import org.mozilla.javascript.NativeArrayIterator.ARRAY_ITERATOR_TYPE;

import java.lang.reflect.Array;
import java.util.*;

/**
 * This class is the abstract parent for all of the various typed arrays. Each one
 * shows a view of a specific NativeArrayBuffer, and modifications here will affect the rest.
 */

public abstract class NativeTypedArrayView<T extends Comparable<T>> extends NativeArrayBufferView implements List<T>, RandomAccess, ExternalArrayData {
    private static final long serialVersionUID = -4963053773152251274L;

    /**
     * The length, in elements, of the array
     */
    protected final int length;

    protected NativeTypedArrayView() {
        super();
        length = 0;
    }

    protected NativeTypedArrayView(NativeArrayBuffer ab, int off, int len, int byteLen) {
        super(ab, off, byteLen);
        length = len;
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_iterator) {
            initPrototypeMethod(getClassName(), id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
            return;
        }

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 1;
                s = "constructor";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            case Id_get:
                arity = 1;
                s = "get";
                break;
            case Id_set:
                arity = 2;
                s = "set";
                break;
            case Id_subarray:
                arity = 2;
                s = "subarray";
                break;
            case Id_join:
                arity = 1;
                s = "join";
                break;
            case Id_indexOf:
                arity = 1;
                s = "indexOf";
                break;
            case Id_lastIndexOf:
                arity = 1;
                s = "lastIndexOf";
                break;
            case Id_slice:
                arity = 2;
                s = "slice";
                break;
            case Id_every:
                arity = 2;
                s = "every";
                break;
            case Id_filter:
                arity = 2;
                s = "filter";
                break;
            case Id_forEach:
                arity = 2;
                s = "forEach";
                break;
            case Id_map:
                arity = 2;
                s = "map";
                break;
            case Id_reduce:
                arity = 2;
                s = "reduce";
                break;
            case Id_reduceRight:
                arity = 2;
                s = "reduceRight";
                break;
            case Id_reverse:
                arity = 0;
                s = "reverse";
                break;
            case Id_some:
                arity = 2;
                s = "some";
                break;
            case Id_copyWithin:
                arity = 3;
                s = "copyWithin";
                break;
            case Id_find:
                arity = 2;
                s = "find";
                break;
            case Id_findIndex:
                arity = 2;
                s = "findIndex";
                break;
            case Id_fill:
                arity = 3;
                s = "fill";
                break;
            case Id_keys:
                arity = 0;
                s = "keys";
                break;
            case Id_values:
                arity = 0;
                s = "values";
                break;
            case Id_entries:
                arity = 0;
                s = "entries";
                break;
            case Id_sort:
                arity = 1;
                s = "sort";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(getClassName(), id, s, fnName, arity);
    }

    // Dispatcher

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args) {
        if (!f.hasTag(getClassName())) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                return js_constructor(cx, scope, args);

            case Id_toString:
                NativeTypedArrayView<T> realThis = realThis(thisObj, f);
                final int arrayLength = realThis.getArrayLength();
                final StringBuilder builder = new StringBuilder();
                if (arrayLength > 0) {
                    builder.append(ScriptRuntime.toString(realThis.js_get(0)));
                }
                for (int i = 1; i < arrayLength; i++) {
                    builder.append(',');
                    builder.append(ScriptRuntime.toString(realThis.js_get(i)));
                }
                return builder.toString();

            case Id_get:
                if (args.length > 0) {
                    return realThis(thisObj, f).js_get(ScriptRuntime.toInt32(args[0]));
                }
                throw ScriptRuntime.constructError("Error", "invalid arguments");

            case Id_set:
                if (args.length > 0) {
                    NativeTypedArrayView<T> self = realThis(thisObj, f);
                    if (args[0] instanceof NativeTypedArrayView) {
                        int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                        self.setRange((NativeTypedArrayView<T>) args[0], offset);
                        return Undefined.instance;
                    }
                    if (args[0] instanceof NativeArray) {
                        int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                        self.setRange((NativeArray) args[0], offset);
                        return Undefined.instance;
                    }
                    if (args[0] instanceof Scriptable) {
                        // Tests show that we need to ignore a non-array object
                        return Undefined.instance;
                    }
                    if (isArg(args, 2)) {
                        return self.js_set(ScriptRuntime.toInt32(args[0]), args[1]);
                    }
                }
                throw ScriptRuntime.constructError("Error", "invalid arguments");

            case Id_subarray:
                if (args.length > 0) {
                    NativeTypedArrayView<T> self = realThis(thisObj, f);
                    int start = ScriptRuntime.toInt32(args[0]);
                    int end = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : self.length;
                    return self.js_subarray(cx, scope, start, end);
                }
                throw ScriptRuntime.constructError("Error", "invalid arguments");

            case Id_join:
                return realThis(thisObj, f).js_join(args.length > 0 ? args[0] : null);
            case Id_indexOf:
                return realThis(thisObj, f).js_indexOf(
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : 0
                );
            case Id_lastIndexOf:
                return realThis(thisObj, f).js_lastIndexOf(
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : 0
                );
            case Id_slice:
                return realThis(thisObj, f).js_slice(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : 0,
                        args.length > 1 ? args[1] : ((NativeTypedArrayView) thisObj).length
                );
            case Id_every:
                return realThis(thisObj, f).js_every(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : thisObj
                );
            case Id_filter:
                return realThis(thisObj, f).js_filter(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : thisObj
                );
            case Id_forEach:
                realThis(thisObj, f).js_forEach(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : thisObj
                );
                return Undefined.instance;
            case Id_map:
                return realThis(thisObj, f).js_map(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : thisObj
                );
            case Id_reduce:
                return realThis(thisObj, f).js_reduce(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        thisObj,
                        args.length > 1 ? args[1] : null
                );
            case Id_reduceRight:
                return realThis(thisObj, f).js_reduceRight(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        thisObj,
                        args.length > 1 ? args[1] : null
                );
            case Id_reverse:
                return realThis(thisObj, f).js_reverse(cx, scope);
            case Id_some:
                return realThis(thisObj, f).js_some(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : thisObj
                );
            case Id_copyWithin:
                return realThis(thisObj, f).js_copyWithin(
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : 0,
                        args.length > 2 ? args[2] : ((NativeTypedArrayView) thisObj).length
                );
            case Id_find:
                return realThis(thisObj, f).js_find(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : thisObj
                );
            case Id_findIndex:
                return realThis(thisObj, f).js_findIndex(
                        cx,
                        scope,
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : thisObj
                );
            case Id_fill:
                return realThis(thisObj, f).js_fill(
                        args.length > 0 ? args[0] : null,
                        args.length > 1 ? args[1] : 0,
                        args.length > 2 ? args[2] : ((NativeTypedArrayView) thisObj).length
                );
            case Id_sort:
                return realThis(thisObj, f).js_sort(
                        cx,
                        scope,
                        thisObj,
                        args.length > 0 ? args[0] : null
                );
            case Id_keys:
                return new NativeArrayIterator(scope, thisObj, ARRAY_ITERATOR_TYPE.KEYS);
            case SymbolId_iterator:
            case Id_values:
                return new NativeArrayIterator(scope, thisObj, ARRAY_ITERATOR_TYPE.VALUES);
            case Id_entries:
                return new NativeArrayIterator(scope, thisObj, ARRAY_ITERATOR_TYPE.ENTRIES);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    // Array properties implementation

    @Override
    public Object get(int index, Scriptable start) {
        return js_get(index);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return !checkIndex(index);
    }

    @Override
    public void put(int index, Scriptable start, Object val) {
        js_set(index, val);
    }

    @Override
    public void delete(int index) {
    }

    @Override
    public Object[] getIds() {
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            ret[i] = Integer.valueOf(i);
        }
        return ret;
    }

    // Actual functions

    protected boolean checkIndex(int index) {
        return ((index < 0) || (index >= length));
    }

    /**
     * Return the number of bytes represented by each element in the array. This can be useful
     * when wishing to manipulate the byte array directly from Java.
     */
    public abstract int getBytesPerElement();

    protected abstract NativeTypedArrayView<T> construct(NativeArrayBuffer ab, int off, int len);

    protected abstract Object js_get(int index);

    protected abstract Object js_set(int index, Object c);

    protected abstract NativeTypedArrayView<T> realThis(Scriptable thisObj, IdFunctionObject f);

    private NativeArrayBuffer makeArrayBuffer(Context cx, Scriptable scope, int length) {
        return (NativeArrayBuffer) cx.newObject(scope, NativeArrayBuffer.CLASS_NAME,
                new Object[]{length});
    }

    private NativeTypedArrayView<T> js_constructor(Context cx, Scriptable scope, Object[] args) {
        if (!isArg(args, 0)) {
            return construct(NativeArrayBuffer.EMPTY_BUFFER, 0, 0);
        }

        final Object arg0 = args[0];
        if (arg0 == null) {
            return construct(NativeArrayBuffer.EMPTY_BUFFER, 0, 0);
        }

        if ((arg0 instanceof Number) || (arg0 instanceof String)) {
            // Create a zeroed-out array of a certain length
            int length = ScriptRuntime.toInt32(arg0);
            NativeArrayBuffer buffer = makeArrayBuffer(cx, scope, length * getBytesPerElement());
            return construct(buffer, 0, length);
        }

        if (arg0 instanceof NativeTypedArrayView) {
            // Copy elements from the old array and convert them into our own
            NativeTypedArrayView<T> src = (NativeTypedArrayView<T>) arg0;
            NativeArrayBuffer na = makeArrayBuffer(cx, scope, src.length * getBytesPerElement());
            NativeTypedArrayView<T> v = construct(na, 0, src.length);

            for (int i = 0; i < src.length; i++) {
                v.js_set(i, src.js_get(i));
            }
            return v;
        }

        if (arg0 instanceof NativeArrayBuffer) {
            // Make a slice of an existing buffer, with shared storage
            NativeArrayBuffer na = (NativeArrayBuffer) arg0;
            int byteOff = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;

            int byteLen;
            if (isArg(args, 2)) {
                byteLen = ScriptRuntime.toInt32(args[2]) * getBytesPerElement();
            } else {
                byteLen = na.getLength() - byteOff;
            }

            if ((byteOff < 0) || (byteOff > na.buffer.length)) {
                throw ScriptRuntime.constructError("RangeError", "offset out of range");
            }
            if ((byteLen < 0) || ((byteOff + byteLen) > na.buffer.length)) {
                throw ScriptRuntime.constructError("RangeError", "length out of range");
            }
            if ((byteOff % getBytesPerElement()) != 0) {
                throw ScriptRuntime.constructError("RangeError", "offset must be a multiple of the byte size");
            }
            if ((byteLen % getBytesPerElement()) != 0) {
                throw ScriptRuntime.constructError("RangeError", "offset and buffer must be a multiple of the byte size");
            }

            return construct(na, byteOff, byteLen / getBytesPerElement());
        }

        if (arg0 instanceof NativeArray) {
            // Copy elements of the array and convert them to the correct type
            NativeArray array = (NativeArray) arg0;

            NativeArrayBuffer na = makeArrayBuffer(cx, scope, array.size() * getBytesPerElement());
            NativeTypedArrayView<T> v = construct(na, 0, array.size());
            for (int i = 0; i < array.size(); i++) {
                // we have to call this here to get the raw value;
                // null has to be forewoded as null
                final Object value = array.get(i, array);
                if (value == Scriptable.NOT_FOUND || value == Undefined.instance) {
                    v.js_set(i, Double.NaN);
                } else if (value instanceof Wrapper) {
                    v.js_set(i, ((Wrapper) value).unwrap());
                } else {
                    v.js_set(i, value);
                }
            }
            return v;
        }

        if (ScriptRuntime.isArrayObject(arg0)) {
            // Copy elements of the array and convert them to the correct type
            Object[] arrayElements = ScriptRuntime.getArrayElements((Scriptable) arg0);

            NativeArrayBuffer na = makeArrayBuffer(cx, scope, arrayElements.length * getBytesPerElement());
            NativeTypedArrayView<T> v = construct(na, 0, arrayElements.length);
            for (int i = 0; i < arrayElements.length; i++) {
                v.js_set(i, arrayElements[i]);
            }
            return v;
        }
        throw ScriptRuntime.constructError("Error", "invalid argument");
    }

    private void setRange(NativeTypedArrayView<T> v, int off) {
        if (off >= length) {
            throw ScriptRuntime.constructError("RangeError", "offset out of range");
        }

        if (v.length > (length - off)) {
            throw ScriptRuntime.constructError("RangeError", "source array too long");
        }

        if (v.arrayBuffer == arrayBuffer) {
            // Copy to temporary space first, as per spec, to avoid messing up overlapping copies
            Object[] tmp = new Object[v.length];
            for (int i = 0; i < v.length; i++) {
                tmp[i] = v.js_get(i);
            }
            for (int i = 0; i < v.length; i++) {
                js_set(i + off, tmp[i]);
            }
        } else {
            for (int i = 0; i < v.length; i++) {
                js_set(i + off, v.js_get(i));
            }
        }
    }

    private void setRange(NativeArray a, int off) {
        if (off > length) {
            throw ScriptRuntime.constructError("RangeError", "offset out of range");
        }
        if ((off + a.size()) > length) {
            throw ScriptRuntime.constructError("RangeError", "offset + length out of range");
        }

        int pos = off;
        for (Object val : a) {
            js_set(pos, val);
            pos++;
        }
    }

    private Object js_subarray(Context cx, Scriptable scope, int s, int e) {
        int start = (s < 0 ? length + s : s);
        int end = (e < 0 ? length + e : e);

        // Clamping behavior as described by the spec.
        start = Math.max(0, start);
        end = Math.min(length, end);
        int len = Math.max(0, (end - start));
        int byteOff = Math.min(start * getBytesPerElement(), arrayBuffer.getLength());

        return cx.newObject(scope, getClassName(), new Object[]{arrayBuffer, byteOff, len});
    }

    private String js_join(Object separator) {
        if (separator == null) {
            separator = ",";
        } else if (!(separator instanceof String)) {
            separator = ScriptRuntime.toString(separator);
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(ScriptRuntime.toString(getArrayElement(i)));
            if (i != length - 1)
                sb.append(separator);
        }

        return sb.toString();
    }

    private int js_indexOf(Object object, Object start) {
        if (!(start instanceof Number)) {
            start = ScriptRuntime.toNumber(start);
        }

        for (int i = (int) start; i < length; i++) {
            if (ScriptRuntime.shallowEq(getArrayElement(i), object)) {
                return i;
            }
        }

        return -1;
    }

    private int js_lastIndexOf(Object object, Object start) {
        if (!(start instanceof Number)) {
            start = ScriptRuntime.toNumber(start);
        }

        for (int i = length; i > (int) start; i--) {
            if (ScriptRuntime.shallowEq(getArrayElement(i), object)) {
                return i;
            }
        }

        return -1;
    }

    private NativeTypedArrayView<T> js_slice(Context cx, Scriptable scope, Object _start, Object _end) {
        if (!(_start instanceof Number)) {
            _start = (int) ScriptRuntime.toNumber(_start);
        }
        if (!(_end instanceof Number)) {
            _end = (int) ScriptRuntime.toNumber(_end);
        }

        int start = (int) _start;
        int end = (int) _end;

        if (start < 0) {
            start = length + start;
        }
        if (end < 0) {
            end = length + end;
        }

        int length = Math.max(end - start, 0);

        NativeArrayBuffer na = makeArrayBuffer(cx, scope, length);
        NativeTypedArrayView<T> view = construct(na, 0, length);

        for (int i = start; i < end; i++) {
            view.js_set(start - i, getArrayElement(i));
        }

        return view;
    }

    private boolean isFalsey(Object obj) {
        return obj == null || obj == Undefined.instance || (obj instanceof Boolean && !((boolean) obj));
    }

    private boolean js_every(Context cx, Scriptable scope, Object cb, Object _thisObj) {
        validateFunctionalArgs(cb, _thisObj, "%TypedArray%.prototype.every");

        Callable fn = (Callable) cb;
        Scriptable thisObj = (Scriptable) _thisObj;

        for (int i = 0; i < length; i++) {
            Object result = fn.call(cx, scope, thisObj, new Object[]{getArrayElement(i), i, _thisObj});
            if (isFalsey(result)) {
                return false;
            }
        }

        return true;
    }

    private NativeTypedArrayView<T> js_filter(Context cx, Scriptable scope, Object cb, Object thisObj) {
        validateFunctionalArgs(cb, thisObj, "%TypedArray%.prototype.filter");

        Callable fn = (Callable) cb;
        LinkedList<T> ll = new LinkedList<>();

        for (int i = 0; i < length; i++) {
            T el = (T) getArrayElement(i);
            Object result = fn.call(cx, scope, (Scriptable) thisObj, new Object[]{el, i, thisObj});
            if (!isFalsey(result)) {
                ll.add(el);
            }
        }

        int length = ll.size();

        NativeArrayBuffer na = makeArrayBuffer(cx, scope, length);
        NativeTypedArrayView<T> view = construct(na, 0, length);

        Iterator<T> ite = ll.iterator();

        int index = 0;
        while (ite.hasNext()) {
            view.setArrayElement(index, ite.next());
            index++;
        }

        return view;
    }

    private void js_forEach(Context cx, Scriptable scope, Object cb, Object thisObj) {
        validateFunctionalArgs(cb, thisObj, "%TypedArray%.prototype.forEach");

        Callable fn = (Callable) cb;

        for (int i = 0; i < length; i++) {
            fn.call(cx, scope, (Scriptable) thisObj, new Object[]{getArrayElement(i), i, thisObj});
        }
    }

    private NativeTypedArrayView<T> js_map(Context cx, Scriptable scope, Object cb, Object thisObj) {
        validateFunctionalArgs(cb, thisObj, "%TypedArray%.prototype.map");

        Callable fn = (Callable) cb;

        for (int i = 0; i < length; i++) {
            Object[] args = new Object[]{getArrayElement(i), i, thisObj};
            setArrayElement(i, fn.call(cx, scope, (Scriptable) thisObj, args));
        }

        return this;
    }

    private Object js_reduce(Context cx, Scriptable scope, Object cb, Object thisObj, Object initialValue) {
        validateFunctionalArgs(cb, thisObj, "%TypedArray%.prototype.reduce");

        int start = 0;

        if (initialValue == null) {
            start = 1;
            initialValue = getArrayElement(0);
        }

        Callable fn = (Callable) cb;

        for (int i = start; i < length; i++) {
            Object[] args = new Object[]{initialValue, getArrayElement(i), i, thisObj};
            initialValue = fn.call(cx, scope, (Scriptable) thisObj, args);
        }

        return initialValue;
    }

    private Object js_reduceRight(Context cx, Scriptable scope, Object cb, Object thisObj, Object initialValue) {
        validateFunctionalArgs(cb, thisObj, "%TypedArray%.prototype.reduceRight");

        int start = length - 1;

        if (initialValue == null) {
            start -= 1;
            initialValue = getArrayElement(0);
        }

        Callable fn = (Callable) cb;

        for (int i = start; i >= 0; i--) {
            Object[] args = new Object[]{initialValue, getArrayElement(i), i, thisObj};
            initialValue = fn.call(cx, scope, (Scriptable) thisObj, args);
        }

        return initialValue;
    }

    private NativeTypedArrayView<T> js_reverse(Context cx, Scriptable scope) {
        NativeArrayBuffer na = makeArrayBuffer(cx, scope, length);
        NativeTypedArrayView<T> view = construct(na, 0, length);

        for (int i = 0; i < length; i++) {
            view.setArrayElement(i, getArrayElement(length - i - 1));
        }

        return view;
    }

    private boolean js_some(Context cx, Scriptable scope, Object cb, Object thisObj) {
        validateFunctionalArgs(cb, thisObj, "%TypedArray%.prototype.some");

        Callable fn = (Callable) cb;

        for (int i = 0; i < length; i++) {
            Object result = fn.call(cx, scope, (Scriptable) thisObj, new Object[]{getArrayElement(i), i, thisObj});
            if (!isFalsey(result)) {
                return true;
            }
        }

        return false;
    }

    private NativeTypedArrayView<T> js_copyWithin(Object _target, Object _start, Object _end) {
        if (_target == null) {
            return this;
        }

        int target = (int) ScriptRuntime.toNumber(_target);
        int start = (int) ScriptRuntime.toNumber(_start);
        int end = (int) ScriptRuntime.toNumber(_end);

        int copyLength = Math.max(end - start, 0);
        if (copyLength == 0) return this;

        if (target + copyLength >= length) {
            copyLength = length - target;
        }

        // To store the elements
        LinkedList<Object> ll = new LinkedList<>();

        for (int i = 0; i < copyLength; i++) {
            ll.add(getArrayElement(start + i));
        }

        // Copy
        int i = 0;
        for (Object el : ll) {
            setArrayElement(target + i++, el);
        }

        return this;
    }

    private Object js_find(Context cx, Scriptable scope, Object cb, Object thisObj) {
        int index = js_findIndex(cx, scope, cb, thisObj);

        if (index == -1) {
            return Undefined.instance;
        } else {
            return getArrayElement(index);
        }
    }

    private int js_findIndex(Context cx, Scriptable scope, Object cb, Object thisObj) {
        validateFunctionalArgs(cb, thisObj, "%TypedArray%.prototype.findIndex");

        Callable fn = (Callable) cb;

        for (int i = 0; i < length; i++) {
            Object el = getArrayElement(i);
            Object[] args = new Object[]{el, i, thisObj};
            Object result = fn.call(cx, scope, (Scriptable) thisObj, args);

            if (!isFalsey(result)) {
                return i;
            }
        }

        return -1;
    }

    private NativeTypedArrayView<T> js_fill(Object value, Object _start, Object _end) {
        if (value == null) return this;

        int start = (int) ScriptRuntime.toNumber(_start);
        int end = (int) ScriptRuntime.toNumber(_end);

        for (int i = start; i < end; i++) {
            setArrayElement(i, value);
        }

        return this;
    }

    private NativeTypedArrayView<T> js_sort(Context cx, Scriptable scope, Scriptable thisObj, Object sorter) {
        Comparator<T> cmp;

        if (sorter == null) {
            cmp = Comparable::compareTo;
        } else if (!(sorter instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.isnt.function", ScriptRuntime.toString(sorter));
        } else {
            Callable fn = (Callable) sorter;
            cmp = (T a, T b) -> {
                Object result = fn.call(cx, scope, thisObj, new Object[]{a, b});
                if (result instanceof Scriptable) {
                    return isFalsey(result) ? 0 : 1;
                }
                return (int) ScriptRuntime.toNumber(result);
            };
        }

        boolean sorted;

        do {
            sorted = true;

            for (int i = 0; i < length - 1; i++) {
                Object el1 = getArrayElement(i);
                Object el2 = getArrayElement(i + 1);
                int res = cmp.compare((T) el1, (T) el2);
                if (res > 0) {
                    sorted = false;
                    setArrayElement(i, el2);
                    setArrayElement(i + 1, el1);
                }
            }
        } while (!sorted);

        return this;
    }

    private void validateFunctionalArgs(Object cb, Object thisObj, String errorStr) {
        if (cb == null) {
            throw ScriptRuntime.typeError2("msg.typed.array.missing.argument", "0", errorStr);
        } else if (!(cb instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.isnt.function", ScriptRuntime.toString(cb));
        } else if (!(thisObj instanceof Scriptable)) {
            throw ScriptRuntime.typeError2("msg.typed.array.invalid.argument", "1", errorStr);
        }
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.ITERATOR.equals(k)) {
            return SymbolId_iterator;
        }
        return 0;
    }

    // #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2019-11-25 20:03:32 CST
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
                    } else if (c == 'm') {
                        if (s.charAt(2) == 'p' && s.charAt(1) == 'a') {
                            id = Id_map;
                            break L0;
                        }
                    } else if (c == 's') {
                        if (s.charAt(2) == 't' && s.charAt(1) == 'e') {
                            id = Id_set;
                            break L0;
                        }
                    }
                    break L;
                case 4:
                    switch (s.charAt(2)) {
                        case 'i':
                            X = "join";
                            id = Id_join;
                            break L;
                        case 'l':
                            X = "fill";
                            id = Id_fill;
                            break L;
                        case 'm':
                            X = "some";
                            id = Id_some;
                            break L;
                        case 'n':
                            X = "find";
                            id = Id_find;
                            break L;
                        case 'r':
                            X = "sort";
                            id = Id_sort;
                            break L;
                        case 'y':
                            X = "keys";
                            id = Id_keys;
                            break L;
                    }
                    break L;
                case 5:
                    c = s.charAt(0);
                    if (c == 'e') {
                        X = "every";
                        id = Id_every;
                    } else if (c == 's') {
                        X = "slice";
                        id = Id_slice;
                    }
                    break L;
                case 6:
                    c = s.charAt(0);
                    if (c == 'f') {
                        X = "filter";
                        id = Id_filter;
                    } else if (c == 'r') {
                        X = "reduce";
                        id = Id_reduce;
                    } else if (c == 'v') {
                        X = "values";
                        id = Id_values;
                    }
                    break L;
                case 7:
                    switch (s.charAt(0)) {
                        case 'e':
                            X = "entries";
                            id = Id_entries;
                            break L;
                        case 'f':
                            X = "forEach";
                            id = Id_forEach;
                            break L;
                        case 'i':
                            X = "indexOf";
                            id = Id_indexOf;
                            break L;
                        case 'r':
                            X = "reverse";
                            id = Id_reverse;
                            break L;
                    }
                    break L;
                case 8:
                    c = s.charAt(0);
                    if (c == 's') {
                        X = "subarray";
                        id = Id_subarray;
                    } else if (c == 't') {
                        X = "toString";
                        id = Id_toString;
                    }
                    break L;
                case 9:
                    X = "findIndex";
                    id = Id_findIndex;
                    break L;
                case 10:
                    X = "copyWithin";
                    id = Id_copyWithin;
                    break L;
                case 11:
                    c = s.charAt(0);
                    if (c == 'c') {
                        X = "constructor";
                        id = Id_constructor;
                    } else if (c == 'l') {
                        X = "lastIndexOf";
                        id = Id_lastIndexOf;
                    } else if (c == 'r') {
                        X = "reduceRight";
                        id = Id_reduceRight;
                    }
                    break L;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    // Table of all functions
    private static final int
            Id_constructor = 1,
            Id_toString = 2,
            Id_get = 3,
            Id_set = 4,
            Id_subarray = 5,
            Id_join = 6,
            Id_indexOf = 7,
            Id_lastIndexOf = 8,
            Id_slice = 9,
            Id_every = 10,
            Id_filter = 11,
            Id_forEach = 12,
            Id_map = 13,
            Id_reduce = 14,
            Id_reduceRight = 15,
            Id_reverse = 16,
            Id_some = 17,
            Id_copyWithin = 18,
            Id_find = 19,
            Id_findIndex = 20,
            Id_fill = 21,
            Id_keys = 22,
            Id_values = 23,
            Id_entries = 24,
            Id_sort = 25,
            SymbolId_iterator = 26;

    protected static final int
            MAX_PROTOTYPE_ID = SymbolId_iterator;

// #/string_id_map#

    // Constructor properties

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        ctor.put("BYTES_PER_ELEMENT", ctor, ScriptRuntime.wrapInt(getBytesPerElement()));
    }

    // Property dispatcher

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected String getInstanceIdName(int id) {
        switch (id) {
            case Id_length:
                return "length";
            case Id_BYTES_PER_ELEMENT:
                return "BYTES_PER_ELEMENT";
            default:
                return super.getInstanceIdName(id);
        }
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        switch (id) {
            case Id_length:
                return ScriptRuntime.wrapInt(length);
            case Id_BYTES_PER_ELEMENT:
                return ScriptRuntime.wrapInt(getBytesPerElement());
            default:
                return super.getInstanceIdValue(id);
        }
    }

// #string_id_map#

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
// #generated# Last update: 2019-11-25 20:03:32 CST
        L0:
        {
            id = 0;
            String X = null;
            int s_length = s.length();
            if (s_length == 6) {
                X = "length";
                id = Id_length;
            } else if (s_length == 17) {
                X = "BYTES_PER_ELEMENT";
                id = Id_BYTES_PER_ELEMENT;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        if (id == 0) {
            return super.findInstanceIdInfo(s);
        }
        return instanceIdInfo(NOT_WRITABLE | NOT_CONFIGURABLE, id);
    }

    /*
     * These must not conflict with ids in the parent since we delegate there for property dispatching.
     */
    private static final int
            Id_length = NativeArrayBufferView.MAX_INSTANCE_ID + 1,
            Id_BYTES_PER_ELEMENT = Id_length + 1,
            MAX_INSTANCE_ID = Id_BYTES_PER_ELEMENT;

// #/string_id_map#

    // External Array implementation

    @Override
    public Object getArrayElement(int index) {
        return js_get(index);
    }

    @Override
    public void setArrayElement(int index, Object value) {
        js_set(index, value);
    }

    @Override
    public int getArrayLength() {
        return length;
    }

    // Abstract List implementation

    @SuppressWarnings("unused")
    @Override
    public boolean containsAll(Collection<?> objects) {
        for (Object o : objects) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < length; i++) {
            if (o.equals(js_get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    @Override
    public int lastIndexOf(Object o) {
        for (int i = length - 1; i >= 0; i--) {
            if (o.equals(js_get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    @Override
    public Object[] toArray() {
        Object[] a = new Object[length];
        for (int i = 0; i < length; i++) {
            a[i] = js_get(i);
        }
        return a;
    }

    @SuppressWarnings("unused")
    @Override
    public <U> U[] toArray(U[] ts) {
        U[] a;

        if (ts.length >= length) {
            a = ts;
        } else {
            a = (U[]) Array.newInstance(ts.getClass().getComponentType(), length);
        }

        for (int i = 0; i < length; i++) {
            try {
                a[i] = (U) js_get(i);
            } catch (ClassCastException cce) {
                throw new ArrayStoreException();
            }
        }
        return a;
    }


    @SuppressWarnings("unused")
    @Override
    public int size() {
        return length;
    }

    @SuppressWarnings("unused")
    @Override
    public boolean isEmpty() {
        return (length == 0);
    }

    @SuppressWarnings("unused")
    @Override
    public boolean contains(Object o) {
        return (indexOf(o) >= 0);
    }


    @Override
    public boolean equals(Object o) {
        try {
            NativeTypedArrayView<T> v = (NativeTypedArrayView<T>) o;
            if (length != v.length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (!js_get(i).equals(v.js_get(i))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException cce) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hc = 0;
        for (int i = 0; i < length; i++) {
            hc += js_get(i).hashCode();
        }
        return hc;
    }

    @SuppressWarnings("unused")
    @Override
    public Iterator<T> iterator() {
        return new NativeTypedArrayIterator<T>(this, 0);
    }

    @SuppressWarnings("unused")
    @Override
    public ListIterator<T> listIterator() {
        return new NativeTypedArrayIterator<T>(this, 0);
    }

    @SuppressWarnings("unused")
    @Override
    public ListIterator<T> listIterator(int start) {
        if (checkIndex(start)) {
            throw new IndexOutOfBoundsException();
        }
        return new NativeTypedArrayIterator<T>(this, start);
    }

    @SuppressWarnings("unused")
    @Override
    public List<T> subList(int i, int i2) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean add(T aByte) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public void add(int i, T aByte) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean addAll(Collection<? extends T> bytes) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean addAll(int i, Collection<? extends T> bytes) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public T remove(int i) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean retainAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }
}
