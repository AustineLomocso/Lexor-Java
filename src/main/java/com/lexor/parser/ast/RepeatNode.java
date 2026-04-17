package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * RepeatNode — Represents a REPEAT WHEN loop (while-style loop).
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * The REPEAT WHEN construct is LEXOR's while-loop equivalent. From the spec:
 *
 *   REPEAT WHEN (<condition>)
 *   START REPEAT
 *     <statements>
 *   END REPEAT
 *
 * The loop repeats its body AS LONG AS the condition evaluates to TRUE.
 * This is a pre-condition loop (condition is checked before each iteration),
 * analogous to Java's  while (condition) { ... }
 *
 * Compare with ForNode:
 *   FOR      — init + condition + update  (counted loop; update is automatic)
 *   REPEAT WHEN — condition only          (free-form loop; programmer manages the counter)
 *
 * EXECUTION ORDER (implemented in Interpreter.visitRepeat):
 *   1. Evaluate condition — if FALSE, skip the entire loop body.
 *   2. Execute body statements in a child Environment scope.
 *   3. Re-evaluate condition — if TRUE, go back to step 2.
 *   4. Exit loop.
 *
 * IMPORTANT: Because the condition is checked first (pre-test loop), it is possible
 * for the body to never execute if the condition starts as FALSE. The Interpreter
 * must not execute the body even once in that case.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • java.util.List  — holds the body statements.
 * • ASTVisitor<T>   — accept() calls visitor.visitRepeat(this).
 *   The Interpreter's visitRepeat() repeatedly evaluates condition (must be BOOL)
 *   and executes body in a fresh child scope on each iteration.
 */
public class RepeatNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: private final ASTNode condition;
    //          The loop continuation condition. Must evaluate to BOOL.
    //          Checked BEFORE each iteration (pre-test / while semantics).
    //
    // TODO 1b: private final List<ASTNode> body;
    //          The statements executed on each iteration where condition is TRUE.
    //
    private final ASTNode       condition;
    private final List<ASTNode> body;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public RepeatNode(int line, ASTNode condition, List<ASTNode> body)
    // TODO 2b: super(line), assign fields.
    //
    public RepeatNode(int line, ASTNode condition, List<ASTNode> body) {
        super(line);
        this.condition = condition;
        this.body      = body;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitRepeat(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitRepeat(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTERS
    // =========================================================================
    // TODO 4a: public ASTNode       getCondition() — returns condition
    // TODO 4b: public List<ASTNode> getBody()      — returns body
    //
    public ASTNode       getCondition() { return condition; }
    public List<ASTNode> getBody()      { return body;      }
}
