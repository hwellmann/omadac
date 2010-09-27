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
import org.gridgain.grid.benchmarks.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Probe that uses {@link GridLocalNodeBenchmark} for specifying load for every node.
 * In order to use this probe, use should configure every grid node to start with
 * {@link GridLocalNodeBenchmark} as attribute (see {@link GridConfiguration#getUserAttributes()}
 * for more information).
 * <p>
 * You can initialize local node benchmarks by adding/uncommenting the following section
 * in GridGain Spring XML file:
 * <pre name="code" class="xml">
 * &lt;property name="userAttributes"&gt;
 *     &lt;map&gt;
 *         &lt;entry key="grid.node.benchmark"&gt;
 *             &lt;bean class="org.gridgain.grid.benchmarks.GridLocalNodeBenchmark" init-method="start"/&gt;
 *         &lt;/entry&gt;
 *     &lt;/map&gt;
 * &lt;/property&gt;
 * </pre>
 * Here is an example of how load balancing SPI would be configured from Spring XML configuration:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveLoadBalancingSpi"&gt;
 *         &lt;property name="loadProbe"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveBenchmarkLoadProbe"&gt;
 *                 &lt;!-- Specify name of benchmark node attribute (the same as above). --&gt;
 *                 &lt;property name="benchmarkAttributeName" value="grid.node.benchmark"/&gt;
 *
 *                 &lt;!-- Benchmarks scores to use. --&gt;
 *                 &lt;property name="useIntegerScore" value="true"/&gt;
 *                 &lt;property name="useLongScore" value="true"/&gt;
 *                 &lt;property name="useDoulbeScore" value="true"/&gt;
 *                 &lt;property name="useIoScore" value="false"/&gt;
 *                 &lt;property name="useTrigonometryScore" value="false"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * Please make sure to properly initialize this probe to use exactly the scores you
 * need in your grid. For example, if your jobs don't do any I/O, then you probably
 * should disable I/O score. If you are not doing any trigonometry calculations,
 * then you should disable trigonometry score.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridAdaptiveBenchmarkLoadProbe implements GridAdaptiveLoadProbe {
    /**
     * Default node attribute name for storing node benchmarks (value is
     * <tt>'grid.node.benchmark'</tt>). See {@link GridNode#getAttribute(String)}
     * for more information.
     */
    public static final String DFLT_BENCHMARK_ATTR = "grid.node.benchmark";

    /** Flag indicating whether to use <tt>integer</tt> score. */
    private boolean useIntScore = true;

    /** Flag indicating whether to use <tt>long</tt> score. */
    private boolean useLongScore = true;

    /** Flag indicating whether to use <tt>double</tt> score. */
    private boolean useDoubleScore = true;

    /** Flag indicating whether to use <tt>trigonometry</tt> score. */
    private boolean useTrigScore = true;

    /** Flag indicating whether to use <tt>I/O</tt> score. */
    private boolean useIoScore = true;

    /** Name of node benchmark attribute. */
    private String benchmarkAttr = DFLT_BENCHMARK_ATTR;

    /**
     * Creates benchmark probe with all defaults. By default, all scores
     * provided in {@link GridLocalNodeBenchmark} class will be used.
     */
    public GridAdaptiveBenchmarkLoadProbe() {
        // No-op.
    }

    /**
     * Creates benchmark probe which allows use to specify which scores to use.
     * See {@link GridLocalNodeBenchmark} for more information on which scores
     * are available and how they are calculated.
     *
     * @param useIntScore Flag indicating whether to use <tt>integer</tt> score.
     * @param useLongScore Flag indicating whether to use <tt>long</tt> score.
     * @param useDoubleScore Flag indicating whether to use <tt>double</tt> score.
     * @param useTrigScore Flag indicating whether to use <tt>trigonometry</tt> score.
     * @param useIoScore Flag indicating whether to use <tt>I/O</tt> score.
     */
    public GridAdaptiveBenchmarkLoadProbe(boolean useIntScore, boolean useLongScore, boolean useDoubleScore,
        boolean useTrigScore, boolean useIoScore) {
        this.useIntScore = useIntScore;
        this.useLongScore = useLongScore;
        this.useDoubleScore = useDoubleScore;
        this.useTrigScore = useTrigScore;
        this.useIoScore = useIoScore;
    }

    /**
     * Creates benchmark probe which allows use to specify which scores to use.
     * See {@link GridLocalNodeBenchmark} for more information on which scores
     * are available and how they are calculated.
     * <p>
     * This constructor also allows to specify the name of node attribute by
     * which node benchmarks should be accessed.
     *
     * @param useIntScore Flag indicating whether to use <tt>integer</tt> score.
     * @param useLongScore Flag indicating whether to use <tt>long</tt> score.
     * @param useDoubleScore Flag indicating whether to use <tt>double</tt> score.
     * @param useTrigScore Flag indicating whether to use <tt>trigonometry</tt> score.
     * @param useIoScore Flag indicating whether to use <tt>I/O</tt> score.
     * @param benchmarkAttr Name of node attribute by which node benchmarks should be accessed.
     */
    public GridAdaptiveBenchmarkLoadProbe(boolean useIntScore, boolean useLongScore, boolean useDoubleScore,
        boolean useTrigScore, boolean useIoScore, String benchmarkAttr) {
        this.useIntScore = useIntScore;
        this.useLongScore = useLongScore;
        this.useDoubleScore = useDoubleScore;
        this.useTrigScore = useTrigScore;
        this.useIoScore = useIoScore;
        this.benchmarkAttr = benchmarkAttr;
    }

    /**
     * Gets name of node attribute by which node benchmarks should be accessed.
     * By default {@link #DFLT_BENCHMARK_ATTR} name is used.
     *
     * @return Name of node attribute by which node benchmarks should be accessed.
     */
    public String getBenchmarkAttributeName() {
        return benchmarkAttr;
    }

    /**
     * Sets name of node attribute by which node benchmarks should be accessed.
     * By default {@link #DFLT_BENCHMARK_ATTR} name is used.
     *
     * @param benchmarkAttr Name of node attribute by which node benchmarks should be accessed.
     */
    public void setBenchmarkAttributeName(String benchmarkAttr) {
        GridArgumentCheck.checkNull(benchmarkAttr, "benchmarkAttr");

        this.benchmarkAttr = benchmarkAttr;
    }

    /**
     * Gets flag indicating whether <tt>integer</tt> score should be used
     * for calculation of node load.
     *
     * @return Flag indicating whether <tt>integer</tt> score should be used
     *      for calculation of node load.
     */
    public boolean isUseIntegerScore() {
        return useIntScore;
    }

    /**
     * Sets flag indicating whether <tt>integer</tt> score should be used
     * for calculation of node load.
     *
     * @param useIntScore Flag indicating whether <tt>integer</tt> score should be used
     *      for calculation of node load.
     */
    public void setUseIntegerScore(boolean useIntScore) {
        this.useIntScore = useIntScore;
    }

    /**
     * Gets flag indicating whether <tt>long</tt> score should be used
     * for calculation of node load.
     *
     * @return Flag indicating whether <tt>long</tt> score should be used
     *      for calculation of node load.
     */
    public boolean isUseLongScore() {
        return useLongScore;
    }

    /**
     * Sets flag indicating whether <tt>long</tt> score should be used
     * for calculation of node load.
     *
     * @param useLongScore Flag indicating whether <tt>long</tt> score should be used
     *      for calculation of node load.
     */
    public void setUseLongScore(boolean useLongScore) {
        this.useLongScore = useLongScore;
    }

    /**
     * Gets flag indicating whether <tt>double</tt> score should be used
     * for calculation of node load.
     *
     * @return Flag indicating whether <tt>double</tt> score should be used
     *      for calculation of node load.
     */
    public boolean isUseDoulbeScore() {
        return useDoubleScore;
    }

    /**
     * Sets flag indicating whether <tt>double</tt> score should be used
     * for calculation of node load.
     *
     * @param useDoubleScore Flag indicating whether <tt>double</tt> score should be used
     *      for calculation of node load.
     */
    public void setUseDoulbeScore(boolean useDoubleScore) {
        this.useDoubleScore = useDoubleScore;
    }

    /**
     * Gets flag indicating whether <tt>trigonometry</tt> score should be used
     * for calculation of node load.
     *
     * @return Flag indicating whether <tt>trigonometry</tt> score should be used
     * for calculation of node load.
     */
    public boolean isUseTrigonometryScore() {
        return useTrigScore;
    }

    /**
     * Sets flag indicating whether <tt>trigonometry</tt> score should be used
     * for calculation of node load.
     *
     * @param useTrigScore Flag indicating whether <tt>trigonometry</tt> score should be used
     *      for calculation of node load.
     */
    public void setUseTrigonometryScore(boolean useTrigScore) {
        this.useTrigScore = useTrigScore;
    }

    /**
     * Gets flag indicating whether <tt>I/O</tt> score should be used
     * for calculation of node load.
     *
     * @return Flag indicating whether <tt>I/O</tt> score should be used
     *      for calculation of node load.
     */
    public boolean isUseIoScore() {
        return useIoScore;
    }

    /**
     * Sets flag indicating whether <tt>I/O</tt> score should be used
     * for calculation of node load.
     *
     * @param useIoScore Flag indicating whether <tt>I/O</tt> score should be used
     *      for calculation of node load.
     */
    public void setUseIoScore(boolean useIoScore) {
        this.useIoScore = useIoScore;
    }

    /**
     * {@inheritDoc}
     */
    public double getLoad(GridNode node, int jobsSentSinceLastUpdate) {
        GridLocalNodeBenchmark benchmark = (GridLocalNodeBenchmark)node.getAttribute(benchmarkAttr);

        if (benchmark == null) {
            return 0.0d;
        }

        double score = 0;

        score += useIntScore == true ? benchmark.getIntegerScore() : 0;
        score += useLongScore == true ? benchmark.getLongScore() : 0;
        score += useDoubleScore == true ? benchmark.getDoubleScore() : 0;
        score += useIoScore == true ? benchmark.getIoScore() : 0;
        score += useTrigScore == true ? benchmark.getTrigonometryScore() : 0;

        // Return 1 over score to get load vs. weight.
        return score == 0 ? 0.0d : 1.0d / score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridAdaptiveBenchmarkLoadProbe.class, this);
    }
}
