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

package org.gridgain.grid.spi.loadbalancing.adaptive;

import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Implementation of node load probing based on active and waiting job count.
 * Based on {@link #setUseAverage(boolean)} parameter, this implementation will
 * either use average job count values or current (default is to use averages).
 * <p>
 * The load of a node is simply calculated by adding active and waiting job counts.
 * <p>
 * Below is an example of how CPU load probe would be configured in GridGain
 * Spring configuration file:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveLoadBalancingSpi"&gt;
 *         &lt;property name="loadProbe"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveJobCountLoadProbe"&gt;
 *                 &lt;property name="useAverage" value="true"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 */
public class GridAdaptiveJobCountLoadProbe implements GridAdaptiveLoadProbe {
    /** Flag indicating whether to use average CPU load vs. current. */
    private boolean useAvg = true;

    /**
     * Initializes active job probe.
     */
    public GridAdaptiveJobCountLoadProbe() {
        // No-op.
    }

    /**
     * Creates new active job prove specifying whether to use average
     * job counts vs. current.
     *
     * @param useAvg Flag indicating whether to use average job counts vs. current.
     */
    public GridAdaptiveJobCountLoadProbe(boolean useAvg) {
        this.useAvg = useAvg;
    }

    /**
     * Gets flag indicating whether to use average job counts vs. current.
     *
     * @return Flag indicating whether to use average job counts vs. current.
     */
    public boolean isUseAverage() {
        return useAvg;
    }

    /**
     * Sets flag indicating whether to use average job counts vs. current.
     *
     * @param useAvg Flag indicating whether to use average job counts vs. current.
     */
    public void setUseAverage(boolean useAvg) {
        this.useAvg = useAvg;
    }


    /**
     * {@inheritDoc}
     */
    public double getLoad(GridNode node, int jobsSentSinceLastUpdate) {
        GridNodeMetrics metrics = node.getMetrics();

        if (useAvg == true) {
            double load = metrics.getAverageActiveJobs() + metrics.getAverageWaitingJobs();

            if (load > 0) {
                return load;
            }
        }

        double load = metrics.getCurrentActiveJobs() + metrics.getCurrentWaitingJobs();

        return load < 0 ? 0 : load;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridAdaptiveJobCountLoadProbe.class, this);
    }
}
