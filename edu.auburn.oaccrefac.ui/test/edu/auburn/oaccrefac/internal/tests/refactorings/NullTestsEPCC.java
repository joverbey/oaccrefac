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
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.NullRefactoring;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Runs the Null refactoring on all EPCC Level 1 loops, failing if the loop cannot be analyzed.
 * 
 * @author Jeff Overbey
 */
@RunWith(Parameterized.class)
public class NullTestsEPCC extends RefactoringTestEPCC<NullRefactoring> {

    private Set<String> expectedFailures = new HashSet<String>(Arrays.asList(new String[] {
            // Loops containing if-statements
            "loop13outer", //
            "loop13inner", //
            "loop19outer", //
            "loop25outer", //
            "loop26outer", //
            "loop32outer", //
            "loop33outer", //
            "loop39outer", //
            "loop39inner", //
            "loop47outer", //
            "loop47inner", //
            "loop57outer", //
            "loop57inner", //
            "loop63outer", //
            "loop63inner", //
            "loop65outer", //
            "loop69outer", //
            "loop69inner", //
            // Not a 0-based counted loop
            "loop43inner", //
            "loop46inner", //
            "loop52inner", //
            "loop56inner", //
            "loop71outer", //
            "loop71inner", //
            "loop72outer", //
            "loop72inner", //
            "loop73outer", //
            "loop73inner", //
            "loop75outer", //
            "loop75inner", //
            "loop75inner2", //
            "loop76outer", //
            "loop76inner", //
            "loop76inner2", //
            "loop77outer", //
            "loop77inner", //
            "loop77inner2", //
            // Conditional (ternary) expression
            "loop54outer", //
            "loop50outer", //
            // Contain a non-0-based loop
            "loop43outer", //
            "loop46outer", //
            "loop52outer", //
            "loop56outer", //
    }));

    public NullTestsEPCC(String description, File fileContainingMarker, int markerOffset, String markerText)
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
