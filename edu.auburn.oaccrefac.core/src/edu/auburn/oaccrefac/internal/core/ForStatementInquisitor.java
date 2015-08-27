/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
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
package edu.auburn.oaccrefac.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryIntegerConstant;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

public class ForStatementInquisitor {

    public static ForStatementInquisitor getInquisitor(IASTForStatement statement) {
        return new ForStatementInquisitor(statement);
    }

    private final IASTForStatement statement;

    public IASTForStatement getStatement() {
        return statement;
    }

    private boolean counted;

    // Patterns of for loops that are acceptable to refactor...
    private static String[] patterns = {
            // Constant upper bound
            "for (i = 0; i < 1; i++) ;", "for (int i = 0; i < 1; i++) ;", "for (i = 0; i <= 1; i++) ;",
            "for (int i = 0; i <= 1; i++) ;", "for (i = 0; i < 1; i+=1) ;", "for (int i = 0; i < 1; i+=1) ;",
            "for (i = 0; i <= 1; i+=1) ;", "for (int i = 0; i <= 1; i+=1) ;", "for (i = 0; i < 1; i=i+1) ;",
            "for (int i = 0; i < 1; i=i+1) ;", "for (i = 0; i <= 1; i=i+1) ;", "for (int i = 0; i <= 1; i=i+1) ;",
            // Variable upper bound
            "for (i = 0; i < j; i++) ;", "for (int i = 0; i < j; i++) ;", "for (i = 0; i <= j; i++) ;",
            "for (int i = 0; i <= j; i++) ;", "for (i = 0; i < j; i+=1) ;", "for (int i = 0; i < j; i+=1) ;",
            "for (i = 0; i <= j; i+=1) ;", "for (int i = 0; i <= j; i+=1) ;", "for (i = 0; i < j; i=i+1) ;",
            "for (int i = 0; i < j; i=i+1) ;", "for (i = 0; i <= j; i=i+1) ;", "for (int i = 0; i <= j; i=i+1) ;",
            // Simple field reference upper bound (used in Livermore Loops)
            "for (i = 0; i < j.k; i++) ;", "for (int i = 0; i < j.k; i++) ;", "for (i = 0; i <= j.k; i++) ;",
            "for (int i = 0; i <= j.k; i++) ;", "for (i = 0; i < j.k; i+=1) ;", "for (int i = 0; i < j.k; i+=1) ;",
            "for (i = 0; i <= j.k; i+=1) ;", "for (int i = 0; i <= j.k; i+=1) ;", "for (i = 0; i < j.k; i=i+1) ;",
            "for (int i = 0; i < j.k; i=i+1) ;", "for (i = 0; i <= j.k; i=i+1) ;",
            "for (int i = 0; i <= j.k; i=i+1) ;", };

    private ForStatementInquisitor(IASTForStatement statement) {
        this.statement = statement;

        // cache whether or not the loop is counted for performance
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
        boolean countedInitialized = false;
        for (String pattern : patterns) {
            IASTForStatement orig = (IASTForStatement) ASTUtil.parseStatementNoFail(pattern);
            IASTForStatement patternAST = orig.copy(CopyStyle.withoutLocations);
            patternAST.accept(new LiteralReplacer());
            patternAST.setBody(new ArbitraryStatement());
            if (ASTMatcher.unify(patternAST, statement) != null) {
                counted = true;
                countedInitialized = true;
                break;
            }
        }
        if (!countedInitialized) {
            counted = false;
        }
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
     * @return
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

    // FIXME fails to handle cases where the lower bound is not 0
    public int getLowerBound() {
        return 0;
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
     * @return
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
     * Assumes loops are perfectly nested Checks if all innermost statements are valid currently, a valid statement is
     * either an assignment statement or null
     * 
     * @param outerLoop
     * @return
     */
    public boolean areAllInnermostStatementsValid() {
        return areAllInnermostStatementsValid(statement);
    }

    private boolean areAllInnermostStatementsValid(IASTForStatement outerLoop) {
        IASTNode body = outerLoop.getBody();
        if (body instanceof IASTCompoundStatement) {
            if (body.getChildren().length == 0) {
                return true;
            }
            // assuming perfect nesting, so only check children[0]
            else if (body.getChildren()[0] instanceof IASTForStatement) {
                return areAllInnermostStatementsValid((IASTForStatement) body.getChildren()[0]);
            } else {
                // check if all children are assignments or null stmts
                for (IASTNode child : body.getChildren()) {
                    // to be an asgt, must be an expr stmt with a bin expr child,
                    // which has asgt operator
                    if (child instanceof IASTExpressionStatement && child.getChildren().length > 0
                            && child.getChildren()[0] instanceof IASTBinaryExpression
                            && ((IASTBinaryExpression) child.getChildren()[0])
                                    .getOperator() == IASTBinaryExpression.op_assign) {
                        continue;
                    } else if (child instanceof IASTNullStatement) {
                        continue;
                    } else {
                        return false;
                    }
                }
                return true;
            }
        } else if (body instanceof IASTForStatement) {
            return areAllInnermostStatementsValid((IASTForStatement) body);
        } else if (body instanceof IASTNullStatement) {
            return true;
        } else { // neither compound nor for statement - body is the only statement
            if (body instanceof IASTExpressionStatement) {
                if (body.getChildren()[0] instanceof IASTBinaryExpression) {
                    if (((IASTBinaryExpression) body.getChildren()[0])
                            .getOperator() == IASTBinaryExpression.op_assign) {
                        return true;
                    }
                }
            }
            // either not binary or not an assignment
            return false;
        }
    }

    public List<IASTForStatement> getPerfectLoopNestHeaders() {
        return getPerfectLoopNestHeaders(statement);
    }

    private static List<IASTForStatement> getPerfectLoopNestHeaders(IASTForStatement outerLoop) {
        List<IASTForStatement> result = new LinkedList<IASTForStatement>();
        result.add(outerLoop);

        if (outerLoop.getBody() instanceof IASTCompoundStatement) {
            IASTNode[] children = outerLoop.getBody().getChildren();
            if (children.length == 1 && children[0] instanceof IASTForStatement) {
                result.addAll(getPerfectLoopNestHeaders((IASTForStatement) children[0]));
            }
        } else if (outerLoop.getBody() instanceof IASTForStatement) {
            result.addAll(getPerfectLoopNestHeaders((IASTForStatement) outerLoop.getBody()));
        }
        return result;
    }

    public boolean isPerfectLoopNest() {
        return isPerfectLoopNest(statement);
    }

    private boolean isPerfectLoopNest(IASTForStatement outerLoop) {
        if (!doesForLoopContainForLoopChild(outerLoop)) {
            return true;
        }

        if (outerLoop.getBody() instanceof IASTCompoundStatement) {
            IASTNode[] children = outerLoop.getBody().getChildren();
            if (children.length == 1 && children[0] instanceof IASTForStatement) {
                return isPerfectLoopNest((IASTForStatement) children[0]);
            } else {
                return false;
            }
        } else {
            return isPerfectLoopNest((IASTForStatement) outerLoop.getBody());
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
        IASTLiteralExpression literal = ASTUtil.findOne(iterationExpression, IASTLiteralExpression.class);
        if (literal != null) {
            return Integer.parseInt(new String(literal.getValue()));
        } else {
            // Otherwise, return one. Generic iterator.
            return 1;
        }
    }

    public List<IASTPreprocessorPragmaStatement> getLeadingPragmas() {
        int loopLoc = statement.getFileLocation().getNodeOffset();
        int precedingStmtOffset = getNearestPrecedingStatementOffset(statement);
        List<IASTPreprocessorPragmaStatement> pragmas = new ArrayList<IASTPreprocessorPragmaStatement>();
        for (IASTPreprocessorStatement pre : statement.getTranslationUnit().getAllPreprocessorStatements()) {
            if (pre instanceof IASTPreprocessorPragmaStatement
                    && ((IASTPreprocessorPragmaStatement) pre).getFileLocation().getNodeOffset() < loopLoc
                    && ((IASTPreprocessorPragmaStatement) pre).getFileLocation()
                            .getNodeOffset() > precedingStmtOffset) {
                pragmas.add((IASTPreprocessorPragmaStatement) pre);
            }
        }
        Collections.sort(pragmas, new Comparator<IASTPreprocessorPragmaStatement>() {

            @Override
            public int compare(IASTPreprocessorPragmaStatement o1, IASTPreprocessorPragmaStatement o2) {
                return o1.getFileLocation().getNodeOffset() - o2.getFileLocation().getNodeOffset();
            }

        });
        return pragmas;
    }

    private int getNearestPrecedingStatementOffset(IASTStatement stmt) {

        class OffsetFinder extends ASTVisitor {

            // the offset of the nearest lexical predecessor of the given node
            int finalOffset;
            int thisOffset;

            public OffsetFinder(int offset) {
                shouldVisitStatements = true;
                shouldVisitDeclarations = true;
                this.thisOffset = offset;
            }

            @Override
            public int visit(IASTStatement stmt) {
                int foundOffset = stmt.getFileLocation().getNodeOffset();
                if (thisOffset - foundOffset < thisOffset - finalOffset && foundOffset < thisOffset) {
                    this.finalOffset = foundOffset;
                }
                return PROCESS_CONTINUE;
            }

            @Override
            public int visit(IASTDeclaration dec) {
                int foundOffset = dec.getFileLocation().getNodeOffset();
                if (thisOffset - foundOffset < thisOffset - finalOffset && foundOffset < thisOffset) {
                    this.finalOffset = foundOffset;
                }
                return PROCESS_CONTINUE;
            }

        }

        OffsetFinder finder = new OffsetFinder(stmt.getFileLocation().getNodeOffset());
        IASTFunctionDefinition containingFunc = ASTUtil.findNearestAncestor(stmt, IASTFunctionDefinition.class);
        containingFunc.accept(finder);
        return finder.finalOffset;
    }

    public String[] getPragmas() {
        List<IASTPreprocessorPragmaStatement> p = getLeadingPragmas();
        String[] pragCode = new String[p.size()];
        for (int i = 0; i < pragCode.length; i++) {
            pragCode[i] = p.get(i).getRawSignature();
        }
        return pragCode;
    }

}
