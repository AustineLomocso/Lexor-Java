package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

/**
 * ASTNode — Abstract base class for every node in the Abstract Syntax Tree.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * After the Lexer produces a flat list of Tokens, the Parser's job is to give
 * that list *structure* — to turn a sequence of words into a tree that reflects
 * the grammar of the program. Every node in that tree is an ASTNode subclass.
 *
 * The tree mirrors the logical nesting of the program:
 *   ProgramNode
 *     DeclarationNode  (x : INT)
 *     PrintNode
 *       BinaryExprNode (+)
 *         VariableNode (x)
 *         LiteralNode  (5)
 *
 * The Semantic Analyzer and Interpreter both walk this tree — they never look
 * at tokens again after the Parser finishes.
 *
 * WHY ABSTRACT?
 * -------------
 * ASTNode itself holds only the data that every node shares (the source line
 * number for error reporting). Concrete behaviour is in subclasses. Making this
 * class abstract prevents anyone from instantiating a meaningless "bare" ASTNode.
 *
 * THE VISITOR HOOK — accept()
 * ---------------------------
 * Every subclass must implement accept(ASTVisitor<T> visitor). This is the
 * Visitor design pattern. It lets SemanticAnalyzer and Interpreter define their
 * own logic for each node type in ONE place (their own class) rather than
 * scattering logic across all the node classes. Each node's accept() simply
 * calls back the correct visit method on the visitor:
 *
 *   // In IfNode:
 *   public <T> T accept(ASTVisitor<T> visitor) { return visitor.visitIf(this); }
 *
 * The generic <T> means:
 *   • SemanticAnalyzer uses  ASTVisitor<Void>    (it checks but doesn't return values)
 *   • Interpreter uses       ASTVisitor<LexorValue> (it evaluates and returns values)
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 * • ASTVisitor<T> (com.lexor.visitor) — the visitor interface that both the
 *   SemanticAnalyzer and Interpreter implement. Imported here so that subclasses
 *   can reference it in their accept() signatures without extra imports.
 *   No external library dependency — this is a project-internal interface.
 */
public abstract class ASTNode {

    // =========================================================================
    // TODO STEP 1 — DECLARE THE line FIELD
    // =========================================================================
    // Every node records the line number from the source file where it originated.
    // This is essential for producing helpful error messages:
    //   "SemanticError at line 7: variable 'x' not declared"
    //
    // The field is:
    //   • protected — subclasses (and their constructors) need direct access to it
    //   • final     — a node's source line never changes once it is created
    //
    // TODO 1a: Declare  protected final int line;
    //
    protected final int line;

    // =========================================================================
    // TODO STEP 2 — WRITE THE PROTECTED CONSTRUCTOR
    // =========================================================================
    // The constructor takes the line number and stores it.
    // It is protected so that only subclasses (and same-package code) can call it
    // via super(line) — external code cannot instantiate ASTNode directly.
    //
    // TODO 2a: Write  protected ASTNode(int line) { this.line = line; }
    //
    protected ASTNode(int line) {
        this.line = line;
    }

    // =========================================================================
    // TODO STEP 3 — DECLARE THE ABSTRACT accept() METHOD
    // =========================================================================
    // This is the core of the Visitor pattern. Every concrete subclass MUST
    // override this method and call the matching visitXxx() method on the visitor.
    //
    // The generic <T> makes accept polymorphic over the visitor's return type,
    // so the same accept() signature works for both:
    //   SemanticAnalyzer (Void return)  and  Interpreter (LexorValue return).
    //
    // Subclass implementation template:
    //   @Override
    //   public <T> T accept(ASTVisitor<T> visitor) {
    //       return visitor.visitXxx(this);   // replace Xxx with the node's name
    //   }
    //
    // TODO 3a: Declare  public abstract <T> T accept(ASTVisitor<T> visitor);
    //
    public abstract <T> T accept(ASTVisitor<T> visitor);

    // =========================================================================
    // TODO STEP 4 — ADD A getLine() ACCESSOR
    // =========================================================================
    // The field is protected so subclasses can read it directly, but external
    // code (error handlers, test assertions) needs a public getter.
    //
    // TODO 4a: Add  public int getLine() { return line; }
    //
    public int getLine() {
        return line;
    }
}
