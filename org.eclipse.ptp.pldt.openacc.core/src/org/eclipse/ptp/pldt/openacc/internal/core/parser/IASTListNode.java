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
package org.eclipse.ptp.pldt.openacc.internal.core.parser;

import java.util.List;

@SuppressWarnings("all")
public interface IASTListNode<T> extends List<T>, IASTNode
{
    void insertBefore(T insertBefore, T newElement);
    void insertAfter(T insertAfter, T newElement);
}
