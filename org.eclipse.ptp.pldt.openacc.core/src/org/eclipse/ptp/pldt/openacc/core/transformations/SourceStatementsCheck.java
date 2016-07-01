/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class SourceStatementsCheck<T extends RefactoringParams> extends Check<T> {

    private final IASTStatement[] statements;
    private final IASTNode[] allEnclosedNodes;

    protected SourceStatementsCheck(RefactoringStatus status, IASTStatement[] statements, IASTNode[] allEnclosedNodes) {
    	super(status);
        this.statements = statements;
        this.allEnclosedNodes = allEnclosedNodes;
    }

    public abstract RefactoringStatus doCheck(IProgressMonitor pm);

    @Override
    public RefactoringStatus performChecks(IProgressMonitor pm, T params) {
        super.performChecks(pm, params);
        if (status.hasFatalError()) {
            return status;
        }
        doCheck(pm);
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

}
