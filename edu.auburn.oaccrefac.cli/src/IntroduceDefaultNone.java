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

    /**
     * checkArgs checks the arguments to the refactoring.
     * 
     * @param args Arguments to the refactoring.
     * @return Value representing the result of checking the arguments.
     */
    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 2) {
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

    /**
     * createCheck creates an IntroduceDefaultNoneCheck.
     * 
     * @param statement Statement to create the check for.
     * @return Check to be performed on the statement.
     */
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

    /**
     * createParams creates generic RefactoringParams.
     * 
     * @param statement Not used.
     * @return null because parameters are not used in this refactoring.
     */
    @Override
    protected RefactoringParams createParams(IASTStatement statement) {
        // RefactoringParams is abstract
        return null;
    }

    /**
     * createAlteration creates a IntroduceDefaultNoneAlteration.
     * 
     * @param reweriter Rewriter for the alteration.
     * @param check Checker for the alteration.
     * @return Alteration for the refactoring.
     * @throws CoreException if creating the alteration fails.
     */
    @Override
    protected IntroDefaultNoneAlteration createAlteration(IASTRewrite rewriter, IntroDefaultNoneCheck check) throws CoreException {
        return new IntroDefaultNoneAlteration(rewriter, check);
    }
    
}
