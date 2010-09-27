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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Deployment storage for {@link GridDeploymentMode#SHARED} and
 * {@link GridDeploymentMode#CONTINUOUS} modes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridDeploymentPerVersionStore extends GridDeploymentStoreAdapter {
    /** Shared deployment cache. */
    private Map<String, List<SharedDeployment>> cache = new HashMap<String, List<SharedDeployment>>();

    /** Set of obsolete class loaders. */
    private Set<UUID> deadClsLdrs = new GridBoundedLinkedHashSet<UUID>(1000);

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
    GridDeploymentPerVersionStore(GridDeploymentSpi spi, GridConfiguration cfg, GridManagerRegistry mgrReg,
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
        Set<SharedDeployment> copy = new HashSet<SharedDeployment>();

        synchronized (mux) {
            for (List<SharedDeployment> deps : cache.values()) {
                for (SharedDeployment dep : deps) {
                    assert dep.getUsages() == 0 : "ASSERTION [line=85, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]. " + "Found left over usage for deployment: " + dep;

                    // Mark undeployed.
                    dep.onUndeployed();

                    copy.add(dep);
                }
            }

            cache.clear();
        }

        for (SharedDeployment dep : copy) {
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
                    List<SharedDeployment> undeployed = new LinkedList<SharedDeployment>();

                    synchronized (mux) {
                        for (Iterator<List<SharedDeployment>> i1 = cache.values().iterator(); i1.hasNext() == true;) {
                            List<SharedDeployment> deps = i1.next();

                            for (Iterator<SharedDeployment> i2 = deps.iterator(); i2.hasNext() == true;) {
                                SharedDeployment dep = i2.next();

                                dep.removeParticipant(node.getId());

                                if (dep.hasParticipants() == false) {
                                    if (dep.getDeploymentMode() == GridDeploymentMode.SHARED) {
                                        if (dep.isUndeployed() == false) {
                                            dep.onUndeployed();

                                            if (dep.getUsages() == 0) {
                                                // Undeploy.
                                                i2.remove();

                                                assert dep.isRemoved() == false : "ASSERTION [line=137, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                                                dep.onRemoved();

                                                undeployed.add(dep);

                                                if (log.isDebugEnabled() == true) {
                                                    log.debug("Undeployed class loader as there are no participating " +
                                                        "nodes: " + dep);
                                                }
                                            }
                                        }
                                    }
                                    else {
                                        if (log.isDebugEnabled() == true) {
                                            log.debug("Preserving deployment without node participants: " + dep);
                                        }
                                    }
                                }
                            }

                            if (deps.isEmpty() == true) {
                                i1.remove();
                            }
                        }
                    }

                    recordUndeployed(undeployed);
                }
            }
        };

        mgrReg.getDiscoveryManager().addDiscoveryListener(discoLsnr);

        List<SharedDeployment> undeployed = new LinkedList<SharedDeployment>();

        synchronized (mux) {
            for (Iterator<List<SharedDeployment>> i1 = cache.values().iterator(); i1.hasNext() == true;) {
                List<SharedDeployment> deps = i1.next();

                for (Iterator<SharedDeployment> i2 = deps.iterator(); i2.hasNext() == true;) {
                    SharedDeployment dep = i2.next();

                    for (UUID nodeId : dep.getParticipantNodeIds()) {
                        if (mgrReg.getDiscoveryManager().getNode(nodeId) == null) {
                            dep.removeParticipant(nodeId);
                        }
                    }

                    if (dep.hasParticipants() == false) {
                        if (dep.getDeploymentMode() == GridDeploymentMode.SHARED) {
                            if (dep.isUndeployed() == false) {
                                dep.onUndeployed();

                                if (dep.getUsages() == 0) {
                                    // Undeploy.
                                    i2.remove();

                                    dep.onRemoved();

                                    undeployed.add(dep);

                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Undeployed class loader as there are no participating nodes: " + dep);
                                    }
                                }
                            }
                        }
                        else {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Preserving deployment without node participants: " + dep);
                            }
                        }
                    }
                }

                if (deps.isEmpty() == true) {
                    i1.remove();
                }
            }
        }

        recordUndeployed(undeployed);

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
        Collection<GridDeploymentClass> deps = new LinkedList<GridDeploymentClass>();

        synchronized (mux) {
            for (List<SharedDeployment> list : cache.values()) {
                for (SharedDeployment d : list) {
                    deps.addAll(d.getDeployedClasses());
                }
            }
        }

        return deps;
    }

    /**
     * {@inheritDoc}
     */
    public GridDeploymentClass acquireClass(GridDeploymentMetadata meta) {
        assert meta != null : "ASSERTION [line=261, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        assert cfg.isPeerClassLoadingEnabled() == true : "ASSERTION [line=263, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        // Validate metadata.
        assert meta.getClassLoaderId() != null : "ASSERTION [line=266, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
        assert meta.getSenderNodeId() != null : "ASSERTION [line=267, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
        assert meta.getSequenceNumber() > 0 : "ASSERTION [line=268, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
        assert meta.getParentLoader() == null : "ASSERTION [line=269, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Starting to peer-load class based on deployment metadata: " + meta);
        }

        while (true) {
            List<SharedDeployment> depsToCheck = null;

            SharedDeployment dep = null;

            synchronized (mux) {
                // Check obsolete request.
                if (isDeadClassLoader(meta) == true) {
                    return null;
                }

                List<SharedDeployment> deps = cache.get(meta.getUserVersion());

                if (deps != null) {
                    assert deps.isEmpty() == false : "ASSERTION [line=289, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                    for (SharedDeployment d : deps) {
                        if (d.hasParticipant(meta.getSenderNodeId(), meta.getClassLoaderId()) == true) {
                            // Done.
                            dep = d;

                            break;
                        }
                    }

                    if (dep == null) {
                        GridPair<Boolean, SharedDeployment> redeployCheck = checkRedeploy(meta);

                        if (redeployCheck.getValue1() == false) {
                            // Checking for redeployment encountered invalid state.
                            return null;
                        }

                        dep = redeployCheck.getValue2();

                        if (dep == null) {
                            // Find existing deployments that need to be checked
                            // whether they should be reused for this request.
                            for (SharedDeployment d : deps) {
                                if (d.isPendingUndeploy() == false && d.isUndeployed() == false) {
                                    if (depsToCheck == null) {
                                        depsToCheck = new LinkedList<SharedDeployment>();
                                    }

                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Adding deployment to check: " + d);
                                    }

                                    depsToCheck.add(d);
                                }
                            }

                            // If no deployment can be reused, create a new one.
                            if (depsToCheck == null) {
                                dep = createNewDeployment(meta, false);

                                deps.add(dep);
                            }
                        }
                    }
                }
                else {
                    GridPair<Boolean, SharedDeployment> redeployCheck = checkRedeploy(meta);

                    if (redeployCheck.getValue1() == false) {
                        // Checking for redeployment encountered invalid state.
                        return null;
                    }

                    dep = redeployCheck.getValue2();

                    if (dep == null) {
                        // Create peer class loader.
                        dep = createNewDeployment(meta, true);
                    }
                }
            }

            if (dep != null) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Found SHARED or CONTINUOUS deployment after first check: " + dep);
                }

                return getDeployedClass(dep, meta);
            }

            assert meta.getParentLoader() == null : "ASSERTION [line=361, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert depsToCheck != null : "ASSERTION [line=362, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert depsToCheck.isEmpty() == false : "ASSERTION [line=363, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            /*
             * Logic below must be performed outside of synchronization
             * because it involves network calls.
             */

            // Check if class can be loaded from existing nodes.
            // In most cases this loop will find something.
            for (SharedDeployment d : depsToCheck) {
                // Load class. Note, that remote node will not load this class.
                // The class will only be loaded on this node.
                GridDeploymentClass depCls = d.getDeployedClass(meta.getClassName(), meta.getAlias());

                if (depCls != null) {
                    synchronized (mux) {
                        if (d.isUndeployed() == false && d.isPendingUndeploy() == false) {
                            if (addParticipant(d, meta) == false) {
                                return null;
                            }

                            d.onAcquire();

                            if (log.isDebugEnabled() == true) {
                                log.debug("Acquired deployment class after verifying it's availabitlity on " +
                                    "existing nodes [depCls=" + depCls + ", meta=" + meta + ']');
                            }

                            return depCls;
                        }
                    }
                }
                else if (log.isDebugEnabled() == true) {
                    log.debug("Deployment cannot be reused (class does not exist on participating nodes) [dep=" + d +
                        ", meta=" + meta + ']');
                }
            }

            // We are here either because all participant nodes failed
            // or the class indeed should have a separate deployment.
            for (SharedDeployment d : depsToCheck) {
                // Temporary class loader.
                ClassLoader temp = new GridDeploymentClassLoader(
                    true,
                    mgrReg,
                    getClass().getClassLoader(),
                    meta.getClassLoaderId(),
                    meta.getSenderNodeId(),
                    meta.getSequenceNumber(),
                    comm,
                    cfg.getPeerClassLoadingTimeout(),
                    log,
                    cfg.getP2PLocalClassPathExclude(),
                    0);

                String path = GridUtils.classNameToResourceName(d.getRandomClassName());

                // We check if any random class from existing deployment can be
                // loaded from sender node. If it can, then we reuse existing
                // deployment.
                InputStream rsrcIn = temp.getResourceAsStream(path);

                if (rsrcIn != null) {
                    // We don't need the actual stream.
                    GridUtils.close(rsrcIn, log);

                    synchronized (mux) {
                        if (d.isUndeployed() == true || d.isPendingUndeploy() == true) {
                            continue;
                        }

                        // Add new node prior to loading the class, so we attempt
                        // to load the class from the latest node.
                        if (addParticipant(d, meta) == false) {
                            return null;
                        }

                        d.onAcquire();
                    }

                    GridDeploymentClass depCls = d.getDeployedClass(meta.getClassName(), meta.getAlias());

                    if (depCls == null) {
                        log.error("Successfully loaded class as resource, but failed to load it as class: " + meta);

                        synchronized (mux) {
                            d.onRelease();
                        }

                        return null;
                    }

                    if (log.isDebugEnabled() == true) {
                        log.debug("Acquired deployment class after verifying other class " +
                            "availability on sender node [depCls=" + depCls + ", rndCls=" +
                            d.getRandomClassName() + ", meta=" + meta + ']');
                    }

                    return depCls;
                }
                else if (log.isDebugEnabled() == true) {
                    log.debug("Deployment cannot be reused (random class could not be loaded from sender node) [dep=" +
                        d + ", meta=" + meta + ']');
                }
            }

            synchronized (mux) {
                if (log.isDebugEnabled() == true) {
                    log.debug("None of the existing class-loaders fit (will try to create a new one): " + meta);
                }

                // Check obsolete request.
                if (isDeadClassLoader(meta) == true) {
                    return null;
                }

                // Check that deployment picture has not changed.
                List<SharedDeployment> deps = cache.get(meta.getUserVersion());

                if (deps != null) {
                    assert deps.isEmpty() == false : "ASSERTION [line=483, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                    boolean retry = false;

                    for (SharedDeployment d : deps) {
                        // Double check if sender was already added.
                        if (d.hasParticipant(meta.getSenderNodeId(), meta.getClassLoaderId()) == true) {
                            dep = d;

                            retry = false;

                            break;
                        }

                        // New deployment was added while outside of synchronization.
                        // Need to recheck it again.
                        if (d.isPendingUndeploy() == false && d.isUndeployed() == false &&
                            depsToCheck.contains(d) == false) {
                            retry = true;
                        }
                    }

                    if (retry == true) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Retrying due to concurrency issues: " + meta);
                        }

                        // Outer while loop.
                        continue;
                    }

                    if (dep == null) {
                        // No new deployments were added, so we can safely add ours.
                        dep = createNewDeployment(meta, false);

                        deps.add(dep);

                        if (log.isDebugEnabled() == true) {
                            log.debug("Adding new deployment within second check [dep=" + dep + ", meta=" + meta + ']');
                        }
                    }
                }
                else {
                    dep = createNewDeployment(meta, true);

                    if (log.isDebugEnabled() == true) {
                        log.debug("Created new deployment within second check [dep=" + dep + ", meta=" + meta + ']');
                    }
                }
            }

            if (dep != null) {
                return getDeployedClass(dep, meta);
            }
        }
    }

    /**
     * Gets deployed class.
     *
     * @param dep Deployment.
     * @param meta Deployment metadata.
     * @return Deployed class.
     */
    private GridDeploymentClass getDeployedClass(SharedDeployment dep, GridDeploymentMetadata meta) {
        if (dep != null) {
            if (checkModeMatch(dep, meta) == false) {
                return null;
            }

            // Load class outside of synchronization.
            GridDeploymentClass depCls = dep.getDeployedClass(meta.getClassName(), meta.getAlias());

            if (log.isDebugEnabled() == true) {
                log.debug("Acquired deployment class [depCls=" + depCls + ", meta=" + meta + ']');
            }

            if (depCls != null) {
                synchronized (mux) {
                    if (dep.isRemoved() == false) {
                        dep.onAcquire();

                        return depCls;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Records all undeployed tasks.
     *
     * @param undeployed Undeployed deployments.
     */
    private void recordUndeployed(List<SharedDeployment> undeployed) {
        if (undeployed != null && undeployed.isEmpty() == false) {
            for (SharedDeployment d : undeployed) {
                d.recordUndeployed();
            }
        }
    }

    /**
     *
     * @param meta Request metadata.
     * @return <tt>True</tt> if class loader is obsolete.
     */
    private boolean isDeadClassLoader(GridDeploymentMetadata meta) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=593, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        if (deadClsLdrs.contains(meta.getClassLoaderId()) == true) {
            if (log.isDebugEnabled() == true) {
                log.debug("Ignoring request for obsolete class loader: " + meta);
            }

            return true;
        }

        return false;
    }

    /**
     * Adds new participant to deployment.
     *
     * @param dep Shared deployment.
     * @param meta Request metadata.
     * @return <tt>True</tt> if participant was added.
     */
    private boolean addParticipant(SharedDeployment dep, GridDeploymentMetadata meta) {
        assert dep != null : "ASSERTION [line=614, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
        assert meta != null : "ASSERTION [line=615, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        assert Thread.holdsLock(mux) == true : "ASSERTION [line=617, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        if (checkModeMatch(dep, meta) == false) {
            return false;
        }

        if (dep.addParticipant(meta.getSenderNodeId(), meta.getClassLoaderId(), meta.getSequenceNumber()) == false) {
            log.warning("Failed to create shared mode deployment " +
                "(requested class loader was already undeployed, did sender node leave grid?) " +
                "[clsLdrId=" + meta.getClassLoaderId() + ", senderNodeId=" + meta.getSenderNodeId() +
                ']');

            return false;
        }

        return true;
    }

    /**
     * Checks if deployment modes match.
     *
     * @param dep Shared deployment.
     * @param meta Request metadata.
     * @return <tt>True</tt> if shared deployment modes match.
     */
    private boolean checkModeMatch(SharedDeployment dep, GridDeploymentMetadata meta) {
        if (dep.getDeploymentMode() != meta.getDeploymentMode()) {
            log.warning("Received invalid deployment mode (will not deploy, make sure that all nodes " +
                "executing the same classes in shared mode have identical GridDeploymentMode parameter) [mode=" +
                meta.getDeploymentMode() + ", expected=" + dep.getDeploymentMode() + ']');

            return false;
        }

        return true;
    }

    /**
     * Removes obsolete deployments in case of redeploy.
     *
     * @param meta Request metadata.
     * @return List of shares deployment.
     */
    private GridPair<Boolean, SharedDeployment> checkRedeploy(GridDeploymentMetadata meta) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=661, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        SharedDeployment newDep = null;

        for (Iterator<List<SharedDeployment>> i1 = cache.values().iterator(); i1.hasNext() == true;) {
            List<SharedDeployment> deps = i1.next();

            for (Iterator<SharedDeployment> i2 = deps.iterator(); i2.hasNext() == true;) {
                final SharedDeployment dep = i2.next();

                if (dep.isUndeployed() == false && dep.isPendingUndeploy() == false) {
                    long undeployTimeout = cfg.getPeerClassLoadingTimeout();

                    SharedDeployment doomed = null;

                    // Only check deployments with no participants.
                    if (dep.hasParticipants() == false) {
                        assert dep.getDeploymentMode() == GridDeploymentMode.CONTINUOUS : "ASSERTION [line=678, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                        if (dep.getExistingDeployedClass(meta.getClassName()) != null) {
                            // Change from shared deploy to shared undeploy or user version change.
                            // Simply remove all deployments with no participating nodes.
                            if (meta.getDeploymentMode() == GridDeploymentMode.SHARED ||
                                meta.getUserVersion().equals(dep.getUserVersion()) == false) {
                                doomed = dep;
                            }
                        }
                    }
                    // If there are participants, we undeploy if class loader ID on some node changed.
                    else if (dep.getExistingDeployedClass(meta.getClassName()) != null) {
                        GridPair<UUID, Long> ldr = dep.getClassLoaderId(meta.getSenderNodeId());

                        if (ldr != null) {
                            if (ldr.getValue1().equals(meta.getClassLoaderId()) == false) {
                                // If deployed sequence number is less, then schedule for undeployment.
                                if (ldr.getValue2() < meta.getSequenceNumber()) {
                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Received request for a class with newer sequence number " +
                                            "(will schedule current class for undeployment) [newSeq=" +
                                            meta.getSequenceNumber() + ", oldSeq=" + ldr.getValue2() +
                                            ", senderNodeId=" + meta.getSenderNodeId() + ", newClsLdrId=" +
                                            meta.getClassLoaderId() + ", oldClsLdrId=" + ldr.getValue1() + ']');
                                    }

                                    doomed = dep;
                                }
                                else if (ldr.getValue2() > meta.getSequenceNumber()) {
                                    long time = System.currentTimeMillis() - dep.getTimestamp();

                                    if (newDep == null && time < cfg.getPeerClassLoadingTimeout()) {
                                        // Set undeployTimeout, so the class will be scheduled
                                        // for undeployment.
                                        undeployTimeout = cfg.getPeerClassLoadingTimeout() - time;

                                        if (log.isDebugEnabled() == true) {
                                            log.debug("Received execution request for a stale class (will deploy and " +
                                                "schedule undeployment in " + undeployTimeout + "ms) " + "[curSeq=" +
                                                ldr.getValue2() + ", staleSeq=" + meta.getSequenceNumber() + ", cls=" +
                                                meta.getClassName() + ", senderNodeId=" + meta.getSenderNodeId() +
                                                ", curLdrId=" + ldr.getValue1() + ", staleLdrId=" +
                                                meta.getClassLoaderId() + ']');
                                        }

                                        // We got the redeployed class before the old one.
                                        // Simply create a temporary deployment for the sender node,
                                        // and schedule undeploy for it.
                                        newDep = createNewDeployment(meta, false);

                                        doomed = newDep;
                                    }
                                    else {
                                        log.warning("Received execution request for a class that has been redeployed " +
                                            "(will ignore): " + meta.getAlias());

                                        return new GridPair<Boolean, SharedDeployment>(false, null);
                                    }
                                }
                                else {
                                    log.error("Sequence number does not correspond to class loader ID [seqNum=" +
                                        meta.getSequenceNumber() + ", dep=" + dep + ']');

                                    return new GridPair<Boolean, SharedDeployment>(false, null);
                                }
                            }
                        }
                    }

                    if (doomed != null) {
                        doomed.onUndeploySheduled();

                        // Lifespan time.
                        final long endTime = System.currentTimeMillis() + undeployTimeout;

                        // Deployment to undeploy.
                        final SharedDeployment undep = doomed;

                        procReg.getTimeoutProcessor().addTimeoutObject(new GridTimeoutObject() {
                            /**
                             * {@inheritDoc}
                             */
                            public UUID getTimeoutId() {
                                return undep.getSharedLoaderId();
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
                                    assert undep.isPendingUndeploy() == true : "ASSERTION [line=780, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                                    if (undep.isUndeployed() == false) {
                                        undep.onUndeployed();

                                        if (undep.getUsages() == 0) {
                                            assert undep.isRemoved() == false : "ASSERTION [line=786, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                                            undep.onRemoved();

                                            removed = true;

                                            Collection<SharedDeployment> deps = cache.get(undep.getUserVersion());

                                            if (deps != null) {
                                                for (Iterator<SharedDeployment> i = deps.iterator();
                                                    i.hasNext() == true;) {
                                                    //noinspection ObjectEquality
                                                    if (i.next() == undep) {
                                                        i.remove();
                                                    }
                                                }

                                                if (deps.isEmpty() == true) {
                                                    cache.remove(undep.getUserVersion());
                                                }
                                            }

                                            if (log.isInfoEnabled() == true) {
                                                log.info("Undeployed class loader due to deployment mode change, " +
                                                    "user version change, or hot redeployment: " + undep);
                                            }
                                        }
                                    }
                                }

                                // Outside synchronization.
                                if (removed == true) {
                                    undep.recordUndeployed();
                                }
                            }
                        });
                    }
                }
            }
        }

        if (newDep != null) {
            List<SharedDeployment> deps = cache.get(meta.getUserVersion());

            if (deps == null) {
                cache.put(meta.getUserVersion(), deps = new LinkedList<SharedDeployment>());
            }

            deps.add(newDep);
        }

        return new GridPair<Boolean, SharedDeployment>(true, newDep);
    }

    /**
     * {@inheritDoc}
     */
    public void explicitUndeploy(UUID nodeId, String rsrcName) {
        List<SharedDeployment> undeployed = new LinkedList<SharedDeployment>();

        synchronized (mux) {
            for (Iterator<List<SharedDeployment>> i1 = cache.values().iterator(); i1.hasNext() == true;) {
                List<SharedDeployment> deps = i1.next();

                for (Iterator<SharedDeployment> i2 = deps.iterator(); i2.hasNext() == true;) {
                    SharedDeployment dep = i2.next();

                    for (GridDeploymentClass depCls : dep.getDeployedClasses()) {
                        if (depCls.hasName(rsrcName) == true) {
                            if (dep.isUndeployed() == false) {
                                dep.onUndeployed();

                                if (dep.getUsages() == 0) {
                                    assert dep.isRemoved() == false : "ASSERTION [line=859, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                                    dep.onRemoved();

                                    // Undeploy.
                                    i2.remove();

                                    undeployed.add(dep);

                                    if (log.isInfoEnabled() == true) {
                                        log.info("Undeployed per-version class loader: " + dep);
                                    }
                                }
                            }

                            break;
                        }
                    }
                }

                if (deps.isEmpty() == true) {
                    i1.remove();
                }
            }
        }

        recordUndeployed(undeployed);
    }

    /**
     * Creates and caches new deployment.
     *
     * @param meta Deployment metadata.
     * @param isCache Whether or not to cache.
     * @return New deployment.
     */
    private SharedDeployment createNewDeployment(GridDeploymentMetadata meta, boolean isCache) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=896, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        assert meta.getParentLoader() == null : "ASSERTION [line=898, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        // Safety.
        if (parent == null) {
            parent = getClass().getClassLoader();
        }

        // Create peer class loader.
        // Note that we are passing empty list for local P2P exclude, as it really
        // does not make sense with shared deployment.
        GridDeploymentClassLoader clsLdr = new GridDeploymentClassLoader(false, mgrReg, parent, meta.getClassLoaderId(),
            meta.getSenderNodeId(), meta.getSequenceNumber(), comm, cfg.getPeerClassLoadingTimeout(), log,
            cfg.getP2PLocalClassPathExclude(), cfg.getPeerClassLoadingMissedResourcesCacheSize());

        // Give this deployment a unique class loader to emphasize that this
        // ID is unique to this shared deployment and is not ID of loader on
        // sender node.
        SharedDeployment dep = new SharedDeployment(meta.getDeploymentMode(), clsLdr, /* loaderID. */ UUID.randomUUID(),
            -1, meta.getUserVersion(), meta.getAlias());

        if (isCache == true) {
            List<SharedDeployment> deps = new LinkedList<SharedDeployment>();

            deps.add(dep);

            cache.put(meta.getUserVersion(), deps);
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Created new deployment: " + dep);
        }

        return dep;
    }

    /**
     *
     * @param cls Class to check.
     * @return <tt>True</tt> if class was deployed by this store.
     */
    private boolean isSharedMode(GridDeploymentClass cls) {
        return
            cls.isLocal() == false &&
            (
                cls.getDeploymentMode() == GridDeploymentMode.CONTINUOUS ||
                cls.getDeploymentMode() == GridDeploymentMode.SHARED
            );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseClass(GridDeploymentClass cls) {
        assert cls != null : "ASSERTION [line=954, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
        assert isSharedMode(cls) == true : "ASSERTION [line=955, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Releasing deployment class: " + cls);
        }

        SharedDeployment removed = null;

        synchronized (mux) {
            List<SharedDeployment> deps = cache.get(cls.getUserVersion());

            assert deps != null : "ASSERTION [line=966, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert deps.isEmpty() == false : "ASSERTION [line=967, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            for (Iterator<SharedDeployment> iter = deps.iterator(); iter.hasNext() == true;) {
                SharedDeployment d = iter.next();

                //noinspection ObjectEquality
                if (d.getClassLoader() == cls.getClassLoader()) {
                    d.onRelease();

                    if (d.isUndeployed() == true && d.getUsages() == 0) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Removing deployment after release: " + d);
                        }

                        iter.remove();

                        assert d.isRemoved() == false : "ASSERTION [line=983, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

                        d.onRemoved();

                        removed = d;
                    }
                    else if (log.isDebugEnabled() == true) {
                        log.debug("Keeping deployment after release: " + d);
                    }

                    break;
                }
            }

            if (deps.isEmpty() == true) {
                cache.remove(cls.getUserVersion());
            }
        }

        if (removed != null) {
            removed.recordUndeployed();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentPerVersionStore.class, this);
    }

    /**
     *
     */
    private class SharedDeployment {
        /** Deployment timestamp. */
        private final long timestamp = System.currentTimeMillis();

        /** Deployment mode. */
        private final GridDeploymentMode depMode;

        /** Class loader. */
        private final GridDeploymentClassLoader clsLdr;

        /** Class loader ID. */
        private final UUID clsLdrId;

        /** User version. */
        private final String userVer;

        /** */
        private final long seqNum;

        /** Class name map. */
        private final ConcurrentMap<String, GridDeploymentClass> clsMap =
            new ConcurrentHashMap<String, GridDeploymentClass>();

        /** Random class name. */
        private final String rndClsName;

        /** Undeployed flag. */
        private boolean undeployed = false;

        /** Usage count. */
        private int usage = 0;

        /** Flag indicating whether this deployment was removed from cache. */
        private boolean removed = false;

        /** Flag indicating whether this class loader is kept for obsolete requests. */
        private boolean pendingUndeploy = false;

        /**
         * @param depMode Deployment mode.
         * @param clsLdr Class loader.
         * @param clsLdrId Class loader ID.
         * @param seqNum Sequence number (mostly meaningless for shared deployment).
         * @param userVer User version.
         * @param rndClsName Random class name (usually the first loaded class).
         */
        SharedDeployment(GridDeploymentMode depMode, GridDeploymentClassLoader clsLdr, UUID clsLdrId, long seqNum,
            String userVer, String rndClsName) {
            this.depMode = depMode;
            this.clsLdr = clsLdr;
            this.clsLdrId = clsLdrId;
            this.seqNum = seqNum;
            this.userVer = userVer;

            assert rndClsName != null : "ASSERTION [line=1072, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            this.rndClsName = rndClsName;
        }

        /**
         * @return Creation timestamp.
         */
        long getTimestamp() {
            return timestamp;
        }

        /**
         *
         * @return Unique ID of this deployment.
         */
        UUID getSharedLoaderId() {
            return clsLdrId;
        }

        /**
         * Gets property rndClsName.
         *
         * @return Property rndClsName.
         */
        String getRandomClassName() {
            return rndClsName;
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
         * Gets property userVer.
         *
         * @return Property userVer.
         */
        String getUserVersion() {
            return userVer;
        }

        /**
         * Gets property clsLdr.
         *
         * @return Property clsLdr.
         */
        public ClassLoader getClassLoader() {
            return clsLdr;
        }

        /**
         *
         * @param nodeId Grid node ID.
         * @param ldrId Class loader ID.
         * @param seqNum Sequence number for the class loader.
         * @return Whether actually added or not.
         */
        boolean addParticipant(UUID nodeId, UUID ldrId, long seqNum) {
            assert nodeId != null : "ASSERTION [line=1136, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert ldrId != null : "ASSERTION [line=1137, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1139, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            if (deadClsLdrs.contains(ldrId) == false) {
                clsLdr.register(nodeId, ldrId, seqNum);

                return true;
            }

            return false;
        }

        /**
         * @param nodeId Node ID to remove.
         */
        void removeParticipant(UUID nodeId) {
            assert nodeId != null : "ASSERTION [line=1154, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1156, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            UUID ldrId = clsLdr.unregister(nodeId);

            if (log.isDebugEnabled() == true) {
                log.debug("Registering dead class loader ID: " + ldrId);
            }

            deadClsLdrs.add(ldrId);
        }

        /**
         * @return Set of participating nodes.
         */
        Collection<UUID> getParticipantNodeIds() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1171, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return clsLdr.getRegisteredNodeIds();
        }

        /**
         * @param nodeId Node ID.
         * @return Class loader ID for node ID.
         */
        GridPair<UUID, Long> getClassLoaderId(UUID nodeId) {
            assert nodeId != null : "ASSERTION [line=1181, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1183, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return clsLdr.getRegisterdClassLoaderId(nodeId);
        }

        /**
         * @return Registered class loader IDs.
         */
        Collection<UUID> getClassLoaderIds() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1192, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return clsLdr.getRegisteredClaassLoaderIds();
        }


        /**
         * @return <tt>True</tt> if deployment has any node participants.
         */
        boolean hasParticipants() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1202, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return clsLdr.hasRegisteredNodes() == true;
        }

        /**
         * Checks if node is participating in deployment.
         *
         * @param nodeId Node ID to check.
         * @param ldrId Class loader ID.
         * @return <tt>True</tt> if node is participating in deployment.
         */
        boolean hasParticipant(UUID nodeId, UUID ldrId) {
            assert nodeId != null : "ASSERTION [line=1215, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert ldrId != null : "ASSERTION [line=1216, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1218, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return clsLdr.hasRegisteredNode(nodeId, ldrId);
        }

        /**
         * Gets property undeployed.
         *
         * @return Property undeployed.
         */
        boolean isUndeployed() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1229, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return undeployed;
        }

        /**
         * Callback for undeployment.
         */
        void onUndeployed() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1238, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            undeployed = true;
        }

        /**
         * Gets property pendingUndeploy.
         *
         * @return Property pendingUndeploy.
         */
        boolean isPendingUndeploy() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1249, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return pendingUndeploy;
        }

        /**
         * Invoked whenever this deployment is scheduled to be undeployed.
         * Used for handling obsolete or phantom requests.
         */
        void onUndeploySheduled() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1259, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            pendingUndeploy = true;
        }

        /**
         * Acquires class.
         */
        void onAcquire() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1268, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert usage >= 0 : "ASSERTION [line=1269, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            usage++;
        }

        /**
         * Releases class.
         */
        void onRelease() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1278, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert usage > 0 : "ASSERTION [line=1279, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            usage--;
        }

        /**
         * Gets usages.
         *
         * @return Usages.
         */
        int getUsages() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1290, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";
            assert usage >= 0 : "ASSERTION [line=1291, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return usage;
        }

        /**
         * Gets property removed.
         *
         * @return Property removed.
         */
        boolean isRemoved() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1302, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            return removed;
        }

        /**
         * Sets property removed.
         */
        void onRemoved() {
            assert Thread.holdsLock(mux) == true : "ASSERTION [line=1311, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            removed = true;

            Collection<UUID> deadIds = clsLdr.getRegisteredClaassLoaderIds();

            if (log.isDebugEnabled() == true) {
                log.debug("Registering dead class loader IDs: " + deadIds);
            }

            deadClsLdrs.addAll(deadIds);
        }

        /**
         * Called for every deployed class.
         *
         * @param cls Deployed class.
         */
        void recordDeployed(GridDeploymentClass cls) {
            assert Thread.holdsLock(mux) == false : "ASSERTION [line=1330, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            String msg = (isTask(cls.getDeployedClass()) == true ? "Task" : "Class") +
                " was deployed in SHARED or CONTINUOUS mode: " + cls;

            GridEventType evt = isTask(cls.getDeployedClass()) == true ?
                GridEventType.TASK_DEPLOYED : GridEventType.CLASS_DEPLOYED;

            mgrReg.getEventStorageManager().record(evt, cls.getAlias(), cls.getUserVersion(), null, msg);

            if (log.isInfoEnabled() == true) {
                log.info(msg);
            }
        }

        /**
         * Called to record all undeployed classes..
         */
        void recordUndeployed() {
            assert Thread.holdsLock(mux) == false : "ASSERTION [line=1349, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            for (GridDeploymentClass cls : clsMap.values()) {
                String msg = (isTask(cls.getDeployedClass()) == true ? "Task" : "Class") +
                    " was undeployed in SHARED or CONTINUOUS mode: " + cls;

                GridEventType evt = isTask(cls.getDeployedClass()) == true ?
                    GridEventType.TASK_UNDEPLOYED : GridEventType.CLASS_UNDEPLOYED;

                mgrReg.getEventStorageManager().record(evt, cls.getAlias(), cls.getUserVersion(), null, msg);

                if (log.isInfoEnabled() == true) {
                    log.info(msg);
                }
            }

            // Resource cleanup.
            procReg.getResourceProcessor().onUndeployed(clsLdr);
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
         * @param clsName Class name of deployed class.
         * @param alias Class alias name of deployed class.
         * @return Deployed class.
         */
        GridDeploymentClass getDeployedClass(String clsName, String alias) {
            GridDeploymentClass depCls = clsMap.get(clsName);

            if (depCls != null) {
                return depCls;
            }

            assert Thread.holdsLock(mux) == false : "ASSERTION [line=1396, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentPerVersionStore.java]";

            //noinspection UnusedCatchParameter
            try {
                depCls = new GridDeploymentClass(
                    Class.forName(clsName, true, clsLdr),
                    alias,
                    depMode,
                    clsLdr,
                    clsLdrId,
                    seqNum,
                    userVer,
                    false);

                GridDeploymentClass cur = clsMap.putIfAbsent(clsName, depCls);

                // Always return existing deployment.
                if (cur != null) {
                    return cur;
                }

                recordDeployed(depCls);

                return depCls;
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return GridToStringBuilder.toString(SharedDeployment.class, this);
        }
    }
}
