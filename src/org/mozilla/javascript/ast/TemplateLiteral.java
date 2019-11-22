package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

import java.util.ArrayList;
import java.util.List;

public class TemplateLiteral extends AstNode {
    private List<AstNode> elements = new ArrayList<>();

    {
        this.type = Token.TEMPLATE;
    }

    public void addString(String literal) {
        StringLiteral lit = new StringLiteral();
        lit.setValue(literal);
        elements.add(lit);
    }

    public void addExpr(AstNode expr) {
        elements.add(expr);
    }

    public List<AstNode> getElements() {
        return elements;
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
}
