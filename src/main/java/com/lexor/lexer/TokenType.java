package com.lexor.lexer;

public enum TokenType {
    // Keywords
    KEYWORD_SCRIPT,
    KEYWORD_START,
    KEYWORD_END,
    KEYWORD_DECLARE,
    KEYWORD_PRINT,
    KEYWORD_SCAN,
    KEYWORD_IF,
    KEYWORD_ELSE,
    KEYWORD_FOR,
    KEYWORD_REPEAT,
    KEYWORD_WHEN,
    KEYWORD_AREA,

    // Types
    TYPE_INT,
    TYPE_CHAR,
    TYPE_BOOL,
    TYPE_FLOAT,

    // Literals
    INT_LITERAL,
    FLOAT_LITERAL,
    CHAR_LITERAL,
    BOOL_LITERAL,
    STRING_LITERAL,

    // Identifier
    IDENTIFIER,

    // Operators
    OP_PLUS,
    OP_MINUS,
    OP_MUL,
    OP_DIV,
    OP_MOD,
    OP_GT,
    OP_LT,
    OP_GTE,
    OP_LTE,
    OP_EQ,
    OP_NEQ,
    OP_AND,
    OP_OR,
    OP_NOT,

    // Symbols & Punctuation
    AMPERSAND,
    DOLLAR,
    LBRACKET,
    RBRACKET,
    LPAREN,
    RPAREN,
    COMMA,
    COLON,
    ASSIGN,

    // Control
    NEWLINE,
    EOF,


};
