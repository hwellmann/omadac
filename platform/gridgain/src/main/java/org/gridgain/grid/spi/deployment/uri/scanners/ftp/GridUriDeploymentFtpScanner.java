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

package org.gridgain.grid.spi.deployment.uri.scanners.ftp;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.deployment.uri.scanners.*;
import org.gridgain.grid.spi.deployment.uri.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FTP scanner scans directory for new files. Scanned directory defined in URI.
 * Scanner doesn't search files in subfolders.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridUriDeploymentFtpScanner extends GridUriDeploymentScanner {
    /** */
    private static final long UNKNOWN_FILE_TSTAMP = -1;

    /** */
    private final GridUriDeploymentFtpConfiguration cfg;

    /** Cache of found files to check if any of it has been updated. */
    private Map<GridUriDeploymentFtpFile, Long> cache = new HashMap<GridUriDeploymentFtpFile, Long>();

    /**
     *
     * @param gridName Grid instance name.
     * @param uri FTP URI.
     * @param deployDir FTP directory.
     * @param freq Scanner frequency.
     * @param filter Scanner filter.
     * @param listener Deployment listener.
     * @param log Logger to use.
     * @throws GridSpiException Thrown in case of any error.
     */
    public GridUriDeploymentFtpScanner(
        String gridName,
        URI uri,
        File deployDir,
        long freq,
        FilenameFilter filter,
        GridUriDeploymentScannerListener listener,
        GridLogger log) throws GridSpiException {
        super(gridName, uri, deployDir, freq, filter, listener, log);

        cfg = initializeFtpConfiguration(uri);

        assert cfg != null : "ASSERTION [line=73, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpScanner.java]";

        testConnection();
   }

    /**
     *
     * @param uri FTP URI.
     * @return FTP configuration.
     */
    private GridUriDeploymentFtpConfiguration initializeFtpConfiguration(URI uri) {
        assert "ftp".equals(uri.getScheme()) == true : "ASSERTION [line=84, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpScanner.java]";

        GridUriDeploymentFtpConfiguration cfg = new GridUriDeploymentFtpConfiguration();

        String userInfo = uri.getUserInfo();
        String username = null;
        String pswd = null;

        if (userInfo != null) {
            String[] arr = userInfo.split(";");

            if (arr != null && arr.length > 0) {
                for (String el : arr) {
                    if (el.startsWith("freq=") == true) {
                        // No-op.
                    }
                    else if (el.indexOf(':') != -1) {
                        int idx = el.indexOf(':');

                        username = el.substring(0, idx);
                        pswd = el.substring(idx + 1);
                    }
                    else {
                        username = el;
                    }
                }
            }
        }

        // Username and password must be defined in URI.
        assert username != null : "ASSERTION [line=114, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpScanner.java]";
        assert pswd != null : "ASSERTION [line=115, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpScanner.java]";

        cfg.setHost(uri.getHost());
        cfg.setPort(uri.getPort());
        cfg.setUsername(username);
        cfg.setPassword(pswd);
        cfg.setDirectory(uri.getPath());

        return cfg;
    }

    /**
     * Checks that ftp can be opened.
     *
     * @throws GridSpiException Thrown in case of any error.
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    private void testConnection() throws GridSpiException {
        GridUriDeploymentFtpClient ftp = new GridUriDeploymentFtpClient(cfg, getLogger());

        try {
            ftp.connect();
        }
        catch (GridUriDeploymentFtpException e) {
            throw (GridSpiException)new GridSpiException("Failed to initialize FTP client.", e).setData(139, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpScanner.java");
        }
        finally {
            try {
                ftp.close();
            }
            catch (GridUriDeploymentFtpException e) {
                getLogger().warning("Failed to close FTP client.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() {
        Set<GridUriDeploymentFtpFile> foundFiles = new HashSet<GridUriDeploymentFtpFile>(cache.size());

        long start = System.currentTimeMillis();

        processFtp(foundFiles);

        if (getLogger().isDebugEnabled() == true) {
            getLogger().debug("FTP scanner time in milliseconds: " + (System.currentTimeMillis() - start));
        }

        if (isFirstScan() == false) {
            Set<GridUriDeploymentFtpFile> delFiles = new HashSet<GridUriDeploymentFtpFile>(cache.keySet());

            delFiles.removeAll(foundFiles);

            if (delFiles.size() > 0) {
                List<String> uris = new ArrayList<String>();

                for (GridUriDeploymentFtpFile file : delFiles) {
                    Long tstamp = cache.get(file);

                    // Ignore files in cache w/o timestamp.
                    if (tstamp != null && tstamp != UNKNOWN_FILE_TSTAMP) {
                        uris.add(getFileUri(file.getName()));
                    }
                }

                cache.keySet().removeAll(delFiles);

                getListener().onDeletedFiles(uris);
            }
        }
    }

    /**
     *
     * @param files File to process.
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    private void processFtp(Set<GridUriDeploymentFtpFile> files) {
        GridUriDeploymentFtpClient ftp = new GridUriDeploymentFtpClient(cfg, getLogger());

        try {
            ftp.connect();

            for (GridUriDeploymentFtpFile file : ftp.getFiles()) {
                String fileName = file.getName();

                if (getFilter().accept(null, fileName.toLowerCase()) == true && file.isFile() == true) {
                    files.add(file);

                    Long lastModified = cache.get(file);

                    Calendar fileTstamp = file.getTimestamp();

                    if (fileTstamp == null) {
                        if (lastModified == null) {
                            // Add new file in cache to avoid print warning every time.
                            cache.put(file, UNKNOWN_FILE_TSTAMP);

                            getLogger().warning("File with unknown timestamp will be ignored " +
                                "(check FTP server configuration): " + file);
                        }
                    }
                    // If file is new or has been modified.
                    else if (lastModified == null || lastModified != fileTstamp.getTimeInMillis()) {
                        cache.put(file, fileTstamp.getTimeInMillis());

                        if (getLogger().isDebugEnabled() == true) {
                            getLogger().debug("Discovered deployment file or directory: " + file);
                        }

                        try {
                            File diskFile = createTempFile(fileName, getDeployDirectory());

                            ftp.downloadToFile(file, diskFile);

                            String fileUri = getFileUri(fileName);

                            // Delete file when JVM stopped.
                            diskFile.deleteOnExit();

                            // Deployment SPI call.
                            // NOTE: If SPI listener blocks then FTP connection may be closed by timeout.
                            getListener().onNewOrUpdatedFile(diskFile, fileUri, fileTstamp.getTimeInMillis());
                        }
                        catch (IOException e) {
                            getLogger().error("Failed to download file: " + fileName, e);
                        }
                    }
                }
            }
        }
        catch (GridUriDeploymentFtpException e) {
            if (isCancelled() == false) {
                getLogger().error("Error while getting files.", e);
            }
        }
        finally {
            try {
                ftp.close();
            }
            catch (GridUriDeploymentFtpException e) {
                getLogger().warning("Failed to close FTP client.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentFtpScanner.class, this,
            "uri", getUri() != null ? GridUriDeploymentUtils.hidePassword(getUri().toString()) : null,
            "freq", getFrequency(),
            "deployDir", getDeployDirectory());
    }
}
