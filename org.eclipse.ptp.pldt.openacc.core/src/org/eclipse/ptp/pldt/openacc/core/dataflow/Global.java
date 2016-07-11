/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTCompletionContext;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.parser.IToken;

public class Global implements IASTName {

	private IBinding binding;
	private static Map<IBinding, Global> globals = new HashMap<IBinding, Global>();
	
	public static Global newInstance(IBinding binding) {
		if(globals.containsKey(binding)) {
			return globals.get(binding);
		}
		Global g = new Global(binding);
		globals.put(binding, g);
		return g;
	}
	
	private Global(IBinding binding) {
		this.binding = binding;
	}
	
	@Override
	public IASTTranslationUnit getTranslationUnit() {
		return null;
	}

	@Override
	public IASTNodeLocation[] getNodeLocations() {
		return null;
	}

	@Override
	public IASTFileLocation getFileLocation() {
		return new IASTFileLocation() {
			
			@Override
			public IASTFileLocation asFileLocation() {
				return this;
			}
			
			@Override
			public int getStartingLineNumber() {
				return -1;
			}
			
			@Override
			public int getNodeOffset() {
				return -1;
			}
			
			@Override
			public int getNodeLength() {
				return -1;
			}
			
			@Override
			public String getFileName() {
				return null;
			}
			
			@Override
			public int getEndingLineNumber() {
				return -1;
			}
			
			@Override
			public IASTPreprocessorIncludeStatement getContextInclusionStatement() {
				return null;
			}
		};
	}

	@Override
	public String getContainingFilename() {
		return null;
	}

	@Override
	public boolean isPartOfTranslationUnitFile() {
		return false;
	}

	@Override
	public IASTNode getParent() {
		return null;
	}

	@Override
	public IASTNode[] getChildren() {
		return new IASTNode[] { };
	}

	@Override
	public void setParent(IASTNode node) {

	}

	@Override
	public ASTNodeProperty getPropertyInParent() {
		return null;
	}

	@Override
	public void setPropertyInParent(ASTNodeProperty property) {

	}

	@Override
	public boolean accept(ASTVisitor visitor) {
		return false;
	}

	@Override
	public String getRawSignature() {
		return binding.getName();
	}

	@Override
	public boolean contains(IASTNode node) {
		return false;
	}

	@Override
	public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
		return null;
	}

	@Override
	public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
		return null;
	}

	@Override
	public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
		return null;
	}

	@Override
	public boolean isFrozen() {
		return false;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public IASTNode getOriginalNode() {
		return this;
	}

	@Override
	public char[] getSimpleID() {
		return binding.getNameCharArray();
	}

	@Override
	public boolean isDeclaration() {
		return true;
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	public boolean isDefinition() {
		return true;
	}

	@Override
	public char[] toCharArray() {
		return binding.getNameCharArray();
	}

	@Override
	public IBinding getBinding() {
		return binding;
	}

	@Override
	public IBinding resolveBinding() {
		return binding;
	}

	@Override
	public int getRoleOfName(boolean allowResolution) {
		return IASTNameOwner.r_unclear;
	}

	@Override
	public IASTCompletionContext getCompletionContext() {
		return null;
	}

	@Override
	public ILinkage getLinkage() {
		return null;
	}

	@Override
	public IASTImageLocation getImageLocation() {
		return null;
	}

	@Override
	public IASTName getLastName() {
		return this;
	}

	@Override
	public IASTName copy() {
		return new Global(binding);
	}

	@Override
	public IASTName copy(CopyStyle style) {
		return new Global(binding);
	}

	@Override
	public void setBinding(IBinding binding) {
		this.binding = binding;
	}

	@Override
	public char[] getLookupKey() {
		return null;
	}

	@Override
	public IBinding getPreBinding() {
		return binding;
	}

	@Override
	public IBinding resolvePreBinding() {
		return binding;
	}

	@Override
	public boolean isQualified() {
		return false;
	}

	@Override
	public String toString() {
		return "GLOBAL:" + binding.toString(); //$NON-NLS-1$
	}
	
}
