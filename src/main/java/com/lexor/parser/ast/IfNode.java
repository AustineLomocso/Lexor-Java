package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * IfNode — Represents an IF / ELSE IF / ELSE conditional structure.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * LEXOR supports three forms of conditional (from the spec):
 *
 *   Form 1 — simple if:
 *     IF (<condition>)
 *     START IF
 *       <statements>
 *     END IF
 *
 *   Form 2 — if-else:
 *     IF (<condition>)
 *     START IF
 *       <statements>
 *     END IF
 *     ELSE
 *     START IF
 *       <statements>
 *     END IF
 *
 *   Form 3 — if / else-if chain / else:
 *     IF (<condition>)
 *     START IF  ...  END IF
 *     ELSE IF (<condition>)
 *     START IF  ...  END IF
 *     ELSE
 *     START IF  ...  END IF
 *
 * IfNode captures all three forms in one class:
 *   condition      — the primary IF condition (always present)
 *   thenBlock      — statements executed when condition is true
 *   elseIfClauses  — zero or more ELSE IF branches (each has its own condition + block)
 *   elseBlock      — optional fallback block (null if no ELSE)
 *
 * WHY ElseIfClause AS AN INNER CLASS?
 * ------------------------------------
 * Each ELSE IF branch is a (condition, body) pair. Rather than using a parallel
 * List<ASTNode> and List<List<ASTNode>>, a small inner class makes the pairing
 * explicit and readable. It is defined as a static nested class so it can be
 * instantiated without an IfNode instance.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • java.util.List  — holds thenBlock, elseIfClauses, elseBlock.
 * • ASTVisitor<T>   — accept() calls visitor.visitIf(this).
 *   The Interpreter's visitIf() evaluates condition, picks the right block,
 *   pushes a child Environment scope, executes the block, then pops the scope.
 */
public class IfNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DEFINE THE ElseIfClause INNER CLASS
    // =========================================================================
    // An ELSE IF clause has exactly two parts: its own condition and its own body.
    //
    // TODO 1a: Declare  public static final class ElseIfClause
    // TODO 1b: Fields:  private final ASTNode condition;
    //                   private final List<ASTNode> body;
    // TODO 1c: Constructor: public ElseIfClause(ASTNode condition, List<ASTNode> body)
    // TODO 1d: Getters: public ASTNode      getCondition()  and
    //                   public List<ASTNode> getBody()
    //
    public static final class ElseIfClause {
        private final ASTNode       condition;
        private final List<ASTNode> body;

        public ElseIfClause(ASTNode condition, List<ASTNode> body) {
            this.condition = condition;
            this.body      = body;
        }

        public ASTNode       getCondition() { return condition; }
        public List<ASTNode> getBody()      { return body;      }
    }

    // =========================================================================
    // TODO STEP 2 — DECLARE FIELDS
    // =========================================================================
    // TODO 2a: private final ASTNode condition;
    //          The primary IF condition expression — must evaluate to BOOL.
    //
    // TODO 2b: private final List<ASTNode> thenBlock;
    //          Statements to execute when condition is TRUE.
    //
    // TODO 2c: private final List<ElseIfClause> elseIfClauses;
    //          Zero or more ELSE IF branches.  Empty list (not null) when none exist.
    //
    // TODO 2d: private final List<ASTNode> elseBlock;
    //          Statements for the ELSE branch.  null when no ELSE is present.
    //
    private final ASTNode             condition;
    private final List<ASTNode>       thenBlock;
    private final List<ElseIfClause>  elseIfClauses;
    private final List<ASTNode>       elseBlock;  // nullable

    // =========================================================================
    // TODO STEP 3 — CONSTRUCTOR
    // =========================================================================
    // TODO 3a: Accept all four parameters. elseBlock may be null.
    // TODO 3b: super(line), assign all fields.
    //
    public IfNode(int line,
                  ASTNode condition,
                  List<ASTNode> thenBlock,
                  List<ElseIfClause> elseIfClauses,
                  List<ASTNode> elseBlock) {
        super(line);
        this.condition     = condition;
        this.thenBlock     = thenBlock;
        this.elseIfClauses = elseIfClauses;
        this.elseBlock     = elseBlock;
    }

    // =========================================================================
    // TODO STEP 4 — accept()
    // =========================================================================
    // TODO 4a: return visitor.visitIf(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIf(this);
    }

    // =========================================================================
    // TODO STEP 5 — GETTERS
    // =========================================================================
    // TODO 5a: public ASTNode            getCondition()     — returns condition
    // TODO 5b: public List<ASTNode>      getThenBlock()     — returns thenBlock
    // TODO 5c: public List<ElseIfClause> getElseIfClauses() — returns elseIfClauses
    // TODO 5d: public List<ASTNode>      getElseBlock()     — returns elseBlock (may be null)
    // TODO 5e: public boolean            hasElse()          — returns elseBlock != null
    //
    public ASTNode            getCondition()     { return condition;     }
    public List<ASTNode>      getThenBlock()     { return thenBlock;     }
    public List<ElseIfClause> getElseIfClauses() { return elseIfClauses; }
    public List<ASTNode>      getElseBlock()     { return elseBlock;     }
    public boolean            hasElse()          { return elseBlock != null; }
}
