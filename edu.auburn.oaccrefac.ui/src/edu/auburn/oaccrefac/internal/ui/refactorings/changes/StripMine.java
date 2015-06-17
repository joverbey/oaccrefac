package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class StripMine extends ForLoopChange {
    
    private int m_stripFactor;
    private IScope m_scope;
    private IASTExpression m_upperBoundExpression;
    
    public StripMine(IASTForStatement loop, int stripFactor, 
            IASTExpression upperBound, IScope scope) {
        super(loop);
        m_stripFactor = stripFactor;
        m_scope = scope;
        m_upperBoundExpression = upperBound;
    }

    @Override
    public IASTForStatement doChange(IASTForStatement loop) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTForStatement inner = loop.copy();

        IASTName counter_name = ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class);  
        String counter_str = new String(counter_name.getSimpleID());
        GenerateInitializer gi = new GenerateInitializer(loop, 
                counter_str, m_scope);
        gi.change();
        
        loop.setConditionExpression(factory.newBinaryExpression(IASTBinaryExpression.op_lessThan, 
                factory.newIdExpression(gi.getGeneratedName()), 
                m_upperBoundExpression));
        
        loop.setIterationExpression(factory.newBinaryExpression(IASTBinaryExpression.op_plusAssign, 
                factory.newIdExpression(gi.getGeneratedName()), 
                factory.newLiteralExpression(
                        IASTLiteralExpression.lk_integer_constant, m_stripFactor+"")));
        
        modifyInnerLoop(inner, gi.getGeneratedName(), factory);
        IASTCompoundStatement innercompound = factory.newCompoundStatement();
        innercompound.addStatement(inner);
        loop.setBody(innercompound);
        return loop;
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

    
}
