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

import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.metrics.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management MBean for {@link GridJdkLocalMetricsSpi} SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to JDK local metrics SPI configuration.")
public interface GridJdkLocalMetricsSpiMBean extends GridLocalMetrics, GridSpiManagementMBean {
    /**
     * Configuration parameter indicating if Hyperic Sigar should be used regardless
     * of JDK version. Hyperic Sigar is used to provide CPU load. Starting with JDK 1.6,
     * method <tt>OperatingSystemMXBean.getSystemLoadAverage()</tt> method was added.
     * However, even in 1.6 and higher this method does not always provide CPU load
     * on some operating systems - in such cases Hyperic Sigar will be used automatically.
     *
     * @return If <tt>true</tt> then Hyperic Sigar should be used regardless of JDK version,
     *      if <tt>false</tt>, then implementation will attempt to use
     *      <tt>OperatingSystemMXBean.getSystemLoadAverage()</tt> for JDK 1.6 and higher.
     */
    @GridMBeanDescription("Parameter indicating if Hyperic Sigar should be used regardless of JDK version.")
    public boolean isPreferSigar();
}
