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

package org.gridgain.grid.kernal.managers.eventstorage;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import static org.gridgain.grid.kernal.GridTopic.EVTSTORAGE_TOPIC;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.discovery.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.eventstorage.*;
import org.gridgain.grid.util.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "ThrowableResultOfMethodCallIgnored"})
public class GridEventStorageManager extends GridManagerAdapter<GridEventStorageSpi> {
    /** */
    private Set<GridLocalEventListener> listeners = null;

    /** */
    private final UUID locNodeId;

    /** */
    private RequestListener msgListener = null;

    /** */
    private final Object mux = new Object();

    /** Grid marshaller. */
    private final GridMarshaller marshaller;

    /** */
    private boolean stopping = false;

    /** */
    private int callCnt = 0;

    /**
     *
     * @param cfg Grid configuration.
     * @param mgrReg Managers registry.
     * @param procReg Processor registry.
     */
    public GridEventStorageManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridEventStorageSpi.class, cfg, procReg, mgrReg, cfg.getEventStorageSpi());

        locNodeId = cfg.getNodeId();

        marshaller = cfg.getMarshaller();
    }

    /**
     *
     */
    private class RequestListener implements GridMessageListener {
        /**
         * {@inheritDoc}
         */
        public void onMessage(UUID nodeId, Serializable msg) {
            assert nodeId != null : "ASSERTION [line=91, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";
            assert msg != null : "ASSERTION [line=92, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

            if (msg instanceof GridEventStorageMessage == false) {
                log.warning("Received unknown message: " + msg);

                return;
            }

            GridEventStorageMessage req = (GridEventStorageMessage)msg;

            GridNode node = mgrReg.getDiscoveryManager().getNode(nodeId);

            if (node == null) {
                log.warning("Failed to resolve sender node that does not exist: " + nodeId);

                return;
            }

            boolean sendEmtpyReply = false;

            synchronized (mux) {
                if (stopping == true) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received event query request while stopping grid (will ignore): " + msg);
                    }

                    sendEmtpyReply = true;
                }
                else {
                    callCnt++;
                }
            }

            if (sendEmtpyReply == true) {
                Collection<GridEvent> evts = Collections.emptyList();

                // Response message.
                GridEventStorageMessage res = new GridEventStorageMessage(evts, null);

                try {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Sending empty event query response to node [nodeId=" + nodeId + "res=" + res + ']');
                    }

                    mgrReg.getCommunicationManager().sendMessage(node, req.getResponseTopic(), res,
                        GridCommunicationThreadPolicy.POOLED_THREAD);
                }
                catch (GridException e) {
                    log.error("Failed to send empty event query response to node [node=" + nodeId + ", res=" + res + ']', e);
                }
            }
            else {
                try {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received event query request: " + req);
                    }

                    Throwable ex = null;

                    GridEventFilter filter = null;

                    GridDeploymentClass depCls = null;

                    try {
                        Collection<GridEvent> evts = null;

                        //noinspection CatchGenericClass
                        try {
                            depCls = mgrReg.getDeploymentManager().acquireGlobalClass(req.getDeploymentMode(),
                                req.getFilterClassName(), req.getFilterClassName(), req.getSequenceNumber(),
                                req.getUserVersion(), nodeId, req.getClassLoaderId());

                            if (depCls == null) {
                                throw (GridException)new GridException("Failed to obtain deployment for event filter " +
                                    "(is peer class loading turned on?): " + req).setData(165, "src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java");
                            }

                            filter = GridMarshalHelper.unmarshal(marshaller, req.getFilter(), depCls.getClassLoader());

                            // Resource injection.
                            procReg.getResourceProcessor().inject(depCls, filter);

                            // Get local events.
                            evts = queryLocalEvents(filter);
                        }
                        catch (GridException e) {
                            log.error("Failed to query events [nodeId=" + nodeId + ", filter=" + filter + ']', e);

                            evts = Collections.emptyList();

                            ex = e;
                        }
                        catch (Throwable e) {
                            log.error("Failed to query events due to user exception [nodeId=" + nodeId +
                                ", filter=" + filter + ']', e);

                            evts = Collections.emptyList();

                            ex = e;
                        }

                        // Response message.
                        GridEventStorageMessage res = new GridEventStorageMessage(evts, ex);

                        try {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Sending event query response to node [nodeId=" + nodeId + "res=" + res + ']');
                            }

                            mgrReg.getCommunicationManager().sendMessage(node, req.getResponseTopic(), res,
                                GridCommunicationThreadPolicy.POOLED_THREAD);
                        }
                        catch (GridException e) {
                            log.error("Failed to send event query response to node [node=" + nodeId + ", res=" + res + ']', e);
                        }
                    }
                    finally {
                        if (depCls != null) {
                            mgrReg.getDeploymentManager().releaseClass(depCls);
                        }
                    }
                }
                finally {
                    synchronized (mux) {
                        assert callCnt > 0 : "ASSERTION [line=216, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

                        callCnt--;

                        if (callCnt == 0) {
                            mux.notifyAll();
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
    public void onKernalStop() {
        GridCommunicationManager comm = mgrReg.getCommunicationManager();

        if (comm != null) {
            comm.removeMessageListener(EVTSTORAGE_TOPIC.topic(), msgListener);
        }

        msgListener = null;

        synchronized (mux) {
            stopping = true;

            while (callCnt > 0) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Waiting for event queries to complete...");
                }

                try {
                    // Wait for all event queries to complete.
                    mux.wait();
                }
                catch (InterruptedException e) {
                    log.warning("Got interrupted while waiting for event queries to complete: " + e);

                    return;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        startSpi();

        listeners = new HashSet<GridLocalEventListener>();

        msgListener = new RequestListener();

        mgrReg.getCommunicationManager().addMessageListener(EVTSTORAGE_TOPIC.topic(), msgListener);

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws GridException {
        synchronized (mux) {
            listeners = null;
        }

        stopSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     *
     * @param type FIXDOC
     * @param ses FIXDOC
     * @param msg FIXDOC
     */
    public void record(GridEventType type, GridTaskSessionImpl ses, String msg) {
        record(type, ses.getTaskName(), ses.getUserVersion(), ses.getId(), ses.getJobId(), null, msg);
    }

    /**
     *
     * @param type FIXDOC
     */
    public void record(GridEventType type) {
        assert type != null : "ASSERTION [line=310, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

        record(type, null, null, null, null);
    }

    /**
     *
     * @param type Event type.
     * @param node Grid node.
     */
    public void record(GridDiscoveryEventType type, GridNode node) {
        assert type != null : "ASSERTION [line=321, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

        GridEventType evtType = null;
        String msg = null;

        switch (type) {
            case FAILED: {
                evtType = GridEventType.NODE_FAILED;

                msg = "Grid node failed: " + node;

                break;
            }

            case JOINED: {
                evtType = GridEventType.NODE_JOINED;

                msg = "Grid node joined: " + node;

                break;
            }

            case LEFT: {
                evtType = GridEventType.NODE_LEFT;

                msg = "Grid node left: " + node;

                break;
            }

            case METRICS_UPDATED: {
                assert false : "ASSERTION [line=352, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]. " + "METRICS_UPDATED discovery event should not be recorded as event.";

                break;
            }

            default: { assert false : "Invalid discovery event type: " + type; }
        }

        record(evtType, null, null, null, null, node.getId(), null, null, msg);
    }

    /**
     *
     * @param type FIXDOC
     * @param taskName FIXDOC
     * @param codeVer Code version.
     * @param evtNodeId FIXDOC
     * @param msg FIXDOC
     */
    public void record(GridEventType type, String taskName, String codeVer, UUID evtNodeId, String msg) {
        record(type, taskName, codeVer, null, null, evtNodeId, null, null, msg);
    }

    /**
     *
     * @param type FIXDOC
     * @param taskName FIXDOC
     * @param codeVer Task version.
     * @param taskSesId FIXDOC
     * @param msg FIXDOC
     * @param jobId FIXDOC
     * @param nodeId Node id.
     */
    public void record(GridEventType type, String taskName, String codeVer, UUID taskSesId, UUID jobId, UUID nodeId,
        String msg) {
        assert type != null : "ASSERTION [line=387, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";
        record(type, taskName, codeVer, taskSesId, jobId, nodeId, null, null, msg);
    }

    /**
     *
     * @param type FIXDOC
     * @param cpKey FIXDOC
     * @param msg FIXDOC
     */
    public void record(GridEventType type, String cpKey, String msg) {
        record(type, null, null, null, null, null, cpKey, null, msg);
    }

    /**
     *
     * @param type FIXDOC
     * @param taskName FIXDOC
     * @param codeVer Task version.
     * @param taskSesId FIXDOC
     * @param jobId FIXDOC
     * @param evtNodeId FIXDOC
     * @param cpKey FIXDOC
     * @param jobResPolicy FIXDOC
     * @param msg FIXDOC
     */
    public void record(GridEventType type, String taskName, String codeVer, UUID taskSesId, UUID jobId, UUID evtNodeId,
        String cpKey, GridJobResultPolicy jobResPolicy, String msg) {
        assert type != null : "ASSERTION [line=415, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

        GridEventImpl evt = new GridEventImpl();

        evt.setType(type);
        evt.setTimestamp(System.currentTimeMillis());
        evt.setTaskName(taskName);
        evt.setUserVersion(codeVer);
        evt.setLocalNodeId(locNodeId);
        evt.setSessionId(taskSesId);
        evt.setJobId(jobId);
        evt.setMessage(msg);
        evt.setEventNodeId(evtNodeId);
        evt.setCheckpointKey(cpKey);
        evt.setJobResultPolicy(jobResPolicy);

        try {
            getSpi().record(evt);
        }
        catch (GridSpiException e) {
            log.error("Failed to record event: " + evt, e);
        }

        notifyListeners(evt);
    }

    /**
     *
     * @param listener FIXDOC
     */
    public void addGridLocalEvenListener(GridLocalEventListener listener) {
        assert listener != null : "ASSERTION [line=446, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

        synchronized (mux) {
            Set<GridLocalEventListener> newListeners = new HashSet<GridLocalEventListener>(listeners.size() + 1,
                1.0f);

            newListeners.addAll(listeners);

            newListeners.add(listener);

            // Seal it.
            listeners = Collections.unmodifiableSet(newListeners);
        }
    }

    /**
     *
     * @param listener FIXDOC
     * @return FIXDOC
     */
    public boolean removeGridLocalEventListener(GridLocalEventListener listener) {
        assert listener != null : "ASSERTION [line=467, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

        synchronized (mux) {
            if (listeners.contains(listener) == true) {
                listeners = new HashSet<GridLocalEventListener>(listeners);

                listeners.remove(listener);

                // Seal it.
                listeners = Collections.unmodifiableSet(listeners);

                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param evt Event to notify about.
     */
    private void notifyListeners(GridEvent evt) {
        Set<GridLocalEventListener> tmpListeners;

        synchronized (mux) {
            // Safety.
            if (listeners == null) {
                return;
            }

            tmpListeners = listeners;
        }

        for (GridLocalEventListener listener : tmpListeners) {
            listener.onEvent(evt);
        }
    }

    /**
     *
     * @param filter FIXDOC
     * @return FIXDOC
     */
    public List<GridEvent> queryLocalEvents(GridEventFilter filter) {
        assert filter != null : "ASSERTION [line=512, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

        return getSpi().queryLocalEvents(filter);
    }

    /**
     *
     * @param filter FIXDOC
     * @param nodes FIXDOC
     * @param timeout Maximum time to wait for result, if <tt>0</tt>, then wait until result is received.
     * @return FIXDOC
     * @throws GridException FIXDOC
     * @see Grid#queryEvents(GridEventFilter, Collection, long)
     */
    @SuppressWarnings({"CallToNativeMethodWhileLocked"})
    public List<GridEvent> query(GridEventFilter filter, Collection<GridNode> nodes, long timeout)
        throws GridException {
        assert filter != null : "ASSERTION [line=529, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";
        assert nodes != null : "ASSERTION [line=530, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";
        assert locNodeId != null : "ASSERTION [line=531, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

        if (nodes.isEmpty() == true) {
            log.warning("Failed to query events for empty nodes collection.");

            return Collections.emptyList();
        }

        GridCommunicationManager commMgr = mgrReg.getCommunicationManager();
        GridDiscoveryManager discoMgr = mgrReg.getDiscoveryManager();

        final List<GridEvent> evts = new ArrayList<GridEvent>();

        final AtomicReference<Throwable> err = new AtomicReference<Throwable>(null);

        final Set<UUID> uids = new HashSet<UUID>();

        final Object queryMux = new Object();

        for (GridNode node : nodes) {
            uids.add(node.getId());
        }

        GridDiscoveryListener discoLsnr = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                if (type == GridDiscoveryEventType.LEFT || type == GridDiscoveryEventType.FAILED) {
                    synchronized (queryMux) {
                        uids.remove(node.getId());

                        if (uids.isEmpty() == true) {
                            queryMux.notifyAll();
                        }
                    }
                }

            }
        };

        GridMessageListener resListener = new GridMessageListener() {
            /**
             * {@inheritDoc}
             */
            public void onMessage(UUID nodeId, Serializable msg) {
                assert nodeId != null : "ASSERTION [line=577, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";
                assert msg != null : "ASSERTION [line=578, file=src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java]";

                if (msg instanceof GridEventStorageMessage == false) {
                    log.error("Received unknown message: " + msg);

                    return;
                }

                GridEventStorageMessage res = (GridEventStorageMessage)msg;

                synchronized (queryMux) {
                    if (uids.remove(nodeId) == true) {
                        if (res.getEvents() != null) {
                            evts.addAll(res.getEvents());
                        }
                    }
                    else {
                        log.warning("Received duplicate response (ignoring) [nodeId=" + nodeId + ", msg=" + res + ']');
                    }

                    if (res.getException() != null) {
                        err.set(res.getException());
                    }

                    if (uids.isEmpty() == true || err.get() != null) {
                        queryMux.notifyAll();
                    }
                }
            }
        };

        String resTopic = EVTSTORAGE_TOPIC.topic(UUID.randomUUID());

        GridDeploymentClass depCls = null;

        try {
            // Add listeners.
            discoMgr.addDiscoveryListener(discoLsnr);
            commMgr.addMessageListener(resTopic, resListener);

            GridByteArrayList serFilter = GridMarshalHelper.marshal(marshaller, filter);

            depCls = mgrReg.getDeploymentManager().deployAndAcquire(filter.getClass(),
                GridUtils.detectClassLoader(filter.getClass()));

            if (depCls == null) {
                throw (GridException)new GridException("Failed to deploy event filter: " + filter).setData(624, "src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java");
            }

            GridEventStorageMessage msg = new GridEventStorageMessage(
                resTopic,
                serFilter,
                filter.getClass().getName(),
                depCls.getClassLoaderId(),
                depCls.getDeploymentMode(),
                depCls.getSequenceNumber(),
                depCls.getUserVersion());

            commMgr.sendMessage(nodes, EVTSTORAGE_TOPIC.topic(), msg, GridCommunicationThreadPolicy.POOLED_THREAD);

            if (timeout == 0) {
                timeout = Long.MAX_VALUE;
            }

            long now = System.currentTimeMillis();

            // Account for overflow of long value.
            long endTime = now + timeout <= 0 ? Long.MAX_VALUE : now + timeout;

            long delta = timeout;

            Collection<UUID> uidsCopy = null;

            synchronized (queryMux) {
                try {
                    while (uids.isEmpty() == false && err.get() == null && delta > 0) {
                        queryMux.wait(delta);

                        delta = endTime - System.currentTimeMillis();
                    }
                }
                catch (InterruptedException e) {
                    throw (GridException)new GridException("Got interrupted while waiting for event query responses.", e).setData(660, "src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java");
                }

                if (err.get() != null) {
                    throw (GridException)new GridException("Failed to query events due to exception on remote node.", err.get()).setData(664, "src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java");
                }

                if (uids.isEmpty() == false) {
                    uidsCopy = new LinkedList<UUID>(uids);
                }
            }

            // Outsite of synchronization.
            if (uidsCopy != null) {
                for (Iterator<UUID> iter = uidsCopy.iterator(); iter.hasNext() == true;) {
                    UUID uid = iter.next();

                    // Ignore nodes that have left the grid.
                    if (mgrReg.getDiscoveryManager().getNode(uid) == null) {
                        iter.remove();
                    }
                }

                if (uidsCopy.isEmpty() == false) {
                    throw (GridException)new GridException("Failed to receive event query response from following nodes: " + uidsCopy).setData(684, "src/java/org/gridgain/grid/kernal/managers/eventstorage/GridEventStorageManager.java");
                }
            }
        }
        finally {
            commMgr.removeMessageListener(resTopic, resListener);
            discoMgr.removeDiscoveryListener(discoLsnr);

            // Release associated class.
            if (depCls != null) {
                mgrReg.getDeploymentManager().releaseClass(depCls);
            }
        }

        return evts;
    }
}

