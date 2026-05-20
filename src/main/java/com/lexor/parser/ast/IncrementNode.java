package com.lexor.parser.ast;

import com.lexor.visitor.ASTVisitor;

public class IncrementNode extends ASTNode {

    private final String varName;
    private final String op;
    private final boolean prefix;

    public IncrementNode(int line, String varName, String op, boolean prefix) {
        super(line);
        this.varName = varName;
        this.op = op;
        this.prefix = prefix;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIncrement(this);
    }

    public String getVarName() { return varName; }
    public String getOp()      { return op; }
    public boolean isPrefix()  { return prefix; }
}
