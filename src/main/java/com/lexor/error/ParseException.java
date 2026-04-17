package com.lexor.error;

public class ParseException extends LexorException {
    public ParseException(String message, int line, int column) {
        super(message);
    }
}