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

package org.gridgain.grid.spi.communication.mail;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.mail.inbox.*;
import org.gridgain.grid.util.mail.outbox.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Email implementation of {@link GridCommunicationSpi}. Email communication is
 * provided for cases where nodes from different networks (even different
 * countries) need to participate in grid task execution together as it usually
 * can penetrate through any firewall. It supports SMTP/POP and IMAP email access
 * protocols and can be used to connect to any public or private email server out
 * there.
 * <p>
 * When working with email communication, make sure that maximum send/receive
 * limit set by mail server is not exceeded. Sometimes it is better to configure
 * your own mail server to avoid such limitations.
 * <p>
 * Note, that due to its nature mail communication is much slower than other
 * implementations of communication SPI's and measures communication delays in
 * minutes rather than in seconds. This means that execution of some tasks
 * can fail more often than with other SPI's due to nodes leaving grid topology.
 * In most cases user should implement a custom failover resolution in
 * {@link GridTask} implementation, which should check if a node
 * is still alive using {@link Grid#pingNode(UUID)} method. Use this SPI
 * whenever it is acceptable to react to node topology changes with substantial
 * delay.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * The following configuration parameters are mandatory:
 * <ul>
 * <li>Outgoing host (see {@link #setOutHost(String)}).</li>
 * <li>Ingoing host (see {@link #setInHost(String)}).</li>
 * <li>Address from (see {@link #setFromAddress(String)}).</li>
 * </ul>
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional.
 * <ul>
 * <li>Outgoing connection type (see {@link #setOutConnectionType(GridMailCommunicationType)})</li>
 * <li>Outgoing port (see {@link #setOutPort(int)}</li>
 * <li>Outgoing user name (see {@link #setOutUsername(String)}</li>
 * <li>Outgoing user password (see {@link #setOutPassword(String)}</li>
 * <li>Outgoing additional JavaMail properties (see {@link #setOutCustomProperties(Properties)}</li>
 * <li>Outgoing protocol (see {@link #setOutProtocol(GridMailCommunicationOutProtocol)} </li>
 * <li>Ingoing connection type (see {@link #setInConnectionType(GridMailCommunicationType)}</li>
 * <li>Ingoing port (see {@link #setInPort(int)}</li>
 * <li>Ingoing user name (see {@link #setInUsername(String)}</li>
 * <li>Ingoing user password (see {@link #setInPassword(String)}</li>
 * <li>Ingoing additional JavaMail properties (see {@link #setInCustomProperties(Properties)}</li>
 * <li>Ingoing protocol (see {@link #setInProtocol(GridMailCommunicationInProtocol)} </li>
 * <li>Mail subject (see {@link #setSubject(String)}</li>
 * <li>Mail folder name (see {@link #setFolderName(String)}</li>
 * <li>Locally stored file name (see {@link #setStoreFileName(String)} </li>
 * <li>Read messages batch size (see {@link #setReadBatchSize(int)}</li>
 * <li>Delay between messages read (see {@link #setReceiverDelay(long)}</li>
 * <li>Number of milliseconds to leave messages on server (see {@link #setLeaveMessagesOnServer(long)}</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridMailCommunicationSpi needs to be explicitely configured:
 * <pre name="code" class="java">
 * GridMailCommuncationSpi commSpi = new GridMailCommuncationSpi();
 *
 * // Inbox configuration.
 * commSpi.setInHost("pop.google.com");
 *
 * // Outbox configuration.
 * commSpi.setOutHost("smtp.google.com");
 *
 * // Incoming/outgoing e-mail address configuration.
 * commSpi.setFromAddress("grid@google.com");
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
 * GridMailCommunicationSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="communicationSpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.communication.mail.GridMailCommunicationSpi"&gt;
 *                 &lt;property name="outHost" value="smtp.google.com"/&gt;
 *                 &lt;property name="inHost" value="pop.google.com"/&gt;
 *                 &lt;property name="fromAddress" value="grid@google.com"/&gt;
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
@SuppressWarnings({"MethodWithTooExceptionsDeclared"})
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridMailCommunicationSpi extends GridSpiAdapter implements GridCommunicationSpi,
    GridMailCommunicationSpiMBean {
    /** Default subject of email (value is <tt>grid.email.comm.msg</tt>). */
    public static final String DFLT_SUBJ = "grid.email.comm.msg";

    /** Default local storage file name (value is <tt>grid-email-comm-msgs.dat</tt>). */
    public static final String DFLT_STORE_FILE_NAME = "grid-email-comm-msgs.dat";

    /** Delay in milliseconds between receiving email (value is <tt>10000</tt>). */
    public static final long DFLT_RECEIVER_DELAY = 10000;

    /** Leave messages on server in milliseconds (value is <tt>86400000</tt>). */
    public static final long DFLT_LEAVE_MSGS_ON_SERVER = 86400000;

    /** Node email address attribute name (value is <tt>comm.email.address</tt>). */
    public static final String ATTR_EMAIL_ADDR = "comm.email.address";

    /** */
    private static final int IDS_IDX = 0;

    /** */
    private static final int OBJ_IDX = 1;

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
    private long recvDelay = DFLT_RECEIVER_DELAY;

    /** */
    private long leaveMsgsOnServer = DFLT_LEAVE_MSGS_ON_SERVER;

    /** */
    private GridMailInboxConfiguration inboxCfg = new GridMailInboxConfiguration();

    /** */
    private GridMailOutboxConfiguration outboxCfg = new GridMailOutboxConfiguration();

    /** */
    private GridSpiThread rcvr = null;

    /** */
    private volatile GridMessageListener listener = null;

    /** */
    private GridMailInbox inbox = null;

    /** */
    private GridMailOutbox outbox = null;

    /**
     * Set SPI default values for mailboxes configurations.
     */
    public GridMailCommunicationSpi() {
        inboxCfg.setStoreFileName(DFLT_STORE_FILE_NAME);

        outboxCfg.setSubject(DFLT_SUBJ);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        assertParameter(outboxCfg.getFrom() != null, "fromAddress != null");

        return GridUtils.makeMap(createSpiAttributeName(ATTR_EMAIL_ADDR), outboxCfg.getFrom());
    }

    /**
     * Sets outgoing host name for sending emails (usually either SMTP or IMAP).
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
     * Sets type of outgoing mail connection which should be one of the following:
     * <ul>
     * <li>{@link GridMailCommunicationType#NONE}</li>
     * <li>{@link GridMailCommunicationType#SSL}</li>
     * <li>{@link GridMailCommunicationType#STARTTLS}</li>
     * </ul>
     * <p>
     * If not provided, default value is {@link GridMailCommunicationType#NONE}.
     *
     * @param type Connection type.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutConnectionType(GridMailCommunicationType type) {
        if (type == null) {
            outboxCfg.setConnectionType(null);

            return;
        }

        // Convert types.
        switch (type) {
            case NONE: { outboxCfg.setConnectionType(GridMailConnectionType.NONE); break; }
            case SSL: { outboxCfg.setConnectionType(GridMailConnectionType.SSL); break; }
            case STARTTLS: { outboxCfg.setConnectionType(GridMailConnectionType.STARTTLS); break; }

            default: { assert false; break; }
        }
    }

    /**
     * Sets port number for outgoing mail.
     * <p>
     * If not provided, default value is <tt>25</tt>.
     *
     * @param port Outgoing email port number.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutPort(int port) {
        outboxCfg.setPort(port);
    }

    /**
     * Sets username for outgoing mail authentication. If provided,
     * then password should also be provided. Username with <tt>null</tt>
     * value means that no authentication will be used.
     * <p>
     * If not provided, default value is <tt>null</tt>.
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
     * If not provided, default value is <tt>null</tt>.
     *
     * @param pswd Outbox password.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutPassword(String pswd) {
        outboxCfg.setPassword(pswd);
    }

    /**
     * Sets incoming host name for receiving emails (usually either POP or IMAP).
     * <p>
     * There is no default value.
     *
     * @param host Incoming email host name.
     */
    @GridSpiConfiguration(optional = false)
    public void setInHost(String host) {
        inboxCfg.setHost(host);
    }

    /**
     * Sets type of incoming mail connection which should be one of the following:
     * <ul>
     * <li>{@link GridMailCommunicationType#NONE}</li>
     * <li>{@link GridMailCommunicationType#SSL}</li>
     * <li>{@link GridMailCommunicationType#STARTTLS}</li>
     * </ul>
     * <p>
     * If not provided, default value is {@link GridMailCommunicationType#NONE}.
     *
     * @param type Connection type.
     *
     */
    @GridSpiConfiguration(optional = true)
    public void setInConnectionType(GridMailCommunicationType type) {
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
     * Sets port number for incoming mail.
     * <p>
     * If not provided, default value is <tt>110</tt>.
     *
     * @param port Incoming email server port number.
     */
    @GridSpiConfiguration(optional = true)
    public void setInPort(int port) {
        inboxCfg.setPort(port);
    }

    /**
     * Sets username for incoming mail authentication. If provided,
     * then password should also be provided. Username with <tt>null</tt>
     * value means that no authentication will be used.
     * <p>
     * If not provided, default value is <tt>null</tt>.
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
     * If not provided, default value is <tt>null</tt>.
     *
     * @param pswd Incoming mailbox password.
     */
    @GridSpiConfiguration(optional = true)
    public void setInPassword(String pswd) {
        inboxCfg.setPassword(pswd);
    }

    /**
     * Sets email message subject.
     * <p>
     * If not provided, default value is <tt>grid.email.comm.msg</tt>.
     *
     * @param subj Email message subject.
     */
    @GridSpiConfiguration(optional = true)
    public void setSubject(String subj) {
        outboxCfg.setSubject(subj);
    }

    /**
     * Sets name of email folder on mail server.
     * <p>
     * If not provided, default value is <tt>Inbox</tt>.
     *
     * @param folderName Mail server folder name.
     */
    @GridSpiConfiguration(optional = true)
    public void setFolderName(String folderName) {
        inboxCfg.setFolderName(folderName);
    }

    /**
     * Sets 'From' address for all email messages. It is added as
     * {@link #ATTR_EMAIL_ADDR} node attribute so it can be accessed
     * on remote nodes. This address is used by other nodes to send email
     * to this node.
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
     * Sets any custom properties required for receiving connection.
     * <p>
     * If not provided, default value is <tt>null</tt>.
     *
     * @param props Custom properties.
     */
    @GridSpiConfiguration(optional = true)
    public void setInCustomProperties(Properties props) {
        inboxCfg.setCustomProperties(props);
    }

    /**
     * Sets any custom properties required for outgoing connection.
     * <p>
     * If not provided, default value is <tt>null</tt>.
     *
     * @param props Custom parameter.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutCustomProperties(Properties props) {
        outboxCfg.setCustomProperties(props);
    }

    /**
     * Sets number of messages fetched from mail server at a time.
     * <p>
     * If not provided, default value is <tt>100</tt>.
     *
     * @param size Number of fetched messages.
     */
    @GridSpiConfiguration(optional = true)
    public void setReadBatchSize(int size) {
        inboxCfg.setReadBatchSize(size);
    }

    /**
     * Sets interval in milliseconds between checking for new messages.
     * <p>
     * If not provided, default value is {@link #DFLT_RECEIVER_DELAY}.
     *
     * @param recvDelay Inverval between receiving messages.
     */
    @GridSpiConfiguration(optional = true)
    public void setReceiverDelay(long recvDelay) {
        this.recvDelay = recvDelay;
    }

    /**
     * Sets outgoing mail protocol. Should be one of the following:
     * <ul>
     * <li>{@link GridMailCommunicationOutProtocol#SMTP}</li>
     * <li>{@link GridMailCommunicationOutProtocol#SMTPS}</li>
     * </ul>
     * <p>
     * If not provided, default value is {@link GridMailCommunicationOutProtocol#SMTP}.
     *
     * @param proto Outgoing mail protocol.
     */
    @GridSpiConfiguration(optional = true)
    public void setOutProtocol(GridMailCommunicationOutProtocol proto) {
        if (proto == null) {
            outboxCfg.setProtocol(null);

            return;
        }

        switch (proto) {
             case SMTP: { outboxCfg.setProtocol(GridMailOutboxProtocol.SMTP); break; }
            case SMTPS: { outboxCfg.setProtocol(GridMailOutboxProtocol.SMTPS); break; }

            default: { assert false; break; }
        }
    }

    /**
     * Sets incoming mail protocol. Should be one of the following:
     * <ul>
     * <li>{@link GridMailCommunicationInProtocol#POP3}</li>
     * <li>{@link GridMailCommunicationInProtocol#POP3S}</li>
     * <li>{@link GridMailCommunicationInProtocol#IMAP}</li>
     * <li>{@link GridMailCommunicationInProtocol#IMAPS}</li>
     * </ul>
     * <p>
     * If not provided, default value is {@link GridMailCommunicationInProtocol#POP3}.
     *
     * @param proto Incoming protocol.
     */
    @GridSpiConfiguration(optional = true)
    public void setInProtocol(GridMailCommunicationInProtocol proto) {
        if (proto == null) {
            inboxCfg.setProtocol(null);

            return;
        }

        switch (proto) {
            case IMAP: { inboxCfg.setProtocol(GridMailInboxProtocol.IMAP); break; }
            case IMAPS: { inboxCfg.setProtocol(GridMailInboxProtocol.IMAPS); break; }
            case POP3: { inboxCfg.setProtocol(GridMailInboxProtocol.POP3); break; }
            case POP3S: { inboxCfg.setProtocol(GridMailInboxProtocol.POP3S); break; }

            default: { assert false; break; }
        }
    }

    /**
     * Sets locally stored full file name for all read messages.
     * Can be either full path or a path relative to GridGain installation home folder.
     * <p>
     * If not provided, default value is {@link #DFLT_STORE_FILE_NAME}.
     *
     * @param fileName Local storage file name.
     */
    @GridSpiConfiguration(optional = true)
    public void setStoreFileName(String fileName) {
        inboxCfg.setStoreFileName(fileName);
    }

    /**
     * Sets incoming messages life-time on mail server in milliseconds.
     * <p>
     * If not provided, default value is {@link #DFLT_LEAVE_MSGS_ON_SERVER}.
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
    public long getReceiverDelay() {
        return recvDelay;
    }

    /**
     * {@inheritDoc}
     */
    public long getLeaveMessagesOnServer() {
        return leaveMsgsOnServer;
    }

    /**
     * {@inheritDoc}
     */
    public String getOutConnectionTypeFormatted() {
        return outboxCfg.getConnectionType() == null ? "" : outboxCfg.getConnectionType().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutProtocolFormatted() {
        return outboxCfg.getProtocol() == null ? "" : outboxCfg.getProtocol().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutHost() {
        return outboxCfg.getHost();
    }

    /**
     * {@inheritDoc}
     */
    public int getOutPort() {
        return outboxCfg.getPort();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutUsername() {
        return outboxCfg.getUsername();
    }

    /**
     * {@inheritDoc}
     */
    public String getInConnectionTypeFormatted() {
        return inboxCfg.getConnectionType() == null ? "" : inboxCfg.getConnectionType().toString();
    }

    /**
     * {@inheritDoc}
     */
    public int getReadBatchSize() {
        return inboxCfg.getReadBatchSize();
    }

    /**
     * {@inheritDoc}
     */
    public String getInProtocolFormatted() {
        return inboxCfg.getProtocol() == null ? "" : inboxCfg.getProtocol().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getInHost() {
        return inboxCfg.getHost();
    }

    /**
     * {@inheritDoc}
     */
    public int getInPort() {
        return inboxCfg.getPort();
    }

    /**
     * {@inheritDoc}
     */
    public String getInUsername() {
        return inboxCfg.getUsername();
    }

    /**
     * {@inheritDoc}
     */
    public String getFolderName() {
        return inboxCfg.getFolderName();
    }

    /**
     * {@inheritDoc}
     */
    public String getSubject() {
        return outboxCfg.getSubject();
    }

    /**
     * {@inheritDoc}
     */
    public Properties getOutCustomProperties() {
        return outboxCfg.getCustomProperties();
    }

    /**
     * {@inheritDoc}
     */
    public Properties getInCustomProperties() {
        return inboxCfg.getCustomProperties();
    }

    /**
     * {@inheritDoc}
     */
    public String getFromAddress() {
        return outboxCfg.getFrom();
    }

    /**
     * {@inheritDoc}
     */
    public String getStoreFileName() {
        return inboxCfg.getStoreFileName();
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(GridNode destNode, Serializable msg) throws GridSpiException {
        assert destNode != null : "ASSERTION [line=692, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=693, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";

        List<GridNode> nodes = Collections.singletonList(destNode);

        try {
            send0(nodes, msg);
        }
        catch (GridMailException e) {
            throw (GridSpiException)new GridSpiException("Failed to send message [destNode=" + destNode + ", msg=" + msg + ']', e).setData(701, "src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(Collection<GridNode> destNodes, Serializable msg) throws GridSpiException {
        assert destNodes != null : "ASSERTION [line=709, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=710, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";
        assert destNodes.size() != 0 : "ASSERTION [line=711, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";

        try {
            send0(destNodes, msg);
        }
        catch (GridMailException e) {
            throw (GridSpiException)new GridSpiException("Failed to send message [destNodes=" + destNodes + ", msg=" + msg + ']', e).setData(717, "src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java");
        }
    }

    /**
     * Sends given message as attachment to every node on list.
     *
     * @param nodes List of nodes message should be sent to.
     * @param msg Message to be sent.
     * @throws GridMailException Thrown if it's not possible to send message.
     */
    private void send0(Collection<GridNode> nodes, Serializable msg) throws GridMailException {
        assert nodes != null : "ASSERTION [line=729, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=730, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";

        List<UUID> ids = new ArrayList<UUID>(nodes.size());

        GridMailOutboxSession ses = outbox.getSession();

        for (GridNode node : nodes) {
            if (nodeId.equals(node.getId()) == true) {
                // Local node shortcut.
                notifyListener(new GridMailCommunicationMessage(nodeId, msg));
            }
            else {
                String addr = (String)node.getAttribute(createSpiAttributeName(ATTR_EMAIL_ADDR));

                if (addr == null) {
                    throw (GridMailException)new GridMailException("Failed to send message to the destination node. Node does not have" +
                        " email address. Check configuration and make sure that you use the same communication SPI" +
                        " on all nodes. Remote node id: " + node.getId()).setData(745, "src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java");
                }

                ses.addToRecipient(addr);

                ids.add(node.getId());
            }
        }

        if (ids.isEmpty() == false) {
            ses.addAttachment((Serializable)ids, "ids", IDS_IDX, marshaller);
            ses.addAttachment(new GridMailCommunicationMessage(nodeId, msg), "obj", OBJ_IDX, marshaller);

            ses.send();

            if (log.isDebugEnabled() == true) {
                log.debug("Message was sent successfully [msg=" + msg + ", magId=" +
                    Arrays.toString(ses.getMessageId()) + ']');
            }
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
        assertParameter(inboxCfg.getConnectionType() != null, "inConnectionType != null");
        assertParameter(inboxCfg.getProtocol() != null, "inProtocol != null");
        assertParameter(inboxCfg.getHost() != null, "inHost != null");
        assertParameter(inboxCfg.getPort() > 0, "inPort != null");
        assertParameter(inboxCfg.getPort() < 65535, "inPort < 65535");
        assertParameter(inboxCfg.getCustomProperties() != null, "inCustomProperties != null");
        assertParameter(inboxCfg.getFolderName() != null, "folderName != null");
        assertParameter(inboxCfg.getReadBatchSize() > 0, "readBatchSize > 0");
        assertParameter(inboxCfg.getStoreFileName() != null, "storeFileName != null");
        assertParameter(recvDelay > 0, "recvDelay > 0");
        assertParameter(leaveMsgsOnServer > 0, "leaveMsgsOnServer > 0");

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("inboxCfg", inboxCfg));
            log.info(configInfo("outboxCfg", outboxCfg));
            log.info(configInfo("recvDelay", recvDelay));
            log.info(configInfo("leaveMsgsOnServer", leaveMsgsOnServer));
        }

        registerMBean(gridName, this, GridMailCommunicationSpiMBean.class);

        // Set message matcher.
        GridMailInboxMatcher matcher = new GridMailInboxMatcher();

        matcher.setSubject(outboxCfg.getSubject());

        inboxCfg.setLogger(log);

        try {
            inbox = GridMailInboxFactory.createInbox(inboxCfg, matcher, marshaller);

            // Check that mailbox can be opened.
            if (log.isInfoEnabled() == true) {
                log.info("Initializing mailbox... This may take a while.");
            }

            inbox.open(true);
        }
        catch (GridMailException e) {
            throw (GridSpiException)new GridSpiException("Failed to initialize inbox.", e).setData(832, "src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java");
        }
        finally {
            GridUtils.close(inbox, false, log);
        }

        assert inbox != null : "ASSERTION [line=838, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";

        outbox = GridMailOutboxFactory.createOutbox(outboxCfg);

        rcvr = new GridSpiThread(gridName, "grid-mail-comm-receiver-sweeper", log) {
            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            @Override
            protected void body() throws InterruptedException {
                assert inbox != null : "ASSERTION [line=849, file=src/java/org/gridgain/grid/spi/communication/mail/GridMailCommunicationSpi.java]";

                long cleanTime = System.currentTimeMillis();
                long rcvTime = cleanTime;

                while (isInterrupted() == false) {
                    try {
                        // Open mailbox with readonly mode.
                        inbox.open(true);

                        List<GridMailInboxMessage> msgs = inbox.readNew();

                        if (msgs != null) {
                            for (GridMailInboxMessage imapMsg : msgs) {
                                List<UUID> ids = (List<UUID>)imapMsg.getAttachment(IDS_IDX).getContent(marshaller);

                                GridMailCommunicationMessage msg = (GridMailCommunicationMessage)imapMsg.
                                    getAttachment(OBJ_IDX).getContent(marshaller);

                                // Ignore messages with empty attachments.
                                if (msg == null || ids == null) {
                                    continue;
                                }

                                for (UUID rcvNodeId : ids) {
                                    if (nodeId.equals(rcvNodeId) == true) {
                                        if (log.isDebugEnabled() == true) {
                                            log.debug("Received message: " + msg);
                                        }

                                        notifyListener(msg);

                                        break;
                                    }
                                }
                            }
                        }
                    }
                    catch (GridMailException e) {
                        log.error("Failed to get messages.", e);
                    }
                    finally {
                        GridUtils.close(inbox, false, log);
                    }

                    rcvTime = System.currentTimeMillis() + recvDelay;

                    // Sweeper.
                    if (System.currentTimeMillis() >= cleanTime) {
                        try {
                            // Open mailbox with read/write mode.
                            inbox.open(false);

                            int cnt = inbox.removeOld(new Date(System.currentTimeMillis() - leaveMsgsOnServer));

                            if (log.isDebugEnabled() == true) {
                                log.debug("Number of messages sweeper is cleaning on server: " + cnt);
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
                        if (log.isDebugEnabled() == true) {
                            log.debug("Mail receiver-sweeper sleeps for: " + (delta < leaveMsgsOnServer ? delta :
                                leaveMsgsOnServer));
                        }

                        Thread.sleep(delta < leaveMsgsOnServer ? delta : leaveMsgsOnServer);
                    }
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Mail receiver-sweeper stopped.");
                }
            }
        };

        rcvr.start();

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     *
     * @param msg Communication message.
     */
    private void notifyListener(GridMailCommunicationMessage msg) {
        final GridMessageListener localListener = listener;

        if (localListener != null) {
            localListener.onMessage(msg.getNodeId(), msg.getMessage());
        }
        else {
            log.warning("Received communication message without any registered listeners (will ignore) [senderNodeId=" +
                msg.getNodeId() + ']');
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        GridUtils.interrupt(rcvr);

        GridUtils.join(rcvr, log);

        rcvr = null;

        if (inbox != null) {
            try {
                inbox.flush();
            }
            catch (GridMailException e) {
                log.error("Failed to flush messages to local store: " + inboxCfg.getStoreFileName(), e);
            }
        }

        unregisterMBean();

        // Clear resources.
        inbox = null;
        outbox = null;

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
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
        return GridToStringBuilder.toString(GridMailCommunicationSpi.class, this);
    }
}
