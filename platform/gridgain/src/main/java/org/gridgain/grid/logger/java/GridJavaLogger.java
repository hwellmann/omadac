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

package org.gridgain.grid.logger.java;

import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import java.util.logging.*;

/**
 * Logger to use with Java logging. Implementation simply delegates to Java Logging.
 * <p>
 * Here is an example of configuring Java logger in GridGain configuration Spring 
 * file to work over log4j implementation. Note that we use the same configuration file 
 * as we provide by default:
 * <pre name="code" class="xml">
 *      ...
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.gridgain.grid.logger.java.GridJavaLogger"&gt;
 *              &lt;constructor-arg type="java.util.logging.Logger"&gt;
 *                  &lt;bean class="java.util.logging.Logger"&gt;
 *                      &lt;constructor-arg type="java.lang.String" value="global"/&gt;
 *                  &lt;/bean&gt; 
 *              &lt;/constructor-arg&gt;
 *          &lt;/bean&gt;
 *      &lt;/property&gt;
 *      ...
 * </pre>
 * or
 * <pre name="code" class="xml">
 *      ...
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.gridgain.grid.logger.java.GridJavaLogger"/&gt;
 *      &lt;/property&gt;
 *      ...
 * </pre>
 * And the same configuration if you'd like to configure GridGain in your code:
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfigurationAdapter();
 *      ...
 *      GridLogger log = new GridJavaLogger(Logger.global);
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 * or which is actually the same:
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfigurationAdapter();
 *      ...
 *      GridLogger log = new GridJavaLogger();
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 * Please take a look at <a target=_new href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/logging/Logger.html>Logger javadoc</a> 
 * for additional information.
 * <p>
 * It's recommended to use GridGain's logger injection instead of using/instantiating 
 * logger in your task/job code. See {@link GridLoggerResource} annotation about logger 
 * injection.
 * 
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJavaLogger implements GridLogger {
    /** Java Logging implementation proxy. */
    private Logger impl = null;

    /**
     * Creates new logger.
     */
    public GridJavaLogger() {
        this(Logger.global);
    }
    
    /**
     * Creates new logger with given implementation.
     *
     * @param impl Java Logging implementation to use.
     */
    public GridJavaLogger(Logger impl) {
        assert impl != null : "ASSERTION [line=98, file=src/java/org/gridgain/grid/logger/java/GridJavaLogger.java]";

        this.impl = impl;
    }

    /**
     * {@inheritDoc}
     */
    public GridLogger getLogger(Object ctgr) {
        return new GridJavaLogger(Logger.getLogger(ctgr.toString()));
    }

    /**
     * {@inheritDoc}
     */
    public void debug(String msg) {
        assert impl.isLoggable(Level.FINE) == true : "Logging at DEBUG level without checking if DEBUG " +
            "level is enabled.";

        impl.fine(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void info(String msg) {
        assert impl.isLoggable(Level.INFO) == true : "Logging at INFO level without checking if INFO " +
            "level is enabled.";

        impl.info(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void warning(String msg) {
        impl.warning(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void warning(String msg, Throwable e) {
        impl.log(Level.WARNING, msg, e);
    }

    /**
     * {@inheritDoc}
     */
    public void error(String msg) {
        impl.warning(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void error(String msg, Throwable e) {
        impl.log(Level.WARNING, msg, e);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDebugEnabled() {
        return impl.isLoggable(Level.FINE) == true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInfoEnabled() {
        return impl.isLoggable(Level.INFO) == true;
    }
}
