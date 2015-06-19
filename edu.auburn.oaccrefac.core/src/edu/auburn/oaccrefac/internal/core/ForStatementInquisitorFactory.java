package edu.auburn.oaccrefac.internal.core;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

/**
 * @author Alexander Calvert
 * 
 * right now this is pointless since you can just call Inquisitor's
 * constructor directly
 *
 */
public class ForStatementInquisitorFactory {

    private ForStatementInquisitorFactory me = null;
    
    private ForStatementInquisitorFactory() {
    }
    
    public ForStatementInquisitorFactory getInstance() {
        if(me == null) {
            me = new ForStatementInquisitorFactory();
        }
        return me;
    }
    
    public ForStatementInquisitor makeEnhancedASTForStatement(IASTForStatement statement) {
        return new ForStatementInquisitor(statement);
    }
}
