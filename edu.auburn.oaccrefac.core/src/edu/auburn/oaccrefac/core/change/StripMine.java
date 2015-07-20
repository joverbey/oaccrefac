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
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class StripMine extends ForLoopChange {
    
    private int m_stripFactor;
    private int m_depth;
    
    private IASTName m_generatedName;
    
    public StripMine(IASTRewrite rewriter, 
            IASTForStatement loop, int stripFactor, int depth) {
        super(rewriter, loop);
        m_stripFactor = stripFactor;
        m_depth = depth;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.getLoopToChange());
        
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
    public IASTRewrite doChange(IASTRewrite rewriter) {
        //Set up which loops we need to deal with
        IASTForStatement byStrip = this.getLoopToChange();
        IASTForStatement inStrip = byStrip.copy();
        
        //Get expressions that we will need...
        this.modifyByStrip(rewriter, byStrip);
        
        IASTStatement body = byStrip.getBody();
        IASTRewrite inStrip_rewriter = null;
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
        return rewriter;
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
    
    private void modifyByStrip(IASTRewrite rewriter, IASTForStatement byStripHeader) {
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
    
    private void genByStripInit(IASTRewrite rewriter, IASTForStatement header) {
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
        
        IASTSimpleDeclSpecifier declSpecifier = factory.newSimpleDeclSpecifier();
        declSpecifier.setType(IASTSimpleDeclSpecifier.t_int);
        IASTSimpleDeclaration declaration = factory.newSimpleDeclaration(declSpecifier);

        IASTInitializer initializer = ASTUtil
                .findOne(header, IASTEqualsInitializer.class);
        if (initializer != null) {
            initializer = initializer.copy();
            IASTDeclarator declarator = factory.newDeclarator(m_generatedName);
            declarator.setInitializer(initializer);
            declaration.addDeclarator(declarator);
        }
        
        this.safeReplace(rewriter,
                header.getInitializerStatement(),
                factory.newDeclarationStatement(declaration));
    }


    private void modifyInStrip(IASTRewrite rewriter, IASTForStatement inStripHeader) {
        modifyInStripInit(rewriter, inStripHeader.getInitializerStatement());
        modifyInStripCondition(rewriter, inStripHeader);
    }

    private void modifyInStripInit(IASTRewrite rewriter, IASTNode tree) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTIdExpression byStripIdExp = factory.newIdExpression(m_generatedName);
        
        class findAndReplace extends ASTVisitor {
            IASTIdExpression m_replacement;
            IASTRewrite m_rewriter;
            public findAndReplace(IASTRewrite rewriter, IASTIdExpression replacement) {
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
    
    private void modifyInStripCondition(IASTRewrite rewriter, IASTForStatement inStripHeader) {
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
