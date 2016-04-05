/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.text.edits.TextEditGroup;

/**
 * {@link IASTRewrite} is an interface with the same API as CDT's ASTRewrite class.
 * <p>
 * The Eclipse plug-in uses CDT's {@link ASTRewrite} class to perform rewriting, while the command-line tool uses its
 * own (modified) ASTRewrite class: the Eclipse plug-in depends on the Eclipse file system, while the command-line tool
 * does not. {@link SourceAlteration} objects receive and return {@link IASTRewrite}s so that they can be used in
 * Eclipse as well as in the command-line tool.
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public interface IASTRewrite {

    IASTNode createLiteralNode(String code);

    void remove(IASTNode node, TextEditGroup editGroup);

    IASTRewrite replace(IASTNode node, IASTNode replacement, TextEditGroup editGroup);

    IASTRewrite insertBefore(IASTNode parent, IASTNode insertionPoint, IASTNode newNode, TextEditGroup editGroup);

    Change rewriteAST();
}
