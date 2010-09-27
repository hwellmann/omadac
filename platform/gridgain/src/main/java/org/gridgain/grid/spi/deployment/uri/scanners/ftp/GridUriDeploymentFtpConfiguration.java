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

import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentFtpConfiguration {
    /** */
    private static final String DFLT_DIR = "/";

    /** */
    private String host = null;

    /** */
    private int port = 21;

    /** */
    private String username = null;

    /** */
    @GridToStringExclude
    private String pswd = null;

    /** */
    private String dir = DFLT_DIR;

    /**
     *
     * @return FIXDOC
     */
    String getHost() {
        return host;
    }

    /**
     *
     * @param host FTP host.
     */
    void setHost(String host) {
        assert host != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpConfiguration.java]";

        this.host = host;
    }

    /**
     *
     * @return FIXDOC
     */
    int getPort() {
        return port;
    }

    /**
     *
     * @param port FTP port.
     */
    void setPort(int port) {
        assert port > 0 : "ASSERTION [line=83, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpConfiguration.java]";

        this.port = port;
    }

    /**
     *
     * @return FIXDOC
     */
    String getUsername() {
        return username;
    }

    /**
     *
     * @param username FTP username.
     */
    void setUsername(String username) {
        assert username != null : "ASSERTION [line=101, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpConfiguration.java]";

        this.username = username;
    }

    /**
     *
     * @return FIXDOC
     */
    String getPassword() {
        return pswd;
    }

    /**
     *
     * @param pswd FTP password.
     */
    void setPassword(String pswd) {
        assert pswd != null : "ASSERTION [line=119, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpConfiguration.java]";

        this.pswd = pswd;
    }

    /**
     *
     * @return FIXDOC
     */
    String getDirectory() {
        return dir;
    }

    /**
     *
     * @param dir FTP remote directory.
     */
    void setDirectory(String dir) {
        this.dir = dir == null ? DFLT_DIR : dir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentFtpConfiguration.class, this);
    }
}
