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

import org.gridgain.grid.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import java.util.*;

/**
 * Deployment manager.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridDeploymentManager extends GridManagerAdapter<GridDeploymentSpi> {
    /** Local deployment storage. */
    private GridDeploymentStore locStore = null;

    /** Isolated mode storage.. */
    private GridDeploymentStore ldrStore = null;

    /** Shared mode storage. */
    private GridDeploymentStore verStore = null;

    /** */
    private GridDeploymentCommunication comm = null;

    /**
     * @param mgrReg Managers registry.
     * @param procReg Processor registry.
     * @param cfg Grid configuration.
     */
    public GridDeploymentManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridDeploymentSpi.class, cfg, procReg, mgrReg, cfg.getDeploymentSpi());
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        if (cfg.isPeerClassLoadingEnabled() == true) {
            assertParameter(cfg.getPeerClassLoadingTimeout() > 0, "peerClassLoadingTimeout > 0");
        }

        startSpi();

        comm = new GridDeploymentCommunication(mgrReg, log);

        comm.start();

        locStore = new GridDeploymentLocalStore(getSpi(), cfg, mgrReg, procReg, comm);
        ldrStore = new GridDeploymentPerLoaderStore(getSpi(), cfg, mgrReg, procReg, comm);
        verStore = new GridDeploymentPerVersionStore(getSpi(), cfg, mgrReg, procReg, comm);

        locStore.start();
        ldrStore.start();
        verStore.start();

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws GridException {
        if (verStore != null) {
            verStore.stop();
        }

        if (ldrStore != null) {
            ldrStore.stop();
        }

        if (locStore != null) {
            locStore.stop();
        }

        if (comm != null) {
            comm.stop();
        }

        getSpi().setListener(null);

        stopSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStart() throws GridException {
        locStore.onKernalStart();
        ldrStore.onKernalStart();
        verStore.onKernalStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStop() {
        if (verStore != null) {
            verStore.onKernalStop();
        }

        if (ldrStore != null) {
            ldrStore.onKernalStop();
        }

        if (locStore != null) {
            locStore.onKernalStop();
        }
    }

    /**
     * @return All deployed tasks.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Class<? extends GridTask<?,?>>> findAllTasks() {
        Collection<GridDeploymentClass> deps = locStore.getDeployments();

        Map<String, Class<? extends GridTask<?,?>>> map = new HashMap<String, Class<? extends GridTask<?,?>>>();

        for (GridDeploymentClass dep : deps) {
            if (GridTask.class.isAssignableFrom(dep.getDeployedClass()) == true) {
                map.put(dep.getAlias(), (Class<? extends GridTask<?,?>>)dep.getDeployedClass());
            }
        }

        return map;
    }

    /**
     * @param taskName Task name.
     */
    public void undeployTask(String taskName) {
        assert taskName != null : "ASSERTION [line=164, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentManager.java]";

        locStore.explicitUndeploy(null, taskName);

        try {
            comm.sendUndeployRequest(taskName);
        }
        catch (GridException e) {
            log.error("Failed to send undeployment request for task: " + taskName, e);
        }
    }

    /**
     * @param nodeId Node ID.
     * @param taskName Task name.
     */
    void undeployTask(UUID nodeId, String taskName) {
        assert taskName != null : "ASSERTION [line=181, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentManager.java]";

        locStore.explicitUndeploy(nodeId, taskName);
        ldrStore.explicitUndeploy(nodeId, taskName);
        verStore.explicitUndeploy(nodeId, taskName);
    }

    /**
     * @param cls Class to deploy.
     * @param clsLdr Class loader.
     * @throws GridException If deployment failed.
     */
    public void deploy(Class<?> cls, ClassLoader clsLdr) throws GridException {
        locStore.explicitDeploy(cls, clsLdr);
    }

    /**
     * @param cls Class to deploy.
     * @param clsLdr Class loader.
     * @return Deployment instance.
     * @throws GridException If deployment failed.
     */
    public GridDeploymentClass deployAndAcquire(Class<?> cls, ClassLoader clsLdr) throws GridException {
        locStore.explicitDeploy(cls, clsLdr);

        return acquireLocalClass(cls.getName());
    }

    /**
     * Gets class loader based on given ID.
     *
     * @param ldrId Class loader ID.
     * @return Class loader of <tt>null</tt> if not found.
     */
    public ClassLoader getClassLoader(UUID ldrId) {
        return locStore.getClassLoader(ldrId);
    }

    /**
     * @param dep Deployment class to release.
     */
    public void releaseClass(GridDeploymentClass dep) {
        if (dep.isLocal() == true) {
            locStore.releaseClass(dep);
        }
        else {
            GridDeploymentMode mode = dep.getDeploymentMode();

            if (mode == GridDeploymentMode.ISOLATED || mode == GridDeploymentMode.PRIVATE) {
                ldrStore.releaseClass(dep);
            }
            else {
                assert mode == GridDeploymentMode.CONTINUOUS || mode == GridDeploymentMode.SHARED :
                    "Invalid deployment mode: " + mode;

                verStore.releaseClass(dep);
            }
        }
    }

    /**
     * @param rsrcName Class name.
     * @return Grid cached task.
     */
    public GridDeploymentClass acquireLocalClass(String rsrcName) {
        GridDeploymentMetadata meta = new GridDeploymentMetadata();

        meta.setRecord(true);
        meta.setDeploymentMode(cfg.getDeploymentMode());
        meta.setAlias(rsrcName);
        meta.setClassName(rsrcName);
        meta.setSenderNodeId(mgrReg.getDiscoveryManager().getLocalNode().getId());

        return locStore.acquireClass(meta);
    }

    /**
     * @param depMode Deployment mode.
     * @param rsrcName Resource name (could be task name).
     * @param clsName Class name.
     * @param seqVer Sequence version.
     * @param userVer User version.
     * @param senderNodeId Sender node ID.
     * @param clsLdrId Class loader ID.
     * @return Deployment class if found.
     */
    public GridDeploymentClass acquireGlobalClass(GridDeploymentMode depMode, String rsrcName, String clsName,
        long seqVer, String userVer, UUID senderNodeId, UUID clsLdrId) {

        GridDeploymentMetadata meta = new GridDeploymentMetadata();

        meta.setDeploymentMode(depMode);
        meta.setClassName(clsName);
        meta.setAlias(rsrcName);
        meta.setSequenceNumber(seqVer);
        meta.setUserVersion(userVer);
        meta.setSenderNodeId(senderNodeId);
        meta.setClassLoaderId(clsLdrId);

        if (cfg.isPeerClassLoadingEnabled() == false) {
            meta.setRecord(true);

            return locStore.acquireClass(meta);
        }

        // In shared mode, if class is locally available, we never load
        // from remote node simply because the class loader needs to be "shared".
        if (isPerVersionMode(meta.getDeploymentMode()) == true) {
            meta.setRecord(true);

            GridDeploymentClass locDep = locStore.acquireClass(meta);

            if (locDep != null) {
                if (isPerVersionMode(locDep.getDeploymentMode()) == false) {
                    log.warning("Failed to deploy class in SHARED or CONTINUOUS mode (class is locally deployed " +
                        "in some other mode). Either change GridConfiguration.getDeploymentMode() property to " +
                        "SHARED or CONTINUOUS or remove class from local classpath and any of " +
                        "the local GAR deployments that may have it [cls=" + meta.getClassName() + ", depMode=" +
                        locDep.getDeploymentMode() + ']');

                    locStore.releaseClass(locDep);

                    return null;
                }

                if (locDep.getUserVersion().equals(meta.getUserVersion()) == false) {
                    log.warning("Failed to deploy class in SHARED or CONTINUOUS mode for given user version " +
                        "(class is locally deployed for a different user version) [cls=" + meta.getClassName() +
                        ", localVer=" + locDep.getUserVersion() +
                        ", otherVer=" + meta.getUserVersion() + ']');

                    locStore.releaseClass(locDep);

                    return null;
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Reusing local deployment for shared mode: " + locDep);
                }

                return locDep;
            }

            return verStore.acquireClass(meta);
        }

        // Private or Isolated mode.
        meta.setRecord(false);

        GridDeploymentClass depCls = locStore.acquireClass(meta);

        if (senderNodeId.equals(mgrReg.getDiscoveryManager().getLocalNode().getId()) == true) {
            if (depCls == null) {
                log.warning("Task got undeployed while deployment was in progress: " + meta);
            }

            // For local execution, return the same deployment as for the task.
            return depCls;
        }

        if (depCls != null) {
            meta.setParentLoader(depCls.getClassLoader());

            // We don't need to hold deployment. The class loader will remain alive
            // for as long as we hold the reference to it. 
            locStore.releaseClass(depCls);
        }

        meta.setRecord(true);

        return ldrStore.acquireClass(meta);
    }

    /**
     * @param mode Mode to check.
     * @return <tt>True</tt> if shared mode.
     */
    private boolean isPerVersionMode(GridDeploymentMode mode) {
        return mode == GridDeploymentMode.CONTINUOUS ||
            mode == GridDeploymentMode.SHARED;
    }
}
