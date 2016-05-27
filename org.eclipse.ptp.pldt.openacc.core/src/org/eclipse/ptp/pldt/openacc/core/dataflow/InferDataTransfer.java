package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

/**
 * Subclasses should define <code>infer()</code> and call it explicitly from their own constructors.
 */
public abstract class InferDataTransfer {
	
	/** everything checking to see if a node copies in something should look to this map, not to the actual AST **/
	protected Map<IASTStatement, Set<IBinding>> transfers;
	
	/** should be used for all data construct hierarchy operations **/
	protected ConstructTree tree;
	
	protected IASTStatement[] construct;
	protected ReachingDefinitions rd;
	protected List<IASTStatement> topoSorted;
	
	protected InferDataTransfer() {
		
	}
	
	public InferDataTransfer(ReachingDefinitions rd, IASTStatement... construct) {
		init(rd, construct);
	}
	
	public InferDataTransfer(IASTStatement... construct) {
		init(new ReachingDefinitions(ASTUtil.findNearestAncestor(construct[0], IASTFunctionDefinition.class)), construct);
	}
	
	private void init(ReachingDefinitions rd, IASTStatement... construct) {
		if(construct.length == 0) {
			throw new IllegalArgumentException("At least one statement should be in the construct");
		}
		transfers = new HashMap<IASTStatement, Set<IBinding>>();
    	tree = new ConstructTree(construct);
    	this.construct = construct;
    	this.rd = rd;
    	
    	class Init extends ASTVisitor {
    		Init() {
    			shouldVisitStatements = true;
    		}
    		@Override
    		public int visit(IASTStatement statement) {
    			if(OpenACCUtil.isAccAccelConstruct(statement) || OpenACCUtil.isAccConstruct(statement, ASTAccDataNode.class)) {
    				transfers.put(statement, treeSetIBinding());
    				tree.addNode(statement);
    			}
    			return PROCESS_CONTINUE;
    		}
    	}
    	for(IASTStatement statement : construct) {
    		statement.accept(new Init());
    	}
    	transfers.put(tree.getRoot(), treeSetIBinding());
    	
    	topoSorted = topoSortAccConstructs();
	}
	
	public Map<IASTStatement, Set<IBinding>> get() {
		return transfers;
	}
	
	/** populates the copies map **/
    protected abstract void infer();
	
	protected TreeSet<IBinding> treeSetIBinding() {
		return new TreeSet<IBinding>(new Comparator<IBinding>() {
			@Override
			public int compare(IBinding o1, IBinding o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
	}
	
	/** sorts this and all descendant data/parallel constructs topologically according to 
	 * the tree, with lower nodes in the list first
	 **/
    private List<IASTStatement> topoSortAccConstructs() {
    	List<IASTStatement> sorted = new ArrayList<IASTStatement>();
    	topovisit(tree.getRoot(), sorted);
    	return sorted;
    }
    
    private void topovisit(IASTStatement root, List<IASTStatement> sorted) {
    	for(IASTStatement child : tree.getChildren(root)) {
    		topovisit(child, sorted);
    	}
    	sorted.add((IASTStatement) root);
    }
    
    @SafeVarargs
	public static IASTStatement normalizeRoot(InferDataTransfer... sets)  {
    	ArbitraryStatement root = new ArbitraryStatement();
    	for(InferDataTransfer set : sets) {
    		Map<IASTStatement, Set<IBinding>> copy = new HashMap<IASTStatement, Set<IBinding>>(set.transfers);
    		for(IASTStatement statement : copy.keySet()) {
    			if(statement instanceof ArbitraryStatement) {
    				set.transfers.put(root, set.transfers.remove(statement));
    				set.topoSorted.set(set.topoSorted.indexOf(statement), root);
    			}
    		}
    	}
    	return root;
    }
    
    public IASTStatement getRoot() {
    	for(IASTStatement statement : transfers.keySet()) {
    		if(statement instanceof ArbitraryStatement) {
    			return statement;
    		}
    	}
    	return null;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	List<IASTStatement> stmts = new ArrayList<IASTStatement>(topoSorted);
    	IASTStatement root = stmts.remove(stmts.size() - 1);
    	stmts.sort(ASTUtil.FORWARD_COMPARATOR);
    	stmts.add(0, root);
    	for(IASTStatement statement : stmts) {
    		sb.append(statement.getClass().getSimpleName());
    		sb.append("@");
    		if(statement instanceof ArbitraryStatement)
    			sb.append("OUTER");
    		else
    			sb.append("l" + statement.getFileLocation().getStartingLineNumber());
    		sb.append(" : ");
    		sb.append(transfers.get(statement));
    		sb.append("\n");
    	}
    	return sb.toString();
    }
    
    /**
     * A wrapper around the AST that treats only ACC constructs as nodes. Subclasses using nodes from this
     * tree should, as a result, be cautious using methods referring to the AST, since the root node of 
     * this tree is not in the main AST. Some wrapper functions special-casing the root node are provided. 
     * 
     * Since the interface for Infer takes an array of statements, a true root node would be a 
     * construct around the statements in that array, but there may/may not be any such statement. 
     * Thus the root node is a constructed ArbitraryStatement. 
     * 
     */

    protected class ConstructTree {
    	private List<IASTStatement> nodes;
    	private IASTStatement[] construct;
    	private ArbitraryStatement root;
    	
    	public ConstructTree(IASTStatement... construct) {
    		this.nodes = new ArrayList<IASTStatement>();
    		root = new ArbitraryStatement();
    		nodes.add(root);
    		this.construct = construct;
    	}

    	public boolean addNode(IASTStatement construct) {
    		nodes.add(construct);
    		return true;
    	}
    	
    	private void replaceRoot(ArbitraryStatement newRoot) {
    		nodes.set(nodes.indexOf(root), newRoot);
    		this.root = newRoot;
    	}
    	
    	public IASTStatement getParent(IASTStatement construct) {
    		if(construct.equals(root)) {
    			return null;
    		}
    		IASTStatement parent = null;
    		for (IASTStatement node : nodes) {
    			if (ASTUtil.isStrictAncestor(construct, node)) {
    				if (parent == null) {
    					parent = node;
    				} else if (ASTUtil.isStrictAncestor(node, parent)) {
    					parent = node;
    				}
    			}
    		}
    		return parent == null? root : parent;
    	}

    	public List<IASTStatement> getChildren(IASTStatement construct) {
    		List<IASTStatement> children = new ArrayList<IASTStatement>();
			for (IASTStatement node : nodes) {
    			IASTStatement parent = getParent(node);
    			if (parent != null && parent.equals(construct)) {
    				children.add(node);
    			}
    		}
    		return children;
    	}
    
    	public IASTStatement getRoot() {
    		return root;
    	}
 
    	public boolean isAncestor(IASTStatement ancestor, IASTNode descendant) {
    		if(ancestor.equals(root)) {
    			//is an ancestor if descendant is anywhere in the main construct
    			for(IASTStatement statement : construct) {
    				if(ASTUtil.isAncestor(descendant, statement)) {
    					return true;
    				}
    			}
    			return false;
    		}
    		else {
    			return ASTUtil.isAncestor(descendant, ancestor);
    		}
    	}
    	
    	public boolean isAccAccelRegion(IASTStatement statement) {
			if(statement.equals(root)) {
				return false;
			}
			else {
				return OpenACCUtil.isAccAccelConstruct(statement);
			}
    	}
    	
    	@Override
    	public String toString() {
    		StringBuilder sb = new StringBuilder();
    		toString(root, sb, 0);
    		return sb.toString();
    	}
    	
    	private void toString(IASTStatement node, StringBuilder sb, int depth) {
    		for(int i = 0; i < depth; i++) {
    			sb.append("\t");
    		}
    		sb.append("-" + node.getClass().getSimpleName() + "@" + node.hashCode());
    		sb.append("\n");
    		for(IASTStatement child : getChildren(node)) {
    			toString(child, sb, depth + 1);
    		}
    	}
    	
    }
    
    
}
