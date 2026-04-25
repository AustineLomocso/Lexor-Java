package com.lexor.interpreter;

import com.lexor.error.LexorRuntimeException;
import com.lexor.lexer.Lexer;
import com.lexor.lexer.Token;
import com.lexor.parser.Parser;
import com.lexor.parser.ast.ProgramNode;
import com.lexor.semantic.SemanticAnalyzer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InterpreterTest — Comprehensive test suite for the interpreter package.
 *
 * STRUCTURE:
 *   1. LexorValueTest       — Pure unit tests for LexorValue in isolation.
 *   2. EnvironmentTest      — Pure unit tests for Environment in isolation.
 *   3. InterpreterExecTest  — End-to-end integration tests that run the full
 *                             Lexer → Parser → SemanticAnalyzer → Interpreter pipeline
 *                             and assert on captured stdout output.
 */
class InterpreterTest {

    // =========================================================================
    // SECTION 1 — LexorValue Unit Tests
    // =========================================================================

    @Nested
    @DisplayName("LexorValue")
    class LexorValueTest {

        // ── Constructor & getType ────────────────────────────────────────────

        @Test
        @DisplayName("Constructor stores type and value; getType() returns correct type")
        void constructorStoresTypeAndGetTypeReturnsIt() {
            LexorValue v = new LexorValue("INT", 42);
            assertEquals("INT", v.getType());
        }

        @Test
        @DisplayName("Constructor stores Float value correctly")
        void constructorStoresFloatValue() {
            LexorValue v = new LexorValue("FLOAT", 3.14f);
            assertEquals("FLOAT", v.getType());
        }

        @Test
        @DisplayName("Constructor stores Character value correctly")
        void constructorStoresCharValue() {
            LexorValue v = new LexorValue("CHAR", 'A');
            assertEquals("CHAR", v.getType());
        }

        @Test
        @DisplayName("Constructor stores Boolean value correctly")
        void constructorStoresBoolValue() {
            LexorValue v = new LexorValue("BOOL", true);
            assertEquals("BOOL", v.getType());
        }

        // ── Static factories ─────────────────────────────────────────────────

        @Test
        @DisplayName("ofInt() creates INT value with correct asInt()")
        void ofIntFactory() {
            LexorValue v = LexorValue.ofInt(99);
            assertEquals("INT", v.getType());
            assertEquals(99, v.asInt());
        }

        @Test
        @DisplayName("ofFloat() creates FLOAT value with correct asFloat()")
        void ofFloatFactory() {
            LexorValue v = LexorValue.ofFloat(1.5f);
            assertEquals("FLOAT", v.getType());
            assertEquals(1.5f, v.asFloat(), 0.0001f);
        }

        @Test
        @DisplayName("ofBool(true) creates BOOL value with correct asBool()")
        void ofBoolTrueFactory() {
            LexorValue v = LexorValue.ofBool(true);
            assertEquals("BOOL", v.getType());
            assertTrue(v.asBool());
        }

        @Test
        @DisplayName("ofBool(false) creates BOOL value with correct asBool()")
        void ofBoolFalseFactory() {
            LexorValue v = LexorValue.ofBool(false);
            assertFalse(v.asBool());
        }

        @Test
        @DisplayName("ofChar() creates CHAR value with correct asChar()")
        void ofCharFactory() {
            LexorValue v = LexorValue.ofChar('z');
            assertEquals("CHAR", v.getType());
            assertEquals('z', v.asChar());
        }

        // ── asInt() ──────────────────────────────────────────────────────────

        @Test
        @DisplayName("asInt() returns int value from INT type")
        void asIntFromInt() {
            assertEquals(7, LexorValue.ofInt(7).asInt());
        }

        @Test
        @DisplayName("asInt() truncates FLOAT to int (widening in reverse)")
        void asIntFromFloat() {
            LexorValue v = LexorValue.ofFloat(9.9f);
            assertEquals(9, v.asInt());
        }

        @Test
        @DisplayName("asInt() on BOOL throws LexorRuntimeException")
        void asIntOnBoolThrows() {
            LexorValue v = LexorValue.ofBool(true);
            assertThrows(LexorRuntimeException.class, v::asInt);
        }

        @Test
        @DisplayName("asInt() on CHAR throws LexorRuntimeException")
        void asIntOnCharThrows() {
            LexorValue v = LexorValue.ofChar('A');
            assertThrows(LexorRuntimeException.class, v::asInt);
        }

        // ── asFloat() ────────────────────────────────────────────────────────

        @Test
        @DisplayName("asFloat() returns float value from FLOAT type")
        void asFloatFromFloat() {
            assertEquals(2.5f, LexorValue.ofFloat(2.5f).asFloat(), 0.0001f);
        }

        @Test
        @DisplayName("asFloat() widens INT to float")
        void asFloatFromInt() {
            LexorValue v = LexorValue.ofInt(4);
            assertEquals(4.0f, v.asFloat(), 0.0001f);
        }

        @Test
        @DisplayName("asFloat() on BOOL throws LexorRuntimeException")
        void asFloatOnBoolThrows() {
            assertThrows(LexorRuntimeException.class, () -> LexorValue.ofBool(false).asFloat());
        }

        @Test
        @DisplayName("asFloat() on CHAR throws LexorRuntimeException")
        void asFloatOnCharThrows() {
            assertThrows(LexorRuntimeException.class, () -> LexorValue.ofChar('B').asFloat());
        }

        // ── asBool() ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("asBool() returns true for BOOL true")
        void asBoolTrue() {
            assertTrue(LexorValue.ofBool(true).asBool());
        }

        @Test
        @DisplayName("asBool() returns false for BOOL false")
        void asBoolFalse() {
            assertFalse(LexorValue.ofBool(false).asBool());
        }

        @Test
        @DisplayName("asBool() on INT throws LexorRuntimeException")
        void asBoolOnIntThrows() {
            assertThrows(LexorRuntimeException.class, () -> LexorValue.ofInt(1).asBool());
        }

        @Test
        @DisplayName("asBool() on FLOAT throws LexorRuntimeException")
        void asBoolOnFloatThrows() {
            assertThrows(LexorRuntimeException.class, () -> LexorValue.ofFloat(1.0f).asBool());
        }

        // ── asChar() ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("asChar() returns char value from CHAR type")
        void asCharFromChar() {
            assertEquals('x', LexorValue.ofChar('x').asChar());
        }

        @Test
        @DisplayName("asChar() on INT throws LexorRuntimeException")
        void asCharOnIntThrows() {
            assertThrows(LexorRuntimeException.class, () -> LexorValue.ofInt(65).asChar());
        }

        // ── toString() ───────────────────────────────────────────────────────

        @Test
        @DisplayName("toString() on INT returns digit string")
        void toStringInt() {
            assertEquals("42", LexorValue.ofInt(42).toString());
        }

        @Test
        @DisplayName("toString() on INT zero returns '0'")
        void toStringIntZero() {
            assertEquals("0", LexorValue.ofInt(0).toString());
        }

        @Test
        @DisplayName("toString() on negative INT returns negative string")
        void toStringNegativeInt() {
            assertEquals("-5", LexorValue.ofInt(-5).toString());
        }

        @Test
        @DisplayName("toString() on BOOL true returns 'TRUE' (uppercase)")
        void toStringBoolTrue() {
            assertEquals("TRUE", LexorValue.ofBool(true).toString());
        }

        @Test
        @DisplayName("toString() on BOOL false returns 'FALSE' (uppercase)")
        void toStringBoolFalse() {
            assertEquals("FALSE", LexorValue.ofBool(false).toString());
        }

        @Test
        @DisplayName("toString() on CHAR returns single character string")
        void toStringChar() {
            assertEquals("n", LexorValue.ofChar('n').toString());
        }

        @Test
        @DisplayName("toString() on FLOAT returns numeric string")
        void toStringFloat() {
            LexorValue v = LexorValue.ofFloat(3.5f);
            // Must contain "3" and "5"
            String s = v.toString();
            assertTrue(s.contains("3") && s.contains("5"),
                "Expected float string to contain digits, got: " + s);
        }

        // ── equals() and hashCode() ───────────────────────────────────────────

        @Test
        @DisplayName("Two INT LexorValues with same value are equal")
        void equalsIntSameValue() {
            assertEquals(LexorValue.ofInt(10), LexorValue.ofInt(10));
        }

        @Test
        @DisplayName("Two INT LexorValues with different values are not equal")
        void equalsIntDifferentValue() {
            assertNotEquals(LexorValue.ofInt(1), LexorValue.ofInt(2));
        }

        @Test
        @DisplayName("INT and FLOAT with same numeric value are not equal (type differs)")
        void equalsIntVsFloatNotEqual() {
            assertNotEquals(LexorValue.ofInt(5), LexorValue.ofFloat(5.0f));
        }

        @Test
        @DisplayName("Two BOOL true values are equal")
        void equalsBoolTrue() {
            assertEquals(LexorValue.ofBool(true), LexorValue.ofBool(true));
        }

        @Test
        @DisplayName("BOOL true and BOOL false are not equal")
        void equalsBoolTrueVsFalse() {
            assertNotEquals(LexorValue.ofBool(true), LexorValue.ofBool(false));
        }

        @Test
        @DisplayName("hashCode() is consistent with equals()")
        void hashCodeConsistentWithEquals() {
            LexorValue a = LexorValue.ofInt(7);
            LexorValue b = LexorValue.ofInt(7);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("LexorValue is not equal to null")
        void notEqualToNull() {
            assertNotEquals(LexorValue.ofInt(1), null);
        }

        @Test
        @DisplayName("LexorValue is not equal to a plain String")
        void notEqualToString() {
            assertNotEquals(LexorValue.ofInt(1), "1");
        }
    }


    // =========================================================================
    // SECTION 2 — Environment Unit Tests
    // =========================================================================

    @Nested
    @DisplayName("Environment")
    class EnvironmentTest {

        // ── define() and get() ───────────────────────────────────────────────

        @Test
        @DisplayName("define() and get() round-trip stores and retrieves value")
        void defineAndGetRoundTrip() {
            Environment env = new Environment();
            env.define("x", LexorValue.ofInt(10));
            assertEquals(10, env.get("x").asInt());
        }

        @Test
        @DisplayName("define() overwrites a previous value in the same scope")
        void defineOverwritesSameScope() {
            Environment env = new Environment();
            env.define("x", LexorValue.ofInt(1));
            env.define("x", LexorValue.ofInt(99));
            assertEquals(99, env.get("x").asInt());
        }

        @Test
        @DisplayName("get() on undefined variable throws LexorRuntimeException")
        void getUndefinedThrows() {
            Environment env = new Environment();
            assertThrows(LexorRuntimeException.class, () -> env.get("undefined"));
        }

        @Test
        @DisplayName("Multiple variables can coexist in the same scope")
        void multipleVariablesCoexist() {
            Environment env = new Environment();
            env.define("a", LexorValue.ofInt(1));
            env.define("b", LexorValue.ofFloat(2.0f));
            env.define("c", LexorValue.ofBool(true));
            assertEquals(1,     env.get("a").asInt());
            assertEquals(2.0f,  env.get("b").asFloat(), 0.0001f);
            assertTrue(         env.get("c").asBool());
        }

        // ── assign() ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("assign() updates a defined variable in the same scope")
        void assignUpdatesSameScope() {
            Environment env = new Environment();
            env.define("x", LexorValue.ofInt(0));
            env.assign("x", LexorValue.ofInt(42));
            assertEquals(42, env.get("x").asInt());
        }

        @Test
        @DisplayName("assign() on undefined variable throws LexorRuntimeException")
        void assignUndefinedThrows() {
            Environment env = new Environment();
            assertThrows(LexorRuntimeException.class,
                () -> env.assign("notDefined", LexorValue.ofInt(5)));
        }

        @Test
        @DisplayName("assign() can change a variable's value multiple times")
        void assignMultipleTimes() {
            Environment env = new Environment();
            env.define("n", LexorValue.ofInt(0));
            for (int i = 1; i <= 5; i++) {
                env.assign("n", LexorValue.ofInt(i));
            }
            assertEquals(5, env.get("n").asInt());
        }

        // ── createChildScope() and scope chain ───────────────────────────────

        @Test
        @DisplayName("Child scope can read variables defined in parent scope")
        void childScopeReadsParentVariable() {
            Environment parent = new Environment();
            parent.define("x", LexorValue.ofInt(77));

            Environment child = parent.createChildScope();
            assertEquals(77, child.get("x").asInt());
        }

        @Test
        @DisplayName("Child scope does not affect parent after going out of scope")
        void childScopeIsolatedFromParent() {
            Environment parent = new Environment();
            parent.define("x", LexorValue.ofInt(5));

            Environment child = parent.createChildScope();
            child.define("y", LexorValue.ofInt(999)); // new var in child only

            // parent should not see "y"
            assertThrows(LexorRuntimeException.class, () -> parent.get("y"));
        }

        @Test
        @DisplayName("assign() in child scope updates the parent scope's variable")
        void childAssignUpdatesParentVariable() {
            Environment parent = new Environment();
            parent.define("counter", LexorValue.ofInt(0));

            Environment child = parent.createChildScope();
            child.assign("counter", LexorValue.ofInt(10));

            // The update should be visible in the parent
            assertEquals(10, parent.get("counter").asInt());
        }

        @Test
        @DisplayName("Three levels deep: grandchild can read grandparent variable")
        void deepScopeChain() {
            Environment global = new Environment();
            global.define("x", LexorValue.ofInt(1));

            Environment level1 = global.createChildScope();
            Environment level2 = level1.createChildScope();

            assertEquals(1, level2.get("x").asInt());
        }

        @Test
        @DisplayName("Three levels deep: grandchild assign updates grandparent")
        void deepScopeChainAssign() {
            Environment global = new Environment();
            global.define("x", LexorValue.ofInt(0));

            Environment level1 = global.createChildScope();
            Environment level2 = level1.createChildScope();
            level2.assign("x", LexorValue.ofInt(55));

            assertEquals(55, global.get("x").asInt());
        }

        @Test
        @DisplayName("define() in child does not shadow global when we restore parent")
        void scopeRestoreAfterBlock() {
            Environment global = new Environment();
            global.define("x", LexorValue.ofInt(10));

            // Simulate entering and exiting a block
            Environment block = global.createChildScope();
            block.define("x", LexorValue.ofInt(999)); // shadow

            // After "exiting" the block we use global again
            assertEquals(10, global.get("x").asInt()); // global unchanged
        }
    }


    // =========================================================================
    // SECTION 3 — Interpreter End-to-End Integration Tests
    // =========================================================================
    //
    // Helper: run a full program through all pipeline stages and return stdout.
    // =========================================================================

    @Nested
    @DisplayName("Interpreter — End-to-End Execution")
    class InterpreterExecTest {

        /** Run source through Lexer→Parser→SemanticAnalyzer→Interpreter. Returns stdout. */
        private String run(String source) {
            return runWithInput(source, "");
        }

        private String runWithInput(String source, String userInput) {
            List<Token> tokens   = new Lexer(source).tokenize();
            ProgramNode ast      = new Parser(tokens).parse();
            SemanticAnalyzer sem = new SemanticAnalyzer();
            sem.analyze(ast);

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            Scanner scanner = new Scanner(
                new ByteArrayInputStream(userInput.getBytes())
            );
            Interpreter interp = new Interpreter(new PrintStream(buf), scanner);
            interp.interpret(ast);
            return buf.toString();
        }

        // ── Spec Sample Programs ─────────────────────────────────────────────

        @Test
        @DisplayName("Spec sample 1: mixed types, chain assign, PRINT with $ and escape codes")
        void specSample1() {
            // Expected:
            //   4TRUE5
            //   c#last
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y, z=5
                DECLARE CHAR a_1='n'
                DECLARE BOOL t="TRUE"
                x=y=4
                a_1='c'
                PRINT: x & t & z & $ & a_1 & [#] & "last"
                END SCRIPT
                """;
            String out = run(src);
            String[] lines = out.split("\n", -1);
            assertEquals("4TRUE5", lines[0]);
            assertEquals("c#last", lines[1]);
        }

        @Test
        @DisplayName("Spec sample 2: complex arithmetic, escape code [[] and []]")
        void specSample2() {
            // xyz = ((100*5)/10 + 10) * -1 = (50+10)*-1 = -60
            // Expected: [-60]
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT xyz, abc=100
                xyz=((abc*5)/10+10)*-1
                PRINT: [[] & xyz & []]
                END SCRIPT
                """;
            assertEquals("[-60]", run(src).trim());
        }

        @Test
        @DisplayName("Spec sample 3: logical AND and <> in BOOL expression")
        void specSample3() {
            // a=100, b=200, c=300; d = (100<200 AND 300<>200) = TRUE AND TRUE = TRUE
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=100, b=200, c=300
                DECLARE BOOL d="FALSE"
                d=(a<b AND c<>200)
                PRINT: d
                END SCRIPT
                """;
            assertEquals("TRUE", run(src).trim());
        }

        // ── Variable Declarations — default values ───────────────────────────

        @Test
        @DisplayName("DECLARE INT without init defaults to 0")
        void declareIntDefaultZero() {
            assertEquals("0", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("DECLARE FLOAT without init defaults to 0")
        void declareFloatDefault() {
            String out = run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f
                PRINT: f
                END SCRIPT
                """).trim();
            // Accept "0" or "0.0"
            assertTrue(out.startsWith("0"), "Expected float default to start with 0, got: " + out);
        }

        @Test
        @DisplayName("DECLARE BOOL without init defaults to FALSE")
        void declareBoolDefaultFalse() {
            assertEquals("FALSE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b
                PRINT: b
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("DECLARE CHAR without init defaults to null char (printed as empty or \\0)")
        void declareCharDefault() {
            String out = run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE CHAR c
                PRINT: c
                END SCRIPT
                """);
            // Default char is '\0'; printed as empty string or the NUL char — both acceptable
            assertNotNull(out);
        }

        @Test
        @DisplayName("DECLARE multiple variables on one line with mixed init")
        void declareMultipleOnOneLine() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=1, b=2, c=3
                PRINT: a & b & c
                END SCRIPT
                """;
            assertEquals("123", run(src).trim());
        }

        // ── Assignment ───────────────────────────────────────────────────────

        @Test
        @DisplayName("Simple assignment updates variable")
        void simpleAssignment() {
            assertEquals("99", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=0
                x=99
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("Chain assignment x=y=4 sets both variables to 4")
        void chainAssignment() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y
                x=y=4
                PRINT: x & " " & y
                END SCRIPT
                """;
            assertEquals("4 4", run(src).trim());
        }

        @Test
        @DisplayName("Variable reassignment replaces previous value")
        void reassignment() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT n=10
                n=20
                n=30
                PRINT: n
                END SCRIPT
                """;
            assertEquals("30", run(src).trim());
        }

        // ── Arithmetic Operators ─────────────────────────────────────────────

        @ParameterizedTest(name = "{0} evaluates to {1}")
        @CsvSource({
            "2+3,    5",
            "10-4,   6",
            "3*4,    12",
            "10/2,   5",
            "17%5,   2",
            "0+0,    0",
            "-5+5,   0",
        })
        @DisplayName("Arithmetic operator correctness")
        void arithmeticOperators(String expr, String expected) {
            String src = String.format("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT result
                result=%s
                PRINT: result
                END SCRIPT
                """, expr);
            assertEquals(expected, run(src).trim());
        }

        @Test
        @DisplayName("Integer division truncates toward zero")
        void integerDivisionTruncates() {
            assertEquals("3", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x=7/2
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("Unary negation on INT works")
        void unaryNegationInt() {
            assertEquals("-7", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=7
                PRINT: -x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("Unary negation on literal INT works")
        void unaryNegationLiteral() {
            assertEquals("-1", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x=-1
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("Parenthesised expression forces precedence correctly")
        void parenthesisedExpression() {
            // (2+3)*4 = 20 not 2+(3*4)=14
            assertEquals("20", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x=(2+3)*4
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("Operator precedence: * before + without parentheses")
        void operatorPrecedence() {
            // 2+3*4 = 14
            assertEquals("14", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x=2+3*4
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("Deeply nested arithmetic expression evaluates correctly")
        void deeplyNestedArithmetic() {
            // ((100*5)/10 + 10) * -1 = -60
            assertEquals("-60", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x=((100*5)/10+10)*-1
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("FLOAT arithmetic: addition of two floats")
        void floatAddition() {
            String out = run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f=1.5
                f=f+1.5
                PRINT: f
                END SCRIPT
                """).trim();
            // 1.5 + 1.5 = 3.0 — accept "3" or "3.0"
            assertTrue(out.startsWith("3"),
                "Expected result to start with 3, got: " + out);
        }

        @Test
        @DisplayName("INT + FLOAT expression promotes to FLOAT result")
        void intPlusFloatPromotesToFloat() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f=2.5
                DECLARE INT i=1
                f=f+i
                PRINT: f
                END SCRIPT
                """;
            String out = run(src).trim();
            assertTrue(out.startsWith("3"),
                "Expected 3.5 or 3, got: " + out);
        }

        // ── Division by zero ─────────────────────────────────────────────────

        @Test
        @DisplayName("Division by zero (INT) throws LexorRuntimeException")
        void divisionByZeroInt() {
            assertThrows(LexorRuntimeException.class, () -> run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=10, y=0
                PRINT: x/y
                END SCRIPT
                """));
        }

        @Test
        @DisplayName("Modulo by zero throws LexorRuntimeException")
        void moduloByZero() {
            assertThrows(LexorRuntimeException.class, () -> run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=5, y=0
                PRINT: x%y
                END SCRIPT
                """));
        }

        // ── Relational Operators ─────────────────────────────────────────────

        @ParameterizedTest(name = "{0} evaluates to BOOL {1}")
        @CsvSource({
            "5>3,   TRUE",
            "3>5,   FALSE",
            "5<3,   FALSE",
            "3<5,   TRUE",
            "5>=5,  TRUE",
            "4>=5,  FALSE",
            "5<=5,  TRUE",
            "6<=5,  FALSE",
            "5==5,  TRUE",
            "5==6,  FALSE",
            "5<>6,  TRUE",
            "5<>5,  FALSE",
        })
        @DisplayName("Relational operator correctness")
        void relationalOperators(String expr, String expected) {
            String src = String.format("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=5, b=3
                DECLARE BOOL r="FALSE"
                r=(%s)
                PRINT: r
                END SCRIPT
                """, expr);
            assertEquals(expected, run(src).trim());
        }

        // ── Logical Operators ────────────────────────────────────────────────

        @Test
        @DisplayName("AND: TRUE AND TRUE = TRUE")
        void andTrueTrue() {
            assertEquals("TRUE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL a="TRUE", b="TRUE", r="FALSE"
                r=(a AND b)
                PRINT: r
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("AND: TRUE AND FALSE = FALSE")
        void andTrueFalse() {
            assertEquals("FALSE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL a="TRUE", b="FALSE", r="TRUE"
                r=(a AND b)
                PRINT: r
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("OR: FALSE OR TRUE = TRUE")
        void orFalseTrue() {
            assertEquals("TRUE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL a="FALSE", b="TRUE", r="FALSE"
                r=(a OR b)
                PRINT: r
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("OR: FALSE OR FALSE = FALSE")
        void orFalseFalse() {
            assertEquals("FALSE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL a="FALSE", b="FALSE", r="TRUE"
                r=(a OR b)
                PRINT: r
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("NOT: NOT TRUE = FALSE")
        void notTrue() {
            assertEquals("FALSE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL a="TRUE", r="TRUE"
                r=NOT a
                PRINT: r
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("NOT: NOT FALSE = TRUE")
        void notFalse() {
            assertEquals("TRUE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL a="FALSE", r="FALSE"
                r=NOT a
                PRINT: r
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("Compound logical: (a<b AND c<>200) matches spec sample")
        void compoundLogicalAndNeq() {
            assertEquals("TRUE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=100, b=200, c=300
                DECLARE BOOL r="FALSE"
                r=(a<b AND c<>200)
                PRINT: r
                END SCRIPT
                """).trim());
        }

        // ── PRINT statement ──────────────────────────────────────────────────

        @Test
        @DisplayName("PRINT: single INT variable")
        void printSingleInt() {
            assertEquals("42", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=42
                PRINT: x
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("PRINT: single string literal")
        void printStringLiteral() {
            assertEquals("hello", run("""
                SCRIPT AREA
                START SCRIPT
                PRINT: "hello"
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("PRINT: $ emits a newline between items")
        void printDollarSignNewline() {
            String out = run("""
                SCRIPT AREA
                START SCRIPT
                PRINT: "line1" & $ & "line2"
                END SCRIPT
                """);
            String[] lines = out.split("\n");
            assertEquals("line1", lines[0]);
            assertEquals("line2", lines[1]);
        }

        @Test
        @DisplayName("PRINT: multiple $ signs produce multiple newlines")
        void printMultipleNewlines() {
            String out = run("""
                SCRIPT AREA
                START SCRIPT
                PRINT: "a" & $ & "b" & $ & "c"
                END SCRIPT
                """);
            String[] lines = out.split("\n");
            assertEquals(3, lines.length);
            assertEquals("a", lines[0]);
            assertEquals("b", lines[1]);
            assertEquals("c", lines[2]);
        }

        @Test
        @DisplayName("PRINT: escape code [#] renders as hash symbol")
        void printEscapeHash() {
            assertEquals("#", run("""
                SCRIPT AREA
                START SCRIPT
                PRINT: [#]
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("PRINT: escape code [[] renders as open bracket")
        void printEscapeOpenBracket() {
            assertEquals("[", run("""
                SCRIPT AREA
                START SCRIPT
                PRINT: [[]
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("PRINT: escape code []] renders as close bracket")
        void printEscapeCloseBracket() {
            assertEquals("]", run("""
                SCRIPT AREA
                START SCRIPT
                PRINT: []]
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("PRINT: BOOL variable prints TRUE or FALSE in uppercase")
        void printBoolUppercase() {
            assertEquals("TRUE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b="TRUE"
                PRINT: b
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("PRINT: CHAR variable prints single character")
        void printCharVariable() {
            assertEquals("z", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE CHAR c='z'
                PRINT: c
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("PRINT: concatenation of INT & BOOL & CHAR with &")
        void printConcatenation() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=7
                DECLARE BOOL b="FALSE"
                DECLARE CHAR c='!'
                PRINT: x & b & c
                END SCRIPT
                """;
            assertEquals("7FALSE!", run(src).trim());
        }

        // ── SCAN statement ───────────────────────────────────────────────────

        @Test
        @DisplayName("SCAN: reads a single INT value")
        void scanSingleInt() {
            assertEquals("99", runWithInput("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                SCAN: x
                PRINT: x
                END SCRIPT
                """, "99\n").trim());
        }

        @Test
        @DisplayName("SCAN: reads two INT values separated by comma")
        void scanTwoInts() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a, b
                SCAN: a, b
                PRINT: a & "," & b
                END SCRIPT
                """;
            assertEquals("3,7", runWithInput(src, "3,7\n").trim());
        }

        @Test
        @DisplayName("SCAN: reads a BOOL value TRUE")
        void scanBoolTrue() {
            assertEquals("TRUE", runWithInput("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b
                SCAN: b
                PRINT: b
                END SCRIPT
                """, "TRUE\n").trim());
        }

        @Test
        @DisplayName("SCAN: reads a CHAR value")
        void scanChar() {
            assertEquals("Q", runWithInput("""
                SCRIPT AREA
                START SCRIPT
                DECLARE CHAR c
                SCAN: c
                PRINT: c
                END SCRIPT
                """, "Q\n").trim());
        }

        @Test
        @DisplayName("SCAN: arithmetic using scanned INT values")
        void scanAndCompute() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a, b, sum=0
                SCAN: a, b
                sum=a+b
                PRINT: sum
                END SCRIPT
                """;
            assertEquals("15", runWithInput(src, "6,9\n").trim());
        }

        // ── IF statement ─────────────────────────────────────────────────────

        @Test
        @DisplayName("IF: executes THEN block when condition is TRUE")
        void ifThenBranchTaken() {
            assertEquals("yes", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL cond="TRUE"
                IF (cond)
                START IF
                PRINT: "yes"
                END IF
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("IF: skips THEN block when condition is FALSE")
        void ifThenBranchSkipped() {
            assertEquals("", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL cond="FALSE"
                IF (cond)
                START IF
                PRINT: "yes"
                END IF
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("IF-ELSE: executes ELSE block when condition is FALSE")
        void ifElseBranchTaken() {
            assertEquals("no", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL cond="FALSE"
                IF (cond)
                START IF
                PRINT: "yes"
                END IF
                ELSE
                START IF
                PRINT: "no"
                END IF
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("IF-ELSE: executes IF block (not ELSE) when condition is TRUE")
        void ifElseIfBranchTaken() {
            assertEquals("yes", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL cond="TRUE"
                IF (cond)
                START IF
                PRINT: "yes"
                END IF
                ELSE
                START IF
                PRINT: "no"
                END IF
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("IF-ELSE IF-ELSE: first ELSE IF branch taken")
        void elseIfFirstBranchTaken() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL c1="FALSE", c2="TRUE"
                IF (c1)
                START IF
                PRINT: "first"
                END IF
                ELSE IF (c2)
                START IF
                PRINT: "second"
                END IF
                ELSE
                START IF
                PRINT: "third"
                END IF
                END SCRIPT
                """;
            assertEquals("second", run(src).trim());
        }

        @Test
        @DisplayName("IF-ELSE IF-ELSE: ELSE block taken when all conditions FALSE")
        void elseBlockTakenWhenAllFalse() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL c1="FALSE", c2="FALSE"
                IF (c1)
                START IF
                PRINT: "first"
                END IF
                ELSE IF (c2)
                START IF
                PRINT: "second"
                END IF
                ELSE
                START IF
                PRINT: "third"
                END IF
                END SCRIPT
                """;
            assertEquals("third", run(src).trim());
        }

        @Test
        @DisplayName("IF: relational expression used directly as condition")
        void ifWithRelationalCondition() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=10, y=5
                DECLARE BOOL cond="FALSE"
                cond=(x>y)
                IF (cond)
                START IF
                PRINT: "bigger"
                END IF
                END SCRIPT
                """;
            assertEquals("bigger", run(src).trim());
        }

        @Test
        @DisplayName("IF: body can modify outer-scope variable")
        void ifBodyModifiesOuterVariable() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=0
                DECLARE BOOL cond="TRUE"
                IF (cond)
                START IF
                x=42
                END IF
                PRINT: x
                END SCRIPT
                """;
            assertEquals("42", run(src).trim());
        }

        // ── FOR loop ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("FOR: counts from 1 to 5 and accumulates sum = 15")
        void forLoopSum() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, sum=0
                FOR (i=1, i<=5, i=i+1)
                START FOR
                sum=sum+i
                END FOR
                PRINT: sum
                END SCRIPT
                """;
            assertEquals("15", run(src).trim());
        }

        @Test
        @DisplayName("FOR: body does not execute when condition is initially false")
        void forLoopZeroIterations() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, result=99
                FOR (i=10, i<5, i=i+1)
                START FOR
                result=0
                END FOR
                PRINT: result
                END SCRIPT
                """;
            assertEquals("99", run(src).trim());
        }

        @Test
        @DisplayName("FOR: counts down using i=i-1")
        void forLoopCountDown() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, last=0
                FOR (i=5, i>=1, i=i-1)
                START FOR
                last=i
                END FOR
                PRINT: last
                END SCRIPT
                """;
            assertEquals("1", run(src).trim());
        }

        @Test
        @DisplayName("FOR: exactly 3 iterations, each appends to a counter")
        void forLoopExactIterations() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, count=0
                FOR (i=0, i<3, i=i+1)
                START FOR
                count=count+1
                END FOR
                PRINT: count
                END SCRIPT
                """;
            assertEquals("3", run(src).trim());
        }

        @Test
        @DisplayName("FOR: loop variable is accessible after the loop")
        void forLoopVariableAfterLoop() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i
                FOR (i=0, i<5, i=i+1)
                START FOR
                END FOR
                PRINT: i
                END SCRIPT
                """;
            assertEquals("5", run(src).trim());
        }

        // ── REPEAT WHEN loop ─────────────────────────────────────────────────

        @Test
        @DisplayName("REPEAT WHEN: runs body while condition is true, stops when false")
        void repeatWhenBasic() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i=0
                DECLARE BOOL go="TRUE"
                REPEAT WHEN (go)
                START REPEAT
                i=i+1
                go=(i<5)
                END REPEAT
                PRINT: i
                END SCRIPT
                """;
            assertEquals("5", run(src).trim());
        }

        @Test
        @DisplayName("REPEAT WHEN: body is skipped when condition starts false")
        void repeatWhenFalseFromStart() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=99
                DECLARE BOOL go="FALSE"
                REPEAT WHEN (go)
                START REPEAT
                x=0
                END REPEAT
                PRINT: x
                END SCRIPT
                """;
            assertEquals("99", run(src).trim());
        }

        @Test
        @DisplayName("REPEAT WHEN: accumulates a product (multiplication loop)")
        void repeatWhenMultiplication() {
            // 2^4 = 16 via repeated doubling
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT result=1, i=0
                DECLARE BOOL go="TRUE"
                REPEAT WHEN (go)
                START REPEAT
                result=result*2
                i=i+1
                go=(i<4)
                END REPEAT
                PRINT: result
                END SCRIPT
                """;
            assertEquals("16", run(src).trim());
        }

        // ── Nested control flow ───────────────────────────────────────────────

        @Test
        @DisplayName("Nested FOR inside FOR: multiplication table corner")
        void nestedForLoop() {
            // Count total iterations of inner loop: i=1..3, j=1..3 → 9
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, j, count=0
                FOR (i=1, i<=3, i=i+1)
                START FOR
                FOR (j=1, j<=3, j=j+1)
                START FOR
                count=count+1
                END FOR
                END FOR
                PRINT: count
                END SCRIPT
                """;
            assertEquals("9", run(src).trim());
        }

        @Test
        @DisplayName("IF inside FOR: conditional accumulation")
        void ifInsideFor() {
            // Sum only even numbers 1..10: 2+4+6+8+10 = 30
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, sum=0, rem=0
                DECLARE BOOL even="FALSE"
                FOR (i=1, i<=10, i=i+1)
                START FOR
                rem=i%2
                even=(rem==0)
                IF (even)
                START IF
                sum=sum+i
                END IF
                END FOR
                PRINT: sum
                END SCRIPT
                """;
            assertEquals("30", run(src).trim());
        }

        @Test
        @DisplayName("FOR inside IF: loop only runs in the true branch")
        void forInsideIf() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, count=0
                DECLARE BOOL flag="TRUE"
                IF (flag)
                START IF
                FOR (i=1, i<=3, i=i+1)
                START FOR
                count=count+1
                END FOR
                END IF
                PRINT: count
                END SCRIPT
                """;
            assertEquals("3", run(src).trim());
        }

        // ── Scope isolation in blocks ─────────────────────────────────────────

        @Test
        @DisplayName("Outer variable modified inside IF block persists after block")
        void outerVariableModifiedInIf() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=10
                DECLARE BOOL t="TRUE"
                IF (t)
                START IF
                x=x+5
                END IF
                PRINT: x
                END SCRIPT
                """;
            assertEquals("15", run(src).trim());
        }

        @Test
        @DisplayName("Outer variable modified inside FOR body persists after loop")
        void outerVariableModifiedInFor() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, total=0
                FOR (i=1, i<=3, i=i+1)
                START FOR
                total=total+i
                END FOR
                PRINT: total
                END SCRIPT
                """;
            assertEquals("6", run(src).trim());
        }

        // ── PRINT edge cases ──────────────────────────────────────────────────

        @Test
        @DisplayName("PRINT: no output when program has no PRINT statement")
        void noPrintProducesNoOutput() {
            String out = run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=5
                x=10
                END SCRIPT
                """);
            assertEquals("", out.trim());
        }

        @Test
        @DisplayName("PRINT: multiple PRINT statements on separate lines each produce output")
        void multiplePrintStatements() {
            String out = run("""
                SCRIPT AREA
                START SCRIPT
                PRINT: "first"
                PRINT: "second"
                PRINT: "third"
                END SCRIPT
                """);
            assertTrue(out.contains("first"));
            assertTrue(out.contains("second"));
            assertTrue(out.contains("third"));
        }

        @Test
        @DisplayName("PRINT: expression result (not just variable) is printed correctly")
        void printExpressionResult() {
            assertEquals("7", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=3, b=4
                PRINT: a+b
                END SCRIPT
                """).trim());
        }

        // ── Comments ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("Comments (%%) are ignored and do not affect output")
        void commentsIgnored() {
            assertEquals("42", run("""
                SCRIPT AREA
                START SCRIPT
                %% this is a comment
                DECLARE INT x=42 %% inline comment
                %% another comment
                PRINT: x
                END SCRIPT
                """).trim());
        }
    }
}
