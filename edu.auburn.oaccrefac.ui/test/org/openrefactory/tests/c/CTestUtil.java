/*******************************************************************************
 * Copyright (c) 2011, 2012 University of Illinois at Urbana-Champaign,
 * Auburn University, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package org.openrefactory.tests.c;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Constants for C unit tests.
 * 
 * @author Jeff Overbey
 */
public final class CTestUtil {
    private CTestUtil() {;}
    
    public static final FilenameFilter C_FILENAME_FILTER =
        new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory()
                        && !name.equalsIgnoreCase("CVS")
                        && !name.equalsIgnoreCase(".svn")
                    || name.endsWith(".c");
            }
        };

    public static final String MARKER = "/*<<<<<";

    public static final String MARKER_END = "*/";
}