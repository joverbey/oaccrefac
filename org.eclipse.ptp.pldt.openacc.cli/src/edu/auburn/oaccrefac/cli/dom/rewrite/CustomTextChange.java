/*******************************************************************************
 * Copyright (c) 2000, 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jeff Overbey (Auburn) - changes for accelerator refactorings
 *******************************************************************************/
package edu.auburn.oaccrefac.cli.dom.rewrite;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.UndoEdit;

public class CustomTextChange extends TextChange {

    private final String sourceCode;

    /**
     * Creates a new <code>TextFileChange</code> for the given file.
     *
     * @param sourceCode
     *            the change's name mainly used to render the change in the UI
     * @param file
     *            the file this text change operates on
     * @throws CModelException
     */
    public CustomTextChange(String sourceCode) {
        super(""); //$NON-NLS-1$
        setTextType("c"); //$NON-NLS-1$
        this.sourceCode = sourceCode;
    }

    @Override
    protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
        return new Document(sourceCode);
    }

    @Override
    protected void commit(IDocument document, IProgressMonitor pm) throws CoreException {
        System.out.println(document.get());
    }

    @Override
    protected Change createUndoChange(UndoEdit edit) {
        return new CustomTextChange(sourceCode);
    }

    @Override
    protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
    }

    @Override
    public Object getModifiedElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        throw new UnsupportedOperationException();
    }
}