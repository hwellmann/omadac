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
import java.util.Map.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.*;

/**
 * Storage for local deployments.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridDeploymentLocalStore extends GridDeploymentStoreAdapter {
    /** Sequence. */
    private static final AtomicLong seq = new AtomicLong(0);

    /** Deployment cache by class name. */
    private final Map<String, LinkedList<LocalDeploymentClass>> cache =
        new HashMap<String, LinkedList<LocalDeploymentClass>>();

    /** Mutex. */
    private final Object mux = new Object();

    /**
     * @param spi Deployment SPI.
     * @param cfg Grid configuration.
     * @param mgrReg Manager registry.
     * @param procReg Processor registry.
     * @param comm Deployment communication.
     */
    GridDeploymentLocalStore(GridDeploymentSpi spi, GridConfiguration cfg, GridManagerRegistry mgrReg,
        GridProcessorRegistry procReg, GridDeploymentCommunication comm) {
        super(spi, cfg, mgrReg, procReg, comm);
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        spi.setListener(new LocalDeploymentListener());

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        spi.setListener(null);

        Map<String, List<LocalDeploymentClass>> copy;

        synchronized (mux) {
            copy = new HashMap<String, List<LocalDeploymentClass>>(cache);

            for (Entry<String, List<LocalDeploymentClass>> entry : copy.entrySet()) {
                entry.setValue(new ArrayList<LocalDeploymentClass>(entry.getValue()));
            }
        }

        for (List<LocalDeploymentClass> deps : copy.values()) {
            for (LocalDeploymentClass cls : deps) {
                // We don't record event, as recording causes invocation of
                // discovery manager which is already stopped.
                undeploy(cls.getClassLoader(), /*don't record event. */false);

                // Safety.
                assert cls.isUndeployed() == true : "ASSERTION [line=98, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";
                assert cls.getUsages() == 0 : "ASSERTION [line=99, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]. " + "Invalid number of usages: " + cls.getUsages();
            }
        }

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridDeploymentClass> getDeployments() {
        List<GridDeploymentClass> deps = new ArrayList<GridDeploymentClass>();

        synchronized (mux) {
            for (List<LocalDeploymentClass> depList : cache.values()) {
                for (LocalDeploymentClass d : depList) {
                    if (deps.contains(d) == false) {
                        deps.add(d);
                    }
                }
            }

            return deps;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getClassLoader(UUID ldrId) {
        synchronized (mux) {
            for (List<LocalDeploymentClass> deps : cache.values()) {
                 for (LocalDeploymentClass cls : deps) {
                     if (cls.getClassLoaderId().equals(ldrId) == true) {
                         return cls.getClassLoader();
                     }
                 }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public GridDeploymentClass acquireClass(GridDeploymentMetadata meta) {
        LocalDeploymentClass dep = null;

        synchronized (mux) {
            // Validate metadata.
            assert meta.getAlias() != null : "ASSERTION [line=153, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

            dep = getDeploymentClass(meta.getAlias());

            if (dep != null) {
                dep.onAcquire();

                if (log.isDebugEnabled() == true) {
                    log.debug("Acquired deployment class from local cache: " + dep);
                }

                return dep;
            }

            GridDeploymentResource rsrc = spi.findResource(meta.getAlias());

            if (rsrc != null) {
                dep = deploy(cfg.getDeploymentMode(), rsrc.getClassLoader(), rsrc.getResourceClass(), rsrc.getName());

                if (dep == null) {
                    return null;
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Acquired deployment class from SPI: " + dep);
                }

                dep.onAcquire();
            }
            // Auto-deploy.
            else {
                ClassLoader ldr = Thread.currentThread().getContextClassLoader();

                // Safety.
                if (ldr == null) {
                    ldr = getClass().getClassLoader();
                }

                //noinspection UnusedCatchParameter
                try {
                    // Check that class can be loaded.
                    Class<?> cls = ldr.loadClass(meta.getAlias());

                    if (ldr.getClass().equals(GridDeploymentClassLoader.class) == true) {
                        GridDeploymentClassLoader depLdr = (GridDeploymentClassLoader)ldr;

                        ldr = depLdr.getParent();
                    }

                    spi.register(ldr, cls);

                    rsrc = spi.findResource(meta.getAlias());

                    if (rsrc != null && rsrc.getResourceClass().equals(cls) == true) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Retrieved auto-loaded resource from spi: " + rsrc);
                        }

                        dep = deploy(cfg.getDeploymentMode(), ldr, cls, rsrc.getName());

                        if (dep == null) {
                            return null;
                        }

                        dep.onAcquire();
                    }
                    else {
                        log.warning("Failed to find resource from deployment SPI even after registering it: " +
                            meta.getAlias());

                        return null;
                    }
                }
                catch (ClassNotFoundException e) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Failed to load class for local auto-deployment: " + meta.getClassName());
                    }

                    return null;
                }
                catch (GridSpiException e) {
                    log.error("Failed to deploy local class: " + meta.getAlias(), e);

                    return null;
                }
            }
        }

        recordDeploy(dep, meta.isRecord());

        if (log.isDebugEnabled() == true) {
            log.debug("Acquired deployment class: " + dep);
        }

        return dep;
    }

    /**
     *
     * @param alias Class alias.
     * @return Deployed class.
     */
    private LocalDeploymentClass getDeploymentClass(String alias) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=256, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

        //noinspection CollectionDeclaredAsConcreteClass
        LinkedList<LocalDeploymentClass> deps = cache.get(alias);

        if (deps != null) {
            assert deps.isEmpty() == false : "ASSERTION [line=262, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

            LocalDeploymentClass depCls = deps.getFirst();

            if (depCls.isUndeployed() == false) {
                return depCls;
            }
        }

        return null;
    }

    /**
     *
     * @param depMode Deployment mode.
     * @param ldr Class loader to deploy.
     * @param cls Class Loader.
     * @param alias Class name or task name.
     * @return Deployment.
     */
    private LocalDeploymentClass deploy(GridDeploymentMode depMode, ClassLoader ldr, Class<?> cls, String alias) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=283, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

        LocalDeploymentClass other = null;

        // Find existing class loader info.
        for (List<LocalDeploymentClass> deps : cache.values()) {
            for (LocalDeploymentClass d : deps) {
                //noinspection ObjectEquality
                if (d.getClassLoader() == ldr) {
                    other = d;

                    break;
                }
            }
        }

        UUID ldrId = null;

        long seqNum = 0;

        String userVer = null;

        // If class loader exists.
        if (other != null) {
            ldrId = other.getClassLoaderId();

            seqNum = other.getSequenceNumber();

            userVer = other.getUserVersion();
        }
        // New class loader.
        else {
            ldrId = UUID.randomUUID();

            seqNum = seq.incrementAndGet();

            userVer = getUserVersion(ldr);
        }

        LocalDeploymentClass dep = new LocalDeploymentClass(cls, alias, depMode, ldr, ldrId, seqNum, userVer);

        //noinspection CollectionDeclaredAsConcreteClass
        LinkedList<LocalDeploymentClass> deps = cache.get(alias);

        if (deps == null) {
            cache.put(alias, deps = new LinkedList<LocalDeploymentClass>());
        }

        if (deps.isEmpty() == false) {
            for (LocalDeploymentClass d : deps) {
                if (d.isUndeployed() == false) {
                    log.error("Found more than one active deployment for the same resource " +
                        "[cls=" + cls + ", alias=" + alias + ", depMode=" + depMode + ", dep=" + d + ']');

                    return null;
                }
            }
        }

        // Add at the beginning of the list for future fast access.
        deps.addFirst(dep);

        if (cls.getName().equals(alias) == false) {
            // Cache by class name.
            cache.put(cls.getName(), deps);
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Created new deployment: " + dep);
        }

        return dep;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseClass(GridDeploymentClass cls) {
        assert cls != null : "ASSERTION [line=362, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

        if (cls.isLocal() == true) {
            if (log.isDebugEnabled() == true) {
                log.debug("Releasing deployment class: " + cls);
            }

            LocalDeploymentClass dep = (LocalDeploymentClass)cls;

            boolean undeployed = false;

            boolean undeployLdr = false;

            synchronized (mux) {
                dep.onRelease();

                if (dep.isUndeployed() == true && dep.getUsages() == 0) {
                    undeployLdr = true;

                    List<LocalDeploymentClass> deps = cache.get(dep.getAlias());

                    if (deps != null) {
                        assert deps.isEmpty() == false : "ASSERTION [line=384, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

                        for (Iterator<LocalDeploymentClass> iter = deps.iterator(); iter.hasNext() == true;) {
                            LocalDeploymentClass d = iter.next();

                            //noinspection ObjectEquality
                            if (d == dep) {
                                undeployed = true;

                                break;
                            }
                        }
                    }

                    if (undeployed == false) {
                        log.error("Released class was not found: " + dep);
                    }
                }
            }

            if (undeployLdr == true) {
                undeploy(dep.getClassLoader(), true);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void explicitDeploy(Class<?> cls, ClassLoader clsLdr) throws GridException {
        try {
            LocalDeploymentClass dep = null;

            // Make sure not to deploy peer loaded tasks with non-local class loader,
            // if local one exists.
            if (clsLdr.getClass().equals(GridDeploymentClassLoader.class) == true) {
                GridDeploymentClassLoader depLdr = (GridDeploymentClassLoader)clsLdr;

                clsLdr = depLdr.getParent();
            }

            synchronized (mux) {
                boolean deployed = spi.register(clsLdr, cls);

                if (deployed == true) {
                    dep = getDeploymentClass(cls.getName());

                    if (dep == null) {
                        GridDeploymentResource rsrc = spi.findResource(cls.getName());

                        //noinspection ObjectEquality
                        if (rsrc != null && rsrc.getClassLoader() == clsLdr) {
                            dep = deploy(cfg.getDeploymentMode(), rsrc.getClassLoader(), rsrc.getResourceClass(),
                                rsrc.getName());
                        }
                    }

                    if (dep != null) {
                        recordDeploy(dep, true);
                    }
                }
            }
        }
        catch (GridSpiException e) {
            recordDeployFailed(cls, clsLdr, true);

            // Avoid double wrapping.
            if (e.getCause() instanceof GridException == true) {
                throw (GridException)e.getCause();
            }

            throw (GridException)new GridException("Failed to deploy class: " + cls.getName(), e).setData(456, "src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void explicitUndeploy(UUID nodeId, String rsrcName) {
        assert rsrcName != null : "ASSERTION [line=464, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

        // Simply delegate to SPI.
        // Internal cache will be cleared once
        // undeployment callback is received from SPI.
        spi.unregister(rsrcName);
    }

    /**
     * Records deploy event.
     *
     * @param cls Deployed class.
     * @param recordEvt Flag indicating whether to record events.
     */
    private void recordDeploy(LocalDeploymentClass cls, boolean recordEvt) {
        assert cls != null : "ASSERTION [line=479, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

        UUID locNodeId = mgrReg.getDiscoveryManager().getLocalNode().getId();

        String msg = (isTask(cls.getDeployedClass()) == true ? "Task" : "Class") + " locally deployed: " + cls;

        GridEventType evt = isTask(cls.getDeployedClass()) == true ?
            GridEventType.TASK_DEPLOYED : GridEventType.CLASS_DEPLOYED;

        if (recordEvt == true) {
            mgrReg.getEventStorageManager().record(evt, cls.getAlias(), cls.getUserVersion(), locNodeId, msg);
        }

        if (log.isInfoEnabled() == true) {
            log.info(msg);
        }
    }

    /**
     * Records deploy event.
     *
     * @param cls Deployed class.
     * @param clsLdr Class loader.
     * @param recordEvt Flag indicating whether to record events.
     */
    @SuppressWarnings({"unchecked"})
    private void recordDeployFailed(Class<?> cls, ClassLoader clsLdr, boolean recordEvt) {
        assert cls != null : "ASSERTION [line=506, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";
        assert clsLdr != null : "ASSERTION [line=507, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

        UUID locNodeId = mgrReg.getDiscoveryManager().getLocalNode().getId();

        String msg = "Failed to deploy " + (isTask(cls) == true ? "task" : "class") +
            " [cls=" + cls + ", clsLdr=" + clsLdr + ']';

        GridEventType evt = isTask(cls) == true ?
            GridEventType.TASK_DEPLOYMENT_FAILED : GridEventType.CLASS_DEPLOYMENT_FAILED;

        String taskName = isTask(cls) == true ? GridUtils.getTaskName((Class<? extends GridTask<?, ?>>)cls) : null;

        if (recordEvt == true) {
            mgrReg.getEventStorageManager().record(evt, taskName, getUserVersion(clsLdr), locNodeId, msg);
        }

        if (log.isInfoEnabled() == true) {
            log.info(msg);
        }
    }

    /**
     * Records undeploy event.
     *
     * @param cls Undeployed class.
     * @param recordEvt Flag indicating whether to record events.
     */
    private void recordUndeploy(LocalDeploymentClass cls, boolean recordEvt) {
        assert cls.isUndeployed() == true : "ASSERTION [line=535, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";
        assert cls.getUsages() == 0 : "ASSERTION [line=536, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]";

        UUID locNodeId = mgrReg.getDiscoveryManager().getLocalNode().getId();

        String msg = isTask(cls.getDeployedClass()) == true ?
            "Task locally undeployed: " + cls : "Class locally undeployed: " + cls;

        GridEventType evt = isTask(cls.getDeployedClass()) == true ?
            GridEventType.TASK_UNDEPLOYED : GridEventType.CLASS_UNDEPLOYED;

        if (recordEvt == true) {
            mgrReg.getEventStorageManager().record(evt, cls.getAlias(), cls.getUserVersion(), locNodeId, msg);
        }

        if (log.isInfoEnabled() == true) {
            log.info(msg);
        }
    }

    /**
     *
     * @param ldr Class loader to undeploy.
     * @param recEvt Whether or not to record the event.
     */
    private void undeploy(ClassLoader ldr, boolean recEvt) {
        Set<LocalDeploymentClass> doomed = new HashSet<LocalDeploymentClass>();

        boolean releaseRsrcs = true;

        synchronized (mux) {
            for (Iterator<LinkedList<LocalDeploymentClass>> i1 = cache.values().iterator(); i1.hasNext() == true;) {
                //noinspection CollectionDeclaredAsConcreteClass
                LinkedList<LocalDeploymentClass> deps = i1.next();

                for (Iterator<LocalDeploymentClass> i2 = deps.iterator(); i2.hasNext() == true;) {
                    LocalDeploymentClass dep = i2.next();

                    //noinspection ObjectEquality
                    if (dep.getClassLoader() == ldr) {
                        dep.undeploy();

                        if (dep.getUsages() == 0) {
                            i2.remove();

                            doomed.add(dep);

                            if (log.isInfoEnabled() == true) {
                                log.info("Removed undeployed class: " + dep);
                            }
                        }
                        else {
                            releaseRsrcs = false;

                            if (log.isInfoEnabled() == true) {
                                log.info("Undeployed class was kept due to active usages " +
                                    "(it will be undeployed once no more usages are there): " + dep);
                            }
                        }
                    }
                }

                if (deps.isEmpty() == true) {
                    i1.remove();
                }
            }
        }

        if (recEvt == true) {
            for (LocalDeploymentClass cls : doomed) {
                recordUndeploy(cls, true);
            }
        }

        if (releaseRsrcs == true) {
            // Resource cleanup.
            procReg.getResourceProcessor().onUndeployed(ldr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentLocalStore.class, this);
    }

    /**
     *
     */
    private class LocalDeploymentClass extends GridDeploymentClass {
        /** Undeployed flag. */
        private boolean undeployed = false;

        /** Number of usages. */
        private int usage = 0;

        /**
         * @param cls Deployed class.
         * @param alias Class alias.
         * @param depMode Deployment mode.
         * @param clsLdr Class loader.
         * @param clsLdrId Class loader ID.
         * @param seqNum Sequence number.
         * @param userVer User version.
         */
        LocalDeploymentClass(Class<?> cls, String alias, GridDeploymentMode depMode, ClassLoader clsLdr,
            UUID clsLdrId, long seqNum, String userVer) {
            super(cls, alias, depMode, clsLdr, clsLdrId, seqNum, userVer, true);
        }

        /**
         * Gets property undeployed.
         *
         * @return Property undeployed.
         */
        synchronized boolean isUndeployed() {
            return undeployed;
        }

        /**
         * Sets property undeployed.
         */
        synchronized void undeploy() {
            undeployed = true;
        }

        /**
         * Acquires class.
         */
        synchronized void onAcquire() {
            assert usage >= 0 : "ASSERTION [line=667, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]. " + "Invalid usage count [cnt=" + usage + ", dep=" + this + ']';

            usage++;
        }

        /**
         * Releases class.
         */
        synchronized void onRelease() {
            assert usage > 0 : "ASSERTION [line=676, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]. " + "Invalid usage count [cnt=" + usage + ", dep=" + this + ']';

            usage--;
        }

        /**
         * Gets usages.
         *
         * @return Usages.
         */
        synchronized int getUsages() {
            assert usage >= 0 : "ASSERTION [line=687, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentLocalStore.java]. " + "Invalid usage count [cnt=" + usage + ", dep=" + this + ']';

            return usage;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return GridToStringBuilder.toString(LocalDeploymentClass.class, this, "super", super.toString());
        }
    }

    /**
     *
     */
    private class LocalDeploymentListener implements GridDeploymentListener {
        /**
         * {@inheritDoc}
         */
        public void onUnregistered(ClassLoader ldr) {
            if (log.isDebugEnabled() == true) {
                log.debug("Received callback from SPI to unregister class loader: " + ldr);
            }

            undeploy(ldr, true);
        }
    }
}
