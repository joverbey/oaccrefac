/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.Activator;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

/**
 * UnrollLoopAlteration defines a loop unrolling refactoring algorithm. Loop unrolling
 * takes a sequential loop and 'unrolls' the loop by copying the body of the loop
 * multiple times in order to skip testing the conditional expression more times than
 * it has to.
 * <p>
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 19; i++) {
 *     a[i] = i;
 * }
 * </pre>
 * 
 * can be unrolled with a factor of 2 to produce
 * 
 * <pre>
 * for (int i = 0; i < 18; i++) {
 *     a[i] = i;
 *     i++;
 *     a[i] = i;
 * }
 * a[i] = i;
 * i++;
 * </pre>
 * 
 * The part at the end is called the 'trailer' and it holds the leftover iterations that could not
 * be satisfied by the unrolled loop body.
 * 
 * @author Adam Eichelkraut
 * @author Alexander Calvert
 * @author Jeff Overbey
 */
public class UnrollLoopAlteration extends ForLoopAlteration<UnrollLoopCheck> {

    private final IASTForStatement loop;
    private final int unrollFactor;
    private final int condOffset;
    private final int extras;
    private final IBinding oldIndexVariable;
    private final String newIndexVariable;

    /**
     * Constructor.
     * 
     * @param rewriter
     *            -- rewriter associated with loop
     * @param unrollFactor
     *            -- how many times to unroll loop body (must be > 0)
     * @throws DOMException
     */
    public UnrollLoopAlteration(IASTRewrite rewriter, int unrollFactor, UnrollLoopCheck check) throws CoreException {
        super(rewriter, check);
        this.unrollFactor = unrollFactor;

        this.loop = this.getLoop();

        Long lower = ForStatementInquisitor.getInquisitor(loop).getLowerBound();
        Long upper = check.getUpperBound();
        
        //TODO: other alteration just assume the check has passed; this is inconsistent
        //should have made this check from the UnrollLoopsCheck object already
        if(lower == null || upper == null) {
            throw new IllegalStateException();
        }
        
        // If the conditional expression is '<=',
        // change it and adjust the bound to include a +1.
        int condOffset = 0;

        IASTBinaryExpression condition = (IASTBinaryExpression) loop.getConditionExpression();
        if (condition.getOperator() == IASTBinaryExpression.op_lessEqual) {
            upper++;
            condOffset++;
        }

        // Number of extra iterations to add after loop based on divisibility.
        this.extras = (upper.intValue() - lower.intValue()) % unrollFactor;
        if (extras != 0) {
            condOffset -= extras;
        }

        this.condOffset = condOffset;

        this.oldIndexVariable = ForStatementInquisitor.getInquisitor(loop).getIndexVariable();
        if (shouldMoveDeclAboveLoop()) {
            try {
            	if (ASTUtil.isNameInScope(this.oldIndexVariable.getName(), loop.getScope().getParent())) {
            		this.newIndexVariable = createNewName(this.oldIndexVariable.getName(), loop.getScope().getParent());
            	} else {
            		this.newIndexVariable = this.oldIndexVariable.getName();
            	}
            } catch (DOMException e) {
                Activator.log(e);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
            }
        } else {
            this.newIndexVariable = this.oldIndexVariable.getName();
        }
    }

    private boolean shouldMoveDeclAboveLoop() {
        return loop.getInitializerStatement() instanceof IASTDeclarationStatement && extras != 0;
    }

    @Override
    protected void doChange() {
        this.insertAfter(loop.getBody(), createTrailer(extras));

        this.replace(loop, forLoop(getInitializer(), getCondition(condOffset), getIterationExpression(), createBody()));

        List<IASTPreprocessorPragmaStatement> prags = ASTUtil.getPragmaNodes(loop);
        int insertPoint = prags.size() > 0 ? prags.get(0).getFileLocation().getNodeOffset()
                : loop.getFileLocation().getNodeOffset();
        this.insert(insertPoint, createLeadingDeclaration());

        finalizeChanges();
    }

    /**
     * @return leftover ("extra") iterations inserted after the loop (when the number of iterations is not divisible by
     *         the unrolling factor)
     */
    private String createTrailer(int extras) {
        StringBuilder trailer = new StringBuilder();
        for (int i = 0; i < extras; i++) {
            appendModifiedOrigBody(trailer);
            trailer.append(newIndexVariable + "++;" + System.lineSeparator());
        }
        return trailer.toString();
    }

    private void appendModifiedOrigBody(StringBuilder sb) {
        IASTNode[] bodyObjects = getBodyObjects();
        for (IASTNode bodyObject : bodyObjects) {
            if (bodyObject instanceof IASTStatement) {
                sb.append(getModifiedStatement((IASTStatement) bodyObject, getUses(getNameFromInitializer())));
            } else {
                sb.append(bodyObject.getRawSignature() + System.lineSeparator());
            }
        }
    }

    private String createBody() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < unrollFactor; i++) {
            appendModifiedOrigBody(body);
            if (i != unrollFactor - 1) {
                body.append(newIndexVariable + "++;" + System.lineSeparator());
            }
        }
        return compound(body.toString());
    }

    private String createLeadingDeclaration() {
        // if the init statement is a declaration, move the declaration
        // to outer scope if possible and continue
        if (shouldMoveDeclAboveLoop()) {
            IASTName varname = ASTUtil.findFirst(loop.getInitializerStatement(), IASTName.class);
            String declaration;
            StringBuilder newDeclaration = new StringBuilder(loop.getInitializerStatement().getRawSignature());
            int start = varname.getFileLocation().getNodeOffset()
                    - loop.getInitializerStatement().getFileLocation().getNodeOffset();
            int end = start + varname.getFileLocation().getNodeLength();
            newDeclaration.replace(start, end, newIndexVariable);
            declaration = newDeclaration.toString();
            return declaration;
        } else {
            return "";
        }
    }

    private String getInitializer() {
        if (shouldMoveDeclAboveLoop()) {
            return "; ";
        } else {
            return getModifiedStatement(loop.getInitializerStatement(), getUses(getNameFromInitializer()));
        }
    }

    private String getCondition(int condOffset) {
        StringBuilder cond = new StringBuilder();
        cond.append(newIndexVariable);
        cond.append(" < ");
        String ub = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2().getRawSignature();
        if (condOffset > 0) {
            ub += " + " + condOffset;
        } else if (condOffset < 0) {
            ub += " - " + (-1 * condOffset);
        }
        cond.append(ub);
        return cond.toString();
    }

    private String getIterationExpression() {
        return newIndexVariable + "++";
    }

    private IASTNode[] getBodyObjects() {
        return getBodyObjects(false);
    }

    // gets statements AND comments from a loop body in forward order
    private IASTNode[] getBodyObjects(boolean reverse) {
        List<IASTNode> objects = new ArrayList<IASTNode>();
        objects.addAll(Arrays.asList(ASTUtil.getStatementsIfCompound(loop.getBody())));
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
        Collections.sort(objects, reverse ? ASTUtil.REVERSE_COMPARATOR : ASTUtil.FORWARD_COMPARATOR);

        return objects.toArray(new IASTNode[objects.size()]);

    }

    private IASTName getNameFromInitializer() {
        IASTStatement initStmt = loop.getInitializerStatement();
        if (initStmt instanceof IASTDeclarationStatement
                && ((IASTDeclarationStatement) initStmt).getDeclaration() instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration decl = (IASTSimpleDeclaration) ((IASTDeclarationStatement) initStmt).getDeclaration();
            return decl.getDeclarators()[0].getName();
        } else if (initStmt instanceof IASTExpressionStatement) {
            IASTExpression expr = ((IASTExpressionStatement) initStmt).getExpression();
            if (expr instanceof IASTBinaryExpression
                    && ((IASTBinaryExpression) expr).getOperator() == IASTBinaryExpression.op_assign) {
                IASTBinaryExpression binExp = (IASTBinaryExpression) expr;
                IASTExpression lhs = binExp.getOperand1();
                if (lhs instanceof IASTIdExpression) {
                    return ((IASTIdExpression) lhs).getName();
                }
            }
        }
        return null;
    }

    private List<IASTName> getUses(IASTName var) {

        class VariableUseFinder extends ASTVisitor {

            private IASTName var;
            public List<IASTName> uses;

            public VariableUseFinder(IASTName var) {
                this.var = var;
                this.uses = new ArrayList<IASTName>();
                shouldVisitNames = true;
            }

            @Override
            public int visit(IASTName name) {
                IBinding nameBinding = name.resolveBinding();
                IBinding varBinding = var.resolveBinding();
                if (nameBinding.equals(varBinding)) {
                    uses.add(name);
                }
                return PROCESS_CONTINUE;
            }

        }

        VariableUseFinder finder = new VariableUseFinder(var);
        loop.accept(finder);
        return finder.uses;

    }

    private String getModifiedStatement(IASTStatement original, List<IASTName> uses) {
        Collections.sort(uses, ASTUtil.REVERSE_COMPARATOR);
        StringBuilder sb = new StringBuilder(original.getRawSignature());
        for (IASTName use : uses) {
            if (ASTUtil.isAncestor(use, original)) {
                int offsetIntoStatement = use.getFileLocation().getNodeOffset()
                        - original.getFileLocation().getNodeOffset();
                sb.replace(offsetIntoStatement, offsetIntoStatement + use.getFileLocation().getNodeLength(),
                        newIndexVariable);
            }
        }
        return sb.toString();
    }

}