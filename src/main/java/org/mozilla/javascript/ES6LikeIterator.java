package org.mozilla.javascript;

import java.util.Iterator;

public class ES6LikeIterator extends ES6Iterator {
    private Iterator iterator;

    public static ES6LikeIterator from(Context cx, Scriptable scope, Object target) {
        return new ES6LikeIterator(new IteratorLikeIterable(cx, scope, target));
    }

    ES6LikeIterator(IteratorLikeIterable iterator) {
        this.iterator = iterator.iterator();
    }

    @Override
    public boolean isDone(Context cx, Scriptable scope) {
        return !iterator.hasNext();
    }

    @Override
    public Object nextValue(Context cx, Scriptable scope) {
        Object o = iterator.next();
        return o;
    }

    @Override
    public String getClassName() {
        return "Iterator";
    }
}
