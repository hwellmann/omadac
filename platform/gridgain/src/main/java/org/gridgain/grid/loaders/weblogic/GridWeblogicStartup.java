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

package org.gridgain.grid.loaders.weblogic;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;
import javax.naming.*;
import org.gridgain.grid.*;
import org.gridgain.grid.gridify.*;
import org.gridgain.grid.gridify.aop.spring.*;
import org.gridgain.grid.loaders.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.java.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.context.support.*;
import weblogic.common.*;
import weblogic.logging.*;
import weblogic.server.*;
import weblogic.work.j2ee.*;

/**
 * GridGain loader for WebLogic implemented as a pair of start and shutdown
 * classes. This is a startup class. Please consult WebLogic documentation
 * on how to configure startup classes in Weblogic. Weblogic loader should
 * be used for tight integration with Weblogic AS. Specifically, Weblogic
 * loader integrates GridGain with Weblogic logging, MBean server, and work
 * manager (<a target=_blank href="http://jcp.org/en/jsr/detail?id=237">JSR-237</a>).
 * <p>
 * The following steps should be taken to configure startup and shutdown classes:
 * <ol>
 * <li>
 *      Add Startup and Shutdown Class in admin console (<tt>Environment -> Startup & Shutdown Classes -> New</tt>).
 * </li>
 * <li>
 *      Add the following parameters for startup class:
 *      <ul>
 *      <li>Name: <tt>GridWeblogicStartup</tt></li>
 *      <li>Classname: <tt>org.gridgain.grid.loaders.weblogic.GridWeblogicStartup</tt></li>
 *      <li>Arguments: <tt>cfgFilePath=config/default-spring.xml</tt></li>
 *      </ul>
 * </li>
 * <li>
 *      Add the following parameters for shutdown class:
 *      <ul>
 *      <li>Name: <tt>GridWeblogicShutdown</tt></li>
 *      <li>Classname: <tt>org.gridgain.grid.loaders.weblogic.GridWeblogicShutdown</tt></li>
 *      </ul>
 * </li>
 * <li>
 *      Change classpath for WebLogic server in startup script:
 *      <tt>CLASSPATH="${CLASSPATH}:${GRIDGAIN_HOME}/gridgain.jar:${GRIDGAIN_HOME}/libs/"</tt>
 * </li>
 * </ol>
 * <p>
 * For more information see
 * <a target=wiki href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Configuring+and+Starting+GridGain">Configuring and Starting GridGain</a> and
 * <a target=_blank href="http://edocs.bea.com/wls/docs100/ConsoleHelp/taskhelp/startup_shutdown/UseStartupAndShutdownClasses.html">Startup and Shutdown Classes.</a>
 * <p>
 * <b>Note</b>: Weblogic is not shipped with GridGain. If you don't have Weblogic, you need to
 * download it separately. See <a target=_blank href="http://www.bea.com">http://www.bea.com</a> for
 * more information.
* <p>
 * <h1 class="header">How to use AOP with Bea WebLogic</h1>
 * The following steps should be taken before using {@link Gridify} annotation in applications on Bea WebLogic.
 * <h2 class="header">AspectJ AOP</h2>
 * <ol>
 * <li>
 *      Add <tt>GridWeblogicStartup</tt> with configuration described above.
 * </li>
 * <li>
 *      Classpath of the WebLogic should contain  the ${GRIDGAIN_HOME}/config/aop/aspectj folder
 *      as well as as all GridGain libraries (see above).
 * </li>
 * <li>
 *      Add JVM option <tt>-javaagent:${GRIDGAIN_HOME}/libs/aspectjweaver-1.5.3.jar</tt>
 *      (replace <tt>${GRIDGAIN_HOME}</tt> with absolute path) into startWeblogic.{sh|bat} script
 *      which is located in "your_domain/bin" directory.
 * </li>
 * </ol>
 * <b>Note</b>: Bea Weblogic works much slower if you use AspectJ with it.
 * <p>
 * <h2 class="header">Spring AOP</h2>
 * Spring AOP framework is based on dynamic proxy implementation and doesn't require any
 * specific runtime parameters for online weaving. All weaving is on-demand and should be performed
 * by calling method {@link GridifySpringEnhancer#enhance(Object)} for the object that has method
 * with Gridify annotation.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridLoader(description = "Weblogic loader")
public class GridWeblogicStartup implements GridWeblogicStartupMBean {
    /** Copyright text. Ant processed. */
    private static final String COPYRIGHT = "Copyright (C) 2005-2009 GridGain Systems.";

    /** Version. Ant processed. */
    private static final String VER = "2.1.1";

    /** Support email. Ant processed. */
    private static final String EMAIL = "support@gridgain.com";

    /** Configuration file path variable name. */
    private static final String cfgFilePathParam = "cfgFilePath";

    /** */
    private static final String workMgrParam = "wmName";

    /** Configuration file path. */
    private String cfgFile = null;

    /** */
    private GridLogger log = null;

    /** */
    private List<String> gridNames = new ArrayList<String>();

    /**
     * Prints logo.
     */
    private void logo() {
        if (log.isInfoEnabled() ==true) {
            log.info("GridGain Weblogic Loader, ver. " + VER);
            log.info(COPYRIGHT);
            log.info("Support: " + EMAIL);
            log.info("");
        }
    }

    /**
     * See <a href="http://e-docs.bea.com/wls/docs100/javadocs/weblogic/common/T3StartupDef.html">
     * http://e-docs.bea.com/wls/docs100/javadocs/weblogic/common/T3StartupDef.html</a> for more
     * information.
     *
     * @param str Virtual name by which the class is registered as a <tt>startupClass</tt> in
     *      the <tt>config.xml</tt> file
     * @param params A hashtable that is made up of the name-value pairs supplied from the
     *      <tt>startupArgs</tt> property
     * @return Result string (log message).
     * @throws Exception Thrown if error occurred.
     */
    @SuppressWarnings({"unchecked", "CatchGenericClass", "deprecation"})
    public String startup(String str, Hashtable params) throws Exception {
        log = new GridJavaLogger(LoggingHelper.getServerLogger());

        logo();

        cfgFile = (String)params.get(cfgFilePathParam);

        if (cfgFile == null) {
            throw new IllegalArgumentException("Failed to read property: " + cfgFilePathParam);
        }

        String workMgrName = (String)params.get(workMgrParam);

        File path = GridUtils.resolveGridGainPath(cfgFile);

        if (path == null) {
            throw new ServerLifecycleException("Spring XML configuration file path is invalid: " + new File(cfgFile) +
                ". Note that this path should be either absolute path or a relative path to GRIDGAIN_HOME.");
        }

        if (path.isFile() == false) {
            throw new ServerLifecycleException("Provided file path is not a file: " + path);
        }

        ApplicationContext springCtx = null;

        try {
            springCtx = new FileSystemXmlApplicationContext(path.toURL().toString());
        }
        catch (BeansException e) {
            throw new ServerLifecycleException("Failed to instantiate Spring XML application context: " +
                e.getMessage(), e);
        }
        catch (MalformedURLException e) {
            throw new ServerLifecycleException("Failed to instantiate Spring XML application context: " +
                e.getMessage(), e);
        }

        Map cfgMap = null;

        try {
            // Note: Spring is not generics-friendly.
            cfgMap = springCtx.getBeansOfType(GridConfiguration.class);
        }
        catch (BeansException e) {
            throw new ServerLifecycleException("Failed to instantiate bean [type=" + GridConfiguration.class + ", error=" +
                e.getMessage() + ']', e);
        }

        if (cfgMap == null) {
            throw new ServerLifecycleException("Failed to find a single grid factory configuration in: " + path);
        }

        if (cfgMap.size() == 0) {
            throw new ServerLifecycleException("Can't find grid factory configuration in: " + path);
        }

        try {
            ExecutorService execSvc = null;

            MBeanServer mbeanSrvr = null;

            for (GridConfiguration cfg : (Collection<GridConfiguration>)cfgMap.values()) {
                assert cfg != null : "ASSERTION [line=231, file=src/java/org/gridgain/grid/loaders/weblogic/GridWeblogicStartup.java]";

                GridConfigurationAdapter adapter = new GridConfigurationAdapter(cfg);

                // Set logger.
                if (cfg.getGridLogger() == null) {
                    adapter.setGridLogger(log);
                }

                if (cfg.getExecutorService() == null) {
                    if (execSvc == null) {
                        execSvc = workMgrName != null ? new GridWorkManagerExecutorService(workMgrName) :
                            new GridWorkManagerExecutorService(J2EEWorkManager.getDefault());
                    }

                    adapter.setExecutorService(execSvc);
                }

                if (cfg.getMBeanServer() == null) {
                    if (mbeanSrvr == null) {
                        InitialContext ctx = null;

                        try {
                            ctx = new InitialContext();

                            mbeanSrvr = (MBeanServer)ctx.lookup("java:comp/jmx/runtime");
                        }
                        catch (Exception e) {
                            throw new IllegalArgumentException("MBean server was not provided and failed to obtain " +
                                "Weblogic MBean server.", e);
                        }
                        finally {
                            if (ctx != null) {
                                ctx.close();
                            }
                        }
                    }

                    adapter.setMBeanServer(mbeanSrvr);
                }

                Grid grid = GridFactory.start(adapter, springCtx);

                // Test if grid is not null - started properly.
                if (grid != null) {
                    gridNames.add(grid.getName());
                }
            }

            return getClass().getSimpleName() + " started successfully.";
        }
        catch (GridException e) {
            // Stop started grids only.
            for (String name: gridNames) {
                GridFactory.stop(name, true);
            }

            throw new ServerLifecycleException("Failed to start GridGain.", e);
        }
    }

    /**
     * See <a href="http://e-docs.bea.com/wls/docs100/javadocs/weblogic/common/T3StartupDef.html">
     * http://e-docs.bea.com/wls/docs100/javadocs/weblogic/common/T3StartupDef.html</a> for more
     * information.
     *
     * @param t3ServicesDef Weblogic services accessor.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public void setServices(T3ServicesDef t3ServicesDef) {
        // No-op.
    }

    /**
     * {@inheritDoc}
     */
    public String getConfigurationFile() {
        return cfgFile;
    }

    /**
     * {@inheritDoc}
     */
    public void setConfigurationFile(String cfgFile) {
        this.cfgFile = cfgFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridWeblogicStartup.class, this);
    }
}
