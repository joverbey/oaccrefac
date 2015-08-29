package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop unrolling refactoring algorithm. Loop unrolling
 * takes a sequential loop and 'unrolls' the loop by copying the body of the loop multiple times in order to skip
 * testing the conditional expression more times than it has to.
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 19; i++) {
 *     a = 5;
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int i = 0; i < 18; i += 2) {
 *     a = 5;
 *     i++;
 *     a = 5;
 * }
 * i++;
 * a = 5;
 * i++;
 * </pre>
 * 
 * The part at the end is called the 'trailer' and it holds the leftover iterations that could not be satisfied by the
 * unrolled loop body.
 * 
 * @author Adam Eichelkraut
 *
 */
public class UnrollLoopAlteration extends ForLoopAlteration {

    private int unrollFactor;
    private Long upperBound;

    /**
     * Constructor.
     * 
     * @param rewriter
     *            -- rewriter associated with loop
     * @param loop
     *            -- loop in which to unroll
     * @param unrollFactor
     *            -- how many times to unroll loop body (must be > 0)
     */
    public UnrollLoopAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, int unrollFactor) {
        super(tu, rewriter, loop);
        this.unrollFactor = unrollFactor;
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        // Check unroll factor validity...
        if (unrollFactor <= 0) {
            init.addFatalError("Invalid loop unroll factor! (<= 0)");
            return;
        }

        // If the upper bound is not a constant, we cannot do loop unrolling
        IASTForStatement loop = getLoopToChange();
        IASTFunctionDefinition enclosing = ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
        ConstantPropagation constantprop_ub = new ConstantPropagation(enclosing);
        IASTExpression ub_expr = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2();
        upperBound = constantprop_ub.evaluate(ub_expr);
        if (upperBound == null) {
            init.addFatalError("Upper bound is not a constant value. Cannot perform unrolling!");
            return;
        }

        IASTStatement body = loop.getBody();
        // if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement) {
            init.addFatalError("Loop body is empty -- nothing to unroll!");
            return;
        }
        return;
    }

    @Override
    protected void doChange() {
        IASTForStatement loop = this.getLoopToChange();

        // TODO move this code somewhere else to use refactoring status
        // If the upper bound is not a constant, we cannot do loop unrolling
        IASTFunctionDefinition enclosing = ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
        ConstantPropagation constantprop_ub = new ConstantPropagation(enclosing);
        IASTExpression ub_expr = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2();
        upperBound = constantprop_ub.evaluate(ub_expr);
        if (upperBound == null) {
            return;
        }

        // get the loop upper bound from AST, if the conditional statement is '<=',
        // change it and adjust the bound to include a +1.
        int upper = upperBound.intValue();
        int condOffset = 0;
        IASTBinaryExpression condition = (IASTBinaryExpression) loop.getConditionExpression();
        if (condition.getOperator() == IASTBinaryExpression.op_lessEqual) {
            upper = upper + 1;
            condOffset = condOffset + 1;
        }

        // Number of extra iterations to add after loop based on divisibility.
        int extras = upper % unrollFactor;
        if (extras != 0) {
            condOffset = condOffset - extras;
        }

        try {

            this.insert(
                    loop.getBody().getFileLocation().getNodeOffset() + loop.getBody().getFileLocation().getNodeLength(),
                    getTrailer(loop, extras));
            this.replace(loop, forLoop(getInitializer(loop), getCondition(loop, condOffset),
                    getIterationExpression(loop), getBody(loop)));
            
            List<IASTPreprocessorPragmaStatement> prags = 
                    InquisitorFactory.getInquisitor(loop).getLeadingPragmas();
            int insertPoint = prags.size() > 0?
                    prags.get(0).getFileLocation().getNodeOffset() :
                        loop.getFileLocation().getNodeOffset();
                            
                    
            this.insert(insertPoint, getLeadingDeclaration(loop));

        } catch (DOMException e) {
            e.printStackTrace();
            return;
        }

        finalizeChanges();
    }

    private String getTrailer(IASTForStatement loop, int extras) throws DOMException {
        StringBuilder trailer = new StringBuilder();
        for (int i = 0; i < extras; i++) {
            IASTNode[] bodyObjects = getBodyObjects(loop);
            trailer.append(newName(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString(), loop.getScope().getParent()) + "++;"
                    + System.lineSeparator());
            for (IASTNode bodyObject : bodyObjects) {
                if(bodyObject instanceof IASTStatement) {
                    trailer.append(getModifiedStatement((IASTStatement) bodyObject, getUses(getNameFromInitializer(loop), loop), 
                            newName(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString(), loop.getScope().getParent())));
                }
                else {
                    trailer.append(bodyObject.getRawSignature() + System.lineSeparator());
                }
            }
        }
        return trailer.toString();
    }

    private String getBody(IASTForStatement loop) throws DOMException {
        
        StringBuilder body = new StringBuilder("");
        for (int i = 0; i < unrollFactor; i++) {
            for (IASTNode bodyObject : getBodyObjects(loop)) {
                if(bodyObject instanceof IASTStatement) {
                    body.append(getModifiedStatement((IASTStatement) bodyObject, getUses(getNameFromInitializer(loop), loop), 
                            newName(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString(), loop.getScope().getParent())));
                }
                else {
                    body.append(bodyObject.getRawSignature() + System.lineSeparator());
                }
            }
            if (i != unrollFactor - 1) {
                body.append(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString() + "++;"
                        + System.lineSeparator());
            }
        }
        return compound(body.toString());
    }

    private String getLeadingDeclaration(IASTForStatement loop) throws DOMException {
        // if the init statement is a declaration, move the declaration
        // to outer scope if possible and continue
        if (loop.getInitializerStatement() instanceof IASTDeclarationStatement) {
            IASTName varname = ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class);
            String declaration;
            StringBuilder newDeclaration = new StringBuilder(loop.getInitializerStatement().getRawSignature());
            int start = varname.getFileLocation().getNodeOffset()
                    - loop.getInitializerStatement().getFileLocation().getNodeOffset();
            int end = start + varname.getFileLocation().getNodeLength();
            newDeclaration.replace(start, end, newName(varname.toString(), loop.getScope().getParent()));
            declaration = newDeclaration.toString();
            return declaration;
        } else {
            return "";
        }
    }

    private String getInitializer(IASTForStatement loop) {
        return loop.getInitializerStatement() instanceof IASTDeclarationStatement ? "; "
                : loop.getInitializerStatement().getRawSignature();
    }

    private String getCondition(IASTForStatement loop, int condOffset) throws DOMException {
        StringBuilder cond = new StringBuilder();
        cond.append(newName(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString(),
                loop.getScope().getParent()));
        cond.append(" < ");
        cond.append(parenth(((IASTBinaryExpression) loop.getConditionExpression()).getOperand2().getRawSignature()
                + " + " + condOffset));
        return cond.toString();
    }

    private String getIterationExpression(IASTForStatement loop) throws DOMException {
        return newName(ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class).toString(),
                loop.getScope().getParent()) + " += " + unrollFactor;
    }

    private String newName(String name, IScope scope) {
        if (ASTUtil.isNameInScope(name, scope)) {
            for (int i = 0; true; i++) {
                String newName = name + "_" + i;
                if (!ASTUtil.isNameInScope(newName, scope)) {
                    return newName;
                }
            }
        } else {
            return name;
        }

    }

    private IASTStatement[] getBodyStatements(IASTForStatement loop) {
        if (loop.getBody() instanceof IASTCompoundStatement) {
            return ((IASTCompoundStatement) loop.getBody()).getStatements();
        } else {
            return new IASTStatement[] { loop.getBody() };
        }
    }

    private IASTNode[] getBodyObjects(IASTForStatement loop) {
        return getBodyObjects(loop, false);
    }

    // gets statements AND comments from a loop body in forward order
    private IASTNode[] getBodyObjects(IASTForStatement loop, boolean reverse) {
        List<IASTNode> objects = new ArrayList<IASTNode>();
        objects.addAll(Arrays.asList(getBodyStatements(loop)));
        for (IASTComment comment : loop.getTranslationUnit().getComments()) {
            // if the comment's offset is in between the end of the loop header and the end of the loop body
            if (comment.getFileLocation()
                    .getNodeOffset() > loop.getIterationExpression().getFileLocation().getNodeOffset()
                            + loop.getIterationExpression().getFileLocation().getNodeLength() + ")".length()
                    && comment.getFileLocation().getNodeOffset() < loop.getBody().getFileLocation().getNodeOffset()
                            + loop.getBody().getFileLocation().getNodeLength()) {
                objects.add(comment);
            }
        }
        Collections.sort(objects, reverse ? ASTUtil.REVERSE_COMPARATOR : ASTUtil.FORWARD_COMPARATOR);

        return objects.toArray(new IASTNode[objects.size()]);

    }

    private IASTName getNameFromInitializer(IASTForStatement loop) {
        IASTStatement initStmt = loop.getInitializerStatement();
        if (initStmt instanceof IASTDeclarationStatement && 
                ((IASTDeclarationStatement) initStmt).getDeclaration() instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration decl = (IASTSimpleDeclaration) ((IASTDeclarationStatement) initStmt).getDeclaration();
            return decl.getDeclarators()[0].getName();
        }
        return null;
    }
    
    private List<IASTName> getUses(IASTName var, IASTForStatement loop) {

        class VariableUseFinder extends ASTVisitor {

            private IASTName var;
            public List<IASTName> uses;

            public VariableUseFinder(IASTName var) {
                this.var = var;
                this.uses = new ArrayList<IASTName>();
                shouldVisitNames = true;
            }

            @Override
            public int visit(IASTName name) {
                if (name.resolveBinding().equals(var.resolveBinding())) {
                    uses.add(name);
                }
                return PROCESS_CONTINUE;
            }

        }

        VariableUseFinder finder = new VariableUseFinder(var);
        loop.accept(finder);
        return finder.uses;

    }

    private String getModifiedStatement(IASTStatement original, List<IASTName> uses, String newName) {
        Collections.sort(uses, ASTUtil.REVERSE_COMPARATOR);
        StringBuilder sb = new StringBuilder(original.getRawSignature());
        for (IASTName use : uses) {
            if (ASTUtil.isAncestor(original, use)) {
                int offsetIntoStatement = use.getFileLocation().getNodeOffset()
                        - original.getFileLocation().getNodeOffset();
                sb.replace(offsetIntoStatement, offsetIntoStatement + use.getFileLocation().getNodeLength(), newName);
            }
        }
        return sb.toString();
    }

}