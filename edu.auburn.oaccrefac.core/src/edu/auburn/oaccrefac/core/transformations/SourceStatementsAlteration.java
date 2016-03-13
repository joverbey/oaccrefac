package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public abstract class SourceStatementsAlteration<T extends SourceStatementsCheck<?>> extends SourceAlteration<T> {

    private IASTStatement[] statements;
    private IASTNode[] allEnclosedNodes;
    private int offset;
    private int length;
    
    public SourceStatementsAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.statements = check.getStatements();
        this.allEnclosedNodes = check.getAllEnclosedNodes();
        offset = statements[0].getFileLocation().getNodeOffset();
        length = statements[statements.length - 1].getFileLocation().getNodeOffset()
                + statements[statements.length - 1].getFileLocation().getNodeLength()
                - offset;
        
    }
    
    public IASTStatement[] getStatements() {
        return statements;
    }
    
    public IASTNode[] getAllEnclosedNodes() {
        return allEnclosedNodes;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

}
