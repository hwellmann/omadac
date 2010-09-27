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

package org.gridgain.grid.spi.discovery.mule2;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.mule.*;
import org.mule.api.*;
import org.mule.api.component.*;
import org.mule.api.config.*;
import org.mule.api.context.*;
import org.mule.api.endpoint.*;
import org.mule.api.lifecycle.*;
import org.mule.api.routing.*;
import org.mule.api.service.*;
import org.mule.config.spring.*;
import org.mule.context.*;
import org.mule.module.client.*;

/**
 * Mule implementation of {@link GridDiscoverySpi} SPI. It uses
 * Mule ESB implementation to discover nodes in the grid.
 * <p>
 * Mule instance may be started before SPI or instantiated during SPI start.
 * SPI uses UMO component which must be declared in Mule configuration file
 * (see {@link #COMPONENT_NAME}). SPI receives all notifications from that
 * component. The component has two input endpoints for getting messages. Names
 * of those endpoints should be declared in component's descriptor properties
 * (see {@link #HANDSHAKE_ENDPOINT_NAME} and {@link #HEARTBEAT_ENDPOINT_NAME}).
 * <p>
 * Here is an example of Mule configuration file that could be used with
 * this SPI:
 * <pre name="code" class="xml">
 * &lt;mule-configuration version="1.0"&gt;
 *     &lt;mule-environment-properties synchronous="true" embedded="true"/&gt;
 *
 *     &lt;!-- Tcp connector configuration. --&gt;
 *     &lt;connector name="tcpConnector" className="org.mule.providers.tcp.TcpConnector"&gt;
 *         &lt;properties&gt;
 *             &lt;property name="tcpProtocolClassName" value="org.mule.providers.tcp.protocols.LengthProtocol"/&gt;
 *         &lt;/properties&gt;
 *     &lt;/connector&gt;
 *
 *     &lt;!-- Multicast connector configuration. --&gt;
 *     &lt;connector name="multicastConnector" className="org.mule.providers.multicast.MulticastConnector"&gt;
 *         &lt;properties&gt;
 *             &lt;property name="loopback" value="true"/&gt;
 *         &lt;/properties&gt;
 *     &lt;/connector&gt;
 *
 *     &lt;model name="gridgain"&gt;
 *         &lt;mule-descriptor name="GridDiscoveryUMO"
 *             implementation="org.gridgain.grid.spi.discovery.mule2.GridMuleDiscoveryComponent"
 *             singleton="true"&gt;
 *             &lt;inbound-router&gt;
 *                 &lt;!-- Listen for handshake data. --&gt;
 *                 &lt;endpoint name="handshake.id" address="tcp://localhost:11001"/&gt;
 *
 *                 &lt;!-- Listen for heartbeat data. --&gt;
 *                 &lt;endpoint name="heartbeat.id" address="multicast://228.1.2.172:30001"/&gt;
 *             &lt;/inbound-router&gt;
 *
 *             &lt;properties&gt;
 *                 &lt;property name="handshake" value="handshake.id"/&gt;
 *                 &lt;property name="heartbeat" value="heartbeat.id"/&gt;
 *             &lt;/properties&gt;
 *         &lt;/mule-descriptor&gt;
 *     &lt;/model&gt;
 * &lt;/mule-configuration&gt;
 * </pre>
 * <p>
 * This SPI has no mandatory parameters.
 * <p>
 * This SPI has the following optional parameters:
 * <ul>
 * <li>
 *      Mule configuration file used when Mule instance isn't started
 *      (see {@link #setConfigurationFile(String)}).
 * </li>
 * <li>
 *      Component name declared in Mule configuration
 *      (see {@link #setComponentName(String)}}).
 * </li>
 * <li>
 *      Heartbeat frequency (see {@link #setHeartbeatFrequency(long)}
 * </li>
 * <li>
 *      Number of retries to broadcast notification to other nodes about this
 *      node leaving grid (see {@link #setLeaveAttempts(int)}).
 * </li>
 * <li>
 *      Number of heartbeats that could be missed before a node is considered
 *      failed. (see {@link #setMaxMissedHeartbeats(int)})
 * </li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridMuleDiscoverySpi needs to be explicitely configured to override default Multicast discovery SPI.
 * <pre name="code" class="java">
 * GridMuleDiscoverySpi spi = new GridMuleDiscoverySpi();
 *
 * // Override default Mule configuration file.
 * spi.setConfigurationFile("/my/config/path/mule.xml");
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
 * GridMuleDiscoverySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.discovery.mule2.GridMuleDiscoverySpi"&gt;
 *                 &lt;property name="configurationFile" value="/my/config/path/mule.xml"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <b>Note</b>: Mule is not shipped with GridGain. If you don't have Mule 2.x, you need to
 * download it separately. See <a target=_blank href="http://www.mulesource.com">http://www.mulesource.com</a> for
 * more information. Once installed, Mule should be available on the classpath for
 * GridGain. If you use <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> script to start
 * a grid node you can simply add Mule JARs to <tt>[GRIDGAIN_HOME]/bin/setenv.{sh|bat}</tt>
 * scripts that's used to set up class path for the main scripts.
 * <p>
 * <b>Note</b>: When using Mule SPIs (communication or discovery) you cannot start
 * multiple GridGain instances in the same VM due to limitations of Mule. GridGain runtime
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
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(false)
public class GridMuleDiscoverySpi extends GridSpiAdapter implements GridDiscoverySpi, GridMuleDiscoverySpiMBean {
    /**
     * Default Mule 2.x configuration path relative to GridGain installation home folder.
     * (value is <tt>config/mule2/mule.xml</tt>).
     */
    public static final String DFLT_CONFIG_FILE = "config/mule2/mule.xml";

    /**
     * Name of component declared in Mule configuration
     * (value is <tt>GridDiscoveryUMO</tt>).
     */
    public static final String COMPONENT_NAME = "GridDiscoveryUMO";

    /**
     * Name of property where input endpoint name declared
     * in Mule configuration for attributes handshake.
     * (value is <tt>handshake</tt>).
     */
    public static final String HANDSHAKE_ENDPOINT_NAME = "handshake";

    /**
     * Name of property where input endpoint name declared
     * in Mule configuration for heartbeats.
     * (value is <tt>heartbeat</tt>).
     */
    public static final String HEARTBEAT_ENDPOINT_NAME = "heartbeat";

    /** Default heartbeat delay (value is <tt>3000</tt>). */
    public static final long DFLT_HEARTBEAT_FREQ = 3000;

    /** Default number of heartbeat messages that could be missed (value is <tt>3</tt>). */
    public static final int DFLT_MAX_MISSED_HEARTBEATS = 3;

    /** Default number of attempts to send leaving notification (value is <tt>3</tt>). */
    public static final int DFLT_LEAVE_ATTEMPTS = 3;

    /** Default ping wait timeout. */
    public static final long DFLT_PING_WAIT = DFLT_LEAVE_ATTEMPTS * DFLT_HEARTBEAT_FREQ;

    /** Heartbeat attribute key should be the same on all nodes. */
    private static final String HEARTBEAT_ATTRIBUTE_KEY = "gridgain:discovery:heartbeat";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /** Name of grid. */
    private String gridName = null;

    /** Listener that will be informed about discovered nodes. */
    private GridDiscoveryListener listener = null;

    /** IoC configuration parameter to specify the name of the Mule configuration file. */
    private String cfgFile = DFLT_CONFIG_FILE;

    /** Name of the component registered in Mule. */
    private String umoName = COMPONENT_NAME;

    /** Delay between heartbeat requests. */
    private long beatFreq = DFLT_HEARTBEAT_FREQ;

    /** Number of heartbeat messages that could be missed before remote node is considered as failed one. */
    private int maxMissedBeats = DFLT_MAX_MISSED_HEARTBEATS;

    /** Number of attempts to send leaving notification. */
    private int leaveAttempts = DFLT_LEAVE_ATTEMPTS;

    /** Ping wait timeout. */
    private long pingWait = DFLT_PING_WAIT;

    /** Mule component. */
    private GridMuleDiscoveryComponent discoUmo = null;

    /** Component inbound endpoint URI for heartbeats. */
    private String heartbeatUri = null;

    /** Component inbound endpoint URI for handshake. */
    private String handshakeUri = null;

    /** Mule context. */
    private MuleContext muleCtx = null;

    /** Mule client. */
    private MuleClient muleClient = null;

    /** Flag indicates whether Mule started before SPI or not.*/
    private boolean stopMule = false;

    /** Thread for sending heartbeats. */
    private GridSpiThread heartbeatSender = null;

    /** Thread for removing invalid nodes. */
    private GridSpiThread nodeSweeper = null;

    /** Map of all nodes in grid. */
    private final Map<UUID, GridMuleDiscoveryNode> allNodes = new HashMap<UUID, GridMuleDiscoveryNode>();

    /** All remote nodes in the topology that are ready. */
    private List<GridNode> rmtNodes = null;

    /** Local node. */
    private GridMuleDiscoveryNode locNode = null;

    /** Local node attributes. */
    private Map<String, Serializable> attrs = null;

    /** Node metrics provider. */
    private GridDiscoveryMetricsProvider metricsProvider = null;

    /** */
    private final Object mux = new Object();

    /** */
    private final Map<Thread, UUID> pingThreads = new HashMap<Thread, UUID>();

    /**
     * {@inheritDoc}
     */
    public String getConfigurationFile() {
        return cfgFile;
    }

    /**
     * Sets either absolute or relative to GridGain installation home folder path to Mule XML
     * configuration file. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_CONFIG_FILE}.
     *
     * @param cfgFile Path to Mule configuration file.
     */
    @GridSpiConfiguration(optional = true)
    public void setConfigurationFile(String cfgFile) {
        this.cfgFile = cfgFile;
    }

    /**
     * {@inheritDoc}
     */
    public String getHeartbeatEndpointUri() {
        return heartbeatUri;
    }

    /**
     * {@inheritDoc}
     */
    public String getHandshakeEndpointUri() {
        return handshakeUri;
    }

    /**
     * Sets delay between heartbeat requests. SPI sends heartbeat messages in
     * configurable time interval to other nodes to notify them about its state.
     * <p>
     * If not provided, default value is {@link #DFLT_HEARTBEAT_FREQ}.
     *
     * @param beatFreq Time in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setHeartbeatFrequency(long beatFreq) {
        this.beatFreq = beatFreq;
    }

    /**
     * {@inheritDoc}
     */
    public long getHeartbeatFrequency() {
        return beatFreq;
    }

    /**
     * Sets number of heartbeat requests that could be missed before remote
     * node is considered to be failed.
     * <p>
     * If not provided, default value is {@link #DFLT_MAX_MISSED_HEARTBEATS}.
     *
     * @param maxMissedBeats Number of missed requests.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaxMissedHeartbeats(int maxMissedBeats) {
        this.maxMissedBeats = maxMissedBeats;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumMissedHeartbeats() {
        return maxMissedBeats;
    }

    /**
     * Sets number of attempts to notify another nodes that this one is leaving
     * grid. Multiple leave requests are sent to increase the chance of successful
     * delivery to every node.
     * <p>
     * If not provided, default value is {@link #DFLT_LEAVE_ATTEMPTS}.
     *
     * @param leaveAttempts Number of attempts.
     */
    @GridSpiConfiguration(optional = true)
    public void setLeaveAttempts(int leaveAttempts) {
        this.leaveAttempts = leaveAttempts;
    }

    /**
     * {@inheritDoc}
     */
    public int getLeaveAttempts() {
        return leaveAttempts;
    }

    /**
     * {@inheritDoc}
     */
    public String getComponentName() {
        return umoName;
    }

    /**
     * Sets name for component registered in Mule.
     * SPI use that name for getting component from Mule instance.
     *
     * @param umoName Name for component registered in Mule.
     */
    @GridSpiConfiguration(optional = true)
    public void setComponentName(String umoName) {
        this.umoName = umoName;
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeAttributes(Map<String, Serializable> attrs) {
        this.attrs = attrs;
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
    public List<GridNode> getRemoteNodes() {
        synchronized (mux) {
            if (rmtNodes == null) {
                rmtNodes = new ArrayList<GridNode>(allNodes.size());

                for (GridMuleDiscoveryNode node : allNodes.values()) {
                    if (node.getState() == GridMuleDiscoveryNodeState.READY && node.equals(locNode) == false) {
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
        assert nodeId != null : "ASSERTION [line=471, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

        if (locNode.getId().equals(nodeId) == true) {
            return locNode;
        }

        synchronized (mux) {
            GridMuleDiscoveryNode node = allNodes.get(nodeId);

            return node != null && node.getState() == GridMuleDiscoveryNodeState.READY ? node : null;
        }
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
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(HEARTBEAT_ATTRIBUTE_KEY), getHeartbeatFrequency());
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        this.gridName = gridName;

        assertParameter(umoName != null, "umoName != null");
        assertParameter(beatFreq > 0, "beatFreq > 0");
        assertParameter(maxMissedBeats > 0, "maxMissedBeats > 0");
        assertParameter(leaveAttempts > 0, "leaveAttempts > 0");

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("umoName", umoName));
            log.info(configInfo("beatFreq", beatFreq));
            log.info(configInfo("maxMissedBeats", maxMissedBeats));
            log.info(configInfo("leaveAttempts", leaveAttempts));
        }

        if (MuleServer.getMuleContext() != null) {
            if (log.isDebugEnabled() == true) {
                log.debug("There is already a Mule manager available, no need to create a new one.");
            }

            muleCtx = MuleServer.getMuleContext();
        }
        else {
            assertParameter(cfgFile != null, "cfgFile != null");

            // Ack parameters.
            if (log.isInfoEnabled() == true) {
                log.info(configInfo("cfgFile", cfgFile));
            }

            File cfgPath = GridUtils.resolveGridGainPath(cfgFile);

            if (cfgPath == null || cfgPath.isDirectory() == true) {
                throw (GridSpiException)new GridSpiException("Invalid Mule configuration file path: " + cfgPath).setData(539, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
            }

            if (cfgPath.canRead() == false) {
                throw (GridSpiException)new GridSpiException("Mule configuration file does not have read permission: " + cfgPath).setData(543, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
            }

            try {
                MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();

                SpringXmlConfigurationBuilder builder = 
                    new SpringXmlConfigurationBuilder(new String[] {cfgPath.getAbsolutePath()});

                muleCtx = muleContextFactory.createMuleContext(builder);
            }
            catch (ConfigurationException e) {
                throw (GridSpiException)new GridSpiException("Filed to start Mule manager.", e).setData(555, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
            }
            catch (InitialisationException e) {
                throw (GridSpiException)new GridSpiException("Filed to start Mule manager.", e).setData(558, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
            }

            stopMule = true;
        }

        try {
            muleClient = new MuleClient();
        }
        catch (MuleException e) {
            throw (GridSpiException)new GridSpiException("Filed to start Mule client.", e).setData(568, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        // Initialize component and all related data.
        initializeComponent();

        assert handshakeUri != null : "ASSERTION [line=574, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";
        assert heartbeatUri != null : "ASSERTION [line=575, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

        locNode = new GridMuleDiscoveryNode(nodeId, System.currentTimeMillis(), GridMuleDiscoveryNodeState.READY,
            handshakeUri, metricsProvider);

        locNode.setAttributes(attrs);

        registerMBean(gridName, this, GridMuleDiscoverySpiMBean.class);

        heartbeatSender = new HeartbeatSender(heartbeatUri);
        nodeSweeper = new NodeSweeper();

        heartbeatSender.start();
        nodeSweeper.start();

        try {
            long timeout = beatFreq < DFLT_HEARTBEAT_FREQ ? beatFreq : DFLT_HEARTBEAT_FREQ;

            // Wait to discover other nodes.
            if (log.isInfoEnabled() == true) {
                log.info("Waiting for initial timeout to discover other nodes (" + timeout + " milliseconds)");
            }

            // Wait for others to add this node to topology.
            Thread.sleep(timeout);
        }
        catch (InterruptedException e) {
            throw (GridSpiException)new GridSpiException("Got interrupted during start.", e).setData(602, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        GridUtils.interrupt(heartbeatSender);
        GridUtils.interrupt(nodeSweeper);

        GridUtils.join(heartbeatSender, log);
        GridUtils.join(nodeSweeper, log);

        // Check Mule instance to avoid restarting.
        if (MuleServer.getMuleContext() != null) {
            assert discoUmo != null : "ASSERTION [line=625, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

            // Clear SPI listener in component.
            discoUmo.setListener(null);

            if (stopMule == true) {
                muleCtx.dispose();
            }
        }

        // Clean resources.
        muleCtx = null;
        muleClient = null;
        discoUmo = null;
        handshakeUri = null;
        heartbeatUri = null;
        stopMule = false;
        heartbeatSender = null;
        nodeSweeper = null;

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=655, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

        if (locNode.getId().equals(nodeId) == true) {
            return true;
        }

        GridMuleDiscoveryNode node = null;

        synchronized (mux) {
            node = allNodes.get(nodeId);

            if (node == null || node.getState() != GridMuleDiscoveryNodeState.READY) {
                return false;
            }

            pingThreads.put(Thread.currentThread(), nodeId);
        }

        try {
            long end = System.currentTimeMillis() + pingWait;

            long delta = pingWait;

            GridMuleDiscoveryMessage request = new GridMuleDiscoveryMessage(locNode.getId(), node.getId(),
                GridMuleDiscoveryMessageType.PING_REQUEST, null, locNode.getStartTime(), handshakeUri, null);

            muleClient.dispatch(node.getHandshakeUri(), GridMarshalHelper.marshal(marshaller, request).getArray(), null);

            if (log.isDebugEnabled() == true) {
                log.debug("Sent ping request [uri=" + node.getHandshakeUri() + ", node=" + node + ", request="
                    + request + ']');
            }

            synchronized (mux) {
                try {
                    while (delta > 0) {
                        mux.wait(delta);

                        // If thread has been removed from ping waiting list,
                        // that means we got a ping response.
                        if (pingThreads.get(Thread.currentThread()) == null) {
                            return true;
                        }

                        delta = end - System.currentTimeMillis();
                    }
                }
                catch (InterruptedException e) {
                    log.warning("Got interrupted while waiting for ping response.", e);

                    return false;
                }
            }
        }
        catch (MuleException e) {
            log.error("Failed to send ping request.", e);
        }
        catch (GridException e) {
            log.error("Failed to send ping request.", e);
        }
        finally {
            synchronized (mux) {
                pingThreads.remove(Thread.currentThread());
            }
        }

        return false;
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
     * Gets component from Mule and initialize all necessary data.
     *
     * @throws GridSpiException Thrown in case of any error.
     */
    @SuppressWarnings({"unchecked"})
    private void initializeComponent() throws GridSpiException {
        assert muleCtx != null : "ASSERTION [line=745, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

        Service umoSrvc = muleCtx.getRegistry().lookupService(umoName);

        if (umoSrvc == null) {
            throw (GridSpiException)new GridSpiException("Failed to get Mule UMO component with name: " + umoName).setData(750, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        try {
            assert umoSrvc.getComponent() instanceof JavaComponent == true : "Invalid component type: " +
                (umoSrvc.getComponent() == null ? null : umoSrvc.getComponent().getClass().getName());

            Object instance = ((JavaComponent)umoSrvc.getComponent()).getObjectFactory().getInstance();

            if (instance instanceof GridMuleDiscoveryComponent == false) {
                throw (GridSpiException)new GridSpiException("Failed to get component instance (invalid type): " +
                (instance == null ? null : instance.getClass().getName())).setData(760, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
            }

            discoUmo = (GridMuleDiscoveryComponent)instance;
        }
        catch (Exception e) {
            throw (GridSpiException)new GridSpiException("Failed to get component instance to check component type.", e).setData(767, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        InboundRouterCollection inRouters = umoSrvc.getInboundRouter();

        if (inRouters == null) {
            throw (GridSpiException)new GridSpiException("Failed to get inbound router.").setData(773, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        List<InboundEndpoint> endpoints = inRouters.getEndpoints();

        if (endpoints == null || endpoints.size() == 0) {
            throw (GridSpiException)new GridSpiException("Failed to get inbound endpoints.").setData(779, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        Map props = discoUmo.getProperties();

        if (props == null || props.isEmpty() == true || props.get(HANDSHAKE_ENDPOINT_NAME) == null ||
            props.get(HEARTBEAT_ENDPOINT_NAME) == null) {
            throw (GridSpiException)new GridSpiException("Failed to get discovery inbound endpoint identifiers.").setData(786, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        // Get handshake inbound endpoint by name.
        InboundEndpoint inEndpoint = inRouters.getEndpoint((String)props.get(HANDSHAKE_ENDPOINT_NAME));

        if (inEndpoint == null) {
            throw (GridSpiException)new GridSpiException("Failed to get discovery handshake inbound endpoint by name: " +
                props.get(HANDSHAKE_ENDPOINT_NAME)).setData(793, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        handshakeUri = inEndpoint.getEndpointURI().toString();

        // Get heartbeat inbound endpoint by name.
        inEndpoint = inRouters.getEndpoint((String)props.get(HEARTBEAT_ENDPOINT_NAME));

        if (inEndpoint == null) {
            throw (GridSpiException)new GridSpiException("Failed to get discovery heartbeat inbound endpoint by name: " +
                props.get(HEARTBEAT_ENDPOINT_NAME)).setData(803, "src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java");
        }

        heartbeatUri = inEndpoint.getEndpointURI().toString();

        // Add SPI listener in component.
        discoUmo.setListener(new GridMuleDiscoveryComponentListener() {
            /**
             * {@inheritDoc}
             */
            public void onMessage(byte[] msg) {
                try {
                    Object obj = GridMarshalHelper.unmarshal(marshaller, new GridByteArrayList(msg),
                        getClass().getClassLoader());

                    if (obj instanceof GridMuleDiscoveryHeartbeat == true) {
                        processHeartbeat((GridMuleDiscoveryHeartbeat)obj);
                    }
                    else if (obj instanceof GridMuleDiscoveryMessage == true) {
                        processMessage((GridMuleDiscoveryMessage)obj);
                    }
                    else {
                        log.warning("Received unknown message [msg=" + obj + ']');
                    }
                }
                catch (MuleException e) {
                    log.error("Failed to receive Mule message.", e);
                }
                catch (GridException e) {
                    log.error("Failed to handle Mule discovery message [msg=" +
                        GridUtils.byteArray2HexString(msg) + ']', e);
                }
            }
        });
    }

    /**
     * Method is called when any discovery event occurs. It calls external listener.
     *
     * @param type Discovery event type.
     * @param node Remote node this event is connected with.
     */
    private void notifyDiscovery(GridDiscoveryEventType type, GridMuleDiscoveryNode node) {
        assert type != null : "ASSERTION [line=847, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";
        assert node != null : "ASSERTION [line=848, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

        if (node.getState().equals(GridMuleDiscoveryNodeState.NEW) == false) {
            GridDiscoveryListener localCopy = listener;

            if (localCopy != null) {
                localCopy.onDiscovery(type, node);
            }
        }
    }

    /**
     * Handle heartbeat message.
     *
     * @param beat Heartbeat message.
     * @throws MuleException Thrown in case of any Mule error.
     * @throws GridException Thrown in case of any error.
     */
    private void processHeartbeat(GridMuleDiscoveryHeartbeat beat) throws MuleException, GridException {
        assert beat != null : "ASSERTION [line=867, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Received Mule discovery heartbeat: " + beat);
        }

        boolean isFirstReq = false;

        GridMuleDiscoveryNode node = null;

        if (nodeId.equals(beat.getNodeId()) == true) {
            if (log.isDebugEnabled() == true) {
                log.debug("Received heartbeat from itself: " + beat);
            }

            notifyDiscovery(GridDiscoveryEventType.METRICS_UPDATED, locNode);

            return;
        }

        GridMuleDiscoveryNode beatNode = null;

        boolean notifyLeave = false;

        synchronized (mux) {
            if (log.isDebugEnabled() == true) {
                log.debug("Received remote heartbeat: " + beat);
            }

            node = allNodes.get(beat.getNodeId());

            if (beat.isLeaving() == true) {
                if (node != null && node.getState() == GridMuleDiscoveryNodeState.READY) {
                    node.setState(GridMuleDiscoveryNodeState.LEFT);

                    node.onHeartbeat(beat.getMetrics());

                    beatNode = node;

                    rmtNodes = null;

                    notifyLeave = true;
                }
                else {
                    return;
                }
            }
            else {
                if (node == null) {
                    allNodes.put(beat.getNodeId(), node = new GridMuleDiscoveryNode(beat.getNodeId(),
                        beat.getStartTime(), GridMuleDiscoveryNodeState.NEW, beat.getHandshakeUri(), beat.getMetrics()));

                    node.onHeartbeat(beat.getMetrics());

                    beatNode = node;

                    isFirstReq = true;
                }
                else if (node.getState() == GridMuleDiscoveryNodeState.READY) {
                    // New heartbeat callback.
                    node.onHeartbeat(beat.getMetrics());

                    beatNode = node;
                }
                // If zombie.
                else if (node.getState() == GridMuleDiscoveryNodeState.LEFT) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received zombie heartbeat for left node (will ignore): " + node);
                    }

                    return;
                }
                // If duplicate node ID.
                else if (node.getStartTime() < beat.getStartTime()) {
                    log.warning("Node with duplicate node ID is trying to join (will ignore): " +
                        node.getId());

                    return;
                }
            }
        }

        if (notifyLeave == true) {
            notifyDiscovery(GridDiscoveryEventType.LEFT, node);

            if (log.isDebugEnabled() == true) {
                log.debug("Node left grid: " + node);
            }
        }

        // Metrics updated callback.
        if (beatNode != null) {
            notifyDiscovery(GridDiscoveryEventType.METRICS_UPDATED, beatNode);
        }

        if (node.getState() == GridMuleDiscoveryNodeState.NEW && isFirstReq == true) {
            GridMuleDiscoveryMessage req = new GridMuleDiscoveryMessage(nodeId, node.getId(),
                GridMuleDiscoveryMessageType.HANDSHAKE_REQUEST, locNode.getAttributes(), locNode.getStartTime(),
                handshakeUri, metricsProvider.getMetrics());

            muleClient.send(beat.getHandshakeUri(), GridMarshalHelper.marshal(marshaller, req).getArray(), null);

            if (log.isDebugEnabled() == true) {
                log.debug("Sent handshake request [uri=" + beat.getHandshakeUri() + ", node=" + node + ", request="
                    + req + ']');
            }
        }
    }

    /**
     * Handle message with node attributes.
     *
     * @param msg Node message with attributes.
     * @throws GridException Thrown in case of any error.
     * @throws MuleException Thrown in case of any Mule error.
     */
    private void processMessage(GridMuleDiscoveryMessage msg) throws GridException, MuleException {
        assert msg != null : "ASSERTION [line=984, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

        // Ignore messages from itself.
        if (nodeId.equals(msg.getFromId()) == true) {
            return;
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Received Mule discovery message: " + msg);
        }

        switch (msg.getType()) {
            case HANDSHAKE_REQUEST: {
                GridMuleDiscoveryNode node = null;

                boolean notify = false;

                synchronized (mux) {
                    node = allNodes.get(msg.getFromId());

                    if (node == null) {
                        allNodes.put(msg.getFromId(), node = new GridMuleDiscoveryNode(msg.getFromId(),
                            msg.getStartTime(), GridMuleDiscoveryNodeState.READY, msg.getAttributes(),
                            msg.getHandshakeUri(), msg.getMetrics()));

                        node.onHeartbeat(msg.getMetrics());

                        rmtNodes = null;

                        notify = true;
                    }
                    else if (node.getState() == GridMuleDiscoveryNodeState.LEFT) {
                        if (node.getStartTime() < msg.getStartTime()) {
                            allNodes.put(msg.getFromId(), node = new GridMuleDiscoveryNode(msg.getFromId(),
                                msg.getStartTime(), GridMuleDiscoveryNodeState.READY, msg.getAttributes(),
                                msg.getHandshakeUri(), msg.getMetrics()));

                            node.onHeartbeat(msg.getMetrics());

                            rmtNodes = null;

                            notify = true;
                        }
                        else {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Received handshake request from left node [node=" + node + ", msg="
                                    + msg + ']');
                            }
                        }
                    }
                    else if (node.getState() == GridMuleDiscoveryNodeState.NEW) {
                        node.setAttributes(msg.getAttributes());
                        node.setState(GridMuleDiscoveryNodeState.READY);

                        node.onHeartbeat(msg.getMetrics());

                        rmtNodes = null;

                        notify = true;
                    }
                }

                // Notify outside the synchronization.
                if (notify == true) {
                    notifyDiscovery(GridDiscoveryEventType.JOINED, node);
                }

                GridMuleDiscoveryMessage res = new GridMuleDiscoveryMessage(nodeId, msg.getFromId(),
                    GridMuleDiscoveryMessageType.HANDSHAKE_RESPONSE, locNode.getAttributes(), locNode.getStartTime(),
                    handshakeUri, metricsProvider.getMetrics());

                muleClient.dispatch(msg.getHandshakeUri(), GridMarshalHelper.marshal(marshaller, res).getArray(), null);

                if (log.isDebugEnabled() == true) {
                    log.debug("Sent handshake response: " + res);
                }

                break;
            }

            case HANDSHAKE_RESPONSE: {
                GridMuleDiscoveryNode node = null;

                boolean notify = false;

                synchronized (mux) {
                    node = allNodes.get(msg.getFromId());

                    if (node != null && node.getState() == GridMuleDiscoveryNodeState.NEW &&
                        node.getStartTime() == msg.getStartTime()) {
                        node.setAttributes(msg.getAttributes());
                        node.setState(GridMuleDiscoveryNodeState.READY);

                        node.onHeartbeat(msg.getMetrics());

                        rmtNodes = null;

                        notify = true;
                    }
                }

                if (notify == true) {
                    assert node != null : "ASSERTION [line=1086, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

                    notifyDiscovery(GridDiscoveryEventType.JOINED, node);
                }

                break;
            }

            case PING_REQUEST: {
                GridMuleDiscoveryNode node = null;

                synchronized (mux) {
                    node = allNodes.get(msg.getFromId());
                }

                if (node != null) {
                    GridMuleDiscoveryMessage res = new GridMuleDiscoveryMessage(nodeId, msg.getFromId(),
                        GridMuleDiscoveryMessageType.PING_RESPONSE, null, locNode.getStartTime(),
                        handshakeUri, null);

                    muleClient.dispatch(msg.getHandshakeUri(), GridMarshalHelper.marshal(marshaller, res).getArray(),
                        null);

                    if (log.isDebugEnabled() == true) {
                        log.debug("Sent ping response: " + res);
                    }
                }

                break;
            }

            case PING_RESPONSE: {
                synchronized (mux) {
                    // Remove all threads waiting for ping response from this node.
                    for (Iterator<UUID> iter = pingThreads.values().iterator(); iter.hasNext() == true;) {
                        if (msg.getFromId().equals(iter.next()) == true) {
                            iter.remove();
                        }
                    }

                    // Notify threads waiting for ping response.
                    mux.notifyAll();
                }

                break;
            }

            default: {
                assert false : "ASSERTION [line=1134, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]. " + "Received message with unknown message type [msg=" + msg + ']';
            }
        }
    }

    /**
     * Heartbeat sender.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class HeartbeatSender extends GridSpiThread {
        /** */
        private final String uri;

        /**
         * Creates new instance of message receiver.
         *
         * @param uri Mule URI.
         */
        HeartbeatSender(String uri) {
            super(gridName, "grid-mule-disco-beat-sender", log);

            assert uri != null : "ASSERTION [line=1157, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";
            assert muleClient != null : "ASSERTION [line=1158, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoverySpi.java]";

            this.uri = uri;

            if (log.isDebugEnabled() == true) {
                log.debug("Created sender instance: " + uri);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            GridMuleDiscoveryHeartbeat beat = new GridMuleDiscoveryHeartbeat(nodeId, handshakeUri,
                locNode.getStartTime());

            try {
                while (isInterrupted() == false) {
                    beat.setMetrics(metricsProvider.getMetrics());

                    sendHeartbeat(beat);

                    Thread.sleep(beatFreq);
                }
            }
            finally {
                // We left grid.
                beat.setLeaving(true);

                for (int i = 0; i < leaveAttempts; i++) {
                    sendHeartbeat(beat);
                }
            }
        }

        /**
         * Send heartbeat.
         *
         * @param beat Message for sending.
         */
        private void sendHeartbeat(GridMuleDiscoveryHeartbeat beat) {
            try {
                muleClient.dispatch(uri, GridMarshalHelper.marshal(marshaller, beat).getArray(), null);
            }
            catch (MuleException e) {
                log.error("Failed to send heart beat.", e);
            }
            catch (GridException e) {
                log.error("Failed to send heart beat.", e);
            }
        }
    }

    /**
     * Thread remove invalid nodes from nodes map.
     */
    private class NodeSweeper extends GridSpiThread {
        /**
         * Creates new instance of node sweeper.
         */
        NodeSweeper() {
            super(gridName, "grid-mule-disco-node-sweeper", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            final long maxSilenceTime = beatFreq * maxMissedBeats;

            List<GridMuleDiscoveryNode> failedNodes = new ArrayList<GridMuleDiscoveryNode>();

            while (isInterrupted() == false) {
                synchronized (mux) {
                    for (Iterator<GridMuleDiscoveryNode> iter = allNodes.values().iterator(); iter.hasNext() == true;) {
                        GridMuleDiscoveryNode node = iter.next();

                        // Check if node needs to be removed from topology.
                        if (System.currentTimeMillis() - node.getLastHeartbeat() > maxSilenceTime) {
                            if (node.getState() != GridMuleDiscoveryNodeState.LEFT) {
                                if (log.isDebugEnabled() == true) {
                                    log.debug("Removed failed node from topology: " + node);
                                }

                                node.setState(GridMuleDiscoveryNodeState.LEFT);

                                rmtNodes = null;

                                failedNodes.add(node);
                            }

                            if (log.isDebugEnabled() == true) {
                                log.debug("Removed node from topology cache: " + node);
                            }

                            iter.remove();
                        }
                    }
                }

                // Notify outside the synchronization.
                for (GridMuleDiscoveryNode node: failedNodes) {
                    // Notify listener of failure.
                    notifyDiscovery(GridDiscoveryEventType.FAILED, node);
                }

                failedNodes.clear();

                Thread.sleep(beatFreq);
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
        return GridToStringBuilder.toString(GridMuleDiscoverySpi.class, this);
    }
}
