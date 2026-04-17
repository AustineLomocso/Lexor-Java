package com.lexor.lexer;

import com.lexor.error.LexorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lexer — Converts raw LEXOR source code text into a flat, ordered list of Token objects.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * The Parser cannot work directly on raw characters — it needs structured, classified
 * units of meaning. The Lexer's job is exactly that transformation: it reads the source
 * character-by-character, groups characters into lexemes (the smallest meaningful sequences),
 * and attaches a TokenType label to each one. The resulting List<Token> is the Lexer's
 * only output and the Parser's only input.
 *
 * This separation of concerns is the core principle of the two-phase frontend:
 *   Phase 1 (Lexer)  — "What are the words?"
 *   Phase 2 (Parser) — "What do the words mean together?"
 *
 * ALGORITHM OVERVIEW
 * ------------------
 * The Lexer maintains three cursors as it walks the source string:
 *   pos    — the index of the character currently being examined
 *   line   — the current 1-based line number (incremented on '\n')
 *   column — the current 1-based column within the line (reset to 1 on '\n')
 *
 * For each position it calls scanToken(), which inspects the current character,
 * decides what kind of token is starting, then consumes as many characters as
 * needed to complete that token (this is called "maximal munch").
 *
 * DEPENDENCIES USED IN THIS CLASS
 * --------------------------------
 *
 * 1. SLF4J (org.slf4j.Logger / LoggerFactory)  — Logging
 *    WHY: During development you need to trace exactly what tokens are produced
 *    for a given input. SLF4J + Logback lets you write:
 *        logger.debug("Token produced: {}", token)
 *    and control verbosity from logback.xml without changing code.
 *    At INFO level (production/test) these calls are near-zero cost.
 *    USAGE: Call logger.debug() after every successfully created Token.
 *
 * 2. Apache Commons Lang (StringUtils)  — Character and string utilities
 *    WHY: StringUtils.isAlpha(), isAlphanumeric(), and Character.isDigit() /
 *    Character.isLetter() give clean, readable character-classification one-liners
 *    instead of hand-rolled ASCII range checks.
 *    USAGE: Use Character.isLetter(c) or Character.isDigit(c) inside helper methods
 *    like isAlphaOrUnderscore() and isDigit().
 *    StringUtils.isNumeric() can validate INT literal candidates quickly.
 *
 * HOW IT IS USED BY OTHER CLASSES
 * --------------------------------
 * • Main.java      — calls  new Lexer(source).tokenize()  and passes the result to Parser.
 * • ReplRunner.java — same call per REPL input line.
 * • LexerTest.java  — instantiates Lexer with hand-crafted source strings and asserts
 *                     the returned token list matches expected types and lexemes.
 */
public class Lexer {

    private static final Logger logger = LoggerFactory.getLogger(Lexer.class);
    // =========================================================================
    // TODO STEP 1 — DECLARE INSTANCE FIELDS
    // =========================================================================
    // The Lexer needs to track its progress through the source string.
    //
    //   source  — the full LEXOR source code passed in at construction time;
    //             never modified, only read
    //   tokens  — the growing list of Token objects being built up; returned
    //             at the end of tokenize()
    //   pos     — current character index into source (0-based)
    //   line    — current line number (1-based; starts at 1)
    //   column  — current column within the current line (1-based; starts at 1)
    //
    // TODO 1a: Declare  private final String source;
    // TODO 1b: Declare  private final List<Token> tokens;   (initialize to new ArrayList<>())
    // TODO 1c: Declare  private int pos;                    (initialize to 0)
    // TODO 1d: Declare  private int line;                   (initialize to 1)
    // TODO 1e: Declare  private int column;                 (initialize to 1)
    //
    private final String source;
    private List<Token> tokens = new ArrayList<>();
    private int pos;
    private int line = 1;
    private int column;
    private boolean insideBrackets = false;

    // =========================================================================
    // TODO STEP 2 — RESERVED KEYWORD MAP
    // =========================================================================
    // When the Lexer scans a word (letter/underscore start, alphanumeric/underscore
    // continuation) it cannot immediately know whether the word is a reserved keyword
    // or a user-defined identifier. The simplest and most efficient solution is a
    // static lookup map: if the scanned word is in the map, it is a keyword;
    // otherwise it is an IDENTIFIER.
    //
    // This is the standard "reserved word" pattern used in almost every real lexer.
    //
    // Populate KEYWORDS with every reserved word from the LEXOR spec mapped to
    // its corresponding TokenType:
    //   "SCRIPT"  -> KEYWORD_SCRIPT
    //   "AREA"    -> KEYWORD_AREA
    //   "START"   -> KEYWORD_START
    //   "END"     -> KEYWORD_END
    //   "DECLARE" -> KEYWORD_DECLARE
    //   "PRINT"   -> KEYWORD_PRINT
    //   "SCAN"    -> KEYWORD_SCAN
    //   "IF"      -> KEYWORD_IF
    //   "ELSE"    -> KEYWORD_ELSE
    //   "FOR"     -> KEYWORD_FOR
    //   "REPEAT"  -> KEYWORD_REPEAT
    //   "WHEN"    -> KEYWORD_WHEN
    //   "INT"     -> TYPE_INT
    //   "FLOAT"   -> TYPE_FLOAT
    //   "CHAR"    -> TYPE_CHAR
    //   "BOOL"    -> TYPE_BOOL
    //   "AND"     -> OP_AND
    //   "OR"      -> OP_OR
    //   "NOT"     -> OP_NOT
    //
    // TODO 2a: Declare  private static final Map<String, TokenType> KEYWORDS
    // TODO 2b: Use Map.of() or Map.ofEntries() to populate it with all entries above.
    //          If you exceed 10 entries (Map.of limit), switch to Map.ofEntries(Map.entry(...), ...)
    //

    private static final Map<String, TokenType> keywordMap = Map.ofEntries(
        Map.entry("SCRIPT", TokenType.KEYWORD_SCRIPT),
        Map.entry("AREA", TokenType.KEYWORD_AREA),
        Map.entry("START", TokenType.KEYWORD_START),
        Map.entry("END", TokenType.KEYWORD_END),
        Map.entry("DECLARE", TokenType.KEYWORD_DECLARE),
        Map.entry("PRINT", TokenType.KEYWORD_PRINT),
        Map.entry("SCAN", TokenType.KEYWORD_SCAN),
        Map.entry("IF", TokenType.KEYWORD_IF),
        Map.entry("ELSE", TokenType.KEYWORD_ELSE),
        Map.entry("FOR", TokenType.KEYWORD_FOR),
        Map.entry("REPEAT", TokenType.KEYWORD_REPEAT),
        Map.entry("WHEN", TokenType.KEYWORD_WHEN),

        Map.entry("INT", TokenType.TYPE_INT),
        Map.entry("FLOAT", TokenType.TYPE_FLOAT),
        Map.entry("CHAR", TokenType.TYPE_CHAR),
        Map.entry("BOOL", TokenType.TYPE_BOOL),

        Map.entry("AND", TokenType.OP_AND),
        Map.entry("OR", TokenType.OP_OR),
        Map.entry("NOT", TokenType.OP_NOT)
    );

    // =========================================================================
    // TODO STEP 3 — WRITE THE CONSTRUCTOR
    // =========================================================================
    // The constructor receives the full source string and sets up the Lexer state.
    //
    // TODO 3a: Accept a single String parameter named 'source'.
    // TODO 3b: Assign it to the final field.
    // TODO 3c: Initialize tokens to new ArrayList<>().
    // TODO 3d: Initialize pos, line, column to 0, 1, 1 respectively.
    // TODO 3e: Add a logger.debug() call logging the source length for traceability.
    //

    public Lexer(String source) {
        this.source = source;
    }

    // =========================================================================
    // TODO STEP 4 — IMPLEMENT THE PUBLIC tokenize() METHOD
    // =========================================================================
    // This is the entry point called by Main and tests. It drives the scan loop.
    //
    // Algorithm:
    //   while (not at end of source)
    //       call scanToken()   <- each call advances pos by at least 1
    //   append a final EOF token
    //   log the total token count at DEBUG level
    //   return the tokens list
    //
    // TODO 4a: Write the while loop that calls scanToken() while !isAtEnd().
    // TODO 4b: After the loop, call  addToken(TokenType.EOF, "")  to signal end of input.
    // TODO 4c: Add  logger.debug("Tokenization complete. {} tokens produced.", tokens.size());
    // TODO 4d: Return the tokens list.
    //

    public List<Token> tokenize(){
        while(!isAtEnd()){
            scanToken();
        }

        addToken(TokenType.EOF, "");
        System.out.println("Total token count: " + tokens.size());
        logger.debug("Tokenization complete. {} tokens produced.", tokens.size());
        return tokens;
    }

    // =========================================================================
    // TODO STEP 5 — IMPLEMENT scanToken() — THE CORE DISPATCH METHOD
    // =========================================================================
    // scanToken() reads the CURRENT character (via advance()) and decides what
    // kind of token is starting. This is essentially a large switch statement.
    //
    // Order matters for multi-character tokens:
    //   Check ">=" BEFORE ">"   (otherwise you match ">" and miss the "=")
    //   Check "<=" BEFORE "<"
    //   Check "<>" BEFORE "<"
    //   Check "==" BEFORE "="
    //   Check "%%" BEFORE "%"   ("%%" is a comment, "%" alone is OP_MOD)
    //
    // Steps inside scanToken():
    //
    // TODO 5a: Call  char c = advance();  to consume and return the current character.
    //
    // TODO 5b: Handle WHITESPACE — spaces and tabs.
    //          Just return without adding any token (whitespace is not significant in LEXOR).
    //          Do NOT return here for '\n' — that is handled separately.
    //
    // TODO 5c: Handle NEWLINE ('\n').
    //          Call  addToken(TokenType.NEWLINE, "\\n");
    //          Then increment  line  and reset  column  to 1.
    //          Also handle '\r' (carriage return on Windows) by skipping it silently
    //          if it is followed by '\n'.
    //
    // TODO 5d: Handle COMMENT ('%').
    //          If the NEXT character is also '%' (peek() == '%'):
    //              Consume characters until the end of the line (or end of source).
    //              This discards the entire comment — produce NO token.
    //          If the next character is NOT '%':
    //              Emit  OP_MOD  for the lone '%'.
    //
    // TODO 5e: Handle SINGLE-CHARACTER TOKENS using a switch:
    //          '('  -> LPAREN         ')'  -> RPAREN
    //          '['  -> LBRACKET       ']'  -> RBRACKET
    //          ','  -> COMMA          ':'  -> COLON
    //          '+'  -> OP_PLUS        '*'  -> OP_MUL
    //          '/'  -> OP_DIV
    //          '&'  -> AMPERSAND      '$'  -> DOLLAR
    //

    //
    // TODO 5g: Handle STRING LITERALS (double-quoted).
    //          If c == '"':
    //              Call  readStringLiteral()  — see Step 8.
    //
    // TODO 5h: Handle CHAR LITERALS (single-quoted).
    //          If c == '\'':
    //              Call  readCharLiteral()  — see Step 9.
    //
    // TODO 5i: Handle NUMBER LITERALS (digits).
    //          If Character.isDigit(c):
    //              Call  readNumber(c)  — see Step 10.
    //
    // TODO 5j: Handle WORDS (identifiers and keywords).
    //          If Character.isLetter(c) or c == '_':
    //              Call  readWord(c)  — see Step 11.
    //
    // TODO 5k: If none of the above matched, this is an unknown character.
    //          Throw a LexorException with the message:
    //          "Unexpected character '" + c + "' at line " + line + ", column " + (column-1)
    //

    private void scanToken() throws LexorException {
        char c = advance();
        if(c == ' ' || c == '\t' || c == '\r') return; // WHITESPACE

        switch (c) {
            case '\n':
                addToken(TokenType.NEWLINE, "\\n");
                line++; column = 1;
                break;
            case '%':
                if(!isAtEnd() && peek() == '%') {
                    advance();
                    while (!isAtEnd() && peek() != '\n') {
                        advance();
                    }
                    return; // no token emitted
                } else {
                    addToken(TokenType.OP_MOD, "%");
                    return;
                }
            case '(': addToken(TokenType.LPAREN, "("); break;
            case ')': addToken(TokenType.RPAREN, ")"); break;
            case '[':
                insideBrackets = true;
                addToken(TokenType.LBRACKET, "[");
                break;
            case ']':
                insideBrackets = false;
                addToken(TokenType.RBRACKET, "]");
                break;
            case '#':
                if (insideBrackets) {
                    addToken(TokenType.IDENTIFIER, "#");
                } else {
                    throw new LexorException("Unexpected character '#' at line " + line);
                }
                break;
            case ',': addToken(TokenType.COMMA, ","); break;
            case ':': addToken(TokenType.COLON, ":"); break;
            case '+': addToken(TokenType.OP_PLUS, "+"); break;
            case '*': addToken(TokenType.OP_MUL, "*"); break;
            case '/': addToken(TokenType.OP_DIV, "/"); break;
            case '&': addToken(TokenType.AMPERSAND, "&"); break;
            case '$': addToken(TokenType.DOLLAR, "$"); break;
            case '-': addToken(TokenType.OP_MINUS, "-"); break;
            case '.': break;


            case '=':
                if (!isAtEnd() && peek() == '=') {
                    advance();
                    addToken(TokenType.OP_EQ, "==");
                } else {
                    addToken(TokenType.ASSIGN, "=");
                }
                break;

            case '<':
                if (!isAtEnd() && peek() == '=') {
                    advance();
                    addToken(TokenType.OP_LTE, "<=");
                } else if (!isAtEnd() && peek() == '>') {
                    advance();
                    addToken(TokenType.OP_NEQ, "<>");
                } else {
                    addToken(TokenType.OP_LT, "<");
                }
                break;

            case '>':
                if (!isAtEnd() && peek() == '=') {
                    advance();
                    addToken(TokenType.OP_GTE, ">=");
                } else {
                    addToken(TokenType.OP_GT, ">");
                }
                break;

            case '"':
                readStringLiteral();
                break;
            case '\'':
                line++;
                readCharLiteral();
                break;
            default:
                if(Character.isDigit(c)){
                    readNumber(c);
                } else if (Character.isLetter(c) || c == '_'){
                    readWord(c);
                } else {
                    throw new LexorException("Unexpected character '" + c + "' at line " + line + ", column " + (column-1));
                }
                break;
        }

    }

    // =========================================================================
    // TODO STEP 6 — IMPLEMENT advance()
    // =========================================================================
    // advance() consumes and returns the character at the current pos, then
    // moves pos forward by 1 and increments column.
    //
    // TODO 6a: Read  source.charAt(pos)  into a local variable.
    // TODO 6b: Increment pos.
    // TODO 6c: Increment column.
    // TODO 6d: Return the character.
    //

    private char advance(){
        char c = source.charAt(pos);
        pos++;
        column++;
        return c;
    }

    // =========================================================================
    // TODO STEP 7 — IMPLEMENT peek() AND peekNext()
    // =========================================================================
    // peek() looks at the CURRENT character WITHOUT consuming it (pos does not move).
    // It is used to look ahead by one character to decide between multi-char tokens.
    //
    // peekNext() looks TWO characters ahead without consuming anything.
    // It is used less often (e.g., deciding if we have a float like "3.14").
    //
    // TODO 7a: peek()     — return '\0' if isAtEnd(), else  source.charAt(pos)
    // TODO 7b: peekNext() — return '\0' if pos+1 >= source.length(), else source.charAt(pos+1)
    //

    private char peek(){
        if (isAtEnd()) return '\0';
        return source.charAt(pos);
    }
    private char peekNext(){
        if (pos + 1 >= source.length()) return '\0';
        return source.charAt(pos + 1);
    }
    // =========================================================================
    // TODO STEP 8 — IMPLEMENT readStringLiteral()
    // =========================================================================
    // Called when a '"' has already been consumed by advance() in scanToken().
    // Reads characters until a closing '"' is found.
    //
    // The LEXOR spec uses string literals in two contexts:
    //   1. BOOL declarations: DECLARE BOOL t="TRUE"   — lexeme should be "TRUE" or "FALSE"
    //   2. PRINT segments:    PRINT: "hello"           — lexeme is the text between quotes
    //
    // The Lexer does NOT need to distinguish these two uses — that is the Parser's job.
    // Just emit a STRING_LITERAL with the text between (but NOT including) the quotes.
    //
    // Algorithm:
    //   Collect characters into a StringBuilder while peek() != '"' and !isAtEnd().
    //   If isAtEnd() before finding '"', throw LexorException "Unterminated string literal".
    //   Consume the closing '"' with advance().
    //   Call addToken(STRING_LITERAL, collected text).
    //
    // Special sub-case: if the text inside is exactly "TRUE" or "FALSE" (case-sensitive),
    // emit BOOL_LITERAL instead of STRING_LITERAL so the Parser can identify boolean values
    // directly without extra work.
    //
    // TODO 8a: Create a StringBuilder to collect the literal content.
    // TODO 8b: Loop: while peek() != '"' and !isAtEnd(), call advance() and append to builder.
    // TODO 8c: Check for unterminated literal (isAtEnd() but no closing quote found).
    // TODO 8d: Consume the closing '"' with advance().
    // TODO 8e: Get the collected text as a String.
    // TODO 8f: If text.equals("TRUE") || text.equals("FALSE"), emit BOOL_LITERAL.
    //          Otherwise emit STRING_LITERAL.
    // TODO 8g: Add a logger.debug() call showing the literal value.
    //

    private void readStringLiteral(){
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') line++; // Handle multi-line strings if they exist
            sb.append(advance());
        }
        if (isAtEnd()){
            throw new RuntimeException("Unexpected end of string literal");
        }
        advance();
        String string = sb.toString();
        if (string.equals("TRUE") ||  string.equals("FALSE")) {
            addToken(TokenType.BOOL_LITERAL, string);
        }
        else{
            addToken(TokenType.STRING_LITERAL, string);
        }
    }

    // =========================================================================
    // TODO STEP 9 — IMPLEMENT readCharLiteral()
    // =========================================================================
    // Called when a '\'' (single quote) has already been consumed.
    // A CHAR literal in LEXOR is a single character inside single quotes: 'n' 'c' etc.
    //
    // Algorithm:
    //   If isAtEnd() or peek() == '\'', throw LexorException "Empty char literal".
    //   Read the single character with advance() — store it.
    //   Expect the closing '\'' — if peek() != '\'' throw LexorException "Unterminated char literal".
    //   Consume the closing '\'' with advance().
    //   Call addToken(CHAR_LITERAL, String.valueOf(theCharacter)).
    //
    // TODO 9a: Check that the literal is not empty (peek() == '\'').
    // TODO 9b: Read the one character via advance().
    // TODO 9c: Verify and consume the closing single-quote.
    // TODO 9d: Emit CHAR_LITERAL.
    //

    private void readCharLiteral(){ // naa rani diri para dili ma red ang method hehe
        if(isAtEnd() || peek() == '\''){
            throw new LexorException("Empty char literal");
        }
        char c = advance();
        if(isAtEnd() || peek() != '\''){
            throw new LexorException("Unexpected character literal");
        }
        advance();
        addToken(TokenType.CHAR_LITERAL, String.valueOf(c));
    }

    // =========================================================================
    // TODO STEP 10 — IMPLEMENT readNumber(char firstDigit)
    // =========================================================================
    // Called when the first digit has already been consumed by scanToken().
    // 'firstDigit' is that already-consumed character and must be included
    // at the start of the number string.
    //
    // A number in LEXOR is either:
    //   INT   — one or more digits:                 e.g.  4  100  200
    //   FLOAT — digits, one '.', then more digits:  e.g.  3.14  0.5
    //
    // Algorithm:
    //   Append firstDigit to a StringBuilder.
    //   While peek() is a digit, advance() and append.
    //   If peek() == '.' AND peekNext() is a digit:
    //       append '.' and advance()
    //       continue consuming digits (the fractional part)
    //       emit FLOAT_LITERAL
    //   Else:
    //       emit INT_LITERAL
    //
    // TODO 10a: Build the integer part using a loop over Character.isDigit(peek()).
    // TODO 10b: Check for a decimal point indicating a FLOAT.
    // TODO 10c: If FLOAT, consume '.' and all following digits, then emit FLOAT_LITERAL.
    // TODO 10d: Otherwise emit INT_LITERAL.
    //

    private void readNumber(char c){
        StringBuilder sb = new StringBuilder();
        sb.append(c);

        while(!isAtEnd() && Character.isDigit(peek())){
            sb.append(advance());
        }

        if(!isAtEnd() && peek() == '.' && Character.isDigit(peekNext())){
            sb.append(advance());
            while(!isAtEnd() && Character.isDigit(peek())){
                sb.append(advance());
            }

            addToken(TokenType.FLOAT_LITERAL, sb.toString());
        } else {
            addToken(TokenType.INT_LITERAL, sb.toString());
        }
    }

    // =========================================================================
    // TODO STEP 11 — IMPLEMENT readWord(char firstChar)
    // =========================================================================
    // Called when the first letter or underscore has already been consumed.
    //
    // LEXOR identifiers: start with letter or '_', continue with letters, digits, or '_'.
    // All reserved keywords are ALL-CAPS words. Since LEXOR is case-sensitive, an
    // identifier "int" (lowercase) is NOT the keyword "INT".
    //
    // Algorithm:
    //   Append firstChar to a StringBuilder.
    //   While peek() is a letter, digit, or '_', advance() and append.
    //   Look up the completed word in the KEYWORDS map.
    //   If found: emit the keyword's TokenType.
    //   If not found: emit IDENTIFIER.
    //
    // TODO 11a: Build the word using Character.isLetterOrDigit(peek()) || peek() == '_'.
    // TODO 11b: Look up the word in KEYWORDS using KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER).
    // TODO 11c: Call addToken with the resolved type and the word as the lexeme.
    // TODO 11d: Log at DEBUG: "Word scanned: '{}' -> {}", word, type
    //
    private void readWord(char c){
        StringBuilder sb = new StringBuilder();
        sb.append(c);
        while(!isAtEnd() &&  Character.isLetter(peek()) ||  peek() == '_' || Character.isDigit(peek())){
            sb.append(advance());
        }
        String word = sb.toString();
        TokenType type = keywordMap.getOrDefault(word, TokenType.IDENTIFIER);
        addToken(type, word);
    }

    // =========================================================================
    // TODO STEP 12 — IMPLEMENT addToken()
    // =========================================================================
    // A private helper that constructs a Token and appends it to the tokens list.
    // It captures the CURRENT line and column at the moment the token is complete.
    //
    // Note: by the time addToken() is called, advance() has already moved pos and
    // column past the last character of the token. The column stored in the token
    // therefore points to the position AFTER the token. If exact start-column
    // reporting is needed, you can track a 'tokenStartColumn' field in scanToken().
    // For LEXOR's purposes, approximate column is fine for error messages.


    // TODO 12a: Create  new Token(type, lexeme, line, column)
    // TODO 12b: Add it to the tokens list.
    // TODO 12c: Log at DEBUG: "Added token: {}", token
    //
    private void addToken(TokenType type, String input){
        Token token = new Token(type, input, this.line, this.column);
        tokens.add(token);
    }

    // =========================================================================
    // TODO STEP 13 — IMPLEMENT isAtEnd()
    // =========================================================================
    // Returns true when pos has reached or passed the end of the source string.
    // Used as the loop condition in tokenize() and as a guard in all read methods.
    //
    // TODO 13a: return  pos >= source.length();
    //
    private boolean isAtEnd(){
        return  pos >= source.length();
    }
}