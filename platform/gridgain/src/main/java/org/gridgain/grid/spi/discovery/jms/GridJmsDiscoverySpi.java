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

package org.gridgain.grid.spi.discovery.jms;

import static org.gridgain.grid.GridDiscoveryEventType.*;
import static org.gridgain.grid.spi.discovery.jms.GridJmsDiscoveryMessageType.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.jms.*;
import javax.naming.*;

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.jms.*;
import org.gridgain.grid.util.runnable.*;
import org.gridgain.grid.util.tostring.*;

/**
 * JMS implementation of {@link GridDiscoverySpi}. This is a topic-based
 * implementation. Each node periodically sends <tt>JOIN_GRID</tt> heartbeat
 * message to notify the others that it's still alive. When node leaves cluster
 * it sends <tt>LEAVE_GRID</tt> message.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2>Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2>Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>JNDI environment (see {@link #setJndiEnvironment(Map)})</li>
 * <li>JMS connection factory name (see {@link #setConnectionFactoryName(String)})</li>
 * <li>JMS connection factory (see {@link #setConnectionFactory(ConnectionFactory)})</li>
 * <li>User name (see {@link #setUser(String)})</li>
 * <li>User password (see {@link #setPassword(String)})</li>
 * <li>Heartbeat messages frequency (see {@link #setHeartbeatFrequency(long)})</li>
 * <li>Maximum number of handshake threads (see {@link #setMaximumHandshakeThreads(int)})</li>
 * <li>Number of missed  heartbeat messages (see {@link #setMaximumMissedHeartbeats(long)})</li>
 * <li>Time to live (see {@link #setTimeToLive(long)})</li>
 * <li>Handshake timeout (see {@link #setHandshakeWaitTime(long)})</li>
 * <li>Ping timeout (see {@link #setPingWaitTime(long)})</li>
 * <li>Topic name (see {@link #setTopicName(String)})</li>
 * <li>Topic (see {@link #setTopic(Topic)})</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridJmsDiscoverySpi needs to be explicitly configured to override default Multicast discovery SPI.
 * <pre name="code" class="java">
 * GridJmsDiscoverySpi spi = new GridJmsDiscoverySpi();
 *
 * // JNDI connection factory name.
 * spi.setConnectionFactoryName("java:ConnectionFactory");
 *
 * // JNDI environment mandatory parameter.
 * Map&lt;Object, Object&gt; env = new Hashtable&lt;Object, Object&gt;(3);
 *
 * env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
 * env.put(Context.PROVIDER_URL, "jnp://localhost: * *");
 * env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
 *
 * spi.setJndiEnvironment(env);
 *
 * // JNDI topic name.
 * spi.setTopicName("topic/myjmstopic");
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
 * GridJmsDiscoverySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.discovery.jms.GridJmsDiscoverySpi"&gt;
 *                 &lt;property name="connectionFactoryName" value="java:ConnectionFactory"/&gt;
 *                 &lt;property name="topicName" value="topic/myjmstopic"/&gt;
 *                 &lt;property name="jndiEnvironment"&gt;
 *                     &lt;map&gt;
 *                         &lt;entry&gt;
 *                             &lt;key&gt;&lt;util:constant static-field="javax.naming.Context.INITIAL_CONTEXT_FACTORY"/&gt;&lt;/key&gt;
 *                             &lt;value&gt;org.jnp.interfaces.NamingContextFactory&lt;/value&gt;
 *                         &lt;/entry&gt;
 *                         &lt;entry&gt;
 *                             &lt;key&gt;&lt;util:constant static-field="javax.naming.Context.PROVIDER_URL"/&gt;&lt;/key&gt;
 *                             &lt;value&gt;jnp://localhost:1099&lt;/value&gt;
 *                         &lt;/entry&gt;
 *                         &lt;entry&gt;
 *                             &lt;key&gt;&lt;util:constant static-field="javax.naming.Context.URL_PKG_PREFIXES"/&gt;&lt;/key&gt;
 *                             &lt;value&gt;org.jboss.naming:org.jnp.interfaces&lt;/value&gt;
 *                         &lt;/entry&gt;
 *                     &lt;/map&gt;
 *                 &lt;/property&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <b>Note</b>: JMS provider is not shipped with GridGain. If you don't have JMS, you need to
 * download it separately. To download JMS provider see <a target=_blank
 * href="http://en.wikipedia.org/wiki/Java_Message_Service#JMS_Provider_Implementations">http://en.wikipedia.org/wiki/Java_Message_Service#JMS_Provider_Implementations</a>
 * for more details. Once installed, JMS provider should be available on the classpath for
 * GridGain. If you use <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> script to start
 * a grid node you can simply add JMS JARs to <tt>[GRIDGAIN_HOME]/bin/setenv.{sh|bat}</tt>
 * scripts that's used to set up class path for the main scripts.
 * <p>
 * This release of GridGain has been tested with:
 * <ul>
 * <li>JBossMQ 4.x</li>
 * <li>ActiveMQ 4.x</li>
 * <li>SunMQ 3.x</li>
 * </ul>
 * <p>
 * <b>Note</b>: When using JMS-based SPIs (communication or discovery) you cannot start
 * multiple GridGain instances in the same VM due to possible limitations of JMS providers. GridGain runtime
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
@GridSpiMultipleInstancesSupport(true)
public class GridJmsDiscoverySpi extends GridSpiAdapter implements GridDiscoverySpi, GridJmsDiscoverySpiMBean {
    /** Node selector to filter out messages for the others (value is <tt>node</tt>). */
    public static final String NODE_SELECTOR = "node";

    /** */
    private String gridName = null;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** Discovery listener. */
    private GridDiscoveryListener listener = null;

    /**
     * Discovery configuration. Connection settings, user and topic name,
     * timeouts are here.
     */
    private final GridJmsDiscoveryConfiguration cfg = new GridJmsDiscoveryConfiguration();

    /** */
    private GridJms gridJms = null;

    /** Heartbeat requests sender implementation. */
    private HeartbeatSender beatSender = null;

    /** Node sweeper implementation. */
    private NodeSweeper nodeSweeper = null;

    /** All nodes within topology even those that are not ready including local node. */
    private Map<UUID, GridJmsDiscoveryNode> allNodes = new HashMap<UUID, GridJmsDiscoveryNode>();

    /**
     * Local node attributes. Should be send back as a response on {@link
     * GridJmsDiscoveryMessageType#REQUEST_ATTRIBUTES} message.
     */
    private Map<String, Serializable> nodeAttrs = new HashMap<String, Serializable>();

    /** All remote nodes in the topology that are ready. */
    private List<GridNode> rmtNodes = null;

    /** Local node instance. */
    private GridJmsDiscoveryNode locNode = null;

    /** */
    private GridDiscoveryMetricsProvider metricsProvider = null;

    /** Synchronization mutex. */
    private final Object mux = new Object();

    /** */
    private boolean isStopping = false;

    /** Handshakes executor. */
    private ThreadPoolExecutor handshakeExec = null;

    /** Handshakes runnable pool. */
    private GridRunnablePool handshakePool = null;

    /** Message listener that handles all discovery requests. */
    private MessageListener msgListener = new MessageListener() {
        /**
         * {@inheritDoc}
         */
        public void onMessage(Message msg) {
            try {
                if (msg instanceof ObjectMessage == true) {
                    Serializable obj = ((ObjectMessage)msg).getObject();

                    if (obj instanceof GridJmsDiscoveryMessage == true) {
                        GridJmsDiscoveryMessage discoMsg = (GridJmsDiscoveryMessage)obj;

                        handleMessage(discoMsg, (TemporaryQueue)msg.getJMSReplyTo());

                        return;
                    }

                    log.warning("Received unknown message: " + obj);
                }
            }
            catch (JMSException e) {
                log.warning("Failed to read JMS discovery message.", e);
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    public String getUser() {
        return cfg.getUser();
    }

    /**
     * Sets user name which is used for connection establishing.
     * Username with <tt>null</tt> value means that no authentication will be used.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is <tt>null</tt>.
     *
     * @param user Name of the user.
     */
    @GridSpiConfiguration(optional = true)
    public void setUser(String user) {
        cfg.setUser(user);
    }

    /**
     * Sets password to establish connection with JMS server.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is <tt>null</tt>.
     *
     * @param pswd Password.
     */
    @GridSpiConfiguration(optional = true)
    public void setPassword(String pswd) {
        cfg.setPassword(pswd);
    }

    /**
     * {@inheritDoc}
     */
    public long getHeartbeatFrequency() {
        return cfg.getHeartbeatFrequency();
    }

    /**
     * Sets interval for sending heartbeat requests.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_HEARTBEAT_FREQ}.
     *
     * @param beatFreq time in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setHeartbeatFrequency(long beatFreq) {
        cfg.setHeartbeatFrequency(beatFreq);
    }

    /**
     * {@inheritDoc}
     */
    public long getMaximumMissedHeartbeats() {
        return cfg.getMaximumMissedHeartbeats();
    }

    /**
     * Sets number of heartbeat requests that could be missed until remote node
     * becomes unavailable. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_MAX_MISSED_HEARTBEATS}.
     *
     * @param maxMissedHeartbeats number of requests.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaximumMissedHeartbeats(long maxMissedHeartbeats) {
        cfg.setMaximumMissedHeartbeats(maxMissedHeartbeats);
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeToLive() {
        return cfg.getTimeToLive();
    }

    /**
     * Sets message's lifetime (in milliseconds).
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_TIME_TO_LIVE}.
     *
     * @param ttl New time-to-live value.
     */
    @GridSpiConfiguration(optional = true)
    public void setTimeToLive(long ttl) {
        cfg.setTimeToLive(ttl);
    }

    /**
     * {@inheritDoc}
     */
    public long getPingWaitTime() {
        return cfg.getPingWaitTime();
    }

    /**
     * Sets handshake timeout. When node gets heartbeat from remote node it
     * asks for the attributes. If remote node does not send them back and
     * this time is out remote node would not be added in grid.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_HANDSHAKE_WAIT_TIME}.
     *
     * @param handshakeWaitTime time in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setHandshakeWaitTime(long handshakeWaitTime) {
        cfg.setHandshakeWaitTime(handshakeWaitTime);
    }

    /**
     * {@inheritDoc}
     */
    public long getHandshakeWaitTime() {
        return cfg.getHandshakeWaitTime();
    }

    /**
     * Sets maximum number of handshake threads. This means maximum
     * number of handshakes that can be executed in parallel.
     * This configuration parameter is optional.
     * <p>
     * Note that if you expect a lot of nodes discovered each other in parallel
     * you should better set higher value. After discovery number of unused
     * threads will be shrank to 1. Typically two nodes that discover each other
     * require one thread.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_MAX_HANDSHAKE_THREADS}.
     *
     * @param maxHandshakeThreads maximum number of handshake threads.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaximumHandshakeThreads(int maxHandshakeThreads) {
        cfg.setMaximumHandshakeThreads(maxHandshakeThreads);
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumHandshakeThreads() {
        return cfg.getMaximumHandshakeThreads();
    }

    /**
     * {@inheritDoc}
     */
    public int getHandshakeActiveThreadCount() {
        assert handshakeExec != null : "ASSERTION [line=408, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getActiveCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getHandshakeTotalCompletedCount() {
        assert handshakeExec != null : "ASSERTION [line=417, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getCompletedTaskCount();
    }

    /**
     * {@inheritDoc}
     */
    public int getHandshakeQueueSize() {
        assert handshakeExec != null : "ASSERTION [line=426, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getQueue().size();
    }

    /**
     * {@inheritDoc}
     */
    public int getHandshakeCorePoolSize() {
        assert handshakeExec != null : "ASSERTION [line=435, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getCorePoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getHandshakeLargestPoolSize() {
        assert handshakeExec != null : "ASSERTION [line=444, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getLargestPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getHandshakeMaximumPoolSize() {
        assert handshakeExec != null : "ASSERTION [line=453, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getMaximumPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getHandshakePoolSize() {
        assert handshakeExec != null : "ASSERTION [line=462, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getHandshakeTotalScheduledCount() {
        assert handshakeExec != null : "ASSERTION [line=471, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        return handshakeExec.getTaskCount();
    }

    /**
     * Sets ping request timeout. When ping request is run out of this
     * time remote node is considered to be failed.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_PING_WAIT_TIME}.
     *
     * @param pingWaitTime time in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setPingWaitTime(long pingWaitTime) {
        cfg.setPingWaitTime(pingWaitTime);
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Object> getJndiEnvironment() {
        return cfg.getJndiEnvironment();
    }

    /**
     * Sets JNDI environment. Various JMS providers will require different
     * JMS environment properties. Refer to corresponding JMS provider
     * documentation for more information. This configuration parameter is
     * mandatory.
     *
     * @param jndiEnv map of naming context variables.
     */
    @GridSpiConfiguration(optional = true)
    public void setJndiEnvironment(Map<Object, Object> jndiEnv) {
        cfg.setJndiEnvironment(jndiEnv);
    }

    /**
     * {@inheritDoc}
     */
    public String getConnectionFactoryName() {
        return cfg.getConnectionFactoryName();
    }

    /**
     * Sets name of the connection factory in the JNDI tree of application
     * server which node will use to create new JMS connection.
     * This configuration parameter is optional but either
     * connection factory name and JNDI environment or connection
     * factory must be set.

     * @param factoryName Connection factory name.
     */
    @GridSpiConfiguration(optional = true)
    public void setConnectionFactoryName(String factoryName) {
        cfg.setConnectionFactoryName(factoryName);
    }

    /**
     * {@inheritDoc}
     */
    public ConnectionFactory getConnectionFactory() {
        return cfg.getConnectionFactory();
    }

    /**
     * Sets JMS connection factory.
     * This configuration parameter is optional but either
     * connection factory name and JNDI environment or connection
     * factory must be set.
     * <p>
     * There is no default value.
     *
     * @param factory JMS connection factory.
     */
    @GridSpiConfiguration(optional = true)
    public void setConnectionFactory(ConnectionFactory factory) {
        cfg.setConnectionFactory(factory);
    }

    /**
     * {@inheritDoc}
     */
    public String getTopicName() {
        return cfg.getTopicName();
    }

    /**
     * Sets name of the topic. Node uses this topic to communicate with
     * other nodes by sending broadcast/private messages.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_TOPIC_NAME}.
     *
     * @param topicName Name of the topic.
     */
    @GridSpiConfiguration(optional = true)
    public void setTopicName(String topicName) {
        cfg.setTopicName(topicName);
    }

    /**
     * {@inheritDoc}
     */
    public Topic getTopic() {
        return cfg.getTopic();
    }

    /**
     * Sets JMS topic. This configuration parameter is optional.
     * <p>
     * There is no default value.
     *
     * @param topic JMS topic name.
     */
    @GridSpiConfiguration(optional = true)
    public void setTopic(Topic topic) {
        cfg.setTopic(topic);
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
    public GridNode getLocalNode() {
        return locNode;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("cfg", cfg));
        }

        this.gridName = gridName;

        // Validate configuration.
        assertParameter(cfg.getHeartbeatFrequency() > 0, "heartbeatFrequency > 0");
        assertParameter(cfg.getMaximumMissedHeartbeats() > 0, "maximumMissedHeartbeats > 0");
        assertParameter(cfg.getTimeToLive() >= 0, "timeToLive >= 0");
        assertParameter(cfg.getPingWaitTime() > 0, "pingWaitTime > 0");
        assertParameter(cfg.getMaximumHandshakeThreads() > 0, "maximumHandshakeThreads > 0");

        // Either connection factory or connection factory name and JNDI environment
        // must be set. But not both of them.
        boolean isFactorySet = cfg.getConnectionFactoryName() != null || cfg.getConnectionFactory() != null;

        assertParameter(isFactorySet == true,
            "cfg.getConnectionFactoryName() != null || cfg.getConnectionFactory() != null");

        boolean isBothFactoriesSet = cfg.getConnectionFactoryName() != null && cfg.getConnectionFactory() != null;

        assertParameter(isBothFactoriesSet == false,
            "!(cfg.getConnectionFactoryName() != null && cfg.getConnectionFactory() != null)");

        if (cfg.getConnectionFactoryName() != null) {
            // If connection factory name is used then queue and topic must me empty.
            boolean isContextUsed = cfg.getConnectionFactoryName() != null &&
                cfg.getQueue() == null && cfg.getTopic() == null;

            assertParameter(isContextUsed == true,
                "cfg.getConnectionFactoryName() != null && cfg.getQueue() == null " +
                "&& cfg.getTopic() == null");

            assertParameter(cfg.getJndiEnvironment() != null, "cfg.getJndiEnvironment() != null");
        }
        else {
            // If connection factory is used then topic name and queue name must
            // empty and topic must be set.
            boolean isObjectUsed = cfg.getConnectionFactory() != null &&
                cfg.getQueueName() == null && cfg.getTopic() != null;

            assertParameter(isObjectUsed == true,
                "cfg.getConnectionFactory() != null && cfg.getQueueName() == null " +
                "&& cfg.getTopic() != null");
        }

        handshakeExec = new GridThreadPoolExecutorService(gridName, 1, cfg.getMaximumHandshakeThreads(), 0,
            new LinkedBlockingQueue<Runnable>());

        handshakePool = new GridRunnablePool(handshakeExec, log);

        registerMBean(gridName, this, GridJmsDiscoverySpiMBean.class);

        // Set topic listener only because all 'read from' operations are done through the topic.
        // Queue is used for sending ping message only.
        cfg.setTopicMessageListener(msgListener);

        cfg.setSelector(NODE_SELECTOR + " IS NULL OR " + NODE_SELECTOR + "=\'" + nodeId.toString() + '\'');

        cfg.setLogger(log);

        gridJms = new GridJms(gridName, cfg);

        // Initialize local node.
        try {
            locNode = new GridJmsDiscoveryNode(nodeId, GridNetworkHelper.getLocalHost().getHostAddress(),
                null, metricsProvider);
        }
        catch (IOException e) {
            throw (GridSpiException)new GridSpiException("Failed to get local host internet address.", e).setData(700, "src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java");
        }

        locNode.setAttributes(nodeAttrs);

        try {
            gridJms.start();

            beatSender = new HeartbeatSender();
            nodeSweeper = new NodeSweeper();

            beatSender.start();
            nodeSweeper.start();
        }
        catch (JMSException e) {
            throw (GridSpiException)new GridSpiException("Failed to start JMS connection.", e).setData(715, "src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java");
        }
        catch (NamingException e) {
            throw (GridSpiException)new GridSpiException("Failed to start JMS connection.", e).setData(718, "src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java");
        }
        catch (RejectedExecutionException e) {
            throw (GridSpiException)new GridSpiException("Failed to start JMS connection due to thread pool execution rejection.", e).setData(721, "src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java");
        }

        try {
            long timeout = cfg.getHeartbeatFrequency() < GridJmsDiscoveryConfiguration.DFLT_HEARTBEAT_FREQ ?
                cfg.getHeartbeatFrequency() : GridJmsDiscoveryConfiguration.DFLT_HEARTBEAT_FREQ;

            // Wait to discover other nodes.
            if (log.isInfoEnabled() == true) {
                log.info("Waiting for initial timeout to discover other nodes (" + timeout + " milliseconds)");
            }

            // Wait for others to add this node to topology.
            Thread.sleep(timeout);
        }
        catch (InterruptedException e) {
            throw (GridSpiException)new GridSpiException("Got interrupted during start.", e).setData(737, "src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java");
        }

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * @throws GridSpiException Thrown if management bean could not be unregistered.
     */
    public void spiStop() throws GridSpiException {
        synchronized (mux) {
            isStopping = true;
        }

        GridUtils.interrupt(beatSender);
        GridUtils.interrupt(nodeSweeper);

        GridUtils.join(beatSender, log);
        GridUtils.join(nodeSweeper, log);

        if (handshakePool != null) {
            handshakePool.join(false);
        }

        GridUtils.shutdownNow(getClass(), handshakeExec, log);

        if (log.isInfoEnabled() == true) {
            log.info("Local grid node has left grid topology.");
        }

        if (gridJms != null) {
            gridJms.stop();
        }

        unregisterMBean();

        // Clear resources.
        gridJms = null;
        locNode = null;
        rmtNodes = null;
        beatSender = null;
        nodeSweeper = null;
        handshakePool = null;
        handshakeExec = null;

        allNodes.clear();

        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<GridNode> getRemoteNodes() {
        synchronized (mux) {
            if (rmtNodes == null) {
                rmtNodes = new ArrayList<GridNode>(allNodes.size());

                for (GridJmsDiscoveryNode node : allNodes.values()) {
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
        assert nodeId != null : "ASSERTION [line=817, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        if (locNode.getId().equals(nodeId) == true) {
            return locNode;
        }

        synchronized (mux) {
            GridJmsDiscoveryNode node = allNodes.get(nodeId);

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
        assert nodeId != null : "ASSERTION [line=856, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        if (this.nodeId.equals(nodeId) == true) {
            return true;
        }

        GridJmsDiscoveryNode node = null;

        synchronized (mux) {
            node = allNodes.get(nodeId);

            if (node == null || node.isReady() == false) {
                return false;
            }
        }
        try {
            // Send request and wait for response.
            GridJmsDiscoveryMessage res = (GridJmsDiscoveryMessage)gridJms.requestToTopic(
                new GridJmsDiscoveryMessage(
                    REQUEST_PING,
                    this.nodeId),
                cfg.getPingWaitTime(),
                NODE_SELECTOR,
                nodeId.toString());

            if (log.isDebugEnabled() == true) {
                log.debug("Got ping reply from node [nodeId=" + nodeId + ", reply=" + res + ']');
            }

            return res != null;
        }
        catch (JMSException e) {
            log.error("Failed to send a ping request to a node ID: " + nodeId, e);

            return false;
        }
    }

    /**
     * Notifies discovery listener about event.
     *
     * @param evt Event type.
     * @param node  Node this event connected with.
     */
    private void notifyDiscovery(GridDiscoveryEventType evt, GridNode node) {
        assert evt != null : "ASSERTION [line=901, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";
        assert node != null : "ASSERTION [line=902, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

        GridDiscoveryListener listener = this.listener;

        if (listener != null) {
            listener.onDiscovery(evt, node);
        }
    }

    /**
     * Handles all messages that comes to local node.
     *
     * @param msg     Incoming message.
     * @param replyTo If <tt>not null</tt> queue local node should use to send a reply.
     */
    private void handleMessage(GridJmsDiscoveryMessage msg, TemporaryQueue replyTo) {
        if (log.isDebugEnabled() == true) {
            log.debug("Received message [msg=" + msg + ", replyTo=" + replyTo + ']');
        }

        switch (msg.getMessageType()) {
            case LEAVE_GRID: {
                synchronized (mux) {
                    GridJmsDiscoveryNode node = allNodes.get(msg.getNodeId());

                    // remove remote node from list of topology nodes.
                    if (node != null) {
                        if (nodeId.equals(msg.getNodeId()) == false) {
                            allNodes.remove(msg.getNodeId());

                            if (node.isReady() == true) {
                                if (log.isInfoEnabled() == true) {
                                    log.info("Grid node has left topology: " + node);
                                }

                                // Reset list of ready nodes.
                                rmtNodes = null;

                                if (node.isReady() == true) {
                                    notifyDiscovery(LEFT, node);
                                }
                                else {
                                    log.warning("Node had never successfully joined (will remove): " + node);
                                }
                            }
                        }
                    }
                }

                break;
            }

            // Adds node to topology nodes list, and if node is not ready,
            // requests node attributes
            case HEARTBEAT: {
                // If not local node.
                if (nodeId.equals(msg.getNodeId()) == false) {
                    GridJmsDiscoveryNode node = null;

                    synchronized (mux) {
                        node = allNodes.get(msg.getNodeId());

                        boolean isNewNode = false;

                        if (node == null) {
                            node = new GridJmsDiscoveryNode(msg.getNodeId(), msg.getAddress(), msg.getMetrics(), null);

                            allNodes.put(msg.getNodeId(), node);

                            isNewNode = true;

                            if (log.isDebugEnabled() == true) {
                                log.debug("Added NEW node to grid: " + node);
                            }
                        }

                        // Heartbeat request came from node.
                        node.onHeartbeat(msg.getMetrics());

                        if (isNewNode == true && nodeId.compareTo(node.getId()) > 0) {
                            assert node.isReady() == false : "ASSERTION [line=982, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

                            if (isStopping == false) {
                                try {
                                    // Process it in separate thread because handshake should be sync.
                                    handshakePool.execute(
                                        new HandshakeRunnable(gridName, "jms-request-attr-runnable", log, node));
                                }
                                catch (GridException e) {
                                    log.error("Failed to handshake with remote node (will remove): " + node, e);

                                    allNodes.remove(node.getId());
                                }
                            }
                        }

                        if (node.isReady() == true) {
                            notifyDiscovery(METRICS_UPDATED, node);
                        }
                    }
                }
                else {
                    // Fire METRICS_UPDATED event for the local node.
                    notifyDiscovery(METRICS_UPDATED, locNode);
                }

                break;
            }

            case REQUEST_ATTRIBUTES: {
                if (nodeId.equals(msg.getNodeId()) == false) {
                    synchronized (mux) {
                        GridJmsDiscoveryNode node = allNodes.get(msg.getNodeId());

                        // Probably unknown node requested attributes. If so register it.
                        if (node == null) {
                            node = new GridJmsDiscoveryNode(msg.getNodeId(), msg.getAddress(), msg.getMetrics(),
                                null);

                            allNodes.put(msg.getNodeId(), node);

                            if (log.isDebugEnabled() == true) {
                                log.debug("Added NEW node to grid: " + node);
                            }
                        }

                        if (node.isReady() == false) {
                            // Reset ready flag.
                            node.setAttributes(msg.getAttributes());

                            // Reset list of ready nodes.
                            rmtNodes = null;

                            notifyDiscovery(JOINED, node);
                        }
                    }

                    // Send back local node attributes (outside of synchronization).
                    try {
                        gridJms.send(
                            replyTo,
                            new GridJmsDiscoveryMessage(
                                RESULT_ATTRIBUTES,
                                nodeId,
                                nodeAttrs,
                                locNode.getPhysicalAddress(),
                                metricsProvider.getMetrics()),
                            NODE_SELECTOR,
                            msg.getNodeId().toString());
                    }
                    catch (JMSException e) {
                        log.error("Failed to send attributes to node: " + msg.getNodeId(), e);
                    }
                }

                break;
            }

            case RESULT_ATTRIBUTES: {
                if (nodeId.equals(msg.getNodeId()) == false) {
                    assert false : "ASSERTION [line=1062, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]. " + "Result attributes should be processed in temporary topic by separate thread";
                }

                break;
            }

            case REQUEST_PING: {
                if (nodeId.equals(msg.getNodeId()) == false) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received ping request: " + msg);
                    }

                    assert replyTo != null : "ASSERTION [line=1074, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

                    // Send ping response.
                    try {
                        gridJms.send(replyTo, new GridJmsDiscoveryMessage(RESPONSE_PING, nodeId));

                        if (log.isDebugEnabled() == true) {
                            log.debug("Sent ping response to node: " + msg.getNodeId());
                        }
                    }
                    catch (JMSException e) {
                        log.error("Failed to send ping response to node: " + msg.getNodeId(), e);
                    }
                }

                break;
            }

            case RESPONSE_PING: {
                if (nodeId.equals(msg.getNodeId()) == false) {
                    assert false : "Ping responses should be processed by temporary topic created in " +
                        "ping requests.";
                }

                break;
            }

            default: {
                assert false : "ASSERTION [line=1102, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]. " + "Unknown message type: " + msg.getMessageType();
            }
        }
    }

    /**
     * Heartbeat messages sender implementation. It sends <tt>JOIN_GRID</tt>
     * message in configurable interval to all nodes. When node leaves topic it sends
     * <tt>LEAVE_GRID</tt> message.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class HeartbeatSender extends GridSpiThread {
        /** */
        private final GridJmsDiscoveryMessage discoMsg;

        /** */
        private final Object senderMux = new Object();

        /** */
        private boolean interrupted = false;

        /**
         * Creates new instance of sender and initialize message with <tt>JOIN_GRID</tt> one.
         */
        HeartbeatSender() {
            super(gridName, "grid-jmsdisco-heartbeat-sender", log);

            discoMsg = new GridJmsDiscoveryMessage(HEARTBEAT, nodeId, null, locNode.getPhysicalAddress(),
                metricsProvider.getMetrics());
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("UnusedCatchParameter")
        @Override
        public void body() throws InterruptedException {
            try {
                while (true) {
                    try {
                        discoMsg.setMetrics(metricsProvider.getMetrics());

                        gridJms.sendToTopic(discoMsg);
                    }
                    catch (JMSException e) {
                        if (isInterrupted() == false) {
                            log.error("Failed to send heartbeat (will try again in " + cfg.getHeartbeatFrequency() +
                                "ms)", e);
                        }
                    }

                    synchronized (senderMux) {
                        if (interrupted == false) {
                            senderMux.wait(cfg.getHeartbeatFrequency());
                        }

                        if (interrupted == true) {
                            break;
                        }
                    }
                }
            }
            // Do one more iteration to allow a leave request to be sent.
            catch (InterruptedException e) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Heartbeat sender got cancelled: " + this);
                }
            }

            // We left grid.
            discoMsg.setMessageType(LEAVE_GRID);

            try {
                gridJms.sendToTopic(discoMsg);
            }
            catch (JMSException e) {
                if (isInterrupted() == false) {
                    log.error("Failed to send LEFT heartbeat", e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void interrupt() {
            synchronized (senderMux) {
                interrupted = true;

                senderMux.notifyAll();
            }
        }
    }

    /**
     * Node sweeper implementation. It keeps nodes list consistent and removes
     * all failed nodes from list.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class NodeSweeper extends GridSpiThread {
        /**
         * Creates new node sweeper instance.
         */
        NodeSweeper() {
            super(gridName, "grid-jms-disco-node-sweeper", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void body() throws InterruptedException {
            while (isInterrupted() == false) {
                final long maxSilenceTime = cfg.getHeartbeatFrequency() * cfg.getMaximumMissedHeartbeats();

                final List<GridJmsDiscoveryNode> failedNodes = new ArrayList<GridJmsDiscoveryNode>();

                long curTime = System.currentTimeMillis();

                synchronized (mux) {
                    for (Iterator<GridJmsDiscoveryNode> iter = allNodes.values().iterator(); iter.hasNext() == true;) {
                        GridJmsDiscoveryNode node = iter.next();

                        // Check if node needs to be removed from topology.
                        if (curTime - node.getLastHeartbeat() > maxSilenceTime) {
                            failedNodes.add(node);

                            iter.remove();

                            rmtNodes = null;
                        }
                    }
                }

                if (failedNodes.isEmpty() == false) {
                    for (GridJmsDiscoveryNode failedNode : failedNodes) {
                        if (failedNode.isReady() == true) {
                            // Notify listener of failure.
                            notifyDiscovery(FAILED, failedNode);
                        }
                        else {
                            log.warning("Node had never successfully joined (will remove): " + failedNode);
                        }
                    }

                    failedNodes.clear();
                }

                Thread.sleep(cfg.getHeartbeatFrequency());
            }
        }
    }

    /**
     * Class processes {@link GridJmsDiscoveryMessageType#HEARTBEAT} request by exchanging attributes
     * with remote node.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    @SuppressWarnings({"NonStaticInitializer"})
    private class HandshakeRunnable extends GridRunnable {
        /** */
        private final GridJmsDiscoveryNode rmtNode;

        /**
         * Creates new runnable that processes {@link GridJmsDiscoveryMessageType#HEARTBEAT} requests.
         *
         * @param gridName Local grid name.
         * @param name Runnable name.
         * @param log  Logger.
         * @param rmtNode Remote node unique identifier.
         */
        HandshakeRunnable(String gridName, String name, GridLogger log, GridJmsDiscoveryNode rmtNode) {
            super(gridName, name, log);

            this.rmtNode = rmtNode;
        }

        /**
         * The implementation sends {@link GridJmsDiscoveryMessageType#REQUEST_ATTRIBUTES}
         * requests and processes response.
         *
         * @throws InterruptedException Thrown in case of interruption.
         */
        @Override
        protected void body() throws InterruptedException {
            if (log.isDebugEnabled() == true) {
                log.debug("Request attributes from node [node=" + rmtNode + ']');
            }

            try {
                // Request attributes.
                GridJmsDiscoveryMessage attrMsg = (GridJmsDiscoveryMessage)gridJms.requestToTopic(
                    new GridJmsDiscoveryMessage(
                        REQUEST_ATTRIBUTES,
                        nodeId,
                        nodeAttrs,
                        locNode.getPhysicalAddress(),
                        metricsProvider.getMetrics()),
                    cfg.getHandshakeWaitTime(),
                    NODE_SELECTOR,
                    rmtNode.getId().toString());

                if (attrMsg != null)  {
                    // Check if node is still not ready (this might happen if request
                    // processing took really long)
                    boolean notify = false;

                    synchronized (mux) {
                        if (rmtNode.isReady() == false && isStopping == false) {
                            Map<String, Serializable> attributes = attrMsg.getAttributes();

                            assert attributes != null : "ASSERTION [line=1320, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoverySpi.java]";

                            rmtNode.setAttributes(attributes);

                            // Reset list of ready nodes.
                            rmtNodes = null;

                            notify = true;
                        }
                    }

                    // Notify outside the synchronization.
                    if (notify == true) {
                        notifyDiscovery(JOINED, rmtNode);
                    }
                }
                // Null message means timeout.
                else {
                    log.warning("Failed to request attributes from node (timeout): " + rmtNode);

                    synchronized (mux) {
                        if (rmtNode.isReady() == false) {
                            allNodes.remove(rmtNode.getId());
                        }
                    }
                }
            }
            catch (JMSException e) {
                if (isCancelled() == false) {
                    log.error("Failed to request attributes from node: " + rmtNode, e);

                    synchronized (mux) {
                        if (rmtNode.isReady() == false) {
                            allNodes.remove(rmtNode.getId());
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
        return GridToStringBuilder.toString(GridJmsDiscoverySpi.class, this);
    }
}
