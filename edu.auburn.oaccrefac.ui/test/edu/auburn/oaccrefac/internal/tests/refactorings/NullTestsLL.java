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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import edu.auburn.oaccrefac.internal.ui.refactorings.NullRefactoring;

/**
 * Runs the Null refactoring on all Livermore loops, failing if the loop cannot be analyzed.
 * 
 * @author Jeff Overbey
 */
@RunWith(Parameterized.class)
public class NullTestsLL extends RefactoringTestLL<NullRefactoring> {

    private Set<String> expectedFailures = new HashSet<String>(Arrays.asList(new String[] {
            // Do-while loop
            "Kernel2outer", //
            // Not a 0-based counted loop
            "Kernel2inner", //
            "Kernel4outer", //
            "", // Kernel4inner should fail but doesn't (???)
            // Cast expression
            "Kernel14-1", //
            // Continue statement
            "Kernel15outer", //
            "Kernel15inner", //
            "Kernel16", //
            // Do-loop with if- and goto-statements
            "Kernel17", //
            // If statement
            "Kernel16", //
            "Kernel20", //
            "Kernel24", //
    }));

    public NullTestsLL(String description, File fileContainingMarker, int markerOffset, String markerText)
            throws Exception {
        super(NullRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    @Override
    protected boolean shouldFail(IFile fileContainingMarker, LinkedList<String> markerFields) {
        return expectedFailures.contains(kernelDescription);
    }

    @Override
    protected File resultFileFor(String filename, String kernelDescription) {
        return null;
    }

    @Override
    protected void compareAgainstResultFile(String originalSource, TextSelection selection) {
    }
}
