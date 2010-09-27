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

package org.gridgain.grid.spi.communication.coherence;

import com.tangosol.net.*;
import com.tangosol.util.*;
import java.io.*;
import java.util.*;
import java.util.UUID;
import java.util.Map.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Oracle Coherence implementation of {@link GridCommunicationSpi} SPI. It uses Coherence data
 * grid framework to communicate with remote nodes.
 * <p>
 * SPI uses Coherence asynchronous call {@link InvocationService#execute(Invocable, Set, InvocationObserver)}
 * to send a message to remote node. If parameter {@link #setAcknowledgment(boolean)}
 * is set to <tt>true</tt>, then this SPI will use Coherence
 * {@link InvocationObserver} to wait for request completion acknowledgment.
 * <p>
 * This SPI has no mandatory parameters.
 * <p>
 * This SPI has the following optional parameters:
 * <ul>
 * <li>Send message acknowledgments (see {@link #setAcknowledgment(boolean)}).</li>
 * <li>Coherence invocation service name created and used by GridGain (see {@link #setServiceName(String)}}).</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridCoherenceCommunicationSpi needs to be explicitely configured to override default
 * TCP communication SPI.
 * <pre name="code" class="java">
 * GridCoherenceCommunicationSpi commSpi = new GridCoherenceCommunicationSpi();
 *
 * // Override default false setting.
 * commSpi.setAcknowledgement(true);
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
 * GridCoherenceCommunicationSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *       ...
 *       &lt;property name="communicationSpi"&gt;
 *           &lt;bean class="org.gridgain.grid.spi.communication.coherence.GridCoherenceCommunicationSpi"&gt;
 *               &lt;property name="acknowledgment" value="true"/&gt;
 *           &lt;/bean&gt;
 *       &lt;/property&gt;
 *       ...
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
 * @see GridCommunicationSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(false)
public class GridCoherenceCommunicationSpi extends GridSpiAdapter implements GridCommunicationSpi,
    GridCoherenceCommunicationSpiMBean {
    /**
     * Name of cluster member Id attribute added to local node attributes at startup
     * (value is <tt>comm.coherence.member.uid</tt>).
     */
    public static final String ATTR_NODE_UID = "comm.coherence.member.uid";

    /** Default Coherence invocation service name (value is <tt>gridgain.comm.srvc</tt>). */
    public static final String DFLT_GRIDGAIN_SRVC = "gridgain.comm.srvc";

    /** Coherence service name attribute. */
    private static final String COHERENCE_SERVICE_NAME = "gridgain:communication:coherenceservicename";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** Acknowledgment property. */
    private boolean ack = false;

    /** Coherence service name. */
    private String srvcName = DFLT_GRIDGAIN_SRVC;

    /** */
    private volatile GridMessageListener listener = null;

    /** Coherence invocation service. */
    private InvocationService srvc = null;

    /** Map of all discovered nodes with Coherence member UID's. */
    private final Map<UUID, UID> allNodes = new HashMap<UUID, UID>();

    /** */
    private MemberListener mbrListener = null;

    /**
     * Sets sending acknowledgment property. This configuration parameter is optional.
     * Used in {@link #sendMessage(Collection, Serializable)}
     * to send message to remote nodes with or without waiting for completion.
     * <p>
     * If not provided, default value is <tt>false</tt>.
     *
     * @param ack Sending acknowledgment.
     */
    @GridSpiConfiguration(optional = true)
    public void setAcknowledgment(boolean ack) {
        this.ack = ack;
    }

    /**
     * Sets name for Coherence service invocation used in grid.
     * <p>
     * If not provided, default value is {@link #DFLT_GRIDGAIN_SRVC}.
     *
     * @param srvcName Invocation service name.
     */
    @GridSpiConfiguration(optional = true)
    public void setServiceName(String srvcName) {
        this.srvcName = srvcName;
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalUid() {
        return srvc == null ? null :
            GridUtils.byteArray2HexString(srvc.getCluster().getLocalMember().getUid().toByteArray());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAcknowledgment() {
        return ack;
    }

    /**
     * {@inheritDoc}
     */
    public String getServiceName() {
        return srvcName;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        assertParameter(srvcName != null, "srvcName != null");

        // Get coherence invocation service.
        srvc = CacheFactory.getInvocationService(srvcName);

        if (srvc == null) {
            throw (GridSpiException)new GridSpiException("Failed to create coherence invocation service in cluster.").setData(213, "src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java");
        }

        // Set communication attributes for node.
        return GridUtils.makeMap(createSpiAttributeName(COHERENCE_SERVICE_NAME), srvcName);
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("ack", ack));
            log.info(configInfo("srvcName", srvcName));
        }

        srvc.setUserContext(this);

        mbrListener = createServiceMembershipListener();

        srvc.addMemberListener(mbrListener);

        // Make grid nodes search in cluster.
        findRemoteMember(null);

        registerMBean(gridName, this, GridCoherenceCommunicationSpiMBean.class);

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        if (srvc != null) {
            // Clear context.
            srvc.setUserContext(null);

            srvc.removeMemberListener(mbrListener);

            srvc.shutdown();
        }

        srvc = null;

        synchronized (allNodes) {
            allNodes.clear();
        }

        unregisterMBean();

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(GridNode destNode, Serializable msg) throws GridSpiException {
        assert destNode != null : "ASSERTION [line=281, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=282, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Sending message to node [node=" + destNode + ", msg=" + msg + ']');
        }

        sendMessage(Collections.singletonList(destNode), msg);
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(Collection<GridNode> destNodes, Serializable msg) throws GridSpiException {
        assert destNodes != null : "ASSERTION [line=295, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=296, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";
        assert destNodes.size() != 0 : "ASSERTION [line=297, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Sending message to nodes [nodes=" + destNodes + ", msg=" + msg + ']');
        }

        Set<Member> mbrs = new HashSet<Member>(destNodes.size());

        Map<Member, GridNode> activeNodes = new HashMap<Member, GridNode>();

        GridSpiException err = null;

        for (GridNode node : destNodes) {
            Member mbr = findLocalMember(node);

            // Implicitly scan members.
            if (mbr == null) {
                mbr = findRemoteMember(node);
            }

            if (mbr != null) {
                mbrs.add(mbr);

                activeNodes.put(mbr, node);
            }
            else {
                log.error("Failed to send message. Node not found in cluster [node=" + node +
                    ", msg=" + msg + ']');

                // Store only first exception.
                if (err == null) {
                    err = (GridSpiException)new GridSpiException("Failed to send message. Node not found in cluster [node=" + node +
                        ", msg=" + msg + ']').setData(328, "src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java");
                }
            }
        }

        if (mbrs.size() > 0) {
            if (ack == true) {
                MessageObserver observer = new MessageObserver(activeNodes);

                // Asynchronously invoke the agent task with waiting for observer completion.
                srvc.execute(new GridCoherenceCommunicationAgent(nodeId, msg), mbrs, observer);

                try {
                    observer.waitAcknowledgment();
                }
                catch (InterruptedException e) {
                    if (err == null) {
                        err = (GridSpiException)new GridSpiException("Failed to send message. Sender was interrupted " +
                            "[activeNodes=" + activeNodes + ", msg=" + msg + ']', e).setData(346, "src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java");
                    }

                    throw err;
                }

                if (err == null) {
                    err = observer.getError();
                }
            }
            else {
                // Asynchronously invoke the agent task on set of members.
                srvc.execute(new GridCoherenceCommunicationAgent(nodeId, msg), mbrs,
                    new MessageObserver(activeNodes));
            }
        }

        if (err != null) {
            throw err;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridMessageListener listener) {
        // Note that we allow null listener to be set.
        this.listener = listener;
    }

    /**
     * Method is called by {@link GridCoherenceCommunicationAgent} when arrived.
     *
     * @param nodeId Sender's node Id.
     * @param msg Message object.
     */
    void onMessage(UUID nodeId, Serializable msg) {
        GridMessageListener localCopy = listener;

        if (localCopy != null) {
            localCopy.onMessage(nodeId, msg);
        }
        else {
            log.warning("Received communication message without any registered listeners (will ignore) [senderNodeId=" +
                nodeId + ']');
        }
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

                // If local node joined in cluster again then we need to scan cluster for exist nodes.
                // Usually local node joined in cluster after disconnection (and local cluster
                // services will be restarted).
                if (evt.isLocal() == true) {
                    assert mbr.equals(srvc.getCluster().getLocalMember()) == true : "ASSERTION [line=417, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";

                    Set<UID> mbrUids = new HashSet<UID>(srvc.getCluster().getMemberSet().size());

                    for (Member clusterMbr : (Set<Member>)srvc.getCluster().getMemberSet()) {
                        mbrUids.add(clusterMbr.getUid());
                    }

                    synchronized (allNodes) {
                        allNodes.put(nodeId, mbr.getUid());

                        // Remove old nodeId's.
                        allNodes.values().retainAll(mbrUids);
                    }
                }
                else {
                    Map mbrNodes = srvc.query(new GridCoherenceCommunicationInfoAgent(nodeId),
                        Collections.singleton(mbr));

                    if (log.isDebugEnabled() == true) {
                        log.debug("Received response from joined node [mbr=" + getMemberInfo(mbr) +
                            ", res=" + mbrNodes + ']');
                    }

                    UUID nodeId = null;

                    if (mbrNodes != null && (nodeId = (UUID)mbrNodes.get(mbr)) != null) {
                        synchronized (allNodes) {
                            allNodes.put(nodeId, mbr.getUid());
                        }
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public void memberLeaving(MemberEvent evt) {
                // No-op.
            }

            /**
             * {@inheritDoc}
             */
            public void memberLeft(MemberEvent evt) {
                Member mbr = evt.getMember();

                if (log.isDebugEnabled() == true) {
                    log.debug("Coherence member left: " + getMemberInfo(mbr));
                }

                processDeletedMember(mbr);
            }
        };
    }

    /**
     * Makes search for Cluster memner with defined grid node.
     *
     * @param node Grid node.
     * @return Member in cluster or <tt>null</tt> if not found.
     */
    @SuppressWarnings({"unchecked"})
    private Member findLocalMember(GridNode node) {
        assert node != null : "ASSERTION [line=481, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";
        assert node.getId() != null : "ASSERTION [line=482, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";

        // Check is local node.
        if (nodeId.equals(node.getId()) == true) {
            return srvc.getCluster().getLocalMember();
        }

        UID mbrUid = null;

        synchronized (allNodes) {
            mbrUid = allNodes.get(node.getId());
        }

        if (mbrUid != null) {
            for (Member mbr : (Set<Member>)srvc.getCluster().getMemberSet()) {
                if (mbr.getUid().equals(mbrUid) == true) {
                    return mbr;
                }
            }
        }

        return null;
    }

    /**
     * Find member in cluster and add them in collection.
     * This function sent broadcast message to all nodes in cluster
     * to get grid nodeId's if member was not found in collection.
     *
     * @param node Grid node.
     * @return Member in cluster or <tt>null</tt> if not found.
     */
    @SuppressWarnings({"unchecked"})
    private Member findRemoteMember(GridNode node) {
        // Check is local node.
        if (node != null && nodeId.equals(node.getId()) == true) {
            return srvc.getCluster().getLocalMember();
        }

        Set<Member> mbrs = new HashSet<Member>(srvc.getCluster().getMemberSet().size());

        // Prepare collection of all "unknown" members from cluster.
        synchronized (allNodes) {
            for (Member mbr : (Set<Member>)srvc.getCluster().getMemberSet()) {
                boolean found = false;

                for (UID uid : allNodes.values()) {
                    if (uid.equals(mbr.getUid()) == true) {
                        found = true;

                        break;
                    }
                }

                if (found == false) {
                    mbrs.add(mbr);
                }
            }
        }

        Map mbrNodes = null;

        if (mbrs.isEmpty() == false) {
            mbrNodes = srvc.query(new GridCoherenceCommunicationInfoAgent(nodeId), mbrs);
        }

        synchronized (allNodes) {
            if (mbrNodes != null) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Node scanning returns result [mbrs=" + mbrs + ", res=" + mbrNodes + ']');
                }

                for (Entry<Member, UUID> entry : (Set<Entry<Member, UUID>>)mbrNodes.entrySet()) {
                    if (entry.getValue() != null) {
                        allNodes.put(entry.getValue(), entry.getKey().getUid());
                    }
                }
            }

            if (node == null) {
                return null;
            }
            
            UID mbrUid = allNodes.get(node.getId());

            if (mbrUid != null) {
                for (Member mbr : (Set<Member>)srvc.getCluster().getMemberSet()) {
                    if (mbr.getUid().equals(mbrUid) == true) {
                        return mbr;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Delete member from local collection.
     *
     * @param mbr Cluster member.
     */
    private void processDeletedMember(Member mbr) {
        synchronized (allNodes) {
            for (Iterator<UID> iter = allNodes.values().iterator(); iter.hasNext() == true;) {
                UID mbrUid = iter.next();

                if (mbrUid.equals(mbr.getUid()) == true) {
                    iter.remove();

                    if (log.isDebugEnabled() == true) {
                        log.debug("Remove node from collection: " + getMemberInfo(mbr));
                    }

                    return;
                }
            }
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
        builder.append(", inetAddr=").append(mbr.getAddress());
        builder.append(", port=").append(mbr.getPort());
        builder.append(']');

        return builder.toString();
    }

    /**
     * Observer for messages which was sent to another nodes.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class MessageObserver implements InvocationObserver {
        /** Indicates is sending operation was finished or not. */
        private boolean isCompleted = false;

        /** */
        private final Object mux = new Object();

        /** Message will be sent to these nodes. */
        private Map<Member, GridNode> nodes = null;

        /** */
        private AtomicReference<GridSpiException> err = new AtomicReference<GridSpiException>(null);

        /**
         * Creates observer.
         *
         * @param nodes Nodes where the agents being sent.
         */
        MessageObserver(Map<Member, GridNode> nodes) {
            assert nodes != null : "ASSERTION [line=649, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";
            assert nodes.size() > 0 : "ASSERTION [line=650, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";

            this.nodes = nodes;
        }

        /**
         * {@inheritDoc}
         */
        public void memberCompleted(Member mbr, Object obj) {
            if (log.isDebugEnabled() == true) {
                log.debug("Message was sent to node: " + getMemberInfo(mbr));
            }
        }

        /**
         * {@inheritDoc}
         */
        public void memberFailed(Member mbr, Throwable t) {
            log.error("Failed to send message: " + getMemberInfo(mbr), t);

            GridNode failedNode = nodes.get(mbr);

            assert failedNode != null : "ASSERTION [line=672, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java]";

            String errMsg = "Failed to send message: " + failedNode;

            log.error(errMsg, t);

            // Record exception.
            // Only record the first exception.
            if (err.get() == null) {
                // This temp variable is required to overcome bug
                // in Ant preparer task for now..
                GridSpiException e1 = (GridSpiException)new GridSpiException(errMsg, t).setData(683, "src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationSpi.java");

                // Only record the first exception.
                err.compareAndSet(null, e1);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void memberLeft(Member mbr) {
            log.error("Member left and message wasn't sent: " + getMemberInfo(mbr));
        }

        /**
         * {@inheritDoc}
         */
        public void invocationCompleted() {
            if (log.isDebugEnabled() == true) {
                log.debug("Message observer invocation completed: " + nodes);
            }

            synchronized (mux) {
                isCompleted = true;

                mux.notifyAll();
            }
        }

        /**
         * Called by SPI when message has been sent to more than one node.
         *
         * @return Exception object or <tt>null</tt> if no errors.
         */
        public GridSpiException getError() {
            return err.get();
        }

        /**
         * Wait until message was sent.
         *
         * @throws InterruptedException This exception is propagated from {@link Object#wait()}.
         */
        public void waitAcknowledgment() throws InterruptedException {
            synchronized (mux) {
                while (true) {
                    // This condition is taken out of the loop to avoid
                    // potentially wrong optimization by the compiler of
                    // moving field access out of the loop causing this loop
                    // to never exit.
                    if (isCompleted == true) {
                        break;
                    }

                    mux.wait();
                }
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
        attrs.add(createSpiAttributeName(COHERENCE_SERVICE_NAME));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCoherenceCommunicationSpi.class, this);
    }
}
