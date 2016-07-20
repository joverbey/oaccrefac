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

public class IntroAtomicAlteration extends SourceStatementsAlteration<IntroAtomicCheck> {

    private int type;

    public IntroAtomicAlteration(IASTRewrite rewriter, IntroAtomicCheck check) {
        super(rewriter, check);
        type = check.getType();
    }

    @Override
    protected void doChange() {
        int offset = getStatements()[0].getFileLocation().getNodeOffset();
        String pragma = "#pragma acc atomic "; //$NON-NLS-1$
        switch (type) {
        case IntroAtomicCheck.READ:
            pragma += "read"; //$NON-NLS-1$
            break;
        case IntroAtomicCheck.WRITE:
            pragma += "write"; //$NON-NLS-1$
            break;
        case IntroAtomicCheck.UPDATE:
            pragma += "update"; //$NON-NLS-1$
            break;
        case IntroAtomicCheck.NONE:
            return;
        }
        insert(offset, pragma + System.lineSeparator());
        finalizeChanges();
    }
}
