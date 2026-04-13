package com.lexor.lexer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenTypeTest — Unit tests for the TokenType enum.
 *
 * WHAT IS BEING TESTED
 * ---------------------
 * 1. Every expected constant actually exists in the enum (smoke test).
 * 2. Constants with the same semantic group are all present (group completeness).
 * 3. Enum identity — TokenType values are singletons (== comparison is safe).
 * 4. name() and valueOf() behave correctly (standard enum contract).
 *
 * DEPENDENCIES
 * ------------
 * JUnit 5 (junit-jupiter) — @Test, @ParameterizedTest, @EnumSource, Assertions.*
 * No Mockito needed — TokenType has no collaborators.
 *
 * HOW TO RUN (IntelliJ)
 * ----------------------
 * Right-click this file in the Project panel → Run 'TokenTypeTest'.
 * Or use the green gutter arrow next to any individual @Test method.
 * All tests should pass once TokenType.java is fully implemented.
 */
@DisplayName("TokenType enum")
class TokenTypeTest {

    // -------------------------------------------------------------------------
    // 1. SMOKE TESTS — Every constant exists
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("All program-structure keyword constants exist")
    void structureKeywordsExist() {
        assertNotNull(TokenType.KEYWORD_SCRIPT);
        assertNotNull(TokenType.KEYWORD_AREA);
        assertNotNull(TokenType.KEYWORD_START);
        assertNotNull(TokenType.KEYWORD_END);
        assertNotNull(TokenType.KEYWORD_DECLARE);
    }

    @Test
    @DisplayName("All I/O keyword constants exist")
    void ioKeywordsExist() {
        assertNotNull(TokenType.KEYWORD_PRINT);
        assertNotNull(TokenType.KEYWORD_SCAN);
    }

    @Test
    @DisplayName("All control-flow keyword constants exist")
    void controlFlowKeywordsExist() {
        assertNotNull(TokenType.KEYWORD_IF);
        assertNotNull(TokenType.KEYWORD_ELSE);
        assertNotNull(TokenType.KEYWORD_FOR);
        assertNotNull(TokenType.KEYWORD_REPEAT);
        assertNotNull(TokenType.KEYWORD_WHEN);
    }

    @Test
    @DisplayName("All data-type keyword constants exist")
    void dataTypeKeywordsExist() {
        assertNotNull(TokenType.TYPE_INT);
        assertNotNull(TokenType.TYPE_FLOAT);
        assertNotNull(TokenType.TYPE_CHAR);
        assertNotNull(TokenType.TYPE_BOOL);
    }

    @Test
    @DisplayName("All literal constants exist")
    void literalConstantsExist() {
        assertNotNull(TokenType.INT_LITERAL);
        assertNotNull(TokenType.FLOAT_LITERAL);
        assertNotNull(TokenType.CHAR_LITERAL);
        assertNotNull(TokenType.BOOL_LITERAL);
        assertNotNull(TokenType.STRING_LITERAL);
    }

    @Test
    @DisplayName("All arithmetic operator constants exist")
    void arithmeticOperatorsExist() {
        assertNotNull(TokenType.OP_PLUS);
        assertNotNull(TokenType.OP_MINUS);
        assertNotNull(TokenType.OP_MUL);
        assertNotNull(TokenType.OP_DIV);
        assertNotNull(TokenType.OP_MOD);
    }

    @Test
    @DisplayName("All relational operator constants exist")
    void relationalOperatorsExist() {
        assertNotNull(TokenType.OP_GT);
        assertNotNull(TokenType.OP_LT);
        assertNotNull(TokenType.OP_GTE);
        assertNotNull(TokenType.OP_LTE);
        assertNotNull(TokenType.OP_EQ);
        assertNotNull(TokenType.OP_NEQ);
    }

    @Test
    @DisplayName("All logical operator constants exist")
    void logicalOperatorsExist() {
        assertNotNull(TokenType.OP_AND);
        assertNotNull(TokenType.OP_OR);
        assertNotNull(TokenType.OP_NOT);
    }

    @Test
    @DisplayName("All special LEXOR symbol constants exist")
    void specialSymbolsExist() {
        assertNotNull(TokenType.AMPERSAND);
        assertNotNull(TokenType.DOLLAR);
        assertNotNull(TokenType.LBRACKET);
        assertNotNull(TokenType.RBRACKET);
        assertNotNull(TokenType.LPAREN);
        assertNotNull(TokenType.RPAREN);
        assertNotNull(TokenType.COMMA);
        assertNotNull(TokenType.COLON);
        assertNotNull(TokenType.ASSIGN);
    }

    @Test
    @DisplayName("Structural tokens NEWLINE and EOF exist")
    void structuralTokensExist() {
        assertNotNull(TokenType.NEWLINE);
        assertNotNull(TokenType.EOF);
    }

    // -------------------------------------------------------------------------
    // 2. ENUM CONTRACT TESTS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("valueOf() round-trips correctly")
    void valueOfRoundTrip() {
        assertEquals(TokenType.KEYWORD_IF, TokenType.valueOf("KEYWORD_IF"));
        assertEquals(TokenType.OP_PLUS,    TokenType.valueOf("OP_PLUS"));
        assertEquals(TokenType.EOF,        TokenType.valueOf("EOF"));
    }

    @Test
    @DisplayName("name() returns the declared constant name")
    void nameReturnsConstantName() {
        assertEquals("KEYWORD_IF",   TokenType.KEYWORD_IF.name());
        assertEquals("INT_LITERAL",  TokenType.INT_LITERAL.name());
        assertEquals("IDENTIFIER",   TokenType.IDENTIFIER.name());
    }

    @ParameterizedTest(name = "enum constant {0} is a singleton (== safe)")
    @EnumSource(TokenType.class)
    @DisplayName("Each constant is the same object when looked up twice")
    void enumSingletonIdentity(TokenType type) {
        assertSame(type, TokenType.valueOf(type.name()));
    }

    @Test
    @DisplayName("ASSIGN and OP_EQ are distinct constants (= vs ==)")
    void assignAndEqAreDistinct() {
        assertNotSame(TokenType.ASSIGN, TokenType.OP_EQ);
        assertNotEquals(TokenType.ASSIGN, TokenType.OP_EQ);
    }

    @Test
    @DisplayName("OP_AND and AMPERSAND are distinct (logical AND vs concatenation &)")
    void andAndAmpersandAreDistinct() {
        assertNotSame(TokenType.OP_AND, TokenType.AMPERSAND);
    }
}
