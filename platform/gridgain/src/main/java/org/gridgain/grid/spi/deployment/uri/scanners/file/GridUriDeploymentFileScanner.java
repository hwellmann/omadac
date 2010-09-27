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

package org.gridgain.grid.spi.deployment.uri.scanners.file;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.deployment.uri.scanners.*;
import org.gridgain.grid.spi.deployment.uri.util.*;
import org.gridgain.grid.util.*;

/**
 * Scanner that processes all URIs with "file" scheme. Usualy URI point to
 * certain directory or file and scanner is in charge of watching all changes
 * (file deletion, creation and so on) and sending notification to the listener
 * about every change.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridUriDeploymentFileScanner extends GridUriDeploymentScanner {
    /** Scanning directory or file. */
    private File scanDir = null;

    /** Cache of found GAR-files or GAR-directories to check if any of it has been updated. */
    private Map<File, Long> tstampCache = new HashMap<File, Long>();

    /** Cache of found files in GAR-folder to check if any of it has been updated. */
    private Map<File, Map<File, Long>> garDirFilesTstampCache = new HashMap<File, Map<File, Long>>();

    /** */
    private FileFilter garFilter = null;

    /** */
    private FileFilter garDirFilesFilter = null;

    /**
     * Creates new instance of scanner with given name.
     *
     * @param gridName Grid name.
     * @param uri URI which scanner should look after.
     * @param deployDir Temporary deployment directory.
     * @param freq Scan frequency.
     * @param filter Found files filter.
     * @param listener Scanner listener which should be notifier about changes.
     * @param log Logger.
     * @throws GridSpiException Thrown if URI is <tt>null</tt> or is not a
     *      directory.
     */
    public GridUriDeploymentFileScanner(
        String gridName,
        URI uri,
        File deployDir,
        long freq,
        FilenameFilter filter,
        GridUriDeploymentScannerListener listener,
        GridLogger log) throws GridSpiException {
        super(gridName, uri, deployDir, freq, filter, listener, log);

        initialize(uri);
    }

    /**
     * Initializes scanner by parsing given URI and extracting scanning
     * directory path and creating file filters.
     *
     * @param uri Scanning URI with "file" scheme.
     * @throws GridSpiException Thrown if URI is <tt>null</tt> or is not a
     *      directory.
     */
    private void initialize(URI uri) throws GridSpiException {
        assert "file".equals(getUri().getScheme()) == true : "ASSERTION [line=93, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/file/GridUriDeploymentFileScanner.java]";

        String scanDirPath = uri.getPath();

        if (scanDirPath != null) {
            scanDir = new File(scanDirPath);
        }

        if (scanDir == null || scanDir.isDirectory() == false) {
            scanDir = null;

            throw (GridSpiException)new GridSpiException("URI is either not provided or is not a directory: " +
                GridUriDeploymentUtils.hidePassword(uri.toString())).setData(104, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/file/GridUriDeploymentFileScanner.java");
        }

        garFilter = new FileFilter() {
            /**
             * {@inheritDoc}
             */
            public boolean accept(File pathname) {
                return getFilter().accept(null, pathname.getName());
            }
        };

        garDirFilesFilter = new FileFilter() {
            /**
             * {@inheritDoc}
             */
            public boolean accept(File pathname) {
                // Allow all files in GAR-directory.
                return pathname.isFile() == true;
            }
        };
    }

    /**
     * Handles changes in scanning directory by tracking files modification date.
     * Checks files modification date agains those one that was collected before
     * and notifies listener about every changed or deleted file.
     */
    @Override
    protected void process() {
        final Set<File> foundFiles = isFirstScan() == true ? new HashSet<File>() :
            new HashSet<File>(tstampCache.size());

        GridDeploymentFileHandler handler = new GridDeploymentFileHandler() {
            /**
             * {@inheritDoc}
             */
            public void handle(File file) {
                foundFiles.add(file);

                handleFile(file);
            }
        };

        // Scan directory for deploy units.
        GridDeploymentFolderScannerHelper.scanFolder(scanDir, garFilter, handler);

        // Print warning if no GAR-units found first time.
        if (isFirstScan() == true && foundFiles.size() == 0) {
            getLogger().warning("No GAR-units found in: " + GridUriDeploymentUtils.hidePassword(getUri().toString()));
        }

        if (isFirstScan() == false) {
            Set<File> deletedFiles = new HashSet<File>(tstampCache.keySet());

            deletedFiles.removeAll(foundFiles);

            if (deletedFiles.size() > 0) {
                List<String> uris = new ArrayList<String>();

                for (File file : deletedFiles) {
                    uris.add(getFileUri(file.getAbsolutePath()));
                }

                // Clear cache.
                tstampCache.keySet().removeAll(deletedFiles);

                garDirFilesTstampCache.keySet().removeAll(deletedFiles);

                getListener().onDeletedFiles(uris);
            }
        }
    }

    /**
     * Tests whether given directory or file was changed since last check and if so
     * copies all directory sub-folders and files or file itself to the deployment
     * directory and than notifies listener about new or updated files.
     *
     * @param file Scanning directory or file.
     */
    private void handleFile(File file) {
        boolean changed = false;

        Long lastModified = null;

        if (file.isDirectory() == true) {
            GridMutable<Long> dirLastModified = new GridMutable<Long>(file.lastModified());

            changed = checkGarDirectoryChanged(file, dirLastModified) == true;

            lastModified = dirLastModified.getValue();
        }
        else {
            lastModified = tstampCache.get(file);

            changed = lastModified == null || lastModified != file.lastModified();

            lastModified = file.lastModified();
        }

        // If file is new or has been modified.
        if (changed == true) {
            tstampCache.put(file, lastModified);

            if (getLogger().isDebugEnabled() == true) {
                getLogger().debug("Discovered deployment file or directory: " + file);
            }

            String fileName = file.getName();

            try {
                File copyFile = createTempFile(fileName, getDeployDirectory());

                // Delete file when JVM stopped.
                copyFile.deleteOnExit();

                if (file.isDirectory() == true) {
                    copyFile = new File(copyFile.getParent(), "dir_" + copyFile.getName());

                    // Delete directory when JVM stopped.
                    copyFile.deleteOnExit();
                }

                // Copy file to deploy directory.
                GridUtils.copy(file, copyFile, true);

                String fileUri = getFileUri(file.getAbsolutePath());

                getListener().onNewOrUpdatedFile(copyFile, fileUri, lastModified);
            }
            catch (IOException e) {
                getLogger().error("Error saving file: " + fileName, e);
            }
        }
    }

    /**
     * Tests whether certain directory was changed since given modification date.
     * It scans all directory files one by one and compares their modification
     * dates with those ones that was collected before.
     * <p>
     * If at least one file was changed (has modification date after given one)
     * whole directory is considered as modified.
     *
     * @param dir Scanning directory.
     * @param lastModified Last calculated Directory modification date.
     * @return <tt>true</tt> if directory was changed since last check and
     *      <tt>false</tt> otherwise.
     */
    private boolean checkGarDirectoryChanged(File dir, final GridMutable<Long> lastModified) {
        final Map<File, Long> clssTstampCache;

        boolean fisrtScan = false;

        if (garDirFilesTstampCache.containsKey(dir) == false) {
            fisrtScan = true;

            garDirFilesTstampCache.put(dir, clssTstampCache = new HashMap<File, Long>());
        }
        else {
            clssTstampCache = garDirFilesTstampCache.get(dir);
        }

        assert clssTstampCache != null : "ASSERTION [line=269, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/file/GridUriDeploymentFileScanner.java]";

        final GridMutable<Boolean> changed = new GridMutable<Boolean>(false);

        final Set<File> foundFiles = fisrtScan == true ? new HashSet<File>() :
            new HashSet<File>(clssTstampCache.size());

        GridDeploymentFileHandler handler = new GridDeploymentFileHandler() {
            /**
             * {@inheritDoc}
             */
            public void handle(File file) {
                foundFiles.add(file);

                Long fileLastModified = clssTstampCache.get(file);

                if (fileLastModified == null || fileLastModified != file.lastModified()) {
                    clssTstampCache.put(file, fileLastModified = file.lastModified());

                    changed.setValue(true);
                }

                // Calculate last modified file in folder.
                if (fileLastModified > lastModified.getValue()) {
                    lastModified.setValue(fileLastModified);
                }
            }
        };

        // Scan GAR-directory for changes.
        GridDeploymentFolderScannerHelper.scanFolder(dir, garDirFilesFilter, handler);

        // Clear cache for deleted files.
        if (fisrtScan == false && clssTstampCache.keySet().retainAll(foundFiles) == true) {
            changed.setValue(true);
        }

        return changed.getValue();
    }

    /**
     * Converts given file name to the URI with "file" scheme.
     *
     * @param name File name to be converted.
     * @return File name with "file://" prefix.
     */
    @Override
    protected String getFileUri(String name) {
        assert name != null : "ASSERTION [line=317, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/file/GridUriDeploymentFileScanner.java]";

        name = name.replace("\\","/");

        return "file://" + (name.charAt(0) == '/' ? "" : '/') + name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(getClass().getName()).append(" [");
        buf.append("scanDir=").append(scanDir);
        buf.append(']');

        return buf.toString();
    }
}

