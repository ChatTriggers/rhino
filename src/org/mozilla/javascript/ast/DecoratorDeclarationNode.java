package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

import java.util.ArrayList;
import java.util.List;

public class DecoratorDeclarationNode extends FunctionNode {
    private List<DecoratorNode> decoratorNodes = new ArrayList<>();

    {
        type = Token.DECORATOR;
        putProp(Node.DECORATOR_PROP, true);
        setRequiresActivation();
    }

    public List<DecoratorNode> getDecoratorNodes() {
        return decoratorNodes;
    }

    public void setDecoratorNodes(List<DecoratorNode> decoratorNodes) {
        this.decoratorNodes = decoratorNodes;
    }

    public void addDecoratorNode(DecoratorNode node) {
        this.decoratorNodes.add(node);
    }
}
