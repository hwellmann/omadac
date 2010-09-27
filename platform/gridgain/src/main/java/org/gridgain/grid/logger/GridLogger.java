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

package org.gridgain.grid.logger;

import org.gridgain.apache.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This interface defines basic logging functionality used throughout the system. We had to
 * abstract it out so that we can use whatever logging is used by the hosting environment. 
 * Currently, <a target=_new href="http://logging.apache.org/log4j/docs/">log4j</a>, 
 * <a target=_new href="http://www.jboss.org/developers/guides/logging">JBoss</a>, 
 * <a target=_new href="http://jakarta.apache.org/commons/logging/">JCL</a> and 
 * console logging are provided as supported implementations.
 * <p>
 * GridGain logger could be configured either from code (for example log4j logger):
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfigurationAdapter();
 *      ...
 *      File xml = GridUtils.resolveGridGainPath("modules/tests/config/log4j-test.xml");
 *      GridLogger log = new GridLog4jLogger(xml);
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 * or in grid configuration file (see JCL logger example below):
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
 *
 * It's recommended to use GridGain's logger injection instead of using/instantiating 
 * logger in your task/job code. See {@link GridLoggerResource} annotation about logger 
 * injection.
 * 
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
@GridToStringExclude
public interface GridLogger {
    /**
     * Creates new logger with given category based off the current instance.
     * 
     * @param ctgr Category for new logger.
     * @return New logger with given category.
     */
    public GridLogger getLogger(Object ctgr);

    /**
     * Logs out debug message.
     * 
     * @param msg Debug message.
     */
    public void debug(String msg);
    
    /**
     * Logs out information message.
     * 
     * @param msg Information message.
     */
    public void info(String msg);

    /**
     * Logs out warning message.
     * 
     * @param msg Warning message.
     */
    public void warning(String msg);

    /**
     * Logs out warning message with optional exception.
     * 
     * @param msg Warning message.
     * @param e Optional exception (can be <tt>null</tt>).
     */
    public void warning(String msg, Throwable e);

    /**
     * Logs out error message.
     * 
     * @param msg Error message.
     */
    public void error(String msg);

    /**
     * Logs error message with optional exception.
     * 
     * @param msg Error message.
     * @param e Optional exception (can be <tt>null</tt>).
     */
    public void error(String msg, Throwable e);

    /**
     * Tests whether <tt>debug</tt> level is enabled.
     * 
     * @return <tt>true</tt> in case when <tt>debug</tt> level is enabled, <tt>false</tt> otherwise.
     */
    public boolean isDebugEnabled();

    /**
     * Tests whether <tt>info</tt> level is enabled.
     * 
     * @return <tt>true</tt> in case when <tt>info</tt> level is enabled, <tt>false</tt> otherwise.
     */
    public boolean isInfoEnabled();
}
