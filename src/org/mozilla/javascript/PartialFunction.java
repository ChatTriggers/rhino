package org.mozilla.javascript;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PartialFunction extends ArrowFunction {
    List<Integer> partialIndices;
    Object[] appliedObjects;

    public PartialFunction(Context cx, Scriptable scope, Callable targetFunction, Scriptable boundThis, int[] partialIndices, Object[] appliedObjects) {
        super(cx, scope, targetFunction, boundThis);

        this.partialIndices = Arrays.stream(partialIndices).boxed().collect(Collectors.toList());
        this.appliedObjects = appliedObjects;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] oldArgs) {
        Scriptable callThis = boundThis != null ? boundThis : ScriptRuntime.getTopCallScope(cx);

        Object[] newArgs = new Object[oldArgs.length + appliedObjects.length];

        int appliedIndex = 0;
        int oldArgsIndex = 0;

        for (int i = 0; i < newArgs.length; i++) {
            int indexOf = partialIndices.indexOf(i);
            if (indexOf == -1) {
                newArgs[i] = appliedObjects[appliedIndex++];
            } else {
                newArgs[i] = oldArgs[oldArgsIndex++];
            }
        }

        return targetFunction.call(cx, scope, callThis, newArgs);
    }
}
