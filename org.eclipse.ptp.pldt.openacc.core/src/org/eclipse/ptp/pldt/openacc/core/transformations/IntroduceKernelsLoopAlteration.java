/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

/**
 * IntroduceKernelsLoopAlteration adds an "acc kernels loop" pragma above a
 * for loop.
 * <p>
 * Before:
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = i;
 * }
 * </pre>
 * After:
 * <pre>
 * #pragma acc kernels loop
 * for (int i = 0; i < 10; i++) {
 *     a[i] = i;
 * }
 * </pre>
 * 
 * @author John William O'Rourke
 *
 */
public class IntroduceKernelsLoopAlteration extends ForLoopAlteration<IntroduceKernelsLoopCheck> {

    public IntroduceKernelsLoopAlteration(IASTRewrite rewriter, IntroduceKernelsLoopCheck check) {
        super(rewriter, check);
    }
    
    @Override
    protected void doChange() {
        int offset = getLoopToChange().getFileLocation().getNodeOffset();
        this.insert(offset, pragma("acc kernels loop"));
        finalizeChanges();
    }
    
}
