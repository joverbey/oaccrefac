package edu.auburn.oaccrefac.core.dataflow;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.codan.core.cxx.internal.model.cfg.ControlFlowGraphBuilder;
import org.eclipse.cdt.codan.core.model.cfg.IBasicBlock;
import org.eclipse.cdt.codan.core.model.cfg.ICfgData;
import org.eclipse.cdt.codan.core.model.cfg.IControlFlowGraph;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.constprop.ConstEnv;
import edu.auburn.oaccrefac.internal.core.constprop.ConstPropNodeEvaluator;

/**
 * Constant propagation analysis.
 * <p>
 * Constant propagation determines, at each point in a function, whether a variable is constant-valued and, if it is,
 * what its value is.
 * <p>
 * This analysis is intraprocedural and assumes all local variables are not aliased. It applies only to integers and
 * assumes integers are 32 bits wide.
 * 
 * @author Jeff Overbey
 */
@SuppressWarnings("restriction")
public class ConstantPropagation {
    /** The control flow graph on which constnat propagation will be performed. */
    private final IControlFlowGraph cfg;

    /** The constant environment at the entry to each block in the CFG. */
    private final Map<IBasicBlock, ConstEnv> entrySets;

    /** The constant environment at the exit from each block in the CFG. */
    private final Map<IBasicBlock, ConstEnv> exitSets;

    /**
     * If an {@link IASTName} node in an AST corresponds to a variable that has been determined to be constant-valued,
     * maps that AST node to its constant value.
     */
    private final Map<IASTName, Long> constValuedNames;

    /**
     * Constructor. Performs constant propagation on the given function.
     * <p>
     * Results can be retried by visiting {@link IASTName} nodes in an AST and using this class's
     * {@link #getConstantValue(IASTName)} method to determine the constant value of that name (if any).
     * 
     * @param func
     *            non-<code>null</code>
     */
    public ConstantPropagation(IASTFunctionDefinition func) {
        this.cfg = new ControlFlowGraphBuilder().build(func);

        this.entrySets = new HashMap<IBasicBlock, ConstEnv>();
        this.exitSets = new HashMap<IBasicBlock, ConstEnv>();
        this.constValuedNames = new HashMap<IASTName, Long>();

        propagateConstants(cfg);
    }

    private void propagateConstants(IControlFlowGraph cfg) {
        boolean changed;
        do {
            changed = false;
            for (IBasicBlock bb : cfg.getNodes()) {
                ConstEnv env = null;
                for (IBasicBlock pred : bb.getIncomingNodes()) {
                    if (pred != null) {
                        ConstEnv exitSet = this.exitSets.get(pred);
                        if (env == null)
                            env = exitSet;
                        else
                            env = env.intersect(exitSet);
                    }
                }
                this.entrySets.put(bb, env);

                ConstEnv before = this.exitSets.get(bb);
                ConstEnv after = propagateAcross(bb, env);
                this.exitSets.put(bb, after);
                changed = changed || !areEqual(before, after);
            }
        } while (changed);
    }

    private boolean areEqual(ConstEnv before, ConstEnv after) {
        if (before != null && after != null)
            return before.equals(after);
        else if (before == null && after == null)
            return true;
        else
            return false;
    }

    private ConstEnv propagateAcross(IBasicBlock bb, ConstEnv entryEnv) {
        Object data = ((ICfgData) bb).getData();
        if (data == null || !(data instanceof IASTNode))
            return entryEnv;

        ConstPropNodeEvaluator.Result result = ConstPropNodeEvaluator.evaluate((IASTNode) data, entryEnv);
        constValuedNames.putAll(result.constValuedNames);
        return result.environment;
    }

    public Long getConstantValue(IASTName name) {
        return constValuedNames.get(name);
    }

    public String toString() {
        Map<IBasicBlock, Integer> nodeNumbers = new HashMap<IBasicBlock, Integer>();
        int n = 1;
        for (IBasicBlock bb : cfg.getNodes()) {
            nodeNumbers.put(bb, n++);
        }

        StringBuilder sb = new StringBuilder();
        for (IBasicBlock bb : cfg.getNodes()) {
            sb.append("NODE " + nodeNumbers.get(bb) + ":\n");

            sb.append("incoming edges from: ");
            for (IBasicBlock pred : bb.getIncomingNodes()) {
                sb.append(nodeNumbers.get(pred) + " ");
            }
            sb.append("\n");

            sb.append("| " + entrySets.get(bb) + "\n");
            if (bb instanceof ICfgData) {
                Object data = ((ICfgData) bb).getData();
                if (data instanceof IASTNode) {
                    sb.append("V " + bb.getClass().getSimpleName() + ": " + ((IASTNode) data).getClass().getSimpleName()
                            + ": " + ASTUtil.toString((IASTNode) data) + "\n");
                } else {
                    sb.append("V " + bb.getClass().getSimpleName() + "\n");
                }
            } else {
                sb.append("V " + bb.getClass().getSimpleName() + "\n");
            }
            sb.append("| " + exitSets.get(bb) + "\n");

            sb.append("outgoing edges to: ");
            for (IBasicBlock succ : bb.getOutgoingNodes()) {
                sb.append(nodeNumbers.get(succ) + " ");
            }
            sb.append("\n");

            sb.append("\n");
        }

        return sb.toString();
    }
}
