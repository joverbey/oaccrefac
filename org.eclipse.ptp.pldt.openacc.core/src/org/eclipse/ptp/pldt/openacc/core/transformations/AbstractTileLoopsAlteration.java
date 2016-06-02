/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public abstract class AbstractTileLoopsAlteration 
	extends ForLoopAlteration<AbstractTileLoopsCheck> {
	
	private int numFactor;
	private String newName;
	
	public AbstractTileLoopsAlteration(IASTRewrite rewriter, int numValue, 
			String newName, AbstractTileLoopsCheck check) {
        super(rewriter, check);
        this.numFactor = numValue;
        this.newName = newName;
    }
	
    @Override
    protected void doChange() {
        IASTForStatement loop = getLoopToChange();
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(loop);
        String indexVar = inq.getIndexVariable().toString();
        if (newName.equals("")) {
        	try {
        		newName = createNewName(indexVar, loop.getScope().getParent());
        	} catch (DOMException e) {
        		e.printStackTrace();
        		return;
        	}
        }

        String innerInit, innerCond, innerIter, innerBody, inner;
        String outerInit, outerCond, outerIter, outer;
        IASTBinaryExpression condExpr = (IASTBinaryExpression) loop.getConditionExpression();
		String compOp = getOperatorAsString(condExpr);
		String ub = condExpr.getOperand2().getRawSignature();
        if (loop.getInitializerStatement() instanceof IASTDeclarationStatement) {
            innerInit = String.format("int %s = %s", indexVar, newName);
        } else {
            innerInit = String.format("%s = %s", indexVar, newName);
        }
        innerCond = getInnerCond(indexVar, newName, numFactor, compOp, ub);
        innerIter = getInnerIter(loop, indexVar, ub, numFactor);
        innerBody = "";
        IASTNode[] innerBodyObjects = getBodyObjects(loop);
        for(IASTNode bodyObject : innerBodyObjects) {
            innerBody += bodyObject.getRawSignature() + System.lineSeparator();
        }
        inner = forLoop(innerInit, innerCond, innerIter, compound(innerBody));

        String initRhs;
        String initType;
        //TODO we're making a lot of typecast assumptions - be sure they won't break anything
        if(loop.getInitializerStatement() instanceof IASTExpressionStatement) {
            IASTExpressionStatement es = (IASTExpressionStatement) loop.getInitializerStatement();
            IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
            initRhs = e.getOperand2().getRawSignature();
            IType type = e.getOperand1().getExpressionType();
            if (type instanceof ITypedef) {
            	initType = ((ITypedef) type).getType().toString();
            }
            else {
            	initType = type.toString();
            }
        }
        else {
            IASTDeclarationStatement ds = (IASTDeclarationStatement) loop.getInitializerStatement();
            IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
            IASTEqualsInitializer init = (IASTEqualsInitializer) dec.getDeclarators()[0].getInitializer();
            initRhs = init.getInitializerClause().getRawSignature();
            initType = dec.getDeclSpecifier().getRawSignature();
        }
        outerInit = String.format("%s %s = %s", initType, newName, initRhs);
        outerCond = getOuterCond(newName, compOp, ub, numFactor);
        outerIter = getOuterIter(newName, numFactor);
        outer = forLoop(outerInit, outerCond, outerIter, compound(inner));
        this.replace(loop, outer);
        finalizeChanges();
    }
	
	private String getOperatorAsString(IASTBinaryExpression condExpr) {
		String compOp;
		switch (condExpr.getOperator()) {
		case IASTBinaryExpression.op_lessEqual:
			compOp = "<=";
			break;
		case IASTBinaryExpression.op_lessThan:
			compOp = "<";
			break;
		default:
			throw new IllegalStateException();
		}
		return compOp;
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
    
    private IASTComment[] getBodyComments(IASTForStatement loop) {
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
    
    // gets statements AND comments from a loop body in forward order
    private IASTNode[] getBodyObjects(IASTForStatement loop) {
        List<IASTNode> objects = new ArrayList<IASTNode>();
        objects.addAll(Arrays.asList(ASTUtil.getStatementsIfCompound(loop.getBody())));
        objects.addAll(Arrays.asList(getBodyComments(loop)));
        Collections.sort(objects, ASTUtil.FORWARD_COMPARATOR);
        return objects.toArray(new IASTNode[objects.size()]);

    }
    
    protected abstract String getOuterCond(String newName, String compOp, String ub, int numValue);
    
    protected abstract String getOuterIter(String newName, int numFactor);
    
    protected abstract String getInnerCond(String indexVar, String newName, int numFactor, 
    		String compOp, String ub);
    
    protected abstract String getInnerIter(IASTForStatement loop, String indexVar, String ub, int NumValue);
}