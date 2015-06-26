package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class FuseLoops extends CompoundModify {

    private IASTForStatement m_first;
    private IASTForStatement m_second;
    
    public FuseLoops(IASTCompoundStatement compound, IASTForStatement first) {
        super(compound);
        m_first = first;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        if (m_first == null) {
            init.addFatalError("Loop cannot be null!");
            return init;
        }
        
        // This gets the selected loop to re-factor.
        boolean found = false;
        IASTNode newnode = m_first;
        while (ASTUtil.getNextSibling(newnode) != null && !found) {
            newnode = ASTUtil.getNextSibling(newnode);
            m_second = findLoop(newnode);
            found = (m_second != null);
        }

        if (!found) {
            init.addFatalError("There is no for loop for fusion to be possible.");
        }
        return init;
    }
    
    @Override
    protected void modifyCompound() {
        IASTStatement body = m_first.getBody();
        IASTCompoundStatement body_compound = convertIntoCompound(body);
        IASTStatement loopbody = m_second.getBody();
        IASTCompoundStatement newbody = convertIntoCompound(loopbody);
        addbody((IASTCompoundStatement) body_compound, newbody);
        IASTForStatement newFor = m_first.copy();
        newFor.setBody(body_compound);
        replace(m_first, newFor);
        remove(m_second);
    }
    
    private IASTCompoundStatement convertIntoCompound(IASTStatement bdy)
    {
        IASTCompoundStatement bdy_compound = null;
        // if the body is not within braces, i.e. short-cut
        // loop pattern, make it a compounded statement
        if (!(bdy instanceof IASTCompoundStatement)) {
            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
            bdy_compound = factory.newCompoundStatement();
            bdy_compound.addStatement(bdy.copy());
        } else {
            bdy_compound = (IASTCompoundStatement) (bdy.copy());
            }
        return bdy_compound;
    }

    private void addbody(IASTCompoundStatement body_compound, IASTCompoundStatement loopbody) {
        for (IASTNode childz : loopbody.getChildren()) {
            body_compound.addStatement((IASTStatement) childz.copy());
        }
    }
    
    private IASTForStatement findLoop(IASTNode tree) {
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

}
