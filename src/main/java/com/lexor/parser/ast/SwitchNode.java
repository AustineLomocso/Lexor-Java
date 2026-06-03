package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * SwitchNode — Represents a SWITCH / CASE / DEFAULT multi-way branch.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * SWITCH dispatches on the value of a single subject expression, choosing the
 * first CASE whose value equals the subject. It is the multi-way analogue of
 * IfNode's IF / ELSE IF / ELSE chain.
 *
 *   SWITCH (<subject>)
 *   START SWITCH
 *       CASE <value>:
 *           <statements>
 *       CASE <value>:
 *           <statements>
 *       DEFAULT:
 *           <statements>
 *   END SWITCH
 *
 * SEMANTICS — AUTO-BREAK
 * ----------------------
 * Only the first matching CASE runs; control then leaves the switch (no
 * fall-through, no BREAK keyword). DEFAULT is optional and runs only when no
 * CASE matched. This mirrors the "exactly one branch" behaviour of IfNode.
 *
 * WHY CaseClause AS AN INNER CLASS?
 * ---------------------------------
 * Each CASE is a (value, body) pair — the same shape as IfNode.ElseIfClause.
 * A small static nested class keeps the pairing explicit and readable.
 */
public class SwitchNode extends ASTNode {

    /** A single CASE: its match value and the statements to run on a match. */
    public static final class CaseClause {
        private final ASTNode       value;
        private final List<ASTNode> body;

        public CaseClause(ASTNode value, List<ASTNode> body) {
            this.value = value;
            this.body  = body;
        }

        public ASTNode       getValue() { return value; }
        public List<ASTNode> getBody()  { return body;  }
    }

    private final ASTNode          subject;      // expression in SWITCH (...)
    private final List<CaseClause> cases;        // ordered CASE clauses
    private final List<ASTNode>    defaultBlock; // nullable — DEFAULT body

    public SwitchNode(int line,
                      ASTNode subject,
                      List<CaseClause> cases,
                      List<ASTNode> defaultBlock) {
        super(line);
        this.subject      = subject;
        this.cases        = cases;
        this.defaultBlock = defaultBlock;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitSwitch(this);
    }

    public ASTNode          getSubject()      { return subject;             }
    public List<CaseClause> getCases()        { return cases;               }
    public List<ASTNode>    getDefaultBlock() { return defaultBlock;        }
    public boolean          hasDefault()      { return defaultBlock != null; }
}
