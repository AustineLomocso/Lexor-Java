package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

/**
 * VariableNode — Represents a variable reference (read) in an expression.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * When a variable name appears in an expression (not on the left side of an
 * assignment, but as part of the value being computed), it is a variable *read*.
 * The Interpreter needs to look up its current value from the Environment.
 *
 * Examples:
 *   PRINT: x & t & z      → x, t, z are each a VariableNode
 *   xyz = ((abc * 5) / 10) → abc is a VariableNode
 *   IF (a < b AND c <> 200) → a, b, c are VariableNodes
 *
 * A VariableNode holds only the variable's name. At runtime, the Interpreter
 * calls  environment.get(name)  to retrieve the current LexorValue.
 *
 * VARIABLE NODE vs. ASSIGNMENT TARGET
 * ------------------------------------
 * On the LEFT side of an assignment (x = 4), the variable is NOT wrapped in a
 * VariableNode — the Parser stores it as a plain String in AssignmentNode.targets.
 * VariableNode is only used when a variable is being *read* as an expression.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • ASTVisitor<T> — accept() calls visitor.visitVariable(this).
 *   The SemanticAnalyzer checks that 'name' is registered in the SymbolTable.
 *   The Interpreter looks up the value in the Environment.
 */
public class VariableNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELD
    // =========================================================================
    // TODO 1a: private final String name;
    //          The exact identifier string as it appears in source (case-sensitive).
    //
    private final String name;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public VariableNode(int line, String name)
    // TODO 2b: super(line), assign name.
    //
    public VariableNode(int line, String name) {
        super(line);
        this.name = name;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitVariable(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariable(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTER
    // =========================================================================
    // TODO 4a: public String getName() — returns name
    //
    public String getName() { return name; }
}
