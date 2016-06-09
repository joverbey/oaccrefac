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

public class IntroDefaultNoneAlteration extends PragmaDirectiveAlteration<IntroDefaultNoneCheck> {

    public IntroDefaultNoneAlteration(IASTRewrite rewriter, IntroDefaultNoneCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() {
        this.replace(getPragma(), getPragma().getRawSignature().trim() + " " + "default(none)");
        finalizeChanges();
    }
    
}
