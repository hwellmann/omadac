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

package org.gridgain.grid.kernal.managers.deployment;

import java.util.*;
import java.util.concurrent.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Deployment storage for {@link GridDeploymentMode#PRIVATE} and
 * {@link GridDeploymentMode#ISOLATED} modes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridDeploymentPerLoaderStore extends GridDeploymentStoreAdapter {
    /** Cache keyed by class loader ID. */
    private Map<UUID, IsolatedDeployment> cache = new HashMap<UUID, IsolatedDeployment>();

    /** Discovery listener. */
    private GridDiscoveryListener discoLsnr = null;

    /** Mutex. */
    private final Object mux = new Object();

    /**
     * @param spi Underlying SPI.
     * @param cfg Grid configuration.
     * @param mgrReg Manager registry.
     * @param procReg Processor registry.
     * @param comm Deployment communication.
     */
    GridDeploymentPerLoaderStore(GridDeploymentSpi spi, GridConfiguration cfg, GridManagerRegistry mgrReg,
        GridProcessorRegistry procReg, GridDeploymentCommunication comm) {
        super(spi, cfg, mgrReg, procReg, comm);
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        Set<IsolatedDeployment> copy = new HashSet<IsolatedDeployment>();

        synchronized (mux) {
            for (IsolatedDeployment dep : cache.values()) {
                assert dep.getUsages() == 0 : "ASSERTION [line=79, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]. " + "Found left over usage for deployment: " + dep;

                // Mark undeployed. This way if any event hits after stop,
                // undeployment won't happen twice.
                dep.onUndeployed();

                copy.add(dep);
            }

            cache.clear();
        }

        for (IsolatedDeployment dep : copy) {
            dep.recordUndeployed();
        }

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStart() throws GridException {
        discoLsnr = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                if (type == GridDiscoveryEventType.LEFT || type == GridDiscoveryEventType.FAILED) {
                    List<IsolatedDeployment> removed = new LinkedList<IsolatedDeployment>();

                    synchronized (mux) {
                        for (Iterator<IsolatedDeployment> iter = cache.values().iterator(); iter.hasNext() == true;) {
                            IsolatedDeployment dep = iter.next();

                            if (dep.getSenderNodeId().equals(node.getId()) == true) {
                                dep.onUndeployed();

                                if (dep.getUsages() == 0) {
                                    iter.remove();

                                    removed.add(dep);
                                }
                            }
                        }
                    }

                    for (IsolatedDeployment dep : removed) {
                        dep.recordUndeployed();
                    }
                }
            }
        };

        mgrReg.getDiscoveryManager().addDiscoveryListener(discoLsnr);

        List<IsolatedDeployment> removed = new LinkedList<IsolatedDeployment>();

        // Check existing deployments for presence of obsolete nodes.
        synchronized (mux) {
            for (Iterator<IsolatedDeployment> iter = cache.values().iterator(); iter.hasNext() == true;) {
                IsolatedDeployment dep = iter.next();

                GridNode node = mgrReg.getDiscoveryManager().getNode(dep.getSenderNodeId());

                if (node == null) {
                    dep.onUndeployed();

                    if (dep.getUsages() == 0) {
                        iter.remove();

                        removed.add(dep);
                    }
                }
            }
        }

        for (IsolatedDeployment dep : removed) {
            dep.recordUndeployed();
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Registered deployment discovery listener: " + discoLsnr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStop() {
        if (discoLsnr != null) {
            mgrReg.getDiscoveryManager().removeDiscoveryListener(discoLsnr);

            if (log.isDebugEnabled() == true) {
                log.debug("Unregistered deployment discovery listener: " + discoLsnr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridDeploymentClass> getDeployments() {
        List<GridDeploymentClass> deps = new LinkedList<GridDeploymentClass>();

        synchronized (mux) {
            for (IsolatedDeployment dep : cache.values()) {
                deps.addAll(dep.getDeployedClasses());
            }
        }

        return deps;
    }

    /**
     * {@inheritDoc}
     */
    public GridDeploymentClass acquireClass(GridDeploymentMetadata meta) {
        assert meta != null : "ASSERTION [line=201, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

        assert cfg.isPeerClassLoadingEnabled() == true : "ASSERTION [line=203, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

        // Validate metadata.
        assert meta.getClassLoaderId() != null : "ASSERTION [line=206, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";
        assert meta.getSenderNodeId() != null : "ASSERTION [line=207, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";
        assert meta.getSequenceNumber() > 0 : "ASSERTION [line=208, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Starting to peer-load class based on deployment metadata: " + meta);
        }

        GridNode sender = mgrReg.getDiscoveryManager().getNode(meta.getSenderNodeId());

        if (sender == null) {
            log.warning("Failed to create Private or Isolated mode deployment (sender node left grid): " + sender);

            return null;
        }

        IsolatedDeployment dep = null;

        long undeployTimeout = 0;

        synchronized (mux) {
            dep = cache.get(meta.getClassLoaderId());

            if (dep != null) {
                if (dep.getSenderNodeId().equals(meta.getSenderNodeId()) == false) {
                    log.error("Sender node ID does not match for Private or Isolated deployment [expected=" +
                        meta.getSenderNodeId() + ", dep=" + dep + ']');

                    return null;
                }
            }
            else {
                // If could not find deployment, make sure to perform clean up.
                // Check if any deployments must be undeployed.
                for (final IsolatedDeployment d : cache.values()) {
                    if (d.getSenderNodeId().equals(meta.getSenderNodeId()) == true &&
                        d.isUndeployed() == false && d.isPendingUndeploy() == false) {
                        if (d.getSequenceNumber() < meta.getSequenceNumber()) {
                            // Undeploy previous class deployments.
                            if (d.getExistingDeployedClass(meta.getClassName()) != null) {
                                if (log.isDebugEnabled() == true) {
                                    log.debug("Received request for a class with newer sequence number " +
                                        "(will schedule current class for undeployment) [cls=" +
                                        meta.getClassName() + ", newSeq=" +
                                        meta.getSequenceNumber() + ", oldSeq=" + d.getSequenceNumber() +
                                        ", senderNodeId=" + meta.getSenderNodeId() + ", curClsLdrId=" +
                                        d.getClassLoaderId() + ", newClsLdrId=" +
                                        meta.getClassLoaderId() + ']');
                                }

                                scheduleUndeploy(d, cfg.getPeerClassLoadingTimeout());
                            }
                        }
                        // If we received execution request even after we waited for P2P
                        // timeout period, we simply ingore it.
                        else if (d.getSequenceNumber() > meta.getSequenceNumber()) {
                            if (d.getExistingDeployedClass(meta.getClassName()) != null) {
                                long time = System.currentTimeMillis() - d.getTimestamp();

                                if (time < cfg.getPeerClassLoadingTimeout()) {
                                    // Set undeployTimeout, so the class will be scheduled
                                    // for undeployment.
                                    undeployTimeout = cfg.getPeerClassLoadingTimeout() - time;

                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Received execution request for a stale class (will deploy and " +
                                            "schedule undeployment in " + undeployTimeout + "ms) " +
                                            "[cls=" + meta.getClassName() + ", curSeq=" + d.getSequenceNumber() +
                                            ", rcvdSeq=" + meta.getSequenceNumber() + ", senderNodeId=" +
                                            meta.getSenderNodeId() + ", curClsLdrId=" + d.getClassLoaderId() +
                                            ", rcvdClsLdrId=" + meta.getClassLoaderId() + ']');
                                    }
                                }
                                else {
                                    log.warning("Received execution request for a class that has been redeployed " +
                                        "(will ignore): " + meta.getAlias());

                                    return null;
                                }
                            }
                        }
                        else {
                            log.error("Sequence number does not correspond to class loader ID [seqNum=" +
                                meta.getSequenceNumber() + ", dep=" + d + ']');

                            return null;
                        }
                    }
                }

                ClassLoader parent = meta.getParentLoader() == null ? Thread.currentThread().getContextClassLoader() :
                    meta.getParentLoader();

                // Safety.
                if (parent == null) {
                    parent = getClass().getClassLoader();
                }

                // Create peer class loader.
                ClassLoader clsLdr = new GridDeploymentClassLoader(true, mgrReg, parent, meta.getClassLoaderId(),
                    meta.getSenderNodeId(), meta.getSequenceNumber(), comm, cfg.getPeerClassLoadingTimeout(), log,
                    cfg.getP2PLocalClassPathExclude(), cfg.getPeerClassLoadingMissedResourcesCacheSize());

                dep = new IsolatedDeployment(meta.getDeploymentMode(), clsLdr, meta.getClassLoaderId(),
                    meta.getSequenceNumber(), meta.getUserVersion(), meta.getSenderNodeId());

                cache.put(meta.getClassLoaderId(), dep);

                // In case if deploying stale class.
                if (undeployTimeout > 0) {
                    scheduleUndeploy(dep, undeployTimeout);
                }
            }
        }

        return getDeployedClass(dep, meta);
    }

    /**
     * Schedules existing deployment for future undeployment.
     *
     * @param dep Deployment.
     * @param timeout Timeout for undeployment to occur.
     */
    private void scheduleUndeploy(final IsolatedDeployment dep, final long timeout) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=331, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

        if (dep.isUndeployed() == false && dep.isPendingUndeploy() == false) {
            dep.onUndeploySheduled();

            procReg.getTimeoutProcessor().addTimeoutObject(new GridTimeoutObject() {
                /** End time. */
                private final long endTime = System.currentTimeMillis() + timeout;

                /**
                 * {@inheritDoc}
                 */
                public UUID getTimeoutId() {
                    return dep.getClassLoaderId();
                }

                /**
                 * {@inheritDoc}
                 */
                public long getEndTime() {
                    return endTime < 0 ? Long.MAX_VALUE : endTime;
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTimeout() {
                    boolean removed = false;

                    // Hot redeployment.
                    synchronized (mux) {
                        if (dep.isUndeployed() == false) {
                            dep.onUndeployed();

                            if (dep.getUsages() == 0) {
                                cache.remove(dep.getClassLoaderId());

                                removed = true;
                            }
                        }
                    }

                    if (removed == true) {
                        dep.recordUndeployed();
                    }
                }
            });
        }
    }

    /**
     *
     * @param dep Deployment.
     * @param meta Metadata.
     * @return Loaded deployed class.
     */
    private GridDeploymentClass getDeployedClass(IsolatedDeployment dep, GridDeploymentMetadata meta) {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=388, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

        // Load class outside of synchronization.
        GridDeploymentClass cls = dep.getDeployedClass(meta);

        if (cls == null) {
            log.warning("Failed to load peer class: " + meta.getAlias());

            return null;
        }

        synchronized (mux) {
            // Be nice and return class loader (deployed or undeployed),
            // if it is still cached.
            if (cache.containsKey(dep.getClassLoaderId()) == true) {
                dep.onAcquire();

                return cls;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void explicitUndeploy(UUID nodeId, String rsrcName) {
        assert nodeId != null : "ASSERTION [line=416, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";
        assert rsrcName != null : "ASSERTION [line=417, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

        List<IsolatedDeployment> undeployed = new LinkedList<IsolatedDeployment>();

        synchronized (mux) {
            for (Iterator<IsolatedDeployment> iter = cache.values().iterator(); iter.hasNext() == true;) {
                IsolatedDeployment dep = iter.next();

                if (dep.getSenderNodeId().equals(nodeId) == true) {
                    if (dep.getExistingDeployedClass(rsrcName) != null) {
                        dep.onUndeployed();

                        if (dep.getUsages() == 0) {
                            iter.remove();

                            undeployed.add(dep);

                            if (log.isInfoEnabled() == true) {
                                log.info("Undeployed Private or Isolated deployment: " + dep);
                            }
                        }
                    }
                }
            }
        }

        for (IsolatedDeployment dep : undeployed) {
            dep.recordUndeployed();
        }
    }

    /**
     *
     * @param cls Class to check.
     * @return <tt>True</tt> if class was deployed by this store.
     */
    private boolean isIsolatedMode(GridDeploymentClass cls) {
        return
            cls.isLocal() == false &&
            (
                cls.getDeploymentMode() == GridDeploymentMode.PRIVATE ||
                cls.getDeploymentMode() == GridDeploymentMode.ISOLATED
            );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseClass(GridDeploymentClass cls) {
        assert cls != null : "ASSERTION [line=467, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";
        assert isIsolatedMode(cls) == true : "ASSERTION [line=468, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Releasing deployment class: " + cls);
        }

        IsolatedDeployment dep = null;

        boolean removed = false;

        synchronized (mux) {
            dep = cache.get(cls.getClassLoaderId());

            assert dep != null : "ASSERTION [line=481, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            dep.onRelease();

            if (dep.isUndeployed() == true && dep.getUsages() == 0) {
                // Undeploy.
                cache.remove((cls.getClassLoaderId()));

                removed = true;
            }
        }

        if (removed == true) {
            dep.recordUndeployed();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentPerLoaderStore.class, this);
    }

    /**
     *
     */
    private class IsolatedDeployment {
        /** Timestamp for creation of this deployment. */
        private final long timestamp = System.currentTimeMillis();

        /** Deployment mode. */
        private final GridDeploymentMode depMode;

        /** Class loader. */
        private final ClassLoader clsLdr;

        /** Class loader ID. */
        private final UUID clsLdrId;

        /** User version. */
        private final String userVer;

        /** Sequence number. */
        private final long seqNum;

        /** Sender node ID. */
        private final UUID senderNodeId;

        /** Class name map. */
        private final ConcurrentMap<String, GridDeploymentClass> clsMap =
            new ConcurrentHashMap<String, GridDeploymentClass>();

        /** Undeployed flag. */
        private boolean undeployed = false;

        /** Flag indicating whether undeploy is pending. */
        private boolean pendingUndeploy = false;

        /** Usage count. */
        private int usage = 0;

        /**
         * @param depMode Deployment mode.
         * @param clsLdr Class loader.
         * @param clsLdrId Class loader ID.
         * @param seqNum Sequence number.
         * @param userVer User version.
         * @param senderNodeId Sender node ID.
         */
        IsolatedDeployment(GridDeploymentMode depMode, ClassLoader clsLdr, UUID clsLdrId, long seqNum, String userVer,
            UUID senderNodeId) {
            this.clsLdr = clsLdr;
            this.clsLdrId = clsLdrId;
            this.seqNum = seqNum;
            this.userVer = userVer;
            this.depMode = depMode;
            this.senderNodeId = senderNodeId;
        }

        /**
         * Gets property timestamp.
         *
         * @return Property timestamp.
         */
        long getTimestamp() {
            return timestamp;
        }

        /**
         * Gets property depMode.
         *
         * @return Property depMode.
         */
        GridDeploymentMode getDeploymentMode() {
            return depMode;
        }

        /**
         * Gets property senderNodeId.
         *
         * @return Property senderNodeId.
         */
        UUID getSenderNodeId() {
            return senderNodeId;
        }

        /**
         * Gets property seqNum.
         *
         * @return Property seqNum.
         */
        long getSequenceNumber() {
            return seqNum;
        }

        /**
         * Gets property clsLdrId.
         *
         * @return Property clsLdrId.
         */
        public UUID getClassLoaderId() {
            return clsLdrId;
        }

        /**
         * Gets property undeployed.
         *
         * @return Property undeployed.
         */
        boolean isUndeployed() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=613, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            return undeployed;
        }

        /**
         * Sets property undeployed.
         */
        void onUndeployed() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=622, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            undeployed = true;
        }

        /**
         * Acquires class.
         */
        void onAcquire() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=631, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";
            assert usage >= 0 : "ASSERTION [line=632, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            usage++;
        }

        /**
         * Releases class.
         */
        void onRelease() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=641, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";
            assert usage > 0 : "ASSERTION [line=642, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            usage--;
        }

        /**
         * Gets usages.
         *
         * @return Usages.
         */
        int getUsages() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=653, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";
            assert usage >= 0 : "ASSERTION [line=654, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            return usage;
        }

        /**
         * Gets property pendingUndeploy.
         *
         * @return Property pendingUndeploy.
         */
        boolean isPendingUndeploy() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=665, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            return pendingUndeploy;
        }

        /**
         * Invoked whenever this deployment is scheduled to be undeployed.
         * Used for handling obsolete or phantom requests.
         */
        void onUndeploySheduled() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=675, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            pendingUndeploy = true;
        }

        /**
         * @return Deployed classes.
         */
        Collection<GridDeploymentClass> getDeployedClasses() {
            return clsMap.values();
        }

        /**
         * @param clsName Class name of deployed class.
         * @return Deployed class.
         */
        GridDeploymentClass getExistingDeployedClass(String clsName) {
            return clsMap.get(clsName);
        }

        /**
         * @return Class loader.
         */
        ClassLoader getClassLoader() {
            return clsLdr;
        }

        /**
         * @param meta Deployment metadata.
         * @return Deployed class.
         */
        GridDeploymentClass getDeployedClass(GridDeploymentMetadata meta) {
            assert Thread.holdsLock(mux) == false : "ASSERTION [line=707, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            GridDeploymentClass depCls = clsMap.get(meta.getClassName());

            if (depCls != null) {
                return depCls;
            }

            //noinspection UnusedCatchParameter
            try {
                depCls = new GridDeploymentClass(
                    Class.forName(meta.getClassName(), true, clsLdr),
                    meta.getAlias(),
                    depMode,
                    clsLdr,
                    clsLdrId,
                    seqNum,
                    userVer,
                    false);

                GridDeploymentClass cur = clsMap.putIfAbsent(meta.getClassName(), depCls);

                // Return existing deployment if one exists.
                if (cur != null) {
                    return cur;
                }

                recordDeployed(depCls, meta.isRecord());

                return depCls;
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }

        /**
         * Called for every deployed class.
         *
         * @param cls Deployed class.
         * @param recordEvt Flag indicating whether to record events.
         */
        void recordDeployed(GridDeploymentClass cls, boolean recordEvt) {
            assert Thread.holdsLock(mux) == false : "ASSERTION [line=750, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            String msg;

            GridEventType evt;

            // Record task event.
            if (isTask(cls.getDeployedClass()) == true) {
                msg = "Task was deployed in Private or Isolated mode: " + cls;

                evt = GridEventType.TASK_DEPLOYED;
            }
            else {
                msg = "Class was deployed in Private or Isolated mode: " + cls;

                evt = GridEventType.CLASS_DEPLOYED;
            }

            if (recordEvt == true) {
                mgrReg.getEventStorageManager().record(evt, cls.getAlias(), cls.getUserVersion(), senderNodeId, msg);
            }

            if (log.isInfoEnabled() == true) {
                log.info(msg);
            }
        }

        /**
         * Called to record all undeployed classes..
         */
        void recordUndeployed() {
            assert Thread.holdsLock(mux) == false : "ASSERTION [line=781, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerLoaderStore.java]";

            for (GridDeploymentClass cls : clsMap.values()) {
                String msg = (isTask(cls.getDeployedClass()) == true ? "Task" : "Class") +
                     " was undeployed in Private or Isolated mode: " + cls;

                GridEventType evt = isTask(cls.getDeployedClass()) == true ?
                    GridEventType.TASK_UNDEPLOYED : GridEventType.CLASS_UNDEPLOYED;

                mgrReg.getEventStorageManager().record(evt, cls.getAlias(), cls.getUserVersion(), senderNodeId, msg);

                if (log.isInfoEnabled() == true) {
                    log.info(msg);
                }
            }

            // Resource cleanup.
            procReg.getResourceProcessor().onUndeployed(clsLdr);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return GridToStringBuilder.toString(IsolatedDeployment.class, this);
        }
    }
}
