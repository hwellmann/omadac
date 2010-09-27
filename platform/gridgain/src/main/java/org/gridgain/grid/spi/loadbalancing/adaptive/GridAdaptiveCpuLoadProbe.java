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
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Implementation of node load probing based on CPU load.
 * <p>
 * Based on {@link #setUseAverage(boolean)}
 * parameter, this implementation will either use average CPU load
 * values or current (default is to use averages).
 * <p>
 * Based on {@link #setUseProcessors(boolean)} parameter, this implementation
 * will either take number of processors on the node into account or not.
 * Since CPU load on multi-processor boxes shows medium load of multiple CPU's it
 * usually means that the remaining capacity is proportional to the number of
 * CPU's (or cores) on the node. This configuration parameter indicates
 * whether to divide each node's CPU load by the number of processors on that node
 * (default is <tt>true</tt>).
 * <p>
 * Also note that in some environments every processor may not be adding 100% of
 * processing power. For example, if you are using multi-core CPU's, then addition of
 * every core would probably result in about 75% of extra CPU power. To account
 * for that, you should set {@link #setProcessorCoefficient(double)} parameter to
 * <tt>0.75</tt> .
 * <p>
 * Below is an example of how CPU load probe would be configured in GridGain
 * Spring configuration file:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveLoadBalancingSpi"&gt;
 *         &lt;property name="loadProbe"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveCpuLoadProbe"&gt;
 *                 &lt;property name="useAverage" value="true"/&gt;
 *                 &lt;property name="useProcessors" value="true"/&gt;
 *                 &lt;property name="processorCoefficient" value="0.9"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * This implementation is used by default by {@link GridAdaptiveLoadBalancingSpi} SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridAdaptiveCpuLoadProbe implements GridAdaptiveLoadProbe {
    /** Flag indicating whether to use average CPU load vs. current. */
    private boolean useAvg = true;

    /**
     * Flag indicating whether to divide each node's CPU load
     * by the number of processors on that node.
     */
    private boolean useProcs = true;

    /**
     * Coefficient of every CPU processor. By default it is <tt>1</tt>, but
     * in some environments every processor may not be adding 100% of processing
     * power. For example, if you are using multi-core CPU's, then addition of
     * every core would probably result in about 75% of extra CPU power, and hence
     * you would set this coefficient to <tt>0.75</tt> .
     */
    private double procCoefficient = 1;

    /**
     * Initializes CPU load probe to use CPU load average by default.
     */
    public GridAdaptiveCpuLoadProbe() {
        // No-op.
    }

    /**
     * Specifies whether to use average CPU load vs. current and whether or
     * not to take number of processors into account.
     * <p>
     * Since CPU load on multi-processor boxes shows medium load of multiple CPU's it
     * usually means that the remaining capacity is proportional to the number of
     * CPU's (or cores) on the node.
     *
     * @param useAvg Flag indicating whether to use average CPU load vs. current
     *      (default is <tt>true</tt>).
     * @param useProcs Flag indicating whether to divide each node's CPU load
     *      by the number of processors on that node (default is <tt>true</tt>).
     */
    public GridAdaptiveCpuLoadProbe(boolean useAvg, boolean useProcs) {
        this.useAvg = useAvg;
        this.useProcs = useProcs;
    }

    /**
     * Specifies whether to use average CPU load vs. current and whether or
     * not to take number of processors into account. It also allows to
     * specify the coefficient of addition power every CPU adds.
     * <p>
     * Since CPU load on multi-processor boxes shows medium load of multiple CPU's it
     * usually means that the remaining capacity is proportional to the number of
     * CPU's (or cores) on the node.
     * <p>
     * Also, in some environments every processor may not be adding 100% of processing
     * power. For example, if you are using multi-core CPU's, then addition of
     * every core would probably result in about 75% of extra CPU power, and hence
     * you would set this coefficient to <tt>0.75</tt> .
     *
     * @param useAvg Flag indicating whether to use average CPU load vs. current
     *      (default is <tt>true</tt>).
     * @param useProcs Flag indicating whether to divide each node's CPU load
     *      by the number of processors on that node (default is <tt>true</tt>).
     * @param procCoefficient Coefficient of every CPU processor (default value is <tt>1</tt>).
     */
    public GridAdaptiveCpuLoadProbe(boolean useAvg, boolean useProcs, double procCoefficient) {
        this.useAvg = useAvg;
        this.useProcs = useProcs;
        this.procCoefficient = procCoefficient;
    }

    /**
     * Gets flag indicating whether to use average CPU load vs. current.
     *
     * @return Flag indicating whether to use average CPU load vs. current.
     */
    public boolean isUseAverage() {
        return useAvg;
    }

    /**
     * Sets flag indicating whether to use average CPU load vs. current.
     * If not explicitly set, then default value is <tt>true</tt>.
     *
     * @param useAvg Flag indicating whether to use average CPU load vs. current.
     */
    public void setUseAverage(boolean useAvg) {
        this.useAvg = useAvg;
    }

    /**
     * Gets flag indicating whether to use average CPU load vs. current
     * (default is <tt>true</tt>).
     * <p>
     * Since CPU load on multi-processor boxes shows medium load of multiple CPU's it
     * usually means that the remaining capacity is proportional to the number of
     * CPU's (or cores) on the node.
     *
     * @return Flag indicating whether to divide each node's CPU load
     *      by the number of processors on that node (default is <tt>true</tt>).
     */
    public boolean isUseProcessors() {
        return useProcs;
    }

    /**
     * Sets flag indicating whether to use average CPU load vs. current
     * (default is <tt>true</tt>).
     * <p>
     * Since CPU load on multi-processor boxes shows medium load of multiple CPU's it
     * usually means that the remaining capacity is proportional to the number of
     * CPU's (or cores) on the node.
     * <p>
     * If not explicitly set, then default value is <tt>true</tt>.
     *
     * @param useProcs Flag indicating whether to divide each node's CPU load
     *      by the number of processors on that node (default is <tt>true</tt>).
     */
    public void setUseProcessors(boolean useProcs) {
        this.useProcs = useProcs;
    }

    /**
     * Gets coefficient of every CPU processor. By default it is <tt>1</tt>, but
     * in some environments every processor may not be adding 100% of processing
     * power. For example, if you are using multi-core CPU's, then addition of
     * every core would probably result in about 75% of extra CPU power, and hence
     * you would set this coefficient to <tt>0.75</tt> .
     * <p>
     * This value is ignored if {@link #isUseProcessors()} is set to <tt>false</tt>.
     *
     * @return Coefficient of every CPU processor.
     */
    public double getProcessorCoefficient() {
        return procCoefficient;
    }

    /**
     * Sets coefficient of every CPU processor. By default it is <tt>1</tt>, but
     * in some environments every processor may not be adding 100% of processing
     * power. For example, if you are using multi-core CPU's, then addition of
     * every core would probably result in about 75% of extra CPU power, and hence
     * you would set this coefficient to <tt>0.75</tt> .
     * <p>
     * This value is ignored if {@link #isUseProcessors()} is set to <tt>false</tt>.
     *
     * @param procCoefficient Coefficient of every CPU processor.
     */
    public void setProcessorCoefficient(double procCoefficient) {
        GridArgumentCheck.checkRange(procCoefficient > 0, "procCoefficient > 0");

        this.procCoefficient = procCoefficient;
    }

    /**
     * {@inheritDoc}
     */
    public double getLoad(GridNode node, int jobsSentSinceLastUpdate) {
        GridNodeMetrics metrics = node.getMetrics();

        double k = 1.0d;

        if (useProcs == true) {
            int procs = metrics.getAvailableProcessors();

            if (procs > 1) {
                k = procs * procCoefficient;
            }
        }

        double load = (useAvg == true ? metrics.getAverageCpuLoad() : metrics.getCurrentCpuLoad()) / k;

        return load < 0 ? 0 : load;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridAdaptiveCpuLoadProbe.class, this);
    }
}
