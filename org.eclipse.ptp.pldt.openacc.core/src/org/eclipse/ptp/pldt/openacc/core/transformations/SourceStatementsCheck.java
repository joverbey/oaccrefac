/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;

public abstract class SourceStatementsCheck<T extends RefactoringParams> extends Check<T> {

    private final IASTStatement[] statements;
    private final IASTNode[] allEnclosedNodes;
    
    //map of each statement with OpenAcc directives to those directives, both as CDT AST nodes and parsed constructs
    Map<IASTStatement, Map<IASTPreprocessorPragmaStatement, IAccConstruct>> accRegions;
    
    protected SourceStatementsCheck(IASTStatement[] statements, IASTNode[] allEnclosedNodes) {
        this.statements = statements;
        this.allEnclosedNodes = allEnclosedNodes;
        this.accRegions = new HashMap<IASTStatement, Map<IASTPreprocessorPragmaStatement, IAccConstruct>>();
    }
    
    public abstract RefactoringStatus check(RefactoringStatus status, IProgressMonitor pm);
    
    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, T params) {
        super.performChecks(status, pm, params);
        if (status.hasFatalError()) {
            return status;
        }
        check(status, pm);
        return status;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
    	if (statements.length == 0) {
    		return null;
    	}
        return statements[0].getTranslationUnit();
    }
    
    public IASTStatement[] getStatements() {
        return statements;
    }

    public IASTNode[] getAllEnclosedNodes() {
        return allEnclosedNodes;
    }
    
    public Map<IASTStatement, Map<IASTPreprocessorPragmaStatement, IAccConstruct>> getAccRegions() {
        return accRegions;
    }
    
}
