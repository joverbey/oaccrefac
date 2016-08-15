/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     William Hester (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.AtomicStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.OpenACCParser;

public class IntroAtomicAlteration extends SourceStatementsAlteration<IntroAtomicCheck> {

	public static final int NONE = 0;
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int UPDATE = 3;
	
    public IntroAtomicAlteration(IASTRewrite rewriter, IntroAtomicCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() {
        for(IASTStatement statement : getStatements()) {
        	int offset = statement.getFileLocation().getNodeOffset();
            switch (determineAtomicType(statement)) {
            case READ:
            	insert(offset, "#pragma acc atomic read" + System.lineSeparator()); //$NON-NLS-1$
                break;
            case WRITE:
            	insert(offset, "#pragma acc atomic write" + System.lineSeparator()); //$NON-NLS-1$
                break;
            case UPDATE:
            	insert(offset, "#pragma acc atomic update" + System.lineSeparator()); //$NON-NLS-1$
                break;
            case NONE:
            	break;
            }
        }
        finalizeChanges();
    }
    
    /**
     * Here, we need to ensure that the variable that should become atomic is
     * inside the parallel region and that the declaration of that variable is
     * outside of the parallel region.
     * 
     * We return NONE if these conditions do not hold, and the appropriate 
     * atomic type otherwise
     */
    private int determineAtomicType(IASTStatement statement) {
    	int type;
        if (!(statement instanceof IASTExpressionStatement)) {
            getCheck().getStatus().addWarning(String.format(Messages.IntroAtomicCheck_MustBeExpression, statement.getFileLocation().getStartingLineNumber()));
            return NONE;
        }
        if (statement.getParent() instanceof IASTForStatement) {
            IASTForStatement forLoop = (IASTForStatement) statement.getParent();
            if (!forLoop.getBody().equals(statement)) {
            	getCheck().getStatus().addWarning(String.format(Messages.IntroAtomicCheck_MustBeInsideForLoop, statement.getFileLocation().getStartingLineNumber()));
                return NONE;
            }
        }

        OpenACCParser parser = new OpenACCParser();
        Map<IAccConstruct, IASTNode> prags = new HashMap<>();
        Map<IASTPreprocessorPragmaStatement, IASTNode> enclosingPragmas = ASTUtil.getEnclosingPragmas(statement);
        for (IASTPreprocessorPragmaStatement pragma : enclosingPragmas.keySet()) {
            try {
                IAccConstruct con = parser.parse(pragma.getRawSignature());
                prags.put(con, enclosingPragmas.get(pragma));
            } catch (Exception e) {
                // Parser couldn't parse the pragma; it's probably not an
                // OpenACC pragma.
            }
        }

        IASTNode parallelRegion = null;
        for (IAccConstruct construct : prags.keySet()) {
            if (OpenACCUtil.isAccAccelConstruct(construct)) {
                parallelRegion = prags.get(construct);
                break;
            }
        }

        if (parallelRegion == null) {
        	getCheck().getStatus().addWarning(String.format(Messages.IntroAtomicCheck_NotInParallelRegion, statement.getFileLocation().getStartingLineNumber()));
            return NONE;
        }
        AtomicStatementInquisitor inquisitor = AtomicStatementInquisitor.newInstance(statement, parallelRegion);
        switch (inquisitor.getType()) {
        case AtomicStatementInquisitor.WRITE:
            type = WRITE;
            break;
        case AtomicStatementInquisitor.READ:
            type = READ;
            break;
        case AtomicStatementInquisitor.UPDATE:
            type = UPDATE;
            break;
        case AtomicStatementInquisitor.NONE:
        	getCheck().getStatus().addWarning(Messages.IntroAtomicCheck_NoSuitableAtomicTypeFound);
            type = NONE;
			break;
		default:
			throw new IllegalStateException();
        }
        return type;
	}
    
}
