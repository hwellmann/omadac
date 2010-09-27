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

package org.gridgain.grid.kernal.managers.checkpoint;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.kernal.*;
import static org.gridgain.grid.kernal.GridTopic.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;

/**
 * This class defines a checkpoint manager.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridCheckpointManager extends GridManagerAdapter<GridCheckpointSpi> {
    /** */
    private final CheckpointRequestListener listener = new CheckpointRequestListener();

    /** */
    private final Map<UUID, Set<String>> keyMap = new HashMap<UUID, Set<String>>();

    /** Grid marshaller. */
    private final GridMarshaller marshaller;

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param cfg Grid configuration.
     * @param mgrReg Managers registry.
     * @param procReg Processor registry.
     */
    public GridCheckpointManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridCheckpointSpi.class, cfg, procReg, mgrReg, cfg.getCheckpointSpi());

        marshaller = cfg.getMarshaller();
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        startSpi();

        mgrReg.getCommunicationManager().addMessageListener(CHECKPOINT.topic(), listener);

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws GridException {
        GridCommunicationManager comm = mgrReg.getCommunicationManager();

        if (comm != null) {
            comm.removeMessageListener(CHECKPOINT.topic(), listener);
        }

        stopSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     *
     * @param ses Task session.
     * @param key Checkpoint key.
     * @param state Checkpoint state to save.
     * @param scope Checkpoint scope.
     * @param timeout Checkpoint timeout.
     * @throws GridException Thrown in case of any errors.
     */
    public void storeCheckpoint(GridTaskSessionImpl ses, String key, Serializable state, GridCheckpointScope scope,
        long timeout) throws GridException {
        long now = System.currentTimeMillis();

        try {
            switch (scope) {
                case GLOBAL_SCOPE: {
                    byte[] data = state == null ? null : GridMarshalHelper.marshal(marshaller, state).getArray();

                    getSpi(ses.getCheckpointSpi()).saveCheckpoint(key, data, timeout);

                    record(GridEventType.CHECKPOINT_SAVED, key);

                    break;
                }

                case SESSION_SCOPE: {
                    if (now > ses.getEndTime()) {
                        log.warning("Checkpoint will not be saved due to session timeout [key=" + key +
                            ", val=" + state + ", ses=" + ses + ']');

                        return;
                    }

                    if (now + timeout > ses.getEndTime() || now + timeout < 0) {
                        timeout = ses.getEndTime() - now;
                    }

                    // Save it first to avoid getting null value on another node.
                    byte[] data = state == null ? null : GridMarshalHelper.marshal(marshaller, state).getArray();

                    final Set<String> keys;

                    synchronized (mux) {
                        keys = keyMap.get(ses.getJobId() == null ? ses.getId() : ses.getJobId());
                    }

                    // Note: Check that keys exists because session may be invalidated during saving
                    // checkpoint from GridFuture.
                    if (keys != null) {
                        synchronized (keys) {
                            keys.add(key);
                        }

                        if (ses.getJobId() != null) {
                            GridNode node = mgrReg.getDiscoveryManager().getNode(ses.getTaskNodeId());

                            if (node != null) {
                                mgrReg.getCommunicationManager().sendMessage(
                                    node,
                                    CHECKPOINT.topic(),
                                    new GridCheckpointRequest(ses.getId(), key, ses.getCheckpointSpi()),
                                    GridCommunicationThreadPolicy.POOLED_THREAD);
                            }
                        }

                        getSpi(ses.getCheckpointSpi()).saveCheckpoint(key, data, timeout);

                        record(GridEventType.CHECKPOINT_SAVED, key, ses);
                    }
                    else {
                        log.warning("Checkpoint will not be saved due to session invalidation [key=" + key +
                            ", val=" + state + ", ses=" + ses + ']');
                    }

                    break;
                }

                default: { assert false : "Unknown checkpoint scope: " + scope; }
            }
        }
        catch (GridSpiException e) {
            throw (GridException)new GridException("Failed to save checkpoint [key=" + key + ", val=" + state + ", scope=" +
               scope + ", timeout=" + timeout + ']', e).setData(179, "src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java");
        }
    }

    /**
     *
     * @param key Checkpoint key.
     * @return Whether or not checkpoint was removed.
     */
    public boolean removeCheckpoint(String key) {
        assert key != null : "ASSERTION [line=190, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java]";

        boolean removed = false;

        for (GridCheckpointSpi spi : getSpis()) {
            if (removed == false) {
                removed = spi.removeCheckpoint(key);
            }
            else {
                spi.removeCheckpoint(key);
            }
        }

        // Generate event only if we removed checkpoint.
        if (removed == true) {
            record(GridEventType.CHECKPOINT_REMOVED, key, null);
        }

        return removed;
    }

    /**
     *
     * @param ses Task session.
     * @param key Checkpoint key.
     * @return Whether or not checkpoint was removed.
     */
    public boolean removeCheckpoint(GridTaskSessionImpl ses, String key) {
        assert ses != null : "ASSERTION [line=218, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java]";
        assert key != null : "ASSERTION [line=219, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java]";

        final Set<String> keys;

        synchronized (mux) {
            keys = keyMap.get(ses.getJobId() == null ? ses.getId() : ses.getJobId());
        }

        boolean removed = false;

        // Note: Check that keys exists because session may be invalidated during removing
        // checkpoint from GridFuture.
        if (keys != null) {
            synchronized (keys) {
                keys.remove(key);
            }

            removed = getSpi(ses.getCheckpointSpi()).removeCheckpoint(key);

            // Generate event only if we removed checkpoint.
            if (removed == true) {
                record(GridEventType.CHECKPOINT_REMOVED, key, ses);
            }
        }
        else {
            log.warning("Checkpoint will not be removed due to session invalidation [key=" + key +
                ", ses=" + ses + ']');
        }

        return removed;
    }

    /**
     *
     * @param ses Task session.
     * @param key Checkpoint key.
     * @return Loaded checkpoint.
     * @throws GridException Thrown in case of any errors.
     */
    public Serializable loadCheckpoint(GridTaskSessionImpl ses, String key) throws GridException {
        assert ses != null : "ASSERTION [line=259, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java]";
        assert key != null : "ASSERTION [line=260, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java]";

        try {
            byte[] data = getSpi(ses.getCheckpointSpi()).loadCheckpoint(key);

            Serializable state = null;

            // Always deserialize with task/session class loader.
            if (data != null) {
                state = GridMarshalHelper.unmarshal(marshaller, new GridByteArrayList(data), ses.getClassLoader());
            }

            record(GridEventType.CHECKPOINT_LOADED, key, ses);

            return state;
        }
        catch (GridSpiException e) {
            throw (GridException)new GridException("Failed to load checkpoint: " + key, e).setData(277, "src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java");
        }
    }

    /**
     *
     * @param ses Task session.
     */
    public void onSessionStart(GridTaskSessionImpl ses) {
        synchronized (mux) {
            keyMap.put(ses.getJobId() == null ? ses.getId() : ses.getJobId(), new HashSet<String>());
        }
    }

    /**
     *
     * @param ses Task session.
     * @param cleanup Whether cleanup or not.
     */
    public void onSessionEnd(GridTaskSessionImpl ses, boolean cleanup) {
        // If on task node.
        if (ses.getJobId() == null) {
            final Set<String> keys;

            synchronized (mux) {
                keys = keyMap.remove(ses.getId());
            }

            if (keys != null) {
                final Set<String> copy;

                synchronized (keys) {
                    copy = new HashSet<String>(keys);
                }

                for (String key : copy) {
                    boolean removed = getSpi(ses.getCheckpointSpi()).removeCheckpoint(key);

                    if (removed == true) {
                        record(GridEventType.CHECKPOINT_REMOVED, key, ses);
                    }
                }
            }
        }
        // If on job node.
        else {
            final Set<String> keys;

            // Clean up memory.
            synchronized (mux) {
                keys = keyMap.remove(ses.getJobId());
            }

            if (cleanup == true && keys != null) {
                final Set<String> copy;

                synchronized (keys) {
                    copy = new HashSet<String>(keys);
                }

                for (String key : copy) {
                    boolean removed = getSpi(ses.getCheckpointSpi()).removeCheckpoint(key);

                    if (removed == true) {
                        record(GridEventType.CHECKPOINT_REMOVED, key, ses);
                    }
                }
            }
        }
    }

    /**
     *
     * @param type Event type.
     * @param key Checkpoint key.
     */
    private void record(GridEventType type, String key) {
        record(type, key, null);
    }

    /**
     *
     * @param type Event type.
     * @param key Checkpoint key.
     * @param ses Grid task session.
     */
    private void record(GridEventType type, String key, GridTaskSessionImpl ses) {
        String msg = null;

        if (type == GridEventType.CHECKPOINT_SAVED) {
            msg = "Checkpoint saved: " + key;
        }
        else if (type == GridEventType.CHECKPOINT_LOADED) {
            msg = "Checkpoint loaded: " + key;
        }
        else {
            assert type == GridEventType.CHECKPOINT_REMOVED : "ASSERTION [line=373, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointManager.java]. " + "Invalid event type: " + type;

            msg = "Checkpoint removed: " + key;
        }

        if (ses != null) {
            mgrReg.getEventStorageManager().record(type, ses.getTaskName(), ses.getUserVersion(),
                ses.getId(), ses.getJobId(), null, key, null, msg);
        }
        else {
            mgrReg.getEventStorageManager().record(type, key, msg);
        }
    }

    /**
     *
     */
    private class CheckpointRequestListener implements GridMessageListener {
        /**
         *
         * @param nodeId ID of the node that sent this message.
         * @param msg Received message.
         */
        public void onMessage(UUID nodeId, Serializable msg) {
            GridCheckpointRequest req = (GridCheckpointRequest)msg;

            if (log.isDebugEnabled() == true) {
                log.debug("Received checkpoint request: " + req);
            }

            final Set<String> keys;

            synchronized (mux) {
                keys = keyMap.get(req.getSessionId());
            }

            if (keys != null) {
                synchronized (keys) {
                    keys.add(req.getKey());
                }
            }
            // Session is over, simply remove stored checkpoint.
            else {
                boolean removed = getSpi(req.getCheckpointSpi()).removeCheckpoint(req.getKey());

                if (removed == true) {
                    record(GridEventType.CHECKPOINT_REMOVED, req.getKey(), null);
                }
            }
        }
    }
}
