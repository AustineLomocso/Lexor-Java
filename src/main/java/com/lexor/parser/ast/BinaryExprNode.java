package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

/**
 * BinaryExprNode — Represents an expression with two operands and one operator.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * Most expressions in LEXOR involve two sub-expressions combined by an operator:
 *   arithmetic:  abc * 5,  x + 10,  y / 2
 *   relational:  a < b,    c <> 200
 *   logical:     a < b AND c <> 200
 *
 * BinaryExprNode stores:
 *   left     — the left-hand sub-expression (any ASTNode that produces a value)
 *   operator — the operator as a String ("+", "-", "*", "/", "%", ">", "<",
 *              ">=", "<=", "==", "<>", "AND", "OR")
 *   right    — the right-hand sub-expression
 *
 * WHY OPERATOR AS STRING, NOT TOKEN?
 * -----------------------------------
 * Storing the operator lexeme string ("+", "AND", etc.) rather than the full Token
 * is a deliberate simplification. The Interpreter only needs the operator symbol to
 * decide what calculation to perform — it does not need line/column from the token.
 * If you later want better error messages, you could store the Token instead.
 *
 * TREE STRUCTURE EXAMPLE
 * ----------------------
 * The expression  (abc * 5) / 10 + 10  becomes:
 *
 *        BinaryExpr("+")
 *       /               \
 *  BinaryExpr("/")     LiteralNode(10)
 *   /         \
 * BinaryExpr("*")   LiteralNode(10)
 *  /         \
 * Var(abc)  Literal(5)
 *
 * The tree shape encodes operator precedence — the deepest nodes evaluate first.
 * This shape is produced naturally by the recursive descent parser's method call hierarchy.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • ASTVisitor<T> — accept() calls visitor.visitBinaryExpr(this).
 *   The SemanticAnalyzer checks that left and right types are compatible with the operator.
 *   The Interpreter evaluates both sides and applies the operator.
 */
public class BinaryExprNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: private final ASTNode left;
    // TODO 1b: private final String  operator;   e.g. "+", "-", "AND", "<>"
    // TODO 1c: private final ASTNode right;
    //
    private final ASTNode left;
    private final String  operator;
    private final ASTNode right;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public BinaryExprNode(int line, ASTNode left, String operator, ASTNode right)
    // TODO 2b: super(line), assign all fields.
    //
    public BinaryExprNode(int line, ASTNode left, String operator, ASTNode right) {
        super(line);
        this.left     = left;
        this.operator = operator;
        this.right    = right;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitBinaryExpr(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryExpr(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTERS
    // =========================================================================
    // TODO 4a: public ASTNode getLeft()     — returns left
    // TODO 4b: public String  getOperator() — returns operator
    // TODO 4c: public ASTNode getRight()    — returns right
    //
    public ASTNode getLeft()     { return left;     }
    public String  getOperator() { return operator; }
    public ASTNode getRight()    { return right;    }
}
