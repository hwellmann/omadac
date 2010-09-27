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

import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.*;

/**
 * Helper that loads classes either from directory or from JAR file.
 * <p>
 * If loading from directory is used, helper scans given directory
 * and all subdirectories recursively and loads all files
 * with ".class" extension by given class loader. If class could not
 * be loaded it will be ignored.
 * <p>
 * If JAR file loading is used helper scans JAR file and tries to
 * load all {@link JarEntry} assuming it's a file name.
 * If at least one of them could not be loaded helper fails.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridUriDeploymentDiscovery {
    /**
     * Enforces singleton.
     */
    private GridUriDeploymentDiscovery() {
        // No-op.
    }

    /**
     * Load classes from given file. File could be either directory or JAR file.
     *
     * @param clsLdr Class loader to load files.
     * @param file Either directory or JAR file which contains classes or
     *      references to them.
     * @return Set of found and loaded classes or empty set if file does not
     *      exist.
     * @throws GridSpiException Thrown if given JAR file references to none
     *      existed class or IOException occurred during processing.
     */
    static Set<Class<? extends GridTask<?, ?>>> getClasses(ClassLoader clsLdr, File file)
        throws GridSpiException {
        Set<Class<? extends GridTask<?, ?>>> rsrcs = new HashSet<Class<? extends GridTask<?, ?>>>();

        if (file.exists() == false) {
            return rsrcs;
        }

        GridUriDeploymentFileResourceLoader fileRsrcLdr = new GridUriDeploymentFileResourceLoader(clsLdr, file);

        if (file.isDirectory() == true) {
            findResourcesInDirectory(fileRsrcLdr, file, rsrcs);
        }
        else {
            try {
                for (JarEntry entry : GridUtils.asIterable(new JarFile(file.getAbsolutePath()).entries())) {
                    Class<? extends GridTask<?, ?>> rsrc = fileRsrcLdr.createResource(entry.getName(), false);

                    if (rsrc != null) {
                        rsrcs.add(rsrc);
                    }
                }
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("Failed to discover classes in file: " + file.getAbsolutePath(), e).setData(89, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentDiscovery.java");
            }
        }

        return rsrcs;
    }

    /**
     * Recursively scans given directory and load all found files by loader.
     *
     * @param clsLdr Loader that could load class from given file.
     * @param dir Directory which should be scanned.
     * @param rsrcs Set which will be filled in.
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    private static void findResourcesInDirectory(GridUriDeploymentFileResourceLoader clsLdr, File dir,
        Set<Class<? extends GridTask<?, ?>>> rsrcs) {
        assert dir.isDirectory() == true : "ASSERTION [line=106, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentDiscovery.java]";

        for (File file : dir.listFiles()) {
            if (file.isDirectory() == true) {
                // Recurse down into directories.
                findResourcesInDirectory(clsLdr, file, rsrcs);
            }
            else {
                Class<? extends GridTask<?, ?>> rsrc = null;

                try {
                    rsrc = clsLdr.createResource(file.getAbsolutePath(), true);
                }
                catch (GridSpiException e) {
                    // Must never happen because we use 'ignoreUnknownRsrc=true'.
                    assert false : "ASSERTION [line=121, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentDiscovery.java]";
                }

                if (rsrc != null) {
                    rsrcs.add(rsrc);
                }
            }
        }
    }
}
