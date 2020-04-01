/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.ast.Symbol;
import org.mozilla.javascript.ast.*;
import org.mozilla.javascript.decorators.DecoratorType;
import org.mozilla.javascript.optimizer.Codegen;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class rewrites the parse tree into an IR suitable for codegen.
 *
 * @author Mike McCabe
 * @author Norris Boyd
 * @see Node
 */
public final class IRFactory extends Parser {
    private static final int LOOP_DO_WHILE = 0;
    private static final int LOOP_WHILE = 1;
    private static final int LOOP_FOR = 2;

    private static final int ALWAYS_TRUE_BOOLEAN = 1;
    private static final int ALWAYS_FALSE_BOOLEAN = -1;

    private Decompiler decompiler = new Decompiler();

    public IRFactory() {
        super();
    }

    public IRFactory(CompilerEnvirons env) {
        this(env, env.getErrorReporter());
    }

    public IRFactory(CompilerEnvirons env, ErrorReporter errorReporter) {
        super(env, errorReporter);
    }

    /**
     * Transforms the tree into a lower-level IR suitable for codegen.
     * Optionally generates the encoded source.
     */
    public ScriptNode transformTree(AstRoot root) {
        currentScriptOrFn = root;
        this.inUseStrictDirective = root.isInStrictMode();
        int sourceStartOffset = decompiler.getCurrentOffset();

        if (Token.printTrees) {
            System.out.println("IRFactory.transformTree");
            System.out.println(root.debugPrint());
        }
        ScriptNode script = (ScriptNode) transform(root);

        int sourceEndOffset = decompiler.getCurrentOffset();
        script.setEncodedSourceBounds(sourceStartOffset,
                sourceEndOffset);

        if (compilerEnv.isGeneratingSource()) {
            script.setEncodedSource(decompiler.getEncodedSource());
        }

        String baseName = "c";
        if (script.getSourceName().length() > 0) {
            baseName = script.getSourceName().replaceAll("\\W", "_");
            if (!Character.isJavaIdentifierStart(baseName.charAt(0))) {
                baseName = "_" + baseName;
            }
        }

        String mainClassName = "org.mozilla.javascript.gen." + baseName + "_" + (Codegen.globalSerialClassCounter + 1);

        File debugOutputPath = Context.getContext().getDebugOutputPath();

        if (debugOutputPath != null) {
            File irDir = new File(debugOutputPath, "ir");
            irDir.mkdirs();
            File outputIR = new File(irDir, mainClassName + ".txt");

            try (FileOutputStream fos = new FileOutputStream(outputIR)) {
                fos.write(script.toStringTree(script).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        decompiler = null;
        return script;
    }

    // Might want to convert this to polymorphism - move transform*
    // functions into the AstNode subclasses.  OTOH that would make
    // IR transformation part of the public AST API - desirable?
    // Another possibility:  create AstTransformer interface and adapter.
    public Node transform(AstNode node) {
        switch (node.getType()) {
            case Token.ARRAYLIT:
                return transformArrayLiteral((ArrayLiteral) node);
            case Token.BLOCK:
                return transformBlock(node);
            case Token.BREAK:
                return transformBreak((BreakStatement) node);
            case Token.CALL:
                return transformFunctionCall((FunctionCall) node);
            case Token.CLASS:
                return transformClass((ClassNode) node);
            case Token.CONTINUE:
                return transformContinue((ContinueStatement) node);
            case Token.DEFAULT:
                Assignment assignment = (Assignment) node;
                Node left = transform(assignment.getLeft());
                Node right = transform(assignment.getRight());
                return new Node(Token.DEFAULT, left, right);
            case Token.DO:
                return transformDoLoop((DoLoop) node);
            case Token.EMPTY:
            case Token.IMPORT:
                return node;
            case Token.EXPORT:
                return transformExport((ExportNode) node);
            case Token.FOR:
                if (node instanceof ForInLoop) {
                    return transformForInLoop((ForInLoop) node);
                }
                return transformForLoop((ForLoop) node);
            case Token.FUNCTION:
                return transformFunction((FunctionNode) node);
            case Token.GENEXPR:
                return transformGenExpr((GeneratorExpression) node);
            case Token.GETELEM:
                return transformElementGet((ElementGet) node);
            case Token.GETPROP:
                return transformPropertyGet((PropertyGet) node);
            case Token.HOOK:
                return transformCondExpr((ConditionalExpression) node);
            case Token.IF:
                return transformIf((IfStatement) node);

            case Token.TRUE:
            case Token.FALSE:
            case Token.THIS:
            case Token.NULL:
            case Token.DEBUGGER:
                return transformLiteral(node);

            case Token.NAME:
                return transformName((Name) node);
            case Token.NUMBER:
                return transformNumber((NumberLiteral) node);
            case Token.NEW:
                return transformNewExpr((NewExpression) node);
            case Token.OBJECTLIT:
                return transformObjectLiteral((ObjectLiteral) node);
            case Token.REGEXP:
                return transformRegExp((RegExpLiteral) node);
            case Token.RETURN:
                return transformReturn((ReturnStatement) node);
            case Token.SCRIPT:
                return transformScript((ScriptNode) node);
            case Token.STRING:
                return transformString((StringLiteral) node);
            case Token.TEMPLATE:
                return transformTemplate((TemplateLiteral) node);
            case Token.SWITCH:
                return transformSwitch((SwitchStatement) node);
            case Token.THROW:
                if (node instanceof ThrowStatement) {
                    return transformThrow((ThrowStatement) node);
                } else {
                    return transformThrowExpr((UnaryExpression) node);
                }
            case Token.TRY:
                return transformTry((TryStatement) node);
            case Token.WHILE:
                return transformWhileLoop((WhileLoop) node);
            case Token.WITH:
                return transformWith((WithStatement) node);
            case Token.YIELD:
                return transformYield((Yield) node);
            default:
                if (node instanceof ExpressionStatement) {
                    return transformExprStmt((ExpressionStatement) node);
                }
                if (node instanceof Assignment) {
                    return transformAssignment((Assignment) node);
                }
                if (node instanceof UnaryExpression) {
                    return transformUnary((UnaryExpression) node);
                }
                if (node instanceof InfixExpression) {
                    return transformInfix((InfixExpression) node);
                }
                if (node instanceof VariableDeclaration) {
                    return transformVariables((VariableDeclaration) node);
                }
                if (node instanceof ParenthesizedExpression) {
                    return transformParenExpr((ParenthesizedExpression) node);
                }
                if (node instanceof LabeledStatement) {
                    return transformLabeledStatement((LabeledStatement) node);
                }
                if (node instanceof LetNode) {
                    return transformLetNode((LetNode) node);
                }
                if (node instanceof DecoratorNode) {
                    return transformDecoratorNode((DecoratorNode) node);
                }
                if (node instanceof DecoratorDeclarationNode) {
                    return transformDecoratorDeclaration((DecoratorDeclarationNode) node);
                }
                throw new IllegalArgumentException("Can't transform: " + node);
        }
    }

    private Node transformArrayLiteral(ArrayLiteral node) {
        if (node.isDestructuring()) {
            return node;
        }
        decompiler.addToken(Token.LB);
        List<AstNode> elems = node.getElements();
        Node array = new Node(Token.ARRAYLIT);
        List<Integer> skipIndexes = null;
        boolean spreading = node.getProp(Node.SPREAD_PROP) != null;
        for (int i = 0; i < elems.size(); ++i) {
            AstNode elem = elems.get(i);
            if (elem.getType() != Token.EMPTY) {
                Node transformed = transform(elem);
                if (elem.getProp(Node.SPREAD_PROP) != null)
                    transformed.putProp(Node.SPREAD_PROP, true);
                array.addChildToBack(transformed);
            } else {
                if (spreading) {
                    array.addChildToBack(elem);
                    continue;
                }

                if (skipIndexes == null) {
                    skipIndexes = new ArrayList<>();
                }
                skipIndexes.add(i);
            }
            if (i < elems.size() - 1)
                decompiler.addToken(Token.COMMA);
        }
        if (node.getProp(Node.SPREAD_PROP) != null) {
            decompiler.addToken(Token.SPREAD);
            array.putProp(Node.SPREAD_PROP, true);
        }
        decompiler.addToken(Token.RB);
        array.putIntProp(Node.DESTRUCTURING_ARRAY_LENGTH, node.getDestructuringLength());
        if (skipIndexes != null) {
            int[] skips = new int[skipIndexes.size()];
            for (int i = 0; i < skipIndexes.size(); i++)
                skips[i] = skipIndexes.get(i);
            array.putProp(Node.SKIP_INDEXES_PROP, skips);
        }
        return array;
    }

    private Node transformAssignment(Assignment node) {
        AstNode left = removeParens(node.getLeft());
        Node target = null;
        if (isDestructuring(left)) {
            decompile(left);
            target = left;
        } else {
            target = transform(left);
        }
        decompiler.addToken(node.getType());
        return createAssignment(node.getType(),
                target,
                transform(node.getRight()));
    }

    private Node transformBlock(AstNode node) {
        if (node instanceof Scope) {
            pushScope((Scope) node);
        }

        try {
            List<Node> kids = new ArrayList<>();
            for (Node kid : node) {
                kids.add(transform((AstNode) kid));
            }
            node.removeChildren();
            for (Node kid : kids) {
                node.addChildToBack(kid);
            }
            return node;
        } finally {
            if (node instanceof Scope) {
                popScope();
            }
        }
    }

    private Node transformBreak(BreakStatement node) {
        decompiler.addToken(Token.BREAK);
        if (node.getBreakLabel() != null) {
            decompiler.addName(node.getBreakLabel().getIdentifier());
        }
        decompiler.addEOL(Token.SEMI);
        return node;
    }

    private Node transformCondExpr(ConditionalExpression node) {
        Node test = transform(node.getTestExpression());
        decompiler.addToken(Token.HOOK);
        Node ifTrue = transform(node.getTrueExpression());
        decompiler.addToken(Token.COLON);
        Node ifFalse = transform(node.getFalseExpression());
        return createCondExpr(test, ifTrue, ifFalse);
    }

    private Node transformContinue(ContinueStatement node) {
        decompiler.addToken(Token.CONTINUE);
        if (node.getLabel() != null) {
            decompiler.addName(node.getLabel().getIdentifier());
        }
        decompiler.addEOL(Token.SEMI);
        return node;
    }

    private Node transformDoLoop(DoLoop loop) {
        loop.setType(Token.LOOP);
        pushScope(loop);
        try {
            decompiler.addToken(Token.DO);
            decompiler.addEOL(Token.LC);
            Node body = transform(loop.getBody());
            decompiler.addToken(Token.RC);
            decompiler.addToken(Token.WHILE);
            decompiler.addToken(Token.LP);
            Node cond = transform(loop.getCondition());
            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.SEMI);
            return createLoop(loop, LOOP_DO_WHILE,
                    body, cond, null, null);
        } finally {
            popScope();
        }
    }

    private Node transformExport(ExportNode node) {
        if (node.getExportedValue() != null) {
            node.addChildToBack(transform(node.getExportedValue()));
        }

        return node;
    }

    private Node transformElementGet(ElementGet node) {
        // OPT: could optimize to createPropertyGet
        // iff elem is string that can not be number
        Node target = transform(node.getTarget());
        boolean chained = node.getProp(Node.CHAINING_PROP) != null;

        if (chained) {
            decompiler.addToken(Token.OPTIONAL_CHAINING);
        }
        decompiler.addToken(Token.LB);
        Node element = transform(node.getElement());
        decompiler.addToken(Token.RB);

        Node newNode = new Node(Token.GETELEM, target, element);

        if (chained) {
            newNode.putProp(Node.CHAINING_PROP, true);
        }

        return newNode;
    }

    private Node transformExprStmt(ExpressionStatement node) {
        Node expr = transform(node.getExpression());
        decompiler.addEOL(Token.SEMI);
        return new Node(node.getType(), expr, node.getLineno());
    }

    private Node transformForInLoop(ForInLoop loop) {
        decompiler.addToken(Token.FOR);
        decompiler.addToken(Token.LP);

        loop.setType(Token.LOOP);
        pushScope(loop);
        try {
            int declType = -1;
            AstNode iter = loop.getIterator();
            if (iter instanceof VariableDeclaration) {
                declType = iter.getType();
            }
            Node lhs = transform(iter);
            if (loop.isForOf()) {
                decompiler.addName("of ");
            } else {
                decompiler.addToken(Token.IN);
            }
            Node obj = transform(loop.getIteratedObject());
            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.LC);
            Node body = transform(loop.getBody());
            decompiler.addEOL(Token.RC);
            return createForIn(declType, loop, lhs, obj, body, loop.isForOf());
        } finally {
            popScope();
        }
    }

    private Node transformForLoop(ForLoop loop) {
        decompiler.addToken(Token.FOR);
        decompiler.addToken(Token.LP);
        loop.setType(Token.LOOP);
        // XXX: Can't use pushScope/popScope here since 'createFor' may split
        // the scope
        Scope savedScope = currentScope;
        currentScope = loop;
        try {
            Node init = transform(loop.getInitializer());
            decompiler.addToken(Token.SEMI);
            Node test = transform(loop.getCondition());
            decompiler.addToken(Token.SEMI);
            Node incr = transform(loop.getIncrement());
            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.LC);
            Node body = transform(loop.getBody());
            decompiler.addEOL(Token.RC);
            return createFor(loop, init, test, incr, body);
        } finally {
            currentScope = savedScope;
        }
    }

    private void transformDestructuringParams(Node root) {
        Node child = root.first;

        while (child != null) {
            if (child.type == Token.DEFAULT) {
                Node newChild = transform((AstNode) (child.last));
                child.replaceChild(child.last, newChild);
            } else if (child.getProp(Node.COMPUTED_PROP) != null) {
                Node newChild = transform((AstNode) child);
                root.replaceChild(child, newChild);
            } else {
                transformDestructuringParams(child);
            }

            child = child.next;
        }
    }

    private Node transformFunction(FunctionNode fn) {
        int functionType = fn.getFunctionType();
        int start = decompiler.markFunctionStart(functionType);
        int index = currentScriptOrFn.addFunction(fn);

        PerFunctionVariables savedVars = new PerFunctionVariables(fn);

        try {
            decompileFunctionHeader(fn);
            // If we start needing to record much more codegen metadata during
            // function parsing, we should lump it all into a helper class.
            Node destructuring = (Node) fn.getProp(Node.DESTRUCTURING_PARAMS);

            // Scan tree for untransformed AstNodes
            if (destructuring != null) {
                transformDestructuringParams(destructuring);
            }

            fn.removeProp(Node.DESTRUCTURING_PARAMS);

            int lineno = fn.getBody().getLineno();
            ++nestingOfFunction;  // only for body, not params
            Node body = transform(fn.getBody());

            if (!fn.isExpressionClosure()) {
                decompiler.addToken(Token.RC);
            }

            if (fn instanceof DecoratorDeclarationNode) {
                for (DecoratorNode dn : ((DecoratorDeclarationNode) fn).getDecoratorNodes()) {
                    transformDecoratorNode(dn);
                }
            }

            // Any @initialize decorators will be in the INITIALIZE_PROP prop
            List<AstNode> initializers = (List<AstNode>) fn.getProp(Node.INITIALIZE_PROP);

            if (initializers != null) {
                for (AstNode initializer : initializers) {
                    if (initializer instanceof ClassNode) {
                        List<Node> dns = ((ClassNode) initializer).getDecorators().stream()
                                .filter(dn -> dn.getDecoratorType() == DecoratorType.INITIALIZE)
                                .map(this::transformDecoratorNode)
                                .collect(Collectors.toList());

                        initializer.putProp(Node.DECORATOR_PROP, dns);
                    } else if (initializer instanceof ClassElement) {
                        List<Node> dns = ((ClassElement) initializer).getDecorators().stream()
                                .filter(dn -> dn.getDecoratorType() == DecoratorType.INITIALIZE)
                                .map(this::transformDecoratorNode)
                                .collect(Collectors.toList());

                        initializer.putProp(Node.DECORATOR_PROP, dns);
                    } else {
                        throw Kit.codeBug();
                    }
                }
            }

            fn.setEncodedSourceBounds(start, decompiler.markFunctionEnd(start));

            if (functionType != FunctionNode.FUNCTION_EXPRESSION && !fn.isExpressionClosure()) {
                // Add EOL only if function is not part of expression
                // since it gets SEMI + EOL from Statement in that case
                decompiler.addToken(Token.EOL);
            }

            if (destructuring != null) {
                body.addChildToFront(new Node(Token.EXPR_VOID, destructuring, lineno));
            }

            int syntheticType = fn.getFunctionType();
            Node pn = initFunction(fn, index, body, syntheticType);

            return pn;

        } finally {
            --nestingOfFunction;
            savedVars.restore();
        }
    }

    public static Object GENERATED_SUPER = new Object();

    private DecoratorDeclarationNode transformDecoratorDeclaration(DecoratorDeclarationNode node) {
        // Add an empty body so that transformFunction doesn't
        // crash with an NPE
        node.setBody(new Block());
        node.setFunctionType(FunctionNode.FUNCTION_STATEMENT);

        List<DecoratorNode> dns = node.getDecoratorNodes();

        if (dns.stream().anyMatch(it -> it.getDecoratorType() == DecoratorType.NUMERICTEMPLATE) && dns.size() != 1) {
            throw ScriptRuntime.typeError("User-defined decorator cannot contain @numericTemplate with any other decorator");
        }


        transformFunction(node);

        return node;
    }

    private Node transformDecoratorNode(DecoratorNode dn) {
        for (AstNode arg : dn.getArguments()) {
            dn.addChildToBack(transform(arg));
        }

        return dn;
    }

    private Node transformClass(ClassNode node) {
        if (node.getExtendsName() != null) {
            node.setExtended(transform(node.getExtendsName()));
        }

        FunctionNode fn = node.getConstructor();

        if (fn == null) {
            fn = new FunctionNode();
            Block body = new Block();
            if (node.getExtended() != null) {
                FunctionCall superCall = new FunctionCall();
                Name superName = new Name();
                superName.setString("super");
                superName.putProp(Node.SUPER_PROP, GENERATED_SUPER);
                superCall.setTarget(superName);
                ExpressionStatement expr = new ExpressionStatement(superCall);
                body.addStatement(expr);
            }
            fn.setBody(body);
        }

        ScriptNode temp = currentScriptOrFn;
        currentScriptOrFn = fn;

        node.setTransformedFn(fn);

        fn.setFunctionName(node.getClassName());
        fn.setFunctionType(FunctionNode.FUNCTION_EXPRESSION);
        fn.setParentClass(node);

        List<Node> decorators = new ArrayList<>();

        for (DecoratorNode dn : node.getDecorators()) {
            if (dn.getDecoratorType() == DecoratorType.INITIALIZE) continue;
            decorators.add(transformDecoratorNode(dn));
        }

        // @initialize decorators must be attached to the
        // constructor
        List<AstNode> initializers = node.getMethods().stream().filter(cm ->
                cm.getDecorators().stream().anyMatch(dn -> dn.getDecoratorType() == DecoratorType.INITIALIZE)
        ).collect(Collectors.toList());

        initializers.addAll(node.getFields().stream().filter(cf ->
                cf.getDecorators().stream().anyMatch(dn -> dn.getDecoratorType() == DecoratorType.INITIALIZE)
        ).collect(Collectors.toList()));

        if (node.getDecorators().stream().anyMatch(dn -> dn.getDecoratorType() == DecoratorType.INITIALIZE)) {
            initializers.add(node);
        }

        node.putProp(Node.DECORATOR_PROP, decorators);
        fn.putProp(Node.INITIALIZE_PROP, initializers);

        currentScriptOrFn = temp;

        Node transformedFn = transform(fn);
        node.addChildToBack(transformedFn);
        node.setParentFn(transformedFn);

        currentScriptOrFn = fn;

        for (ClassMethod cm : node.getMethods()) {
            cm.setNameKey(getPropKey(cm.getName()));
            cm.addChildToBack(transform(cm.getFunction()));

            decorators = new ArrayList<>();

            for (DecoratorNode dn : cm.getDecorators()) {
                if (dn.getDecoratorType() == DecoratorType.INITIALIZE) continue;
                decorators.add(transformDecoratorNode(dn));
            }

            cm.putProp(Node.DECORATOR_PROP, decorators);
            node.addChildToBack(cm);
        }

        for (ClassField cp : node.getFields()) {
            if (!cp.isStatic()) {
                currentScriptOrFn = temp;
            }

            cp.setNameKey(getPropKey(cp.getName()));
            cp.addChildToBack(transform(cp.getDefaultValue()));

            if (!cp.isStatic()) {
                currentScriptOrFn = fn;
            }

            decorators = new ArrayList<>();

            for (DecoratorNode dn : cp.getDecorators()) {
                if (dn.getDecoratorType() == DecoratorType.INITIALIZE) continue;
                decorators.add(transformDecoratorNode(dn));
            }

            cp.putProp(Node.DECORATOR_PROP, decorators);
            node.addChildToBack(cp);
        }

        currentScriptOrFn = temp;

        return node;
    }

    private Node transformFunctionCall(FunctionCall node) {
        Node call = createCallOrNew(Token.CALL, transform(node.getTarget()));

        if (node.getProp(Node.CHAINING_PROP) != null) {
            call.putProp(Node.CHAINING_PROP, node.getProp(Node.CHAINING_PROP));
        } else if (node.getProp(Node.SPREAD_PROP) != null) {
            call.putProp(Node.SPREAD_PROP, node.getProp(Node.SPREAD_PROP));
        }

        if (node.getProp(Node.PRIVATE_ACCESS_PROP) != null) {
            call.putProp(Node.PRIVATE_ACCESS_PROP, true);
        }

        call.setLineno(node.getLineno());
        decompiler.addToken(Token.LP);
        List<AstNode> args = node.getArguments();
        for (int i = 0; i < args.size(); i++) {
            AstNode arg = args.get(i);
            if (arg.getType() == Token.HOOK && arg instanceof EmptyExpression) {
                call.putProp(Node.PARTIAL_PROP, true);
                call.addChildToBack(arg);
                decompiler.addToken(Token.HOOK);
                continue;
            }
            Node child = transform(arg);
            if (arg.getProp(Node.SPREAD_PROP) != null) {
                child.putProp(Node.SPREAD_PROP, true);
            }
            call.addChildToBack(child);
            if (i < args.size() - 1) {
                decompiler.addToken(Token.COMMA);
            }
        }
        decompiler.addToken(Token.RP);

        return call;
    }

    private Node transformGenExpr(GeneratorExpression node) {
        Node pn;

        FunctionNode fn = new FunctionNode();
        fn.setSourceName(currentScriptOrFn.getNextTempName());
        fn.setIsGenerator();
        fn.setFunctionType(FunctionNode.FUNCTION_EXPRESSION);
        fn.setRequiresActivation();

        int functionType = fn.getFunctionType();
        int start = decompiler.markFunctionStart(functionType);
        decompileFunctionHeader(fn);
        int index = currentScriptOrFn.addFunction(fn);

        PerFunctionVariables savedVars = new PerFunctionVariables(fn);
        try {
            // If we start needing to record much more codegen metadata during
            // function parsing, we should lump it all into a helper class.
            Node destructuring = (Node) fn.getProp(Node.DESTRUCTURING_PARAMS);
            fn.removeProp(Node.DESTRUCTURING_PARAMS);

            int lineno = node.lineno;
            ++nestingOfFunction;  // only for body, not params
            Node body = genExprTransformHelper(node);

            if (!fn.isExpressionClosure()) {
                decompiler.addToken(Token.RC);
            }
            fn.setEncodedSourceBounds(start, decompiler.markFunctionEnd(start));

            if (functionType != FunctionNode.FUNCTION_EXPRESSION && !fn.isExpressionClosure()) {
                // Add EOL only if function is not part of expression
                // since it gets SEMI + EOL from Statement in that case
                decompiler.addToken(Token.EOL);
            }

            if (destructuring != null) {
                body.addChildToFront(new Node(Token.EXPR_VOID,
                        destructuring, lineno));
            }

            int syntheticType = fn.getFunctionType();
            pn = initFunction(fn, index, body, syntheticType);
        } finally {
            --nestingOfFunction;
            savedVars.restore();
        }

        Node call = createCallOrNew(Token.CALL, pn);
        call.setLineno(node.getLineno());
        decompiler.addToken(Token.LP);
        decompiler.addToken(Token.RP);
        return call;
    }

    private Node genExprTransformHelper(GeneratorExpression node) {
        decompiler.addToken(Token.LP);
        int lineno = node.getLineno();
        Node expr = transform(node.getResult());

        List<GeneratorExpressionLoop> loops = node.getLoops();
        int numLoops = loops.size();

        // Walk through loops, collecting and defining their iterator symbols.
        Node[] iterators = new Node[numLoops];
        Node[] iteratedObjs = new Node[numLoops];

        for (int i = 0; i < numLoops; i++) {
            GeneratorExpressionLoop acl = loops.get(i);
            decompiler.addName(" ");
            decompiler.addToken(Token.FOR);
            decompiler.addToken(Token.LP);

            AstNode iter = acl.getIterator();
            String name = null;
            if (iter.getType() == Token.NAME) {
                name = iter.getString();
                decompiler.addName(name);
            } else {
                // destructuring assignment
                decompile(iter);
                name = currentScriptOrFn.getNextTempName();
                defineSymbol(Token.LP, name, false);
                expr = createBinary(
                        Token.COMMA,
                        createAssignment(Token.ASSIGN, iter, createName(name)), expr
                );
            }
            Node init = createName(name);
            // Define as a let since we want the scope of the variable to
            // be restricted to the array comprehension
            defineSymbol(Token.LET, name, false);
            iterators[i] = init;

            if (acl.isForOf()) {
                decompiler.addName("of ");
            } else {
                decompiler.addToken(Token.IN);
            }
            iteratedObjs[i] = transform(acl.getIteratedObject());
            decompiler.addToken(Token.RP);
        }

        // generate code for tmpArray.push(body)
        Node yield = new Node(Token.YIELD, expr, node.getLineno());

        Node body = new Node(Token.EXPR_VOID, yield, lineno);

        if (node.getFilter() != null) {
            decompiler.addName(" ");
            decompiler.addToken(Token.IF);
            decompiler.addToken(Token.LP);
            body = createIf(transform(node.getFilter()), body, null, lineno);
            decompiler.addToken(Token.RP);
        }

        // Now walk loops in reverse to build up the body statement.
        int pushed = 0;
        try {
            for (int i = numLoops - 1; i >= 0; i--) {
                GeneratorExpressionLoop acl = loops.get(i);
                Scope loop = createLoopNode(null,  // no label
                        acl.getLineno());
                pushScope(loop);
                pushed++;
                body = createForIn(Token.LET, loop, iterators[i], iteratedObjs[i], body, acl.isForOf());
            }
        } finally {
            for (int i = 0; i < pushed; i++) {
                popScope();
            }
        }

        decompiler.addToken(Token.RP);

        return body;
    }

    private Node transformIf(IfStatement n) {
        decompiler.addToken(Token.IF);
        decompiler.addToken(Token.LP);
        Node cond = transform(n.getCondition());
        decompiler.addToken(Token.RP);
        decompiler.addEOL(Token.LC);
        Node ifTrue = transform(n.getThenPart());
        Node ifFalse = null;
        if (n.getElsePart() != null) {
            decompiler.addToken(Token.RC);
            decompiler.addToken(Token.ELSE);
            decompiler.addEOL(Token.LC);
            ifFalse = transform(n.getElsePart());
        }
        decompiler.addEOL(Token.RC);
        return createIf(cond, ifTrue, ifFalse, n.getLineno());
    }

    private Node transformInfix(InfixExpression node) {
        Node left = transform(node.getLeft());
        decompiler.addToken(node.getType());
        Node right = transform(node.getRight());
        return createBinary(node.getType(), left, right);
    }

    private Node transformLabeledStatement(LabeledStatement ls) {
        Label label = ls.getFirstLabel();
        List<Label> labels = ls.getLabels();
        decompiler.addName(label.getName());
        if (labels.size() > 1) {
            // more than one label
            for (Label lb : labels.subList(1, labels.size())) {
                decompiler.addEOL(Token.COLON);
                decompiler.addName(lb.getName());
            }
        }
        if (ls.getStatement().getType() == Token.BLOCK) {
            // reuse OBJECTLIT for ':' workaround, cf. transformObjectLiteral()
            decompiler.addToken(Token.OBJECTLIT);
            decompiler.addEOL(Token.LC);
        } else {
            decompiler.addEOL(Token.COLON);
        }
        Node statement = transform(ls.getStatement());
        if (ls.getStatement().getType() == Token.BLOCK) {
            decompiler.addEOL(Token.RC);
        }

        // Make a target and put it _after_ the statement node.  Add in the
        // LABEL node, so breaks get the right target.
        Node breakTarget = Node.newTarget();
        Node block = new Node(Token.BLOCK, label, statement, breakTarget);
        label.target = breakTarget;

        return block;
    }

    private Node transformLetNode(LetNode node) {
        pushScope(node);
        try {
            decompiler.addToken(Token.LET);
            decompiler.addToken(Token.LP);
            Node vars = transformVariableInitializers(node.getVariables());
            decompiler.addToken(Token.RP);
            node.addChildToBack(vars);
            boolean letExpr = node.getType() == Token.LETEXPR;
            if (node.getBody() != null) {
                if (letExpr) {
                    decompiler.addName(" ");
                } else {
                    decompiler.addEOL(Token.LC);
                }
                node.addChildToBack(transform(node.getBody()));
                if (!letExpr) {
                    decompiler.addEOL(Token.RC);
                }
            }
            return node;
        } finally {
            popScope();
        }
    }

    private Node transformLiteral(AstNode node) {
        decompiler.addToken(node.getType());
        return node;
    }

    private Node transformName(Name node) {
        if (node.getParent() != null && node.getParent().getProp(Node.DECORATOR_PROP) != null) {
            node.putProp(Node.DECORATOR_PROP, true);
            decompiler.addToken(Token.AT);
        }
        decompiler.addName(node.getIdentifier());
        return node;
    }

    private Node transformNewExpr(NewExpression node) {
        decompiler.addToken(Token.NEW);
        Node nx = createCallOrNew(Token.NEW, transform(node.getTarget()));
        nx.setLineno(node.getLineno());
        List<AstNode> args = node.getArguments();
        decompiler.addToken(Token.LP);
        for (int i = 0; i < args.size(); i++) {
            AstNode arg = args.get(i);
            nx.addChildToBack(transform(arg));
            if (i < args.size() - 1) {
                decompiler.addToken(Token.COMMA);
            }
        }
        decompiler.addToken(Token.RP);
        if (node.getInitializer() != null) {
            nx.addChildToBack(transformObjectLiteral(node.getInitializer()));
        }
        return nx;
    }

    private Node transformNumber(NumberLiteral node) {
        decompiler.addNumber(node.getNumber());
        DecoratorNode dn = node.getDecoratorNode();
        if (dn != null) {
            transformDecoratorNode(dn);
        }
        return node;
    }

    private Node transformObjectLiteral(ObjectLiteral node) {
        if (node.isDestructuring()) {
            return node;
        }
        // createObjectLiteral rewrites its argument as object
        // creation plus object property entries, so later compiler
        // stages don't need to know about object literals.
        decompiler.addToken(Token.LC);
        List<ObjectProperty> elems = node.getElements();
        Node object = new Node(Token.OBJECTLIT);
        Object[] properties;
        int[] spreadIndices;
        if (elems.isEmpty()) {
            properties = ScriptRuntime.emptyArgs;
            spreadIndices = new int[0];
        } else {
            int size = elems.size(), nonSpreadIndex = 0, spreadIndex = 0;
            properties = new Object[(int) elems.stream().filter(it -> it.getSpread() == null).count()];
            spreadIndices = new int[elems.size() - properties.length];
            for (ObjectProperty prop : elems) {
                if (prop.isGetterMethod()) {
                    decompiler.addToken(Token.GET);
                } else if (prop.isSetterMethod()) {
                    decompiler.addToken(Token.SET);
                } else if (prop.isNormalMethod()) {
                    decompiler.addToken(Token.METHOD);
                }

                AstNode spread = prop.getSpread();

                if (spread == null) {
                    properties[nonSpreadIndex++] = getPropKey(prop.getLeft());

                    // OBJECTLIT is used as ':' in object literal for
                    // decompilation to solve spacing ambiguity.
                    if (!(prop.isMethod())) {
                        decompiler.addToken(Token.OBJECTLIT);
                    }

                    Node right = transform(prop.getRight());
                    if (prop.isGetterMethod()) {
                        right = createUnary(Token.GET, right);
                    } else if (prop.isSetterMethod()) {
                        right = createUnary(Token.SET, right);
                    } else if (prop.isNormalMethod()) {
                        right = createUnary(Token.METHOD, right);
                    }
                    object.addChildToBack(right);

                    if (nonSpreadIndex < size) {
                        decompiler.addToken(Token.COMMA);
                    }
                } else {
                    spreadIndices[spreadIndex] = nonSpreadIndex + spreadIndex;
                    spreadIndex++;
                    Node transformed = transform(spread);
                    object.addChildToBack(transformed);
                }
            }
        }

        decompiler.addToken(Token.RC);
        object.putProp(Node.OBJECT_IDS_PROP, properties);
        object.putProp(Node.SPREAD_IDS_PROP, spreadIndices);
        return object;
    }

    private Object getPropKey(Node id) {
        Object key;

        if (id.getProp(Node.COMPUTED_PROP) != null) {
            key = transform((AstNode) id);
            decompiler.addToken(Token.LB);
            decompile((AstNode) id);
            decompiler.addToken(Token.RB);
        } else if (id instanceof Name) {
            if (id.getProp(Node.SPREAD_PROP) != null) {
                decompiler.addToken(Token.SPREAD);
                key = id;
            } else {
                String s = ((Name) id).getIdentifier();
                decompiler.addName(s);
                key = ScriptRuntime.getIndexObject(s);
            }
        } else if (id instanceof StringLiteral) {
            String s = ((StringLiteral) id).getValue();
            decompiler.addString(s);
            key = ScriptRuntime.getIndexObject(s);
        } else if (id instanceof NumberLiteral) {
            double n = ((NumberLiteral) id).getNumber();
            decompiler.addNumber(n);
            key = ScriptRuntime.getIndexObject(n);
        } else {
            throw Kit.codeBug();
        }
        return key;
    }

    private Node transformParenExpr(ParenthesizedExpression node) {
        AstNode expr = node.getExpression();
        decompiler.addToken(Token.LP);
        int count = 1;
        while (expr instanceof ParenthesizedExpression) {
            decompiler.addToken(Token.LP);
            count++;
            expr = ((ParenthesizedExpression) expr).getExpression();
        }
        Node result = transform(expr);
        for (int i = 0; i < count; i++) {
            decompiler.addToken(Token.RP);
        }
        result.putProp(Node.PARENTHESIZED_PROP, Boolean.TRUE);
        return result;
    }

    private Node transformPropertyGet(PropertyGet node) {
        Node target = transform(node.getTarget());
        String name = node.getProperty().getIdentifier();
        if (node.getProp(Node.CHAINING_PROP) != null) {
            decompiler.addToken(Token.OPTIONAL_CHAINING);
        } else {
            decompiler.addToken(Token.DOT);
        }

        if (node.getProp(Node.PRIVATE_ACCESS_PROP) != null) {
            decompiler.addToken(Token.HASHTAG);
        }

        decompiler.addName(name);
        return createPropertyGet(target, name, node);
    }

    private Node transformRegExp(RegExpLiteral node) {
        decompiler.addRegexp(node.getValue(), node.getFlags());
        currentScriptOrFn.addRegExp(node);
        return node;
    }

    private Node transformReturn(ReturnStatement node) {
        boolean expClosure = Boolean.TRUE.equals(node.getProp(Node.EXPRESSION_CLOSURE_PROP));
        boolean isArrow = Boolean.TRUE.equals(node.getProp(Node.ARROW_FUNCTION_PROP));
        if (expClosure) {
            if (!isArrow) {
                decompiler.addName(" ");
            }
        } else {
            decompiler.addToken(Token.RETURN);
        }
        AstNode rv = node.getReturnValue();
        Node value = rv == null ? null : transform(rv);
        if (!expClosure) decompiler.addEOL(Token.SEMI);
        return rv == null
                ? new Node(Token.RETURN, node.getLineno())
                : new Node(Token.RETURN, value, node.getLineno());
    }

    private Node transformScript(ScriptNode node) {
        decompiler.addToken(Token.SCRIPT);
        if (currentScope != null) Kit.codeBug();
        currentScope = node;
        Node body = new Node(Token.BLOCK);
        for (Node kid : node) {
            body.addChildToBack(transform((AstNode) kid));
        }
        node.removeChildren();
        Node children = body.getFirstChild();
        if (children != null) {
            node.addChildrenToBack(children);
        }
        return node;
    }

    private Node transformString(StringLiteral node) {
        boolean spread = node.getProp(Node.SPREAD_PROP) != null;

        if (spread) {
            decompiler.addToken(Token.SPREAD);
        }

        decompiler.addString(node.getValue());

        if (spread) {
            Node array = new Node(Token.ARRAYLIT);
            array.putProp(Node.SPREAD_PROP, true);

            String str = node.getValue();
            int index = 0;
            while (index < str.length()) {
                int newIndex = str.offsetByCodePoints(index, 1);
                String value = str.substring(index, newIndex);
                index = newIndex;

                StringLiteral sl = new StringLiteral();
                sl.setValue(value);
                array.addChildToBack(transformString(sl));
            }
            return array;
        } else {
            return Node.newString(node.getValue());
        }
    }

    private Node transformTemplate(TemplateLiteral lit) {
        AstNode target = lit.getTarget();

        if (target != null) {
            lit.setTransformedTarget(transform(target));
        }

        for (AstNode part : lit.getElements()) {
            lit.addChildToBack(transform(part));
        }

        return lit;
    }

    private Node transformSwitch(SwitchStatement node) {
        // The switch will be rewritten from:
        //
        // switch (expr) {
        //   case test1: statements1;
        //   ...
        //   default: statementsDefault;
        //   ...
        //   case testN: statementsN;
        // }
        //
        // to:
        //
        // {
        //     switch (expr) {
        //       case test1: goto label1;
        //       ...
        //       case testN: goto labelN;
        //     }
        //     goto labelDefault;
        //   label1:
        //     statements1;
        //   ...
        //   labelDefault:
        //     statementsDefault;
        //   ...
        //   labelN:
        //     statementsN;
        //   breakLabel:
        // }
        //
        // where inside switch each "break;" without label will be replaced
        // by "goto breakLabel".
        //
        // If the original switch does not have the default label, then
        // after the switch he transformed code would contain this goto:
        //     goto breakLabel;
        // instead of:
        //     goto labelDefault;

        decompiler.addToken(Token.SWITCH);
        decompiler.addToken(Token.LP);
        Node switchExpr = transform(node.getExpression());
        decompiler.addToken(Token.RP);
        node.addChildToBack(switchExpr);

        Node block = new Node(Token.BLOCK, node, node.getLineno());
        decompiler.addEOL(Token.LC);

        for (SwitchCase sc : node.getCases()) {
            AstNode expr = sc.getExpression();
            Node caseExpr = null;

            if (expr != null) {
                decompiler.addToken(Token.CASE);
                caseExpr = transform(expr);
            } else {
                decompiler.addToken(Token.DEFAULT);
            }
            decompiler.addEOL(Token.COLON);

            List<AstNode> stmts = sc.getStatements();
            Node body = new Block();
            if (stmts != null) {
                for (AstNode kid : stmts) {
                    body.addChildToBack(transform(kid));
                }
            }
            addSwitchCase(block, caseExpr, body);
        }
        decompiler.addEOL(Token.RC);
        closeSwitch(block);
        return block;
    }

    private Node transformThrowExpr(UnaryExpression node) {
        decompiler.addToken(Token.THROW);
        Node value = transform(node.getOperand());
        return new Node(Token.THROW, value, node.getLineno());
    }

    private Node transformThrow(ThrowStatement node) {
        decompiler.addToken(Token.THROW);
        Node value = transform(node.getExpression());
        decompiler.addEOL(Token.SEMI);
        return new Node(Token.THROW, value, node.getLineno());
    }

    private Node transformTry(TryStatement node) {
        decompiler.addToken(Token.TRY);
        decompiler.addEOL(Token.LC);
        Node tryBlock = transform(node.getTryBlock());
        decompiler.addEOL(Token.RC);

        Node catchBlocks = new Block();
        for (CatchClause cc : node.getCatchClauses()) {
            decompiler.addToken(Token.CATCH);
            decompiler.addToken(Token.LP);

            String varName = cc.getVarName().getIdentifier();
            decompiler.addName(varName);

            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.LC);

            Node body = transform(cc.getBody());
            decompiler.addEOL(Token.RC);

            catchBlocks.addChildToBack(createCatch(varName, body, cc.getLineno()));
        }
        Node finallyBlock = null;
        if (node.getFinallyBlock() != null) {
            decompiler.addToken(Token.FINALLY);
            decompiler.addEOL(Token.LC);
            finallyBlock = transform(node.getFinallyBlock());
            decompiler.addEOL(Token.RC);
        }
        return createTryCatchFinally(tryBlock, catchBlocks,
                finallyBlock, node.getLineno());
    }

    private Node transformUnary(UnaryExpression node) {
        int type = node.getType();
        if (node.isPrefix()) {
            decompiler.addToken(type);
        }
        Node child = transform(node.getOperand());
        if (node.isPostfix()) {
            decompiler.addToken(type);
        }
        if (type == Token.INC || type == Token.DEC) {
            return createIncDec(type, node.isPostfix(), child);
        }
        return createUnary(type, child);
    }

    private Node transformVariables(VariableDeclaration node) {
        decompiler.addToken(node.getType());
        transformVariableInitializers(node);

        // Might be most robust to have parser record whether it was
        // a variable declaration statement, possibly as a node property.
        AstNode parent = node.getParent();
        if (!(parent instanceof Loop)
                && !(parent instanceof LetNode)) {
            decompiler.addEOL(Token.SEMI);
        }
        return node;
    }

    private Node transformVariableInitializers(VariableDeclaration node) {
        List<VariableInitializer> vars = node.getVariables();
        int size = vars.size(), i = 0;
        for (VariableInitializer var : vars) {
            AstNode target = var.getTarget();
            AstNode init = var.getInitializer();

            Node left = null;
            if (var.isDestructuring()) {
                decompile(target);  // decompile but don't transform
//                left = transform(target);
                left = target;
            } else {
                left = transform(target);
            }

            Node right = null;
            if (init != null) {
                decompiler.addToken(Token.ASSIGN);
                right = transform(init);
            }

            if (var.isDestructuring()) {
                if (right == null) {  // TODO:  should this ever happen?
                    node.addChildToBack(left);
                } else {
                    Node d = createDestructuringAssignment(node.getType(), left, right);

                    node.addChildToBack(d);
                }
            } else {
                if (right != null) {
                    left.addChildToBack(right);
                }
                node.addChildToBack(left);
            }
            if (i++ < size - 1) {
                decompiler.addToken(Token.COMMA);
            }
        }
        return node;
    }

    private Node transformWhileLoop(WhileLoop loop) {
        decompiler.addToken(Token.WHILE);
        loop.setType(Token.LOOP);
        pushScope(loop);
        try {
            decompiler.addToken(Token.LP);
            Node cond = transform(loop.getCondition());
            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.LC);
            Node body = transform(loop.getBody());
            decompiler.addEOL(Token.RC);
            return createLoop(loop, LOOP_WHILE, body, cond, null, null);
        } finally {
            popScope();
        }
    }

    private Node transformWith(WithStatement node) {
        decompiler.addToken(Token.WITH);
        decompiler.addToken(Token.LP);
        Node expr = transform(node.getExpression());
        decompiler.addToken(Token.RP);
        decompiler.addEOL(Token.LC);
        Node stmt = transform(node.getStatement());
        decompiler.addEOL(Token.RC);
        return createWith(expr, stmt, node.getLineno());
    }

    private Node transformYield(Yield node) {
        decompiler.addToken(Token.YIELD);
        Node kid = node.getValue() == null ? null : transform(node.getValue());
        if (kid != null)
            return new Node(Token.YIELD, kid, node.getLineno());
        return new Node(Token.YIELD, node.getLineno());
    }

    /**
     * If caseExpression argument is null it indicates a default label.
     */
    private void addSwitchCase(Node switchBlock, Node caseExpression,
                               Node statements) {
        if (switchBlock.getType() != Token.BLOCK) throw Kit.codeBug();
        Jump switchNode = (Jump) switchBlock.getFirstChild();
        if (switchNode.getType() != Token.SWITCH) throw Kit.codeBug();

        Node gotoTarget = Node.newTarget();
        if (caseExpression != null) {
            Jump caseNode = new Jump(Token.CASE, caseExpression);
            caseNode.target = gotoTarget;
            switchNode.addChildToBack(caseNode);
        } else {
            switchNode.setDefault(gotoTarget);
        }
        switchBlock.addChildToBack(gotoTarget);
        switchBlock.addChildToBack(statements);
    }

    private void closeSwitch(Node switchBlock) {
        if (switchBlock.getType() != Token.BLOCK) throw Kit.codeBug();
        Jump switchNode = (Jump) switchBlock.getFirstChild();
        if (switchNode.getType() != Token.SWITCH) throw Kit.codeBug();

        Node switchBreakTarget = Node.newTarget();
        // switchNode.target is only used by NodeTransformer
        // to detect switch end
        switchNode.target = switchBreakTarget;

        Node defaultTarget = switchNode.getDefault();
        if (defaultTarget == null) {
            defaultTarget = switchBreakTarget;
        }

        switchBlock.addChildAfter(makeJump(Token.GOTO, defaultTarget),
                switchNode);
        switchBlock.addChildToBack(switchBreakTarget);
    }

    private Node createExprStatementNoReturn(Node expr, int lineno) {
        return new Node(Token.EXPR_VOID, expr, lineno);
    }

    private Node createString(String string) {
        return Node.newString(string);
    }

    /**
     * Catch clause of try/catch/finally
     *
     * @param varName   the name of the variable to bind to the exception
     * @param stmts     the statements in the catch clause
     * @param lineno    the starting line number of the catch clause
     */
    private Node createCatch(String varName, Node stmts, int lineno) {
        return new Node(Token.CATCH, createName(varName), stmts, lineno);
    }

    private Node initFunction(FunctionNode fnNode, int functionIndex, Node statements, int functionType) {
        fnNode.setFunctionType(functionType);
        fnNode.addChildToBack(statements);

        int functionCount = fnNode.getFunctionCount();
        if (functionCount != 0) {
            // Functions containing other functions require activation objects
            fnNode.setRequiresActivation();
        }

        if (functionType == FunctionNode.FUNCTION_EXPRESSION) {
            Name name = fnNode.getFunctionName();
            if (name != null && name.length() != 0
                    && fnNode.getSymbol(name.getIdentifier()) == null) {
                // A function expression needs to have its name as a
                // variable (if it isn't already allocated as a variable).
                // See ECMA Ch. 13.  We add code to the beginning of the
                // function to initialize a local variable of the
                // function's name to the function value, but only if the
                // function doesn't already define a formal parameter, var,
                // or nested function with the same name.
                fnNode.putSymbol(new Symbol(Token.FUNCTION, name.getIdentifier()));
                Node setFn = new Node(Token.EXPR_VOID,
                        new Node(Token.SETNAME,
                                Node.newString(Token.BINDNAME,
                                        name.getIdentifier()),
                                new Node(Token.THISFN)));
                statements.addChildrenToFront(setFn);
            }
        }

        // Add return to end if needed.
        Node lastStmt = statements.getLastChild();
        if (lastStmt == null || lastStmt.getType() != Token.RETURN) {
            statements.addChildToBack(new Node(Token.RETURN));
        }

        Node result = Node.newString(Token.FUNCTION, fnNode.getName());
        result.putIntProp(Node.FUNCTION_PROP, functionIndex);
        return result;
    }

    /**
     * Create loop node. The code generator will later call
     * createWhile|createDoWhile|createFor|createForIn
     * to finish loop generation.
     */
    private Scope createLoopNode(Node loopLabel, int lineno) {
        Scope result = createScopeNode(Token.LOOP, lineno);
        if (loopLabel != null) {
            ((Jump) loopLabel).setLoop(result);
        }
        return result;
    }

    private Node createFor(Scope loop, Node init,
                           Node test, Node incr, Node body) {
        if (init.getType() == Token.LET) {
            // rewrite "for (let i=s; i < N; i++)..." as
            // "let (i=s) { for (; i < N; i++)..." so that "s" is evaluated
            // outside the scope of the for.
            Scope let = Scope.splitScope(loop);
            let.setType(Token.LET);
            let.addChildrenToBack(init);
            let.addChildToBack(createLoop(loop, LOOP_FOR, body, test,
                    new Node(Token.EMPTY), incr));
            return let;
        }
        return createLoop(loop, LOOP_FOR, body, test, init, incr);
    }

    private Node createLoop(Jump loop, int loopType, Node body,
                            Node cond, Node init, Node incr) {
        Node bodyTarget = Node.newTarget();
        Node condTarget = Node.newTarget();
        if (loopType == LOOP_FOR && cond.getType() == Token.EMPTY) {
            cond = new Node(Token.TRUE);
        }
        Jump IFEQ = new Jump(Token.IFEQ, cond);
        IFEQ.target = bodyTarget;
        Node breakTarget = Node.newTarget();

        loop.addChildToBack(bodyTarget);
        loop.addChildrenToBack(body);
        if (loopType == LOOP_WHILE || loopType == LOOP_FOR) {
            // propagate lineno to condition
            loop.addChildrenToBack(new Node(Token.EMPTY, loop.getLineno()));
        }
        loop.addChildToBack(condTarget);
        loop.addChildToBack(IFEQ);
        loop.addChildToBack(breakTarget);

        loop.target = breakTarget;
        Node continueTarget = condTarget;

        if (loopType == LOOP_WHILE || loopType == LOOP_FOR) {
            // Just add a GOTO to the condition in the do..while
            loop.addChildToFront(makeJump(Token.GOTO, condTarget));

            if (loopType == LOOP_FOR) {
                int initType = init.getType();
                if (initType != Token.EMPTY) {
                    if (initType != Token.VAR && initType != Token.LET && initType != Token.CONST) {
                        init = new Node(Token.EXPR_VOID, init);
                    }
                    loop.addChildToFront(init);
                }
                Node incrTarget = Node.newTarget();
                loop.addChildAfter(incrTarget, body);
                if (incr.getType() != Token.EMPTY) {
                    incr = new Node(Token.EXPR_VOID, incr);
                    loop.addChildAfter(incr, incrTarget);
                }
                continueTarget = incrTarget;
            }
        }

        loop.setContinue(continueTarget);
        return loop;
    }

    /**
     * Generate IR for a for..in loop.
     */
    private Node createForIn(int declType, Node loop, Node lhs, Node obj, Node body, boolean isForOf) {
        int destructuring = -1;
        int destructuringLen = 0;
        Node lvalue;
        int type = lhs.getType();
        if (type == Token.VAR || type == Token.LET || type == Token.CONST) {
            Node kid = lhs.getLastChild();
            int kidType = kid.getType();
            if (kidType == Token.ARRAYLIT || kidType == Token.OBJECTLIT) {
                type = destructuring = kidType;
                lvalue = kid;
                destructuringLen = 0;
                if (kid instanceof ArrayLiteral)
                    destructuringLen = ((ArrayLiteral) kid).getDestructuringLength();
            } else if (kidType == Token.NAME) {
                lvalue = Node.newString(Token.NAME, kid.getString());
            } else {
                reportError("msg.bad.for.in.lhs");
                return null;
            }
        } else if (type == Token.ARRAYLIT || type == Token.OBJECTLIT) {
            destructuring = type;
            lvalue = lhs;
            destructuringLen = 0;
            if (lhs instanceof ArrayLiteral)
                destructuringLen = ((ArrayLiteral) lhs).getDestructuringLength();
        } else {
            lvalue = makeReference(lhs);
            if (lvalue == null) {
                reportError("msg.bad.for.in.lhs");
                return null;
            }
        }

        Node localBlock = new Node(Token.LOCAL_BLOCK);
        int initType = isForOf ? Token.ENUM_INIT_VALUES_IN_ORDER
                : (destructuring != -1
                ? Token.ENUM_INIT_ARRAY
                : Token.ENUM_INIT_KEYS);
        Node init = new Node(initType, obj);
        init.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
        Node cond = new Node(Token.ENUM_NEXT);
        cond.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
        Node id = new Node(Token.ENUM_ID);
        id.putProp(Node.LOCAL_BLOCK_PROP, localBlock);

        Node newBody = new Node(Token.BLOCK);
        Node assign;
        if (destructuring != -1) {
            assign = createDestructuringAssignment(declType, lvalue, id);
            if (!isForOf && (destructuring == Token.OBJECTLIT || destructuringLen != 2)) {
                // destructuring assignment is only allowed in for..each or
                // with an array type of length 2 (to hold key and value)
                reportError("msg.bad.for.in.destruct");
            }
        } else {
            assign = simpleAssignment(lvalue, id);
        }
        newBody.addChildToBack(new Node(Token.EXPR_VOID, assign));
        newBody.addChildToBack(body);

        loop = createLoop((Jump) loop, LOOP_WHILE, newBody, cond, null, null);
        loop.addChildToFront(init);
        if (type == Token.VAR || type == Token.LET || type == Token.CONST)
            loop.addChildToFront(lhs);
        localBlock.addChildToBack(loop);

        return localBlock;
    }

    /**
     * Try/Catch/Finally
     * <p>
     * The IRFactory tries to express as much as possible in the tree;
     * the responsibilities remaining for Codegen are to add the Java
     * handlers: (Either (but not both) of TARGET and FINALLY might not
     * be defined)
     * <p>
     * - a catch handler for javascript exceptions that unwraps the
     * exception onto the stack and GOTOes to the catch target
     * <p>
     * - a finally handler
     * <p>
     * ... and a goto to GOTO around these handlers.
     */
    private Node createTryCatchFinally(Node tryBlock, Node catchBlocks,
                                       Node finallyBlock, int lineno) {
        boolean hasFinally = (finallyBlock != null)
                && (finallyBlock.getType() != Token.BLOCK
                || finallyBlock.hasChildren());

        // short circuit
        if (tryBlock.getType() == Token.BLOCK && !tryBlock.hasChildren()
                && !hasFinally) {
            return tryBlock;
        }

        boolean hasCatch = catchBlocks.hasChildren();

        // short circuit
        if (!hasFinally && !hasCatch) {
            // bc finally might be an empty block...
            return tryBlock;
        }

        Node handlerBlock = new Node(Token.LOCAL_BLOCK);
        Jump pn = new Jump(Token.TRY, tryBlock, lineno);
        pn.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);

        if (hasCatch) {
            // jump around catch code
            Node endCatch = Node.newTarget();
            pn.addChildToBack(makeJump(Token.GOTO, endCatch));

            // make a TARGET for the catch that the tcf node knows about
            Node catchTarget = Node.newTarget();
            pn.target = catchTarget;
            // mark it
            pn.addChildToBack(catchTarget);

            //
            //  Given
            //
            //   try {
            //       tryBlock;
            //   } catch (e if condition1) {
            //       something1;
            //   ...
            //
            //   } catch (e if conditionN) {
            //       somethingN;
            //   } catch (e) {
            //       somethingDefault;
            //   }
            //
            //  rewrite as
            //
            //   try {
            //       tryBlock;
            //       goto after_catch:
            //   } catch (x) {
            //       with (newCatchScope(e, x)) {
            //           if (condition1) {
            //               something1;
            //               goto after_catch;
            //           }
            //       }
            //   ...
            //       with (newCatchScope(e, x)) {
            //           if (conditionN) {
            //               somethingN;
            //               goto after_catch;
            //           }
            //       }
            //       with (newCatchScope(e, x)) {
            //           somethingDefault;
            //           goto after_catch;
            //       }
            //   }
            // after_catch:
            //
            // If there is no default catch, then the last with block
            // arround  "somethingDefault;" is replaced by "rethrow;"

            // It is assumed that catch handler generation will store
            // exeception object in handlerBlock register

            // Block with local for exception scope objects
            Node catchScopeBlock = new Node(Token.LOCAL_BLOCK);

            // expects catchblocks children to be (cond block) pairs.
            Node cb = catchBlocks.getFirstChild();
            boolean hasDefault = false;
            int scopeIndex = 0;
            while (cb != null) {
                int catchLineNo = cb.getLineno();

                Node name = cb.getFirstChild();
                Node catchStatement = name.getNext();
                cb.removeChild(name);
                cb.removeChild(catchStatement);

                // Add goto to the catch statement to jump out of catch
                // but prefix it with LEAVEWITH since try..catch produces
                // "with"code in order to limit the scope of the exception
                // object.
                catchStatement.addChildToBack(new Node(Token.LEAVEWITH));
                catchStatement.addChildToBack(makeJump(Token.GOTO, endCatch));

                // Create condition "if" when present
                hasDefault = true;

                // Generate code to create the scope object and store
                // it in catchScopeBlock register
                Node catchScope = new Node(Token.CATCH_SCOPE, name,
                        createUseLocal(handlerBlock));
                catchScope.putProp(Node.LOCAL_BLOCK_PROP, catchScopeBlock);
                catchScope.putIntProp(Node.CATCH_SCOPE_PROP, scopeIndex);
                catchScopeBlock.addChildToBack(catchScope);

                // Add with statement based on catch scope object
                catchScopeBlock.addChildToBack(createWith(createUseLocal(catchScopeBlock), catchStatement, catchLineNo));

                // move to next cb
                cb = cb.getNext();
                ++scopeIndex;
            }
            pn.addChildToBack(catchScopeBlock);
            if (!hasDefault) {
                // Generate code to rethrow if no catch clause was executed
                Node rethrow = new Node(Token.RETHROW);
                rethrow.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);
                pn.addChildToBack(rethrow);
            }

            pn.addChildToBack(endCatch);
        }

        if (hasFinally) {
            Node finallyTarget = Node.newTarget();
            pn.setFinally(finallyTarget);

            // add jsr finally to the try block
            pn.addChildToBack(makeJump(Token.JSR, finallyTarget));

            // jump around finally code
            Node finallyEnd = Node.newTarget();
            pn.addChildToBack(makeJump(Token.GOTO, finallyEnd));

            pn.addChildToBack(finallyTarget);
            Node fBlock = new Node(Token.FINALLY, finallyBlock);
            fBlock.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);
            pn.addChildToBack(fBlock);

            pn.addChildToBack(finallyEnd);
        }
        handlerBlock.addChildToBack(pn);
        return handlerBlock;
    }

    private Node createWith(Node obj, Node body, int lineno) {
        setRequiresActivation();
        Node result = new Node(Token.BLOCK, lineno);
        result.addChildToBack(new Node(Token.ENTERWITH, obj));
        Node bodyNode = new Node(Token.WITH, body, lineno);
        result.addChildrenToBack(bodyNode);
        result.addChildToBack(new Node(Token.LEAVEWITH));
        return result;
    }

    private Node createIf(Node cond, Node ifTrue, Node ifFalse, int lineno) {
        int condStatus = isAlwaysDefinedBoolean(cond);
        if (condStatus == ALWAYS_TRUE_BOOLEAN) {
            return ifTrue;
        } else if (condStatus == ALWAYS_FALSE_BOOLEAN) {
            if (ifFalse != null) {
                return ifFalse;
            }
            // Replace if (false) xxx by empty block
            return new Node(Token.BLOCK, lineno);
        }

        Node result = new Node(Token.BLOCK, lineno);
        Node ifNotTarget = Node.newTarget();
        Jump IFNE = new Jump(Token.IFNE, cond);
        IFNE.target = ifNotTarget;

        result.addChildToBack(IFNE);
        result.addChildrenToBack(ifTrue);

        if (ifFalse != null) {
            Node endTarget = Node.newTarget();
            result.addChildToBack(makeJump(Token.GOTO, endTarget));
            result.addChildToBack(ifNotTarget);
            result.addChildrenToBack(ifFalse);
            result.addChildToBack(endTarget);
        } else {
            result.addChildToBack(ifNotTarget);
        }

        return result;
    }

    private Node createCondExpr(Node cond, Node ifTrue, Node ifFalse) {
        int condStatus = isAlwaysDefinedBoolean(cond);
        if (condStatus == ALWAYS_TRUE_BOOLEAN) {
            return ifTrue;
        } else if (condStatus == ALWAYS_FALSE_BOOLEAN) {
            return ifFalse;
        }
        return new Node(Token.HOOK, cond, ifTrue, ifFalse);
    }

    private Node createUnary(int nodeType, Node child) {
        int childType = child.getType();
        switch (nodeType) {
            case Token.DELPROP: {
                Node n;
                if (childType == Token.NAME) {
                    // Transform Delete(Name "a")
                    //  to Delete(Bind("a"), String("a"))
                    child.setType(Token.BINDNAME);
                    Node left = child;
                    Node right = Node.newString(child.getString());
                    n = new Node(nodeType, left, right);
                } else if (childType == Token.GETPROP ||
                        childType == Token.GETELEM) {
                    Node left = child.getFirstChild();
                    Node right = child.getLastChild();
                    child.removeChild(left);
                    child.removeChild(right);
                    n = new Node(nodeType, left, right);
                } else if (childType == Token.GET_REF) {
                    Node ref = child.getFirstChild();
                    child.removeChild(ref);
                    n = new Node(Token.DEL_REF, ref);
                } else {
                    // Always evaluate delete operand, see ES5 11.4.1 & bug #726121
                    n = new Node(nodeType, new Node(Token.TRUE), child);
                }
                return n;
            }
            case Token.TYPEOF:
                if (childType == Token.NAME) {
                    child.setType(Token.TYPEOFNAME);
                    return child;
                }
                break;
            case Token.BITNOT:
                if (childType == Token.NUMBER) {
                    int value = ScriptRuntime.toInt32(child.getDouble());
                    child.setDouble(~value);
                    return child;
                }
                break;
            case Token.NEG:
                if (childType == Token.NUMBER) {
                    child.setDouble(-child.getDouble());
                    return child;
                }
                break;
            case Token.NOT: {
                int status = isAlwaysDefinedBoolean(child);
                if (status != 0) {
                    int type;
                    if (status == ALWAYS_TRUE_BOOLEAN) {
                        type = Token.FALSE;
                    } else {
                        type = Token.TRUE;
                    }
                    if (childType == Token.TRUE || childType == Token.FALSE) {
                        child.setType(type);
                        return child;
                    }
                    return new Node(type);
                }
                break;
            }
        }
        return new Node(nodeType, child);
    }

    private Node createCallOrNew(int nodeType, Node child) {
        int type = Node.NON_SPECIALCALL;
        if (child.getType() == Token.NAME) {
            String name = child.getString();
            if (name.equals("eval")) {
                type = Node.SPECIALCALL_EVAL;
            } else if (name.equals("With")) {
                type = Node.SPECIALCALL_WITH;
            }
        } else if (child.getType() == Token.GETPROP) {
            String name = child.getLastChild().getString();
            if (name.equals("eval")) {
                type = Node.SPECIALCALL_EVAL;
            }
        }
        Node node = new Node(nodeType, child);
        if (type != Node.NON_SPECIALCALL) {
            // Calls to these functions require activation objects.
            setRequiresActivation();
            node.putIntProp(Node.SPECIALCALL_PROP, type);
        }
        return node;
    }

    private Node createIncDec(int nodeType, boolean post, Node child) {
        child = makeReference(child);
        int childType = child.getType();

        switch (childType) {
            case Token.NAME:
            case Token.GETPROP:
            case Token.GETELEM:
            case Token.GET_REF: {
                Node n = new Node(nodeType, child);
                int incrDecrMask = 0;
                if (nodeType == Token.DEC) {
                    incrDecrMask |= Node.DECR_FLAG;
                }
                if (post) {
                    incrDecrMask |= Node.POST_FLAG;
                }
                n.putIntProp(Node.INCRDECR_PROP, incrDecrMask);
                return n;
            }
        }
        throw Kit.codeBug();
    }

    private Node createPropertyGet(Node target, String name, Node parent) {
            if (target == null) {
                return createName(name);
            }
            checkActivationName(name, Token.GETPROP);
            if (ScriptRuntime.isSpecialProperty(name)) {
                Node ref = new Node(Token.REF_SPECIAL, target);
                ref.putProp(Node.NAME_PROP, name);
                return new Node(Token.GET_REF, ref);
            }

            Node node = new Node(Token.GETPROP, target, Node.newString(name));

            if (parent != null && parent.getProp(Node.CHAINING_PROP) != null) {
                node.putProp(Node.CHAINING_PROP, true);
            }

            if (parent != null && parent.getProp(Node.PRIVATE_ACCESS_PROP) != null) {
                node.putProp(Node.PRIVATE_ACCESS_PROP, true);
            }

            return node;
    }

    private Node createBinary(int nodeType, Node left, Node right) {
        switch (nodeType) {

            case Token.ADD:
                // numerical addition and string concatenation
                if (left.type == Token.STRING) {
                    String s2;
                    if (right.type == Token.STRING) {
                        s2 = right.getString();
                    } else if (right.type == Token.NUMBER) {
                        s2 = ScriptRuntime.numberToString(right.getDouble(), 10);
                    } else {
                        break;
                    }
                    String s1 = left.getString();
                    left.setString(s1.concat(s2));
                    return left;
                } else if (left.type == Token.NUMBER) {
                    if (right.type == Token.NUMBER) {
                        left.setDouble(left.getDouble() + right.getDouble());
                        return left;
                    } else if (right.type == Token.STRING) {
                        String s1, s2;
                        s1 = ScriptRuntime.numberToString(left.getDouble(), 10);
                        s2 = right.getString();
                        right.setString(s1.concat(s2));
                        return right;
                    }
                }
                // can't do anything if we don't know  both types - since
                // 0 + object is supposed to call toString on the object and do
                // string concantenation rather than addition
                break;

            case Token.SUB:
                // numerical subtraction
                if (left.type == Token.NUMBER) {
                    double ld = left.getDouble();
                    if (right.type == Token.NUMBER) {
                        //both numbers
                        left.setDouble(ld - right.getDouble());
                        return left;
                    } else if (ld == 0.0) {
                        // first 0: 0-x -> -x
                        return new Node(Token.NEG, right);
                    }
                } else if (right.type == Token.NUMBER) {
                    if (right.getDouble() == 0.0) {
                        //second 0: x - 0 -> +x
                        // can not make simply x because x - 0 must be number
                        return new Node(Token.POS, left);
                    }
                }
                break;

            case Token.MUL:
                // numerical multiplication
                if (left.type == Token.NUMBER) {
                    double ld = left.getDouble();
                    if (right.type == Token.NUMBER) {
                        //both numbers
                        left.setDouble(ld * right.getDouble());
                        return left;
                    } else if (ld == 1.0) {
                        // first 1: 1 *  x -> +x
                        return new Node(Token.POS, right);
                    }
                } else if (right.type == Token.NUMBER) {
                    if (right.getDouble() == 1.0) {
                        //second 1: x * 1 -> +x
                        // can not make simply x because x - 0 must be number
                        return new Node(Token.POS, left);
                    }
                }
                // can't do x*0: Infinity * 0 gives NaN, not 0
                break;

            case Token.DIV:
                // number division
                if (right.type == Token.NUMBER) {
                    double rd = right.getDouble();
                    if (left.type == Token.NUMBER) {
                        // both constants -- just divide, trust Java to handle x/0
                        left.setDouble(left.getDouble() / rd);
                        return left;
                    } else if (rd == 1.0) {
                        // second 1: x/1 -> +x
                        // not simply x to force number convertion
                        return new Node(Token.POS, left);
                    }
                }
                break;

            case Token.AND: {
                // Since x && y gives x, not false, when Boolean(x) is false,
                // and y, not Boolean(y), when Boolean(x) is true, x && y
                // can only be simplified if x is defined. See bug 309957.

                int leftStatus = isAlwaysDefinedBoolean(left);
                if (leftStatus == ALWAYS_FALSE_BOOLEAN) {
                    // if the first one is false, just return it
                    return left;
                } else if (leftStatus == ALWAYS_TRUE_BOOLEAN) {
                    // if first is true, set to second
                    return right;
                }
                break;
            }

            case Token.OR: {
                // Since x || y gives x, not true, when Boolean(x) is true,
                // and y, not Boolean(y), when Boolean(x) is false, x || y
                // can only be simplified if x is defined. See bug 309957.

                int leftStatus = isAlwaysDefinedBoolean(left);
                if (leftStatus == ALWAYS_TRUE_BOOLEAN) {
                    // if the first one is true, just return it
                    return left;
                } else if (leftStatus == ALWAYS_FALSE_BOOLEAN) {
                    // if first is false, set to second
                    return right;
                }
                break;
            }
        }

        return new Node(nodeType, left, right);
    }

    private Node createAssignment(int assignType, Node left, Node right) {
        Node ref = makeReference(left);
        if (ref == null) {
            if (left.getType() == Token.ARRAYLIT ||
                    left.getType() == Token.OBJECTLIT) {
                if (assignType != Token.ASSIGN) {
                    reportError("msg.bad.destruct.op");
                    return right;
                }
                return createDestructuringAssignment(-1, left, right);
            }
            reportError("msg.bad.assign.left");
            return right;
        }
        left = ref;

        int assignOp;
        switch (assignType) {
            case Token.ASSIGN:
                return simpleAssignment(left, right);
            case Token.ASSIGN_BITOR:
                assignOp = Token.BITOR;
                break;
            case Token.ASSIGN_BITXOR:
                assignOp = Token.BITXOR;
                break;
            case Token.ASSIGN_BITAND:
                assignOp = Token.BITAND;
                break;
            case Token.ASSIGN_LSH:
                assignOp = Token.LSH;
                break;
            case Token.ASSIGN_RSH:
                assignOp = Token.RSH;
                break;
            case Token.ASSIGN_URSH:
                assignOp = Token.URSH;
                break;
            case Token.ASSIGN_ADD:
                assignOp = Token.ADD;
                break;
            case Token.ASSIGN_SUB:
                assignOp = Token.SUB;
                break;
            case Token.ASSIGN_MUL:
                assignOp = Token.MUL;
                break;
            case Token.ASSIGN_EXP:
                assignOp = Token.EXP;
                break;
            case Token.ASSIGN_DIV:
                assignOp = Token.DIV;
                break;
            case Token.ASSIGN_MOD:
                assignOp = Token.MOD;
                break;
            case Token.ASSIGN_AND:
                assignOp = Token.AND;
                break;
            case Token.ASSIGN_OR:
                assignOp = Token.OR;
                break;
            case Token.ASSIGN_NULLISH:
                assignOp = Token.NULLISH_COALESCING;
                break;
            default:
                throw Kit.codeBug();
        }

        int nodeType = left.getType();
        switch (nodeType) {
            case Token.NAME: {
                Node op = new Node(assignOp, left, right);
                Node lvalueLeft = Node.newString(Token.BINDNAME, left.getString());
                return new Node(Token.SETNAME, lvalueLeft, op);
            }
            case Token.GETPROP:
            case Token.GETELEM: {
                Node obj = left.getFirstChild();
                Node id = left.getLastChild();

                int type = nodeType == Token.GETPROP
                        ? Token.SETPROP_OP
                        : Token.SETELEM_OP;

                Node opLeft = new Node(Token.USE_STACK);
                Node op = new Node(assignOp, opLeft, right);
                return new Node(type, obj, id, op);
            }
            case Token.GET_REF: {
                ref = left.getFirstChild();
                checkMutableReference(ref);
                Node opLeft = new Node(Token.USE_STACK);
                Node op = new Node(assignOp, opLeft, right);
                return new Node(Token.SET_REF_OP, ref, op);
            }
        }

        throw Kit.codeBug();
    }

    private Node createUseLocal(Node localBlock) {
        if (Token.LOCAL_BLOCK != localBlock.getType()) throw Kit.codeBug();
        Node result = new Node(Token.LOCAL_LOAD);
        result.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
        return result;
    }

    private Jump makeJump(int type, Node target) {
        Jump n = new Jump(type);
        n.target = target;
        return n;
    }

    private Node makeReference(Node node) {
        int type = node.getType();
        switch (type) {
            case Token.NAME:
            case Token.GETPROP:
            case Token.GETELEM:
            case Token.GET_REF:
                return node;
            case Token.CALL:
                node.setType(Token.REF_CALL);
                return new Node(Token.GET_REF, node);
        }
        // Signal caller to report error
        return null;
    }

    // Check if Node always mean true or false in boolean context
    private static int isAlwaysDefinedBoolean(Node node) {
        switch (node.getType()) {
            case Token.FALSE:
            case Token.NULL:
                return ALWAYS_FALSE_BOOLEAN;
            case Token.TRUE:
                return ALWAYS_TRUE_BOOLEAN;
            case Token.NUMBER: {
                double num = node.getDouble();
                if (!Double.isNaN(num) && num != 0.0) {
                    return ALWAYS_TRUE_BOOLEAN;
                }
                return ALWAYS_FALSE_BOOLEAN;
            }
        }
        return 0;
    }

    // Check if node is the target of a destructuring bind.
    boolean isDestructuring(Node n) {
        return n instanceof DestructuringForm
                && ((DestructuringForm) n).isDestructuring();
    }

    void decompileFunctionHeader(FunctionNode fn) {
        if (fn.getFunctionName() != null) {
            decompiler.addName(fn.getName());
        }
        boolean isArrow = fn.getFunctionType() == FunctionNode.ARROW_FUNCTION;
        boolean noParen = isArrow && fn.getLp() == -1;
        if (!noParen) {
            decompiler.addToken(Token.LP);
        }

        List<AstNode> params = fn.getParams();
        Map<Integer, Node> defaultParams = fn.getDefaultParams();

        for (int i = 0; i < params.size(); i++) {
            decompile(params.get(i));

            if (defaultParams.containsKey(i)) {
                decompiler.addToken(Token.ASSIGN);
                AstNode node = (AstNode) defaultParams.get(i);
                Node transformed = transform(node);
                fn.setDefaultParam(i, transformed);
                fn.addChildrenToBack(transformed);
            }

            if (i < params.size() - 1) {
                decompiler.addToken(Token.COMMA);
            }
        }

        if (!noParen) {
            decompiler.addToken(Token.RP);
        }
        if (isArrow) {
            decompiler.addToken(Token.ARROW);
        }
        if (!fn.isExpressionClosure()) {
            decompiler.addEOL(Token.LC);
        }
    }

    void decompile(AstNode node) {
        switch (node.getType()) {
            case Token.ARRAYLIT:
                decompileArrayLiteral((ArrayLiteral) node);
                break;
            case Token.OBJECTLIT:
                decompileObjectLiteral((ObjectLiteral) node);
                break;
            case Token.STRING:
                decompiler.addString(((StringLiteral) node).getValue());
                break;
            case Token.NAME:
                decompiler.addName(((Name) node).getIdentifier());
                break;
            case Token.NUMBER:
                decompiler.addNumber(((NumberLiteral) node).getNumber());
                break;
            case Token.GETPROP:
                decompilePropertyGet((PropertyGet) node);
                break;
            case Token.EMPTY:
                break;
            case Token.GETELEM:
                decompileElementGet((ElementGet) node);
                break;
            case Token.THIS:
                decompiler.addToken(node.getType());
                break;
            default:
//                 Kit.codeBug("unexpected token: "
//                         + Token.typeToName(node.getType()));
        }
    }

    // used for destructuring forms, since we don't transform() them
    void decompileArrayLiteral(ArrayLiteral node) {
        decompiler.addToken(Token.LB);
        List<AstNode> elems = node.getElements();
        int size = elems.size();
        for (int i = 0; i < size; i++) {
            AstNode elem = elems.get(i);
            if (elem.getType() == Token.ASSIGN) {
                Assignment assignment = (Assignment) elem;
                decompile(assignment.getLeft());
                decompiler.addToken(Token.ASSIGN);
                decompile(assignment.getRight());
            } else {
                decompile(elem);
            }
            if (i < size - 1) {
                decompiler.addToken(Token.COMMA);
            }
        }
        decompiler.addToken(Token.RB);
    }

    // only used for destructuring forms
    void decompileObjectLiteral(ObjectLiteral node) {
        decompiler.addToken(Token.LC);
        List<ObjectProperty> props = node.getElements();
        int size = props.size();
        for (int i = 0; i < size; i++) {
            ObjectProperty prop = props.get(i);
            boolean destructuringShorthand = Boolean.TRUE.equals(prop.getProp(Node.DESTRUCTURING_SHORTHAND));
            AstNode spread = prop.getSpread();
            if (spread != null) {
                decompiler.addToken(Token.SPREAD);
                decompile(spread);
            } else {
                decompile(prop.getLeft());

                if (!destructuringShorthand) {
                    decompiler.addToken(Token.COLON);
                    decompile(prop.getRight());
                }
            }
            if (i < size - 1) {
                decompiler.addToken(Token.COMMA);
            }
        }
        decompiler.addToken(Token.RC);
    }

    // only used for destructuring forms
    void decompilePropertyGet(PropertyGet node) {
        decompile(node.getTarget());
        decompiler.addToken(Token.DOT);
        decompile(node.getProperty());
    }

    // only used for destructuring forms
    void decompileElementGet(ElementGet node) {
        decompile(node.getTarget());
        decompiler.addToken(Token.LB);
        decompile(node.getElement());
        decompiler.addToken(Token.RB);
    }
}
