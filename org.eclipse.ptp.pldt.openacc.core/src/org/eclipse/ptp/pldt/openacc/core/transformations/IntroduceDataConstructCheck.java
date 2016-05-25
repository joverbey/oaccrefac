/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     William Hester (Auburn) - Decouple SourceStatementsCheck and
 *     			IntroduceDataConstructCheck
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyin;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyout;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCreate;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

public class IntroduceDataConstructCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroduceDataConstructCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        super(statements, statementsAndComments);
    }

    @Override
    public RefactoringStatus doCheck(RefactoringStatus status, IProgressMonitor pm) {
        doReachingDefinitionsCheck(status,
                new ReachingDefinitions(ASTUtil.findNearestAncestor(getStatements()[0], IASTFunctionDefinition.class)));
        return status;
    }

    protected void doReachingDefinitionsCheck(RefactoringStatus status, ReachingDefinitions rd) {
        Map<IASTStatement, Set<IBinding>> copyin = new InferCopyin(rd, getStatements()).get();
        Map<IASTStatement, Set<IBinding>> copyout = new InferCopyout(rd, getStatements()).get();
        Map<IASTStatement, Set<IBinding>> create = new InferCreate(rd, getStatements()).get();
        boolean allRootsEmpty = true;
        for (IASTStatement statement : copyin.keySet()) {
            if (statement instanceof ArbitraryStatement && !copyin.get(statement).isEmpty()) {
                allRootsEmpty = false;
            }
        }
        for (IASTStatement statement : copyout.keySet()) {
            if (statement instanceof ArbitraryStatement && !copyout.get(statement).isEmpty()) {
                allRootsEmpty = false;
            }
        }
        for (IASTStatement statement : create.keySet()) {
            if (statement instanceof ArbitraryStatement && !create.get(statement).isEmpty()) {
                allRootsEmpty = false;
            }
        }
        if (allRootsEmpty) {
            status.addError("Resulting data construct cannot do any data transfer");
        }
    }
}
