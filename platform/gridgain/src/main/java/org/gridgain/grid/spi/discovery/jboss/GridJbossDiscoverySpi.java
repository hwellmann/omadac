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

package org.gridgain.grid.spi.discovery.jboss;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.naming.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import static org.gridgain.grid.GridDiscoveryEventType.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.jboss.ha.framework.interfaces.*;

/**
 * JBoss implementation of {@link GridDiscoverySpi} SPI. It uses JBoss cluster capabilities
 * to discover remote nodes in grid. SPI registers in cluster service with name {@link #DISCO_SERVICE_NAME}
 * and every node in the cluster makes cluster method call with name {@link #DISCO_METHOD_NAME} to receive
 * the remote node data.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has the following optional parameters:
 * <ul>
 * <li>JBoss cluster partition JNDI name (see {@link #setPartitionJndiName(String)}).</li>
 * <li>Metrics frequency (see {@link #setMetricsFrequency(long)}).</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridJbossDiscoverySpi needs to be explicitly configured to override default Multicast discovery SPI.
 * <pre name="code" class="java">
 * GridJbossDiscoverySpi spi = new GridJbossDiscoverySpi();
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default discovery SPI.
 * cfg.setDiscoverySpi(spi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridJbossDiscoverySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.discovery.jboss.GridJbossDiscoverySpi"/&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <b>Note</b>: JBoss is not shipped with GridGain. If you don't have JBoss, you need to
 * download it separately. See <a target=_blank href="http://www.jboss.com">http://www.jboss.com</a> for
 * more information. Once installed, JBoss should be available on the classpath for
 * GridGain. Most likely you will be starting GridGain from JBoss (you can use supplied loader
 * for JBoss). Alternatively, if you use <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> script to
 * start a grid node you can simply add JBoss JARs to <tt>[GRIDGAIN_HOME]/bin/setenv.{sh|bat}</tt>
 * scripts that's used to set up class path for the main scripts.
 * <p>
 * <b>Note</b>: When using JBoss discovery SPI you cannot start
 * multiple GridGain instances in the same VM due to limitations of JBoss. GridGain runtime
 * will detect this situation and prevent GridGain from starting in such case.
 * See {@link GridSpiMultipleInstancesSupport} for detail.
 * <p>
 * <b>Note</b>: This SPI use <tt>HAPartition.HAMembershipExtendedListener</tt> from JBoss Cluster API.
 * See bug <a target=_blank href="http://jira.jboss.com/jira/browse/JBAS-3833">http://jira.jboss.com/jira/browse/JBAS-3833</a>
 * and fixed JBoss AS versions: JBossAS-5.0.0.Beta1, JBossAS-4.2.0.CR1, JBossAS-4.0.5.SP1 and higher.
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridDiscoverySpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(false)
public class GridJbossDiscoverySpi extends GridSpiAdapter implements GridDiscoverySpi, GridJbossDiscoverySpiMBean {
    /**
     * Default JBoss cluster partition JNDI name (value is <tt>/HAPartition/DefaultPartition</tt>).
     */
    public static final String DFLT_PARTITION_JNDI_NAME = "/HAPartition/DefaultPartition";

    /** Name of service registered in cluster (value is <tt>attrInfoService</tt>). */
    public static final String DISCO_SERVICE_NAME = "attrInfoService";

    /** Method name of service registered in cluster (value is <tt>remoteExec</tt>). */
    public static final String DISCO_METHOD_NAME = "remoteExec";

    /** Default metrics heartbeat delay (value is <tt>3000</tt>).*/
    public static final long DFLT_METRICS_FREQ = 3000;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /** */
    private volatile GridDiscoveryListener listener = null;

    /** Local node. */
    private GridJbossDiscoveryNode locNode = null;

    /** Local data. */
    private GridJbossDiscoveryNodeData localData = null;

    /** Set of remote nodes that have state <tt>READY</tt>. */
    private List<GridNode> rmtNodes = null;

    /**  Map of all nodes in grid. */
    private Map<UUID, GridJbossDiscoveryNode> allNodes = new HashMap<UUID, GridJbossDiscoveryNode>();

    /** Cluster partition JNDI name. */
    private String partJndiName = DFLT_PARTITION_JNDI_NAME;

    /** */
    private HAPartition part = null;

    /** */
    private HAPartition.HAMembershipListener haListener = null;

    /** Local node attributes. */
    private Map<String, Serializable> nodeAttrs = null;

    /** */
    private final Object mux = new Object();

    /** */
    private String gridName = null;

    /** */
    private GridSpiThread metricsUpdater = null;

    /** Local node metrics provider. */
    private GridDiscoveryMetricsProvider metricsProvider = null;

    /** Delay between metrics requests. */
    private long metricsFreq = DFLT_METRICS_FREQ;

    /**
     * IoC configuration parameter to specify JNDI name for partition.
     *
     * @param partJndiName Sets JNDI partition name.
     */
    @GridSpiConfiguration(optional = true)
    public void setPartitionJndiName(String partJndiName) {
        this.partJndiName = partJndiName;
    }

    /**
     * Method called by Spi in cluster.
     * <b>NOTE:</b> Method should be public. It is used in cluster by Java reflection.
     *
     * @param data Called node data.
     * @return Local node data.
     */
    @SuppressWarnings({"ClassEscapesDefinedScope"})
    public byte[] remoteExec(GridJbossDiscoveryNodeData data) {
        synchronized (mux) {
            rmtNodes = null;
        }

        process(data);

        try {
            // Send local data only for alive nodes.
            return data.isLeave() == true ? null : GridMarshalHelper.marshal(marshaller, localData).getArray();
        }
        catch (GridException e) {
            log.error("Failed to serialize local data: " + localData, e);

            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<GridNode> getRemoteNodes() {
        synchronized (mux) {
            if (rmtNodes == null) {
                rmtNodes = new ArrayList<GridNode>(allNodes.size());

                for (GridJbossDiscoveryNode node : allNodes.values()) {
                    if (node.isReady() == true && node.equals(locNode) == false) {
                        rmtNodes.add(node);
                    }
                }

                // Seal it.
                rmtNodes = Collections.unmodifiableList(rmtNodes);
            }

            return rmtNodes;
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=241, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";

        if (locNode.getId().equals(nodeId) == true) {
            return locNode;
        }

        synchronized (mux) {
            GridJbossDiscoveryNode node = allNodes.get(nodeId);

            return node != null && node.isReady() == true ? node : null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<UUID> getRemoteNodeIds() {
        Set<UUID> ids = new HashSet<UUID>();

        for (GridNode node : getRemoteNodes()) {
            ids.add(node.getId());
        }

        return ids;
    }

    /**
     * {@inheritDoc}
     */
    public int getRemoteNodeCount() {
        Collection<GridNode> tmp = getRemoteNodes();

        return tmp == null ? 0 : tmp.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=280, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";

        synchronized (mux) {
            for (GridJbossDiscoveryNode node : allNodes.values()) {
                if (node.getId() != null && nodeId.equals(node.getId()) == true && node.isReady() == true) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Sets delay between metrics requests. SPI sends broadcast messages in
     * configurable time interval to other nodes to notify them about its metrics.
     * <p>
     * If not provided the default value is {@link #DFLT_METRICS_FREQ}.
     *
     * @param metricsFreq Time in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setMetricsFrequency(long metricsFreq) {
        this.metricsFreq = metricsFreq;
    }

    /**
     * {@inheritDoc}
     */
    public long getMetricsFrequency() {
        return metricsFreq;
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getLocalNode() {
        return locNode;
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridDiscoveryListener listener) {
        this.listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    public void setMetricsProvider(GridDiscoveryMetricsProvider metricsProvider) {
        this.metricsProvider = metricsProvider;
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeAttributes(Map<String, Serializable> attrs) {
        // Seal it.
        nodeAttrs = Collections.unmodifiableMap(attrs);
    }

    /**
     * {@inheritDoc}
     */
    // Warning suppression is due to JGroups...
    @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(partJndiName != null, "partitionJndiName != null");

        this.gridName = gridName;

        try {
            //noinspection JNDIResourceOpenedButNotSafelyClosed
            InitialContext jndiCtx = new InitialContext();

            part = (HAPartition)jndiCtx.lookup(partJndiName);
        }
        catch (NamingException e) {
            throw (GridSpiException)new GridSpiException("Failed to lookup HA partition in JNDI: " + partJndiName, e).setData(362, "src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java");
        }

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("partJndiName", partJndiName));
        }

        // Get local metrics outside the synchronization to
        // avoid possible deadlocks.
        GridNodeMetrics locMetrics = metricsProvider.getMetrics();

        synchronized (mux) {
            ClusterNode[] nodes = part.getClusterNodes();

            String localNodeName = part.getNodeName();

            for (ClusterNode node : nodes) {
                GridJbossDiscoveryNode discoNode;

                if (node.getName().equals(localNodeName) == true) {
                    assert locNode == null : "ASSERTION [line=383, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";
                    assert localData == null : "ASSERTION [line=384, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";

                    locNode = new GridJbossDiscoveryNode(node.getOriginalJGAddress(), metricsProvider);

                    discoNode = locNode;

                    localData = new GridJbossDiscoveryNodeData(locNode.getJBossId(), nodeId, nodeAttrs,
                        locMetrics);

                    locNode.onDataReceived(localData);
                }
                else {
                    discoNode = new GridJbossDiscoveryNode(node.getOriginalJGAddress());
                }

                allNodes.put(discoNode.getId(), discoNode);
            }

            assert locNode != null : "ASSERTION [line=402, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";
            assert localData != null : "ASSERTION [line=403, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";
        }

        haListener = new HAPartition.AsynchHAMembershipExtendedListener() {
            /**
             * {@inheritDoc}
             */
            @SuppressWarnings({"CollectionDeclaredAsConcreteClass", "UseOfObsoleteCollectionType", "unchecked"})
            public void membershipChanged(Vector deadMembers, Vector newMembers, Vector allMembers) {
                if (log.isDebugEnabled() == true) {
                    log.debug("JBoss cluster membership changed.");
                }

                handleChangedMembership(deadMembers, newMembers);
            }

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            public void membershipChangedDuringMerge(Vector deadMembers, Vector newMembers, Vector allMembers,
                Vector originatingGroups) {
                if (log.isDebugEnabled() == true) {
                    log.debug("JBoss cluster membership changed during merge.");
                }

                handleChangedMembership(deadMembers, newMembers);

                // Send local attributes in cluster.
                try {
                    part.callAsynchMethodOnCluster(DISCO_SERVICE_NAME, DISCO_METHOD_NAME,
                        new Object[] { localData }, new Class[] { GridJbossDiscoveryNodeData.class }, true);
                }
                // NOTE: Catching java.lang.Exception is due to JBoss design...
                catch (Exception e) {
                    log.error("Failed to send/receive grid data for cluster members.", e);
                }
            }

            /**
             * Handle membership changes event.
             * Note that method remoteExec() may be finished before this method.
             *
             * @param deadMembers FIXDOC
             * @param newMembers FIXDOC
             */
            @SuppressWarnings({"CollectionDeclaredAsConcreteClass", "UseOfObsoleteCollectionType", "unchecked"})
            private void handleChangedMembership(Vector deadMembers, Vector newMembers) {
                if (log.isDebugEnabled() ==  true) {
                    log.debug("Changed membership [newMembers=" + newMembers + ", deadMembers=" + deadMembers + ']');
                }

                List<GridJbossDiscoveryNode> deadNodes = new ArrayList<GridJbossDiscoveryNode>();

                synchronized (mux) {
                    // Reset discovery cache.
                    rmtNodes = null;

                    for (Object member : deadMembers) {
                        // Ignore metrics provider here as we are not going to use this node.
                        GridJbossDiscoveryNode deadNode = getAnyNode(new GridJbossDiscoveryNode(((ClusterNode)member).
                            getOriginalJGAddress()).getJBossId());

                        if (deadNode != null) {
                            allNodes.remove(deadNode.getId());

                            if (deadNode.isReady() == true) {
                                deadNodes.add(deadNode);
                            }
                            else {
                                log.warning("Node had never successfully joined (will remove): " + deadNode);
                            }
                        }
                    }
                }

                // Notify outside the synchronization.
                for (GridJbossDiscoveryNode node: deadNodes) {
                    notifyDiscovery(FAILED, node);

                    if (log.isInfoEnabled() == true) {
                        log.info("Node has failed and left topology: " + node);
                    }
                }
            }
        };

        // Register as a listener of cluster membership changes
        part.registerMembershipListener(haListener);

        part.registerRPCHandler(DISCO_SERVICE_NAME, this);

        // Lookup members in cluster and receive data from them.
        try {
            List answers = null;

            synchronized (mux) {
                answers = part.callMethodOnCluster(DISCO_SERVICE_NAME, DISCO_METHOD_NAME,
                    new Object[] { localData }, new Class[] { GridJbossDiscoveryNodeData.class }, true);

                // Reset discovery cache.
                rmtNodes = null;
            }

            if (answers != null) {
                for (Object answer : answers) {
                    if (answer != null && answer instanceof byte[] == true) {
                        Object answerObj = GridMarshalHelper.unmarshal(marshaller,
                            new GridByteArrayList((byte[])answer), answer.getClass().getClassLoader());

                        if (answerObj != null && answerObj instanceof GridJbossDiscoveryNodeData == true) {
                            process((GridJbossDiscoveryNodeData)answerObj);
                        }
                        else {
                            log.warning("Failed to deserialize answer: " + answer);
                        }
                    }
                    else {
                        log.warning("Received invalid answer from cluster node [answer=" + answer + ']');
                    }
                }
            }
        }
        // NOTE: Catching java.lang.Exception is due to JBoss design...
        catch (Exception e) {
            log.error("Failed to receive grid data for cluster members.", e);
        }

        metricsUpdater = new JbossNodesMetricsUpdater();

        metricsUpdater.start();

        registerMBean(gridName, this, GridJbossDiscoverySpiMBean.class);

        // Ack ok start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void spiStop() throws GridSpiException {
        GridUtils.interrupt(metricsUpdater);
        GridUtils.join(metricsUpdater, log);

        metricsUpdater = null;

        if (part != null) {
            if (haListener != null) {
                part.unregisterMembershipListener(haListener);
            }

            part.unregisterRPCHandler(DISCO_SERVICE_NAME, this);

            // Send leave message to remote nodes.
            try {
                //noinspection unchecked
                part.callMethodOnCluster(DISCO_SERVICE_NAME, DISCO_METHOD_NAME,
                    new Object[] { new GridJbossDiscoveryNodeData(localData.getJBossId(), nodeId,
                        Collections.EMPTY_MAP, true, localData.getMetrics()) },
                    new Class[] { GridJbossDiscoveryNodeData.class }, true);
            }
            // NOTE: Catching java.lang.Exception is due to JBoss design...
            catch (Exception e) {
                log.error("Failed to send leave data for cluster members.", e);
            }
        }

        unregisterMBean();

        if (log.isInfoEnabled() == true) {
            log.info("Local grid node has left grid topology.");
        }

        allNodes.clear();

        // Clear resources.
        part = null;
        haListener = null;
        rmtNodes = null;
        locNode = null;
        localData = null;

        // Ack ok stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * Update node based on received data.
     *
     * @param data Node data.
     */
    private void process(GridJbossDiscoveryNodeData data) {
        assert data != null : "ASSERTION [line=601, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=602, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";

        GridJbossDiscoveryNode node = getAnyNode(data.getJBossId());

        if (data.isLeave() == true) {
            if (node != null) {
                allNodes.remove(node.getId());

                if (node.isReady() == true) {
                    notifyDiscovery(LEFT, node);
                }
                else {
                    log.warning("Node had never successfully joined (will remove) [nodeId=" + data.getId() +
                        ", jbossId=" + GridUtils.byteArray2HexString(data.getJBossId()) + ']');
                }
            }

            if (log.isInfoEnabled() == true) {
                log.info("Node has left topology [nodeId=" + data.getId() +
                    ", jbossId=" + GridUtils.byteArray2HexString(data.getJBossId()) + ']');
            }

            return;
        }

        if (node == null) {
            try {
                node = new GridJbossDiscoveryNode(data);

                allNodes.put(node.getId(), node);
            }
            catch (UnknownHostException e) {
                log.error("Received data message for unknown node (check your network): " + data, e);

                return;
            }
        }

        if (node.isReady() == false) {
            UUID saveId = node.getId();

            node.onDataReceived(data);

            synchronized (mux) {
                // Remove temporary id.
                allNodes.remove(saveId);

                allNodes.put(node.getId(), node);
            }

            notifyDiscovery(JOINED, node);

            if (log.isInfoEnabled() == true) {
                log.info("New node has joined topology: " + node);
            }
        }
        else {
            if (log.isDebugEnabled() == true) {
                log.debug("Received data for existing node [nodeId=" + data.getId() +
                    ", jbossId=" + GridUtils.byteArray2HexString(data.getJBossId()) + ']');
            }
        }
    }

    /**
     * Method is called when any discovery event occurs. It calls external listener.
     *
     * @param evt Discovery event type.
     * @param node Remote node this event is connected with.
     */
    private void notifyDiscovery(GridDiscoveryEventType evt, GridJbossDiscoveryNode node) {
        assert evt != null : "ASSERTION [line=673, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";
        assert node != null : "ASSERTION [line=674, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";

        if (node.getStatus().equals(GridJbossDiscoveryNodeState.NEW_NO_DATA) == false) {
            GridDiscoveryListener listener = this.listener;

            if (listener != null) {
                listener.onDiscovery(evt, node);
            }
        }
    }

    /**
     * Make node search by JBoss Id in collection of discovered nodes.
     *
     * @param id JBoss Id.
     * @return JBoss node.
     */
    private GridJbossDiscoveryNode getAnyNode(byte[] id) {
        synchronized (mux) {
            for (GridJbossDiscoveryNode node : allNodes.values()) {
                if (Arrays.equals(node.getJBossId(), id) == true) {
                    return node;
                }
            }
        }

        return null;
    }

    /**
     * Gets local node metrics.
     *
     * @return Local node metrics.
     */
    private byte[] getLocalNodeMetrics() {
        byte[] data = new byte[GridDiscoveryMetricsHelper.METRICS_SIZE];

        // Local node metrics.
        GridDiscoveryMetricsHelper.serialize(data, 0, metricsProvider.getMetrics());

        return data;
    }

    /**
     * JBoss cluster metrics sender.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class JbossNodesMetricsUpdater extends GridSpiThread {
        /** Distributed state category. */
        private static final String CATEGORY = "metrics";

        /** Cluster distributed state. */
        private final DistributedState distState;

        /** Distributed state listener. */
        private final DistributedState.DSListenerEx listenerState;

        /**
         * Creates new nodes metrics updater.
         */
        JbossNodesMetricsUpdater() {
            super(gridName, "grid-disco-jboss-metrics-updater", log);

            // Get distributed state.
            distState = part.getDistributedStateService();

            listenerState = new JbossStateListener();

            /* Register state listener. */
            distState.registerDSListenerEx(CATEGORY, listenerState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            try {
                while (isInterrupted() == false) {
                    byte[] localNodeMetrics = null;

                    try {
                        localNodeMetrics = getLocalNodeMetrics();

                        distState.set(CATEGORY, locNode.getJBossId(), localNodeMetrics);

                        if (log.isDebugEnabled() == true) {
                            log.debug("Local node metrics were published.");
                        }
                    }
                    catch (Exception e) {
                        log.error("Failed to publish local node metrics.", e);
                    }

                    Thread.sleep(metricsFreq);
                }
            }
            finally {
                distState.unregisterDSListenerEx(CATEGORY, listenerState);

                try {
                    // Asynchronously remove metrics for stopped node.
                    distState.remove(CATEGORY, locNode.getJBossId(), true);
                }
                catch (Exception e) {
                    log.error("Failed to remove metrics for stopped node: " + nodeId, e);
                }
            }
        }

        /**
         * Listener that updated remote nodes metrics.
         *
         * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
         * @version 2.1.1
         */
        private class JbossStateListener implements DistributedState.DSListenerEx {
            /**
             * {@inheritDoc}
             */
            public void keyHasBeenRemoved(String category, Serializable key, Serializable value, boolean locally) {
                // No-op.
            }

            /**
             * {@inheritDoc}
             */
            public void valueHasChanged(String category, Serializable key, Serializable value, boolean locally) {
                if (CATEGORY.equals(category) == true) {
                    assert key != null : "ASSERTION [line=805, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";
                    assert value != null : "ASSERTION [line=806, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";
                    assert value instanceof byte[] == true : "ASSERTION [line=807, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoverySpi.java]";

                    byte[] metrics = (byte[]) value;

                    GridJbossDiscoveryNode node = null;

                    synchronized(mux) {
                        node = getAnyNode((byte[]) key);

                        if (node != null) {
                            if (node.getId().equals(nodeId) == false) {
                                node.onMetricsReceived(GridDiscoveryMetricsHelper.deserialize(metrics, 0));

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Node metrics were updated: " + node);
                                }
                            }

                            notifyDiscovery(METRICS_UPDATED, node);
                        }
                        else {
                            log.warning("Received metrics from unknown node: " + key);
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJbossDiscoverySpi.class, this);
    }
}
