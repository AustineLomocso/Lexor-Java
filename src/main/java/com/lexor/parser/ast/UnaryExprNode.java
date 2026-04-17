package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

/**
 * UnaryExprNode — Represents an expression with a single operand and a prefix operator.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * Some operators act on only one value:
 *   -x     → negate a numeric value    (unary minus,  operator = "-")
 *   +x     → leave a numeric value as-is (unary plus, operator = "+")
 *   NOT d  → invert a BOOL value        (logical NOT, operator = "NOT")
 *
 * From the spec sample:
 *   xyz = ((abc * 5) / 10 + 10) * -1    ← the "-1" is UnaryExpr("-", LiteralNode(1))
 *
 * WHY A SEPARATE CLASS FROM BinaryExprNode?
 * -----------------------------------------
 * A unary expression has a fundamentally different shape — one child, not two.
 * Merging it into BinaryExprNode with a null 'left' would be error-prone and harder
 * to read in the Interpreter's switch. A dedicated class is cleaner.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • ASTVisitor<T> — accept() calls visitor.visitUnaryExpr(this).
 *   The SemanticAnalyzer checks the operand's type against the operator
 *   (numeric types for +/-, BOOL for NOT).
 *   The Interpreter applies the operator to the evaluated operand.
 */
public class UnaryExprNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: private final String  operator;  "-", "+", or "NOT"
    // TODO 1b: private final ASTNode operand;   the single sub-expression
    //
    private final String  operator;
    private final ASTNode operand;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public UnaryExprNode(int line, String operator, ASTNode operand)
    // TODO 2b: super(line), assign fields.
    //
    public UnaryExprNode(int line, String operator, ASTNode operand) {
        super(line);
        this.operator = operator;
        this.operand  = operand;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitUnaryExpr(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryExpr(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTERS
    // =========================================================================
    // TODO 4a: public String  getOperator() — returns operator
    // TODO 4b: public ASTNode getOperand()  — returns operand
    //
    public String  getOperator() { return operator; }
    public ASTNode getOperand()  { return operand;  }
}
