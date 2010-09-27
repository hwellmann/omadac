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

package org.gridgain.grid.spi.checkpoint.coherence;

import com.tangosol.net.*;
import java.util.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.resources.*;

/**
 * This class defines Coherence-based checkpoint SPI implementation. All checkpoints are
 * stored in distributed cache and available from all nodes in the grid. Note that every
 * node must have access to the cache. The reason of having it is because a job state
 * can be saved on one node and loaded on another (e.g., if a job gets
 * preempted on a different node after node failure).
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 * <li>Cache name (see {@link #setCacheName(String)})</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * <tt>GridCoherenceCheckpointSpi</tt> can be configured as follows:
 * <pre name="code" class="java">
 * GridCoherenceCheckpointSpi checkpointSpi = new GridCoherenceCheckpointSpi();
 *
 * // Override default cache name.
 * checkpointSpi.setCacheName("myCacheName");
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default checkpoint SPI.
 * cfg.setCheckpointSpi(checkpointSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * <tt>GridCoherenceCheckpointSpi</tt> can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *     ...
 *     &lt;property name="checkpointSpi"&gt;
 *         &lt;bean class="org.gridgain.grid.spi.checkpoint.coherence.GridCoherenceCheckpointSpi"&gt;
 *             &lt;!-- Change to own cache name in your environment. --&gt;
 *             &lt;property name="cacheName" value="myCacheName"/&gt;
 *         &lt;/bean&gt;
 *     &lt;/property&gt;
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * <h1 class="header">Availability</h1>
 * <b>Note</b>: Coherence is not shipped with GridGain. If you don't have Coherence, you need to
 * download it separately. See <a target=_blank href="http://www.tangosol.com">http://www.tangosol.com</a> for
 * more information. Once installed, Coherence should be available on the classpath for
 * GridGain. If you use <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> script to start
 * a grid node you can simply add Coherence JARs to <tt>[GRIDGAIN_HOME]/bin/setenv.{sh|bat}</tt>
 * scripts that's used to set up class path for the main scripts.
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridCoherenceCheckpointSpi extends GridSpiAdapter implements GridCheckpointSpi,
    GridCoherenceCheckpointSpiMBean {
    /** Default Coherence cache name (value is <tt>gridgain.checkpoint.cache</tt>). */
    public static final String DFLT_CACHE_NAME = "gridgain.checkpoint.cache";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** Cache name. */
    private String cacheName = DFLT_CACHE_NAME;

    /** Coherence cache. */
    private NamedCache cache = null;

    /**
     * Sets name for Coherence cache used in grid.
     * <p>
     * If not provided, default value is {@link #DFLT_CACHE_NAME}.
     *
     * @param cacheName Coherence cache name used in grid.
     */
    @GridSpiConfiguration(optional = true)
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        assertParameter(cacheName != null, "cacheName != null");

        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("cacheName", cacheName));
        }

        cache = CacheFactory.getCache(cacheName);

        if (cache == null) {
            throw (GridSpiException)new GridSpiException("Failed to obtain Coherence Cache for cacheName:" + cacheName).setData(149, "src/java/org/gridgain/grid/spi/checkpoint/coherence/GridCoherenceCheckpointSpi.java");
        }

        registerMBean(gridName, this, GridCoherenceCheckpointSpiMBean.class);

        if (log.isInfoEnabled() ==true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        cache = null;

        // Ack ok stop.
        if (log.isInfoEnabled() ==true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] loadCheckpoint(String key) throws GridSpiException {
        assert key != null : "ASSERTION [line=177, file=src/java/org/gridgain/grid/spi/checkpoint/coherence/GridCoherenceCheckpointSpi.java]";

        GridCoherenceCheckpointData data = (GridCoherenceCheckpointData)cache.get(key);

        return data == null ? null : data.getState();
    }

    /**
     * {@inheritDoc}
     */
    public void saveCheckpoint(String key, byte[] state, long timeout) throws GridSpiException {
        assert key != null : "ASSERTION [line=188, file=src/java/org/gridgain/grid/spi/checkpoint/coherence/GridCoherenceCheckpointSpi.java]";

        cache.put(key, new GridCoherenceCheckpointData(state), timeout);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCheckpoint(String key) {
        assert key != null : "ASSERTION [line=197, file=src/java/org/gridgain/grid/spi/checkpoint/coherence/GridCoherenceCheckpointSpi.java]";

        return cache.remove(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(2);
        
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));
        
        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCoherenceCheckpointSpi.class, this);
    }
}
