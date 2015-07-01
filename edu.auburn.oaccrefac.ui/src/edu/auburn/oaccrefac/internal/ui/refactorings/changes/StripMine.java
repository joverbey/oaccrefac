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
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class StripMine extends ForLoopChange {
    
    private int m_stripFactor;
    private int m_depth;
    
    public StripMine(IASTForStatement loop, int stripFactor, int depth) {
        super(loop);
        m_stripFactor = stripFactor;
        m_depth = depth;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(getOriginal());
        
        if (m_stripFactor <= 0) {
            init.addFatalError("Invalid strip factor (<= 0).");
            return init;
        }
        
        if (m_depth < 0 || m_depth >= inq.getPerfectLoopNestHeaders().size()) {
            init.addFatalError("There is no for-loop at depth " + m_depth);
            return init;
        }
        
        int iterator = inq.getIterationFactor(m_depth);
        if (m_stripFactor % iterator != 0 || m_stripFactor <= iterator) {
            init.addFatalError("Strip mine factor must be greater than and "
                    + "divisible by the intended loop's iteration factor.");
            return init;
        }
        
        return init;
    }

    @Override
    public IASTForStatement doChange(IASTForStatement loop) {
        //Set up which loops we need to deal with
        IASTForStatement outer = ASTUtil.findDepth(loop, IASTForStatement.class, m_depth);
        IASTForStatement inner = outer.copy();
        
        //Get expressions that we will need...
        IASTExpression upperBound = getUpperBoundExpression(outer);
        IASTName counter_name = ASTUtil.findOne(outer.getInitializerStatement(), IASTName.class);  
        String counter_str = new String(counter_name.getSimpleID());
        
        //Generate initializer for outer loop
        GenerateInitializer gi = new GenerateInitializer(outer, 
                getOriginal().getScope(), counter_str, null);
        if (gi.checkConditions(new RefactoringStatus()).hasFatalError()) {
            throw new OperationCanceledException("Error handling generating initializer!");
        }
        outer = gi.change(this);
        
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        outer.setConditionExpression(factory.newBinaryExpression(IASTBinaryExpression.op_lessThan, 
                factory.newIdExpression(gi.getGeneratedName()), 
                upperBound));
        
        outer.setIterationExpression(factory.newBinaryExpression(IASTBinaryExpression.op_plusAssign, 
                factory.newIdExpression(gi.getGeneratedName()), 
                factory.newLiteralExpression(
                        IASTLiteralExpression.lk_integer_constant, m_stripFactor+"")));
        
        modifyInnerLoop(inner, gi.getGeneratedName(), upperBound, factory);
        IASTCompoundStatement innercompound = factory.newCompoundStatement();
        innercompound.addStatement(inner);
        outer.setBody(innercompound);
        return loop;
    }

    private IASTExpression getUpperBoundExpression(IASTForStatement loop) {
        IASTExpression ub = null;
        if (loop.getConditionExpression() instanceof IASTBinaryExpression) {
            IASTBinaryExpression cond_be = (IASTBinaryExpression) loop.getConditionExpression();
            ub = (IASTExpression)cond_be.getOperand2();
        } else {
            throw new UnsupportedOperationException("Non-binary conditional statements unsupported");
        }
        return ub;
    }


    private void modifyInnerLoop(IASTForStatement loop, IASTName outer_counter_name, 
            IASTExpression upperBound, ICNodeFactory factory) {
        IASTIdExpression outer_idexp = factory.newIdExpression(outer_counter_name);
        modifyInitializer(loop.getInitializerStatement(), outer_idexp);
        modifyCondition(loop, outer_idexp, upperBound, factory);
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
            IASTExpression upperBound, ICNodeFactory factory) {
        IASTExpression conditionExpression = loop.getConditionExpression();
        
        IASTBinaryExpression upperbound_check = null;
        IASTBinaryExpression mine_check = null;
        if (conditionExpression instanceof IASTBinaryExpression) {
            upperbound_check = (IASTBinaryExpression)conditionExpression.copy();
            mine_check = (IASTBinaryExpression)conditionExpression.copy();
        }
        
        IASTLiteralExpression factorliteral = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, 
                m_stripFactor+"");
        IASTBinaryExpression plusfactor = factory.newBinaryExpression(
                IASTBinaryExpression.op_plus, 
                outer_idexp, 
                factorliteral);
        mine_check.setOperand2(plusfactor);

        upperbound_check.setOperator(IASTBinaryExpression.op_lessThan);
        upperbound_check.setOperand2(upperBound);
        
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
