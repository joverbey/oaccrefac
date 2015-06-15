package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
public class LoopStripMiningRefactoring extends ForLoopRefactoring {

    private int m_stripFactor;
    private IASTExpression m_upperBoundExpression;
    
    public LoopStripMiningRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_stripFactor = -1;
        m_upperBoundExpression = null;
    }
    
    public boolean setStripFactor(int factor) {
        if (factor > 1) {
            m_stripFactor = factor;
            return true;
        }
        return false;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus initStatus) {
        if (m_stripFactor <= 1) {
            initStatus.addFatalError("Strip factor is invalid (<= 1)");
        }
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {        
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();

        //Get copy of original loop
        IASTForStatement loop = getLoop().copy();
 
        IASTForStatement originalLoop = getLoop();
        IASTExpression originalCondition = originalLoop.getConditionExpression();
        if (originalCondition instanceof IASTBinaryExpression) {
            m_upperBoundExpression = ((IASTBinaryExpression)originalCondition).getOperand2().copy();
            if (!checkBoundDivisibility()) {
                throw new OperationCanceledException("Known divisibility error!");
            }
        }
        
        IASTForStatement outerLoop = createOuterLoop(loop, factory);
        rewriter.replace(originalLoop, outerLoop, null);
    }
    
    /**
     * This method returns a boolean value depicting whether it is still
     * possible to continue refactoring based on the divisbility between the
     * original loop's incrementer and the strip factor. The factor must be
     * greater than, and divisible by the factor.
     * consequence of non-divisible factors.
     * @return -- true or false on whether refactoring may continue
     */
    private boolean checkBoundDivisibility() {
        IASTExpression iterator = getLoop().getIterationExpression();
        if (iterator instanceof IASTBinaryExpression) {
            IASTBinaryExpression bin = (IASTBinaryExpression) iterator;
            IASTExpression op2 = bin.getOperand2();
            if (op2 instanceof IASTLiteralExpression) {
                IASTLiteralExpression op2lit = (IASTLiteralExpression) op2;
                if (op2lit.getKind() == IASTLiteralExpression.lk_integer_constant) {
                    int value = Integer.parseInt(new String(op2lit.getValue()));
                    if (m_stripFactor % value != 0 || m_stripFactor <= value) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void modifyInnerLoop(IASTForStatement loop, IASTName outer_counter_name, ICNodeFactory factory) {
        IASTIdExpression outer_idexp = factory.newIdExpression(outer_counter_name);
        modifyInitializer(loop.getInitializerStatement(), outer_idexp);
        modifyCondition(loop, outer_idexp, factory);
    }

    private void modifyInitializer(IASTNode tree, IASTIdExpression to_set) {
        
        class findAndReplace extends ASTVisitor {
            IASTIdExpression m_replacement;
            public findAndReplace(IASTIdExpression replacement) {
                m_replacement = replacement;
                shouldVisitInitializers = true;
                shouldVisitExpressions = true;
            }
            
            @Override
            public int visit(IASTInitializer visitor) {
                if (visitor instanceof IASTEqualsInitializer) {
                    ((IASTEqualsInitializer) visitor).setInitializerClause(m_replacement);
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }
            
            @Override
            public int visit(IASTExpression visitor) {
                if (visitor instanceof IASTBinaryExpression) {
                    ((IASTBinaryExpression) visitor).setOperand2(m_replacement);
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }            
        }
        tree.accept(new findAndReplace(to_set));
    }
    
    private void modifyCondition(IASTForStatement loop, IASTIdExpression outer_idexp,
            ICNodeFactory factory) {
        IASTExpression conditionExpression = loop.getConditionExpression();
        
        IASTBinaryExpression upperbound_check = null;
        IASTBinaryExpression mine_check = null;
        if (conditionExpression instanceof IASTBinaryExpression) {
            upperbound_check = (IASTBinaryExpression)conditionExpression.copy();
            mine_check = (IASTBinaryExpression)conditionExpression.copy();
        }
        
        int adjustedFactor = m_stripFactor - 1;
        IASTLiteralExpression factorliteral = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, 
                adjustedFactor+"");
        IASTBinaryExpression plusfactor = factory.newBinaryExpression(
                IASTBinaryExpression.op_plus, 
                outer_idexp, 
                factorliteral);
        mine_check.setOperand2(plusfactor);

        upperbound_check.setOperator(IASTBinaryExpression.op_lessThan);
        upperbound_check.setOperand2(m_upperBoundExpression);
        
        IASTBinaryExpression logicand = factory.newBinaryExpression(
                IASTBinaryExpression.op_logicalAnd, 
                mine_check, 
                upperbound_check);
        
        IASTUnaryExpression parenth = factory.newUnaryExpression(
                IASTUnaryExpression.op_bracketedPrimary, 
                logicand);

        loop.setConditionExpression(parenth);
    }
    
//=======================================================================================
//      OUTER LOOP GENERATION METHODS    
//=======================================================================================

    private IASTForStatement createOuterLoop(IASTForStatement inner, 
            ICNodeFactory factory) {
        IASTName counter_name = findFirstName(getLoop().getInitializerStatement());
        IASTName outer_name = generateOuterInitializerName(counter_name, factory);
        
        modifyInnerLoop(inner, outer_name, factory);
        
        IASTCompoundStatement innercompound = factory.newCompoundStatement();
        innercompound.addStatement(inner);
        
        return factory.newForStatement(generateOuterInitializer(outer_name, factory), 
                generateOuterCondition(outer_name, factory), 
                generateOuterIteration(outer_name, factory), 
                innercompound);
    }
    
    private IASTName generateOuterInitializerName(IASTName loopcounter, ICNodeFactory factory) {
        IASTForStatement original_loop = getLoop();
        int diffcounter = 0;
        String loopcounter_str = new String(loopcounter.getSimpleID());
        String gen_str = loopcounter_str+"_"+diffcounter;
        IASTName gen = factory.newName(gen_str.toCharArray());
        while (isNameInScope(gen, original_loop.getScope())) {
            diffcounter++;
            gen_str = loopcounter_str+"_"+diffcounter;
            gen = factory.newName(gen_str.toCharArray());
        }
        return gen;
    }
    
    private IASTDeclarationStatement generateOuterInitializer(IASTName varname, ICNodeFactory factory) {
        
        IASTLiteralExpression zero = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, "0");
        IASTEqualsInitializer initializer = factory.newEqualsInitializer(zero);
        IASTDeclarator declarator = factory.newDeclarator(varname);
        declarator.setInitializer(initializer);
        IASTSimpleDeclSpecifier declSpecifier = factory.newSimpleDeclSpecifier();
        declSpecifier.setType(IASTSimpleDeclSpecifier.t_int);
        
        IASTSimpleDeclaration declaration = factory.newSimpleDeclaration(declSpecifier);
        declaration.addDeclarator(declarator);
        return factory.newDeclarationStatement(declaration);
    }
    
    private IASTExpression generateOuterCondition(IASTName countername, ICNodeFactory factory) {        
        return factory.newBinaryExpression(IASTBinaryExpression.op_lessThan, 
                factory.newIdExpression(countername), 
                m_upperBoundExpression);
    }

    private IASTExpression generateOuterIteration(IASTName countername, ICNodeFactory factory) {
        return factory.newBinaryExpression(IASTBinaryExpression.op_plusAssign, 
                factory.newIdExpression(countername), 
                factory.newLiteralExpression(
                        IASTLiteralExpression.lk_integer_constant, m_stripFactor+""));
    }
    
  //=======================================================================================

    
}
