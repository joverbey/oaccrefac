package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

/**
 *  This class implements refactoring for loop interchange. Loop
 *  interchange is the exchange of the ordering of two iteration
 *  variables used in nested loops.
 * 
 *  Currently supports only perfectly, doubly nested loops.
 *
 */
public class LoopInterchangeRefactoring extends ForLoopRefactoring {

    public LoopInterchangeRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        IASTForStatement loop = getLoop();
        IASTForStatement xchng = findNextLoop(loop);
        
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTForStatement inner = factory.newForStatement(
                loop.getInitializerStatement(), 
                loop.getConditionExpression(), 
                loop.getIterationExpression(), 
                xchng.getBody());
        IASTForStatement outer = factory.newForStatement(
                xchng.getInitializerStatement(), 
                xchng.getConditionExpression(), 
                xchng.getIterationExpression(), 
                inner);
        
        rewriter.replace(loop, outer, null);
        
    }
    
    private IASTForStatement findNextLoop(IASTNode tree) {
        
        class LoopFinder extends ASTVisitor {
            private IASTForStatement forloop = null;
            
            public LoopFinder() {
                shouldVisitStatements = true;
            }
            
            @Override
            public int visit(IASTStatement visitor) {
                if (visitor instanceof IASTForStatement) {
                    forloop = (IASTForStatement) visitor;
                    return ASTVisitor.PROCESS_ABORT;
                } else {
                    return ASTVisitor.PROCESS_CONTINUE;
                }
            }
        }
        
        LoopFinder finder = new LoopFinder();
        tree.accept(finder);
        return finder.forloop;
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }

}
