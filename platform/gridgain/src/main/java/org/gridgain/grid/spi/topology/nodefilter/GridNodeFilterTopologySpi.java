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

package org.gridgain.grid.spi.topology.nodefilter;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.jsr305.*;

/**
 * This class provides implementation for topology SPI based on {@link GridNodeFilter}.
 * The implementation returns nodes that are accepted by {@link GridNodeFilter} provided
 * in configuration. If no filters were provided, all nodes, local and remote,
 * will be included into topology.
 * <p>
 * This topology allows for fine grained node provisioning for grid task execution. Nodes
 * can be filtered based on any parameter available on {@link GridNode}. For example,
 * you can filter nodes based on operating system, number of CPU's, available heap memory,
 * average job execution time, current CPU load, any node attribute and about 50 more metrics
 * available in {@link GridNodeMetrics}. Take a look at the following
 * methods on {@link GridNode} interface which may be used for filtering:
 * <ul>
 * <li>
 *  {@link GridNode#getPhysicalAddress()} - in most cases this parameter represents the IP
 *  address the node.
 * </li>
 * <li>
 *  {@link GridNode#getAttributes()} - attributes attached to a grid node. Node
 *  attributes are specified in grid configuration via {@link GridConfiguration#getUserAttributes()}
 *  parameter. Note that all system and environment properties are automatically pre-set as
 *  node attributes for every node.
 * </li>
 * <li>
 *  {@link GridNode#getMetrics()}} - about <tt>50</tt> node metrics parameters that are periodically
 *  updated, such as heap, CPU, job counts, average job execution times, etc...
 * </li>
 * </ul>
 * <h1 class="header">Apache JEXL Node Filter</h1>
 * GridGain also comes with {@link GridJexlNodeFilter} implementation which allows you
 * to conveniently filter nodes based on Apache JEXL expression language. Refer to
 * <a href="http://commons.apache.org/jexl/">Apache JEXL</a> documentation for specifics of
 * JEXL expression language. {@link GridJexlNodeFilter} allows for a fairly simple way to
 * provide complex SLA-based task topology specifications. For example, the configuration
 * examples below show how the SPI can be configured with {@link GridJexlNodeFilter} to
 * include all Windows XP nodes with more than one processor or core and that are not loaded
 * over 50%.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 *      <li>
 *          {@link #setFilter(GridNodeFilter)} - Node filter
 *          that should be used for decision to accept node.
 *      </li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridNodeFilterTopologySpi needs to be explicitely configured.
 * <pre name="code" class="java">
 * GridNodeFilterTopologySpi topSpi = new GridNodeFilterTopologySpi();
 *
 * GridNodeFilter filter = new GridJexlNodeFilter(
 *     "node.metrics.availableProcessors > 1 && " + 
 *     "node.metrics.averageCpuLoad < 0.5 && " + 
 *     "node.attributes['os.name'] == 'Windows XP'");
 *
 * // Add filter.
 * topSpi.setFilter(filter);
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override topology SPI.
 * cfg.setTopologySpi(topSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridNodeFilterTopologySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *       ...
 *       &lt;property name="topologySpi"&gt;
 *           &lt;bean class="org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi"&gt;
 *               &lt;property name="filter"&gt;
 *                    &lt;bean class="org.gridgain.grid.GridJexlNodeFilter"&gt;
 *                        &lt;property name="expression"&gt;
 *                            &lt;value&gt;
 *                                &lt;![CDATA[node.metrics.availableProcessors > 1 &&
 *                                node.metrics.averageCpuLoad < 0.5 &&
 *                                node.attributes['os.name'] == 'Windows XP']]&gt;
 *                            &lt;/value&gt;
 *                        &lt;/property&gt;
 *                    &lt;/bean&gt;
 *                &lt;/property&gt;
 *           &lt;/bean&gt;
 *       &lt;/property&gt;
 *       ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
public class GridNodeFilterTopologySpi extends GridSpiAdapter implements GridTopologySpi,
    GridNodeFilterTopologySpiMBean {
    /** Injected grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** Configured node filter. */
    private GridNodeFilter filter = null;

    /**
     * {@inheritDoc}
     */
    public GridNodeFilter getFilter() {
        return filter;
    }

    /**
     * Sets filter for nodes to be included into task topology.
     *
     * @param filter Filter to use.
     * @see GridJexlNodeFilter
     */
    @GridSpiConfiguration(optional = true)
    public void setFilter(GridNodeFilter filter) {
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        if (filter == null) {
            log.warning("'Filter' configuration parameter is not set for GridNodeFilterTopologySpi " +
                "(all nodes will be accepted).");
        }

        registerMBean(gridName, this, GridNodeFilterTopologySpiMBean.class);

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("filter", filter));
        }

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getTopology(GridTaskSession ses, Collection<GridNode> grid) throws GridSpiException {
        List<GridNode> top = new ArrayList<GridNode>(grid.size());

        for (GridNode node : grid) {
            if (filter == null || filter.accept(node) == true) {
                top.add(node);

                if (log.isDebugEnabled() == true) {
                    log.debug("Included node into topology: " + node);
                }
            }
        }

        return top;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridNodeFilterTopologySpi.class, this);
    }
}
