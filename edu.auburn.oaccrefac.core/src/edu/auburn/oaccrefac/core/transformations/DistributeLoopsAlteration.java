/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * Inheriting from {@link ForLoopAlteration}, DistributeLoopsAlteration defines a refactoring to distribute loops. Loop distribution takes
 * the body of a for-loop and splits the statements into separate for-loops with the same header, if possible.
 * <p>
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = i;
 *     b[i] = 10 - i;
 * }
 * </pre>
 * 
 * Refactors to
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = i;
 * }
 * for (int i = 0; i < 10; i++) {
 *     b[i] = 10 - i;
 * }
 * </pre>
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public class DistributeLoopsAlteration extends ForLoopAlteration<DistributeLoopsCheck> {

    /**
     * Constructor that takes a for-loop to distribute.
     * 
     * @param rewriter
     *            -- base rewriter for loop
     * @param loop
     *            -- loop to be distributed
     */
    public DistributeLoopsAlteration(IASTRewrite rewriter, DistributeLoopsCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() {
        String init = this.getLoopToChange().getInitializerStatement().getRawSignature();
        String cond = this.getLoopToChange().getConditionExpression().getRawSignature();
        String incr = this.getLoopToChange().getIterationExpression().getRawSignature();
        ForStatementInquisitor loop = InquisitorFactory.getInquisitor(getLoopToChange());
        // Remove the old loop from the statement list
        this.remove(getLoopToChange());

        // Precondition guarantees that body is a compound statement
        IASTCompoundStatement body = (IASTCompoundStatement) this.getLoopToChange().getBody();

        // For each child, create new for loop with same header and child as body
        IASTStatement[] stmts = body.getStatements();
        int offset = this.getLoopToChange().getFileLocation().getNodeOffset();
        
        for (int i = stmts.length - 1; i >= 0; i--) {
            if(loop.getPragmas().length != 0){
                this.insert(offset, pragma("acc parallel loop"));
            }
            String newBody = "";
            for(IASTComment comment : ASTUtil.getLeadingComments(stmts[i])) {
                newBody += comment.getRawSignature() + System.lineSeparator();
            }
            newBody += stmts[i].getRawSignature();
            newBody = compound(newBody);
            this.insert(offset, forLoop(init, cond, incr, newBody));
        }

        finalizeChanges();
    }

}
