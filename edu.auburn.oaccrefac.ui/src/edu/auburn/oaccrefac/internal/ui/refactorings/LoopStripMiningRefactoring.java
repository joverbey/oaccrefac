package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;

@SuppressWarnings("restriction")
public class LoopStripMiningRefactoring extends ForLoopRefactoring {

    private int m_stripFactor;
    
    public LoopStripMiningRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_stripFactor = 1;
    }
    
    public boolean setStripFactor(int factor) {
        if (factor > 1) {
            m_stripFactor = factor;
            return true;
        }
        return false;
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        //Get original loop header information
        int upperBound = getUpperBoundValue();
        IASTForStatement loop = getLoop();
        //Get loop incrementer name
        IASTName counter_name = findFirstName(getLoop().getInitializerStatement());
        
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        //Create new incrementer name (and check for scope conflicts)
        String counter_name_str = new String(counter_name.getSimpleID());
        int diff_counter = 1;
        IASTName outerCounter = factory.newName((counter_name_str + "_" + diff_counter).toCharArray());
        try {
            while (isNameInScope(outerCounter, loop.getScope().getParent())) {
                diff_counter++;
                outerCounter = factory.newName((counter_name_str + "_" + diff_counter).toCharArray());
            }
        } catch (DOMException e) {
            e.printStackTrace();
        }
        
        IASTForStatement outerLoop = createOuterLoop(outerCounter, upperBound);
        readjustInnerLoop(loop, outerCounter, rewriter);
        IASTCompoundStatement innerCompound = factory.newCompoundStatement();
        //innerCompound.addStatement(innerLoop);
        outerLoop.setBody(innerCompound);
        rewriter.replace(loop, outerLoop, null);
    }
    
    public IASTForStatement createOuterLoop(IASTName loopCounterName, int upperBound) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        
        IASTLiteralExpression zero_literal = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, "0");
        IASTEqualsInitializer init_equals = factory.newEqualsInitializer(zero_literal);
        IASTDeclarator init_declarator = factory.newDeclarator(loopCounterName);
        init_declarator.setInitializer(init_equals);

        ICASTSimpleDeclSpecifier integer_specifier = factory.newSimpleDeclSpecifier();
        integer_specifier.setType(ICASTSimpleDeclSpecifier.t_int);
        
        IASTSimpleDeclaration init_declaration = factory.newSimpleDeclaration(integer_specifier);
        init_declaration.addDeclarator(init_declarator);
        IASTDeclarationStatement init = factory.newDeclarationStatement(init_declaration);
        
        
        int upper_strip = (upperBound / m_stripFactor);
        IASTLiteralExpression upper_literal = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, upper_strip+"");
        IASTIdExpression counter_id = factory.newIdExpression(loopCounterName);
        IASTBinaryExpression condition = factory.newBinaryExpression(
                IASTBinaryExpression.op_lessThan, 
                counter_id, 
                upper_literal);
        
        IASTLiteralExpression iter_literal = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, m_stripFactor+"");
        IASTBinaryExpression iterationExpression = factory.newBinaryExpression(
                IASTBinaryExpression.op_plusAssign, 
                counter_id, 
                iter_literal);
        
        IASTStatement body = factory.newNullStatement();
        
        IASTForStatement newLoop = factory.newForStatement(init, condition, iterationExpression, body);
        return newLoop;
    }
    
    public void readjustInnerLoop(IASTForStatement inner, IASTName outerCounterName, ASTRewrite rewriter) {
        
        
    }

}
