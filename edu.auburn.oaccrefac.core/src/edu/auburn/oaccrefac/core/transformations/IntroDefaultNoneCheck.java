package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.parser.ASTAccDefaultnoneClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsLoopClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsLoopNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelLoopClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelLoopNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelNode;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;
import edu.auburn.oaccrefac.core.parser.IAccKernelsClause;
import edu.auburn.oaccrefac.core.parser.IAccKernelsLoopClause;
import edu.auburn.oaccrefac.core.parser.IAccParallelClause;
import edu.auburn.oaccrefac.core.parser.IAccParallelLoopClause;
import edu.auburn.oaccrefac.core.parser.OpenACCParser;

public class IntroDefaultNoneCheck extends PragmaDirectiveCheck<RefactoringParams> {

    public IntroDefaultNoneCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        super(pragma, statement);
    }
    
    @Override
    protected void doFormCheck(RefactoringStatus status) {
        
        IAccConstruct pragmaAST;
        try {
            pragmaAST = new OpenACCParser().parse(getPragma().getRawSignature());
        } catch (Exception e) {
            status.addFatalError("Could not parse selected pragma directive.");
            return;
        }
        
        if(doesACCConstructHaveDefaultNoneClause(pragmaAST)) {
            status.addFatalError("Pragma directive already has default(none) clause.");
            return;
        }
    }
    
    private boolean doesACCConstructHaveDefaultNoneClause(IAccConstruct pragmaAST) {
        if (pragmaAST instanceof ASTAccParallelLoopNode) {
            return doesParallelLoopNodeHaveDefaultNoneClause((ASTAccParallelLoopNode) pragmaAST);
        }
        else if(pragmaAST instanceof ASTAccParallelNode) {
            return doesParallelNodeHaveDefaultNoneClause((ASTAccParallelNode) pragmaAST); 
        }
        else if(pragmaAST instanceof ASTAccKernelsLoopNode) {
            return doesKernelsLoopNodeHaveDefaultNoneClause((ASTAccKernelsLoopNode) pragmaAST); 
        }
        else if(pragmaAST instanceof ASTAccKernelsNode) {
            return doesKernelsNodeHaveDefaultNoneClause((ASTAccKernelsNode) pragmaAST); 
        }
        else {
            throw new IllegalStateException();
        }
    }
    
    private boolean doesKernelsNodeHaveDefaultNoneClause(ASTAccKernelsNode pragmaAST) {
        if(pragmaAST.getAccKernelsClauseList() == null) {
            return false;
        }
        for (ASTAccKernelsClauseListNode listItem : pragmaAST.getAccKernelsClauseList()) {
            IAccKernelsClause clause = listItem.getAccKernelsClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            }
        }
        return false;
    }

    private boolean doesKernelsLoopNodeHaveDefaultNoneClause(ASTAccKernelsLoopNode pragmaAST) {
        if(pragmaAST.getAccKernelsLoopClauseList() == null) {
            return false;
        }
        for (ASTAccKernelsLoopClauseListNode listItem : pragmaAST.getAccKernelsLoopClauseList()) {
            IAccKernelsLoopClause clause = listItem.getAccKernelsLoopClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            } 
        }
        return false;
    }

    private boolean doesParallelNodeHaveDefaultNoneClause(ASTAccParallelNode pragmaAST) {
        if(pragmaAST.getAccParallelClauseList() == null) {
            return false;
        }
        for (ASTAccParallelClauseListNode listItem : pragmaAST.getAccParallelClauseList()) {
            IAccParallelClause clause = listItem.getAccParallelClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            } 
        }
        return false;
    }

    private boolean doesParallelLoopNodeHaveDefaultNoneClause(ASTAccParallelLoopNode pragmaAST) {
        if(pragmaAST.getAccParallelLoopClauseList() == null) {
            return false;
        }
        for (ASTAccParallelLoopClauseListNode listItem : pragmaAST.getAccParallelLoopClauseList()) {
            IAccParallelLoopClause clause = listItem.getAccParallelLoopClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            } 
        }
        return false;
    }
    
}
