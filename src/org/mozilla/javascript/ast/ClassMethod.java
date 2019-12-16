package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

import java.util.ArrayList;
import java.util.List;

public class ClassMethod extends InfixExpression {
    private boolean isStatic = false;
    private AstNode name;
    private FunctionNode function;
    private Object nameKey;
    private List<DecoratorNode> decorators = new ArrayList<>();

    {
        type = Token.METHOD;
    }

    public ClassMethod(AstNode name, FunctionNode function) {
        super(name, function);
        this.name = name;
        this.function = function;
    }

    public void setIsStatic() {
        isStatic = true;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public AstNode getName() {
        return name;
    }

    public FunctionNode getFunction() {
        return function;
    }

    public void setIsGetterMethod() {
        type = Token.GET;
    }

    public boolean isGetterMethod() {
        return type == Token.GET;
    }

    public void setIsSetterMethod() {
        type = Token.SET;
    }

    public boolean isSetterMethod() {
        return type == Token.SET;
    }

    public Object getNameKey() {
        return nameKey;
    }

    public void setNameKey(Object nameKey) {
        this.nameKey = nameKey;
    }

    public List<DecoratorNode> getDecorators() {
        return decorators;
    }

    public void setDecorators(List<DecoratorNode> decorators) {
        this.decorators = decorators;
    }
}
