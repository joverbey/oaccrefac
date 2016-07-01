/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *     Carl Worley - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.core;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IASTNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;

public class OpenACCUtil {

    public static <T extends IAccConstruct> boolean isAccConstruct(org.eclipse.cdt.core.dom.ast.IASTNode statement, Class<T> accClazz) {
    	for(IASTPreprocessorPragmaStatement pragma : ASTUtil.getPragmaNodes(statement)) {
    		IAccConstruct construct = null;
    		try {
    			construct = new OpenACCParser().parse(pragma.getRawSignature());
    		}
    		catch(Exception e) {
    			continue;
    		}
    		if((construct != null) && (accClazz.isInstance(construct))) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isAccConstruct(IASTStatement statement) {
    	for(IASTPreprocessorPragmaStatement pragma : ASTUtil.getPragmaNodes(statement)) {
    		try {
    			new OpenACCParser().parse(pragma.getRawSignature());
    		}
    		catch(Exception e) {
    			continue;
    		}
    		return true;
    	}
    	return false;
    }
  
    public static boolean isAccAccelConstruct(org.eclipse.cdt.core.dom.ast.IASTNode statement) {
    	return isAccConstruct(statement, ASTAccParallelNode.class) || 
    			isAccConstruct(statement, ASTAccParallelLoopNode.class) || 
    			isAccConstruct(statement, ASTAccKernelsNode.class) ||
    			isAccConstruct(statement, ASTAccKernelsLoopNode.class);
    }
    
    public static boolean isAccAccelConstruct(IAccConstruct pragma) {
    	return pragma instanceof IAccConstruct ||
    			pragma instanceof ASTAccParallelLoopNode ||
    			pragma instanceof ASTAccKernelsNode ||
    			pragma instanceof ASTAccKernelsLoopNode;
    }

    private OpenACCUtil() {
    }
    
    public static <T> List<T> find(IASTNode parent, Class<T> clazz) {
		List<T> results = new LinkedList<T>();
		findAndAdd(parent, clazz, results);
		return results;
	}
    
    private static <T> void findAndAdd(IASTNode parent, Class<T> clazz, List<T> results) {
		if (clazz.isInstance(parent)) {
			results.add(clazz.cast(parent));
		}

		for (IASTNode child : parent.getChildren()) {
			findAndAdd(child, clazz, results);
		}
	}
    
}
