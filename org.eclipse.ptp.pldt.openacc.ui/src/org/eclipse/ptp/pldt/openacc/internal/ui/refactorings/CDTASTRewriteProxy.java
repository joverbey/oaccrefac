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
package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.text.edits.TextEditGroup;

public class CDTASTRewriteProxy implements IASTRewrite {

    private final ASTRewrite rewrite;

    public CDTASTRewriteProxy(ASTRewrite r) {
        rewrite = r;
    }

    @Override
    public IASTNode createLiteralNode(String code) {
        return rewrite.createLiteralNode(code);
    }

    @Override
    public void remove(IASTNode node, TextEditGroup editGroup) {
        rewrite.remove(node, editGroup);
    }

    @Override
    public CDTASTRewriteProxy replace(IASTNode node, IASTNode replacement, TextEditGroup editGroup) {
        return new CDTASTRewriteProxy(rewrite.replace(node, replacement, editGroup));
    }

    @Override
    public CDTASTRewriteProxy insertBefore(IASTNode parent, IASTNode insertionPoint, IASTNode newNode,
            TextEditGroup editGroup) {
        return new CDTASTRewriteProxy(rewrite.insertBefore(parent, insertionPoint, newNode, editGroup));
    }

    @Override
    public Change rewriteAST() {
        return rewrite.rewriteAST();
    }
}
