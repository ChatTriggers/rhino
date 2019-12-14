package org.mozilla.javascript.tools.compattable;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class CompatTableContextFactory extends ContextFactory {
    public static final int MAX_RUNTIME = 60000;

    static {
        ContextFactory.initGlobal(new CompatTableContextFactory());
    }

    private static class CompatTableContext extends Context {
        long startTime;
    }

    @Override
    protected void onContextCreated(Context cx) {
        cx.setLanguageVersion(Context.VERSION_ES6);
    }

    @Override
    protected Context makeContext() {
        CompatTableContext cx = new CompatTableContext();
        cx.setInstructionObserverThreshold(10000);
        return cx;
    }

    @Override
    protected void observeInstructionCount(Context cx, int instructionCount) {
        if (System.currentTimeMillis() - ((CompatTableContext) cx).startTime > MAX_RUNTIME) {
            throw new Error("Max timeout exceeded");
        }
    }

    @Override
    protected Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ((CompatTableContext) cx).startTime = System.currentTimeMillis();
        return super.doTopCall(callable, cx, scope, thisObj, args);
    }
}
