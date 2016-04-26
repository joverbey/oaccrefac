package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;

public abstract class SourceFileAlteration<T extends SourceFileCheck<?>> extends SourceAlteration<T> {
    
    private List<IASTPreprocessorPragmaStatement> statements;
    private int offset;
    private int length;
    
    public SourceFileAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.statements = check.getStatements();
        
    }
    
    public List<IASTPreprocessorPragmaStatement> getStatements() {
        return statements;
    }

}
