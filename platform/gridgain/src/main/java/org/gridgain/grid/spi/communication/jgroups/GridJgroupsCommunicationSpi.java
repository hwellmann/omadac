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

package org.gridgain.grid.spi.communication.jgroups;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.stack.*;

/**
 * JGroups implementation of {@link GridCommunicationSpi} SPI. It uses JGroups
 * to communicate with one or more other nodes.
 * <p>
 * This SPI has no mandatory parameters.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has the following optional configuration parameters:
 * <ul>
 * <li>JGroups configuration file (see {@link #setConfigurationFile(String)},
 * {@link #setConfigurationUrl(URL)}).</li>
 * <li>
 * Join timeout to wait for replies from other nodes at startup
 * (see {@link #setSendTimeout(long)}).
 * </li>
 * <li>JGroups service id name (see {@link #setGroupName(String)}).</li>
 * <li>JGroups stack name (see {@link #setStackName(String)}).</li>
 * </ul>
 * Note, if you have an OS with IPv6 enabled, Java applications may try to route
 * IP multicast traffic over IPv6. Use "-Djava.net.preferIPv4Stack=true" system
 * property at VM startup to prevent this. You may also wish to specify local bind
 * address in JGroups configuration file to make sure that JGroups binds to correct
 * network interface.
 * <p>
 * <h2 class="header">Java Example</h2>
 * GridJgroupsCommunicationSpi needs to be explicitely configured to override default
 * TCP communication SPI.
 * <pre name="code" class="java">
 * GridJgroupsCommunicationSpi commSpi = new GridJgroupsCommunicationSpi();
 *
 * // Override default JGroups configuration file.
 * commSpi.setConfigurationFile("/my/config/path/jgroups.xml");
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
 * GridJgroupsCommunicationSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="communicationSpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.communication.jgroups.GridJgroupsCommunicationSpi"&gt;
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
 * @see GridCommunicationSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridJgroupsCommunicationSpi extends GridSpiAdapter implements GridCommunicationSpi,
    GridJgroupsCommunicationSpiMBean {
    /**
     * Default JGroups group name (value is <tt>grid.comm.jgroups</tt>).
     */
    public static final String DFLT_GRP_NAME = "grid.comm.jgroups";

    /**
     * Name of address attribute added to local node attributes at startup
     * (value is <tt>grid.comm.jgroups.address</tt>).
     */
    static final String ATTR_ADDR = "grid.comm.jgroups.address";

    /**
     * Default JGroups stack name (value is <tt>grid.jgroups.stack</tt>).
     */
    public static final String DFLT_STACK_NAME = "grid.jgroups.stack";

    /**
     * Default JGroups configuration path relative to GridGain installation home folder.
     * (value is <tt>config/jgroups/multicast/jgroups.xml</tt>).
     */
    public static final String DFLT_CONFIG_FILE = "config/jgroups/multicast/jgroups.xml";

    /**
     * Default timeout for message acknowledgments
     * (value is <tt>10000</tt>).
     */
    public static final long DFLT_SEND_TIMEOUT = 10000;

    /**
     * JGroups multiplexor channel name (value is <tt>grid.jgroups.mplex.channel</tt>).
     * Name is fixed to use multiplexor over the same channel.
     */
    private static final String CHNL_NAME = "grid.jgroups.mplex.channel";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    private JChannelFactory factory = null;

    /** */
    private Channel channel = null;

    /** */
    private MessageDispatcher dispatcher = null;

    /** */
    private GridMessageListener listener = null;

    /** IoC configuration parameter to specify the name of the JGroups configuration file. */
    private String cfgFile = DFLT_CONFIG_FILE;

    /** IoC configuration parameter to specify the URL of the JGroups configuration file. */
    private URL cfgUrl = null;

    /** Timeout for getting an acknowledgment for a message. */
    private long sendTimeout = DFLT_SEND_TIMEOUT;

    /** IoC configuration parameter to specify the JGroups channel group name. */
    private String grpName = DFLT_GRP_NAME;

    /** IoC configuration parameter to specify the JGroups stack name. */
    private String stackName = DFLT_STACK_NAME;

    /** */
    private File cfgPath = null;

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
     * {@inheritDoc}
     */
    public String getConfigurationFile() {
        return cfgFile;
    }

    /**
     * {@inheritDoc}
     */
    public InetAddress getLocalAddress() {
        return ((IpAddress)channel.getLocalAddress()).getIpAddress();
    }

    /**
     * {@inheritDoc}
     */
    public int getLocalPort() {
        return ((IpAddress)channel.getLocalAddress()).getPort();
    }

    /**
     * Sets time limit in milliseconds to wait for message acknowledgements
     * from remote nodes. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_SEND_TIMEOUT}.
     *
     * @param sendTimeout Timeout to wait for responses.
     */
    @GridSpiConfiguration(optional = true)
    public void setSendTimeout(long sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public long getSendTimeout() {
        return sendTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public String getGroupName() {
        return grpName;
    }

    /**
     * Sets JGroups group name. In order to communicate with
     * each other, nodes must have the same group name.
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
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        assertParameter(grpName != null, "grpName != null");
        assertParameter(sendTimeout > 0, "sendTimeout > 0");

        if (cfgUrl != null) {
            try {
                cfgPath = GridUrlHelper.downloadUrl(cfgUrl, File.createTempFile("jgroups", "xml"));
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("Failed to download configuration file: " + cfgUrl, e).setData(306, "src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java");
            }
        }
        else {
            assertParameter(cfgFile != null, "cfgFile != null");

            cfgPath = GridUtils.resolveGridGainPath(cfgFile);
        }

        if (cfgPath == null || cfgPath.isDirectory() == true) {
            throw (GridSpiException)new GridSpiException("Invalid JGroups configuration file path: " + cfgPath).setData(316, "src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java");
        }

        if (cfgPath.canRead() == false) {
            throw (GridSpiException)new GridSpiException("JGroups configuration file does not have read permission: " + cfgPath).setData(320, "src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java");
        }

        // Create and connect channel for correct invocation of channel.getLocalAddress()
        createChannel();

        // Set address and port as local node attributes.
        // Set user local node attributes to be broadcasted to all nodes.
        return GridUtils.makeMap(createSpiAttributeName(ATTR_ADDR), channel.getLocalAddress());
    }

    /**
     * Creates new channel.
     *
     * @throws GridSpiException Thrown
     */
    private void createChannel() throws GridSpiException {
        try {
            factory = new JChannelFactory();

            factory.setDomain(GridUtils.JMX_DOMAIN);
            factory.setExposeChannels(true);
            factory.setExposeProtocols(false);
            factory.setMultiplexerConfig(cfgPath);

            channel = factory.createMultiplexerChannel(stackName, grpName);
        }
        catch (Exception e) {
            throw (GridSpiException)new GridSpiException("Failed to create JGroups channel from configuration file: " + cfgPath, e).setData(348, "src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java");
        }

        try {
            channel.connect(CHNL_NAME);
        }
        catch (ChannelException e) {
            closeChannel();

            throw (GridSpiException)new GridSpiException("Failed to connect to JGroups channel: " + CHNL_NAME, e).setData(357, "src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        // Don't create channel as it is already created in getNodeAttributes()
        if (sendTimeout < 1000) {
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

            log.info(configInfo("grpName", grpName));
            log.info(configInfo("sendTimeout", sendTimeout));
        }

        registerMBean(gridName, this, GridJgroupsCommunicationSpiMBean.class);

        if (log.isDebugEnabled() ==true) {
            log.debug("Successfully connected to the channel: " + CHNL_NAME);
        }

        dispatcher = new MessageDispatcher(channel, null, null, new RequestHandler() {
            /**
             * {@inheritDoc}
             */
            public Object handle(Message msg) {
                if (msg.getObject() instanceof GridJgroupsCommunicationMessage == false) {
                    log.error("Received unknown message: " + msg.getObject());

                    return null;
                }

                // Pass received message to listeners.
                GridJgroupsCommunicationMessage gridMsg = (GridJgroupsCommunicationMessage)msg.getObject();

                if (log.isDebugEnabled() == true) {
                    log.debug("Received grid jgroups communication message: " + gridMsg);
                }

                notifyListener(gridMsg);

                // No reply.
                return null;
            }
        });

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     *
     * @param gridMsg Communication message.
     */
    private void notifyListener(GridJgroupsCommunicationMessage gridMsg) {
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
        closeChannel();

        if (dispatcher != null) {
            dispatcher.stop();
        }

        unregisterMBean();

        // Clean resources.
        channel = null;
        dispatcher = null;

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * Close channel.
     */
    private void closeChannel() {
        if (channel != null && channel.isOpen() == true) {
            channel.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass", "UseOfObsoleteCollectionType"})
    public void sendMessage(GridNode destNode, Serializable msg) throws GridSpiException {
        assert destNode != null : "ASSERTION [line=476, file=src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=477, file=src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java]";

        // Node address is placed into node attributes on startup.
        IpAddress ipAddr = (IpAddress)destNode.getAttribute(createSpiAttributeName(ATTR_ADDR));

        // Sanity check.
        if (ipAddr == null) {
            throw (GridSpiException)new GridSpiException("Failed to send message to the destination node. Node does not have" +
                " IP address. Check configuration and make sure that you use the same communication SPI" +
                " on all nodes. Remote node id: " + destNode.getId()).setData(484, "src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java");
        }

        // Local node shortcut
        if (nodeId.equals(destNode.getId()) == true) {
            notifyListener(new GridJgroupsCommunicationMessage(nodeId, msg));
        }
        else {
            Vector<IpAddress> dests = new Vector<IpAddress>(1); // JGroups ugliness as far as Vector...

            dests.add(ipAddr);

            // Send the message to the destination.
            dispatcher.castMessage(dests, new Message(ipAddr, channel.getLocalAddress(),
                new GridJgroupsCommunicationMessage(nodeId, msg)), GroupRequest.GET_NONE, sendTimeout);
        }

        if (log.isDebugEnabled() ==true) {
            log.debug("Message sent [node=" + ipAddr + ", msg=" + msg + ']');
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass", "UseOfObsoleteCollectionType"})
    public void sendMessage(Collection<GridNode> destNodes, Serializable msg) throws GridSpiException {
        assert destNodes != null : "ASSERTION [line=513, file=src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=514, file=src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java]";
        assert destNodes.size() != 0 : "ASSERTION [line=515, file=src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java]";

        Vector<IpAddress> dests = new Vector<IpAddress>(destNodes.size()); // JGroups ugliness as far as Vector...

        // Loop through the nodes list creating address and putting it to the vector of destinations.
        for (GridNode node : destNodes) {
            // Local node shortcut
            if (node.getId().equals(nodeId) == true) {
                notifyListener(new GridJgroupsCommunicationMessage(nodeId, msg));
            }
            else {
                IpAddress ipAddr = (IpAddress)node.getAttribute(createSpiAttributeName(ATTR_ADDR));

                // Sanity check.
                if (ipAddr == null) {
                    throw (GridSpiException)new GridSpiException("Failed to send message to the destination node. Node does not have" +
                        " IP address. Check configuration and make sure that you use the same communication SPI" +
                        " on all nodes. Remote node id: " + node.getId()).setData(530, "src/java/org/gridgain/grid/spi/communication/jgroups/GridJgroupsCommunicationSpi.java");
                }

                dests.add(ipAddr);
            }
        }

        if (dests.isEmpty() == false) {
            // Send the message to the destinations contained in the vector.
            dispatcher.castMessage(dests, new Message(null, channel.getLocalAddress(),
                new GridJgroupsCommunicationMessage(nodeId, msg)), GroupRequest.GET_NONE, sendTimeout);
        }

        if (log.isDebugEnabled() ==true) {
            log.debug("Message sent [nodes=" + destNodes + ", msg=" + msg + ']');
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridMessageListener listener) {
        this.listener = listener;
    }

    /**
     * This method is used exclusively for testing.
     *
     * @return JGroups channel used for communication.
     */
    Channel getChannel() {
        return channel;
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
        return GridToStringBuilder.toString(GridJgroupsCommunicationSpi.class, this);
    }
}
