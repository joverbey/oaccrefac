/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Jacob Neeley (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IScope;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * The LoopCuttingAlteration refactoring cuts a for loop into multiple
 * loops.
 * <p>
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 15; i++)
 *     // ...
 * </pre>
 * 
 * Refactors to: The outer loop is the by-strip and the inner loop is the
 * in-strip loop...
 * 
 * <pre>
 * for (int set = 0; set < 3, set++)
 *     for (int i = set; i < 15, i+=3)
 *          // ...
 * </pre>
 *          
 * @author Jeff Overbey
 * @author Jacob Neeley
 */
public class LoopCuttingAlteration extends ForLoopAlteration<LoopCuttingCheck> {

    private int cutFactor;
    
    public LoopCuttingAlteration(IASTRewrite rewriter, int cutFactorIn, LoopCuttingCheck check) {
        super(rewriter, check);
        cutFactor = cutFactorIn;
    }

    @Override
    protected void doChange() throws Exception {
        IASTForStatement loop = getLoopToChange();
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(loop);
        String indexVar = inq.getIndexVariable().toString();
        String newName;
        try {
            newName = createNewName(indexVar, loop.getScope().getParent());
        } catch (DOMException e) {
            e.printStackTrace();
            return;
        }
        
        String innerInit, innerCond, innerIter, innerBody, inner;
        String outerInit, outerCond, outerIter, outer;
        String ub = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2().getRawSignature();
        if (loop.getInitializerStatement() instanceof IASTDeclarationStatement) {
            innerInit = String.format("int %s = %s", indexVar, newName);
        } else {
            innerInit = String.format("%s = %s", indexVar, newName);
        }
        innerCond = parenth(String.format("%s < %s", indexVar, ub));
        innerIter = indexVar + "+=" + ub + "/" + cutFactor;
        innerBody = "";
        IASTNode[] innerBodyObjects = getBodyObjects(loop);
        for(IASTNode bodyObject : innerBodyObjects) {
            innerBody += bodyObject.getRawSignature() + System.lineSeparator();
        }
        inner = forLoop(innerInit, innerCond, innerIter, compound(innerBody));
        
        String initRhs;
        if(loop.getInitializerStatement() instanceof IASTExpressionStatement) {
            IASTExpressionStatement es = (IASTExpressionStatement) loop.getInitializerStatement();
            IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
            initRhs = e.getOperand2().getRawSignature();
        }
        else {
            IASTDeclarationStatement ds = (IASTDeclarationStatement) loop.getInitializerStatement();
            IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
            IASTEqualsInitializer init = (IASTEqualsInitializer) dec.getDeclarators()[0].getInitializer();
            initRhs = init.getInitializerClause().getRawSignature();
        }
        outerInit = String.format("int %s = %s", newName, initRhs);
        outerCond = String.format("%s < %s", newName, ub + " / " + cutFactor);
        outerIter = newName + "++";
        outer = forLoop(outerInit, outerCond, outerIter, compound(inner));
        this.replace(loop, outer);
        finalizeChanges();
    }
    
    /**
     * @return name, if it is not already used in the given scope, and otherwise some variation on name (name_0, name_1,
     *         name_2, etc.) that is not in scope
     */
    private String createNewName(String name, IScope scope) {
        for (int i = 0; true; i++) {
            String newName = name + "_" + i;
            if (!ASTUtil.isNameInScope(newName, scope)) {
                return newName;
            }
        }
    }
    
    private IASTStatement[] getBodyStatements(IASTForStatement loop) {
        if (loop.getBody() instanceof IASTCompoundStatement) {
            return ((IASTCompoundStatement) loop.getBody()).getStatements();
        } else {
            return new IASTStatement[] { loop.getBody() };
        }
    }

    private IASTComment[] getBodyComments(IASTForStatement loop) {
        List<IASTComment> comments = new ArrayList<IASTComment>();
        for (IASTComment comment : loop.getTranslationUnit().getComments()) {
            // if the comment's offset is in between the end of the loop header and the end of the loop body
            if (comment.getFileLocation()
                    .getNodeOffset() > loop.getIterationExpression().getFileLocation().getNodeOffset()
                            + loop.getIterationExpression().getFileLocation().getNodeLength() + ")".length()
                    && comment.getFileLocation().getNodeOffset() < loop.getBody().getFileLocation().getNodeOffset()
                            + loop.getBody().getFileLocation().getNodeLength()) {
                for(IASTStatement stmt : getBodyStatements(loop)) {
                    if(!ASTUtil.doesNodeLexicallyContain(stmt, comment)) {
                        comments.add(comment);      
                    }
                }
            }
        }
        Collections.sort(comments, ASTUtil.FORWARD_COMPARATOR);

        return comments.toArray(new IASTComment[comments.size()]);
    }

    // gets statements AND comments from a loop body in forward order
    private IASTNode[] getBodyObjects(IASTForStatement loop) {
        List<IASTNode> objects = new ArrayList<IASTNode>();
        objects.addAll(Arrays.asList(getBodyStatements(loop)));
        objects.addAll(Arrays.asList(getBodyComments(loop)));
        Collections.sort(objects, ASTUtil.FORWARD_COMPARATOR);
        return objects.toArray(new IASTNode[objects.size()]);

    }
    
}
