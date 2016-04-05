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

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class PragmaDirectiveCheck<T extends RefactoringParams> extends Check<T> {

    private IASTPreprocessorPragmaStatement pragma;
    private IASTStatement statement;

    public PragmaDirectiveCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        this.pragma = pragma;
        this.statement = statement;
    }
    
    protected void doFormCheck(RefactoringStatus status) { }
    
    public RefactoringStatus formCheck(RefactoringStatus status, IProgressMonitor pm) {
        doFormCheck(status);
        return status;
    }
    
    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, T params) {
        super.performChecks(status, pm, params);
        if(status.hasFatalError()) {
            return status;
        }
        formCheck(status, pm);
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
