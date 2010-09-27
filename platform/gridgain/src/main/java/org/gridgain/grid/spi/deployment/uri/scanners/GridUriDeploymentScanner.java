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
import java.net.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.deployment.uri.util.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Base deployment scanner implementation. It simplifies scanner implementation
 * by providing loggers, executors and file names parsing methods.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public abstract class GridUriDeploymentScanner {
    /** Grid name. */
    private final String gridName;

    /** URI that scanner should looks after. */
    @GridToStringExclude
    private final URI uri;

    /** Temporary deployment directory. */
    private final File deployDir;

    /** Scan frequency. */
    private final long freq;

    /** Found files filter. */
    private final FilenameFilter filter;

    /** Scanner listener which should be notified about changes. */
    private final GridUriDeploymentScannerListener listener;

    /** Logger. */
    private final GridLogger log;

    /** Scanner implementation. */
    private GridSpiThread scanner = null;

    /** Whether first scan completed or not. */
    private boolean firstScan = true;

    /**
     * Scans URI for new, updated or deleted files.
     */
    protected abstract void process();

    /**
     * Creates new scanner.
     *
     * @param gridName Grid name.
     * @param uri URI which scanner should looks after.
     * @param deployDir Temporary deployment directory.
     * @param freq Scan frequency.
     * @param filter Found files filter.
     * @param listener Scanner listener which should be notifier about changes.
     * @param log Logger.
     */
    protected GridUriDeploymentScanner(
        String gridName,
        URI uri,
        File deployDir,
        long freq,
        FilenameFilter filter,
        GridUriDeploymentScannerListener listener,
        GridLogger log) {
        assert uri != null : "ASSERTION [line=92, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";
        assert freq > 0 : "ASSERTION [line=93, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";
        assert deployDir != null : "ASSERTION [line=94, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";
        assert filter != null : "ASSERTION [line=95, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";
        assert log != null : "ASSERTION [line=96, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";
        assert listener != null : "ASSERTION [line=97, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";

        this.gridName = gridName;
        this.uri = uri;
        this.deployDir = deployDir;
        this.freq = freq;
        this.filter = filter;
        this.log = log.getLogger(getClass());
        this.listener = listener;
    }

    /**
     * Starts scanner by given executor.
     */
    public void start() {
        scanner = new GridSpiThread(gridName, "grid-uri-scanner", log) {
            /**
             * {@inheritDoc}
             */
            @Override
            protected void body() throws InterruptedException  {
                try {
                    while (isInterrupted() == false) {
                        try {
                            process();
                        }
                        finally {
                            // Do it in finally to avoid any hanging.
                            if (firstScan == true) {
                                firstScan = false;

                                listener.onFirstScanFinished();
                            }
                        }

                        Thread.sleep(freq);
                    }
                }
                finally {
                    // Double check. If we were cancelled before anything has been scanned.
                    if (firstScan == true) {
                        firstScan = false;

                        listener.onFirstScanFinished();
                    }
                }
            }
        };

        scanner.start();

        if (log.isDebugEnabled() == true) {
            log.debug("URI scanner started.");
        }
    }

    /**
     * Cancels scanner execution.
     */
    public void cancel() {
        GridUtils.interrupt(scanner);
    }

    /**
     * Joins scanner thread.
     */
    public void join() {
        GridUtils.join(scanner, log);

        if (log.isDebugEnabled() == true) {
            log.debug("URI scanner stopped.");
        }
    }

    /**
     * Tests whether scanner was cancelled before or not.
     *
     * @return <tt>true</tt> if scanner was cancelled and <tt>false</tt>
     *      otherwise.
     */
    protected boolean isCancelled() {
        assert scanner != null : "ASSERTION [line=178, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";

        return scanner.isInterrupted();
    }

    /**
     * Creates temp file in temp directory.
     *
     * @param fileName File name.
     * @param tmpDir dir to creating file.
     * @return created file.
     * @throws IOException if error occur.
     */
    protected File createTempFile(String fileName, File tmpDir) throws IOException {
        assert fileName != null : "ASSERTION [line=192, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";

        int idx = fileName.lastIndexOf('.');

        if (idx == -1) {
            idx = fileName.length();
        }

        String prefix = fileName.substring(0, idx);
        if (idx < 3) { // Prefix must be at least 3 characters long. See File.createTempFile(...).
            prefix += "___";
        }

        String sufix = fileName.substring(idx);

        return File.createTempFile(prefix, sufix, tmpDir);
    }

    /**
     * Gets file URI for the given file name. It extends any given name with {@link #uri}.
     *
     * @param name File name.
     * @return URI for the given file name.
     */
    protected String getFileUri(String name) {
        assert name != null : "ASSERTION [line=217, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/GridUriDeploymentScanner.java]";

        String fileUri = uri.toString();

        fileUri = fileUri.length() > 0 && fileUri.charAt(fileUri.length() - 1) == '/' ? fileUri + name :
            fileUri + '/' + name;

        return fileUri;
    }

    /**
     * Tests whether first scan completed or not.
     *
     * @return <tt>true</tt> if first scan has been already completed and
     *      <tt>false</tt> otherwise.
     */
    protected boolean isFirstScan() {
        return firstScan;
    }

    /**
     * Gets deployment URI.
     *
     * @return Deployment URI.
     */
    protected final URI getUri() {
        return uri;
    }

    /**
     * Gets deployment frequency.
     *
     * @return Deployment frequency.
     */
    protected final long getFrequency() {
        return freq;
    }

    /**
     * Gets temporary deployment directory.
     *
     * @return Temporary deployment directory.
     */
    protected final File getDeployDirectory() {
        return deployDir;
    }

    /**
     * Gets filter for found files. Before {@link #listener} is notified about
     * changes with certain file last should be accepted by filter.
     *
     * @return New, updated or deleted file filter.
     */
    protected final FilenameFilter getFilter() {
        return filter;
    }

    /**
     * Gets deployment listener.
     *
     * @return Listener which should be notified about all deployment events
     *      by scanner.
     */
    protected final GridUriDeploymentScannerListener getListener() {
        return listener;
    }

    /**
     * Gets scanner logger.
     *
     * @return Logger.
     */
    protected final GridLogger getLogger() {
        return log;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentScanner.class, this,
            "uri", uri != null ? GridUriDeploymentUtils.hidePassword(uri.toString()) : null);
    }
}
