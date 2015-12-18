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

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneCheck;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneAlteration;

/**
 * IntroduceDefaultNone performs the introduce default none refactoring.
 */
public class IntroduceDefaultNone extends StatementMain<RefactoringParams, IntroDefaultNoneCheck, IntroDefaultNoneAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
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

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: IntroduceDefaultNone <filename.c>");
    }
    
    @Override
    protected IntroDefaultNoneCheck createCheck(IASTStatement statement) {
        int pragmaPosition = statement.getFileLocation().getStartingLineNumber() - 1;
        IASTPreprocessorPragmaStatement pragma = null;
        for (IASTPreprocessorStatement otherPragma : statement.getTranslationUnit().getAllPreprocessorStatements()) {
            if (otherPragma.getFileLocation().getStartingLineNumber() == pragmaPosition) {
                pragma = (IASTPreprocessorPragmaStatement) otherPragma;
                break;
            }
        }
        return new IntroDefaultNoneCheck(pragma, statement);
    }

    @Override
    protected RefactoringParams createParams(IASTStatement statement) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected IntroDefaultNoneAlteration createAlteration(IASTRewrite rewriter, IntroDefaultNoneCheck check) throws CoreException {
        return new IntroDefaultNoneAlteration(rewriter, check);
    }
    
}
