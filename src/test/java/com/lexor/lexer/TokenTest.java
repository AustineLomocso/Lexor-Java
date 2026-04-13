package com.lexor.lexer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenTest — Unit tests for the Token data class.
 *
 * WHAT IS BEING TESTED
 * ---------------------
 * 1. Constructor correctly stores all four fields.
 * 2. Getters return the stored values.
 * 3. isType() returns true for the correct type and false for others.
 * 4. toString() produces the expected human-readable format.
 * 5. Immutability — no setter methods exist (compile-time enforcement).
 *
 * DEPENDENCIES
 * ------------
 * JUnit 5 — @Test, @Nested, @DisplayName, Assertions.*
 * No Mockito — Token is a pure value object with no collaborators.
 *
 * HOW TO RUN (IntelliJ)
 * ----------------------
 * Right-click this file → Run 'TokenTest'.
 * All tests should pass once Token.java is implemented.
 */
@DisplayName("Token data class")
class TokenTest {

    // Shared fixture — a representative Token used across multiple tests
    private static final Token SAMPLE = new Token(TokenType.KEYWORD_IF, "IF", 4, 1);

    // -------------------------------------------------------------------------
    // 1. CONSTRUCTOR + GETTERS
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Constructor and getters")
    class ConstructorAndGetters {

        @Test
        @DisplayName("getType() returns the type passed to the constructor")
        void getTypeReturnsConstructorValue() {
            assertEquals(TokenType.KEYWORD_IF, SAMPLE.getType());
        }

        @Test
        @DisplayName("getLexeme() returns the lexeme passed to the constructor")
        void getLexemeReturnsConstructorValue() {
            assertEquals("IF", SAMPLE.getLexeme());
        }

        @Test
        @DisplayName("getLine() returns the line number passed to the constructor")
        void getLineReturnsConstructorValue() {
            assertEquals(4, SAMPLE.getLine());
        }

        @Test
        @DisplayName("getColumn() returns the column number passed to the constructor")
        void getColumnReturnsConstructorValue() {
            assertEquals(1, SAMPLE.getColumn());
        }

        @Test
        @DisplayName("Two tokens with identical fields store independent values")
        void twoTokensAreIndependent() {
            Token t1 = new Token(TokenType.INT_LITERAL, "42",  1, 5);
            Token t2 = new Token(TokenType.INT_LITERAL, "100", 2, 7);
            assertNotEquals(t1.getLexeme(), t2.getLexeme());
            assertNotEquals(t1.getLine(),   t2.getLine());
        }

        @Test
        @DisplayName("Empty lexeme is stored correctly (e.g. EOF token)")
        void emptyLexemeIsStored() {
            Token eof = new Token(TokenType.EOF, "", 10, 1);
            assertEquals("", eof.getLexeme());
            assertEquals(TokenType.EOF, eof.getType());
        }
    }

    // -------------------------------------------------------------------------
    // 2. isType()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isType()")
    class IsType {

        @Test
        @DisplayName("Returns true when type matches")
        void returnsTrueForMatchingType() {
            assertTrue(SAMPLE .isType(TokenType.KEYWORD_IF));
        }

        @Test
        @DisplayName("Returns false when type does not match")
        void returnsFalseForNonMatchingType() {
            assertFalse(SAMPLE.isType(TokenType.KEYWORD_FOR));
            assertFalse(SAMPLE.isType(TokenType.IDENTIFIER));
            assertFalse(SAMPLE.isType(TokenType.EOF));
        }

        @Test
        @DisplayName("Works correctly for literal tokens")
        void worksForLiteralToken() {
            Token intTok = new Token(TokenType.INT_LITERAL, "5", 1, 1);
            assertTrue(intTok.isType(TokenType.INT_LITERAL));
            assertFalse(intTok.isType(TokenType.FLOAT_LITERAL));
        }
    }

    // -------------------------------------------------------------------------
    // 3. toString()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Contains the token type name")
        void containsTypeName() {
            assertTrue(SAMPLE.toString().contains("KEYWORD_IF"));
        }

        @Test
        @DisplayName("Contains the lexeme")
        void containsLexeme() {
            assertTrue(SAMPLE.toString().contains("IF"));
        }

        @Test
        @DisplayName("Contains the line number")
        void containsLineNumber() {
            assertTrue(SAMPLE.toString().contains("4"));
        }

        @Test
        @DisplayName("Contains the column number")
        void containsColumnNumber() {
            assertTrue(SAMPLE.toString().contains("1"));
        }

        @Test
        @DisplayName("Matches expected format: Token[TYPE, \"lexeme\", line=N, col=M]")
        void matchesExpectedFormat() {
            Token t = new Token(TokenType.OP_PLUS, "+", 2, 8);
            assertEquals("Token[OP_PLUS, \"+\", line=2, col=8]", t.toString());
        }
    }
}
