package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TemplateLiteral extends AstNode {
    private List<AstNode> elements = new ArrayList<>();
    private Set<Integer> isExpr = new HashSet<>();
    private AstNode target = null;
    private Node transformedTarget = null;

    {
        this.type = Token.TEMPLATE;
    }

    public void addString(String literal) {
        StringLiteral lit = new StringLiteral();
        lit.setValue(literal);
        elements.add(lit);
    }

    public void addExpr(AstNode expr) {
        isExpr.add(elements.size());
        elements.add(expr);
    }

    public List<AstNode> getElements() {
        return elements;
    }

    public boolean isExpr(int index) {
        return isExpr.contains(index);
    }

    @Override
    public String toSource(int depth) {
        return "";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            for (AstNode e : elements) {
                e.visit(v);
            }
        }
    }

    public void setTarget(AstNode target) {
        this.target = target;
    }

    public AstNode getTarget() {
        return target;
    }

    public void setTransformedTarget(Node transformedTarget) {
        this.transformedTarget = transformedTarget;
    }

    public Node getTransformedTarget() {
        return transformedTarget;
    }
}
