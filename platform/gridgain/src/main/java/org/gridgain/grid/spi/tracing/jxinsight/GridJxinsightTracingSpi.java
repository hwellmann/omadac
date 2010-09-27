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

package org.gridgain.grid.spi.tracing.jxinsight;

import com.jinspired.jxinsight.trace.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.tracing.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Tracing SPI implementation that receives method call notifications from local grid
 * and informs JXInsight Tracer.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has no optional configuration parameters.
 * <b>Note</b>: JXInsight is not shipped with GridGain. If you don't have JXInsight, you need to
 * download it separately. See <a target=_blank href="http://www.jinspired.com/products/jxinsight">http://www.jinspired.com/products/jxinsight</a>
 * for more information. Once installed, JXInsight should be available on the classpath for
 * GridGain. If you use <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> script to start
 * a grid node you can simply add JXInsight JARs to <tt>[GRIDGAIN_HOME]/bin/setenv.{sh|bat}</tt>
 * scripts that's used to set up class path for the main scripts.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(false)
public class GridJxinsightTracingSpi extends GridSpiAdapter implements GridTracingSpi, GridJxinsightTracingSpiMBean {
    /** Logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        registerMBean(gridName, this, GridJxinsightTracingSpiMBean.class);

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
    public void beforeCall(Class<?> cls, String methodName, Object[] args) {
        if (Tracer.isEnabled() == true) {
            Tracer.start(cls.getSimpleName() + '.' + methodName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void afterCall(Class<?> cls, String methodName, Object[] args, Object res, Throwable exc) {
        if (Tracer.isEnabled() == true) {
            String trace = cls.getSimpleName() + '.' + methodName;

            if (Tracer.getCurrentTrace().equals(trace) == true)  {
                Tracer.stop();
            }
            else {
                log.error("Unable to close trace as it was not started in this thread (ignoring): " + trace);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJxinsightTracingSpi.class, this);
    }  
}
