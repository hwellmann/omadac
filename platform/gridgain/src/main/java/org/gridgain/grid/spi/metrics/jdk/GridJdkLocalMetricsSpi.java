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

package org.gridgain.grid.spi.metrics.jdk;

import java.lang.management.*;
import java.lang.reflect.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.metrics.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.jsr305.*;

/**
 * This class provides JDK MXBean based local VM metrics. Note that average
 * CPU load cannot be obtained from JDK 1.5 and on some operating systems
 * (including Windows Vista) even from JDK 1.6 (although JDK 1.6 supposedly added
 * support for it). For cases when CPU load cannot
 * be obtained from JDK, GridGain ships with
 * <a href="http://www.hyperic.com/products/sigar.html">Hyperic SIGAR</a> metrics.
 * However, Hyperic SIGAR is licensed under GPL (unlike LGPL license for GridGain); so
 * if GPL license cannot be used for your business, you should remove Hyperic libraries
 * from <tt>[GRIDGAIN_HOME]/libs</tt> folder.
 * <p>
 * If CPU load cannot be obtained either from JDK or Hyperic, then
 * {@link GridLocalMetrics#getCurrentCpuLoad()}
 * method will always return <tt>-1</tt>.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>Always prefer Hyperic Sigar regardless of JDK version (see {@link #setPreferSigar(boolean)})</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridJdkLocalMetricsSpi extends GridSpiAdapter implements GridLocalMetricsSpi,
    GridJdkLocalMetricsSpiMBean {
    /** */
    private MemoryMXBean mem  = null;

    /** */
    private OperatingSystemMXBean os = null;

    /** */
    private RuntimeMXBean runtime = null;

    /** */
    private ThreadMXBean threads = null;

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    private volatile GridLocalMetrics metrics = null;

    /** */
    private Object sigar = null;

    /** */
    private Method sigarCpuPercMtd = null;

    /** */
    private Method sigarCpuCombinedMtd = null;

    /** */
    private Method jdk6CpuLoadMtd = null;

    /** */
    private volatile boolean isPreferSigar = true;

    /**
     * {@inheritDoc}
     */
    public boolean isPreferSigar() {
        return isPreferSigar;
    }

    /**
     * Configuration parameter indicating if Hyperic Sigar should be used regardless
     * of JDK version. Hyperic Sigar is used to provide CPU load. Starting with JDK 1.6,
     * method <tt>OperatingSystemMXBean.getSystemLoadAverage()</tt> method was added.
     * However, even in 1.6 and higher this method does not always provide CPU load
     * on some operating systems (or provides incorrect value) - in such cases Hyperic
     * Sigar will be used automatically.
     * <p>
     * By default the value is <tt>true</tt>.
     *
     * @param isPreferSigar If <tt>true</tt> then Hyperic Sigar should be used regardless of
     *      JDK version, if <tt>false</tt>, then implementation will attempt to use
     *      <tt>OperatingSystemMXBean.getSystemLoadAverage()</tt> for JDK 1.6 and higher.
     */
    @GridSpiConfiguration(optional = true)
    public void setPreferSigar(boolean isPreferSigar) {
        this.isPreferSigar = isPreferSigar;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException {
        startStopwatch();

        if (log.isInfoEnabled() == true) {
            log.info(configInfo("isPreferSigar", isPreferSigar));
        }

        mem = ManagementFactory.getMemoryMXBean();
        os = ManagementFactory.getOperatingSystemMXBean();
        runtime = ManagementFactory.getRuntimeMXBean();
        threads = ManagementFactory.getThreadMXBean();

        String jdkVer = GridOs.getJdkVersion();

        if (isPreferSigar == false && jdkVer.contains("1.5") == false) {
            // We support JDK 1.5 or higher so if it's not 1.5.x then 1.6+
            initializeJdk16();
        }
        else {
            initializeSigar();
        }

        if (jdk6CpuLoadMtd != null) {
            if (log.isInfoEnabled() == true) {
                log.info("JDK 1.6 method 'OperatingSystemMXBean.getSystemLoadAverage()' " +
                    "will be used to detect average CPU load.");
            }
        }
        else if (sigar != null) {
            // Force sigar if found. Note that Sigar is GPL-licensed.
            isPreferSigar = true;

            if (log.isInfoEnabled() == true) {
                log.info("Hyperic Sigar 'CpuPerc.getCombined()' method will be used to detect average CPU load. " +
                    "Note that Hyperic Sigar is licensed under GPL. For more " +
                    "information visit: http://support.hyperic.com/confluence/display/SIGAR/Home");
            }
        }
        else {
            log.warning("System CPU load cannot be detected (either upgrade to JDK 1.6 or higher or add " +
                "Hyperic Sigar to classpath). CPU load will be returned as -1.");
        }

        metrics = getMetrics();

        registerMBean(gridName, this, GridJdkLocalMetricsSpiMBean.class);

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
     *
     */
    @SuppressWarnings({"CatchGenericClass"})
    private void initializeSigar() {
        // Detect if Sigar is available in classpath.
        try {
            Object sigar = Class.forName("org.hyperic.sigar.Sigar").newInstance();

            Method proxyMtd = Class.forName("org.hyperic.sigar.SigarProxyCache").getMethod("newInstance",
                sigar.getClass(), int.class);

            // Update CPU info every 2 seconds.
            this.sigar = proxyMtd.invoke(null, sigar, 2000);

            sigarCpuPercMtd = this.sigar.getClass().getMethod("getCpuPerc");

            sigarCpuCombinedMtd = sigarCpuPercMtd.getReturnType().getMethod("getCombined");
        }
        // Purposely catch generic exception.
        catch (Exception e) {
            sigar = null;

            log.warning("Failed to find Hyperic Sigar in classpath: " + e.getMessage());
        }
    }

    /**
     *
     */
    @SuppressWarnings({"CatchGenericClass"})
    private void initializeJdk16() {
        try {
            jdk6CpuLoadMtd = OperatingSystemMXBean.class.getMethod("getSystemLoadAverage");
        }
        // Purposely catch generic exception.
        catch (Exception e) {
            jdk6CpuLoadMtd = null;

            log.warning("Failed to find JDK 1.6 or higher CPU load metrics: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridLocalMetrics getMetrics() {
        MemoryUsage heap = mem.getHeapMemoryUsage();
        MemoryUsage nonHeap = null;

        // Workaround of exception in WebSphere.
        // We received the following exception:
        //java.lang.IllegalArgumentException: used value cannot be larger than the committed value
        //at java.lang.management.MemoryUsage.<init>(MemoryUsage.java:105)
        //at com.ibm.lang.management.MemoryMXBeanImpl.getNonHeapMemoryUsageImpl(Native Method)
        //at com.ibm.lang.management.MemoryMXBeanImpl.getNonHeapMemoryUsage(MemoryMXBeanImpl.java:143)
        //at org.gridgain.grid.spi.metrics.jdk.GridJdkLocalMetricsSpi.getMetrics(GridJdkLocalMetricsSpi.java:242)        //
        //
        // We so had to workaround this with exception handling, because we can not control classes from WebSphere.
        try {
            nonHeap = mem.getNonHeapMemoryUsage();
        }
        catch(IllegalArgumentException e) {
            nonHeap = new MemoryUsage(0, 0, 0, 0);
        }

        metrics = new GridLocalMetricsAdapter(
            os.getAvailableProcessors(),
            getCpuLoad(),
            heap.getInit(),
            heap.getUsed(),
            heap.getCommitted(),
            heap.getMax(),
            nonHeap.getInit(),
            nonHeap.getUsed(),
            nonHeap.getCommitted(),
            nonHeap.getMax(),
            runtime.getUptime(),
            runtime.getStartTime(),
            threads.getThreadCount(),
            threads.getPeakThreadCount(),
            threads.getTotalStartedThreadCount(),
            threads.getDaemonThreadCount()
        );

        return metrics;
    }

    /**
     *
     * @return CPU load.
     */
    @SuppressWarnings({"CatchGenericClass"})
    private double getCpuLoad() {
        double load = -1;

        if (isPreferSigar == false) {
            load = getJdk6CpuLoad();

            if (load < 0) {
                load = getSigarCpuLoad();

                if (load >= 0) {
                    log.warning("JDK 1.6 CPU load is not available (switching to Hyperic Sigar " +
                        "'CpuPerc.getCombined()' CPU metrics).");

                    isPreferSigar = true;
                }
            }
        }
        else {
            load = getSigarCpuLoad();

            if (load < 0) {
                load = getJdk6CpuLoad();

                if (load >= 0) {
                    log.warning("Hyperic Sigar CPU metrics not available (switching to JDK 1.6 " +
                        "'OperatingSystemMXBean.getSystemLoadAverage()' CPU metrics).");

                    isPreferSigar = false;
                }
            }
        }

        if (Double.isNaN(load) == true || Double.isInfinite(load) == true) {
            load = 0.5;
        }

        return load;
    }

    /**
     *
     * @return FIXDOC
     */
    @SuppressWarnings({"CatchGenericClass"})
    private double getJdk6CpuLoad() {
        if (jdk6CpuLoadMtd != null) {
            try {
                return (Double)jdk6CpuLoadMtd.invoke(os) / 100;
            }
            // Purposely catch generic exception.
            catch (Exception e) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Failed to obtain JDK 1.6 CPU load (will return -1): " + e.getMessage());
                }
            }
        }

        return -1;
    }

    /**
     *
     * @return FIXDOC
     */
    @SuppressWarnings({"CatchGenericClass"})
   private double getSigarCpuLoad() {
        if (sigar != null) {
            try {
                return (Double)sigarCpuCombinedMtd.invoke(sigarCpuPercMtd.invoke(sigar));
            }
            // Purposely catch generic exception.
            catch (Exception e) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Failed to obtain Hyperic Sigar CPU load (will return -1): " + e.getMessage());
                }
            }
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public int getAvailableProcessors() {
        return metrics.getAvailableProcessors();
    }

    /**
     * {@inheritDoc}
     */
    public double getCurrentCpuLoad() {
        return metrics.getCurrentCpuLoad();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryInitialized() {
        return metrics.getHeapMemoryInitialized();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryUsed() {
        return metrics.getHeapMemoryUsed();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryCommitted() {
        return metrics.getHeapMemoryCommitted();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryMaximum() {
        return metrics.getHeapMemoryMaximum();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryInitialized() {
        return metrics.getNonHeapMemoryInitialized();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryUsed() {
        return metrics.getNonHeapMemoryUsed();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryCommitted() {
        return metrics.getNonHeapMemoryCommitted();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryMaximum() {
        return metrics.getNonHeapMemoryMaximum();
    }

    /**
     * {@inheritDoc}
     */
    public long getUptime() {
        return metrics.getUptime();
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTime() {
        return metrics.getStartTime();
    }

    /**
     * {@inheritDoc}
     */
    public int getThreadCount() {
        return metrics.getThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    public int getPeakThreadCount() {
        return metrics.getPeakThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTotalStartedThreadCount() {
        return metrics.getTotalStartedThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    public int getDaemonThreadCount() {
        return metrics.getDaemonThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJdkLocalMetricsSpi.class, this);
    }
}
