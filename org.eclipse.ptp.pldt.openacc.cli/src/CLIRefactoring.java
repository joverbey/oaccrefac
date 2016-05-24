
/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.Check;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.SourceAlteration;

/**
 * Main serves as a generic base for any refactoring Runnables.
 *
 * @param <P>
 *            Refactoring parameters.
 * @param <C>
 *            Checker.
 * @param <A>
 *            Source alteration.
 */
public abstract class CLIRefactoring<P extends RefactoringParams, C extends Check<P>> {

	private C check;
    
    public RefactoringStatus performChecks(IASTStatement statement) {
    	check = createCheck(statement);
    	if (check == null) {
    		return null;
    	}
    	return check.performChecks(new RefactoringStatus(), new NullProgressMonitor(), createParams(statement));
    }
    
    public final String performAlteration(IASTRewrite rw) {
    	try {
            SourceAlteration<?> xform = createAlteration(rw, check);
            xform.change();
            xform.rewriteAST().perform(new NullProgressMonitor());
        } catch (CoreException e) {
            return e.getMessage();
        }
    	return null;
    }

    /**
     * createParams creates parameters for an alteration.
     * 
     * @param statement
     *            Statement to create the parameters for.
     * @return Params.
     */
    protected P createParams(IASTStatement statement) {
    	return null;
    }

    /**
     * createCheck creates the checker for an alteration.
     * 
     * @param statement
     *            Statement to check.
     * @return Checker.
     */
    protected abstract C createCheck(IASTStatement statement);

    /**
     * createAlteration writes the alteration.
     * 
     * @param rewriter
     * @param check
     * @return Alteration to use on file.
     * @throws CoreException
     */
    public abstract SourceAlteration<?> createAlteration(IASTRewrite rewriter, C check) throws CoreException;
}