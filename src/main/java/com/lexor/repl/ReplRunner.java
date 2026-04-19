package com.lexor.repl;

// =============================================================================
// FILE: ReplRunner.java
// PACKAGE: com.lexor.repl
// =============================================================================
//
// PURPOSE:
//   ReplRunner implements the interactive Read-Eval-Print Loop (REPL) for
//   LEXOR. It allows users to type LEXOR code directly into the terminal and
//   see results immediately — without writing a source file. This is the mode
//   activated by the `--repl` or `-r` flag in Main.java.
//
// WHAT IS A REPL?
//   A REPL cycles through four phases indefinitely:
//     READ   → Accept input from the user
//     EVAL   → Run the full Lexer → Parser → Semantic → Interpreter pipeline
//     PRINT  → Display output (handled inside the pipeline via PRINT statements)
//     LOOP   → Return to READ
//
//   The user exits by typing a special command (e.g., ":exit" or ":quit"),
//   or by pressing Ctrl+D (EOF on Unix) / Ctrl+Z (EOF on Windows).
//
// JLINE3 INTEGRATION:
//   ReplRunner uses the JLine3 library (org.jline) to provide:
//     - Line editing (left/right arrow keys to move cursor within a line)
//     - History (up/down arrow keys to recall previous inputs)
//     - Tab completion (optional, for future keyword completion)
//     - Graceful handling of Ctrl+C (interrupt) and Ctrl+D (EOF)
//
//   JLine3 wraps System.in with a Terminal object and uses LineReader to
//   collect user input. This replaces a simple Scanner(System.in).
//
// MULTI-LINE INPUT HANDLING:
//   LEXOR programs span multiple lines (START SCRIPT...END SCRIPT). The REPL
//   must detect whether the user has finished typing a complete input block
//   or is still in the middle of one. Two strategies:
//
//   STRATEGY A — Trigger on END SCRIPT (recommended):
//     Buffer lines until the user types "END SCRIPT" on its own line.
//     Then run the pipeline on the entire buffer. This gives LEXOR's natural
//     block structure in the REPL.
//
//   STRATEGY B — Single-statement mode:
//     Wrap each single line automatically with SCRIPT AREA / START SCRIPT /
//     END SCRIPT boilerplate, so users only need to type one statement at a time.
//     Simpler for beginners; less flexible.
//
//   RECOMMENDATION: Start with Strategy B during development (simpler to test),
//   then migrate to Strategy A for a production REPL experience.
//
// ENVIRONMENT PERSISTENCE:
//   In the REPL, variable declarations should persist across inputs so students
//   can declare a variable in one input and use it in the next. This means
//   the same Interpreter instance (and its Environment) must be reused across
//   the REPL loop. Only the Lexer and Parser are re-created per input.
//
// =============================================================================

// TODO: Import org.jline.reader.LineReader
// TODO: Import org.jline.reader.LineReaderBuilder
// TODO: Import org.jline.reader.UserInterruptException
// TODO: Import org.jline.reader.EndOfFileException
// TODO: Import org.jline.terminal.Terminal
// TODO: Import org.jline.terminal.TerminalBuilder
// TODO: Import com.lexor.lexer.Lexer
// TODO: Import com.lexor.lexer.Token
// TODO: Import com.lexor.parser.Parser
// TODO: Import com.lexor.parser.ast.ProgramNode
// TODO: Import com.lexor.semantic.SemanticAnalyzer
// TODO: Import com.lexor.interpreter.Interpreter
// TODO: Import com.lexor.error.LexorException (base class)
// TODO: Import java.io.IOException
// TODO: Import java.util.List
// TODO: Import org.slf4j.Logger, org.slf4j.LoggerFactory

// TODO: public class ReplRunner { ... }

// -----------------------------------------------------------------------------
// CONSTANTS:
// -----------------------------------------------------------------------------
//
// TODO: private static final String PROMPT = "lexor> ";
//       - The text shown at the start of each input line.
//       - You may use ANSI color codes for a colored prompt:
//           "\u001B[32mlexor>\u001B[0m "   → green "lexor>" then reset color
//       - Keep it simple first; add color later.
//
// TODO: private static final String PROMPT_CONTINUE = "  ...> ";
//       - Shown on subsequent lines when the user is in the middle of a
//         multi-line block (Strategy A). Signals that input is incomplete.
//
// TODO: private static final String EXIT_COMMAND = ":exit";
//       - If the user types exactly ":exit" or ":quit", the REPL shuts down.
//
// TODO: private static final String QUIT_COMMAND = ":quit";

// -----------------------------------------------------------------------------
// FIELDS TO DECLARE:
// -----------------------------------------------------------------------------
//
// TODO: private static final Logger log = LoggerFactory.getLogger(ReplRunner.class);
//
// TODO: private final Interpreter interpreter;
//       - Single, shared Interpreter instance across all REPL iterations.
//       - Preserves Environment state (declared variables persist).
//
// TODO: private final SemanticAnalyzer semanticAnalyzer;
//       - Shared SemanticAnalyzer. Its SymbolTable also persists across inputs
//         so declared variables remain known across REPL entries.
//
//       NOTE ON SHARED SEMANTIC ANALYZER:
//         If the user declares INT x in one input and then uses x in another,
//         the shared SymbolTable already knows x. This is the desired behaviour.
//         However, if the user makes a semantic error in one input, you must
//         ensure the SymbolTable state is not corrupted before the next input.
//         Strategy: catch SemanticException, log the error, and continue the
//         REPL loop WITHOUT updating the SymbolTable with the bad declaration.

// -----------------------------------------------------------------------------
// CONSTRUCTOR:
// -----------------------------------------------------------------------------
//
// TODO:
//         public ReplRunner() {
//             this.interpreter      = new Interpreter();
//             this.semanticAnalyzer = new SemanticAnalyzer();
//         }

// =============================================================================
// PUBLIC ENTRY POINT
// =============================================================================

// TODO: public void start()
//
//   Builds the JLine3 terminal and enters the REPL loop.
//
//   Implementation skeleton:
//
//     Terminal terminal = TerminalBuilder.builder()
//         .system(true)          // use the real system terminal
//         .build();
//
//     LineReader reader = LineReaderBuilder.builder()
//         .terminal(terminal)
//         .variable(LineReader.HISTORY_FILE, ".lexor_history")  // persist history
//         .build();
//
//     printWelcomeBanner(terminal);
//
//     while (true) {
//         String line;
//         try {
//             line = reader.readLine(PROMPT);
//         } catch (UserInterruptException e) {
//             // Ctrl+C pressed — clear current line and continue
//             terminal.writer().println("(interrupted — type :exit to quit)");
//             continue;
//         } catch (EndOfFileException e) {
//             // Ctrl+D pressed — exit cleanly
//             terminal.writer().println("\nBye!");
//             break;
//         }
//
//         if (line == null || line.isBlank()) continue;
//         if (line.trim().equals(EXIT_COMMAND) || line.trim().equals(QUIT_COMMAND)) {
//             terminal.writer().println("Bye!");
//             break;
//         }
//
//         // Handle special REPL commands (see handleSpecialCommand below)
//         if (line.trim().startsWith(":")) {
//             handleSpecialCommand(line.trim(), terminal);
//             continue;
//         }
//
//         // Collect full input block (Strategy B: wrap single line)
//         String source = wrapInput(line);
//
//         // Run the pipeline on `source`
//         runPipeline(source, terminal);
//     }
//
//     terminal.close();

// =============================================================================
// PIPELINE RUNNER
// =============================================================================

// TODO: private void runPipeline(String source, Terminal terminal)
//
//   Runs the Lexer → Parser → SemanticAnalyzer → Interpreter pipeline on a
//   source string. Catches and reports errors without crashing the REPL.
//
//   Implementation:
//     try {
//         // Stage 1: Lex
//         Lexer lexer = new Lexer(source);
//         List<Token> tokens = lexer.tokenize();
//         log.debug("Lexed {} tokens", tokens.size());
//
//         // Stage 2: Parse
//         Parser parser = new Parser(tokens);
//         ProgramNode ast = parser.parse();
//         log.debug("AST built successfully");
//
//         // Stage 3: Semantic Analysis
//         semanticAnalyzer.analyze(ast);
//         log.debug("Semantic check passed");
//
//         // Stage 4: Interpret
//         interpreter.interpret(ast);
//         log.debug("Execution complete");
//
//     } catch (com.lexor.error.ParseException e) {
//         terminal.writer().printf("[Parse Error] Line %d: %s%n", e.getLine(), e.getMessage());
//         terminal.writer().flush();
//     } catch (com.lexor.error.SemanticException e) {
//         terminal.writer().printf("[Semantic Error] Line %d: %s%n", e.getLine(), e.getMessage());
//         terminal.writer().flush();
//     } catch (com.lexor.error.LexorRuntimeException e) {
//         terminal.writer().printf("[Runtime Error] Line %d: %s%n", e.getLine(), e.getMessage());
//         terminal.writer().flush();
//     } catch (com.lexor.error.LexorException e) {
//         terminal.writer().printf("[Error] %s%n", e.getMessage());
//         terminal.writer().flush();
//     } catch (Exception e) {
//         // Unexpected internal interpreter error — print stack trace for debugging
//         terminal.writer().println("[Internal Error] " + e.getMessage());
//         log.error("Unexpected error during REPL execution", e);
//     }

// =============================================================================
// INPUT WRAPPING (STRATEGY B)
// =============================================================================

// TODO: private String wrapInput(String userLine)
//
//   Wraps a single user input line with the required LEXOR program boilerplate
//   so the Parser sees a complete program structure.
//
//   Implementation:
//     return "SCRIPT AREA\n"
//          + "START SCRIPT\n"
//          + userLine + "\n"
//          + "END SCRIPT\n";
//
//   STRATEGY A ALTERNATIVE:
//   If implementing multi-line input (Strategy A), this method is replaced by
//   a buffer loop that accumulates lines until "END SCRIPT" is typed. The
//   entire buffer is returned as-is (already a complete program).
//
//   Multi-line buffer skeleton:
//     StringBuilder buffer = new StringBuilder();
//     buffer.append(userLine).append("\n");
//     while (!userLine.trim().equals("END SCRIPT")) {
//         userLine = reader.readLine(PROMPT_CONTINUE);
//         buffer.append(userLine).append("\n");
//     }
//     return buffer.toString();

// =============================================================================
// SPECIAL REPL COMMANDS
// =============================================================================

// TODO: private void handleSpecialCommand(String command, Terminal terminal)
//
//   Handles REPL meta-commands that start with ":". These control the REPL
//   itself rather than executing LEXOR code.
//
//   Commands to implement:
//
//   ":help"
//     Print a help message listing available commands and LEXOR syntax hints.
//     terminal.writer().println("Commands: :exit :quit :clear :env :help");
//
//   ":env"
//     Display all currently declared variables and their values.
//     Calls interpreter.dumpEnvironment() if you add that method, or
//     stores a reference to the Environment and calls environment.dump().
//     Useful for students to see what variables are currently in scope.
//
//   ":clear"
//     Reset the interpreter and symbol table, clearing all declared variables.
//     Allows starting fresh without restarting the REPL.
//     Call: interpreter.resetEnvironment() and semanticAnalyzer.resetSymbolTable().
//     You will need to add these reset methods to their respective classes.
//
//   Unknown command:
//     terminal.writer().println("Unknown command: " + command + ". Type :help for help.");

// =============================================================================
// WELCOME BANNER
// =============================================================================

// TODO: private void printWelcomeBanner(Terminal terminal)
//
//   Prints a startup message when the REPL first launches.
//
//   Suggested content:
//     terminal.writer().println("╔══════════════════════════════╗");
//     terminal.writer().println("║   LEXOR Interpreter v1.0     ║");
//     terminal.writer().println("║   Type :help for commands    ║");
//     terminal.writer().println("║   Type :exit to quit         ║");
//     terminal.writer().println("╚══════════════════════════════╝");
//     terminal.writer().flush();
//
//   ANSI color version (optional — adds color if terminal supports it):
//     Use terminal.getType() to check if the terminal supports ANSI codes
//     before adding them, to avoid garbled output on Windows CMD.
//
//   The terminal.writer() is a PrintWriter connected to the JLine3 terminal,
//   so use terminal.writer().println() instead of System.out.println() in
//   all REPL output to avoid interleaving with JLine's input handling.

// =============================================================================
// REPL HISTORY NOTE:
// =============================================================================
//
//   JLine3 automatically saves command history to the file specified in:
//     .variable(LineReader.HISTORY_FILE, ".lexor_history")
//   This file is created in the current working directory. Students can press
//   UP/DOWN arrow keys to recall previous commands across REPL sessions.
//   To disable persistence, remove the .variable() call — history will still
//   work within one session (in memory only).
//
// =============================================================================
// TESTING THE REPL:
// =============================================================================
//
//   The REPL is harder to unit test than other stages because it depends on
//   interactive I/O. Recommended approach:
//
//   1. Extract runPipeline() into a separate, testable method (already done above).
//      Test it directly with a source string — no terminal needed.
//
//   2. Use Mockito to mock the Terminal and LineReader in integration tests.
//
//   3. Test wrapInput() independently to ensure the boilerplate is correct.
//
//   4. Manually test with: java -jar lexor.jar --repl
//
// =============================================================================
