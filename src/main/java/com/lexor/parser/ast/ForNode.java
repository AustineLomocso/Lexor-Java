package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * ForNode — Represents a FOR counted-loop control structure.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * The FOR loop in LEXOR follows this syntax from the spec:
 *
 *   FOR (<initialization>, <condition>, <update>)
 *   START FOR
 *     <statements>
 *   END FOR
 *
 * Example:
 *   FOR (i=0, i<10, i=i+1)
 *   START FOR
 *     PRINT: i
 *   END FOR
 *
 * The three header clauses map to:
 *   init      — AssignmentNode  (the  i=0  part; sets the loop variable before the loop starts)
 *   condition — ASTNode         (the  i<10 part; evaluated before each iteration; must be BOOL)
 *   update    — AssignmentNode  (the  i=i+1 part; executed at the end of each iteration)
 *
 * WHY USE AssignmentNode FOR init AND update?
 * --------------------------------------------
 * The LEXOR FOR spec explicitly shows assignment expressions in those positions.
 * Storing them as AssignmentNode (rather than plain ASTNode) lets the Interpreter
 * call visitAssignment() on them directly without extra type-casting.
 *
 * EXECUTION ORDER (implemented in Interpreter.visitFor):
 *   1. Execute init once.
 *   2. Evaluate condition — if FALSE, exit loop immediately.
 *   3. Execute body statements in a child Environment scope.
 *   4. Execute update.
 *   5. Go to step 2.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • java.util.List  — holds the body statements.
 * • ASTVisitor<T>   — accept() calls visitor.visitFor(this).
 */
public class ForNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: private final AssignmentNode init;
    //          The loop initialization, e.g. i=0.  Executed once before the loop.
    //
    // TODO 1b: private final ASTNode condition;
    //          The continuation condition, e.g. i<10.  Must evaluate to BOOL.
    //          Checked before every iteration.
    //
    // TODO 1c: private final AssignmentNode update;
    //          The loop increment/decrement, e.g. i=i+1.  Executed after each body pass.
    //
    // TODO 1d: private final List<ASTNode> body;
    //          The statements to execute on each iteration.
    //
    private final AssignmentNode init;
    private final ASTNode        condition;
    private final AssignmentNode update;
    private final List<ASTNode>  body;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: Accept (int line, AssignmentNode init, ASTNode condition,
    //                  AssignmentNode update, List<ASTNode> body)
    // TODO 2b: super(line), assign all fields.
    //
    public ForNode(int line,
                   AssignmentNode init,
                   ASTNode condition,
                   AssignmentNode update,
                   List<ASTNode> body) {
        super(line);
        this.init      = init;
        this.condition = condition;
        this.update    = update;
        this.body      = body;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitFor(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFor(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTERS
    // =========================================================================
    // TODO 4a: public AssignmentNode getInit()      — returns init
    // TODO 4b: public ASTNode        getCondition() — returns condition
    // TODO 4c: public AssignmentNode getUpdate()    — returns update
    // TODO 4d: public List<ASTNode>  getBody()      — returns body
    //
    public AssignmentNode getInit()      { return init;      }
    public ASTNode        getCondition() { return condition; }
    public AssignmentNode getUpdate()    { return update;    }
    public List<ASTNode>  getBody()      { return body;      }
}
