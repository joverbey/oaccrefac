package edu.auburn.oaccrefac.core.dataflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.codan.core.model.cfg.IBasicBlock;
import org.eclipse.cdt.codan.core.model.cfg.ICfgData;
import org.eclipse.cdt.codan.core.model.cfg.IControlFlowGraph;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

@SuppressWarnings("restriction")
public class ReachingDefinitions { 

    private IControlFlowGraph cfg;
    
    private Map<IBasicBlock, RDVarSet> entrySets;
    private Map<IBasicBlock, RDVarSet> exitSets;
    
    public ReachingDefinitions(IASTFunctionDefinition func) {
        this.cfg = new ControlFlowGraphBuilder().build(func);
        
        this.entrySets = new HashMap<IBasicBlock, RDVarSet>();
        this.exitSets = new HashMap<IBasicBlock, RDVarSet>();
        
//        System.out.println(ASTUtil.isNameInScope("a", func.getScope()));
        
        identifyReachingDefinitions(cfg);
        
    }

    public Set<IASTNode> reachingDefinitions(IASTName varUse) {
        //use should be a block, since that is what can be "reached" by a definition
        //returns a list of definitions of variable that reach the given use
        IBinding variable = varUse.resolveBinding();
        //get block corresponding to use
        RDVarSet entrySet = null;
        for(IBasicBlock bb : entrySets.keySet()) {
            Object data = ((ICfgData) bb).getData();
            if (data != null && data instanceof IASTNode && ASTUtil.isAncestor((IASTNode) data, varUse)) {
                entrySet = entrySets.get(bb);
            }
        }
        if(entrySet == null) {
            //the use node had no entry set, either because there are no reaching defs or because it wasn't a valid block node
            return new HashSet<IASTNode>();
        }
        
        return entrySet.get(variable) == null ? new HashSet<IASTNode>() : entrySet.get(variable);
    }
    
    private void identifyReachingDefinitions(IControlFlowGraph cfg) {
        boolean changed;
//        for(IBasicBlock bb : cfg.getNodes()) {
//            System.out.println(bb);
//            System.out.println("Preds: ");
//            for(IBasicBlock pred : bb.getIncomingNodes()) {
//                System.out.println("\t" + pred);
//            }
//        }
        do {
            changed = false;
            
            Map<IBasicBlock, RDVarSet> newEntrySets = new HashMap<IBasicBlock, RDVarSet>();
            Map<IBasicBlock, RDVarSet> newExitSets = new HashMap<IBasicBlock, RDVarSet>();
            
            for (IBasicBlock bb : cfg.getNodes()) {
                
                RDVarSet bbEntry = new RDVarSet();
                RDVarSet bbExit = new RDVarSet();

                for(IBasicBlock pred : bb.getIncomingNodes()) {
                    if(exitSets.containsKey(pred)) {
                        bbEntry.union(exitSets.get(pred));
                    }
                }

                if(entrySets.containsKey(bb)) {
                    bbExit.union(entrySets.get(bb));
                }
                for(IBinding write : varsWrittenToIn(bb)) {
                    bbExit.killAll(write);
                }
                for(IBinding write : varsWrittenToIn(bb)) {
                    Object data = ((ICfgData) bb).getData();
                    if (data != null && data instanceof IASTNode) {
                        bbExit.add(write, (IASTNode) data);
                    }
                }
                
                changed = changed || !areEqual(bbEntry, entrySets.get(bb)) || !areEqual(bbExit, exitSets.get(bb));
                
                newEntrySets.put(bb, bbEntry);
                newExitSets.put(bb, bbExit);
            }
            
            for(IBasicBlock bb : newEntrySets.keySet()) {
                entrySets.put(bb, newEntrySets.get(bb));
            }
            for(IBasicBlock bb : newExitSets.keySet()) {
                exitSets.put(bb, newExitSets.get(bb));
            }
            
        } while (changed);
        return;
    }
    
//    //TODO finish this?
//    //use should be either an IASTStatement or an IASTExpression
//    public boolean reaches(IASTName definition, IASTNode use) {
//        
//        if(!isDefinition(definition)) {
//            throw new IllegalArgumentException("Definition given is not a variable definition");
//        }
//        
//        IBasicBlock useBB = null;
//        for(IBasicBlock bb : entrySets.keySet()) {
//            Object data = ((ICfgData) bb).getData();
//            if (data != null && data instanceof IASTNode && data.equals(use)) {
//                useBB = bb; 
//                break;
//            }
//        }
//        
//        if(useBB == null) {
//            // The use node given is not represented by any block
//            // (it should be either an IASTStatement or, in a few cases, an IASTExpression)
//            throw new IllegalArgumentException("No block for the node");
//        }
//        
//        RDVarSet entrySetForBlock = entrySets.get(useBB);
//        if (entrySetForBlock == null) {
//            // If we get here, we found the right block for the given use, but there is no set of
//            // definitions that reach it. This should not be able to happen.
//            throw new IllegalStateException("No entry set for use or no definitions reaching the use");
//        }
//        
//        //all definitions of the variable represented by the argument called "definition" that reach the block represented by the argument "use"
//        Set<IASTNode> defsOfDefThatReachBlock = entrySetForBlock.get(definition.resolveBinding());
//        if (defsOfDefThatReachBlock == null) {
//            return false;
//        }
//        
//        IASTStatement defStmt = ASTUtil.findNearestAncestor(definition, IASTStatement.class);
//        IASTUnaryExpression defUnaryExpr = ASTUtil.findNearestAncestor(definition, IASTUnaryExpression.class);
//        IASTBinaryExpression defBinaryExpr = ASTUtil.findNearestAncestor(definition, IASTBinaryExpression.class);
//        /**
//         * TODO add a check for scope here i.e., in: <code>
//         * for(int i = 0; i < 10; i++) {
//         *     for(int j = 0; j < 10; j++) {
//         *     }
//         * }
//         * </code> the definition at <int j = 0> is said to reach the first line of the outer loop; the CFG
//         * gives that impression, but scoping actually disallows this.
//         * 
//         */
//        return defsOfDefThatReachBlock.contains(defStmt) || defsOfDefThatReachBlock.contains(defUnaryExpr)
//                || defsOfDefThatReachBlock.contains(defBinaryExpr);
//
//    }
    
    private boolean isDefinition(IASTName name) {
        //should correspond to varsWrittenToIn on our definition of a "definition"
        IASTStatement defStmt = ASTUtil.findNearestAncestor(name, IASTStatement.class);
        IASTUnaryExpression defUnaryExpr = ASTUtil.findNearestAncestor(name, IASTUnaryExpression.class);
        IASTBinaryExpression defBinaryExpr = ASTUtil.findNearestAncestor(name, IASTBinaryExpression.class);
        if(defStmt instanceof IASTDeclarationStatement) {
            if(((IASTDeclarationStatement) defStmt).getDeclaration() instanceof IASTSimpleDeclaration) {
                IASTSimpleDeclaration simpleDec = (IASTSimpleDeclaration) (((IASTDeclarationStatement) defStmt).getDeclaration());
                //int x; int x, y; int x = 1;
                for(IASTDeclarator decl : simpleDec.getDeclarators()) {
                    if(decl.getName().equals(name)) {
                        return true;
                    }
                }
            }
        }
        else {
            if((defStmt instanceof IASTExpressionStatement && ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTUnaryExpression) || defUnaryExpr != null) {
                //x++; x--; ++x; --x;
                IASTUnaryExpression unary;
                if((defStmt instanceof IASTExpressionStatement && ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTUnaryExpression)) {
                    unary = (IASTUnaryExpression) (((IASTExpressionStatement) defStmt).getExpression());
                }
                else {
                    unary = defUnaryExpr;
                }
                if(unary.getOperand() instanceof IASTIdExpression && 
                        (unary.getOperator() == IASTUnaryExpression.op_postFixIncr || 
                        unary.getOperator() == IASTUnaryExpression.op_prefixIncr ||
                        unary.getOperator() == IASTUnaryExpression.op_postFixDecr ||
                        unary.getOperator() == IASTUnaryExpression.op_prefixIncr)) {
                    if(((IASTIdExpression) unary.getOperand()).getName().equals(name)) {
                        return true;
                    }
                }
                else if(unary.getOperand() instanceof IASTArraySubscriptExpression) {
                    IASTArraySubscriptExpression arrSubExpr = (IASTArraySubscriptExpression) unary.getOperand();
                    if(arrSubExpr.getArrayExpression() instanceof IASTIdExpression) {
                        if(((IASTIdExpression) arrSubExpr.getArrayExpression()).getName().equals(name)) {
                            return true;
                        }
                    }
                }
            }
            
            if((defStmt instanceof IASTExpressionStatement && ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTBinaryExpression) || defBinaryExpr != null) {
                //x = 1; x += 1; ...
                IASTBinaryExpression binary;
                if((defStmt instanceof IASTExpressionStatement && ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTUnaryExpression)) {
                    binary = (IASTBinaryExpression) (((IASTExpressionStatement) defStmt).getExpression());
                }
                else {
                    binary = defBinaryExpr;
                }
                if(binary.getOperand1() instanceof IASTIdExpression && 
                        (binary.getOperator() == IASTBinaryExpression.op_assign ||
                        binary.getOperator() == IASTBinaryExpression.op_binaryAndAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_binaryOrAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_binaryXorAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_divideAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_minusAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_moduloAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_multiplyAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_plusAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_shiftLeftAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_shiftRightAssign)) {
                    if(((IASTIdExpression) binary.getOperand1()).getName().equals(name)) {
                        return true;
                    }
                }
                else if(binary.getOperand1() instanceof IASTArraySubscriptExpression) {
                    IASTArraySubscriptExpression arrSubExpr = (IASTArraySubscriptExpression) binary.getOperand1();
                    if(arrSubExpr.getArrayExpression() instanceof IASTIdExpression) {
                        if(((IASTIdExpression) arrSubExpr.getArrayExpression()).getName().equals(name)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private List<IBinding> varsWrittenToIn(IBasicBlock bb) {
        List<IBinding> writeAccesses = new ArrayList<IBinding>();
        Object data = ((ICfgData) bb).getData();
        if (data == null || !(data instanceof IASTNode))
            return writeAccesses;
        
        if(data instanceof IASTDeclarationStatement) {
            if(((IASTDeclarationStatement) data).getDeclaration() instanceof IASTSimpleDeclaration) {
                //int x; int x, y; int x = 1;
                for(IASTDeclarator dec : ((IASTSimpleDeclaration) (((IASTDeclarationStatement) data).getDeclaration())).getDeclarators()) {
                    writeAccesses.add(dec.getName().resolveBinding());
                }
            }
        }
        else if(data instanceof IASTExpressionStatement || data instanceof IASTExpression) {
            IASTExpression expr;
            if(data instanceof IASTExpressionStatement) {
                expr = ((IASTExpressionStatement) data).getExpression();
            }
            else /*if(data instanceof IASTExpression)*/ {
                expr = (IASTExpression) data;
            }
            if(expr instanceof IASTUnaryExpression) {
                //x++; x--; ++x; --x;
                IASTUnaryExpression unary = (IASTUnaryExpression) expr;
                if(unary.getOperand() instanceof IASTIdExpression && 
                        (unary.getOperator() == IASTUnaryExpression.op_postFixIncr || 
                        unary.getOperator() == IASTUnaryExpression.op_prefixIncr ||
                        unary.getOperator() == IASTUnaryExpression.op_postFixDecr ||
                        unary.getOperator() == IASTUnaryExpression.op_prefixIncr)) {
                    writeAccesses.add(((IASTIdExpression) (unary.getOperand())).getName().resolveBinding());
                }
            }
            //x = 1; x += 1; ...
            else if(expr instanceof IASTBinaryExpression) {
                IASTBinaryExpression binary = (IASTBinaryExpression) expr;
                if(binary.getOperand1() instanceof IASTIdExpression && 
                        (binary.getOperator() == IASTBinaryExpression.op_assign ||
                        binary.getOperator() == IASTBinaryExpression.op_binaryAndAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_binaryOrAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_binaryXorAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_divideAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_minusAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_moduloAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_multiplyAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_plusAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_shiftLeftAssign ||
                        binary.getOperator() == IASTBinaryExpression.op_shiftRightAssign)) {
                    writeAccesses.add(((IASTIdExpression) ((IASTBinaryExpression) expr).getOperand1()).getName().resolveBinding());
                }
                else if(binary.getOperand1() instanceof IASTArraySubscriptExpression) {
                    if(((IASTArraySubscriptExpression) binary.getOperand1()).getArrayExpression() instanceof IASTIdExpression) {
                        writeAccesses.add(((IASTIdExpression) ((IASTArraySubscriptExpression) binary.getOperand1()).getArrayExpression()).getName().resolveBinding());
                    }
                }
            }
        }
        return writeAccesses;
    }
    
    private boolean areEqual(RDVarSet first, RDVarSet second) {
        
        if(first == null && second == null) {
            return true;
        }
        else if(first == null || second == null) {
            return false;
        }
        
        if(first.isEmpty() && second.isEmpty()) {
            return true;
        }
        else if(first.isEmpty() || second.isEmpty()) {
            return false;
        }
        
        for(IBinding var : first.getMap().keySet()) {
            if(second.getMap().containsKey(var)) {
                for(IASTNode statement : first.getMap().get(var)) {
                    if(!second.getMap().get(var).contains(statement)) {
                        return false;
                    }
                }
            }
            else {
                return false;
            }
        }
        for(IBinding var : second.getMap().keySet()) {
            if(first.getMap().containsKey(var)) {
                for(IASTNode statement : second.getMap().get(var)) {
                    if(!first.getMap().get(var).contains(statement)) {
                        return false;
                    }
                }
            }
            else {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IBasicBlock bb : cfg.getNodes()) {
            Object data = ((ICfgData) bb).getData();
            if (data != null && data instanceof IASTNode) {
                sb.append(((IASTNode) data).getRawSignature() + " at "
                        + ((IASTNode) data).getFileLocation().getStartingLineNumber() + ":");
                // }
                // else {
                // sb.append(bb + ":");
                // }
                sb.append(System.lineSeparator());

                sb.append("\tEntries: ");
                if (entrySets.containsKey(bb)) {
                    sb.append(entrySets.get(bb));
                }

                sb.append(System.lineSeparator());

                sb.append("\tExits: ");
                if (exitSets.containsKey(bb)) {
                    sb.append(exitSets.get(bb));
                }

                sb.append(System.lineSeparator());
            }

        }
        return sb.toString();
    }
    
}
