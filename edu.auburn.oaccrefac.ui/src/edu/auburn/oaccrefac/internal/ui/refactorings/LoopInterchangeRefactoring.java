package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
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
 */
public class LoopInterchangeRefactoring extends ForLoopRefactoring {
    
    private int m_depth;
    
    public LoopInterchangeRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_depth = 1;
    }
    
    public void setExchangeDepth(int depth) {
        if (depth > 0) {
            m_depth = depth;
        }
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        //Get the top level loop and the loop to refactor.
        IASTForStatement loop = getLoop();
        //Finds our loop at hardcoded loop depth (DOUBLY_NESTED_DEPTH)
        IASTForStatement xchng = findLoop(loop, m_depth);
        
        rewriter.replace(loop.getInitializerStatement(), xchng.getInitializerStatement(), null);
        rewriter.replace(loop.getConditionExpression(), xchng.getConditionExpression(), null);
        rewriter.replace(loop.getIterationExpression(), xchng.getIterationExpression(), null);
        
        rewriter.replace(xchng.getInitializerStatement(), loop.getInitializerStatement(), null);
        rewriter.replace(xchng.getConditionExpression(), loop.getConditionExpression(), null);
        rewriter.replace(xchng.getIterationExpression(), loop.getIterationExpression(), null);
    }
    
    /**
     * Method takes in a tree and finds the nth for loop statement
     * within the tree. Since we are expecting a perfectly nested loop
     * from our preconditions, this is essentially the depth in which
     * the for loop lies.
     * @param tree -- tree in which to search for loop
     * @param depth -- depth in which to find the loop
     * @return the node for the for-loop at depth
     */
    private IASTForStatement findLoop(IASTNode tree, int depth) {
        
        class LoopFinder extends ASTVisitor {
            private IASTForStatement forloop = null;
            private int current_depth;
            private int find_depth;
            
            public LoopFinder(int depth) {
                find_depth = depth;
                current_depth = 0;
                shouldVisitStatements = true;
            }
            
            @Override
            public int visit(IASTStatement visitor) {
                if (visitor instanceof IASTForStatement) {
                    current_depth++;
                    if (current_depth == find_depth) {
                        forloop = (IASTForStatement) visitor;
                        return ASTVisitor.PROCESS_ABORT;
                    } else {
                        return ASTVisitor.PROCESS_CONTINUE;
                    }
                } else {
                    return ASTVisitor.PROCESS_CONTINUE;
                }
            }
        }
        
        LoopFinder finder = new LoopFinder(depth);
        tree.accept(finder);
        return finder.forloop;
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }

}
