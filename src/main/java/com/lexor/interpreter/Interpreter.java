package com.lexor.interpreter;

// =============================================================================
// FILE: Interpreter.java
// PACKAGE: com.lexor.interpreter
// =============================================================================
//
// PURPOSE:
//   The Interpreter is the final stage of the LEXOR pipeline. It receives a
//   semantically validated AST (ProgramNode) and executes it by recursively
//   walking every node. It implements ASTVisitor<LexorValue> so each visit
//   method can return the runtime result of evaluating a node.
//
// EXECUTION MODEL — Tree-Walking Interpretation:
//   There is no compilation to bytecode or machine code. Every time a node is
//   "executed", the Interpreter visits it directly:
//     - Statement nodes (DeclarationNode, PrintNode, etc.) produce side effects
//       (storing values, printing, reading input) and return null.
//     - Expression nodes (BinaryExprNode, LiteralNode, VariableNode, etc.)
//       return a LexorValue representing the evaluated result.
//
// THE evaluate() HELPER:
//   Instead of calling node.accept(this) everywhere, a private evaluate()
//   wrapper is used for expression contexts:
//
//     private LexorValue evaluate(ASTNode node) {
//         return node.accept(this);
//     }
//
//   This makes visitBinaryExpr() read naturally:
//     LexorValue left  = evaluate(node.getLeft());
//     LexorValue right = evaluate(node.getRight());
//
// SCOPE MANAGEMENT:
//   The Interpreter holds a single `environment` field that points to the
//   currently active Environment. When entering a block (IF/FOR/REPEAT), a
//   child scope is created, set as current, the block is executed, and then
//   the parent scope is restored. This ensures variable shadowing and isolation
//   work correctly.
//
// =============================================================================

// TODO: Import com.lexor.visitor.ASTVisitor
// TODO: Import all AST nodes: com.lexor.parser.ast.*
// TODO: Import com.lexor.error.LexorRuntimeException
// TODO: Import java.io.PrintStream
// TODO: Import java.util.Map, java.util.HashMap
// TODO: Import java.util.Scanner
// TODO: Import org.slf4j.Logger, org.slf4j.LoggerFactory

import ch.qos.logback.classic.pattern.LevelConverter;
import com.lexor.error.LexorException;
import com.lexor.error.LexorRuntimeException;
import com.lexor.parser.ast.*;
import com.lexor.visitor.ASTVisitor;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.slf4j.Logger;

// TODO: public class Interpreter implements ASTVisitor<LexorValue> { ... }
public class Interpreter implements ASTVisitor<LexorValue> {


// -----------------------------------------------------------------------------
// CONSTANTS:
// -----------------------------------------------------------------------------
//
// TODO: private static final int MAX_ITERATIONS = 1_000_000;
//       - Safety cap for FOR and REPEAT WHEN loops.
//       - If a loop runs more than this many iterations, throw LexorRuntimeException.
//       - Protects students from accidentally writing infinite loops.
//
// TODO: private static final Map<String, String> ESCAPE_MAP
//       - Precomputed map of escape codes to their replacement strings.
//       - Initialize statically:
//           static {
//               ESCAPE_MAP = new HashMap<>();
//               ESCAPE_MAP.put("[#]", "#");
//               ESCAPE_MAP.put("[[]", "[");
//               ESCAPE_MAP.put("[]]", "]");
//               ESCAPE_MAP.put("[&]", "&");
//               ESCAPE_MAP.put("[$]", "$");
//           }
//       - Add more escape codes here as the LEXOR spec defines them.
    private static final int MAX_ITERATIONS = 1000000;
    private static final Map<String, String> ESCAPE_MAP = new HashMap<>(){
        {
            put("[#]", "#");
            put("[[]n", "[");
            put("[]]n", "]");
            put("[&]n", "&");
            put("[$]n", "$");
        }
    };
// -----------------------------------------------------------------------------
// FIELDS TO DECLARE:
// -----------------------------------------------------------------------------
//
// TODO: private static final Logger log = LoggerFactory.getLogger(Interpreter.class);
//
// TODO: private Environment environment;
//       - The currently active runtime scope. Starts as a global Environment.
//       - Reassigned when entering/exiting block scopes.
//
// TODO: private final PrintStream output;
//       - Where PRINT statements write their output.
//       - Injected via constructor so tests can redirect to a ByteArrayOutputStream.
//       - Default: System.out
//
// TODO: private final Scanner input;
//       - Where SCAN statements read user input from.
//       - Injected via constructor so tests can supply a StringReader.
//       - Default: new Scanner(System.in)
    private static final Logger log = LoggerFactory.getLogger(Interpreter.class);
    private Environment environment;
    private final PrintStream output;
    private final Scanner input;
// -----------------------------------------------------------------------------
// CONSTRUCTORS:
// -----------------------------------------------------------------------------
//
// TODO: Default constructor (for normal use):
//
    public Interpreter() {
        this.environment = new Environment();
        this.output      = System.out;
        this.input       = new Scanner(System.in);
    }
//
// TODO: Testable constructor (inject custom I/O streams):
//
    public Interpreter(PrintStream output, Scanner input) {
        this.environment = new Environment();
        this.output      = output;
        this.input       = input;
    }
//
//   In InterpreterTest.java, create the interpreter with:
//     ByteArrayOutputStream buf = new ByteArrayOutputStream();
//     Interpreter interp = new Interpreter(new PrintStream(buf), new Scanner("5\n"));
//     // then assert buf.toString() equals expected output

// =============================================================================
// PUBLIC ENTRY POINT
// =============================================================================

// TODO: public void interpret(ProgramNode program)
//
//   Called by Main.java after SemanticAnalyzer passes.
//   Simply delegates the tree walk to the visitor mechanism.
//
//   Implementation:
//     log.debug("Starting interpretation");
//     program.accept(this);
//     log.debug("Interpretation complete");
//
//   Any LexorRuntimeException thrown during execution propagates to Main.java,
//   which catches it and prints a user-friendly runtime error message.
    public void interpret (ProgramNode programNode) {
        log.debug("Starting interpretation");
        programNode.accept(this);
        log.debug("Interpretation complete");
    }
// =============================================================================
// VISITOR IMPLEMENTATIONS — STATEMENTS
// =============================================================================

// TODO: @Override public LexorValue visitProgram(ProgramNode n)
//
//   Executes declarations first, then statements, in order.
//
//   Implementation:
//     for (DeclarationNode d : n.getDeclarations()) d.accept(this);
//     for (ASTNode s : n.getStatements())           s.accept(this);
//     return null;
    @Override
    public LexorValue visitProgram(ProgramNode programNode) {
        for(DeclarationNode declarationNode : programNode.getDeclarations()) declarationNode.accept(this);
        for(ASTNode astNode : programNode.getStatements()) astNode.accept(this);
        return null;
    }
// TODO: @Override public LexorValue visitDeclaration(DeclarationNode n)
//
//   Allocates the variable in the Environment with its initial value.
//
//   Implementation:
//     LexorValue initialValue;
//     if (n.getInitializer() != null) {
//         initialValue = evaluate(n.getInitializer());
//     } else {
//         initialValue = defaultValue(n.getType());   // see private helper below
//     }
//     environment.define(n.getName(), initialValue);
//     log.debug("Declared {} = {}", n.getName(), initialValue);
//     return null;
    @Override
    public LexorValue visitDeclaration(DeclarationNode declarationNode) {
        LexorValue initialValue;
        if(declarationNode.getInitializer() != null){
            initialValue = evaluate(declarationNode.getInitializer());
        }else{
            initialValue = defaultValue(declarationNode.getTypeName());
        }
        environment.define(declarationNode.getName(), initialValue);
        log.debug("Declaration '" + declarationNode.getName() + "' initialized");
        return null;
    }
// TODO: @Override public LexorValue visitAssignment(AssignmentNode n)
//
//   Evaluates the right-hand expression and stores it in each target variable.
//
//   Implementation:
//     LexorValue result = evaluate(n.getValue());
//     for (String name : n.getTargets()) {
//         environment.assign(name, result);
//         log.debug("Assigned {} = {}", name, result);
//     }
//     return null;
    @Override
    public LexorValue visitAssignment(AssignmentNode assignmentNode) {
        LexorValue result = evaluate(assignmentNode.getValue());
        for(String name: assignmentNode.getTargets()){
            environment.assign(name, result);
            log.debug("Assigned {} = {}", name, result);
        }
        return null;
    }
// TODO: @Override public LexorValue visitPrint(PrintNode n)
//
//   Evaluates each segment and writes the concatenated result to output.
//   Handles escape code resolution via resolveEscapeCode().
//
//   Implementation:
//     StringBuilder sb = new StringBuilder();
//     for (ASTNode segment : n.getSegments()) {
//         LexorValue val  = evaluate(segment);
//         String     text = val.toString();
//         text = resolveEscapeCode(text);
//         sb.append(text);
//     }
//     output.print(sb.toString());
//     return null;
//
//   NOTE: PRINT does NOT append a newline automatically.
//   The $ token (represented as LiteralNode("\n", ...)) handles line breaks.
//   output.print() is used instead of output.println() for this reason.
    @Override
    public LexorValue visitPrint(PrintNode n){
        StringBuilder sb = new StringBuilder();
        for(ASTNode segment : n.getSegments()){
            LexorValue val = evaluate(segment);
            if(val == null) return null;
            String text = val.toString();
            if (val.getType().equals("BOOL")) {
                text = text.toUpperCase();
            }
            text = resolveEscapeCode(text);
            sb.append(text);
        }
        output.print(sb.toString());
        return null;
    }
// TODO: @Override public LexorValue visitScan(ScanNode n)
//
//   Reads one line from stdin, splits on commas, coerces each part to the
//   correct type, and stores it in the corresponding variable.
//
//   Implementation:
//     Step 1 — Read input: String line = input.nextLine();
//     Step 2 — Split: String[] parts = line.split(",", -1);
//               Trim whitespace: for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
//     Step 3 — Size check:
//               if (parts.length != n.getVariables().size())
//                   throw new LexorRuntimeException(
//                       "SCAN expected " + n.getVariables().size() + " value(s), got " + parts.length, 0, 0);
//     Step 4 — For each (varName, rawPart) pair:
//               LexorValue val = coerce(rawPart, environment.get(varName).getType());
//               environment.assign(varName, val);
//     Step 5 — Return null.
//
//   NOTE: coerce() uses environment.get(varName).getType() to know what type to
//   parse into. This works because DeclarationNode already defined the variable
//   with its default value (so the type is known).
    @Override
    public LexorValue visitScan(ScanNode n) {
        String line = input.nextLine();
        String[] parts = line.split(",", -1);
        for(String part: parts){
            part.trim();
        }
        List<String> variables = n.getVariables();
        if(parts.length != n.getVariables().size()){
            throw new LexorRuntimeException("SCAN expected "+n.getVariables().size()+"value(s), got "+parts.length,0,0);
        }
        for (int i = 0; i < parts.length; i++) {
            String varName = variables.get(i);
            String rawPart = parts[i];
            LexorValue val = coerce(rawPart, environment.get(varName).getType());
            environment.assign(varName, val);
        }
        return null;
    }
// TODO: @Override public LexorValue visitIf(IfNode n)
//
//   Evaluates the condition; executes the first matching branch in a child scope.
//
//   Implementation:
//     LexorValue cond = evaluate(n.getCondition());
//     if (cond.asBool()) {
//         executeBlock(n.getThenBlock());
//         return null;
//     }
//     for (IfNode.ElseIfClause clause : n.getElseIfClauses()) {
//         if (evaluate(clause.getCondition()).asBool()) {
//             executeBlock(clause.getBody());
//             return null;
//         }
//     }
//     if (n.getElseBlock() != null) {
//         executeBlock(n.getElseBlock());
//     }
//     return null;
    @Override
    public LexorValue visitIf(IfNode n) {
        LexorValue cond = evaluate(n.getCondition());
        if(cond == null) return null;
        if (cond.asBool()) {
            executeBlock(n.getThenBlock());
            return null;
        }
        for (IfNode.ElseIfClause clause : n.getElseIfClauses()) {
            if (evaluate(clause.getCondition()).asBool()) {
                executeBlock(clause.getBody());
                return null;
            }
        }
        if (n.getElseBlock() != null) executeBlock(n.getElseBlock());
        return null;
    }
// TODO: @Override public LexorValue visitFor(ForNode n)
//
//   Executes the FOR loop: init once, then body + update while condition holds.
//
//   Implementation:
//     n.getInit().accept(this);              // run init assignment
//     int iterations = 0;
//     while (evaluate(n.getCondition()).asBool()) {
//         if (++iterations > MAX_ITERATIONS)
//             throw new LexorRuntimeException("Possible infinite loop in FOR", n.getLine(), 0);
//         executeBlock(n.getBody());
//         n.getUpdate().accept(this);        // run update assignment
//     }
//     return null;
    @Override
    public LexorValue visitFor(ForNode n){
        n.getInit().accept(this);
        int iterations = 0;
        while(evaluate(n.getCondition()).asBool()){
            if(++iterations > MAX_ITERATIONS){
                throw new LexorRuntimeException("Too many iterations",0,MAX_ITERATIONS);
            }
            executeBlock(n.getBody());
            n.getUpdate().accept(this);
        }
        return null;
    }
// TODO: @Override public LexorValue visitRepeat(RepeatNode n)
//
//   Executes the REPEAT WHEN loop: body first, then condition check.
//
//   Implementation:
//     int iterations = 0;

    @Override
    public LexorValue visitRepeat(RepeatNode n) {
        int iterations = 0;
        while (evaluate(n.getCondition()).asBool()) {
            if (++iterations > MAX_ITERATIONS) {
                throw new LexorRuntimeException("Possible infinite loop in REPEAT WHEN", n.getLine(), 0);
            }
            executeBlock(n.getBody());
        }
        return null;
    }
// =============================================================================
// VISITOR IMPLEMENTATIONS — EXPRESSIONS
// =============================================================================

// TODO: @Override public LexorValue visitLiteral(LiteralNode n)
//
//   Wraps the literal value in a LexorValue and returns it.
//
//   Implementation:
//     return new LexorValue(n.getType(), n.getValue());
//
//   The Object stored in LiteralNode is already the correct Java type
//   (Integer, Float, Character, Boolean) because the Parser parsed it that way.
    @Override
    public LexorValue visitLiteral(LiteralNode n){
        return new LexorValue(n.getTypeName(), n.getValue());
    }
// TODO: @Override public LexorValue visitVariable(VariableNode n)
//
//   Looks up and returns the variable's current runtime value.
//
//   Implementation:
//     LexorValue val = environment.get(n.getName());
//     log.debug("Read variable {} = {}", n.getName(), val);
//     return val;
    @Override
    public LexorValue visitVariable(VariableNode n){
        LexorValue val = environment.get(n.getName());
        log.debug("Read variable {} = {}",  n.getName(), val);
        return val;
    }
// TODO: @Override public LexorValue visitBinaryExpr(BinaryExprNode n)
//
//   Evaluates both sides, applies the operator, returns a new LexorValue.
//
//   Implementation outline:
//     LexorValue left  = evaluate(n.getLeft());
//     LexorValue right = evaluate(n.getRight());
//     return applyBinaryOp(left, n.getOperator(), right, n.getLine());
//
//   Delegate the actual switch logic to a private helper applyBinaryOp()
//   to keep this method short. See the private helpers section below.
    @Override
    public LexorValue visitBinaryExpr(BinaryExprNode n){
        LexorValue left = evaluate(n.getLeft());
        LexorValue right = evaluate(n.getRight());
        return applyBinaryOp(left, n.getOperator(), right, n.getLine());
    }
// TODO: @Override public LexorValue visitUnaryExpr(UnaryExprNode n)
//
//   Evaluates the operand, applies the unary operator.
//
//   Implementation:
//     LexorValue operand = evaluate(n.getOperand());
//     return switch (n.getOperator()) {
//         case "-"   -> operand.getType().equals("FLOAT")
//                           ? LexorValue.ofFloat(-operand.asFloat())
//                           : LexorValue.ofInt(-operand.asInt());
//         case "NOT" -> LexorValue.ofBool(!operand.asBool());
//         default    -> throw new LexorRuntimeException(
//                           "Unknown unary operator: " + n.getOperator(), n.getLine(), 0);
//     };
    @Override
    public LexorValue visitUnaryExpr(UnaryExprNode n) {
        LexorValue operand = evaluate(n.getOperand());
        return switch (n.getOperator()) {
            case "-" -> operand.getType().equals("FLOAT")
                    ? LexorValue.ofFloat(-operand.asFloat())
                    : LexorValue.ofInt(-operand.asInt());
            case "NOT" -> LexorValue.ofBool(!operand.asBool());
            default -> throw new LexorRuntimeException(
                    "Unknown unary operator: " + n.getOperator(), n.getLine(), 0);
        };
    }

// =============================================================================
// PRIVATE HELPERS
// =============================================================================

// TODO: private LexorValue evaluate(ASTNode node)
//
//   Convenience wrapper around accept() for expression contexts.
//   Keeps visitXxx() methods readable.
//
//   Implementation:
//     return node.accept(this);

// TODO: private void executeBlock(java.util.List<ASTNode> statements)
//
//   Runs a list of statements inside a fresh child scope.
//   Restores the parent scope when finished (even on exception — use try/finally).
//
//   Implementation:
//     Environment outer = this.environment;
//     this.environment  = outer.createChildScope();
//     try {
//         for (ASTNode stmt : statements) stmt.accept(this);
//     } finally {
//         this.environment = outer;   // always restore, even if exception thrown
//     }

// TODO: private LexorValue applyBinaryOp(LexorValue left, String op, LexorValue right, int line)
//
//   The heart of expression evaluation. Switch on `op` and compute the result.
//
//   Strategy — determine if operation should use float or int arithmetic:
//     boolean isFloat = left.getType().equals("FLOAT") || right.getType().equals("FLOAT");
//
//   Then switch:
//     case "+"  → isFloat ? ofFloat(left.asFloat() + right.asFloat())
//                         : ofInt(left.asInt()   + right.asInt())
//     case "-"  → same pattern as "+"
//     case "*"  → same pattern
//     case "/"  → guard: if right is 0, throw LexorRuntimeException("Division by zero")
//                 isFloat ? ofFloat(left.asFloat() / right.asFloat())
//                         : ofInt(left.asInt()   / right.asInt())
//     case "%"  → INT only: guard zero, ofInt(left.asInt() % right.asInt())
//     case "<"  → isFloat ? ofBool(left.asFloat() < right.asFloat())
//                         : ofBool(left.asInt()   < right.asInt())
//     case ">"  → same pattern
//     case "<=" → same pattern
//     case ">=" → same pattern
//     case "==" → ofBool(left.equals(right))    // uses LexorValue.equals()
//     case "<>" → ofBool(!left.equals(right))
//     case "AND"→ ofBool(left.asBool() && right.asBool())
//     case "OR" → ofBool(left.asBool() || right.asBool())
//     default   → throw LexorRuntimeException("Unknown operator: " + op, line, 0)

// TODO: private LexorValue defaultValue(String type)
//
//   Returns the zero/false/null default LexorValue for a given type.
//   Used when a variable is declared without an initializer.
//
//   Implementation:
//     return switch (type) {
//         case "INT"   -> LexorValue.ofInt(0);
//         case "FLOAT" -> LexorValue.ofFloat(0.0f);
//         case "BOOL"  -> LexorValue.ofBool(false);
//         case "CHAR"  -> new LexorValue("CHAR", '\0');
//         default      -> throw new LexorRuntimeException("Unknown type: " + type, 0, 0);
//     };

// TODO: private LexorValue coerce(String raw, String targetType)
//
//   Parses a raw string (from SCAN input) into a LexorValue of the target type.
//   Throws LexorRuntimeException on parse failure (e.g., user types "abc" for INT).
//
//   Implementation:
//     try {
//         return switch (targetType) {
//             case "INT"   -> LexorValue.ofInt(Integer.parseInt(raw));
//             case "FLOAT" -> LexorValue.ofFloat(Float.parseFloat(raw));
//             case "BOOL"  -> {
//                 if (raw.equalsIgnoreCase("TRUE"))  yield LexorValue.ofBool(true);
//                 if (raw.equalsIgnoreCase("FALSE")) yield LexorValue.ofBool(false);
//                 throw new LexorRuntimeException("Invalid BOOL input: " + raw, 0, 0);
//             }
//             case "CHAR"  -> {
//                 if (raw.length() != 1)
//                     throw new LexorRuntimeException("CHAR input must be exactly one character", 0, 0);
//                 yield new LexorValue("CHAR", raw.charAt(0));
//             }
//             default -> throw new LexorRuntimeException("Unknown type: " + targetType, 0, 0);
//         };
//     } catch (NumberFormatException e) {
//         throw new LexorRuntimeException(
//             "Cannot convert '" + raw + "' to " + targetType, 0, 0);
//     }

// TODO: private String resolveEscapeCode(String text)
//
//   Replaces a known escape code string with its literal character.
//   Uses the static ESCAPE_MAP declared at the top of the class.
//
//   Implementation:
//     String replacement = ESCAPE_MAP.get(text);
//     return replacement != null ? replacement : text;
//
//   NOTE: If text is NOT a recognized escape code (e.g., it's a normal string
//   like "Hello"), getOrDefault returns the original text unchanged.
//   Only LiteralNode values that look like "[X]" will ever match the map keys.
//
// =============================================================================
private LexorValue evaluate(ASTNode node) {
    return node.accept(this);
}

    private void executeBlock(List<ASTNode> statements) {
        Environment outer = this.environment;
        this.environment = outer.createChildScope();
        try {
            for (ASTNode stmt : statements) {
                stmt.accept(this);
            }
        } finally {
            // Always restore the parent scope, even if a runtime error occurs!
            this.environment = outer;
        }
    }

    private LexorValue applyBinaryOp(LexorValue left, String op, LexorValue right, int line) {
        if(left == null || right == null)return LexorValue.ofBool(false);
        boolean isFloat = left.getType().equals("FLOAT") || right.getType().equals("FLOAT");

        switch (op) {
            case "+":
                return isFloat ? LexorValue.ofFloat(left.asFloat() + right.asFloat())
                        : LexorValue.ofInt(left.asInt() + right.asInt());
            case "-":
                return isFloat ? LexorValue.ofFloat(left.asFloat() - right.asFloat())
                        : LexorValue.ofInt(left.asInt() - right.asInt());
            case "*":
                return isFloat ? LexorValue.ofFloat(left.asFloat() * right.asFloat())
                        : LexorValue.ofInt(left.asInt() * right.asInt());
            case "/":
                if (right.asFloat() == 0.0f) throw new LexorRuntimeException("Division by zero", line, 0);
                return isFloat ? LexorValue.ofFloat(left.asFloat() / right.asFloat())
                        : LexorValue.ofInt(left.asInt() / right.asInt());
            case "%":
                if (right.asInt() == 0) throw new LexorRuntimeException("Division by zero", line, 0);
                return LexorValue.ofInt(left.asInt() % right.asInt());
            case "<":
                return isFloat ? LexorValue.ofBool(left.asFloat() < right.asFloat())
                        : LexorValue.ofBool(left.asInt() < right.asInt());
            case ">":
                return isFloat ? LexorValue.ofBool(left.asFloat() > right.asFloat())
                        : LexorValue.ofBool(left.asInt() > right.asInt());
            case "<=":
                return isFloat ? LexorValue.ofBool(left.asFloat() <= right.asFloat())
                        : LexorValue.ofBool(left.asInt() <= right.asInt());
            case ">=":
                return isFloat ? LexorValue.ofBool(left.asFloat() >= right.asFloat())
                        : LexorValue.ofBool(left.asInt() >= right.asInt());
            case "==":
                return LexorValue.ofBool(left.equals(right));
            case "<>":
                return LexorValue.ofBool(!left.equals(right));
            case "AND":
                return LexorValue.ofBool(left.asBool() && right.asBool());
            case "OR":
                return LexorValue.ofBool(left.asBool() || right.asBool());
            default:
                throw new LexorRuntimeException("Unknown operator: " + op, line, 0);
        }
    }

    private LexorValue defaultValue(String type) {
        return switch (type) {
            case "INT"   -> LexorValue.ofInt(0);
            case "FLOAT" -> LexorValue.ofFloat(0.0f);
            case "BOOL"  -> LexorValue.ofBool(false);
            case "CHAR"  -> new LexorValue("CHAR", '\0');
            default      -> throw new LexorRuntimeException("Unknown type: " + type, 0, 0);
        };
    }

    private LexorValue coerce(String raw, String targetType) {
        try {
            return switch (targetType) {
                case "INT"   -> LexorValue.ofInt(Integer.parseInt(raw));
                case "FLOAT" -> LexorValue.ofFloat(Float.parseFloat(raw));
                case "BOOL"  -> {
                    if (raw.equalsIgnoreCase("TRUE"))  yield LexorValue.ofBool(true);
                    if (raw.equalsIgnoreCase("FALSE")) yield LexorValue.ofBool(false);
                    throw new LexorRuntimeException("Invalid BOOL input: " + raw, 0, 0);
                }
                case "CHAR"  -> {
                    if (raw.length() != 1)
                        throw new LexorRuntimeException("CHAR input must be exactly one character", 0, 0);
                    yield new LexorValue("CHAR", raw.charAt(0));
                }
                default -> throw new LexorRuntimeException("Unknown type: " + targetType, 0, 0);
            };
        } catch (NumberFormatException e) {
            throw new LexorRuntimeException("Cannot convert '" + raw + "' to " + targetType, 0, 0);
        }
    }

    private String resolveEscapeCode(String text) {
        String replacement = ESCAPE_MAP.get(text);
        return replacement != null ? replacement : text;
    }
}