/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
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
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.FileStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

/**
 * Class is meant to be an abstract base class for all ForLoop transformation refactorings. It includes all methods that
 * would be synonymous in all for-loop refactorings. The only method that is different in the refactorings is the
 * 'collectModifications' method, which is to be overridden in order to make the magic happen.
 *
 */
@SuppressWarnings("restriction")
public abstract class ForLoopRefactoring extends CRefactoring {

    private IASTTranslationUnit ast;
    private IASTForStatement forloop;

    /**
     * The constructor for ForLoopRefactoring takes items for refactoring and tosses them up to the super class as well
     * as does some checks to make sure things are capiche.
     */
    public ForLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);

        if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
    }

    /**
     * This is the abstract method that is the implementation for all refactorings. Override this method in inherited
     * classes and use the rewriter to collect changes for refactoring. Tip: Make bigger changes at a time -- making a
     * ton of small replacements and additions in the rewriter may cause issues with overwritting text edits and nodes
     * that don't appear to be in the AST. You can create new nodes using a node factory of the form ICNodeFactory
     * factory = ASTNodeFactoryFactory.getCDefaultFactory(); Tip: While some cases it may be helpful to do the above,
     * see 'LoopInterchangeRefactoring' for a case in which it is more practical to simply use the replace method on the
     * tree instead. Tip: Trying to add nodes from an existing AST (say, from the refactored code) into a node created
     * from the factory may give you a 'this node is frozen' error. When adding a node from an existing AST to one
     * created from a factory, call 'node'.copy() to create an unfrozen copy of the node.
     * 
     * @param rewriter
     * @param pm
     */
    protected abstract void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException;

    /**
     * This method is the driver for the refactoring implementation that you defined above. It does some initialization
     * before the refactoring (that is also similar to all for loop refactorings) and then calls your implementation.
     */
    @Override
    protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
            throws CoreException, OperationCanceledException {

        pm.subTask("Calculating modifications...");
        ASTRewrite rewriter = collector.rewriterForTranslationUnit(getAST());
        // Other initialization stuff here...

        refactor(new CDTASTRewriteProxy(rewriter), pm);
    }

    /**
     * Checks some initial conditions based on the element to be refactored. The method is typically called by the UI to
     * perform an initial checks after an action has been executed. The refactoring has to be considered as not being
     * executable if the returned status has the severity of RefactoringStatus#FATAL.
     */
    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        if (initStatus.hasFatalError()) {
            return initStatus;
        }

        pm.subTask("Waiting for indexer...");
        prepareIndexer(pm);
        pm.subTask("Analyzing selection...");

        SubMonitor progress = SubMonitor.convert(pm, 10);
        ast = getAST(tu, progress.newChild(9));
        forloop = findLoop(ast);
        if (forloop == null) {
            initStatus.addFatalError("Please select a for loop.");
            return initStatus;
        }

        String msg = String.format("Selected loop (line %d) is: %s",
                forloop.getFileLocation().getStartingLineNumber(), ASTUtil.summarize(forloop));
        initStatus.addInfo(msg, getLocation(forloop));

        ForStatementInquisitor forLoop = ForStatementInquisitor.getInquisitor(forloop);

        pm.subTask("Checking initial conditions...");
        if (!forLoop.isCountedLoop()) {
            initStatus.addFatalError("Loop form not supported!", getLocation(forloop));
            return initStatus;
        }

        doCheckInitialConditions(initStatus, progress.newChild(1));

        if (containsUnsupportedOp(forloop)) {
            initStatus.addFatalError(
                    "Cannot refactor -- loop contains " + "iteration augment statement (break or continue or goto)");
        }

        pm.subTask("Done checking initial conditions");
        return initStatus;
    }

    private boolean containsUnsupportedOp(IASTForStatement forStmt) {
        return !ASTUtil.find(forStmt, IASTBreakStatement.class).isEmpty()
                || !ASTUtil.find(forStmt, IASTContinueStatement.class).isEmpty()
                || !ASTUtil.find(forStmt, IASTGotoStatement.class).isEmpty();
    }

    protected RefactoringStatusContext getLocation(IASTNode node) {
        IASTFileLocation fileLocation = node.getFileLocation();
        Region region = new Region(fileLocation.getNodeOffset(), fileLocation.getNodeLength());
        return new FileStatusContext(tu.getFile(), region);
    }

    @Override
    protected RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext checkContext)
            throws CoreException, OperationCanceledException {
        RefactoringStatus result = new RefactoringStatus();
        pm.subTask("Determining if transformation can be safely performed...");
        doCheckFinalConditions(result, pm);
        return result;
    }

    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
    }

    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
    }

    /**
     * Indexes the project if the project has not already been indexed. Something to do with references???
     * 
     * @param pm
     * @throws CoreException
     */
    private void prepareIndexer(IProgressMonitor pm) throws CoreException {
        IIndexManager im = CCorePlugin.getIndexManager();
        while (!im.isProjectIndexed(project)) {
            im.joinIndexer(500, pm);
            if (pm.isCanceled())
                throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
        }
        if (!im.isProjectIndexed(project))
            throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
    }

    /**
     * This function finds the first CASTForStatement (for-loop) within a selection. It defines a private class
     * (Visitor) to traverse the translation unit AST (which is an AST for our source file). When we call ast.accept(v),
     * it traverses the tree to find the first acceptance within the 'selectedRegion' (a protected variable from
     * CRefactoring).
     * 
     * @param ast
     *            -- our AST to traverse
     * @return CASTForStatement to perform refactoring on
     */
    protected CASTForStatement findLoop(IASTTranslationUnit ast) {
        IASTForStatement first_for = null;
        List<IASTForStatement> fors = ASTUtil.find(ast, IASTForStatement.class);
        int begin = selectedRegion.getOffset();
        int end = selectedRegion.getLength() + begin;
        
        for (IASTForStatement loop : fors) {
            IASTFileLocation loc = loop.getFileLocation(); 
            if (loc.getNodeOffset() >= begin
               && loc.getNodeOffset() < end) {
                if (first_for != null) {
                    IASTFileLocation firstloc = first_for.getFileLocation();
                    if (firstloc.getNodeOffset() > loc.getNodeOffset()) {
                        first_for = loop;
                    }
                } else {
                    first_for = loop;
                }
            }
        }
        
        return (CASTForStatement) first_for;
    }

    // *************************************************************************
    // Getters & Setters

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null; // Refactoring history is not supported.
    }

    protected CASTForStatement getLoop() {
        return (CASTForStatement) forloop;
    }

    protected IASTTranslationUnit getAST() {
        return ast;
    }

    // *************************************************************************

}
