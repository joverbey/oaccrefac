package org.eclipse.ptp.pldt.openacc.internal.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;

public class FunctionNode {

	private FunctionNode root;
	private Level level;
	private IASTFunctionDefinition definition;
	private ArrayList<FunctionNode> children = new ArrayList<FunctionNode>();
	private ArrayList<FunctionNode> parents = new ArrayList<FunctionNode>();
	
	private enum Level {SEQ, VECTOR, WORKER, GANG}

	//Used to create root of the tree, which may contain multiple unrelated definitions as children.
	public FunctionNode(List<IASTFunctionDefinition> definitions) {
		this.definition = null;
		this.root = this;
		for (IASTFunctionDefinition definition : definitions) {
			children.add(new FunctionNode(definition, root, this));
		}
	}
	
	private FunctionNode(IASTFunctionDefinition definition, FunctionNode root, FunctionNode parent) {
		this.definition = definition;
		this.root = root;
		this.parents.add(parent);
		this.addChildren();
		this.findInherentRestriction();
	}
	
	private void addChildren() {
		for (IASTFunctionCallExpression call : ASTUtil.find(definition, IASTFunctionCallExpression.class)) {
			IASTFunctionDefinition functionDefinition = findFunctionDefinition(call);
			FunctionNode existingNode = root.findDescendent(functionDefinition, new HashSet<FunctionNode>());
			if (existingNode != null) {
				existingNode.parents.add(this);
				this.children.add(existingNode);
			} else {
				this.children.add(new FunctionNode(functionDefinition, root, this));
			}
		}
	}
	
	private void findInherentRestriction() {
		level = null;
		for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getInternalPragmaNodes(definition.getBody())) {
			String rawSignature = pragma.getRawSignature();
			if (rawSignature.contains("seq") && (level == null)) {
				level = Level.SEQ;
			} else if (rawSignature.contains("vector") && (level == null || level == Level.SEQ)) {
				level = Level.VECTOR;
			} else if (rawSignature.contains("worker") && (level == null || level.compareTo(Level.WORKER) < 0)) {
				level = Level.WORKER;
			} else if (rawSignature.contains("gang") && (level == null || level.compareTo(Level.GANG) < 0)) {
				level = Level.GANG;
			}
		}
	}
	
	private FunctionNode findDescendent(IASTFunctionDefinition definition, HashSet<FunctionNode> visited) {
		if (visited.contains(this)) {
			return null;
		}
		visited.add(this);
		if (this.definition != null && this.definition.equals(definition)) {
			return this;
		}
		for (FunctionNode child : children) {
			FunctionNode output = child.findDescendent(definition, visited);
			if (output != null) {
				return output;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		String output = "";
		if (root.equals(this)) {
			output += "Root, ";
		}
		output += "Level: ";
		if (level != null) {
			switch(level) {
			case SEQ:
				output += "SEQ, ";
				break;
			case VECTOR:
				output += "VECTOR, ";
				break;
			case WORKER:
				output += "WORKER, ";
				break;
			case GANG:
				output += "GANG, ";
				break;
			default:
				break;
			}
		} else {
			output += "null, ";
		}
		output += "Children: ";
		output += children.size();
		output += ", Parents: ";
		output += parents.size();
		return output;
	}
	
	//TODO: Add support for overloaded functions
	public static IASTFunctionDefinition findFunctionDefinition(IASTFunctionCallExpression call) {
		if (!(call.getFunctionNameExpression() instanceof IASTIdExpression)) {
			return null;
		}
		IASTName callName = ((IASTIdExpression) call.getFunctionNameExpression()).getName();
		List<IASTFunctionDefinition> functionDefinitions = 
				ASTUtil.find(call.getTranslationUnit(), IASTFunctionDefinition.class);
		for (IASTFunctionDefinition definition : functionDefinitions) {
			IASTName definitionName = definition.getDeclarator().getName();
			if (definitionName.toString().equals(callName.toString())) {
				return definition;
			}
		}
		return null;
	}
}
