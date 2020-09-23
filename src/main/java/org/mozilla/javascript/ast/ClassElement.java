package org.mozilla.javascript.ast;

import java.util.List;

public interface ClassElement {
    void setIsStatic();

    boolean isStatic();

    void setIsPrivate();

    boolean isPrivate();

    Object getNameKey();

    void setNameKey(Object nameKey);

    List<DecoratorNode> getDecorators();
}
