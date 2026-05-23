package com.lexor.error;

public class LexorException extends RuntimeException {

    private final int line;
    private final int column;

    public LexorException(String message) {
        super(message);
        this.line   = 0;
        this.column = 0;
    }

    public LexorException(String message, int line, int column) {
        super(message);
        this.line   = line;
        this.column = column;
    }

    public int getLine()   { return line;   }
    public int getColumn() { return column; }
}
