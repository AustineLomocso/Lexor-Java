package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * AssignmentNode — Represents a variable assignment statement, including chained assignment.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * LEXOR allows both simple and chained assignment on the same line:
 *   x=4          → assigns 4 to x
 *   x=y=4        → assigns 4 to y first, then 4 to x (right-to-left evaluation)
 *
 * The 'targets' list holds all the variable names on the left side (in left-to-right
 * order as written). The Interpreter evaluates 'value' once and stores the result into
 * every target, walking the targets list right-to-left.
 *
 * AssignmentNode also appears inside ForNode to represent the initialization and
 * update clauses of a FOR loop:
 *   FOR (i=0, i<10, i=i+1)  → init = AssignmentNode(i,0), update = AssignmentNode(i,i+1)
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • java.util.List — holds the target variable names.
 * • ASTVisitor<T>  — accept() calls visitor.visitAssignment(this).
 */
public class AssignmentNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: Declare  private final List<String> targets;
    //          For  x=4        this is ["x"]
    //          For  x=y=4      this is ["x", "y"]
    //
    // TODO 1b: Declare  private final ASTNode value;
    //          The expression whose result is assigned to all targets.
    //
    private final List<String> targets;
    private final ASTNode      value;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public AssignmentNode(int line, List<String> targets, ASTNode value)
    // TODO 2b: super(line), assign fields.
    //
    public AssignmentNode(int line, List<String> targets, ASTNode value) {
        super(line);
        this.targets = targets;
        this.value   = value;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitAssignment(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAssignment(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTERS
    // =========================================================================
    // TODO 4a: public List<String> getTargets() — returns targets
    // TODO 4b: public ASTNode      getValue()   — returns value
    //
    public List<String> getTargets() { return targets; }
    public ASTNode      getValue()   { return value;   }
}
