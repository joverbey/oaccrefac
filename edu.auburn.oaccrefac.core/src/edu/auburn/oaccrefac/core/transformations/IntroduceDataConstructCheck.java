/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.core.transformations;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelNode;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.OpenACCUtil;

public class IntroduceDataConstructCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroduceDataConstructCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        super(statements, statementsAndComments);
    }

    @Override
    protected void doReachingDefinitionsCheck(RefactoringStatus status, ReachingDefinitions rd) {
        populateAccMap();
        //TODO i don't like calling inferCopy* both here and in the alteration, but i don't know what to do about it 
        //TODO handle case where construct being introduced inside other construct w/ copies
        /*
         * we check that: 
         * for every var that occurs in both the new and the internal construct's copyin set,
         *      the definitions reaching that var are the same for each construct
         * and similar for copyout sets
         */
        for(IASTStatement stmt : getAccRegions().keySet()) {
            for(IAccConstruct con : getAccRegions().get(stmt).values()) {
                if(con instanceof ASTAccParallelNode || con instanceof ASTAccKernelsNode) {
                    //check the copied definitions for this construct
                    if(!checkCopyins(getStatements(), stmt, con, rd) || !checkCopyouts(getStatements(), stmt, con, rd)) {
                        status.addWarning("Introducing a data construct here could change values copied to or from the accelerator.");
                    }
                }
            }
        }
    }

    private boolean checkCopyins(IASTStatement[] intro, IASTStatement par, IAccConstruct parCon, ReachingDefinitions rd) {
        /*
         * for a variable in both copyin sets, 
         *     the set of RD from outside the new construct must be the same as the set of RD
         *     from outside the par region
         */
        Set<IASTName> introRd = rd.reachingDefinitions(intro);
        Set<IASTName> parRd = rd.reachingDefinitions(par);
        for(String inferred : OpenACCUtil.inferCopyin(rd, intro)) {
            for(String current : OpenACCUtil.inferCopyin(rd, par)) {
                if(current.equals(inferred)) {
                    Set<IASTName> a = subsetWhereOutsideConstruct(subsetWhereNameEquals(introRd, current), intro);
                    Set<IASTName> b = subsetWhereOutsideConstruct(subsetWhereNameEquals(parRd, current), par);
                    if(!a.containsAll(b) || b.containsAll(a)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private boolean checkCopyouts(IASTStatement[] intro, IASTStatement par, IAccConstruct parCon, ReachingDefinitions rd) {
        /*
         * for a variable in both copy sets, 
         *     the set of RU outside the new construct must be the same as the set of RU
         *     outside the par region
         */
        Set<IASTName> introRd = rd.reachedUses(intro);
        Set<IASTName> parRd = rd.reachedUses(par);
        for(String inferred : OpenACCUtil.inferCopyout(rd, intro)) {
            for(String current : OpenACCUtil.inferCopyout(rd, par)) {
                if(current.equals(inferred)) {
                    Set<IASTName> a = subsetWhereOutsideConstruct(subsetWhereNameEquals(introRd, current), intro);
                    Set<IASTName> b = subsetWhereOutsideConstruct(subsetWhereNameEquals(parRd, current), par);
                    if(!a.containsAll(b) || b.containsAll(a)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private Set<IASTName> subsetWhereNameEquals(Set<IASTName> names, String toMatch) {
        Set<IASTName> subset = new HashSet<IASTName>();
        for(IASTName name : names) {
            if(name.getRawSignature().equals(toMatch)) {
                subset.add(name);
            }
        }
        return subset;
    }
    
    private Set<IASTName> subsetWhereOutsideConstruct(Set<IASTName> names, IASTStatement... construct) {
        Set<IASTName> subset = new HashSet<IASTName>();
        for(IASTName name : names) {
            if(!ASTUtil.inStatements(name, getStatements())) {
                subset.add(name);
            }
        }
        return subset;
    }

}

