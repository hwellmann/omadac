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

package org.gridgain.grid.spi.communication.jms;

import java.io.*;
import java.util.*;
import javax.jms.*;
// Single class import is required.
import javax.jms.Queue;
import javax.naming.*;
import org.gridgain.grid.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.jms.*;
import org.gridgain.grid.util.tostring.*;

/**
 * JMS implementation of {@link GridCommunicationSpi}. This implementation uses
 * JMS <tt>topic</tt> and <tt>queue</tt> to send messages to an individual node
 * or to a group of remote nodes.
 * <p>
 * Note that <tt>queue</tt> is optional. If provided, then <tt>queue</tt> will
 * be used for sending messages to a single node
 * (method {@link #sendMessage(GridNode, Serializable)}, otherwise
 * <tt>topic</tt> will be used in which case messages will be sent to all
 * nodes, but only destination node will process them and others will ignore
 * them. <tt>Topic</tt> is always used for communication with more than one node
 * (method {@link #sendMessage(Collection, Serializable)}.
 * Both, <tt>topic</tt> and <tt>queue</tt> will be first obtained from JNDI lookup.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>JMS connection factory name (see {@link #setConnectionFactoryName(String)})</li>
 * <li>JNDI environment (see {@link #setJndiEnvironment(Map)})</li>
 * <li>JMS connection factory (see {@link #setConnectionFactory(ConnectionFactory)})</li>
 * <li>Messages delivery mode (see {@link #setDeliveryMode(int)})</li>
 * <li>Messages priority (see {@link #setPriority(int)})</li>
 * <li>Time to live (see {@link #setTimeToLive(long)})</li>
 * <li>Queue name (see {@link #setQueueName(String)})</li>
 * <li>Queue (see {@link #setQueue(Queue)})</li>
 * <li>Topic name (see {@link #setTopicName(String)})</li>
 * <li>Topic (see {@link #setTopic(Topic)})</li>
 * <li>Whether messages are transacted or not (see {@link #setTransacted(boolean)})</li>
 * <li>User name (see {@link #setUser(String)})</li>
 * <li>User password (see {@link #setPassword(String)})</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * In order to use GridJmsCommunicationSpi it needs to be explicitely configured:
 * <pre name="code" class="java">
 * GridJmsCommunicationSpi commSpi = new GridJmsCommunicationSpi();
 *
 * // JNDI connection factory name.
 * commSpi.setConnectionFactoryName("java:ConnectionFactory");
 *
 * // JNDI environment mandatory parameter.
 * Map&lt;Object, Object&gt; env = new Hashtable&lt;Object, Object&gt;(3);
 * 
 * env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
 * env.put(Context.PROVIDER_URL, "jnp://localhost:1099");
 * env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
 * 
 * commSpi.setJndiEnvironment(env);
 * 
 * // JNDI topic name.
 * commSpi.setTopicName("topic/myjmstopic");
 * 
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default communication SPI.
 * cfg.setCommunicationSpi(commSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridJmsCommunicationSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="communicationSpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.communication.jms.GridJmsCommunicationSpi"&gt;
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
 * &lt;/bean&gt;</pre>
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
 * @see GridCommunicationSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(false)
public class GridJmsCommunicationSpi extends GridSpiAdapter implements GridCommunicationSpi,
    GridJmsCommunicationSpiMBean {
    /**
     * Name of the node attribute that refers to the queue name
     * (value is <tt>grid.jms.queue.GridJmsCommunicationSpi</tt>).
     */
    public static final String ATTR_QUEUE_NAME = "grid.jms.queue." + GridJmsCommunicationSpi.class.getSimpleName();

    /** JMS communication configuration. */
    private GridJmsConfiguration cfg = new GridJmsConfiguration();

    /** Listener that will be informed about incoming messages. */
    private volatile GridMessageListener listener = null;

    /** */
    private static final String NODE_SELECTOR = "node";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    private GridJms gridJms = null;

    /**
     * {@inheritDoc}
     */
    public boolean isTransacted() {
        return cfg.isTransacted();
    }

    /**
     * Indicates whether JMS messages are transacted or not.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is <tt>false</tt>.
     *
     * @param transacted If <tt>true</tt> then session will support transactions,
     *      otherwise it will not.
     */
    @GridSpiConfiguration(optional = true)
    public void setTransacted(boolean transacted) {
        cfg.setTransacted(transacted);
    }

    /**
     * {@inheritDoc}
     */
    public int getDeliveryMode() {
        return cfg.getDeliveryMode();
    }

    /**
     * Sets message delivery mode. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link Message#DEFAULT_DELIVERY_MODE}.
     *
     * @param deliveryMode JMS delivery mode as defined in {@link DeliveryMode}.
     */
    @GridSpiConfiguration(optional = true)
    public void setDeliveryMode(int deliveryMode) {
        cfg.setDeliveryMode(deliveryMode);
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        return cfg.getPriority();
    }

    /**
     * Sets message delivery priority. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link Message#DEFAULT_PRIORITY}.
     *
     * @param priority JMS message priority as defined in {@link Message}.
     */
    @GridSpiConfiguration(optional = true)
    public void setPriority(int priority) {
        cfg.setPriority(priority);
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeToLive() {
        return cfg.getTimeToLive();
    }

    /**
     * Sets message time-to-live (in milliseconds).
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link Message#DEFAULT_TIME_TO_LIVE}.
     *
     * @param ttl Message time-to-live value.
     */
    @GridSpiConfiguration(optional = true)
    public void setTimeToLive(long ttl) {
        cfg.setTimeToLive(ttl);
    }

    /**
     * {@inheritDoc}
     */
    public String getQueueName() {
        return cfg.getQueueName();
    }

    /**
     * Sets JNDI name for JMS queue.
     * If provided, then <tt>queue</tt> will be used for node-to-node
     * communication (method {@link #sendMessage(GridNode, Serializable)}),
     * otherwise <tt>topic</tt> will be used.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is <tt>null</tt>.
     *
     * @param qName Name of JMS queue.
     */
    @GridSpiConfiguration(optional = true)
    public void setQueueName(String qName) {
        cfg.setQueueName(qName);
    }

    /**
     * {@inheritDoc}
     */
    public Queue getQueue() {
        return cfg.getQueue();
    }

    /**
     * Sets JMS queue.
     * If provided, then <tt>queue</tt> will be used for node-to-node
     * communication (method {@link #sendMessage(GridNode, Serializable)}),
     * otherwise <tt>topic</tt> will be used.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is <tt>null</tt>.
     *
     * @param queue JMS queue.
     */
    @GridSpiConfiguration(optional = true)
    public void setQueue(Queue queue) {
        cfg.setQueue(queue);
    }

    /**
     * {@inheritDoc}
     */
    public String getTopicName() {
        return cfg.getTopicName();
    }

    /**
     * Sets JNDI name for JMS topic. This configuration parameter is optional
     * but ether topic name or topic must be set.
     * <p>
     * There is no default value.
     *
     * @param tName JMS topic name.
     */
    @GridSpiConfiguration(optional = true)
    public void setTopicName(String tName) {
        cfg.setTopicName(tName);
    }

    /**
     * {@inheritDoc}
     */
    public Topic getTopic() {
        return cfg.getTopic();
    }

    /**
     * Sets JMS topic. This configuration parameter is optional
     * but ether topic name or topic must be set.
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
    public String getConnectionFactoryName() {
        return cfg.getConnectionFactoryName();
    }

    /**
     * Sets the JNDI name of JMS connection factory. This configuration
     * parameter is optional but either connection factory name
     * and JNDI environment or connection factory must be set.
     * <p>
     * There is no default value.
     *
     * @param factoryName JMS connection factory name.
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
     * Sets JMS connection factory. This configuration
     * parameter is optional but either connection factory name
     * and JNDI environment or connection factory must be set.
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
     * @param user JMS connection username.
     */
    @GridSpiConfiguration(optional = true)
    public void setUser(String user) {
        cfg.setUser(user);
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Object> getJndiEnvironment() {
        return cfg.getJndiEnvironment();
    }

    /**
     * Sets JNDI environment properties. For example for JBoss the following
     * environment parameters are required:
     * <ul>
     * <li>{@link Context#INITIAL_CONTEXT_FACTORY}</li>
     * <li>{@link Context#PROVIDER_URL}</li>
     * <li>{@link Context#URL_PKG_PREFIXES}</li>
     * </ul>
     * <p>
     * There is no default value.
     *
     * @param jndiEnv Map of naming context variables.
     */
    @GridSpiConfiguration(optional = true)
    public void setJndiEnvironment(Map<Object, Object> jndiEnv) {
        cfg.setJndiEnvironment(jndiEnv);
    }

    /**
     * {@inheritDoc}
     */
    public String getPassword() {
        return cfg.getPassword();
    }

    /**
     * Sets password to establish connection with JMS server.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is <tt>null</tt>.
     *
     * @param pswd JMS connection password.
     */
    @GridSpiConfiguration(optional = true)
    public void setPassword(String pswd) {
        cfg.setPassword(pswd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(ATTR_QUEUE_NAME), cfg.getQueueName());
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

        // Assert on parameters.
        assertParameter(cfg.getDeliveryMode() == DeliveryMode.PERSISTENT
            || cfg.getDeliveryMode() == DeliveryMode.NON_PERSISTENT,
            "deliveryMode == DeliveryMode.PERSISTENT || deliveryMode == DeliveryMode.NON_PERSISTENT");
        assertParameter(cfg.getPriority() > 0, "priority > 0");
        assertParameter(cfg.getTimeToLive() >= 0, "timeToLive >= 0");

        // Either connection factory or connection factory name and JNDI environment
        // must be set. But not both of them.
        boolean isFactorySet = cfg.getConnectionFactoryName() != null || cfg.getConnectionFactory() != null;

        assertParameter(isFactorySet == true,
            "cfg.getConnectionFactoryName() != null || cfg.getConnectionFactory() != null");

        boolean isBothFactoriesSet = cfg.getConnectionFactoryName() != null && cfg.getConnectionFactory() != null;

        assertParameter(isBothFactoriesSet == false,
            "!(cfg.getConnectionFactoryName() != null && cfg.getConnectionFactory() != null)");

        if (cfg.getConnectionFactoryName() != null) {
            // If connection factory name is used then queue and topic must me empty
            // and topic name must be set.
            boolean isContextUsed = cfg.getConnectionFactoryName() != null &&
                cfg.getQueue() == null && cfg.getTopic() == null && cfg.getTopicName() != null;

            assertParameter(isContextUsed == true,
                "cfg.getConnectionFactoryName() != null && cfg.getQueue() == null " +
                "&& cfg.getTopic() == null && cfg.getTopicName() != null");
        }
        else {
            // If connection factory is used then topic name and queue name must
            // empty and topic must be set.
            boolean isObjectUsed = cfg.getConnectionFactory() != null &&
                cfg.getQueueName() == null && cfg.getTopicName() == null && cfg.getTopic() != null;

            assertParameter(isObjectUsed == true,
                "cfg.getConnectionFactory() != null && cfg.getQueueName() == null " +
                "&& cfg.getTopicName() == null && cfg.getTopic() != null");
        }

        registerMBean(gridName, this, GridJmsCommunicationSpiMBean.class);

        // Set up selector.
        cfg.setSelector(NODE_SELECTOR + " IS NULL OR " + NODE_SELECTOR + "=\'" + nodeId + '\'');

        cfg.setLogger(log);

        // Topic listener.
        cfg.setTopicMessageListener(new MessageListener() {
            /**
             * {@inheritDoc}
             */
            public void onMessage(Message msg) {
                try {
                    GridJmsCommunicationMessage gridMsg = (GridJmsCommunicationMessage)((ObjectMessage)msg).
                        getObject();

                    for (Serializable rcvNodeId : gridMsg.getNodesIds()) {
                        if (nodeId.equals(rcvNodeId) == true) {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Received message: " + gridMsg.getMessage());
                            }

                            notifyListener(gridMsg);

                            break;
                        }
                    }
                }
                catch (JMSException e) {
                    log.error("Failed to receive message.", e);
                }
            }
        });

        // Queue listener.
        if (cfg.getQueueName() != null) {
            cfg.setQueueMessageListener(new MessageListener() {
                /**
                 * {@inheritDoc}
                 */
                public void onMessage(Message msg) {
                    try {
                        GridJmsCommunicationMessage gridMsg = (GridJmsCommunicationMessage)((ObjectMessage)msg).
                            getObject();

                        if (log.isDebugEnabled() == true) {
                            log.debug("Received message: " + gridMsg.getMessage());
                        }

                        notifyListener(gridMsg);
                    }
                    catch (JMSException e) {
                        log.error("Failed to receive message.", e);
                    }
                }
            });
        }

        gridJms = new GridJms(gridName, cfg);

        try {
            gridJms.start();
        }
        catch (JMSException e) {
            close();

            throw (GridSpiException)new GridSpiException("Failed to start JMS listeners.", e).setData(594, "src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java");
        }
        catch (NamingException e) {
            close();

            throw (GridSpiException)new GridSpiException("Failed to start JMS listeners.", e).setData(599, "src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java");
        }

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     *
     * @param gridMsg Communication message.
     */
    private void notifyListener(GridJmsCommunicationMessage gridMsg) {
        final GridMessageListener listener = this.listener;

        if (listener != null) {
            // Notify listener of a new message.
            listener.onMessage(gridMsg.getNodeId(), gridMsg.getMessage());
        }
        else {
            log.warning("Received communication message without any registered listeners (will ignore) [senderNodeId=" +
                gridMsg.getNodeId() + ']');
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        close();

        unregisterMBean();

        // Clear resources.
        gridJms = null;

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * Stops JMS implementation.
     */
    private void close() {
        if (gridJms != null) {
            gridJms.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridMessageListener listener) {
        this.listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(GridNode destNode, Serializable msg) throws GridSpiException {
        assert destNode != null : "ASSERTION [line=662, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=663, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Sending message to node [destNode=" + destNode + ", msg=" + msg + ']');
        }

        // Local node shortcut.
        if (nodeId.equals(destNode.getId()) == true) {
            notifyListener(new GridJmsCommunicationMessage(nodeId, msg, Collections.singletonList(nodeId)));
        }
        else {
            String name = (String)destNode.getAttribute(createSpiAttributeName(ATTR_QUEUE_NAME));

            // Sends to topic if queue name is empty otherwise sends to queue
            if (name == null) {
                sendMessage(Collections.singletonList(destNode), msg);

                return;
            }

            // Send to queue
            try {
                gridJms.sendToQueue(name,
                    new GridJmsCommunicationMessage(nodeId, msg, Collections.singletonList(destNode.getId())));
            }
            catch (JMSException e) {
                throw (GridSpiException)new GridSpiException("Failed to send message [destNode=" + destNode + ", msg=" + msg + ']', e).setData(689, "src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java");
            }
            catch (NamingException e) {
                throw (GridSpiException)new GridSpiException("Failed to send message [destNode=" + destNode + ", msg=" + msg + ']', e).setData(692, "src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(Collection<GridNode> destNodes, Serializable msg) throws GridSpiException {
        assert destNodes != null : "ASSERTION [line=701, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=702, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java]";
        assert destNodes.size() != 0 : "ASSERTION [line=703, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Sending message to nodes [destNodes=" + destNodes + ", msg=" + msg + ']');
        }

        List<UUID> ids = new ArrayList<UUID>(destNodes.size());

        for (GridNode node : destNodes) {
            if (nodeId.equals(node.getId()) == true) {
                notifyListener(new GridJmsCommunicationMessage(nodeId, msg, Collections.singletonList(nodeId)));
            }
            else {
                ids.add(node.getId());
            }
        }

        if (ids.isEmpty() == false) {
            try {
                gridJms.sendToTopic(new GridJmsCommunicationMessage(nodeId, msg, ids));
            }
            catch (JMSException e) {
                throw (GridSpiException)new GridSpiException("Failed to send message [destNodes=" + destNodes + ", msg=" + msg + ']', e).setData(725, "src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationSpi.java");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(2);
        
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));
        
        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJmsCommunicationSpi.class, this);
    }  
}
