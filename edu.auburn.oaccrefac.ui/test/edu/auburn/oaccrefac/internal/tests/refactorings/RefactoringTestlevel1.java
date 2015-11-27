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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextSelection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.auburn.oaccrefac.internal.util.IOUtil;

/**
 * Base class for testing C refactorings on the epcc level1 Loops.
 * 
 * @author Jeff Overbey
 *
 * @param <R> {@link CRefactoring} subclass under test
 */
@RunWith(Parameterized.class)
@SuppressWarnings("restriction")
public abstract class RefactoringTestlevel1<R extends CRefactoring> extends RefactoringTest<R> {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        Iterable<Object[]> result = generateParameters("testcode-epcc/level1.c");
        return result;
    }

    private String kernelDescription;

    protected RefactoringTestlevel1(Class<R> refactoringClass, File fileContainingMarker, int markerOffset,
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
    
    protected abstract File resultFileFor(String filename, String kernelDescription);

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
    
    @Override
    protected void compareAgainstResultFile(String originalSource, TextSelection selection) 
            throws IOException, URISyntaxException, CoreException {
        for (String filename : files.keySet()) {
            String toInject = IOUtil.read(resultFileFor(filename)); //$NON-NLS-1$ //$NON-NLS-2$
            
            String expected = injectResult(originalSource, selection, toInject);
            String actual = readWorkspaceFile(filename); //$NON-NLS-1$ //$NON-NLS-2$
            expected = removeCRs(expected);
            actual = removeCRs(actual);
            if (!expected.equals(actual)) {
                System.err.println("Actual result does not match contents of .result file -- " + filename);
            }
            assertEquals(expected, actual);
        }
    }
    
    private String injectResult(String original, TextSelection selection, String inject) {
        String before = original.substring(0, selection.getOffset());
        String after = original.substring(
                selection.getOffset() + selection.getLength(), 
                original.length());
        return (before + inject + after);
    }
    
    private String removeCRs(String toRemove) {
        return toRemove.replace("\r", "");
    }
    
}
