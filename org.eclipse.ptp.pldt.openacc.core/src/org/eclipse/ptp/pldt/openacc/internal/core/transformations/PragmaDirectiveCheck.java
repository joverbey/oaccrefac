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
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class PragmaDirectiveCheck<T extends RefactoringParams> extends Check<T> {

    private IASTPreprocessorPragmaStatement pragma;
    private IASTStatement statement;

    public PragmaDirectiveCheck(RefactoringStatus status, IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
    	super(status);
        this.pragma = pragma;
        this.statement = statement;
    }
    
    protected void doFormCheck() { }
    
    public RefactoringStatus formCheck(IProgressMonitor pm) {
        doFormCheck();
        return status;
    }
    
    @Override
    public RefactoringStatus performChecks(IProgressMonitor pm, T params) {
        super.performChecks(pm, params);
        if(status.hasFatalError()) {
            return status;
        }
        formCheck(pm);
        return status;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return pragma.getTranslationUnit();
    }
    
    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

    public IASTStatement getStatement() {
        return statement;
    }
    
}
