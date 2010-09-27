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

import java.lang.annotation.*;
import org.gridgain.apache.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.spi.*;

/**
 * This annotation allows task to specify what SPIs it wants to use.
 * Starting with <tt>GridGain 2.1</tt> you can start multiple instances
 * of {@link GridTopologySpi}, {@link GridLoadBalancingSpi},
 * {@link GridFailoverSpi}, and {@link GridCheckpointSpi}. If you do that,
 * you need to tell a task which SPI to use (by default it will use the fist
 * SPI in the list).
 * <p>
 * <h1 class="header">Example</h1>
 * This example shows how to configure different SPI's for different tasks. Let's
 * assume that you have two worker nodes, <tt>Node1</tt> and <tt>Node2</tt>.
 * Let's also assume that you configure <tt>Node1</tt> to belong to <tt>SegmentA</tt>
 * and <tt>Node2</tt> to belong to <tt>SegmentB</tt>. Here is a sample configuration
 * for <tt>Node1</tt>
 * <pre name="code" class="xml">
 * &lt;bean id="grid.cfg" class="org.gridgain.grid.GridConfigurationAdapter" scope="singleton"&gt;
 *     &lt;property name="userAttributes"&gt;
 *         &lt;map&gt;
 *             &lt;entry key="segment" value="A"/&gt;
 *         &lt;/map&gt;
 *     &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * <tt>Node2</tt> configuration looks similar to <tt>Node1</tt> with <tt>'segment'</tt> attribute
 * set to <tt>'B'</tt>.
 * <pre name="code" class="xml">
 * &lt;bean id="grid.cfg" class="org.gridgain.grid.GridConfigurationAdapter" scope="singleton"&gt;
 *     &lt;property name="userAttributes"&gt;
 *         &lt;map&gt;
 *             &lt;entry key="segment" value="B"/&gt;
 *         &lt;/map&gt;
 *     &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * Now, if you have <tt>Task1</tt> and <tt>Task2</tt> starting from some master node <tt>NodeM</tt>,
 * you can easily configure <tt>Task1</tt> to only run on <tt>SegmentA</tt> and <tt>Task2</tt> to
 * only run on <tt>SegmentB</tt>. Here is how configuration on master node <tt>NodeM</tt> would
 * look like:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.cfg" class="org.gridgain.grid.GridConfigurationAdapter" scope="singleton"&gt;
 *     &lt;!--
 *         Topology SPIs. We have two named SPIs: One picks up nodes
 *         that have attribute "segment" set to "A" and another one sees
 *         nodes that have attribute "segment" set to "B".
 *     --&gt;
 *     &lt;property name="topologySpi"&gt;
 *         &lt;list&gt;
 *             &lt;bean class="org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi"&gt;
 *                 &lt;property name="name" value="topologyA"/&gt;
 *                 &lt;property name="filter"&gt;
 *                     &lt;bean class="org.gridgain.grid.GridJexlNodeFilter"&gt;
 *                         &lt;property name="expression" value="node.attributes['segment'] == 'A'"/&gt;
 *                     &lt;/bean&gt;
 *                 &lt;/property&gt;
 *             &lt;/bean&gt;
 *             &lt;bean class="org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi"&gt;
 *                 &lt;property name="name" value="topologyB"/&gt;
 *                 &lt;property name="filter"&gt;
 *                     &lt;bean class="org.gridgain.grid.GridJexlNodeFilter"&gt;
 *                         &lt;property name="expression" value="node.attributes['segment'] == 'B'"/&gt;
 *                     &lt;/bean&gt;
 *                 &lt;/property&gt;
 *             &lt;/bean&gt;
 *         &lt;/list&gt;
 *     &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * Then your <tt>Task1</tt> and <tt>Task2</tt> would look as follows (note the <tt>@GridTaskSpis</tt>
 * annotation).
 * <pre name="code" class="java">
 * &#64;GridTaskSpis(topologySpi="topologyA")
 * public class GridSegmentATask extends GridTaskSplitAdapter&lt;String, Integer&gt; {
 * ...
 * }
 * </pre>
 * and
 * <pre name="code" class="java">
 * &#64;GridTaskSpis(topologySpi="topologyB")
 * public class GridSegmentBTask extends GridTaskSplitAdapter&lt;String, Integer&gt; {
 * ...
 * }
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Apache20LicenseCompatible
public @interface GridTaskSpis {
    /**
     * Optional load balancing SPI name. By default, SPI name is equal
     * to the name of the SPI class. You can change SPI name by explicitely
     * supplying {@link GridSpi#getName()} parameter in grid configuration.
     */
    public String loadBalancingSpi() default "";

    /**
     * Optional failover SPI name. By default, SPI name is equal
     * to the name of the SPI class. You can change SPI name by explicitely
     * supplying {@link GridSpi#getName()} parameter in grid configuration.
     */
    public String failoverSpi() default "";

    /**
     * Optional topology SPI name. By default, SPI name is equal
     * to the name of the SPI class. You can change SPI name by explicitely
     * supplying {@link GridSpi#getName()} parameter in grid configuration.
     */
    public String topologySpi() default "";

    /**
     * Optional checkpoint SPI name. By default, SPI name is equal
     * to the name of the SPI class. You can change SPI name by explicitely
     * supplying {@link GridSpi#getName()} parameter in grid configuration.
     */
    public String checkpointSpi() default "";
}
