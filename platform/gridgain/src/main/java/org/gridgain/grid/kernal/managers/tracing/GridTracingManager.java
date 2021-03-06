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

package org.gridgain.grid.kernal.managers.tracing;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.spi.tracing.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridTracingManager extends GridManagerAdapter<GridTracingSpi> {
    /** Interception listener. */
    private GridMethodsInterceptionListener lsnr = new GridMethodsInterceptionListener();

    /** Proxy factory. */
    private GridProxyFactory proxyFactory = null;

    /**
     * Creates new tracing manager.
     *
     * @param cfg Grid configuration.
     * @param procReg Registry to get access to processors.
     * @param mgrReg Registry to get access to the other managers.
     */
    public GridTracingManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridTracingSpi.class, cfg, procReg, mgrReg, cfg.getTracingSpi());
    }

    /**
     * Sets proxy factory.
     *
     * @param proxyFactory Proxy factory.
     */
    public void setProxyFactory(GridProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        assert proxyFactory != null : "ASSERTION [line=67, file=src/java/org/gridgain/grid/kernal/managers/tracing/GridTracingManager.java]";

        startSpi();

        proxyFactory.addListener(lsnr);

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws GridException {
        proxyFactory.removeListener(lsnr);

        stopSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     * Class handles all interception and redirects to the registered SPI.
     */
    private class GridMethodsInterceptionListener implements GridProxyListener {
        /**
         * {@inheritDoc}
         */
        public void beforeCall(Class<?> cls, String methodName, Object[] args) {
            for (GridTracingSpi spi : getSpis()) {
                spi.beforeCall(cls, methodName, args);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void afterCall(Class<?> cls, String methodName, Object[] args, Object res, Throwable exc) {
            for (GridTracingSpi spi : getSpis()) {
                spi.afterCall(cls, methodName, args, res, exc);
            }
        }
    }
}
