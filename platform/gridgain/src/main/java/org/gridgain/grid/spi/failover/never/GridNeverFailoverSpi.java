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

package org.gridgain.grid.spi.failover.never;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides failover SPI implementation that never fails over. This implementation
 * never fails over a failed job by always returning <tt>null</tt> out of
 * {@link GridFailoverSpi#failover(GridFailoverContext, List)} method.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has no optional configuration parameters.
 * <p>
 * Here is a Java example on how to configure grid with <tt>GridNeverFailoverSpi</tt>:
 * <pre name="code" class="java">
 * GridJobStealingFailoverSpi spi = new GridJobStealingFailoverSpi();
 * 
 * // Override maximum failover attempts.
 * spi.setMaximumFailoverAttempts(5);
 * 
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 * 
 * // Override default failover SPI.
 * cfg.setFailoverSpiSpi(spi);
 * 
 * // Start grid.
 * GridFactory.start(cfg);  
 * </pre>
 * Here is an example on how to configure grid with <tt>GridNeverFailoverSpi</tt> from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;property name="failoverSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.failover.never.GridNeverFailoverSpi"/&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridFailoverSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridNeverFailoverSpi extends GridSpiAdapter implements GridFailoverSpi, GridNeverFailoverSpiMBean {
    /** Injected grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        registerMBean(gridName, this, GridNeverFailoverSpiMBean.class);

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
     * {@inheritDoc}
     */
    public GridNode failover(GridFailoverContext ctx, List<GridNode> top) {
        log.warning("Returning 'null' node for failed job (failover will not happen) [job=" + 
            ctx.getJobResult().getJob() + ", task=" +  ctx.getTaskSession().getTaskName() + 
            ", sessionId=" + ctx.getTaskSession().getId() + ']');
        
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridNeverFailoverSpi.class, this);
    }  
}
