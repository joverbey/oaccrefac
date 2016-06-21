/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core;

import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ConstantPropagation;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ASTMatcher;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryIntegerConstant;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

public class ForStatementInquisitor {

	public static ForStatementInquisitor getInquisitor(IASTForStatement loop) {
		return InquisitorFactory.getInquisitor(loop);
	}

	private static class InquisitorFactory {
		private static final WeakHashMap<IASTForStatement, ForStatementInquisitor> refs = new WeakHashMap<>();

		/**
		 * Factory method. Creates a {@link ForStatementInquisitor} (or returns a cached copy) for an {@link IASTForStatement}.
		 * <p>
		 * <code>forStmt</code> may be <code>null</code>.
		 * 
		 * @param forStmt
		 *            statement for which to create an inquisitor
		 * @return {@link ForStatementInquisitor} (non-<code>null</code>)
		 */
		public static ForStatementInquisitor getInquisitor(IASTForStatement forStmt) {
			// Is there a cached ForStatementInquisitor for this loop?
			if (refs.containsKey(forStmt) && refs.get(forStmt) != null)
				return refs.get(forStmt);

			// Otherwise we need to create a new one
			ForStatementInquisitor value = new ForStatementInquisitor(forStmt);
			refs.put(forStmt, value);
			return value;
		}
	}

    private final IASTForStatement statement;

    public IASTForStatement getStatement() {
        return statement;
    }

    private boolean counted;

    // Patterns of for loops that are acceptable to refactor...
    private static String[] patterns = {
            // Constant upper bound
            "for (i = 0; i < 1; i++) ;", //
            "for (i = 0; i <= 1; i++) ;", //
            "for (i = 0; i < 1; i+=1) ;", //
            "for (i = 0; i <= 1; i+=1) ;", //
            "for (i = 0; i < 1; i=i+1) ;", //
            "for (i = 0; i <= 1; i=i+1) ;", //
            "for (int i = 0; i < 1; i++) ;", //
            "for (int i = 0; i <= 1; i++) ;", //
            "for (int i = 0; i < 1; i+=1) ;", //
            "for (int i = 0; i <= 1; i+=1) ;", //
            "for (int i = 0; i < 1; i=i+1) ;", //
            "for (int i = 0; i <= 1; i=i+1) ;", //
            "for (int32_t i = 0; i < 1; i++) ;", //
            "for (int32_t i = 0; i <= 1; i++) ;", //
            "for (int32_t i = 0; i < 1; i+=1) ;", //
            "for (int32_t i = 0; i <= 1; i+=1) ;", //
            "for (int32_t i = 0; i < 1; i=i+1) ;", //
            "for (int32_t i = 0; i <= 1; i=i+1) ;", //
            "for (uint32_t i = 0; i < 1; i++) ;", //
            "for (uint32_t i = 0; i <= 1; i++) ;", //
            "for (uint32_t i = 0; i < 1; i+=1) ;", //
            "for (uint32_t i = 0; i <= 1; i+=1) ;", //
            "for (uint32_t i = 0; i < 1; i=i+1) ;", //
            "for (uint32_t i = 0; i <= 1; i=i+1) ;", //
            "for (long i = 0; i < 1; i++) ;", //
            "for (long i = 0; i <= 1; i++) ;", //
            "for (long i = 0; i < 1; i+=1) ;", //
            "for (long i = 0; i <= 1; i+=1) ;", //
            "for (long i = 0; i < 1; i=i+1) ;", //
            "for (long i = 0; i <= 1; i=i+1) ;", //
            "for (int64_t i = 0; i < 1; i++) ;", //
            "for (int64_t i = 0; i <= 1; i++) ;", //
            "for (int64_t i = 0; i < 1; i+=1) ;", //
            "for (int64_t i = 0; i <= 1; i+=1) ;", //
            "for (int64_t i = 0; i < 1; i=i+1) ;", //
            "for (int64_t i = 0; i <= 1; i=i+1) ;", //
            "for (uint64_t i = 0; i < 1; i++) ;", //
            "for (uint64_t i = 0; i <= 1; i++) ;", //
            "for (uint64_t i = 0; i < 1; i+=1) ;", //
            "for (uint64_t i = 0; i <= 1; i+=1) ;", //
            "for (uint64_t i = 0; i < 1; i=i+1) ;", //
            "for (uint64_t i = 0; i <= 1; i=i+1) ;", //
            // Variable upper bound
            "for (i = 0; i < j; i++) ;", //
            "for (i = 0; i <= j; i++) ;", //
            "for (i = 0; i < j; i+=1) ;", //
            "for (i = 0; i <= j; i+=1) ;", //
            "for (i = 0; i < j; i=i+1) ;", //
            "for (i = 0; i <= j; i=i+1) ;", //
            "for (int i = 0; i < j; i++) ;", //
            "for (int i = 0; i <= j; i++) ;", //
            "for (int i = 0; i < j; i+=1) ;", //
            "for (int i = 0; i <= j; i+=1) ;", //
            "for (int i = 0; i < j; i=i+1) ;", //
            "for (int i = 0; i <= j; i=i+1) ;", //
            "for (int32_t i = 0; i < j; i++) ;", //
            "for (int32_t i = 0; i <= j; i++) ;", //
            "for (int32_t i = 0; i < j; i+=1) ;", //
            "for (int32_t i = 0; i <= j; i+=1) ;", //
            "for (int32_t i = 0; i < j; i=i+1) ;", //
            "for (int32_t i = 0; i <= j; i=i+1) ;", //
            "for (uint32_t i = 0; i < j; i++) ;", //
            "for (uint32_t i = 0; i <= j; i++) ;", //
            "for (uint32_t i = 0; i < j; i+=1) ;", //
            "for (uint32_t i = 0; i <= j; i+=1) ;", //
            "for (uint32_t i = 0; i < j; i=i+1) ;", //
            "for (uint32_t i = 0; i <= j; i=i+1) ;", //
            "for (long i = 0; i < j; i++) ;", //
            "for (long i = 0; i <= j; i++) ;", //
            "for (long i = 0; i < j; i+=1) ;", //
            "for (long i = 0; i <= j; i+=1) ;", //
            "for (long i = 0; i < j; i=i+1) ;", //
            "for (long i = 0; i <= j; i=i+1) ;", //
            "for (int64_t i = 0; i < j; i++) ;", //
            "for (int64_t i = 0; i <= j; i++) ;", //
            "for (int64_t i = 0; i < j; i+=1) ;", //
            "for (int64_t i = 0; i <= j; i+=1) ;", //
            "for (int64_t i = 0; i < j; i=i+1) ;", //
            "for (int64_t i = 0; i <= j; i=i+1) ;", //
            "for (uint64_t i = 0; i < j; i++) ;", //
            "for (uint64_t i = 0; i <= j; i++) ;", //
            "for (uint64_t i = 0; i < j; i+=1) ;", //
            "for (uint64_t i = 0; i <= j; i+=1) ;", //
            "for (uint64_t i = 0; i < j; i=i+1) ;", //
            "for (uint64_t i = 0; i <= j; i=i+1) ;", //
            // Simple field reference upper bound (used in Livermore Loops)
            "for (i = 0; i < j.k; i++) ;", //
            "for (i = 0; i <= j.k; i++) ;", //
            "for (i = 0; i < j.k; i+=1) ;", //
            "for (i = 0; i <= j.k; i+=1) ;", //
            "for (i = 0; i < j.k; i=i+1) ;", //
            "for (i = 0; i <= j.k; i=i+1) ;", //
            "for (int i = 0; i < j.k; i++) ;", //
            "for (int i = 0; i <= j.k; i++) ;", //
            "for (int i = 0; i < j.k; i+=1) ;", //
            "for (int i = 0; i <= j.k; i+=1) ;", //
            "for (int i = 0; i < j.k; i=i+1) ;", //
            "for (int i = 0; i <= j.k; i=i+1) ;", //
            "for (int32_t i = 0; i < j.k; i++) ;", //
            "for (int32_t i = 0; i <= j.k; i++) ;", //
            "for (int32_t i = 0; i < j.k; i+=1) ;", //
            "for (int32_t i = 0; i <= j.k; i+=1) ;", //
            "for (int32_t i = 0; i < j.k; i=i+1) ;", //
            "for (int32_t i = 0; i <= j.k; i=i+1) ;", //
            "for (uint32_t i = 0; i < j.k; i++) ;", //
            "for (uint32_t i = 0; i <= j.k; i++) ;", //
            "for (uint32_t i = 0; i < j.k; i+=1) ;", //
            "for (uint32_t i = 0; i <= j.k; i+=1) ;", //
            "for (uint32_t i = 0; i < j.k; i=i+1) ;", //
            "for (uint32_t i = 0; i <= j.k; i=i+1) ;", //
            "for (long i = 0; i < j.k; i++) ;", //
            "for (long i = 0; i <= j.k; i++) ;", //
            "for (long i = 0; i < j.k; i+=1) ;", //
            "for (long i = 0; i <= j.k; i+=1) ;", //
            "for (long i = 0; i < j.k; i=i+1) ;", //
            "for (long i = 0; i <= j.k; i=i+1) ;", //
            "for (int64_t i = 0; i < j.k; i++) ;", //
            "for (int64_t i = 0; i <= j.k; i++) ;", //
            "for (int64_t i = 0; i < j.k; i+=1) ;", //
            "for (int64_t i = 0; i <= j.k; i+=1) ;", //
            "for (int64_t i = 0; i < j.k; i=i+1) ;", //
            "for (int64_t i = 0; i <= j.k; i=i+1) ;", //
            "for (uint64_t i = 0; i < j.k; i++) ;", //
            "for (uint64_t i = 0; i <= j.k; i++) ;", //
            "for (uint64_t i = 0; i < j.k; i+=1) ;", //
            "for (uint64_t i = 0; i <= j.k; i+=1) ;", //
            "for (uint64_t i = 0; i < j.k; i=i+1) ;", //
            "for (uint64_t i = 0; i <= j.k; i=i+1) ;", //
    };

    private ForStatementInquisitor(IASTForStatement statement) {
        this.statement = statement;
        this.counted = determineIfLoopIsCounted(statement); // cache this for performance
    }

	private static boolean determineIfLoopIsCounted(IASTForStatement statement) {
		class LiteralReplacer extends ASTVisitor {
            public LiteralReplacer() {
                shouldVisitExpressions = true;
            }

            @Override
            public int visit(IASTExpression expr) {
                if (expr instanceof IASTLiteralExpression && expr.getParent() != null) {
                    IASTLiteralExpression literal = (IASTLiteralExpression) expr;
                    if (literal.getParent() instanceof IASTBinaryExpression)
                        ((IASTBinaryExpression) literal.getParent()).setOperand2(new ArbitraryIntegerConstant());
                    else if (literal.getParent() instanceof IASTEqualsInitializer)
                        ((IASTEqualsInitializer) literal.getParent())
                                .setInitializerClause(new ArbitraryIntegerConstant());
                }
                return PROCESS_CONTINUE;
            }
        }

        for (String pattern : patterns) {
            IASTForStatement orig = (IASTForStatement) ASTUtil.parseStatementNoFail(pattern);
            IASTForStatement patternAST = orig.copy(CopyStyle.withoutLocations);
            patternAST.accept(new LiteralReplacer());
            patternAST.setBody(new ArbitraryStatement());
            if (ASTMatcher.unify(patternAST, statement) != null) {
                return true;
            }
        }    
        return false;
	}

    /**
     * Method matches the parameter with all of the patterns defined in the pattern string array above. It parses each
     * string into a corresponding AST and then uses a pattern matching utility to match if the pattern is loosely
     * synonymous to the matchee. (Basically, we have to check some pre-conditions before refactoring or else we could
     * run into some hairy cases such as an array subscript expression being in the initializer statement...which would
     * be a nightmare to refactor).
     * 
     * @param matchee
     *            -- tree or AST to match
     * @return Boolean describing whether the matchee matches any supported pattern
     * @throws CoreException
     */
    public boolean isCountedLoop() {
        return counted;
    }

    /**
     * Returns the index variable for a counted loop, or <code>null</code> if the loop does not match one of the
     * supported patterns for counted loops.
     * 
     * @param matchee
     * @return IBinding
     */
    public IBinding getIndexVariable() {
        if (!isCountedLoop())
            return null;

        class NameFinder extends ASTVisitor {
            private IASTName name = null;

            NameFinder() {
                super(true);
            }

            @Override
            public int visit(IASTName name) {
                if (this.name == null)
                    this.name = name;
                return PROCESS_ABORT;
            }
        }

        NameFinder nameFinder = new NameFinder();
        statement.getConditionExpression().accept(nameFinder);
        // nameFinder.name will be non-null for all of the patterns we match
        return nameFinder.name.resolveBinding();
    }

    public Long getLowerBound() {
        // This makes assumptions about the tree structure based on the patterns above
        IASTFunctionDefinition enclosingFunction = ASTUtil.findNearestAncestor(statement, IASTFunctionDefinition.class);
        if (statement.getInitializerStatement() instanceof IASTDeclarationStatement) {
            IASTDeclarationStatement stmt = (IASTDeclarationStatement) statement.getInitializerStatement();
            IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) stmt.getDeclaration();
            for (IASTDeclarator declarator : simpleDecl.getDeclarators()) {
                IASTEqualsInitializer eqInit = (IASTEqualsInitializer) declarator.getInitializer();
                IASTInitializerClause initializer = eqInit.getInitializerClause();
                return new ConstantPropagation(enclosingFunction).evaluate((IASTExpression) initializer);
            }
        } else if (statement.getInitializerStatement() instanceof IASTExpressionStatement) {
            IASTExpressionStatement stmt = (IASTExpressionStatement) statement.getInitializerStatement();
            return new ConstantPropagation(enclosingFunction).evaluate(stmt.getExpression());
        }
        throw new IllegalStateException();
    }

    public Long getInclusiveUpperBound() {
        IASTBinaryExpression condExpr = (IASTBinaryExpression) statement.getConditionExpression();
        IASTExpression ubExpr = condExpr.getOperand2();

        IASTFunctionDefinition enclosingFunction = ASTUtil.findNearestAncestor(statement, IASTFunctionDefinition.class);
        Long newUB = new ConstantPropagation(enclosingFunction).evaluate(ubExpr);
        if (newUB == null)
            return null;

        if (condExpr.getOperator() == IASTBinaryExpression.op_lessThan)
            newUB = newUB.longValue() - 1;

        return newUB;
    }

    /**
     * Assumes loops are perfectly nested
     * 
     * @param outerLoop
     * @return IBinding
     */
    public IASTStatement getInnermostLoopBody() {
        return getInnermostLoopBody(statement);
    }

    private IASTStatement getInnermostLoopBody(IASTForStatement outerLoop) {
        IASTStatement body = outerLoop.getBody();
        if (body instanceof IASTForStatement) {
            return getInnermostLoopBody((IASTForStatement) body);
        } else if (body instanceof IASTCompoundStatement) {
            IASTNode[] children = ((IASTCompoundStatement) body).getChildren();
            if (children.length == 1 && children[0] instanceof IASTForStatement) {
                return getInnermostLoopBody((IASTForStatement) children[0]);
            } else {
                return body;
            }
        } else {
            return body;
        }
    }

    /**
     * Checks if all innermost statements are valid currently, a valid statement is
     * either an assignment statement or null
     * 
     * @param outerLoop
     * @return first invalid statement or <code>null</code>
     */
    public IASTNode getFirstUnsupportedStmt() {
        return getFirstUnsupportedStmtInLoop(statement);
    }

    private IASTNode getFirstUnsupportedStmtInLoop(IASTForStatement outerLoop) {
        IASTNode body = outerLoop.getBody();
        if (body instanceof IASTCompoundStatement) {
            if (body.getChildren().length == 0) {
                return null;
            } else if (body.getChildren().length == 1 && body.getChildren()[0] instanceof IASTForStatement) {
                // perfect nesting, so only check children[0]
                return getFirstUnsupportedStmtInLoop((IASTForStatement) body.getChildren()[0]);
            } else {
                // check if all children are assignments or null stmts
                for (IASTNode child : body.getChildren()) {
                    // to be an asgt, must be an expr stmt with a bin expr child,
                    // which has asgt operator
                    if (!isLoopBodyStmtSupported(child))
                        return child;
                }
                return null;
            }
        } else if (body instanceof IASTForStatement) {
            return getFirstUnsupportedStmtInLoop((IASTForStatement) body);
        } else { // neither compound nor for statement - body is the only statement
            return isLoopBodyStmtSupported(body) ? null : body;
        }
    }

    private boolean isLoopBodyStmtSupported(IASTNode body) {
        if (body instanceof IASTNullStatement) {
            return true;
        } else if (body instanceof IASTDeclarationStatement) {
            return true;
        } else if (body instanceof IASTForStatement) {
            return true;
        } else if (body instanceof IASTExpressionStatement) {
            if (body.getChildren()[0] instanceof IASTBinaryExpression) {
                IASTBinaryExpression expr = (IASTBinaryExpression) body.getChildren()[0];
                return ASTPatternUtil.getAssignment(expr) != null || ASTPatternUtil.getAssignEq(expr) != null;
            } else if (body.getChildren()[0] instanceof IASTUnaryExpression) {
                IASTUnaryExpression expr = (IASTUnaryExpression) body.getChildren()[0];
                return ASTPatternUtil.getIncrDecr(expr) != null;
            }
        }
        return false;
    }

    public List<IASTForStatement> getPerfectlyNestedLoops() {
        return getPerfectlyNestedLoops(statement);
    }

    private static List<IASTForStatement> getPerfectlyNestedLoops(IASTForStatement outerLoop) {
        List<IASTForStatement> result = new LinkedList<IASTForStatement>();
        result.add(outerLoop);

        if (outerLoop.getBody() instanceof IASTCompoundStatement) {
            IASTNode[] children = outerLoop.getBody().getChildren();
            if (children.length == 1 && children[0] instanceof IASTForStatement) {
                result.addAll(getPerfectlyNestedLoops((IASTForStatement) children[0]));
            }
        } else if (outerLoop.getBody() instanceof IASTForStatement) {
            result.addAll(getPerfectlyNestedLoops((IASTForStatement) outerLoop.getBody()));
        }
        return result;
    }

    /**
     * Returns whether or not the loop and all of its subloops are perfectly nested 
     * 
     * @return IBinding
     */
    public boolean isPerfectLoopNest() {
        return isPerfectLoopNest(Integer.MAX_VALUE, statement);
    }
    
    /**
     * Returns whether or not the loop and its subloops are perfectly nested up to the given depth
     * 
     * @param depth A single loop with no nesting is considered to have depth 0
     * @return IBinding
     */
    public boolean isPerfectLoopNest(int depth) {
        return isPerfectLoopNest(depth, statement);
    }

    private boolean isPerfectLoopNest(int depth, IASTForStatement outerLoop) {
        if (!doesForLoopContainForLoopChild(outerLoop)) {
            return true;
        }
        if(depth == 0) {
            return true;
        }

        if (outerLoop.getBody() instanceof IASTCompoundStatement) {
            IASTNode[] children = outerLoop.getBody().getChildren();
            if (children.length == 1 && children[0] instanceof IASTForStatement) {
                return isPerfectLoopNest(depth - 1, (IASTForStatement) children[0]);
            } else {
                return false;
            }
        } else {
            return isPerfectLoopNest(depth - 1, (IASTForStatement) outerLoop.getBody());
        }
    }

    private boolean doesForLoopContainForLoopChild(IASTForStatement loop) {
        class Visitor extends ASTVisitor {
            public Visitor() {
                shouldVisitStatements = true;
            }

            @Override
            public int visit(IASTStatement statement) {
                if (statement instanceof IASTForStatement) {
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }
        }

        // If we've aborted, it's because we found a for statement
        return !loop.getBody().accept(new Visitor());
    }

    /**
     * Method returns the number in which a loop iterates by.
     * 
     * @param depth
     *            -- loop depth in which to query
     * @return integer describing linear loop's iterator returns -1 if invalid depth
     */
    public int getIterationFactor(int depth) {
        IASTForStatement header = ASTUtil.findDepth(statement, IASTForStatement.class, depth);
        if (header == null) {
            return -1;
        }
        IASTExpression iterationExpression = header.getIterationExpression();

        // Based on our accepted patterns, the only literal in this expression
        // should be how the linear iteration is depicted. For now, find the
        // only literal expression in this and return it.
        IASTLiteralExpression literal = ASTUtil.findFirst(iterationExpression, IASTLiteralExpression.class);
        if (literal != null) {
            return Integer.parseInt(new String(literal.getValue()));
        } else {
            // Otherwise, return one. Generic iterator.
            return 1;
        }
    }
    
    public int getIterationFactor() {
        return getIterationFactor(0);
    }
}
