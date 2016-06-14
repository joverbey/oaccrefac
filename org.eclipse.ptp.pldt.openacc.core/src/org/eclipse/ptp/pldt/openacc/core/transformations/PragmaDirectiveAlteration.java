/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public abstract class PragmaDirectiveAlteration<T extends PragmaDirectiveCheck<?>> extends SourceAlteration<T> {

    private IASTPreprocessorPragmaStatement pragma;
    private IASTStatement statement;
    
    public PragmaDirectiveAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.pragma = check.getPragma();
        this.statement = check.getStatement();
    }
    
    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

    public IASTStatement getStatement() {
        return statement;
    }

}
