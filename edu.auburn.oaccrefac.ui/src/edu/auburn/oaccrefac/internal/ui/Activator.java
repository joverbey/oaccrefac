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
package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "edu.auburn.oaccrefac.ui"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Logs the specified status with this plug-in's log.
     *
     * @param status
     *            status to log
     */
    public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }

    /**
     * Logs an internal error with the specified {@code Throwable}.
     *
     * @param t
     *            the {@code Throwable} to be logged
     */
    public static void log(Throwable t) {
        log(new Status(IStatus.ERROR, PLUGIN_ID, 1, "Internal Error", t)); //$NON-NLS-1$
    }

    /**
     * Logs an internal error with the specified message.
     *
     * @param message
     *            the error message to log
     */
    public static void log(String message) {
        log(new Status(IStatus.ERROR, PLUGIN_ID, 1, message, null));
    }

    /**
     * Logs an internal error with the specified message and {@code Throwable}.
     *
     * @param message
     *            the error message to log
     * @param t
     *            the {@code Throwable} to be logged
     *
     * @since 2.1
     */
    public static void log(String message, Throwable t) {
        log(new Status(IStatus.ERROR, PLUGIN_ID, 1, message, t));
    }
}
