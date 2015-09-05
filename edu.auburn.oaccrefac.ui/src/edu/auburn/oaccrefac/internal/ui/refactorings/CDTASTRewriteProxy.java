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
package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;

public class CDTASTRewriteProxy implements IASTRewrite {

    private final ASTRewrite rewrite;

    public CDTASTRewriteProxy(ASTRewrite rewrite) {
        this.rewrite = rewrite;
    }

    public IASTNode createLiteralNode(String code) {
        return this.rewrite.createLiteralNode(code);
    }

    public void remove(IASTNode node, TextEditGroup editGroup) {
        this.rewrite.remove(node, editGroup);
    }

    public CDTASTRewriteProxy replace(IASTNode node, IASTNode replacement, TextEditGroup editGroup) {
        return new CDTASTRewriteProxy(this.rewrite.replace(node, replacement, editGroup));
    }

    public CDTASTRewriteProxy insertBefore(IASTNode parent, IASTNode insertionPoint, IASTNode newNode,
            TextEditGroup editGroup) {
        return new CDTASTRewriteProxy(this.rewrite.insertBefore(parent, insertionPoint, newNode, editGroup));
    }

    public Change rewriteAST() {
        return this.rewrite.rewriteAST();
    }
}
