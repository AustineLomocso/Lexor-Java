package com.lexor.lexer;

public class Token {
    private TokenType type;
    private String lexeme;
    private int line;
    private int column;
    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    public boolean isType(TokenType type) {
        return this.type == type;
    }

    public TokenType getType() {
        return type;
    }
    public String getLexeme() {
        return lexeme;
    }
    public int getLine() {
        return line;
    }
    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return String.format("Token[%s, \"%s\", line=%d, col=%d]", type, lexeme, line, column);
    }


}
