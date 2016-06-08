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
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

/**
 * This class defines the base strategy interface to be derived from for changes made to a for loop.
 * 
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopAlteration<T extends ForLoopCheck<?>> extends SourceAlteration<T> {

    private final IASTForStatement loop;
    
    /**
     * Constructor that takes a for-loop and a rewriter (for base)
     * 
     * @param rewriter
     *            -- rewriter to be given to base class
     * @param loopToChange
     *            -- loop to change
     * @throws IllegalArgumentException
     *             if the for loop is null
     */
    public ForLoopAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.loop = check.getLoop();
    }

    /**
     * Gets the loop set from constructor
     * 
     * @return loop to change
     */
    public IASTForStatement getLoop() {
        return loop;
    }
    
    /**
     * @return name that doesn't exist in the given scope
     */
    protected String createNewName(String name, IScope scope) {
        for (int i = 0; true; i++) {
            String newName = name + "_" + i;
            if (!ASTUtil.isNameInScope(newName, scope)) {
                return newName;
            }
        }
    }
    
 // gets statements AND comments from a loop body in forward order
    protected IASTNode[] getBodyObjects(IASTForStatement loop) {
        List<IASTNode> objects = new ArrayList<IASTNode>();
        objects.addAll(Arrays.asList(ASTUtil.getStatementsIfCompound(loop.getBody())));
        objects.addAll(Arrays.asList(getBodyComments(loop)));
        Collections.sort(objects, ASTUtil.FORWARD_COMPARATOR);
        return objects.toArray(new IASTNode[objects.size()]);

    }
    
    protected IASTComment[] getBodyComments(IASTForStatement loop) {
        List<IASTComment> comments = new ArrayList<IASTComment>();
        for (IASTComment comment : loop.getTranslationUnit().getComments()) {
            // if the comment's offset is in between the end of the loop header and the end of the loop body
            if (comment.getFileLocation()
                    .getNodeOffset() > loop.getIterationExpression().getFileLocation().getNodeOffset()
                            + loop.getIterationExpression().getFileLocation().getNodeLength() + ")".length()
                    && comment.getFileLocation().getNodeOffset() < loop.getBody().getFileLocation().getNodeOffset()
                            + loop.getBody().getFileLocation().getNodeLength()) {
            	boolean inner = false;
                for(IASTStatement stmt : ASTUtil.getStatementsIfCompound(loop.getBody())) {
                    if(ASTUtil.doesNodeLexicallyContain(stmt, comment)) {
                        inner = true;
                        break;
                    }
                }
                if (!inner) {
                	comments.add(comment);
                }
            }
        }
        Collections.sort(comments, ASTUtil.FORWARD_COMPARATOR);

        return comments.toArray(new IASTComment[comments.size()]);
    }

}
