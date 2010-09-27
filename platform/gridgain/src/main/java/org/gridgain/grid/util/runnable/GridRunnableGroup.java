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

package org.gridgain.grid.util.runnable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Group to which all runnables belong. This class contains general
 * management functionality for runnables.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridRunnableGroup {
    /** Singleton instance. */
    private static final Map<String, GridRunnableGroup> grps = new HashMap<String, GridRunnableGroup>();

    /** Read-write lock. */
    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /** Dummy object for active map. */
    private static final Object dummy = new Object();

    /** Grid name. */
    @SuppressWarnings("unused")
    private final String gridName;

    /** Map of runnables concurrently executing. */
    private final ConcurrentMap<GridRunnable, Object> activeMap =
        new ConcurrentHashMap<GridRunnable, Object>(100, 0.75f, 64);

    /**
     * Create a group for specified grid.
     *
     * @param gridName Name of grid to create group for.
     */
    private GridRunnableGroup(String gridName) {
        this.gridName = gridName;
    }

    /**
     * Gets singleton instance. This method is called very
     * frequently, for example, every time a job starts and
     * ends execution, and for that reason is well-optimized
     * for concurrent access.
     *
     * @param gridName Name of grid the group is for.
     * @return Singleton instance.
     */
    public static GridRunnableGroup getInstance(String gridName) {
        GridRunnableGroup grp = null;

        rwLock.readLock().lock();

        try {
            grp = grps.get(gridName);

            if (grp != null) {
                return grp;
            }
        }
        finally {
            rwLock.readLock().unlock();
        }

        rwLock.writeLock().lock();

        try {
            grp = grps.get(gridName);

            // Check again as another thread could have
            // created the group already.
            if (grp == null) {
                grps.put(gridName, grp = new GridRunnableGroup(gridName));
            }

            return grp;
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Removes grid runnable group for given grid.
     * This should happen only on grid shutdown.
     *
     * @param gridName Name of grid.
     */
    public static void removeInstance(String gridName) {
        rwLock.writeLock().lock();

        try {
            grps.remove(gridName);
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Gets currently running runnable tasks.
     *
     * @return Currently running runnable tasks.
     */
    public Set<GridRunnable> getActiveSet() {
        return new HashSet<GridRunnable>(activeMap.keySet());
    }

    /**
     * Callback initiated by instances of {@link GridRunnable} at execution startup.
     *
     * @param r Runnable task that got started.
     */
    void onStart(GridRunnable r) {
        // No synchronization for concurrent map.
        activeMap.put(r, dummy);
    }

    /**
     * Callback initiated by instances of {@link GridRunnable} at end of execution.
     *
     * @param r Runnable task that is ending.
     */
    void onFinish(GridRunnable r) {
        // No synchronization for concurrent map.
        activeMap.remove(r);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridRunnableGroup.class, this);
    }
}
