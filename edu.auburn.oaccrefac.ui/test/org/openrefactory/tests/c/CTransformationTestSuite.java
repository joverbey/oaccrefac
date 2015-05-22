/*******************************************************************************
 * Copyright (c) 2011 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package org.openrefactory.tests.c;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.rephraserengine.core.util.Spawner;
import org.eclipse.rephraserengine.testing.junit3.RefactoringTestSuite;

/**
 * Common superclass for C refactoring test suites.
 *
 * @author Jeff Overbey
 */
public abstract class CTransformationTestSuite<R extends CRefactoring> extends RefactoringTestSuite<R> {
    
    public CTransformationTestSuite(Class<R> clazz, String directory) throws Exception {
        super(clazz,
                CTestUtil.MARKER,
                CTestUtil.MARKER_END,
                new File(directory),
                CTestUtil.C_FILENAME_FILTER);
    }

    @Override
    protected boolean shouldCompile(IFile fileContainingMarker) {
        return true;
    }

    @Override
    protected String compileAndRunProgram(Map<String, IFile> files) throws Exception {
        Spawner.SHOW_OUTPUT_ON_ERROR = true;
        File cwd = files.values().iterator().next().getParent().getLocation().toFile();
        
        List<String> args = new ArrayList<String>();
        args.add("gcc");
        args.add("-o");
        args.add("a.out");
        args.add("-std=c99");
        args.addAll(files.keySet());
        addAdditionalCCArgs(args);
        Spawner.run(cwd, args);
        
        return Spawner.run(cwd, cwd.getAbsolutePath() + File.separator + "a.out");
    }

    protected void addAdditionalCCArgs(List<String> args) {
    }

    @Override
    protected Test createTestFor(File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        TestSuite suite = new TestSuite(fileContainingMarker + " " + markerText);
        suite.addTest(super.createTestFor(fileContainingMarker, markerOffset, markerText));
        return suite;
    }
}