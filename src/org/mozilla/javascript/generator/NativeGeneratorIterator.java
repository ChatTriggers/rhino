package org.mozilla.javascript.generator;

import org.mozilla.javascript.*;

import java.util.Iterator;

public class NativeGeneratorIterator extends ES6Iterator {
    private static final String GEN_ITERATOR_TAG = "GeneratorIterator";
    private NativeGenerator generator;
    private Object prevValue;
    private boolean done;

    public static void init(ScriptableObject scope, boolean sealed) {
        ES6Iterator.init(scope, sealed, new NativeGeneratorIterator(), GEN_ITERATOR_TAG);
    }

    private NativeGeneratorIterator() { super(); }

    public NativeGeneratorIterator(Context cx, Scriptable scope, NativeGenerator generator) {
        super(scope, GEN_ITERATOR_TAG);
        this.generator = generator;

        Object value = getNext(cx, scope);
        if (isDone(value)) {
            done = true;
        } else {
            prevValue = value;
        }
    }

    @Override
    public boolean isDone(Context cx, Scriptable scope) {
        return done;
    }

    @Override
    public Object nextValue(Context cx, Scriptable scope) {
        if (done) {
            return Undefined.instance;
        }

        Object toRet = prevValue;
        prevValue = getNext(cx, scope);

        if (isDone(prevValue)) {
            done = true;
        }

        return ScriptableObject.getProperty((Scriptable) toRet, "value");
    }

    private Object getNext(Context cx, Scriptable scope) {
        return generator.resume(cx, scope, NativeGenerator.GENERATOR_SEND, Undefined.instance);
    }

    private boolean isDone(Object obj) {
        if (!(obj instanceof NativeObject)) {
            throw Kit.codeBug();
        }

        return (Boolean) ScriptableObject.getProperty((Scriptable) obj, "done");
    }

    @Override
    public String getClassName() {
        return "Generator Iterator";
    }

    @Override
    protected String getTag() {
        return GEN_ITERATOR_TAG;
    }
}
