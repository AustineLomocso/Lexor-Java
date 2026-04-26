package com.lexor.repl;

import com.lexor.interpreter.Interpreter;
import com.lexor.lexer.Lexer;
import com.lexor.lexer.Token;
import com.lexor.parser.Parser;
import com.lexor.parser.ast.ProgramNode;
import com.lexor.semantic.SemanticAnalyzer;
import com.lexor.error.LexorException;
// Note: Assuming these specific exception classes exist in your error package based on the comments
// import com.lexor.error.ParseException;
// import com.lexor.error.SemanticException;
// import com.lexor.error.LexorRuntimeException;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * ReplRunner implements the interactive Read-Eval-Print Loop (REPL) for LEXOR.
 */
public class ReplRunner {

    // -----------------------------------------------------------------------------
    // CONSTANTS
    // -----------------------------------------------------------------------------
    private static final String PROMPT = "lexor> ";
    private static final String PROMPT_CONTINUE = " ...> ";
    private static final String EXIT_COMMAND = ":exit";
    private static final String QUIT_COMMAND = ":quit";

    // -----------------------------------------------------------------------------
    // FIELDS
    // -----------------------------------------------------------------------------
    private static final Logger log = LoggerFactory.getLogger(ReplRunner.class);

    // Shared instances to maintain state (declared variables) across inputs
    private final Interpreter interpreter;
    private final SemanticAnalyzer semanticAnalyzer;

    // -----------------------------------------------------------------------------
    // CONSTRUCTOR
    // -----------------------------------------------------------------------------
    public ReplRunner(Interpreter interpreter, SemanticAnalyzer semanticAnalyzer) {
        this.interpreter = interpreter;
        this.semanticAnalyzer = semanticAnalyzer;
    }

    // =============================================================================
    // PUBLIC ENTRY POINT
    // =============================================================================
    public void start() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .system(true) // use the real system terminal
                .build();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.HISTORY_FILE, ".lexor_history") // persist history
                .build();

        printWelcomeBanner(terminal);

        while (true) {
            String line;
            try {
                line = reader.readLine(PROMPT);
            } catch (UserInterruptException e) {
                // Ctrl+C pressed — clear current line and continue
                terminal.writer().println("(interrupted — type :exit to quit)");
                continue;
            } catch (EndOfFileException e) {
                // Ctrl+D pressed — exit cleanly
                terminal.writer().println("\nBye!");
                break;
            }

            if (line == null || line.isBlank()) continue;

            if (line.trim().equals(EXIT_COMMAND) || line.trim().equals(QUIT_COMMAND)) {
                terminal.writer().println("Bye!");
                break;
            }

            // Handle special REPL commands
            if (line.trim().startsWith(":")) {
                handleSpecialCommand(line.trim(), terminal);
                continue;
            }

            // Collect full input block
            String source = wrapInput(line, reader);

            // Run the pipeline on the constructed source
            runPipeline(source, terminal);
        }

        terminal.close();
    }

    // =============================================================================
    // PIPELINE RUNNER
    // =============================================================================
    private void runPipeline(String source, Terminal terminal) {
        try {
            // Stage 1: Lex
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            log.debug("Lexed {} tokens", tokens.size());

            // Stage 2: Parse
            Parser parser = new Parser(tokens);
            ProgramNode ast = parser.parse();
            log.debug("AST built successfully");

            // Stage 3: Semantic Analysis
            semanticAnalyzer.analyze(ast);
            log.debug("Semantic check passed");

            // Stage 4: Interpret
            interpreter.interpret(ast);
            log.debug("Execution complete");

            // Use fully qualified names or imports based on your exact package structure
        } catch (Exception e) {
            // Check for specific custom exceptions.
            // Replace with distinct catch blocks if ParseException, SemanticException, etc. are imported.
            if (e.getClass().getSimpleName().equals("ParseException")) {
                terminal.writer().printf("[Parse Error] %s%n", e.getMessage());
            } else if (e.getClass().getSimpleName().equals("SemanticException")) {
                terminal.writer().printf("[Semantic Error] %s%n", e.getMessage());
            } else if (e.getClass().getSimpleName().equals("LexorRuntimeException")) {
                terminal.writer().printf("[Runtime Error] %s%n", e.getMessage());
            } else if (e instanceof LexorException) {
                terminal.writer().printf("[Error] %s%n", e.getMessage());
            } else {
                terminal.writer().println("[Internal Error] " + e.getMessage());
                log.error("Unexpected error during REPL execution", e);
            }
            terminal.writer().flush();
        }
    }

    // =============================================================================
    // INPUT WRAPPING
    // =============================================================================
    private String wrapInput(String userLine, LineReader reader) {
        // --- STRATEGY B (Single-line wrapping - Uncomment to use) ---
        /*
        return "SCRIPT AREA\n"
             + "START SCRIPT\n"
             + userLine + "\n"
             + "END SCRIPT\n";
        */

        // --- STRATEGY A (Multi-line buffer until "END SCRIPT") ---
        StringBuilder buffer = new StringBuilder();
        buffer.append(userLine).append("\n");

        while (!userLine.trim().equals("END SCRIPT")) {
            try {
                userLine = reader.readLine(PROMPT_CONTINUE);
                buffer.append(userLine).append("\n");
            } catch (UserInterruptException | EndOfFileException e) {
                // Break out gracefully if user cancels mid-block
                break;
            }
        }
        return buffer.toString();
    }

    // =============================================================================
    // SPECIAL REPL COMMANDS
    // =============================================================================
    private void handleSpecialCommand(String command, Terminal terminal) {
        switch (command) {
            case ":help":
                terminal.writer().println("Commands: :exit  :quit  :clear  :env  :help");
                break;

            case ":env":
                // TODO: Implement interpreter.dumpEnvironment() to show variables
                terminal.writer().println("[Environment Dump] Not yet implemented.");
                // interpreter.dumpEnvironment(terminal.writer());
                break;

            case ":clear":
                // TODO: Implement reset methods in your Interpreter and SemanticAnalyzer classes
                terminal.writer().println("[Clear] Resetting environment and symbol table...");
                // interpreter.resetEnvironment();
                // semanticAnalyzer.resetSymbolTable();
                break;

            default:
                terminal.writer().println("Unknown command: " + command + ". Type :help for commands.");
                break;
        }
        terminal.writer().flush();
    }

    // =============================================================================
    // WELCOME BANNER
    // =============================================================================
    private void printWelcomeBanner(Terminal terminal) {
        terminal.writer().println("╔══════════════════════════════╗");
        terminal.writer().println("║   LEXOR Interpreter v1.0     ║");
        terminal.writer().println("║   Type :help for commands    ║");
        terminal.writer().println("║   Type :exit to quit         ║");
        terminal.writer().println("╚══════════════════════════════╝");
        terminal.writer().flush();
    }
}