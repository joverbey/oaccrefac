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
package edu.auburn.oaccrefac.internal.tests.refactorings;

import java.io.File;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Base class for testing C refactorings on the Livermore Loops.
 * 
 * @author Jeff Overbey
 *
 * @param <R> {@link CRefactoring} subclass under test
 */
@RunWith(Parameterized.class)
@SuppressWarnings("restriction")
public abstract class RefactoringTestLL<R extends CRefactoring> extends RefactoringTestNamedLoops<R> {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        Iterable<Object[]> result = generateParameters("testcode-ll/llloops.c");
        return result;
    }

    protected RefactoringTestLL(Class<R> refactoringClass, File fileContainingMarker, int markerOffset,
            String markerText) throws Exception {
        super(refactoringClass, fileContainingMarker, markerOffset, markerText);
    }
}
