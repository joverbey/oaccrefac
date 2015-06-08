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

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopFusionRefactoring;
import edu.auburn.oaccrefac.internal.util.RefactoringTestSuite;
import junit.framework.Test;

public class LoopFusionTests extends RefactoringTestSuite<LoopFusionRefactoring> {
    public static Test suite() throws Exception {
        return new LoopFusionTests();
    }

    public LoopFusionTests() throws Exception {
        super(LoopFusionRefactoring.class, "testcode/LoopFusion");
    }
}
