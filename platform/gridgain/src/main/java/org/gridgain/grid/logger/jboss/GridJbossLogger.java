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

package org.gridgain.grid.logger.jboss;

import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.jboss.logging.*;

/**
 * Logger to use in JBoss loaders. Implementation simply delegates to 
 * <a target=_new href="http://www.jboss.org/developers/guides/logging">JBoss</a> logging.
 * <p>
 * Please take a look at <a target=_new href="http://wiki.jboss.org/wiki/Logging">JBoss Wiki</a> 
 * and <a target=_new href="http://docs.jboss.org/process-guide/en/html/logging.html>Logging guide</a> 
 * for additional information.
 * <p>
 * It's recommended to use GridGain's logger injection instead of using/instantiating 
 * logger in your task/job code. See {@link GridLoggerResource} annotation about logger 
 * injection.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridJbossLogger
 */
public class GridJbossLogger implements GridLogger {
    /** Log4j implementation proxy. */
    private Logger impl = null;

    /**
     * Creates new logger with given implementation.
     */
    public GridJbossLogger() {
        this(Logger.getLogger("root"));
    }

    /**
     * Creates new logger with given implementation.
     *
     * @param impl Log4j implementation to use.
     */
    public GridJbossLogger(Logger impl) {
        assert impl != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/logger/jboss/GridJbossLogger.java]";

        this.impl = impl;
    }

    /**
     * {@inheritDoc}
     */
    public GridJbossLogger getLogger(Object ctgr) {
        return new GridJbossLogger(Logger.getLogger(ctgr.toString()));
    }

    /**
     * {@inheritDoc}
     */
    public void debug(String msg) {
        assert impl.isDebugEnabled() == true : "ASSERTION [line=77, file=src/java/org/gridgain/grid/logger/jboss/GridJbossLogger.java]. " + "Logging at DEBUG level without checking if DEBUG level is enabled.";

        impl.debug(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void info(String msg) {
        assert impl.isInfoEnabled() == true : "ASSERTION [line=86, file=src/java/org/gridgain/grid/logger/jboss/GridJbossLogger.java]. " + "Logging at INFO level without checking if INFO level is enabled.";

        impl.info(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void warning(String msg) {
        impl.warn(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void warning(String msg, Throwable e) {
        impl.warn(msg, e);
    }

    /**
     * {@inheritDoc}
     */
    public void error(String msg) {
        impl.error(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void error(String msg, Throwable e) {
        impl.error(msg, e);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDebugEnabled() {
        return impl.isDebugEnabled() == true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInfoEnabled() {
        return impl.isInfoEnabled() == true;
    }
}
