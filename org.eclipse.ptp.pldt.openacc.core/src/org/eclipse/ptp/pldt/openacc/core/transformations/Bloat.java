package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;

public class Bloat extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

	IASTForStatement container;
	
	public Bloat(IASTRewrite rewriter, ExpandDataConstructCheck check, IASTForStatement container) {
		super(rewriter, check);
		this.container = container;
	}
	
	@Override
	public void doChange() {
		remove(getPragma());
		replace(container.getBody(), compound(decompound(getStatement().getRawSignature())));
		
		String pragma = getPragma().getRawSignature();
		for(IASTComment comment : getStatement().getTranslationUnit().getComments()) {
			if(comment.getFileLocation().getStartingLineNumber() == getPragma().getFileLocation().getStartingLineNumber()) {
				pragma += " " + comment.getRawSignature();
				this.remove(comment);
			}
		}
		
		insertBefore(container, pragma + NL + LCURLY);
		insertAfter(container, RCURLY);
		finalizeChanges();
	}

}
