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

import com.enterprisedt.net.ftp.*;
import java.io.*;
import java.text.*;
import java.util.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentFtpClient {
    /** Timeout in milliseconds on the underlying socket. */
    private static final int TIMEOUT = 60000;

    /** */
    private final GridUriDeploymentFtpConfiguration cfg;

    /** */
    private final GridLogger log;

    /** */
    private FTPClient ftp = null;

    /** */
    private boolean isConnected = false;

    /**
     *
     * @param cfg FTP configuration.
     * @param log Logger to use.
     */
    GridUriDeploymentFtpClient(GridUriDeploymentFtpConfiguration cfg, GridLogger log) {
        assert cfg != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java]";
        assert log != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java]";

        this.cfg = cfg;
        this.log = log;
    }


    /**
     *
     * @param rmtFile Remote file.
     * @param localFile Local file.
     * @throws GridUriDeploymentFtpException Thrown in case of any error.
     */
    void downloadToFile(GridUriDeploymentFtpFile rmtFile, File localFile) throws GridUriDeploymentFtpException {
        assert ftp != null : "ASSERTION [line=75, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java]";
        assert rmtFile != null : "ASSERTION [line=76, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java]";
        assert localFile != null : "ASSERTION [line=77, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java]";

        String dirName = rmtFile.getParentDirectrory();

        if (dirName.length() == 0 || '/' != dirName.charAt(dirName.length() - 1)) {
            dirName += '/';
        }

        String srcPath = dirName + rmtFile.getName();

        BufferedOutputStream out = null;

        try {
            try {
                out = new BufferedOutputStream(new FileOutputStream(localFile));

                ftp.get(out, srcPath);
            }
            finally {
                GridUtils.close(out, log);
            }
        }
        catch (IOException e) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to download file [rmtFile=" + srcPath + ", localFile=" +
                localFile + ']', e).setData(100, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }
        catch (FTPException e) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to download file [rmtFile=" + srcPath + ", localFile=" +
                localFile + ']', e).setData(104, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }
    }

    /**
     *
     * @throws GridUriDeploymentFtpException Thrown in case of any error.
     */
    void connect() throws GridUriDeploymentFtpException {
        ftp = new FTPClient();

        try {
            ftp.setRemoteHost(cfg.getHost());
            ftp.setRemotePort(cfg.getPort());

            // Set socket timeout to avoid an infinite timeout.
            ftp.setTimeout(TIMEOUT);

            ftp.connect();

            ftp.login(cfg.getUsername(), cfg.getPassword());

            // Set up passive binary transfers.
            ftp.setConnectMode(FTPConnectMode.PASV);
            ftp.setType(FTPTransferType.BINARY);

            if (ftp.connected() == false) {
                ftp.quit();

                throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("FTP server refused connection [host=" + cfg.getHost() +
                    ", port=" + cfg.getPort() + ", username=" + cfg.getUsername() + ']').setData(134, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
            }
        }
        catch (IOException e) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to connect to host [host=" + cfg.getHost() +
                ", port=" + cfg.getPort() + ']', e).setData(139, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }
        catch (FTPException e) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to connect to host [host=" + cfg.getHost() +
                ", port=" + cfg.getPort() + ']', e).setData(143, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }

        isConnected = true;
    }

    /**
     *
     * @throws GridUriDeploymentFtpException Thrown in case of any error.
     */
    void close() throws GridUriDeploymentFtpException {
        if (isConnected == false) {
            return;
        }

        assert ftp != null : "ASSERTION [line=159, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java]";

        Exception e = null;

        try {
            ftp.quit();
        }
        catch (IOException e1) {
            e = e1;
        }
        catch (FTPException e1) {
            e = e1;
        }
        finally{
            if (ftp.connected() == true) {
                try {
                    ftp.quit();
                }
                catch (IOException e1) {
                    // Don't loose the initial exception.
                    if (e == null) {
                        e = e1;
                    }
                }
                catch (FTPException e1) {
                    // Don't loose the initial exception.
                    if (e == null) {
                        e = e1;
                    }
                }
            }
        }

        ftp = null;

        isConnected = false;

        if (e != null) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to close FTP client.", e).setData(197, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }
    }

    /**
     *
     * @return List of files.
     * @throws GridUriDeploymentFtpException Thrown in case of any error.
     */
    List<GridUriDeploymentFtpFile> getFiles() throws GridUriDeploymentFtpException {
        try {
            assert cfg.getDirectory() != null : "ASSERTION [line=208, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java]";

            List<GridUriDeploymentFtpFile> clientFiles = new ArrayList<GridUriDeploymentFtpFile>();

            FTPFile[] files = ftp.dirDetails(cfg.getDirectory());

            for (FTPFile file : files) {
                clientFiles.add(new GridUriDeploymentFtpFile(cfg.getDirectory(), file));
            }

            return clientFiles;
        }
        catch (IOException e) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to get files in directory: " + cfg.getDirectory(), e).setData(221, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }
        catch (FTPException e) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to get files in directory: " + cfg.getDirectory(), e).setData(224, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }
        catch (ParseException e) {
            throw (GridUriDeploymentFtpException)new GridUriDeploymentFtpException("Failed to get files in directory: " + cfg.getDirectory(), e).setData(227, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpClient.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentFtpClient.class, this);
    }
}
