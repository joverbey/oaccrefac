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

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;

import edu.auburn.oaccrefac.core.transformations.Check;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.SourceAlteration;

/**
 * LoopMain is a generic base for loop refactorings.
 *
 * @param <P> Refactoring parameters.
 * @param <C> Checker.
 * @param <A> Source alteration.
 */
public abstract class LoopMain<P extends RefactoringParams, C extends Check<P>, A extends SourceAlteration<C>> extends Main<IASTForStatement, P, C, A> {

    @Override
    protected IASTForStatement convertStatement(IASTStatement statement) {
        return (IASTForStatement) statement;
    }
    
}