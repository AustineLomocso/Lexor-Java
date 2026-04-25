package com.lexor.interpreter;

// =============================================================================
// FILE: LexorValue.java
// PACKAGE: com.lexor.interpreter
// =============================================================================
//
// PURPOSE:
//   LexorValue is the universal runtime value container used by the Interpreter.
//   Every value that exists at runtime — every variable's current value, every
//   expression result, every literal evaluation — is wrapped in a LexorValue.
//
//   It pairs a raw Java Object (the actual data) with a type string ("INT",
//   "FLOAT", "CHAR", "BOOL") so the Interpreter always knows how to handle it.
//
// WHY NOT USE Java primitives directly?
//   Java primitives (int, float, char, boolean) cannot be stored uniformly in
//   collections or returned from a single method with a single return type.
//   LexorValue acts as a typed wrapper, similar to how Java's own Integer,
//   Float, etc. wrap primitives — but with an explicit LEXOR type tag attached.
//
// JAVA TYPES USED INTERNALLY (the `value` field):
//   LEXOR "INT"   → Java Integer
//   LEXOR "FLOAT" → Java Float
//   LEXOR "CHAR"  → Java Character
//   LEXOR "BOOL"  → Java Boolean
//
// =============================================================================

// TODO: Import com.lexor.error.LexorRuntimeException
import com.lexor.error.LexorException;

import java.util.Objects;

// TODO: public class LexorValue { ... }
public class LexorValue {


// -----------------------------------------------------------------------------
// FIELDS TO DECLARE:
// -----------------------------------------------------------------------------
//
// TODO: private final String type;
//       - One of: "INT", "FLOAT", "CHAR", "BOOL"
//       - Final: a LexorValue is immutable — operations create new instances.
//
// TODO: private final Object value;
//       - The actual runtime data stored as a Java Object.
//       - The accessor methods below cast this safely.
    private final String type;
    private final Object value;
// -----------------------------------------------------------------------------
// CONSTRUCTOR:
// -----------------------------------------------------------------------------
//
// TODO:
//         public LexorValue(String type, Object value) {
//             this.type  = type;
//             this.value = value;
//         }
    public LexorValue(String type, Object value) {
        this.type = type;
        this.value = value;
    }
//
//   Usage examples by the Interpreter:
//     new LexorValue("INT",   42)
//     new LexorValue("FLOAT", 3.14f)
//     new LexorValue("CHAR",  'A')
//     new LexorValue("BOOL",  true)

// -----------------------------------------------------------------------------
// GETTER FOR TYPE:
// -----------------------------------------------------------------------------
//
// TODO: public String getType() { return type; }
//
//   The Interpreter uses getType() when deciding which arithmetic operation to
//   apply (e.g., INT division vs. FLOAT division) and when constructing result
//   LexorValues with the correct promoted type.
    public String getType() {
        return type;
    }
// =============================================================================
// TYPED ACCESSOR METHODS
// =============================================================================
//
// Each accessor casts the internal Object to the expected Java type.
// If the actual stored type doesn't match the requested type, throw
// LexorRuntimeException with a clear message — this should not happen if the
// SemanticAnalyzer passed, but guards against interpreter bugs.

// TODO: public int asInt()
//
//   Returns the value as a Java int.
//
//   Implementation:
//     if (value instanceof Integer i) return i;
//     if (value instanceof Float   f) return f.intValue();   // truncate for FLOAT→INT edge cases
//     throw new LexorRuntimeException(
//         "Cannot read '" + type + "' value as INT", 0, 0);
//
//   Called by: Interpreter when performing arithmetic on INT operands,
//              in relational comparisons, and as a loop counter.
    public int asInt(){
        if(value instanceof Integer i) return i;
        if(value instanceof Float f) return f.intValue();
        throw new LexorException("Type " + type + " is not an integer");

    }
// TODO: public float asFloat()
//
//   Returns the value as a Java float.
//
//   Implementation:
//     if (value instanceof Float   f) return f;
//     if (value instanceof Integer i) return i.floatValue();  // widen INT to FLOAT
//     throw new LexorRuntimeException(
//         "Cannot read '" + type + "' value as FLOAT", 0, 0);
//
//   Called by: Interpreter when either operand in arithmetic is FLOAT.
    public float asFloat(){
        if(value instanceof Float f) return f;
        if(value instanceof Integer i) return i.floatValue();
        throw new LexorException("Type " + type + " is not a float");
    }
// TODO: public boolean asBool()
//
//   Returns the value as a Java boolean.
//
//   Implementation:
//     if (value instanceof Boolean b) return b;
//     throw new LexorRuntimeException(
//         "Cannot read '" + type + "' value as BOOL", 0, 0);
//
//   Called by: Interpreter in IF, FOR, REPEAT WHEN conditions,
//              and for AND/OR/NOT logical operations.
    public boolean asBool(){
        if(value instanceof Boolean b) return b;
        throw new LexorException("Type " + type + " is not a boolean");
    }
// TODO: public char asChar()
//
//   Returns the value as a Java char.
//
//   Implementation:
//     if (value instanceof Character c) return c;
//     throw new LexorRuntimeException(
//         "Cannot read '" + type + "' value as CHAR", 0, 0);
//
//   Called by: Interpreter when printing a CHAR variable.
    public char asChar(){
        if(value instanceof Character c) return c;
        throw new LexorException("Type " + type + " is not a character");
    }
// =============================================================================
// toString() OVERRIDE
// =============================================================================

// TODO: @Override public String toString()
//
//   Converts the value to its human-readable string for PRINT output.
//
//   Implementation:
//     switch (type) {
//         case "INT"   -> return String.valueOf((int) asInt());
//         case "FLOAT" -> return String.valueOf(asFloat());
//         case "CHAR"  -> return String.valueOf(asChar());
//         case "BOOL"  -> return asBool() ? "TRUE" : "FALSE";
//         default      -> return String.valueOf(value);
//     }
//
//   BOOL NOTE: LEXOR displays booleans as "TRUE" and "FALSE" (uppercase),
//   matching the literal syntax used in the source language. Do NOT use
//   Java's default "true"/"false".
//
//   FLOAT NOTE: Consider using String.format("%.2f", asFloat()) if LEXOR
//   specifies a fixed decimal display for floats. Otherwise, valueOf is fine.
    @Override
    public String toString(){
        switch(type){
            case "int": return String.valueOf((int)(asInt()));
            case "float": return String.valueOf((float)(asFloat()));
            case "bool": return asBool() ? "TRUE" : "FALSE";
            case "char": return String.valueOf(asChar());
            default: return String.valueOf(value);
        }
    }
// =============================================================================
// equals() AND hashCode() (OPTIONAL BUT RECOMMENDED)
// =============================================================================

// TODO: @Override public boolean equals(Object obj)
//
//   Used by the Interpreter for == and <> comparisons in BinaryExprNode.
//
//   Implementation:
//     if (this == obj) return true;
//     if (!(obj instanceof LexorValue other)) return false;
//     return this.type.equals(other.type) && this.value.equals(other.value);
//
//   This allows the == operator in LEXOR to compare by value, not by Java
//   object identity. Without this override, == on two LexorValue("INT", 5)
//   instances would return false (different Java objects).
    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof LexorValue other)) return false;
        return this.type.equals(other.type) && this.value.equals(other.value);
    }
// TODO: @Override public int hashCode()
//   Required whenever equals() is overridden (Java contract).
//   Use: return Objects.hash(type, value);
//   Import java.util.Objects for this.

// =============================================================================
// STATIC FACTORY METHODS (OPTIONAL — for convenience)
// =============================================================================
    @Override
    public int hashCode(){
        return Objects.hash(type, value);
    }
// TODO (optional): Add static factory methods to avoid repeating "new LexorValue(...)":
//
   public static LexorValue ofInt(int v)     { return new LexorValue("INT",   v); }
   public static LexorValue ofFloat(float v) { return new LexorValue("FLOAT", v); }
   public static LexorValue ofBool(boolean v){ return new LexorValue("BOOL",  v); }
   public static LexorValue ofChar(char v)   { return new LexorValue("CHAR",  v); }
//
//   Usage in Interpreter: return LexorValue.ofInt(left.asInt() + right.asInt());
//   This is cleaner than: return new LexorValue("INT", left.asInt() + right.asInt());

// =============================================================================
}