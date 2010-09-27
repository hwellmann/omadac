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

package org.gridgain.grid.logger.log4j;

import java.io.*;
import java.net.*;
import org.apache.log4j.*;
import org.apache.log4j.varia.*;
import org.apache.log4j.xml.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Log4j-based implementation for logging. This logger should be used
 * by loaders that have prefer <a target=_new href="http://logging.apache.org/log4j/docs/">log4j</a>-based logging.
 * <p>
 * Here is a typical example of configuring log4j logger in GridGain configuration file:
 * <pre name="code" class="xml">
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.gridgain.grid.logger.log4j.GridLog4jLogger"&gt;
 *              &lt;constructor-arg type="java.lang.String" value="config/default-log4j.xml"/&gt;
 *          &lt;/bean>
 *      &lt;/property&gt;
 * </pre>
 * and from your code:
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfigurationAdapter();
 *      ...
 *      File xml = GridUtils.resolveGridGainPath("modules/tests/config/log4j-test.xml");
 *      GridLogger log = new GridLog4jLogger(xml);
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 *
 * Please take a look at <a target=_new href="http://logging.apache.org/log4j/1.2/index.html>Apache Log4j 1.2</a>
 * for additional information.
 * <p>
 * It's recommended to use GridGain's logger injection instead of using/instantiating
 * logger in your task/job code. See {@link GridLoggerResource} annotation about logger
 * injection.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridLog4jLogger implements GridLogger {
    /** Log4j implementation proxy. */
    @GridToStringExclude
    private final Logger impl;

    /** Path to configuration file. */
    private final String path;

    /**
     * Creates new logger and automatically detects if root logger already
     * has appenders configured. If it does not, the root logger will be
     * configured with default appender (analogous to calling
     * {@link #GridLog4jLogger(boolean) GridLog4jLogger(boolean)}
     * with parameter <tt>true</tt>, otherwise, existing appenders will be used (analogous
     * to calling {@link #GridLog4jLogger(boolean) GridLog4jLogger(boolean)}
     * with parameter <tt>false</tt>).
     */
    public GridLog4jLogger() {
        this(isConfigured() == false);
    }

    /**
     * Creates new logger. If initialize parameter is <tt>true</tt> the Log4j
     * logger will be initialized with default console appender. In this case
     * the log level will be set to <tt>DEBUG</tt> if system property
     * <tt>GRIDGAIN_DFLT_LOG_DEBUG</tt> is present with any <tt>non-null</tt>
     * value, otherwise the log level will be set to <tt>INFO</tt>.
     *
     * @param init If <tt>true</tt>, then a default console appender with
     *      following pattern layout will be created: <tt>%d{ABSOLUTE} %-5p [%c{1}] %m%n</tt>.
     *      If <tt>false</tt>, then no implicit initialization will take place,
     *      and <tt>Log4j</tt> should be configured prior to calling this
     *      constructor.
     */
    public GridLog4jLogger(boolean init) {
        if (init == true) {
            final String fmt = "[%d{ABSOLUTE}][%-5p][%t][%c{1}] %m%n";

            // Configure output that should go to System.out
            ConsoleAppender app = new ConsoleAppender(new PatternLayout(fmt), ConsoleAppender.SYSTEM_OUT);

            LevelRangeFilter lvlFilter = new LevelRangeFilter();

            lvlFilter.setLevelMin(Level.DEBUG);
            lvlFilter.setLevelMax(Level.INFO);

            app.addFilter(lvlFilter);

            BasicConfigurator.configure(app);

            // Configure output that should go to System.err
            app = new ConsoleAppender(new PatternLayout(fmt), ConsoleAppender.SYSTEM_ERR);

            app.setThreshold(Level.WARN);

            impl = Logger.getRootLogger();

            impl.addAppender(app);

            impl.setLevel(System.getProperty("GRIDGAIN_DFLT_LOG_DEBUG") != null ? Level.DEBUG : Level.INFO);
        }
        else {
            impl = Logger.getRootLogger();
        }

        path = null;
    }

    /**
     * Creates new logger with given implementation.
     *
     * @param impl Log4j implementation to use.
     */
    public GridLog4jLogger(Logger impl) {
        assert impl != null : "ASSERTION [line=141, file=src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java]";

        this.impl = impl;

        path = null;
    }

    /**
     * Creates new logger with given configuration <tt>path</tt>.
     *
     * @param path Path to log4j configuration XML file.
     * @throws GridException Thrown in case logger can't be created.
     */
    public GridLog4jLogger(String path) throws GridException {
        if (path == null) {
            throw (GridException)new GridException("Configuration XML file for Log4j must be specified.").setData(156, "src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java");
        }

        this.path = path;

        File cfgFile = GridUtils.resolveGridGainPath(path);

        if (cfgFile == null || cfgFile.isDirectory() == true) {
            throw (GridException)new GridException("Log4j configuration path was not found or is a directory: " + path).setData(164, "src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java");
        }

        DOMConfigurator.configure(cfgFile.getAbsolutePath());

        impl = Logger.getRootLogger();
    }

    /**
     * Creates new logger with given configuration <tt>cfgFile</tt>.
     *
     * @param cfgFile Log4j configuration XML file.
     * @throws GridException Thrown in case logger can't be created.
     */
    public GridLog4jLogger(File cfgFile) throws GridException {
        if (cfgFile == null) {
            throw (GridException)new GridException("Configuration XML file for Log4j must be specified.").setData(180, "src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java");
        }

        if (cfgFile.exists() == false || cfgFile.isDirectory() == true) {
            throw (GridException)new GridException("Log4j configuration path was not found or is a directory: " + cfgFile).setData(184, "src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java");
        }

        path = cfgFile.getAbsolutePath();

        DOMConfigurator.configure(path);

        impl = Logger.getRootLogger();
    }

    /**
     * Creates new logger with given configuration <tt>cfgUrl</tt>.
     *
     * @param cfgUrl URL for Log4j configuration XML file.
     * @throws GridException Thrown in case logger can't be created.
     */
    public GridLog4jLogger(URL cfgUrl) throws GridException {
        if (cfgUrl == null) {
            throw (GridException)new GridException("Configuration XML file for Log4j must be specified.").setData(202, "src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java");
        }

        DOMConfigurator.configure(cfgUrl);

        impl = Logger.getRootLogger();

        path = null;
    }

    /**
     * Checks if Log4j is already configured within this VM or not.
     *
     * @return <tt>True</tt> if log4j was already configured, <tt>false</tt> otherwise.
     */
    public static boolean isConfigured() {
        return Logger.getRootLogger().getAllAppenders().hasMoreElements() == true;
    }

    /**
     * Sets level for internal log4j implementation.
     *
     * @param level Log level to set.
     */
    public void setLevel(Level level) {
        impl.setLevel(level);
    }

    /**
     * Gets {@link GridLogger} wrapper around log4j logger for the given
     * category. If category is <tt>null</tt>, then root logger is returned. If
     * category is an instance of {@link Class} then <tt>((Class)ctgr).getName()</tt>
     * is used as category name.
     *
     * @param ctgr {@inheritDoc}
     * @return {@link GridLogger} wrapper around log4j logger.
     */
    public GridLog4jLogger getLogger(Object ctgr) {
        return new GridLog4jLogger(ctgr == null ? Logger.getRootLogger() :
            ctgr instanceof Class == true ? Logger.getLogger(((Class<?>)ctgr).getName()) :
                Logger.getLogger(ctgr.toString()));
    }

    /**
     * {@inheritDoc}
     */
    public void debug(String msg) {
        assert impl.isDebugEnabled() == true : "ASSERTION [line=249, file=src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java]. " + "Logging at DEBUG level without checking if DEBUG level is enabled.";

        impl.debug(msg);
    }

    /**
     * {@inheritDoc}
     */
    public void info(String msg) {
        assert impl.isInfoEnabled() == true : "ASSERTION [line=258, file=src/java/org/gridgain/grid/logger/log4j/GridLog4jLogger.java]. " + "Logging at INFO level without checking if INFO level is enabled.";

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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridLog4jLogger.class, this);
    }
}
