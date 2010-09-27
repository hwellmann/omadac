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

import org.gridgain.apache.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.*;

/**
 * Grid deployment SPI is in charge of deploying tasks and classes from different
 * sources.
 * <p>
 * Class loaders that are in charge of loading task classes (and other classes)
 * can be deployed directly by calling {@link #register(ClassLoader, Class)}} method or
 * by SPI itself, for example by asynchronously scanning some folder for new tasks.
 * When method {@link #findResource(String)} is called by the system, SPI must return a
 * class loader associated with given class. Every time a class loader
 * gets (re)deployed or released, callbacks
 * {@link GridDeploymentListener#onUnregistered(ClassLoader)}} must be called by SPI.
 * <p>
 * If peer class loading is enabled (which is default behavior, see
 * {@link GridConfiguration#isPeerClassLoadingEnabled()}), then it is usually
 * enough to deploy class loader only on one grid node. Once a task starts executing
 * on the grid, all other nodes will automatically load all task classes from
 * the node that initiated the execution. Hot redeployment is also supported
 * with peer class loading. Every time a task changes and gets redeployed on a
 * node, all other nodes will detect it and will redeploy this task as well.
 * <strong>
 * Note that peer class loading comes into effect only if a task was
 * not locally deployed, otherwise, preference will always be given to
 * local deployment.
 * </strong>
 * <p>
 * Gridgain provides the following <tt>GridDeploymentSpi</tt> implementations:
 * <ul>
 * <li>{@link org.gridgain.grid.spi.deployment.local.GridLocalDeploymentSpi}</li>
 * <li>{@link org.gridgain.grid.spi.deployment.uri.GridUriDeploymentSpi}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridDeploymentSpi extends GridSpi {
    /**
     * Finds class loader for the given class.
     *
     * @param rsrcName Class name or class alias to find class loader for.
     * @return Deployed class loader, or <tt>null</tt> if not deployed.
     */
    public GridDeploymentResource findResource(String rsrcName);

    /**
     * Registers a class loader with this SPI. This method exists
     * to be able to add external class loaders to deployment SPI.
     * Deployment SPI may also have its own class loaders. For example,
     * in case of GAR deployment, every GAR file is loaded and deployed
     * with a separate class loader maintained internally by the SPI.
     * <p>
     * The array of classes passed in should be checked for presence of
     * {@link GridTaskName} annotations. The classes that have this annotation
     * should be accessible by this name from {@link #findResource(String)} method.
     *
     * @param ldr Class loader to register.
     * @param rsrc Class that should be checked for aliases.
     *      Currently the only alias in the system is {@link GridTaskName} for
     *      task classes; in future, there may be others.
     * @return <tt>True</tt> if resource was registered.
     * @throws GridSpiException If registration failed.
     */
    public boolean register(ClassLoader ldr, Class<?> rsrc) throws GridSpiException;

    /**
     * Unregisters all class loaders that have a class with given name or have
     * a class with give {@link GridTaskName} value.
     *
     * @param rsrcName Either class name or {@link GridTaskName} value for a class
     *      whose class loader needs to be unregistered.
     * @return <tt>True</tt> if resource was unregistered.
     */
    public boolean unregister(String rsrcName);

    /**
     * Sets deployment event listener. Grid implementation will use this listener
     * to properly add and remove various deployments.
     *
     * @param lsnr Listener for deployment events.
     */
    public void setListener(GridDeploymentListener lsnr);
}
