package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop tiling refactoring algorithm. Loop tiling takes a
 * perfectly nested loop and 'tiles' the loop nest by performing loop strip mining on a specified loop and afterwards
 * interchanging the by-strip loop header as many times as possible.
 * 
 * For example,
 * 
 * <pre>
 * for (int j = 0; j < 20; j++) {
 *     for (int i = 0; i < 10; i++) {
 *         // do something
 *     }
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int i_0 = 0; i_0 < 10; i_0 += 2) {
 *     for (int j = 0; j < 20; j++) {
 *         for (int i = i_0; (i < i_0 + 2 && i < 10); i++) {
 *             // do something...
 *         }
 *     }
 * }
 * </pre>
 * 
 * @author Adam Eichelkraut
 *
 */
public class TileLoopsAlteration extends ForLoopAlteration<TileLoopsCheck> {

    private int depth;
    private int stripFactor;
    private int propagate;

    private ForStatementInquisitor inq;
    private IASTName generatedName;

    /**
     * Constructor. Takes strip depth, strip factor, and how many times to propagate interchange for tiling. Loop nest
     * must be perfectly nested.
     * 
     * @param rewriter
     *            -- rewriter associated with loop argument
     * @param loop
     *            -- for loop in which to tile
     * @param stripDepth
     *            -- strip depth of perfectly nested loop headers
     * @param stripFactor
     *            -- strip factor in which to strip mine header at depth
     * @param propagateInterchange
     *            -- how many times to interchange headers (-1 for arbitrary)
     */
    public TileLoopsAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, int stripDepth,
            int stripFactor, int propagateInterchange, TileLoopsCheck check) {
        super(tu, rewriter, loop, check);
        this.depth = stripDepth;
        this.stripFactor = stripFactor;
        this.propagate = propagateInterchange;
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        //TODO dependence analysis??? how to do i dunno
        
        //DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm);
        // if (dependenceAnalysis != null && dependenceAnalysis.()) {
        // status.addError("This loop cannot be parallelized because it carries a dependence.",
        // getLocation(getLoop()));
        // }
        inq = ForStatementInquisitor.getInquisitor(getLoopToChange());
        if (!inq.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be tiled.");
            return;
        }

        if (propagate > depth) {
            init.addWarning(
                    "Warning: propagation higher than depth -- propagation " + "will occur as many times as possible.");
            return;
        }

        // TODO -- make this better (this stuff is from strip mining-specific code)
        if (stripFactor <= 0) {
            init.addFatalError("Invalid strip factor (<= 0).");
            return;
        }

        if (depth < 0 || depth >= inq.getPerfectLoopNestHeaders().size()) {
            init.addFatalError("There is no for-loop at depth " + depth);
            return;
        }

        int iterator = inq.getIterationFactor(depth);
        if (stripFactor % iterator != 0 || stripFactor <= iterator) {
            init.addFatalError("Strip mine factor must be greater than and "
                    + "divisible by the intended loop's iteration factor.");
            return;
        }
        // TODO_end ----------------------------------------------------------------

        return;
    }

    @Override
    protected void doChange() {
//        //Set up which loops we need to deal with
//        IASTForStatement byStrip = m_inq.getPerfectLoopNestHeaders().get(m_depth);
//        IASTForStatement inStrip = byStrip.copy();
//        
//        //Modify by-strip loop
//        this.modifyByStrip(rewriter, byStrip);
//        
//        //Modify in-strip loop
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
//        this.modifyInStrip(inStrip_rewriter, inStrip);
//        this.safeReplace(inStrip_rewriter, inStrip.getBody(), byStrip.getBody().copy());
//        
//        //Begin interchanging loop headers for tiling-effect
//        IASTForStatement interchange = m_inq.getPerfectLoopNestHeaders().get(m_depth-1);
//        for (int i = 0; checkIteration(i); i++) {
//            interchange = m_inq.getPerfectLoopNestHeaders().get(m_depth-i-1);
//        }
//        this.exchangeLoopHeaders(rewriter, byStrip, interchange);
//        return rewriter;
    }
    
//    /**
//     * Helper method to check the iteration based on set parameters. If the
//     * propagation factor is unspecified, then do something else.
//     * @author Adam Eichelkraut
//     * @param iteration -- for loop iteration
//     * @return -- T/F whether to continue interchanging headers
//     */
//    private boolean checkIteration(int iteration) {
//        if (m_propagate < 0) {
//            return (m_depth-iteration > 0);
//        } else {
//            return (iteration < m_propagate && m_depth-iteration > 0);
//        }
//    }
//    
//    //TODO -- make this better (this stuff is from strip mining-specific code)
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
//        
//    }
    
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
//
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
//
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
    //--------------------------------------------------------------------------------------
}
