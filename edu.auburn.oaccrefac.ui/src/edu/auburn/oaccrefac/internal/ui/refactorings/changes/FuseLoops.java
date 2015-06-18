package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;

public class FuseLoops extends CompoundModify {

    private IASTForStatement m_first;
    private IASTForStatement m_second;
    
    public FuseLoops(IASTCompoundStatement compound, 
            IASTForStatement first, IASTForStatement second) {
        super(compound);
        if (first != null && second != null) {
            m_first = first;
            m_second = second;
        } else {
            throw new IllegalArgumentException("Cannot fuse with null loop");
        }
    }
    
    @Override
    protected IASTCompoundStatement doChange() {
        IASTStatement body = m_first.getBody();
        IASTCompoundStatement body_compound = convertIntoCompound(body);
        IASTStatement loopbody = m_second.getBody();
        IASTCompoundStatement newbody = convertIntoCompound(loopbody);
        addbody((IASTCompoundStatement) body_compound, newbody);
        IASTForStatement newFor = m_first.copy();
        newFor.setBody(body_compound);
        replace(m_first, newFor);
        remove(m_second);
        return super.doChange();
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

}
