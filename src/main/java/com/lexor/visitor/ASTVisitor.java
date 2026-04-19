package com.lexor.visitor;

// =============================================================================
// FILE: ASTVisitor.java
// PACKAGE: com.lexor.visitor
// =============================================================================
//
// PURPOSE:
//   This interface defines the Visitor Design Pattern contract for the entire
//   LEXOR interpreter. Both the SemanticAnalyzer and the Interpreter implement
//   this interface, allowing them to "visit" (process) every type of AST node
//   without the nodes themselves needing to know who is visiting them.
//
// DESIGN PATTERN — VISITOR:
//   The Visitor pattern separates an algorithm (semantic checking, execution)
//   from the object structure it operates on (AST nodes). This means:
//     - Adding a new analysis pass = create a new class implementing ASTVisitor
//     - Adding a new node type    = add one method here + implement in each visitor
//   This prevents bloating node classes with unrelated logic.
//
// GENERIC TYPE PARAMETER <T>:
//   The interface is generic so different visitors can return different types:
//     - SemanticAnalyzer implements ASTVisitor<Void>   → returns nothing (null)
//     - Interpreter      implements ASTVisitor<LexorValue> → returns runtime values
//   This lets the same interface serve both passes cleanly.
//
// HOW accept() CONNECTS TO THIS:
//   Every ASTNode subclass has an accept(ASTVisitor<T> v) method that calls
//   back the appropriate visitXxx() method on the visitor. For example:
//     BinaryExprNode.accept(v)  →  calls v.visitBinaryExpr(this)
//   This "double dispatch" is what makes the Visitor pattern work.
//
// =============================================================================

// TODO: Import all AST node classes from com.lexor.parser.ast.*
//       You will need every concrete node class listed below as a parameter type.

// TODO: Declare the interface with a generic type parameter T:
//
//         public interface ASTVisitor<T> { ... }
//
//       T is the return type of every visit method. Using a generic parameter
//       instead of Object keeps the code type-safe and avoids casting.

// -----------------------------------------------------------------------------
// METHODS TO DECLARE (one per concrete ASTNode subclass):
// -----------------------------------------------------------------------------
//
// Each method follows the naming convention:  T visitXxx(XxxNode n);
// The parameter is always the specific node type — never the base ASTNode.
// The return type is always T (the generic parameter).
//
// TODO: Declare the following visit methods:
//
//   1. T visitProgram(ProgramNode n)
//      - Called when the visitor reaches the root of the AST.
//      - The SemanticAnalyzer uses this to kick off a full tree walk.
//      - The Interpreter uses this to execute every top-level statement.
//
//   2. T visitDeclaration(DeclarationNode n)
//      - Called for each DECLARE statement.
//      - SemanticAnalyzer: register the variable in the SymbolTable,
//        check for duplicate declarations, validate the initializer type.
//      - Interpreter: allocate the variable in the Environment, optionally
//        store the evaluated initializer value.
//
//   3. T visitAssignment(AssignmentNode n)
//      - Called for each assignment (e.g., x = 5, or x = y = 10).
//      - SemanticAnalyzer: verify each target is declared and the value
//        expression's type matches the declared type.
//      - Interpreter: evaluate the right-hand side, then store the result
//        in the Environment for each target variable.
//
//   4. T visitPrint(PrintNode n)
//      - Called for each PRINT: statement.
//      - SemanticAnalyzer: ensure all segment expressions are valid.
//      - Interpreter: evaluate each segment, handle & (concatenation with space),
//        handle $ (newline), handle [X] escape codes, then write to output.
//
//   5. T visitScan(ScanNode n)
//      - Called for each SCAN: statement.
//      - SemanticAnalyzer: verify all listed variable names are declared.
//      - Interpreter: read a line from stdin, split on commas, coerce each
//        token to the target variable's declared type, store in Environment.
//
//   6. T visitBinaryExpr(BinaryExprNode n)
//      - Called for every two-operand expression (+, -, *, /, %, <, >, <=, >=, ==, <>, AND, OR).
//      - SemanticAnalyzer: recursively type-check left and right operands,
//        then validate that the operator is legal for those types
//        (e.g., AND/OR require BOOL operands; arithmetic requires INT or FLOAT).
//      - Interpreter: evaluate left and right sub-expressions, then apply
//        the operator and return a new LexorValue with the result.
//
//   7. T visitUnaryExpr(UnaryExprNode n)
//      - Called for unary minus (-x) and logical NOT (NOT x).
//      - SemanticAnalyzer: verify the operand type matches the operator
//        (NOT requires BOOL; unary minus requires INT or FLOAT).
//      - Interpreter: evaluate the operand and negate or invert it.
//
//   8. T visitLiteral(LiteralNode n)
//      - Called for bare literal values (42, 3.14, 'c', "TRUE", "FALSE").
//      - SemanticAnalyzer: literals are always type-safe; may return the
//        literal's type string for type-checking parent nodes.
//      - Interpreter: wrap the literal's raw value in a LexorValue and return it.
//
//   9. T visitVariable(VariableNode n)
//      - Called when an identifier appears in an expression context.
//      - SemanticAnalyzer: verify the variable has been declared.
//      - Interpreter: look up the variable's current value in the Environment
//        and return it as a LexorValue.
//
//  10. T visitIf(IfNode n)
//      - Called for IF / ELSE IF / ELSE blocks.
//      - SemanticAnalyzer: verify the condition is BOOL, recursively
//        validate all branches.
//      - Interpreter: evaluate the condition; if true, execute the then-block;
//        otherwise try each ELSE IF clause in order; if none match, run else-block.
//
//  11. T visitFor(ForNode n)
//      - Called for FOR loops (init ; condition ; update).
//      - SemanticAnalyzer: check init target is declared and INT, condition
//        is BOOL, update target is declared.
//      - Interpreter: run the init assignment once; while the condition is true,
//        execute the body and then the update assignment; repeat.
//
//  12. T visitRepeat(RepeatNode n)
//      - Called for REPEAT WHEN loops (do-while equivalent).
//      - SemanticAnalyzer: verify the condition expression is BOOL,
//        recursively validate the body.
//      - Interpreter: execute the body once unconditionally, then re-evaluate
//        the condition; if true, repeat; stop when condition is false.
//
// -----------------------------------------------------------------------------
// IMPLEMENTATION NOTES:
// -----------------------------------------------------------------------------
//
// - All methods should be declared without a default body (abstract interface methods).
//   Example syntax:
//       T visitProgram(ProgramNode n);
//
// - Java interfaces have public abstract methods implicitly, so no modifiers needed,
//   but you may add "public" for clarity.
//
// - Do NOT implement any logic inside this interface. It is a pure contract.
//
// - When implementing this interface (in SemanticAnalyzer or Interpreter),
//   IntelliJ will auto-generate stubs for all methods via Alt+Enter →
//   "Implement methods". Use that to avoid typos.
//
// - If you add a new ASTNode subclass later (e.g., FunctionCallNode), you MUST:
//     a) Add T visitFunctionCall(FunctionCallNode n) here.
//     b) Add accept() in FunctionCallNode calling v.visitFunctionCall(this).
//     c) Implement the new method in SemanticAnalyzer AND Interpreter.
//
// =============================================================================
