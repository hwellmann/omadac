/*
 * GRIDGAIN - OPEN CLOUD PLATFORM.
 * COPYRIGHT (C) 2005-2008 GRIDGAIN SYSTEMS. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE AS PUBLISHED BY THE FREE SOFTWARE FOUNDATION; EITHER
 * VERSION 2.1 OF THE LICENSE, OR (AT YOUR OPTION) ANY LATER
 * VERSION.
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  SEE THE
 * GNU LESSER GENERAL PUBLIC LICENSE FOR MORE DETAILS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE ALONG WITH THIS LIBRARY; IF NOT, WRITE TO THE FREE
 * SOFTWARE FOUNDATION, INC., 51 FRANKLIN ST, FIFTH FLOOR, BOSTON, MA
 * 02110-1301 USA
 */

package org.gridgain.grid.spi.deployment.uri;

import java.io.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.*;

/**
 * Class loader helper that could load class from the file using certain
 * class loader.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentFileResourceLoader {
    /** Class loader class should be loaded by. */
    private final ClassLoader clsLdr;

    /** Initial directory. */
    private final File scanPathDir;

    /**
     * Creates new instance of loader helper.
     *
     * @param clsLdr Class loader class should be loaded by.
     * @param scanPathDir Initial directory.
     */
    GridUriDeploymentFileResourceLoader(ClassLoader clsLdr, File scanPathDir) {
        this.clsLdr = clsLdr;
        this.scanPathDir = scanPathDir;
    }

    /**
     * Creates new class from file with given file name.
     *
     * @param fileName Name of the class to be loaded. It might be either
     *      fully-qualified or just a class name.
     * @param ignoreUnknownRsrc Whether unresolved classes should be
     *      ignored or not.
     * @return Loaded class.
     * @throws GridSpiException If class could not be loaded and
     *      <tt>ignoreUnknownRsrc</tt> parameter is <tt>true</tt>.
     */
    @SuppressWarnings("unchecked")
    Class<? extends GridTask<?, ?>> createResource(String fileName, boolean ignoreUnknownRsrc) throws GridSpiException {
        if (scanPathDir.isDirectory()) {
            fileName = fileName.substring(scanPathDir.getAbsolutePath().length() + 1);
        }

        if (fileName.endsWith(".class") == true) {
            String str = fileName;

            // Replace separators.
            str = str.replaceAll("\\/|\\\\", ".");

            // Strip off '.class' extention.
            str = str.substring(0, str.indexOf(".class"));

            try {
                return (Class<? extends GridTask<?, ?>>)clsLdr.loadClass(str);
            }
            catch (ClassNotFoundException e) {
                if (ignoreUnknownRsrc == true) {
                    // No-op.
                }
                else {
                    throw (GridSpiException)new GridSpiException("Failed to load class: " + str, e).setData(87, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentFileResourceLoader.java");
                }
            }
        }

        // Not a class resource.
        return null;
    }
}
