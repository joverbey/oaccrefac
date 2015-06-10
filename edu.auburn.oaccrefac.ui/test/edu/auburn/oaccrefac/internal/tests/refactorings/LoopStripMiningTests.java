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

import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopStripMiningRefactoring;
import edu.auburn.oaccrefac.internal.util.RefactoringTestSuite;
import junit.framework.Test;

public class LoopStripMiningTests extends RefactoringTestSuite<LoopStripMiningRefactoring> {
    public static Test suite() throws Exception {
        return new LoopStripMiningTests();
    }

    public LoopStripMiningTests() throws Exception {
        super(LoopStripMiningRefactoring.class, "testcode/LoopStripMining");
    }

    @Override
    protected void configureRefactoring(LoopStripMiningRefactoring refactoring, IFile file,
            TextSelection selection, LinkedList<String> markerFields) {
         String unrollFactor = markerFields.removeFirst();
         refactoring.setStripFactor(Integer.parseInt(unrollFactor));
    }
}