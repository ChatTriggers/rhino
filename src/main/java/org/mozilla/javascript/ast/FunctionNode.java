/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

import java.util.*;

/**
 * A JavaScript function declaration or expression.
 * <p>Node type is {@link Token#FUNCTION}.</p>
 *
 * <pre><i>FunctionDeclaration</i> :
 *        <b>function</b> Identifier ( FormalParameterListopt ) { FunctionBody }
 * <i>FunctionExpression</i> :
 *        <b>function</b> Identifieropt ( FormalParameterListopt ) { FunctionBody }
 * <i>FormalParameterList</i> :
 *        Identifier
 *        FormalParameterList , Identifier
 * <i>FunctionBody</i> :
 *        SourceElements
 * <i>Program</i> :
 *        SourceElements
 * <i>SourceElements</i> :
 *        SourceElement
 *        SourceElements SourceElement
 * <i>SourceElement</i> :
 *        Statement
 *        FunctionDeclaration</pre>
 * <p>
 * JavaScript 1.8 introduces "function closures" of the form
 * <pre>function ([params] ) Expression</pre>
 * <p>
 * In this case the FunctionNode node will have no body but will have an
 * expression.
 */
public class FunctionNode extends ScriptNode {

    /**
     * There are three types of functions that can be defined. The first
     * is a function statement. This is a function appearing as a top-level
     * statement (i.e., not nested inside some other statement) in either a
     * script or a function.<p>
     * <p>
     * The second is a function expression, which is a function appearing in
     * an expression except for the third type, which is...<p>
     * <p>
     * The third type is a function expression where the expression is the
     * top-level expression in an expression statement.<p>
     * <p>
     * The three types of functions have different treatment and must be
     * distinguished.
     */
    public static final int FUNCTION_STATEMENT = 1;
    public static final int FUNCTION_EXPRESSION = 2;
    public static final int FUNCTION_EXPRESSION_STATEMENT = 3;
    public static final int ARROW_FUNCTION = 4;

    public enum Form {FUNCTION, GETTER, SETTER, METHOD}

    private static final List<AstNode> NO_PARAMS = Collections.unmodifiableList(new ArrayList<>());
    private static final Map<Integer, Node> NO_DEFAULT_PARAMS = Collections.unmodifiableMap(new HashMap<>());

    private Name functionName;
    private List<AstNode> params;
    private Map<Integer, Node> defaultParams;

    private boolean hasComplexParameters = false;

    private AstNode body;
    private boolean isExpressionClosure;
    private boolean isConstructable = true;
    private ClassNode parentClass = null;
    private boolean isStatic = false;
    private boolean isPrivate = false;
    private Form functionForm = Form.FUNCTION;
    private int lp = -1;
    private int rp = -1;

    // codegen variables
    private int functionType;
    private boolean needsActivation;
    private boolean isGenerator;
    private List<Node> generatorResumePoints = new ArrayList<>();
    private Map<Node, int[]> liveLocals;

    {
        type = Token.FUNCTION;
    }

    public FunctionNode() {
    }

    public FunctionNode(int pos) {
        super(pos);
    }

    public FunctionNode(int pos, Name name) {
        super(pos);
        setFunctionName(name);
    }

    /**
     * Returns function name
     *
     * @return function name, {@code null} for anonymous functions
     */
    public Name getFunctionName() {
        return functionName;
    }

    /**
     * Sets function name, and sets its parent to this node.
     *
     * @param name function name, {@code null} for anonymous functions
     */
    public void setFunctionName(Name name) {
        functionName = name;
        if (name != null)
            name.setParent(this);
    }

    /**
     * Returns the function name as a string
     *
     * @return the function name, {@code ""} if anonymous
     */
    public String getName() {
        return functionName != null ? functionName.getIdentifier() : "";
    }

    /**
     * Returns the function parameter list
     *
     * @return the function parameter list.  Returns an immutable empty
     * list if there are no parameters.
     */
    public List<AstNode> getParams() {
        return params != null ? params : NO_PARAMS;
    }

    public boolean hasComplexParameters() {
        return hasComplexParameters;
    }

    public void setHasComplexParameters() {
        this.hasComplexParameters = true;
    }

    /**
     * Returns the function parameter list
     *
     * @return the function parameter list.  Returns an immutable empty
     * list if there are no parameters.
     */
    public Map<Integer, Node> getDefaultParams() {
        return defaultParams != null ? defaultParams : NO_DEFAULT_PARAMS;
    }

    public void setDefaultParam(int index, Node transformed) {
        defaultParams.put(index, transformed);
    }

    /**
     * Sets the function parameter list, and sets the parent for
     * each element of the list.
     *
     * @param params the function parameter list, or {@code null} if no params
     */
    public void setParams(List<AstNode> params) {
        if (params == null) {
            this.params = null;
        } else {
            if (this.params != null)
                this.params.clear();
            for (AstNode param : params)
                addParam(param);
        }
    }

    /**
     * Adds a parameter to the function parameter list.
     * Sets the parent of the param node to this node.
     *
     * @param param the parameter
     * @throws IllegalArgumentException if param is {@code null}
     */
    public void addParam(AstNode param) {
        assertNotNull(param);
        if (params == null) {
            params = new ArrayList<>();
        }
        params.add(param);
        param.setParent(this);
    }

    public void addDefaultParam(int index, AstNode defaultParam) {
        assertNotNull(defaultParam);
        if (defaultParams == null) {
            defaultParams = new HashMap<>();
        }
        defaultParams.put(index, defaultParam);
        defaultParam.setParent(this);
    }

    /**
     * Returns true if the specified {@link AstNode} node is a parameter
     * of this Function node.  This provides a way during AST traversal
     * to disambiguate the function name node from the parameter nodes.
     */
    public boolean isParam(AstNode node) {
        return params != null && params.contains(node);
    }

    /**
     * Returns function body.  Normally a {@link Block}, but can be a plain
     * {@link AstNode} if it's a function closure.
     *
     * @return the body.  Can be {@code null} only if the AST is malformed.
     */
    public AstNode getBody() {
        return body;
    }

    /**
     * Sets function body, and sets its parent to this node.
     * Also sets the encoded source bounds based on the body bounds.
     * Assumes the function node absolute position has already been set,
     * and the body node's absolute position and length are set.<p>
     *
     * @param body function body.  Its parent is set to this node, and its
     *             position is updated to be relative to this node.
     * @throws IllegalArgumentException if body is {@code null}
     */
    public void setBody(AstNode body) {
        assertNotNull(body);
        this.body = body;
        if (Boolean.TRUE.equals(body.getProp(Node.EXPRESSION_CLOSURE_PROP))) {
            setIsExpressionClosure(true);
        }
        int absEnd = body.getPosition() + body.getLength();
        body.setParent(this);
        this.setLength(absEnd - this.position);
        setEncodedSourceBounds(this.position, absEnd);
    }

    /**
     * Returns left paren position, -1 if missing
     */
    public int getLp() {
        return lp;
    }

    /**
     * Sets left paren position
     */
    public void setLp(int lp) {
        this.lp = lp;
    }

    /**
     * Returns right paren position, -1 if missing
     */
    public int getRp() {
        return rp;
    }

    /**
     * Sets right paren position
     */
    public void setRp(int rp) {
        this.rp = rp;
    }

    /**
     * Sets both paren positions
     */
    public void setParens(int lp, int rp) {
        this.lp = lp;
        this.rp = rp;
    }

    /**
     * Returns whether this is a 1.8 function closure
     */
    public boolean isExpressionClosure() {
        return isExpressionClosure;
    }

    /**
     * Sets whether this is a 1.8 function closure
     */
    public void setIsExpressionClosure(boolean isExpressionClosure) {
        this.isExpressionClosure = isExpressionClosure;
    }

    public boolean isConstructable() {
        return isConstructable;
    }

    public void setConstructable(boolean constructable) {
        isConstructable = constructable;
    }

    public boolean isClassConstructor() {
        return parentClass != null;
    }

    public ClassNode getParentClass() {
        return parentClass;
    }

    public void setParentClass(ClassNode classConstructor) {
        parentClass = classConstructor;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    /**
     * Return true if this function requires an Ecma-262 Activation object.
     * The Activation object is implemented by
     * {@link org.mozilla.javascript.NativeCall}, and is fairly expensive
     * to create, so when possible, the interpreter attempts to use a plain
     * call frame instead.
     *
     * @return true if this function needs activation.  It could be needed
     * if there is a lexical closure, or in a number of other situations.
     */
    public boolean requiresActivation() {
        return needsActivation;
    }

    public void setRequiresActivation() {
        needsActivation = true;
    }

    public boolean isGenerator() {
        return isGenerator;
    }

    public void setIsGenerator() {
        isConstructable = false;
        isGenerator = true;
    }

    public void addResumptionPoint(Node target) {
        generatorResumePoints.add(target);
    }

    public List<Node> getResumptionPoints() {
        return generatorResumePoints;
    }

    public Map<Node, int[]> getLiveLocals() {
        return liveLocals;
    }

    public void addLiveLocals(Node node, int[] locals) {
        if (liveLocals == null)
            liveLocals = new HashMap<>();
        liveLocals.put(node, locals);
    }

    @Override
    public int addFunction(FunctionNode fnNode) {
        int result = super.addFunction(fnNode);
        if (getFunctionCount() > 0) {
            needsActivation = true;
        }
        return result;
    }

    /**
     * Returns the function type (statement, expr, statement expr)
     */
    public int getFunctionType() {
        return functionType;
    }

    public void setFunctionType(int type) {
        functionType = type;
    }

    public boolean isMethod() {
        return functionForm == Form.GETTER || functionForm == Form.SETTER || functionForm == Form.METHOD;
    }

    public boolean isGetterMethod() {
        return functionForm == Form.GETTER;
    }

    public boolean isSetterMethod() {
        return functionForm == Form.SETTER;
    }

    public boolean isNormalMethod() {
        return functionForm == Form.METHOD;
    }

    public void setFunctionIsGetterMethod() {
        functionForm = Form.GETTER;
    }

    public void setFunctionIsSetterMethod() {
        functionForm = Form.SETTER;
    }

    public void setFunctionIsNormalMethod() {
        functionForm = Form.METHOD;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        boolean isArrow = functionType == ARROW_FUNCTION;
        if (!isMethod()) {
            sb.append(makeIndent(depth));
            if (!isArrow) {
                sb.append("function");
            }
        }
        if (functionName != null) {
            sb.append(" ");
            sb.append(functionName.toSource(0));
        }
        if (params == null) {
            sb.append("() ");
        } else if (isArrow && lp == -1) {
            // no paren
            printList(params, sb);
            sb.append(" ");
        } else {
            sb.append("(");
            printList(params, sb);
            sb.append(") ");
        }
        if (isArrow) {
            sb.append("=> ");
        }
        if (isExpressionClosure) {
            AstNode body = getBody();
            if (body.getLastChild() instanceof ReturnStatement) {
                // omit "return" keyword, just print the expression
                body = ((ReturnStatement) body.getLastChild()).getReturnValue();
                sb.append(body.toSource(0));
                if (functionType == FUNCTION_STATEMENT) {
                    sb.append(";");
                }
            } else {
                // should never happen
                sb.append(" ");
                sb.append(body.toSource(0));
            }
        } else {
            sb.append(getBody().toSource(depth).trim());
        }
        if (functionType == FUNCTION_STATEMENT || isMethod()) {
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Visits this node, the function name node if supplied,
     * the parameters, and the body.  If there is a member-expr node,
     * it is visited last.
     */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            if (functionName != null) {
                functionName.visit(v);
            }
            for (AstNode param : getParams()) {
                param.visit(v);
            }
            getBody().visit(v);
        }
    }
}
