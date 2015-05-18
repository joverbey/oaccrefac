/*******************************************************************************
 * Copyright (c) 2011, 2012 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 * 		Martin Schwab & Thomas Kallenberg - initial API and implementation
 * 		Sergey Prigogin (Google)
 ******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTForStatement;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

/**
 * Determines whether a valid function was selected by the user to be able to
 * run the appropriate strategy for moving the function body to another
 * position.
 */
@SuppressWarnings("restriction")
public class IntroOpenACCParallelRefactoring extends CRefactoring {
	private ITextSelection selection;
	
	public IntroOpenACCParallelRefactoring(ICElement element, ITextSelection selection, ICProject project) {
		super(element, selection, project);
		if (selection == null || tu.getResource() == null || project == null)
			initStatus.addFatalError("Invalid selection");
		this.selection = selection;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		pm.subTask("Waiting for indexer...");
		prepareIndexer(pm);
		pm.subTask("Analyzing selection...");
		return initStatus;
	}

	private void prepareIndexer(IProgressMonitor pm) throws CoreException  {
		IIndexManager im = CCorePlugin.getIndexManager();
		while (!im.isProjectIndexed(project)) {
			im.joinIndexer(500, pm);
			if (pm.isCanceled())
				throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
		}
		if (!im.isProjectIndexed(project))
			throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
	}

	@Override
	protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
			throws CoreException {
		pm.subTask("Calculating modifications...");
		
		SubMonitor progress = SubMonitor.convert(pm, 10);
		IASTTranslationUnit ast = getAST(tu, progress.newChild(9));
		ASTRewrite rewriter = collector.rewriterForTranslationUnit(ast);
		CASTForStatement loop = findLoop(ast);
		IASTNode pragma = rewriter.createLiteralNode("	#pragma acc parallel loop private(n)\n");
		rewriter.insertBefore(loop.getParent(), loop, pragma,
				new TextEditGroup("Insert #pragma"));
	}

	private CASTForStatement findLoop(IASTTranslationUnit ast) {
		class Visitor extends ASTVisitor {
			private CASTForStatement loop = null;

			public Visitor() {
				shouldVisitStatements = true;
			}

			@Override
			public int visit(IASTStatement statement) {
				if (statement instanceof CASTForStatement) {
					loop = (CASTForStatement)statement;
					return PROCESS_ABORT;
				}
				return PROCESS_CONTINUE;
			}
		}
		Visitor v = new Visitor();
		ast.accept(v);
		return v.loop;
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return null;  // Refactoring history is not supported.
	}
}
