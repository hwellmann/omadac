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

package org.gridgain.grid.loaders.websphere;

import com.ibm.websphere.management.*;
import com.ibm.websphere.runtime.*;
import com.ibm.ws.asynchbeans.*;
import com.ibm.wsspi.asynchbeans.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;
import org.apache.commons.logging.*;
import org.gridgain.grid.*;
import org.gridgain.grid.gridify.*;
import org.gridgain.grid.gridify.aop.spring.*;
import org.gridgain.grid.loaders.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.jcl.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * This is GridGain loader implemented as Websphere custom service (MBean). Websphere
 * loader should be used to provide tight integration between GridGain and Websphere AS.
 * Specifically, Websphere loader integrates GridGain with Websphere logging, MBean server and work
 * manager (<a target=_blank href="http://jcp.org/en/jsr/detail?id=237">JSR-237</a>).
 * <p>
 * The following steps should be taken to configure this loader:
 * <ol>
 * <li>
 *      Add CustomService in administration console (<tt>Application Servers -> server1 -> Custom Services -> New</tt>).
 * </li>
 * <li>
 *      Add custom property for this service: <tt>cfgFilePath=config/default-spring.xml</tt>.
 * </li>
 * <li>
 *      Add the following parameters:
 *      <ul>
 *      <li>Classname: <tt>org.gridgain.grid.loaders.websphere.GridWebsphereLoader</tt></li>
 *      <li>Display Name: <tt>GridGain</tt></li>
 *      <li>
 *          Classpath (replace <tt>[GRIDGAIN_HOME]</tt> with absolute path):
 *          <tt>[GRIDGAIN_HOME]/gridgain.jar:[GRIDGAIN_HOME]/libs/</tt>
 *      </li>
 *      </ul>
 * </li>
 * </ol>
 * <p>
 * For more information consult <a target=_blank href="http://publib.boulder.ibm.com/infocenter/wasinfo/v6r1/index.jsp?topic=/com.ibm.websphere.base.doc/info/aes/ae/trun_customservice.html">Developing Custom Services</a>
 * and <a target=wiki href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Configuring+and+Starting+GridGain">Configuring and Starting GridGain</a>.
 * <p>
 * <b>Note</b>: Websphere is not shipped with GridGain. If you don't have Websphere, you need to
 * download it separately. See <a target=_blank href="http://www.ibm.com/software/websphere/">http://www.ibm.com/software/websphere/</a> for
 * more information.
 * <p>
 * <h1 class="header">How to use AOP with Websphere</h1>
 * The following steps should be taken before using {@link Gridify} annotation in applications on Websphere.
 * <h2 class="header">AspectJ AOP</h2>
 * <ol>
 * <li>
 *      Add <tt>GridWebsphereLoader</tt> with configuration described above.
 * </li>
 * <li>
 *      Classpath text field for Custom Service (<tt>GridWebsphereLoader</tt>) should contain
 *      the <tt>[GRIDGAIN_HOME]/config/aop/aspectj</tt> folder..
 * </li>
 * <li>
 *      Add JVM option <tt>-javaagent:[GRIDGAIN_HOME]/libs/aspectjweaver-1.5.3.jar</tt>
 *      (replace <tt>[GRIDGAIN_HOME]</tt> with absolute path) in admin console
 *      (<tt>Application servers > server1 > Process Definition > Java Virtual Machine</tt> text field
 *      <tt>Generic JVM arguments</tt>)
 * </li>
 * <li>
 *      Add java permission for GridGain classes in <tt>server.policy</tt> file.
 *      For example, in file <tt>/opt/IBM/WebSphere/AppServer/profiles/AppSrv01/properties/server.policy</tt>
 * <pre name="code" class="java">
 * grant codeBase "file:/home/link/svnroot/gridgain/work/libs/-" {
 *     // Allow everything for now
 *     permission java.security.AllPermission;
 * };
 * </pre>
 * </ol>
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
@GridLoader(description = "Websphere loader")
public class GridWebsphereLoader implements GridWebsphereLoaderMBean, CustomService {
    /** Copyright text. Ant processed. */
    private static final String COPYRIGHT = "Copyright (C) 2005-2009 GridGain Systems.";

    /** Version. Ant processed. */
    private static final String VER = "2.1.1";

    /** Support email. Ant processed. */
    private static final String EMAIL = "support@gridgain.com";

    /** Configuration file path. */
    private String cfgFile = null;

    /** Configuration file path variable name. */
    private static final String cfgFilePathParam = "cfgFilePath";

    /** */
    private static final String workMgrParam = "wmName";

    /** */
    private GridLogger log = null;

    /** */
    private List<String> gridNames = new ArrayList<String>();

    /**
     * Prints logo.
     */
    private void logo() {
        if (log.isInfoEnabled() ==true) {
            log.info("GridGain Websphere Loader, ver. " + VER);
            log.info(COPYRIGHT);
            log.info("Support: " + EMAIL);
            log.info("");
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void initialize(Properties properties) throws Exception {
        log = new GridJclLogger(LogFactory.getLog("GridGain"));

        logo();

        cfgFile = properties.getProperty(cfgFilePathParam);

        if (cfgFile == null) {
            throw new IllegalArgumentException("Failed to read property: " + cfgFilePathParam);
        }

        String workMgrName = properties.getProperty(workMgrParam);

        File path = GridUtils.resolveGridGainPath(cfgFile);

        if (path == null) {
            throw new IllegalArgumentException("Spring XML configuration file path is invalid: " + new File(cfgFile) +
                ". Note that this path should be either absolute path or a relative path to GRIDGAIN_HOME.");
        }

        if (path.isFile() == false) {
            throw new IllegalArgumentException("Provided file path is not a file: " + path);
        }

        ApplicationContext springCtx = null;

        try {
            springCtx = new FileSystemXmlApplicationContext(path.toURI().toURL().toString());
        }
        catch (BeansException e) {
            throw new IllegalArgumentException("Failed to instantiate Spring XML application context: " +
                e.getMessage(), e);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failed to instantiate Spring XML application context: " +
                e.getMessage(), e);
        }

        Map cfgMap = null;

        try {
            // Note: Spring is not generics-friendly.
            cfgMap = springCtx.getBeansOfType(GridConfiguration.class);
        }
        catch (BeansException e) {
            throw new IllegalArgumentException("Failed to instantiate bean [type=" + GridConfiguration.class + ", error=" +
                e.getMessage() + ']', e);
        }

        if (cfgMap == null) {
            throw new IllegalArgumentException("Failed to find a single grid factory configuration in: " + path);
        }

        if (cfgMap.size() == 0) {
            throw new IllegalArgumentException("Can't find grid factory configuration in: " + path);
        }

        try {
            ExecutorService execSvc = null;

            MBeanServer mbeanSrvr = null;

            for (GridConfiguration cfg : (Collection<GridConfiguration>)cfgMap.values()) {
                assert cfg != null : "ASSERTION [line=223, file=src/java/org/gridgain/grid/loaders/websphere/GridWebsphereLoader.java]";

                GridConfigurationAdapter adapter = new GridConfigurationAdapter(cfg);

                // Set WebSphere logger.
                if (cfg.getGridLogger() == null) {
                    adapter.setGridLogger(log);
                }

                if (cfg.getExecutorService() == null) {
                    if (execSvc == null) {
                        if (workMgrName != null) {
                            execSvc = new GridWorkManagerExecutorService(workMgrName);
                        }
                        else {
                            // Obtain/create singleton.
                            J2EEServiceManager j2eeMgr = J2EEServiceManager.getSelf();

                            // Start it if was not started before.
                            j2eeMgr.start();

                            // Create new configuration.
                            CommonJWorkManagerConfiguration workMgrCfg = j2eeMgr.createCommonJWorkManagerConfiguration();

                            workMgrCfg.setName("GridGain");
                            workMgrCfg.setJNDIName("wm/gridgain");

                            // Set worker.
                            execSvc = new GridWorkManagerExecutorService(j2eeMgr.getCommonJWorkManager(workMgrCfg));
                        }
                    }

                    adapter.setExecutorService(execSvc);
                }

                if (cfg.getMBeanServer() == null) {
                    if (mbeanSrvr == null) {
                        mbeanSrvr = AdminServiceFactory.getMBeanFactory().getMBeanServer();
                    }

                    adapter.setMBeanServer(mbeanSrvr);
                }

                Grid grid = GridFactory.start(adapter, springCtx);

                // Test if grid is not null - started properly.
                if (grid != null) {
                    gridNames.add(grid.getName());
                }
            }
        }
        catch (GridException e) {
            // Stop started grids only.
            for (String name: gridNames) {
                GridFactory.stop(name, true);
            }

            throw new IllegalArgumentException("Failed to start GridGain.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() throws Exception {
        // Stop started grids only.
        for (String name: gridNames) {
            GridFactory.stop(name, true);
        }
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
        return GridToStringBuilder.toString(GridWebsphereLoader.class, this);
    }
}
