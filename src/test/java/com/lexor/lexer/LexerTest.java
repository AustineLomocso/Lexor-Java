package com.lexor.lexer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LexerTest — Unit tests for the Lexer class.
 *
 * WHAT IS BEING TESTED
 * ---------------------
 * 1.  Keywords          — every reserved word produces the correct TokenType.
 * 2.  Identifiers       — valid names become IDENTIFIER; underscore and mixed-case names work.
 * 3.  Int literals       — digit sequences produce INT_LITERAL with correct lexeme.
 * 4.  Float literals     — decimal numbers produce FLOAT_LITERAL.
 * 5.  Char literals      — single-quoted characters produce CHAR_LITERAL.
 * 6.  Bool/String lits   — double-quoted "TRUE"/"FALSE" → BOOL_LITERAL; others → STRING_LITERAL.
 * 7.  Operators          — every single and multi-character operator tokenizes correctly.
 * 8.  Special LEXOR syms — & (AMPERSAND), $ (DOLLAR), [ ] escape brackets.
 * 9.  Comments           — %% lines are stripped; no tokens produced.
 * 10. Newlines           — each \n produces exactly one NEWLINE token.
 * 11. Whitespace         — spaces and tabs between tokens are silently skipped.
 * 12. Full programs      — the three spec sample programs tokenize without error.
 * 13. Error cases        — unknown characters throw a runtime exception.
 * 14. EOF               — the last token is always EOF.
 *
 * DEPENDENCIES
 * ------------
 * JUnit 5 (junit-jupiter):
 *   @Test, @Nested, @DisplayName                — test structure
 *   @ParameterizedTest + @CsvSource/@ValueSource — table-driven tests (avoids repetition)
 *   Assertions.*                                 — assertAll(), assertEquals(), assertThrows()
 *
 * No Mockito — Lexer has no injected collaborators; SLF4J logging is a side effect
 * that we do not need to verify in unit tests.
 *
 * HOW TO RUN (IntelliJ)
 * ----------------------
 * Right-click this file → Run 'LexerTest'.
 * To run a single nested class: expand the file in the test runner tree and click the class.
 * All tests should pass once Lexer.java is fully implemented.
 *
 * HELPER METHOD
 * -------------
 * lex(source) — shorthand that constructs a Lexer and calls tokenize(),
 * returning the List<Token>. Used in every test method below.
 */
@DisplayName("Lexer")
class LexerTest {

    /** Convenience wrapper so every test can stay on one line. */
    private List<Token> lex(String source) {
        return new Lexer(source).tokenize();
    }

    /** Returns the type of the token at position idx in the list. */
    private TokenType typeAt(List<Token> tokens, int idx) {
        return tokens.get(idx).getType();
    }

    /** Asserts that the last token in any list is always EOF. */
    private void assertEndsWithEof(List<Token> tokens) {
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType(),
                "Last token must always be EOF");
    }

    // =========================================================================
    // 1. KEYWORDS
    // =========================================================================

    @Nested
    @DisplayName("Keywords")
    class Keywords {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
            "SCRIPT,  KEYWORD_SCRIPT",
            "AREA,    KEYWORD_AREA",
            "START,   KEYWORD_START",
            "END,     KEYWORD_END",
            "DECLARE, KEYWORD_DECLARE",
            "PRINT,   KEYWORD_PRINT",
            "SCAN,    KEYWORD_SCAN",
            "IF,      KEYWORD_IF",
            "ELSE,    KEYWORD_ELSE",
            "FOR,     KEYWORD_FOR",
            "REPEAT,  KEYWORD_REPEAT",
            "WHEN,    KEYWORD_WHEN",
            "INT,     TYPE_INT",
            "FLOAT,   TYPE_FLOAT",
            "CHAR,    TYPE_CHAR",
            "BOOL,    TYPE_BOOL",
            "AND,     OP_AND",
            "OR,      OP_OR",
            "NOT,     OP_NOT"
        })
        @DisplayName("Each reserved word produces the correct TokenType")
        void reservedWordMapsToCorrectType(String word, String expectedTypeName) {
            List<Token> tokens = lex(word);
            TokenType expected = TokenType.valueOf(expectedTypeName.trim());
            assertEquals(expected, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("Keywords are case-sensitive: lowercase 'if' is an IDENTIFIER")
        void lowercaseKeywordIsIdentifier() {
            List<Token> tokens = lex("if");
            assertEquals(TokenType.IDENTIFIER, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("Mixed-case 'Declare' is an IDENTIFIER, not a keyword")
        void mixedCaseIsIdentifier() {
            List<Token> tokens = lex("Declare");
            assertEquals(TokenType.IDENTIFIER, typeAt(tokens, 0));
        }
    }

    // =========================================================================
    // 2. IDENTIFIERS
    // =========================================================================

    @Nested
    @DisplayName("Identifiers")
    class Identifiers {

        @ParameterizedTest(name = "\"{0}\" is a valid identifier")
        @ValueSource(strings = {"x", "y", "myVar", "a_1", "_count", "xyz", "abc123"})
        @DisplayName("Valid identifier names produce IDENTIFIER token")
        void validIdentifiers(String name) {
            List<Token> tokens = lex(name);
            assertEquals(TokenType.IDENTIFIER, typeAt(tokens, 0));
            assertEquals(name, tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("Identifier lexeme is preserved exactly (case-sensitive)")
        void identifierPreservesCase() {
            List<Token> tokens = lex("myVar");
            assertEquals("myVar", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("Underscore-prefixed identifier is valid")
        void underscorePrefixedIdentifier() {
            List<Token> tokens = lex("_x");
            assertEquals(TokenType.IDENTIFIER, typeAt(tokens, 0));
        }
    }

    // =========================================================================
    // 3. INT LITERALS
    // =========================================================================

    @Nested
    @DisplayName("INT literals")
    class IntLiterals {

        @ParameterizedTest(name = "\"{0}\" -> INT_LITERAL")
        @ValueSource(strings = {"0", "4", "42", "100", "200", "1000"})
        @DisplayName("Digit sequences produce INT_LITERAL")
        void intLiteralProduced(String digits) {
            List<Token> tokens = lex(digits);
            assertEquals(TokenType.INT_LITERAL, typeAt(tokens, 0));
            assertEquals(digits, tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("INT literal lexeme round-trips correctly")
        void intLexemeRoundTrips() {
            List<Token> tokens = lex("9999");
            assertEquals("9999", tokens.get(0).getLexeme());
        }
    }

    // =========================================================================
    // 4. FLOAT LITERALS
    // =========================================================================

    @Nested
    @DisplayName("FLOAT literals")
    class FloatLiterals {

        @ParameterizedTest(name = "\"{0}\" -> FLOAT_LITERAL")
        @ValueSource(strings = {"3.14", "0.5", "100.0", "9.99"})
        @DisplayName("Decimal numbers produce FLOAT_LITERAL")
        void floatLiteralProduced(String num) {
            List<Token> tokens = lex(num);
            assertEquals(TokenType.FLOAT_LITERAL, typeAt(tokens, 0));
            assertEquals(num, tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("Trailing dot without digits is NOT a float — dot stays as a future error")
        void integerFollowedByDotIsIntThenDot() {
            // "5." — the '.' is not followed by a digit, so '5' is INT_LITERAL
            List<Token> tokens = lex("5.");
            assertEquals(TokenType.INT_LITERAL, typeAt(tokens, 0));
        }
    }

    // =========================================================================
    // 5. CHAR LITERALS
    // =========================================================================

    @Nested
    @DisplayName("CHAR literals")
    class CharLiterals {

        @Test
        @DisplayName("Single-quoted letter produces CHAR_LITERAL with correct lexeme")
        void charLiteralLetter() {
            List<Token> tokens = lex("'n'");
            assertEquals(TokenType.CHAR_LITERAL, typeAt(tokens, 0));
            assertEquals("n", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("Single-quoted digit is a valid CHAR literal")
        void charLiteralDigit() {
            List<Token> tokens = lex("'5'");
            assertEquals(TokenType.CHAR_LITERAL, typeAt(tokens, 0));
            assertEquals("5", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("CHAR literal spec example: 'c'")
        void charLiteralFromSpec() {
            List<Token> tokens = lex("'c'");
            assertEquals(TokenType.CHAR_LITERAL, typeAt(tokens, 0));
            assertEquals("c", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("Empty char literal throws exception")
        void emptyCharLiteralThrows() {
            assertThrows(RuntimeException.class, () -> lex("''"));
        }
    }

    // =========================================================================
    // 6. BOOL AND STRING LITERALS
    // =========================================================================

    @Nested
    @DisplayName("BOOL and STRING literals")
    class BoolAndStringLiterals {

        @Test
        @DisplayName("\"TRUE\" produces BOOL_LITERAL with lexeme TRUE")
        void trueBoolLiteral() {
            List<Token> tokens = lex("\"TRUE\"");
            assertEquals(TokenType.BOOL_LITERAL, typeAt(tokens, 0));
            assertEquals("TRUE", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("\"FALSE\" produces BOOL_LITERAL with lexeme FALSE")
        void falseBoolLiteral() {
            List<Token> tokens = lex("\"FALSE\"");
            assertEquals(TokenType.BOOL_LITERAL, typeAt(tokens, 0));
            assertEquals("FALSE", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("\"hello\" produces STRING_LITERAL")
        void stringLiteral() {
            List<Token> tokens = lex("\"hello\"");
            assertEquals(TokenType.STRING_LITERAL, typeAt(tokens, 0));
            assertEquals("hello", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("\"last\" from spec sample produces STRING_LITERAL")
        void specSampleStringLiteral() {
            List<Token> tokens = lex("\"last\"");
            assertEquals(TokenType.STRING_LITERAL, typeAt(tokens, 0));
            assertEquals("last", tokens.get(0).getLexeme());
        }

        @Test
        @DisplayName("Unterminated string literal throws exception")
        void unterminatedStringThrows() {
            assertThrows(RuntimeException.class, () -> lex("\"hello"));
        }
    }

    // =========================================================================
    // 7. OPERATORS
    // =========================================================================

    @Nested
    @DisplayName("Operators")
    class Operators {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
            "+,  OP_PLUS",
            "-,  OP_MINUS",
            "*,  OP_MUL",
            "/,  OP_DIV",
            "%,  OP_MOD",
            ">,  OP_GT",
            "<,  OP_LT",
            ">=, OP_GTE",
            "<=, OP_LTE",
            "==, OP_EQ",
            "<>, OP_NEQ",
            "=,  ASSIGN"
        })
        @DisplayName("Operator symbols produce correct TokenType")
        void operatorTokens(String symbol, String expectedType) {
            List<Token> tokens = lex(symbol.trim());
            assertEquals(TokenType.valueOf(expectedType.trim()), typeAt(tokens, 0));
        }

        @Test
        @DisplayName(">= is preferred over > when followed by =")
        void gtePreferredOverGt() {
            List<Token> tokens = lex(">=");
            assertEquals(1, tokens.size() - 1, "Should be exactly 1 operator token before EOF");
            assertEquals(TokenType.OP_GTE, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("<> is preferred over < when followed by >")
        void neqPreferredOverLt() {
            List<Token> tokens = lex("<>");
            assertEquals(TokenType.OP_NEQ, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("= is ASSIGN when not followed by another =")
        void singleEqualsIsAssign() {
            List<Token> tokens = lex("=");
            assertEquals(TokenType.ASSIGN, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("== is OP_EQ (equality test)")
        void doubleEqualsIsEq() {
            List<Token> tokens = lex("==");
            assertEquals(TokenType.OP_EQ, typeAt(tokens, 0));
        }
    }

    // =========================================================================
    // 8. SPECIAL LEXOR SYMBOLS
    // =========================================================================

    @Nested
    @DisplayName("Special LEXOR symbols")
    class SpecialSymbols {

        @Test
        @DisplayName("& produces AMPERSAND (concatenation, not logical AND)")
        void ampersandToken() {
            List<Token> tokens = lex("&");
            assertEquals(TokenType.AMPERSAND, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("$ produces DOLLAR (newline marker in PRINT)")
        void dollarToken() {
            List<Token> tokens = lex("$");
            assertEquals(TokenType.DOLLAR, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("[ produces LBRACKET and ] produces RBRACKET")
        void bracketTokens() {
            List<Token> tokens = lex("[]");
            assertEquals(TokenType.LBRACKET, typeAt(tokens, 0));
            assertEquals(TokenType.RBRACKET, typeAt(tokens, 1));
        }

        @Test
        @DisplayName("Escape sequence [#] tokenizes as LBRACKET, IDENTIFIER(#-area), RBRACKET")
        void escapeCodeBracketsTokenized() {
            // The Lexer emits [ and ] as bracket tokens.
            // The content inside is a separate token.
            // The Interpreter handles the semantic meaning of [x] at runtime.
            List<Token> tokens = lex("[#]");
            assertEquals(TokenType.LBRACKET, typeAt(tokens, 0));
            assertEquals(TokenType.RBRACKET, typeAt(tokens, 2));
        }
    }

    // =========================================================================
    // 9. COMMENTS
    // =========================================================================

    @Nested
    @DisplayName("Comments")
    class Comments {

        @Test
        @DisplayName("%% comment produces no tokens (only EOF)")
        void commentProducesNoTokens() {
            List<Token> tokens = lex("%% this is a comment");
            assertEquals(1, tokens.size(), "Only EOF expected");
            assertEquals(TokenType.EOF, tokens.get(0).getType());
        }

        @Test
        @DisplayName("Code before %% comment is tokenized; code after is not")
        void codeBeforeCommentIsTokenized() {
            List<Token> tokens = lex("x %% ignore this");
            assertEquals(TokenType.IDENTIFIER, typeAt(tokens, 0));
            assertEquals("x", tokens.get(0).getLexeme());
            // After x and EOF, nothing from the comment
            assertEquals(2, tokens.size());
        }

        @Test
        @DisplayName("Comment on its own line does not affect subsequent lines")
        void commentDoesNotAffectNextLine() {
            String src = "%% comment\nx";
            List<Token> tokens = lex(src);
            // Tokens: NEWLINE, IDENTIFIER(x), EOF
            boolean hasIdentifier = tokens.stream()
                .anyMatch(t -> t.getType() == TokenType.IDENTIFIER && t.getLexeme().equals("x"));
            assertTrue(hasIdentifier);
        }

        @Test
        @DisplayName("Lone % (without second %) is tokenized as OP_MOD")
        void lonePcentIsModulo() {
            List<Token> tokens = lex("10 % 3");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.OP_MOD));
        }
    }

    // =========================================================================
    // 10. NEWLINES
    // =========================================================================

    @Nested
    @DisplayName("Newlines")
    class Newlines {

        @Test
        @DisplayName("A single \\n produces one NEWLINE token")
        void singleNewline() {
            List<Token> tokens = lex("\n");
            assertEquals(TokenType.NEWLINE, typeAt(tokens, 0));
        }

        @Test
        @DisplayName("Two statements on two lines produce two NEWLINE tokens")
        void twoNewlinesForTwoLines() {
            List<Token> tokens = lex("x\ny\n");
            long newlineCount = tokens.stream()
                .filter(t -> t.getType() == TokenType.NEWLINE)
                .count();
            assertEquals(2, newlineCount);
        }

        @Test
        @DisplayName("Line number in Token is incremented correctly after each \\n")
        void lineNumberIncrements() {
            List<Token> tokens = lex("x\ny");
            Token yToken = tokens.stream()
                .filter(t -> t.getLexeme().equals("y"))
                .findFirst()
                .orElseThrow();
            assertEquals(2, yToken.getLine(), "y is on line 2");
        }
    }

    // =========================================================================
    // 11. WHITESPACE
    // =========================================================================

    @Nested
    @DisplayName("Whitespace handling")
    class Whitespace {

        @Test
        @DisplayName("Spaces between tokens are silently discarded")
        void spacesDiscarded() {
            List<Token> t1 = lex("x=4");
            List<Token> t2 = lex("x = 4");
            // Both should produce the same token types
            assertEquals(t1.size(), t2.size());
            for (int i = 0; i < t1.size(); i++) {
                assertEquals(t1.get(i).getType(), t2.get(i).getType());
            }
        }

        @Test
        @DisplayName("Tabs between tokens are silently discarded")
        void tabsDiscarded() {
            List<Token> tokens = lex("x\t=\t4");
            assertEquals(TokenType.IDENTIFIER, typeAt(tokens, 0));
            assertEquals(TokenType.ASSIGN,     typeAt(tokens, 1));
            assertEquals(TokenType.INT_LITERAL, typeAt(tokens, 2));
        }
    }

    // =========================================================================
    // 12. FULL PROGRAM TOKENIZATION (spec sample programs)
    // =========================================================================

    @Nested
    @DisplayName("Full program tokenization")
    class FullPrograms {

        @Test
        @DisplayName("Spec sample 1 — hello program — tokenizes without exception")
        void sampleProgramHello() {
            String src = """
                %% this is a sample program in LEXOR
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
            assertDoesNotThrow(() -> lex(src));
        }

        @Test
        @DisplayName("Spec sample 2 — arithmetic — tokenizes without exception")
        void sampleProgramArithmetic() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT xyz, abc=100
                xyz= ((abc *5)/10 + 10) * -1
                PRINT: [[] & xyz & []]
                END SCRIPT
                """;
            assertDoesNotThrow(() -> lex(src));
        }

        @Test
        @DisplayName("Spec sample 3 — logical — tokenizes without exception")
        void sampleProgramLogical() {
            String src = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=100, b=200, c=300
                DECLARE BOOL d="FALSE"
                d = (a < b AND c <>200)
                PRINT: d
                END SCRIPT
                """;
            assertDoesNotThrow(() -> lex(src));
        }

        @Test
        @DisplayName("Spec sample 1 — last token is always EOF")
        void lastTokenIsEof() {
            String src = "SCRIPT AREA\nSTART SCRIPT\nEND SCRIPT\n";
            assertEndsWithEof(lex(src));
        }
    }

    // =========================================================================
    // 13. ERROR CASES
    // =========================================================================

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("Unknown character @ throws RuntimeException")
        void unknownCharAtSign() {
            assertThrows(RuntimeException.class, () -> lex("@"));
        }

        @Test
        @DisplayName("Unknown character # (outside brackets) throws RuntimeException")
        void unknownCharHash() {
            assertThrows(RuntimeException.class, () -> lex("#"));
        }
    }

    // =========================================================================
    // 14. EOF
    // =========================================================================

    @Nested
    @DisplayName("EOF token")
    class EofToken {

        @Test
        @DisplayName("Empty source produces only EOF")
        void emptySourceProducesOnlyEof() {
            List<Token> tokens = lex("");
            assertEquals(1, tokens.size());
            assertEquals(TokenType.EOF, tokens.get(0).getType());
        }

        @Test
        @DisplayName("EOF token always has empty lexeme")
        void eofHasEmptyLexeme() {
            List<Token> tokens = lex("x");
            Token eof = tokens.get(tokens.size() - 1);
            assertEquals("", eof.getLexeme());
        }

        @Test
        @DisplayName("EOF is present regardless of how many real tokens precede it")
        void eofAlwaysPresent() {
            assertEndsWithEof(lex("DECLARE INT x"));
            assertEndsWithEof(lex("PRINT: \"hello\""));
            assertEndsWithEof(lex(""));
        }
    }
}
