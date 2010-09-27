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

import org.gridgain.apache.*;

/**
 * Instances of classes that implement this interface are used to filter grid nodes.
 * These instances are used to filter nodes in method {@link Grid#getNodes(GridNodeFilter)}.
 * They are also used by {@link org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi}
 * to provide task topology based on user-defined node filters.
 * <p>
 * GridGain also comes with {@link GridJexlNodeFilter} implementation which allows you
 * to conveniently filter nodes based on Apache JEXL expression language. For more
 * information refer to <a href="http://commons.apache.org/jexl/">Apache JEXL</a>
 * documentation.
 * <p>
 * Together with {@link org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi}
 * {@link GridJexlNodeFilter} allows for a fairly simple way to provide complex SLA-based
 * task topology specifications. For example, expression below shows how the SPI can be
 * configured with {@link GridJexlNodeFilter} to include all Windows XP nodes
 * with more than one processor or core and that are not loaded over 50%
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *       ...
 *       &lt;property name="topologySpi"&gt;
 *           &lt;bean class="org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi"&gt;
 *               &lt;property name="filter"&gt;
 *                    &lt;bean class="org.gridgain.grid.GridJexlNodeFilter"&gt;
 *                        &lt;property name="expression"
 *                            value="node.metrics.availableProcessors > 1 && node.metrics.averageCpuLoad < 0.5 && node.attributes['os.name'] == 'Windows XP'"/&gt;
 *                    &lt;/bean&gt;
 *                &lt;/property&gt;
 *           &lt;/bean&gt;
 *       &lt;/property&gt;
 *       ...
 * &lt;/bean&gt;
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridNodeFilter {
    /**
     * Tests if a specified grid node should be accepted.
     *
     * @param node Node to check.
     * @return <tt>True</tt> if node is accepted, <tt>false</tt> otherwise.
     */
    public boolean accept(GridNode node);
}
