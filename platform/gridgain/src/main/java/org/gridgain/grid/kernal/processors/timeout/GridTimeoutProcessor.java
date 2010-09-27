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

package org.gridgain.grid.kernal.processors.timeout;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.runnable.*;

/**
 * Detects timeout events and processes them.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridTimeoutProcessor extends GridProcessorAdapter {
    /** */
    private final GridThread timeoutWorker;

    /** */
    private final Map<UUID, GridTimeoutObject> timeoutObjs = new HashMap<UUID, GridTimeoutObject>();

    /** */
    private long nextWakeupTime = 0;

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param mgrReg Managers registry.
     * @param procReg Processor registry.
     * @param cfg Grid configuration.
     */
    public GridTimeoutProcessor(GridManagerRegistry mgrReg, GridProcessorRegistry procReg, GridConfiguration cfg) {
        super(mgrReg, procReg, cfg);

        timeoutWorker = new GridThread(cfg.getGridName(), "grid-timeout-worker",
            new TimeoutWorker());
    }

    /**
     * {@inheritDoc}
     */
    public void start() {
        timeoutWorker.start();

        if (log.isDebugEnabled() == true) {
            log.debug("Timeout processor started.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(boolean cancel) throws GridException {
        timeoutWorker.interrupt();

        try {
            timeoutWorker.join();
        }
        catch (InterruptedException e) {
            throw (GridException)new GridException("Failed to stop timeout processor." ,e).setData(84, "src/java/org/gridgain/grid/kernal/processors/timeout/GridTimeoutProcessor.java");
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Timeout processor stopped.");
        }
    }

    /**
     *
     * @param timeoutObj FIXDOC
     */
    public void addTimeoutObject(GridTimeoutObject timeoutObj) {
        if (timeoutObj.getEndTime() <= 0 || timeoutObj.getEndTime() == Long.MAX_VALUE) {
            // Timeout will never happen.
            return;
        }

        synchronized (mux) {
            timeoutObjs.put(timeoutObj.getTimeoutId(), timeoutObj);

            if (timeoutObj.getEndTime() < nextWakeupTime) {
                mux.notifyAll();
            }
        }
    }

    /**
     *
     * @param timeoutObj FIXDOC
     */
    public void removeTimeoutObject(GridTimeoutObject timeoutObj) {
        removeTimeoutObject(timeoutObj.getTimeoutId());
    }

    /**
     *
     * @param timeoutId FIXDOC
     */
    public void removeTimeoutObject(UUID timeoutId) {
        synchronized (mux) {
            timeoutObjs.remove(timeoutId);
        }
    }

    /**
     * Handles job timeouts.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class TimeoutWorker extends GridRunnable {
        /**
         *
         */
        TimeoutWorker() {
            super(cfg.getGridName(), "grid-job-timeout-worker", log);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"UnconditionalWait"})
        @Override
        protected void body() throws InterruptedException {
            List<GridTimeoutObject> expiredObjs = new ArrayList<GridTimeoutObject>();

            while (isCancelled() == false) {
                long nextTime = Long.MAX_VALUE;

                long now = System.currentTimeMillis();

                synchronized (mux) {
                    for (Iterator<Map.Entry<UUID, GridTimeoutObject>> iter = timeoutObjs.entrySet().iterator();
                        iter.hasNext();) {
                        Map.Entry<UUID, GridTimeoutObject> entry = iter.next();

                        if (entry.getValue().getEndTime() <= now) {
                            expiredObjs.add(entry.getValue());

                            iter.remove();
                        }
                        else if (entry.getValue().getEndTime() < nextTime) {
                            nextTime = entry.getValue().getEndTime();
                        }
                    }
                }

                // Process timed out jobs outside of synchronization.
                for (GridTimeoutObject expiredObj : expiredObjs) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Timeout has occurred: " + expiredObj);
                    }

                    expiredObj.onTimeout();
                }

                expiredObjs.clear();

                now = System.currentTimeMillis();

                if (nextTime > now) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Going to sleep for: " + (nextTime - now) + "ms.");
                    }

                    synchronized (mux) {
                        nextWakeupTime = nextTime - now;

                        mux.wait(nextWakeupTime);
                    }
                }
            }
        }
    }
}
