package com.lexor;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

// Import your pipeline classes based on the ReplRunner structure
import com.lexor.lexer.Lexer;
import com.lexor.lexer.Token;
import com.lexor.parser.Parser;
import com.lexor.parser.ast.ProgramNode;
import com.lexor.semantic.SemanticAnalyzer;
import com.lexor.interpreter.Interpreter;
import com.lexor.repl.ReplRunner;

@Command(name = "lexor", mixinStandardHelpOptions = true, version = "1.0",
        description = "Runs a LEXOR source file or starts an interactive REPL.")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "LEXOR source file (.lxr)", arity = "0..1")
    private File sourceFile;

    @Option(names = {"--repl", "-r"}, description = "Start interactive REPL mode")
    private boolean replMode;

    @Override
    public Integer call() throws Exception {

        // 1. Initialize the environment/memory
        // We do this here so both the REPL and the File runner can use them!
        Interpreter interpreter = new Interpreter();
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();

        if (replMode) {
            System.out.println("Starting LEXOR REPL...");
            new ReplRunner(interpreter, semanticAnalyzer).start();
            return 0;

        } else if (sourceFile != null) {
            // Call our new helper method to run the file
            runFile(sourceFile, interpreter, semanticAnalyzer);
            return 0;

        } else {
            System.err.println("Provide a .lxr file or use --repl");
            return 1;
        }
    }

    /**
     * Reads a .lxr file and pushes it through the interpreter pipeline.
     */
    private void runFile(File file, Interpreter interpreter, SemanticAnalyzer semanticAnalyzer) {
        try {
            // STEP 0: Read the entire file into a single Java String
            String sourceCode = Files.readString(file.toPath());

            // STEP 1: Lexer (Convert raw string into Tokens)
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            // STEP 2: Parser (Convert Tokens into an Abstract Syntax Tree)
            Parser parser = new Parser(tokens);
            ProgramNode ast = parser.parse();

            // STEP 3: Semantic Analysis (Check for logical errors like type mismatches)
            semanticAnalyzer.analyze(ast);

            // STEP 4: Interpreter (Execute the code to the terminal)
            interpreter.interpret(ast);

        } catch (Exception e) {
            // If the user's LEXOR code has a syntax error, or the file doesn't exist, it prints here
            System.err.println("[Error] Could not execute " + file.getName() + ":\n" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}