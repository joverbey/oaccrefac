/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.parser.ASTAccDataNode;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;
import edu.auburn.oaccrefac.core.parser.OpenACCParser;

public class ExpandDataConstructCheck extends PragmaDirectiveCheck<RefactoringParams> {

    private ASTAccDataNode construct;
    
    public ExpandDataConstructCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        super(pragma, statement);
    }
    
    @Override
    public void doFormCheck(RefactoringStatus status) {
        String msg = "The pragma must be an acc data construct.";
        try {
            construct = (ASTAccDataNode) (new OpenACCParser().parse(getPragma().getRawSignature()));
        }
        catch(Exception e) {
            //will enter on Exception from parser or ClassCastException if ACC non-data pragma
            status.addFatalError(msg);
        }
    }
    
    public ASTAccDataNode getConstruct() {
        return construct;
    }

}
