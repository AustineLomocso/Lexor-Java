package com.lexor.parser;

import com.lexor.error.ParseException;
import com.lexor.lexer.Token;
import com.lexor.lexer.TokenType;
import com.lexor.parser.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser — Transforms a flat List<Token> into a structured Abstract Syntax Tree (AST).
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * WHY THIS CLASS EXISTS
 * ══════════════════════════════════════════════════════════════════════════════
 * The Lexer gave us a stream of tokens — each one classified and labelled, but
 * with no sense of structure. The Parser's job is to impose grammar on that
 * stream: to recognise patterns like "DECLARE INT x=5" or "IF (x < 10)" and
 * build tree objects that represent those constructs.
 *
 * The result — a ProgramNode tree — is handed to the SemanticAnalyzer and then
 * the Interpreter. Neither of those stages ever looks at tokens again.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * WHY RECURSIVE DESCENT?
 * ══════════════════════════════════════════════════════════════════════════════
 * LEXOR's grammar is LL(1) — every construct can be identified by looking at
 * just the next token. Recursive descent is the most natural implementation of
 * an LL(1) grammar:
 *
 *   • Each grammar rule becomes one Java method.
 *   • The method call stack mirrors the nesting of the grammar.
 *   • Operator precedence is encoded by the order of method calls — no
 *     precedence table needed.
 *   • Errors are easy to report: every method knows exactly what it expected.
 *
 * Alternative approaches (table-driven LL parsers, LALR parser generators like
 * ANTLR) would be overkill for a language of this size and complexity.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * EXPRESSION PRECEDENCE LADDER (lowest → highest)
 * ══════════════════════════════════════════════════════════════════════════════
 * The expression parsing methods are arranged so that each level calls the one
 * ABOVE it in the precedence chain. The deepest method evaluates first.
 *
 *   parseExpression()        ← entry point — calls parseLogical()
 *     parseLogical()         ← AND, OR
 *       parseUnaryLogical()  ← NOT (unary logical)
 *         parseRelational()  ← >, <, >=, <=, ==, <>
 *           parseAdditive()  ← +, -
 *             parseMultiplicative()  ← *, /, %
 *               parseUnaryArith()   ← unary +, unary -
 *                 parsePrimary()    ← literals, identifiers, (expr)
 *
 * This ordering means multiplication binds tighter than addition, relational
 * tighter than logical — matching the LEXOR spec's operator precedence table.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * DEPENDENCIES USED IN THIS CLASS
 * ══════════════════════════════════════════════════════════════════════════════
 * • SLF4J (Logger / LoggerFactory)
 *     WHY: Log every major parse decision at DEBUG level. During development
 *     you can trace "parseIf() entered at line 4" without adding print statements.
 *     The logger is the same one used in Lexer — controlled by logback.xml.
 *     USAGE: logger.debug("Parsing IF at line {}", peek().getLine());
 *
 * • ParseException (com.lexor.error)
 *     WHY: All syntax errors must carry the line/column of the offending token
 *     so the student gets useful error messages. ParseException extends
 *     LexorException which carries that metadata.
 *     USAGE: throw error("Expected END IF at line " + peek().getLine());
 *
 * • java.util.List / ArrayList
 *     WHY: Declaration lists, statement blocks, PRINT segments, and SCAN
 *     variable lists are all modelled as ArrayList during parsing, then stored
 *     as List<> in the nodes.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * HOW NODES ARE BUILT — QUICK REFERENCE
 * ══════════════════════════════════════════════════════════════════════════════
 * Every parse method returns an ASTNode. The type of node it builds is:
 *
 *   parse()                 → new ProgramNode(line, declarations, statements)
 *   parseDeclaration()      → new DeclarationNode(line, typeName, name, initializer)
 *   parseStatement()        → dispatches, returns result of called method
 *   parsePrint()            → new PrintNode(line, segments)
 *   parseScan()             → new ScanNode(line, variables)
 *   parseAssignment()       → new AssignmentNode(line, targets, value)
 *   parseIf()               → new IfNode(line, cond, then, elseIfs, elseBlock)
 *   parseFor()              → new ForNode(line, init, cond, update, body)
 *   parseRepeat()           → new RepeatNode(line, condition, body)
 *   parseExpression() …     → new BinaryExprNode(line, left, op, right)
 *   parseUnaryArith/Logical → new UnaryExprNode(line, op, operand)
 *   parsePrimary()          → new LiteralNode | VariableNode | (recurse)
 */
public class Parser {

    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    // =========================================================================
    // TODO STEP 1 — DECLARE FIELDS
    // =========================================================================
    // The Parser needs two things and nothing else:
    //
    //   tokens  — the immutable list of Token objects from the Lexer.
    //             Never modified; we only move `current` forward.
    //
    //   current — a zero-based index into `tokens` pointing to the token
    //             we are about to examine. Starts at 0.
    //             Advances via advance(). Never goes backwards.
    //
    // Why not use an Iterator<Token>?
    //   An index lets us implement peek() cheaply (tokens.get(current)) and
    //   previous() (tokens.get(current-1)) without extra bookkeeping.
    //
    // TODO 1a: Declare  private final List<Token> tokens;
    // TODO 1b: Declare  private int current = 0;
    //

    // =========================================================================
    // TODO STEP 2 — CONSTRUCTOR
    // =========================================================================
    // Accept the token list, assign it, log the count at DEBUG level.
    //
    // TODO 2a: public Parser(List<Token> tokens)
    // TODO 2b: this.tokens = tokens;
    // TODO 2c: logger.debug("Parser initialized with {} tokens.", tokens.size());
    //



    // =========================================================================
    // =========================================================================
    //  SECTION A — HELPER METHODS
    //  These are the building blocks. Implement them FIRST.
    //  Every grammar method below depends on them.
    // =========================================================================
    // =========================================================================

    // -------------------------------------------------------------------------
    // TODO STEP 3 — peek()
    // -------------------------------------------------------------------------
    // Returns the CURRENT token WITHOUT consuming it.
    // This is the single most-used method in the parser.
    //
    // WHY: Before deciding which grammar rule applies, we look at the next token
    // without committing to consuming it. e.g. "is the next token IF?"
    //
    // Implementation:  return tokens.get(current);
    //
    // TODO 3a: private Token peek()
    //


    // -------------------------------------------------------------------------
    // TODO STEP 4 — previous()
    // -------------------------------------------------------------------------
    // Returns the token JUST BEFORE the current position — i.e. the last token
    // that was consumed by advance().
    //
    // WHY: After calling match() or advance() we often need to know what token
    // was just matched. For example, after match(OP_PLUS), previous().getLexeme()
    // gives us "+" to store in BinaryExprNode.
    //
    // Implementation:  return tokens.get(current - 1);
    //
    // TODO 4a: private Token previous()


    // -------------------------------------------------------------------------
    // TODO STEP 5 — isAtEnd()
    // -------------------------------------------------------------------------
    // Returns true if the current token is EOF.
    //
    // WHY: Every loop in the parser must terminate. Checking isAtEnd() prevents
    // infinite loops when malformed input is missing a closing keyword.
    //
    // Implementation:  return peek().isType(TokenType.EOF);
    //
    // TODO 5a: private boolean isAtEnd()
    //

    // -------------------------------------------------------------------------
    // TODO STEP 6 — check(TokenType type)
    // -------------------------------------------------------------------------
    // Returns true if the CURRENT token has the given type, WITHOUT consuming it.
    //
    // WHY: Lets us conditionally branch without permanently advancing.
    //      "If the next token is IF, enter parseIf() — but don't consume IF yet,
    //       parseIf() will do that."
    //
    // Implementation:
    //   if (isAtEnd()) return false;
    //   return peek().isType(type);
    //
    // TODO 6a: private boolean check(TokenType type)
    //


    // -------------------------------------------------------------------------
    // TODO STEP 7 — advance()
    // -------------------------------------------------------------------------
    // CONSUMES and returns the current token, then moves current forward by 1.
    //
    // WHY: This is how the parser "reads" a token. Called by match() and
    // consume() but never directly by grammar methods (they use match/consume).
    //
    // Implementation:
    //   if (!isAtEnd()) current++;
    //   return previous();
    //
    // TODO 7a: private Token advance()
    //

    // -------------------------------------------------------------------------
    // TODO STEP 8 — match(TokenType... types)
    // -------------------------------------------------------------------------
    // If the CURRENT token matches ANY of the given types, consume it and return
    // true. Otherwise, do nothing and return false.
    //
    // WHY: Used for optional or multi-option tokens, e.g.:
    //   if (match(OP_PLUS, OP_MINUS)) { // is this + or - ?
    //       String op = previous().getLexeme();
    //       ...
    //   }
    //
    // The varargs signature allows checking multiple alternatives in one call.
    //
    // Implementation:
    //   for (TokenType type : types) {
    //       if (check(type)) { advance(); return true; }
    //   }
    //   return false;
    //
    // TODO 8a: private boolean match(TokenType... types)
    //


    // -------------------------------------------------------------------------
    // TODO STEP 9 — consume(TokenType type, String message)
    // -------------------------------------------------------------------------
    // ASSERT that the current token is of the expected type, then consume it.
    // If the token does NOT match, throw a ParseException.
    //
    // WHY: When we KNOW a token must be present (e.g., END IF must follow a
    // block), we use consume() rather than match(). A mismatch is a syntax error.
    //
    // This is the primary source of ParseException in the parser. The `message`
    // parameter becomes the error text shown to the student.
    //
    // Implementation:
    //   if (check(type)) return advance();
    //   throw error("Expected " + message + " but found '" + peek().getLexeme()
    //               + "' at line " + peek().getLine());
    //
    // TODO 9a: private Token consume(TokenType type, String message)
    //

    // -------------------------------------------------------------------------
    // TODO STEP 10 — skipNewlines()
    // -------------------------------------------------------------------------
    // Consumes all consecutive NEWLINE tokens without producing any AST output.
    //
    // WHY: LEXOR is line-delimited — every physical line ends with a NEWLINE
    // token. But blank lines, comment lines, and the lines holding structural
    // keywords (SCRIPT AREA, START IF, END FOR …) also produce NEWLINE tokens
    // that are not meaningful to the grammar. Calling skipNewlines() at the
    // right moments keeps grammar methods clean.
    //
    // Call skipNewlines():
    //   • At the start of parse() before processing declarations.
    //   • Between statements inside a block.
    //   • After consuming a keyword-only line (START IF, END FOR, etc.)
    //
    // Implementation:
    //   while (check(TokenType.NEWLINE)) advance();
    //
    // TODO 10a: private void skipNewlines()
    //


    // -------------------------------------------------------------------------
    // TODO STEP 11 — error(String message)
    // -------------------------------------------------------------------------
    // Creates and returns a ParseException carrying the message.
    // Grammar methods throw the returned exception immediately:
    //   throw error("Expected COLON after PRINT");
    //
    // WHY return rather than throw? Some call sites benefit from   throw error()
    // syntax for control flow, while others (in switch expressions) prefer
    // returning the exception object.
    //
    // Implementation:
    //   Token t = peek();
    //   return new ParseException(message, t.getLine(), t.getColumn());
    //
    // TODO 11a: private ParseException error(String message)
    //



    // =========================================================================
    // =========================================================================
    //  SECTION B — TOP-LEVEL PROGRAM STRUCTURE
    // =========================================================================
    // =========================================================================

    // -------------------------------------------------------------------------
    // TODO STEP 12 — parse()   [PUBLIC ENTRY POINT]
    // -------------------------------------------------------------------------
    // Called by Main.java and tests. Drives the entire parse and returns the
    // root ProgramNode. This is the ONLY public method.
    //
    // LEXOR program structure:
    //   SCRIPT AREA
    //   START SCRIPT
    //     <declarations>   ← MUST come first; cannot be placed elsewhere
    //     <statements>
    //   END SCRIPT
    //
    // ALGORITHM:
    //   1. skipNewlines()
    //   2. consume(KEYWORD_SCRIPT, "SCRIPT")
    //   3. consume(KEYWORD_AREA,   "AREA")
    //   4. skipNewlines()
    //   5. consume(KEYWORD_START,  "START")
    //   6. consume(KEYWORD_SCRIPT, "SCRIPT")
    //   7. skipNewlines()
    //   8. Collect declarations:
    //        while peek() is KEYWORD_DECLARE → add parseDeclaration() to list
    //        skip newlines between declarations
    //   9. Collect statements:
    //        while peek() is not KEYWORD_END and not EOF
    //            → skipNewlines(), then add parseStatement() to list
    //  10. consume(KEYWORD_END,    "END")
    //  11. consume(KEYWORD_SCRIPT, "SCRIPT")
    //  12. return new ProgramNode(1, declarations, statements)
    //
    // NODE BUILT:
    //   new ProgramNode(
    //       line,                        ← use 1 (program always starts at line 1)
    //       declarations,                ← List<DeclarationNode>
    //       statements                   ← List<ASTNode>
    //   )
    //
    // TODO 12a: Implement the full parse() method following the algorithm above.
    // TODO 12b: Log "Parsing program structure" at DEBUG before starting.
    // TODO 12c: Log "Program parsed: {} declarations, {} statements" at DEBUG when done.
    //



    // =========================================================================
    // =========================================================================
    //  SECTION C — DECLARATIONS
    // =========================================================================
    // =========================================================================

    // -------------------------------------------------------------------------
    // TODO STEP 13 — parseDeclaration()
    // -------------------------------------------------------------------------
    // Parses a single DECLARE line and returns ALL variable nodes declared on it.
    // Returns List<DeclarationNode> (not just one) because LEXOR allows:
    //   DECLARE INT x, y, z=5   ← three variables on one line
    //
    // WHY return a List?
    //   A single DECLARE line may declare many variables. Returning a list lets
    //   parse() call declarations.addAll(parseDeclaration()) cleanly.
    //
    // ALGORITHM:
    //   1. consume(KEYWORD_DECLARE, "DECLARE")
    //   2. Read the type token — must be one of TYPE_INT, TYPE_FLOAT, TYPE_CHAR,
    //      TYPE_BOOL. Use consume() with a descriptive error if none match.
    //      String typeName = advance().getLexeme()  after confirming it is a type.
    //   3. Loop — parse comma-separated variable declarations:
    //      a. consume(IDENTIFIER, "variable name") → get name
    //      b. ASTNode initializer = null
    //      c. if match(ASSIGN):           ← optional = initializer
    //             initializer = parsePrimary()   ← only literals allowed here
    //             (the spec shows only literals as initializers: z=5, 'n', "TRUE")
    //      d. Add new DeclarationNode(line, typeName, name, initializer) to list
    //      e. if match(COMMA) → continue loop, else break
    //   4. Consume end-of-line: consume or skip NEWLINE
    //   5. Return the list
    //
    // NODE BUILT (one per variable name):
    //   new DeclarationNode(
    //       token.getLine(),    ← line of the DECLARE keyword
    //       typeName,           ← "INT", "FLOAT", "CHAR", or "BOOL"
    //       name,               ← the identifier string, e.g. "x"
    //       initializer         ← a LiteralNode, or null if no '=' was present
    //   )
    //
    // EXAMPLE: DECLARE INT x, y, z=5
    //   → DeclarationNode(INT, x, null)
    //   → DeclarationNode(INT, y, null)
    //   → DeclarationNode(INT, z, LiteralNode(5, INT))
    //
    // TODO 13a: Implement parseDeclaration() following the algorithm above.
    // TODO 13b: Validate that the next token IS a type keyword; throw error if not.
    // TODO 13c: Log "Parsing declaration of type {}" at DEBUG.
    //



    // =========================================================================
    // =========================================================================
    //  SECTION D — STATEMENT DISPATCHER
    // =========================================================================
    // =========================================================================

    // -------------------------------------------------------------------------
    // TODO STEP 14 — parseStatement()
    // -------------------------------------------------------------------------
    // Looks at the CURRENT token and dispatches to the correct specific parser.
    // This is the "router" of the statement level.
    //
    // WHY a separate dispatcher?
    //   Keeping all the routing logic in one place makes it easy to add new
    //   statement types. Each specific parse method focuses only on its own
    //   grammar without caring about what came before.
    //
    // DISPATCH TABLE:
    //   KEYWORD_PRINT  → parsePrint()
    //   KEYWORD_SCAN   → parseScan()
    //   KEYWORD_IF     → parseIf()
    //   KEYWORD_FOR    → parseFor()
    //   KEYWORD_REPEAT → parseRepeat()
    //   IDENTIFIER     → parseAssignment()
    //     (any non-keyword token at statement level must be an assignment)
    //   anything else  → throw error("Unexpected token at statement level")
    //
    // TODO 14a: Implement the dispatch using if/else-if on check() calls.
    //           Do NOT consume the token here — let each method do that.
    // TODO 14b: Log "Dispatching statement at line {} token {}" at DEBUG.
    //



    // =========================================================================
    // =========================================================================
    //  SECTION E — INDIVIDUAL STATEMENT PARSERS
    // =========================================================================
    // =========================================================================

    // -------------------------------------------------------------------------
    // TODO STEP 15 — parsePrint()
    // -------------------------------------------------------------------------
    // Parses:   PRINT: <segment> & <segment> & ...
    //
    // WHY a segment list?
    //   PRINT output is not a single expression — it is multiple values
    //   concatenated with '&'. Each segment is independently evaluated and
    //   converted to a string at runtime. The Interpreter joins them.
    //
    // THREE KINDS OF PRINT SEGMENT:
    //   A) Normal expression (variable, literal, arithmetic) → parseExpression()
    //   B) Dollar sign ($)  → newline marker
    //      Represent as: new LiteralNode(line, "\n", "NEWLINE_MARKER")
    //   C) Escape code [x]  → literal character output, e.g. [#] outputs '#'
    //      Pattern: LBRACKET <any-token> RBRACKET
    //      Represent as: new LiteralNode(line, escapedChar, "ESCAPE")
    //
    // ALGORITHM:
    //   1. consume(KEYWORD_PRINT, "PRINT")
    //   2. consume(COLON, "':' after PRINT")
    //   3. Loop — parse segments:
    //      a. If match(DOLLAR)   → add LiteralNode("\n", "NEWLINE_MARKER")
    //      b. If match(LBRACKET):
    //             Token inner = advance()  ← any token inside the brackets
    //             consume(RBRACKET, "']' to close escape code")
    //             add LiteralNode(inner.getLexeme(), "ESCAPE")
    //      c. Else → add parseExpression()
    //      d. If match(AMPERSAND) → continue loop, else break
    //   4. consume or skip NEWLINE
    //   5. return new PrintNode(line, segments)
    //
    // NODE BUILT:
    //   new PrintNode(
    //       line,           ← line of the PRINT keyword
    //       segments        ← List<ASTNode>, one per &-separated segment
    //   )
    //
    // TODO 15a: Implement parsePrint() following the algorithm above.
    // TODO 15b: Handle all three segment types.
    // TODO 15c: Log segment count at DEBUG when done.
    //


    // -------------------------------------------------------------------------
    // TODO STEP 16 — parseScan()
    // -------------------------------------------------------------------------
    // Parses:   SCAN: <varName> , <varName> , ...
    //
    // WHY only identifiers (not expressions)?
    //   SCAN writes INTO variables. The targets must be l-values — storage
    //   locations. In LEXOR that means plain variable names only, never
    //   complex expressions.
    //
    // ALGORITHM:
    //   1. consume(KEYWORD_SCAN, "SCAN")
    //   2. consume(COLON, "':' after SCAN")
    //   3. Loop — collect variable names:
    //      a. consume(IDENTIFIER, "variable name") → add name to list
    //      b. if match(COMMA) → continue, else break
    //   4. consume or skip NEWLINE
    //   5. return new ScanNode(line, variables)
    //
    // NODE BUILT:
    //   new ScanNode(
    //       line,       ← line of the SCAN keyword
    //       variables   ← List<String>, e.g. ["x", "y"]
    //   )
    //
    // TODO 16a: Implement parseScan() following the algorithm above.
    //


    // -------------------------------------------------------------------------
    // TODO STEP 17 — parseAssignment()
    // -------------------------------------------------------------------------
    // Parses:  x = <expr>    OR    x = y = <expr>   (chained)
    //
    // WHY collect a targets list?
    //   LEXOR allows chained assignment:  x=y=4
    //   All targets receive the same value. We collect all the identifiers
    //   on the left side, then evaluate the rightmost expression ONCE.
    //   The Interpreter walks targets right-to-left, storing the value in each.
    //
    // HOW TO DETECT CHAINING:
    //   After consuming IDENTIFIER and ASSIGN, peek TWO tokens ahead:
    //     If pattern is  IDENTIFIER ASSIGN  again → another target → loop
    //     Otherwise → this is the value expression
    //
    //   Simpler approach: greedily consume (IDENTIFIER ASSIGN) pairs as long
    //   as tokens.get(current) is IDENTIFIER AND tokens.get(current+1) is ASSIGN.
    //   This requires a lookAhead(offset) helper (see TODO 17e).
    //
    // ALGORITHM:
    //   1. Save line number
    //   2. Loop — while current token is IDENTIFIER and next is ASSIGN:
    //        a. targets.add(consume(IDENTIFIER).getLexeme())
    //        b. consume(ASSIGN, "=")
    //   3. value = parseExpression()   ← parse the final value
    //   4. consume or skip NEWLINE
    //   5. return new AssignmentNode(line, targets, value)
    //
    // NODE BUILT:
    //   new AssignmentNode(
    //       line,      ← line where assignment begins
    //       targets,   ← List<String> e.g. ["x", "y"] for x=y=4
    //       value      ← ASTNode — the expression whose result is stored
    //   )
    //
    // TODO 17a: Implement parseAssignment() following the algorithm above.
    // TODO 17b: Ensure at least ONE target is collected before parsing the value.
    // TODO 17c: Throw an error if the loop collects zero targets (should not happen
    //           given parseStatement() only routes here for IDENTIFIER).
    // TODO 17d: Log the targets list and value type at DEBUG.
    // TODO 17e: Implement a private helper:
    //           private boolean checkAhead(int offset, TokenType type)
    //           that returns tokens.get(current + offset).isType(type)
    //           guarded by a bounds check. Use it to detect chaining.
    //


    // -------------------------------------------------------------------------
    // TODO STEP 18 — parseIf()
    // -------------------------------------------------------------------------
    // Parses the full IF / ELSE IF / ELSE construct.
    //
    // STRUCTURE:
    //   IF (<condition>)
    //   START IF
    //     <statements>
    //   END IF
    //   [ELSE IF (<condition>)           ← zero or more
    //    START IF <statements> END IF]
    //   [ELSE                            ← optional
    //    START IF <statements> END IF]
    //
    // DETECTING ELSE vs ELSE IF:
    //   After END IF, check:
    //     peek() is KEYWORD_ELSE  AND  tokens.get(current+1) is KEYWORD_IF
    //       → ELSE IF branch (consume ELSE, then parseElseIfClause())
    //     peek() is KEYWORD_ELSE  (but next is NOT KEYWORD_IF)
    //       → plain ELSE branch
    //     otherwise → no more branches
    //
    // ALGORITHM:
    //   1. consume(KEYWORD_IF, "IF")
    //   2. consume(LPAREN, "'(' after IF")
    //   3. condition = parseExpression()
    //   4. consume(RPAREN, "')' after condition")
    //   5. skipNewlines()
    //   6. thenBlock = parseBlock("IF")    ← see parseBlock() step 19
    //   7. Loop — while peek() is KEYWORD_ELSE:
    //      a. If checkAhead(1, KEYWORD_IF):
    //             advance() [ELSE], advance() [IF]
    //             consume LPAREN, cond = parseExpression(), consume RPAREN
    //             skipNewlines()
    //             body = parseBlock("IF")
    //             elseIfClauses.add(new IfNode.ElseIfClause(cond, body))
    //      b. Else (plain ELSE):
    //             advance() [ELSE]
    //             skipNewlines()
    //             elseBlock = parseBlock("IF")
    //             break
    //   8. return new IfNode(line, condition, thenBlock, elseIfClauses, elseBlock)
    //
    // NODE BUILT:
    //   new IfNode(
    //       line,
    //       condition,        ← ASTNode — must evaluate to BOOL at runtime
    //       thenBlock,        ← List<ASTNode>
    //       elseIfClauses,    ← List<IfNode.ElseIfClause> (may be empty)
    //       elseBlock         ← List<ASTNode> or null if no ELSE
    //   )
    //
    // TODO 18a: Implement parseIf() following the algorithm above.
    // TODO 18b: Log "Parsing IF at line {}" at DEBUG.
    //


    // -------------------------------------------------------------------------
    // TODO STEP 19 — parseBlock(String keyword)
    // -------------------------------------------------------------------------
    // Parses:  START <keyword>  <statements>  END <keyword>
    //
    // WHY a shared method?
    //   IF, FOR, and REPEAT all have the same START … END block structure.
    //   One method handles all three; the keyword string ("IF", "FOR", "REPEAT")
    //   is used to produce precise error messages.
    //
    // ALGORITHM:
    //   1. consume(KEYWORD_START, "START")
    //   2. Consume the keyword token matching `keyword` param
    //      (for "IF" → consume KEYWORD_IF; for "FOR" → consume KEYWORD_FOR;
    //       for "REPEAT" → consume KEYWORD_REPEAT)
    //   3. skipNewlines()
    //   4. Loop — while not END and not EOF:
    //        skipNewlines()
    //        if peek() is KEYWORD_END → break
    //        statements.add(parseStatement())
    //        skipNewlines()
    //   5. consume(KEYWORD_END,    "END " + keyword)
    //   6. Consume the matching keyword token (same as step 2)
    //   7. skipNewlines()
    //   8. return statements
    //
    // TODO 19a: Implement parseBlock(String keyword).
    // TODO 19b: Map keyword string → TokenType for the START and END tokens.
    //           Use a helper or a switch:
    //             "IF"     → KEYWORD_IF
    //             "FOR"    → KEYWORD_FOR
    //             "REPEAT" → KEYWORD_REPEAT
    //


    // -------------------------------------------------------------------------
    // TODO STEP 20 — parseFor()
    // -------------------------------------------------------------------------
    // Parses:  FOR (<init>, <condition>, <update>)  START FOR … END FOR
    //
    // The FOR header contains THREE comma-separated clauses inside parentheses.
    //   init       — an assignment:    i=0
    //   condition  — a BOOL expression: i<10
    //   update     — an assignment:    i=i+1
    //
    // WHY AssignmentNode for init and update?
    //   The spec explicitly shows assignments in those positions.
    //   Using AssignmentNode keeps them consistent with standalone assignments
    //   and lets the Interpreter call visitAssignment() on them directly.
    //
    // ALGORITHM:
    //   1. consume(KEYWORD_FOR, "FOR")
    //   2. consume(LPAREN, "'(' after FOR")
    //   3. init      = parseForAssignment()   ← see TODO 20b
    //   4. consume(COMMA, "','")
    //   5. condition = parseExpression()
    //   6. consume(COMMA, "','")
    //   7. update    = parseForAssignment()
    //   8. consume(RPAREN, "')' after FOR header")
    //   9. skipNewlines()
    //  10. body = parseBlock("FOR")
    //  11. return new ForNode(line, init, condition, update, body)
    //
    // NODE BUILT:
    //   new ForNode(
    //       line,
    //       init,        ← AssignmentNode e.g. i=0
    //       condition,   ← ASTNode e.g. BinaryExprNode(i < 10)
    //       update,      ← AssignmentNode e.g. i=i+1
    //       body         ← List<ASTNode>
    //   )
    //
    // TODO 20a: Implement parseFor().
    // TODO 20b: Implement a private helper parseForAssignment() that parses
    //           a single  "identifier = expression"  assignment inside the FOR
    //           header (no NEWLINE consumed at the end since the header continues).
    //           Returns AssignmentNode.
    //


    // -------------------------------------------------------------------------
    // TODO STEP 21 — parseRepeat()
    // -------------------------------------------------------------------------
    // Parses:  REPEAT WHEN (<condition>)  START REPEAT … END REPEAT
    //
    // WHY PRE-TEST (while) semantics?
    //   The spec shows REPEAT WHEN with the condition BEFORE the body — this is
    //   a while loop, not a do-while. The Interpreter checks the condition first;
    //   if false from the start, the body never executes.
    //
    // ALGORITHM:
    //   1. consume(KEYWORD_REPEAT, "REPEAT")
    //   2. consume(KEYWORD_WHEN,   "WHEN")
    //   3. consume(LPAREN, "'('")
    //   4. condition = parseExpression()
    //   5. consume(RPAREN, "')'")
    //   6. skipNewlines()
    //   7. body = parseBlock("REPEAT")
    //   8. return new RepeatNode(line, condition, body)
    //
    // NODE BUILT:
    //   new RepeatNode(
    //       line,
    //       condition,   ← ASTNode — must evaluate to BOOL
    //       body         ← List<ASTNode>
    //   )
    //
    // TODO 21a: Implement parseRepeat().
    //



    // =========================================================================
    // =========================================================================
    //  SECTION F — EXPRESSION PRECEDENCE LADDER
    //  Read bottom-up: parsePrimary() runs first, parseLogical() runs last.
    // =========================================================================
    // =========================================================================

    // -------------------------------------------------------------------------
    // TODO STEP 22 — parseExpression()   [ENTRY POINT FOR EXPRESSIONS]
    // -------------------------------------------------------------------------
    // Top of the precedence ladder. Just delegates to parseLogical().
    //
    // WHY have this wrapper?
    //   It provides a single stable entry point. If LEXOR ever adds a new
    //   precedence level above logical (e.g. ternary), we insert it here
    //   without touching call sites that use parseExpression().
    //
    // Implementation: return parseLogical();
    //
    // TODO 22a: Implement parseExpression() as a delegation to parseLogical().
    //

    // -------------------------------------------------------------------------
    // TODO STEP 23 — parseLogical()
    // -------------------------------------------------------------------------
    // Handles:  <expr> AND <expr>   |   <expr> OR <expr>
    //
    // AND and OR are LEFT-ASSOCIATIVE:  a AND b AND c  →  (a AND b) AND c
    //
    // ALGORITHM (standard left-recursive elimination loop):
    //   1. left = parseUnaryLogical()
    //   2. while match(OP_AND, OP_OR):
    //        op = previous().getLexeme()   ← "AND" or "OR"
    //        right = parseUnaryLogical()
    //        left = new BinaryExprNode(line, left, op, right)
    //   3. return left
    //
    // WHY a loop instead of recursion?
    //   Recursive calls for left-associative operators would build right-leaning
    //   trees. A loop naturally builds left-leaning trees:
    //     a AND b AND c  becomes  BinaryExpr(BinaryExpr(a,AND,b), AND, c)
    //
    // NODE BUILT:
    //   new BinaryExprNode(
    //       line,        ← line of the left operand
    //       left,        ← ASTNode
    //       "AND"/"OR",  ← operator string
    //       right        ← ASTNode
    //   )
    //
    // TODO 23a: Implement parseLogical().
    //


    // -------------------------------------------------------------------------
    // TODO STEP 24 — parseUnaryLogical()
    // -------------------------------------------------------------------------
    // Handles:  NOT <expr>
    //
    // NOT is a PREFIX unary operator — it applies to the expression to its right.
    // It has higher precedence than AND/OR but lower than relational operators.
    //
    // ALGORITHM:
    //   if match(OP_NOT):
    //       op = previous().getLexeme()    ← "NOT"
    //       operand = parseUnaryLogical()  ← allows NOT NOT x (double negation)
    //       return new UnaryExprNode(line, op, operand)
    //   return parseRelational()
    //
    // NODE BUILT (when NOT is present):
    //   new UnaryExprNode(
    //       line,
    //       "NOT",
    //       operand    ← ASTNode
    //   )
    //
    // TODO 24a: Implement parseUnaryLogical().
    //


    // -------------------------------------------------------------------------
    // TODO STEP 25 — parseRelational()
    // -------------------------------------------------------------------------
    // Handles:  <expr> > <expr>  |  >= | < | <= | == | <>
    //
    // Relational operators are NON-ASSOCIATIVE in LEXOR — you cannot chain:
    //   a < b < c   is not meaningful in LEXOR (would need parentheses)
    // But implementing a loop is harmless since the SemanticAnalyzer will
    // catch type errors if someone tries to chain them.
    //
    // ALGORITHM (same left-associative loop as parseLogical):
    //   1. left = parseAdditive()
    //   2. while match(OP_GT, OP_LT, OP_GTE, OP_LTE, OP_EQ, OP_NEQ):
    //        op = previous().getLexeme()
    //        right = parseAdditive()
    //        left = new BinaryExprNode(line, left, op, right)
    //   3. return left
    //
    // TODO 25a: Implement parseRelational().
    //


    // -------------------------------------------------------------------------
    // TODO STEP 26 — parseAdditive()
    // -------------------------------------------------------------------------
    // Handles:  <expr> + <expr>   |   <expr> - <expr>
    //
    // ALGORITHM (left-associative loop):
    //   1. left = parseMultiplicative()
    //   2. while match(OP_PLUS, OP_MINUS):
    //        op = previous().getLexeme()
    //        right = parseMultiplicative()
    //        left = new BinaryExprNode(line, left, op, right)
    //   3. return left
    //
    // TODO 26a: Implement parseAdditive().
    //


    // -------------------------------------------------------------------------
    // TODO STEP 27 — parseMultiplicative()
    // -------------------------------------------------------------------------
    // Handles:  <expr> * <expr>   |   / |   %
    //
    // ALGORITHM (left-associative loop):
    //   1. left = parseUnaryArith()
    //   2. while match(OP_MUL, OP_DIV, OP_MOD):
    //        op = previous().getLexeme()
    //        right = parseUnaryArith()
    //        left = new BinaryExprNode(line, left, op, right)
    //   3. return left
    //
    // TODO 27a: Implement parseMultiplicative().
    //


    // -------------------------------------------------------------------------
    // TODO STEP 28 — parseUnaryArith()
    // -------------------------------------------------------------------------
    // Handles:  -<expr>   |   +<expr>   (arithmetic unary prefix)
    //
    // WHY separate from parseUnaryLogical()?
    //   NOT applies to BOOL operands. Unary +/- applies to numeric operands.
    //   Keeping them separate allows the SemanticAnalyzer to validate each
    //   kind independently without merging semantically different operations.
    //
    // ALGORITHM:
    //   if match(OP_MINUS, OP_PLUS):
    //       op = previous().getLexeme()    ← "-" or "+"
    //       operand = parseUnaryArith()    ← allows --x (double negative)
    //       return new UnaryExprNode(line, op, operand)
    //   return parsePrimary()
    //
    // NODE BUILT (when unary op is present):
    //   new UnaryExprNode(line, "-" or "+", operand)
    //
    // TODO 28a: Implement parseUnaryArith().
    //


    // -------------------------------------------------------------------------
    // TODO STEP 29 — parsePrimary()   [BASE OF THE EXPRESSION CHAIN]
    // -------------------------------------------------------------------------
    // Handles the atoms — the simplest expressions that need no further parsing:
    //   • Integer literal      → new LiteralNode(line, Integer.parseInt(lexeme), "INT")
    //   • Float literal        → new LiteralNode(line, Float.parseFloat(lexeme), "FLOAT")
    //   • Char literal         → new LiteralNode(line, lexeme.charAt(0), "CHAR")
    //   • Bool literal         → new LiteralNode(line, Boolean.parseBoolean(lexeme), "BOOL")
    //   • String literal       → new LiteralNode(line, lexeme, "STRING")
    //   • Identifier           → new VariableNode(line, lexeme)
    //   • Parenthesised expr   → consume LPAREN, parseExpression(), consume RPAREN
    //
    // WHY parseExpression() inside parentheses, not parsePrimary()?
    //   Because (a + b) * c needs the inner expression to be a full expression,
    //   not just a primary atom. Calling parseExpression() restarts the full
    //   precedence chain inside the parentheses.
    //
    // NODE BUILT (examples):
    //   INT_LITERAL  "42"  → new LiteralNode(line, 42,         "INT")
    //   FLOAT_LITERAL      → new LiteralNode(line, 3.14f,      "FLOAT")
    //   CHAR_LITERAL "n"   → new LiteralNode(line, 'n',        "CHAR")
    //   BOOL_LITERAL "TRUE"→ new LiteralNode(line, true,       "BOOL")
    //   STRING_LITERAL     → new LiteralNode(line, "hello",    "STRING")
    //   IDENTIFIER "x"     → new VariableNode(line, "x")
    //
    // TODO 29a: Implement parsePrimary() handling all six cases listed above.
    // TODO 29b: For INT_LITERAL, use Integer.parseInt(previous().getLexeme())
    //           after consuming the token.
    // TODO 29c: For FLOAT_LITERAL, use Float.parseFloat(previous().getLexeme()).
    // TODO 29d: For CHAR_LITERAL, use previous().getLexeme().charAt(0).
    // TODO 29e: For BOOL_LITERAL, use previous().getLexeme().equals("TRUE").
    // TODO 29f: Throw error("Expected expression") if no case matches.
    //

}