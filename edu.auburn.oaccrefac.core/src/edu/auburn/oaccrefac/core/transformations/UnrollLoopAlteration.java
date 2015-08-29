package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop unrolling refactoring algorithm. Loop unrolling
 * takes a sequential loop and 'unrolls' the loop by copying the body of the loop multiple times in order to skip
 * testing the conditional expression more times than it has to.
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 19; i++) {
 *     a = 5;
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int i = 0; i < 18; i += 2) {
 *     a = 5;
 *     i++;
 *     a = 5;
 * }
 * i++;
 * a = 5;
 * i++;
 * </pre>
 * 
 * The part at the end is called the 'trailer' and it holds the leftover iterations that could not be satisfied by the
 * unrolled loop body.
 * 
 * @author Adam Eichelkraut
 *
 */
public class UnrollLoopAlteration extends ForLoopAlteration {

    private int unrollFactor;
    private Long upperBound;

    /**
     * Constructor.
     * 
     * @param rewriter
     *            -- rewriter associated with loop
     * @param loop
     *            -- loop in which to unroll
     * @param unrollFactor
     *            -- how many times to unroll loop body (must be > 0)
     */
    public UnrollLoopAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, int unrollFactor) {
        super(tu, rewriter, loop);
        this.unrollFactor = unrollFactor;
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        // Check unroll factor validity...
        if (unrollFactor <= 0) {
            init.addFatalError("Invalid loop unroll factor! (<= 0)");
            return;
        }

        // If the upper bound is not a constant, we cannot do loop unrolling
        IASTForStatement loop = getLoopToChange();
        IASTFunctionDefinition enclosing = ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
        ConstantPropagation constantprop_ub = new ConstantPropagation(enclosing);
        IASTExpression ub_expr = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2();
        upperBound = constantprop_ub.evaluate(ub_expr);
        if (upperBound == null) {
            init.addFatalError("Upper bound is not a constant value. Cannot perform unrolling!");
            return;
        }

        IASTStatement body = loop.getBody();
        // if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement) {
            init.addFatalError("Loop body is empty -- nothing to unroll!");
            return;
        }
        return;
    }

    @Override
    protected void doChange() {
        IASTForStatement loop = this.getLoopToChange();

        //TODO move his code somewhere else to use refactoring status
        //If the upper bound is not a constant, we cannot do loop unrolling
        IASTFunctionDefinition enclosing = ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
        ConstantPropagation constantprop_ub = new ConstantPropagation(enclosing);
        IASTExpression ub_expr = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2();
        upperBound = constantprop_ub.evaluate(ub_expr);
        if (upperBound == null) {
            return;
        }

        // get the loop upper bound from AST, if the conditional statement is '<=',
        // change it and adjust the bound to include a +1.
        int upper = upperBound.intValue();
        int condOffset = 0;
        IASTBinaryExpression condition = (IASTBinaryExpression) loop.getConditionExpression();
        if (condition.getOperator() == IASTBinaryExpression.op_lessEqual) {
            upper = upper + 1;
            condOffset = condOffset + 1;
        }

        // Number of extra iterations to add after loop based on divisibility.
        int extras = upper % unrollFactor;
        if (extras != 0) {
            condOffset = condOffset - extras;
        }

        try {
        
            this.insert(loop.getBody().getFileLocation().getNodeOffset() + loop.getBody().getFileLocation().getNodeLength(),
                    getTrailer(loop, extras));
            this.replace(loop, forLoop(getInitializer(loop), getCondition(loop, condOffset), getIterationExpression(loop),
                    getBody(loop)));
            this.insert(loop.getFileLocation().getNodeOffset(), getLeadingDeclaration(loop));
        
        }
        catch (DOMException e) {
            e.printStackTrace();
            return;
        }

        finalizeChanges();
    }

    private String getTrailer(IASTForStatement loop, int extras) {
        StringBuilder trailer = new StringBuilder();
        for (int i = 0; i < extras; i++) {
            IASTNode[] bodyObjects = getBodyObjects(loop);
            trailer.append(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString() + "++;"
                    + System.lineSeparator());
            for (IASTNode bodyObject : bodyObjects) {
                trailer.append(bodyObject.getRawSignature());
            }
        }
        return trailer.toString();
    }

    private String getBody(IASTForStatement loop) {
        StringBuilder body = new StringBuilder("");
        for (int i = 0; i < unrollFactor; i++) {
            for (IASTNode bodyObject : getBodyObjects(loop)) {
                body.append(bodyObject.getRawSignature() + System.lineSeparator());
            }
            if (i != unrollFactor - 1) {
                body.append(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString() + "++;"
                        + System.lineSeparator());
            }
        }
        return compound(body.toString());
    }

    private String getLeadingDeclaration(IASTForStatement loop) {
        // if the init statement is a declaration, move the declaration
        // to outer scope if possible and continue
        // TODO don't move the declaration in between loop and pragma
        try {
            if (loop.getInitializerStatement() instanceof IASTDeclarationStatement) {
                IASTName varname = ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class);
                String declaration;
                StringBuilder newDeclaration = new StringBuilder(loop.getInitializerStatement().getRawSignature());
                int start = varname.getFileLocation().getNodeOffset()
                        - loop.getInitializerStatement().getFileLocation().getNodeOffset();
                int end = start + varname.getFileLocation().getNodeLength();
                newDeclaration.replace(start, end, newName(varname.toString(), loop.getScope().getParent()));
                declaration = newDeclaration.toString();
                return declaration;
            } else {
                return "";
            }
        } catch (DOMException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getInitializer(IASTForStatement loop) {
        return loop.getInitializerStatement() instanceof IASTDeclarationStatement ? "; "
                : loop.getInitializerStatement().getRawSignature();
    }

    private String getCondition(IASTForStatement loop, int condOffset) throws DOMException {
        StringBuilder cond = new StringBuilder();
        cond.append(newName(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString(), loop.getScope().getParent()));
        cond.append(" < ");
        cond.append(parenth(((IASTBinaryExpression) loop.getConditionExpression()).getOperand2().getRawSignature() + " + " + condOffset));
        return cond.toString();
    }

    private String getIterationExpression(IASTForStatement loop) throws DOMException {
        return newName(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString(), loop.getScope().getParent()) + " += " + unrollFactor;
    }

    private String newName(String name, IScope scope) {
        if(ASTUtil.isNameInScope(name, scope)) {
            for (int i = 0; true; i++) {
                String newName = name + "_" + i;
                if (!ASTUtil.isNameInScope(newName, scope)) {
                    return newName;
                }
            }
        }
        else {
            return name;
        }
        
    }

    private IASTStatement[] getBodyStatements(IASTForStatement loop) {
        if (loop.getBody() instanceof IASTCompoundStatement) {
            return ((IASTCompoundStatement) loop.getBody()).getStatements();
        } else {
            return new IASTStatement[] { loop.getBody() };
        }
    }

    private IASTNode[] getBodyObjects(IASTForStatement loop) {
        return getBodyObjects(loop, false);
    }

    // gets statements AND comments from a loop body in forward order
    private IASTNode[] getBodyObjects(IASTForStatement loop, boolean reverse) {
        List<IASTNode> objects = new ArrayList<IASTNode>();
        objects.addAll(Arrays.asList(getBodyStatements(loop)));
        for (IASTComment comment : loop.getTranslationUnit().getComments()) {
            // if the comment's offset is in between the end of the loop header and the end of the loop body
            if (comment.getFileLocation()
                    .getNodeOffset() > loop.getIterationExpression().getFileLocation().getNodeOffset()
                            + loop.getIterationExpression().getFileLocation().getNodeLength() + ")".length()
                    && comment.getFileLocation().getNodeOffset() < loop.getBody().getFileLocation().getNodeOffset()
                            + loop.getBody().getFileLocation().getNodeLength()) {
                objects.add(comment);
            }
        }
        Collections.sort(objects, reverse ? new Comparator<IASTNode>() {

            @Override
            public int compare(IASTNode node1, IASTNode node2) {
                return node2.getFileLocation().getNodeOffset() - node1.getFileLocation().getNodeOffset();
            }

        } : new Comparator<IASTNode>() {

            @Override
            public int compare(IASTNode node1, IASTNode node2) {
                return node1.getFileLocation().getNodeOffset() - node2.getFileLocation().getNodeOffset();
            }
        });

        return objects.toArray(new IASTNode[objects.size()]);

    }

    // * Unrolling function that unrolls the loop body multiple times within itself,
    // * adding iteration expression statements inbetween body copies in order to
    // * maintain original behavior.
    // * @param rewriter -- rewriter associated with loop argument
    // * @param loop -- loop in which to unroll
    // * @return -- rewriter? I still don't know.
    // */
    // private IASTRewrite unroll(IASTRewrite rewriter, IASTForStatement loop) {
    // ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
    // IASTExpression iter_expr = loop.getIterationExpression();
    // IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
    //
    // IASTStatement body = loop.getBody();
    // IASTNode[] chilluns = body.getChildren();
    //
    // IASTCompoundStatement newBody = factory.newCompoundStatement();
    // IASTRewrite body_rewriter = this.safeReplace(rewriter, body, newBody);
    // for (int i = 0; i < m_unrollFactor; i++) {
    // if (body instanceof IASTCompoundStatement) {
    // for (int j = 0; j < chilluns.length; j++) {
    // if (chilluns[j] instanceof IASTStatement)
    // this.safeInsertBefore(body_rewriter, newBody, null, chilluns[j].copy());
    // }
    // } else {
    // this.safeInsertBefore(body_rewriter, newBody, null, body.copy());
    // }
    // if (i != m_unrollFactor-1) {
    // this.safeInsertBefore(body_rewriter, newBody, null, iter_exprstmt);
    // }
    // }
    //
    // return rewriter;
    // }

}