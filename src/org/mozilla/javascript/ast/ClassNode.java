package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

import java.util.ArrayList;
import java.util.List;

public class ClassNode extends ScriptNode {
    private Name className;
    private AstNode extendsName;
    private FunctionNode constructor = null;
    private List<ClassMethod> classMethods = new ArrayList<>();
    private Node parentFn = null;

    {
        type = Token.CLASS;
    }

    public ClassNode() {

    }

    public ClassNode(int pos) {
        super(pos);
    }

    public ClassNode(int pos, Name name) {
        super(pos);
        className = name;
        if (name != null)
            name.setParent(this);
    }

    public Name getClassName() {
        return className;
    }

    public void setClassName(Name name) {
        this.className = name;
    }

    public void setExtendsNode(AstNode name) {
        this.extendsName = name;
    }

    public AstNode getExtendsName() {
        return extendsName;
    }

    public FunctionNode getConstructor() {
        return constructor;
    }

    public void setConstructor(FunctionNode constructor) {
        this.constructor = constructor;
    }

    public List<ClassMethod> getClassMethods() {
        return classMethods;
    }

    public void setClassMethods(List<ClassMethod> classMethods) {
        this.classMethods = classMethods;
    }

    public Node getParentFn() {
        return parentFn;
    }

    public void setParentFn(Node parentFn) {
        this.parentFn = parentFn;
    }
}
