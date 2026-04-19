package com.lexor.interpreter;

// =============================================================================
// FILE: Environment.java
// PACKAGE: com.lexor.interpreter
// =============================================================================
//
// PURPOSE:
//   Environment is the Interpreter's runtime variable store. It maps variable
//   names to their current LexorValue at any point during execution. It is the
//   runtime counterpart to the semantic-phase SymbolTable.
//
//   Environment supports NESTED SCOPES via a parent reference. When the
//   Interpreter enters a control-flow block (IF branch, FOR body, REPEAT body),
//   it creates a child Environment linked to the current one. When the block
//   exits, the child is discarded and the parent is active again. This means:
//     - Variables defined in outer scope are visible inside inner blocks.
//     - Variables defined in inner blocks (if LEXOR allows them) disappear
//       when the block ends.
//
// SCOPE CHAIN VISUAL:
//
//   Global Environment
//   ┌─────────────────────────┐
//   │  x → LexorValue(INT,5)  │  ← declared at SCRIPT level
//   │  i → LexorValue(INT,0)  │
//   └──────────┬──────────────┘
//              │ parent
//   FOR Body Child Scope
//   ┌─────────────────────────┐
//   │  (no new declarations)  │  ← get("x") walks up to parent and finds it
//   └─────────────────────────┘
//
// LEXOR SCOPING NOTE:
//   In LEXOR v1, DECLARE statements only appear at the top of SCRIPT AREA
//   (global scope). Block-level declarations inside FOR/IF/REPEAT are not
//   part of the language. However, the scoped Environment is still implemented
//   now so future language versions or extensions can support it without
//   a redesign. For now, get() and assign() on inner scopes will always
//   walk up to the global scope.
//
// =============================================================================

// TODO: Import java.util.HashMap, java.util.Map
// TODO: Import com.lexor.error.LexorRuntimeException

// TODO: public class Environment { ... }

// -----------------------------------------------------------------------------
// FIELDS TO DECLARE:
// -----------------------------------------------------------------------------
//
// TODO: private final Map<String, LexorValue> store;
//       - The variable storage for THIS scope level only.
//       - Use HashMap for O(1) get/put.
//
// TODO: private final Environment parent;
//       - Reference to the enclosing scope. Null for the global scope.
//       - Used by get() and assign() to walk up the scope chain.

// -----------------------------------------------------------------------------
// CONSTRUCTORS:
// -----------------------------------------------------------------------------
//
// TODO: Global scope constructor (no parent):
//
//         public Environment() {
//             this.store  = new HashMap<>();
//             this.parent = null;
//         }
//
// TODO: Child scope constructor (receives parent):
//
//         public Environment(Environment parent) {
//             this.store  = new HashMap<>();
//             this.parent = parent;
//         }
//
//   The Interpreter should call createChildScope() rather than this constructor
//   directly, to keep scope creation self-contained (see factory method below).

// =============================================================================
// METHODS TO IMPLEMENT:
// =============================================================================

// TODO: public void define(String name, LexorValue value)
//
//   Declares a NEW variable in THIS (current) scope level only.
//   Used by Interpreter.visitDeclaration() to allocate each DECLARE statement.
//
//   Implementation:
//     store.put(name, value);
//
//   NOTE: Do NOT walk up to the parent here. define() always writes to the
//   current scope. If a variable with the same name already exists in the
//   parent scope (which SemanticAnalyzer should have prevented), this creates
//   a shadow variable — acceptable behaviour for scoped languages.
//
//   SemanticAnalyzer already prevents duplicate declarations, so no need to
//   guard against them here (but you may add an assertion if you want).

// TODO: public LexorValue get(String name)
//
//   Retrieves the current value of a variable, walking up the scope chain.
//   Used by Interpreter.visitVariable() in every expression evaluation.
//
//   Implementation:
//     Step 1 — Check the current scope: if (store.containsKey(name)) return store.get(name);
//     Step 2 — Walk to parent: if (parent != null) return parent.get(name);
//     Step 3 — Not found: throw new LexorRuntimeException(
//                             "Undefined variable '" + name + "'", 0, 0);
//
//   The chain walk ensures that a variable declared in the global scope is
//   accessible from any nested block, no matter how deep.

// TODO: public void assign(String name, LexorValue value)
//
//   Updates the value of an already-declared variable, walking up the scope
//   chain to find where it was defined. Used by Interpreter.visitAssignment().
//
//   Implementation:
//     Step 1 — Is it in the current scope?
//                 if (store.containsKey(name)) { store.put(name, value); return; }
//     Step 2 — Try the parent scope:
//                 if (parent != null) { parent.assign(name, value); return; }
//     Step 3 — Not found anywhere: throw new LexorRuntimeException(
//                                      "Undefined variable '" + name + "'", 0, 0);
//
//   WHY SEPARATE define() and assign()?
//   - define() CREATES a new slot in the current scope (for DECLARE).
//   - assign() UPDATES an existing slot wherever it lives in the chain (for =).
//   If you used only one method (like always calling put()), a FOR loop body
//   that reassigns "i" would create a new "i" in the loop scope instead of
//   updating the outer "i". That would break loop counters.

// TODO: public Environment createChildScope()
//
//   Factory method — creates and returns a new child Environment linked to
//   this one as its parent. The child is empty; it inherits from the parent
//   via get() and assign() scope chain walking.
//
//   Implementation:
//     return new Environment(this);
//
//   Called by Interpreter when entering IF/FOR/REPEAT blocks:
//     Environment saved  = this.environment;
//     this.environment   = this.environment.createChildScope();
//     // ... execute block statements ...
//     this.environment   = saved;   // restore after block exits

// =============================================================================
// DEBUGGING HELPER (OPTIONAL BUT USEFUL)
// =============================================================================

// TODO: public String dump()
//
//   Returns a formatted string showing all variable names and values in the
//   current scope (not parent scopes). Useful during testing and debugging.
//
//   Implementation:
//     StringBuilder sb = new StringBuilder("Environment{\n");
//     for (Map.Entry<String, LexorValue> e : store.entrySet()) {
//         sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
//     }
//     sb.append("}");
//     return sb.toString();
//
//   Call this in Interpreter with log.debug(environment.dump()) after each
//   statement to trace runtime state during development.

// =============================================================================
// REPL PERSISTENCE NOTE:
// =============================================================================
//
//   In REPL mode (ReplRunner), the Interpreter's global Environment should
//   persist across input lines so that a variable declared in one REPL
//   entry is still available in the next.
//
//   To achieve this, ReplRunner should hold one long-lived Interpreter instance
//   and reuse it across iterations, rather than creating a new Interpreter per
//   input. Alternatively, ReplRunner can serialize and restore the Environment
//   before each run. The simpler approach (single Interpreter instance) is
//   recommended.
//
// =============================================================================
