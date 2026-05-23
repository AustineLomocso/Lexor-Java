package com.lexor.web;

public class RunResult {
    public final String output;
    public final String errorType;
    public final String error;
    public final int    line;
    public final int    col;

    public RunResult(String output, String errorType, String error, int line, int col) {
        this.output    = output;
        this.errorType = errorType;
        this.error     = error;
        this.line      = line;
        this.col       = col;
    }

    public static RunResult success(String output) {
        return new RunResult(output, null, null, 0, 0);
    }

    public static RunResult failure(String errorType, String error, int line, int col) {
        return new RunResult("", errorType, error, line, col);
    }
}
