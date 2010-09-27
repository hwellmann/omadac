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

package org.gridgain.grid.kernal.managers.communication;

import static org.gridgain.grid.kernal.managers.communication.GridCommunicationThreadPolicy.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.discovery.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.runnable.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridCommunicationManager extends GridManagerAdapter<GridCommunicationSpi> {
    /** */
    public static final String USER_COMM_TOPIC = "gridgain:user:comm";

    /** */
    public static final int MAX_CLOSED_TOPICS = 10000;

    /** */
    private final Map<String, Set<GridMessageListener>> listenerMap = new HashMap<String, Set<GridMessageListener>>();

    /** */
    private GridRunnablePool workerPool = null;

    /** */
    private GridRunnablePool p2PPool = null;

    /** */
    private GridRunnablePool sysPool = null;

    /** */
    private GridDiscoveryListener discoListener = null;

    /** */
    private final Map<String, GridCommunicationMessageSet> msgSetMap =
        new HashMap<String, GridCommunicationMessageSet>();

    /** */
    private final Map<String, AtomicLong> msgIdMap = new HashMap<String, AtomicLong>();

    /** Finished job topic names queue with the fixed size. */
    private final Set<String> closedTopics = new GridBoundedLinkedHashSet<String>(MAX_CLOSED_TOPICS);

    /** Local node ID. */
    private final UUID locNodeId;

    /** */
    private final long discoDelay;

    /** Cache for messages that were received prior to discovery. */
    private final Map<UUID, List<GridCommunicationMessage>> discoWaitMap =
        new HashMap<UUID, List<GridCommunicationMessage>>();

    /** Communication message listener. */
    private GridMessageListener msgListener = null;

    /** */
    private int callCnt = 0;

    /** */
    private boolean stopping = false;

    /** Mutex. */
    private final Object mux = new Object();

    /** Grid marshaller. */
    private final GridMarshaller marshaller;

    /**
     *
     * @param cfg Grid configuration.
     * @param mgrReg Managers registry.
     * @param procReg Processors registry.
     */
    public GridCommunicationManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridCommunicationSpi.class, cfg, procReg, mgrReg, cfg.getCommunicationSpi());

        assert procReg != null : "ASSERTION [line=111, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        locNodeId = cfg.getNodeId();

        long cfgDiscoDelay = cfg.getDiscoveryStartupDelay();

        discoDelay = cfgDiscoDelay == 0 ? GridConfiguration.DFLT_DISCOVERY_STARTUP_DELAY : cfgDiscoDelay;

        marshaller = cfg.getMarshaller();
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        assertParameter(discoDelay > 0, "discoveryStartupDelay > 0");

        startSpi();

        workerPool = new GridRunnablePool(cfg.getExecutorService(), log);

        p2PPool = new GridRunnablePool(cfg.getPeerClassLoadingExecutorService(), log);

        sysPool = new GridRunnablePool(cfg.getSystemExecutorService(), log);

        getSpi().setListener(msgListener = new GridMessageListener() {
            /**
             * {@inheritDoc}
             */
            public void onMessage(final UUID nodeId, Serializable msg) {
                assert nodeId != null : "ASSERTION [line=141, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
                assert msg != null : "ASSERTION [line=142, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

                if (log.isDebugEnabled() == true) {
                    log.debug("Received communication message: " + msg);
                }

                final GridCommunicationMessage commMsg = (GridCommunicationMessage)msg;

                // If discovery was not started, then it means that we got bound to the
                // same port as a previous node that was started on this IP and got a message
                // destined for a previous node.
                if (commMsg.getDestinationIds().contains(locNodeId) == false) {
                    log.warning("Received message whose destination does not match this node (will ignore): " +
                        msg);

                    return;
                }

                if (nodeId.equals(commMsg.getSenderId()) == false) {
                    log.error("Expected node ID does not match the one in message (message will be ignored) " +
                        "[expected=" + nodeId + ", msgNodeId=" + commMsg.getSenderId() + ']');

                    return;
                }

                boolean isCalled = false;

                try {
                    synchronized (mux) {
                        if (stopping == true) {
                            log.warning("Received communication message while stopping grid (will ignore): " + msg);

                            return;
                        }

                        // Although we check closed topics prior to processing
                        // every message, we still check it here to avoid redundant
                        // placement of messages on wait list whenever possible.
                        if (closedTopics.contains(commMsg.getTopic()) == true) {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Message is ignored as it came for the closed topic: " + msg);
                            }

                            return;
                        }

                        callCnt++;

                        isCalled = true;

                        // Remove expired messages from wait list.
                        processWaitList();

                        // Received message before a node got discovered or after it left.
                        if (mgrReg.getDiscoveryManager().getNode(nodeId) == null) {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Adding message to waiting list [senderId=" + nodeId + ", msg=" + msg +
                                    ']');
                            }

                            addToWaitList(commMsg);

                            return;
                        }
                    }

                    // If message is urgent, then process in peer executor service.
                    // This is done to avoid extra waiting and potential deadlocks
                    // as thread pool may not have any available threads to give.
                    if (commMsg.getPolicy() == NEW_THREAD) {
                        processUrgentMessage(nodeId, commMsg);
                    }
                    // Execute in thread pool.
                    else {
                        assert commMsg.getPolicy() == POOLED_THREAD : "ASSERTION [line=216, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

                        if (commMsg.isOrdered() == false) {
                            processRegularMessage(nodeId, commMsg);
                        }
                        else {
                            processOrderedMessage(nodeId, commMsg);
                        }
                    }
                }
                finally {
                    if (isCalled == true) {
                        synchronized (mux) {
                            callCnt--;

                            if (callCnt == 0) {
                                mux.notifyAll();
                            }
                        }
                    }
                }
            }
        });

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStart() throws GridException {
        super.onKernalStart();

        discoListener = new GridDiscoveryListener() {
            /*
             * This listener will remove all message sets that came
             * from failed or left node.
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                switch (type) {
                    case JOINED: {
                        List<GridCommunicationMessage> waitList = null;

                        synchronized (mux) {
                            // Note, that we still may get a new wait list
                            // if the joining node left while we are still
                            // in this code. In this case we don't care about it
                            // and will let those messages naturally expire.
                            waitList = discoWaitMap.remove(node.getId());
                        }

                        if (waitList != null) {
                            // Process messages on wait list outside of synchronization.
                            for (GridCommunicationMessage msg : waitList) {
                                msgListener.onMessage(msg.getSenderId(), msg);
                            }
                        }

                        break;
                    }

                    case FAILED:
                    case LEFT: {
                        List<GridCommunicationMessageSet> timeoutSet = new ArrayList<GridCommunicationMessageSet>();

                        synchronized (mux) {
                            // Remove messages waiting for this node to join.
                            List<GridCommunicationMessage> waitList = discoWaitMap.remove(node.getId());

                            if (log.isDebugEnabled() == true) {
                                log.debug("Removed messages from discovery startup delay list " +
                                    "(sender node left topology): " + waitList);
                            }

                            // Clean up ordered messages.
                            for (Iterator<GridCommunicationMessageSet> iter = msgSetMap.values().iterator();
                                iter.hasNext() == true;) {
                                GridCommunicationMessageSet set = iter.next();

                                if (set.getNodeId().equals(node.getId()) == true) {
                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Removing message set due to node leaving grid: " + set);
                                    }

                                    timeoutSet.add(set);

                                    iter.remove();

                                    // Node may still send stale messages for this topic
                                    // even after discovery notification is done.
                                    closedTopics.add(set.getTopic());
                                }
                            }
                        }

                        // Unregister timeout listener.
                        for (GridCommunicationMessageSet msgSet : timeoutSet) {
                            procReg.getTimeoutProcessor().removeTimeoutObject(msgSet.getTimeoutId());
                        }

                        break;
                    }

                    case METRICS_UPDATED: { break; } // No-op.

                    default: { assert false; }
                }
            }
        };

        mgrReg.getDiscoveryManager().addDiscoveryListener(discoListener);

        // Make sure that there are no stale nodes due to window between communication
        // manager start and kernal start.
        List<GridCommunicationMessageSet> timeoutSet = new ArrayList<GridCommunicationMessageSet>();

        synchronized (mux) {
            // Clean up ordered messages.
            for (Iterator<GridCommunicationMessageSet> iter = msgSetMap.values().iterator(); iter.hasNext() == true;) {
                GridCommunicationMessageSet set = iter.next();

                // If message set belongs to failed or left node.
                if (mgrReg.getDiscoveryManager().getNode(set.getNodeId()) == null) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Removing message set due to node leaving grid: " + set);
                    }

                    // Remove message sets for this node.
                    iter.remove();

                    timeoutSet.add(set);

                    // Node may still send stale messages for this topic
                    // even after discovery notification is done.
                    closedTopics.add(set.getTopic());
                }
            }
        }

        // Unregister timeout listener.
        for (GridCommunicationMessageSet msgSet : timeoutSet) {
            procReg.getTimeoutProcessor().removeTimeoutObject(msgSet.getTimeoutId());
        }
    }

    /**
     * Adds new message to discovery wait list.
     *
     * @param newMsg Message to add.
     */
    private void addToWaitList(GridCommunicationMessage newMsg) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=370, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        // Add new message.
        List<GridCommunicationMessage> waitList = discoWaitMap.get(newMsg.getSenderId());

        if (waitList == null) {
            discoWaitMap.put(newMsg.getSenderId(), waitList = new LinkedList<GridCommunicationMessage>());
        }

        waitList.add(newMsg);
    }

    /**
     * Removes expired messages from wait list.
     */
    private void processWaitList() {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=386, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        for (Iterator<List<GridCommunicationMessage>> i1 = discoWaitMap.values().iterator(); i1.hasNext() == true;) {
            List<GridCommunicationMessage> msgs = i1.next();

            for (Iterator<GridCommunicationMessage> i2 = msgs.iterator(); i2.hasNext() == true;) {
                GridCommunicationMessage msg = i2.next();

                if (System.currentTimeMillis() - msg.getReceiveTime() > discoDelay) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Removing expired message from discovery wait list. " +
                            "This is normal when received a message after sender node has left the grid. It also may " +
                            "happen (although unlikely) if sender node has not been discovered yet and " +
                            "GridConfiguration.getDiscoveryStartupDelay() value is too small (1 minute by default). " +
                            "Make sure to increase this parameter if you believe that message should have been " +
                            "processed. Removed message: " + msg);
                    }

                    i2.remove();
                }
            }

            if (msgs.isEmpty() == true) {
                i1.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStop() {
        // No more communication messages.
        getSpi().setListener(null);

        synchronized (mux) {
            // Set stopping flag.
            stopping = true;

            // Wait for all method calls to complete. Note that we can only
            // do it after interrupting all tasks.
            while (true) {
                assert callCnt >= 0 : "ASSERTION [line=429, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

                // This condition is taken out of the loop to avoid
                // potentially wrong optimization by the compiler of
                // moving field access out of the loop causing this loop
                // to never exit.
                if (callCnt == 0) {
                    break;
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Waiting for communication listener to finish: " + callCnt);
                }

                try {
                    // Release mux.
                    mux.wait();
                }
                catch (InterruptedException e) {
                    log.error("Got interrupted while stopping (shutdown is incomplete)", e);
                }
            }
        }

        GridDiscoveryManager discoMgr = mgrReg.getDiscoveryManager();

        if (discoMgr != null && discoListener != null) {
            discoMgr.removeDiscoveryListener(discoListener);
        }

        super.onKernalStop();
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws GridException {
        stopSpi();

        // Stop should be done in proper order.
        // First we stop regular workers,
        // Then ordered and urgent at the end.
        if (workerPool != null) {
            workerPool.join(true);
        }

        if (sysPool != null) {
            sysPool.join(true);
        }

        if (p2PPool != null) {
            p2PPool.join(true);
        }

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     *
     * @param nodeId Node ID.
     * @param msg Urgent message.
     */
    private void processUrgentMessage(final UUID nodeId, final GridCommunicationMessage msg) {
        assert msg.getPolicy() == NEW_THREAD : "ASSERTION [line=494, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        final Set<GridMessageListener> listeners;

        synchronized (mux) {
            if (closedTopics.contains(msg.getTopic()) == true) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Message is ignored as it came for the closed topic: " + msg);
                }

                return;
            }

            listeners = listenerMap.get(msg.getTopic());
        }

        if (listeners != null && listeners.isEmpty() == false) {
            try {
                p2PPool.execute(new GridRunnable(cfg.getGridName(), "comm-mgr-urgent-worker", log) {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    protected void body() throws InterruptedException {
                        try {
                            Serializable deserMsg = unmarshal(msg);

                            for (final GridMessageListener listener : listeners) {
                                listener.onMessage(nodeId, deserMsg);
                            }
                        }
                        catch (GridException e) {
                            log.error("Failed to deserialize urgent communication message:" + msg, e);
                        }
                    }
                });
            }
            // Should never happen.
            catch (GridException e) {
                log.warning("Failed to process message (if execution rejection, increase the upper bound " +
                    "on ExecutorService provided in " +
                    "GridConfiguration.getPeerClassLoadingExecutorService()). Will process message in " +
                    "the communication thread.", e);

                try {
                    Serializable deserMsg = unmarshal(msg);

                    for (final GridMessageListener listener : listeners) {
                        listener.onMessage(nodeId, deserMsg);
                    }
                }
                catch (GridException ex) {
                    log.error("Failed to deserialize urgent communication message:" + msg, ex);
                }
            }
        }
    }

    /**
     *
     * @param nodeId Node ID.
     * @param msg Regular message.
     */
    private void processRegularMessage(final UUID nodeId, final GridCommunicationMessage msg) {
        assert msg.getPolicy() == POOLED_THREAD : "ASSERTION [line=558, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert msg.isOrdered() == false : "ASSERTION [line=559, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        final Set<GridMessageListener> listeners;

        synchronized (mux) {
            if (closedTopics.contains(msg.getTopic()) == true) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Message is ignored as it came for the closed topic: " + msg);
                }

                return;
            }

            listeners = listenerMap.get(msg.getTopic());
        }

        if (listeners != null && listeners.isEmpty() == false) {
            // Note, that since listeners are stored in unmodifiable collection, we
            // don't have to hold synchronization lock during event notifications.
            try {
                workerPool.execute(new GridRunnable(cfg.getGridName(), "comm-mgr-unordered-worker", log) {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    protected void body() throws InterruptedException {
                        try {
                            Serializable deserMsg = unmarshal(msg);

                            for (GridMessageListener listener : listeners) {
                                listener.onMessage(nodeId, deserMsg);
                            }
                        }
                        catch (GridException e) {
                            log.error("Failed to deserialize regular communication message:" + msg, e);
                        }
                    }
                });
            }
            catch (GridException e) {
                log.warning("Failed to process message (if execution rejection, increase the upper bound " +
                    "on ExecutorService provided in GridConfiguration.getExecutorService()). Will process message in " +
                    "the communication thread.", e);

                try {
                    Serializable deserMsg = unmarshal(msg);

                    for (GridMessageListener listener : listeners) {
                        listener.onMessage(nodeId, deserMsg);
                    }
                }
                catch (GridException ex) {
                    log.error("Failed to deserialize regular communication message:" + msg, ex);
                }
            }
        }
    }

    /**
     *
     * @param nodeId Node ID.
     * @param msg Ordered message.
     */
    private void processOrderedMessage(UUID nodeId, final GridCommunicationMessage msg) {
        assert msg.getPolicy() == POOLED_THREAD : "ASSERTION [line=623, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert msg.isOrdered() == true : "ASSERTION [line=624, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        assert msg.getTimeout() > 0 : "ASSERTION [line=626, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]. " + "Message timeout of 0 should never be sent: " + msg;

        long endTime = msg.getTimeout() + System.currentTimeMillis();

        // Account for overflow.
        if (endTime < 0) {
            endTime = Long.MAX_VALUE;
        }

        final GridCommunicationMessageSet msgSet;

        boolean isNew = false;

        synchronized (mux) {
            if (closedTopics.contains(msg.getTopic()) == true) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Message is ignored as it came for the closed topic: " + msg);
                }

                return;
            }

            GridCommunicationMessageSet set = msgSetMap.get(msg.getTopic());

            if (set == null) {
                msgSetMap.put(msg.getTopic(), set = new GridCommunicationMessageSet(msg.getTopic(), nodeId, endTime));

                isNew = true;
            }

            msgSet = set;
        }

        if (isNew == true) {
            procReg.getTimeoutProcessor().addTimeoutObject(new GridTimeoutObject() {
                /**
                 * {@inheritDoc}
                 */
                public UUID getTimeoutId() {
                    return msgSet.getTimeoutId();
                }

                /**
                 * {@inheritDoc}
                 */
                public long getEndTime() {
                    return msgSet.getEndTime();
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTimeout() {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Removing message set due to timeout: " + msgSet);
                    }

                    synchronized (mux) {
                        msgSetMap.remove(msgSet.getTopic());
                    }
                }
            });
        }

        final Set<GridMessageListener> listeners;

        synchronized (msgSet) {
            msgSet.add(msg);

            listeners = listenerMap.get(msg.getTopic());
        }

        if (listeners != null && listeners.isEmpty() == false) {
            try {
                sysPool.execute(new GridRunnable(cfg.getGridName(), "comm-mgr-ordered-worker", log) {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    protected void body() throws InterruptedException {
                        unwindMessageSet(msgSet, listeners);
                    }
                });
            }
            catch (GridException e) {
                log.warning("Failed to process message (if execution rejection, increase the upper bound " +
                    "on system executor service provided inGridConfiguration.getSystemExecutorService()). Will " +
                    "process message in the communication thread.", e);

                unwindMessageSet(msgSet, listeners);
            }
        }
        else {
            // Note that we simply keep messages if listener is not
            // registered yet, until one will be registered.
            if (log.isDebugEnabled() == true) {
                log.debug("Received message for unknown listener " +
                    "(messages will be kept until a listener is registered): " + msg);
            }
        }
    }

    /**
     *
     * @param msgSet Message set to unwind.
     * @param listeners Listeners to notify.
     */
    private void unwindMessageSet(GridCommunicationMessageSet msgSet, Collection<GridMessageListener> listeners) {
        // Loop until message set is empty or
        // another thread owns the reservation.
        while (true) {
            Collection<GridCommunicationMessage> orderedMsgs = null;

            boolean selfReserved = false;

            try {
                synchronized (msgSet) {
                    if (msgSet.reserve() == true) {
                        selfReserved = true;

                        orderedMsgs = msgSet.unwind();

                        // No more messages to process.
                        if (orderedMsgs.isEmpty() == true) {
                            return;
                        }
                    }
                    else {
                        // Another thread owns reservation.
                        return;
                    }
                }

                for (GridCommunicationMessage msg : orderedMsgs) {
                    try {
                        Serializable deserMsg = unmarshal(msg);

                        // Don't synchronize on listeners as the collection
                        // is immutable.
                        for (GridMessageListener listener : listeners) {
                            // Notify messages without synchronizing on msgSet.
                            listener.onMessage(msgSet.getNodeId(), deserMsg);
                        }
                    }
                    catch (GridException e) {
                        log.error("Failed to deserialize ordered communication message:" + msg, e);
                    }
                }
            }
            finally {
                if (selfReserved == true) {
                    synchronized (msgSet) {
                        msgSet.unreserve();
                    }
                }
            }
        }
    }

    /**
     * Unmarshall given message with appropriate class loader.
     *
     * @param msg communication message
     * @return Unmarshalled message.
     * @throws GridException If deserialization failed.
     */
    private Serializable unmarshal(GridCommunicationMessage msg) throws GridException {
        return GridMarshalHelper.unmarshal(marshaller, msg.getMessage(), getClass().getClassLoader());
    }


    /**
     *
     * @param node Destination node.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param policy Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    public void sendMessage(GridNode node, String topic, Serializable msg, GridCommunicationThreadPolicy policy)
        throws GridException {
        assert node != null : "ASSERTION [line=807, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert topic != null : "ASSERTION [line=808, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert msg != null : "ASSERTION [line=809, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert policy != null : "ASSERTION [line=810, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        GridByteArrayList serMsg = GridMarshalHelper.marshal(marshaller, msg);

        try {
            getSpi().sendMessage(node, new GridCommunicationMessage(locNodeId, node.getId(), topic, serMsg, policy));
        }
        catch (GridSpiException e) {
            throw (GridException)new GridException("Failed to send message [node=" + node + ", topic=" + topic +
                ", msg=" + msg + ", policy=" + policy + ']', e).setData(818, "src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java");
        }
    }

    /**
     *
     * @param topic Message topic.
     * @return Next ordered message ID.
     */
    public long getNextMessageId(String topic) {
        long msgId;

        synchronized (msgIdMap) {
            AtomicLong lastMsgId = msgIdMap.get(topic);

            if (lastMsgId == null) {
                msgIdMap.put(topic, lastMsgId = new AtomicLong(0));
            }

            msgId = lastMsgId.incrementAndGet();
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Getting next message ID [topic=" + topic + ", msgId=" + msgId + ']');
        }

        return msgId;
    }

    /**
     *
     * @param topic Message topic.
     */
    public void removeMessageId(String topic) {
        if (log.isDebugEnabled() == true) {
            log.debug("Remove message ID for topic: " + topic);
        }

        synchronized (msgIdMap) {
            msgIdMap.remove(topic);
        }
    }

    /**
     *
     * @param node Destination node.
     * @param topic Topic to send the message to.
     * @param msgId Ordered message ID.
     * @param msg Message to send.
     * @param policy Type of processing.
     * @param timeout Timeout to keep a message on receiving queue.
     * @throws GridException Thrown in case of any errors.
     */
    public void sendOrderedMessage(GridNode node, String topic, long msgId, Serializable msg,
        GridCommunicationThreadPolicy policy, long timeout)
        throws GridException {
        assert node != null : "ASSERTION [line=875, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert topic != null : "ASSERTION [line=876, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert msg != null : "ASSERTION [line=877, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert policy != null : "ASSERTION [line=878, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert msgId > 0 : "ASSERTION [line=879, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        GridByteArrayList serMsg = GridMarshalHelper.marshal(marshaller, msg);

        try {
            getSpi().sendMessage(node, new GridCommunicationMessage(locNodeId, node.getId(), topic, serMsg, policy,
                msgId, timeout));
        }
        catch (GridSpiException e) {
            throw (GridException)new GridException("Failed to send ordered message [node=" + node + ", topic=" + topic +
                ", msg=" + msg + ", policy=" + policy + ']', e).setData(888, "src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java");
        }
    }

    /**
     *
     * @param nodes Destination nodes.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param policy Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    public void sendMessage(Collection<GridNode> nodes, String topic, Serializable msg,
        GridCommunicationThreadPolicy policy) throws GridException {
        assert nodes != null : "ASSERTION [line=903, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert topic != null : "ASSERTION [line=904, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert msg != null : "ASSERTION [line=905, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert policy != null : "ASSERTION [line=906, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        try {
            GridByteArrayList serMsg = GridMarshalHelper.marshal(marshaller, msg);

            // Small optimization, as communication SPIs may have lighter implementation for sending
            // messages to one node vs. many.
            if (nodes.size() == 1) {
                GridNode node = nodes.iterator().next();

                getSpi().sendMessage(node, new GridCommunicationMessage(locNodeId, node.getId(), topic, serMsg, policy));
            }
            else if (nodes.size() > 1) {
                List<UUID> destIds = new ArrayList<UUID>(nodes.size());

                for (GridNode node : nodes) {
                    destIds.add(node.getId());
                }

                getSpi().sendMessage(nodes, new GridCommunicationMessage(locNodeId, destIds, topic, serMsg, policy));
            }
            else {
                log.warning("Failed to send message to empty nodes collection [topic="  + topic + ", msg=" +
                    msg + ", policy=" + policy + ']');
            }
        }
        catch (GridSpiException e) {
            throw (GridException)new GridException("Failed to send message [nodes=" + nodes + ", topic=" + topic +
                ", msg=" + msg + ", policy=" + policy + ']', e).setData(933, "src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java");
        }
    }

    /**
     *
     * @param topic Listener's topic.
     * @param listener Listener to add.
     */
    public void addMessageListener(final String topic, final GridMessageListener listener) {
        assert listener != null : "ASSERTION [line=944, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";
        assert topic != null : "ASSERTION [line=945, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        Set<GridMessageListener> listeners;

        final GridCommunicationMessageSet msgSet;

        synchronized (mux) {
            Set<GridMessageListener> temp = listenerMap.get(topic);

            if (temp == null) {
                // The returned set is immutable.
                listeners = Collections.singleton(listener);

                msgSet = msgSetMap.get(topic);

                // Make sure that new topic is not in the list of closed topics.
                closedTopics.remove(topic);
            }
            else {
                msgSet = null;

                // Avoid resizing.
                listeners = new HashSet<GridMessageListener>(temp.size() + 1, 1.0f);

                listeners.addAll(temp);

                listeners.add(listener);

                // Seal it.
                listeners = Collections.unmodifiableSet(listeners);
            }

            listenerMap.put(topic, listeners);
        }

        if (msgSet != null) {
            try {
                workerPool.execute(new GridRunnable(cfg.getGridName(), "comm-mgr-ordered-worker", log) {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    protected void body() throws InterruptedException {
                        unwindMessageSet(msgSet, Collections.singletonList(listener));
                    }
                });
            }
            catch (GridException e) {
                log.warning("Failed to process message (if execution rejection, increase the upper bound " +
                    "on executor service provided in GridConfiguration.getExecutorService()). Will process message " +
                    "in the communication thread.", e);

                unwindMessageSet(msgSet, Collections.singletonList(listener));
            }
        }
    }

    /**
     *
     * @param topic Message topic.
     * @return Whether or not listener was indeed removed.
     */
    public boolean removeMessageListener(String topic) {
        return removeMessageListener(topic, null);
    }

    /**
     *
     * @param topic Listener's topic.
     * @param listener Listener to remove.
     * @return Whether or not the listener was removed.
     */
    public boolean removeMessageListener(String topic, GridMessageListener listener) {
        assert topic != null : "ASSERTION [line=1018, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationManager.java]";

        boolean removed = true;

        GridCommunicationMessageSet msgSet = null;

        synchronized (mux) {
            Set<GridMessageListener> listeners = listenerMap.remove(topic);

            // If removing listener before subscription happened.
            if (listeners == null) {
                msgSet = msgSetMap.remove(topic);

                closedTopics.add(topic);

                removed = false;
            }
            else {
                // If listener is null, then remove all listeners.
                if (listener == null) {
                    msgSet = msgSetMap.remove(topic);

                    closedTopics.add(topic);
                }
                else if (listeners.contains(listener) == true) {
                    // If removing last subscribed listener.
                    if (listeners.size() == 1) {
                        msgSet = msgSetMap.remove(topic);

                        closedTopics.add(topic);
                    }
                    // Remove the specified listener and leave
                    // other subscribed listeners untouched.
                    else {
                        listeners = new HashSet<GridMessageListener>(listeners);

                        listeners.remove(listener);

                        // Seal it.
                        listeners = Collections.unmodifiableSet(listeners);

                        listenerMap.put(topic, listeners);
                    }
                }
                // Nothing to remove.
                else {
                    removed = false;
                }
            }
        }

        if (msgSet != null) {
            procReg.getTimeoutProcessor().removeTimeoutObject(msgSet.getTimeoutId());
        }

        if (removed == true && log.isDebugEnabled() == true) {
            log.debug("Removed message listener [topic=" + topic + ", listener=" + listener + ']');
        }

        return removed;
    }
}
