package com.lexor.error;

public class LexorException extends RuntimeException{
    public LexorException(String message){
        super(message);
    }

    public LexorException(String message, int line, int column) {
    }
}
