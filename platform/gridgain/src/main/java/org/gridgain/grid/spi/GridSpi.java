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

package org.gridgain.grid.spi;

import java.io.*;
import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.resources.*;
import org.gridgain.jsr305.*;

/**
 * This interface defines life-cycle of SPI implementation. Every SPI implementation should implement
 * this interface. Kernal will not load SPI that doesn't implement this interface.
 * <p>
 * Grid SPI's can be injected using IoC (dependency injection)
 * with grid resources. Both, field and method based injection are supported.
 * The following grid resources can be injected:
 * <ul>
 * <li>{@link GridLoggerResource}</li>
 * <li>{@link GridLocalNodeIdResource}</li>
 * <li>{@link GridHomeResource}</li>
 * <li>{@link GridMBeanServerResource}</li>
 * <li>{@link GridExecutorServiceResource}</li>
 * <li>{@link GridMarshallerResource}</li>
 * <li>{@link GridSpringApplicationContextResource}</li>
 * <li>{@link GridSpringResource}</li>
 * </ul>
 * Refer to corresponding resource documentation for more information.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridSpi {
    /**
     * Gets SPI name.
     *
     * @return SPI name.
     */
    public String getName();

    /**
     * This method is called before SPI starts (before method {@link #spiStart(String)}
     * is called). It allows SPI implementation to add attributes to a local
     * node. Kernel collects these attributes from all SPI implementations
     * loaded up and then passes it to discovery SPI so that they can be
     * exchanged with other nodes.
     *
     * @return Map of local node attributes this SPI wants to add.
     * @throws GridSpiException Throws in case of any error.
     */
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException;

    /**
     * This method is called to start SPI. After this method returns
     * successfully kernel assumes that SPI is fully operational.
     *
     * @param gridName Name of grid instance this SPI is being started for
     *    (<tt>null</tt> for default grid).
     * @throws GridSpiException Throws in case of any error during SPI start.
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException;

    /**
     * Callback invoked when SPI context is initialized. SPI implementation
     * may store SPI context for future access.
     * <p>
     * This method is invoked after {@link #spiStart(String)} method is
     * completed, so SPI should be fully functional at this point. Use this
     * method for post-start initialization, such as subscribing a discovery
     * listener, sending a message to remote node, etc...
     *
     * @param spiCtx Spi context.
     * @throws GridSpiException If context initialization failed (grid will be stopped).
     */
    public void onContextInitialized(GridSpiContext spiCtx) throws GridSpiException;

    /**
     * Callback invoked prior to stopping grid before SPI context is destroyed.
     * Once this method is complete, grid will begin shutdown sequence. Use this
     * callback for de-initialization logic that may involve SPI context. Note that
     * invoking SPI context after this callback is complete is considered
     * illegal and may produce unknown results.
     * <p>
     * If {@link GridSpiAdapter} is used for SPI implementation, then it will
     * replace actual context with dummy no-op context which is usually good-enough
     * since grid is about to shut down.
     */
    public void onContextDestroyed();

    /**
     * This method is called to stop SPI. After this method returns kernel
     * assumes that this SPI is finished and all resources acquired by it
     * are released.
     * <b>
     * Note that this method can be called at any point including during
     * recovery of failed start. It should make no assumptions on what state SPI
     * will be in when this method is called.
     * </b>
     *
     * @throws GridSpiException Thrown in case of any error during SPI stop.
     */
    public void spiStop() throws GridSpiException;
}
