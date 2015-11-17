/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneCheck;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneAlteration;

/**
 * Command line driver to introduce a kernels loop.
 */
public class IntroduceDefaultNone extends Main<RefactoringParams, IntroDefaultNoneCheck, IntroDefaultNoneAlteration> {
    
    public static void main(String[] args) {
        new IntroduceDefaultNone().run(args);
    }

    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 1) {
            printUsage();
            return false;
        }
        return true;
    }

    private void printUsage() {
        System.err.println("Usage: IntroduceDefaultNone <filename.c>");
    }

    @Override
    protected IntroDefaultNoneCheck createCheck(IASTForStatement loop) {
        // What if there is more than one pragma on the loop?
        List<IASTPreprocessorPragmaStatement> pragmas = ForStatementInquisitor.getInquisitor(loop).getLeadingPragmas();
        // What is the second parameter to this?
        return new IntroDefaultNoneCheck(pragmas.get(0), null);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected IntroDefaultNoneAlteration createAlteration(IASTRewrite rewriter, IntroDefaultNoneCheck check) throws CoreException {
        return new IntroDefaultNoneAlteration(rewriter, check);
    }
    
}
