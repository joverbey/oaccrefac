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
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop fusion refactoring algorithm. Loop fusion takes the
 * bodies of two identical for-loop headers and places them in one.
 * 
 * <p>
 * As of now, these loop headers MUST be completely identical and MUST be right next to each other. The reason they must
 * be next to each other is because there could be statements between the two loops that could change the meaning of the
 * program if two loops were merged into one.
 * </p>
 * 
 * <p>
 * For example,
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
 * Refactors to:
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = i;
 *     b[i] = 10 - i;
 * }
 * </pre>
 * </p>
 *
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 *
 */
public class FuseLoopsAlteration extends ForLoopAlteration<FuseLoopsCheck> {

    private IASTForStatement first;
    private IASTForStatement second;

    /**
     * Constructor that takes a for-loop to perform fusion on
     * @param rewriter
     *            -- base rewriter for loop
     */
    public FuseLoopsAlteration(IASTRewrite rewriter, FuseLoopsCheck check) {
        super(rewriter, check);
        first = check.getLoop1();
        second = check.getLoop2();
    }

    @Override
    protected void doChange() {
        for (IASTPreprocessorPragmaStatement prag : ASTUtil.getPragmaNodes(second)) {
            remove(prag);
        }

        //FIXME this is a workaround. see issue #48 
        insertBefore(first, "");
        
        replace(getLastBodyIndex(first), getFirstBodyIndex(second) - getLastBodyIndex(first), NL);
        
        if(!(first.getBody() instanceof IASTCompoundStatement)) {
        	insert(getFirstBodyIndex(first), LCURLY);
        }
        if(!(second.getBody() instanceof IASTCompoundStatement)) {
        	insert(getLastBodyIndex(second), RCURLY);
        }
        
        Map<IASTName, String> newNames = getNameModifications(first, second);
        for(IASTName name : newNames.keySet()) {
        	replace(name, newNames.get(name));
        }
        
        finalizeChanges();

    }

    private Map<IASTName, String> getNameModifications(IASTForStatement loop1, IASTForStatement loop2) {
        Map<IASTName, String> map = new HashMap<IASTName, String>();
        ForStatementInquisitor inq1 = ForStatementInquisitor.getInquisitor(loop1);
        ForStatementInquisitor inq2 = ForStatementInquisitor.getInquisitor(loop2);
        for (IASTDeclarator d1 : ASTUtil.find(loop1, IASTDeclarator.class)) {
            for (IASTDeclarator d2 : ASTUtil.find(loop2, IASTDeclarator.class)) {
            	//if we neither d1 nor d2 is the index variable and d1 and d2 have identical names
            	if(!d1.getName().resolveBinding().equals(inq1.getIndexVariable()) && !d2.getName().resolveBinding().equals(inq2.getIndexVariable())) {
		        	if (d1.getName().getRawSignature().equals(d2.getName().getRawSignature())) {
		        		IScope scope1 = loop1.getBody() instanceof IASTCompoundStatement? ((IASTCompoundStatement) loop1.getBody()).getScope() : loop1.getScope();
		        		IScope scope2 = loop2.getBody() instanceof IASTCompoundStatement? ((IASTCompoundStatement) loop2.getBody()).getScope() : loop2.getScope();
		                String newName = newName(d2.getName().getRawSignature(), scope1, scope2);
		                for (IASTName use : getUses(d2, loop2)) {
		                    map.put(use, newName);
		                }
		            }
            	}
            }
        }
        return map;
    }
    
    private String newName(String name, IScope scope1, IScope scope2) {
        for (int i = 0; true; i++) {
            String newName = name + "_" + i;
            if (!ASTUtil.isNameInScope(newName, scope1) && !ASTUtil.isNameInScope(newName, scope2)) {
                return newName;
            }
        }
    }

    private List<IASTName> getUses(IASTDeclarator decl, IASTForStatement loop) {
    	List<IASTName> uses = new ArrayList<IASTName>();
    	for(IASTName name : ASTUtil.find(loop, IASTName.class)) {
    		if(decl.getName().resolveBinding().equals(name.resolveBinding())) {
    			uses.add(name);
    		}
    	}
        return uses;
    }

    //FIXME?: could use getSyntax() with ITokens instead?
    private int getFirstBodyIndex(IASTForStatement loop) {
    	if(loop.getBody() instanceof IASTCompoundStatement) {
    		String rawSignature = loop.getBody().getRawSignature();
			int afterFirstCurly = rawSignature.indexOf(LCURLY) + 1;
    		int beforeLastCurly = rawSignature.lastIndexOf(RCURLY);
    		String body = rawSignature.substring(afterFirstCurly, beforeLastCurly).trim();
    		return rawSignature.indexOf(body) + loop.getBody().getFileLocation().getNodeOffset();
    	}
    	else {
    		//FIXME?: if there is an rparen in a comment right after the iter expr, this may break
    		String rawSignature = loop.getRawSignature();
			int iterEnd = loop.getIterationExpression().getFileLocation().getNodeOffset()
					+ loop.getIterationExpression().getFileLocation().getNodeLength()
					- loop.getFileLocation().getNodeOffset();
			String sub = rawSignature.substring(iterEnd);
			int headerEnd = sub.indexOf(RPAREN) + 1;
			sub = sub.substring(headerEnd).trim();
			return rawSignature.indexOf(sub) + loop.getFileLocation().getNodeOffset();
    	}
    }
    
    private int getLastBodyIndex(IASTForStatement loop) {
    	String rawSignature = loop.getBody().getRawSignature();
    	if(loop.getBody() instanceof IASTCompoundStatement) {
			int afterFirstCurly = rawSignature.indexOf(LCURLY) + 1;
    		int beforeLastCurly = rawSignature.lastIndexOf(RCURLY);
    		String body = rawSignature.substring(afterFirstCurly, beforeLastCurly).trim();
    		return rawSignature.indexOf(body) + loop.getBody().getFileLocation().getNodeOffset() + body.length();
    	}
    	else {
    		return rawSignature.indexOf(rawSignature.trim()) + loop.getBody().getFileLocation().getNodeOffset() + rawSignature.trim().length();
    	}
    }
}
