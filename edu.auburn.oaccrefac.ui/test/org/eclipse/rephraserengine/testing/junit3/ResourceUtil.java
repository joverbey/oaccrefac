/*******************************************************************************
 * Copyright (c) 2010, 2012 University of Illinois at Urbana-Champaign,
 * Auburn University, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.rephraserengine.testing.junit3;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Utility methods for converting between {@link IFile}/{@link IResource} objects and (
 * <code>String</code>) filenames used by the VPG and stored in its database.
 * <p>
 * Copied from org.eclipse.rephraserengine.core.vpg.eclipse.ResourceUtil
 * 
 * @author Jeff Overbey
 * 
 * @since 8.0
 */
public final class ResourceUtil
{
    private ResourceUtil() {;}
    
    ///////////////////////////////////////////////////////////////////////////
    // Utility Methods (IFile<->Filename Mapping)
    ///////////////////////////////////////////////////////////////////////////

    public static IFile getIFileForFilename(String filename)
    {
        IResource resource = getIResourceForFilename(filename);
        if (resource instanceof IFile)
            return (IFile)resource;
        else
            return null;
    }

    public static String getFilenameForIFile(IFile file)
    {
        return getFilenameForIResource(file);
    }

    public static IResource getIResourceForFilename(String filename)
    {
        return ResourcesPlugin.getWorkspace().getRoot().findMember(filename);
    }

    public static String getFilenameForIResource(IResource resource)
    {
        if (resource == null)
            return null;
        else
            return resource.getFullPath().toString();
    }
}
