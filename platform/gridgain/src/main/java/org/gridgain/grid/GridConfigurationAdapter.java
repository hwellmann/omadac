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
import javax.management.*;
import org.gridgain.apache.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
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
import org.gridgain.grid.util.tostring.*;

/**
 * Adapter for {@link GridConfiguration} interface. Use it to add custom configuration
 * for grid. Note that you should only set values that differ from defaults, as grid
 * will automatically pick default values for all values that are not set.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public class GridConfigurationAdapter implements GridConfiguration {
    /** Optional grid name. */
    private String gridName = null;

    /** User attributes. */
    private Map<String, ? extends Serializable> userAttrs = null;

    /** Logger. */
    private GridLogger log = null;

    /** Executor service. */
    private ExecutorService execSvc = null;

    /** Executor service. */
    private ExecutorService systemSvc = null;

    /** Executor service. */
    private ExecutorService p2PSvc = null;

    /** Gridgain installation folder. */
    private String ggHome = null;

    /** MBean server. */
    private MBeanServer mbeanServer = null;

    /** Local node ID. */
    private UUID nodeId = null;

    /** Marshaller. */
    private GridMarshaller marshaller = null;

    /** Whether or not peer class loading is enabled. */
    private boolean isPeerClsLoadingEnabled = DFLT_PEER_CLASS_LOADING_ENABLED;

    /** List of package prefixes from the system class path that should be P2P loaded. */
    private List<String> p2PLocalClassPathExclude = null;

    /** Maximum P2P class loading timeout. */
    private long peerClsLoadingTimeout = DFLT_PEER_CLASS_LOADING_TIMEOUT;

    /** Metrics history time. */
    private int metricsHistorySize = DFLT_METRICS_HISTORY_SIZE;

    /** Metrics expire time. */
    private long metricsExpireTime = DFLT_METRICS_EXPIRE_TIME;

    /** Collection of life-cycle beans. */
    private Collection<? extends GridLifecycleBean> lifecycleBeans = null;

    /** Discovery SPI. */
    private GridDiscoverySpi discoSpi = null;

    /** Communication SPI. */
    private GridCommunicationSpi commSpi = null;

    /** Event storage SPI. */
    private GridEventStorageSpi evtSpi = null;

    /** Collision SPI. */
    private GridCollisionSpi colSpi = null;

    /** Metrics SPI. */
    private GridLocalMetricsSpi metricsSpi = null;

    /** Deployment SPI. */
    @GridToStringInclude
    private GridDeploymentSpi deploySpi = null;

    /** Checkpoint SPI. */
    @GridToStringInclude
    private GridCheckpointSpi[] cpSpi = null;

    /** Tracing SPI. */
    @GridToStringInclude
    private GridTracingSpi[] traceSpi = null;

    /** Failover SPI. */
    @GridToStringInclude
    private GridFailoverSpi[] failSpi = null;

    /** Topology SPI. */
    @GridToStringInclude
    private GridTopologySpi[] topSpi = null;

    /** Load balancing SPI. */
    @GridToStringInclude
    private GridLoadBalancingSpi[] loadBalancingSpi = null;

    /** Discovery startup delay. */
    private long discoStartupDelay = 0;

    /** Tasks classes sharing mode. */
    private GridDeploymentMode deployMode = DFLT_DEPLOYMENT_MODE;

    /** Cache size of missed resources. */
    private int missedRsrcsCacheSize = DFLT_PEER_CLASS_LOADING_MISSED_RESROUCES_CACHE_SIZE;

    /**
     * Creates valid grid configuration with all default values.
     */
    public GridConfigurationAdapter() {
        // No-op.
    }

    /**
     * Creates grid configuration by coping all configuration properties from
     * given configuration.
     *
     * @param cfg Grid configuration to copy from.
     */
    public GridConfigurationAdapter(GridConfiguration cfg) {
        assert cfg != null : "ASSERTION [line=164, file=src/java/org/gridgain/grid/GridConfigurationAdapter.java]";

        // SPIs.
        discoSpi = cfg.getDiscoverySpi();
        commSpi = cfg.getCommunicationSpi();
        deploySpi = cfg.getDeploymentSpi();
        evtSpi = cfg.getEventStorageSpi();
        cpSpi = cfg.getCheckpointSpi();
        colSpi = cfg.getCollisionSpi();
        failSpi = cfg.getFailoverSpi();
        topSpi = cfg.getTopologySpi();
        metricsSpi = cfg.getMetricsSpi();
        loadBalancingSpi = cfg.getLoadBalancingSpi();
        traceSpi = cfg.getTracingSpi();

        gridName = cfg.getGridName();
        userAttrs = cfg.getUserAttributes();
        log = cfg.getGridLogger();
        execSvc = cfg.getExecutorService();
        systemSvc = cfg.getSystemExecutorService();
        p2PSvc = cfg.getPeerClassLoadingExecutorService();
        ggHome = cfg.getGridGainHome();
        mbeanServer = cfg.getMBeanServer();
        nodeId = cfg.getNodeId();
        marshaller = cfg.getMarshaller();
        isPeerClsLoadingEnabled = cfg.isPeerClassLoadingEnabled();
        metricsHistorySize = cfg.getMetricsHistorySize();
        metricsExpireTime = cfg.getMetricsExpireTime();
        discoStartupDelay = cfg.getDiscoveryStartupDelay();
        peerClsLoadingTimeout = cfg.getPeerClassLoadingTimeout();
        lifecycleBeans = cfg.getLifecycleBeans();
        missedRsrcsCacheSize = cfg.getPeerClassLoadingMissedResourcesCacheSize();
    }

    /**
     * Gets optional grid name. Returns <tt>null</tt> if non-default grid name was not
     * provided.
     *
     * @return Optional grid name. Can be <tt>null</tt>, which is default grid name, if
     *      non-default grid name was not provided.
     */
    public String getGridName() {
        return gridName;
    }

    /**
     * Sets grid name. Note that <tt>null</tt> is a default grid name.
     *
     * @param gridName Grid name to set. Can be <tt>null</tt>, which is default
     *      grid name.
     */
    public void setGridName(String gridName) {
        this.gridName = gridName;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, ? extends Serializable> getUserAttributes() {
        return userAttrs;
    }

    /**
     * Sets user attributes for this node.
     *
     * @param userAttrs User attributes for this node.
     * @see GridConfiguration#getUserAttributes()
     */
    public void setUserAttributes(Map<String, ? extends Serializable> userAttrs) {
        this.userAttrs = userAttrs;
    }

    /**
     * {@inheritDoc}
     */
    public GridLogger getGridLogger() {
        return log;
    }

    /**
     * Sets logger to use within grid.
     *
     * @param log Logger to use within grid.
     * @see GridConfiguration#getGridLogger()
     */
    public void setGridLogger(GridLogger log) {
        this.log = log;
    }

    /**
     * {@inheritDoc}
     */
    public ExecutorService getExecutorService() {
        return execSvc;
    }

    /**
     * {@inheritDoc}
     */
    public ExecutorService getSystemExecutorService() {
        return systemSvc;
    }

    /**
     * {@inheritDoc}
     */
    public ExecutorService getPeerClassLoadingExecutorService() {
        return p2PSvc;
    }

    /**
     * Sets thread pool to use within grid.
     *
     * @param execSvc Thread pool to use within grid.
     * @see GridConfiguration#getExecutorService()
     */
    public void setExecutorService(ExecutorService execSvc) {
        this.execSvc = execSvc;
    }

    /**
     * Sets system thread pool to use within grid.
     *
     * @param systemSvc Thread pool to use within grid.
     * @see GridConfiguration#getSystemExecutorService()
     */
    public void setSystemExecutorService(ExecutorService systemSvc) {
        this.systemSvc = systemSvc;
    }

    /**
     * Sets thread pool to use for peer class loading.
     *
     * @param p2PSvc Thread pool to use within grid.
     * @see GridConfiguration#getPeerClassLoadingExecutorService()
     */
    public void setPeerClassLoadingExecutorService(ExecutorService p2PSvc) {
        this.p2PSvc = p2PSvc;
    }

    /**
     * {@inheritDoc}
     */
    public String getGridGainHome() {
        return ggHome;
    }

    /**
     * Sets <tt>GridGain</tt> installation folder.
     *
     * @param ggHome <tt>GridGain</tt> installation folder.
     * @see GridConfiguration#getGridGainHome()
     */
    public void setGridGainHome(String ggHome) {
        this.ggHome = ggHome;
    }

    /**
     * {@inheritDoc}
     */
    public MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    /**
     * Sets initialized and started MBean server.
     *
     * @param mbeanServer Initialized and started MBean server.
     */
    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getNodeId() {
        return nodeId;
    }

    /**
     * Sets unique identifier for local node.
     *
     * @param nodeId Unique identifier for local node.
     * @see GridConfiguration#getNodeId()
     */
    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * {@inheritDoc}
     */
    public GridMarshaller getMarshaller() {
        return marshaller;
    }

    /**
     * Sets marshaller to use within grid.
     *
     * @param marshaller Marshaller to use within grid.
     * @see GridConfiguration#getMarshaller()
     */
    public void setMarshaller(GridMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPeerClassLoadingEnabled() {
        return isPeerClsLoadingEnabled;
    }

    /**
     * Enables/disables peer class loading.
     *
     * @param isPeerClsLoadingEnabled <tt>true</tt> if peer class loading is
     *      enabled, <tt>false</tt> otherwise.
     */
    public void setPeerClassLoadingEnabled(boolean isPeerClsLoadingEnabled) {
        this.isPeerClsLoadingEnabled = isPeerClsLoadingEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getP2PLocalClassPathExclude() {
        return p2PLocalClassPathExclude;
    }

    /**
     * Sets list of packages in a system class path that should be to P2P
     * loaded even if they exist locally.
     *
     * @param localClassPathExclude List of P2P loaded packages. Package
     *      name supports '*' at the end like in package import clause.
     */
    public void setP2PLocalClassPathExclude(List<String> localClassPathExclude) {
        p2PLocalClassPathExclude = localClassPathExclude;
    }

    /**
     * {@inheritDoc}
     */
    public int getMetricsHistorySize() {
        return metricsHistorySize;
    }

    /**
     * Sets number of metrics kept in history to compute totals and averages.
     * If not explicitly set, then default value is <tt>10,000</tt>.
     *
     * @param metricsHistorySize Number of metrics kept in history to use for
     *      metric totals and averages calculations.
     */
    public void setMetricsHistorySize(int metricsHistorySize) {
        this.metricsHistorySize = metricsHistorySize;
    }

    /**
     * {@inheritDoc}
     */
    public long getMetricsExpireTime() {
        return metricsExpireTime;
    }

    /**
     * Sets time in milliseconds after which a certain metric value is considered expired.
     * If not set explicitly, then default value is <tt>600,000</tt> milliseconds (10 minutes).
     *
     * @param metricsExpireTime The metricsExpireTime to set.
     */
    public void setMetricsExpireTime(long metricsExpireTime) {
        this.metricsExpireTime = metricsExpireTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getPeerClassLoadingTimeout() {
        return peerClsLoadingTimeout;
    }

    /**
     * Sets maximum timeout in milliseconds to wait for class-loading responses from
     * remote nodes. After reaching this timeout {@link ClassNotFoundException}
     * will be thrown.
     * <p>
     * If not provided (value is <tt>0</tt>), the default vlaue is <tt>10,000</tt>
     * or 10 seconds.
     *
     * @param peerClsLoadingTimeout Maximum timeout for peer-class-loading requests.
     */
    public void setPeerClassLoadingTimeout(long peerClsLoadingTimeout) {
        this.peerClsLoadingTimeout = peerClsLoadingTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<? extends GridLifecycleBean> getLifecycleBeans() {
        return lifecycleBeans;
    }

    /**
     * Sets a collection of lifecycle beans. These beans will be automatically
     * notified of grid lifecycle events. Use lifecycle beans whenever you
     * want to perform certain logic before and after grid startup and stopping
     * routines.
     *
     * @param lifecycleBeans Collection of lifecycle beans.
     * @see GridLifecycleEventType
     */
    public void setLifecycleBeans(Collection<? extends GridLifecycleBean> lifecycleBeans) {
        this.lifecycleBeans = lifecycleBeans;
    }

    /**
     * {@inheritDoc}
     */
    public GridEventStorageSpi getEventStorageSpi() {
        return evtSpi;
    }

    /**
     * Sets fully configured instance of {@link GridEventStorageSpi}.
     *
     * @param evtSpi Fully configured instance of {@link GridEventStorageSpi}.
     * @see GridConfiguration#getEventStorageSpi()
     */
    public void setEventStorageSpi(GridEventStorageSpi evtSpi) {
        this.evtSpi = evtSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridDiscoverySpi getDiscoverySpi() {
        return discoSpi;
    }

    /**
     * Sets fully configured instance of {@link GridDiscoverySpi}.
     *
     * @param discoSpi Fully configured instance of {@link GridDiscoverySpi}.
     * @see GridConfiguration#getDiscoverySpi()
     */
    public void setDiscoverySpi(GridDiscoverySpi discoSpi) {
        this.discoSpi = discoSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridCommunicationSpi getCommunicationSpi() {
        return commSpi;
    }

    /**
     * Sets fully configured instance of {@link GridCommunicationSpi}.
     *
     * @param commSpi Fully configured instance of {@link GridCommunicationSpi}.
     * @see GridConfiguration#getCommunicationSpi()
     */
    public void setCommunicationSpi(GridCommunicationSpi commSpi) {
        this.commSpi = commSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridCollisionSpi getCollisionSpi() {
        return colSpi;
    }

    /**
     * Sets fully configured instance of {@link GridCollisionSpi}.
     *
     * @param colSpi Fully configured instance of {@link GridCollisionSpi} or
     *      <tt>null</tt> if no SPI provided.
     * @see GridConfiguration#getCollisionSpi()
     */
    public void setCollisionSpi(GridCollisionSpi colSpi) {
        this.colSpi = colSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridLocalMetricsSpi getMetricsSpi() {
        return metricsSpi;
    }

    /**
     * Sets fully configured instance of {@link GridLocalMetricsSpi}.
     *
     * @param metricsSpi Fully configured instance of {@link GridLocalMetricsSpi} or
     *      <tt>null</tt> if no SPI provided.
     * @see GridConfiguration#getMetricsSpi()
     */
    public void setMetricsSpi(GridLocalMetricsSpi metricsSpi) {
        this.metricsSpi = metricsSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridDeploymentSpi getDeploymentSpi() {
        return deploySpi;
    }

    /**
     * Sets fully configured instance of {@link GridDeploymentSpi}.
     *
     * @param deploySpi Fully configured instance of {@link GridDeploymentSpi}.
     * @see GridConfiguration#getDeploymentSpi()
     */
    public void setDeploymentSpi(GridDeploymentSpi deploySpi) {
        this.deploySpi = deploySpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridCheckpointSpi[] getCheckpointSpi() {
        return cpSpi;
    }

    /**
     * Sets fully configured instance of {@link GridCheckpointSpi}.
     *
     * @param cpSpi Fully configured instance of {@link GridCheckpointSpi}.
     * @see GridConfiguration#getCheckpointSpi()
     */
    public void setCheckpointSpi(GridCheckpointSpi... cpSpi) {
        this.cpSpi = cpSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridTracingSpi[] getTracingSpi() {
        return traceSpi;
    }

    /**
     * Sets fully configured instance of {@link GridTracingSpi}.
     *
     * @param traceSpi Fully configured instance of {@link GridTracingSpi} or
     *      <tt>null</tt> if no SPI provided.
     * @see GridConfiguration#getTracingSpi()
     */
    public void setTracingSpi(GridTracingSpi... traceSpi) {
        this.traceSpi = traceSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridFailoverSpi[] getFailoverSpi() {
        return failSpi;
    }

    /**
     * Sets fully configured instance of {@link GridFailoverSpi}.
     *
     * @param failSpi Fully configured instance of {@link GridFailoverSpi} or
     *      <tt>null</tt> if no SPI provided.
     * @see GridConfiguration#getFailoverSpi()
     */
    public void setFailoverSpi(GridFailoverSpi... failSpi) {
        this.failSpi = failSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridTopologySpi[] getTopologySpi() {
        return topSpi;
    }

    /**
     * Sets fully configured instance of {@link GridTopologySpi}.
     *
     * @param topSpi Fully configured instance of {@link GridTopologySpi} or
     *      <tt>null</tt> if no SPI provided.
     * @see GridConfiguration#getTopologySpi()
     */
    public void setTopologySpi(GridTopologySpi... topSpi) {
        this.topSpi = topSpi;
    }

    /**
     * {@inheritDoc}
     */
    public GridLoadBalancingSpi[] getLoadBalancingSpi() {
        return loadBalancingSpi;
    }

    /**
     * {@inheritDoc}
     */
    public long getDiscoveryStartupDelay() {
        return discoStartupDelay;
    }

    /**
     * Sets time in milliseconds after which a certain metric value is considered expired.
     * If not set explicitly, then default value is <tt>600,000</tt> milliseconds (10 minutes).
     *
     * @param discoStartupDelay Time in milliseconds for when nodes
     *      can be out-of-sync during startup.
     */
    public void setDiscoveryStartupDelay(long discoStartupDelay) {
        this.discoStartupDelay = discoStartupDelay;
    }

    /**
     * Sets fully configured instance of {@link GridLoadBalancingSpi}.
     *
     * @param loadBalancingSpi Fully configured instance of {@link GridLoadBalancingSpi} or
     *      <tt>null</tt> if no SPI provided.
     * @see GridConfiguration#getLoadBalancingSpi()
     */
    public void setLoadBalancingSpi(GridLoadBalancingSpi... loadBalancingSpi) {
        this.loadBalancingSpi = loadBalancingSpi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridConfigurationAdapter.class, this);
    }

    /**
     * Sets task classes and resources sharing mode.
     *
     * @param deployMode Task classes and resources sharing mode.
     */
    public void setDeploymentMode(GridDeploymentMode deployMode) {
        this.deployMode = deployMode;
    }

    /**
     * {@inheritDoc}
     */
    public GridDeploymentMode getDeploymentMode() {
        return deployMode;
    }

    /**
     * Sets size of missed resources cache. Set 0 to avoid
     * missed resources caching.
     *
     * @param missedRsrcsCacheSize size of missed resources cache.
     */
    public void setPeerClassLoadingMissedResourcesCacheSize(int missedRsrcsCacheSize) {
        this.missedRsrcsCacheSize = missedRsrcsCacheSize;
    }

    /**
     * {@inheritDoc}
     */
    public int getPeerClassLoadingMissedResourcesCacheSize() {
        return missedRsrcsCacheSize;
    }
}
