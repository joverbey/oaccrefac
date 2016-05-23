package org.eclipse.ptp.pldt.openacc.core.transformations;

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
		insertBefore(container, getPragma() + NL + LCURLY);
		insertAfter(container, RCURLY);
		finalizeChanges();
	}

}
