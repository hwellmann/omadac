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

package org.gridgain.grid.spi.discovery.mail;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import static org.gridgain.grid.GridDiscoveryEventType.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.mail.inbox.*;
import org.gridgain.grid.util.mail.outbox.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Email implementation of {@link GridDiscoverySpi}. Email discovery is
 * provided for cases where nodes from different networks need to participate
 * in a grid. When working with email discovery, make sure that maximum send/receive
 * limit set by mail server is not exceeded. Sometimes it is better to configure
 * your own mail server to avoid such limitations.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * The following configuration parameters are mandatory:
 * <ul>
 * <li>Outgoing host (see {@link #setOutHost(String)}).</li>
 * <li>Ingoing host (see {@link #setInHost(String)}).</li>
 * <li>Address from (see {@link #setFromAddress(String)}).</li>
 * <li>Broadcast address (see {@link #setBroadcastAddress(String)}).</li>
 * </ul>
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional.
 * <ul>
 * <li>Heartbeat frequency (see {@link #setHeartbeatFrequency(long)}</li>
 * <li>Delay between messages read (see {@link #setReceiverDelay(long)}</li>
 * <li>Ping wait delay (see {@link #setPingResponseWait(long)}</li>
 * <li>Number of heartbeats sending before node leaves topology (see {@link #setMaxMissedHeartbeats(long)}</li>
 * <li>Number of milliseconds to leave messages on server (see {@link #setLeaveMessagesOnServer(long)}</li>
 * <li>Outgoing user name (see {@link #setOutUsername(String)}</li>
 * <li>Outgoing user password (see {@link #setOutPassword(String)}</li>
 * <li>Outgoing connection type (see {@link #setOutConnectionType(GridMailDiscoveryType)})</li>
 * <li>Outgoing protocol (see {@link #setOutProtocol(GridMailDiscoveryOutProtocol)} </li>
 * <li>Outgoing port (see {@link #setOutPort(int)}</li>
 * <li>Outgoing additional JavaMail properties (see {@link #setOutCustomProperties(Properties)}</li>
 * <li>Incoming connection type (see {@link #setInConnectionType(GridMailDiscoveryType)}</li>
 * <li>Incoming port (see {@link #setInPort(int)}</li>
 * <li>Incoming user name (see {@link #setInUsername(String)}</li>
 * <li>Incoming user password (see {@link #setInPassword(String)}</li>
 * <li>Incoming additional JavaMail properties (see {@link #setInCustomProperties(Properties)}</li>
 * <li>Incoming protocol (see {@link #setInProtocol(GridMailDiscoveryInProtocol)} </li>
 * <li>Mail subject (see {@link #setSubject(String)}</li>
 * <li>Mail folder name (see {@link #setFolderName(String)}</li>
 * <li>Locally stored file name (see {@link #setStoreFileName(String)} </li>
 * <li>Read messages batch size (see {@link #setReadBatchSize(int)}</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridMailDiscoverySpi needs to be explicitly configured to override default Multicast discovery SPI.
 * <pre name="code" class="java">
 * GridMailDiscoverySpi spi = new GridMailDiscoverySpi();
 *
 * // Inbox configuration.
 * spi.setInHost("pop.google.com");
 *
 * // Outbox configuration.
 * spi.setOutHost("smtp.google.com");
 *
 * // Incoming/outgoing e-mail address configuration.
 * spi.setFromAddress("grid@google.com");
 *
 * // Broadcast address.
 * spi.setBroadcastAddress("grid-broadcast@google.com")
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
 * GridMailDiscoverySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.discovery.mail.GridMailDiscoverySpi"&gt;
 *                 &lt;property name="outHost" value="smtp.google.com"/&gt;
 *                 &lt;property name="inHost" value="pop.google.com"/&gt;
 *                 &lt;property name="fromAddress" value="grid@google.com"/&gt;
 *                 &lt;property name="broadcastAddress" value="grid-broadcast@google.com"/&gt;
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
public class GridMailDiscoverySpi extends GridSpiAdapter implements GridDiscoverySpi, GridMailDiscoverySpiMBean {
    /** Default heartbeat delay (value is <tt>60000</tt>).*/
    public static final long DFLT_HEARTBEAT_FREQ = 60000;

    /** Delay in milliseconds between receiving email (value is <tt>30000</tt>). */
    public static final long DFLT_RECEIVER_DELAY = 30000;

    /** Default ping wait timeout (value is {@link #DFLT_RECEIVER_DELAY} * <tt>2</tt>). */
    public static final long DFLT_PING_WAIT = DFLT_RECEIVER_DELAY * 2;

    /** Default number of heartbeat messages that could be missed (value is <tt>3</tt>). */
    public static final int DFLT_MAX_MISSED_HEARTBEATS = 3;

    /**
     * Leave messages on server in milliseconds
     * (value is ({@link #DFLT_MAX_MISSED_HEARTBEATS} + <tt>2</tt>) * {@link #DFLT_HEARTBEAT_FREQ}).
     */
    public static final long DFLT_LEAVE_MSGS_ON_SERVER = (DFLT_MAX_MISSED_HEARTBEATS + 2) * DFLT_HEARTBEAT_FREQ;

    /** Default subject of email (value is <tt>grid.email.discovery.msg</tt>). */
    public static final String DFLT_MAIL_SUBJECT = "grid.email.discovery.msg";

    /** Default local storage file name (value is <tt>grid-email-discovery-msgs.dat</tt>). */
    public static final String DFLT_STORE_FILE_NAME = "grid-email-discovery-msgs.dat";

    /** Heartbeat attribute key should be the same on all nodes. */
    private static final String HEARTBEAT_ATTRIBUTE_KEY = "gridgain:discovery:heartbeat";

    /** Message attachment index. */
    private static final int MSG_IDX = 0;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /** Delay between heartbeat requests. */
    private long beatFreq = DFLT_HEARTBEAT_FREQ;

    /** Delay in milliseconds between receiving email. */
    private long recvDelay = DFLT_RECEIVER_DELAY;

    /** Number of heartbeat messages that could be missed before remote node is considered as failed one. */
    private long maxMissedBeats = DFLT_MAX_MISSED_HEARTBEATS;

    /** Leave messages on server in milliseconds. */
    private long leaveMsgsOnServer = DFLT_LEAVE_MSGS_ON_SERVER;

    /** Ping wait timeout. */
    private long pingWait = DFLT_PING_WAIT;

    /** Name of grid. */
    private String gridName = null;

    /** Broadcast email address. */
    private String bcastAddr = null;

    /** Inbox mail configuration. */
    private final GridMailInboxConfiguration inboxCfg = new GridMailInboxConfiguration();

    /** Outbox mail configuration.*/
    private final GridMailOutboxConfiguration outboxCfg = new GridMailOutboxConfiguration();

    /** */
    private volatile GridDiscoveryListener listener = null;

    /** */
    private MailSender mailSender = null;

    /** */
    private MailReceiver mailRcvr = null;

    /** */
    private NodeSweeper nodeSweeper = null;

    /** Map of all nodes in grid. */
    private final Map<UUID, GridMailDiscoveryNode> allNodes = new HashMap<UUID, GridMailDiscoveryNode>();

    /** Local node attributes. */
    private Map<String, Serializable> nodeAttrs = null;

    /** Set of remote nodes that have state <tt>READY</tt>. */
    private List<GridNode> rmtNodes = null;

    /** Local node. */
    private GridMailDiscoveryNode locNode = null;

    /** Mail inbox. */
    private GridMailInbox inbox = null;

    /** Mail outbox. */
    private GridMailOutbox outbox = null;

    /** */
    private final Map<Thread, UUID> pingThreads = new HashMap<Thread, UUID>();

    /** Node start time. */
    private long nodeStartTime = 0;

    /** */
    private GridDiscoveryMetricsProvider metricsProvider = null;

    /** */
    private final Object mux = new Object();

    /**
     * Creates discovery SPI.
     */
    public GridMailDiscoverySpi() {
        // Pre-set default store file name.
        inboxCfg.setStoreFileName(DFLT_STORE_FILE_NAME);

        // Pre-set default subject.
        outboxCfg.setSubject(DFLT_MAIL_SUBJECT);
    }

    /**
     * {@inheritDoc}
     */
    public long getHeartbeatFrequency() {
        return beatFreq;
    }

    /**
     * Sets delay between heartbeat requests. SPI sends broadcast messages in
     * configurable time interval to other nodes to notify them about its state.
     * <p>
     * If not provided the default value is {@link #DFLT_HEARTBEAT_FREQ}.
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
    public long getReceiverDelay() {
        return recvDelay;
    }

    /**
     * Sets interval in milliseconds between checking for new messages.
     * <p>
     * If not provided the default value is {@link #DFLT_RECEIVER_DELAY}.
     *
     * @param recvDelay Interval between receiving messages.
     */
    @GridSpiConfiguration(optional = true)
    public void setReceiverDelay(long recvDelay) {
        this.recvDelay = recvDelay;
    }


    /**
     * {@inheritDoc}
     */
    public long getPingResponseWait() {
        return pingWait;
    }

    /**
     * Sets ping node wait timeout in milliseconds.
     * <p>
     * If not provided the default value is {@link #DFLT_PING_WAIT}.
     *
     * @param pingWait Timeout in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setPingResponseWait(long pingWait) {
        this.pingWait = pingWait;
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
    public long getMaximumMissedHeartbeats() {
        return maxMissedBeats;
    }

    /**
     * Sets number of heartbeat requests that could be missed before remote
     * node is considered to be failed.
     * <p>
     * If not provided the default value is {@link #DFLT_MAX_MISSED_HEARTBEATS}.
     *
     * @param maxMissedBeats Number of missed requests.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaxMissedHeartbeats(long maxMissedBeats) {
        this.maxMissedBeats = maxMissedBeats;
    }

    /**
     * {@inheritDoc}
     */
    public long getLeaveMessagesOnServer() {
        return leaveMsgsOnServer;
    }

    /**
     * Sets incoming messages life-time on mail server in milliseconds.
     * <p>
     * If not provided the default value is {@link #DFLT_LEAVE_MSGS_ON_SERVER}.
     *
     * @param leaveMsgsOnServer Time to live for incoming messages.
     */
    @GridSpiConfiguration(optional = true)
    public void setLeaveMessagesOnServer(long leaveMsgsOnServer) {
        this.leaveMsgsOnServer = leaveMsgsOnServer;
    }

    /**
     * {@inheritDoc}
     */
    public String getOutConnectionTypeFormatted() {
        return outboxCfg.getConnectionType() == null ? "" : outboxCfg.getConnectionType().toString();
    }

    /**
     * Sets type of outgoing mail connection which should be one of the following:
     * <ul>
     * <li>{@link GridMailDiscoveryType#NONE}</li>
     * <li>{@link GridMailDiscoveryType#SSL}</li>
     * <li>{@link GridMailDiscoveryType#STARTTLS}</li>
     * </ul>
     * <p>
     * If not provided the default value is {@link GridMailDiscoveryType#NONE}.
     *
     * @param type Connection type.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutConnectionType(GridMailDiscoveryType type) {
        if (type == null) {
            outboxCfg.setConnectionType(null);

            return;
        }

        // Convert types.
        switch (type) {
            case NONE: { outboxCfg.setConnectionType(GridMailConnectionType.NONE); break; }
            case SSL: { outboxCfg.setConnectionType(GridMailConnectionType.SSL); break; }
            case STARTTLS: { outboxCfg.setConnectionType(GridMailConnectionType.STARTTLS); break; }

            default: { assert false : "Unknown mail discovery type: " + type; }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getOutProtocolFormatted() {
        return outboxCfg.getProtocol() == null ? "" : outboxCfg.getProtocol().toString();
    }

    /**
     * Sets outgoing mail protocol. Should be one of the following:
     * <ul>
     * <li>{@link GridMailDiscoveryOutProtocol#SMTP}</li>
     * <li>{@link GridMailDiscoveryOutProtocol#SMTPS}</li>
     * </ul>
     * <p>
     * If not provided the default value is {@link GridMailDiscoveryOutProtocol#SMTP}.
     *
     * @param proto Outgoing mail protocol.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutProtocol(GridMailDiscoveryOutProtocol proto) {
        if (proto == null) {
            outboxCfg.setProtocol(null);

            return;
        }

        switch (proto) {
            case SMTP: { outboxCfg.setProtocol(GridMailOutboxProtocol.SMTP); break; }
            case SMTPS: { outboxCfg.setProtocol(GridMailOutboxProtocol.SMTPS); break; }

            default: { assert false: "Unknown mail discovery protocol: " + proto; }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getOutHost() {
        return outboxCfg.getHost();
    }

    /**
     * Sets outgoing host name for sending email messages
     * (usually either SMTP or IMAP).
     * <p>
     * There is no default value for the parameter.
     *
     * @param host Outgoing email host name.
     */
    @GridSpiConfiguration(optional = false)
    public void setOutHost(String host) {
        outboxCfg.setHost(host);
    }

    /**
     * {@inheritDoc}
     */
    public int getOutPort() {
        return outboxCfg.getPort();
    }

    /**
     * Sets port number for outgoing mail.
     * <p>
     * If not provided the default value is <tt>25</tt>.
     *
     * @param port Outgoing email port number.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutPort(int port) {
        outboxCfg.setPort(port);
    }

    /**
     * {@inheritDoc}
     */
    public String getOutUsername() {
        return outboxCfg.getUsername();
    }

    /**
     * Sets username for outgoing mail authentication. If provided,
     * then password should also be provided. Username with <tt>null</tt>
     * value means that no authentication will be used.
     * <p>
     * If not provided the default value is <tt>null</tt>.
     *
     * @param username Outbox username.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutUsername(String username) {
        outboxCfg.setUsername(username);
    }

    /**
     * Sets password for outgoing mail authentication. If provided,
     * then username should also be provided.
     * <p>
     * If not provided the default value is <tt>null</tt>.
     *
     * @param pswd Outbox password.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutPassword(String pswd) {
        outboxCfg.setPassword(pswd);
    }

    /**
     * {@inheritDoc}
     */
    public String getInConnectionTypeFormatted() {
        return inboxCfg.getConnectionType() == null ? "" : inboxCfg.getConnectionType().toString();
    }

    /**
     * Sets type of incoming mail connection which should be one of the following:
     * <ul>
     * <li>{@link GridMailDiscoveryType#NONE}</li>
     * <li>{@link GridMailDiscoveryType#SSL}</li>
     * <li>{@link GridMailDiscoveryType#STARTTLS}</li>
     * </ul>
     * <p>
     * If not provided the default value is {@link GridMailDiscoveryType#NONE}.
     *
     * @param type Connection type.
     */
    @GridSpiConfiguration(optional = true)
    public void setInConnectionType(GridMailDiscoveryType type) {
        if (type == null) {
            inboxCfg.setConnectionType(null);

            return;
        }

        // Convert types.
        switch (type) {
            case NONE: { inboxCfg.setConnectionType(GridMailConnectionType.NONE); break; }
            case SSL: { inboxCfg.setConnectionType(GridMailConnectionType.SSL); break; }
            case STARTTLS: { inboxCfg.setConnectionType(GridMailConnectionType.STARTTLS); break; }

            default: { assert false; break; }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getReadBatchSize() {
        return inboxCfg.getReadBatchSize();
    }

    /**
     * Sets number of messages fetched from mail server at a time.
     * <p>
     * If not provided the default value is <tt>100</tt>.
     *
     * @param readBatchSize Number of fetched messages.
     */
    @GridSpiConfiguration(optional = true)
    public void setReadBatchSize(int readBatchSize) {
        inboxCfg.setReadBatchSize(readBatchSize);
    }

    /**
     * {@inheritDoc}
     */
    public String getInProtocolFormatted() {
        return inboxCfg.getProtocol() == null ? "" : inboxCfg.getProtocol().toString();
    }

    /**
     * Sets incoming mail protocol. Should be one of the following:
     * <ul>
     * <li>{@link GridMailDiscoveryInProtocol#POP3}</li>
     * <li>{@link GridMailDiscoveryInProtocol#POP3S}</li>
     * <li>{@link GridMailDiscoveryInProtocol#IMAP}</li>
     * <li>{@link GridMailDiscoveryInProtocol#IMAPS}</li>
     * </ul>
     * <p>
     * If not provided the default value is {@link GridMailDiscoveryInProtocol#POP3}.
     *
     * @param proto Incoming protocol.
     */
    @GridSpiConfiguration(optional = true)
    public void setInProtocol(GridMailDiscoveryInProtocol proto) {
        if (proto == null) {
            inboxCfg.setProtocol(null);

            return;
        }

        switch (proto) {
            case IMAP: { inboxCfg.setProtocol(GridMailInboxProtocol.IMAP); break; }
            case IMAPS: { inboxCfg.setProtocol(GridMailInboxProtocol.IMAPS); break; }
            case POP3: { inboxCfg.setProtocol(GridMailInboxProtocol.POP3); break; }
            case POP3S: { inboxCfg.setProtocol(GridMailInboxProtocol.POP3S); break; }

            default: { assert false : "Unknown mail discovery incoming protocol: " + proto; }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getInHost() {
        return inboxCfg.getHost();
    }

    /**
     * Sets incoming host name for receiving email messages (usually either
     * POP or IMAP).
     * <p>
     * There is no default value.
     *
     * @param host Incoming email host name. There is no default value.
     */
    @GridSpiConfiguration(optional = false)
    public void setInHost(String host) {
        inboxCfg.setHost(host);
    }

    /**
     * {@inheritDoc}
     */
    public int getInPort() {
        return inboxCfg.getPort();
    }

    /**
     * Sets port number for incoming mail.
     * <p>
     * If not provided the default value is <tt>110</tt>.
     *
     * @param port Incoming email server port number.
     */
    @GridSpiConfiguration(optional = true)
    public void setInPort(int port) {
        inboxCfg.setPort(port);
    }

    /**
     * {@inheritDoc}
     */
    public String getInUsername() {
        return inboxCfg.getUsername();
    }

    /**
     * Sets username for incoming mail authentication. If provided,
     * then password should also be provided. Username with <tt>null</tt>
     * value means that no authentication will be used.
     * <p>
     * If not provided the default value is <tt>null</tt>.
     *
     * @param username Incoming mailbox username.
     */
    @GridSpiConfiguration(optional = true)
    public void setInUsername(String username) {
        inboxCfg.setUsername(username);
    }

    /**
     * Sets password for incoming mail authentication. If provided,
     * then username should also be provided.
     * <p>
     * If not provided the default value is <tt>null</tt>.
     *
     * @param pswd Incoming mailbox password.
     */
    @GridSpiConfiguration(optional = true)
    public void setInPassword(String pswd) {
        inboxCfg.setPassword(pswd);
    }

    /**
     * {@inheritDoc}
     */
    public String getFolderName() {
        return inboxCfg.getFolderName();
    }

    /**
     * Sets name of email folder on mail server.
     * <p>
     * If not provided the default value is <tt>Inbox</tt>.
     *
     * @param folderName Mail server folder name.
     */
    @GridSpiConfiguration(optional = true)
    public void setFolderName(String folderName) {
        inboxCfg.setFolderName(folderName);
    }

    /**
     * {@inheritDoc}
     */
    public String getSubject() {
        return outboxCfg.getSubject();
    }

    /**
     * Sets email message subject.
     * <p>
     * If not provided the default value is {@link #DFLT_MAIL_SUBJECT}.
     *
     * @param subj Email message subject.
     */
    @GridSpiConfiguration(optional = true)
    public void setSubject(String subj) {
        outboxCfg.setSubject(subj);
    }

    /**
     * {@inheritDoc}
     */
    public Properties getOutCustomProperties() {
        return outboxCfg.getCustomProperties();
    }

    /**
     * Sets any custom properties required for outgoing connection.
     * <p>
     * If not provided the default value is <tt>null</tt>.
     *
     * @param props Custom parameter.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutCustomProperties(Properties props) {
        outboxCfg.setCustomProperties(props);
    }

    /**
     * {@inheritDoc}
     */
    public Properties getInCustomProperties() {
        return inboxCfg.getCustomProperties();
    }

    /**
     * Sets any custom properties required for receiving connection.
     * <p>
     * If not provided the default value is <tt>null</tt>.
     *
     * @param props Custom properties.
     */
    @GridSpiConfiguration(optional = true)
    public void setInCustomProperties(Properties props) {
        inboxCfg.setCustomProperties(props);
    }

    /**
     * {@inheritDoc}
     */
    public String getFromAddress() {
        return outboxCfg.getFrom();
    }

    /**
     * Sets 'From' address for all email messages.
     * <p>
     * There is no default value.
     *
     * @param addr Email address for data exchange.
     */
    @GridSpiConfiguration(optional = false)
    public void setFromAddress(String addr) {
        outboxCfg.setFrom(addr);
    }

    /**
     * {@inheritDoc}
     */
    public String getBroadcastAddress() {
        return bcastAddr;
    }

    /**
     * Sets broadcast email address used by node to discover each other.
     * <p>
     * There is no default value.
     *
     * @param bcastAddr Email address.
     */
    @GridSpiConfiguration(optional = false)
    public void setBroadcastAddress(String bcastAddr) {
        this.bcastAddr = bcastAddr;
    }

    /**
     * {@inheritDoc}
     */
    public String getStoreFileName() {
        return inboxCfg.getStoreFileName();
    }

    /**
     * Sets locally stored full file name for all read messages.
     * Can be either full path or a path relative to GridGain installation home folder.
     * <p>
     * If not provided the default value is {@link #DFLT_STORE_FILE_NAME}.
     *
     * @param fileName Local storage file name.
     */
    @GridSpiConfiguration(optional = true)
    public void setStoreFileName(String fileName) {
        inboxCfg.setStoreFileName(fileName);
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

        // Validate configuration parameters.
        assertParameter(outboxCfg.getConnectionType() != null, "outConnectionType != null");
        assertParameter(outboxCfg.getProtocol() != null, "outProtocol != null");
        assertParameter(outboxCfg.getHost() != null, "outHost != null");
        assertParameter(outboxCfg.getPort() > 0, "outPort > 0");
        assertParameter(outboxCfg.getPort() < 65535, "outPort < 65535");
        assertParameter(outboxCfg.getCustomProperties() != null, "outCustomProperties != null");
        assertParameter(outboxCfg.getSubject() != null, "subject != null");
        assertParameter(outboxCfg.getFrom() != null, "fromAddress != null");
        assertParameter(bcastAddr != null, "broadcastAddress != null");
        assertParameter(inboxCfg.getConnectionType() != null, "inConnectionType != null");
        assertParameter(inboxCfg.getProtocol() != null, "inProtocol != null");
        assertParameter(inboxCfg.getHost() != null, "inHost != null");
        assertParameter(inboxCfg.getPort() > 0, "inPort != null");
        assertParameter(inboxCfg.getPort() < 65535, "inPort < 65535");
        assertParameter(inboxCfg.getCustomProperties() != null, "inCustomProperties != null");
        assertParameter(inboxCfg.getFolderName() != null, "folderName != null");
        assertParameter(inboxCfg.getReadBatchSize() > 0, "readBatchSize > 0");
        assertParameter(inboxCfg.getStoreFileName() != null, "storeFileName != null");
        assertParameter(beatFreq > 0, "heartbeatFrequency > 0");
        assertParameter(recvDelay > 0, "receiverDelay > 0");
        assertParameter(maxMissedBeats > 0, "maxMissedHeartbeats > 0");
        assertParameter(leaveMsgsOnServer > 0, "leaveMessagesOnServer > 0");
        assertParameter(pingWait > 0, "pingResponseWait > 0");

        // Attributes object should be defined.
        assertParameter(nodeAttrs != null, "nodeAttrs != null");

        this.gridName = gridName;

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("inboxCfg", inboxCfg));
            log.info(configInfo("outboxCfg", outboxCfg));
            log.info(configInfo("bcastAddr", bcastAddr));
            log.info(configInfo("recvDelay", recvDelay));
            log.info(configInfo("beatFreq", beatFreq));
            log.info(configInfo("maxMissedBeats", maxMissedBeats));
            log.info(configInfo("pingWait", pingWait));
            log.info(configInfo("leaveMsgsOnServer", leaveMsgsOnServer));
        }

        nodeStartTime = System.currentTimeMillis();

        if (maxMissedBeats * beatFreq <= recvDelay) {
            log.warning("Message receive delay should be less than heartbeat frequency multiply " +
                "by maximum missed heartbeats: " + recvDelay);
        }

        if (getPingResponseWait() < recvDelay * 2) {
            log.warning("Ping response wait time should be not less than message receive " +
                "delay multiply by 2: " + recvDelay);
        }

        if (leaveMsgsOnServer <= maxMissedBeats * beatFreq) {
            log.warning("Leave messages on server wait time should be greater than heartbeat frequency multiply " +
                "by maximum missed heartbeats: " + leaveMsgsOnServer);
        }

        registerMBean(gridName, this, GridMailDiscoverySpiMBean.class);

        GridMailInboxMatcher matcher = new GridMailInboxMatcher();

        matcher.setSubject(getSubject());

        inboxCfg.setLogger(log);

        try {
            inbox = GridMailInboxFactory.createInbox(inboxCfg, matcher, marshaller);

            // Check that mailbox can be opened.
            if (log.isDebugEnabled() == true) {
                log.debug("Initializing mailbox... This may take a while.");
            }

            inbox.open(true);
        }
        catch (GridMailException e) {
            throw (GridSpiException)new GridSpiException("Failed to initialize inbox.", e).setData(925, "src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java");
        }
        finally {
            GridUtils.close(inbox, false, log);
        }

        assert inbox != null : "ASSERTION [line=931, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";

        outbox = GridMailOutboxFactory.createOutbox(outboxCfg);

        // Initialize local node.
        locNode = new GridMailDiscoveryNode(nodeId, outboxCfg.getFrom(), nodeStartTime,
            GridMailDiscoveryNodeState.READY, null, metricsProvider);

        locNode.setAttributes(nodeAttrs);

        nodeSweeper = new NodeSweeper();
        mailRcvr = new MailReceiver();
        mailSender = new MailSender();

        nodeSweeper.start();
        mailRcvr.start();
        mailSender.start();

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        GridUtils.interrupt(mailSender);
        GridUtils.interrupt(mailRcvr);
        GridUtils.interrupt(nodeSweeper);

        GridUtils.join(mailSender, log);
        GridUtils.join(mailRcvr, log);
        GridUtils.join(nodeSweeper, log);

        nodeStartTime = 0;

        if (log.isDebugEnabled() == true) {
            log.debug("Local grid node has left grid topology.");
        }

        if (inbox != null) {
            try {
                inbox.flush();
            }
            catch (GridMailException e) {
                log.error("Failed to flush messages to local store: " + getStoreFileName(), e);
            }
        }

        GridUtils.close(inbox, true, log);

        unregisterMBean();

        // Clear resources.
        inbox = null;
        outbox = null;
        locNode = null;
        allNodes.clear();
        rmtNodes = null;
        nodeSweeper = null;
        mailRcvr = null;
        mailSender = null;
        pingThreads.clear();

        // Ack stop.
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

                for (GridMailDiscoveryNode node : allNodes.values()) {
                    if (node.getState() == GridMailDiscoveryNodeState.READY && node.equals(locNode) == false) {
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
        assert nodeId != null : "ASSERTION [line=1029, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";

        if (locNode.getId().equals(nodeId) == true) {
            return locNode;
        }

        synchronized (mux) {
            GridMailDiscoveryNode node = allNodes.get(nodeId);

            return node != null && node.getState() == GridMailDiscoveryNodeState.READY ? node : null;
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
        assert nodeId != null : "ASSERTION [line=1068, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";

        //Don't send email messages for pinging local node.
        if (locNode.getId().equals(nodeId) == true) {
            return true;
        }

        synchronized (mux) {
            GridMailDiscoveryNode node = allNodes.get(nodeId);

            if (node == null || node.getState() != GridMailDiscoveryNodeState.READY) {
                return false;
            }

            try {
                pingThreads.put(Thread.currentThread(), nodeId);

                long end = System.currentTimeMillis() + pingWait;

                long delta = pingWait;

                mailSender.wakeUp(false);

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
            finally {
                pingThreads.remove(Thread.currentThread());
            }

            return false;
        }
    }

    /**
     * Method is called when any discovery event occurs. It calls external listener.
     *
     * @param evt Discovery event type.
     * @param node Remote node this event is connected with.
     */
    private void notifyDiscovery(GridDiscoveryEventType evt, GridMailDiscoveryNode node) {
        assert evt != null : "ASSERTION [line=1125, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";
        assert node != null : "ASSERTION [line=1126, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";

        if (node.getState().equals(GridMailDiscoveryNodeState.NEW) == false) {
            GridDiscoveryListener listener = this.listener;

            if (listener != null) {
                listener.onDiscovery(evt, node);
            }
        }
    }

    /**
     * Handle received messages.
     *
     * @param msgs Received messages
     */
    private void processMessages(List<GridMailDiscoveryMessage> msgs) {
        assert msgs != null : "ASSERTION [line=1143, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";

        boolean sendMsg = false;

        boolean sendAttrs = false;

        for (GridMailDiscoveryMessage msg : msgs) {
            // Own messages skipped when read new messages.
            assert nodeId.equals(msg.getSourceNodeId()) == false : "ASSERTION [line=1151, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";

            UUID nodeId = msg.getSourceNodeId();

            GridDiscoveryEventType evt = null;

            GridMailDiscoveryNode node = null;

            synchronized (mux) {
                node = allNodes.get(nodeId);

                if (node != null) {
                    if (msg.getNodeStartTime() < node.getStartTime()) {
                        log.warning("Received old message form restarted node [node=" + node + ", msg=" +
                            msg + ']');

                        continue;
                    }

                    if (node.getState() == GridMailDiscoveryNodeState.LEFT) {
                        log.warning("Received message from off node [node=" + node + ", msg=" + msg + ']');

                        continue;
                    }

                    if (msg.getNodeStartTime() > node.getStartTime() && msg.isLeave() == false) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Grid node was restarted and was removed from topology: " + node);
                        }

                        node.setState(GridMailDiscoveryNodeState.LEFT);

                        node.onHeartbeat(msg.getMetrics());

                        rmtNodes = null;

                        if (node.getState() == GridMailDiscoveryNodeState.READY) {
                            evt = FAILED;
                        }
                    }
                }

                if (msg.isLeave() == true) {
                    if (node != null) {
                        if (node.getState() == GridMailDiscoveryNodeState.READY) {
                            // Notify only ready nodes.
                            evt = LEFT;
                        }
                        else {
                            log.warning("Node had never successfully joined: " + node);
                        }

                        node.setState(GridMailDiscoveryNodeState.LEFT);

                        node.onHeartbeat(msg.getMetrics());

                        if (log.isInfoEnabled() == true) {
                            log.info("Grid node has left topology: " + node);
                        }
                    }
                }
                else {
                    if (node == null) {
                        // New node.
                        node = new GridMailDiscoveryNode(nodeId, msg.getFromAddress(), msg.getNodeStartTime(),
                            GridMailDiscoveryNodeState.NEW, msg.getMetrics(), null);

                        allNodes.put(nodeId, node);
                    }
                    else {
                        evt = METRICS_UPDATED;
                    }

                    node.onHeartbeat(msg.getMetrics());

                    if (msg.getAttributeNodes() != null && msg.getAttributeNodes().contains(this.nodeId) == true) {
                        sendAttrs = true;
                    }

                    if (msg.getPingedNodes() != null && msg.getPingedNodes().contains(this.nodeId) == true) {
                        sendMsg = true;

                        if (log.isDebugEnabled() == true) {
                            log.debug("Received ping request from node with ID: " + msg.getSourceNodeId());
                        }
                    }

                    if (node.getState() == GridMailDiscoveryNodeState.NEW) {
                        if (msg.getAttributes() == null) {
                            sendMsg = true;
                        }
                        else {
                            node.onDataReceived(msg.getAttributes());

                            evt = JOINED;

                            if (log.isInfoEnabled() == true) {
                                log.info("Added new node to topology: " + node);
                            }
                        }
                    }
                    else {
                        // Remove all threads waiting for ping response from this node.
                        for (Iterator<UUID> iter = pingThreads.values().iterator(); iter.hasNext() == true;) {
                            if (node.getId().equals(iter.next()) == true) {
                                iter.remove();
                            }
                        }

                        // Notify threads waiting for ping response.
                        mux.notifyAll();
                    }
                }

                if (evt == LEFT || evt == JOINED) {
                    // Reset list of ready nodes.
                    rmtNodes = null;
                }
            }

            if (evt != null) {
                notifyDiscovery(evt, node);
            }
        }

        if (sendMsg == true || sendAttrs == true) {
            mailSender.wakeUp(sendAttrs);
        }
    }

    /**
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class NodeSweeper extends GridSpiThread {
        /**
         *
         */
        NodeSweeper() {
            super(gridName, "grid-mail-disco-node-sweeper", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void body() throws InterruptedException {
            final long maxSilenceTime = beatFreq * maxMissedBeats;

            final List<GridMailDiscoveryNode> failedNodes = new ArrayList<GridMailDiscoveryNode>();

            while (isInterrupted() == false) {
                long currentTime = System.currentTimeMillis();

                synchronized (mux) {
                    for (Iterator<GridMailDiscoveryNode> i = allNodes.values().iterator(); i.hasNext() == true;) {
                        GridMailDiscoveryNode node = i.next();

                        if (currentTime - node.getLastHeartbeat() > maxSilenceTime) {
                            if (node.getState() != GridMailDiscoveryNodeState.LEFT) {
                                failedNodes.add(node);
                            }

                            i.remove();

                            rmtNodes = null;
                        }
                    }
                }

                if (failedNodes.isEmpty() == false) {
                    for (GridMailDiscoveryNode node : failedNodes) {
                        if (node.getState() == GridMailDiscoveryNodeState.READY) {
                            log.warning("Removed failed node from topology: " + node);

                            // Notify listener of failure.
                            notifyDiscovery(FAILED, node);
                        }
                        else {
                            assert node.getState() == GridMailDiscoveryNodeState.NEW : "ASSERTION [line=1329, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoverySpi.java]";

                            log.warning("Node had never successfully joined (will remove): " + node);
                        }

                    }

                    failedNodes.clear();
                }

                Thread.sleep(beatFreq);
            }
        }
    }

    /**
     * Heartbeats may contain node's attributes.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class MailSender extends GridSpiThread {
        /** */
        private long nextSendTime = 0;

        /** */
        private boolean sendAttrs = false;

        /**
         *
         */
        MailSender() {
            super(gridName, "grid-mail-disco-sender", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void body() throws InterruptedException {
            boolean isLeaving = false;

            nextSendTime = System.currentTimeMillis() + beatFreq;

            while (isLeaving == false) {
                GridMailDiscoveryMessage msg = null;

                isLeaving = isInterrupted();

                // Get metrics outside of synchronization
                // to avoid possible deadlocks.
                GridNodeMetrics locMetrics = metricsProvider.getMetrics();

                synchronized (mux) {
                    Set<UUID> attrNodes = null;

                    Set<UUID> pingNodes = null;

                    if (isLeaving == false) {
                        for (GridMailDiscoveryNode node : allNodes.values()) {
                            if (node.getState() == GridMailDiscoveryNodeState.NEW) {
                                if (attrNodes == null) {
                                    attrNodes = new HashSet<UUID>();
                                }

                                attrNodes.add(node.getId());
                            }
                        }

                        if (pingThreads.isEmpty() == false) {
                            pingNodes = new HashSet<UUID>(pingThreads.values());
                        }
                    }

                    msg = new GridMailDiscoveryMessage(
                        nodeId,
                        isLeaving,
                        pingNodes,
                        attrNodes,
                        sendAttrs == true ? nodeAttrs : null,
                        locNode.getPhysicalAddress(),
                        System.currentTimeMillis(),
                        nodeStartTime,
                        locMetrics);

                    sendAttrs = false;
                }

                try {
                    GridMailOutboxSession ses = outbox.getSession();

                    ses.addToRecipient(getBroadcastAddress());
                    ses.addAttachment(msg, "msg", MSG_IDX, marshaller);

                    ses.send();

                    if (log.isDebugEnabled() == true) {
                        log.debug("Sent message [msg=" + msg + ", msgId=" +
                            Arrays.toString(ses.getMessageId()) + ']');
                    }
                }
                catch (GridMailException e) {
                    log.error("Failed to send mail discovery message: " + msg, e);
                }

                if (isLeaving == false) {
                    waitNext();
                }
            }
        }

        /**
         *
         * @param sendAttrs Attributes.
         */
        synchronized void wakeUp(boolean sendAttrs) {
            if (sendAttrs == true) {
                this.sendAttrs = sendAttrs;
            }

            nextSendTime = System.currentTimeMillis();

            notifyAll();
        }

        /**
         *
         */
        private synchronized void waitNext() {
            //noinspection UnusedCatchParameter
            try {
                long delta = nextSendTime - System.currentTimeMillis();

                while (delta > 0) {
                    wait(delta);

                    delta = nextSendTime - System.currentTimeMillis();
                }
            }
            catch (InterruptedException e) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Mail sender thread got interrupted (ignoring): " + this);
                }
            }

            nextSendTime += beatFreq;
        }
    }

    /**
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class MailReceiver extends GridSpiThread {
        /**
         *
         */
        MailReceiver() {
            super(gridName, "grid-mail-disco-receiver-sweeper", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void body() throws InterruptedException {
            long cleanTime = System.currentTimeMillis();
            long rcvTime = cleanTime;

            while (isInterrupted() == false) {
                List<GridMailDiscoveryMessage> msgs = null;

                try {
                    // Open mailbox with readonly mode.
                    inbox.open(true);

                    List<GridMailInboxMessage> newMsgs = inbox.readNew();

                    if (newMsgs != null && newMsgs.isEmpty() == false) {
                        msgs = new ArrayList<GridMailDiscoveryMessage>(newMsgs.size());

                        for (GridMailInboxMessage msg : newMsgs) {
                            GridMailDiscoveryMessage discoMsg = (GridMailDiscoveryMessage)msg.
                                getAttachment(MSG_IDX).getContent(marshaller);

                            if (nodeId.equals(discoMsg.getSourceNodeId()) == true) {
                                // Skip own messages but fire METRICS_UPDATED
                                // event if if is not leaving message.
                                if (discoMsg.isLeave() == false) {
                                    notifyDiscovery(METRICS_UPDATED, locNode);
                                }

                                continue;
                            }

                            msgs.add(discoMsg);
                        }
                    }
                }
                catch (GridMailException e) {
                    log.error("Failed to get messages.", e);
                }
                finally {
                    GridUtils.close(inbox, false, log);
                }

                // Prepare information for mailSender.
                if (msgs != null) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received new message(s): " + msgs);
                    }

                    processMessages(msgs);
                }

                rcvTime = System.currentTimeMillis() + recvDelay;

                // Sweeper.
                if (cleanTime <= System.currentTimeMillis()) {
                    Date deadLine = new Date(System.currentTimeMillis() - leaveMsgsOnServer);

                    try {
                        inbox.open(false);

                        int deleted = inbox.removeOld(deadLine);

                        if (log.isDebugEnabled() == true) {
                            log.debug("Deleted messages count: " + deleted);
                        }
                    }
                    catch (GridMailException e) {
                        log.error("Failed while cleaning messages on server.", e);
                    }
                    finally {
                        GridUtils.close(inbox, true, log);
                    }

                    cleanTime = System.currentTimeMillis() + leaveMsgsOnServer;
                }

                long delta = rcvTime - System.currentTimeMillis();

                if (delta > 0) {
                    Thread.sleep(delta < leaveMsgsOnServer ? delta : leaveMsgsOnServer);
                }
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Mail receiver-sweeper stopped.");
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
        return GridToStringBuilder.toString(GridMailDiscoverySpi.class, this);
    }
}
