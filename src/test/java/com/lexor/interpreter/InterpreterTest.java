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
 * Revised InterpreterTest — every test is annotated with the bug it catches.
 *
 * BUG MAP:
 *   BUG-1  LexorValue.toString() switch uses lowercase keys ("bool","int","float","char")
 *          but the type field is always uppercase ("BOOL","INT","FLOAT","CHAR").
 *          Effect: every BOOL value prints "true"/"false" instead of "TRUE"/"FALSE".
 *          Fix:    change all four switch case strings to uppercase.
 *
 *   BUG-2  asInt/asFloat/asBool/asChar throw LexorException (wrong class, wrong
 *          constructor arity) instead of LexorRuntimeException(message, line, col).
 *          Effect: compilation error; wrong exception type if it compiled.
 *          Fix:    replace LexorException(...) with LexorRuntimeException(msg, 0, 0).
 *
 *   BUG-3  getRawValue() method is missing from LexorValue.
 *          Effect: Interpreter cannot do == / <> comparisons (NullPointerException or compile error).
 *          Fix:    add public Object getRawValue() { return value; }
 *
 *   BUG-4  defaultFor(String type) static method is missing from LexorValue.
 *          Effect: uninitialised DECLARE variables throw NullPointerException.
 *          Fix:    add the method returning zero values per type.
 *
 *   BUG-5  Environment.get(String) does not walk the scope chain.
 *          It calls store.get(name) directly, returning null silently for
 *          any variable not in the immediate scope.
 *          Fix:    make get() delegate to getValue() or re-implement chain walk.
 *
 *   BUG-6  The scope-walking method is named getValue() but the Interpreter
 *          calls get(), so BUG-5's broken stub is always used.
 *          Fix:    rename getValue() to get() and delete the broken stub.
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
        @DisplayName("getType() returns the exact string passed to constructor")
        void getTypeReturnsConstructorValue() {
            assertEquals("INT",   new LexorValue("INT",   42).getType());
            assertEquals("FLOAT", new LexorValue("FLOAT", 1.0f).getType());
            assertEquals("CHAR",  new LexorValue("CHAR",  'A').getType());
            assertEquals("BOOL",  new LexorValue("BOOL",  true).getType());
        }

        // ── Static factories ─────────────────────────────────────────────────

        @Test
        @DisplayName("ofInt() creates INT LexorValue; asInt() returns stored value")
        void ofInt() {
            LexorValue v = LexorValue.ofInt(7);
            assertEquals("INT", v.getType());
            assertEquals(7, v.asInt());
        }

        @Test
        @DisplayName("ofFloat() creates FLOAT LexorValue; asFloat() returns stored value")
        void ofFloat() {
            LexorValue v = LexorValue.ofFloat(3.14f);
            assertEquals("FLOAT", v.getType());
            assertEquals(3.14f, v.asFloat(), 0.001f);
        }

        @Test
        @DisplayName("ofBool(true) creates BOOL LexorValue; asBool() returns true")
        void ofBoolTrue() {
            LexorValue v = LexorValue.ofBool(true);
            assertEquals("BOOL", v.getType());
            assertTrue(v.asBool());
        }

        @Test
        @DisplayName("ofBool(false) creates BOOL LexorValue; asBool() returns false")
        void ofBoolFalse() {
            assertFalse(LexorValue.ofBool(false).asBool());
        }

        @Test
        @DisplayName("ofChar() creates CHAR LexorValue; asChar() returns stored value")
        void ofChar() {
            LexorValue v = LexorValue.ofChar('z');
            assertEquals("CHAR", v.getType());
            assertEquals('z', v.asChar());
        }

        // ── asInt() ──────────────────────────────────────────────────────────

        @Test
        @DisplayName("asInt() returns correct value from INT LexorValue")
        void asIntFromInt() {
            assertEquals(42, LexorValue.ofInt(42).asInt());
        }

        @Test
        @DisplayName("asInt() truncates FLOAT value (9.9f → 9)")
        void asIntTruncatesFloat() {
            assertEquals(9, LexorValue.ofFloat(9.9f).asInt());
        }

        @Test
        @DisplayName("[BUG-2] asInt() on BOOL must throw LexorRuntimeException, not LexorException")
        void asIntOnBoolThrowsLexorRuntimeException() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.ofBool(true).asInt(),
                    "BUG-2: currently throws LexorException — must throw LexorRuntimeException");
        }

        @Test
        @DisplayName("[BUG-2] asInt() on CHAR must throw LexorRuntimeException")
        void asIntOnCharThrowsLexorRuntimeException() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.ofChar('A').asInt(),
                    "BUG-2: wrong exception class thrown");
        }

        // ── asFloat() ────────────────────────────────────────────────────────

        @Test
        @DisplayName("asFloat() returns correct value from FLOAT LexorValue")
        void asFloatFromFloat() {
            assertEquals(2.5f, LexorValue.ofFloat(2.5f).asFloat(), 0.0001f);
        }

        @Test
        @DisplayName("asFloat() widens INT to float (4 → 4.0f)")
        void asFloatWidensInt() {
            assertEquals(4.0f, LexorValue.ofInt(4).asFloat(), 0.0001f);
        }

        @Test
        @DisplayName("[BUG-2] asFloat() on BOOL must throw LexorRuntimeException")
        void asFloatOnBoolThrowsLexorRuntimeException() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.ofBool(false).asFloat(),
                    "BUG-2: wrong exception class");
        }

        @Test
        @DisplayName("[BUG-2] asFloat() on CHAR must throw LexorRuntimeException")
        void asFloatOnCharThrowsLexorRuntimeException() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.ofChar('B').asFloat(),
                    "BUG-2: wrong exception class");
        }

        // ── asBool() ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("asBool() returns correct value from BOOL LexorValue")
        void asBoolFromBool() {
            assertTrue(LexorValue.ofBool(true).asBool());
            assertFalse(LexorValue.ofBool(false).asBool());
        }

        @Test
        @DisplayName("[BUG-2] asBool() on INT must throw LexorRuntimeException")
        void asBoolOnIntThrowsLexorRuntimeException() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.ofInt(1).asBool(),
                    "BUG-2: wrong exception class");
        }

        @Test
        @DisplayName("[BUG-2] asBool() on FLOAT must throw LexorRuntimeException")
        void asBoolOnFloatThrowsLexorRuntimeException() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.ofFloat(1.0f).asBool(),
                    "BUG-2: wrong exception class");
        }

        // ── asChar() ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("asChar() returns correct value from CHAR LexorValue")
        void asCharFromChar() {
            assertEquals('x', LexorValue.ofChar('x').asChar());
        }

        @Test
        @DisplayName("[BUG-2] asChar() on INT must throw LexorRuntimeException")
        void asCharOnIntThrowsLexorRuntimeException() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.ofInt(65).asChar(),
                    "BUG-2: wrong exception class");
        }

        // ── toString() — BUG-1 ───────────────────────────────────────────────

        @Test
        @DisplayName("toString() on INT 42 returns '42'")
        void toStringInt() {
            assertEquals("42", LexorValue.ofInt(42).toString());
        }

        @Test
        @DisplayName("toString() on INT -5 returns '-5'")
        void toStringNegativeInt() {
            assertEquals("-5", LexorValue.ofInt(-5).toString());
        }

        @Test
        @DisplayName("[BUG-1] toString() on BOOL true must return 'TRUE' (uppercase)")
        void toStringBoolTrueMustBeUppercase() {
            assertEquals("TRUE", LexorValue.ofBool(true).toString(),
                    "BUG-1: switch key is 'bool' (lowercase) so case never matches; " +
                            "default path returns Java's 'true'. Fix: change to 'BOOL'.");
        }

        @Test
        @DisplayName("[BUG-1] toString() on BOOL false must return 'FALSE' (uppercase)")
        void toStringBoolFalseMustBeUppercase() {
            assertEquals("FALSE", LexorValue.ofBool(false).toString(),
                    "BUG-1: returns 'false' instead of 'FALSE'. Fix: uppercase switch key.");
        }

        @Test
        @DisplayName("toString() on CHAR returns single character string")
        void toStringChar() {
            assertEquals("n", LexorValue.ofChar('n').toString());
        }

        @Test
        @DisplayName("toString() on FLOAT contains the numeric digits")
        void toStringFloat() {
            String s = LexorValue.ofFloat(3.5f).toString();
            assertTrue(s.contains("3") && s.contains("5"),
                    "Expected string containing '3' and '5', got: " + s);
        }

        // ── getRawValue() — BUG-3 ────────────────────────────────────────────

        @Test
        @DisplayName("[BUG-3] getRawValue() returns the raw Object value for INT")
        void getRawValueInt() {
            // BUG-3: getRawValue() is missing — this will not compile until it is added.
            assertEquals(Integer.valueOf(10), LexorValue.ofInt(10).getRawValue());
        }

        @Test
        @DisplayName("[BUG-3] getRawValue() returns Boolean.TRUE for BOOL true")
        void getRawValueBoolTrue() {
            assertEquals(Boolean.TRUE, LexorValue.ofBool(true).getRawValue());
        }

        @Test
        @DisplayName("[BUG-3] getRawValue() returns Character for CHAR value")
        void getRawValueChar() {
            assertEquals('Q', LexorValue.ofChar('Q').getRawValue());
        }

        // ── defaultFor() — BUG-4 ─────────────────────────────────────────────

        @Test
        @DisplayName("[BUG-4] defaultFor('INT') must return INT value 0")
        void defaultForInt() {
            // BUG-4: method missing — compile error until added.
            LexorValue v = LexorValue.defaultFor("INT");
            assertEquals("INT", v.getType());
            assertEquals(0, v.asInt());
        }

        @Test
        @DisplayName("[BUG-4] defaultFor('FLOAT') must return FLOAT value 0.0")
        void defaultForFloat() {
            LexorValue v = LexorValue.defaultFor("FLOAT");
            assertEquals("FLOAT", v.getType());
            assertEquals(0.0f, v.asFloat(), 0.0001f);
        }

        @Test
        @DisplayName("[BUG-4] defaultFor('BOOL') must return BOOL false")
        void defaultForBool() {
            LexorValue v = LexorValue.defaultFor("BOOL");
            assertEquals("BOOL", v.getType());
            assertFalse(v.asBool());
        }

        @Test
        @DisplayName("[BUG-4] defaultFor('CHAR') must return CHAR null character")
        void defaultForChar() {
            LexorValue v = LexorValue.defaultFor("CHAR");
            assertEquals("CHAR", v.getType());
            assertEquals('\0', v.asChar());
        }

        @Test
        @DisplayName("[BUG-4] defaultFor() with unknown type throws LexorRuntimeException")
        void defaultForUnknownTypeThrows() {
            assertThrows(LexorRuntimeException.class,
                    () -> LexorValue.defaultFor("STRING"));
        }

        // ── equals() and hashCode() ───────────────────────────────────────────

        @Test
        @DisplayName("Two INT LexorValues with same value are equal")
        void equalsIntSameValue() {
            assertEquals(LexorValue.ofInt(10), LexorValue.ofInt(10));
        }

        @Test
        @DisplayName("Two INT LexorValues with different values are not equal")
        void equalsIntDifferentValues() {
            assertNotEquals(LexorValue.ofInt(1), LexorValue.ofInt(2));
        }

        @Test
        @DisplayName("INT and FLOAT with same numeric value are not equal — type tag differs")
        void equalsIntVsFloatDifferent() {
            assertNotEquals(LexorValue.ofInt(5), LexorValue.ofFloat(5.0f));
        }

        @Test
        @DisplayName("BOOL true and BOOL true are equal")
        void equalsBoolSameValue() {
            assertEquals(LexorValue.ofBool(true), LexorValue.ofBool(true));
        }

        @Test
        @DisplayName("BOOL true and BOOL false are not equal")
        void equalsBoolDifferentValues() {
            assertNotEquals(LexorValue.ofBool(true), LexorValue.ofBool(false));
        }

        @Test
        @DisplayName("hashCode() is consistent with equals()")
        void hashCodeConsistency() {
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
    }


    // =========================================================================
    // SECTION 2 — Environment Unit Tests
    // =========================================================================

    @Nested
    @DisplayName("Environment")
    class EnvironmentTest {

        // ── define() and get() — BUG-5 / BUG-6 ──────────────────────────────

        @Test
        @DisplayName("[BUG-5/6] define() then get() in same scope returns the value")
        void defineAndGetSameScope() {
            Environment env = new Environment();
            env.define("x", LexorValue.ofInt(10));
            LexorValue result = env.get("x");
            assertNotNull(result,
                    "BUG-5: get() returned null — broken stub calls store.get() directly without chain walk");
            assertEquals(10, result.asInt());
        }

        @Test
        @DisplayName("[BUG-5/6] get() on undefined variable throws LexorRuntimeException")
        void getUndefinedThrows() {
            Environment env = new Environment();
            assertThrows(LexorRuntimeException.class,
                    () -> env.get("notDefined"),
                    "BUG-5: broken get() returns null instead of throwing");
        }

        @Test
        @DisplayName("Multiple variables coexist in the same scope")
        void multipleVariables() {
            Environment env = new Environment();
            env.define("a", LexorValue.ofInt(1));
            env.define("b", LexorValue.ofFloat(2.0f));
            env.define("c", LexorValue.ofBool(true));
            assertEquals(1,    env.get("a").asInt());
            assertEquals(2.0f, env.get("b").asFloat(), 0.001f);
            assertTrue(        env.get("c").asBool());
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
                    () -> env.assign("ghost", LexorValue.ofInt(5)));
        }

        @Test
        @DisplayName("assign() can update the same variable multiple times")
        void assignMultipleTimes() {
            Environment env = new Environment();
            env.define("n", LexorValue.ofInt(0));
            for (int i = 1; i <= 5; i++) env.assign("n", LexorValue.ofInt(i));
            assertEquals(5, env.get("n").asInt());
        }

        // ── createChildScope() — BUG-5/6 ─────────────────────────────────────

        @Test
        @DisplayName("[BUG-5/6] Child scope get() reads variable from parent scope")
        void childReadsParentVariable() {
            Environment parent = new Environment();
            parent.define("x", LexorValue.ofInt(77));

            Environment child = parent.createChildScope();
            LexorValue result = child.get("x");
            assertNotNull(result,
                    "BUG-5/6: child.get('x') returned null — scope chain not walked");
            assertEquals(77, result.asInt());
        }

        @Test
        @DisplayName("Child variable not visible in parent after block")
        void childVariableNotInParent() {
            Environment parent = new Environment();
            Environment child  = parent.createChildScope();
            child.define("y", LexorValue.ofInt(999));
            assertThrows(LexorRuntimeException.class, () -> parent.get("y"));
        }

        @Test
        @DisplayName("[BUG-5/6] assign() in child scope updates the parent's variable")
        void childAssignUpdatesParent() {
            Environment parent = new Environment();
            parent.define("counter", LexorValue.ofInt(0));

            Environment child = parent.createChildScope();
            child.assign("counter", LexorValue.ofInt(10));

            assertEquals(10, parent.get("counter").asInt(),
                    "BUG-5/6: parent variable not updated by child assign()");
        }

        @Test
        @DisplayName("[BUG-5/6] Three levels deep: grandchild get() reaches grandparent")
        void deepChainGet() {
            Environment global = new Environment();
            global.define("x", LexorValue.ofInt(1));

            Environment l1 = global.createChildScope();
            Environment l2 = l1.createChildScope();

            LexorValue result = l2.get("x");
            assertNotNull(result, "BUG-5/6: deep chain get() returned null");
            assertEquals(1, result.asInt());
        }

        @Test
        @DisplayName("[BUG-5/6] Three levels deep: grandchild assign() updates grandparent")
        void deepChainAssign() {
            Environment global = new Environment();
            global.define("x", LexorValue.ofInt(0));

            Environment l1 = global.createChildScope();
            Environment l2 = l1.createChildScope();
            l2.assign("x", LexorValue.ofInt(55));

            assertEquals(55, global.get("x").asInt());
        }

        @Test
        @DisplayName("Restoring parent scope after child block leaves parent value intact")
        void scopeRestore() {
            Environment global = new Environment();
            global.define("x", LexorValue.ofInt(10));

            Environment block = global.createChildScope();
            block.define("x", LexorValue.ofInt(999)); // shadow in child only

            assertEquals(10, global.get("x").asInt());
        }
    }


    // =========================================================================
    // SECTION 3 — Interpreter End-to-End Integration Tests
    // =========================================================================

    @Nested
    @DisplayName("Interpreter — End-to-End Execution")
    class InterpreterExecTest {

        private String run(String source) {
            return runWithInput(source, "");
        }

        private String runWithInput(String source, String userInput) {
            List<Token>      tokens = new Lexer(source).tokenize();
            ProgramNode      ast    = new Parser(tokens).parse();
            SemanticAnalyzer sem    = new SemanticAnalyzer();
            sem.analyze(ast);

            ByteArrayOutputStream buf     = new ByteArrayOutputStream();
            Scanner               scanner = new Scanner(
                    new ByteArrayInputStream(userInput.getBytes())
            );
            Interpreter interp = new Interpreter(new PrintStream(buf), scanner);
            interp.interpret(ast);
            return buf.toString();
        }

        // ── Spec Sample Programs ─────────────────────────────────────────────

        @Test
        @DisplayName("Spec sample 1 — exact output match (BUG-1 and BUG-5/6 both affect this)")
        void specSample1() {
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
            assertEquals("4TRUE5", lines[0],
                    "Line 1: BUG-1 would give '4true5'; BUG-5/6 would give NullPointerException");
            assertEquals("c#last", lines[1]);
        }

        @Test
        @DisplayName("Spec sample 2 — complex arithmetic with unary minus")
        void specSample2() {
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
        @DisplayName("Spec sample 3 — logical AND result must print 'TRUE' (BUG-1)")
        void specSample3() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=100, b=200, c=300
                DECLARE BOOL d="FALSE"
                d=(a<b AND c<>200)
                PRINT: d
                END SCRIPT
                """;
            assertEquals("TRUE", run(src).trim(),
                    "BUG-1: BOOL toString() prints 'true' instead of 'TRUE'");
        }

        // ── Default values — BUG-4 ───────────────────────────────────────────

        @Test
        @DisplayName("[BUG-4] Uninitialised INT defaults to 0")
        void defaultInt() {
            assertEquals("0", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                PRINT: x
                END SCRIPT
                """).trim(), "BUG-4: defaultFor() missing → NullPointerException");
        }

        @Test
        @DisplayName("[BUG-4] Uninitialised BOOL defaults to FALSE (also tests BUG-1)")
        void defaultBool() {
            assertEquals("FALSE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b
                PRINT: b
                END SCRIPT
                """).trim(), "BUG-4: defaultFor() missing  OR  BUG-1: prints 'false'");
        }

        @Test
        @DisplayName("[BUG-4] Three uninitialised INTs all default to 0")
        void defaultMultipleInts() {
            assertEquals("000", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a, b, c
                PRINT: a & b & c
                END SCRIPT
                """).trim());
        }

        // ── BOOL printing — BUG-1 ────────────────────────────────────────────

        @Test
        @DisplayName("[BUG-1] BOOL initialised TRUE prints 'TRUE' (uppercase)")
        void printBoolTrueUppercase() {
            assertEquals("TRUE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b="TRUE"
                PRINT: b
                END SCRIPT
                """).trim(), "BUG-1: toString() case key wrong");
        }

        @Test
        @DisplayName("[BUG-1] BOOL initialised FALSE prints 'FALSE' (uppercase)")
        void printBoolFalseUppercase() {
            assertEquals("FALSE", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b="FALSE"
                PRINT: b
                END SCRIPT
                """).trim(), "BUG-1: toString() case key wrong");
        }

        // ── Assignment ───────────────────────────────────────────────────────

        @Test
        @DisplayName("Simple assignment updates variable correctly")
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
        @DisplayName("[BUG-5/6] Chain assignment x=y=4 sets both to 4")
        void chainAssignment() {
            assertEquals("4 4", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y
                x=y=4
                PRINT: x & " " & y
                END SCRIPT
                """).trim(), "BUG-5/6 scope chain issue may cause wrong values");
        }

        // ── Arithmetic ───────────────────────────────────────────────────────

        @ParameterizedTest(name = "INT: {0} = {1}")
        @CsvSource({"2+3,5", "10-4,6", "3*4,12", "10/2,5", "17%5,2", "-5+5,0"})
        @DisplayName("Arithmetic operators produce correct INT results")
        void intArithmetic(String expr, String expected) {
            assertEquals(expected, run(String.format("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT result
                result=%s
                PRINT: result
                END SCRIPT
                """, expr)).trim());
        }

        @Test
        @DisplayName("Parentheses override precedence: (2+3)*4 = 20")
        void parenthesisPrecedence() {
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
        @DisplayName("Deeply nested: ((100*5)/10+10)*-1 = -60")
        void deeplyNested() {
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
        @DisplayName("Division by zero throws LexorRuntimeException")
        void divisionByZero() {
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

        // ── Relational operators — BUG-1 ─────────────────────────────────────

        @ParameterizedTest(name = "Relational {0} → {1}")
        @CsvSource({
                "5>3,TRUE", "3>5,FALSE", "3<5,TRUE", "5<=5,TRUE",
                "5>=5,TRUE", "5==5,TRUE", "5==6,FALSE", "5<>6,TRUE", "5<>5,FALSE"
        })
        @DisplayName("[BUG-1] Relational operators must print uppercase TRUE/FALSE")
        void relationalOperators(String expr, String expected) {
            assertEquals(expected, run(String.format("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=5, b=3
                DECLARE BOOL r="FALSE"
                r=(%s)
                PRINT: r
                END SCRIPT
                """, expr)).trim(), "BUG-1: BOOL toString() uses lowercase switch key");
        }

        // ── Logical operators — BUG-1 ─────────────────────────────────────────

        @Test
        @DisplayName("[BUG-1] AND: TRUE AND FALSE = FALSE (uppercase)")
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
        @DisplayName("[BUG-1] OR: FALSE OR TRUE = TRUE (uppercase)")
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
        @DisplayName("[BUG-1] NOT TRUE = FALSE (uppercase)")
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

        // ── IF / ELSE — BUG-5/6 ──────────────────────────────────────────────

        @Test
        @DisplayName("IF true branch executes")
        void ifTrueBranch() {
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
        @DisplayName("IF false branch is skipped")
        void ifFalseBranchSkipped() {
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
        @DisplayName("IF-ELSE: ELSE executes when condition is FALSE")
        void ifElse() {
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
        @DisplayName("ELSE IF: correct branch taken")
        void elseIfBranch() {
            assertEquals("second", run("""
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
                """).trim());
        }

        @Test
        @DisplayName("[BUG-5/6] IF body can modify outer-scope variable")
        void ifBodyModifiesOuterVar() {
            assertEquals("42", run("""
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
                """).trim(), "BUG-5/6: assign() in child scope not reaching outer variable");
        }

        // ── FOR loop — BUG-5/6 ───────────────────────────────────────────────

        @Test
        @DisplayName("[BUG-5/6] FOR loop: sum 1..5 = 15")
        void forLoopSum() {
            assertEquals("15", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, sum=0
                FOR (i=1, i<=5, i=i+1)
                START FOR
                sum=sum+i
                END FOR
                PRINT: sum
                END SCRIPT
                """).trim(), "BUG-5/6: sum not updated across scope boundary");
        }

        @Test
        @DisplayName("FOR loop body skipped when condition starts false")
        void forZeroIterations() {
            assertEquals("99", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, result=99
                FOR (i=10, i<5, i=i+1)
                START FOR
                result=0
                END FOR
                PRINT: result
                END SCRIPT
                """).trim());
        }

        @Test
        @DisplayName("[BUG-5/6] FOR loop variable accessible after loop")
        void forVarAfterLoop() {
            assertEquals("5", run("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i
                FOR (i=0, i<5, i=i+1)
                START FOR
                END FOR
                PRINT: i
                END SCRIPT
                """).trim());
        }

        // ── REPEAT WHEN — BUG-5/6 ────────────────────────────────────────────

        @Test
        @DisplayName("[BUG-5/6] REPEAT WHEN: counts to 5")
        void repeatWhenCounts() {
            assertEquals("5", run("""
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
                """).trim(), "BUG-5/6: i and go updates not persisting across scope boundary");
        }

        @Test
        @DisplayName("REPEAT WHEN body skipped when condition starts false")
        void repeatWhenFalseStart() {
            assertEquals("99", run("""
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
                """).trim());
        }

        // ── Nested control flow — BUG-5/6 ────────────────────────────────────

        @Test
        @DisplayName("[BUG-5/6] Nested FOR: 3x3 = 9 iterations")
        void nestedFor() {
            assertEquals("9", run("""
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
                """).trim(), "BUG-5/6: 'count' not reachable from doubly-nested scope");
        }

        @Test
        @DisplayName("[BUG-5/6] IF inside FOR: sum of even numbers 2..10 = 30")
        void ifInsideFor() {
            assertEquals("30", run("""
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
                """).trim());
        }

        // ── SCAN ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("SCAN reads single INT")
        void scanInt() {
            assertEquals("42", runWithInput("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                SCAN: x
                PRINT: x
                END SCRIPT
                """, "42\n").trim());
        }

        @Test
        @DisplayName("SCAN reads two comma-separated INTs")
        void scanTwoInts() {
            assertEquals("3,7", runWithInput("""
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a, b
                SCAN: a, b
                PRINT: a & "," & b
                END SCRIPT
                """, "3,7\n").trim());
        }

        // ── Comments ─────────────────────────────────────────────────────────

        @Test
        @DisplayName("%% comments are ignored and do not affect output")
        void commentsIgnored() {
            assertEquals("42", run("""
                SCRIPT AREA
                START SCRIPT
                %% this is a comment
                DECLARE INT x=42 %% inline
                PRINT: x
                END SCRIPT
                """).trim());
        }
    }
}