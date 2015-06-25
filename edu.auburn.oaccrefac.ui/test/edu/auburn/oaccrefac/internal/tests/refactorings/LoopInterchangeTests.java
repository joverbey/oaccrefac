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
import org.junit.runners.Parameterized.Parameters;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopInterchangeRefactoring;

@RunWith(Parameterized.class)
public class LoopInterchangeTests extends RefactoringTest<LoopInterchangeRefactoring> {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> generateParameters() throws Exception {
        Iterable<Object[]> params = generateParameters("testcode/LoopInterchange");
        return params;
    }

    public LoopInterchangeTests(String description, File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        super(LoopInterchangeRefactoring.class, fileContainingMarker, markerOffset, markerText);
    }

    @Override
    protected void configureRefactoring(LoopInterchangeRefactoring refactoring, IFile file,
            TextSelection selection, LinkedList<String> markerFields) {
         String unrollFactor = markerFields.removeFirst();
         refactoring.setExchangeDepth(Integer.parseInt(unrollFactor));
    }
}