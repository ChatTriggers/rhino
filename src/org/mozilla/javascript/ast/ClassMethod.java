package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

public class ClassMethod extends InfixExpression {
    private boolean isStatic = false;
    private AstNode name;
    private FunctionNode function;

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
}
