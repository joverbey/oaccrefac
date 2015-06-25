package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class EnsureCompoundBody extends ForLoopChange {

    public EnsureCompoundBody(IASTForStatement loop) {
        super(loop);
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        return init;
    }
    
    @Override
    protected IASTForStatement doChange(IASTForStatement loop) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        if (!(loop.getBody() instanceof IASTCompoundStatement)) {
            IASTCompoundStatement compoundBody = factory.newCompoundStatement();
            compoundBody.addStatement(loop.getBody());
            loop.setBody(compoundBody);
        }
        return loop;
    }



}
