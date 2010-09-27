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

package org.gridgain.grid.loaders.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.logging.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.jcl.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * This class defines servlet-based GridGain loader. This loader can be used to start GridGain
 * inside any web container as servlet.
 * Loader must be defined in <tt>web.xml</tt> file.
 * <pre name="code" class="xml">
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;GridGain&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;org.gridgain.grid.loaders.servlet.GridServletLoader&lt;/servlet-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;cfgFilePath&lt;/param-name&gt;
 *         &lt;param-value&gt;config/default-spring.xml&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 * </pre>
 * <p>
 * Servlet-based loader may be used in any web container like Tomcat, Jetty and etc.
 * Depending on the way this loader is deployed the GridGain instance can be accessed
 * by either all web applications or by only one. See web container class loading architecture:
 * <ul>
 * <li><a target=_blank href="http://tomcat.apache.org/tomcat-5.5-doc/class-loader-howto.html">http://tomcat.apache.org/tomcat-5.5-doc/class-loader-howto.html</a></li>
 * <li><a target=_blank href="http://docs.codehaus.org/display/JETTY/Classloading">http://docs.codehaus.org/display/JETTY/Classloading</a></li>
 * </ul>
 * <p>
 * <h2 class="header">Tomcat</h2>
 * There are two ways to start GridGain on Tomcat.
 * <ul>
 * <li>GridGain started when web container starts and GridGain instance is accessible only to all web applications.
 *      <ol>
 *      <li>Add GridGain libraries in Tomcat common loader.
 *          Add in file <tt>${TOMCAT_HOME}/conf/catalina.properties</tt> for property <tt>common.loader</tt>
 *          the following <tt>${GRIDGAIN_HOME}/gridgain.jar,${GRIDGAIN_HOME}/libs/*.jar</tt>
 *          (replace <tt>${GRIDGAIN_HOME}</tt> with absolute path).
 *      </li>
 *      <li>GridGain servlet-based loader in <tt>${TOMCAT_HOME}/conf/web.xml</tt>
 *          <pre name="code" class="xml">
 *          &lt;servlet&gt;
 *              &lt;servlet-name&gt;GridGain&lt;/servlet-name&gt;
 *              &lt;servlet-class&gt;org.gridgain.grid.loaders.servlet.GridServletLoader&lt;/servlet-class&gt;
 *              &lt;init-param&gt;
 *                  &lt;param-name&gt;cfgFilePath&lt;/param-name&gt;
 *                  &lt;param-value&gt;config/default-spring.xml&lt;/param-value&gt;
 *              &lt;/init-param&gt;
 *              &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *          &lt;/servlet&gt;
 *          </pre>
 *      </li>
 *      </ol>
 * </li>
 * <li>
 * GridGain started from WAR-file and GridGain instance is accessible only to that web application.
 * Difference with approach described above is that all libraries should be added in WAR file without
 * changes in Tomcat configuration files.
 * </li>
 * </ul>
 * <p>
 * <h2 class="header">Jetty</h2>
 * Below is Java code example with Jetty API:
 * <pre name="code" class="java">
 * Server service = new Server();
 *
 * service.addListener("localhost:8090");
 *
 * ServletHttpContext ctx = (ServletHttpContext)service.getContext("/");
 *
 * ServletHolder servlet = ctx.addServlet("GridGain", "/GridGainLoader",
 *      "org.gridgain.grid.loaders.servlet.GridServletLoader");
 *
 * servlet.setInitParameter("cfgFilePath", "config/default-spring.xml");
 *
 * servlet.setInitOrder(1);
 *
 * servlet.start();
 *
 * service.start();
 * </pre>
 * For more information see
 * <a target=wiki href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Configuring+and+Starting+GridGain">Configuring and Starting GridGain</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridServletLoader extends HttpServlet {
    /** Copyright text. Ant processed. */
    private static final String COPYRIGHT = "Copyright (C) 2005-2009 GridGain Systems.";

    /** Version. Ant processed. */
    private static final String VER = "2.1.1";

    /** Support email. Ant processed. */
    private static final String EMAIL = "support@gridgain.com";

    /** Grid loaded flag. */
    private static boolean loaded = false;

    /** Configuration file path. */
    private String cfgFile = null;

    /** Configuration file path variable name. */
    private static final String cfgFilePathParam = "cfgFilePath";

    /** */
    private GridLogger log = null;

    /** */
    private List<String> gridNames = new ArrayList<String>();

    /**
     * Prints logo.
     */
    private void logo() {
        if (log.isInfoEnabled() ==true) {
            log.info("GridGain Servlet Loader, ver. " + VER);
            log.info(COPYRIGHT);
            log.info("Support: " + EMAIL);
            log.info("");
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public void init() throws ServletException {
        // Avoid multiple servlet instances. GridGain should be loaded once.
        if (loaded == true) {
            return;
        }

        log = new GridJclLogger(LogFactory.getLog("GridGain"));

        logo();

        cfgFile = getServletConfig().getInitParameter(cfgFilePathParam);

        File path = GridUtils.resolveGridGainPath(cfgFile);

        if (path == null) {
            throw new ServletException("Spring XML configuration file path is invalid: " + new File(cfgFile) +
                ". Note that this path should be either absolute path or a relative path to GRIDGAIN_HOME.");
        }

        if (path.isFile() == false) {
            throw new ServletException("Provided file path is not a file: " + path);
        }

        ApplicationContext springCtx = null;

        try {
            springCtx = new FileSystemXmlApplicationContext(path.toURI().toURL().toString());
        }
        catch (BeansException e) {
            throw new ServletException("Failed to instantiate Spring XML application context: " +
                e.getMessage(), e);
        }
        catch (MalformedURLException e) {
            throw new ServletException("Failed to instantiate Spring XML application context: " +
                e.getMessage(), e);
        }

        Map cfgMap = null;

        try {
            // Note: Spring is not generics-friendly.
            cfgMap = springCtx.getBeansOfType(GridConfiguration.class);
        }
        catch (BeansException e) {
            throw new ServletException("Failed to instantiate bean [type=" + GridConfiguration.class + ", error=" +
                e.getMessage() + ']', e);
        }

        if (cfgMap == null) {
            throw new ServletException("Failed to find a single grid factory configuration in: " + path);
        }

        if (cfgMap.size() == 0) {
            throw new ServletException("Can't find grid factory configuration in: " + path);
        }

        try {
            for (GridConfiguration cfg : (Collection<GridConfiguration>)cfgMap.values()) {
                assert cfg != null : "ASSERTION [line=221, file=src/java/org/gridgain/grid/loaders/servlet/GridServletLoader.java]";

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

            throw new ServletException("Failed to start GridGain.", e);
        }

        loaded = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // Stop started grids only.
        for (String name: gridNames) {
            GridFactory.stop(name, true);
        }

        loaded = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridServletLoader.class, this);
    }
}
