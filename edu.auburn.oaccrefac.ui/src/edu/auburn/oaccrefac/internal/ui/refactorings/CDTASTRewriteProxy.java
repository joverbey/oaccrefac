package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.change.IASTRewrite;

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
