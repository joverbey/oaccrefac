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

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.auburn.oaccrefac.internal.ui.refactorings.DistributeLoopsRefactoring;

@RunWith(Parameterized.class)
public class DistributeLoopsTestsComplete extends RefactoringTestComplete<DistributeLoopsRefactoring> {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        return generateParameters("testcode-epcc/level1.c");
    }

    public DistributeLoopsTestsComplete(String description, File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        super(DistributeLoopsRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    @Override
    protected File resultFileFor(String filename, String kernelDescription) {
        // TODO Auto-generated method stub
        return null;
    }
}
