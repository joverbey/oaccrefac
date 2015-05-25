package edu.auburn.oaccrefac.internal.core.patternmatching;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.ISemanticProblem;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.eclipse.cdt.internal.core.dom.parser.ProblemType;

/**
 * This class represents an arbitrary integer literal in an AST pattern.
 * 
 * @author Jeff Overbey
 */
@SuppressWarnings("restriction")
public final class ArbitraryIntegerConstant extends ASTNode implements IASTExpression {
    public ArbitraryIntegerConstant() {
    }

    @Override
    public ArbitraryIntegerConstant copy() {
        return copy(CopyStyle.withoutLocations);
    }

    @Override
    public ArbitraryIntegerConstant copy(CopyStyle style) {
        ArbitraryIntegerConstant copy = new ArbitraryIntegerConstant();
        return copy(copy, style);
    }

    @Override
    public String toString() {
        return "?????";
    }

    @Override
    public boolean accept(ASTVisitor action) {
        if (action.shouldVisitExpressions) {
            switch (action.visit(this)) {
            case ASTVisitor.PROCESS_ABORT:
                return false;
            case ASTVisitor.PROCESS_SKIP:
                return true;
            default:
                break;
            }
        }
        if (action.shouldVisitExpressions) {
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
    public IType getExpressionType() {
        return new ProblemType(ISemanticProblem.TYPE_UNKNOWN_FOR_EXPRESSION);
    }

    @Override
    public final ValueCategory getValueCategory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLValue() {
        throw new UnsupportedOperationException();
    }
}
