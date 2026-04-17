package com.lexor.parser;

import com.lexor.error.ParseException;
import com.lexor.lexer.Lexer;
import com.lexor.lexer.Token;
import com.lexor.parser.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParserTest — Unit and integration tests for the Parser class.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * WHAT IS BEING TESTED
 * ══════════════════════════════════════════════════════════════════════════════
 * 1.  Program structure    — SCRIPT AREA / START SCRIPT / END SCRIPT
 * 2.  Declarations         — all four types; multiple vars; with/without initializer
 * 3.  Assignments          — simple and chained (x=y=4)
 * 4.  Arithmetic exprs     — operator precedence, unary minus, parentheses
 * 5.  Relational exprs     — all six comparison operators
 * 6.  Logical exprs        — AND, OR, NOT, mixed precedence
 * 7.  PRINT statement      — segments, &, $, escape codes [x], string literals
 * 8.  SCAN statement       — single and multiple variables
 * 9.  IF / ELSE IF / ELSE  — all three forms
 * 10. FOR loop             — header clauses, body
 * 11. REPEAT WHEN loop     — condition and body
 * 12. AST node types       — assert concrete node classes returned
 * 13. Error cases          — missing keywords, bad structure → ParseException
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * APPROACH — PIPELINE HELPER
 * ══════════════════════════════════════════════════════════════════════════════
 * parse(source) lexes then parses in one call, returning the ProgramNode root.
 * All tests call this helper — no manual token list construction needed.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * DEPENDENCIES
 * ══════════════════════════════════════════════════════════════════════════════
 * JUnit 5 — @Test, @Nested, @DisplayName, @ParameterizedTest, Assertions.*
 * No Mockito — Parser and Lexer are both pure functions of their input.
 *
 * HOW TO RUN (IntelliJ)
 * ----------------------
 * Right-click this file → Run 'ParserTest'.
 * All tests should pass once Parser.java is fully implemented.
 */
@DisplayName("Parser")
class ParserTest {

    // =========================================================================
    // PIPELINE HELPERS
    // =========================================================================

    /** Lex + Parse a source string. Returns the ProgramNode root. */
    private ProgramNode parse(String source) {
        List<Token> tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    /**
     * Wraps source inside SCRIPT AREA / START SCRIPT … END SCRIPT.
     * Use this for tests that focus on inner statements, not program structure.
     */
    private ProgramNode parseProgram(String innerSource) {
        return parse("""
                SCRIPT AREA
                START SCRIPT
                """ + innerSource + """
                END SCRIPT
                """);
    }

    /**
     * Returns the first statement from a program containing only declarations
     * and one or more statements.
     */
    private ASTNode firstStatement(String innerSource) {
        return parseProgram(innerSource).getStatements().get(0);
    }

    /**
     * Wraps innerSource in a full program that includes the required DECLARE
     * block, then returns the first statement.
     */
    private ASTNode firstStmt(String declarations, String statement) {
        return parseProgram(declarations + "\n" + statement).getStatements().get(0);
    }


    // =========================================================================
    // 1. PROGRAM STRUCTURE
    // =========================================================================

    @Nested
    @DisplayName("Program structure")
    class ProgramStructure {

        @Test
        @DisplayName("Minimal valid program parses without exception")
        void minimalProgram() {
            assertDoesNotThrow(() -> parse("""
                    SCRIPT AREA
                    START SCRIPT
                    END SCRIPT
                    """));
        }

        @Test
        @DisplayName("parse() returns a ProgramNode")
        void returnsProgram() {
            ProgramNode p = parse("SCRIPT AREA\nSTART SCRIPT\nEND SCRIPT\n");
            assertNotNull(p);
        }

        @Test
        @DisplayName("Empty program has zero declarations and zero statements")
        void emptyProgramHasZeroChildren() {
            ProgramNode p = parse("SCRIPT AREA\nSTART SCRIPT\nEND SCRIPT\n");
            assertTrue(p.getDeclarations().isEmpty());
            assertTrue(p.getStatements().isEmpty());
        }

        @Test
        @DisplayName("Missing SCRIPT AREA throws ParseException")
        void missingScriptArea() {
            assertThrows(ParseException.class, () -> parse("""
                    START SCRIPT
                    END SCRIPT
                    """));
        }

        @Test
        @DisplayName("Missing END SCRIPT throws ParseException")
        void missingEndScript() {
            assertThrows(ParseException.class, () -> parse("""
                    SCRIPT AREA
                    START SCRIPT
                    DECLARE INT x
                    """));
        }

        @Test
        @DisplayName("Spec sample program 1 parses without exception")
        void specSample1() {
            assertDoesNotThrow(() -> parse("""
                    SCRIPT AREA
                    START SCRIPT
                    DECLARE INT x, y, z=5
                    DECLARE CHAR a_1='n'
                    DECLARE BOOL t="TRUE"
                    x=y=4
                    a_1='c'
                    PRINT: x & t & z & $ & a_1 & [#] & "last"
                    END SCRIPT
                    """));
        }
    }


    // =========================================================================
    // 2. DECLARATIONS
    // =========================================================================

    @Nested
    @DisplayName("Declarations")
    class Declarations {

        @ParameterizedTest(name = "DECLARE {0} x → DeclarationNode with typeName={0}")
        @CsvSource({"INT", "FLOAT", "CHAR", "BOOL"})
        @DisplayName("All four type keywords produce correct DeclarationNode")
        void allTypes(String typeName) {
            String init = switch (typeName) {
                case "INT"   -> "";
                case "FLOAT" -> "";
                case "CHAR"  -> "='a'";
                case "BOOL"  -> "=\"TRUE\"";
                default      -> "";
            };
            ProgramNode p = parseProgram("DECLARE " + typeName + " x" + init + "\n");
            assertEquals(1, p.getDeclarations().size());
            assertEquals(typeName, p.getDeclarations().get(0).getTypeName());
        }

        @Test
        @DisplayName("Single declaration with no initializer — getInitializer() is null")
        void noInitializer() {
            ProgramNode p = parseProgram("DECLARE INT x\n");
            DeclarationNode d = p.getDeclarations().get(0);
            assertEquals("x", d.getName());
            assertFalse(d.hasInitializer());
            assertNull(d.getInitializer());
        }

        @Test
        @DisplayName("Declaration with INT initializer — LiteralNode with value 5")
        void intInitializer() {
            ProgramNode p = parseProgram("DECLARE INT z=5\n");
            DeclarationNode d = p.getDeclarations().get(0);
            assertTrue(d.hasInitializer());
            LiteralNode lit = assertInstanceOf(LiteralNode.class, d.getInitializer());
            assertEquals("INT", lit.getTypeName());
            assertEquals(5, lit.getValue());
        }

        @Test
        @DisplayName("Declaration with CHAR initializer — LiteralNode with value 'n'")
        void charInitializer() {
            ProgramNode p = parseProgram("DECLARE CHAR a='n'\n");
            LiteralNode lit = assertInstanceOf(LiteralNode.class,
                    p.getDeclarations().get(0).getInitializer());
            assertEquals('n', lit.getValue());
            assertEquals("CHAR", lit.getTypeName());
        }

        @Test
        @DisplayName("Declaration with BOOL initializer TRUE — LiteralNode with value true")
        void boolTrueInitializer() {
            ProgramNode p = parseProgram("DECLARE BOOL t=\"TRUE\"\n");
            LiteralNode lit = assertInstanceOf(LiteralNode.class,
                    p.getDeclarations().get(0).getInitializer());
            assertEquals(true, lit.getValue());
        }

        @Test
        @DisplayName("Declaration with BOOL initializer FALSE — LiteralNode with value false")
        void boolFalseInitializer() {
            ProgramNode p = parseProgram("DECLARE BOOL t=\"FALSE\"\n");
            LiteralNode lit = assertInstanceOf(LiteralNode.class,
                    p.getDeclarations().get(0).getInitializer());
            assertEquals(false, lit.getValue());
        }

        @Test
        @DisplayName("Multiple variables on one DECLARE line — correct count and order")
        void multipleVarsOnOneLine() {
            ProgramNode p = parseProgram("DECLARE INT x, y, z=5\n");
            List<DeclarationNode> decls = p.getDeclarations();
            assertEquals(3, decls.size());
            assertEquals("x", decls.get(0).getName());
            assertEquals("y", decls.get(1).getName());
            assertEquals("z", decls.get(2).getName());
            assertTrue(decls.get(2).hasInitializer());
        }

        @Test
        @DisplayName("Multiple DECLARE lines — all collected into one list")
        void multipleDeclareLines() {
            ProgramNode p = parseProgram("""
                    DECLARE INT a=100, b=200, c=300
                    DECLARE BOOL d="FALSE"
                    """);
            assertEquals(4, p.getDeclarations().size());
        }

        @Test
        @DisplayName("DECLARE without type keyword throws ParseException")
        void missingTypeThrows() {
            assertThrows(ParseException.class, () ->
                    parseProgram("DECLARE x\n"));
        }
    }


    // =========================================================================
    // 3. ASSIGNMENTS
    // =========================================================================

    @Nested
    @DisplayName("Assignments")
    class Assignments {

        @Test
        @DisplayName("Simple assignment: x=4 → AssignmentNode with one target")
        void simpleAssignment() {
            ASTNode stmt = firstStmt("DECLARE INT x\n", "x=4\n");
            AssignmentNode a = assertInstanceOf(AssignmentNode.class, stmt);
            assertEquals(List.of("x"), a.getTargets());
            LiteralNode val = assertInstanceOf(LiteralNode.class, a.getValue());
            assertEquals(4, val.getValue());
        }

        @Test
        @DisplayName("Chained assignment: x=y=4 → AssignmentNode with two targets")
        void chainedAssignment() {
            ASTNode stmt = firstStmt("DECLARE INT x\nDECLARE INT y\n", "x=y=4\n");
            AssignmentNode a = assertInstanceOf(AssignmentNode.class, stmt);
            assertEquals(2, a.getTargets().size());
            assertEquals("x", a.getTargets().get(0));
            assertEquals("y", a.getTargets().get(1));
        }

        @Test
        @DisplayName("Assignment value is a variable reference → VariableNode")
        void assignmentFromVariable() {
            ASTNode stmt = firstStmt("DECLARE INT x\nDECLARE INT y\n", "x=y\n");
            AssignmentNode a = assertInstanceOf(AssignmentNode.class, stmt);
            VariableNode v = assertInstanceOf(VariableNode.class, a.getValue());
            assertEquals("y", v.getName());
        }
    }


    // =========================================================================
    // 4. ARITHMETIC EXPRESSIONS
    // =========================================================================

    @Nested
    @DisplayName("Arithmetic expressions")
    class ArithmeticExpressions {

        /** Parse an assignment  z = <expr>  and return the value node. */
        private ASTNode parseExpr(String expr) {
            return ((AssignmentNode) firstStmt(
                    "DECLARE INT z\nDECLARE INT a\nDECLARE INT b\n",
                    "z=" + expr + "\n")).getValue();
        }

        @Test
        @DisplayName("Integer literal parses to LiteralNode(INT)")
        void intLiteral() {
            LiteralNode n = assertInstanceOf(LiteralNode.class, parseExpr("42"));
            assertEquals(42, n.getValue());
            assertEquals("INT", n.getTypeName());
        }

        @Test
        @DisplayName("Float literal parses to LiteralNode(FLOAT)")
        void floatLiteral() {
            ASTNode n = ((AssignmentNode) firstStmt(
                    "DECLARE FLOAT z\n", "z=3.14\n")).getValue();
            LiteralNode lit = assertInstanceOf(LiteralNode.class, n);
            assertEquals("FLOAT", lit.getTypeName());
            assertEquals(3.14f, (Float) lit.getValue(), 0.001f);
        }

        @Test
        @DisplayName("a + b → BinaryExprNode with operator '+'")
        void additionBinary() {
            BinaryExprNode n = assertInstanceOf(BinaryExprNode.class, parseExpr("a+b"));
            assertEquals("+", n.getOperator());
            assertInstanceOf(VariableNode.class, n.getLeft());
            assertInstanceOf(VariableNode.class, n.getRight());
        }

        @Test
        @DisplayName("a - b → BinaryExprNode with operator '-'")
        void subtractionBinary() {
            BinaryExprNode n = assertInstanceOf(BinaryExprNode.class, parseExpr("a-b"));
            assertEquals("-", n.getOperator());
        }

        @Test
        @DisplayName("a * b → BinaryExprNode with operator '*'")
        void multiplication() {
            BinaryExprNode n = assertInstanceOf(BinaryExprNode.class, parseExpr("a*b"));
            assertEquals("*", n.getOperator());
        }

        @Test
        @DisplayName("a / b → BinaryExprNode with operator '/'")
        void division() {
            assertEquals("/", assertInstanceOf(BinaryExprNode.class,
                    parseExpr("a/b")).getOperator());
        }

        @Test
        @DisplayName("a % b → BinaryExprNode with operator '%'")
        void modulo() {
            assertEquals("%", assertInstanceOf(BinaryExprNode.class,
                    parseExpr("a%b")).getOperator());
        }

        @Test
        @DisplayName("Unary minus: -1 → UnaryExprNode('-', LiteralNode(1))")
        void unaryMinus() {
            UnaryExprNode u = assertInstanceOf(UnaryExprNode.class, parseExpr("-1"));
            assertEquals("-", u.getOperator());
            LiteralNode operand = assertInstanceOf(LiteralNode.class, u.getOperand());
            assertEquals(1, operand.getValue());
        }

        @Test
        @DisplayName("Spec sample: ((abc*5)/10+10)*-1 parses without exception")
        void complexArithmetic() {
            assertDoesNotThrow(() -> firstStmt(
                    "DECLARE INT xyz\nDECLARE INT abc=100\n",
                    "xyz=((abc*5)/10+10)*-1\n"));
        }

        @Test
        @DisplayName("Multiplication has higher precedence than addition: a+b*c → a+(b*c)")
        void precedenceMulOverAdd() {
            // a + b * c  should parse as  BinaryExpr(a, +, BinaryExpr(b, *, c))
            BinaryExprNode root = assertInstanceOf(BinaryExprNode.class, parseExpr("a+b*a"));
            assertEquals("+", root.getOperator());
            // right child should be the multiplication
            BinaryExprNode right = assertInstanceOf(BinaryExprNode.class, root.getRight());
            assertEquals("*", right.getOperator());
        }

        @Test
        @DisplayName("Parentheses override precedence: (a+b)*c → multiply at root")
        void parenthesesOverridePrecedence() {
            BinaryExprNode root = assertInstanceOf(BinaryExprNode.class, parseExpr("(a+b)*a"));
            assertEquals("*", root.getOperator());
            BinaryExprNode left = assertInstanceOf(BinaryExprNode.class, root.getLeft());
            assertEquals("+", left.getOperator());
        }
    }


    // =========================================================================
    // 5. RELATIONAL EXPRESSIONS
    // =========================================================================

    @Nested
    @DisplayName("Relational expressions")
    class RelationalExpressions {

        private BinaryExprNode parseRelation(String expr) {
            return assertInstanceOf(BinaryExprNode.class,
                    ((AssignmentNode) firstStmt(
                            "DECLARE BOOL d\nDECLARE INT a=100\nDECLARE INT b=200\n",
                            "d=(a" + expr + "b)\n")).getValue());
        }

        @ParameterizedTest(name = "a {0} b → BinaryExprNode operator=''{0}''")
        @CsvSource({">", "<", ">=", "<=", "==", "<>"})
        @DisplayName("All six relational operators parse correctly")
        void allRelationalOps(String op) {
            BinaryExprNode n = parseRelation(op);
            assertEquals(op, n.getOperator());
        }
    }


    // =========================================================================
    // 6. LOGICAL EXPRESSIONS
    // =========================================================================

    @Nested
    @DisplayName("Logical expressions")
    class LogicalExpressions {

        private ASTNode parseLogical(String expr) {
            return ((AssignmentNode) firstStmt(
                    "DECLARE BOOL d\nDECLARE INT a=100\nDECLARE INT b=200\nDECLARE INT c=300\n",
                    "d=(" + expr + ")\n")).getValue();
        }

        @Test
        @DisplayName("a<b AND c<>200 → BinaryExprNode with operator 'AND'")
        void andExpression() {
            BinaryExprNode n = assertInstanceOf(BinaryExprNode.class,
                    parseLogical("a<b AND c<>200"));
            assertEquals("AND", n.getOperator());
        }

        @Test
        @DisplayName("a<b OR c==300 → BinaryExprNode with operator 'OR'")
        void orExpression() {
            BinaryExprNode n = assertInstanceOf(BinaryExprNode.class,
                    parseLogical("a<b OR c==300"));
            assertEquals("OR", n.getOperator());
        }

        @Test
        @DisplayName("NOT d → UnaryExprNode with operator 'NOT'")
        void notExpression() {
            ASTNode stmt = firstStmt(
                    "DECLARE BOOL d\nDECLARE BOOL e\n", "d=NOT e\n");
            AssignmentNode a = assertInstanceOf(AssignmentNode.class, stmt);
            UnaryExprNode u = assertInstanceOf(UnaryExprNode.class, a.getValue());
            assertEquals("NOT", u.getOperator());
        }

        @Test
        @DisplayName("Relational has higher precedence than AND: a<b AND c>0 → AND at root")
        void relationalBeforeAnd() {
            BinaryExprNode root = assertInstanceOf(BinaryExprNode.class,
                    parseLogical("a<b AND c>a"));
            assertEquals("AND", root.getOperator());
            // both children should be relational nodes
            assertInstanceOf(BinaryExprNode.class, root.getLeft());
            assertInstanceOf(BinaryExprNode.class, root.getRight());
        }
    }


    // =========================================================================
    // 7. PRINT STATEMENT
    // =========================================================================

    @Nested
    @DisplayName("PRINT statement")
    class PrintStatement {

        private PrintNode parsePrint(String printLine) {
            return assertInstanceOf(PrintNode.class,
                    firstStmt("DECLARE INT x\nDECLARE BOOL t\n", printLine));
        }

        @Test
        @DisplayName("PRINT with single variable → PrintNode with one VariableNode segment")
        void singleVariable() {
            PrintNode p = parsePrint("PRINT: x\n");
            assertEquals(1, p.getSegments().size());
            assertInstanceOf(VariableNode.class, p.getSegments().get(0));
        }

        @Test
        @DisplayName("PRINT: x & t → two segments separated by &")
        void twoSegments() {
            PrintNode p = parsePrint("PRINT: x & t\n");
            assertEquals(2, p.getSegments().size());
        }

        @Test
        @DisplayName("PRINT: $ → segment is LiteralNode with NEWLINE_MARKER type")
        void dollarIsNewlineMarker() {
            PrintNode p = parsePrint("PRINT: $\n");
            LiteralNode seg = assertInstanceOf(LiteralNode.class, p.getSegments().get(0));
            assertEquals("NEWLINE_MARKER", seg.getTypeName());
            assertEquals("\n", seg.getValue());
        }

        @Test
        @DisplayName("PRINT: [#] → segment is LiteralNode with ESCAPE type and value '#'")
        void escapeHashCode() {
            PrintNode p = parsePrint("PRINT: [#]\n");
            LiteralNode seg = assertInstanceOf(LiteralNode.class, p.getSegments().get(0));
            assertEquals("ESCAPE", seg.getTypeName());
            assertEquals("#", seg.getValue());
        }

        @Test
        @DisplayName("PRINT: [[] → escape code for literal '['")
        void escapeOpenBracket() {
            PrintNode p = parsePrint("PRINT: [[]\n");
            LiteralNode seg = assertInstanceOf(LiteralNode.class, p.getSegments().get(0));
            assertEquals("ESCAPE", seg.getTypeName());
            assertEquals("[", seg.getValue());
        }

        @Test
        @DisplayName("PRINT: \"last\" → segment is STRING LiteralNode")
        void stringSegment() {
            PrintNode p = parsePrint("PRINT: \"last\"\n");
            LiteralNode seg = assertInstanceOf(LiteralNode.class, p.getSegments().get(0));
            assertEquals("STRING", seg.getTypeName());
            assertEquals("last", seg.getValue());
        }

        @Test
        @DisplayName("Spec PRINT line: x & t & z & $ & a_1 & [#] & \"last\" → 7 segments")
        void specPrintLine() {
            ProgramNode prog = parse("""
                    SCRIPT AREA
                    START SCRIPT
                    DECLARE INT x, z
                    DECLARE BOOL t
                    DECLARE CHAR a_1='c'
                    PRINT: x & t & z & $ & a_1 & [#] & "last"
                    END SCRIPT
                    """);
            PrintNode p = assertInstanceOf(PrintNode.class, prog.getStatements().get(0));
            assertEquals(7, p.getSegments().size());
        }

        @Test
        @DisplayName("PRINT without COLON throws ParseException")
        void missingColon() {
            assertThrows(ParseException.class, () ->
                    parseProgram("DECLARE INT x\nPRINT x\n"));
        }
    }


    // =========================================================================
    // 8. SCAN STATEMENT
    // =========================================================================

    @Nested
    @DisplayName("SCAN statement")
    class ScanStatement {

        @Test
        @DisplayName("SCAN: x → ScanNode with one variable")
        void singleVariable() {
            ScanNode s = assertInstanceOf(ScanNode.class,
                    firstStmt("DECLARE INT x\n", "SCAN: x\n"));
            assertEquals(List.of("x"), s.getVariables());
        }

        @Test
        @DisplayName("SCAN: x, y → ScanNode with two variables in order")
        void twoVariables() {
            ScanNode s = assertInstanceOf(ScanNode.class,
                    firstStmt("DECLARE INT x\nDECLARE INT y\n", "SCAN: x, y\n"));
            assertEquals(List.of("x", "y"), s.getVariables());
        }

        @Test
        @DisplayName("SCAN without COLON throws ParseException")
        void missingColon() {
            assertThrows(ParseException.class, () ->
                    parseProgram("DECLARE INT x\nSCAN x\n"));
        }
    }


    // =========================================================================
    // 9. IF / ELSE IF / ELSE
    // =========================================================================

    @Nested
    @DisplayName("IF / ELSE IF / ELSE")
    class IfElse {

        private IfNode parseIf(String src) {
            return assertInstanceOf(IfNode.class,
                    parseProgram(src).getStatements().get(0));
        }

        @Test
        @DisplayName("Simple IF: condition stored, thenBlock has one statement")
        void simpleIf() {
            IfNode n = parseIf("""
                    DECLARE INT x=4
                    IF (x == 4)
                    START IF
                    PRINT: "yes"
                    END IF
                    """);
            assertNotNull(n.getCondition());
            assertEquals(1, n.getThenBlock().size());
            assertFalse(n.hasElse());
            assertTrue(n.getElseIfClauses().isEmpty());
        }

        @Test
        @DisplayName("IF condition is a BinaryExprNode (==)")
        void conditionIsBinaryExpr() {
            IfNode n = parseIf("""
                    DECLARE INT x
                    IF (x == 4)
                    START IF
                    PRINT: x
                    END IF
                    """);
            BinaryExprNode cond = assertInstanceOf(BinaryExprNode.class, n.getCondition());
            assertEquals("==", cond.getOperator());
        }

        @Test
        @DisplayName("IF-ELSE: hasElse() returns true, elseBlock has one statement")
        void ifElse() {
            IfNode n = parseIf("""
                    DECLARE INT x
                    IF (x == 4)
                    START IF
                    PRINT: "yes"
                    END IF
                    ELSE
                    START IF
                    PRINT: "no"
                    END IF
                    """);
            assertTrue(n.hasElse());
            assertEquals(1, n.getElseBlock().size());
        }

        @Test
        @DisplayName("IF-ELSE IF: one ElseIfClause with its own condition")
        void ifElseIf() {
            IfNode n = parseIf("""
                    DECLARE INT x
                    IF (x == 4)
                    START IF
                    PRINT: "four"
                    END IF
                    ELSE IF (x == 10)
                    START IF
                    PRINT: "ten"
                    END IF
                    """);
            assertEquals(1, n.getElseIfClauses().size());
            assertFalse(n.hasElse());
            IfNode.ElseIfClause clause = n.getElseIfClauses().get(0);
            assertNotNull(clause.getCondition());
            assertEquals(1, clause.getBody().size());
        }

        @Test
        @DisplayName("IF-ELSE IF-ELSE: two clauses and an else block")
        void ifElseIfElse() {
            IfNode n = parseIf("""
                    DECLARE INT x
                    IF (x == 4)
                    START IF
                    PRINT: "four"
                    END IF
                    ELSE IF (x == 10)
                    START IF
                    PRINT: "ten"
                    END IF
                    ELSE
                    START IF
                    PRINT: "other"
                    END IF
                    """);
            assertEquals(1, n.getElseIfClauses().size());
            assertTrue(n.hasElse());
        }

        @Test
        @DisplayName("Missing START IF throws ParseException")
        void missingStartIf() {
            assertThrows(ParseException.class, () -> parseProgram("""
                    DECLARE INT x
                    IF (x == 4)
                    PRINT: x
                    END IF
                    """));
        }

        @Test
        @DisplayName("Missing END IF throws ParseException")
        void missingEndIf() {
            assertThrows(ParseException.class, () -> parseProgram("""
                    DECLARE INT x
                    IF (x == 4)
                    START IF
                    PRINT: x
                    """));
        }
    }


    // =========================================================================
    // 10. FOR LOOP
    // =========================================================================

    @Nested
    @DisplayName("FOR loop")
    class ForLoop {

        private ForNode parseFor() {
            return assertInstanceOf(ForNode.class, parseProgram("""
                    DECLARE INT x=0
                    FOR (x=0, x<5, x=x+1)
                    START FOR
                    PRINT: x
                    END FOR
                    """).getStatements().get(0));
        }

        @Test
        @DisplayName("FOR parses without exception")
        void parsesOk() {
            assertDoesNotThrow(this::parseFor);
        }

        @Test
        @DisplayName("FOR init is AssignmentNode with target 'x' and value 0")
        void initNode() {
            ForNode f = parseFor();
            AssignmentNode init = f.getInit();
            assertEquals(List.of("x"), init.getTargets());
            LiteralNode val = assertInstanceOf(LiteralNode.class, init.getValue());
            assertEquals(0, val.getValue());
        }

        @Test
        @DisplayName("FOR condition is BinaryExprNode with operator '<'")
        void conditionNode() {
            BinaryExprNode cond = assertInstanceOf(BinaryExprNode.class,
                    parseFor().getCondition());
            assertEquals("<", cond.getOperator());
        }

        @Test
        @DisplayName("FOR update is AssignmentNode with target 'x'")
        void updateNode() {
            ForNode f = parseFor();
            assertEquals(List.of("x"), f.getUpdate().getTargets());
        }

        @Test
        @DisplayName("FOR body has one statement")
        void bodyCount() {
            assertEquals(1, parseFor().getBody().size());
        }

        @Test
        @DisplayName("Missing FOR header parentheses throws ParseException")
        void missingParens() {
            assertThrows(ParseException.class, () -> parseProgram("""
                    DECLARE INT x
                    FOR x=0, x<5, x=x+1
                    START FOR
                    END FOR
                    """));
        }
    }


    // =========================================================================
    // 11. REPEAT WHEN LOOP
    // =========================================================================

    @Nested
    @DisplayName("REPEAT WHEN loop")
    class RepeatWhen {

        private RepeatNode parseRepeat() {
            return assertInstanceOf(RepeatNode.class, parseProgram("""
                    DECLARE INT x=0
                    REPEAT WHEN (x < 10)
                    START REPEAT
                    x=x+1
                    END REPEAT
                    """).getStatements().get(0));
        }

        @Test
        @DisplayName("REPEAT WHEN parses without exception")
        void parsesOk() {
            assertDoesNotThrow(this::parseRepeat);
        }

        @Test
        @DisplayName("REPEAT condition is BinaryExprNode with operator '<'")
        void conditionNode() {
            BinaryExprNode cond = assertInstanceOf(BinaryExprNode.class,
                    parseRepeat().getCondition());
            assertEquals("<", cond.getOperator());
        }

        @Test
        @DisplayName("REPEAT body has one statement")
        void bodyCount() {
            assertEquals(1, parseRepeat().getBody().size());
        }

        @Test
        @DisplayName("REPEAT without WHEN throws ParseException")
        void missingWhen() {
            assertThrows(ParseException.class, () -> parseProgram("""
                    DECLARE INT x
                    REPEAT (x < 10)
                    START REPEAT
                    END REPEAT
                    """));
        }
    }


    // =========================================================================
    // 12. AST NODE TYPE ASSERTIONS
    // =========================================================================

    @Nested
    @DisplayName("AST node types returned")
    class NodeTypes {

        @Test
        @DisplayName("Variable reference in expression produces VariableNode")
        void variableReference() {
            AssignmentNode a = assertInstanceOf(AssignmentNode.class,
                    firstStmt("DECLARE INT x\nDECLARE INT y\n", "x=y\n"));
            assertInstanceOf(VariableNode.class, a.getValue());
        }

        @Test
        @DisplayName("Nested parenthesised expression still produces correct node")
        void nestedParens() {
            AssignmentNode a = assertInstanceOf(AssignmentNode.class,
                    firstStmt("DECLARE INT x\nDECLARE INT y\n", "x=((y))\n"));
            assertInstanceOf(VariableNode.class, a.getValue());
        }

        @Test
        @DisplayName("PRINT statement produces PrintNode")
        void printNodeType() {
            assertInstanceOf(PrintNode.class,
                    firstStmt("DECLARE INT x\n", "PRINT: x\n"));
        }

        @Test
        @DisplayName("SCAN statement produces ScanNode")
        void scanNodeType() {
            assertInstanceOf(ScanNode.class,
                    firstStmt("DECLARE INT x\n", "SCAN: x\n"));
        }

        @Test
        @DisplayName("IF statement produces IfNode")
        void ifNodeType() {
            assertInstanceOf(IfNode.class,
                    firstStmt("DECLARE BOOL b=\"TRUE\"\n",
                            "IF (b == b)\nSTART IF\nPRINT: b\nEND IF\n"));
        }

        @Test
        @DisplayName("FOR produces ForNode")
        void forNodeType() {
            assertInstanceOf(ForNode.class, parseProgram("""
                    DECLARE INT i
                    FOR (i=0, i<1, i=i+1)
                    START FOR
                    END FOR
                    """).getStatements().get(0));
        }

        @Test
        @DisplayName("REPEAT WHEN produces RepeatNode")
        void repeatNodeType() {
            assertInstanceOf(RepeatNode.class, parseProgram("""
                    DECLARE BOOL b="FALSE"
                    REPEAT WHEN (b == b)
                    START REPEAT
                    END REPEAT
                    """).getStatements().get(0));
        }
    }


    // =========================================================================
    // 13. ERROR CASES
    // =========================================================================

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("Unexpected token at statement level throws ParseException")
        void unexpectedStatementToken() {
            assertThrows(ParseException.class, () ->
                    parseProgram("DECLARE INT x\n42\n"));
        }

        @Test
        @DisplayName("Missing ')' after IF condition throws ParseException")
        void missingRParenAfterIfCondition() {
            assertThrows(ParseException.class, () -> parseProgram("""
                    DECLARE INT x
                    IF (x == 4
                    START IF
                    END IF
                    """));
        }

        @Test
        @DisplayName("Missing '(' after IF throws ParseException")
        void missingLParenAfterIf() {
            assertThrows(ParseException.class, () -> parseProgram("""
                    DECLARE INT x
                    IF x == 4
                    START IF
                    END IF
                    """));
        }

        @Test
        @DisplayName("FOR missing comma between clauses throws ParseException")
        void forMissingComma() {
            assertThrows(ParseException.class, () -> parseProgram("""
                    DECLARE INT i
                    FOR (i=0 i<5 i=i+1)
                    START FOR
                    END FOR
                    """));
        }

        @Test
        @DisplayName("Declaration placed after a statement throws ParseException")
        void declarationAfterStatement() {
            assertThrows(Exception.class, () -> parseProgram("""
                    DECLARE INT x
                    x=4
                    DECLARE INT y
                    """));
        }
    }
}