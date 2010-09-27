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

package org.gridgain.grid.spi.discovery.jgroups;

import static org.gridgain.grid.GridDiscoveryEventType.FAILED;
import static org.gridgain.grid.GridDiscoveryEventType.JOINED;
import static org.gridgain.grid.GridDiscoveryEventType.LEFT;
import static org.gridgain.grid.GridDiscoveryEventType.METRICS_UPDATED;
import static org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoveryMessageType.EXCHANGE_ATTRS;
import static org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoveryMessageType.GET_ATTRS;
import static org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoveryMessageType.METRICS_UPDATE;
import static org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoveryMessageType.SET_ATTRS;
import static org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoveryNodeState.NEW_HAS_DATA;
import static org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoveryNodeState.NEW_NO_DATA;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.gridgain.grid.GridDiscoveryEventType;
import org.gridgain.grid.GridDiscoveryListener;
import org.gridgain.grid.GridNode;
import org.gridgain.grid.GridNodeMetrics;
import org.gridgain.grid.kernal.GridNodeAttributes;
import org.gridgain.grid.logger.GridLogger;
import org.gridgain.grid.resources.GridLocalNodeIdResource;
import org.gridgain.grid.resources.GridLoggerResource;
import org.gridgain.grid.spi.GridSpiAdapter;
import org.gridgain.grid.spi.GridSpiConfiguration;
import org.gridgain.grid.spi.GridSpiException;
import org.gridgain.grid.spi.GridSpiInfo;
import org.gridgain.grid.spi.GridSpiMultipleInstancesSupport;
import org.gridgain.grid.spi.GridSpiThread;
import org.gridgain.grid.spi.discovery.GridDiscoveryMetricsProvider;
import org.gridgain.grid.spi.discovery.GridDiscoverySpi;
import org.gridgain.grid.util.GridUrlHelper;
import org.gridgain.grid.util.GridUtils;
import org.gridgain.grid.util.tostring.GridToStringBuilder;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.ChannelListener;
import org.jgroups.JChannelFactory;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.RspList;

/**
 * JGroups implementation of {@link GridDiscoverySpi} SPI. It uses JGroups
 * to discover nodes in the grid.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has the following optional parameters:
 * <ul>
 * <li>JGroups configuration file (see {@link #setConfigurationFile(String)},
 * {@link #setConfigurationUrl(URL)}).</li>
 * <li>
 * Join timeout to wait for replies from other nodes at startup
 * (see {@link #setJoinTimeout(long)}).
 * </li>
 * <li>JGroups service id name (see {@link #setGroupName(String)}).</li>
 * <li>JGroups stack name (see {@link #setStackName(String)}).</li>
 * <li>Metrics frequency (see {@link #setMetricsFrequency(long)}).</li>
 * </ul>
 * Note, if you have an OS with IPv6 enabled, Java applications may try to route
 * IP multicast traffic over IPv6. Use "-Djava.net.preferIPv4Stack=true" system
 * property at VM startup to prevent this. You may also wish to specify local bind
 * address in JGroups configuration file to make sure that JGroups binds to correct
 * network interface.
 * <p>
 * <h2 class="header">Java Example</h2>
 * GridJgroupsDiscoverySpi needs to be explicitely configured to override default Multicast discovery SPI.
 * <pre name="code" class="java">
 * GridJgroupsDiscoverySpi spi = new GridJgroupsDiscoverySpi();
 *
 * // Override default JGroups configuration file.
 * spi.setConfigurationFile("/my/config/path/jgroups.xml");
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
 * GridJgroupsDiscoverySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoverySpi"&gt;
 *                 &lt;property name="configurationFile" value="/my/config/path/jgroups.xml"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
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
@GridSpiMultipleInstancesSupport(true)
public class GridJgroupsDiscoverySpi extends GridSpiAdapter implements GridDiscoverySpi, GridJgroupsDiscoverySpiMBean {
    /**
     * Default JGroups group name (value is <tt>grid.disco.jgroups.channel</tt>).
     */
    public static final String DFLT_GRP_NAME = "grid.disco.jgroups";

    /**
     * Default JGroups stack name (value is <tt>grid.jgroups.stack</tt>).
     */
    public static final String DFLT_STACK_NAME = "grid.jgroups.stack";

    /** Default timeout to discover all nodes (value is <tt>10000</tt>). */
    public static final long DFLT_JOIN_TIMEOUT = 10000;

    /**
     * Default JGroups configuration path relative to GridGain installation home folder
     * (value is <tt>config/jgroups/multicast/jgroups.xml</tt>).
     */
    public static final String DFLT_CONFIG_FILE = "config/jgroups/multicast/jgroups.xml";

    /** Default metrics heartbeat delay (value is <tt>3000</tt>).*/
    public static final long DFLT_METRICS_FREQ = 3000;

    /**
     * JGroups multiplexor channel name (value is <tt>grid.jgroups.mplex.channel</tt>).
     * Name is fixed to use multiplexor over the same channel.
     */
    private static final String CHNL_NAME = "grid.jgroups.mplex.channel";

    /** Heartbeat attribute key should be the same on all nodes. */
    private static final String HEARTBEAT_ATTRIBUTE_KEY = "gridgain:discovery:heartbeat";

    /**
     * Name of address attribute added to local node attributes at startup
     * (value is <tt>grid.disco.jgroups.address</tt>).
     */
    private static final String ATTR_ADDR = "grid.disco.jgroups.address";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    private Channel channel = null;

    /** */
    private JgroupsChannelListener channelListener = null;

    /** Set of all nodes in grid. */
    private Set<GridJgroupsDiscoveryNode> allNodes = new HashSet<GridJgroupsDiscoveryNode>();

    /** Map of ready nodes. */
    private Map<UUID, GridJgroupsDiscoveryNode> readyNodes = new HashMap<UUID, GridJgroupsDiscoveryNode>();

    /** Set of remote nodes that have state <tt>READY</tt>. */
    private List<GridNode> rmtNodes = null;

    /** Local  node. */
    private GridJgroupsDiscoveryNode localNode = null;

    /** Local node attributes. */
    private Map<String, Serializable> nodeAttrs = null;

    /** */
    private volatile GridDiscoveryListener listener = null;

    /** IoC configuration parameter to specify the name of the JGroups configuration file. */
    private String cfgFile = DFLT_CONFIG_FILE;

    /** IoC configuration parameter to specify the URL of the JGroups configuration file. */
    private URL cfgUrl = null;

    /** Join timeout, default is 10 seconds. */
    private long joinTimeout = DFLT_JOIN_TIMEOUT;

    /** */
    private MessageDispatcher dispatcher = null;

    /** IoC configuration parameter to specify the JGroups channel group name. */
    private String grpName = DFLT_GRP_NAME;

    /** IoC configuration parameter to specify the JGroups stack name. */
    private String stackName = DFLT_STACK_NAME;

    /** */
    private final Object mux = new Object();

    /** */
    private String gridName = null;

    /** */
    private GridSpiThread metricsSender = null;

    /** */
    private GridDiscoveryMetricsProvider metricsProvider = null;

    /** Delay between metrics requests. */
    private long metricsFreq = DFLT_METRICS_FREQ;

    /**
     * {@inheritDoc}
     */
    public List<GridNode> getRemoteNodes() {
        synchronized (mux) {
            if (rmtNodes == null) {
                rmtNodes = new ArrayList<GridNode>(readyNodes.size());

                for (GridJgroupsDiscoveryNode node : readyNodes.values()) {
                    assert node.isReady() == true : "ASSERTION [line=226, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

                    if (node.equals(localNode) == false) {
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
     * Sets either absolute or relative to GridGain installation home folder path to JGroups XML
     * configuration file. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_CONFIG_FILE}.
     *
     * @param cfgFile Path to JGroups configuration file.
     */
    @GridSpiConfiguration(optional = true)
    public void setConfigurationFile(String cfgFile) {
        this.cfgFile = cfgFile;
    }

    /**
     * Sets URL to JGroups XML configuration file.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_CONFIG_FILE}.
     *
     * @param cfgUrl URL to JGroups configuration file.
     */
    @GridSpiConfiguration(optional = true)
    public void setConfigurationUrl(URL cfgUrl) {
        this.cfgUrl = cfgUrl;
    }

    /**
     * Sets time limit in milliseconds to wait for responses from remote nodes.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_JOIN_TIMEOUT}.
     *
     * @param joinTimeout Timeout to wait for responses.
     */
    @GridSpiConfiguration(optional = true)
    public void setJoinTimeout(long joinTimeout) {
        this.joinTimeout = joinTimeout;
    }

    /**
     * Sets JGroups group name. In order to communicate with
     * each other, nodes must have the same group name.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_GRP_NAME}.
     *
     * @param grpName JGroups group name.
     */
    @GridSpiConfiguration(optional = true)
    public void setGroupName(String grpName) {
        this.grpName = grpName;
    }

    /**
     * {@inheritDoc}
     */
    public String getStackName() {
        return stackName;
    }

    /**
     * Sets JGroups stack name. In order to use multiplexor
     * over the same channel SPIs must have the same stack name.
     * Stack name is a name of configuration in the configuration file.
     * <p>
     * If not provided, default value is {@link #DFLT_STACK_NAME}.
     *
     * @param stackName JGroups stack name.
     */
    @GridSpiConfiguration(optional = true)
    public void setStackName(String stackName) {
        this.stackName = stackName;
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
    public String getConfigurationFile() {
        return cfgFile;
    }

    /**
     * {@inheritDoc}
     */
    public long getJoinTimeout() {
        return joinTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public int getLocalPort() {
        return ((IpAddress)channel.getLocalAddress()).getPort();
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalHost() {
        return ((IpAddress)channel.getLocalAddress()).getIpAddress().getHostAddress();
    }

    /**
     * {@inheritDoc}
     */
    public String getGroupName() {
        return grpName;
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
    public GridNode getLocalNode() {
        return localNode;
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
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(joinTimeout > 0, "joinTimeout > 0");
        assertParameter(grpName != null, "grpName != null");

        this.gridName = gridName;

        File cfgPath = null;

        if (cfgUrl != null) {
            try {
                cfgPath = GridUrlHelper.downloadUrl(cfgUrl, File.createTempFile("jgroups", "xml"));
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("Failed to download configuration file: " + cfgUrl, e).setData(448, "src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java");
            }
        }
        else {
            assertParameter(cfgFile != null, "cfgFile != null");

            cfgPath = GridUtils.resolveGridGainPath(cfgFile);
        }

        if (cfgPath == null || cfgPath.isDirectory() == true) {
            throw (GridSpiException)new GridSpiException("Invalid JGroups configuration file path: " + cfgPath).setData(458, "src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java");
        }

        if (cfgPath.canRead() == false) {
            throw (GridSpiException)new GridSpiException("JGroups configuration file does not have read permission: " + cfgPath).setData(462, "src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java");
        }

        if (joinTimeout < 1000) {
            log.warning("Join timeout value < 1 second which is too small for most of the systems.");
        }

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            if (cfgUrl != null) {
                log.info(configInfo("cfgUrl", cfgUrl));
            }
            else {
                log.info(configInfo("cfgFile", cfgFile));
            }

            log.info(configInfo("joinTimeout", joinTimeout));
            log.info(configInfo("grpName", grpName));
        }

        registerMBean(gridName, this, GridJgroupsDiscoverySpiMBean.class);

        try {
            JChannelFactory factory = new JChannelFactory();

            factory.setDomain(GridUtils.JMX_DOMAIN);
            factory.setExposeChannels(true);
            factory.setExposeProtocols(false);
            factory.setMultiplexerConfig(cfgPath);

            channel = factory.createMultiplexerChannel(stackName, grpName);

            Map<String, Serializable> attrs = new HashMap<String, Serializable>(nodeAttrs);

            // Add local address in attributes.
            attrs.put(ATTR_ADDR, channel.getLocalAddress());

            nodeAttrs = attrs;

            dispatcher = new MessageDispatcher(channel, null, new JgroupsMembershipHandler(),
                new JgroupsRequestHandler());

            dispatcher.start();

            // Log all channel events.
            channel.addChannelListener(channelListener = new JgroupsChannelListener());

            channel.connect(CHNL_NAME);

            if (log.isDebugEnabled() == true) {
                log.debug("JGroups channel members: " + channel.getView().getMembers());
            }

            // Initialize local and remote nodes.
            synchronized (mux) {
                for (Object mbr : channel.getView().getMembers()) {
                    IpAddress ipAddr = (IpAddress)mbr;

                    GridJgroupsDiscoveryNode node = getAnyNode(ipAddr.getIpAddress(), ipAddr.getPort());

                    assert localNode == null || ipAddr.equals(channel.getLocalAddress()) == false : "ASSERTION [line=522, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

                    if (localNode == null) {
                        // Determine local node.
                        if (ipAddr.equals(channel.getLocalAddress()) == true) {
                            // Local node should be created with metrics provider.
                            localNode = new GridJgroupsDiscoveryNode(ipAddr.getIpAddress(), ipAddr.getPort(),
                                metricsProvider);

                            // Set local node properties.
                            localNode.onDataReceived(nodeId, nodeAttrs);

                            allNodes.add(localNode);

                            // Local node is ready and we need to put it on list of local nodes.
                            readyNodes.put(localNode.getId(), localNode);

                            assert localNode.isReady() == true : "ASSERTION [line=539, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

                            if (log.isInfoEnabled() == true) {
                                log.info("Discovered local node: " + localNode);
                            }

                            continue;
                        }
                    }

                    if (node == null) {
                        // Discovered node. Create it with null metrics. They will be updated later.
                        allNodes.add(node = new GridJgroupsDiscoveryNode(ipAddr.getIpAddress(), ipAddr.getPort()));
                    }

                    if (log.isDebugEnabled() == true) {
                        log.debug("Discovered node: " + node);
                    }
                }

                if (localNode == null) {
                    throw (GridSpiException)new GridSpiException("Local node is not part of discovery (check OS and JGroups " +
                        "configuration).").setData(560, "src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java");
                }

                // Reset discovery cache.
                rmtNodes = null;
            }

            // Request attributes from remote nodes. We don't send local
            // attributes and metrics as we need to receive remote node data
            // first and then send own attributes.
            RspList ress = dispatcher.castMessage(null, new Message(null, channel.getLocalAddress(),
                new GridJgroupsDiscoveryMessage(GET_ATTRS, nodeId, (IpAddress)channel.getLocalAddress(),
                    null, null)), GroupRequest.GET_ALL, joinTimeout);

            if (ress.numSuspectedMembers() > 0) {
                log.warning("Failed to receive SET_ATTRS replies from nodes: " + ress.getSuspectedMembers());
            }

            // Create and start metrics sender.
            metricsSender = new JgroupsNodesMetricsSender();

            metricsSender.start();
        }
        catch (ChannelException e) {
            closeChannel();

            throw (GridSpiException)new GridSpiException("Error connecting to JGroups channel: " + CHNL_NAME, e).setData(587, "src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java");
        }
        catch (GridSpiException e) {
            closeChannel();

            throw e;
        }
        catch (Exception e) {
            closeChannel();

            throw (GridSpiException)new GridSpiException("Failed to initialize metrics sender.", e).setData(597, "src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java");
        }

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        GridUtils.interrupt(metricsSender);
        GridUtils.join(metricsSender, log);

        metricsSender = null;

        closeChannel();

        if (dispatcher != null) {
            dispatcher.stop();
        }

        unregisterMBean();

        if (log.isInfoEnabled() == true) {
            log.info("Local grid node has left grid topology.");
        }

        // Clean resources.
        localNode = null;
        rmtNodes = null;

        allNodes.clear();
        readyNodes.clear();

        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=642, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

        synchronized (mux) {
            return readyNodes.get(nodeId) != null;
        }
    }

    /**
     * Method is called when any discovery event occurs. It calls external listener.
     *
     * @param evt Discovery event type.
     * @param node Remote node this event is connected with.
     */
    private void notifyDiscovery(GridDiscoveryEventType evt, GridJgroupsDiscoveryNode node) {
        assert evt != null : "ASSERTION [line=656, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";
        assert node != null : "ASSERTION [line=657, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

        if (node.getStatus().equals(NEW_NO_DATA) == false) {
            GridDiscoveryListener listener = this.listener;

            if (listener != null) {
                listener.onDiscovery(evt, node);
            }
        }
    }

    /**
     * Close channel.
     */
    private void closeChannel() {
        if (channel != null && channel.isOpen() == true) {
            channel.removeChannelListener(channelListener);

            channel.close();
        }

        channelListener = null;
    }

    /**
     * Make node search by IP address and port in collection of discovered nodes.
     *
     * @param addr IP address.
     * @param port Port number.
     * @return Jgroups node.
     */
    private GridJgroupsDiscoveryNode getAnyNode(InetAddress addr, int port) {
        synchronized (mux) {
            for (GridJgroupsDiscoveryNode node : allNodes) {
                if (node.getAddress().equals(addr) == true && node.getPort() == port) {
                    return node;
                }
            }
        }

        return null;
    }

    /**
     * Make node search by node id in collection of discovered nodes.
     *
     * @param nodeId Node id.
     * @return Jgroups node.
     */
    public GridNode getNode(UUID nodeId) {
        synchronized (mux) {
            return readyNodes.get(nodeId);
        }
    }

    /**
     * Handler for node attribute requests.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class JgroupsRequestHandler implements RequestHandler {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
        public Object handle(Message req) {
            GridJgroupsDiscoveryMessage msg = (GridJgroupsDiscoveryMessage)req.getObject();

            GridJgroupsDiscoveryNode node = getAnyNode(msg.getIpAddress().getIpAddress(),
                msg.getIpAddress().getPort());

            // Metrics from local node should be received.
            if (node != null && node.equals(localNode) == true && msg.getType() != METRICS_UPDATE) {
                // Ignore own messages.
                if (log.isDebugEnabled() == true) {
                    log.debug("Ignoring node data message from local node: " + msg);
                }

                return null;
            }

            if (msg.getType() == GET_ATTRS) {
                // We get attributes request from the new node.
                // Send back local attributes and metrics and request
                // remote node ones.
                sendAttributes(msg, EXCHANGE_ATTRS);
            }
            else if (msg.getType() == SET_ATTRS) {
                // We get attributes from remote node - process them.
                receiveAttributes(msg);
            }
            else if (msg.getType() == EXCHANGE_ATTRS) {
                // We get remote nodes attributes which requested our ones.
                receiveAttributes(msg);

                // Send back own attributes and metrics - do not request remote
                // ones as we already got them.
                sendAttributes(msg, SET_ATTRS);
            }
            else if (msg.getType() == METRICS_UPDATE) {
                if (node != null) {
                    // This might happen that node does not have id yet (at startup).
                    // But we found it by address and need to update metrics anyway.
                    // Node will get id later when handshake is completed.
                    if (node.getId() == null || node.getId().equals(nodeId) == false) {
                        node.onMetricsReceived(msg.getMetrics());

                        if (log.isDebugEnabled() == true) {
                            log.debug("Node metrics were updated: " + node);
                        }
                    }

                    notifyDiscovery(METRICS_UPDATED, node);
                }
                else {
                    log.warning("Received metrics from unknown node: " + msg.getId());
                }
            }
            else {
                log.warning("Received unknown message type [type=" + msg.getType() + ", node=" + msg.getId() + ']');
            }

            return null;
        }

        /**
         * Handles attributes request.
         *
         * @param msg {@link GridJgroupsDiscoveryMessageType#GET_ATTRS} or
         *      {@link GridJgroupsDiscoveryMessageType#EXCHANGE_ATTRS} message.
         * @param msgType type of the message which should be sent back.
         *      Might be {@link GridJgroupsDiscoveryMessageType#EXCHANGE_ATTRS} or
         *      {@link GridJgroupsDiscoveryMessageType#SET_ATTRS}
         */
        private void sendAttributes(GridJgroupsDiscoveryMessage msg, GridJgroupsDiscoveryMessageType msgType) {
            assert msg.getType() == GET_ATTRS || msg.getType() == EXCHANGE_ATTRS : "ASSERTION [line=793, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";
            assert msgType == SET_ATTRS || msgType == EXCHANGE_ATTRS : "ASSERTION [line=794, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

            // Forced to use Vector due to JGroups design...
            //noinspection UseOfObsoleteCollectionType,CollectionDeclaredAsConcreteClass
            Vector<IpAddress> destAddrs = new Vector<IpAddress>(1);

            destAddrs.add(msg.getIpAddress());

            dispatcher.castMessage(destAddrs,
                new Message(msg.getIpAddress(), channel.getLocalAddress(),
                new GridJgroupsDiscoveryMessage(msgType, nodeId,
                (IpAddress)channel.getLocalAddress(), nodeAttrs, metricsProvider.getMetrics())),
                GroupRequest.GET_N, joinTimeout);

            if (log.isDebugEnabled() == true) {
                log.debug("Asynchronously send attributes to remote node [rmtMode=" + destAddrs +
                    ", locMbr=" + channel.getLocalAddress() + ']');
            }
        }

        /**
         * Handles remote node attributes.
         *
         * @param msg {@link GridJgroupsDiscoveryMessageType#SET_ATTRS} or
         *      {@link GridJgroupsDiscoveryMessageType#EXCHANGE_ATTRS} message.
         */
        private void receiveAttributes(GridJgroupsDiscoveryMessage msg) {
            assert msg.getType() == SET_ATTRS || msg.getType() == EXCHANGE_ATTRS : "ASSERTION [line=821, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

            // This is a remote node attributes
            synchronized (mux) {
                GridJgroupsDiscoveryNode node = getAnyNode(msg.getIpAddress().getIpAddress(),
                    msg.getIpAddress().getPort());

                GridNodeMetrics metrics = msg.getMetrics();

                if (node == null) {
                    allNodes.add(new GridJgroupsDiscoveryNode(msg, metrics));

                    if (log.isDebugEnabled() == true) {
                        log.debug("Received data message prior to initializing node: " + msg);
                    }
                }
                else {
                    assert node.equals(localNode) == false : "ASSERTION [line=838, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

                    if (log.isDebugEnabled() == true) {
                        log.debug("Received node data message: " + msg);
                    }

                    if (node.isSuspect() == true) {
                        // Note: remote node restarted and attributes has been changed.
                        if (node.isReady() == true && msg.getId().equals(node.getId()) == false) {
                            log.warning("Remote suspected node was restarted: " + node);

                            notifyDiscovery(FAILED, node);

                            allNodes.remove(node);

                            readyNodes.remove(node.getId());

                            // Create node as new.
                            allNodes.add(node = new GridJgroupsDiscoveryNode(msg.getIpAddress().getIpAddress(),
                                msg.getIpAddress().getPort()));
                        }

                        node.setSuspect(false);
                    }

                    // If two NEW nodes start investigating each other then
                    // it could happen that they set attributes of each other simultaneously
                    // and them let know each other about their attributes.
                    // It's ok because new node will never start process jobs until
                    // it is started.
                    if (node.isReady() == false) {
                        // Update with attributes and metrics.
                        node.onDataReceived(msg.getId(), msg.getAttributes());
                        node.onMetricsReceived(metrics);

                        assert node.isReady() == true : "ASSERTION [line=873, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

                        // Add/replace ready node.
                        readyNodes.put(node.getId(), node);

                        // Reset discovery cache.
                        rmtNodes = null;

                        notifyDiscovery(JOINED, node);
                    }
                }
            }
        }
    }

    /**
     * Listener for JGroups discovery channel. For the most part simply logs
     * all channel events.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class JgroupsChannelListener implements ChannelListener {
        /**
         * {@inheritDoc}
         */
        public void channelConnected(Channel channel) {
            if (log.isDebugEnabled() == true) {
                log.debug("JGroups channel has connected: " + CHNL_NAME);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void channelDisconnected(Channel channel) {
            if (log.isDebugEnabled() == true) {
                log.debug("JGroups channel has been disconnected: " + CHNL_NAME);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void channelClosed(Channel channel) {
            if (log.isDebugEnabled() == true) {
                log.debug("JGroups channel has been closed: " + grpName);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void channelShunned() {
            if (log.isDebugEnabled() == true) {
                log.debug("JGroups channel has been shunned: " + grpName);
            }

            // Local node got shunned out of topology.
            synchronized (mux) {
                allNodes.clear();

                readyNodes.clear();

                rmtNodes = null;

                notifyDiscovery(LEFT, localNode);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void channelReconnected(Address addr) {
            if (log.isDebugEnabled() == true) {
                log.debug("JGroups channel has been reconnected [address=" + addr + ", channel=" +
                    CHNL_NAME + ']');
            }
        }
    }

    /**
     * Topology listener.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class JgroupsMembershipHandler implements MembershipListener {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"UseOfObsoleteCollectionType", "CollectionDeclaredAsConcreteClass"})
        public void viewAccepted(View view) {
            // Services view might be null during startup.
            if (view == null) {
                return;
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Membership changes [mbrs=" + view.getMembers() +
                    ", locMbr=" + channel.getLocalAddress() + ']');
            }

            synchronized (mux) {
                // Reset discovery cache.
                rmtNodes = null;

                // Check for left and failed nodes.
                for (Iterator<GridJgroupsDiscoveryNode> iter = allNodes.iterator(); iter.hasNext() == true;) {
                    GridJgroupsDiscoveryNode node = iter.next();

                    if (view.containsMember(new IpAddress(node.getAddress(), node.getPort())) == false) {
                        // Node is no longer in discovery.
                        iter.remove();

                        if (node.isReady() == true) {
                            readyNodes.remove(node.getId());

                            if (node.isSuspect() == true) {
                                log.warning("Removed failed node: " + node);
                            }
                            else if (log.isInfoEnabled() == true) {
                                log.info("Removed ready node: " + node);
                            }

                            notifyDiscovery(node.isSuspect() == true ? FAILED : LEFT, node);
                        }
                        else {
                            log.warning("Node had never successfully joined (will remove): " + node);
                        }
                    }
                }

                // Check for joining nodes.
                for (Object mbr : view.getMembers()) {
                    IpAddress addr = (IpAddress)mbr;

                    GridJgroupsDiscoveryNode node = getAnyNode(addr.getIpAddress(), addr.getPort());

                    if (node != null) {
                        // Data got received before view got processed.
                        if (node.getStatus() == NEW_HAS_DATA) {
                            node.onViewReceived();

                            assert node.isReady() == true : "ASSERTION [line=1017, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

                            readyNodes.put(node.getId(), node);

                            rmtNodes = null;

                            notifyDiscovery(JOINED, node);
                        }
                    }
                    // Ignore local member.
                    else if (channel.getLocalAddress().equals(addr) == false) {
                        GridJgroupsDiscoveryNode newNode = new GridJgroupsDiscoveryNode(
                            addr.getIpAddress(), addr.getPort());

                        assert newNode.isReady() == false : "ASSERTION [line=1031, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoverySpi.java]";

                        allNodes.add(newNode);

                        if (log.isDebugEnabled() == true) {
                            log.debug("Discovered new not-ready node [newNode=" + newNode +
                                ", locMbr=" + channel.getLocalAddress() + ']');
                        }
                    }
                }
            }

            // Note that we don't send metrics and attributes to the nodes.
            // New node has to let everyone know that it needs remote nodes
            // attributes and then when it receives them sends in its turn
            // own attributes.
        }

        /**
         * {@inheritDoc}
         */
        public void suspect(Address addr) {
            IpAddress ipAddr = (IpAddress)addr;

            synchronized (mux) {
                GridJgroupsDiscoveryNode node = getAnyNode(ipAddr.getIpAddress(), ipAddr.getPort());

                if (node != null) {
                    node.setSuspect(true);

                    log.warning("Received 'suspect' message for node: " + node);

                    return;
                }

                log.warning("Received 'suspect' message for non-existing member [address" +
                    ipAddr.getIpAddress() + ", port=" + ipAddr.getPort() + ']');
            }
        }

        /**
         * {@inheritDoc}
         */
        public void block() {
            if (log.isDebugEnabled() == true) {
                log.debug("Received block call.");
            }
        }
    }

    /**
     * JGroups cluster metrics sender.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class JgroupsNodesMetricsSender extends GridSpiThread {
        /**
         * Creates new nodes metrics sender.
         */
        JgroupsNodesMetricsSender() {
            super(gridName, "grid-disco-jgroups-metrics-updater", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            while (isInterrupted() == false) {
                dispatcher.castMessage(null, new Message(null, channel.getLocalAddress(),
                    new GridJgroupsDiscoveryMessage(METRICS_UPDATE, nodeId, (IpAddress)channel.getLocalAddress(),
                        null, metricsProvider.getMetrics())), GroupRequest.GET_NONE, 0);

                if (log.isDebugEnabled() == true) {
                    log.debug("Local node metrics were published.");
                }

                Thread.sleep(metricsFreq);
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
        return GridToStringBuilder.toString(GridJgroupsDiscoverySpi.class, this);
    }
}
