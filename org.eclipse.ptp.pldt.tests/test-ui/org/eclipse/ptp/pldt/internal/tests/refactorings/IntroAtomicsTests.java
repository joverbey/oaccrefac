/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Auburn) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.internal.tests.refactorings;

import java.io.File;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.IntroduceAtomicsRefactoring;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class IntroAtomicsTests extends RefactoringTest<IntroduceAtomicsRefactoring> {
    
    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        return generateParameters("testcode/IntroAtomics");
    }

    public IntroAtomicsTests(String description, File fileContainingMarker, int markerOffset, String markerText)
            throws Exception {
        super(IntroduceAtomicsRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    @Override
    protected void configureRefactoring(IntroduceAtomicsRefactoring refactoring, IFile file,
            TextSelection selection, LinkedList<String> markerFields) {
    }
    
}
