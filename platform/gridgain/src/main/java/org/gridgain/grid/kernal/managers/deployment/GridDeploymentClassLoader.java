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
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Class loader that is able to resolve task subclasses and resources
 * by requesting remote node. Every class that could not be resolved
 * by system class loader will be downloaded from given remote node
 * by task deployment identifier. If identifier has been changed on
 * remote node this class will throw exception.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"CustomClassloader"})
class GridDeploymentClassLoader extends ClassLoader {
    /** <tt>True</tt> for single node deployment. */
    private final boolean singleNode;

    /** Manager registry. */
    private final GridManagerRegistry mgrReg;

    /** */
    @GridToStringExclude
    private final GridLogger log;

    /**
     * Node ID -> Loader ID + seqNum.
     * <p>
     * This map is ordered by access order to make sure that P2P requests
     * are sent to the last accessed node first.
     */
    @GridToStringInclude
    private final Map<UUID, GridPair<UUID, Long>> nodeLdrMap;

    /** */
    @GridToStringExclude
    private final GridDeploymentCommunication comm;

    /** */
    private final List<String> p2pExclude;

    /** P2P timeout. */
    private final long p2pTimeout;

    /** Cache of missed resources names. */
    @GridToStringExclude
    private final GridBoundedLinkedHashSet<String> missedRsrcs;

    /** */
    private final Object mux = new Object();

    /**
     * Creates a new peer class loader.
     * <p>
     * If there is a security manager, its
     * {@link SecurityManager#checkCreateClassLoader()}
     * method is invoked. This may result in a security exception.
     *
     * @param singleNode <tt>True</tt> for single node.
     * @param mgrReg Manager registry.
     * @param parent Parent class loader.
     * @param clsLdrId Remote class loader identifier.
     * @param nodeId ID of node that have initiated task.
     * @param seqNum Sequence number for the class loader.
     * @param comm Communication manager loader will work through.
     * @param p2pTimeout Timeout for class-loading requests.
     * @param log Logger.
     * @param p2pExclude List of P2P loaded packages.
     * @param missedRsrcsCacheSize Size of the missed resources cache.
     * @throws SecurityException If a security manager exists and its
     *      <tt>checkCreateClassLoader</tt> method doesn't allow creation
     *      of a new class loader.
     */
    GridDeploymentClassLoader(boolean singleNode, GridManagerRegistry mgrReg, ClassLoader parent,
        UUID clsLdrId, UUID nodeId, long seqNum, GridDeploymentCommunication comm, long p2pTimeout, GridLogger log,
        List<String> p2pExclude, int missedRsrcsCacheSize) throws SecurityException {
        super(parent);

        assert mgrReg != null : "ASSERTION [line=107, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";
        assert comm != null : "ASSERTION [line=108, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";
        assert p2pTimeout > 0 : "ASSERTION [line=109, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";
        assert log != null : "ASSERTION [line=110, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";
        assert clsLdrId != null : "ASSERTION [line=111, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        this.singleNode = singleNode;
        this.mgrReg = mgrReg;
        this.comm = comm;
        this.p2pTimeout = p2pTimeout;
        this.log = log;
        this.p2pExclude = p2pExclude;

        Map<UUID, GridPair<UUID, Long>> map = new LinkedHashMap<UUID, GridPair<UUID, Long>>(1, 0.75f, true);

        map.put(nodeId, new GridPair<UUID, Long>(clsLdrId, seqNum));

        nodeLdrMap = singleNode == true ? Collections.unmodifiableMap(map) : map;

        missedRsrcs = missedRsrcsCacheSize > 0 ?
            new GridBoundedLinkedHashSet<String>(missedRsrcsCacheSize) : null;
    }

    /**
     * Adds new node and remote class loader id to this class loader.
     * Class loader will ask all associated nodes for the class/resource
     * until find it.
     *
     * @param nodeId Participating node ID.
     * @param ldrId Participating class loader id.
     * @param seqNum Sequence number for the class loader.
     */
    void register(UUID nodeId, UUID ldrId, long seqNum) {
        assert nodeId != null : "ASSERTION [line=140, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";
        assert ldrId != null : "ASSERTION [line=141, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        synchronized (mux) {
            // Make sure to do get in order to chagne iteration order,
            // i.e. put this node first.
            GridPair<UUID, Long> pair = nodeLdrMap.get(nodeId);

            if (pair == null) {
                nodeLdrMap.put(nodeId, new GridPair<UUID, Long>(ldrId, seqNum));
            }
        }
    }

    /**
     * Remove remote node and remote class loader id associated with it from
     * internal map.
     *
     * @param nodeId Participating node ID.
     * @return Removed class loader ID.
     */
    UUID unregister(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=162, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        synchronized (mux) {
            GridPair<UUID, Long> removed = nodeLdrMap.remove(nodeId);

            return removed == null ? null : removed.getValue1();
        }
    }

    /**
     * @return Registered nodes.
     */
    Collection<UUID> getRegisteredNodeIds() {
        synchronized (mux) {
            return new ArrayList<UUID>(nodeLdrMap.keySet());
        }
    }

    /**
     * @return Registered class loader IDs.
     */
    Collection<UUID> getRegisteredClaassLoaderIds() {
        Collection<UUID> ldrIds = new LinkedList<UUID>();

        synchronized (mux) {
            for (GridPair<UUID, Long> pair : nodeLdrMap.values()) {
                ldrIds.add(pair.getValue1());
            }
        }

        return ldrIds;
    }

    /**
     * @param nodeId Node ID.
     * @return Class loader ID for node ID.
     */
    GridPair<UUID, Long> getRegisterdClassLoaderId(UUID nodeId) {
        synchronized (mux) {
            return nodeLdrMap.get(nodeId);
        }
    }

    /**
     * Checks if node is participating in deployment.
     *
     * @param nodeId Node ID to check.
     * @param ldrId Class loader ID.
     * @return <tt>True</tt> if node is participating in deployment.
     */
    boolean hasRegisteredNode(UUID nodeId, UUID ldrId) {
        assert nodeId != null : "ASSERTION [line=213, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";
        assert ldrId != null : "ASSERTION [line=214, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        GridPair<UUID, Long> pair = null;

        synchronized (mux) {
            pair = nodeLdrMap.get(nodeId);
        }

        return pair != null ? ldrId.equals(pair.getValue1()) == true : false;
    }

    /**
     * @return <tt>True</tt> if class loader has registered nodes.
     */
    boolean hasRegisteredNodes() {
        synchronized (mux) {
            return nodeLdrMap.isEmpty() == false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=239, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        // Check if we have package name on list of P2P loaded.
        // GridJob must be always loaded locally to avoid
        // any possible class casting issues.
        Class<?> cls = null;

        //noinspection CatchGenericClass
        try {
            if (p2pExclude != null && name.equals("org.gridgain.grid.GridJob") == false) {
                for (String path : p2pExclude) {
                    // Remove star (*) at the end.
                    //noinspection SingleCharacterStartsWith
                    if (path.endsWith("*") == true) {
                        path  = path.substring(0, path.length() - 1);
                    }

                    if (name.startsWith(path) == true) {
                        // P2P loaded class.
                        cls = p2pLoadClass(name, true);

                        break;
                    }
                }
            }

            if (cls == null) {
                cls = loadClass(name, true);
            }
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        // Catch Throwable to secure against any errors resulted from
        // corrupted class definitions or other user errors.
        catch (Throwable e) {
            throw new ClassNotFoundException("Failed to load class due to unexpected error: " + name, e);
        }

        return cls;
    }

    /**
     * Loads the class with the specified binary name.  The
     * default implementation of this method searches for classes in the
     * following order:
     * <p>
     * <ol>
     * <li> Invoke {@link #findLoadedClass(String)} to check if the class
     * has already been loaded. </li>
     * <li>Invoke the {@link #findClass(String)} method to find the class.</li>
     * </ol>
     * <p> If the class was found using the above steps, and the
     * <tt>resolve</tt> flag is true, this method will then invoke the {@link
     * #resolveClass(Class)} method on the resulting <tt>Class</tt> object.
     *
     * @param name The binary name of the class.
     * @param resolve If <tt>true</tt> then resolve the class.
     * @return The resulting <tt>Class</tt> object.
     * @throws ClassNotFoundException If the class could not be found
     */
    private Class<?> p2pLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=301, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        // First, check if the class has already been loaded.
        Class<?> cls = findLoadedClass(name);

        if (cls == null) {
            cls = findClass(name);
        }

        if (resolve == true) {
            resolveClass(cls);
        }

        return cls;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=322, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        String path = GridUtils.classNameToResourceName(name);

        GridByteArrayList byteSrc = sendClassRequest(name, path);

        return defineClass(name, byteSrc.getInternalArray(), 0, byteSrc.getSize());
    }

    /**
     * Computes end time based on timeout value passed in.
     *
     * @param timeout Timeout.
     * @return End time.
     */
    private long computeEndTime(long timeout) {
        long endTime = System.currentTimeMillis() + timeout;

        // Account for overflow.
        if (endTime < 0) {
            endTime = Long.MAX_VALUE;
        }

        return endTime;
    }

    /**
     * Sends class-loading request to all nodes associated with this class loader.
     *
     * @param name Class name.
     * @param path Class path.
     * @return Class byte source.
     * @throws ClassNotFoundException If class was not found.
     */
    @SuppressWarnings({"CallToNativeMethodWhileLocked"})
    private GridByteArrayList sendClassRequest(String name, String path) throws ClassNotFoundException {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=358, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        long endTime = computeEndTime(p2pTimeout);

        GridDeploymentResponse res = null;

        Collection<Map.Entry<UUID, GridPair<UUID, Long>>> entries = null;

        synchronized (mux) {
            // Skip requests for the previously missed classes.
            if (missedRsrcs != null && missedRsrcs.contains(path) == true) {
                //noinspection ArithmeticOnVolatileField
                throw new ClassNotFoundException("Failed to peer load class [class=" + name + ", nodeClsLdrIds=" +
                    nodeLdrMap + ", parentClsLoader=" + getParent() + ']');
            }

            // If single-node mode, then node cannot change and we simnply reuse the entry set.
            // Otherwise, copy and preserve order for iteration.
            entries = singleNode == true ? nodeLdrMap.entrySet() :
                new ArrayList<Map.Entry<UUID, GridPair<UUID, Long>>>(nodeLdrMap.entrySet());
        }

        GridException err = null;

        for (Map.Entry<UUID, GridPair<UUID, Long>> entry: entries) {
            UUID nodeId = entry.getKey();

            if (nodeId.equals(mgrReg.getDiscoveryManager().getLocalNode().getId()) == true) {
                // Skip local node as it is already used as parent class loader.
                continue;
            }

            UUID ldrId = entry.getValue().getValue1();

            GridNode node = mgrReg.getDiscoveryManager().getNode(nodeId);

            if (node == null) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Found inactive node is class loader (will skip): " + nodeId);
                }

                continue;
            }

            try {
                res = comm.sendResourceRequest(path, ldrId, node, endTime);

                if (res == null) {
                    String msg = "Failed to send class-loading node request to node (is node alive?) [node=" +
                        node.getId() + ", clsName=" + name + ", clsPath=" + path + ", clsLdrId=" + ldrId +
                        ", parentClsLdr=" + getParent() + ']';

                    log.warning(msg);

                    err = (GridException)new GridException(msg).setData(412, "src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java");

                    continue;
                }

                if (res.isSuccess() == true) {
                    return res.getByteSource();
                }

                // In case of shared resources/classes all nodes should have it.
                log.warning("Failed to find class on remote node [class=" + name + ", nodeId=" + node.getId() +
                    ", clsLdrId=" + entry.getValue());

                synchronized (mux) {
                    if (missedRsrcs != null) {
                        missedRsrcs.add(path);
                    }
                }

                throw new ClassNotFoundException("Failed to peer load class [class=" + name + ", nodeClsLdrs=" +
                    entries + ", parentClsLoader=" + getParent() + ']');
            }
            catch (GridException e) {
                // This thread should be interrupted again in communication if it
                // got interrupted. So we assume that thread can be interrupted
                // by processing cancellation request.
                if (Thread.currentThread().isInterrupted() == true) {
                    log.error("Failed to find class probably due to task/job cancellation: " + name, e);
                }
                else {
                    log.warning("Failed to send class-loading node request to node (is node alive?) [node=" +
                        node.getId() + ", clsName=" + name + ", clsPath=" + path + ", clsLdrId=" + ldrId +
                        ", parentClsLdr=" + getParent() + ", err=" + e + ']');

                    err = e;
                }
            }
        }

        //noinspection ArithmeticOnVolatileField
        throw new ClassNotFoundException("Failed to peer load class [class=" + name + ", nodeClsLdrs=" +
            entries + ", parentClsLoader=" + getParent() + ']', err);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=461, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        InputStream in = ClassLoader.getSystemResourceAsStream(name);

        if (in == null) {
            in = super.getResourceAsStream(name);
        }

        if (in == null) {
            in = sendResourceRequest(name);
        }

        return in;
    }

    /**
     * Sends resource request to all remote nodes associated with this class loader.
     *
     * @param name Resource name.
     * @return InputStream for resource or <tt>null</tt> if resource could not be found.
     */
    @SuppressWarnings({"CallToNativeMethodWhileLocked"})
    private InputStream sendResourceRequest(String name) {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=484, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClassLoader.java]";

        InputStream in = null;

        long endTime = computeEndTime(p2pTimeout);

        Collection<Map.Entry<UUID, GridPair<UUID, Long>>> entries = null;

        synchronized (mux) {
            // Skip requests for the previously missed classes.
            if (missedRsrcs != null && missedRsrcs.contains(name) == true) {
                return null;
            }

            // If single-node mode, then node cannot change and we simnply reuse the entry set.
            // Otherwise, copy and preserve order for iteration.
            entries = singleNode == true ? nodeLdrMap.entrySet() :
                new ArrayList<Map.Entry<UUID, GridPair<UUID, Long>>>(nodeLdrMap.entrySet());
        }

        for (Map.Entry<UUID, GridPair<UUID, Long>> entry: entries) {
            UUID nodeId = entry.getKey();

            if (nodeId.equals(mgrReg.getDiscoveryManager().getLocalNode().getId()) == true) {
                // Skip local node as it is already used as parent class loader.
                continue;
            }

            UUID ldrId = entry.getValue().getValue1();

            GridNode node = mgrReg.getDiscoveryManager().getNode(nodeId);

            if (node == null) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Found inactive node is class loader (will skip): " + nodeId);
                }

                continue;
            }

            try {
                // Request is sent with timeout that is why we can use synchronization here.
                GridDeploymentResponse res = comm.sendResourceRequest(name, ldrId, node, endTime);

                if (res == null) {
                    log.warning("Failed to get resource from node (is node alive?) [nodeId=" +
                        node.getId() + ", clsLdrId=" + entry.getValue() + ", resName=" +
                        name + ", parentClsLdr=" + getParent() + ']');
                }
                else if (res.isSuccess() == false) {
                    synchronized (mux) {
                        // Cache unsuccessfully loaded resource.
                        if (missedRsrcs != null) {
                            missedRsrcs.add(name);
                        }
                    }

                    // Some frameworks like Spring often ask for the resources
                    // just in case - nothing will happen if there are no such
                    // resources. So we print out INFO level message.
                    if (log.isInfoEnabled() == true) {
                        log.info("Failed to get resource from node [nodeId=" +
                            node.getId() + ", clsLdrId=" + entry.getValue() + ", resName=" +
                            name + ", parentClsLdr=" + getParent() + ", msg=" + res.getErrorMessage() + ']');
                    }

                    // Do not ask other nodes in case of shared mode all of them should have the resource.
                    return null;
                }
                else {
                    return new ByteArrayInputStream(res.getByteSource().getInternalArray(), 0,
                        res.getByteSource().getSize());
                }
            }
            catch (GridException e) {
                // This thread should be interrupted again in communication if it
                // got interrupted. So we assume that thread can be interrupted
                // by processing cancellation request.
                if (Thread.currentThread().isInterrupted() == true) {
                    log.error("Failed to get resource probably due to task/job cancellation: " + name, e);
                }
                else {
                    log.warning("Failed to get resource from node (is node alive?) [nodeId=" +
                        node.getId() + ", clsLdrId=" + entry.getValue() + ", resName=" +
                        name + ", parentClsLdr=" + getParent() + ", err=" + e + ']');
                }
            }
        }

        return in;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentClassLoader.class, this);
    }
}
