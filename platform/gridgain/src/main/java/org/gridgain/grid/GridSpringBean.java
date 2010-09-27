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

package org.gridgain.grid;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.context.*;

/**
 * Grid Spring bean allows to bypass {@link GridFactory} methods.
 * In other words, this bean class allows to inject new grid instance from
 * Spring configuration file directly without invoking static
 * {@link GridFactory} methods. This class can be wired directly from
 * Spring and can be referenced from within other Spring beans.
 * By virtue of implementing {@link DisposableBean} and {@link InitializingBean}
 * interfaces, <tt>GridSpringBean</tt> automatically starts and stops underlying
 * grid instance.
 * <p>
 * <tt>GridSpringBean</tt> has following configuration parameters.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This bean has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>Grid configuration (see {@link #setConfiguration(GridConfiguration)})</li>
 * </ul>
 * <h1 class="header">Spring Configuration Example</h1>
 * Here is a typical example of describing it in Spring file:
 * <pre name="code" class="xml">
 * &lt;bean id="mySpringBean" class="org.gridgain.grid.GridSpringBean" scope="singleton"&gt;
 *     &lt;property name="configuration"&gt;
 *         &lt;bean id="grid.cfg" class="org.gridgain.grid.GridConfigurationAdapter" scope="singleton"&gt;
 *             &lt;property name="gridName" value="mySpringGrid"/&gt;
 *         &lt;/bean&gt;
 *     &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * Or use default configuration:
 * <pre name="code" class="xml">
 * &lt;bean id="mySpringBean" class="org.gridgain.grid.GridSpringBean" scope="singleton"/&gt;
 * </pre>
 * <h1 class="header">Java Example</h1>
 * Here is how you may access this bean from code:
 * <pre name="code" class="java">
 * AbstractApplicationContext ctx = new FileSystemXmlApplicationContext("/path/to/spring/file");
 *
 * // Register Spring hook to destroy bean automatically.
 * ctx.registerShutdownHook();
 *
 * Grid grid = (Grid)ctx.getBean("mySpringBean");
 * </pre>
 * <p>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridSpringBean implements Grid, DisposableBean, InitializingBean, ApplicationContextAware {
    /** */
    private Grid grid = null;

    /** */
    private GridConfiguration cfg = null;

    /** */
    private ApplicationContext appCtx = null;

    /**
     * Gets grid configuration.
     *
     * @return Grid Configuration.
     */
    public GridConfiguration getConfiguration() {
        return cfg;
    }

    /**
     * Sets grid configuration.
     *
     * @param cfg Grid configuration.
     */
    public void setConfiguration(GridConfiguration cfg) {
        this.cfg = cfg;
    }

    /**
     * {@inheritDoc}
     */
    public void addDiscoveryListener(GridDiscoveryListener listener) {
        assert grid != null : "ASSERTION [line=112, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.addDiscoveryListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void addLocalEventListener(GridLocalEventListener listener) {
        assert grid != null : "ASSERTION [line=121, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.addLocalEventListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void addMessageListener(GridMessageListener listener) {
        assert grid != null : "ASSERTION [line=130, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.addMessageListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void deployTask(Class<? extends GridTask> taskCls) throws GridException {
        assert grid != null : "ASSERTION [line=140, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.deployTask(taskCls);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void deployTask(Class<? extends GridTask> taskCls, ClassLoader clsLoader) throws GridException {
        assert grid != null : "ASSERTION [line=150, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.deployTask(taskCls, clsLoader);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg) {
        assert grid != null : "ASSERTION [line=159, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskName, arg);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg, long timeout) {
        assert grid != null : "ASSERTION [line=168, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskName, arg, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg, GridTaskListener listener) {
        assert grid != null : "ASSERTION [line=177, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskName, arg, listener);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg, long timeout, GridTaskListener listener) {
        assert grid != null : "ASSERTION [line=186, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskName, arg, timeout, listener);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg) {
        assert grid != null : "ASSERTION [line=195, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskCls, arg);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg, long timeout) {
        assert grid != null : "ASSERTION [line=204, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskCls, arg, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg,
        GridTaskListener listener) {
        assert grid != null : "ASSERTION [line=214, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskCls, arg, listener);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg, long timeout,
        GridTaskListener listener) {
        assert grid != null : "ASSERTION [line=224, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(taskCls, arg, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg) {
        assert grid != null : "ASSERTION [line=233, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(task, arg);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg, long timeout) {
        assert grid != null : "ASSERTION [line=242, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(task, arg, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg, GridTaskListener listener) {
        assert grid != null : "ASSERTION [line=251, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(task, arg, listener);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg, long timeout,
        GridTaskListener listener) {
        assert grid != null : "ASSERTION [line=261, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.execute(task, arg, timeout, listener);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getAllNodes() {
        assert grid != null : "ASSERTION [line=270, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getAllNodes();
    }

    /**
     * {@inheritDoc}
     */
    public long getAllTopologyHash() {
        assert grid != null : "ASSERTION [line=279, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getAllTopologyHash();
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getLocalNode() {
        assert grid != null : "ASSERTION [line=288, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getLocalNode();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Class<? extends GridTask<?, ?>>> getLocalTasks() {
        assert grid != null : "ASSERTION [line=297, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getLocalTasks();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        assert grid != null : "ASSERTION [line=306, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getName();
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getNode(UUID nodeId) {
        assert grid != null : "ASSERTION [line=315, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getNode(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getNodes(GridNodeFilter filter) {
        assert grid != null : "ASSERTION [line=324, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getNodes(filter);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getRemoteNodes() {
        assert grid != null : "ASSERTION [line=333, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getRemoteNodes();
    }

    /**
     * {@inheritDoc}
     */
    public long getTopologyHash(Collection<GridNode> nodes) {
        assert grid != null : "ASSERTION [line=342, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.getTopologyHash(nodes);
    }

    /**
     * {@inheritDoc}
     */
    public ExecutorService newGridExecutorService() {
        assert grid != null : "ASSERTION [line=351, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.newGridExecutorService();
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(UUID nodeId) {
        assert grid != null : "ASSERTION [line=360, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.pingNode(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    public List<GridEvent> queryEvents(GridEventFilter filter, Collection<GridNode> nodes, long timeout)
        throws GridException {
        assert grid != null : "ASSERTION [line=370, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.queryEvents(filter, nodes, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public List<GridEvent> queryLocalEvents(GridEventFilter filter) {
        assert grid != null : "ASSERTION [line=379, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.queryLocalEvents(filter);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeDiscoveryListener(GridDiscoveryListener listener) {
        assert grid != null : "ASSERTION [line=388, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.removeDiscoveryListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeLocalEventListener(GridLocalEventListener listener) {
        assert grid != null : "ASSERTION [line=397, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.removeLocalEventListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeMessageListener(GridMessageListener listener) {
        assert grid != null : "ASSERTION [line=406, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        return grid.removeMessageListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(GridNode node, Serializable msg) throws GridException {
        assert grid != null : "ASSERTION [line=415, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.sendMessage(node, msg);
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(Collection<GridNode> nodes, Serializable msg) throws GridException {
        assert grid != null : "ASSERTION [line=424, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.sendMessage(nodes, msg);
    }

    /**
     * {@inheritDoc}
     */
    public void undeployTask(String taskName) throws GridException {
        assert grid != null : "ASSERTION [line=433, file=src/java/org/gridgain/grid/GridSpringBean.java]";

        grid.undeployTask(taskName);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception {
        // If there were some errors when afterPropertiesSet() was called.
        if (grid != null) {
            // Do not cancel started tasks, wait for them.
            GridFactory.stop(grid.getName(), false, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void afterPropertiesSet() throws Exception {
        if (cfg == null) {
            cfg = new GridConfigurationAdapter();
        }

        GridFactory.start(cfg, appCtx);

        grid = GridFactory.getGrid(cfg.getGridName());
    }

    /**
     * {@inheritDoc}
     */
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        appCtx = ctx;
    }
}
