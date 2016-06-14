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
package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccNoConstruct extends ASTNode implements IAccConstruct
{

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccNoConstruct(this);
        visitor.visitIAccConstruct(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 0;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

