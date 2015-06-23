package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class GenerateInitializer extends ForLoopChange {

    private String m_base;
    private IScope m_scope;
    private IASTName m_name;
    private IASTInitializerClause m_equalsInitializer;
    
    public GenerateInitializer(IASTForStatement loop, IScope scope) {
        super(loop);
        m_base = "";
        if (scope != null) {
            m_scope = scope;
        }
        m_name = null;
    }
    
    public GenerateInitializer(IASTForStatement loop, String base, IScope scope) {
        super(loop);
        if (base != null) {
            m_base = base;
        } else {
            m_base = "";
        }
        if (scope != null) {
            m_scope = scope;
        } else {
            throw new IllegalArgumentException("Scope cannot be null");
        }
        m_name = null;
    }
    
    public GenerateInitializer(IASTForStatement loop, String base, 
            IASTInitializerClause clause, IScope scope) {
        super(loop);
        if (base != null) {
            m_base = base;
        } else {
            m_base = "";
        }
        if (scope != null) {
            m_scope = scope;
        } else {
            throw new IllegalArgumentException("Scope cannot be null");
        }
        m_name = null;
        m_equalsInitializer = clause;
    }
    
    public GenerateInitializer(IASTForStatement loop, 
            IASTInitializerClause clause, IScope scope) {
        super(loop);
        m_base = "";
        if (scope != null) {
            m_scope = scope;
        } else {
            throw new IllegalArgumentException("Scope cannot be null");
        }
        m_name = null;
        m_equalsInitializer = clause;
    }

    @Override
    public IASTForStatement doChange(IASTForStatement loop) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        
        if (m_equalsInitializer == null) {
            m_equalsInitializer = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, "0");
        }
        
        IASTEqualsInitializer initializer = factory.newEqualsInitializer(m_equalsInitializer);
        IASTDeclarator declarator = factory.newDeclarator(generateName(loop));
        declarator.setInitializer(initializer);
        IASTSimpleDeclSpecifier declSpecifier = factory.newSimpleDeclSpecifier();
        declSpecifier.setType(IASTSimpleDeclSpecifier.t_int);
        
        IASTSimpleDeclaration declaration = factory.newSimpleDeclaration(declSpecifier);
        declaration.addDeclarator(declarator);
        loop.setInitializerStatement(factory.newDeclarationStatement(declaration));
        
        return loop;
    }
    
    public IASTName getGeneratedName() {
        return m_name;
    }
    
    private IASTName generateName(IASTForStatement loop) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        int diffcounter = 0;
        String gen_str = m_base+"_"+diffcounter;
        m_name = factory.newName(gen_str.toCharArray());
        while (ASTUtil.isNameInScope(m_name, m_scope)
                || ASTUtil.isNameInScope(m_name, loop.getScope())) {
            diffcounter++;
            gen_str = m_base+"_"+diffcounter;
            m_name = factory.newName(gen_str.toCharArray());
        }
        return m_name;
    }

}
