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

package org.gridgain.grid.kernal.managers.collision;

import java.util.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.spi.collision.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridCollisionManager extends GridManagerAdapter<GridCollisionSpi> {
    /** */
    private final AtomicReference<GridCollisionExternalListener> extListener =
        new AtomicReference<GridCollisionExternalListener>(null);

    /**
     * @param cfg Grid configuration.
     * @param mgrReg Managers registry.
     * @param procReg Processor registry.
     */
    public GridCollisionManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridCollisionSpi.class, cfg, procReg, mgrReg, cfg.getCollisionSpi());
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        startSpi();

        getSpi().setExternalCollisionListener(new GridCollisionExternalListener() {
            /**
             * {@inheritDoc}
             */
            public void onExternalCollision() {
                GridCollisionExternalListener listener = extListener.get();

                if (listener != null) {
                    listener.onExternalCollision();
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
    public void stop() throws GridException {
        stopSpi();

        // Unsubscribe.
        getSpi().setExternalCollisionListener(null);

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     *
     * @param listener Listener to external collision events.
     */
    public void setCollisionExternalListener(GridCollisionExternalListener listener) {
        if (listener != null && extListener.compareAndSet(null, listener) == false) {
            assert false : "Collision external listener has already been set " +
                "(perhaps need to add support for multiple listeners)";
        }
        else if (log.isDebugEnabled() == true) {
            log.debug("Successfully set external collision listner: " + listener);
        }
    }

    /**
     *
     * @param waitJobs List of waiting jobs.
     * @param activeJobs List of active jobs.
     */
    public void onCollision(Collection<GridCollisionJobContext> waitJobs,
        Collection<GridCollisionJobContext> activeJobs) {
        if (log.isDebugEnabled() == true) {
            log.debug("Resolving job collisions [waitJobs=" + waitJobs + ", activeJobs=" + activeJobs + ']');
        }

        getSpi().onCollision(waitJobs, activeJobs);
    }
}
