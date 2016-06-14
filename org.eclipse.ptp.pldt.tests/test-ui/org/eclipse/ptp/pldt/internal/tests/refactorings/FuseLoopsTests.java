/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
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

import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.FuseLoopsRefactoring;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FuseLoopsTests extends RefactoringTest<FuseLoopsRefactoring> {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        return generateParameters("testcode/FuseLoops");
    }

    public FuseLoopsTests(String description, File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        super(FuseLoopsRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }
}
