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

/**
 * Resource injector implementations contain logic and resources that
 * should be injected for selected target objects.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
interface GridResourceInjector {
    /**
     * Injects resource into field. Caches injected resource with the given key if needed.
     *
     * @param field Field to inject.
     * @param target Target object the field belongs to.
     * @param depCls Deployment class.
     * @throws GridException If injection failed.
     */
    public void inject(GridResourceField field, Object target, GridDeploymentClass depCls) throws GridException;

    /**
     * Injects resource with a setter method. Caches injected resource with the given key if needed.
     *
     * @param mtd Setter method.
     * @param target Target object the field belongs to.
     * @param depCls Deployment class.
     * @throws GridException If injection failed.
     */
    public void inject(GridResourceMethod mtd, Object target, GridDeploymentClass depCls) throws GridException;

    /**
     * Remove cached resources from cache by key.
     *
     * @param ldr Class loader.
     */
    public void undeploy(ClassLoader ldr);
}
