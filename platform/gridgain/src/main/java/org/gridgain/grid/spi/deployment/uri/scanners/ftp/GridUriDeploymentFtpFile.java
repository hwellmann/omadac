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

import java.util.*;
import com.enterprisedt.net.ftp.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentFtpFile {
    /** */
    private final String dir;

    /** */
    private final FTPFile file;

    /**
     *
     * @param dir Remote FTP directory.
     * @param file FTP file.
     */
    GridUriDeploymentFtpFile(String dir, FTPFile file) {
        assert dir != null : "ASSERTION [line=47, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpFile.java]";
        assert file != null : "ASSERTION [line=48, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpFile.java]";
        assert file.getName() != null : "ASSERTION [line=49, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/ftp/GridUriDeploymentFtpFile.java]";

        this.dir = dir;
        this.file = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof GridUriDeploymentFtpFile == false) {
            return false;
        }

        final GridUriDeploymentFtpFile other = (GridUriDeploymentFtpFile)obj;

        return dir.equals(other.dir) != false && file.getName().equals(other.file.getName()) != false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int res = dir.hashCode();

        res = 29 * res + file.getName().hashCode();

        return res;
    }

    /**
     * @return FIXDOC
     */
    String getName() {
        return file.getName();
    }

    /**
     * @return FIXDOC
     */
    Calendar getTimestamp() {
        Date date = file.lastModified();

        Calendar cal = null;

        if (date != null) {
            cal = Calendar.getInstance();

            cal.setTime(date);
        }

        return cal;
    }

    /**
     * @return FIXDOC
     */
    boolean isDirectory() {
        return file.isDir();
    }

    /**
     * @return FIXDOC
     */
    boolean isFile() {
        return file.isDir() == false && file.isLink() == false;
    }

    /**
     * @return FIXDOC
     */
    String getParentDirectrory() {
        return dir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentFtpFile.class, this);
    }
}
