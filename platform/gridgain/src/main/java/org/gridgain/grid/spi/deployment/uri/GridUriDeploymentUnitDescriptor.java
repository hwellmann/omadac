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
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.deployment.uri.util.*;
import org.gridgain.grid.util.tostring.*;
import static org.gridgain.grid.spi.deployment.uri.GridUriDeploymentUnitDescriptor.Type.*;

/**
 * Container for information about tasks and file where classes placed. It also contains tasks instances.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentUnitDescriptor {
    /**
     * Container type.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    enum Type {
        /**
         * Container has reference to the file with tasks.
         */
        FILE,

        /**
         * Container keeps tasks deployed directly.
         */
        CLASS
    }

    /**
     * Container type.
     */
    private final Type type;

    /**
     * If type is {@link Type#FILE} contains URI of {@link #file} otherwise must be null.
     */
    @GridToStringExclude
    private final String uri;

    /**
     * If type is {@link Type#FILE} contains file with tasks otherwise must be null.
     */
    private final File file;

    /**
     * Tasks deployment timestamp.
     */
    private final long tstamp;

    /** */
    private final ClassLoader clsLdr;

    /** Map of all resources. */
    private final Map<String, String> rsrcs = new HashMap<String, String>();

    /**
     * Constructs descriptor for GAR file.
     *
     * @param uri       GAR file URI.
     * @param file      File itself.
     * @param tstamp    Tasks deployment timestamp.
     * @param clsLdr Class loader.
     */
    GridUriDeploymentUnitDescriptor(String uri, File file, long tstamp, ClassLoader clsLdr) {
        assert uri != null : "ASSERTION [line=91, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentUnitDescriptor.java]";
        assert file != null : "ASSERTION [line=92, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentUnitDescriptor.java]";
        assert tstamp > 0 : "ASSERTION [line=93, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentUnitDescriptor.java]";

        this.uri = uri;
        this.file = file;
        this.tstamp = tstamp;
        this.clsLdr = clsLdr;
        type = FILE;
    }

    /**
     * Constructs deployment unit descriptor based on timestamp and {@link GridTask} instances.
     *
     * @param tstamp Tasks deployment timestamp.
     * @param clsLdr Class loader.
     */
    GridUriDeploymentUnitDescriptor(long tstamp, ClassLoader clsLdr) {
        assert clsLdr != null : "ASSERTION [line=109, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentUnitDescriptor.java]";
        assert tstamp > 0 : "ASSERTION [line=110, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentUnitDescriptor.java]";

        this.tstamp = tstamp;
        this.clsLdr = clsLdr;
        uri = null;
        file = null;
        type = CLASS;
    }

    /**
     * Gets descriptor type.
     *
     * @return Descriptor type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets file URL.
     *
     * @return <tt>null</tt> it tasks were deployed directly and reference to the GAR file URI if tasks were deployed
     *         from the file.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Tasks GAR file.
     *
     * @return <tt>null</tt> if tasks were deployed directly and GAR file if tasks were deployed from it.
     */
    public File getFile() {
        return file;
    }

    /**
     * Gets tasks deployment timestamp.
     *
     * @return Tasks deployment timestamp.
     */
    public long getTimestamp() {
        return tstamp;
    }

    /**
     * Deployed task.
     *
     * @return Deployed task.
     */
    public ClassLoader getClassLoader() {
        return clsLdr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return clsLdr.hashCode();
    }

    /**
     * Getter for property 'rsrcs'.
     *
     * @return Value for property 'rsrcs'.
     */
    public Map<String, String> getResources() {
        return rsrcs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof GridUriDeploymentUnitDescriptor == true &&
            clsLdr.equals(((GridUriDeploymentUnitDescriptor)obj).clsLdr) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentUnitDescriptor.class, this,
            "uri", GridUriDeploymentUtils.hidePassword(uri));
    }
}
