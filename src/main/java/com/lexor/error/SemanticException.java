package com.lexor.error;

/**
 * SemanticException — Thrown by SemanticAnalyzer when the AST violates a
 * semantic rule (type mismatch, undeclared variable, duplicate declaration, etc.).
 *
 * Carries the source line number so error messages can pinpoint the bad statement.
 *
 * Two construction paths:
 *   new SemanticException(message)            — line defaults to 0 (position unknown)
 *   new SemanticException(message, line, col) — precise position from the AST node
 */
public class SemanticException extends RuntimeException {

    private final int line;
    private final int col;

    /**
     * Convenience constructor used when the exact line is unknown or not yet
     * available (e.g., inside SymbolTable.lookup before the caller can attach
     * the node's line).  getLine() returns 0.
     */
    public SemanticException(String message) {
        super(message);
        this.line = 0;
        this.col  = 0;
    }

    /**
     * Full constructor.  The SemanticAnalyzer (and SymbolTable) should prefer
     * this form so that getLine() returns a useful positive value.
     *
     * @param message human-readable description of the rule that was violated
     * @param line    1-based source line number from the relevant ASTNode
     * @param col     1-based column (pass 0 if column tracking is not implemented)
     */
    public SemanticException(String message, int line, int col) {
        super(message);
        this.line = line;
        this.col  = col;
    }

    /** Returns the 1-based source line of the offending construct, or 0 if unknown. */
    public int getLine() {
        return line;
    }

    /** Returns the 1-based column of the offending construct, or 0 if unknown. */
    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        if (line > 0) {
            return "SemanticException at line " + line
                    + (col > 0 ? ", col " + col : "")
                    + ": " + getMessage();
        }
        return "SemanticException: " + getMessage();
    }
}