package edu.auburn.oaccrefac.core.dataflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.codan.core.cxx.internal.model.cfg.ControlFlowGraphBuilder;
import org.eclipse.cdt.codan.core.model.cfg.IBasicBlock;
import org.eclipse.cdt.codan.core.model.cfg.ICfgData;
import org.eclipse.cdt.codan.core.model.cfg.IControlFlowGraph;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

@SuppressWarnings("restriction")
public class OpenACCReachingDefinitions {

    private IControlFlowGraph cfg;
    
    private Map<VariableAccess, Set<IASTStatement>> reachingDefs;
    private Map<IBasicBlock, RDVarSet> entries;
    private Map<IBasicBlock, RDVarSet> exits;
    
    public OpenACCReachingDefinitions(IASTFunctionDefinition func) {
        this.cfg = new ControlFlowGraphBuilder().build(func);
        
        this.reachingDefs = new HashMap<VariableAccess, Set<IASTStatement>>();
        this.entries = new HashMap<IBasicBlock, RDVarSet>();
        this.exits = new HashMap<IBasicBlock, RDVarSet>();
        
        identifyReachingDefinitions(cfg);
        
    }
    
    private void identifyReachingDefinitions(IControlFlowGraph cfg) {
        boolean changed;
        do {
            changed = false;
            for (IBasicBlock bb : cfg.getNodes()) {
//                for (IBasicBlock pred : bb.getIncomingNodes()) {
//                    if (pred != null) {
//                        ConstEnv exitSet = this.exitSets.get(pred);
//                        if (env == null)
//                            env = exitSet;
//                        else
//                            env = env.intersect(exitSet);
//                    }
//                }
//                this.entrySets.put(bb, env);
//
//                ConstEnv before = this.exitSets.get(bb);
//                ConstEnv after = propagateAcross(bb, env);
//                this.exitSets.put(bb, after);
                
                //RDentry(x) = RDexit(x-1)
                //RDexit(x) = (RDentry(x) - kill(x)) U gen(x)
                
                //entry set becomes all of the exit sets of the previous nodes
                RDVarSet bbEntry = new RDVarSet();
                RDVarSet bbExit = new RDVarSet();
                
                for(IBasicBlock pred : bb.getIncomingNodes()) {
                    bbEntry.union(exits.get(pred));
                }
                //exit set is the entry set minus everything that get killed plus everything that gets generated
                //TODO check if null
                bbExit.union(entries.get(bb));
                bbExit.subtract(killSet(bb));
                bbExit.union(genSet(bb));
                
                //get sets before changes are made (before and after)
                //make changes
                
//                changed = changed || !areEqual(before, after);
            }
        } while (changed);
        
    }

    private RDVarSet killSet(IBasicBlock bb) {
        RDVarSet kill = new RDVarSet();
        Object data = ((ICfgData) bb).getData();
        if (data == null || !(data instanceof IASTNode))
            return kill;
        
        
        return null;
    }
    
    private RDVarSet genSet(IBasicBlock bb) {
        return null;
    }
    
    private boolean areEqual(Set<IBasicBlock> first, Set<IBasicBlock> second) {
        for(IBasicBlock el1 : first) {
            if(!second.contains(el1)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean reaches(VariableAccess write, VariableAccess use) {
        return false;
    }
    
}
