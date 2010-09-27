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
 * GNU LESSER GENERAL LICENSE FOR MORE DETAILS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE ALONG WITH THIS LIBRARY; IF NOT, WRITE TO THE FREE
 * SOFTWARE FOUNDATION, INC., 51 FRANKLIN ST, FIFTH FLOOR, BOSTON, MA
 * 02110-1301 USA
 */

package org.gridgain.grid.spi.loadbalancing.affinity;

import java.security.*;
import java.util.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;

/**
 * Controls data to node affinity. For reference on algorithm see
 * <a href="http://weblogs.java.net/blog/tomwhite/archive/2007/11/consistent_hash.html">Tom White's Blog</a>.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridAffinity {
    /** Grid logger. */
    private final GridLogger log;

    /** Affinity seed. */
    private final String affSeed;

    /** Map of hash assignments. */
    private final SortedMap<Integer, GridNode> circle = new TreeMap<Integer, GridNode>();

    /** Flag indicating whether exception has been logged for hash function. */
    private boolean isErrLogged = false;

    /** Read/write lock. */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     *
     * @param affSeed Affinity seed.
     * @param log Grid logger.
     */
    GridAffinity(String affSeed, GridLogger log) {
        assert affSeed != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/spi/loadbalancing/affinity/GridAffinity.java]";
        assert log != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/spi/loadbalancing/affinity/GridAffinity.java]";

        this.affSeed = affSeed;
        this.log = log;
    }

    /**
     * Adds a node.
     *
     * @param node New node.
     * @param replicas Number of replicas for the node.
     */
    void add(GridNode node, int replicas) {
        String prefix = affSeed + node.getId().toString();

        rwLock.writeLock().lock();

        try {
            for (int i = 0; i < replicas; i++) {
                circle.put(hash(prefix + i), node);
            }
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Removes a node.
     *
     * @param node Node to remove.
     * @param replicas Number of replicas for the node.
     */
    void remove(GridNode node, int replicas) {
        String prefix = affSeed + node.getId().toString();

        rwLock.writeLock().lock();

        try {
            for (int i = 0; i < replicas; i++) {
                circle.remove(hash(prefix + i));
            }
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Gets node for a key.
     *
     * @param key Key.
     * @return Node.
     */
    GridNode get(Object key) {
        int hash = hash(key);

        rwLock.readLock().lock();

        try {
            if (circle.isEmpty() == true) {
                log.warning("There are no nodes present in topology.");

                return null;
            }

            SortedMap<Integer, GridNode> tailMap = circle.tailMap(hash);

            // Get first node hash in the circle clock-wise.
            return circle.get(tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey());
        }
        finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Gets node for a given key within given topology.
     *
     * @param key Key to get node for.
     * @param top Topology of nodes.
     * @return Node for key, or <tt>null</tt> if node was not found.
     */
    GridNode get(Object key, Set<GridNode> top) {
        int cnt = 0;

        int hash = hash(key);

        rwLock.readLock().lock();

        try {
            if (circle.isEmpty() == true) {
                log.warning("There are no nodes present in topology.");

                return null;
            }

            SortedMap<Integer, GridNode> tailMap = circle.tailMap(hash);

            int size = circle.size();

            // Move clock-wise starting from selected position.
            for (GridNode node : tailMap.values()) {
                if (top.contains(node) == true) {
                    return node;
                }

                if (cnt++ >= size) {
                    break;
                }
            }

            if (cnt < size) {
                // Wrap around moving clock-wise.
                for (GridNode node : circle.values()) {
                    if (top.contains(node) == true) {
                        return node;
                    }

                    if (cnt++ >= size) {
                        break;
                    }
                }
            }
        }
        finally {
            rwLock.readLock().unlock();
        }

        return null;
    }

    /**
     * Gets hash code for a given object.
     *
     * @param o Object to get hash code for.
     * @return Hash code.
     */
    private int hash(Object o) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            return bytesToInt(md5.digest(GridUtils.intToBytes(o.hashCode())));
        }
        catch (NoSuchAlgorithmException e) {
            if (isErrLogged == false) {
                isErrLogged = true;

                log.error("Failed to get an instance of MD5 message digest (will use default hashcode)", e);
            }

            return o.hashCode();
        }
    }

    /**
     * Constructs <tt>int</tt> from byte array.
     *
     * @param bytes Array of bytes.
     * @return Integer value.
     */
    private int bytesToInt(byte[] bytes) {
        assert bytes != null : "ASSERTION [line=223, file=src/java/org/gridgain/grid/spi/loadbalancing/affinity/GridAffinity.java]";

        int bytesCnt = Integer.SIZE >> 3;

        if (bytesCnt > bytes.length) {
            bytesCnt = bytes.length;
        }

        int off = 0;
        int res = 0;

        for (int i = 0; i < bytesCnt; i++) {
            int shift = (bytesCnt - i - 1) << 3;

            res |= (0xffL & bytes[off++]) << shift;
        }

        return res;
    }
}
