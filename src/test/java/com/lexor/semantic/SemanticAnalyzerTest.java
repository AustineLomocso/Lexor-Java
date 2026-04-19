package com.lexor.semantic;

import com.lexor.error.SemanticException;
import com.lexor.parser.ast.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// =============================================================================
// SemanticAnalyzerTest.java
// src/test/java/com/lexor/semantic/SemanticAnalyzerTest.java
//
// Strategy: every test builds an AST directly — no Lexer or Parser is involved.
// This means a bug in the Parser can never cause a SemanticAnalyzer test to fail.
//
// Naming convention:
//   rNN_description_passes()  — valid program; assertDoesNotThrow
//   rNN_description_throws()  — invalid program; assertThrows(SemanticException.class)
// =============================================================================

@DisplayName("SemanticAnalyzer — Full Rule Suite")
@TestMethodOrder(OrderAnnotation.class)
class SemanticAnalyzerTest {

    // =========================================================================
    // FIXTURE
    // =========================================================================

    private SemanticAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SemanticAnalyzer();
    }

    // =========================================================================
    // HELPER FACTORY METHODS
    // Every test goes through these so constructor signatures are centralised.
    // =========================================================================

    /** Wrap declarations + statements in a root ProgramNode. */
    private ProgramNode program(List<DeclarationNode> decls, List<ASTNode> stmts) {
        return new ProgramNode(1, decls, stmts);
    }

    /** Program with declarations only (no statements). */
    private ProgramNode declsOnly(List<DeclarationNode> decls) {
        return program(decls, Collections.emptyList());
    }

    /** Program with statements only (no top-level declarations). */
    private ProgramNode stmtsOnly(List<ASTNode> stmts) {
        return program(Collections.emptyList(), stmts);
    }

    /** Declaration with NO initializer. */
    private DeclarationNode decl(String type, String name) {
        return new DeclarationNode(1, type, name, null);
    }

    /** Declaration WITH an initializer expression. */
    private DeclarationNode decl(String type, String name, ASTNode initializer) {
        return new DeclarationNode(1, type, name, initializer);
    }

    private LiteralNode intLit(int value) {
        return new LiteralNode(1, value, "INT");
    }

    private LiteralNode floatLit(float value) {
        return new LiteralNode(1, value, "FLOAT");
    }

    private LiteralNode boolLit(boolean value) {
        return new LiteralNode(1, value, "BOOL");
    }

    private LiteralNode charLit(char value) {
        return new LiteralNode(1, value, "CHAR");
    }

    private VariableNode var(String name) {
        return new VariableNode(1, name);
    }

    private BinaryExprNode binExpr(ASTNode left, String op, ASTNode right) {
        return new BinaryExprNode(1, left, op, right);
    }

    private UnaryExprNode unary(String op, ASTNode operand) {
        return new UnaryExprNode(1, op, operand);
    }

    /** Single-target assignment. */
    private AssignmentNode assign(String target, ASTNode value) {
        return new AssignmentNode(1, List.of(target), value);
    }

    /** Chained assignment:  a = b = c = value */
    private AssignmentNode assignChain(List<String> targets, ASTNode value) {
        return new AssignmentNode(1, targets, value);
    }

    private PrintNode print(List<ASTNode> segments) {
        return new PrintNode(1, segments);
    }

    private ScanNode scan(List<String> vars) {
        return new ScanNode(1, vars);
    }

    /** IF with no ELSE IF clauses and no ELSE block. */
    private IfNode ifOnly(ASTNode condition, List<ASTNode> body) {
        return new IfNode(1, condition, body, Collections.emptyList(), null);
    }

    /** IF with an ELSE block and no ELSE IF clauses. */
    private IfNode ifElse(ASTNode condition, List<ASTNode> thenBody, List<ASTNode> elseBody) {
        return new IfNode(1, condition, thenBody, Collections.emptyList(), elseBody);
    }

    /**
     * IF with one ELSE IF clause and no ELSE block.
     * Passes the ElseIfClause directly so tests can control its condition.
     */
    private IfNode ifWithElseIf(ASTNode condition,
                                List<ASTNode> thenBody,
                                IfNode.ElseIfClause elseIfClause) {
        return new IfNode(1, condition, thenBody, List.of(elseIfClause), null);
    }

    /**
     * IF with one ELSE IF clause AND an ELSE block.
     */
    private IfNode ifWithElseIfAndElse(ASTNode condition,
                                       List<ASTNode> thenBody,
                                       IfNode.ElseIfClause elseIfClause,
                                       List<ASTNode> elseBody) {
        return new IfNode(1, condition, thenBody, List.of(elseIfClause), elseBody);
    }

    private ForNode forLoop(AssignmentNode init,
                            ASTNode condition,
                            AssignmentNode update,
                            List<ASTNode> body) {
        return new ForNode(1, init, condition, update, body);
    }

    private RepeatNode repeatWhen(ASTNode condition, List<ASTNode> body) {
        return new RepeatNode(1, condition, body);
    }

    /**
     * Runs analyze() and asserts it completes without throwing.
     * All "valid program" tests call this.
     */
    private void assertValid(ProgramNode program) {
        assertDoesNotThrow(
                () -> analyzer.analyze(program),
                "Expected valid program to pass semantic analysis without errors"
        );
    }

    /**
     * Runs analyze() and asserts SemanticException is thrown.
     * Returns the exception so callers can make additional assertions on it.
     */
    private SemanticException assertInvalid(ProgramNode program) {
        return assertThrows(
                SemanticException.class,
                () -> analyzer.analyze(program),
                "Expected SemanticException but analysis passed — rule is not being enforced"
        );
    }

    // =========================================================================
    // SECTION 1 — BASELINE / SMOKE TESTS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("BASELINE — completely empty program does not throw")
    void baseline_emptyProgram_passes() {
        assertValid(program(Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    @Order(2)
    @DisplayName("BASELINE — single INT declaration without initializer passes")
    void baseline_singleIntDecl_noInit_passes() {
        assertValid(declsOnly(List.of(decl("INT", "x"))));
    }

    @Test
    @Order(3)
    @DisplayName("BASELINE — all four types declared without initializers pass")
    void baseline_allFourTypes_noInit_passes() {
        assertValid(declsOnly(List.of(
                decl("INT",   "i"),
                decl("FLOAT", "f"),
                decl("CHAR",  "c"),
                decl("BOOL",  "b")
        )));
    }

    @Test
    @Order(4)
    @DisplayName("BASELINE — single statement with declared variable passes")
    void baseline_assignDeclaredVar_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", intLit(0)))
        ));
    }

    // =========================================================================
    // SECTION 2 — R1: DUPLICATE DECLARATION
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("R1 PASS — two variables with different names are both valid")
    void r1_differentNames_passes() {
        assertValid(declsOnly(List.of(
                decl("INT", "x"),
                decl("INT", "y")
        )));
    }

    @Test
    @Order(11)
    @DisplayName("R1 FAIL — same name declared twice with same type throws")
    void r1_duplicateName_sameType_throws() {
        ProgramNode program = declsOnly(List.of(
                decl("INT", "x"),
                decl("INT", "x")
        ));
        SemanticException ex = assertInvalid(program);
        assertTrue(
                ex.getMessage().toLowerCase().contains("x"),
                "Error message must name the duplicate variable 'x', got: " + ex.getMessage()
        );
    }

    @Test
    @Order(12)
    @DisplayName("R1 FAIL — same name declared twice with DIFFERENT types throws")
    void r1_duplicateName_differentType_throws() {
        assertInvalid(declsOnly(List.of(
                decl("INT",  "x"),
                decl("BOOL", "x")
        )));
    }

    @Test
    @Order(13)
    @DisplayName("R1 FAIL — third declaration is a duplicate of the first (non-consecutive)")
    void r1_nonConsecutiveDuplicate_throws() {
        assertInvalid(declsOnly(List.of(
                decl("INT",   "a"),
                decl("FLOAT", "b"),
                decl("INT",   "a")
        )));
    }

    @Test
    @Order(14)
    @DisplayName("R1 FAIL — two separate duplicate pairs in the same program both caught")
    void r1_multipleDuplicatePairs_throws() {
        // At least one duplicate must be caught — the analyzer may stop at the first.
        assertInvalid(declsOnly(List.of(
                decl("INT",  "p"),
                decl("BOOL", "q"),
                decl("INT",  "p"),   // duplicate
                decl("BOOL", "q")    // duplicate
        )));
    }

    // =========================================================================
    // SECTION 3 — R2: UNDECLARED VARIABLE IN ASSIGNMENT TARGET
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("R2 PASS — assigning to a declared variable passes")
    void r2_assignDeclaredVar_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", intLit(5)))
        ));
    }

    @Test
    @Order(21)
    @DisplayName("R2 FAIL — assigning to a completely undeclared variable throws")
    void r2_assignUndeclaredVar_throws() {
        ProgramNode program = stmtsOnly(List.of(assign("z", intLit(5))));
        SemanticException ex = assertInvalid(program);
        assertTrue(
                ex.getMessage().toLowerCase().contains("z"),
                "Error message must name the undeclared target 'z', got: " + ex.getMessage()
        );
    }

    @Test
    @Order(22)
    @DisplayName("R2 FAIL — second statement assigns to undeclared variable (first valid)")
    void r2_secondAssignUndeclared_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(
                        assign("x", intLit(1)),       // valid
                        assign("ghost", intLit(2))    // ghost not declared
                )
        ));
    }

    // =========================================================================
    // SECTION 4 — R3: UNDECLARED VARIABLE IN EXPRESSION
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("R3 PASS — declared variable used in binary expression passes")
    void r3_declaredVarInBinaryExpr_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y")),
                List.of(assign("y", binExpr(var("x"), "+", intLit(1))))
        ));
    }

    @Test
    @Order(31)
    @DisplayName("R3 FAIL — undeclared variable on RHS of assignment throws")
    void r3_undeclaredVarOnRhs_throws() {
        assertInvalid(program(
                List.of(decl("INT", "y")),
                List.of(assign("y", binExpr(var("ghost"), "+", intLit(1))))
        ));
    }

    @Test
    @Order(32)
    @DisplayName("R3 FAIL — undeclared variable buried at depth 3 inside expression throws")
    void r3_undeclaredVarDeepNested_throws() {
        // a = (1 + (2 * phantom))  — phantom is not declared
        assertInvalid(program(
                List.of(decl("INT", "a")),
                List.of(assign("a",
                        binExpr(intLit(1), "+",
                                binExpr(intLit(2), "*", var("phantom")))))
        ));
    }

    @Test
    @Order(33)
    @DisplayName("R3 FAIL — undeclared variable used as unary operand throws")
    void r3_undeclaredVarInUnaryExpr_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", unary("-", var("missing"))))
        ));
    }

    @Test
    @Order(34)
    @DisplayName("R3 FAIL — undeclared variable in IF condition expression throws")
    void r3_undeclaredVarInIfCondition_throws() {
        // IF ghost > 0  — ghost not declared
        assertInvalid(stmtsOnly(List.of(
                ifOnly(binExpr(var("ghost"), ">", intLit(0)), Collections.emptyList())
        )));
    }

    // =========================================================================
    // SECTION 5 — R4: UNDECLARED VARIABLE IN SCAN
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("R4 PASS — SCAN with all declared variables passes")
    void r4_scan_allDeclared_passes() {
        assertValid(program(
                List.of(decl("INT", "a"), decl("FLOAT", "b")),
                List.of(scan(List.of("a", "b")))
        ));
    }

    @Test
    @Order(41)
    @DisplayName("R4 PASS — SCAN with a single declared variable passes")
    void r4_scan_singleDeclared_passes() {
        assertValid(program(
                List.of(decl("INT", "n")),
                List.of(scan(List.of("n")))
        ));
    }

    @Test
    @Order(42)
    @DisplayName("R4 FAIL — SCAN with one undeclared variable among declared ones throws")
    void r4_scan_oneUndeclared_throws() {
        assertInvalid(program(
                List.of(decl("INT", "a")),
                List.of(scan(List.of("a", "mystery")))
        ));
    }

    @Test
    @Order(43)
    @DisplayName("R4 FAIL — SCAN where every variable is undeclared throws")
    void r4_scan_allUndeclared_throws() {
        assertInvalid(stmtsOnly(List.of(scan(List.of("p", "q", "r")))));
    }

    // =========================================================================
    // SECTION 6 — R5: UNDECLARED VARIABLE IN PRINT
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("R5 PASS — PRINT with a declared variable passes")
    void r5_print_declaredVar_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(print(List.of(var("x"))))
        ));
    }

    @Test
    @Order(51)
    @DisplayName("R5 PASS — PRINT with only literals passes (no variables to check)")
    void r5_print_literalsOnly_passes() {
        assertValid(stmtsOnly(List.of(
                print(List.of(intLit(42), boolLit(true), charLit('Z'), floatLit(1.5f)))
        )));
    }

    @Test
    @Order(52)
    @DisplayName("R5 PASS — PRINT with multiple declared variables passes")
    void r5_print_multipleDeclaredVars_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("BOOL", "b")),
                List.of(print(List.of(var("x"), var("b"))))
        ));
    }

    @Test
    @Order(53)
    @DisplayName("R5 FAIL — PRINT with a completely undeclared variable throws")
    void r5_print_undeclaredVar_throws() {
        ProgramNode program = stmtsOnly(List.of(print(List.of(var("shadow")))));
        SemanticException ex = assertInvalid(program);
        assertTrue(
                ex.getMessage().toLowerCase().contains("shadow"),
                "Error must mention 'shadow', got: " + ex.getMessage()
        );
    }

    @Test
    @Order(54)
    @DisplayName("R5 FAIL — PRINT with mix of valid literal and undeclared variable throws")
    void r5_print_mixedLiteralAndUndeclared_throws() {
        assertInvalid(stmtsOnly(List.of(
                print(List.of(intLit(42), var("ghost")))
        )));
    }

    @Test
    @Order(55)
    @DisplayName("R5 FAIL — PRINT with valid var followed by undeclared var throws")
    void r5_print_validThenUndeclared_throws() {
        assertInvalid(program(
                List.of(decl("INT", "good")),
                List.of(print(List.of(var("good"), var("bad"))))
        ));
    }

    // =========================================================================
    // SECTION 7 — R6: DECLARATION INITIALIZER TYPE MISMATCH
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("R6 PASS — INT declared with INT literal initializer")
    void r6_intDecl_intLit_passes() {
        assertValid(declsOnly(List.of(decl("INT", "x", intLit(10)))));
    }

    @Test
    @Order(61)
    @DisplayName("R6 PASS — FLOAT declared with FLOAT literal initializer")
    void r6_floatDecl_floatLit_passes() {
        assertValid(declsOnly(List.of(decl("FLOAT", "f", floatLit(3.14f)))));
    }

    @Test
    @Order(62)
    @DisplayName("R6 PASS — BOOL declared with BOOL literal (true) initializer")
    void r6_boolDecl_boolLitTrue_passes() {
        assertValid(declsOnly(List.of(decl("BOOL", "flag", boolLit(true)))));
    }

    @Test
    @Order(63)
    @DisplayName("R6 PASS — BOOL declared with BOOL literal (false) initializer")
    void r6_boolDecl_boolLitFalse_passes() {
        assertValid(declsOnly(List.of(decl("BOOL", "flag", boolLit(false)))));
    }

    @Test
    @Order(64)
    @DisplayName("R6 PASS — CHAR declared with CHAR literal initializer")
    void r6_charDecl_charLit_passes() {
        assertValid(declsOnly(List.of(decl("CHAR", "c", charLit('A')))));
    }

    @Test
    @Order(65)
    @DisplayName("R6 FAIL — INT declared with BOOL literal throws")
    void r6_intDecl_boolLit_throws() {
        assertInvalid(declsOnly(List.of(decl("INT", "x", boolLit(true)))));
    }

    @Test
    @Order(66)
    @DisplayName("R6 FAIL — INT declared with FLOAT literal throws")
    void r6_intDecl_floatLit_throws() {
        assertInvalid(declsOnly(List.of(decl("INT", "x", floatLit(3.14f)))));
    }

    @Test
    @Order(67)
    @DisplayName("R6 FAIL — INT declared with CHAR literal throws")
    void r6_intDecl_charLit_throws() {
        assertInvalid(declsOnly(List.of(decl("INT", "x", charLit('Z')))));
    }

    @Test
    @Order(68)
    @DisplayName("R6 FAIL — BOOL declared with INT literal throws")
    void r6_boolDecl_intLit_throws() {
        assertInvalid(declsOnly(List.of(decl("BOOL", "flag", intLit(1)))));
    }

    @Test
    @Order(69)
    @DisplayName("R6 FAIL — BOOL declared with FLOAT literal throws")
    void r6_boolDecl_floatLit_throws() {
        assertInvalid(declsOnly(List.of(decl("BOOL", "b", floatLit(0.0f)))));
    }

    @Test
    @Order(70)
    @DisplayName("R6 FAIL — BOOL declared with CHAR literal throws")
    void r6_boolDecl_charLit_throws() {
        assertInvalid(declsOnly(List.of(decl("BOOL", "b", charLit('T')))));
    }

    @Test
    @Order(71)
    @DisplayName("R6 FAIL — CHAR declared with INT literal throws")
    void r6_charDecl_intLit_throws() {
        assertInvalid(declsOnly(List.of(decl("CHAR", "c", intLit(65)))));
    }

    @Test
    @Order(72)
    @DisplayName("R6 FAIL — CHAR declared with BOOL literal throws")
    void r6_charDecl_boolLit_throws() {
        assertInvalid(declsOnly(List.of(decl("CHAR", "c", boolLit(false)))));
    }

    @Test
    @Order(73)
    @DisplayName("R6 FAIL — FLOAT declared with BOOL literal throws")
    void r6_floatDecl_boolLit_throws() {
        assertInvalid(declsOnly(List.of(decl("FLOAT", "f", boolLit(true)))));
    }

    @Test
    @Order(74)
    @DisplayName("R6 FAIL — FLOAT declared with CHAR literal throws")
    void r6_floatDecl_charLit_throws() {
        assertInvalid(declsOnly(List.of(decl("FLOAT", "f", charLit('X')))));
    }

    // =========================================================================
    // SECTION 8 — R7: ASSIGNMENT VALUE TYPE MISMATCH
    // =========================================================================

    @Test
    @Order(80)
    @DisplayName("R7 PASS — INT variable assigned INT literal")
    void r7_intAssign_intLit_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", intLit(42)))
        ));
    }

    @Test
    @Order(81)
    @DisplayName("R7 PASS — FLOAT variable assigned FLOAT literal")
    void r7_floatAssign_floatLit_passes() {
        assertValid(program(
                List.of(decl("FLOAT", "f")),
                List.of(assign("f", floatLit(2.718f)))
        ));
    }

    @Test
    @Order(82)
    @DisplayName("R7 PASS — BOOL variable assigned BOOL literal true")
    void r7_boolAssign_boolLitTrue_passes() {
        assertValid(program(
                List.of(decl("BOOL", "b")),
                List.of(assign("b", boolLit(true)))
        ));
    }

    @Test
    @Order(83)
    @DisplayName("R7 PASS — BOOL variable assigned BOOL literal false")
    void r7_boolAssign_boolLitFalse_passes() {
        assertValid(program(
                List.of(decl("BOOL", "b")),
                List.of(assign("b", boolLit(false)))
        ));
    }

    @Test
    @Order(84)
    @DisplayName("R7 PASS — CHAR variable assigned CHAR literal")
    void r7_charAssign_charLit_passes() {
        assertValid(program(
                List.of(decl("CHAR", "c")),
                List.of(assign("c", charLit('Q')))
        ));
    }

    @Test
    @Order(85)
    @DisplayName("R7 PASS — INT variable assigned result of INT+INT binary expression")
    void r7_intAssign_intPlusIntExpr_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y")),
                List.of(assign("y", binExpr(var("x"), "+", intLit(1))))
        ));
    }

    @Test
    @Order(86)
    @DisplayName("R7 PASS — BOOL variable assigned result of relational expression")
    void r7_boolAssign_relationalExpr_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("BOOL", "result")),
                List.of(assign("result", binExpr(var("x"), ">", intLit(0))))
        ));
    }

    @Test
    @Order(87)
    @DisplayName("R7 FAIL — INT variable assigned BOOL literal throws")
    void r7_intAssign_boolLit_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", boolLit(true)))
        ));
    }

    @Test
    @Order(88)
    @DisplayName("R7 FAIL — INT variable assigned FLOAT literal throws")
    void r7_intAssign_floatLit_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", floatLit(3.14f)))
        ));
    }

    @Test
    @Order(89)
    @DisplayName("R7 FAIL — INT variable assigned CHAR literal throws")
    void r7_intAssign_charLit_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", charLit('A')))
        ));
    }

    @Test
    @Order(90)
    @DisplayName("R7 FAIL — BOOL variable assigned INT literal throws")
    void r7_boolAssign_intLit_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "b")),
                List.of(assign("b", intLit(0)))
        ));
    }

    @Test
    @Order(91)
    @DisplayName("R7 FAIL — BOOL variable assigned FLOAT literal throws")
    void r7_boolAssign_floatLit_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "b")),
                List.of(assign("b", floatLit(1.0f)))
        ));
    }

    @Test
    @Order(92)
    @DisplayName("R7 FAIL — BOOL variable assigned CHAR literal throws")
    void r7_boolAssign_charLit_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "b")),
                List.of(assign("b", charLit('T')))
        ));
    }

    @Test
    @Order(93)
    @DisplayName("R7 FAIL — CHAR variable assigned INT literal throws")
    void r7_charAssign_intLit_throws() {
        assertInvalid(program(
                List.of(decl("CHAR", "c")),
                List.of(assign("c", intLit(65)))
        ));
    }

    @Test
    @Order(94)
    @DisplayName("R7 FAIL — FLOAT variable assigned BOOL literal throws")
    void r7_floatAssign_boolLit_throws() {
        assertInvalid(program(
                List.of(decl("FLOAT", "f")),
                List.of(assign("f", boolLit(false)))
        ));
    }

    @Test
    @Order(95)
    @DisplayName("R7 FAIL — INT variable assigned BOOL expression (AND result) throws")
    void r7_intAssign_boolExprResult_throws() {
        // b AND TRUE produces BOOL — cannot be stored in INT
        assertInvalid(program(
                List.of(decl("BOOL", "b"), decl("INT", "x")),
                List.of(assign("x", binExpr(var("b"), "AND", boolLit(true))))
        ));
    }

    // =========================================================================
    // SECTION 9 — R8: ARITHMETIC OPERATORS REQUIRE NUMERIC OPERANDS
    // =========================================================================

    @ParameterizedTest(name = "R8 PASS — INT {0} INT is valid arithmetic")
    @Order(100)
    @ValueSource(strings = {"+", "-", "*", "/", "%"})
    void r8_intArithmetic_int_passes(String op) {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y"), decl("INT", "z")),
                List.of(assign("z", binExpr(var("x"), op, var("y"))))
        ));
    }

    @ParameterizedTest(name = "R8 PASS — FLOAT {0} FLOAT is valid arithmetic")
    @Order(101)
    @ValueSource(strings = {"+", "-", "*", "/"})
    void r8_floatArithmetic_float_passes(String op) {
        assertValid(program(
                List.of(decl("FLOAT", "a"), decl("FLOAT", "b"), decl("FLOAT", "c")),
                List.of(assign("c", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R8 FAIL — BOOL {0} BOOL in arithmetic throws")
    @Order(102)
    @ValueSource(strings = {"+", "-", "*", "/", "%"})
    void r8_boolArithmetic_bool_throws(String op) {
        assertInvalid(program(
                List.of(decl("BOOL", "p"), decl("BOOL", "q"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("p"), op, var("q"))))
        ));
    }

    @ParameterizedTest(name = "R8 FAIL — CHAR {0} CHAR in arithmetic throws")
    @Order(103)
    @ValueSource(strings = {"+", "-", "*", "/"})
    void r8_charArithmetic_char_throws(String op) {
        assertInvalid(program(
                List.of(decl("CHAR", "a"), decl("CHAR", "b"), decl("INT", "c")),
                List.of(assign("c", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R8 FAIL — INT {0} BOOL (mixed) in arithmetic throws")
    @Order(104)
    @ValueSource(strings = {"+", "-", "*", "/"})
    void r8_intArithmetic_bool_throws(String op) {
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "b"), decl("INT", "r")),
                List.of(assign("r", binExpr(var("x"), op, var("b"))))
        ));
    }

    // =========================================================================
    // SECTION 10 — R9: RELATIONAL OPERATORS REQUIRE NUMERIC OPERANDS
    // =========================================================================

    @ParameterizedTest(name = "R9 PASS — INT {0} INT is valid relational")
    @Order(110)
    @ValueSource(strings = {"<", ">", "<=", ">="})
    void r9_intRelational_int_passes(String op) {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y"), decl("BOOL", "result")),
                List.of(assign("result", binExpr(var("x"), op, var("y"))))
        ));
    }

    @ParameterizedTest(name = "R9 PASS — FLOAT {0} FLOAT is valid relational")
    @Order(111)
    @ValueSource(strings = {"<", ">", "<=", ">="})
    void r9_floatRelational_float_passes(String op) {
        assertValid(program(
                List.of(decl("FLOAT", "a"), decl("FLOAT", "b"), decl("BOOL", "result")),
                List.of(assign("result", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R9 FAIL — BOOL {0} BOOL in relational throws")
    @Order(112)
    @ValueSource(strings = {"<", ">", "<=", ">="})
    void r9_boolRelational_bool_throws(String op) {
        assertInvalid(program(
                List.of(decl("BOOL", "p"), decl("BOOL", "q"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("p"), op, var("q"))))
        ));
    }

    @ParameterizedTest(name = "R9 FAIL — CHAR {0} CHAR in relational throws")
    @Order(113)
    @ValueSource(strings = {"<", ">", "<=", ">="})
    void r9_charRelational_char_throws(String op) {
        assertInvalid(program(
                List.of(decl("CHAR", "a"), decl("CHAR", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("a"), op, var("b"))))
        ));
    }

    // =========================================================================
    // SECTION 11 — R10: EQUALITY OPERATORS REQUIRE SAME-TYPE OPERANDS
    // =========================================================================

    @ParameterizedTest(name = "R10 PASS — INT {0} INT equality is valid")
    @Order(120)
    @ValueSource(strings = {"==", "<>"})
    void r10_intEquality_int_passes(String op) {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("x"), op, var("y"))))
        ));
    }

    @ParameterizedTest(name = "R10 PASS — BOOL {0} BOOL equality is valid")
    @Order(121)
    @ValueSource(strings = {"==", "<>"})
    void r10_boolEquality_bool_passes(String op) {
        assertValid(program(
                List.of(decl("BOOL", "a"), decl("BOOL", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R10 PASS — FLOAT {0} FLOAT equality is valid")
    @Order(122)
    @ValueSource(strings = {"==", "<>"})
    void r10_floatEquality_float_passes(String op) {
        assertValid(program(
                List.of(decl("FLOAT", "a"), decl("FLOAT", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R10 PASS — CHAR {0} CHAR equality is valid")
    @Order(123)
    @ValueSource(strings = {"==", "<>"})
    void r10_charEquality_char_passes(String op) {
        assertValid(program(
                List.of(decl("CHAR", "a"), decl("CHAR", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R10 FAIL — INT {0} BOOL cross-type equality throws")
    @Order(124)
    @ValueSource(strings = {"==", "<>"})
    void r10_intEquality_bool_throws(String op) {
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("x"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R10 FAIL — INT {0} CHAR cross-type equality throws")
    @Order(125)
    @ValueSource(strings = {"==", "<>"})
    void r10_intEquality_char_throws(String op) {
        assertInvalid(program(
                List.of(decl("INT", "n"), decl("CHAR", "c"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("n"), op, var("c"))))
        ));
    }

    @ParameterizedTest(name = "R10 FAIL — FLOAT {0} BOOL cross-type equality throws")
    @Order(126)
    @ValueSource(strings = {"==", "<>"})
    void r10_floatEquality_bool_throws(String op) {
        assertInvalid(program(
                List.of(decl("FLOAT", "f"), decl("BOOL", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("f"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R10 FAIL — BOOL {0} CHAR cross-type equality throws")
    @Order(127)
    @ValueSource(strings = {"==", "<>"})
    void r10_boolEquality_char_throws(String op) {
        assertInvalid(program(
                List.of(decl("BOOL", "b"), decl("CHAR", "c"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("b"), op, var("c"))))
        ));
    }

    // =========================================================================
    // SECTION 12 — R11: LOGICAL OPERATORS REQUIRE BOOL OPERANDS
    // =========================================================================

    @ParameterizedTest(name = "R11 PASS — BOOL {0} BOOL is valid logical")
    @Order(130)
    @ValueSource(strings = {"AND", "OR"})
    void r11_boolLogical_bool_passes(String op) {
        assertValid(program(
                List.of(decl("BOOL", "a"), decl("BOOL", "b"), decl("BOOL", "result")),
                List.of(assign("result", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R11 FAIL — INT {0} INT in logical throws")
    @Order(131)
    @ValueSource(strings = {"AND", "OR"})
    void r11_intLogical_int_throws(String op) {
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("INT", "y"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("x"), op, var("y"))))
        ));
    }

    @ParameterizedTest(name = "R11 FAIL — FLOAT {0} FLOAT in logical throws")
    @Order(132)
    @ValueSource(strings = {"AND", "OR"})
    void r11_floatLogical_float_throws(String op) {
        assertInvalid(program(
                List.of(decl("FLOAT", "a"), decl("FLOAT", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("a"), op, var("b"))))
        ));
    }

    @ParameterizedTest(name = "R11 FAIL — BOOL {0} INT (mixed) in logical throws")
    @Order(133)
    @ValueSource(strings = {"AND", "OR"})
    void r11_boolLogical_int_mixed_throws(String op) {
        assertInvalid(program(
                List.of(decl("BOOL", "b"), decl("INT", "x"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("b"), op, var("x"))))
        ));
    }

    @ParameterizedTest(name = "R11 FAIL — INT {0} BOOL (mixed, swapped) in logical throws")
    @Order(134)
    @ValueSource(strings = {"AND", "OR"})
    void r11_intLogical_bool_mixedSwapped_throws(String op) {
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "b"), decl("BOOL", "r")),
                List.of(assign("r", binExpr(var("x"), op, var("b"))))
        ));
    }

    // =========================================================================
    // SECTION 13 — R12: UNARY MINUS REQUIRES NUMERIC OPERAND
    // =========================================================================

    @Test
    @Order(140)
    @DisplayName("R12 PASS — unary minus on INT variable passes")
    void r12_unaryMinus_int_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y")),
                List.of(assign("y", unary("-", var("x"))))
        ));
    }

    @Test
    @Order(141)
    @DisplayName("R12 PASS — unary minus on FLOAT variable passes")
    void r12_unaryMinus_float_passes() {
        assertValid(program(
                List.of(decl("FLOAT", "f"), decl("FLOAT", "g")),
                List.of(assign("g", unary("-", var("f"))))
        ));
    }

    @Test
    @Order(142)
    @DisplayName("R12 PASS — unary minus on INT literal passes")
    void r12_unaryMinus_intLit_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", unary("-", intLit(5))))
        ));
    }

    @Test
    @Order(143)
    @DisplayName("R12 FAIL — unary minus on BOOL variable throws")
    void r12_unaryMinus_bool_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "b"), decl("INT", "x")),
                List.of(assign("x", unary("-", var("b"))))
        ));
    }

    @Test
    @Order(144)
    @DisplayName("R12 FAIL — unary minus on CHAR variable throws")
    void r12_unaryMinus_char_throws() {
        assertInvalid(program(
                List.of(decl("CHAR", "c"), decl("INT", "x")),
                List.of(assign("x", unary("-", var("c"))))
        ));
    }

    @Test
    @Order(145)
    @DisplayName("R12 FAIL — unary minus on BOOL literal throws")
    void r12_unaryMinus_boolLit_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", unary("-", boolLit(true))))
        ));
    }

    // =========================================================================
    // SECTION 14 — R13: NOT REQUIRES BOOL OPERAND
    // =========================================================================

    @Test
    @Order(150)
    @DisplayName("R13 PASS — NOT on BOOL variable passes")
    void r13_not_bool_passes() {
        assertValid(program(
                List.of(decl("BOOL", "flag"), decl("BOOL", "result")),
                List.of(assign("result", unary("NOT", var("flag"))))
        ));
    }

    @Test
    @Order(151)
    @DisplayName("R13 PASS — NOT on BOOL literal passes")
    void r13_not_boolLit_passes() {
        assertValid(program(
                List.of(decl("BOOL", "r")),
                List.of(assign("r", unary("NOT", boolLit(false))))
        ));
    }

    @Test
    @Order(152)
    @DisplayName("R13 PASS — NOT on a relational expression (BOOL result) passes")
    void r13_not_relationalExpr_passes() {
        // NOT (x > 0)  — the relational produces BOOL, so NOT is valid
        assertValid(program(
                List.of(decl("INT", "x"), decl("BOOL", "r")),
                List.of(assign("r", unary("NOT", binExpr(var("x"), ">", intLit(0)))))
        ));
    }

    @Test
    @Order(153)
    @DisplayName("R13 FAIL — NOT on INT variable throws")
    void r13_not_int_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "r")),
                List.of(assign("r", unary("NOT", var("x"))))
        ));
    }

    @Test
    @Order(154)
    @DisplayName("R13 FAIL — NOT on FLOAT variable throws")
    void r13_not_float_throws() {
        assertInvalid(program(
                List.of(decl("FLOAT", "f"), decl("BOOL", "r")),
                List.of(assign("r", unary("NOT", var("f"))))
        ));
    }

    @Test
    @Order(155)
    @DisplayName("R13 FAIL — NOT on CHAR variable throws")
    void r13_not_char_throws() {
        assertInvalid(program(
                List.of(decl("CHAR", "c"), decl("BOOL", "r")),
                List.of(assign("r", unary("NOT", var("c"))))
        ));
    }

    @Test
    @Order(156)
    @DisplayName("R13 FAIL — NOT on INT literal throws")
    void r13_not_intLit_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "r")),
                List.of(assign("r", unary("NOT", intLit(0))))
        ));
    }

    @Test
    @Order(157)
    @DisplayName("R13 FAIL — NOT on arithmetic expression (INT result) throws")
    void r13_not_arithmeticExpr_throws() {
        // NOT (x + 1)  — x+1 is INT, not BOOL
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "r")),
                List.of(assign("r", unary("NOT", binExpr(var("x"), "+", intLit(1)))))
        ));
    }

    // =========================================================================
    // SECTION 15 — R14: IF CONDITION MUST BE BOOL
    // =========================================================================

    @Test
    @Order(160)
    @DisplayName("R14 PASS — IF condition is a BOOL variable")
    void r14_if_boolVar_passes() {
        assertValid(program(
                List.of(decl("BOOL", "flag")),
                List.of(ifOnly(var("flag"), Collections.emptyList()))
        ));
    }

    @Test
    @Order(161)
    @DisplayName("R14 PASS — IF condition is a BOOL literal TRUE")
    void r14_if_boolLitTrue_passes() {
        assertValid(stmtsOnly(List.of(
                ifOnly(boolLit(true), Collections.emptyList())
        )));
    }

    @Test
    @Order(162)
    @DisplayName("R14 PASS — IF condition is a relational binary expression")
    void r14_if_relationalExpr_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y")),
                List.of(ifOnly(binExpr(var("x"), ">", var("y")), Collections.emptyList()))
        ));
    }

    @Test
    @Order(163)
    @DisplayName("R14 PASS — IF condition is a logical AND of two comparisons")
    void r14_if_logicalAndExpr_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y")),
                List.of(ifOnly(
                        binExpr(
                                binExpr(var("x"), ">", intLit(0)),
                                "AND",
                                binExpr(var("y"), ">", intLit(0))
                        ),
                        Collections.emptyList()
                ))
        ));
    }

    @Test
    @Order(164)
    @DisplayName("R14 FAIL — IF condition is an INT variable throws")
    void r14_if_intVar_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(ifOnly(var("x"), Collections.emptyList()))
        ));
    }

    @Test
    @Order(165)
    @DisplayName("R14 FAIL — IF condition is a FLOAT literal throws")
    void r14_if_floatLit_throws() {
        assertInvalid(stmtsOnly(List.of(
                ifOnly(floatLit(1.0f), Collections.emptyList())
        )));
    }

    @Test
    @Order(166)
    @DisplayName("R14 FAIL — IF condition is a CHAR variable throws")
    void r14_if_charVar_throws() {
        assertInvalid(program(
                List.of(decl("CHAR", "c")),
                List.of(ifOnly(var("c"), Collections.emptyList()))
        ));
    }

    @Test
    @Order(167)
    @DisplayName("R14 FAIL — IF condition is an arithmetic expression (INT result) throws")
    void r14_if_arithmeticExpr_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(ifOnly(binExpr(var("x"), "+", intLit(1)), Collections.emptyList()))
        ));
    }

    // =========================================================================
    // SECTION 16 — R15: ELSE IF CONDITION MUST BE BOOL
    // =========================================================================

    @Test
    @Order(170)
    @DisplayName("R15 PASS — ELSE IF condition is a BOOL variable")
    void r15_elseIf_boolVar_passes() {
        // IF TRUE ... ELSE IF boolVar ... END IF
        IfNode.ElseIfClause clause = new IfNode.ElseIfClause(
                var("extra"),
                Collections.emptyList()
        );
        assertValid(program(
                List.of(decl("BOOL", "extra")),
                List.of(ifWithElseIf(boolLit(true), Collections.emptyList(), clause))
        ));
    }

    @Test
    @Order(171)
    @DisplayName("R15 PASS — ELSE IF condition is a relational expression")
    void r15_elseIf_relationalExpr_passes() {
        // IF TRUE ... ELSE IF (x > 5) ... END IF
        IfNode.ElseIfClause clause = new IfNode.ElseIfClause(
                binExpr(var("x"), ">", intLit(5)),
                Collections.emptyList()
        );
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(ifWithElseIf(boolLit(true), Collections.emptyList(), clause))
        ));
    }

    @Test
    @Order(172)
    @DisplayName("R15 PASS — ELSE IF with valid body statements passes")
    void r15_elseIf_validBody_passes() {
        IfNode.ElseIfClause clause = new IfNode.ElseIfClause(
                var("flag"),
                List.of(assign("x", intLit(99)))
        );
        assertValid(program(
                List.of(decl("BOOL", "flag"), decl("INT", "x")),
                List.of(ifWithElseIf(boolLit(false), Collections.emptyList(), clause))
        ));
    }

    @Test
    @Order(173)
    @DisplayName("R15 FAIL — ELSE IF condition is INT variable throws")
    void r15_elseIf_intVar_throws() {
        IfNode.ElseIfClause badClause = new IfNode.ElseIfClause(
                var("x"),
                Collections.emptyList()
        );
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(ifWithElseIf(boolLit(true), Collections.emptyList(), badClause))
        ));
    }

    @Test
    @Order(174)
    @DisplayName("R15 FAIL — ELSE IF condition is a FLOAT literal throws")
    void r15_elseIf_floatLit_throws() {
        IfNode.ElseIfClause badClause = new IfNode.ElseIfClause(
                floatLit(2.5f),
                Collections.emptyList()
        );
        assertInvalid(stmtsOnly(List.of(
                ifWithElseIf(boolLit(true), Collections.emptyList(), badClause)
        )));
    }

    @Test
    @Order(175)
    @DisplayName("R15 FAIL — type error inside ELSE IF body is caught")
    void r15_elseIf_typeErrorInBody_throws() {
        // ELSE IF TRUE body has: intVar = TRUE  — type mismatch inside the body
        IfNode.ElseIfClause badClause = new IfNode.ElseIfClause(
                boolLit(true),
                List.of(assign("intVar", boolLit(true)))   // INT = BOOL
        );
        assertInvalid(program(
                List.of(decl("INT", "intVar")),
                List.of(ifWithElseIf(boolLit(false), Collections.emptyList(), badClause))
        ));
    }

    // =========================================================================
    // SECTION 17 — R16: FOR CONDITION MUST BE BOOL
    // =========================================================================

    @Test
    @Order(180)
    @DisplayName("R16 PASS — FOR with relational condition (i < 10) passes")
    void r16_for_relationalCondition_passes() {
        assertValid(program(
                List.of(decl("INT", "i")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        binExpr(var("i"), "<", intLit(10)),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        Collections.emptyList()
                ))
        ));
    }

    @Test
    @Order(181)
    @DisplayName("R16 PASS — FOR with BOOL variable condition passes")
    void r16_for_boolVarCondition_passes() {
        assertValid(program(
                List.of(decl("INT", "i"), decl("BOOL", "running")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        var("running"),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        Collections.emptyList()
                ))
        ));
    }

    @Test
    @Order(182)
    @DisplayName("R16 FAIL — FOR condition is INT variable throws")
    void r16_for_intVarCondition_throws() {
        assertInvalid(program(
                List.of(decl("INT", "i")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        var("i"),   // INT, not BOOL
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        Collections.emptyList()
                ))
        ));
    }

    @Test
    @Order(183)
    @DisplayName("R16 FAIL — FOR condition is arithmetic expression throws")
    void r16_for_arithmeticCondition_throws() {
        // i + 1 produces INT — not a valid loop condition
        assertInvalid(program(
                List.of(decl("INT", "i")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        binExpr(var("i"), "+", intLit(1)),   // INT result
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        Collections.emptyList()
                ))
        ));
    }

    @Test
    @Order(184)
    @DisplayName("R16 FAIL — FOR condition is FLOAT literal throws")
    void r16_for_floatLitCondition_throws() {
        assertInvalid(program(
                List.of(decl("INT", "i")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        floatLit(1.0f),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        Collections.emptyList()
                ))
        ));
    }

    // =========================================================================
    // SECTION 18 — R17: REPEAT WHEN CONDITION MUST BE BOOL
    // =========================================================================

    @Test
    @Order(190)
    @DisplayName("R17 PASS — REPEAT WHEN condition is BOOL variable passes")
    void r17_repeat_boolVarCondition_passes() {
        assertValid(program(
                List.of(decl("BOOL", "keepGoing")),
                List.of(repeatWhen(var("keepGoing"), Collections.emptyList()))
        ));
    }

    @Test
    @Order(191)
    @DisplayName("R17 PASS — REPEAT WHEN condition is relational expression passes")
    void r17_repeat_relationalCondition_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(repeatWhen(binExpr(var("x"), "<>", intLit(0)), Collections.emptyList()))
        ));
    }

    @Test
    @Order(192)
    @DisplayName("R17 PASS — REPEAT WHEN condition is BOOL literal false passes")
    void r17_repeat_boolLitFalse_passes() {
        // A loop that runs once and stops — syntactically and semantically valid.
        assertValid(stmtsOnly(List.of(
                repeatWhen(boolLit(false), Collections.emptyList())
        )));
    }

    @Test
    @Order(193)
    @DisplayName("R17 FAIL — REPEAT WHEN condition is INT variable throws")
    void r17_repeat_intVarCondition_throws() {
        assertInvalid(program(
                List.of(decl("INT", "count")),
                List.of(repeatWhen(var("count"), Collections.emptyList()))
        ));
    }

    @Test
    @Order(194)
    @DisplayName("R17 FAIL — REPEAT WHEN condition is FLOAT literal throws")
    void r17_repeat_floatLitCondition_throws() {
        assertInvalid(stmtsOnly(List.of(
                repeatWhen(floatLit(1.0f), Collections.emptyList())
        )));
    }

    @Test
    @Order(195)
    @DisplayName("R17 FAIL — REPEAT WHEN condition is CHAR variable throws")
    void r17_repeat_charVarCondition_throws() {
        assertInvalid(program(
                List.of(decl("CHAR", "c")),
                List.of(repeatWhen(var("c"), Collections.emptyList()))
        ));
    }

    @Test
    @Order(196)
    @DisplayName("R17 FAIL — REPEAT WHEN condition is arithmetic expression throws")
    void r17_repeat_arithmeticCondition_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(repeatWhen(binExpr(var("x"), "-", intLit(1)), Collections.emptyList()))
        ));
    }

    // =========================================================================
    // SECTION 19 — R18 & R19: FOR INIT AND UPDATE TARGETS MUST BE DECLARED
    // =========================================================================

    @Test
    @Order(200)
    @DisplayName("R18 FAIL — FOR init assigns to undeclared variable throws")
    void r18_forInit_undeclaredTarget_throws() {
        // FOR z = 0 ; ...  — z never declared
        assertInvalid(stmtsOnly(List.of(
                forLoop(
                        assign("z", intLit(0)),
                        boolLit(true),
                        assign("z", intLit(1)),
                        Collections.emptyList()
                )
        )));
    }

    @Test
    @Order(201)
    @DisplayName("R19 FAIL — FOR update assigns to undeclared variable throws")
    void r19_forUpdate_undeclaredTarget_throws() {
        // DECLARE INT i — but update writes to 'ghost'
        assertInvalid(program(
                List.of(decl("INT", "i")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        binExpr(var("i"), "<", intLit(5)),
                        assign("ghost", binExpr(var("i"), "+", intLit(1))),
                        Collections.emptyList()
                ))
        ));
    }

    @Test
    @Order(202)
    @DisplayName("R18 FAIL — FOR init type mismatch (BOOL var assigned INT) throws")
    void r18_forInit_typeMismatch_throws() {
        // DECLARE BOOL b — init tries b = 0 (BOOL = INT)
        assertInvalid(program(
                List.of(decl("BOOL", "b")),
                List.of(forLoop(
                        assign("b", intLit(0)),              // type mismatch
                        boolLit(true),
                        assign("b", boolLit(false)),
                        Collections.emptyList()
                ))
        ));
    }

    // =========================================================================
    // SECTION 20 — R20: NESTED EXPRESSIONS ARE CHECKED RECURSIVELY
    // =========================================================================

    @Test
    @Order(210)
    @DisplayName("R20 PASS — four-level nested arithmetic expression passes")
    void r20_deeplyNestedArithmetic_passes() {
        // result = ((a + b) * (c - 1)) / 2
        assertValid(program(
                List.of(
                        decl("INT", "a"), decl("INT", "b"),
                        decl("INT", "c"), decl("INT", "result")
                ),
                List.of(assign("result",
                        binExpr(
                                binExpr(
                                        binExpr(var("a"), "+", var("b")),
                                        "*",
                                        binExpr(var("c"), "-", intLit(1))
                                ),
                                "/",
                                intLit(2)
                        )
                ))
        ));
    }

    @Test
    @Order(211)
    @DisplayName("R20 FAIL — type error at inner node of deep expression is caught")
    void r20_typeErrorAtInnerNode_throws() {
        // result = a + (flag * 2)  — flag is BOOL, cannot multiply
        assertInvalid(program(
                List.of(decl("INT", "a"), decl("BOOL", "flag"), decl("INT", "result")),
                List.of(assign("result",
                        binExpr(var("a"), "+",
                                binExpr(var("flag"), "*", intLit(2)))
                ))
        ));
    }

    @Test
    @Order(212)
    @DisplayName("R20 PASS — chained AND of three relational comparisons (all BOOL) passes")
    void r20_chainedAndOfComparisons_passes() {
        // result = (x > 0) AND (y > 0) AND (z > 0)
        assertValid(program(
                List.of(
                        decl("INT", "x"), decl("INT", "y"),
                        decl("INT", "z"), decl("BOOL", "result")
                ),
                List.of(assign("result",
                        binExpr(
                                binExpr(
                                        binExpr(var("x"), ">", intLit(0)),
                                        "AND",
                                        binExpr(var("y"), ">", intLit(0))
                                ),
                                "AND",
                                binExpr(var("z"), ">", intLit(0))
                        )
                ))
        ));
    }

    @Test
    @Order(213)
    @DisplayName("R20 FAIL — BOOL in left leaf of five-level tree is caught")
    void r20_boolInLeftLeafOfDeepTree_throws() {
        // result = ((flag + b) * c) / d  — flag is BOOL, + requires numeric
        assertInvalid(program(
                List.of(
                        decl("BOOL", "flag"), decl("INT", "b"),
                        decl("INT", "c"), decl("INT", "d"), decl("INT", "result")
                ),
                List.of(assign("result",
                        binExpr(
                                binExpr(
                                        binExpr(var("flag"), "+", var("b")),   // error here
                                        "*",
                                        var("c")
                                ),
                                "/",
                                var("d")
                        )
                ))
        ));
    }

    // =========================================================================
    // SECTION 21 — R21: CHAINED ASSIGNMENTS
    // =========================================================================

    @Test
    @Order(220)
    @DisplayName("R21 PASS — chained assignment where all targets are declared and same type")
    void r21_chainedAssign_allValid_passes() {
        // a = b = c = 0
        assertValid(program(
                List.of(decl("INT", "a"), decl("INT", "b"), decl("INT", "c")),
                List.of(assignChain(List.of("a", "b", "c"), intLit(0)))
        ));
    }

    @Test
    @Order(221)
    @DisplayName("R21 PASS — two-target chained assignment with same types")
    void r21_chainedAssign_twoTargets_passes() {
        // x = y = 42
        assertValid(program(
                List.of(decl("INT", "x"), decl("INT", "y")),
                List.of(assignChain(List.of("x", "y"), intLit(42)))
        ));
    }

    @Test
    @Order(222)
    @DisplayName("R21 FAIL — chained assignment where one target is undeclared throws")
    void r21_chainedAssign_oneUndeclared_throws() {
        // a = phantom = 0  — phantom not declared
        assertInvalid(program(
                List.of(decl("INT", "a")),
                List.of(assignChain(List.of("a", "phantom"), intLit(0)))
        ));
    }

    @Test
    @Order(223)
    @DisplayName("R21 FAIL — chained assignment with type mismatch in one target throws")
    void r21_chainedAssign_typeMismatch_throws() {
        // a = b = 0  — b is BOOL, value is INT
        assertInvalid(program(
                List.of(decl("INT", "a"), decl("BOOL", "b")),
                List.of(assignChain(List.of("a", "b"), intLit(0)))
        ));
    }

    @Test
    @Order(224)
    @DisplayName("R21 FAIL — chained assignment where ALL targets are undeclared throws")
    void r21_chainedAssign_allUndeclared_throws() {
        assertInvalid(stmtsOnly(List.of(
                assignChain(List.of("x", "y", "z"), intLit(1))
        )));
    }

    // =========================================================================
    // SECTION 22 — R22: IF/ELSE IF/ELSE BODIES ARE FULLY VALIDATED
    // =========================================================================

    @Test
    @Order(230)
    @DisplayName("R22 PASS — IF/ELSE bodies with valid statements pass")
    void r22_ifElse_validBodies_passes() {
        assertValid(program(
                List.of(decl("INT", "x"), decl("BOOL", "flag")),
                List.of(ifElse(
                        var("flag"),
                        List.of(assign("x", intLit(1))),
                        List.of(assign("x", intLit(2)))
                ))
        ));
    }

    @Test
    @Order(231)
    @DisplayName("R22 FAIL — type error in IF then-block is caught")
    void r22_typeErrorInThenBlock_throws() {
        // IF flag → x = TRUE  (INT = BOOL)
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "flag")),
                List.of(ifOnly(var("flag"), List.of(assign("x", boolLit(true)))))
        ));
    }

    @Test
    @Order(232)
    @DisplayName("R22 FAIL — undeclared variable in IF then-block is caught")
    void r22_undeclaredInThenBlock_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "flag")),
                List.of(ifOnly(var("flag"), List.of(assign("ghost", intLit(0)))))
        ));
    }

    @Test
    @Order(233)
    @DisplayName("R22 FAIL — type error in ELSE block is caught")
    void r22_typeErrorInElseBlock_throws() {
        // ELSE block: x = FALSE  (INT = BOOL)
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "flag")),
                List.of(ifElse(
                        var("flag"),
                        Collections.emptyList(),
                        List.of(assign("x", boolLit(false)))
                ))
        ));
    }

    @Test
    @Order(234)
    @DisplayName("R22 FAIL — undeclared variable in ELSE block is caught")
    void r22_undeclaredInElseBlock_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "flag")),
                List.of(ifElse(
                        var("flag"),
                        Collections.emptyList(),
                        List.of(assign("nobody", intLit(5)))
                ))
        ));
    }

    @Test
    @Order(235)
    @DisplayName("R22 PASS — nested IF inside IF outer-block passes when all valid")
    void r22_nestedIfInsideIf_passes() {
        assertValid(program(
                List.of(decl("BOOL", "a"), decl("BOOL", "b"), decl("INT", "x")),
                List.of(ifOnly(
                        var("a"),
                        List.of(
                                ifOnly(var("b"), List.of(assign("x", intLit(99))))
                        )
                ))
        ));
    }

    // =========================================================================
    // SECTION 23 — R23: FOR AND REPEAT BODIES ARE FULLY VALIDATED
    // =========================================================================

    @Test
    @Order(240)
    @DisplayName("R23 FAIL — type error inside FOR body is caught")
    void r23_typeErrorInForBody_throws() {
        // FOR i = 0 ; i < 5 ; i = i+1 — body: flag = i  (BOOL = INT)
        assertInvalid(program(
                List.of(decl("INT", "i"), decl("BOOL", "flag")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        binExpr(var("i"), "<", intLit(5)),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        List.of(assign("flag", var("i")))
                ))
        ));
    }

    @Test
    @Order(241)
    @DisplayName("R23 FAIL — undeclared variable inside FOR body is caught")
    void r23_undeclaredInForBody_throws() {
        assertInvalid(program(
                List.of(decl("INT", "i")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        binExpr(var("i"), "<", intLit(5)),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        List.of(assign("phantom", intLit(1)))
                ))
        ));
    }

    @Test
    @Order(242)
    @DisplayName("R23 FAIL — type error inside REPEAT WHEN body is caught")
    void r23_typeErrorInRepeatBody_throws() {
        // body: intVar = TRUE  (INT = BOOL)
        assertInvalid(program(
                List.of(decl("INT", "intVar"), decl("BOOL", "cond")),
                List.of(repeatWhen(
                        var("cond"),
                        List.of(assign("intVar", boolLit(true)))
                ))
        ));
    }

    @Test
    @Order(243)
    @DisplayName("R23 FAIL — undeclared variable inside REPEAT WHEN body is caught")
    void r23_undeclaredInRepeatBody_throws() {
        assertInvalid(program(
                List.of(decl("BOOL", "cond")),
                List.of(repeatWhen(
                        var("cond"),
                        List.of(assign("ghost", intLit(1)))
                ))
        ));
    }

    // =========================================================================
    // SECTION 24 — R24: PRINT ACCEPTS ANY TYPE
    // =========================================================================

    @Test
    @Order(250)
    @DisplayName("R24 PASS — PRINT with INT variable passes")
    void r24_print_intVar_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(print(List.of(var("x"))))
        ));
    }

    @Test
    @Order(251)
    @DisplayName("R24 PASS — PRINT with FLOAT variable passes")
    void r24_print_floatVar_passes() {
        assertValid(program(
                List.of(decl("FLOAT", "f")),
                List.of(print(List.of(var("f"))))
        ));
    }

    @Test
    @Order(252)
    @DisplayName("R24 PASS — PRINT with BOOL variable passes")
    void r24_print_boolVar_passes() {
        assertValid(program(
                List.of(decl("BOOL", "b")),
                List.of(print(List.of(var("b"))))
        ));
    }

    @Test
    @Order(253)
    @DisplayName("R24 PASS — PRINT with CHAR variable passes")
    void r24_print_charVar_passes() {
        assertValid(program(
                List.of(decl("CHAR", "c")),
                List.of(print(List.of(var("c"))))
        ));
    }

    @Test
    @Order(254)
    @DisplayName("R24 PASS — PRINT with mixed declared variables of all four types passes")
    void r24_print_allFourTypes_passes() {
        assertValid(program(
                List.of(
                        decl("INT",   "i"),
                        decl("FLOAT", "f"),
                        decl("CHAR",  "c"),
                        decl("BOOL",  "b")
                ),
                List.of(print(List.of(var("i"), var("f"), var("c"), var("b"))))
        ));
    }

    @Test
    @Order(255)
    @DisplayName("R24 PASS — PRINT with mixed literals of all four types passes")
    void r24_print_allFourLiteralTypes_passes() {
        assertValid(stmtsOnly(List.of(
                print(List.of(intLit(1), floatLit(2.0f), boolLit(true), charLit('Z')))
        )));
    }

    @Test
    @Order(256)
    @DisplayName("R24 PASS — PRINT with expression segment (relational) passes")
    void r24_print_boolExprSegment_passes() {
        // PRINT: x > 0  — a BOOL expression; all types are valid in PRINT
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(print(List.of(binExpr(var("x"), ">", intLit(0)))))
        ));
    }

    // =========================================================================
    // SECTION 25 — R25: LITERALS ARE ALWAYS VALID
    // =========================================================================

    @Test
    @Order(260)
    @DisplayName("R25 PASS — INT literal in isolation (as assignment RHS) passes")
    void r25_intLit_assignmentRhs_passes() {
        assertValid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", intLit(0)))
        ));
    }

    @Test
    @Order(261)
    @DisplayName("R25 PASS — BOOL literal as IF condition passes")
    void r25_boolLit_asIfCondition_passes() {
        assertValid(stmtsOnly(List.of(
                ifOnly(boolLit(true), Collections.emptyList())
        )));
    }

    @Test
    @Order(262)
    @DisplayName("R25 PASS — FLOAT literal as declaration initializer passes")
    void r25_floatLit_asInitializer_passes() {
        assertValid(declsOnly(List.of(decl("FLOAT", "pi", floatLit(3.14159f)))));
    }

    @Test
    @Order(263)
    @DisplayName("R25 PASS — CHAR literal as CHAR variable initializer passes")
    void r25_charLit_asInitializer_passes() {
        assertValid(declsOnly(List.of(decl("CHAR", "letter", charLit('G')))));
    }

    // =========================================================================
    // SECTION 26 — R26 & R27: INT-FLOAT WIDENING AND STRICT NARROWING
    // =========================================================================

    @Test
    @Order(270)
    @DisplayName("R27 FAIL — FLOAT-to-INT narrowing in declaration is always rejected")
    void r27_floatToInt_declaration_throws() {
        // DECLARE INT x = 3.14  — cannot silently narrow FLOAT to INT
        assertInvalid(declsOnly(List.of(decl("INT", "x", floatLit(3.14f)))));
    }

    @Test
    @Order(271)
    @DisplayName("R27 FAIL — FLOAT-to-INT narrowing in assignment is always rejected")
    void r27_floatToInt_assignment_throws() {
        assertInvalid(program(
                List.of(decl("INT", "x")),
                List.of(assign("x", floatLit(1.0f)))
        ));
    }

    // =========================================================================
    // SECTION 27 — R28: MIXED INT+FLOAT ARITHMETIC TYPE PROMOTION
    // =========================================================================

    @Test
    @Order(280)
    @DisplayName("R28 PASS — INT + FLOAT result assigned to FLOAT variable passes")
    void r28_intPlusFloat_toFloat_passes() {
        // DECLARE INT x / DECLARE FLOAT f / DECLARE FLOAT result
        // result = x + f   — INT+FLOAT promotes to FLOAT
        assertValid(program(
                List.of(decl("INT", "x"), decl("FLOAT", "f"), decl("FLOAT", "result")),
                List.of(assign("result", binExpr(var("x"), "+", var("f"))))
        ));
    }

    @Test
    @Order(281)
    @DisplayName("R28 PASS — FLOAT - INT result assigned to FLOAT variable passes")
    void r28_floatMinusInt_toFloat_passes() {
        assertValid(program(
                List.of(decl("FLOAT", "f"), decl("INT", "x"), decl("FLOAT", "result")),
                List.of(assign("result", binExpr(var("f"), "-", var("x"))))
        ));
    }

    @Test
    @Order(282)
    @DisplayName("R28 FAIL — INT + FLOAT result assigned to INT variable throws")
    void r28_intPlusFloat_toInt_throws() {
        // The promoted result is FLOAT — cannot be stored in INT
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("FLOAT", "f"), decl("INT", "result")),
                List.of(assign("result", binExpr(var("x"), "+", var("f"))))
        ));
    }

    // =========================================================================
    // SECTION 28 — R29: DEEP NESTING
    // =========================================================================

    @Test
    @Order(290)
    @DisplayName("R29 PASS — five-level nested arithmetic expression passes")
    void r29_fiveLevel_nestedArithmetic_passes() {
        // result = ((a + b) * (c - d)) / (e % 3)
        assertValid(program(
                List.of(
                        decl("INT", "a"), decl("INT", "b"), decl("INT", "c"),
                        decl("INT", "d"), decl("INT", "e"), decl("INT", "result")
                ),
                List.of(assign("result",
                        binExpr(
                                binExpr(
                                        binExpr(var("a"), "+", var("b")),
                                        "*",
                                        binExpr(var("c"), "-", var("d"))
                                ),
                                "/",
                                binExpr(var("e"), "%", intLit(3))
                        )
                ))
        ));
    }

    @Test
    @Order(291)
    @DisplayName("R29 FAIL — type error at level 3 of 5-level expression is caught")
    void r29_typeErrorAtLevel3_throws() {
        // result = (a + b) * (a - flag)  — flag is BOOL
        assertInvalid(program(
                List.of(
                        decl("INT", "a"), decl("INT", "b"),
                        decl("BOOL", "flag"), decl("INT", "result")
                ),
                List.of(assign("result",
                        binExpr(
                                binExpr(var("a"), "+", var("b")),
                                "*",
                                binExpr(var("a"), "-", var("flag"))   // type error
                        )
                ))
        ));
    }

    @Test
    @Order(292)
    @DisplayName("R29 FAIL — undeclared variable at leaf of six-level tree is caught")
    void r29_undeclaredAtLeaf_sixLevels_throws() {
        // result = (1 + (2 * (3 - (4 / (5 % ghost)))))  — ghost not declared
        assertInvalid(program(
                List.of(decl("INT", "result")),
                List.of(assign("result",
                        binExpr(intLit(1), "+",
                                binExpr(intLit(2), "*",
                                        binExpr(intLit(3), "-",
                                                binExpr(intLit(4), "/",
                                                        binExpr(intLit(5), "%", var("ghost"))
                                                )
                                        )
                                )
                        )
                ))
        ));
    }

    // =========================================================================
    // SECTION 29 — R30: EMPTY BODIES DO NOT CRASH
    // =========================================================================

    @Test
    @Order(300)
    @DisplayName("R30 PASS — IF with empty then-block does not throw")
    void r30_emptyIfBody_passes() {
        assertValid(stmtsOnly(List.of(
                ifOnly(boolLit(true), Collections.emptyList())
        )));
    }

    @Test
    @Order(301)
    @DisplayName("R30 PASS — IF/ELSE both with empty bodies does not throw")
    void r30_emptyIfElseBodies_passes() {
        assertValid(stmtsOnly(List.of(
                ifElse(boolLit(true), Collections.emptyList(), Collections.emptyList())
        )));
    }

    @Test
    @Order(302)
    @DisplayName("R30 PASS — FOR with empty body does not throw")
    void r30_emptyForBody_passes() {
        assertValid(program(
                List.of(decl("INT", "i")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        binExpr(var("i"), "<", intLit(5)),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        Collections.emptyList()
                ))
        ));
    }

    @Test
    @Order(303)
    @DisplayName("R30 PASS — REPEAT WHEN with empty body does not throw")
    void r30_emptyRepeatBody_passes() {
        assertValid(stmtsOnly(List.of(
                repeatWhen(boolLit(false), Collections.emptyList())
        )));
    }

    @Test
    @Order(304)
    @DisplayName("R30 PASS — program with no statements and no declarations does not throw")
    void r30_emptyEverything_passes() {
        assertValid(program(Collections.emptyList(), Collections.emptyList()));
    }

    // =========================================================================
    // SECTION 30 — ERROR MESSAGE QUALITY
    // =========================================================================

    @Test
    @Order(310)
    @DisplayName("ERR — SemanticException message is not null and not blank")
    void err_exceptionMessageNotBlank() {
        ProgramNode program = declsOnly(List.of(
                decl("INT", "x"),
                decl("INT", "x")
        ));
        SemanticException ex = assertInvalid(program);
        assertNotNull(ex.getMessage(),
                "SemanticException.getMessage() must not return null");
        assertFalse(ex.getMessage().isBlank(),
                "SemanticException.getMessage() must not be blank");
    }

    @Test
    @Order(311)
    @DisplayName("ERR — duplicate declaration error message names the variable")
    void err_duplicateDecl_messageNamesVar() {
        ProgramNode program = declsOnly(List.of(
                decl("INT", "myCounter"),
                decl("INT", "myCounter")
        ));
        SemanticException ex = assertInvalid(program);
        assertTrue(
                ex.getMessage().contains("myCounter"),
                "Message must contain 'myCounter', got: " + ex.getMessage()
        );
    }

    @Test
    @Order(312)
    @DisplayName("ERR — undeclared variable error message names the variable")
    void err_undeclaredVar_messageNamesVar() {
        ProgramNode program = stmtsOnly(List.of(assign("xyzzy", intLit(1))));
        SemanticException ex = assertInvalid(program);
        assertTrue(
                ex.getMessage().contains("xyzzy"),
                "Message must contain 'xyzzy', got: " + ex.getMessage()
        );
    }

    @Test
    @Order(313)
    @DisplayName("ERR — SemanticException carries a positive line number")
    void err_exceptionHasPositiveLineNumber() {
        ProgramNode program = declsOnly(List.of(
                decl("INT", "x"),
                decl("INT", "x")
        ));
        SemanticException ex = assertInvalid(program);
        // Assumes SemanticException (via LexorException) exposes getLine().
        assertTrue(ex.getLine() > 0,
                "SemanticException.getLine() must return a positive value, got: " + ex.getLine());
    }

    @Test
    @Order(314)
    @DisplayName("ERR — second analysis on a fresh analyzer after first error passes cleanly")
    void err_freshAnalyzerAfterError_passes() {
        // First analysis — should fail.
        ProgramNode bad = declsOnly(List.of(decl("INT","x"), decl("INT","x")));
        assertInvalid(bad);

        // The bad analysis is on the OLD analyzer instance.
        // @BeforeEach creates a new one for each test, so this just validates
        // that SemanticAnalyzer state is never shared between tests.
        SemanticAnalyzer fresh = new SemanticAnalyzer();
        ProgramNode good = declsOnly(List.of(decl("INT","x")));
        assertDoesNotThrow(() -> fresh.analyze(good),
                "A fresh SemanticAnalyzer must not carry over state from a previous failed analysis");
    }

    // =========================================================================
    // SECTION 31 — INTEGRATION / COMPOUND SCENARIOS
    // =========================================================================

    @Test
    @Order(320)
    @DisplayName("INTEGRATION PASS — counter loop with conditional print (all valid)")
    void integration_counterLoopWithConditionalPrint_passes() {
        // DECLARE INT i
        // DECLARE BOOL shouldPrint
        // FOR i = 1 ; i <= 5 ; i = i + 1
        //   shouldPrint = i > 3
        //   IF shouldPrint → PRINT: i
        // END FOR
        assertValid(program(
                List.of(decl("INT", "i"), decl("BOOL", "shouldPrint")),
                List.of(forLoop(
                        assign("i", intLit(1)),
                        binExpr(var("i"), "<=", intLit(5)),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        List.of(
                                assign("shouldPrint", binExpr(var("i"), ">", intLit(3))),
                                ifOnly(var("shouldPrint"), List.of(print(List.of(var("i")))))
                        )
                ))
        ));
    }

    @Test
    @Order(321)
    @DisplayName("INTEGRATION PASS — user-input loop with type-checked comparison")
    void integration_inputLoopWithComparison_passes() {
        // DECLARE INT x
        // DECLARE BOOL positive
        // REPEAT
        //   SCAN: x
        //   positive = x > 0
        // END REPEAT WHEN positive
        assertValid(program(
                List.of(decl("INT", "x"), decl("BOOL", "positive")),
                List.of(repeatWhen(
                        var("positive"),
                        List.of(
                                scan(List.of("x")),
                                assign("positive", binExpr(var("x"), ">", intLit(0)))
                        )
                ))
        ));
    }

    @Test
    @Order(322)
    @DisplayName("INTEGRATION FAIL — error in second of three statements is caught")
    void integration_errorInMiddleStatement_throws() {
        // x = 1              — valid
        // x = b AND TRUE     — x is INT, AND produces BOOL
        // x = 3              — never reached
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "b")),
                List.of(
                        assign("x", intLit(1)),
                        assign("x", binExpr(var("b"), "AND", boolLit(true))),  // error
                        assign("x", intLit(3))
                )
        ));
    }

    @Test
    @Order(323)
    @DisplayName("INTEGRATION FAIL — error hidden inside nested REPEAT inside FOR is caught")
    void integration_errorDeepInsideNestedLoops_throws() {
        // FOR i = 0 ; i < 3 ; i = i+1
        //   REPEAT
        //     flag = i   (BOOL = INT — type error)
        //   END REPEAT WHEN flag
        assertInvalid(program(
                List.of(decl("INT", "i"), decl("BOOL", "flag")),
                List.of(forLoop(
                        assign("i", intLit(0)),
                        binExpr(var("i"), "<", intLit(3)),
                        assign("i", binExpr(var("i"), "+", intLit(1))),
                        List.of(repeatWhen(
                                var("flag"),
                                List.of(assign("flag", var("i")))   // BOOL = INT
                        ))
                ))
        ));
    }

    @Test
    @Order(324)
    @DisplayName("INTEGRATION PASS — full program with all statement types")
    void integration_fullValidProgram_allStatementTypes_passes() {
        // DECLARE INT count
        // DECLARE FLOAT avg
        // DECLARE BOOL done
        // DECLARE CHAR grade
        // count = 0 / avg = 0.0 / done = FALSE / grade = 'F'
        // PRINT: count
        // SCAN: count
        // IF count > 10 → grade = 'A'
        // FOR count = 0 ; count < 5 ; count = count+1 → PRINT: avg
        // REPEAT → done = TRUE  END REPEAT WHEN done
        assertValid(program(
                List.of(
                        decl("INT",   "count"),
                        decl("FLOAT", "avg"),
                        decl("BOOL",  "done"),
                        decl("CHAR",  "grade")
                ),
                List.of(
                        assign("count", intLit(0)),
                        assign("avg",   floatLit(0.0f)),
                        assign("done",  boolLit(false)),
                        assign("grade", charLit('F')),
                        print(List.of(var("count"))),
                        scan(List.of("count")),
                        ifOnly(
                                binExpr(var("count"), ">", intLit(10)),
                                List.of(assign("grade", charLit('A')))
                        ),
                        forLoop(
                                assign("count", intLit(0)),
                                binExpr(var("count"), "<", intLit(5)),
                                assign("count", binExpr(var("count"), "+", intLit(1))),
                                List.of(print(List.of(var("avg"))))
                        ),
                        repeatWhen(
                                var("done"),
                                List.of(assign("done", boolLit(true)))
                        )
                )
        ));
    }

    @Test
    @Order(325)
    @DisplayName("INTEGRATION FAIL — valid first half, error in second half is caught")
    void integration_errorInSecondHalf_throws() {
        // DECLARE INT x / DECLARE BOOL b
        // First half — all valid
        // x = 10
        // b = x > 5
        // PRINT: x
        // SCAN: x
        // Second half — error
        // b = x + 1   (BOOL = INT)
        assertInvalid(program(
                List.of(decl("INT", "x"), decl("BOOL", "b")),
                List.of(
                        assign("x", intLit(10)),
                        assign("b", binExpr(var("x"), ">", intLit(5))),
                        print(List.of(var("x"))),
                        scan(List.of("x")),
                        assign("b", binExpr(var("x"), "+", intLit(1)))   // BOOL = INT
                )
        ));
    }
}