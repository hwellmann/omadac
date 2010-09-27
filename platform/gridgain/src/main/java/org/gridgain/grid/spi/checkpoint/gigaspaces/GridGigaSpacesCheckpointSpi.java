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

package org.gridgain.grid.spi.checkpoint.gigaspaces;

import com.j_spaces.core.*;
import com.j_spaces.core.client.*;
import java.util.*;
import net.jini.core.lease.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.resources.*;
import org.openspaces.core.*;
import org.openspaces.core.space.*;

/**
 * This class defines GigaSpaces-based implementation for checkpoint SPI.
 * All checkpoints are stored in distributed cache and available for all
 * nodes in the grid. Note that every node must have access to the cache. The
 * reason for having it is because a job state can be saved on one node and
 * loaded on another (e.g. if a job gets preempted on a different node after node failure).
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters.
 * <ul>
 * <li>GigaSpaces URL (see {@link #setSpaceUrl(String)})</li>
 * </ul>
 * <ul>
 * <li>GigaSpaces Space (see {@link #setSpace(GigaSpace)})</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridGigaSpacesCheckpointSpi can be configured as follows:
 * <pre name="code" class="java">
 * GridGigaSpacesCheckpointSpi checkpointSpi = new GridGigaSpacesCheckpointSpi();
 *
 * // Provide GigaSpaces URL.
 * checkpointSpi.setSpaceUrl("/./mySpace");
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
 * GridGigaSpacesCheckpointSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *     ...
 *     &lt;property name="checkpointSpi"&gt;
 *         &lt;bean class="org.gridgain.grid.spi.checkpoint.gigaspaces.GridGigaSpacesCheckpointSpi"&gt;
 *             &lt;!-- Change to GigaSpaces URL in your environment. --&gt;
 *             &lt;property name="spaceUrl" value="/./mySpace"/&gt;
 *         &lt;/bean&gt;
 *     &lt;/property&gt;
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * <h1 class="header">Availability</h1>
 * Note that GigaSpaces is not shipped with GridGain. To use this SPI you need to
 * have GigaSpaces installed or download it separately:
 * <ul>
 * <li>See <a href="http://www.gigaspaces.com">www.gigaspaces.com</a> for general information.</li>
 * <li>Download GigaSpaces 6 Community Edition at <a href="http://www.gigaspaces.com/os_downloads.html">http://www.gigaspaces.com/os_downloads.html</a></li>
 * <li>See Open Spaces documentation at <a href="http://www.gigaspaces.com/wiki/display/GS6/Open+Spaces">http://www.gigaspaces.com/wiki/display/GS6/Open+Spaces</a></li>
 * </ul>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridCheckpointSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridGigaSpacesCheckpointSpi extends GridSpiAdapter implements GridCheckpointSpi,
    GridGigaSpacesCheckpointSpiMBean {
    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** Space provided outside by user or configuration. */
    @GridToStringExclude
    private GigaSpace gigaSpace = null;

    /** Internally created space for given URL. */
    @GridToStringExclude
    private GigaSpace space = null;

    /** Space URL. */
    private String spaceUrl = null;

    /**
     * {@inheritDoc}
     */
    public String getSpaceUrl() {
        return spaceUrl;
    }

    /**
     * Sets GigaSpaces connection URL.
     *
     * @param spaceUrl GigaSpaces URL to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setSpaceUrl(String spaceUrl) {
        this.spaceUrl = spaceUrl;
    }

    /**
     * {@inheritDoc}
     */
    public GigaSpace getSpace() {
        return gigaSpace;
    }

    /**
     * Sets GigaSpace gigaSpace object.
     *
     * @param gigaSpace GigaSpace gigaSpace.
     */
    @GridSpiConfiguration(optional = true)
    public void setSpace(GigaSpace gigaSpace) {
        this.gigaSpace = gigaSpace;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        assertParameter(spaceUrl != null || gigaSpace != null, "spaceUrl != null || gigaSpace != null");

        // Only one should be set.
        assertParameter(spaceUrl == null && gigaSpace != null || spaceUrl != null && gigaSpace == null,
            "spaceUrl == null && gigaSpace != null || spaceUrl != null && gigaSpace == null");

        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("spaceUrl", spaceUrl));
            log.info(configInfo("gigaSpace", gigaSpace));
        }

        if (gigaSpace == null) {
            UrlSpaceFactoryBean urlBeanFactory = new UrlSpaceFactoryBean();

            urlBeanFactory.setUrl(spaceUrl);

            urlBeanFactory.afterPropertiesSet();

            GigaSpaceFactoryBean spaceBeanFactory = new GigaSpaceFactoryBean();

            try {
                spaceBeanFactory.setSpace((IJSpace)urlBeanFactory.getObject());

                spaceBeanFactory.afterPropertiesSet();

                space = (GigaSpace)spaceBeanFactory.getObject();

                if (space == null) {
                    throw (GridSpiException)new GridSpiException("Failed to obtain GigaSpace for URL: " + spaceUrl).setData(194, "src/java/org/gridgain/grid/spi/checkpoint/gigaspaces/GridGigaSpacesCheckpointSpi.java");
                }
            }
            catch (Exception e) {
                String errMsg = "Failed to obtain GigaSpace.";

                log.error(errMsg, e);

                throw (GridSpiException)new GridSpiException(errMsg, e).setData(202, "src/java/org/gridgain/grid/spi/checkpoint/gigaspaces/GridGigaSpacesCheckpointSpi.java");
            }
        }
        else {
            space = gigaSpace;
        }

        registerMBean(gridName, this, GridGigaSpacesCheckpointSpiMBean.class);

        if (log.isInfoEnabled() ==true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        // Ack ok stop.
        if (log.isInfoEnabled() ==true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] loadCheckpoint(String key) throws GridSpiException {
        assert key != null : "ASSERTION [line=232, file=src/java/org/gridgain/grid/spi/checkpoint/gigaspaces/GridGigaSpacesCheckpointSpi.java]";

        GridGigaSpacesCheckpointData cpData = space.read(getEntry(key, null), 0);

        return cpData != null ? cpData.state : null;
    }

    /**
     * Creates and returns template for the given key and state.
     *
     * @param key Checkpoint key.
     * @param state Checkpoint state.
     * @return Template with embedded entry info.
     */
    private GridGigaSpacesCheckpointData getEntry(String key, byte[] state) {
        GridGigaSpacesCheckpointData entry = new GridGigaSpacesCheckpointData(state);

        EntryInfo info = new EntryInfo(key , 0);

        // GigaSpaces naming...
        entry.__setEntryInfo(info);

        return entry;
    }

    /**
     * {@inheritDoc}
     */
    public void saveCheckpoint(String key, byte[] state, long timeout) throws GridSpiException {
        assert key != null : "ASSERTION [line=261, file=src/java/org/gridgain/grid/spi/checkpoint/gigaspaces/GridGigaSpacesCheckpointSpi.java]";

        // Infinite timeout means maximum possible value.
        if (timeout == 0) {
            timeout = Lease.FOREVER;
        }

        space.write(getEntry(key, state), timeout);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCheckpoint(String key) {
        assert key != null : "ASSERTION [line=275, file=src/java/org/gridgain/grid/spi/checkpoint/gigaspaces/GridGigaSpacesCheckpointSpi.java]";

        return space.take(getEntry(key, null)) != null;
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
        return GridToStringBuilder.toString(GridGigaSpacesCheckpointSpi.class, this);
    }
}
