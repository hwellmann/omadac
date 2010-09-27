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

package org.gridgain.grid.loaders.glassfish;

import com.sun.appserv.server.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.logging.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.jcl.*;
import org.gridgain.grid.util.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * This is GridGain loader implemented as GlassFish life-cycle listener module. GlassFish
 * loader should be used to provide tight integration between GridGain and GlassFish AS.
 * Current loader implementation works on both GlassFish v1 and GlassFish v2 servers.
 * <p>
 * The following steps should be taken to configure this loader:
 * <ol>
 * <li>
 *      Add GridGain libraries in GlassFish common loader.<br/>
 *      See GlassFish <a target=_blank href="https://glassfish.dev.java.net/javaee5/docs/DG/beade.html">Class Loaders</a>.
 * </li>
 * <li>
 *      Create life-cycle listener module.<br/>
 *      Use command line or administration GUI.<br/>
 *      asadmin> create-lifecycle-module --user admin --passwordfile ../adminpassword.txt
 *      --classname "org.gridgain.grid.loaders.glassfish.GridGlassfishLoader" --property cfgFilePath="config/default-spring.xml" GridGain
 * </li>
 * </ol>
 * <p>
 * For more information consult <a target=_blank href="https://glassfish.dev.java.net/javaee5/docs/DocsIndex.html">GlassFish Project - Documentation Home Page</a>
 * and <a target=wiki href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Configuring+and+Starting+GridGain">Configuring and Starting GridGain</a>.
 * <p>
 * <b>Note</b>: GlassFish is not shipped with GridGain. If you don't have GlassFish, you need to
 * download it separately. See <a target=_blank href="https://glassfish.dev.java.net">https://glassfish.dev.java.net</a> for
 * more information.
 * <p>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridGlassfishLoader implements LifecycleListener {
    /**
     * Copyright text. Ant processed.
     */
    private static final String COPYRIGHT = "Copyright (C) 2005-2009 GridGain Systems.";

    /**
     * Version. Ant processed.
     */
    private static final String VER = "2.1.1";

    /**
     * Support email. Ant processed.
     */
    private static final String EMAIL = "support@gridgain.com";

    /**
     * Configuration file path.
     */
    private String cfgFile = null;

    /**
     * Configuration file path variable name.
     */
    private static final String cfgFilePathParam = "cfgFilePath";

    /** */
    private GridLogger log = null;

    /** */
    private ClassLoader ctxClsLdr = null;

    /** */
    private List<String> gridNames = new ArrayList<String>();

    /**
     * Prints logo.
     */
    private void logo() {
        if (log.isInfoEnabled() == true) {
            log.info("GridGain Glassfish Loader, ver. " + VER);
            log.info(COPYRIGHT);
            log.info("Support: " + EMAIL);
            log.info("");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void handleEvent(LifecycleEvent evt) throws ServerLifecycleException {
        if (evt.getEventType() == LifecycleEvent.INIT_EVENT) {
            start((Properties)evt.getData());
        }
        else if (evt.getEventType() == LifecycleEvent.SHUTDOWN_EVENT) {
            stop();
        }
    }

    /**
     * Starts all grids with given properties.
     *
     * @param props Startup properties.
     * @throws ServerLifecycleException Thrown in case of startup fails.
     */
    @SuppressWarnings({"unchecked"})
    private void start(Properties props) throws ServerLifecycleException {
        log = new GridJclLogger(LogFactory.getLog("GridGain"));

        logo();

        if (props != null) {
            cfgFile = props.getProperty(cfgFilePathParam);
        }

        ctxClsLdr = Thread.currentThread().getContextClassLoader();

        // Set thread context classloader because Spring use it for loading classes.
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

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
            springCtx = new FileSystemXmlApplicationContext(path.toURI().toURL().toString());
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
            for (GridConfiguration cfg : (Collection<GridConfiguration>)cfgMap.values()) {
                assert cfg != null : "ASSERTION [line=193, file=src/java/org/gridgain/grid/loaders/glassfish/GridGlassfishLoader.java]";

                GridConfigurationAdapter adapter = new GridConfigurationAdapter(cfg);

                // Set Glassfish logger.
                if (cfg.getGridLogger() == null) {
                    adapter.setGridLogger(log);
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

            throw new ServerLifecycleException("Failed to start GridGain.", e);
        }
    }

    /**
     * Stops grids.
     */
    private void stop() {
        Thread.currentThread().setContextClassLoader(ctxClsLdr);

        // Stop started grids only.
        for (String name: gridNames) {
            GridFactory.stop(name, true);
        }
    }
}
