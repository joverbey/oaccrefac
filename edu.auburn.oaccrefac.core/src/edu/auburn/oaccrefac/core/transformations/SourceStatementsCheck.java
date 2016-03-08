/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

public abstract class SourceStatementsCheck<T extends RefactoringParams> extends Check<T> {

    private final IASTStatement[] statements;
    private final IASTNode[] statementsAndComments;
    
    protected SourceStatementsCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        this.statements = statements;
        this.statementsAndComments = statementsAndComments;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return statements[0].getTranslationUnit();
    }
    
    public IASTStatement[] getStatements() {
        return statements;
    }

    public IASTNode[] getStatementsAndComments() {
        return statementsAndComments;
    }

}
