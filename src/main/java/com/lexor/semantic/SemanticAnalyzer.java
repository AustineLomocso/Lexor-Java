package com.lexor.semantic;

// =============================================================================
// FILE: SemanticAnalyzer.java
// PACKAGE: com.lexor.semantic
// =============================================================================
//
// PURPOSE:
//   The SemanticAnalyzer performs a single pre-execution pass over the entire
//   AST to catch errors that the Parser cannot detect (because the Parser only
//   checks SYNTAX, not MEANING). It validates:
//
//     1. No variable is used before it is declared.
//     2. No variable is declared more than once.
//     3. Assignment types match the variable's declared type (strong typing).
//     4. All operands in expressions have compatible types for their operators.
//     5. Control-flow conditions (IF, FOR, REPEAT WHEN) evaluate to BOOL.
//     6. SCAN targets are declared variables.
//
//   If any check fails, SemanticException is thrown with line number info.
//   If the entire AST passes, the Interpreter is safe to execute it.
//
// IMPLEMENTS: ASTVisitor<Void>
//   SemanticAnalyzer uses Void (capital-V, the java.lang boxed type) as T.
//   All visit methods return null — the analyzer's job is validation, not
//   value production. However, visitBinaryExpr(), visitLiteral(), etc. return
//   the INFERRED TYPE STRING ("INT", "BOOL", etc.) as a Void-cast.
//
//   *** IMPORTANT TYPE-RETURN TRICK ***
//   Because Void is the return type but we need to propagate type strings up
//   the expression tree, you have two options:
//
//   OPTION A (simpler): Change T to String for SemanticAnalyzer specifically.
//     SemanticAnalyzer implements ASTVisitor<String>
//     - visitDeclaration() returns null (or "VOID")
//     - visitBinaryExpr() returns "INT", "BOOL", etc.
//     - visitProgram() returns null
//     This avoids casting and is easier to read.
//
//   OPTION B (strict Void): Keep ASTVisitor<Void> and use a separate field
//     private String lastType; that is set before returning null from expression visitors.
//     Parent nodes read lastType after calling child.accept(this).
//
//   RECOMMENDATION: Use OPTION A. It is cleaner and more intuitive.
//
// =============================================================================

// TODO: Import com.lexor.visitor.ASTVisitor
// TODO: Import all AST node classes: com.lexor.parser.ast.*
// TODO: Import SemanticException from com.lexor.error
// TODO: Import org.slf4j.Logger, org.slf4j.LoggerFactory

// TODO: Declare the class implementing ASTVisitor<String> (or Void, see note above):
//
//         public class SemanticAnalyzer implements ASTVisitor<String> { ... }

// -----------------------------------------------------------------------------
// FIELDS TO DECLARE:
// -----------------------------------------------------------------------------
//
// TODO: private static final Logger log = LoggerFactory.getLogger(SemanticAnalyzer.class);
//
// TODO: private final SymbolTable symbolTable;
//       - Injected or created in the constructor.
//       - Stores every declared variable and its type.

// -----------------------------------------------------------------------------
// CONSTRUCTOR:
// -----------------------------------------------------------------------------
//
// TODO:
//         public SemanticAnalyzer() {
//             this.symbolTable = new SymbolTable();
//         }
//
//       OR accept a pre-built SymbolTable for testability:
//
//         public SemanticAnalyzer(SymbolTable symbolTable) {
//             this.symbolTable = symbolTable;
//         }

// =============================================================================
// PUBLIC ENTRY POINT
// =============================================================================

// TODO: public void analyze(ProgramNode program)
//
//   Called by Main.java after the Parser produces a ProgramNode.
//   Kicks off the full tree walk.
//
//   Implementation:
//     log.debug("Starting semantic analysis");
//     program.accept(this);
//     log.debug("Semantic analysis passed — no errors found");
//
//   Any SemanticException thrown during traversal propagates out to Main.java,
//   which catches it and prints the error message to stderr.

// =============================================================================
// VISITOR METHOD IMPLEMENTATIONS
// =============================================================================

// TODO: @Override public String visitProgram(ProgramNode n)
//
//   Visits all declarations first (so all names are in SymbolTable before
//   any statement tries to reference them), then visits all statements.
//
//   Implementation:
//     for (DeclarationNode d : n.getDeclarations()) d.accept(this);
//     for (ASTNode s : n.getStatements())           s.accept(this);
//     return null;

// TODO: @Override public String visitDeclaration(DeclarationNode n)
//
//   Registers the variable; validates the initializer type if present.
//
//   Implementation:
//     Step 1 — Register: symbolTable.declare(n.getName(), n.getType(), n.getLine());
//               (declare() throws SemanticException on duplicate — let it propagate)
//     Step 2 — If n.getInitializer() != null:
//                 String initType = n.getInitializer().accept(this);
//                 checkTypeCompatibility(n.getType(), initType, n.getLine());
//     Step 3 — return null;

// TODO: @Override public String visitAssignment(AssignmentNode n)
//
//   Verifies each target is declared and the value type matches.
//
//   Implementation:
//     Step 1 — Evaluate value type: String valType = n.getValue().accept(this);
//     Step 2 — For each name in n.getTargets():
//                 SymbolTable.TypeInfo info = symbolTable.lookup(name);  // throws if undeclared
//                 checkTypeCompatibility(info.getType(), valType, n.getLine());
//     Step 3 — return null;

// TODO: @Override public String visitPrint(PrintNode n)
//
//   Validates each segment expression. PRINT accepts any type (all print as string).
//
//   Implementation:
//     for (ASTNode seg : n.getSegments()) seg.accept(this);
//     return null;

// TODO: @Override public String visitScan(ScanNode n)
//
//   Verifies each target variable is declared.
//
//   Implementation:
//     for (String name : n.getVariables()) {
//         if (!symbolTable.isDeclared(name))
//             throw new SemanticException("Undeclared variable in SCAN: " + name, n.getLine(), 0);
//     }
//     return null;

// TODO: @Override public String visitBinaryExpr(BinaryExprNode n)
//
//   Type-checks both operands, validates operator compatibility, returns result type.
//
//   Implementation outline (see BinaryExprNode for full detail):
//     String leftType  = n.getLeft().accept(this);
//     String rightType = n.getRight().accept(this);
//
//     switch on n.getOperator():
//       "+", "-", "*", "/", "%" → both must be INT or FLOAT → return wider type
//       "<", ">", "<=", ">="   → both must be INT or FLOAT → return "BOOL"
//       "==", "<>"              → both must be same type    → return "BOOL"
//       "AND", "OR"             → both must be "BOOL"       → return "BOOL"
//
//     On any mismatch: throw SemanticException("Type mismatch for operator " + op, n.getLine(), 0)

// TODO: @Override public String visitUnaryExpr(UnaryExprNode n)
//
//   Validates operand type for the unary operator.
//
//   Implementation:
//     String type = n.getOperand().accept(this);
//     if ("-".equals(n.getOperator()) && !type.equals("INT") && !type.equals("FLOAT"))
//         throw SemanticException("Unary '-' requires INT or FLOAT", n.getLine(), 0);
//     if ("NOT".equals(n.getOperator()) && !type.equals("BOOL"))
//         throw SemanticException("NOT requires BOOL operand", n.getLine(), 0);
//     return type;

// TODO: @Override public String visitLiteral(LiteralNode n)
//
//   Literals are always valid. Just return the literal's type string.
//
//   Implementation:
//     return n.getType();

// TODO: @Override public String visitVariable(VariableNode n)
//
//   Looks up the variable in the SymbolTable to confirm it's declared.
//   Returns its declared type for parent expression type-checking.
//
//   Implementation:
//     SymbolTable.TypeInfo info = symbolTable.lookup(n.getName());  // throws if undeclared
//     return info.getType();

// TODO: @Override public String visitIf(IfNode n)
//
//   Validates condition is BOOL, then validates all branches recursively.
//
//   Implementation:
//     String condType = n.getCondition().accept(this);
//     if (!"BOOL".equals(condType))
//         throw SemanticException("IF condition must be BOOL", n.getLine(), 0);
//     for (ASTNode stmt : n.getThenBlock()) stmt.accept(this);
//     for (IfNode.ElseIfClause clause : n.getElseIfClauses()) {
//         String clauseType = clause.getCondition().accept(this);
//         if (!"BOOL".equals(clauseType)) throw SemanticException(...)
//         for (ASTNode stmt : clause.getBody()) stmt.accept(this);
//     }
//     if (n.getElseBlock() != null)
//         for (ASTNode stmt : n.getElseBlock()) stmt.accept(this);
//     return null;

// TODO: @Override public String visitFor(ForNode n)
//
//   Validates init, condition (must be BOOL), update, and body.
//
//   Implementation:
//     n.getInit().accept(this);
//     String condType = n.getCondition().accept(this);
//     if (!"BOOL".equals(condType)) throw SemanticException("FOR condition must be BOOL", ...)
//     n.getUpdate().accept(this);
//     for (ASTNode stmt : n.getBody()) stmt.accept(this);
//     return null;

// TODO: @Override public String visitRepeat(RepeatNode n)
//
//   Validates body first (body runs before condition at runtime), then condition.
//
//   Implementation:
//     for (ASTNode stmt : n.getBody()) stmt.accept(this);
//     String condType = n.getCondition().accept(this);
//     if (!"BOOL".equals(condType)) throw SemanticException("REPEAT WHEN condition must be BOOL", ...)
//     return null;

// =============================================================================
// PRIVATE HELPER
// =============================================================================

// TODO: private void checkTypeCompatibility(String expected, String actual, int line)
//
//   Centralized type mismatch check. Throws if types don't match.
//
//   Implementation:
//     Exact match: if (expected.equals(actual)) return;
//     Widening:    INT → FLOAT is allowed (for initializers and assignments if spec permits).
//                  if ("FLOAT".equals(expected) && "INT".equals(actual)) return;
//     Otherwise:   throw new SemanticException(
//                      "Type mismatch: expected " + expected + " but got " + actual, line, 0);
//
//   STRICT MODE NOTE: LEXOR is strongly typed. Do NOT silently coerce unless the
//   language specification explicitly allows it. Check the spec carefully.
//   If INT → FLOAT coercion is not allowed, remove that widening case.
//
// =============================================================================
