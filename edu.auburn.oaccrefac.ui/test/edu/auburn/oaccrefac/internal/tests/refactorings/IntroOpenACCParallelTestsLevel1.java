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

import edu.auburn.oaccrefac.internal.ui.refactorings.IntroOpenACCParallelRefactoring;

@RunWith(Parameterized.class)
public class IntroOpenACCParallelTestsLevel1 extends RefactoringTestEPCC<IntroOpenACCParallelRefactoring> {
    
    private static final String m_testDir = "testcode-level1/IntroOpenACCParallel/";
    
    public IntroOpenACCParallelTestsLevel1(String description, File fileContainingMarker, int markerOffset, String markerText)
            throws Exception {
        super(IntroOpenACCParallelRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    protected File resultFileFor(String filename, String kernelDescription) {
        return new File(String.format(m_testDir+"%s.c.result", kernelDescription));
    }
}
