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
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopStripMiningRefactoring;

@RunWith(Parameterized.class)
public class LoopStripMiningTestsLL extends RefactoringTestLL<LoopStripMiningRefactoring> {
    
    private static final String m_testDir = "testcode-ll/StripMining/";

    public LoopStripMiningTestsLL(String description, File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        super(LoopStripMiningRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }
    
    @Override
    protected void configureRefactoring(LoopStripMiningRefactoring refactoring, IFile file, TextSelection selection, LinkedList<String> markerFields) {
        super.configureRefactoring(refactoring, file, selection, markerFields); 
        //For now, just set the parameter to a constant 2
        //TODO Figure out a way to test multiple input parameters? From result file maybe?
        refactoring.setStripFactor(3);
    }

    @Override
    protected File resultFileFor(String filename, String kernelDescription) {
        return new File(String.format(m_testDir+"%s.c.result", kernelDescription));
    }
}