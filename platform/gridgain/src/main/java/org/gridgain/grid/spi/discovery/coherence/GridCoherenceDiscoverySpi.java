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

package org.gridgain.grid.spi.discovery.coherence;

import com.tangosol.net.*;
import com.tangosol.util.*;
import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.UUID;
import org.gridgain.grid.*;
import static org.gridgain.grid.GridDiscoveryEventType.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Oracle Coherence implementation of {@link GridDiscoverySpi} SPI. It uses Coherence
 * cluster capabilities discover remote nodes in grid. SPI works with Coherence distributed cache
 * named {@link #DFLT_GRIDGAIN_CACHE} and every node in the cluster works with that cache
 * to communicate with other remote nodes.
 * <p>
 * All grid nodes have information about Coherence cluster members they are associated with in
 * attribute by name {@link #ATTR_COHERENCE_MBR}. Use
 * {@link GridNode#getAttribute(String) GridNode.getAttribute(ATTR_COHERENCE_MBR)} to get a handle
 * on {@link GridCoherenceMember} class.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has the following optional parameters:
 * <ul>
 * <li>Coherence cache name (see {@link #setCacheName(String)}).</li>
 * <li>Grid node metrics update frequency (see {@link #setMetricsFrequency(long)}.</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridCoherenceDiscoverySpi needs to be explicitly configured to override default Multicast discovery SPI.
 * <pre name="code" class="java">
 * GridCoherenceDiscoverySpi spi = new GridCoherenceDiscoverySpi();
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default discovery SPI.
 * cfg.setDiscoverySpi(spi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <p>
 * <h2 class="header">Spring Example</h2>
 * GridCoherenceDiscoverySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.discovery.coherence.GridCoherenceDiscoverySpi"/&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <b>Note</b>: Coherence is not shipped with GridGain. If you don't have Coherence, you need to
 * download it separately. See <a target=_blank href="http://www.oracle.com/tangosol/index.html">http://www.oracle.com/tangosol/index.html</a> for
 * more information. Once installed, Coherence should be available on the classpath for
 * GridGain. If you use <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> script to start
 * a grid node you can simply add Coherence JARs to <tt>[GRIDGAIN_HOME]/bin/setenv.{sh|bat}</tt>
 * scripts that's used to set up class path for the main scripts.
 * <p>
 * <b>Note</b>: When using Coherence SPIs (communication or discovery) you cannot start
 * multiple GridGain instances in the same VM due to limitations of Coherence. GridGain runtime
 * will detect this situation and prevent GridGain from starting in such case.
 * See {@link GridSpiMultipleInstancesSupport} for details.
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridDiscoverySpi
 */
@SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(false)
public class GridCoherenceDiscoverySpi extends GridSpiAdapter implements GridDiscoverySpi,
    GridCoherenceDiscoverySpiMBean {
    /** Default Coherence cache name (value is <tt>gridgain.discovery.cache</tt>). */
    public static final String DFLT_GRIDGAIN_CACHE = "gridgain.discovery.cache";

    /**
     * Name of cluster {@link GridCoherenceMember} attribute added to local node attributes
     * at startup (value is <tt>disco.coherence.member</tt>).
     */
    public static final String ATTR_COHERENCE_MBR = "disco.coherence.member";

    /** Name of Coherence cache used by SPI (value is <tt>disco.coherence.cache</tt>). */
    public static final String ATTR_COHERENCE_CACHE_NAME = "disco.coherence.cache";

    /** Default metrics heartbeat delay (value is <tt>3000</tt>).*/
    public static final long DFLT_METRICS_FREQ = 3000;

    /** Prefix used for keys which nodes updates cache with new metrics value (value is <tt>heartbeat_</tt>).*/
    private static final String HEARTBEAT_KEY_PREFIX = "heartbeat_";

    /** Heartbeat attribute key should be the same on all nodes. */
    private static final String HEARTBEAT_ATTRIBUTE_KEY = "gridgain:discovery:heartbeat";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** Map of all discovered nodes with different statuses. */
    private Map<UUID, GridCoherenceDiscoveryNode> allNodes = new HashMap<UUID, GridCoherenceDiscoveryNode>();

    /** Set of remote nodes that have state <tt>READY</tt>. */
    private List<GridNode> rmtNodes = null;

    /** Local node. */
    private GridCoherenceDiscoveryNode locNode = null;

    /** Discovery listener. */
    private GridDiscoveryListener listener = null;

    /** Local node attributes. */
    private Map<String, Serializable> nodeAttrs = null;

    /** Local node data. */
    private GridCoherenceDiscoveryNodeData locData = null;

    /** */
    private final Object mux = new Object();

    /** Coherence service name. */
    private String cacheName = DFLT_GRIDGAIN_CACHE;

    /** */
    private String gridName = null;

    /** */
    private GridSpiThread metricsUpdater = null;

    /** Local node metrics provider. */
    private GridDiscoveryMetricsProvider metricsProvider = null;

    /** Delay between metrics requests. */
    private long metricsFreq = DFLT_METRICS_FREQ;

    /** */
    private NamedCache cache = null;

    /** */
    private MapListener cacheListener = null;

    /** */
    private MemberListener mbrListener = null;

    /** */
    private boolean stopping = false;

    /** Enable/disable flag for cache heartbeat updater. */
    private volatile boolean cacheBusy = false;

    /**
     * Sets name for Coherence cache used in grid.
     * <p>
     * If not provided, default value is {@link #DFLT_GRIDGAIN_CACHE}.
     *
     * @param cacheName Cache name.
     */
    @GridSpiConfiguration(optional = true)
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return cacheName;
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
    public void setNodeAttributes(Map<String, Serializable> attrs) {
        // Seal it.
        nodeAttrs = Collections.unmodifiableMap(attrs);
    }

    /**
     * {@inheritDoc}
     */
    public List<GridNode> getRemoteNodes() {
        synchronized (mux) {
            if (rmtNodes == null) {
                rmtNodes = new ArrayList<GridNode>(allNodes.size());

                for (GridCoherenceDiscoveryNode node : allNodes.values()) {
                    if (node.equals(locNode) == false) {
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
        assert nodeId != null : "ASSERTION [line=268, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

        if (locNode.getId().equals(nodeId) == true) {
            return locNode;
        }

        synchronized (mux) {
            return allNodes.get(nodeId);
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
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(HEARTBEAT_ATTRIBUTE_KEY), getMetricsFrequency());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(cacheName != null, "cacheName != null");
        assertParameter(metricsFreq > 0, "metricsFrequency > 0");

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("cacheName", cacheName));
            log.info(configInfo("metricsFrequency", metricsFreq));
        }

        assertParameter(cacheName != null, "cacheName != null");

        stopping = false;

        cache = CacheFactory.getCache(cacheName);

        Member locMbr = CacheFactory.getCluster().getLocalMember();

        Map<String, Serializable> attrs = new HashMap<String, Serializable>(nodeAttrs);

        attrs.put(ATTR_COHERENCE_MBR, new GridCoherenceMember(locMbr));
        attrs.put(ATTR_COHERENCE_CACHE_NAME, cacheName);

        nodeAttrs = attrs;

        locNode = new GridCoherenceDiscoveryNode(locMbr.getAddress(), locMbr.getUid().toByteArray(), metricsProvider);

        locData = new GridCoherenceDiscoveryNodeData(nodeId, locMbr.getUid().toByteArray(), locMbr.getAddress(),
            nodeAttrs, metricsProvider.getMetrics());

        locNode.onDataReceived(locData);

        cacheListener = createMapListener();
        mbrListener = createServiceMembershipListener();

        // Process all cache entries.
        synchronized (mux) {
            // Add local node data. Put it in mux to avoid finding this node
            // on remote.
            cache.put(locNode.getId(), locData);

            // Add map listener for cache.
            cache.addMapListener(cacheListener);

            // Add listener for cluster event notifications.
            cache.getCacheService().addMemberListener(mbrListener);

            for (Entry entry : (Set<Entry>)cache.entrySet()) {
                if (entry.getKey() instanceof UUID) {
                    if (entry.getValue() instanceof GridCoherenceDiscoveryNodeData) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Found node data in cache [key=" + entry.getKey() +
                                ", value=" + entry.getValue() + ']');
                        }

                        // Handle node data from cache.
                        processNewOrUpdatedNode((UUID)entry.getKey(), (GridCoherenceDiscoveryNodeData)entry.getValue(),
                            false);
                    }
                    else {
                        log.warning("Unknown node data type found during SPI start [nodeId=" + entry.getKey() +
                            ", nodeDataClass=" + (entry.getValue() == null ? null :
                            entry.getValue().getClass().getName()));
                    }
                }
            }

            // Refresh returned collection.
            rmtNodes = null;
        }

        metricsUpdater = new CoherenceNodesMetricsUpdater();

        metricsUpdater.start();

        registerMBean(gridName, this, GridCoherenceDiscoverySpiMBean.class);

        // Ack ok start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public void spiStop() throws GridSpiException {
        stopping = true;

        if (cache != null && locData != null) {
            locData.setLeave(true);

            // Update cache and all nodes should update source node flag.
            cache.put(nodeId, locData);
        }

        GridUtils.interrupt(metricsUpdater);
        GridUtils.join(metricsUpdater, log);

        metricsUpdater = null;

        if (cache != null) {
            // Remove cache listener and local node object with attributes.
            cache.removeMapListener(cacheListener);
            cache.remove(nodeId);

            // Remove listener for cluster event notifications.
            cache.getCacheService().removeMemberListener(mbrListener);
        }

        cache = null;
        cacheListener = null;
        mbrListener = null;
        rmtNodes = null;
        locData = null;

        // Unregister SPI MBean.
        unregisterMBean();

        // Ack ok stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=467, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

        if (this.nodeId.equals(nodeId) == true) {
            return true;
        }

        synchronized (mux) {
            for (GridCoherenceDiscoveryNode node : allNodes.values()) {
                if (node.getId() != null && nodeId.equals(node.getId()) == true) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates map listener for nodes in replicated cache.
     *
     * @return Listener.
     */
    private MapListener createMapListener() {
        return new MapListener() {
            /**
             * {@inheritDoc}
             */
            public void entryInserted(MapEvent evt) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Inserted in cache [key=" + evt.getKey() + ", value=" + evt.getNewValue() + ']');
                }

                if (evt.getKey() instanceof UUID) {
                    assert evt.getNewValue() instanceof GridCoherenceDiscoveryNodeData == true : "ASSERTION [line=500, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

                    processNewOrUpdatedNode((UUID)evt.getKey(), (GridCoherenceDiscoveryNodeData)evt.getNewValue(),
                        nodeId.equals(evt.getKey()) == false);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void entryUpdated(MapEvent evt) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Updated in cache [key=" + evt.getKey() + ", oldValue=" + evt.getOldValue() +
                        ", newValue=" + evt.getNewValue() + ']');
                }

                if (evt.getKey() instanceof UUID) {
                    assert evt.getNewValue() instanceof GridCoherenceDiscoveryNodeData == true : "ASSERTION [line=517, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

                    processNewOrUpdatedNode((UUID)evt.getKey(), (GridCoherenceDiscoveryNodeData)evt.getNewValue(),
                        nodeId.equals(evt.getKey()) == false);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void entryDeleted(MapEvent evt) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Deleted in cache [key=" + evt.getKey() + ", oldValue=" + evt.getOldValue() +
                        ", newValue=" + evt.getNewValue() + ']');
                }

                if (evt.getKey() instanceof UUID) {
                    processDeletedNode((UUID)evt.getKey(), nodeId.equals(evt.getKey()) == false);
                }
            }
        };
    }

    /**
     * Creates membership listener for cluster.
     *
     * @return Listener.
     */
    private MemberListener createServiceMembershipListener() {
        return new MemberListener() {
            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            public void memberJoined(MemberEvent evt) {
                Member mbr = evt.getMember();

                if (log.isDebugEnabled() == true) {
                    log.debug("Coherence cluster member joined: " + getMemberInfo(mbr));
                }

                // If local node joined in cluster again then we need to create
                // new local node and rescan cache for exist nodes.
                // Usually local node joined in cluster after disconnection (and local cluster
                // services will be restarted).
                if (evt.isLocal() == true) {
                    Member locMbr = CacheFactory.getCluster().getLocalMember();

                    assert mbr.getUid().equals(locMbr.getUid()) == true : "ASSERTION [line=565, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

                    log.warning("Local node rejoined. Perhaps Coherence services was restarted [oldMbrUid=" +
                        GridUtils.byteArray2HexString(locNode.getMemberUid()) +
                        ", newMbrUid=" + GridUtils.byteArray2HexString(locMbr.getUid().toByteArray()) + ']');

                    Map<String, Serializable> attrs = new HashMap<String, Serializable>(nodeAttrs);

                    attrs.put(ATTR_COHERENCE_MBR, new GridCoherenceMember(locMbr));
                    attrs.put(ATTR_COHERENCE_CACHE_NAME, cacheName);

                    synchronized (mux) {
                        nodeAttrs = attrs;

                        // Create new local node with new attributes.
                        locNode = new GridCoherenceDiscoveryNode(locMbr.getAddress(), locMbr.getUid().toByteArray(),
                            metricsProvider);

                        locData = new GridCoherenceDiscoveryNodeData(nodeId, locMbr.getUid().toByteArray(),
                            locMbr.getAddress(), nodeAttrs, metricsProvider.getMetrics());

                        locNode.onDataReceived(locData);

                        // Add local node data. Put it in mux to avoid finding this node on remote.
                        cache.put(locNode.getId(), locData);

                        Set<UUID> nodeIds = new HashSet<UUID>(allNodes.keySet());

                        // Process all cache entries.
                        for (Entry entry : (Set<Entry>)cache.entrySet()) {
                            if (entry.getKey() instanceof UUID) {
                                if (entry.getValue() instanceof GridCoherenceDiscoveryNodeData) {
                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Found node data in cache [key=" + entry.getKey() +
                                            ", value=" + entry.getValue() + ']');
                                    }

                                    // Handle node data from cache.
                                    processNewOrUpdatedNode((UUID)entry.getKey(),
                                        (GridCoherenceDiscoveryNodeData)entry.getValue(),
                                        nodeId.equals(entry.getKey()) == false);
                                }
                                else {
                                    log.warning("Unknown node data type found during node rejoin " +
                                        "[nodeId=" + entry.getKey() +
                                        ", nodeDataClass=" + (entry.getValue() == null ? null :
                                        entry.getValue().getClass().getName()));
                                }

                                nodeIds.remove(entry.getKey());
                            }
                        }

                        // Process disappeared nodes.
                        for (UUID id : nodeIds) {
                            boolean notify = nodeId.equals(id) == false;
                            // Print deleted and current nodes collection.
                            if (log.isDebugEnabled() == true) {
                                log.debug("Process disappeared node [nodeId=" + id +
                                    ", allNodes=" + getAllNodesIdsInfo() + ']');
                            }

                            for (Iterator<GridCoherenceDiscoveryNode> iter = allNodes.values().iterator();
                                iter.hasNext() == true;) {
                                GridCoherenceDiscoveryNode node = iter.next();

                                if (id.equals(node.getId()) == true) {
                                    iter.remove();

                                    if (log.isInfoEnabled() == true) {
                                        log.info("Node " + (node.isLeaving() == true ? "left" : "failed") +
                                            ": " + node);
                                    }

                                    if (notify == true) {
                                        notifyDiscovery(node.isLeaving() == true ? LEFT : FAILED, node);
                                    }

                                    break;
                                }
                            }
                        }

                        // Refresh returned collection.
                        rmtNodes = null;

                        // Mark cache as not busy to enable heartbeat updater.
                        cacheBusy = false;

                        if (log.isDebugEnabled() == true) {
                            log.debug("Enable cache heartbeat updater.");
                        }
                    }
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Return from 'memberJoined' listener call: " + nodeId);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void memberLeaving(MemberEvent evt) {
                Member mbr = evt.getMember();

                if (log.isDebugEnabled() == true) {
                    log.debug("Coherence cluster member leaving: " + getMemberInfo(mbr));
                }
            }

            /**
             * {@inheritDoc}
             */
            public void memberLeft(MemberEvent evt) {
                Member mbr = evt.getMember();

                if (log.isDebugEnabled() == true) {
                    log.debug("Coherence member left: " + getMemberInfo(mbr));
                }

                synchronized (mux) {
                    for (Iterator<GridCoherenceDiscoveryNode> iter = allNodes.values().iterator();
                        iter.hasNext() == true;) {
                        GridCoherenceDiscoveryNode node = iter.next();

                        if (Arrays.equals(node.getMemberUid(), mbr.getUid().toByteArray()) == true) {
                            if (log.isInfoEnabled() == true) {
                                log.info("Node " + (node.isLeaving() == true ? "left" : "failed") + ": " + node);
                            }

                            iter.remove();

                            // Refresh returned collection.
                            rmtNodes = null;

                            // Remove local node from collection if local node left cluster.
                            // There is code fix for disconnected nodes when local node should be rejoined.
                            // Local node will be created as new when Coherence cluster services restarted.
                            if (Arrays.equals(locNode.getMemberUid(), node.getMemberUid()) == true) {
                                // Mark cache as busy to disable heartbeat updater.
                                cacheBusy = true;

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Disable cache heartbeat updater.");
                                }

                                log.warning("Coherence cluster services was stopped on local node and member" +
                                    "has left cluster. Node was disconnected: " + nodeId);
                            }
                            else {
                                notifyDiscovery(node.isLeaving() == true ? LEFT : FAILED, node);

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Removed node from cache: " + node);
                                }

                                cache.remove(node.getId());
                                cache.remove(HEARTBEAT_KEY_PREFIX + node.getId());
                            }

                            break;
                        }
                    }
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Return from 'memberLeft' listener call: " + nodeId);
                }
            }
        };
    }

    /**
     *
     * @param key FIXDOC
     * @param nodeData FIXDOC
     * @param notify FIXDOC
     */
    private void processNewOrUpdatedNode(UUID key, GridCoherenceDiscoveryNodeData nodeData, boolean notify) {
        assert key != null : "ASSERTION [line=745, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";
        assert nodeData != null : "ASSERTION [line=746, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

        GridCoherenceDiscoveryNode node = null;

        synchronized (mux) {
            // Print new/updated node and current nodes collection.
            if (log.isDebugEnabled() == true) {
                log.debug("Process new or updated node [nodeId=" + key + ", allNodes=" + getAllNodesIdsInfo() + ']');
            }

            node = getAnyNode(key);

            if (node == null) {
                node = new GridCoherenceDiscoveryNode(nodeData);

                allNodes.put(node.getId(), node);

                // Refresh returned collection.
                rmtNodes = null;

                // Call users listeners outside the synchronization.
                if (log.isDebugEnabled() == true) {
                    log.debug("Node joined: " + node);
                }

                if (notify == true) {
                    notifyDiscovery(JOINED, node);
                }
            }
            // Set leaving flag for target node.
            // It is used for differs situations when node fails or normal turned off.
            else if (nodeData.isLeave() == true) {
                node.onLeaving();
            }
        }
    }

    /**
     *
     * @param key Node id.
     * @param notify Notify flag.
     */
    private void processDeletedNode(UUID key, boolean notify) {
        assert key != null : "ASSERTION [line=789, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

        GridCoherenceDiscoveryNode node = null;

        synchronized (mux) {
            // Print deleted and current nodes collection.
            if (log.isDebugEnabled() == true) {
                log.debug("Process deleted node [nodeId=" + key + ", allNodes=" + getAllNodesIdsInfo() + ']');
            }

            for (Iterator<GridCoherenceDiscoveryNode> iter = allNodes.values().iterator(); iter.hasNext() == true;) {
                node = iter.next();

                if (key.equals(node.getId()) == true) {
                    iter.remove();

                    // Refresh returned collection.
                    rmtNodes = null;

                    if (log.isInfoEnabled() == true) {
                        log.info("Node " + (node.isLeaving() == true ? "left" : "failed") + ": " + node);
                    }

                    if (notify == true) {
                        notifyDiscovery(node.isLeaving() == true ? LEFT : FAILED, node);
                    }

                    break;
                }
            }
        }
    }

    /**
     * Method is called when any discovery event occurs. It calls external listener.
     *
     * @param evt Discovery event type.
     * @param node Remote node this event is connected with.
     */
    private void notifyDiscovery(GridDiscoveryEventType evt, GridNode node) {
        GridDiscoveryListener localCopy = listener;

        if (localCopy != null) {
            localCopy.onDiscovery(evt, node);
        }
    }

    /**
     * Prepare member data info for logging.
     *
     * @param mbr Cluster member.
     * @return String with cluster member information.
     */
    private String getMemberInfo(Member mbr) {
        if (mbr == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("[uid=").append(GridUtils.byteArray2HexString(mbr.getUid().toByteArray()));
        builder.append(", id=").append(mbr.getId());
        builder.append(", machineId=").append(mbr.getMachineId());
        builder.append(", inetAddress=").append(mbr.getAddress());
        builder.append(", port=").append(mbr.getPort());
        builder.append(']');

        return builder.toString();
    }

    /**
     * Prepare all nodes ID's data info for logging.
     *
     * @return String with nodes ID's.
     */
    private String getAllNodesIdsInfo() {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=865, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

        StringBuilder builder = new StringBuilder();

        Set<UUID> set = allNodes.keySet();
        int i = 1;

        builder.append('{');

        for (UUID uuid : set) {
            builder.append(uuid);

            if (i < set.size()) {
                builder.append(", ");
            }

            i++;
        }

        builder.append('}');

        return builder.toString();
    }

    /**
     * Make node search by node id in collection of discovered nodes.
     *
     * @param nodeId Node id.
     * @return Coherence node.
     */
    private GridCoherenceDiscoveryNode getAnyNode(UUID nodeId) {
        synchronized (mux) {
            return allNodes.get(nodeId);
        }
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
    private class CoherenceNodesMetricsUpdater extends GridSpiThread {
        /** Heartbeat cache listener. */
        private final CoherenceStateListener heartbeatListener;

        /**
         * Creates new nodes metrics updater.
         */
        CoherenceNodesMetricsUpdater() {
            super(gridName, "grid-disco-coherence-metrics-updater", log);

            heartbeatListener = new CoherenceStateListener();

            /* Register heartbeat listener. */
            cache.addMapListener(heartbeatListener);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"unchecked"})
        @Override
        protected void body() throws InterruptedException {
            try {
                while (isInterrupted() == false) {
                    if (cacheBusy == false) {
                        byte[] localNodeMetrics = getLocalNodeMetrics();

                        // Check that cache contains local node data.
                        if (cache.containsKey(nodeId) == false) {
                            log.warning("Cache doesn't contain local node: " + nodeId);

                            cache.put(nodeId, locData);
                        }

                        // Heartbeats based on string keys.
                        cache.put(HEARTBEAT_KEY_PREFIX + nodeId.toString(), localNodeMetrics, metricsFreq);

                        if (log.isDebugEnabled() == true) {
                            log.debug("Local node metrics were published.");
                        }
                    }

                    Thread.sleep(metricsFreq);
                }
            }
            catch (InterruptedException e) {
                // Do not re-throw if it is being stopped.
                if (stopping == false) {
                    throw e;
                }
            }
            finally {
                cache.removeMapListener(heartbeatListener);

                cache.remove(HEARTBEAT_KEY_PREFIX + nodeId.toString());
            }
        }

        /**
         * Listener that updates remote nodes metrics.
         *
         * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
         * @version 2.1.1
         */
        private class CoherenceStateListener implements MapListener {
            /**
             * {@inheritDoc}
             */
            public void entryInserted(MapEvent evt) {
                updateMetric(evt);
            }

            /**
             * {@inheritDoc}
             */
            public void entryUpdated(MapEvent evt) {
                updateMetric(evt);
            }

            /**
             * {@inheritDoc}
             */
            public void entryDeleted(MapEvent evt) {
                // No-op.
            }

            /**
             * Update metrics for node.
             *
             * @param evt FIXDOC
             */
            private void updateMetric(MapEvent evt) {
                assert evt != null : "ASSERTION [line=1013, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

                if (evt.getKey() instanceof String) {
                    String key = (String)evt.getKey();

                    int idx = key.indexOf(HEARTBEAT_KEY_PREFIX);

                    if (idx != -1) {
                        UUID keyNodeId = null;

                        try {
                            keyNodeId = UUID.fromString(key.substring(idx + HEARTBEAT_KEY_PREFIX.length()));
                        }
                        catch (IllegalArgumentException e) {
                            log.error("Failed to get nodeId from key: " + key, e);

                            return;
                        }

                        assert keyNodeId != null : "ASSERTION [line=1032, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

                        byte[] metrics = (byte[]) evt.getNewValue();

                        GridCoherenceDiscoveryNode node = null;

                        boolean notify = false;

                        synchronized(mux) {
                            node = getAnyNode(keyNodeId);

                            if (node != null) {
                                // Ignore local node.
                                if (node.getId().equals(nodeId) == false) {
                                    node.onMetricsReceived(GridDiscoveryMetricsHelper.deserialize(metrics, 0));

                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Node metrics were updated: " + node);
                                    }
                                }

                                notify = true;
                            }
                            else {
                                log.warning("Received metrics from unknown node: " + keyNodeId);
                            }
                        }

                        if (notify == true) {
                            assert node != null : "ASSERTION [line=1061, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoverySpi.java]";

                            // Notify about new metrics update for remote node.
                            notifyDiscovery(METRICS_UPDATED, node);
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
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(3);

        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));
        attrs.add(createSpiAttributeName(HEARTBEAT_ATTRIBUTE_KEY));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCoherenceDiscoverySpi.class, this);
    }
}


