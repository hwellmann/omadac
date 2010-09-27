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

package org.gridgain.grid.spi.metrics;

import java.io.*;

import org.gridgain.apache.*;

/**
 * This class represents runtime information available for current VM.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridLocalMetrics extends Serializable {
    /**
     * Returns the number of processors available to the Java virtual machine.
     * This method is equivalent to the {@link Runtime#availableProcessors()}
     * method.
     * <p> This value may change during a particular invocation of
     * the virtual machine.
     *
     * @return The number of processors available to the virtual
     *      machine; never smaller than one.
     */
    public int getAvailableProcessors();

    /**
     * Returns the system load average for the last minute.
     * The system load average is the sum of the number of runnable entities
     * queued to the {@linkplain #getAvailableProcessors available processors}
     * and the number of runnable entities running on the available processors
     * averaged over a period of time.
     * The way in which the load average is calculated is operating system
     * specific but is typically a damped time-dependent average.
     * <p>
     * If the load average is not available, a negative value is returned.
     * <p>
     * This method is designed to provide a hint about the system load
     * and may be queried frequently. The load average may be unavailable on
     * some platform where it is expensive to implement this method.
     * <p>
     * If you are running JDK 1.6 or above and 
     * {@link org.gridgain.grid.spi.metrics.jdk.GridJdkLocalMetricsSpi#setPreferSigar(boolean)}
     * is set to <tt>false</tt> (default is <tt>true</tt>), then this method is equivalent to
     * <tt>OperatingSystemMXBean.getSystemLoadAverage()</tt> method. Otherwise,
     * for JDK 1.5 Hyperic Sigar monitoring will be used.
     * <p>
     * Note that Hyperic Sigar is licensed under GPL. If this license is not
     * suitable for your business, remove hyperic libs from your classpath. In
     * case if this method will not detect JDK 1.6 and will not find Hyperic Sigar
     * libraries in the classpath, <tt>-1</tt> will be returned.
     *
     * @return The system load average; or a negative value if not available.
     */
    public double getCurrentCpuLoad();

    /**
     * Returns the amount of heap memory in bytes that the Java virtual machine
     * initially requests from the operating system for memory management.
     * This method returns <tt>-1</tt> if the initial memory size is undefined.
     * <p>
     * This value represents a setting of the heap memory for Java VM and is
     * not a sum of all initial heap values for all memory pools.
     *
     * @return The initial size of memory in bytes; <tt>-1</tt> if undefined.
     */
    public long getHeapMemoryInitialized();

    /**
     * Returns the current heap size that is used for object allocation.
     * The heap consists of one or more memory pools. This value is
     * the sum of <tt>used</tt> heap memory values of all heap memory pools.
     * <p>
     * The amount of used memory in the returned is the amount of memory
     * occupied by both live objects and garbage objects that have not
     * been collected, if any.
     *
     * @return Amount of heap memory used.
     */
    public long getHeapMemoryUsed();

    /**
     * Returns the amount of heap memory in bytes that is committed for
     * the Java virtual machine to use. This amount of memory is
     * guaranteed for the Java virtual machine to use.
     * The heap consists of one or more memory pools. This value is
     * the sum of <tt>committed</tt> heap memory values of all heap memory pools.
     *
     * @return The amount of committed memory in bytes.
     */
    public long getHeapMemoryCommitted();

    /**
     * Returns the maximum amount of heap memory in bytes that can be
     * used for memory management. This method returns <tt>-1</tt>
     * if the maximum memory size is undefined.
     * <p>
     * This amount of memory is not guaranteed to be available
     * for memory management if it is greater than the amount of
     * committed memory.  The Java virtual machine may fail to allocate
     * memory even if the amount of used memory does not exceed this
     * maximum size.
     * <p>
     * This value represents a setting of the heap memory for Java VM and is
     * not a sum of all initial heap values for all memory pools.
     *
     * @return The maximum amount of memory in bytes; <tt>-1</tt> if undefined.
     */
    public long getHeapMemoryMaximum();

    /**
     * Returns the amount of non-heap memory in bytes that the Java virtual machine
     * initially requests from the operating system for memory management.
     * This method returns <tt>-1</tt> if the initial memory size is undefined.
     * <p>
     * This value represents a setting of non-heap memory for Java VM and is
     * not a sum of all initial heap values for all memory pools.
     *
     * @return The initial size of memory in bytes; <tt>-1</tt> if undefined.
     */
    public long getNonHeapMemoryInitialized();

    /**
     * Returns the current non-heap memory size that is used by Java VM.
     * The non-heap memory consists of one or more memory pools. This value is
     * the sum of <tt>used</tt> non-heap memory values of all non-heap memory pools.
     *
     * @return Amount of none-heap memory used.
     */
    public long getNonHeapMemoryUsed();

    /**
     * Returns the amount of non-heap memory in bytes that is committed for
     * the Java virtual machine to use. This amount of memory is
     * guaranteed for the Java virtual machine to use.
     * The non-heap memory consists of one or more memory pools. This value is
     * the sum of <tt>committed</tt> non-heap memory values of all non-heap memory pools.
     *
     * @return The amount of committed memory in bytes.
     */
    public long getNonHeapMemoryCommitted();

    /**
     * Returns the maximum amount of non-heap memory in bytes that can be
     * used for memory management. This method returns <tt>-1</tt>
     * if the maximum memory size is undefined.
     * <p>
     * This amount of memory is not guaranteed to be available
     * for memory management if it is greater than the amount of
     * committed memory.  The Java virtual machine may fail to allocate
     * memory even if the amount of used memory does not exceed this
     * maximum size.
     * <p>
     * This value represents a setting of the non-heap memory for Java VM and is
     * not a sum of all initial non-heap values for all memory pools.
     *
     * @return The maximum amount of memory in bytes; <tt>-1</tt> if undefined.
     */
    public long getNonHeapMemoryMaximum();

    /**
     * Returns the uptime of the Java virtual machine in milliseconds.
     *
     * @return Uptime of the Java virtual machine in milliseconds.
     */
    public long getUptime();

    /**
     * Returns the start time of the Java virtual machine in milliseconds.
     * This method returns the approximate time when the Java virtual
     * machine started.
     *
     * @return Start time of the Java virtual machine in milliseconds.
     */
    public long getStartTime();

    /**
     * Returns the current number of live threads including both
     * daemon and non-daemon threads.
     *
     * @return the current number of live threads.
     */
    public int getThreadCount();

    /**
     * Returns the peak live thread count since the Java virtual machine
     * started or peak was reset.
     *
     * @return The peak live thread count.
     */
    public int getPeakThreadCount();

    /**
     * Returns the total number of threads created and also started
     * since the Java virtual machine started.
     *
     * @return The total number of threads started.
     */
    public long getTotalStartedThreadCount();

    /**
     * Returns the current number of live daemon threads.
     *
     * @return The current number of live daemon threads.
     */
    public int getDaemonThreadCount();
}
