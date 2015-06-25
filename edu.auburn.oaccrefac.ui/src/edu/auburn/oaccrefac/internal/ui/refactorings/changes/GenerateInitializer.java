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
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class GenerateInitializer extends ForLoopChange {

    private String m_base;
    private IScope m_scope;
    private IASTName m_name;
    private IASTInitializerClause m_equalsInitializer;

    public GenerateInitializer(IASTForStatement loop, IScope scope, 
            String base, IASTInitializerClause clause) {
        super(loop);
        m_base = base;
        m_scope = scope;
        m_equalsInitializer = clause;
        m_name = null;
    }

    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        
        if (m_scope == null) {
            init.addFatalError("Generate Initializer -- Error: input scope cannot"
                    + "be null!");
            return init;
        }
        
        //If the base string is null, generate a default one and warn client in status
        if (m_base == null) {
            init.addWarning("Generate Initializer -- Warning: Base string argument"
                    + "is null, generating default blank base string");
            m_base = "";
        }
        
        //If the equals initializer is null, we will just add
        //a default initializer, but warn client in status
        if (m_equalsInitializer == null) {
            init.addWarning("Generate Initializer -- Warning: Equals initializer"
                    + "null, generating default initializer of literal 0.");
            m_equalsInitializer = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, "0");
        }
        
        return init;
    }

    @Override
    public IASTForStatement doChange(IASTForStatement loop) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        
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
