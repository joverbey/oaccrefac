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
package org.eclipse.ptp.pldt.openacc.internal.core.patternmatching;

/**
 * Simply an ArbitraryExpression that is an L-Value.
 * 
 * @author William Hester
 */
public class ArbitraryLValue extends ArbitraryExpression {
    
    private String id;
    
    public ArbitraryLValue(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    @Override
    public boolean isLValue() {
        return true;
    }
}
