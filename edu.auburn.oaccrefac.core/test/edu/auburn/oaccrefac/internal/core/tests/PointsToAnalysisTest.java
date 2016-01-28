/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.core.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import edu.auburn.oaccrefac.core.dependence.PointsToAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Unit tests for {@link PointsToAnalysis}.
 * 
 * Test various cases that should or should not work for the PointsToAnalysis.
 */
public class PointsToAnalysisTest {
    
    @Test
    public void checkTwoPointers() throws CoreException {
        IASTTranslationUnit translationUnit = ASTUtil.translationUnitForString(
                "void foo(int* restrict e) {\n"
                        + "    int* a;\n"
                        + "    int* b;\n"
                        + "    int* const c;\n"
                        + "    int* d;\n"
                        + "}"
        );
        IASTFunctionDefinition function = ASTUtil.findOne(translationUnit, IASTFunctionDefinition.class);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        assertTrue(analysis.variablesMayPointToSame(
                findVariable(translationUnit, "a"), findVariable(translationUnit, "b")
        ));
        assertFalse(analysis.variablesMayPointToSame(
                findVariable(translationUnit, "a"), findVariable(translationUnit, "e")
        ));
    }
    
    /**
     * findVariable finds the IVariable in a given translationUnit give its name.
     * 
     * @param translationUnit Where to search for the variable at.
     * @param variableName The name of the variable to search for.
     * 
     * @return IVariable represented by the variable name.
     */
    private IVariable findVariable(IASTTranslationUnit translationUnit, final String variableName) {
        class NameVisitor extends ASTVisitor {
            private IVariable variable = null;
            public NameVisitor() {
                this.shouldVisitNames = true;
            }
            @Override
            public int visit(IASTName name) {
                IBinding binding = name.resolveBinding();
                if (binding instanceof IVariable && binding.getName().equals(variableName)) {
                    this.variable = (IVariable) binding;
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }
        }
        NameVisitor visitor = new NameVisitor();
        translationUnit.accept(visitor);
        return visitor.variable;
    }

}
