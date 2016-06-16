package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;

public abstract class SourceFileAlteration<T extends SourceFileCheck<?>> extends SourceAlteration<T> {
    
    private Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> pragmas;
    
    public SourceFileAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.pragmas = check.getPragmas();
        
    }
    
    public Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> getPragmas() {
        return pragmas;
    }

}
