package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.decorators.DecoratorType;

public class DecoratorNode extends FunctionCall {
    private DecoratorType decoratorType;

    {
        putProp(Node.DECORATOR_PROP, true);
    }

    public DecoratorNode(int pos, Name name) {
        super(pos);
        setTarget(name);
    }

    public DecoratorType getDecoratorType() {
        return decoratorType;
    }

    public void setDecoratorType(DecoratorType decoratorType) {
        this.decoratorType = decoratorType;
    }
}
