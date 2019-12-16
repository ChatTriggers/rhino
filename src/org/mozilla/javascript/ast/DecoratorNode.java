package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.decorators.DecoratorType;

import java.util.ArrayList;
import java.util.List;

public class DecoratorNode extends ScriptNode {
    private Name name;
    private List<AstNode> arguments = new ArrayList<>();
    private DecoratorType decoratorType = DecoratorType.UNKNOWN;

    {
        type = Token.XMLATTR;
    }

    public DecoratorNode(Name name) {
        this.name = name;
    }

    public DecoratorNode(Name name, List<AstNode> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public List<AstNode> getArguments() {
        return arguments;
    }

    public void setArguments(List<AstNode> arguments) {
        this.arguments = arguments;
    }

    public DecoratorType getDecoratorType() {
        return decoratorType;
    }

    public void setDecoratorType(DecoratorType decoratorType) {
        this.decoratorType = decoratorType;
    }
}
