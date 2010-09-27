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

package org.gridgain.grid;

import java.io.*;
import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.spi.*;

/**
 * Interface representing a single grid node. Use {@link #getAttribute(String)} or
 * {@link #getMetrics()} to get static and dynamic information about remote nodes.
 * <tt>GridNode</tt> list, which includes all nodes within task topology, is provided
 * to {@link GridTask#map(List, Object)} method. You can also get a handle on
 * discovered nodes by calling any of the following methods:
 * <ul>
 * <li>{@link Grid#getLocalNode()}</li>
 * <li>{@link Grid#getRemoteNodes()}</li>
 * <li>{@link Grid#getAllNodes()}</li>
 * </ul>
 * <p>
 * <h1 class="header">Grid Node Attributes</h1>
 * You can use grid node attributes to provide static information about a node.
 * This information is initialized once within grid, during node startup, and
 * remains the same throughout the lifetime of a node. Use
 * {@link GridConfiguration#getUserAttributes()} method to initialize your custom
 * node attributes at startup. For example, to provide benchmark data about
 * every node from Spring XML configuration file, you would do the following:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.cfg" class="org.gridgain.grid.GridConfigurationAdapter" scope="singleton">
 *     ...
 *     &lt;property name="userAttributes">
 *         &lt;map>
 *             &lt;entry key="grid.node.benchmark">
 *                 &lt;bean class="org.gridgain.grid.benchmarks.GridLocalNodeBenchmark" init-method="start"/>
 *             &lt;/entry>
 *         &lt;/map>
 *     &lt;/property>
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * The system adds the following attributes automatically:
 * <ul>
 * <li><tt>{@link System#getProperties()}</tt> - All system properties.</li>
 * <li><tt>{@link System#getenv(String)}</tt> - All environment properties.</li>
 * <li><tt>org.gridgain.build.ver</tt> - GridGain build version.</li>
 * <li><tt>org.gridgain.jit.name</tt> - Name of JIT compiler used.</li>
 * <li><tt>org.gridgain.net.itf.name</tt> - Name of network interface.</li>
 * <li><tt>org.gridgain.user.name</tt> - Operating system user name.</li>
 * <li><tt>org.gridgain.grid.name</tt> - Grid name (see {@link Grid#getName()}).</li>
 * <li>
 *      <tt>spiName.org.gridgain.spi.class</tt> - SPI implementation class for every SPI,
 *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
 * </li>
 * <li>
 *      <tt>spiName.org.gridgain.spi.ver</tt> - SPI version for every SPI,
 *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
 * </li>
 * </ul>
 * <p>
 * Note that all System and Environment properties for all nodes are automatically included
 * into node attributes. This gives you an ability to get any information specified
 * in {@link System#getProperties()} about any node. So for example, in order to print out
 * information about Operating System for all nodes you would do the following:
 * <pre name="code" class="java">
 * for (GridNode node : GridFactory.getGrid().getAllNodes()) {
 *     System.out.println("Operating system name: " + node.getAttribute("os.name"));
 *     System.out.println("Operating system architecture: " + node.getAttribute("os.arch"));
 *     System.out.println("Operating system version: " + node.getAttribute("os.version"));
 * }
 * </pre>
 * <p>
 * <h1 class="header">Grid Node Metrics</h1>
 * Grid node metrics (see {@link #getMetrics()}) are updated frequently for all nodes
 * and can be used to get dynamic information about a node. The frequency of update
 * is often directly related to the heartbeat exchange between nodes. So if, for example,
 * default {@link org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoverySpi} is used,
 * the metrics data will be updated every <tt>3</tt> seconds by default.
 * <p>
 * Grid node metrics provide information about other nodes that can frequently change,
 * such as Heap and Non-Heap memory utilization, CPU load, number of active and waiting
 * grid jobs, etc... This information can become useful during job collision resolution or
 * {@link GridTask#map(List, Object)} operation when jobs are assigned to remote nodes
 * for execution. For example, you can only pick nodes that don't have any jobs waiting
 * to be executed.
 * <p>
 * Local node metrics are registered as <tt>MBean</tt> and can be accessed from
 * any JMX management console. The simplest way is to use standard <tt>jconsole</tt>
 * that comes with JDK as it also provides ability to view any node parameter
 * as a graph.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridNode extends Serializable {
    /**
     * Gets globally unique node ID.
     *
     * @return Globally unique node ID.
     */
    public UUID getId();

    /**
     * Gets physical address of the node. In most cases, although it is not
     * strictly guaranteed, it is an IP address of a node.
     *
     * @return Physical address of the node.
     */
    public String getPhysicalAddress();

    /**
     * Gets a node attribute. Attributes are assigned to nodes at startup
     * via {@link GridConfiguration#getUserAttributes()} method.
     * <p>
     * The system adds the following attributes automatically:
     * <ul>
     * <li><tt>{@link System#getProperties()}</tt> - All system properties.</li>
     * <li><tt>{@link System#getenv(String)}</tt> - All environment properties.</li>
     * <li><tt>org.gridgain.build.ver</tt> - GridGain build version.</li>
     * <li><tt>org.gridgain.jit.name</tt> - Name of JIT compiler used.</li>
     * <li><tt>org.gridgain.net.itf.name</tt> - Name of network interface.</li>
     * <li><tt>org.gridgain.user.name</tt> - Operating system user name.</li>
     * <li><tt>org.gridgain.grid.name</tt> - Grid name (see {@link Grid#getName()}).</li>
     * <li>
     *      <tt>spiName.org.gridgain.spi.class</tt> - SPI implementation class for every SPI,
     *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
     * </li>
     * <li>
     *      <tt>spiName.org.gridgain.spi.ver</tt> - SPI version for every SPI,
     *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
     * </li>
     * </ul>
     * <p>
     * Note that attributes cannot be changed at runtime.
     * @param <T> Attribute Type.
     *
     * @param name Attribute name. <b>Note</b> that attribute names starting with
     *      <tt>org.gridgain</tt> are reserved for internal use.
     * @return Attribute value.
     */
    public <T extends Serializable> T getAttribute(String name);

    /**
     * Gets metrics for this node. Note that node metrics are constantly updated
     * and provide up to date information about nodes. For example, you can get
     * an idea about CPU load on remote node via {@link GridNodeMetrics#getCurrentCpuLoad()}
     * method and use it during {@link GridTask#map(List, Object)} or during collision
     * resolution.
     * <p>
     * Node metrics are updated with some delay which is directly related to heartbeat
     * frequency. For example, when used with default
     * {@link org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoverySpi}
     * the update will happen every <tt>2</tt> seconds.
     *
     * @return Runtime metrics for this node.
     */
    public GridNodeMetrics getMetrics();

    /**
     * Gets all node attributes. Attributes are assigned to nodes at startup
     * via {@link GridConfiguration#getUserAttributes()} method.
     * <p>
     * The system adds the following attributes automatically:
     * <ul>
     * <li><tt>{@link System#getProperties()}</tt> - All system properties.</li>
     * <li><tt>{@link System#getenv(String)}</tt> - All environment properties.</li>
     * <li><tt>org.gridgain.build.ver</tt> - GridGain build version.</li>
     * <li><tt>org.gridgain.jit.name</tt> - Name of JIT compiler used.</li>
     * <li><tt>org.gridgain.net.itf.name</tt> - Name of network interface.</li>
     * <li><tt>org.gridgain.user.name</tt> - Operating system user name.</li>
     * <li><tt>org.gridgain.grid.name</tt> - Grid name (see {@link Grid#getName()}).</li>
     * <li>
     *      <tt>spiName.org.gridgain.spi.class</tt> - SPI implementation class for every SPI,
     *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
     * </li>
     * <li>
     *      <tt>spiName.org.gridgain.spi.ver</tt> - SPI version for every SPI,
     *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
     * </li>
     * </ul>
     * <p>
     * Note that attributes cannot be changed at runtime.
     * @param <T> Attribute Type.
     *
     * @return All node attributes.
     */
    public <T extends Serializable> Map<String, T> getAttributes();
}
