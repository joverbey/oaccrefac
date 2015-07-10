package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
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
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

public class TileLoops extends ForLoopChange {

    private int m_depth;
    private int m_stripFactor;
    private int m_propagate;
    
    private ForStatementInquisitor m_inq;
    private IASTName m_generatedName;
    
    public TileLoops(ASTRewrite rewriter, IASTForStatement loop, 
            int stripDepth, int stripFactor, int propagateInterchange) {
        super(rewriter, loop);
        m_depth = stripDepth;
        m_stripFactor = stripFactor;
        m_propagate = propagateInterchange;
    }

    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        //DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm);
        // if (dependenceAnalysis != null && dependenceAnalysis.()) {
        // status.addError("This loop cannot be parallelized because it carries a dependence.",
        // getLocation(getLoop()));
        // }
        m_inq = ForStatementInquisitor.getInquisitor(getLoopToChange());
        if (!m_inq.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be tiled.");
            return init;
        }
        
        if (m_propagate > m_depth) {
            init.addWarning("Warning: propagation higher than depth -- propagation "
                    + "will occur as many times as possible.");
            return init;
        }
        
        //TODO -- make this better (this stuff is from strip mining-specific code)
        if (m_stripFactor <= 0) {
            init.addFatalError("Invalid strip factor (<= 0).");
            return init;
        }
        
        if (m_depth < 0 || m_depth >= m_inq.getPerfectLoopNestHeaders().size()) {
            init.addFatalError("There is no for-loop at depth " + m_depth);
            return init;
        }
        
        int iterator = m_inq.getIterationFactor(m_depth);
        if (m_stripFactor % iterator != 0 || m_stripFactor <= iterator) {
            init.addFatalError("Strip mine factor must be greater than and "
                    + "divisible by the intended loop's iteration factor.");
            return init;
        }
        //TODO_end  ----------------------------------------------------------------
        
        return init;
    }
    
    @Override
    protected ASTRewrite doChange(ASTRewrite rewriter) {
        //Set up which loops we need to deal with
        IASTForStatement byStrip = m_inq.getPerfectLoopNestHeaders().get(m_depth);
        IASTForStatement inStrip = byStrip.copy();
        
        //Get expressions that we will need...
        this.modifyByStrip(rewriter, byStrip);
        
        IASTStatement body = byStrip.getBody();
        ASTRewrite inStrip_rewriter = null;
        if (body instanceof IASTCompoundStatement) {
            IASTNode chilluns[] = body.getChildren();
            for (IASTNode child : chilluns) {
                safeRemove(rewriter, child);
            }
            inStrip_rewriter = this.safeInsertBefore(rewriter, body, null, inStrip);
        } else {
            inStrip_rewriter = this.safeReplace(rewriter, byStrip.getBody(), inStrip);
        }
        
        this.modifyInStrip(inStrip_rewriter, inStrip);
        

        this.safeReplace(inStrip_rewriter, inStrip.getBody(), byStrip.getBody().copy());
        
        IASTForStatement interchange = m_inq.getPerfectLoopNestHeaders().get(m_depth-1);
        for (int i = 0; checkIteration(i); i++) {
            interchange = m_inq.getPerfectLoopNestHeaders().get(m_depth-i-1);
        }
        this.exchangeLoopHeaders(rewriter, byStrip, interchange);
        return rewriter;
    }
    
    private boolean checkIteration(int iteration) {
        if (m_propagate < 0) {
            return (m_depth-iteration > 0);
        } else {
            return (iteration < m_propagate && m_depth-iteration > 0);
        }
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
    
    private void modifyByStrip(ASTRewrite rewriter, IASTForStatement byStripHeader) {
        IASTExpression upperBound = getUpperBoundExpression(byStripHeader);
        this.genByStripInit(rewriter, byStripHeader);

        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTBinaryExpression newCond = factory.newBinaryExpression(
                IASTBinaryExpression.op_lessThan, 
                factory.newIdExpression(m_generatedName), 
                upperBound.copy());
        this.safeReplace(rewriter, byStripHeader.getConditionExpression(), newCond);
        
        IASTBinaryExpression newIter = factory.newBinaryExpression(
                IASTBinaryExpression.op_plusAssign, 
                factory.newIdExpression(m_generatedName), 
                factory.newLiteralExpression(
                        IASTLiteralExpression.lk_integer_constant, m_stripFactor+""));
        
        this.safeReplace(rewriter, byStripHeader.getIterationExpression(), newIter);
        
    }
    
    private void genByStripInit(ASTRewrite rewriter, IASTForStatement header) {
        IASTName counter_name = ASTUtil.findOne(header.getInitializerStatement(), IASTName.class);  
        String counter_str = new String(counter_name.getSimpleID());
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        int diffcounter = 0;
        String gen_str = counter_str+"_"+diffcounter;
        m_generatedName = factory.newName(gen_str.toCharArray());
        while (ASTUtil.isNameInScope(m_generatedName, header.getScope())) {
            diffcounter++;
            gen_str = counter_str+"_"+diffcounter;
            m_generatedName = factory.newName(gen_str.toCharArray());
        }
        IASTLiteralExpression zeroLit = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, "0");
        IASTEqualsInitializer initializer = factory.newEqualsInitializer(zeroLit);
        IASTDeclarator declarator = factory.newDeclarator(m_generatedName);
        declarator.setInitializer(initializer);
        IASTSimpleDeclSpecifier declSpecifier = factory.newSimpleDeclSpecifier();
        declSpecifier.setType(IASTSimpleDeclSpecifier.t_int);
        
        IASTSimpleDeclaration declaration = factory.newSimpleDeclaration(declSpecifier);
        declaration.addDeclarator(declarator);
        
        this.safeReplace(rewriter,
                header.getInitializerStatement(),
                factory.newDeclarationStatement(declaration));
    }


    private void modifyInStrip(ASTRewrite rewriter, IASTForStatement inStripHeader) {
        modifyInStripInit(rewriter, inStripHeader.getInitializerStatement());
        modifyInStripCondition(rewriter, inStripHeader);
    }

    private void modifyInStripInit(ASTRewrite rewriter, IASTNode tree) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTIdExpression byStripIdExp = factory.newIdExpression(m_generatedName);
        
        class findAndReplace extends ASTVisitor {
            IASTIdExpression m_replacement;
            ASTRewrite m_rewriter;
            public findAndReplace(ASTRewrite rewriter, IASTIdExpression replacement) {
                m_rewriter = rewriter;
                m_replacement = replacement;
                shouldVisitInitializers = true;
                shouldVisitExpressions = true;
            }
            
            @Override
            public int visit(IASTInitializer visitor) {
                if (visitor instanceof IASTEqualsInitializer) {
                    safeReplace(m_rewriter, 
                            ((IASTEqualsInitializer) visitor).getInitializerClause(),
                            m_replacement);
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }
            
            @Override
            public int visit(IASTExpression visitor) {
                if (visitor instanceof IASTBinaryExpression) {
                    safeReplace(m_rewriter, 
                            ((IASTBinaryExpression) visitor).getOperand2(), 
                            m_replacement);
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }            
        }
        tree.accept(new findAndReplace(rewriter, byStripIdExp));
    }
    
    private void modifyInStripCondition(ASTRewrite rewriter, IASTForStatement inStripHeader) {
        IASTExpression condition = inStripHeader.getConditionExpression();
        
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTIdExpression byStripIdExp = factory.newIdExpression(m_generatedName);
        
        IASTBinaryExpression mine_check = null;
        if (condition instanceof IASTBinaryExpression) {
            mine_check = (IASTBinaryExpression)condition.copy();
        } else {
            throw new UnsupportedOperationException("Unsupported non-binary condition exprn");
        }
        
        IASTLiteralExpression factorliteral = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, 
                m_stripFactor+"");
        IASTBinaryExpression plusfactor = factory.newBinaryExpression(
                IASTBinaryExpression.op_plus, 
                byStripIdExp, 
                factorliteral);
        mine_check.setOperand2(plusfactor);
        
        IASTBinaryExpression logicand = factory.newBinaryExpression(
                IASTBinaryExpression.op_logicalAnd, 
                mine_check, 
                condition.copy());
        
        IASTUnaryExpression parenth = factory.newUnaryExpression(
                IASTUnaryExpression.op_bracketedPrimary, 
                logicand);

        this.safeReplace(rewriter, inStripHeader.getConditionExpression(), parenth);
    }

    

}
