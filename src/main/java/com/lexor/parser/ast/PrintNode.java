package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * PrintNode — Represents a PRINT output statement.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * The PRINT statement is LEXOR's only way to write to standard output.
 * Its syntax is:
 *   PRINT: <segment> & <segment> & <segment> ...
 *
 * Each segment separated by '&' is an independent expression that gets
 * evaluated and converted to a string, then all results are concatenated
 * and written to stdout as a single line (unless '$' segments insert newlines).
 *
 * Special segment rules (from the spec):
 *   x           → evaluate variable x, stringify it
 *   "hello"     → string literal — output "hello" verbatim
 *   $           → newline / carriage return
 *   [#]         → escape code — the character inside brackets is output literally
 *                 e.g. [[] outputs '[',  []] outputs ']',  [#] outputs '#'
 *
 * The Parser builds a List<ASTNode> where each element is one segment
 * (a VariableNode, LiteralNode, or a special marker node). The Interpreter's
 * visitPrint() iterates this list, evaluates each, and concatenates the results.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • java.util.List — holds the evaluated segments.
 * • ASTVisitor<T>  — accept() calls visitor.visitPrint(this).
 */
public class PrintNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: Declare  private final List<ASTNode> segments;
    //          Each element is one '&'-separated piece of the PRINT output.
    //          The Interpreter evaluates them left-to-right.
    //
    private final List<ASTNode> segments;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public PrintNode(int line, List<ASTNode> segments)
    // TODO 2b: super(line), assign field.
    //
    public PrintNode(int line, List<ASTNode> segments) {
        super(line);
        this.segments = segments;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitPrint(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPrint(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTER
    // =========================================================================
    // TODO 4a: public List<ASTNode> getSegments() — returns segments
    //
    public List<ASTNode> getSegments() { return segments; }
}
