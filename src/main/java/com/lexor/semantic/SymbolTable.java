package com.lexor.semantic;

// =============================================================================
// FILE: SymbolTable.java
// PACKAGE: com.lexor.semantic
// =============================================================================
//
// PURPOSE:
//   The SymbolTable is a compile-time registry that maps every variable name
//   to its type and declaration line. It is used exclusively during the Semantic
//   Analysis phase — NOT at runtime. The Interpreter uses Environment instead.
//
//   Think of SymbolTable as the SemanticAnalyzer's notepad:
//   "I have seen a variable named X, it was declared as INT on line 3."
//
// DIFFERENCE FROM Environment:
//   SymbolTable       → stores TYPES (used during semantic checking)
//   Environment       → stores VALUES (used during actual execution)
//   Both map variable names, but serve entirely different purposes.
//
// INNER CLASS — TypeInfo:
//   Each SymbolTable entry is a TypeInfo object wrapping:
//     - String type:  the declared type ("INT", "FLOAT", "CHAR", "BOOL")
//     - int line:     the source line of the declaration (for error messages)
//
//   Declare it as a public static inner class inside SymbolTable:
//
//     public static class TypeInfo {
//         private final String type;
//         private final int line;
//         public TypeInfo(String type, int line) { ... }
//         public String getType() { return type; }
//         public int    getLine() { return line; }
//     }
//
// FIELDS:
//
//   private Map<String, TypeInfo> table
//     - The actual storage: variable name → TypeInfo.
//     - Use HashMap for O(1) lookup.
//     - In LEXOR, all variables are declared at global scope, so one flat map suffices.
//
// =============================================================================

// TODO: Import java.util.HashMap, java.util.Map
// TODO: Import SemanticException from com.lexor.error

// TODO: public class SymbolTable { ... }

// TODO: Declare static inner class TypeInfo (see above).

// TODO: private final Map<String, TypeInfo> table = new HashMap<>();

// =============================================================================
// METHODS TO IMPLEMENT:
// =============================================================================

// TODO: public void declare(String name, String type, int line)
//
//   Registers a new variable in the symbol table.
//
//   Implementation:
//     Step 1 — Check for duplicate:
//                 if (table.containsKey(name))
//                     throw new SemanticException(
//                         "Variable '" + name + "' already declared", line, 0);
//     Step 2 — Store the entry:
//                 table.put(name, new TypeInfo(type, line));
//
//   This is called by SemanticAnalyzer.visitDeclaration() for each DECLARE statement.

// TODO: public TypeInfo lookup(String name)
//
//   Returns the TypeInfo for a declared variable, or throws if not found.
//
//   Implementation:
//     TypeInfo info = table.get(name);
//     if (info == null)
//         throw new SemanticException("Undeclared variable '" + name + "'", 0, 0);
//     return info;
//
//   Called by SemanticAnalyzer when visiting assignments, expressions, and
//   control-flow conditions that reference a variable.
//
//   NOTE: The line=0 in the thrown exception is a placeholder. For more accurate
//   error reporting, SemanticAnalyzer can catch this and rethrow with the node's
//   line number attached.

// TODO: public boolean isDeclared(String name)
//
//   Returns true if the variable has been registered, false otherwise.
//   Does NOT throw — this is for conditional checks without exception flow.
//
//   Implementation:
//     return table.containsKey(name);
//
//   Useful for pre-checking before calling declare() to avoid duplicate exceptions
//   in certain parser recovery scenarios.

// TODO: public void clear()
//
//   Resets the symbol table to empty.
//   Call this in the REPL between sessions if variables should not persist
//   across inputs (depends on REPL design — see ReplRunner for notes).
//
//   Implementation:
//     table.clear();

// =============================================================================
// USAGE EXAMPLE (inside SemanticAnalyzer):
// =============================================================================
//
//   @Override
//   public Void visitDeclaration(DeclarationNode n) {
//       symbolTable.declare(n.getName(), n.getType(), n.getLine());  // registers
//       if (n.getInitializer() != null) {
//           String initType = (String) n.getInitializer().accept(this);
//           if (!initType.equals(n.getType()))
//               throw new SemanticException("Type mismatch in declaration", n.getLine(), 0);
//       }
//       return null;
//   }
//
//   @Override
//   public Void visitVariable(VariableNode n) {
//       symbolTable.lookup(n.getName());    // throws if undeclared
//       return null;
//   }
//
// =============================================================================
