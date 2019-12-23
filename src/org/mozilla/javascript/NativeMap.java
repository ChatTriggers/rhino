/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;

public class NativeMap extends IdScriptableObject {
    private static final long serialVersionUID = 1171922614280016891L;
    private static final Object MAP_TAG = "Map";
    static final String ITERATOR_TAG = "Map Iterator";

    private static final Object NULL_VALUE = new Object();

    private final Hashtable entries = new Hashtable();

    private boolean instanceOfMap = false;

    static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeMap obj = new NativeMap();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false);

        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        desc.put("enumerable", desc, false);
        desc.put("configurable", desc, true);
        desc.put("get", desc, obj.get(NativeSet.GETSIZE, obj));
        obj.defineOwnProperty(cx, "size", desc);

        if (sealed) {
            obj.sealObject();
        }
    }

    @Override
    public String getClassName() {
        return "Map";
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        addIdFunctionProperty(ctor, MAP_TAG, ConstructorId_groupBy, "groupBy", 1);
        addIdFunctionProperty(ctor, MAP_TAG, ConstructorId_keyBy, "keyBy", 1);
        addCtorSpecies(ctor);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args) {
        if (!f.hasTag(MAP_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case ConstructorId_groupBy:
                return js_groupBy(cx, scope, args);
            case ConstructorId_keyBy:
                return js_keyBy(cx, scope, args);
            case Id_constructor:
                return js_construct(cx, scope, thisObj, args);
            case Id_set:
                return realThis(thisObj, f).js_set(
                        args.length > 0 ? args[0] : Undefined.instance,
                        args.length > 1 ? args[1] : Undefined.instance);
            case Id_delete:
                return realThis(thisObj, f).js_delete(args.length > 0 ? args[0] : Undefined.instance);
            case Id_get:
                return realThis(thisObj, f).js_get(args.length > 0 ? args[0] : Undefined.instance);
            case Id_has:
                return realThis(thisObj, f).js_has(args.length > 0 ? args[0] : Undefined.instance);
            case Id_clear:
                return realThis(thisObj, f).js_clear();
            case Id_keys:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.KEYS);
            case Id_values:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.VALUES);
            case Id_entries:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.BOTH);
            case Id_forEach:
                return realThis(thisObj, f).js_forEach(cx, scope,
                        args.length > 0 ? args[0] : Undefined.instance,
                        args.length > 1 ? args[1] : Undefined.instance);
            case Id_mapKeys:
            case Id_mapValues:
                return realThis(thisObj, f).js_map(cx, scope, args, id == Id_mapKeys);
            case Id_keyOf:
                return realThis(thisObj, f).js_keyOf(cx, scope, args);
            case Id_includes:
                return realThis(thisObj, f).js_includes(cx, scope, args);
            case Id_find:
            case Id_findKey:
                return realThis(thisObj, f).js_find(cx, scope, args, id == Id_findKey);
            case Id_some:
                return realThis(thisObj, f).js_some(cx, scope, args);
            case Id_every:
                return realThis(thisObj, f).js_every(cx, scope, args);
            case Id_reduce:
                return realThis(thisObj, f).js_reduce(cx, scope, args);
            case Id_deleteAll:
                return realThis(thisObj, f).js_deleteAll(cx, scope, args);
            case Id_update:
                return realThis(thisObj, f).js_update(cx, scope, args);
            case Id_filter:
                return realThis(thisObj, f).js_filter(cx, scope, args);
            case Id_merge:
                return realThis(thisObj, f).js_merge(cx, scope, args);
            case SymbolId_getSize:
                return realThis(thisObj, f).js_getSize();
        }
        throw new IllegalArgumentException("Map.prototype has no method: " + f.getFunctionName());
    }

    private NativeMap js_construct(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (thisObj == null) {
            NativeMap nm = new NativeMap();
            nm.instanceOfMap = true;
            if (args.length > 0) {
                loadFromIterable(cx, scope, nm, args[0]);
            }
            return nm;
        }
        throw ScriptRuntime.typeError1("msg.no.new", "Map");
    }

    private Object js_groupBy(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeMap map = (NativeMap) species.construct(cx, scope, new Object[0]);

        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg1;
        ES6Iterator iterator = ScriptRuntime.toIterator(cx, scope, ScriptableObject.ensureScriptable(arg0), false);

        Object next;

        while (true) {
            next = iterator.next(cx, scope);

            if (!(next instanceof Scriptable) || ScriptRuntime.toBoolean(ScriptableObject.getProperty((Scriptable) next, "done"))) {
                break;
            }

            Object item = ScriptableObject.getProperty((Scriptable) next, "value");
            Object key = cb.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{ item });
            boolean entryWasFound = false;

            for (Hashtable.Entry en : map.entries) {
                if (ScriptRuntime.sameZero(en.key, key)) {
                    Object grouped = en.value;
                    if (!(grouped instanceof NativeArray)) {
                        throw Kit.codeBug("Expected grouped to be a NativeArray");
                    }

                    ScriptableObject.callMethod((NativeArray) grouped, "push", new Object[]{ item });
                    entryWasFound = true;
                }
            }

            if (!entryWasFound) {
                NativeArray groupedList = cx.newArray(scope, new Object[]{ item });
                map.entries.put(key, groupedList);
            }
        }

        return map;
    }

    private Object js_keyBy(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeMap map = (NativeMap) species.construct(cx, scope, new Object[0]);

        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg1;
        ES6Iterator iterator = ScriptRuntime.toIterator(cx, scope, ScriptableObject.ensureScriptable(arg0), false);

        while (true) {
            Object next = iterator.next(cx, scope);

            if (!(next instanceof Scriptable) || ScriptRuntime.toBoolean(ScriptableObject.getProperty((Scriptable) next, "done"))) {
                break;
            }

            Object item = ScriptableObject.getProperty((Scriptable) next, "value");
            Object key = cb.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{ item });

            map.entries.put(key, item);
        }

        return map;
    }

    private Object js_set(Object k, Object v) {
        // Map.get() does not distinguish between "not found" and a null value. So,
        // replace true null here with a marker so that we can re-convert in "get".
        final Object value = (v == null ? NULL_VALUE : v);
        // Special handling of "negative zero" from the spec.
        Object key = k;
        if ((key instanceof Number) &&
                ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
            key = 0.0;
        }
        entries.put(key, value);
        return this;
    }

    private Object js_delete(Object arg) {
        final Object e = entries.delete(arg);
        return (e != null);
    }

    private Object js_get(Object arg) {
        final Object val = entries.get(arg);
        if (val == null) {
            return Undefined.instance;
        }
        if (val == NULL_VALUE) {
            return null;
        }
        return val;
    }

    private boolean js_has(Object arg) {
        return entries.has(arg);
    }

    private Object js_getSize() {
        return entries.size();
    }

    private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type) {
        return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator());
    }

    private Object js_clear() {
        entries.clear();
        return Undefined.instance;
    }

    private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.typeError2("msg.isnt.function.it.is", arg1, ScriptRuntime.typeof(arg1));
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
            Object val = e.value;
            if (val == NULL_VALUE) {
                val = null;
            }

            f.call(cx, scope, thisObj, new Object[]{val, e.key, this});
        }
        return Undefined.instance;
    }

    private Object js_map(Context cx, Scriptable scope, Object[] args, boolean keys) {
        Object cb = args.length > 0 ? args[0] : null;

        if (!(cb instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable callback = (Callable) cb;

        BaseFunction species = getSpecies(this);

        if (species == null) {
            // TODO: Error
            throw Kit.codeBug();
        }

        NativeMap nm = (NativeMap) species.construct(cx, scope, new Object[]{});

        for (Hashtable.Entry en : entries) {
            if (keys) {
                nm.entries.put(callback.call(cx, scope, this, new Object[]{en.value, en.key, this}), en.value);
            } else {
                nm.entries.put(en.key, callback.call(cx, scope, this, new Object[]{ en.value, en.key, this }));
            }
        }

        return nm;
    }

    private Object js_keyOf(Context cx, Scriptable scope, Object[] args) {
        Object searchElement = args.length > 0 ? args[0] : Undefined.instance;

        for (Hashtable.Entry en : entries) {
            if (ScriptRuntime.shallowEq(searchElement, en.value)) {
                return en.key;
            }
        }

        return Undefined.instance;
    }

    private Object js_includes(Context cx, Scriptable scope, Object[] args) {
        Object searchElement = args.length > 0 ? args[0] : Undefined.instance;

        for (Hashtable.Entry en : entries) {
            if (ScriptRuntime.sameZero(searchElement, en.value)) {
                return true;
            }
        }

        return false;
    }

    private Object js_find(Context cx, Scriptable scope, Object[] args, boolean key) {
        Object arg0 = args.length == 0 ? null : args[0];

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.object.not.callable", ScriptRuntime.toString(arg0));
        }

        Callable cb = (Callable) arg0;

        Object thisObj = args.length > 1 ? args[1] : null;

        if (!(thisObj instanceof Scriptable)) {
            thisObj = Undefined.SCRIPTABLE_UNDEFINED;
        }

        for (Hashtable.Entry en : entries) {
            boolean result = ScriptRuntime.toBoolean(cb.call(cx, scope, (Scriptable) thisObj, new Object[]{ en.value, en.key, this }));

            if (result) {
                return key ? en.key : en.value;
            }
        }

        return Undefined.instance;
    }

    private boolean js_some(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.object.not.callable", ScriptRuntime.toString(arg0));
        }

        Callable cb = (Callable) arg0;

        Object thisObj = args.length > 1 ? args[1] : null;

        if (!(thisObj instanceof Scriptable)) {
            thisObj = Undefined.SCRIPTABLE_UNDEFINED;
        }

        for (Hashtable.Entry en : entries) {
            boolean result = ScriptRuntime.toBoolean(cb.call(cx, scope, (Scriptable) thisObj, new Object[]{ en.value, en.key, this }));

            if (result) {
                return true;
            }
        }

        return false;
    }

    private Object js_every(Context cx, Scriptable scope, Object[] args) {
        Object arg0 = args.length == 0 ? null : args[0];

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.object.not.callable", ScriptRuntime.toString(arg0));
        }

        Callable cb = (Callable) arg0;

        Object thisObj = args.length > 1 ? args[1] : null;

        if (!(thisObj instanceof Scriptable)) {
            thisObj = Undefined.SCRIPTABLE_UNDEFINED;
        }

        for (Hashtable.Entry en : entries) {
            boolean result = ScriptRuntime.toBoolean(cb.call(cx, scope, (Scriptable) thisObj, new Object[]{ en.value, en.key, this }));

            if (!result) {
                return false;
            }
        }

        return true;
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
                accumulator = cb.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[]{ accumulator, en.value, en.key, this });
            }
            first = false;
        }

        if (first && Undefined.isUndefined(accumulator)) {
            throw ScriptRuntime.typeError("Map is empty and no accumulator was provided to the reduce method");
        }

        return accumulator;
    }

    private Object js_deleteAll(Context cx, Scriptable scope, Object[] args) {
        for (Object arg : args) {
            js_delete(arg);
        }

        return this;
    }

    private Object js_update(Context cx, Scriptable scope, Object[] args) {
        Object key = args.length == 0 ? Undefined.instance : args[0];
        Object arg1 = args.length > 1 ? args[1] : Undefined.instance;
        Object arg2 = args.length > 2 ? args[2] : Undefined.instance;

        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.typeError1("msg.object.not.callable", ScriptRuntime.toString(arg1));
        }

        Callable cb = (Callable) arg1;
        Callable thunk = arg2 instanceof Callable ? (Callable) arg2 : null;

        boolean present = js_has(key);

        if (!present && !(arg2 instanceof Callable)) {
            throw ScriptRuntime.typeError("Key is not present in map, and no value supplier was provided");
        }

        Object value = present ? js_get(key) : ((Callable) thunk).call(cx, scope, this, new Object[]{ key, this });
        Object newValue = cb.call(cx, scope, this, new Object[]{ value, key, this });
        js_set(key, newValue);

        return this;
    }

    private Object js_filter(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeMap map = (NativeMap) species.construct(cx, scope, new Object[0]);

        Object arg0 = args.length == 0 ? null : args[0];
        Object arg1 = args.length > 1 ? args[1] : null;

        if (!(arg0 instanceof Callable)) {
            throw ScriptRuntime.typeError("Callback is not callable");
        }

        Callable cb = (Callable) arg0;
        Scriptable thisObj = arg1 instanceof Scriptable ? (Scriptable) arg1 : Undefined.SCRIPTABLE_UNDEFINED;

        for (Hashtable.Entry en : entries) {
            Object result = cb.call(cx, scope, thisObj, new Object[]{ en.value, en.key, this });
            if (ScriptRuntime.toBoolean(result)) {
                map.entries.put(en.key, en.value);
            }
        }

        return map;
    }

    // TODO: Generalize to all iterables?
    // Spec just says iterables, but it implies that it should
    // really only work with other maps.
    private Object js_merge(Context cx, Scriptable scope, Object[] args) {
        Function species = getSpecies(this);

        if (species == null) {
            throw ScriptRuntime.typeError("'this' is not constructable");
        }

        NativeMap map = (NativeMap) species.construct(cx, scope, new Object[0]);

        for (Hashtable.Entry en : entries) {
            map.entries.put(en.key, en.value);
        }

        for (Object arg : args) {
            if (!(arg instanceof NativeMap)) {
                throw ScriptRuntime.typeError("Expected Map, got " + ScriptRuntime.typeof(arg));
            }

            NativeMap other = (NativeMap) arg;

            for (Hashtable.Entry en : other.entries) {
                map.entries.put(en.key, en.value);
            }
        }

        return map;
    }

    /**
     * If an "iterable" object was passed to the constructor, there are many many things
     * to do... Make this static because NativeWeakMap has the exact same requirement.
     */
    static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject map, Object arg1) {
        if ((arg1 == null) || Undefined.instance.equals(arg1)) {
            return;
        }

        // Call the "[Symbol.iterator]" property as a function.
        final Object ito = ScriptRuntime.callIterator(arg1, cx, scope);
        if (Undefined.instance.equals(ito)) {
            // Per spec, ignore if the iterator is undefined
            return;
        }

        // Find the "add" function of our own prototype, since it might have
        // been replaced. Since we're not fully constructed yet, create a dummy instance
        // so that we can get our own prototype.
        ScriptableObject dummy = ensureScriptableObject(cx.newObject(scope, map.getClassName()));
        final Callable set =
                ScriptRuntime.getPropFunctionAndThis(dummy.getPrototype(), "set", cx, scope);
        ScriptRuntime.lastStoredScriptable(cx);

        // Finally, run through all the iterated values and add them!
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
            for (Object val : it) {
                Scriptable sVal = ScriptableObject.ensureScriptable(val);
                if (sVal instanceof Symbol) {
                    throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(sVal));
                }
                Object finalKey = sVal.get(0, sVal);
                if (finalKey == NOT_FOUND) {
                    finalKey = Undefined.instance;
                }
                Object finalVal = sVal.get(1, sVal);
                if (finalVal == NOT_FOUND) {
                    finalVal = Undefined.instance;
                }
                set.call(cx, scope, map, new Object[]{finalKey, finalVal});
            }
        }
    }

    private NativeMap realThis(Scriptable thisObj, IdFunctionObject f) {
        if (thisObj == null) {
            throw incompatibleCallError(f);
        }
        try {
            final NativeMap nm = (NativeMap) thisObj;
            if (!nm.instanceOfMap) {
                // Check for "Map internal data tag"
                throw incompatibleCallError(f);
            }
            return nm;
        } catch (ClassCastException cce) {
            throw incompatibleCallError(f);
        }
    }

    @Override
    protected void initPrototypeId(int id) {
        switch (id) {
            case SymbolId_getSize:
                initPrototypeMethod(MAP_TAG, id, NativeSet.GETSIZE, "get size", 0);
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
            case Id_set:
                arity = 2;
                s = "set";
                break;
            case Id_get:
                arity = 1;
                s = "get";
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
            case Id_forEach:
                arity = 1;
                s = "forEach";
                break;
            case Id_mapKeys:
                arity = 1;
                s = "mapKeys";
                break;
            case Id_mapValues:
                arity = 1;
                s = "mapValues";
                break;
            case Id_keyOf:
                arity = 1;
                s = "keyOf";
                break;
            case Id_includes:
                arity = 1;
                s = "includes";
                break;
            case Id_find:
                arity = 1;
                s = "find";
                break;
            case Id_findKey:
                arity = 1;
                s = "findKey";
                break;
            case Id_some:
                arity = 1;
                s = "some";
                break;
            case Id_every:
                arity = 1;
                s = "every";
                break;
            case Id_reduce:
                arity = 1;
                s = "reduce";
                break;
            case Id_deleteAll:
                arity = 1;
                s = "deleteAll";
                break;
            case Id_update:
                arity = 1;
                s = "update";
                break;
            case Id_filter:
                arity = 1;
                s = "filter";
                break;
            case Id_merge:
                arity = 1;
                s = "merge";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(MAP_TAG, id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (NativeSet.GETSIZE.equals(k)) {
            return SymbolId_getSize;
        }
        if (SymbolKey.ITERATOR.equals(k)) {
            // ECMA spec says that the "Symbol.iterator" property of the prototype has the
            // "same value" as the "entries" property. We implement this by returning the
            // ID of "entries" when the iterator symbol is accessed.
            return Id_entries;
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
// #generated# Last update: 2019-12-22 16:16:03 PST
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 3: c=s.charAt(0);
                if (c=='g') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_get; break L0;} }
                else if (c=='h') { if (s.charAt(2)=='s' && s.charAt(1)=='a') {id=Id_has; break L0;} }
                else if (c=='s') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_set; break L0;} }
                break L;
            case 4: c=s.charAt(0);
                if (c=='f') { X="find";id=Id_find; }
                else if (c=='k') { X="keys";id=Id_keys; }
                else if (c=='s') { X="some";id=Id_some; }
                break L;
            case 5: switch (s.charAt(0)) {
                case 'c': X="clear";id=Id_clear; break L;
                case 'e': X="every";id=Id_every; break L;
                case 'k': X="keyOf";id=Id_keyOf; break L;
                case 'm': X="merge";id=Id_merge; break L;
                } break L;
            case 6: switch (s.charAt(0)) {
                case 'd': X="delete";id=Id_delete; break L;
                case 'f': X="filter";id=Id_filter; break L;
                case 'r': X="reduce";id=Id_reduce; break L;
                case 'u': X="update";id=Id_update; break L;
                case 'v': X="values";id=Id_values; break L;
                } break L;
            case 7: switch (s.charAt(1)) {
                case 'a': X="mapKeys";id=Id_mapKeys; break L;
                case 'i': X="findKey";id=Id_findKey; break L;
                case 'n': X="entries";id=Id_entries; break L;
                case 'o': X="forEach";id=Id_forEach; break L;
                } break L;
            case 8: X="includes";id=Id_includes; break L;
            case 9: c=s.charAt(0);
                if (c=='d') { X="deleteAll";id=Id_deleteAll; }
                else if (c=='m') { X="mapValues";id=Id_mapValues; }
                break L;
            case 11: X="constructor";id=Id_constructor; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    // Note that "SymbolId_iterator" is not present here. That's because the spec
    // requires that it be the same value as the "entries" prototype property.
    private static final int
            ConstructorId_groupBy = -1,
            ConstructorId_keyBy = -2,

            Id_constructor = 1,
            Id_set = 2,
            Id_get = 3,
            Id_delete = 4,
            Id_has = 5,
            Id_clear = 6,
            Id_keys = 7,
            Id_values = 8,
            Id_entries = 9,
            Id_forEach = 10,
            Id_mapKeys = 11,
            Id_mapValues = 12,
            Id_keyOf = 13,
            Id_includes = 14,
            Id_find = 15,
            Id_findKey = 16,
            Id_some = 17,
            Id_every = 18,
            Id_reduce = 19,
            Id_deleteAll = 20,
            Id_update = 21,
            Id_filter = 22,
            Id_merge = 23,
            SymbolId_getSize = 24,
            SymbolId_toStringTag = 25,
            MAX_PROTOTYPE_ID = SymbolId_toStringTag;

// #/string_id_map#
}
