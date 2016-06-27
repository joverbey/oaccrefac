/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.dependence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTPatternUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.BindingComparator;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.Pair;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DirectionHierarchyTester;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.FunctionWhitelist;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.LinearExpression;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.VariableAccess;

/**
 * Analyzes data dependences between statements.

 * @author Jeff Overbey
 * @author Adam Eichelkraut
 * @author Alexander Calvert
 */
public class DependenceAnalysis {

	private final List<VariableAccess> variableAccesses;

    private final Set<DataDependence> dependences;
	
	private IBinding index;
	
    /**
     * Analyzes dependences in a sequence of C statements.
     * 
     * @throws DependenceTestFailure
     */	
    public DependenceAnalysis(IProgressMonitor pm, IASTStatement... statements) throws DependenceTestFailure, OperationCanceledException {
    	this.variableAccesses = new ArrayList<VariableAccess>();
        this.dependences = new HashSet<DataDependence>();

        pm.subTask(Messages.DependenceAnalysis_FindingVariableAccesses);
        collectAccessesFromStatements(statements);
        IASTForStatement loop = null;
        if (statements.length != 0) {
        	loop = ASTUtil.findNearestAncestor(statements[0], IASTForStatement.class);
        }
        index = ForStatementInquisitor.getInquisitor(loop).getIndexVariable();
        
        
        
        pm.subTask(Messages.DependenceAnalysis_AnalyzingDependences);
        computeDependences(pm);
    }
    
    public List<VariableAccess> getVariableAccesses() {
        return this.variableAccesses;
    }
    
    public int carryDependenceCount() {
  	int count = 0;
    	for (DataDependence dependence : dependences) {
    		if (dependence.isLoopCarried()) {
    			count += 1;
    		}
    	}
    	return count;
    }

    private void computeDependences(IProgressMonitor pm) throws DependenceTestFailure {
        SubMonitor progress = SubMonitor.convert(pm, getVariableAccesses().size() * getVariableAccesses().size());

        for (VariableAccess v1 : getVariableAccesses()) {
			progress.subTask(String.format(Messages.DependenceAnalysis_AnalyzingLine,
					v1.getVariableName().getFileLocation().getStartingLineNumber(), v1));
            if (writesToIndex(v1)) {
            	throw new DependenceTestFailure(String.format(Messages.DependenceAnalysis_LoopCannotBeAnalyzed,
            		v1.getVariableName().getFileLocation().getStartingLineNumber()));
            }
            for (VariableAccess v2 : getVariableAccesses()) {
                if (v1.refersToSameVariableAs(v2) && (v1.isWrite() || v2.isWrite()) && feasibleControlFlow(v1, v2)
                        //if the sink is a declaration of the same variable, there is no dependence
                        && !(v2.getEnclosingStatement() instanceof IASTDeclarationStatement)) {
                    DependenceType dependenceType = v1.getDependenceTypeTo(v2);
                    VariableAccess decl = declaratorInLoopNestFor(v1); //same for v2
                    if (v1.isScalarAccess() || v2.isScalarAccess()) {
                        Direction[] directionVector = new Direction[v1.numEnclosingLoops()];
                        Arrays.fill(directionVector, Direction.ANY);
                        if(decl == null) {
                          addDependence(new DataDependence(v1, v2, directionVector, dependenceType));
                        }
                        else if(!v1.equals(v2) || decl.getEnclosingLoops().size() != v1.getEnclosingLoops().size()) {
                        	int v1Decl = v1.getCommonEnclosingLoops(decl).size();
                        	int v2Decl = v2.getCommonEnclosingLoops(decl).size();
                        	Arrays.fill(directionVector, 0, Math.min(v1Decl, v2Decl), Direction.EQ);
                        	addDependence(new DataDependence(v1, v2, directionVector, dependenceType));
                        }
                    } else {
                        List<IASTForStatement> commonLoops = v1.getCommonEnclosingLoops(v2);
                        List<IBinding> indexVars = DependenceAnalysis.getLoopIndexVariables(commonLoops);
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
                            ForStatementInquisitor thisLoop = ForStatementInquisitor.getInquisitor(commonLoops.get(i));
                            Long lb = thisLoop.getLowerBound();
                            Long ub = thisLoop.getInclusiveUpperBound();
                            if (lb != null && Integer.MIN_VALUE + 1 <= lb.longValue()
                                    && lb.longValue() <= Integer.MAX_VALUE - 1) {
                                lowerBounds[i] = (int) lb.longValue();
                            }
                            if (ub != null && Integer.MIN_VALUE + 1 <= ub.longValue()
                                    && ub.longValue() <= Integer.MAX_VALUE - 1) {
                                upperBounds[i] = (int) ub.longValue();
                            }
                        }

                        DirectionHierarchyTester dht = new DirectionHierarchyTester(lowerBounds, upperBounds,
                                writeCoefficients, readCoefficients, otherVars.size());
                        Set<Direction[]> dvs = dht.getPossibleDependenceDirections();
                        for (Direction[] directionVector : dvs) {
                        	DataDependence dep = new DataDependence(v1, v2, directionVector, dependenceType);
                            if(!(v1.equals(v2) && dep.isLoopIndependent()) && !carriedByLoopContainingDeclaration(directionVector, decl)) {
                            	addDependence(dep);
                            }
                        }
                    }
                }

                progress.worked(1);
                if (progress.isCanceled()) {
                    throw new OperationCanceledException(Messages.DependenceAnalysis_DependenceTestCancelled);
                }
            }
        }
    }
    
    private boolean carriedByLoopContainingDeclaration(Direction[] directionVector, VariableAccess decl) {
    	if(decl == null) {
    		return false;
    	}
    	List<IASTForStatement> declLoops = decl.getEnclosingLoops();
    	for(int i = 0; i < declLoops.size(); i++) {
    		if(directionVector[i] != Direction.EQ) {
    			return true;
    		}
    	}
    	return false;
    }

	private VariableAccess declaratorInLoopNestFor(VariableAccess use) {
    	List<IASTForStatement> loops = use.getEnclosingLoops();
    	for(IASTForStatement loop : loops) {
    		for(IASTDeclarator decl : ASTUtil.find(loop, IASTDeclarator.class)) {
				if (decl.getName().resolveBinding().equals(use.getBinding())) {
					for(VariableAccess v : variableAccesses) {
						if(v.bindsTo(use.getBinding())) {
							return v;
						}
						else {
							return new VariableAccess(true, decl.getName());
						}
					}
				}
    		}
    	}
    	return null;
    }
    
    private boolean writesToIndex(VariableAccess write) {
    	return write.isWrite() && write.bindsTo(index);
    }

	private static List<IBinding> getLoopIndexVariables(List<IASTForStatement> loops) {
	    List<IBinding> result = new ArrayList<IBinding>(loops.size());
	    for (IASTForStatement forStmt : loops) {
	        ForStatementInquisitor loop = ForStatementInquisitor.getInquisitor(forStmt);
	        IBinding variable = loop.getIndexVariable();
	        if (variable != null) {
	            result.add(variable);
	        }
	    }
	    return result;
	}
	
	private void addDependence(DataDependence dataDependence) {
        this.dependences.add(dataDependence);
    }

    private Set<IBinding> collectAllVariables(LinearExpression[]... exprs) {
        Set<IBinding> vars = new TreeSet<IBinding>(new BindingComparator());
        for (LinearExpression[] array : exprs) {
            for (LinearExpression e : array) {
                vars.addAll(e.getVariables());
            }
        }
        return vars;
    }

    private int[] fillArray(int length, int value) {
        int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }

    private boolean feasibleControlFlow(VariableAccess v1, VariableAccess v2) {
        return v1.isInCommonLoopsWith(v2) || v1.enclosingStatementLexicallyPrecedes(v2);
    }

    private void collectAccessesFromStatements(IASTStatement... statements) throws DependenceTestFailure {
        for (IASTStatement stmt : statements) {
            if (stmt instanceof IASTDeclarationStatement) {
                collectAccessesFrom((IASTDeclarationStatement) stmt);
            } else if (stmt instanceof IASTForStatement) {
                collectAccessesFrom((IASTForStatement) stmt);
            } else if (stmt instanceof IASTIfStatement) {
                collectAccessesFrom((IASTIfStatement) stmt);
            } else if (stmt instanceof IASTSwitchStatement) {
                collectAccessesFrom((IASTSwitchStatement) stmt);
            } else if (stmt instanceof IASTExpressionStatement) {
                collectAccessesFrom((IASTExpressionStatement) stmt);
            } else if (stmt instanceof IASTNullStatement) {
                collectAccessesFrom((IASTNullStatement) stmt);
            } else if (stmt instanceof IASTCaseStatement) {
                collectAccessesFrom((IASTCaseStatement) stmt);
            } else if (stmt instanceof IASTDefaultStatement) {
                collectAccessesFrom((IASTDefaultStatement) stmt);
            } else if (stmt instanceof IASTCompoundStatement) {
                collectAccessesFrom((IASTCompoundStatement) stmt);
            } else if (stmt instanceof IASTBreakStatement) {
                collectAccessesFrom((IASTBreakStatement) stmt);
            } else if (stmt instanceof IASTContinueStatement) {
                collectAccessesFrom((IASTContinueStatement) stmt);
            } else if (stmt instanceof IASTWhileStatement) {
                collectAccessesFrom((IASTWhileStatement) stmt);
            } else {
                throw unsupported(stmt);
            }
        }
    }

    private void collectAccessesFrom(IASTNullStatement stmt) {
        // Nothing to do
    }
    
    private void collectAccessesFrom(IASTBreakStatement stmt) {
        // Nothing to do; see ForLoopCheck#containsUnsupportedOp
    }
    
    private void collectAccessesFrom(IASTContinueStatement stmt) {
        // Nothing to do; see ForLoopCheck#containsUnsupportedOp
    }

	private void collectAccessesFrom(IASTCaseStatement stmt) throws DependenceTestFailure {
        collectAccessesFromExpression(stmt.getExpression());
    }
    
    private void collectAccessesFrom(IASTDefaultStatement stmt) throws DependenceTestFailure {
        //Nothing to do
    }

    private void collectAccessesFrom(IASTCompoundStatement stmt) throws DependenceTestFailure {
        collectAccessesFromStatements(stmt.getStatements());
    }

    private void collectAccessesFrom(IASTForStatement stmt) throws DependenceTestFailure {
        ForStatementInquisitor forLoop = ForStatementInquisitor.getInquisitor(stmt);
        if (!forLoop.isCountedLoop()) {
            throw unsupported(stmt);
        }

        collectAccessesFromStatements(stmt.getBody());
    }
    
    private void collectAccessesFrom(IASTWhileStatement stmt) throws DependenceTestFailure {
    	collectAccessesFromExpression(stmt.getCondition());
        collectAccessesFromStatements(stmt.getBody());
    }
    
    private void collectAccessesFrom(IASTIfStatement stmt) throws DependenceTestFailure {
    	collectAccessesFromExpression(stmt.getConditionExpression());
    	collectAccessesFromStatements(stmt.getThenClause());
    	IASTStatement elsePart = stmt.getElseClause();
    	if (elsePart != null) {
    		collectAccessesFromStatements(elsePart);
    	}
    }
    
    private void collectAccessesFrom(IASTSwitchStatement stmt) throws DependenceTestFailure {
        collectAccessesFromStatements(stmt.getBody());
    }

    private void collectAccessesFrom(IASTDeclarationStatement stmt) throws DependenceTestFailure {
        if (!(stmt.getDeclaration() instanceof IASTSimpleDeclaration))
            throw unsupported(stmt);

        IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) stmt.getDeclaration();
        for (IASTDeclarator declarator : simpleDecl.getDeclarators()) {
            if (declarator.getNestedDeclarator() != null || declarator.getPointerOperators().length > 0)
                throw unsupported(stmt);

            variableAccesses.add(new VariableAccess(true, declarator.getName()));

            if (declarator.getInitializer() != null) {
                if (!(declarator.getInitializer() instanceof IASTEqualsInitializer))
                    throw unsupported(stmt);
                IASTEqualsInitializer eqInit = (IASTEqualsInitializer) declarator.getInitializer();
                IASTInitializerClause initializer = eqInit.getInitializerClause();
                if (!(initializer instanceof IASTExpression))
                    throw unsupported(stmt);
                collectAccessesFromExpression((IASTExpression) initializer);
            }
        }
    }

    private void collectAccessesFrom(IASTExpressionStatement stmt) throws DependenceTestFailure {
        Pair<IASTExpression, IASTExpression> asgt = ASTPatternUtil.getAssignment(stmt.getExpression());
        if (asgt != null) {
            collectAccessesFromAssignmentLHS(asgt.getFirst());
            collectAccessesFromExpression(asgt.getSecond());
            return;
        }

        Pair<IASTExpression, IASTExpression> assignEq = ASTPatternUtil.getAssignEq(stmt.getExpression());
        if (assignEq != null) {
            // The variable on the LHS is both read and written
            collectAccessesFromAssignmentLHS(assignEq.getFirst());
            collectAccessesFromExpression(assignEq.getFirst());
            collectAccessesFromExpression(assignEq.getSecond());
            return;
        }

        IASTExpression incrDecr = ASTPatternUtil.getIncrDecr(stmt.getExpression());
        if (incrDecr != null) {
            collectAccessesFromExpression(incrDecr);
            collectAccessesFromAssignmentLHS(incrDecr);
            return;
        }
        
        IASTFunctionCallExpression function = ASTPatternUtil.getFuncExpression(stmt.getExpression());
        if (function != null) {
        	collectAccessesFromExpression(function);
        	return;
        }

        throw unsupported(stmt);
    }

    private void collectAccessesFromAssignmentLHS(IASTExpression expr) throws DependenceTestFailure {
        IASTName scalar = ASTPatternUtil.getIdExpression(expr);
        if (scalar != null) {
            variableAccesses.add(new VariableAccess(true, scalar));
            return;
        }

        Pair<IASTName, IASTName> fieldReference = ASTPatternUtil.getSimpleFieldReference(expr);
        if (fieldReference != null) {
            IASTName owner = fieldReference.getFirst();
            IASTName field = fieldReference.getSecond();
            variableAccesses.add(new VariableAccess(false, owner));
            variableAccesses.add(new VariableAccess(true, field));
            return;
        }

        Pair<IASTName, LinearExpression[]> arrayAccess = ASTPatternUtil.getMultidimArrayAccess(expr);
        if (arrayAccess != null) {
            variableAccesses.add(new VariableAccess(true, arrayAccess.getFirst(), arrayAccess.getSecond()));
            IASTInitializerClause arg = ((IASTArraySubscriptExpression) expr).getArgument();
            if (arg instanceof IASTExpression) {
            	collectAccessesFromExpression((IASTExpression)arg);
                return;
            }
        }
        
        IASTExpression incrDecr = ASTPatternUtil.getIncrDecr(expr);
        if (incrDecr != null) {
            collectAccessesFromExpression(incrDecr);
            collectAccessesFromAssignmentLHS(incrDecr);
            return;
        }

        throw unsupported(expr);
    }

    private void collectAccessesFromExpression(IASTExpression expr) throws DependenceTestFailure {
        if (expr instanceof IASTConditionalExpression) {
            collectAccessesFrom((IASTConditionalExpression) expr);
        } else if (expr instanceof IASTBinaryExpression) {
                collectAccessesFrom((IASTBinaryExpression) expr);
        } else if (expr instanceof IASTUnaryExpression) {
            collectAccessesFrom((IASTUnaryExpression) expr);
        } else if (expr instanceof IASTLiteralExpression) {
            collectAccessesFrom((IASTLiteralExpression) expr);
        } else if (expr instanceof IASTIdExpression) {
            collectAccessesFrom((IASTIdExpression) expr);
        } else if (expr instanceof IASTArraySubscriptExpression) {
            collectAccessesFrom((IASTArraySubscriptExpression) expr);
        } else if (expr instanceof IASTFieldReference) {
            collectAccessesFrom((IASTFieldReference) expr);
        } else if (expr instanceof IASTFunctionCallExpression) {
            collectAccessesFrom((IASTFunctionCallExpression) expr);
        } else {
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTConditionalExpression expr) throws DependenceTestFailure {
        collectAccessesFromExpression(expr.getLogicalConditionExpression());
        collectAccessesFromExpression(expr.getPositiveResultExpression());
        collectAccessesFromExpression(expr.getNegativeResultExpression());
    }

    private void collectAccessesFrom(IASTBinaryExpression expr) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTBinaryExpression.op_plus:
        case IASTBinaryExpression.op_minus:
        case IASTBinaryExpression.op_multiply:
        case IASTBinaryExpression.op_divide:
        case IASTBinaryExpression.op_modulo:
        case IASTBinaryExpression.op_binaryAnd:
        case IASTBinaryExpression.op_binaryOr:
        case IASTBinaryExpression.op_binaryXor:
        case IASTBinaryExpression.op_shiftLeft:
        case IASTBinaryExpression.op_shiftRight:
        case IASTBinaryExpression.op_greaterThan:
        case IASTBinaryExpression.op_greaterEqual:
        case IASTBinaryExpression.op_lessThan:
        case IASTBinaryExpression.op_lessEqual:
        case IASTBinaryExpression.op_equals:
        case IASTBinaryExpression.op_notequals:
        case IASTBinaryExpression.op_logicalAnd:
        case IASTBinaryExpression.op_logicalOr:
        case IASTBinaryExpression.op_min:
        case IASTBinaryExpression.op_max:
            collectAccessesFromExpression(expr.getOperand1());
            collectAccessesFromExpression(expr.getOperand2());
            break;
        default:
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTUnaryExpression expr) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTUnaryExpression.op_bracketedPrimary:
        case IASTUnaryExpression.op_plus:
        case IASTUnaryExpression.op_minus:
        case IASTUnaryExpression.op_not:
        case IASTUnaryExpression.op_tilde:
        case IASTUnaryExpression.op_sizeof:
            collectAccessesFromExpression(expr.getOperand());
            break;
        case IASTUnaryExpression.op_prefixIncr:
        case IASTUnaryExpression.op_prefixDecr:
        case IASTUnaryExpression.op_postFixIncr:
        case IASTUnaryExpression.op_postFixDecr:
        	collectAccessesFromAssignmentLHS(expr.getOperand());
            collectAccessesFromExpression(expr.getOperand());
            break;
        default:
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTLiteralExpression expr) {
        // Nothing to do
    }

    private void collectAccessesFrom(IASTIdExpression expr) throws DependenceTestFailure {
        IASTName name = ASTPatternUtil.getIdExpression(expr);
        if (name == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(false, name));
    }

    private void collectAccessesFrom(IASTFieldReference expr) throws DependenceTestFailure {
        Pair<IASTName, IASTName> pair = ASTPatternUtil.getSimpleFieldReference(expr);
        if (pair == null)
            throw unsupported(expr);

        IASTName owner = pair.getFirst();
        IASTName field = pair.getSecond();
        variableAccesses.add(new VariableAccess(false, owner));
        variableAccesses.add(new VariableAccess(false, field));
    }

    private void collectAccessesFrom(IASTFunctionCallExpression expr) throws DependenceTestFailure {
        if (FunctionWhitelist.isWhitelisted(expr)) {
            // Whitelisted functions are known not to affect any aliased variables
            for (IASTInitializerClause arg : expr.getArguments()) {
                if (arg instanceof IASTExpression) {
                    collectAccessesFromExpression((IASTExpression)arg);
                } else {
                    throw unsupported(arg);
                }
            }
        } else {
            IASTFunctionDefinition function = ASTUtil.findNearestAncestor(expr, IASTFunctionDefinition.class);
            // Conservatively assume that a function call might change every variable
            // whose address is taken (via pointers)...
            AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, new NullProgressMonitor());
            for (IVariable var : analysis.getAddressTakenVariables()) {
                variableAccesses.add(new VariableAccess(false, expr, var)); // Read
                variableAccesses.add(new VariableAccess(true, expr, var));  // Write
            }
            // ...but otherwise it will not change any local variables
        }
    }

    private void collectAccessesFrom(IASTArraySubscriptExpression expr) throws DependenceTestFailure {
        Pair<IASTName, LinearExpression[]> arrayAccess = ASTPatternUtil.getMultidimArrayAccess(expr);
        if (arrayAccess == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(false, arrayAccess.getFirst(), arrayAccess.getSecond()));

        collectAccessesFromExpression((IASTExpression) expr.getArgument());
    }

    private static DependenceTestFailure unsupported(IASTNode node) {
        return new DependenceTestFailure(String.format(Messages.DependenceAnalysis_UnsupportedConstruct,
                node.getFileLocation().getStartingLineNumber(), //
                node.getClass().getSimpleName(), ASTUtil.toString(node)));
    }

    public Set<DataDependence> getDependences() {
        return Collections.unmodifiableSet(dependences);
    }

    public boolean hasLevel1CarriedDependence() {
        for (DataDependence dep : dependences) {
            if (dep.getLevel() == 1) {
                return true;
            }
        }
        return false;
    }

}
