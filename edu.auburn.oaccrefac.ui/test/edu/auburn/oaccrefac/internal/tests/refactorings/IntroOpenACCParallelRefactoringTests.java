/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Auburn) - Initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.tests.refactorings;

import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;

import edu.auburn.oaccrefac.internal.ui.refactorings.IntroOpenACCParallelRefactoring;
import edu.auburn.oaccrefac.internal.util.RefactoringTestSuite;
import junit.framework.Test;

public class IntroOpenACCParallelRefactoringTests extends RefactoringTestSuite<IntroOpenACCParallelRefactoring> {
    public static Test suite() throws Exception {
        return new IntroOpenACCParallelRefactoringTests();
    }

    public IntroOpenACCParallelRefactoringTests() throws Exception {
        super(IntroOpenACCParallelRefactoring.class, "testcode/IntroOpenACCParallel");
    }

    @Override
    protected void configureRefactoring(IntroOpenACCParallelRefactoring refactoring, IFile file,
            TextSelection selection, LinkedList<String> markerFields) {
        // String vectorLength = markerFields.removeFirst();
        // refactoring.setMaxVectorLength(vectorLength);
    }
}
