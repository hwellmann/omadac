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
import java.lang.management.*;
import java.net.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import javax.management.*;
import org.apache.log4j.*;
import org.apache.log4j.varia.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.log4j.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.marshaller.jboss.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.spi.checkpoint.sharedfs.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.spi.collision.fifoqueue.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.spi.deployment.local.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.multicast.*;
import org.gridgain.grid.spi.eventstorage.*;
import org.gridgain.grid.spi.eventstorage.memory.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.spi.loadbalancing.roundrobin.*;
import org.gridgain.grid.spi.metrics.*;
import org.gridgain.grid.spi.metrics.jdk.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.grid.spi.topology.basic.*;
import org.gridgain.grid.spi.tracing.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.runnable.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * This interface defines a factory for the main GridGain API. It controls Grid life cycle
 * and allows listening for grid events.
 * <h1 class="header">Grid Loaders</h1>
 * Although user can call grid factory directly to start and stop grid, grid is
 * often started and stopped by grid loaders. Some examples
 * of Grid loaders are:
 * <ul>
 * <li>{@link org.gridgain.grid.loaders.cmdline.GridCommandLineLoader}</li>
 * <li>{@link org.gridgain.grid.loaders.jboss.GridJbossLoader}</li>
 * <li>{@link org.gridgain.grid.loaders.weblogic.GridWeblogicStartup} and {@link org.gridgain.grid.loaders.weblogic.GridWeblogicShutdown}</li>
 * <li>{@link org.gridgain.grid.loaders.websphere.GridWebsphereLoader}</li>
 * <li>{@link org.gridgain.grid.loaders.glassfish.GridGlassfishLoader}</li>
 * <li>{@link org.gridgain.grid.loaders.servlet.GridServletLoader}</li>
 * </ul>
 * <p>
 * <h1 class="header">Grid Configuration</h1>
 * Note that all parameters passed in {@link GridConfiguration} to {@link #start(GridConfiguration)}
 * method are optional. If not provided, default values will be used. Below is the table that provides
 * default value for every configuration parameter.
 * <table class="doctable">
 *   <tr>
 *     <th>GridConfiguration Method</th>
 *     <th>Default Value</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>{@link GridConfiguration#getGridName()}</td>
 *     <td><tt>null</tt></td>
 *     <td>Grid name (<tt>null</tt> is a default value).</td>
 *   </tr>
 *   <tr>
 *     <td>{@link GridConfiguration#getUserAttributes()}</td>
 *     <td>Empty set.</td>
 *     <td>Attributes to be set on local node.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getGridLogger()}</td>
 *      <td>Log4J logger initialized to log only to console (see {@link GridLog4jLogger#GridLog4jLogger(boolean)})</td>
 *      <td>Logger to use within grid.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getExecutorService()}</td>
 *      <td>
 *        Grid thread pool with default initialization (see {@link GridThreadPoolExecutorService}).
 *        The size of the thread pool is <tt>100</tt> by default.
 *      </td>
 *      <td>
 *          Executor service used by grid to handle {@link GridTask} and {@link GridJob} execution.
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getSystemExecutorService()}</td>
 *      <td>
 *          Default thread pool size is <tt>5</tt>.
 *      </td>
 *      <td>
 *          This executor service is in charge of processing {@link GridTaskSession}
 *          requests and user messages sent via {@link Grid#sendMessage(GridNode, Serializable)} method.
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getPeerClassLoadingExecutorService()}</td>
 *      <td>
 *          Default thread pool size is <tt>20</tt>.
 *      </td>
 *      <td>
 *          This executor service is in charge of handling peer class
 *          loading requests/responses.
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getGridGainHome()}</td>
 *      <td>Inferred (see {@link GridConfiguration#getGridGainHome()} for details).</td>
 *      <td>Path to GridGain installation folder.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getMBeanServer()}</td>
 *      <td>Platform MBean server (see {@link ManagementFactory#getPlatformMBeanServer()}).</td>
 *      <td>MBean server used by grid.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getNodeId()}</td>
 *      <td>Randomly generated node ID (see {@link UUID#randomUUID()}).</td>
 *      <td>Local node unique identifier.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#isPeerClassLoadingEnabled()}</td>
 *      <td>
 *          <tt>true</tt>
 *      </td>
 *      <td>
 *          Boolean flag to control peer class loading. When enabled, if a task
 *          is not deployed on a node, the system will attempt to load it from
 *          a peer node (usually from the node that initiated task execution).
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getPeerClassLoadingMissedResourcesCacheSize()}</td>
 *      <td>
 *          <tt>true</tt>
 *      </td>
 *      <td>
 *          Size of internal missed resources cache. Grid will cache all missed
 *          resources and all subsequent call for the same resource on the same class
 *          loader will return <tt>null</tt> immediately. If class was missed then
 *          subsequent call for the same class on the class loader will throw
 *          corresponding exception. But if cache size is less than needed redundant
 *          peer resources/classes requests can be sent.
 *      </td>
 *   </tr>
 *   <tr>
 *     <td>{@link GridConfiguration#getP2PLocalClassPathExclude()}</td>
 *     <td>Empty list.</td>
 *     <td>
 *          List of packages to exclude from local classpath when executing grid tasks.
 *          All classes within these packages will be loaded on demand from the
 *          node that initiated task execution.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@link GridConfiguration#getMetricsExpireTime()}</td>
 *     <td>{@link GridConfiguration#DFLT_METRICS_EXPIRE_TIME}</td>
 *     <td>
 *          Maximum time until node metrics from remote nodes are considered expired.
 *          Node metrics are frequently updated (usually with every heartbeat) and
 *          provide runtime information about remote nodes (see {@link GridNode#getMetrics()}).
 *     </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getMetricsHistorySize()}</td>
 *      <td>{@link GridConfiguration#DFLT_METRICS_HISTORY_SIZE}</td>
 *     <td>
 *          Maximum number of metrics to keep in the memory. The larger this number,
 *          the more precise the metrics calculations are (at the expense of more memory
 *          consumption). Node metrics are frequently updated (usually with every heartbeat)
 *          and provide runtime information about remote nodes (see {@link GridNode#getMetrics()}).
 *     </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getPeerClassLoadingTimeout()}</td>
 *      <td>{@link GridConfiguration#DFLT_PEER_CLASS_LOADING_TIMEOUT}</td>
 *     <td>
 *          Maximum timeout in milliseconds to wait for class-loading responses from
 *          remote nodes. After reaching this timeout {@link ClassNotFoundException}
 *          will be thrown.
 *     </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getLifecycleBeans()}</td>
 *      <td>
 *          The default is <tt>null</tt>.
 *      </td>
 *     <td>
 *          Collection of {@link GridLifecycleBean} beans. Use these beans whenever you need to
 *          plug some cusom logic before or after grid startup or stopping routines.
 *     </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getDiscoveryStartupDelay()}</td>
 *      <td>
 *          The default value is <tt>60,000ms</tt> specified by
 *          {@link GridConfiguration#DFLT_DISCOVERY_STARTUP_DELAY}. This should be good enough for
 *          vast majority of configurations. However, if you do anticipate an even larger
 *          delay, you should increase this value.
 *      </td>
 *     <td>
 *          This value is used to expire messages from waiting list whenever node
 *          discovery discrepancies happen. During startup, it is possible for some SPI's, such as
 *          <tt>GridMuleDiscoverySpi</tt> or <tt>GridJmsDiscoverySpi</tt>, to have a
 *          small time window when Node A has discovered Node B, but Node B
 *          has not discovered Node A yet. Such time window is usually very small,
 *          a matter of milliseconds, but certain JMS providers or some Mule
 *          messaging protocols may be very slow and hence have larger discovery
 *          delay window.
 *     </td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getDiscoverySpi() }</td>
 *      <td>Multicast-based grid discovery (see {@link GridMulticastDiscoverySpi}).</td>
 *      <td>Fully configured SPI used for discovering remote grid nodes.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getCommunicationSpi()}</td>
 *      <td>TCP-based communication (see {@link GridTcpCommunicationSpi}).</td>
 *      <td>Fully configured SPI used for communication between grid nodes.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getDeploymentSpi()}</td>
 *      <td>Local (in-memory) deployment SPI (see {@link GridLocalDeploymentSpi}).</td>
 *      <td>Fully configured SPI used for deploying grid tasks.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getCheckpointSpi()}</td>
 *      <td>
 *          Shared file system checkpoint SPI which (see {@link GridSharedFsCheckpointSpi}).
 *      </td>
 *      <td>Fully configured SPI used for storing intermediate job state to safeguard against failures.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getEventStorageSpi()}</td>
 *      <td>In-memory limited event storage (see {@link GridMemoryEventStorageSpi}).</td>
 *      <td>Fully configured SPI used for storing grid events.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getTopologySpi()}</td>
 *      <td>Basic topology spi that provides all discovered grid nodes (see {@link GridBasicTopologySpi}).</td>
 *      <td>Fully configured SPI used for determining grid task execution topology.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getFailoverSpi()}</td>
 *      <td>Failover SPI that always fails over jobs to another node (see {@link GridAlwaysFailoverSpi}).</td>
 *      <td>Fully configured SPI used for failing over jobs to another node in case of node failure.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getCollisionSpi()}</td>
 *      <td>Job collision SPI that executes all incoming jobs in FIFO order (see {@link GridFifoQueueCollisionSpi}).</td>
 *      <td>Fully configured SPI used for handling job collisions.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getMetricsSpi()}</td>
 *      <td>Basic JDK-based local VM Metrics implementation (see {@link GridJdkLocalMetricsSpi}).</td>
 *      <td>Fully configured SPI used for providing local VM metrics.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getLoadBalancingSpi()}</td>
 *      <td>
 *          Round-Robin implementation that will pick a random start node for every task and will
 *          iterate through all nodes starting at the start node sequentially for every grid job.
 *          (see {@link GridRoundRobinLoadBalancingSpi}).
 *      </td>
 *      <td>Fully configured SPI used for load balancing of jobs within grid.</td>
 *   </tr>
 *   <tr>
 *      <td>{@link GridConfiguration#getTracingSpi()}</td>
 *      <td>If not provided, then tracing will not be performed.</td>
 *      <td>Fully configured SPI used for tracing public grid method invocations.</td>
 *   </tr>
 * </table>
 * <h1 class="header">Multiple Grid Instances</h1>
 * Note that you can start multiple grid nodes within same VM by specifying
 * grid name (see {@link #start(GridConfiguration)}). This may be useful
 * for testing whenever it is needed to check internal job parameters when jobs
 * are running on different nodes. In most cases, however, you should use default
 * no-name grid.
 * <h1 class="header">Shutdown Hook</h1>
 * GridFactory sets shutdown hook (see {@link Runtime#addShutdownHook(Thread)}) to stop
 * all started grids when VM is exiting but it is recommended to stop all started grid
 * instances in your code where you started them.
 * Defining <tt>GRIDGAIN_NO_SHUTDOWN_HOOK</tt> system property or environment variable
 * and setting value to <tt>true</tt> will prevent GridFactory of setting up its own
 * shutdown hook and you can stop all grid instances in your own one.
 * <h1 class="header">Examples</h1>
 * Use {@link #start()} method to start grid with default configuration. You can also use
 * {@link GridConfigurationAdapter} to override some default configuration. Below is an
 * example on how to start grid with <strong>URI deployment</strong> and
 * <strong>JGroups-based discovery</strong> SPI's.
 * <pre name="code" class="java">
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * GridUriDeployment deploySpi = new GridUriDeployment();
 *
 * deploySpi.setUriList(Collections.singletonList("classes://tmp/ideoutput/classes"));
 *
 * GridJgroupsDiscovery discoSpi = new GridJgroupsDiscovery();
 *
 * discoSpi.setConfigurationFile("/config/jgroups/multicast/jgroups.xml");
 *
 * cfg.setDeploymentSpi(deploySpi);
 * cfg.setDiscoverySpi(discoSpi);
 *
 * GridFactroy.start(cfg);
 * </pre>
 * Here is how a grid instance can be configured from Spring XML configuration file. The
 * example below configures a grid instance with additional user attributes
 * (see {@link GridNode#getAttributes()}) and specifies a grid name:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.cfg" class="org.gridgain.grid.GridConfigurationAdapter" scope="singleton"&gt;
 *     ...
 *     &lt;property name="gridName" value="mygrid"/&gt;
 *     &lt;property name="userAttributes"&gt;
 *         &lt;map&gt;
 *             &lt;entry key="group" value="worker"/&gt;
 *             &lt;entry key="grid.node.benchmark"&gt;
 *                 &lt;bean class="org.gridgain.grid.benchmarks.GridLocalNodeBenchmark" init-method="start"/&gt;
 *             &lt;/entry&gt;
 *         &lt;/map&gt;
 *     &lt;/property&gt;
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * A grid instance with Spring configuration above can be started as following. Note that
 * you do not need to pass path to Spring XML file if you are using
 * <tt>GRIDGAIN_HOME/config/default-spring.xml</tt>. Also note, that the path can be
 * absolute or relative to GRIDGAIN_HOME.
 * <pre name="code" class="java">
 * GridFactory.start("/path/to/spring/xml/file.xml");
 * </pre>
 * You can also instantiate grid directly from Spring without using <tt>GridFactory</tt>.
 * For more information refer to {@link GridSpringBean} documentation.

 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridFactory {
    /** Default configuration path relative to GridGain home. */
    private static final String DFLT_CFG = "config/default-spring.xml";

    /** Gridgain Home environment or system variable name. */
    private static final String GRIDGAIN_HOME = "GRIDGAIN_HOME";

    /** Gridgain shutdown hook environment or system variable name. */
    private static final String GRIDGAIN_NO_SHUTDOWN_HOOK = "GRIDGAIN_NO_SHUTDOWN_HOOK";

    /** */
    private static final int P2P_THREADS = 20;

    /** */
    private static final int SYSTEM_THREADS = 5;

    /** Default grid. */
    private static GridNamedInstance dfltGrid = null;

    /** Map of named grids. */
    private static final Map<String, GridNamedInstance> grids = new HashMap<String, GridNamedInstance>();

    /** List of state listeners. */
    private static final List<GridFactoryListener> listeners = new ArrayList<GridFactoryListener>();

    /** Synchronization mutex. */
    private static final Object mux = new Object();

    /**
     * Checks runtime version to be 1.5.x or 1.6.x.
     * This will load pretty much first so we must do these checks here.
     */
    static {
        // Check 1.7 just in case for forward compatibility.
        if (GridOs.getJdkVersion().contains("1.5") == false &&
            GridOs.getJdkVersion().contains("1.6") == false &&
            GridOs.getJdkVersion().contains("1.7") == false ) {
            throw new IllegalStateException("GridGain requires Java 5 or above. Current Java version " +
                "is not supported: " + GridOs.getJdkVersion());
        }

        String ggHome = System.getProperty("GRIDGAIN_HOME");

        if (ggHome == null || ggHome.length() == 0) {
            ggHome = System.getenv("GRIDGAIN_HOME");

            if (ggHome != null && ggHome.length() > 0) {
                System.setProperty("GRIDGAIN_HOME", ggHome);
            }
        }
    }

    /**
     * Enforces singleton.
     */
    private GridFactory() {
        // No-op.
    }

    /**
     * Gets state of grid default grid.
     *
     * @return Default grid state.
     */
    public static GridFactoryState getState() {
        return getState(null);
    }

    /**
     * Gets states of named grid. If name is <tt>null</tt>, then state of
     * default no-name grid is returned.
     *
     * @param name Grid name. If name is <tt>null</tt>, then state of
     *      default no-name grid is returned.
     * @return Grid state.
     */
    public static GridFactoryState getState(String name) {
        GridNamedInstance grid = null;

        synchronized (mux) {
            grid = name == null ? dfltGrid : grids.get(name);
        }

        if (grid == null) {
            return GridFactoryState.STOPPED;
        }

        return grid.getState();
    }

    /**
     * Stops default grid. This method is identical to <tt>GridFactory.stop(null, cancel)</tt> call.
     * Note that method does not wait for all tasks to be completed.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      default grid will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution
     * @return <tt>true</tt> if default grid instance was indeed stopped,
     *      <tt>false</tt> otherwise (if it was not started).
     */
    public static boolean stop(boolean cancel) {
        return stop(null, cancel);
    }

    /**
     * Stops default grid. This method is identical to <tt>GridFactory.stop(null, cancel, wait)</tt> call.
     * If wait parameter is set to <tt>true</tt> then it will wait for all
     * tasks to be finished.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      default grid will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution.
     * @param wait If <tt>true</tt> then method will wait for all task being
     *      executed until they finish their execution.
     * @return <tt>true</tt> if default grid instance was indeed stopped,
     *      <tt>false</tt> otherwise (if it was not started).
     */
    public static boolean stop(boolean cancel, boolean wait) {
        return stop(null, cancel, wait);
    }

    /**
     * Stops named grid. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted. If
     * grid name is <tt>null</tt>, then default no-name grid will be stopped.
     * It does not wait for the tasks to finish their execution.
     *
     * @param name Grid name. If <tt>null</tt>, then default no-name grid will
     *      be stopped.
     * @param cancel If <tt>true</tt> then all jobs currently will be cancelled
     *      by calling {@link GridJob#cancel()} method. Note that just like with
     *      {@link Thread#interrupt()}, it is up to the actual job to exit from
     *      execution. If <tt>false</tt>, then jobs currently running will not be
     *      canceled. In either case, grid node will wait for completion of all
     *      jobs running on it before stopping.
     * @return <tt>true</tt> if named grid instance was indeed found and stopped,
     *      <tt>false</tt> otherwise (the instance with given <tt>name</tt> was
     *      not found).
     */
    public static boolean stop(String name, boolean cancel) {
        return stop(name, cancel, false);
    }

    /**
     * Stops named grid. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted. If
     * grid name is <tt>null</tt>, then default no-name grid will be stopped.
     * If wait parameter is set to <tt>true</tt> then grid will wait for all
     * tasks to be finished.
     *
     * @param name Grid name. If <tt>null</tt>, then default no-name grid will
     *      be stopped.
     * @param cancel If <tt>true</tt> then all jobs currently will be cancelled
     *      by calling {@link GridJob#cancel()} method. Note that just like with
     *      {@link Thread#interrupt()}, it is up to the actual job to exit from
     *      execution. If <tt>false</tt>, then jobs currently running will not be
     *      canceled. In either case, grid node will wait for completion of all
     *      jobs running on it before stopping.
     * @param wait If <tt>true</tt> then method will wait for all task being
     *      executed until they finish their execution.
     * @return <tt>true</tt> if named grid instance was indeed found and stopped,
     *      <tt>false</tt> otherwise (the instance with given <tt>name</tt> was
     *      not found).
     */
    public static boolean stop(String name, boolean cancel, boolean wait) {
        GridNamedInstance grid = null;

        synchronized (mux) {
            grid = name == null ? dfltGrid : grids.get(name);
        }

        if (grid != null) {
            grid.stop(cancel, wait);

            synchronized (mux) {
                if (name == null) {
                    dfltGrid = null;
                }
                else {
                    grids.remove(name);
                }
            }

            return true;
        }

        // We don't have log at this point...
        System.err.println("WARN: Ignoring attempt to stop grid instance that " +
            "was either already stopped or never started: " + name);

        return false;
    }

    /**
     * Stops <b>all</b> started grids. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted.
     * It does not wait for the tasks to finish their execution.
     * <p>
     * <b>Note:</b> it is usually safer and more appropriate to stop grid instances individually
     * instead of blanket operation. In most cases, the party that started the grid instance
     * should be responsible for stopping it.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      all grids will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution
     */
    public static void stopAll(boolean cancel) {
       stopAll(cancel, false);
    }

    /**
     * Stops <b>all</b> started grids. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted.
     * If wait parameter is set to <tt>true</tt> then grid will wait for all
     * tasks to be finished.
     * <p>
     * <b>Note:</b> it is usually safer and more appropriate to stop grid instances individually
     * instead of blanket operation. In most cases, the party that started the grid instance
     * should be responsible for stopping it.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      all grids will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution
     * @param wait If <tt>true</tt> then method will wait for all tasks being
     *      executed until they finish their execution.
     */
    public static void stopAll(boolean cancel, boolean wait) {
        System.err.println("WARN: Attempting to stop ALL grid instances. It is usually better to stop grid " +
            "instances individually instead of blanket stop.");

        List<GridNamedInstance> copy = new ArrayList<GridNamedInstance>();

        synchronized (mux) {
            if (dfltGrid != null) {
                copy.add(dfltGrid);
            }

            copy.addAll(grids.values());
        }

        // Stop the rest and clear grids map.
        for (GridNamedInstance grid: copy) {
            grid.stop(cancel, wait);
        }

        synchronized (mux) {
            dfltGrid = null;

            grids.clear();
        }
    }

    /**
     * Starts grid with default configuration. By default this method will
     * use grid configuration defined in <tt>GRIDGAIN_HOME/config/default-spring.xml</tt>
     * configuration file. If such file is not found, then all system defaults will be used.
     *
     * @return Started grid.
     * @throws GridException If default grid could not be started. This exception will be thrown
     *      also if default grid has already been started.
     */
    public static Grid start() throws GridException {
        return start((ApplicationContext)null);
    }

    /**
     * Starts grid with default configuration. By default this method will
     * use grid configuration defined in <tt>GRIDGAIN_HOME/config/default-spring.xml</tt>
     * configuration file. If such file is not found, then all system defaults will be used.
     *
     * @param springCtx Spring application context.
     * @return Started grid.
     * @throws GridException If default grid could not be started. This exception will be thrown
     *      also if default grid has already been started.
     */
    public static Grid start(ApplicationContext springCtx) throws GridException {
        File path = GridUtils.resolveGridGainPath(DFLT_CFG);

        if (path != null) {
            return start(DFLT_CFG);
        }

        System.err.println("WARN: Default Spring XML configuration file not found relative to GRIDGAIN_HOME, " +
            "will use system defaults (is your GRIDGAIN_HOME set?): " + DFLT_CFG);

        return start0(new GridConfigurationAdapter(), springCtx).getGrid();
    }

    /**
     * Starts grid with given configuration. Note that this method is no-op if grid with the name
     * provided in given configuration is already started.
     *
     * @param cfg Grid configuration. This cannot be <tt>null</tt>.
     * @return Started grid.
     * @throws GridException If grid could not be started. This exception will be thrown
     *      also if named grid has already been started.
     */
    public static Grid start(GridConfiguration cfg) throws GridException {
        return start(cfg, null);
    }

    /**
     * Starts grid with given configuration. Note that this method is no-op if grid with the name
     * provided in given configuration is already started.
     *
     * @param cfg Grid configuration. This cannot be <tt>null</tt>.
     * @param springCtx Spring application context, possibly <tt>null</tt>. If provided, this
     *      context can be injected into grid tasks and grid jobs using
     *      {@link GridSpringApplicationContextResource @GridSpringApplicationContextResource} annotation.
     * @return Started grid.
     * @throws GridException If grid could not be started. This exception will be thrown
     *      also if named grid has already been started.
     */
    public static Grid start(GridConfiguration cfg, ApplicationContext springCtx) throws GridException {
        GridArgumentCheck.checkNull(cfg, "cfg");

        return start0(cfg, springCtx).getGrid();
    }

    /**
     * Starts all grids specified within given Spring XML configuration file. If grid with given name
     * is already started, then exception is thrown. In this case all instances that may
     * have been started so far will be stopped too.
     * <p>
     * Usually Spring XML configuration file will contain only one Grid definition. Note that
     * Grid configuration bean(s) is retrieved form configuration file by type, so the name of
     * the Grid configuration bean is ignored.
     *
     * @param springCfgPath Spring XML configuration file path or URL. This cannot be <tt>null</tt>.
     * @return Started grid. If Spring configuration contains multiple grid instances,
     *      then the 1st found instance is returned.
     * @throws GridException If grid could not be started or configuration
     *      read. This exception will be thrown also if grid with given name has already
     *      been started or Spring XML configuration file is invalid.
     */
    public static Grid start(String springCfgPath) throws GridException {
        return start(springCfgPath, null);
    }

    /**
     * Starts all grids specified within given Spring XML configuration file. If grid with given name
     * is already started, then exception is thrown. In this case all instances that may
     * have been started so far will be stopped too.
     * <p>
     * Usually Spring XML configuration file will contain only one Grid definition. Note that
     * Grid configuration bean(s) is retrieved form configuration file by type, so the name of
     * the Grid configuration bean is ignored.
     *
     * @param springCfgPath Spring XML configuration file path or URL. This cannot be <tt>null</tt>.
     * @param ctx Spring application context.
     * @return Started grid. If Spring configuration contains multiple grid instances,
     *      then the 1st found instance is returned.
     * @throws GridException If grid could not be started or configuration
     *      read. This exception will be thrown also if grid with given name has already
     *      been started or Spring XML configuration file is invalid.
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    public static Grid start(String springCfgPath, ApplicationContext ctx) throws GridException {
        GridArgumentCheck.checkNull(springCfgPath, "springCfgPath");

        URL url = null;

        try {
            url = new URL(springCfgPath);
        }
        catch (MalformedURLException e) {
            // No-op.
        }

        if (url == null) {
            File path = GridUtils.resolveGridGainPath(springCfgPath);

            if (path == null) {
                throw (GridException)new GridException("Spring XML configuration file path is invalid: " + new File(springCfgPath) +
                    ". Note that this path should be either absolute or a relative local file system path " +
                    "or valid URL to GRIDGAIN_HOME.").setData(749, "src/java/org/gridgain/grid/GridFactory.java");
            }

            if (path.isFile() == false) {
                throw (GridException)new GridException("Provided file path is not a file: " + path).setData(755, "src/java/org/gridgain/grid/GridFactory.java");
            }

            try {
                url = path.toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw (GridException)new GridException("Failed to instantiate Spring XML application context: " + e.getMessage(), e).setData(762, "src/java/org/gridgain/grid/GridFactory.java");
            }
        }

        return start(url, ctx);
    }

    /**
     * Starts all grids specified within given Spring XML configuration file URL. If grid with given name
     * is already started, then exception is thrown. In this case all instances that may
     * have been started so far will be stopped too.
     * <p>
     * Usually Spring XML configuration file will contain only one Grid definition. Note that
     * Grid configuration bean(s) is retrieved form configuration file by type, so the name of
     * the Grid configuration bean is ignored.
     *
     * @param springCfgUrl Spring XML configuration file URL. This cannot be <tt>null</tt>.
     * @return Started grid. If Spring configuration contains multiple grid instances,
     *      then the 1st found instance is returned.
     * @throws GridException If grid could not be started or configuration
     *      read. This exception will be thrown also if grid with given name has already
     *      been started or Spring XML configuration file is invalid.
     */
    // Warning is due to Spring.
    @SuppressWarnings("unchecked")
    public static Grid start(URL springCfgUrl) throws GridException {
        return start(springCfgUrl, null);
    }

    /**
     * Starts all grids specified within given Spring XML configuration file URL. If grid with given name
     * is already started, then exception is thrown. In this case all instances that may
     * have been started so far will be stopped too.
     * <p>
     * Usually Spring XML configuration file will contain only one Grid definition. Note that
     * Grid configuration bean(s) is retrieved form configuration file by type, so the name of
     * the Grid configuration bean is ignored.
     *
     * @param springCfgUrl Spring XML configuration file URL. This cannot be <tt>null</tt>.
     * @param ctx Spring application context.
     * @return Started grid. If Spring configuration contains multiple grid instances,
     *      then the 1st found instance is returned.
     * @throws GridException If grid could not be started or configuration
     *      read. This exception will be thrown also if grid with given name has already
     *      been started or Spring XML configuration file is invalid.
     */
    // Warning is due to Spring.
    @SuppressWarnings("unchecked")
    public static Grid start(URL springCfgUrl, ApplicationContext ctx) throws GridException {
        GridArgumentCheck.checkNull(springCfgUrl, "springCfgUrl");

        // Add no-op logger to remove no-appender warning.
        Appender app = new NullAppender();

        Logger.getRootLogger().addAppender(app);

        ApplicationContext springCtx = null;

        try {
            springCtx = new FileSystemXmlApplicationContext(springCfgUrl.toString());
        }
        catch (BeansException e) {
            throw (GridException)new GridException("Failed to instantiate Spring XML application context: " + e.getMessage(), e).setData(824, "src/java/org/gridgain/grid/GridFactory.java");
        }

        Map cfgMap = null;

        try {
            // Note: Spring is not generics-friendly.
            cfgMap = springCtx.getBeansOfType(GridConfiguration.class);
        }
        catch (BeansException e) {
            throw (GridException)new GridException("Failed to instantiate bean [type=" + GridConfiguration.class + ", error=" +
                e.getMessage() + ']', e).setData(834, "src/java/org/gridgain/grid/GridFactory.java");
        }

        if (cfgMap == null) {
            throw (GridException)new GridException("Failed to find a single grid factory configuration in: " + springCfgUrl).setData(839, "src/java/org/gridgain/grid/GridFactory.java");
        }

        // Remove previously added no-op logger.
        Logger.getRootLogger().removeAppender(app);

        if (cfgMap.size() == 0) {
            throw (GridException)new GridException("Can't find grid factory configuration in: " + springCfgUrl).setData(846, "src/java/org/gridgain/grid/GridFactory.java");
        }

        List<GridNamedInstance> grids = new ArrayList<GridNamedInstance>(cfgMap.size());

        try {
            for (GridConfiguration cfg : (Collection<GridConfiguration>)cfgMap.values()) {
                assert cfg != null : "ASSERTION [line=853, file=src/java/org/gridgain/grid/GridFactory.java]";

                // Use either user defined context or our one.
                GridNamedInstance grid = start0(cfg, ctx == null ? springCtx: ctx);

                // Add it if it was not stopped during startup.
                if (grid != null) {
                    grids.add(grid);
                }
            }
        }
        catch (GridException e) {
            // Stop all instances started so far.
            for (GridNamedInstance grid : grids) {
                //noinspection CatchGenericClass
                try {
                    grid.stop(true, false);
                }
                catch (Exception e1) {
                    grid.log.error("Error when stopping grid: " + grid, e1);
                }
            }

            throw e;
        }

        // Return the first grid started.
        GridNamedInstance res = grids.size() > 0 ? grids.get(0) : null;

        return res != null ? res.getGrid() : null;
    }

    /**
     * Starts grid with given configuration.
     *
     * @param cfg Grid configuration.
     * @param springCtx Spring application context.
     * @return Started grid.
     * @throws GridException If grid could not be started.
     */
    private static GridNamedInstance start0(GridConfiguration cfg, ApplicationContext springCtx)
        throws GridException {
        assert cfg != null : "ASSERTION [line=895, file=src/java/org/gridgain/grid/GridFactory.java]";

        String name = cfg.getGridName();

        GridNamedInstance grid = null;

        synchronized (mux) {
            if (name == null) {
                // If default grid is started - throw exception.
                if (dfltGrid != null) {
                    throw (GridException)new GridException("Default grid instance has already been started.").setData(905, "src/java/org/gridgain/grid/GridFactory.java");
                }

                grid = dfltGrid = new GridNamedInstance(null);
            }
            else {
                if (name.length() == 0) {
                    throw (GridException)new GridException("Non default grid instances cannot have empty string name.").setData(912, "src/java/org/gridgain/grid/GridFactory.java");
                }

                if (grids.containsKey(name) == true) {
                    throw (GridException)new GridException("Grid instance with this name has already been started: " + name).setData(916, "src/java/org/gridgain/grid/GridFactory.java");
                }

                grids.put(name, grid = new GridNamedInstance(name));
            }
        }

        try {
            grid.start(cfg, (grids.size() == 1 && dfltGrid == null) || (grids.size() == 0 && dfltGrid != null),
                springCtx);
        }
        finally {
            if (grid.getState() != GridFactoryState.STARTED) {
                grids.remove(name);

                grid = null;

                if (name == null) {
                    dfltGrid = null;
                }
            }
        }

        return grid;
    }

    /**
     * Gets an instance of default no-name grid. Note that
     * caller of this method should not assume that it will return the same
     * instance every time.
     * <p>
     * This method is identical to <tt>GridFactory.getGrid(null)</tt> call.
     *
     * @return An instance of default no-name grid. This method never returns
     *      <tt>null</tt>.
     * @throws IllegalStateException Thrown if default grid was not properly
     *      initialized or grid instance was stopped or was not started.
     */
    public static Grid getGrid() throws IllegalStateException {
        return getGrid((String)null);
    }

    /**
     * Gets a list of all grids started so far.
     *
     * @return List of all grids started so far.
     */
    public static List<Grid> getAllGrids() {
        synchronized (mux) {
            List<Grid> allGrids = new ArrayList<Grid>(grids.size() + (dfltGrid == null ? 0 : 1));

            if (dfltGrid != null) {
                allGrids.add(dfltGrid.getGrid());
            }

            for (GridNamedInstance grid : grids.values()) {
                allGrids.add(grid.getGrid());
            }

            return allGrids;
        }
    }
    
    /**
     * Gets a grid instance for given local node ID. Note that grid instance and local node have
     * one-to-one relationship where node has ID and instance has name of the grid to which 
     * both grid instance and its node belong. Note also that caller of this method
     * should not assume that it will return the same instance every time.
     *
     * @param localNodeId ID of local node the requested grid instance is managing.
     * @return An instance of named grid. This method never returns
     *      <tt>null</tt>.
     * @throws IllegalStateException Thrown if grid was not properly
     *      initialized or grid instance was stopped or was not started.
     */
    public static Grid getGrid(UUID localNodeId) throws IllegalStateException {
        GridArgumentCheck.checkNull(localNodeId, "localNodeId");
     
        synchronized (mux) {
            for (GridNamedInstance grid : grids.values()) {
                if (grid.getGrid().getLocalNodeId().equals(localNodeId) == true) {
                    return grid.getGrid();
                }
            }
        }
        
        throw new IllegalStateException("Grid instance with given local node ID was not properly " +
            "started or was stopped: " + localNodeId);
    }

    /**
     * Gets an named grid instance. If grid name is <tt>null</tt> or empty string,
     * then default no-name grid will be returned. Note that caller of this method
     * should not assume that it will return the same instance every time.
     * <p>
     * Note that Java VM can run multiple grid instances and every grid instance (and its
     * node) can belong to a different grid. Grid name defines what grid a particular grid 
     * instance (and correspondently its node) belongs to.
     *
     * @param name Grid name to which requested grid instance belongs to. If <tt>null</tt>, 
     *      then grid instanced belonging to a default no-name grid will be returned.
     * @return An instance of named grid. This method never returns
     *      <tt>null</tt>.
     * @throws IllegalStateException Thrown if default grid was not properly
     *      initialized or grid instance was stopped or was not started.
     */
    public static Grid getGrid(String name) throws IllegalStateException {
        GridNamedInstance grid = null;

        synchronized (mux) {
            grid = name == null ? dfltGrid : grids.get(name);
        }

        if (grid == null) {
            throw new IllegalStateException("Grid instance was not properly started or was stopped: " + name);
        }

        return grid.getGrid();
    }

    /**
     * Adds a listener for grid life cycle events.
     *
     * @param listener Listener for grid life cycle events.
     */
    public static void addListener(GridFactoryListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        synchronized (listeners) {
            if (listeners.contains(listener) == false) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Removes listener added by {@link #addListener(GridFactoryListener)} method.
     *
     * @param listener Listener to remove.
     * @return <tt>true</tt> if listener was added before, <tt>false</tt> otherwise.
     */
    public static boolean removeListener(GridFactoryListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        synchronized (listeners) {
            return listeners.remove(listener);
        }
    }

    /**
     *
     * @param gridName Grid instance name.
     * @param state Factory state.
     */
    private static void notifyStateChange(String gridName, GridFactoryState state) {
        Collection<GridFactoryListener> localCopy = null;

        synchronized (listeners) {
            localCopy = new ArrayList<GridFactoryListener>(listeners);
        }

        for (GridFactoryListener listener : localCopy) {
            listener.onStateChange(gridName, state);
        }
    }

    /**
     * Grid data container.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private static final class GridNamedInstance {
        /** GridFactory MBean registered in selected MBeanServer. */
        private static final Map<MBeanServer, GridMBeanServerData> mbeans =
            new HashMap<MBeanServer, GridMBeanServerData>();

        /** Grid name. */
        private final String name;

        /** Grid instance. */
        private GridKernal grid = null;

        /** Executor service. */
        private ExecutorService execSvc = null;

        /** System executor service. */
        private ExecutorService sysExecSvc = null;

        /** P2P executor service. */
        private ExecutorService p2pExecSvc = null;

        /** Grid state. */
        private GridFactoryState state = GridFactoryState.STOPPED;

        /** Shutdown hook. */
        private Thread shutdownHook = null;

        /** Grid log. */
        private GridLogger log = null;

        /**
         * Creates un-started named instance.
         *
         * @param name Grid name (possibly <tt>null</tt> for default grid).
         */
        GridNamedInstance(String name) {
            this.name = name;
        }

        /**
         * Gets grid name.
         *
         * @return Grid name.
         */
        String getName() {
            return name;
        }

        /**
         * Gets grid instance.
         *
         * @return Grid instance.
         */
        synchronized GridKernal getGrid() {
            return grid;
        }

        /**
         * Gets grid state.
         *
         * @return Grid state.
         */
        synchronized GridFactoryState getState() {
            return state;
        }

        /**
         *
         * @param spi SPI implementation.
         * @throws GridException Thrown in case if multi-instance is not supported.
         */
        private void ensureMultiInstanceSupport(GridSpi spi) throws GridException {
            GridSpiMultipleInstancesSupport ann = GridUtils.getAnnotation(spi.getClass(),
                GridSpiMultipleInstancesSupport.class);

            if (ann == null || ann.value() == false) {
                throw (GridException)new GridException("SPI implementation doesn't support multiple grid instances in " +
                    "the same VM: " + spi).setData(1163, "src/java/org/gridgain/grid/GridFactory.java");
            }
        }

        /**
         *
         * @param spis SPI implementations.
         * @throws GridException Thrown in case if multi-instance is not supported.
         */
        private void ensureMultiInstanceSupport(GridSpi[] spis) throws GridException {
            for (GridSpi spi : spis) {
                ensureMultiInstanceSupport(spi);
            }
        }

        /**
         * Starts grid with given configuration.
         *
         * @param cfg Grid configuration (possibly <tt>null</tt>).
         * @param single Whether or not this is a single grid instance in current VM.
         * @param springCtx Spring application context.
         * @throws GridException If start failed.
         */
        synchronized void start(GridConfiguration cfg, boolean single, ApplicationContext springCtx)
            throws GridException {
            if (grid != null) {
                // No-op if grid is already started.
                return;
            }

            if (cfg == null) {
                cfg = new GridConfigurationAdapter();
            }

            GridConfigurationAdapter myCfg = new GridConfigurationAdapter();

            /*
             * Set up all defaults and perform all checks.
             */

            // Ensure invariant.
            // It's a bit dirty - but this is a result of late refactoring
            // and I don't want to reshuffle a lot of code.
            assert GridUtils.equalsWithNulls(name, cfg.getGridName()) : "ASSERTION [line=1207, file=src/java/org/gridgain/grid/GridFactory.java]";

            myCfg.setGridName(cfg.getGridName());

            String ggHome = cfg.getGridGainHome();

            // Set GridGain home.
            if (ggHome == null) {
                ggHome = System.getProperty(GRIDGAIN_HOME);

                if (ggHome == null) {
                    ggHome = System.getenv(GRIDGAIN_HOME);
                }

                if (ggHome == null) {
                    throw (GridException)new GridException("Failed to detect GridGain installation home. It was neither provided in " +
                        "GridConfiguration nor it could be detected from " + GRIDGAIN_HOME +
                        " system property or environmental variable.").setData(1222, "src/java/org/gridgain/grid/GridFactory.java");
                }
            }

            File ggHomeFile = new File(ggHome);

            if (ggHomeFile.exists() == false || ggHomeFile.isDirectory() == false) {
                throw (GridException)new GridException("Invalid GridGain installation home folder: " + ggHome).setData(1231, "src/java/org/gridgain/grid/GridFactory.java");
            }

            myCfg.setGridGainHome(ggHome);

            // Copy values that don't need extra processing.
            myCfg.setPeerClassLoadingEnabled(cfg.isPeerClassLoadingEnabled());
            myCfg.setDeploymentMode(cfg.getDeploymentMode());
            myCfg.setPeerClassLoadingTimeout(cfg.getPeerClassLoadingTimeout());
            myCfg.setDiscoveryStartupDelay(cfg.getDiscoveryStartupDelay());
            myCfg.setMetricsHistorySize(cfg.getMetricsHistorySize());
            myCfg.setMetricsExpireTime(cfg.getMetricsExpireTime());
            myCfg.setLifecycleBeans(cfg.getLifecycleBeans());
            myCfg.setPeerClassLoadingMissedResourcesCacheSize(cfg.getPeerClassLoadingMissedResourcesCacheSize());

            Map<String, ? extends Serializable> attrs = cfg.getUserAttributes();

            if (attrs == null) {
                attrs = Collections.emptyMap();
            }

            MBeanServer mbeanServer = cfg.getMBeanServer();
            UUID nodeId = cfg.getNodeId();
            GridMarshaller marshaller = cfg.getMarshaller();
            List<String> p2pExclude = cfg.getP2PLocalClassPathExclude();

            GridCommunicationSpi commSpi = cfg.getCommunicationSpi();
            GridDiscoverySpi discoSpi = cfg.getDiscoverySpi();
            GridEventStorageSpi evtSpi = cfg.getEventStorageSpi();
            GridCollisionSpi colSpi = cfg.getCollisionSpi();
            GridLocalMetricsSpi metricsSpi = cfg.getMetricsSpi();
            GridDeploymentSpi deploySpi = cfg.getDeploymentSpi();

            GridCheckpointSpi[] cpSpi = cfg.getCheckpointSpi();
            GridTopologySpi[] topSpi = cfg.getTopologySpi();
            GridFailoverSpi[] failSpi = cfg.getFailoverSpi();
            GridLoadBalancingSpi[] loadBalancingSpi = cfg.getLoadBalancingSpi();
            GridTracingSpi[] traceSpi = cfg.getTracingSpi();

            GridLogger cfgLog = cfg.getGridLogger();

            if (cfgLog == null) {
                File path = GridUtils.resolveGridGainPath("config/default-log4j.xml");

                if (path == null || GridLog4jLogger.isConfigured() == true) {
                    // Default constructor will automatically detect if root logger
                    // is already configured or not.
                    cfgLog = new GridLog4jLogger();
                }
                else {
                    cfgLog = new GridLog4jLogger(path);
                }
            }

            // Initialize factory's log.
            log = cfgLog.getLogger(GridFactory.class);

            GridProxyFactory proxyFactory = new GridProxyFactory(cfgLog);

            execSvc = cfg.getExecutorService();
            sysExecSvc = cfg.getSystemExecutorService();
            p2pExecSvc = cfg.getPeerClassLoadingExecutorService();

            if (execSvc == null) {
                execSvc = new GridThreadPoolExecutorService(cfg.getGridName());

                // Prestart all threads as they are guaranteed to be needed.
                ((ThreadPoolExecutor)execSvc).prestartAllCoreThreads();
            }

            if (sysExecSvc == null) {
                // Note that since we use {@link LinkedBlockingQueue}, number of
                // maximum threads has no effect.
                // Note, that we do not prestart threads here as system pool may
                // not be needed.
                sysExecSvc = new GridThreadPoolExecutorService(cfg.getGridName(), SYSTEM_THREADS,
                    SYSTEM_THREADS, 0, new LinkedBlockingQueue<Runnable>());
            }

            if (p2pExecSvc == null) {
                // Note that since we use {@link LinkedBlockingQueue}, number of
                // maximum threads has no effect.
                // Note, that we do not prestart threads here as class loading pool may
                // not be needed.
                p2pExecSvc = new GridThreadPoolExecutorService(cfg.getGridName(), P2P_THREADS,
                    P2P_THREADS, 0, new LinkedBlockingQueue<Runnable>());
            }

            if (traceSpi != null) {
                // Make existing object proxy.
                execSvc = proxyFactory.getProxy(execSvc);
                sysExecSvc = proxyFactory.getProxy(sysExecSvc);
                p2pExecSvc = proxyFactory.getProxy(p2pExecSvc);
            }

            if (marshaller == null) {
                marshaller = new GridJBossMarshaller();
            }

            myCfg.setUserAttributes(attrs);
            myCfg.setMBeanServer(mbeanServer == null ? ManagementFactory.getPlatformMBeanServer() : mbeanServer);
            myCfg.setGridLogger(cfgLog);
            myCfg.setMarshaller(marshaller);
            myCfg.setExecutorService(execSvc);
            myCfg.setSystemExecutorService(sysExecSvc);
            myCfg.setPeerClassLoadingExecutorService(p2pExecSvc);
            myCfg.setNodeId(nodeId == null ? UUID.randomUUID() : nodeId);

            if (p2pExclude == null) {
                p2pExclude = Collections.emptyList();
            }

            myCfg.setP2PLocalClassPathExclude(p2pExclude);

            /*
             * Initialize default SPI implementations.
             * =======================================
             */

            if (commSpi == null) {
                commSpi = new GridTcpCommunicationSpi();
            }

            if (discoSpi == null) {
                discoSpi = new GridMulticastDiscoverySpi();
            }

            if (evtSpi == null) {
                evtSpi = new GridMemoryEventStorageSpi();
            }

            if (colSpi == null) {
                colSpi = new GridFifoQueueCollisionSpi();
            }

            if (metricsSpi == null) {
                metricsSpi = new GridJdkLocalMetricsSpi();
            }

            if (deploySpi == null) {
                deploySpi = new GridLocalDeploymentSpi();
            }

            if (cpSpi == null) {
                cpSpi = new GridCheckpointSpi[] {new GridSharedFsCheckpointSpi()};
            }

            if (topSpi == null) {
                topSpi = new GridTopologySpi[] {new GridBasicTopologySpi()};
            }

            if (failSpi == null) {
                failSpi = new GridFailoverSpi[] {new GridAlwaysFailoverSpi()};
            }

            if (loadBalancingSpi == null) {
                loadBalancingSpi = new GridLoadBalancingSpi[] {new GridRoundRobinLoadBalancingSpi()};
            }

            // Wrap SPIs and Grid instance with proxies if interception is enabled.
            if (traceSpi != null) {
                commSpi = wrapSpi(proxyFactory, commSpi);
                discoSpi = wrapSpi(proxyFactory, discoSpi);
                colSpi = wrapSpi(proxyFactory, colSpi);
                metricsSpi = wrapSpi(proxyFactory, metricsSpi);
                evtSpi = wrapSpi(proxyFactory, evtSpi);
                deploySpi = wrapSpi(proxyFactory, deploySpi);

                cpSpi = wrapSpis(proxyFactory, cpSpi);
                topSpi = wrapSpis(proxyFactory, topSpi);
                failSpi = wrapSpis(proxyFactory, failSpi);
                loadBalancingSpi = wrapSpis(proxyFactory, loadBalancingSpi);
            }

            myCfg.setCommunicationSpi(commSpi);
            myCfg.setDiscoverySpi(discoSpi);
            myCfg.setCheckpointSpi(cpSpi);
            myCfg.setEventStorageSpi(evtSpi);
            myCfg.setMetricsSpi(metricsSpi);
            myCfg.setDeploymentSpi(deploySpi);
            myCfg.setTopologySpi(topSpi);
            myCfg.setFailoverSpi(failSpi);
            myCfg.setCollisionSpi(colSpi);
            myCfg.setLoadBalancingSpi(loadBalancingSpi);

            // Don't trace the tracing SPI.
            if (traceSpi != null) {
                myCfg.setTracingSpi(traceSpi);
            }

            // Ensure that SPIs support multiple grid instances, if required.
            if (single == false) {
                ensureMultiInstanceSupport(deploySpi);
                ensureMultiInstanceSupport(commSpi);
                ensureMultiInstanceSupport(discoSpi);
                ensureMultiInstanceSupport(cpSpi);
                ensureMultiInstanceSupport(evtSpi);
                ensureMultiInstanceSupport(topSpi);
                ensureMultiInstanceSupport(colSpi);
                ensureMultiInstanceSupport(failSpi);
                ensureMultiInstanceSupport(metricsSpi);
                ensureMultiInstanceSupport(loadBalancingSpi);

                if (traceSpi != null) {
                    ensureMultiInstanceSupport(traceSpi);
                }
            }

            grid = new GridKernal(proxyFactory, springCtx);

            if (traceSpi != null) {
                grid = proxyFactory.getProxy(
                    grid,
                    proxyFactory,
                    GridProxyFactory.class,
                    springCtx,
                    ApplicationContext.class);
            }

            // Register GridFactory MBean for current grid instance.
            //noinspection CatchGenericClass
            try {
                registerFactoryMbean(myCfg.getMBeanServer());
            }
            catch (GridException e) {
                grid = null;

                stopExecutor(log);

                throw e;
            }
            // Catch Throwable to protect against any possible failure.
            catch (Throwable e) {
                grid = null;

                stopExecutor(log);

                throw (GridException)new GridException("Unexpected exception when starting grid.", e).setData(1468, "src/java/org/gridgain/grid/GridFactory.java");
            }

            //noinspection CatchGenericClass
            try {
                grid.start(myCfg);

                state = GridFactoryState.STARTED;

                if (log.isDebugEnabled() == true) {
                    log.debug("Grid factory started ok.");
                }
            }
            catch (GridException e) {
                grid = null;

                stopExecutor(log);

                unregisterFactoryMBean();

                throw e;
            }
            // Catch Throwable to protect against any possible failure.
            catch (Throwable e) {
                grid = null;

                stopExecutor(log);

                unregisterFactoryMBean();

                throw (GridException)new GridException("Unexpected exception when starting grid.", e).setData(1498, "src/java/org/gridgain/grid/GridFactory.java");
            }

            String ggNoHook = System.getProperty(GRIDGAIN_NO_SHUTDOWN_HOOK);

            if (ggNoHook == null || ggNoHook.length() == 0) {
                ggNoHook = System.getenv(GRIDGAIN_NO_SHUTDOWN_HOOK);
            }

            // Do not set it up only if GRIDGAIN_NO_SHUTDOWN_HOOK=TRUE is provided.
            if (ggNoHook == null || "TRUE".equalsIgnoreCase(ggNoHook) == false) {
                //noinspection ClassExplicitlyExtendsThread
                Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void run() {
                        if (log.isInfoEnabled() == true) {
                            log.info("Invoking shutdown hook...");
                        }

                        GridNamedInstance.this.stop(true, false);
                    }
                });

                if (log.isDebugEnabled() == true) {
                    log.debug("Shutdown hook is installed.");
                }
            }
            else {
                if (log.isDebugEnabled() == true) {
                    log.debug("Shutdown hook has not been installed bacause environment " +
                        "or system property " + GRIDGAIN_NO_SHUTDOWN_HOOK + " is set.");
                }
            }

            notifyStateChange(name, GridFactoryState.STARTED);
        }


        /**
         *
         * @param <T> SPI type.
         * @param spi SPI to wrap.
         * @param proxy Proxy.
         * @return Wrapped SPI instance.
         * @throws GridException If wrapping failed.
         */
        private <T extends GridSpi> T wrapSpi(GridProxyFactory proxy, T spi) throws GridException {
            return proxy.getProxy(spi);
        }

        /**
         *
         * @param <T> SPI type.
         * @param spis SPIs to wrap.
         * @param proxy Proxy.
         * @return Wrapped SPI instance.
         * @throws GridException If wrapping failed.
         */
        private <T extends GridSpi> T[] wrapSpis(GridProxyFactory proxy, T[] spis) throws GridException {
            T[] copy = spis.clone();

            for (int i = 0; i < spis.length; i++) {
                copy[i] = wrapSpi(proxy, spis[i]);
            }

            return copy;
        }

        /**
         * Stops grid.
         *
         * @param interrupt Flag indicating whether all currently running jobs
         *      should be interrupted.
         * @param wait If <tt>true</tt> then method will wait for all task being
         *      executed until they finish their execution.
         */
        synchronized void stop(boolean interrupt, boolean wait) {
            if (grid == null) {
                if (log != null) {
                    log.warning("Attempting to stop an already stopped grid instance (will ignore): " + name);
                }

                return;
            }

            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);

                    shutdownHook = null;

                    if (log.isDebugEnabled() == true) {
                        log.debug("Shutdown hook is removed.");
                    }
                }
                catch (IllegalStateException e) {
                    // Shutdown is in progress...
                    if (log.isDebugEnabled() == true) {
                        log.debug("Shutdown is in progress (ignoring): " + e.getMessage());
                    }
                }
            }

            // Unregister GridFactory MBean.
            unregisterFactoryMBean();

            //noinspection CatchGenericClass
            try {
                grid.stop(interrupt, wait);

                if (log.isDebugEnabled() == true) {
                    log.debug("Grid instance stopped ok: " + name);
                }
            }
            catch (Throwable e) {
                log.error("Failed to properly stop grid instance due to undeclared exception.", e);
            }
            finally {
                grid = null;

                state = GridFactoryState.STOPPED;

                stopExecutor(log);

                notifyStateChange(name, GridFactoryState.STOPPED);

                log = null;
            }
        }

        /**
         * Stops executor service if it has been started.
         *
         * @param log Grid logger.
         */
        private void stopExecutor(GridLogger log) {
            assert log != null : "ASSERTION [line=1637, file=src/java/org/gridgain/grid/GridFactory.java]";
            assert execSvc != null : "ASSERTION [line=1638, file=src/java/org/gridgain/grid/GridFactory.java]";
            assert sysExecSvc != null : "ASSERTION [line=1639, file=src/java/org/gridgain/grid/GridFactory.java]";
            assert p2pExecSvc != null : "ASSERTION [line=1640, file=src/java/org/gridgain/grid/GridFactory.java]";

            /*
             * 1.
             * Attempt to stop all still active grid runnables.
             * Note that these runnable should have been stopped
             * by the kernal. We are trying to be defensive here
             * so the logic is repetitive with kernal.
             */
            for (GridRunnable r : GridRunnableGroup.getInstance(name).getActiveSet()) {
                String n1 = r.getGridName() == null ? "" : r.getGridName();
                String n2 = name == null ? "" : name;

                /*
                 * We should never get a runnable from one grid instance
                 * in the runnable group for another grid instance.
                 */
                assert n1.equals(n2) == true : "ASSERTION [line=1657, file=src/java/org/gridgain/grid/GridFactory.java]";

                log.warning("Runnable job outlived grid: " + r);

                GridUtils.cancel(r);
                GridUtils.join(r, log);
            }

            // Release memory.
            GridRunnableGroup.removeInstance(name);

            /*
             * 2.
             * If it was us who started the executor services than we
             * stop it. Otherwise, we do no-op since executor service
             * was started before us.
             */
            if (execSvc instanceof GridThreadPoolExecutorService == true) {
                GridUtils.shutdownNow(getClass(), execSvc, log);

                execSvc = null;
            }

            if (sysExecSvc instanceof GridThreadPoolExecutorService == true) {
                GridUtils.shutdownNow(getClass(), sysExecSvc, log);

                sysExecSvc = null;
            }

            if (p2pExecSvc instanceof GridThreadPoolExecutorService == true) {
                GridUtils.shutdownNow(getClass(), p2pExecSvc, log);

                p2pExecSvc = null;
            }
        }

        /**
         * Registers delegate Mbean instance for {@link GridFactory}.
         *
         * @param server MBeanServer where mbean should be registered.
         * @throws GridException If registration failed.
         */
        private void registerFactoryMbean(MBeanServer server) throws GridException {
            synchronized (mbeans) {
                GridMBeanServerData data = mbeans.get(server);

                if (data == null) {
                    try {
                        GridFactoryMBean mbean = new GridFactoryMBeanAdapter();

                        ObjectName objName = GridUtils.makeMBeanName(
                            null,
                            "Kernal",
                            GridFactory.class.getSimpleName()
                        );

                        // Make check if MBean was already registered.
                        if (server.queryMBeans(objName, null).isEmpty() == false) {
                            log.warning("MBean was already registered: " + objName);
                        }
                        else {
                            objName = GridUtils.registerMBean(
                                server,
                                null,
                                "Kernal",
                                GridFactory.class.getSimpleName(),
                                mbean,
                                GridFactoryMBean.class
                            );

                            data = new GridMBeanServerData(objName);

                            mbeans.put(server, data);

                            if (log.isDebugEnabled() == true) {
                                log.debug("Registered MBean: " + objName);
                            }
                        }
                    }
                    catch (JMException e) {
                        throw (GridException)new GridException("Failed to register MBean.", e).setData(1737, "src/java/org/gridgain/grid/GridFactory.java");
                    }
                }

                if (data != null) {
                    data.addGrid(name);
                    data.setCounter(data.getCounter() + 1);
                }
            }
        }

        /**
         * Unregister delegate Mbean instance for {@link GridFactory}.
         */
        public void unregisterFactoryMBean() {
            synchronized(mbeans) {
                Iterator<Entry<MBeanServer, GridMBeanServerData>> iter = mbeans.entrySet().iterator();

                while (iter.hasNext() == true) {
                    Entry<MBeanServer, GridMBeanServerData> entry = iter.next();

                    if (entry.getValue().containsGrid(name) == true) {
                        GridMBeanServerData data = entry.getValue();

                        assert data != null : "ASSERTION [line=1761, file=src/java/org/gridgain/grid/GridFactory.java]";

                        // Unregister MBean if no grid instances started for current MBeanServer.
                        if (data.getCounter() == 1) {
                            try {
                                entry.getKey().unregisterMBean(data.getMbean());

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Unregistered MBean: " + data.getMbean());
                                }
                            }
                            catch (JMException e) {
                                log.error("Failed to unregister MBean.", e);
                            }

                            iter.remove();
                        }
                        else {
                            // Decrement counter.
                            data.setCounter(data.getCounter() - 1);
                            data.removeGrid(name);
                        }
                    }
                }
            }
        }

        /**
         * Grid factory MBean data container.
         * Contains necessary data for selected MBeanServer.
         *
         * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
         * @version 2.1.1
         */
        private static class GridMBeanServerData {
            /** Set of grid names for selected MBeanServer. */
            private Set<String> gridNames = new HashSet<String>();

            /** */
            private ObjectName mbean = null;

            /** Count of grid instances. */
            private int counter = 0;

            /**
             * Create data container.
             *
             * @param mbean Object name of MBean.
             */
            GridMBeanServerData(ObjectName mbean) {
                assert mbean != null : "ASSERTION [line=1811, file=src/java/org/gridgain/grid/GridFactory.java]";

                this.mbean = mbean;
            }

            /**
             * Add grid name.
             *
             * @param gridName Grid name.
             */
            public void addGrid(String gridName) {
                gridNames.add(gridName);
            }

            /**
             * Remove grid name.
             *
             * @param gridName Grid name.
             */
            public void removeGrid(String gridName) {
                gridNames.remove(gridName);
            }

            /**
             * Returns <tt>true</tt> if data contains the specified
             * grid name.
             *
             * @param gridName Grid name.
             * @return FIXDOC.
             */
            public boolean containsGrid(String gridName) {
                return gridNames.contains(gridName);
            }

            /**
             * Gets name used in MBean server.
             *
             * @return Object name of MBean.
             */
            public ObjectName getMbean() {
                return mbean;
            }

            /**
             * Gets number of grid instances working with MBeanServer.
             *
             * @return Number of grid instances.
             */
            public int getCounter() {
                return counter;
            }

            /**
             * Sets number of grid instances working with MBeanServer.
             *
             * @param counter Number of grid instances.
             */
            public void setCounter(int counter) {
                this.counter = counter;
            }
        }
    }
}
