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
import static org.junit.Assert.assertNotNull;
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

// int base = 1;                                                            
// int a = 1;                                                               
// int b = 1;                                                               
// int c = 1;                                                               
// int* first_ptr = &base;                                                  
// int* aliased_ptr = &*((a + b) + (first_ptr++ + b) + c + b++);    
//
// What if a pointer is simply assigned to another? Can't that also be   
// aliased?                                                              
//                                                                       
// So in the below case, z is aliased?                                   
//                                                                       
// To check this, you could also just see if pointer type is assigned.   
//
// int* z;                                                                  
// int* zz = z;                                                             
// zz = &a; 

/**
 * Unit tests for {@link PointsToAnalysis}.
 * 
 * Test various cases that should or should not work for the PointsToAnalysis.
 */
public class PointsToAnalysisTest {
    
    /**
     * Checks that exception is thrown when variable doesn't exist.
     * 
     * @throws CoreException if it can't make a translation unit out of the function.
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkVariableDoesntExist() throws CoreException {
        IASTTranslationUnit func1 = ASTUtil.translationUnitForString("void foo() { int a; }");
        assertNotNull(func1);
        IASTTranslationUnit func2 = ASTUtil.translationUnitForString("void foo() { }");
        assertNotNull(func2);
        IVariable a = findVariable(func1, "a");
        assertNotNull(a);
        PointsToAnalysis analysis = new PointsToAnalysis(ASTUtil.findOne(func2, IASTFunctionDefinition.class), null);
        analysis.mayBeAliased(a); 
    }
      
    @Test
    public void checkComplexExpression() {
        // Finish this with comment above.
    }
    
    @Test
    public void checkNoAmpersandExpression() {
        // Finish this with comment above.
    }
    
    /**
     * Checks simple address-of.
     * 
     * @throws CoreException
     */
    @Test
    public void checkSimpleAddressOf() throws CoreException {
        abTest("int a = 1;", "int* b = &a");
    }
    
    /**
     * Checks nested address-of.
     * 
     * @throws CoreException
     */
    @Test
    public void checkNestedAddressOf() throws CoreException {
        abTest("int a = 1;", "int* b = (((&(((a))))));");
    }
    
    /**
     * Checks operators applied to pointer type.
     * 
     * @throws CoreException
     */
    @Test
    public void checkOperatorAddressOf() throws CoreException {
        abTest("int a = 1", "int* b = &*++a;");
    }
    
    /**
     * Checks address-of dereference chains.
     * 
     * @throws CoreException
     */
    @Test
    public void checkAddressOfDereference() throws CoreException {
        abTest("int* a;", "int* b = &*a;");
    }
    
    /**
     * Checks structs.
     * 
     * @throws CoreException
     */
    @Test
    public void checkStructAddressOf() throws CoreException {
        abTest("typedef struct {", "    int x;", "} A;", "A a;", "A* b = &a;");
    }
    
    /**
     * Checks struct members.
     * 
     * @throws CoreException
     */
    @Test
    public void checkStructMemberAddressOf() throws CoreException {
        abTest("typedef struct {", "    int x;", "} A;", "A a;", "A* b = &a.x;");
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
    
    /**
     * abTest checks that the variable a may be aliased and the variable b may not.
     * 
     * @param functionBody with two local variables, a and b, where a may be aliased and b may not be.
     * functionBody only contains body of a function without newlines where each element is its own line.
     * 
     * @throws CoreException if function can't be converted to a translation unit.
     */
    private void abTest(String... functionBody) throws CoreException {
        String function = "void foo() {\n";
        for (String line : functionBody) {
            function += "    " + line + "\n";
        }
        function += "}";
        IASTTranslationUnit func = ASTUtil.translationUnitForString(function);
        assertNotNull(func);
        PointsToAnalysis analysis = new PointsToAnalysis(ASTUtil.findOne(func, IASTFunctionDefinition.class), null);
        IVariable a = findVariable(func, "a");
        assertNotNull(a);
        assertTrue(analysis.mayBeAliased(a));
        IVariable b = findVariable(func, "b");
        assertNotNull(b);
        assertFalse(analysis.mayBeAliased(b));
    }

}
