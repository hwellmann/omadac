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
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import java.util.*;

/**
 * Adapter for all store implementations.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
abstract class GridDeploymentStoreAdapter implements GridDeploymentStore {
    /** Logger. */
    protected final GridLogger log;

    /** Deployment SPI. */
    protected final GridDeploymentSpi spi;

    /** Processor registry. */
    protected final GridProcessorRegistry procReg;

    /** Manager registry. */
    protected final GridManagerRegistry mgrReg;

    /** Grid configuration. */
    protected final GridConfiguration cfg;

    /** Deployment communication. */
    protected final GridDeploymentCommunication comm;

    /**
     * @param spi Underlying SPI.
     * @param cfg Grid configuration.
     * @param mgrReg Manager registry.
     * @param procReg Processor registry.
     * @param comm Deployment communication.
     */
    GridDeploymentStoreAdapter(GridDeploymentSpi spi, GridConfiguration cfg, GridManagerRegistry mgrReg,
        GridProcessorRegistry procReg, GridDeploymentCommunication comm) {
        assert spi != null : "ASSERTION [line=67, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentStoreAdapter.java]";
        assert cfg != null : "ASSERTION [line=68, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentStoreAdapter.java]";
        assert mgrReg != null : "ASSERTION [line=69, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentStoreAdapter.java]";
        assert procReg != null : "ASSERTION [line=70, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentStoreAdapter.java]";
        assert comm != null : "ASSERTION [line=71, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentStoreAdapter.java]";

        this.spi = spi;
        this.cfg = cfg;
        this.mgrReg = mgrReg;
        this.procReg = procReg;
        this.comm = comm;

        log = cfg.getGridLogger().getLogger(getClass());
    }

    /**
     * @return Startup log message.
     */
    protected final String startInfo() {
        return "Deployment store started: " + this;
    }

    /**
     * @return Stop log message.
     */
    protected final String stopInfo() {
        return "Deployment store stopped: " + this;
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader(UUID ldrId) {
        assert false : "ASSERTION [line=100, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentStoreAdapter.java]. " + "Getting class loader based on ID is not supported for this deployment store: " + this;

        // Never reached.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void onKernalStart() throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Ignoring kernel started callback: " + this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onKernalStop() {
        // No-op.
    }

    /**
     * {@inheritDoc}
     */
    public void explicitDeploy(Class<?> cls, ClassLoader clsLdr) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Ignoring explicit deploy [cls=" + cls + ", clsLdr=" + clsLdr + ']');
        }
    }

    /**
     * {@inheritDoc}
     */
    public void releaseClass(GridDeploymentClass cls) {
        if (log.isDebugEnabled() == true) {
            log.debug("Ignoring class release: " + cls);
        }
    }

    /**
     * @param ldr Class loader.
     * @return User version.
     */
    protected final String getUserVersion(ClassLoader ldr) {
        return GridUserVersionHelper.getUserVersion(ldr, log);
    }

    /**
     * @param cls Class to check.
     * @return <tt>True</tt> if class is task class.
     */
    protected final boolean isTask(Class<?> cls) {
        return GridTask.class.isAssignableFrom(cls) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentStoreAdapter.class, this);
    }
}
