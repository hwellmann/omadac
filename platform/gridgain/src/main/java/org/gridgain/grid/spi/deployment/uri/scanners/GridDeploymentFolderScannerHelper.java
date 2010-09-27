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

package org.gridgain.grid.spi.deployment.uri.scanners;

import java.io.*;

/**
 * Helper class that recursively goes through the list of
 * files/directories and handles them by calling given handler.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridDeploymentFolderScannerHelper {
    /**
     * Enforces singleton.
     */
    private GridDeploymentFolderScannerHelper() {
        // No-op.
    }

    /**
     * Applies given file to the handler if it is not filtered out by given
     * filter. If given file is not accepted by filter and it is a directory
     * than recursively scans directory and call the same method for every
     * found file.
     *
     * @param file File that should be handled.
     * @param filter File filter.
     * @param handler Handler which should handle files.
     */
    public static void scanFolder(File file, FileFilter filter, GridDeploymentFileHandler handler) {
        assert file != null : "ASSERTION [line=52, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridDeploymentFolderScannerHelper.java]";

        if (filter.accept(file) == true) {
            handler.handle(file);
        }
        else if (file.isDirectory() == true) {
            for (File child : file.listFiles()) {
                scanFolder(child, filter, handler);
            }
        }
    }
}
