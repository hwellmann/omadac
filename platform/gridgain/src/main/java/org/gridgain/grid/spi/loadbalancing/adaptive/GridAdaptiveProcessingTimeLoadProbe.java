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
 * Implementation of node load probing based on total job processing time.
 * Based on {@link #setUseAverage(boolean)}
 * parameter, this implementation will either use average job execution
 * time values or current (default is to use averages). The algorithm
 * returns a sum of job wait time and job execution time.
 * <p>
 * Below is an example of how CPU load probe would be configured in GridGain
 * Spring configuration file:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveLoadBalancingSpi"&gt;
 *         &lt;property name="loadProbe"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveProcessingTimeLoadProbe"&gt;
 *                 &lt;property name="useAverage" value="true"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridAdaptiveProcessingTimeLoadProbe implements GridAdaptiveLoadProbe {
    /** Flag indicating whether to use average execution time vs. current. */
    private boolean useAvg = true;

    /**
     * Initializes execution time load probe to use
     * execution time average by default.
     */
    public GridAdaptiveProcessingTimeLoadProbe() {
        // No-op.
    }

    /**
     * Specifies whether to use average execution time vs. current.
     *
     * @param useAvg Flag indicating whether to use average execution time vs. current.
     */
    public GridAdaptiveProcessingTimeLoadProbe(boolean useAvg) {
        this.useAvg = useAvg;
    }

    /**
     * Gets flag indicating whether to use average execution time vs. current.
     *
     * @return Flag indicating whether to use average execution time vs. current.
     */
    public boolean isUseAverage() {
        return useAvg;
    }

    /**
     * Sets flag indicating whether to use average execution time vs. current.
     *
     * @param useAvg Flag indicating whether to use average execution time vs. current.
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
            double load = metrics.getAverageJobExecuteTime() + metrics.getAverageJobWaitTime();

            // If load is greater than 0, then we can use average times.
            // Otherwise, we will proceed to using current times.
            if (load > 0) {
                return load;
            }
        }

        double load = metrics.getCurrentJobExecuteTime() + metrics.getCurrentJobWaitTime();

        return load < 0 ? 0 : load;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridAdaptiveProcessingTimeLoadProbe.class, this);
    }
}
