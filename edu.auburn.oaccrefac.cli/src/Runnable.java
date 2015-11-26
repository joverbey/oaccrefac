/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

/**
 * Runnable interface allows a class to be run with a set of arguments.
 */
public interface Runnable {
    
    /**
     * run runs the Runnable.
     * 
     * @param args Arguments to run with.
     */
    abstract void run(String[] args);
    
}
