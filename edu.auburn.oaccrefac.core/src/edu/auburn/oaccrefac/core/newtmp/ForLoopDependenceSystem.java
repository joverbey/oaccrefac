package edu.auburn.oaccrefac.core.newtmp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.Matrix;

/**
 * @author Alexander Calvert
 * 
 * Class to encapsulate a for loop data dependence system
 *
 * TODO: finish constructor (see other todo's for new fields, etc.)
 * TODO: this class contains the inequality matrix for the loop system, but 
 *  FMDepTest generates it from array info internally from loop bounds, etc.
 *  Either this class should keep loop bounds, etc. instead of the matrix, 
 *  or FMDepTest should take the matrix as an argument rather than loop bounds, etc.
 *  This would, however, mess with the IDependenceTest interface. 
 *  
 *  int[] lowerBounds, int[] upperBounds, int[][] writeCoefficients, int[][] readCoefficients
 *  
 *
 */
@SuppressWarnings({"restriction", "unused"})
public class ForLoopDependenceSystem {

    private static final int DEP_EXISTS = 0;
    private static final int DEP_DOESNT_EXIST = 1;
    private static final int DEP_MIGHT_EXIST = 2;
    
    private final Matrix inequalityMatrix = null;
    private final DataDependence[] dependences = null;

    /** 
     * Constructor
     * takes a for statement in, setting up this instance's dependence 
     * dependence system of equations if it is valid 
     * 
     * @param outerLoop the outer for loop to be 
     */
    public ForLoopDependenceSystem(CPPASTForStatement outerLoop) {
        //consolidate most of the original code from Calvin's ForLoopDependence
        //to this constructor
        //should check for valid/invalid for statements and bodies and generate both
        //matrices to represent the dependences in this loop
        
        //check if loop is perfectly nested
        if(!isForLoopPerfectlyNested(outerLoop)) {
            ASTUtil.raise("Loop must be perfectly nested", outerLoop);
        }
        
        //check if only statements on innermost loop are assignments
        if(!areAllInnermostStatementsValid(outerLoop)) {
            ASTUtil.raise("Innermost statements must be assignments (or null statements)", outerLoop);
        }
        
        
        
        
        
    }

    public Matrix getInequalityMatrix() {
        return inequalityMatrix;
    }

    public DataDependence[] getDependences() {
        return dependences;
    }

    /**   
     *  return a particular dependence within this system
     * 
     * @param statement the statement the dependence exists on
     * @return the dependence
     */
    public DataDependence getDependence(IASTExpressionStatement statement1, IASTExpressionStatement statement2, DependenceType type) {
        for(DataDependence dependence : dependences) {
            //checking for for object equivalence rather than just separate identical objects should be enough - 
            //the appropriate statement being used should be the same node on the same tree
            if(dependence.equals(statement1) && dependence.equals(statement2) && dependence.getType() == type) {
                return dependence;
            }
        }
        throw new IllegalArgumentException("Statement does not exist within loop system");
    }
    
    private void p(Object o) {
        System.out.println(o);
    }
    
    /* ****************************************************************
     * Methods to perform checks on incoming loop in constructor to ensure it meets
     * the qualifications for this dependence analysis
     * ****************************************************************/
    private boolean isForLoopPerfectlyNested(CPPASTForStatement outerLoop) {
        IASTNode[] loopChildren = outerLoop.getChildren();
        if(!doesForLoopContainForLoopChild(outerLoop)) {            
            return true;
        }
        else { //outerLoop has for loop children
            if(outerLoop.getBody() instanceof IASTCompoundStatement) {
                IASTNode[] children = outerLoop.getBody().getChildren();
                if(children.length == 1 && children[0] instanceof CPPASTForStatement) {
                    return isForLoopPerfectlyNested((CPPASTForStatement) children[0]);
                }
                else {
                    return false; 
                }
            }
            else {
                return isForLoopPerfectlyNested((CPPASTForStatement) outerLoop.getBody());
            }
        }
    }

    private boolean doesForLoopContainForLoopChild(CPPASTForStatement loop) {
        
        class Visitor extends ASTVisitor {

            public Visitor() {
                shouldVisitStatements = true;
            }

            @Override
            public int visit(IASTStatement statement) {
                if(statement instanceof CPPASTForStatement) {
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }
        }
        //if we've aborted, it's because we found a for statement
        return !loop.getBody().accept(new Visitor());

    }
    
    /** Assumes loops are perfectly nested
     * Checks if all innermost statements are valid
     *  currently, a valid statement is either an assignment statement or null
     * @param outerLoop
     * @return
     */
    private boolean areAllInnermostStatementsValid(CPPASTForStatement outerLoop) {
        IASTNode body = outerLoop.getBody();
        if(body instanceof IASTCompoundStatement) {
            p("compound stmt body");
            if(body.getChildren().length == 0) {
                p("body has no children");
                return true;
            }
            //assuming perfect nesting, so only check children[0]
            else if(body.getChildren()[0] instanceof CPPASTForStatement) {
                p("body has a for stmt child, calling recursively");
                return areAllInnermostStatementsValid((CPPASTForStatement) body.getChildren()[0]);
            }
            else {
                //check if all children are assignments or null stmts
                for(IASTNode child : body.getChildren()) {
                    //to be an asgt, must be an expr stmt with a bin expr child, 
                    //which has asgt operator
                    if(child instanceof IASTExpressionStatement 
                            && child.getChildren().length > 0
                            && child.getChildren()[0] instanceof IASTBinaryExpression
                            && ((IASTBinaryExpression) child.getChildren()[0]).getOperator() == IASTBinaryExpression.op_assign) {
                        
                        p("found binex child");
                       continue;
                    }
                    else if(child instanceof IASTNullStatement) {
                        p("found null child");
                        continue;
                    }
                    else {
                        p("child is not an assignment or null");
                        return false;
                    }
                }
                return true;
            }
        }
        else if(body instanceof CPPASTForStatement) {
            p("simple body, for loop, calling recursively");
            return areAllInnermostStatementsValid((CPPASTForStatement) body);
        }
        else { //neither compound nor for statement - body is the only statement
            p("simple body, non-for loop");
            if(body instanceof IASTBinaryExpression) {
                if(((IASTBinaryExpression) body).getOperator() == IASTBinaryExpression.op_assign) {
                    p("only stmt is a asgt");
                    return true;
                }
            }
            //either not binary or not an assignment
            p("only stmt is not asgt");
            return false;
        }
    }    
    
    
    /** TODO: get nesting level from loop somehow (maybe in constructor)
     *  TODO: get other arguments for the tester; probably also from constructor
     *  TODO: if test() method changes to take a DirectionVector arg, change this as well
     *  
     * @return a set containing all directions in which there might be a dependence
     */
    
    public Set<DirectionVector> getPossibleDependenceDirections() {
        
        int nestingLevel = 4;
        DirectionVector allDirections = new DirectionVector(Direction.ANY, nestingLevel);
        
        Set<DirectionVector> results = new HashSet<DirectionVector>();
        
        return getPossibleDependenceDirections(allDirections, results);
        
    }
    private Set<DirectionVector> getPossibleDependenceDirections(DirectionVector dv, Set<DirectionVector> results) {
        
        /*
         * run on given direction vector
         * if there is no dependence,
         *  return empty list 
         * otherwise
         *  add given direction vector to the list
         *  get new direction vectors where all initial non-'*' elements 
         *      are the same and the first '*' is replaced with '<'/'='/'>'
         *  recursively run on each of three new direction vectors, adding the result to the list
         * 
         */
        
        FourierMotzkinDependenceTest fourierMotzkin = new FourierMotzkinDependenceTest();
        
        int[] lowerBounds = {};
        int[] upperBounds = {};
        int[][] writeCoefficients = {{}};
        int[][] readCoefficients = {{}};
        Direction[] dvEls = dv.getElements();
        
        //if there is no dependence
        if(fourierMotzkin.test(lowerBounds, upperBounds, writeCoefficients, readCoefficients, dvEls)) {
            return results;
        }
        else {
            
            Direction[] originalVector = dv.getElements();                        
            int firstAny = Arrays.asList(originalVector).indexOf(Direction.ANY);

            //if we have a dependence, but this vector is at the 
            //bottom of the hierarchy (no '*' element)
            if(firstAny < 0) {
                results.add(dv);
                return results;
            }
            else {
                Direction[] newLTEls = Arrays.copyOf(originalVector, originalVector.length);
                Direction[] newGTEls = Arrays.copyOf(originalVector, originalVector.length);
                Direction[] newEQEls = Arrays.copyOf(originalVector, originalVector.length);
                newLTEls[firstAny] = Direction.LT;
                newGTEls[firstAny] = Direction.GT;
                newEQEls[firstAny] = Direction.EQ;
                DirectionVector newLT = new DirectionVector(newLTEls);
                DirectionVector newGT = new DirectionVector(newGTEls);
                DirectionVector newEQ = new DirectionVector(newEQEls);
                
                results.addAll(getPossibleDependenceDirections(newLT, results));
                results.addAll(getPossibleDependenceDirections(newGT, results));
                results.addAll(getPossibleDependenceDirections(newEQ, results));
                
                return results;
                
            }
        }        
        
    }
    
    
}
