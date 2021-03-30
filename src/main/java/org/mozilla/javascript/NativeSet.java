/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;

public class NativeSet extends IdScriptableObject {
    private static final long serialVersionUID = -8442212766987072986L;
    private static final Object SET_TAG = "Set";
    static final String ITERATOR_TAG = "Set Iterator";

    static final SymbolKey GETSIZE = new SymbolKey("[Symbol.getSize]");

    private final Hashtable entries = new Hashtable();

    private boolean instanceOfSet = false;

    static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeSet obj = new NativeSet();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false);

        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        desc.put("enumerable", desc, false);
        desc.put("configurable", desc, true);
        desc.put("get", desc, obj.get(GETSIZE, obj));
        obj.defineOwnProperty(cx, "size", desc);

        if (sealed) {
            obj.sealObject();
        }
    }

    @Override
    public String getClassName() {
        return "Set";
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        addCtorSpecies(ctor);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args) {
        if (!f.hasTag(SET_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        final int id = f.methodId();
        switch (id) {
            case Id_constructor:
                return js_constructor(cx, scope, thisObj, args.length > 0 ? args[0] : Undefined.instance);
            case Id_add:
                return realThis(thisObj, f).js_add(args.length > 0 ? args[0] : Undefined.instance);
            case Id_delete:
                return realThis(thisObj, f).js_delete(args.length > 0 ? args[0] : Undefined.instance);
            case Id_has:
                return realThis(thisObj, f).js_has(args.length > 0 ? args[0] : Undefined.instance);
            case Id_clear:
                return realThis(thisObj, f).js_clear();
            case Id_values:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.VALUES);
            case Id_entries:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.BOTH);
            case Id_forEach:
                return realThis(thisObj, f).js_forEach(cx, scope,
                        args.length > 0 ? args[0] : Undefined.instance,
                        args.length > 1 ? args[1] : Undefined.instance);

            case Id_addAll:
                return realThis(thisObj, f).js_addAll(args);
            case Id_deleteAll:
                return realThis(thisObj, f).js_deleteAll(args);
            case Id_every:
                return realThis(thisObj, f).js_every(cx, scope, args);
            case Id_filter:
                return realThis(thisObj, f).js_filter(cx, scope, args);
            case Id_find:
                return realThis(thisObj, f).js_find(cx, scope, args);
            case Id_join:
                return realThis(thisObj, f).js_join(args);
            case Id_map:
                return realThis(thisObj, f).js_map(cx, scope, args);
            case Id_reduce:
                return realThis(thisObj, f).js_reduce(cx, scope, args);
            case Id_some:
                return realThis(thisObj, f).js_some(cx, scope, args);
            case Id_intersection:
                return realThis(thisObj, f).js_intersection(cx, scope, args);
            case Id_union:
                return realThis(thisObj, f).js_union(cx, scope, args);
            case Id_difference:
                return realThis(thisObj, f).js_difference(cx, scope, args);
            case Id_symmetricDifference:
                return realThis(thisObj, f).js_symmetricDifference(cx, scope, args);
            case Id_isDisjointFrom:
                return realThis(thisObj, f).js_isDisjointFrom(cx, scope, args);
            case Id_isSubsetOf:
                return realThis(thisObj, f).js_isSubsetOf(cx, scope, args);
            case Id_isSupersetOf:
                return realThis(thisObj, f).js_isSupersetOf(cx, scope, args);
            case SymbolId_getSize:
                return realThis(thisObj, f).js_getSize();
        }
        throw new IllegalArgumentException("Set.prototype has no method: " + f.getFunctionName());
    }

    private NativeSet js_constructor(Context cx, Scriptable scope, Scriptable thisObj, Object arg) {
        if (thisObj == null) {
            NativeSet ns = new NativeSet();
            ns.instanceOfSet = true;
            loadFromIterable(cx, scope, ns, arg);
            return ns;
        } else {
            throw ScriptRuntime.typeError1("msg.no.new", "Set");
        }
    }

    private Object js_add(Object k) {
        // Special handling of "negative zero" from the spec.
        Object key = k;
        if ((key instanceof Number) &&
                ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
            key = 0.0;
        }
        entries.put(key, key);
        return this;
    }

    private boolean js_delete(Object arg) {
        final Object ov = entries.delete(arg);
        return (ov != null);
    }

    private boolean js_has(Object arg) {
        return entries.has(arg);
    }

    private Object js_clear() {
        entries.clear();
        return Undefined.instance;
    }

    private Object js_getSize() {
        return entries.size();
    }

    private NativeCollectionIterator js_iterator(Scriptable scope, NativeCollectionIterator.Type type) {
        return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator());
    }

    private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(arg1);
        }
        final Callable f = (Callable) arg1;

        boolean isStrict = cx.isStrictMode();
        Iterator<Hashtable.Entry> i = entries.iterator();
        while (i.hasNext()) {
            // Per spec must convert every time so that primitives are always regenerated...
            Scriptable thisObj = ScriptRuntime.toObjectOrNull(cx, arg2, scope);

            if (thisObj == null && !isStrict) {
                thisObj = scope;
            }
            if (thisObj == null) {
                thisObj = Undefined.SCRIPTABLE_UNDEFINED;
            }

            final Hashtable.Entry e = i.next();
            f.call(cx, scope, thisObj, new Object[]{e.value, e.value, this});
        }
        return Undefined.instance;
    }

    private Object js_addAll(Object[] args) {
        for (Object arg : args) {
            js_add(arg);
        }

        return this;
    }

    private Object js_deleteAll(Object[] args) {
        boolean allDeleted = true;

        for (Object arg : args) {
            allDeleted &= js_delete(arg);
        }

        return allDeleted;
    }

    private Object js_every(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg0;
        Scriptable thisObj = arg1 instanceof Scriptable ? (Scriptable) arg1 : Undefined.SCRIPTABLE_UNDEFINED;

        for (Hashtable.Entry en : entries) {
            Object result = cb.call(cx, scope, thisObj, new Object[]{ en.key, en.key, this });

            if (!ScriptRuntime.toBoolean(result)) {
                return false;
            }
        }

        return true;
    }

    private Object js_filter(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeSet set = (NativeSet) species.construct(cx, scope, new Object[0]);

        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg0;
        Scriptable thisObj = arg1 instanceof Scriptable ? (Scriptable) arg1 : Undefined.SCRIPTABLE_UNDEFINED;

        for (Hashtable.Entry en : entries) {
            Object result = cb.call(cx, scope, thisObj, new Object[]{ en.key, en.key, this });

            if (ScriptRuntime.toBoolean(result)) {
                set.entries.put(en.key, en.key);
            }
        }

        return set;
    }

    private Object js_find(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg0;
        Scriptable thisObj = arg1 instanceof Scriptable ? (Scriptable) arg1 : Undefined.SCRIPTABLE_UNDEFINED;

        for (Hashtable.Entry en : entries) {
            Object result = cb.call(cx, scope, thisObj, new Object[]{ en.key, en.key, this });

            if (ScriptRuntime.toBoolean(result)) {
                return en.key;
            }
        }

        return Undefined.instance;
    }

    private Object js_join(Object[] args) {
        String separator = args.length > 0 ? ScriptRuntime.toString(args[0]) : ",";

        StringBuilder sb = new StringBuilder();

        for (Hashtable.Entry en : entries) {
            sb.append(ScriptRuntime.toString(en.key)).append(separator);
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private Object js_map(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeSet set = (NativeSet) species.construct(cx, scope, new Object[0]);

        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg0;
        Scriptable thisObj = arg1 instanceof Scriptable ? (Scriptable) arg1 : Undefined.SCRIPTABLE_UNDEFINED;

        for (Hashtable.Entry en : entries) {
            Object result = cb.call(cx, scope, thisObj, new Object[]{ en.key, en.key, this });
            set.entries.put(result, result);
        }

        return set;
    }

    private Object js_reduce(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? Undefined.instance : args[0];

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.object.not.callable", ScriptRuntime.toString(arg0));
        }

        Callable cb = (Callable) arg0;
        Object accumulator = args.length > 1 ? args[1] : Undefined.instance;
        boolean first = true;

        for (Hashtable.Entry en : entries) {
            if (first && Undefined.isUndefined(accumulator)) {
                accumulator = en.value;
            } else {
                accumulator = cb.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{ accumulator, en.key, en.key, this });
            }
            first = false;
        }

        if (first && Undefined.isUndefined(accumulator)) {
            throw ScriptRuntime.typeError("Map is empty and no accumulator was provided to the reduce method");
        }

        return accumulator;
    }

    private Object js_some(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg0;
        Scriptable thisObj = arg1 instanceof Scriptable ? (Scriptable) arg1 : Undefined.SCRIPTABLE_UNDEFINED;

        for (Hashtable.Entry en : entries) {
            Object result = cb.call(cx, scope, thisObj, new Object[]{ en.key, en.key, this });

            if (ScriptRuntime.toBoolean(result)) {
                return true;
            }
        }

        return false;
    }

    private Object js_intersection(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeSet set = (NativeSet) species.construct(cx, scope, new Object[0]);

        Object arg0 = args.length == 0 ? null : args[0];
        ES6Iterator it = ScriptRuntime.toIterator(cx, scope, ScriptableObject.ensureScriptable(arg0), false);

        while (!it.isDone(cx, scope)) {
            Object value = it.nextValue(cx, scope);
            if (js_has(value)) {
                set.js_add(value);
            }
        }

        return set;
    }

    private Object js_union(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeSet set = (NativeSet) species.construct(cx, scope, new Object[0]);
        ES6Iterator it = ScriptRuntime.toIterator(cx, scope, args.length > 0 ? ScriptableObject.ensureScriptable(args[0]) : null, false);

        for (Hashtable.Entry en : entries) {
            set.entries.put(en.key, en.key);
        }

        while (!it.isDone(cx, scope)) {
            Object value = it.nextValue(cx, scope);
            set.entries.put(value, value);
        }

        return set;
    }

    private Object js_difference(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeSet set = (NativeSet) species.construct(cx, scope, new Object[0]);

        for (Hashtable.Entry en : entries) {
            set.entries.put(en.key, en.key);
        }

        Object arg0 = args.length == 0 ? null : args[0];
        ES6Iterator it = ScriptRuntime.toIterator(cx, scope, ScriptableObject.ensureScriptable(arg0), false);

        while (!it.isDone(cx, scope)) {
            Object value = it.nextValue(cx, scope);
            set.js_delete(value);
        }

        return set;
    }

    private Object js_symmetricDifference(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeSet set = (NativeSet) species.construct(cx, scope, new Object[0]);

        for (Hashtable.Entry en : entries) {
            set.entries.put(en.key, en.key);
        }

        Object arg0 = args.length == 0 ? null : args[0];
        ES6Iterator it = ScriptRuntime.toIterator(cx, scope, ScriptableObject.ensureScriptable(arg0), false);

        while (!it.isDone(cx, scope)) {
            Object value = it.nextValue(cx, scope);
            boolean removed = set.js_delete(value);

            if (!removed) {
                set.js_add(value);
            }
        }

        return set;
    }

    private Object js_isDisjointFrom(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];
        ES6Iterator it = ScriptRuntime.toIterator(cx, scope, ScriptableObject.ensureScriptable(arg0), false);

        while (!it.isDone(cx, scope)) {
            Object value = it.nextValue(cx, scope);
            if (js_has(value)) {
                return false;
            }
        }

        return true;
    }

    private Object js_isSubsetOf(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];

        if (!(arg0 instanceof NativeSet)) {
            arg0 = js_constructor(cx, scope, null, arg0);
        }

        NativeSet other = (NativeSet) arg0;

        for (Hashtable.Entry en : entries) {
            if (!other.entries.has(en.key)) {
                return false;
            }
        }

        return true;
    }

    private Object js_isSupersetOf(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];

        if (!(arg0 instanceof NativeSet)) {
            arg0 = js_constructor(cx, scope, null, arg0);
        }

        NativeSet other = (NativeSet) arg0;

        for (Hashtable.Entry en : other.entries) {
            if (!entries.has(en.key)) {
                return false;
            }
        }

        return true;
    }

    /**
     * If an "iterable" object was passed to the constructor, there are many many things
     * to do. This is common code with NativeWeakSet.
     */
    static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject set, Object arg1) {
        if ((arg1 == null) || Undefined.instance.equals(arg1)) {
            return;
        }

        // Call the "[Symbol.iterator]" property as a function.
        Object ito = ScriptRuntime.callIterator(arg1, cx, scope);
        if (Undefined.instance.equals(ito)) {
            // Per spec, ignore if the iterator returns undefined
            return;
        }

        // Find the "add" function of our own prototype, since it might have
        // been replaced. Since we're not fully constructed yet, create a dummy instance
        // so that we can get our own prototype.
        ScriptableObject dummy = ensureScriptableObject(cx.newObject(scope, set.getClassName()));
        final Callable add =
                ScriptRuntime.getPropFunctionAndThis(dummy.getPrototype(), "add", cx, scope);
        // Clean up the value left around by the previous function
        ScriptRuntime.lastStoredScriptable(cx);

        // Finally, run through all the iterated values and add them!
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
            for (Object val : it) {
                final Object finalVal = val == Scriptable.NOT_FOUND ? Undefined.instance : val;
                add.call(cx, scope, set, new Object[]{finalVal});
            }
        }
    }

    private NativeSet realThis(Scriptable thisObj, IdFunctionObject f) {
        if (thisObj == null) {
            throw incompatibleCallError(f);
        }
        try {
            final NativeSet ns = (NativeSet) ScriptRuntime.unwrapProxy(thisObj);
            if (!ns.instanceOfSet) {
                // If we get here, then this object doesn't have the "Set internal data slot."
                throw incompatibleCallError(f);
            }
            return ns;
        } catch (ClassCastException cce) {
            throw incompatibleCallError(f);
        }
    }

    @Override
    protected void initPrototypeId(int id) {
        switch (id) {
            case SymbolId_getSize:
                initPrototypeMethod(SET_TAG, id, GETSIZE, "get size", 0);
                return;
            case SymbolId_toStringTag:
                initPrototypeValue(SymbolId_toStringTag, SymbolKey.TO_STRING_TAG,
                        getClassName(), NOT_ENUMERABLE | NOT_WRITABLE);
                return;
            // fallthrough
        }

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 0;
                s = "constructor";
                break;
            case Id_add:
                arity = 1;
                s = "add";
                break;
            case Id_delete:
                arity = 1;
                s = "delete";
                break;
            case Id_has:
                arity = 1;
                s = "has";
                break;
            case Id_clear:
                arity = 0;
                s = "clear";
                break;
            case Id_entries:
                arity = 0;
                s = "entries";
                break;
            case Id_values:
                arity = 0;
                s = "values";
                break;
            case Id_forEach:
                arity = 1;
                s = "forEach";
                break;
            case Id_addAll:
                arity = 0;
                s = "addAll";
                break;
            case Id_deleteAll:
                arity = 1;
                s = "deleteAll";
                break;
            case Id_every:
                arity = 1;
                s = "every";
                break;
            case Id_filter:
                arity = 1;
                s = "filter";
                break;
            case Id_find:
                arity = 1;
                s = "find";
                break;
            case Id_join:
                arity = 1;
                s = "join";
                break;
            case Id_map:
                arity = 1;
                s = "map";
                break;
            case Id_reduce:
                arity = 1;
                s = "reduce";
                break;
            case Id_some:
                arity = 1;
                s = "some";
                break;
            case Id_intersection:
                arity = 1;
                s = "intersection";
                break;
            case Id_union:
                arity = 1;
                s = "union";
                break;
            case Id_difference:
                arity = 1;
                s = "difference";
                break;
            case Id_symmetricDifference:
                arity = 1;
                s = "symmetricDifference";
                break;
            case Id_isDisjointFrom:
                arity = 1;
                s = "isDisjointFrom";
                break;
            case Id_isSubsetOf:
                arity = 1;
                s = "isSubsetOf";
                break;
            case Id_isSupersetOf:
                arity = 1;
                s = "isSupersetOf";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(SET_TAG, id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (GETSIZE.equals(k)) {
            return SymbolId_getSize;
        }
        if (SymbolKey.ITERATOR.equals(k)) {
            return Id_values;
        }
        if (SymbolKey.TO_STRING_TAG.equals(k)) {
            return SymbolId_toStringTag;
        }
        return 0;
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2019-12-22 18:11:48 PST
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 3: c=s.charAt(0);
                if (c=='a') { if (s.charAt(2)=='d' && s.charAt(1)=='d') {id=Id_add; break L0;} }
                else if (c=='h') { if (s.charAt(2)=='s' && s.charAt(1)=='a') {id=Id_has; break L0;} }
                else if (c=='m') { if (s.charAt(2)=='p' && s.charAt(1)=='a') {id=Id_map; break L0;} }
                break L;
            case 4: switch (s.charAt(0)) {
                case 'f': X="find";id=Id_find; break L;
                case 'j': X="join";id=Id_join; break L;
                case 'k': X="keys";id=Id_keys; break L;
                case 's': X="some";id=Id_some; break L;
                } break L;
            case 5: c=s.charAt(0);
                if (c=='c') { X="clear";id=Id_clear; }
                else if (c=='e') { X="every";id=Id_every; }
                else if (c=='u') { X="union";id=Id_union; }
                break L;
            case 6: switch (s.charAt(0)) {
                case 'a': X="addAll";id=Id_addAll; break L;
                case 'd': X="delete";id=Id_delete; break L;
                case 'f': X="filter";id=Id_filter; break L;
                case 'r': X="reduce";id=Id_reduce; break L;
                case 'v': X="values";id=Id_values; break L;
                } break L;
            case 7: c=s.charAt(0);
                if (c=='e') { X="entries";id=Id_entries; }
                else if (c=='f') { X="forEach";id=Id_forEach; }
                break L;
            case 9: X="deleteAll";id=Id_deleteAll; break L;
            case 10: c=s.charAt(0);
                if (c=='d') { X="difference";id=Id_difference; }
                else if (c=='i') { X="isSubsetOf";id=Id_isSubsetOf; }
                break L;
            case 11: X="constructor";id=Id_constructor; break L;
            case 12: c=s.charAt(1);
                if (c=='n') { X="intersection";id=Id_intersection; }
                else if (c=='s') { X="isSupersetOf";id=Id_isSupersetOf; }
                break L;
            case 14: X="isDisjointFrom";id=Id_isDisjointFrom; break L;
            case 19: X="symmetricDifference";id=Id_symmetricDifference; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    // Note that SymbolId_iterator is not present because it is required to have the
    // same value as the "values" entry.
    // Similarly, "keys" is supposed to have the same value as "values," which is why
    // both have the same ID.
    private static final int
            Id_constructor = 1,
            Id_add = 2,
            Id_delete = 3,
            Id_has = 4,
            Id_clear = 5,
            Id_keys = 6,
            Id_values = 6,  // These are deliberately the same to match the spec
            Id_entries = 7,
            Id_forEach = 8,
            Id_addAll = 9,
            Id_deleteAll = 10,
            Id_every = 11,
            Id_filter = 12,
            Id_find = 13,
            Id_join = 14,
            Id_map = 15,
            Id_reduce = 16,
            Id_some = 17,
            Id_intersection = 18,
            Id_union = 19,
            Id_difference = 20,
            Id_symmetricDifference = 21,
            Id_isDisjointFrom = 22,
            Id_isSubsetOf = 23,
            Id_isSupersetOf = 24,
            SymbolId_getSize = 25,
            SymbolId_toStringTag = 26,
            MAX_PROTOTYPE_ID = SymbolId_toStringTag;

// #/string_id_map#
}

