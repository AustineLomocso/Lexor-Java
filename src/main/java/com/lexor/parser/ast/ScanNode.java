package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * ScanNode — Represents a SCAN input statement.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * SCAN is LEXOR's input statement. It reads one or more comma-separated values
 * from standard input and stores each into a declared variable.
 *
 * Syntax:
 *   SCAN: x, y
 *
 * The user types values at the prompt separated by commas. The Interpreter:
 *   1. Reads the entire input line.
 *   2. Splits it on ','.
 *   3. Trims whitespace from each part.
 *   4. Coerces each string value to the declared type of the corresponding variable.
 *   5. Stores the coerced value in the Environment.
 *
 * The Parser simply records the list of variable name strings. No expression
 * evaluation is needed for the targets — they are identifiers, not expressions.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • java.util.List — holds the target variable names.
 * • ASTVisitor<T>  — accept() calls visitor.visitScan(this).
 */
public class ScanNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: Declare  private final List<String> variables;
    //          Each String is a variable name that will receive one input value.
    //          For  SCAN: x, y  this is ["x", "y"].
    //
    private final List<String> variables;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public ScanNode(int line, List<String> variables)
    // TODO 2b: super(line), assign field.
    //
    public ScanNode(int line, List<String> variables) {
        super(line);
        this.variables = variables;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitScan(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitScan(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTER
    // =========================================================================
    // TODO 4a: public List<String> getVariables() — returns variables
    //
    public List<String> getVariables() { return variables; }
}
