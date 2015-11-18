package edu.auburn.oaccrefac.core.dataflow;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

@SuppressWarnings("restriction")
public class OpenACCReachingDefinitions { 

    private IControlFlowGraph cfg;
    
    private Map<VariableAccess, Set<IASTStatement>> reachingDefs;
    private Map<IBasicBlock, RDVarSet> entrySets;
    private Map<IBasicBlock, RDVarSet> exitSets;
    
    public OpenACCReachingDefinitions(IASTFunctionDefinition func) throws DependenceTestFailure {
        this.cfg = new ControlFlowGraphBuilder().build(func);
        
        this.reachingDefs = new HashMap<VariableAccess, Set<IASTStatement>>();
        this.entrySets = new HashMap<IBasicBlock, RDVarSet>();
        this.exitSets = new HashMap<IBasicBlock, RDVarSet>();
        
        identifyReachingDefinitions(cfg);
        
    }
    
    private void identifyReachingDefinitions(IControlFlowGraph cfg) throws DependenceTestFailure {
        boolean changed;
        for(IBasicBlock bb : cfg.getNodes()) {
            System.out.println(bb);
            System.out.println("Preds: ");
            for(IBasicBlock pred : bb.getIncomingNodes()) {
                System.out.println("\t" + pred);
            }
        }
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
    }
    
    //FIXME finish this
    public boolean reaches(IASTStatement definition, VariableAccess use) {
        for(IBasicBlock bb : entrySets.keySet()) {
            Object data = ((ICfgData) bb).getData();
            if (data == null || !(data instanceof IASTNode)) {
                return false;
            }
            if(data.equals(use.getEnclosingStatement())) {
                return entrySets.get(bb).get(use.getVariableName().resolveBinding()).contains(definition);
            }
        }
        return true;
    }
    
    //TODO be sure the collectAccesses methods support everything or use some other means to kill/gens
    private List<IBinding> varsWrittenToIn(IBasicBlock bb) throws DependenceTestFailure {
        List<IBinding> writeAccesses = new ArrayList<IBinding>();
        Object data = ((ICfgData) bb).getData();
        if (data == null || !(data instanceof IASTNode))
            return writeAccesses;
        
        if(data instanceof IASTDeclarationStatement) {
            if(((IASTDeclarationStatement) data).getDeclaration() instanceof IASTSimpleDeclaration) {
                //int x; int x, y; int x = 1;
                for(IASTDeclarator dec : ((IASTSimpleDeclaration) ((IASTDeclarationStatement) data).getDeclaration()).getDeclarators()) {
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
