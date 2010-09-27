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

package org.gridgain.grid.loaders.jboss;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.loaders.*;
import org.gridgain.grid.logger.jboss.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.jboss.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.jboss.system.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * This is GridGain loader implemented as JBoss service. See {@link GridJbossLoaderMBean} for
 * configuration information (according to JBoss service convention). This loader should be
 * used for tight integration with JBoss. Specifically, it integrates GridGain with JBoss's logger
 * and MBean server. This loader should be used with <tt>[GRIDGAIN_HOME/config/jboss/jboss-service.xml</tt>
 * file shipped with GridGain. See <a target=wiki href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Configuring+and+Starting+GridGain">Configuring and Starting GridGain</a>
 * for more information.
 * <p>
 * <b>Note</b>: JBoss is not shipped with GridGain. If you don't have JBoss, you need to
 * download it separately. See <a target=_blank href="http://www.jboss.com">http://www.jboss.com</a> for
 * more information.
 * <p>
 * <b>Note</b>: When using JBoss discovery SPI ({@link GridJbossDiscoverySpi}) you cannot start
 * multiple GridGain instances in the same VM due to limitations of JBoss. GridGain runtime
 * will detect this situation and prevent GridGain from starting in such case.
 * See {@link GridSpiMultipleInstancesSupport} for detail.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridLoader(description = "JBoss loader")
public class GridJbossLoader extends ServiceMBeanSupport implements GridJbossLoaderMBean {
    /** Copyright text. Ant processed. */
    private static final String COPYRIGHT = "Copyright (C) 2005-2009 GridGain Systems.";

    /** Version. Ant processed. */
    private static final String VER = "2.1.1";

    /** Support email. Ant processed. */
    private static final String EMAIL = "support@gridgain.com";

    /** Configuration file path. */
    private String cfgFile = null;

    /** */
    private List<String> gridNames = new ArrayList<String>();

    /**
     * Prints logo.
     */
    private void logo() {
        if (getLog().isInfoEnabled() ==true) {
            getLog().info("GridGain JBoss 4 Loader, ver. " + VER);
            getLog().info(COPYRIGHT);
            getLog().info("Support: " + EMAIL);
            getLog().info("");
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    protected void startService() throws Exception {
        logo();

        File path = GridUtils.resolveGridGainPath(cfgFile);

        if (path == null) {
            throw (GridException)new GridException("Spring XML configuration file path is invalid: " + new File(cfgFile) +
                ". Note that this path should be either absolute path or a relative path to GRIDGAIN_HOME.").setData(99, "src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java");
        }

        if (path.isFile() == false) {
            throw (GridException)new GridException("Provided file path is not a file: " + path).setData(104, "src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java");
        }

        ApplicationContext springCtx = null;

        try {
            springCtx = new FileSystemXmlApplicationContext(path.toURI().toURL().toString());
        }
        catch (BeansException e) {
            throw (GridException)new GridException("Failed to instantiate Spring XML application context: " + e.getMessage(), e).setData(113, "src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java");
        }
        catch (MalformedURLException e) {
            throw (GridException)new GridException("Failed to instantiate Spring XML application context: " + e.getMessage(), e).setData(116, "src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java");
        }

        Map cfgMap = null;

        try {
            // Note: Spring is not generics-friendly.
            cfgMap = springCtx.getBeansOfType(GridConfiguration.class);
        }
        catch (BeansException e) {
            throw (GridException)new GridException("Failed to instantiate bean [type=" + GridConfiguration.class + ", error=" +
                e.getMessage() + ']', e).setData(126, "src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java");
        }

        if (cfgMap == null) {
            throw (GridException)new GridException("Failed to find a single grid factory configuration in: " + path).setData(131, "src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java");
        }

        if (cfgMap.size() == 0) {
            throw (GridException)new GridException("Can't find grid factory configuration in: " + path).setData(135, "src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java");
        }

        for (GridConfiguration cfg : (Collection<GridConfiguration>)cfgMap.values()) {
            assert cfg != null : "ASSERTION [line=139, file=src/java/org/gridgain/grid/loaders/jboss/GridJbossLoader.java]";

            GridConfigurationAdapter adapter = new GridConfigurationAdapter(cfg);

            if (cfg.getMBeanServer() == null) {
                adapter.setMBeanServer(getServer());
            }

            if (cfg.getGridLogger() == null) {
                adapter.setGridLogger(new GridJbossLogger(getLog()));
            }

            Grid grid = GridFactory.start(adapter, springCtx);

            // Test if grid is not null - started properly.
            if (grid != null) {
                gridNames.add(grid.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopService() throws Exception {
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
        return GridToStringBuilder.toString(GridJbossLoader.class, this);
    }
}
