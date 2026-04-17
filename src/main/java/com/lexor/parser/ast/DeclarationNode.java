package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

/**
 * DeclarationNode — Represents a single variable declaration statement.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * In LEXOR, every variable must be declared before use. A DECLARE statement
 * introduces one or more variables of a given type and optionally gives them
 * an initial value. The Parser creates one DeclarationNode per variable name
 * found on a DECLARE line.
 *
 * Examples from the spec:
 *   DECLARE INT x, y, z=5
 *     -> three DeclarationNodes: (INT, x, null), (INT, y, null), (INT, z, LiteralNode(5))
 *
 *   DECLARE CHAR a_1='n'
 *     -> one DeclarationNode: (CHAR, a_1, LiteralNode('n'))
 *
 *   DECLARE BOOL t="TRUE"
 *     -> one DeclarationNode: (BOOL, t, LiteralNode(TRUE))
 *
 * FIELDS
 * ------
 *   typeName    — the declared data type as a String: "INT", "FLOAT", "CHAR", or "BOOL"
 *   name        — the variable's identifier (case-sensitive)
 *   initializer — an optional ASTNode representing the initial value expression;
 *                 null if no initializer was provided (e.g., DECLARE INT x)
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • ASTVisitor<T> — accept() calls visitor.visitDeclaration(this).
 *   The SemanticAnalyzer's visitDeclaration registers the variable in the SymbolTable.
 *   The Interpreter's visitDeclaration allocates storage in the Environment and
 *   optionally evaluates the initializer.
 */
public class DeclarationNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // TODO 1a: Declare  private final String typeName;
    //          Stores "INT", "FLOAT", "CHAR", or "BOOL" — the Interpreter
    //          uses this string to create a correctly typed LexorValue.
    //
    // TODO 1b: Declare  private final String name;
    //          The variable name. Case-sensitive. Must match identifier rules.
    //
    // TODO 1c: Declare  private final ASTNode initializer;
    //          The expression to evaluate and store at declaration time.
    //          This is null when no '=' initializer appears in the source.
    //
    private final String  typeName;
    private final String  name;
    private final ASTNode initializer;  // nullable

    // =========================================================================
    // TODO STEP 2 — WRITE THE CONSTRUCTOR
    // =========================================================================
    // The Parser calls this once per variable name on a DECLARE line.
    //
    // TODO 2a: Constructor signature:
    //          public DeclarationNode(int line, String typeName, String name, ASTNode initializer)
    // TODO 2b: Call super(line).
    // TODO 2c: Assign all three fields.
    //
    public DeclarationNode(int line, String typeName, String name, ASTNode initializer) {
        super(line);
        this.typeName    = typeName;
        this.name        = name;
        this.initializer = initializer;
    }

    // =========================================================================
    // TODO STEP 3 — IMPLEMENT accept()
    // =========================================================================
    // TODO 3a: return visitor.visitDeclaration(this);
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitDeclaration(this);
    }

    // =========================================================================
    // TODO STEP 4 — ADD GETTERS
    // =========================================================================
    // The SemanticAnalyzer needs typeName and name to register the variable.
    // The Interpreter needs all three to allocate storage and set the initial value.
    //
    // TODO 4a: public String  getTypeName()    — returns typeName
    // TODO 4b: public String  getName()        — returns name
    // TODO 4c: public ASTNode getInitializer() — returns initializer (may be null)
    //
    // TODO 4d: Add a convenience method  public boolean hasInitializer()
    //          that returns  initializer != null  so callers don't null-check everywhere.
    //
    public String  getTypeName()    { return typeName;    }
    public String  getName()        { return name;        }
    public ASTNode getInitializer() { return initializer; }

    public boolean hasInitializer() { return initializer != null; }
}
