package edu.auburn.oaccrefac.core.newtmp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForLoopUtil;
import edu.auburn.oaccrefac.internal.core.fromphotran.DependenceTestFailure;

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
    
    //private final Matrix inequalityMatrix = null;
    private final Set<DataDependence> dependences;

    /** 
     * Constructor
     * takes a for statement in, setting up this instance's dependence 
     * dependence system of equations if it is valid 
     * 
     * @param outerLoop the outer for loop to be 
     * @throws DependenceTestFailure 
     */
    public ForLoopDependenceSystem(CPPASTForStatement outerLoop) throws DependenceTestFailure {
        //consolidate most of the original code from Calvin's ForLoopDependence
        //to this constructor
        //should check for valid/invalid for statements and bodies and generate both
        //matrices to represent the dependences in this loop
        
        //check if loop is perfectly nested
        if(!ForLoopUtil.isPerfectLoopNest(outerLoop)) {
            ASTUtil.raise("Loop must be perfectly nested", outerLoop);
        }
        
        //check if only statements on innermost loop are assignments
        if(!areAllInnermostStatementsValid(outerLoop)) {
            ASTUtil.raise("Innermost statements must be assignments (or null statements)", outerLoop);
        }
        
        this.dependences = new DependenceAnalysis().analyze(ForLoopUtil.getInnermostLoopBody(outerLoop));
        
        
    }

//    public Matrix getInequalityMatrix() {
//        return inequalityMatrix;
//    }

    public Set<DataDependence> getDependences() {
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
    
    /** Assumes loops are perfectly nested
     * Checks if all innermost statements are valid
     *  currently, a valid statement is either an assignment statement or null
     * @param outerLoop
     * @return
     */
    private boolean areAllInnermostStatementsValid(CPPASTForStatement outerLoop) {
        IASTNode body = outerLoop.getBody();
        if(body instanceof IASTCompoundStatement) {
            if(body.getChildren().length == 0) {
                return true;
            }
            //assuming perfect nesting, so only check children[0]
            else if(body.getChildren()[0] instanceof CPPASTForStatement) {
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
                       continue;
                    }
                    else if(child instanceof IASTNullStatement) {
                        continue;
                    }
                    else {
                        return false;
                    }
                }
                return true;
            }
        }
        else if(body instanceof CPPASTForStatement) {
            return areAllInnermostStatementsValid((CPPASTForStatement) body);
        }
        else { //neither compound nor for statement - body is the only statement
            if(body instanceof IASTBinaryExpression) {
                if(((IASTBinaryExpression) body).getOperator() == IASTBinaryExpression.op_assign) {
                    return true;
                }
            }
            //either not binary or not an assignment
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
         *  return list as-is 
         * otherwise
         *  add given direction vector to the list
         *  get new direction vectors where all initial non-'*' elements 
         *      are the same and the first '*' is replaced with '<'/'='/'>'
         *  recursively run on each of three new direction vectors, adding the result to the list
         * 
         */
        
        FourierMotzkinDependenceTester fourierMotzkin = new FourierMotzkinDependenceTester();
        
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
            //bottom of the hierarchy (no '*' element in the vector)
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
