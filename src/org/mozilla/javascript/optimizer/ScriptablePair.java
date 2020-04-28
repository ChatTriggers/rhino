package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Scriptable;

public class ScriptablePair {
    private final Scriptable first;
    private final Scriptable second;

    public ScriptablePair(Scriptable first, Scriptable second) {
        this.first = first;
        this.second = second;
    }

    public Scriptable getFirst() {
        return first;
    }

    public Scriptable getSecond() {
        return second;
    }
}
