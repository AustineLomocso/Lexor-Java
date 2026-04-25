package com.lexor.error;

/**
 * LexorRuntimeException — thrown when a runtime error occurs during interpretation.
 *
 * Examples of runtime errors:
 *   - Division by zero
 *   - Modulo by zero
 *   - Undefined variable accessed (should not happen after SemanticAnalyzer, but guarded)
 *   - Type coercion failure (e.g. SCAN input "abc" for an INT variable)
 *   - Possible infinite loop (iteration cap exceeded)
 *   - Wrong type passed to a typed accessor (e.g. calling asInt() on a BOOL LexorValue)
 *
 * Extends LexorException so Main.java can catch all interpreter errors
 * with a single catch (LexorException e) block.
 */
public class LexorRuntimeException extends LexorException {

    /**
     * @param message  Human-readable description of what went wrong.
     * @param line     Source line where the error occurred (0 if not applicable).
     * @param column   Source column where the error occurred (0 if not applicable).
     */
    public LexorRuntimeException(String message, int line, int column) {
        super(message, line, column);
    }
}