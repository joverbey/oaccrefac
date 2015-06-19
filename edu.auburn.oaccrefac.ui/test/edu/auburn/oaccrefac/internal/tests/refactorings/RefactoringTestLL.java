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

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
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
public abstract class RefactoringTestLL<R extends CRefactoring> extends RefactoringTest<R> {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        Iterable<Object[]> result = generateParameters("testcode-ll/llloops.c");
        return result;
    }

    private String kernelDescription;

    protected RefactoringTestLL(Class<R> refactoringClass, File fileContainingMarker, int markerOffset,
            String markerText) throws Exception {
        super(refactoringClass, fileContainingMarker, markerOffset, markerText);
    }

    @Override
    protected void configureRefactoring(R refactoring, IFile file, TextSelection selection, LinkedList<String> markerFields) {
         this.kernelDescription = markerFields.removeFirst();
    }

    @Override
    protected Map<String, IFile> importFiles(File jioFileContainingMarker) throws Exception {
        IFile thisFile = importFile(jioFileContainingMarker);
        refreshProject();
        return Collections.<String, IFile>singletonMap(thisFile.getName(), thisFile);
    }

    @Override
    protected boolean shouldCompile(IFile fileContainingMarker) {
        return false;
    }

    @Override
    protected final File resultFileFor(String filename) {
        return resultFileFor(filename, kernelDescription);
    }

    @Override
    protected void validateRemainingMarkerFields(LinkedList<String> markerFields) {
    }

    @Override
    protected boolean shouldFail(IFile fileContainingMarker, LinkedList<String> markerFields) {
        return !resultFileFor(fileContainingMarker.getName(), kernelDescription).exists();
    }

    protected abstract File resultFileFor(String filename, String kernelDescription);
}
