/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    John William O'Rourke (Auburn) - Initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.tests.refactorings;

import java.io.File;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.auburn.oaccrefac.internal.ui.refactorings.IntroduceKernelsLoopRefactoring;

@RunWith(Parameterized.class)
public class IntroduceKernelsLoopTest extends RefactoringTest<IntroduceKernelsLoopRefactoring> {

    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        return generateParameters("testcode/IntroduceKernelsLoop");
    }

    public IntroduceKernelsLoopTest(String description, File fileContainingMarker, int markerOffset, String markerText)
            throws Exception {
        super(IntroduceKernelsLoopRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    @Override
    protected void configureRefactoring(IntroduceKernelsLoopRefactoring refactoring, IFile file,
            TextSelection selection, LinkedList<String> markerFields) {
        // String vectorLength = markerFields.removeFirst();
        // refactoring.setMaxVectorLength(vectorLength);
    }
    
}
