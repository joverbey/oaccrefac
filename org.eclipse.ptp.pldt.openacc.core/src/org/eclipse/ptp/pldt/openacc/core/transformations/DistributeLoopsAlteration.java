/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

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
        String init = this.getLoop().getInitializerStatement().getRawSignature();
        String cond = this.getLoop().getConditionExpression().getRawSignature();
        String incr = this.getLoop().getIterationExpression().getRawSignature();
        // Remove the old loop from the statement list
        this.remove(getLoop());

        // Precondition guarantees that body is a compound statement
        IASTCompoundStatement body = (IASTCompoundStatement) this.getLoop().getBody();

        // For each child, create new for loop with same header and child as body
        IASTStatement[] stmts = body.getStatements();
        int offset = this.getLoop().getFileLocation().getNodeOffset();
        
        for (int i = stmts.length - 1; i >= 0; i--) {
            if(ASTUtil.getPragmaNodes(getLoop()).size() != 0){
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
