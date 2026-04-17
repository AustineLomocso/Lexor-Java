package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

import java.util.List;

/**
 * ProgramNode — The root node of the entire AST. Represents a complete LEXOR program.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * Every LEXOR program has the structure:
 *
 *   SCRIPT AREA
 *   START SCRIPT
 *     <variable declarations>   ← must come first, before any executable code
 *     <statements>              ← executable code: assignments, PRINT, IF, FOR, etc.
 *   END SCRIPT
 *
 * ProgramNode mirrors this structure exactly by holding two separate lists:
 *   1. declarations — all DECLARE statements (processed first during interpretation)
 *   2. statements   — all executable statements (processed after declarations)
 *
 * WHY SEPARATE LISTS?
 * -------------------
 * The LEXOR spec explicitly states: "all variable declarations follow right after
 * START SCRIPT and cannot be placed anywhere else." By keeping declarations separate,
 * the Semantic Analyzer can validate all declarations in one pass before checking
 * any statement — matching the language's own scoping rules.
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • java.util.List — standard Java. The two lists are passed in from the Parser
 *   (already built as ArrayList<>) and stored as the interface type List<> for
 *   flexibility. No mutation after construction — the lists are read-only once stored.
 * • ASTVisitor<T>  — the visitor interface. accept() delegates to visitProgram().
 */
public class ProgramNode extends ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE THE TWO LIST FIELDS
    // =========================================================================
    // Both lists are final: the Parser builds them and hands them to this node;
    // no one should swap them out afterwards.
    //
    // TODO 1a: Declare  private final List<DeclarationNode> declarations;
    // TODO 1b: Declare  private final List<ASTNode> statements;
    //
    private final List<DeclarationNode> declarations;
    private final List<ASTNode>         statements;

    // =========================================================================
    // TODO STEP 2 — WRITE THE CONSTRUCTOR
    // =========================================================================
    // The Parser creates ProgramNode once — after it has collected all declarations
    // and all statements — passing both lists in here.
    //
    // TODO 2a: Accept (int line, List<DeclarationNode> declarations, List<ASTNode> statements).
    // TODO 2b: Call super(line).
    // TODO 2c: Assign both lists to their fields.
    //
    public ProgramNode(int line,
                       List<DeclarationNode> declarations,
                       List<ASTNode> statements) {
        super(line);
        this.declarations = declarations;
        this.statements   = statements;
    }

    // =========================================================================
    // TODO STEP 3 — IMPLEMENT accept()
    // =========================================================================
    // Delegates to visitor.visitProgram(this).
    // The Interpreter's visitProgram() will iterate declarations first,
    // then iterate statements.
    //
    // TODO 3a: Override accept() to return visitor.visitProgram(this).
    //
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitProgram(this);
    }

    // =========================================================================
    // TODO STEP 4 — ADD GETTERS
    // =========================================================================
    // The Interpreter and SemanticAnalyzer both need to iterate the two lists.
    // Return unmodifiable views so callers cannot mutate the node's internal state.
    //
    // TODO 4a: public List<DeclarationNode> getDeclarations() — return declarations
    // TODO 4b: public List<ASTNode>         getStatements()   — return statements
    //
    public List<DeclarationNode> getDeclarations() { return declarations; }
    public List<ASTNode>         getStatements()   { return statements;   }
}
