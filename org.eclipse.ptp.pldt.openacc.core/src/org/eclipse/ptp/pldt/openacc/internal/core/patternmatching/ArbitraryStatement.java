/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.patternmatching;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.core.dom.parser.ASTAttributeOwner;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTNullStatement;

/**
 * This class represents an unknown statement in an AST pattern.
 * <p>
 * Based on {@link CASTNullStatement}
 * 
 * @see ASTMatcher
 */
@SuppressWarnings("restriction")
public final class ArbitraryStatement extends ASTAttributeOwner implements IASTStatement {
    @Override
    public boolean accept(ASTVisitor action) {
        if (action.shouldVisitStatements) {
            switch (action.visit(this)) {
            case ASTVisitor.PROCESS_ABORT:
                return false;
            case ASTVisitor.PROCESS_SKIP:
                return true;
            default:
                break;
            }
        }

        //if (!acceptByAttributes(action))
        //    return false;
        if (!acceptByAttributeSpecifiers(action))
            return false;

        if (action.shouldVisitStatements) {
            switch (action.leave(this)) {
            case ASTVisitor.PROCESS_ABORT:
                return false;
            case ASTVisitor.PROCESS_SKIP:
                return true;
            default:
                break;
            }
        }
        return true;
    }

    @Override
    public ArbitraryStatement copy() {
        return copy(CopyStyle.withoutLocations);
    }

    @Override
    public ArbitraryStatement copy(CopyStyle style) {
        ArbitraryStatement copy = new ArbitraryStatement();
        return copy(copy, style);
    }
}
