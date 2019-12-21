package org.mozilla.javascript.ast;

public interface ClassElement {
    void setIsStatic();

    boolean isStatic();

    void setIsPrivate();

    boolean isPrivate();

    Object getNameKey();

    void setNameKey(Object nameKey);
}
