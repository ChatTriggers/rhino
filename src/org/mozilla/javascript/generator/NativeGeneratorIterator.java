package org.mozilla.javascript.generator;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ES6Iterator;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import java.util.Iterator;

public class NativeGeneratorIterator extends ES6Iterator {
    private static final String GEN_ITERATOR_TAG = "GeneratorIterator";
    private NativeGenerator generator;
    private Scriptable scope;

    public NativeGeneratorIterator(Scriptable scope, NativeGenerator generator) {
        this.scope = scope;
        this.generator = generator;
    }

    @Override
    public boolean isDone(Context cx, Scriptable scope) {
        return generator.done;
    }

    @Override
    public Object nextValue(Context cx, Scriptable scope) {
        return generator.resume(cx, scope, NativeGenerator.GENERATOR_SEND, Undefined.instance);
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
