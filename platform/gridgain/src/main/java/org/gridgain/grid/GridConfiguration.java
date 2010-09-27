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
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;
import org.gridgain.apache.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.eventstorage.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.spi.metrics.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.grid.spi.tracing.*;

/**
 * This interface defines grid runtime configuration. This configuration is passed to
 * {@link GridFactory#start(GridConfiguration)} method. It defines all configuration
 * parameters required to start a grid instance. Usually, a special
 * class called "loader" will create an instance of this interface and call
 * {@link GridFactory#start(GridConfiguration)} method to initialize GridGain instance.
 * <p>
 * Note, that absolutely every configuration property in <tt>GridConfiguration</tt> is optional.
 * Once can simply create new instance of {@link GridConfigurationAdapter}, for example,
 * and pass it to {@link GridFactory#start(GridConfiguration)} to start grid with
 * default configuration. See {@link GridFactory} documentation for information about
 * default configuration properties used and more information on how to start grid.
 * <p>
 * <b>
 * For more information about grid configuration and startup refer to {@link GridFactory}
 * documentation which includes description and default values for every configuration
 * property.
 * </b>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridConfiguration {
    /**
     * Default flag for peer class loading. By default the value is <tt>true</tt>
     * which means that peer class loading is enabled.
     */
    public static final boolean DFLT_PEER_CLASS_LOADING_ENABLED = true;

    /** Default metrics history size (value is <tt>10000</tt>). */
    public static final int DFLT_METRICS_HISTORY_SIZE = 10000;

    /**
     * Default metrics expire time. The value is {@link Long#MAX_VALUE} which
     * means that metrics never expire.
     */
    public static final long DFLT_METRICS_EXPIRE_TIME = Long.MAX_VALUE;

    /** Default maximum peer class loading timeout in milliseconds (value is <tt>2,000ms</tt>). */
    public static final long DFLT_PEER_CLASS_LOADING_TIMEOUT = 5000;

    /** Default discovery startup delay in milliseconds (value is <tt>60,000ms</tt>). */
    public static final long DFLT_DISCOVERY_STARTUP_DELAY = 60000;

    /** Default deployment mode (value is {@link GridDeploymentMode#PRIVATE}). */
    public static final GridDeploymentMode DFLT_DEPLOYMENT_MODE = GridDeploymentMode.ISOLATED;

    /** Default cache size for missed resources. */
    public static final int DFLT_PEER_CLASS_LOADING_MISSED_RESROUCES_CACHE_SIZE = 100;

    /**
     * Gets optional name of this grid instance. If name is not provides, <tt>null</tt> will
     * be used as a default name for the grid instance.
     *
     * @return Grid name. Can be <tt>null</tt> which is default if non-default name was
     *      not provided.
     */
    public String getGridName();

    /**
     * Should return any user-defined attributes to be added to this node. These attributes can
     * then be accessed on nodes by calling {@link GridNode#getAttribute(String)} or
     * {@link GridNode#getAttributes()} methods.
     * <p>
     * Note that system adds the following attributes automatically (so you don't have to add
     * them manually):
     * <ul>
     * <li><tt>{@link System#getProperties()}</tt> - All system properties.</li>
     * <li><tt>{@link System#getenv(String)}</tt> - All environment properties.</li>
     * <li><tt>org.gridgain.build.ver</tt> - GridGain build version.</li>
     * <li><tt>org.gridgain.jit.name</tt> - Name of JIT compiler used.</li>
     * <li><tt>org.gridgain.net.itf.name</tt> - Name of network interface.</li>
     * <li><tt>org.gridgain.user.name</tt> - Operating system user name.</li>
     * <li><tt>org.gridgain.grid.name</tt> - Grid name (see {@link Grid#getName()}).</li>
     * <li>
     *      <tt>spiName.org.gridgain.spi.class</tt> - SPI implementation class for every SPI,
     *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
     * </li>
     * <li>
     *      <tt>spiName.org.gridgain.spi.ver</tt> - SPI version for every SPI,
     *      where <tt>spiName</tt> is the name of the SPI (see {@link GridSpi#getName()}.
     * </li>
     * </ul>
     * <p>
     * Note that grid will add all System properties and environment properties
     * to grid node attributes also. SPI's may also add node attributes that are
     * used for SPI implementation.
     * <p>
     * <b>NOTE:</b> attributes names starting with <tt>org.gridgain</tt> are reserved
     * for internal use.
     *
     * @return User defined attributes for this node.
     */
    public Map<String, ? extends Serializable> getUserAttributes();

    /**
     * Should return an instance of logger to use in grid. If not provided, default value will be used.
     * See {@link GridFactory} for information on default configuration.
     *
     * @return Logger to use in grid.
     */
    public GridLogger getGridLogger();

    /**
     * Should return an instance of marshaller to use in grid. If not provided, default value will be used.
     * See {@link GridFactory} for information on default configuration.
     *
     * @return Marshaller to use in grid.
     */
    public GridMarshaller getMarshaller();

    /**
     * Should return an instance of fully configured thread pool to be used in grid.
     * This executor service will be in charge of processing {@link GridTask GridTasks}
     * and {@link GridJob GridJobs}.
     * <p>
     * If not provided, default value will be used. See {@link GridFactory} for
     * information on default configuration.
     *
     * @return Thread pool implementation to be used in grid to process job execution
     *      requests and user messages sent to the node.
     */
    public ExecutorService getExecutorService();

    /**
     * Should return an instance of fully configured executor service that is
     * in charge of processing {@link GridTaskSession} requests and user messages
     * sent via {@link Grid#sendMessage(GridNode, Serializable)} method.
     * <p>
     * If not provided, default value will be used. See {@link GridFactory} for
     * information on default configuration.
     *
     * @return Thread pool implementation to be used in grid for job responses
     *      and session attributes processing.
     */
    public ExecutorService getSystemExecutorService();

    /**
     * Should return an instance of fully configured executor service which
     * is in charge of peer class loading requests/responses. If you don't use
     * P2P class loading and use GAR deployment only we would recommend to decrease
     * the value of total threads to <tt>1</tt>.
     * <p>
     * If not provided, default value will be used. See {@link GridFactory} for
     * information on default configuration.
     *
     * @return Thread pool implementation to be used for peer class loading
     *      requests handling.
     */
    public ExecutorService getPeerClassLoadingExecutorService();

    /**
     * Should return GridGain installation home folder. If not provided, the system will check
     * <tt>GRIDGAIN_HOME</tt> system property and environment variable in that order. If
     * <tt>GRIDGAIN_HOME</tt> still could not be obtained, then grid will not start and exception
     * will be thrown.
     *
     * @return GridGain installation home or <tt>null</tt> to make the system attempt to
     *      infer it automatically.
     */
    public String getGridGainHome();

    /**
     * Should return MBean server instance. If not provided, the system will use default
     * platform MBean server.
     *
     * @return MBean server instance or <tt>null</tt> to make the system create a default one.
     * @see ManagementFactory#getPlatformMBeanServer()
     */
    public MBeanServer getMBeanServer();

    /**
     * Unique identifier for this node within grid. If not provided, default value will be used.
     * See {@link GridFactory} for information on default configuration.
     *
     * @return Unique identifier for this node within grid.
     */
    public UUID getNodeId();

    /**
     * Returns <tt>true</tt> if peer class loading is enabled, <tt>false</tt>
     * otherwise. Default value is <tt>true</tt> specified by {@link #DFLT_PEER_CLASS_LOADING_ENABLED}.
     * <p>
     * When peer class loading is enabled and task is not deployed on local node,
     * local node will try to load classes from the node that initiated task
     * execution. This way, a task can be physically deployed only on one node
     * and then internally penetrate to all other nodes.
     * <p>
     * See {@link GridTask} documentation for more information about task deployment.
     *
     * @return <tt>true</tt> if peer class loading is enabled, <tt>false</tt>
     *      otherwise.
     */
    public boolean isPeerClassLoadingEnabled();

    /**
     * Should return list of packages from the system classpath that need to
     * be peer-to-peer loaded from task originating node.
     * '*' is supported at the end of the package name which means
     * that all sub-packages and their classes are included like in Java
     * package import clause.
     *
     * @return List of peer-to-peer loaded package names.
     */
    public List<String> getP2PLocalClassPathExclude();

    /**
     * Number of metrics to keep in memory to calculate totals and averages.
     * If not provided (value is <tt>0</tt>), then default value
     * {@link #DFLT_METRICS_HISTORY_SIZE} is used.
     *
     * @return Metrics history size.
     */
    public int getMetricsHistorySize();

    /**
     * Elapsed time in milliseconds after which metrics are considered expired.
     * If not provided (value is <tt>0</tt>), then default value
     * {@link #DFLT_METRICS_EXPIRE_TIME} is used.
     *
     * @return Metrics expire time.
     */
    public long getMetricsExpireTime();

    /**
     * Maximum timeout in milliseconds to wait for class-loading responses from
     * remote nodes. After reaching this timeout {@link ClassNotFoundException}
     * will be thrown.
     * <p>
     * If not provided (value is <tt>0</tt>), then default value
     * {@link #DFLT_PEER_CLASS_LOADING_TIMEOUT} is used.
     *
     * @return Maximum timeout for peer-class-loading requests.
     */
    public long getPeerClassLoadingTimeout();

    /**
     * Returns a collection of life-cycle beans. These beans will be automatically
     * notified of grid life-cycle events. Use life-cycle beans whenever you
     * want to perform certain logic before and after grid startup and stopping
     * routines.
     *
     * @return Collection of life-cycle beans.
     * @see GridLifecycleBean
     * @see GridLifecycleEventType
     */
    public Collection<? extends GridLifecycleBean> getLifecycleBeans();

    /**
     * Should return fully configured discovery SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid discovery SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridDiscoverySpi getDiscoverySpi();

    /**
     * Should return fully configured SPI communication  implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid communication SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridCommunicationSpi getCommunicationSpi();

    /**
     * Should return fully configured event SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid event SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridEventStorageSpi getEventStorageSpi();

    /**
     * Should return fully configured collision SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid collision SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridCollisionSpi getCollisionSpi();

    /**
     * Should return fully configured metrics SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid metrics SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridLocalMetricsSpi getMetricsSpi();

    /**
     * Should return fully configured deployment SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid deployment SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridDeploymentSpi getDeploymentSpi();

    /**
     * Should return fully configured checkpoint SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid checkpoint SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridCheckpointSpi[] getCheckpointSpi();

    /**
     * Should return fully configured failover SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid failover SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridFailoverSpi[] getFailoverSpi();

    /**
     * Should return fully configured topology SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid topology SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridTopologySpi[] getTopologySpi();

    /**
     * Should return fully configured load balancing SPI implementation. If not provided, default
     * implementation will be used. See {@link GridFactory} for information on default configuration.
     *
     * @return Grid load balancing SPI implementation or <tt>null</tt> to use default implementation.
     */
    public GridLoadBalancingSpi[] getLoadBalancingSpi();

    /**
     * Should return fully configured tracing SPI implementation. If not provided, returns
     * <tt>null</tt>. Note that tracing SPI is optional. If not provided - no tracing will
     * be configured (i.e. there is no default tracing SPI implementation).
     *
     * @return Grid tracing SPI implementation or <tt>null</tt> to <b>not use tracing.</b>
     */
    public GridTracingSpi[] getTracingSpi();

    /**
     * This value is used to expire messages from waiting list whenever node
     * discovery discrepancies happen.
     * <p>
     * During startup, it is possible for some SPI's, such as
     * <tt>GridMuleDiscoverySpi</tt> or <tt>GridJmsDiscoverySpi</tt>, to have a
     * small time window when Node A has discovered Node B, but Node B
     * has not discovered Node A yet. Such time window is usually very small,
     * a matter of milliseconds, but certain JMS providers or some Mule
     * messaging protocols may be very slow and hence have larger discovery
     * delay window.
     * <p>
     * The default value of this property is <tt>60,000</tt> specified by
     * {@link #DFLT_DISCOVERY_STARTUP_DELAY}. This should be good enough for vast
     * majority of configurations. However, if you do anticipate an even larger
     * delay, you should increase this value.
     *
     * @return Time in milliseconds for when nodes can be out-of-sync.
     */
    public long getDiscoveryStartupDelay();

    /**
     * Gets deployment mode for deploying tasks and other classes on this node.
     * Refer to {@link GridDeploymentMode} documentation for more information.
     *
     * @return Deployment mode.
     */
    public GridDeploymentMode getDeploymentMode();

    /**
     * Returns missed resources cache size. If size greater than <tt>0</tt>, missed
     * resources will be cached and next resource request ignored. If size is <tt>0</tt>,
     * then request for the resource will be sent to the remote node every time this
     * resource is requested.
     *
     * @return Missed resources cache size.
     */
    public int getPeerClassLoadingMissedResourcesCacheSize();
}
