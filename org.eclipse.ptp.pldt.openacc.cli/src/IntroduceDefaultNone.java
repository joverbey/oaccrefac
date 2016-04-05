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
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroDefaultNoneAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroDefaultNoneCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

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
        if (!((args.length == 3 && args[1].equals("-ln")) || (args.length == 1 ))) {
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
        System.err.println("Usage2: IntroduceDefaultNone <filename.c> -ln <loopname>");
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