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

package org.gridgain.grid.spi.loadbalancing.affinity;

import java.io.*;
import java.util.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridAffinityLoadBalancingSpi}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to affinity load balancing SPI configuration.")
public interface GridAffinityLoadBalancingSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets number of virtual nodes for Consistent Hashing
     * algorithm. For more information about algorithm, see
     * <a href="http://weblogs.java.net/blog/tomwhite/archive/2007/11/consistent_hash.html">Tom White's Blog</a>.
     *
     * @return Number of virtual nodes.
     */
    @GridMBeanDescription("Gets number of virtual nodes for Consistent Hashing algorithm.")
    public int getVirtualNodeCount();

    /**
     * Gets map of node attributes for nodes that should participate in affinity assignment.
     * Only nodes that have this attributes set will be included.
     * <p>
     * Default value is <tt>null</tt> which means all nodes will be added.
     *
     * @return Map of node attributes.
     */
    @GridMBeanDescription("Gets map of node attributes for nodes that should participate in affinity assignment.")
    public Map<String, ? extends Serializable> getAffinityNodeAttributes();

    /**
     * Gets affinity seed used by Consistent Hashing algorithm.
     * By default this seed is empty string.
     * <p>
     * Whenever starting multiple instances of this SPI, you should make
     * sure that every instance has a different seed to achieve different
     * affinity assignment. Otherwise, affinity assignment for different
     * instances of this SPI will be identical, which defeats the purpose
     * of starting multiple affinity load balancing SPI's altogether.
     * <p>
     * <b>Note that affinity seed must be identical for corresponding
     * instances of this SPI on all nodes.</b> If this is not the case,
     * then different nodes will calculate affinity differently which may
     * result in multiple nodes responsible for the same affinity key.
     *
     * @return Non-null value for affinity seed.
     */
    @GridMBeanDescription("Gets affinity seed used for Consistent Hashing algorithm.")
    public String getAffinitySeed();
}
