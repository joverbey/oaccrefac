/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.dependence;

/**
 * DependneceType represents the available types of dependences.
 * 
 * @author Alexander Calvert
 * @author Jeff Overbey
 */
public enum DependenceType {
    FLOW, ANTI, OUTPUT, INPUT;
}
