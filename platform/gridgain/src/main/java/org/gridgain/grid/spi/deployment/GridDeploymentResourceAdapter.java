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

package org.gridgain.grid.spi.deployment;

import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridDeploymentResourceAdapter implements GridDeploymentResource {
    /** */
    private final String name;

    /** */
    private final Class<?> rsrcCls;

    /** */
    private final ClassLoader clsLdr;

    /**
     * Creates resource.
     *
     * @param name Resource name.
     * @param rsrcCls Class.
     * @param clsLdr Class loader.
     */
    public GridDeploymentResourceAdapter(String name, Class<?> rsrcCls, ClassLoader clsLdr) {
        assert name != null : "ASSERTION [line=50, file=src/java/org/gridgain/grid/spi/deployment/GridDeploymentResourceAdapter.java]";
        assert rsrcCls != null : "ASSERTION [line=51, file=src/java/org/gridgain/grid/spi/deployment/GridDeploymentResourceAdapter.java]";
        assert clsLdr != null : "ASSERTION [line=52, file=src/java/org/gridgain/grid/spi/deployment/GridDeploymentResourceAdapter.java]";

        this.name = name;
        this.rsrcCls = rsrcCls;
        this.clsLdr = clsLdr;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getResourceClass() {
        return rsrcCls;
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader() {
        return clsLdr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        GridDeploymentResourceAdapter rsrc = (GridDeploymentResourceAdapter)obj;

        return clsLdr.equals(rsrc.clsLdr) == true && name.equals(rsrc.name) == true &&
            rsrcCls.equals(rsrc.rsrcCls) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int res = name.hashCode();

        res = 31 * res + rsrcCls.hashCode();
        res = 31 * res + clsLdr.hashCode();

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentResourceAdapter.class, this);
    }
}
