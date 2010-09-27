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

package org.gridgain.grid.kernal.processors.resource;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Simple injector which wraps only one resource object.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <T> Type of injected resource.
 */
class GridResourceBasicInjector<T> implements GridResourceInjector {
    /** Resource to inject. */
    private final T rsrc;

    /**
     * Creates injector.
     *
     * @param rsrc Resource to inject.
     */
    GridResourceBasicInjector(T rsrc) {
        this.rsrc = rsrc;
    }

    /**
     * Gets resource.
     *
     * @return Resource
     */
    public T getResource() {
        return rsrc;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void inject(GridResourceField field, Object target,
        GridDeploymentClass depCls) throws GridException {
        GridResourceUtils.inject(field.getField(), target, rsrc);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void inject(GridResourceMethod mtd, Object target,
        GridDeploymentClass depCls)
        throws GridException {
        GridResourceUtils.inject(mtd.getMethod(), target, rsrc);
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(ClassLoader ldr) {
        // No-op. There is no cache.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridResourceBasicInjector.class, this);
    }
}
