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

package org.gridgain.grid.kernal.managers.deployment;

import java.util.*;
import org.gridgain.grid.*;

/**
 * Interface for all deployment stores.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridDeploymentStore {
    /**
     * Starts store.
     *
     * @throws GridException If start failed.
     */
    public void start() throws GridException;

    /**
     * Stops store.
     */
    public void stop();

    /**
     * Kernal started callback.
     *
     * @throws GridException If callback execution failed.
     */
    public void onKernalStart() throws GridException;

    /**
     * Kernel stopping callback.
     */
    public void onKernalStop();

    /**
     *
     * @param meta Deployment metadata.
     * @return Deployment.
     */
    public GridDeploymentClass acquireClass(GridDeploymentMetadata meta);

    /**
     * Gets class loader based on ID.
     *
     * @param ldrId Class loader ID.
     * @return Class loader of <tt>null</tt> if not found.
     */
    public ClassLoader getClassLoader(UUID ldrId);

    /**
     * @param cls Class to release.
     */
    public void releaseClass(GridDeploymentClass cls);

    /**
     *
     * @return All current deployments.
     */
    public Collection<GridDeploymentClass> getDeployments();

    /**
     * Explicitely deploys class.
     *
     * @param cls Class to explicitly deploy.
     * @param clsLdr Class loader.
     * @throws GridException Id deployment failed.
     */
    public void explicitDeploy(Class<?> cls, ClassLoader clsLdr) throws GridException;

    /**
     * @param nodeId ID of node that initiated request.
     * @param rsrcName Undeploys all deployments that have given
     */
    public void explicitUndeploy(UUID nodeId, String rsrcName);
}
