/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     William Hester (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class IntroAtomicCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroAtomicCheck(RefactoringStatus status, IASTStatement[] statements, IASTNode[] statementsAndComments) {
        super(status, statements, statementsAndComments);
    }

    @Override
    public RefactoringStatus doCheck(IProgressMonitor pm) {
        return status;
    }
}
