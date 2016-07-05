/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.codan.core.model.cfg.IBasicBlock;
import org.eclipse.cdt.codan.core.model.cfg.ICfgData;
import org.eclipse.cdt.codan.core.model.cfg.IControlFlowGraph;
import org.eclipse.cdt.codan.core.model.cfg.IExitNode;
import org.eclipse.cdt.codan.core.model.cfg.IStartNode;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.EScopeKind;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTPatternUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

@SuppressWarnings("restriction")
public class ReachingDefinitionsAnalysis { 

	private IControlFlowGraph cfg;
    private IASTFunctionDefinition func;
    
    private Map<IBasicBlock, RDVarSet> entrySets;
    private Map<IBasicBlock, RDVarSet> exitSets;
    
    private static Map<IASTFunctionDefinition, ReachingDefinitionsAnalysis> cache = new HashMap<>();
    
    public static ReachingDefinitionsAnalysis forFunction(IASTFunctionDefinition func) {
    	if(!cache.containsKey(func)) {
    		cache.put(func, new ReachingDefinitionsAnalysis(func));
    	}
		return cache.get(func);
	}
    
    private ReachingDefinitionsAnalysis(IASTFunctionDefinition func) {
        this.cfg = new ControlFlowGraphBuilder().build(func);
        this.func = func;
        
        this.entrySets = new HashMap<IBasicBlock, RDVarSet>();
        this.exitSets = new HashMap<IBasicBlock, RDVarSet>();
        
        identifyReachingDefinitions(cfg);
        
    }

    /**
     * Returns a set of all definitions of a variable that reach this occurrence of the variable. If the variable is being
     * defined at this occurrence, it is treated as if no definitions reach it. 
     * @param varUse a use a variable
     * @return a set of <code>IASTNode</code> definitions reaching the variable use
     */
    public Set<IASTName> reachingDefinitions(IASTName varUse) {
        
        //if this occurrence of the var is a definition of var, treat it as if nothing reaches it
        if(ASTPatternUtil.isDefinition(varUse)) {
            return new HashSet<IASTName>();
        }
        
        //use should be a block, since that is what can be "reached" by a definition
        //returns a list of definitions of variable that reach the given use
        IBinding variable = varUse.resolveBinding();
        //get block corresponding to use
        RDVarSet entrySet = null;
        for(IBasicBlock bb : entrySets.keySet()) {
            Object data = ((ICfgData) bb).getData();
            if (data != null && data instanceof IASTNode && ASTUtil.isAncestor(varUse, (IASTNode) data)) {
                entrySet = entrySets.get(bb);
            }
        }
        if(entrySet == null) {
            //the use node had no entry set, either because there are no reaching defs or because it wasn't a valid block node
            return new HashSet<IASTName>();
        }
        
        Set<IASTNode> reachingDefs = entrySet.get(variable);
        if(reachingDefs == null) {
            return new HashSet<IASTName>();
        }
        
        /* get the names from the given nodes that represent the definitions of varUse
         * such as the "x" in "int x = y;" or the "y" in "y++"
         */
        Set<IASTName> reachingDefNames = new HashSet<IASTName>();
        for(IASTNode def : reachingDefs) {
            for(IASTName name : ASTUtil.find(def, IASTName.class)) {
                //if a name found under the definition node is a definition and refers to the right variable
                if((name instanceof Global || isParam(name) || ASTPatternUtil.isDefinition(name)) && name.resolveBinding().equals(varUse.resolveBinding())) {
                    reachingDefNames.add(name);
                }
            }
        }
        return reachingDefNames;
        
    }
    
	/**
     * Given a name which is used as a definition, returns a set of every use of the same variable
     * that the definition reaches. 
     * 
     * @param def the definition (i.e., the <code>x</code> in <code>int x = y;</code> or <code>x++</code>)
     * @return the set of uses reached by <code>def</code>
     */
    public Set<IASTName> reachedUses(IASTName def) {
        
        if(!ASTPatternUtil.isDefinition(def)) {
            return new HashSet<IASTName>();
        }
        
        Set<IASTName> reachedUses = new HashSet<IASTName>();
        
        for(IBasicBlock bb : entrySets.keySet()) {
        	RDVarSet entry = entrySets.get(bb);
        	//if this is a definition found in the entry set
        	if(entry.contains(def)) {
        		Object data = ((ICfgData) bb).getData();
        		if(data instanceof IASTNode) {
        			for(IASTName use : ASTUtil.find(((IASTNode) data), IASTName.class)) {
        				if(use.resolveBinding().equals(def.resolveBinding()) && !ASTPatternUtil.isDefinition(use)) {
        					reachedUses.add(use);
        				}
        			}
        		}
        		if(bb instanceof IExitNode) {
        			if(isNonLocal(def.resolveBinding())) {
        				reachedUses.add(Global.newInstance(def.resolveBinding()));
        			}
        		}
        	}
        }
        
        return reachedUses;
        
    }
    
    /**
     * Returns a set of all definitions that reach occurrences of variables used in the given node. If a variable 
     * is being defined at an occurrence, it is treated as if no definitions reach that occurrence. 
     * @param node a tree node that may or may not contain variable uses
     * @return a set of IASTNode definitions reaching the variable uses in the node
     */
    public Set<IASTName> reachingDefinitions(IASTNode node) {
        Set<IASTName> defs = new HashSet<IASTName>();
        for(IASTName name : ASTUtil.find(node, IASTName.class)) {
            defs.addAll(reachingDefinitions(name));
        }
        return defs;
    }
    
    public Set<IASTName> reachedUses(IASTNode node) {
        Set<IASTName> uses = new HashSet<IASTName>();
        for(IASTName name : ASTUtil.find(node, IASTName.class)) {
            uses.addAll(reachedUses(name));
        }
        return uses;
    }
    
    public Set<IASTName> reachingDefinitions(IASTStatement[] statements) {
        Set<IASTName> defs = new HashSet<IASTName>();
        for(IASTStatement statement : statements) {
            defs.addAll(reachingDefinitions(statement));
        }
        return defs;
    }
    
    public Set<IASTName> reachedUses(IASTStatement[] statements) {
        Set<IASTName> uses = new HashSet<IASTName>();
        for(IASTStatement statement : statements) {
            uses.addAll(reachedUses(statement));
        }
        return uses;
    }
    
    private void identifyReachingDefinitions(IControlFlowGraph cfg) {

        boolean changed;
        do {
            changed = false;
            
            Map<IBasicBlock, RDVarSet> newEntrySets = new HashMap<IBasicBlock, RDVarSet>();
            Map<IBasicBlock, RDVarSet> newExitSets = new HashMap<IBasicBlock, RDVarSet>();
            
            for (IBasicBlock bb : cfg.getNodes()) {
                
                RDVarSet bbEntry = new RDVarSet();
                RDVarSet bbExit = new RDVarSet();
                
                if(bb instanceof IStartNode) {
                	addGlobalDefs(bb, bbEntry);
                	addParams(bb, bbEntry);
                }

                for(IBasicBlock pred : bb.getIncomingNodes()) {
                    if(exitSets.containsKey(pred)) {
                        bbEntry.union(exitSets.get(pred));
                    }
                }

                if(entrySets.containsKey(bb)) {
                    bbExit.union(entrySets.get(bb));
                }
                for(IASTName write : varWritesIn(bb)) {
                	//if writing to an array, don't consider it to kill previous writes
                	IASTArraySubscriptExpression arr = ASTUtil.findNearestAncestor(write, IASTArraySubscriptExpression.class);
                	if(arr == null || !ASTUtil.isAncestor(write, arr.getArrayExpression())) {
                		bbExit.killAll(write.resolveBinding());
                	}
                }
                for(IASTName write : varWritesIn(bb)) {
                    Object data = ((ICfgData) bb).getData();
                    if (data != null && data instanceof IASTNode) {
                        bbExit.add(write.resolveBinding(), (IASTNode) data);
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

    private void addGlobalDefs(IBasicBlock startNode, RDVarSet entries) {
    	for(IASTName ref : ASTUtil.find(func, IASTName.class)) {
    		IBinding binding = ref.resolveBinding();
			if(isNonLocal(binding)) {
				Global fake = Global.newInstance(binding);
				entries.add(binding, fake);
			}
    	}
	}

	private boolean isNonLocal(IBinding binding) {
		try {
			return binding instanceof IVariable && !binding.getScope().getKind().equals(EScopeKind.eLocal);
		} catch (DOMException e) {
			return false;
		}
	}
    
    private void addParams(IBasicBlock startNode, RDVarSet entries) {
    	if(func.getDeclarator() instanceof IASTStandardFunctionDeclarator) {
    		for(IASTParameterDeclaration d : ((IASTStandardFunctionDeclarator) func.getDeclarator()).getParameters()) {
    			entries.add(d.getDeclarator().getName().resolveBinding(), d);
    		}
    	}
    	else if(func.getDeclarator() instanceof ICASTKnRFunctionDeclarator) {
    		for(IASTName n : ((ICASTKnRFunctionDeclarator) func.getDeclarator()).getParameterNames()) {
    			entries.add(n.resolveBinding(), n);
    		}
    	}
	}
    
    private boolean isParam(IASTName name) {
		return name.resolveBinding() instanceof IVariable && ASTUtil.findNearestAncestor(name, IASTFunctionDeclarator.class) != null;
	}

	private List<IASTName> varWritesIn(IBasicBlock bb) {
        List<IASTName> writeAccesses = new ArrayList<IASTName>();
        Object data = ((ICfgData) bb).getData();
        if (data == null || !(data instanceof IASTNode))
            return writeAccesses;
        
        if(data instanceof IASTDeclarationStatement) {
            if(((IASTDeclarationStatement) data).getDeclaration() instanceof IASTSimpleDeclaration) {
                //int x; int x, y; int x = 1;
                for(IASTDeclarator dec : ((IASTSimpleDeclaration) (((IASTDeclarationStatement) data).getDeclaration())).getDeclarators()) {
                    writeAccesses.add(dec.getName());
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
                    writeAccesses.add(((IASTIdExpression) (unary.getOperand())).getName());
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
                    writeAccesses.add(((IASTIdExpression) ((IASTBinaryExpression) expr).getOperand1()).getName());
                }
                else if(binary.getOperand1() instanceof IASTArraySubscriptExpression) {
                    if(((IASTArraySubscriptExpression) binary.getOperand1()).getArrayExpression() instanceof IASTIdExpression) {
                        writeAccesses.add(((IASTIdExpression) ((IASTArraySubscriptExpression) binary.getOperand1()).getArrayExpression()).getName());
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
