package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop strip mine refactoring algorithm. Loop strip mining
 * takes a sequential loop and essentially creates 'strips' through perfectly nesting a by-strip loop and an in-strip
 * loop.
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     // do something
 * }
 * </pre>
 * 
 * Refactors to: The outer is the by-strip, inner is the in-strip...
 * 
 * <pre>
 * for (int i_0 = 0; i_0 < 10; i_0 += 2) {
 *     for (int i = i_0; (i < i_0 + 2 && i < 10); i++) {
 *         // do something...
 *     }
 * }
 * </pre>
 * 
 * @author Adam Eichelkraut
 */
public class StripMineAlteration extends ForLoopAlteration<StripMineCheck> {

    private int stripFactor;
    private int depth;
    // We use this in multiple methods... couldn't think
    // of a better way of not making it a member variable...
    private IASTName generatedName;

    /**
     * Constructor. Takes parameters for strip factor and strip depth to tell the refactoring which perfectly nested
     * loop to strip mine.
     * 
     * @author Adam Eichelkraut
     * @param rewriter
     *            -- rewriter associated with the for loop
     * @param loop
     *            -- for loop to refactor
     * @param stripFactor
     *            -- factor for how large strips are
     * @param depth
     *            -- perfectly nested loop depth in 'loop' to strip mine
     */
    public StripMineAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, int stripFactor, int depth, StripMineCheck check) {
        super(tu, rewriter, loop, check);
        this.stripFactor = stripFactor;
        this.depth = depth;
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.getLoopToChange());

        // Check strip factor validity...
        if (stripFactor <= 0) {
            init.addFatalError("Invalid strip factor (<= 0).");
            return;
        }

        // Check depth validity...
        if (depth < 0 || depth >= inq.getPerfectLoopNestHeaders().size()) {
            init.addFatalError("There is no for-loop at depth " + depth);
            return;
        }

        // If the strip factor is not divisible by the original linear
        // iteration factor, (i.e. loop counts by 4), then we cannot
        // strip mine because the refactoring will change behavior
        int iterator = inq.getIterationFactor(depth);
        if (stripFactor % iterator != 0 || stripFactor <= iterator) {
            init.addFatalError("Strip mine factor must be greater than and "
                    + "divisible by the intended loop's iteration factor.");
            return;
        }

        return;
    }

    @Override
    public void doChange() {
//        //Set up which loops we need to deal with
//        IASTForStatement byStrip = this.getLoopToChange();
//        IASTForStatement inStrip = byStrip.copy();
//        
//        //The first loop (original) will be the by-strip
//        //loop, modify this loop first.
//        this.modifyByStrip(rewriter, byStrip);
//        
//        IASTStatement body = byStrip.getBody();
//        IASTRewrite inStrip_rewriter = null;
//        if (body instanceof IASTCompoundStatement) {
//            IASTNode chilluns[] = body.getChildren();
//            for (IASTNode child : chilluns) {
//                safeRemove(rewriter, child);
//            }
//            inStrip_rewriter = this.safeInsertBefore(rewriter, body, null, inStrip);
//        } else {
//            inStrip_rewriter = this.safeReplace(rewriter, byStrip.getBody(), inStrip);
//        }
//        
//        this.modifyInStrip(inStrip_rewriter, inStrip);
//        
//
//        this.safeReplace(inStrip_rewriter, inStrip.getBody(), byStrip.getBody().copy());
//        return rewriter;
//    }
//    
//    /**
//     * Takes a rewriter and modifies the outer loop's expressions in order to
//     * iterate in strips rather than sequentially.
//     * @author Adam Eichelkraut
//     * @param rewriter -- rewriter associated with strip header
//     * @param byStripHeader -- for statement to be the by-strip header
//     */
//    private void modifyByStrip(IASTRewrite rewriter, IASTForStatement byStripHeader) {
//        IASTExpression upperBound = this.getUpperBoundExpression(byStripHeader);
//        //Generate initializer statement
//        this.genByStripInit(rewriter, byStripHeader);
//        
//        //Generate condition expression
//        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//        IASTBinaryExpression newCond = factory.newBinaryExpression(
//                IASTBinaryExpression.op_lessThan, 
//                factory.newIdExpression(m_generatedName), 
//                upperBound.copy());
//        this.safeReplace(rewriter, byStripHeader.getConditionExpression(), newCond);
//        
//        //Generate iteration statement
//        IASTBinaryExpression newIter = factory.newBinaryExpression(
//                IASTBinaryExpression.op_plusAssign, 
//                factory.newIdExpression(m_generatedName), 
//                factory.newLiteralExpression(
//                        IASTLiteralExpression.lk_integer_constant, m_stripFactor+""));
//        this.safeReplace(rewriter, byStripHeader.getIterationExpression(), newIter);
        
    }
    
//    /**
//     * Generates the initializer statement for the by-strip loop. Basically, it
//     * takes the original loop variable name and changes it in order for it to 
//     * be unique to the scope
//     * @author Adam Eichelkraut
//     * @param rewriter -- rewriter associated with the header
//     * @param header -- for loop header
//     */
//    private void genByStripInit(IASTRewrite rewriter, IASTForStatement header) {
//        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//        IASTName counter_name = ASTUtil.findOne(header.getInitializerStatement(), IASTName.class);  
//        //Generate new name from old counter name...
//        m_generatedName = generateNewName(counter_name, header.getScope());
//        
//        //Get the initializer expression if there is one
//        IASTStatement headerInitializer = header.getInitializerStatement();
//        IASTInitializer right_equals = ASTUtil
//                .findOne(headerInitializer, IASTEqualsInitializer.class);
//        if (right_equals != null) {
//            right_equals = right_equals.copy();
//        } else if (headerInitializer instanceof IASTExpressionStatement) {
//            //Sometimes the loop header isn't a declaration, but instead
//            //a simple binary expression such as for(i = 0; ...)
//            IASTExpressionStatement exprSt = 
//                    (IASTExpressionStatement) headerInitializer;
//            IASTExpression expr = exprSt.getExpression();
//            if (expr instanceof IASTBinaryExpression) {
//                IASTExpression op2 = ((IASTBinaryExpression) expr).getOperand2();
//                right_equals = factory.newEqualsInitializer(op2.copy());
//            } else {
//                throw new UnsupportedOperationException("Loop initialization "
//                        + "expression is unsupported!");
//            }
//        }
//        //Create the replacement, and replace
//        IASTDeclarationStatement replacement = this.generateVariableDecl(
//                m_generatedName, 
//                IASTSimpleDeclSpecifier.t_int, 
//                right_equals);
//        this.safeReplace(rewriter, headerInitializer, replacement);
//    }

//    /**
//     * Modifies the in-strip loop header in order to accommodate the newly-formed
//     * by-strip loop header. It needs to modify the initializer statement to replace
//     * the right hand side of the equals to the new by-strip counter. It also needs
//     * to modify the condition to work with the by-strip loop too.
//     * @author Adam Eichelkraut
//     * @param rewriter
//     * @param inStripHeader
//     */
//    private void modifyInStrip(IASTRewrite rewriter, IASTForStatement inStripHeader) {
//        modifyInStripInit(rewriter, inStripHeader.getInitializerStatement());
//        modifyInStripCondition(rewriter, inStripHeader);
//    }

//    /**
//     * Modifies the in-strip initializer statement by replacing all of the
//     * right hand side of initializers and binary expressions to the new
//     * by-strip id expression.
//     * @author Adam Eichelkraut
//     * @param rewriter -- rewriter associated with the tree
//     * @param tree -- node which is the in-strip initializer
//     */
//    private void modifyInStripInit(IASTRewrite rewriter, IASTNode tree) {
//        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//        IASTIdExpression byStripIdExp = factory.newIdExpression(m_generatedName);
//        
//        class findAndReplace extends ASTVisitor {
//            IASTIdExpression m_replacement;
//            IASTRewrite m_rewriter;
//            public findAndReplace(IASTRewrite rewriter, IASTIdExpression replacement) {
//                m_rewriter = rewriter;
//                m_replacement = replacement;
//                shouldVisitInitializers = true;
//                shouldVisitExpressions = true;
//            }
//            
//            @Override
//            public int visit(IASTInitializer visitor) {
//                if (visitor instanceof IASTEqualsInitializer) {
//                    safeReplace(m_rewriter, 
//                            ((IASTEqualsInitializer) visitor).getInitializerClause(),
//                            m_replacement);
//                    return PROCESS_ABORT;
//                }
//                return PROCESS_CONTINUE;
//            }
//            
//            @Override
//            public int visit(IASTExpression visitor) {
//                if (visitor instanceof IASTBinaryExpression) {
//                    safeReplace(m_rewriter, 
//                            ((IASTBinaryExpression) visitor).getOperand2(), 
//                            m_replacement);
//                    return PROCESS_ABORT;
//                }
//                return PROCESS_CONTINUE;
//            }            
//        }
//        tree.accept(new findAndReplace(rewriter, byStripIdExp));
//    }
    
//    /**
//     * Modifies the in-strip condition expression in order to check that the
//     * in-strip counter is less than the (by-strip counter + strip factor) and
//     * the upper bound expression
//     * @author Adam Eichelkraut
//     * @param rewriter -- rewriter associated with in-strip header
//     * @param inStripHeader -- in-strip header
//     * @throws UnsupportedOperationException  if condition is not a binary expression
//     */
//    private void modifyInStripCondition(IASTRewrite rewriter, IASTForStatement inStripHeader) {
//        IASTExpression condition = inStripHeader.getConditionExpression();
//        
//        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//        IASTIdExpression byStripIdExp = factory.newIdExpression(m_generatedName);
//        
//        IASTBinaryExpression mine_check = null;
//        if (condition instanceof IASTBinaryExpression) {
//            mine_check = (IASTBinaryExpression)condition.copy();
//        } else {
//            throw new UnsupportedOperationException("Unsupported non-binary condition exprn");
//        }
//        
//        IASTLiteralExpression factorliteral = factory.newLiteralExpression(
//                IASTLiteralExpression.lk_integer_constant, 
//                m_stripFactor+"");
//        IASTBinaryExpression plusfactor = factory.newBinaryExpression(
//                IASTBinaryExpression.op_plus, 
//                byStripIdExp, 
//                factorliteral);
//        mine_check.setOperand2(plusfactor);
//        
//        IASTBinaryExpression logicand = factory.newBinaryExpression(
//                IASTBinaryExpression.op_logicalAnd, 
//                mine_check, 
//                condition.copy());
//        
//        IASTUnaryExpression parenth = factory.newUnaryExpression(
//                IASTUnaryExpression.op_bracketedPrimary, 
//                logicand);
//
//        this.safeReplace(rewriter, inStripHeader.getConditionExpression(), parenth);
//    }

    
}
