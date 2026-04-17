package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

/**
 * LiteralNode — Represents a constant value written directly in source code.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * A literal is the simplest possible expression — it evaluates to itself.
 * LEXOR has four literal forms corresponding to its four data types:
 *
 *   INT    → integer number         e.g.  4  100  -1  (negative handled by UnaryExprNode)
 *   FLOAT  → decimal number         e.g.  3.14  0.5
 *   CHAR   → single character       e.g.  'n'  'c'    (stored without quotes)
 *   BOOL   → boolean word           e.g.  "TRUE"  "FALSE"  (stored without quotes)
 *
 * The 'value' field is typed as Object to hold any of the four Java types
 * (Integer, Float, Character, Boolean) with one class. The Interpreter casts
 * it to the correct Java type based on 'typeName'.
 *
 * WHY Object AND NOT GENERICS?
 * ----------------------------
 * Making LiteralNode generic (LiteralNode<T>) would require every collection that
 * holds ASTNode to use wildcards (List<ASTNode<?>>), making the rest of the code
 * much noisier. The single Object field with a typeName string is a simpler trade-off
 * for a student project of this scope.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • ASTVisitor<T> — accept() calls visitor.visitLiteral(this).
 *   The Interpreter wraps the value in a LexorValue of the appropriate type.
 *   The SemanticAnalyzer reads typeName to determine what type this literal contributes
 *   to an expression.
 */
public class LiteralNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: private final Object value;
    //          Holds the already-parsed Java value:
    //            INT   → Integer (parsed with Integer.parseInt() in the Parser)
    //            FLOAT → Float   (parsed with Float.parseFloat() in the Parser)
    //            CHAR  → Character
    //            BOOL  → Boolean (parsed from "TRUE"/"FALSE" string in the Parser)
    //
    // TODO 1b: private final String typeName;
    //          One of "INT", "FLOAT", "CHAR", "BOOL".
    //          The Interpreter uses this to create the correct LexorValue subtype.
    //
    private final Object value;
    private final String typeName;

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // TODO 2a: public LiteralNode(int line, Object value, String typeName)
    // TODO 2b: super(line), assign fields.
    //
    public LiteralNode(int line, Object value, String typeName) {
        super(line);
        this.value    = value;
        this.typeName = typeName;
    }

    // =========================================================================
    // TODO STEP 3 — accept()
    // =========================================================================
    // TODO 3a: return visitor.visitLiteral(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLiteral(this);
    }

    // =========================================================================
    // TODO STEP 4 — GETTERS
    // =========================================================================
    // TODO 4a: public Object getValue()    — returns the raw Java value
    // TODO 4b: public String getTypeName() — returns "INT", "FLOAT", "CHAR", or "BOOL"
    //
    public Object getValue()    { return value;    }
    public String getTypeName() { return typeName; }
}
