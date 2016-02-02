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

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopTilingRefactoring;

@RunWith(Parameterized.class)
public class LoopTilingTestslevel1 extends RefactoringTestEPCC<LoopTilingRefactoring> {
    
    private static final String m_testDir = "testcode-level1/LoopTiling/";
    
    public LoopTilingTestslevel1(String description, File fileContainingMarker, int markerOffset, String markerText)
            throws Exception {
        super(LoopTilingRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    protected File resultFileFor(String filename, String kernelDescription) {
        return new File(String.format(m_testDir+"%s.c.result", kernelDescription));
    }
}
