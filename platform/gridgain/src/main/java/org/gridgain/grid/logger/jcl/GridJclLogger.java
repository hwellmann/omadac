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

package org.gridgain.grid.logger.jcl;

import org.apache.commons.logging.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

/**
 * This logger wraps any JCL (<a target=_blank href="http://jakarta.apache.org/commons/logging/">Jakarta Commons Logging</a>)
 * loggers. Implementation simply delegates to underlying JCL logger. This logger
 * should be used by loaders that have JCL-based internal logging (e.g., Websphere).
 * <p>
 * Here is an example of configuring JCL logger in GridGain configuration Spring 
 * file to work over log4j implementation. Note that we use the same configuration file 
 * as we provide by default:
 * <pre name="code" class="xml">
 *      ...
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.gridgain.grid.logger.jcl.GridJclLogger"&gt;
 *              &lt;constructor-arg type="org.apache.commons.logging.Log"&gt;
 *                  &lt;bean class="org.apache.commons.logging.impl.Log4JLogger"&gt;
 *                      &lt;constructor-arg type="java.lang.String" value="config/default-log4j.xml"/&gt;
 *                  &lt;/bean&gt; 
 *              &lt;/constructor-arg&gt;
 *          &lt;/bean&gt;
 *      &lt;/property&gt;
 *      ...
 * </pre>
 * If you are using system properties to configure JCL logger use following configuration:
 * <pre name="code" class="xml">
 *      ...
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.gridgain.grid.logger.jcl.GridJclLogger"/&gt;
 *      &lt;/property&gt;
 *      ...
 * </pre> 
 * And the same configuration if you'd like to configure GridGain in your code:
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfigurationAdapter();
 *      ...
 *      GridLogger log = new GridJclLogger(new Log4JLogger("config/default-log4j.xml"));
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 * or following for the configuration by means of system properties:
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfigurationAdapter();
 *      ...
 *      GridLogger log = new GridJclLogger();
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 * 
 * Please take a look at <a target=_new href="http://wiki.apache.org/jakarta-commons/Logging/FrequentlyAskedQuestions>Commons logging F.A.Q.</a>
 * for additional information about JCL configuration.
 * <p>
 * It's recommended to use GridGain's logger injection instead of using/instantiating 
 * logger in your task/job code. See {@link GridLoggerResource} annotation about logger 
 * injection.
 * 
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJclLogger implements GridLogger {
    /** JCL implementation proxy. */
    private Log impl = null;

    /**
     * Creates new logger.
     */
    public GridJclLogger() {
        this(LogFactory.getLog(GridJclLogger.class.getName()));
    }
    
    /**
     * Creates new logger with given implementation.
     *
     * @param impl JCL implementation to use.
     */
    public GridJclLogger(Log impl) {
        assert impl != null : "ASSERTION [line=101, file=src/java/org/gridgain/grid/logger/jcl/GridJclLogger.java]";

        this.impl = impl;
    }

    /**
     * {@inheritDoc}
     */
    public GridLogger getLogger(Object ctgr) {
        return new GridJclLogger(LogFactory.getLog(ctgr.toString()));
    }

    /**
     * {@inheritDoc}
     */
    public void debug(String msg) {
        assert impl.isDebugEnabled() == true : "ASSERTION [line=117, file=src/java/org/gridgain/grid/logger/jcl/GridJclLogger.java]. " + "Logging at DEBUG level without checking if DEBUG level is enabled.";

        impl.debug(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void info(String msg) {
        assert impl.isInfoEnabled() == true : "ASSERTION [line=126, file=src/java/org/gridgain/grid/logger/jcl/GridJclLogger.java]. " + "Logging at INFO level without checking if INFO level is enabled.";

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
