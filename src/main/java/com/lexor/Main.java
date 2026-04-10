package com.lexor;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "lexor", mixinStandardHelpOptions = true, version = "1.0",
        description = "Runs a LEXOR source file or starts an interactive REPL.")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "LEXOR source file (.lxr)", arity = "0..1")
    private File sourceFile;

    @Option(names = {"--repl", "-r"}, description = "Start interactive REPL mode")
    private boolean replMode;

    @Override
    public Integer call() throws Exception {
        if (replMode) {
            // new ReplRunner().start();
        } else if (sourceFile != null) {
            System.out.println("SUCCESS! LEXOR Interpreter started.");
            System.out.println("Target file: " + sourceFile.getPath());
        } else {
            System.err.println("Provide a .lxr file or use --repl");
            return 1;
        }
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}