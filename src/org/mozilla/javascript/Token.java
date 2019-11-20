/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.optimizer.Codegen;

/**
 * This class implements the JavaScript scanner.
 * <p>
 * It is based on the C source files jsscan.c and jsscan.h
 * in the jsref package.
 *
 * @author Mike McCabe
 * @author Brendan Eich
 * @see org.mozilla.javascript.Parser
 */

public class Token {
    public enum CommentType {
        LINE, BLOCK_COMMENT, JSDOC, HTML
    }

    // debug flags
    public static final boolean printTrees = false;
    static final boolean printICode = false;
    static final boolean printNames = printTrees || printICode;

    /**
     * Token types.  These values correspond to JSTokenType values in
     * jsscan.c.
     */

    public final static int
            // start enum
            ERROR = -1, // well-known as the only code < EOF
            EOF = 0,  // end of file token - (not EOF_CHAR)
            EOL = 1,  // end of line

            // Interpreter reuses the following as bytecodes
            FIRST_BYTECODE_TOKEN = 2,

            ENTERWITH = 2,
            LEAVEWITH = 3,
            RETURN = 4,
            GOTO = 5,
            IFEQ = 6,
            IFNE = 7,
            SETNAME = 8,
            BITOR = 9,
            BITXOR = 10,
            BITAND = 11,
            EQ = 12,
            NE = 13,
            LT = 14,
            LE = 15,
            GT = 16,
            GE = 17,
            LSH = 18,
            RSH = 19,
            URSH = 20,
            ADD = 21,
            SUB = 22,
            MUL = 23,
            DIV = 24,
            MOD = 25,
            EXP = 26,
            NOT = 27,
            BITNOT = 28,
            POS = 29,
            NEG = 30,
            NEW = 31,
            DELPROP = 32,
            TYPEOF = 33,
            GETPROP = 34,
            GETPROPNOWARN = 35,
            SETPROP = 36,
            GETELEM = 37,
            SETELEM = 38,
            CALL = 39,
            NAME = 40,
            NUMBER = 41,
            STRING = 42,
            NULL = 43,
            THIS = 44,
            FALSE = 45,
            TRUE = 46,
            SHEQ = 47,   // shallow equality (===)
            SHNE = 48,   // shallow inequality (!==)
            REGEXP = 49,
            BINDNAME = 50,
            THROW = 51,
            RETHROW = 52, // rethrow caught exception: catch (e if ) use it
            IN = 53,
            INSTANCEOF = 54,
            LOCAL_LOAD = 55,
            GETVAR = 56,
            SETVAR = 57,
            CATCH_SCOPE = 58,
            ENUM_INIT_KEYS = 59,
            ENUM_INIT_VALUES = 60,
            ENUM_INIT_ARRAY = 61,
            ENUM_INIT_VALUES_IN_ORDER = 62,
            ENUM_NEXT = 63,
            ENUM_ID = 64,
            THISFN = 65,
            RETURN_RESULT = 66, // to return previously stored return result
            ARRAYLIT = 67, // array literal
            OBJECTLIT = 68, // object literal
            GET_REF = 69, // *reference
            SET_REF = 70, // *reference    = something
            DEL_REF = 71, // delete reference
            REF_CALL = 72, // f(args)    = something or f(args)++
            REF_SPECIAL = 73, // reference for special properties like __proto
            YIELD = 74,  // JS 1.7 yield pseudo keyword
            STRICT_SETNAME = 75,

            // For XML support:
            DEFAULTNAMESPACE = 76, // default xml namespace =
            ESCXMLATTR = 77,
            ESCXMLTEXT = 78,
            REF_MEMBER = 79, // Reference for x.@y, x..y etc.
            REF_NS_MEMBER = 80, // Reference for x.ns::y, x..ns::y etc.
            REF_NAME = 81, // Reference for @y, @[y] etc.
            REF_NS_NAME = 82, // Reference for ns::y, @ns::y@[y] etc.

            // End of interpreter bytecodes
            LAST_BYTECODE_TOKEN = REF_NS_NAME,

            TRY = 83,
            SEMI = 84,  // semicolon
            LB = 85,  // left and right brackets
            RB = 86,
            LC = 87,  // left and right curlies (braces)
            RC = 88,
            LP = 89,  // left and right parentheses
            RP = 90,
            COMMA = 91,  // comma operator

            ASSIGN = 92,  // simple assignment  (=)
            ASSIGN_BITOR = 93,  // |=
            ASSIGN_BITXOR = 94,  // ^=
            ASSIGN_BITAND = 95,  // |=
            ASSIGN_LSH = 96,  // <<=
            ASSIGN_RSH = 97,  // >>=
            ASSIGN_URSH = 98,  // >>>=
            ASSIGN_ADD = 99,  // +=
            ASSIGN_SUB = 100, // -=
            ASSIGN_MUL = 101,  // *=
            ASSIGN_DIV = 102,  // /=
            ASSIGN_MOD = 103,  // %=
            ASSIGN_EXP = 104,  // **=

            FIRST_ASSIGN = ASSIGN,
            LAST_ASSIGN = ASSIGN_EXP,

            HOOK = 105, // conditional (?:)
            COLON = 106,
            OR = 107, // logical or (||)
            AND = 108, // logical and (&&)
            INC = 109, // increment/decrement (++ --)
            DEC = 110,
            DOT = 111, // member operator (.)
            SPREAD = 112,
            OPTIONAL_CHAINING = 113, // optional chaining member operator (?.) https://tc39.es/proposal-optional-chaining/
            PIPELINE = 114, // pipeline operator (|>) https://tc39.es/proposal-pipeline-operator
            NULLISH_COALESCING = 115, // pipeline operator (??) https://tc39.es/proposal-nullish-coalescing/
            FUNCTION = 116, // function keyword
            EXPORT = 117, // export keyword
            IMPORT = 118, // import keyword
            IF = 119, // if keyword
            ELSE = 120, // else keyword
            SWITCH = 121, // switch keyword
            CASE = 122, // case keyword
            DEFAULT = 123, // default keyword
            WHILE = 124, // while keyword
            DO = 125, // do keyword
            FOR = 126, // for keyword
            BREAK = 127, // break keyword
            CONTINUE = 128, // continue keyword
            VAR = 129, // var keyword
            WITH = 130, // with keyword
            CATCH = 131, // catch keyword
            FINALLY = 132, // finally keyword
            VOID = 133, // void keyword
            RESERVED = 134, // reserved keywords

            EMPTY = 135,

            /* types used for the parse tree - these never get returned
             * by the scanner.
             */
            BLOCK = 136, // statement block
            LABEL = 137, // label
            TARGET = 138,
            LOOP = 139,
            EXPR_VOID = 140, // expression statement in functions
            EXPR_RESULT = 141, // expression statement in scripts
            JSR = 142,
            SCRIPT = 143, // top-level node for entire script
            TYPEOFNAME = 144, // for typeof(simple-name)
            USE_STACK = 145,
            SETPROP_OP = 146, // x.y op= something
            SETELEM_OP = 147, // x[y] op= something
            LOCAL_BLOCK = 148,
            SET_REF_OP = 149, // *reference op= something

            // For XML support:
            DOTDOT = 150,  // member operator (..)
            COLONCOLON = 151,  // namespace::name
            XML = 152,  // XML type
            DOTQUERY = 153,  // .() -- e.g., x.emps.emp.(name == "terry")
            XMLATTR = 154,  // @
            XMLEND = 155,

            // Optimizer-only-tokens
            TO_OBJECT = 156,
            TO_DOUBLE = 157,

            GET = 158,  // JS 1.5 get pseudo keyword
            SET = 159,  // JS 1.5 set pseudo keyword
            LET = 160,  // JS 1.7 let pseudo keyword
            CONST = 161,
            SETCONST = 162,
            SETCONSTVAR = 163,
            ARRAYCOMP = 164,  // array comprehension
            LETEXPR = 165,
            WITHEXPR = 166,
            DEBUGGER = 167,
            COMMENT = 168,
            GENEXPR = 169,
            METHOD = 170, // ES6 MethodDefinition
            ARROW = 171,  // ES6 ArrowFunction
            LAST_TOKEN = 172;

    /**
     * Returns a name for the token.  If Rhino is compiled with certain
     * hardcoded debugging flags in this file, it calls {@code #typeToName};
     * otherwise it returns a string whose value is the token number.
     */
    public static String name(int token) {
        if (!printNames && !Codegen.DEBUG_CODEGEN) {
            return String.valueOf(token);
        }
        return typeToName(token);
    }

    /**
     * Always returns a human-readable string for the token name.
     * For instance, {@link #FINALLY} has the name "FINALLY".
     *
     * @param token the token code
     * @return the actual name for the token code
     */
    public static String typeToName(int token) {
        switch (token) {
            case ERROR:
                return "ERROR";
            case EOF:
                return "EOF";
            case EOL:
                return "EOL";
            case ENTERWITH:
                return "ENTERWITH";
            case LEAVEWITH:
                return "LEAVEWITH";
            case RETURN:
                return "RETURN";
            case GOTO:
                return "GOTO";
            case IFEQ:
                return "IFEQ";
            case IFNE:
                return "IFNE";
            case SETNAME:
                return "SETNAME";
            case BITOR:
                return "BITOR";
            case BITXOR:
                return "BITXOR";
            case BITAND:
                return "BITAND";
            case EQ:
                return "EQ";
            case NE:
                return "NE";
            case LT:
                return "LT";
            case LE:
                return "LE";
            case GT:
                return "GT";
            case GE:
                return "GE";
            case LSH:
                return "LSH";
            case RSH:
                return "RSH";
            case URSH:
                return "URSH";
            case ADD:
                return "ADD";
            case SUB:
                return "SUB";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case MOD:
                return "MOD";
            case EXP:
                return "EXP";
            case NOT:
                return "NOT";
            case BITNOT:
                return "BITNOT";
            case POS:
                return "POS";
            case NEG:
                return "NEG";
            case NEW:
                return "NEW";
            case DELPROP:
                return "DELPROP";
            case TYPEOF:
                return "TYPEOF";
            case GETPROP:
                return "GETPROP";
            case GETPROPNOWARN:
                return "GETPROPNOWARN";
            case SETPROP:
                return "SETPROP";
            case GETELEM:
                return "GETELEM";
            case SETELEM:
                return "SETELEM";
            case CALL:
                return "CALL";
            case NAME:
                return "NAME";
            case NUMBER:
                return "NUMBER";
            case STRING:
                return "STRING";
            case NULL:
                return "NULL";
            case THIS:
                return "THIS";
            case FALSE:
                return "FALSE";
            case TRUE:
                return "TRUE";
            case SHEQ:
                return "SHEQ";
            case SHNE:
                return "SHNE";
            case REGEXP:
                return "REGEXP";
            case BINDNAME:
                return "BINDNAME";
            case THROW:
                return "THROW";
            case RETHROW:
                return "RETHROW";
            case IN:
                return "IN";
            case INSTANCEOF:
                return "INSTANCEOF";
            case LOCAL_LOAD:
                return "LOCAL_LOAD";
            case GETVAR:
                return "GETVAR";
            case SETVAR:
                return "SETVAR";
            case CATCH_SCOPE:
                return "CATCH_SCOPE";
            case ENUM_INIT_KEYS:
                return "ENUM_INIT_KEYS";
            case ENUM_INIT_VALUES:
                return "ENUM_INIT_VALUES";
            case ENUM_INIT_ARRAY:
                return "ENUM_INIT_ARRAY";
            case ENUM_INIT_VALUES_IN_ORDER:
                return "ENUM_INIT_VALUES_IN_ORDER";
            case ENUM_NEXT:
                return "ENUM_NEXT";
            case ENUM_ID:
                return "ENUM_ID";
            case THISFN:
                return "THISFN";
            case RETURN_RESULT:
                return "RETURN_RESULT";
            case ARRAYLIT:
                return "ARRAYLIT";
            case OBJECTLIT:
                return "OBJECTLIT";
            case GET_REF:
                return "GET_REF";
            case SET_REF:
                return "SET_REF";
            case DEL_REF:
                return "DEL_REF";
            case REF_CALL:
                return "REF_CALL";
            case REF_SPECIAL:
                return "REF_SPECIAL";
            case DEFAULTNAMESPACE:
                return "DEFAULTNAMESPACE";
            case ESCXMLTEXT:
                return "ESCXMLTEXT";
            case ESCXMLATTR:
                return "ESCXMLATTR";
            case REF_MEMBER:
                return "REF_MEMBER";
            case REF_NS_MEMBER:
                return "REF_NS_MEMBER";
            case REF_NAME:
                return "REF_NAME";
            case REF_NS_NAME:
                return "REF_NS_NAME";
            case TRY:
                return "TRY";
            case SEMI:
                return "SEMI";
            case LB:
                return "LB";
            case RB:
                return "RB";
            case LC:
                return "LC";
            case RC:
                return "RC";
            case LP:
                return "LP";
            case RP:
                return "RP";
            case COMMA:
                return "COMMA";
            case ASSIGN:
                return "ASSIGN";
            case ASSIGN_BITOR:
                return "ASSIGN_BITOR";
            case ASSIGN_BITXOR:
                return "ASSIGN_BITXOR";
            case ASSIGN_BITAND:
                return "ASSIGN_BITAND";
            case ASSIGN_LSH:
                return "ASSIGN_LSH";
            case ASSIGN_RSH:
                return "ASSIGN_RSH";
            case ASSIGN_URSH:
                return "ASSIGN_URSH";
            case ASSIGN_ADD:
                return "ASSIGN_ADD";
            case ASSIGN_SUB:
                return "ASSIGN_SUB";
            case ASSIGN_MUL:
                return "ASSIGN_MUL";
            case ASSIGN_EXP:
                return "ASSIGN_EXP";
            case ASSIGN_DIV:
                return "ASSIGN_DIV";
            case ASSIGN_MOD:
                return "ASSIGN_MOD";
            case HOOK:
                return "HOOK";
            case COLON:
                return "COLON";
            case OR:
                return "OR";
            case AND:
                return "AND";
            case INC:
                return "INC";
            case DEC:
                return "DEC";
            case DOT:
                return "DOT";
            case FUNCTION:
                return "FUNCTION";
            case EXPORT:
                return "EXPORT";
            case IMPORT:
                return "IMPORT";
            case IF:
                return "IF";
            case ELSE:
                return "ELSE";
            case SWITCH:
                return "SWITCH";
            case CASE:
                return "CASE";
            case DEFAULT:
                return "DEFAULT";
            case WHILE:
                return "WHILE";
            case DO:
                return "DO";
            case FOR:
                return "FOR";
            case BREAK:
                return "BREAK";
            case CONTINUE:
                return "CONTINUE";
            case VAR:
                return "VAR";
            case WITH:
                return "WITH";
            case CATCH:
                return "CATCH";
            case FINALLY:
                return "FINALLY";
            case VOID:
                return "VOID";
            case RESERVED:
                return "RESERVED";
            case EMPTY:
                return "EMPTY";
            case BLOCK:
                return "BLOCK";
            case LABEL:
                return "LABEL";
            case TARGET:
                return "TARGET";
            case LOOP:
                return "LOOP";
            case EXPR_VOID:
                return "EXPR_VOID";
            case EXPR_RESULT:
                return "EXPR_RESULT";
            case JSR:
                return "JSR";
            case SCRIPT:
                return "SCRIPT";
            case TYPEOFNAME:
                return "TYPEOFNAME";
            case USE_STACK:
                return "USE_STACK";
            case SETPROP_OP:
                return "SETPROP_OP";
            case SETELEM_OP:
                return "SETELEM_OP";
            case LOCAL_BLOCK:
                return "LOCAL_BLOCK";
            case SET_REF_OP:
                return "SET_REF_OP";
            case DOTDOT:
                return "DOTDOT";
            case COLONCOLON:
                return "COLONCOLON";
            case XML:
                return "XML";
            case DOTQUERY:
                return "DOTQUERY";
            case XMLATTR:
                return "XMLATTR";
            case XMLEND:
                return "XMLEND";
            case TO_OBJECT:
                return "TO_OBJECT";
            case TO_DOUBLE:
                return "TO_DOUBLE";
            case GET:
                return "GET";
            case SET:
                return "SET";
            case LET:
                return "LET";
            case YIELD:
                return "YIELD";
            case CONST:
                return "CONST";
            case SETCONST:
                return "SETCONST";
            case SETCONSTVAR:
                return "SETCONSTVAR";
            case ARRAYCOMP:
                return "ARRAYCOMP";
            case WITHEXPR:
                return "WITHEXPR";
            case LETEXPR:
                return "LETEXPR";
            case DEBUGGER:
                return "DEBUGGER";
            case COMMENT:
                return "COMMENT";
            case GENEXPR:
                return "GENEXPR";
            case METHOD:
                return "METHOD";
            case ARROW:
                return "ARROW";
            case OPTIONAL_CHAINING:
                return "OPTIONAL CHAINING";
        }

        // Token without name
        throw new IllegalStateException(String.valueOf(token));
    }

    /**
     * Convert a keyword token to a name string for use with the
     * {@link Context#FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER} feature.
     *
     * @param token A token
     * @return the corresponding name string
     */
    public static String keywordToName(int token) {
        switch (token) {
            case Token.BREAK:
                return "break";
            case Token.CASE:
                return "case";
            case Token.CONTINUE:
                return "continue";
            case Token.DEFAULT:
                return "default";
            case Token.DELPROP:
                return "delete";
            case Token.DO:
                return "do";
            case Token.ELSE:
                return "else";
            case Token.FALSE:
                return "false";
            case Token.FOR:
                return "for";
            case Token.FUNCTION:
                return "function";
            case Token.IF:
                return "if";
            case Token.IN:
                return "in";
            case Token.LET:
                return "let";
            case Token.NEW:
                return "new";
            case Token.NULL:
                return "null";
            case Token.RETURN:
                return "return";
            case Token.SWITCH:
                return "switch";
            case Token.THIS:
                return "this";
            case Token.TRUE:
                return "true";
            case Token.TYPEOF:
                return "typeof";
            case Token.VAR:
                return "var";
            case Token.VOID:
                return "void";
            case Token.WHILE:
                return "while";
            case Token.WITH:
                return "with";
            case Token.YIELD:
                return "yield";
            case Token.CATCH:
                return "catch";
            case Token.CONST:
                return "const";
            case Token.DEBUGGER:
                return "debugger";
            case Token.FINALLY:
                return "finally";
            case Token.INSTANCEOF:
                return "instanceof";
            case Token.THROW:
                return "throw";
            case Token.TRY:
                return "try";
            default:
                return null;
        }
    }

    /**
     * Return true if the passed code is a valid Token constant.
     *
     * @param code a potential token code
     * @return true if it's a known token
     */
    public static boolean isValidToken(int code) {
        return code >= ERROR
                && code <= LAST_TOKEN;
    }
}
