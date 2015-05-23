package edu.auburn.oaccrefac.internal.core.patternmatching;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.core.dom.parser.ASTAttributeOwner;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTNullStatement;

/**
 * This class represents an unknown statement in an AST pattern.
 * <p>
 * Based on {@link CASTNullStatement}
 * 
 * @author Jeff Overbey
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