package edu.auburn.oaccrefac.core.dependence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;
import edu.auburn.oaccrefac.internal.core.dependence.DirectionHierarchyTester;
import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

/**
 * Analyzes data dependences between statements.
 * 
 * @author Alexander Calvert
 * @author Jeff Overbey
 */
public class DependenceAnalysis extends AbstractDependenceAnalysis {

    /**
     * Constructor.  Analyzes dependences in a sequence of C statements.
     * 
     * @throws DependenceTestFailure
     */
    public DependenceAnalysis(IProgressMonitor pm, IASTStatement... statements) throws DependenceTestFailure, OperationCanceledException {
        super(pm, statements);

        pm.subTask("Analyzing dependences...");
        computeDependences(pm);
    }
    
    @Override
    protected void computeDependences(IProgressMonitor pm) throws DependenceTestFailure {
        SubMonitor progress = SubMonitor.convert(pm, getVariableAccesses().size() * getVariableAccesses().size());
        for (VariableAccess v1 : getVariableAccesses()) {
            progress.subTask(String.format("Analyzing line %d - %s",
                    v1.getVariableName().getFileLocation().getStartingLineNumber(), v1));
            for (VariableAccess v2 : getVariableAccesses()) {
                if (v1.refersToSameVariableAs(v2) && (v1.isWrite() || v2.isWrite()) && feasibleControlFlow(v1, v2)) {
                    IASTStatement s1 = v1.getEnclosingStatement();
                    IASTStatement s2 = v2.getEnclosingStatement();
                    DependenceType dependenceType = v1.getDependenceTypeTo(v2);
                    if (v1.isScalarAccess() || v2.isScalarAccess()) {
                        Direction[] directionVector = new Direction[v1.numEnclosingLoops()];
                        Arrays.fill(directionVector, Direction.ANY);
                        addDependence(new DataDependence(s1, s2, directionVector, dependenceType));
                    } else {
                        List<IASTForStatement> commonLoops = v1.getCommonEnclosingLoops(v2);
                        List<IBinding> indexVars = ASTUtil.getLoopIndexVariables(commonLoops);
                        Set<IBinding> otherVars = collectAllVariables(v1.getLinearSubscriptExpressions(),
                                v2.getLinearSubscriptExpressions());
                        otherVars.removeAll(indexVars);
                              
                        List<IBinding> vars = new ArrayList<IBinding>(indexVars.size() + otherVars.size());
                        vars.addAll(indexVars);
                        vars.addAll(otherVars);
                        

                        int[][] writeCoefficients = v1.collectCoefficients(vars);
                        int[][] readCoefficients = v2.collectCoefficients(vars);

                        int[] lowerBounds = fillArray(commonLoops.size(), Integer.MIN_VALUE + 1);
                        int[] upperBounds = fillArray(commonLoops.size(), Integer.MAX_VALUE - 1);
                        for (int i = 0; i < commonLoops.size(); i++) {
                            ForStatementInquisitor thisLoop = InquisitorFactory.getInquisitor(commonLoops.get(i));
                            lowerBounds[i] = thisLoop.getLowerBound();
                            Long ub = thisLoop.getInclusiveUpperBound();
                            if (ub != null && Integer.MIN_VALUE+1 <= ub.longValue() && ub.longValue() <= Integer.MAX_VALUE-1)
                                upperBounds[i] = (int)ub.longValue();
                        }

                        DirectionHierarchyTester dht = new DirectionHierarchyTester(lowerBounds, upperBounds,
                                writeCoefficients, readCoefficients, otherVars.size());
                        for (Direction[] directionVector : dht.getPossibleDependenceDirections()) {
                            addDependence(new DataDependence(s1, s2, directionVector, dependenceType));
                        }
                    }
                }

                progress.worked(1);
                if (progress.isCanceled()) {
                    throw new OperationCanceledException("Dependence test cancelled.");
                }
            }
        }
    }

}
