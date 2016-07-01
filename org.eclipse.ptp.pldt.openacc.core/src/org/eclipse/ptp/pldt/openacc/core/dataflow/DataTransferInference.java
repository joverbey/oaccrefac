/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

/**
 * Subclasses should define <code>infer()</code> and call it explicitly from their own constructors.
 */
public abstract class DataTransferInference {
	
	/** everything checking to see if a node copies in something should look to this map, not to the actual AST **/
	protected Map<IASTStatement, Set<IBinding>> transfers;
	
	/** should be used for all data construct hierarchy operations **/
	protected ConstructTree tree;
	
	protected IASTStatement[] construct;
	protected ReachingDefinitionsAnalysis rd;
	protected List<IASTStatement> topoSorted;
	
	private static Map<IASTStatement[], ArbitraryStatement> roots;
	
	static {
		roots = new TreeMap<IASTStatement[], ArbitraryStatement>(new Comparator<IASTStatement[]>() {

			@Override
			public int compare(IASTStatement[] o1, IASTStatement[] o2) {
				if(o1.length != o2.length) return -1;
				for(int i = 0; i < o1.length; i++) {
					if(!o1[i].equals(o2[i])) {
						return 1;
					}
				}
				return 0;
			}
			
		});
	}
	
	protected DataTransferInference() {
		
	}
	
	public DataTransferInference(IASTStatement[] construct, IASTStatement... accIgnore) {
		init(ReachingDefinitionsAnalysis.forFunction(ASTUtil.findNearestAncestor(construct[0], IASTFunctionDefinition.class)), construct, accIgnore);
	}
	
	public DataTransferInference(IASTStatement... construct) {
		init(ReachingDefinitionsAnalysis.forFunction(ASTUtil.findNearestAncestor(construct[0], IASTFunctionDefinition.class)), construct, new IASTStatement[] {});
	}
	
	private void init(ReachingDefinitionsAnalysis rd, IASTStatement[] construct, IASTStatement[] accIgnore) {
		if(construct.length == 0) {
			throw new IllegalArgumentException("At least one statement should be in the construct");
		}
		
		ArbitraryStatement root;
		if(roots.containsKey(construct)) {
			root = roots.get(construct);
		}
		else {
			root = new ArbitraryStatement();
			roots.put(construct, root);
		}
		
		transfers = new HashMap<IASTStatement, Set<IBinding>>();
    	tree = new ConstructTree(root, construct);
		this.construct = construct;
    	
		this.rd = rd;
    	
    	class Init extends ASTVisitor {
    		Init() {
    			shouldVisitStatements = true;
    		}
    		@Override
    		public int visit(IASTStatement statement) {
    			if(!Arrays.asList(accIgnore).contains(statement) && (OpenACCUtil.isAccAccelConstruct(statement) || OpenACCUtil.isAccConstruct(statement, ASTAccDataNode.class))) {
    				transfers.put(statement, treeSetIBinding());
    				tree.addNode(statement);
    			}
    			return PROCESS_CONTINUE;
    		}
    	}
    	for(IASTStatement statement : construct) {
    		statement.accept(new Init());
    	}
    	transfers.put(root, treeSetIBinding());
    	
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
    
    protected boolean isUninitializedDeclaration(IASTName name) {
    	IASTDeclarator decl = ASTUtil.findNearestAncestor(name, IASTDeclarator.class);
		IASTSimpleDeclaration simple = ASTUtil.findNearestAncestor(name, IASTSimpleDeclaration.class);
		if(simple == null || decl == null || decl.getInitializer() != null) {
			return false;
		}
		return true;
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
    	
    	public ConstructTree(ArbitraryStatement root, IASTStatement[] construct) {
    		this.nodes = new ArrayList<IASTStatement>();
    		this.root = root;
    		nodes.add(root);
    		this.construct = construct;
    	}

    	public boolean addNode(IASTStatement construct) {
    		nodes.add(construct);
    		return true;
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
 
    	public boolean isAncestor(IASTNode descendant, IASTStatement ancestor) {
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
