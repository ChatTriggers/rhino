package org.mozilla.javascript.decorators;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public interface Decorator {
    int CLASS   = 1 << 0;
    int METHOD  = 1 << 1;
    int FIELD   = 1 << 2;
    int PUBLIC  = 1 << 3;
    int PRIVATE = 1 << 4;
    int STATIC  = 1 << 5;

    Object consume(Context cx, Scriptable scope, Scriptable thisObj, Object obj, int descriptor, Object[] descriptorArgs, Object[] metadata);
}
