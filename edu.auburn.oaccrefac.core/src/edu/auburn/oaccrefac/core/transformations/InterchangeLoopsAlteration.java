/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop interchange refactoring algorithm. Loop interchange
 * swaps the headers of two perfectly nested loops, given that it causes no dependency issues from
 * {@link InterchangeLoopsCheck}.
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     for (int j = 1; j < 20; j++) {
 *         // do something...
 *     }
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int j = 1; j < 20; j++) {
 *     for (int i = 0; i < 10; i++) {
 *         // do something...
 *     }
 * }
 * </pre>
 * 
 * @author Adam Eichelkraut
 */
public class InterchangeLoopsAlteration extends ForLoopAlteration<InterchangeLoopsCheck> {

    private IASTForStatement outer;
    private IASTForStatement inner;

    /**
     * Constructor that takes in two loops to interchange
     * @param rewriter
     *            -- rewriter associated with these nodes
     * 
     * @throws IllegalArgumentException
     *             if second loop is null
     */
    public InterchangeLoopsAlteration(IASTRewrite rewriter, InterchangeLoopsCheck check) {
        super(rewriter, check);
        this.outer = check.getOuterLoop();
        this.inner = check.getInnerLoop();
    }

    @Override
    protected void doChange() {
        IASTForStatement first = getLoopToChange();
        List<IASTPreprocessorPragmaStatement> firstPrags = ASTUtil.getLeadingPragmas(first);
        List<IASTPreprocessorPragmaStatement> secondPrags = ASTUtil.getLeadingPragmas(inner);
        Collections.reverse(firstPrags);
        Collections.reverse(secondPrags);

        replace(inner.getIterationExpression(), first.getIterationExpression().getRawSignature());
        replace(inner.getConditionExpression(), first.getConditionExpression().getRawSignature());
        replace(inner.getInitializerStatement(), first.getInitializerStatement().getRawSignature());
        for (IASTPreprocessorPragmaStatement prag : firstPrags) {
            insert(inner.getFileLocation().getNodeOffset(), prag.getRawSignature() + System.lineSeparator());
        }
        for (IASTPreprocessorPragmaStatement prag : secondPrags) {
            remove(prag.getFileLocation().getNodeOffset(),
                    prag.getFileLocation().getNodeLength() + System.lineSeparator().length());
        }

        replace(first.getIterationExpression(), inner.getIterationExpression().getRawSignature());
        replace(first.getConditionExpression(), inner.getConditionExpression().getRawSignature());
        replace(first.getInitializerStatement(), inner.getInitializerStatement().getRawSignature());
        for (IASTPreprocessorPragmaStatement prag : secondPrags) {
            insert(first.getFileLocation().getNodeOffset(), prag.getRawSignature() + System.lineSeparator());
        }
        for (IASTPreprocessorPragmaStatement prag : firstPrags) {
            remove(prag.getFileLocation().getNodeOffset(),
                    prag.getFileLocation().getNodeLength() + System.lineSeparator().length());
        }
        finalizeChanges();

    }
}
