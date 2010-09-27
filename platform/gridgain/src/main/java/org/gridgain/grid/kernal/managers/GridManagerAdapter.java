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

package org.gridgain.grid.kernal.managers;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.*;
import static org.gridgain.grid.kernal.managers.communication.GridCommunicationThreadPolicy.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Convenience adapter for grid managers.
 *
 * @param <S> SPI wrapped by this manager.
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public abstract class GridManagerAdapter<S extends GridSpi> implements GridManager {
    /** System line separator. */
    protected static final String NL = System.getProperty("line.separator");

    /** */
    @GridToStringExclude
    protected final GridManagerRegistry mgrReg;

    /** */
    @GridToStringExclude
    protected final GridProcessorRegistry procReg;

    /** */
    @GridToStringExclude
    protected final GridConfiguration cfg;

    /** */
    @GridToStringExclude
    protected final GridLogger log;

    /** */
    @GridToStringExclude
    private final S[] spi;

    /** */
    @GridToStringExclude
    private final S[] proxy;

    /** Context read-write lock. */
    @GridToStringExclude
    private final ReadWriteLock ctxRwLock = new ReentrantReadWriteLock();

    /**
     *
     * @param spi Specific SPI instance.
     * @param itf SPI interface.
     * @param procReg Resources context for injection.
     * @param mgrReg Managers registry.
     * @param cfg Grid configuration.
     */
    @SuppressWarnings("unchecked")
    protected GridManagerAdapter(Class<S> itf, GridConfiguration cfg, GridProcessorRegistry procReg,
        GridManagerRegistry mgrReg, S... spi) {
        assert spi != null : "ASSERTION [line=87, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";
        assert spi.length > 0 : "ASSERTION [line=88, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";
        assert procReg != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";
        assert mgrReg != null : "ASSERTION [line=90, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";
        assert cfg != null : "ASSERTION [line=91, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";

        this.spi = spi;

        proxy = (S[])Array.newInstance(itf, spi.length);

        for (int i = 0; i < spi.length; i++) {
            final GridSpi unwrappedSpi = this.spi[i];

            // Create proxy SPI to wrap all calls into context real lock.
            // This is done to avoid context destruction during any of the
            // public SPI method invocations.
            proxy[i] = (S)Proxy.newProxyInstance(spi.getClass().getClassLoader(), new Class[] { itf },
                new InvocationHandler() {
                    /**
                     * {@inheritDoc}
                     */
                    @SuppressWarnings({"ProhibitedExceptionDeclared", "ProhibitedExceptionThrown"})
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // Wrap all SPI invocations inside of read-lock to
                        // prevent context destruction during SPI invocations.
                        ctxRwLock.readLock().lock();

                        try {
                            return method.invoke(unwrappedSpi, args);
                        }
                        catch (InvocationTargetException e) {
                            if (e.getCause() != null) {
                                throw e.getCause();
                            }

                            throw e;
                        }
                        finally {
                            ctxRwLock.readLock().unlock();
                        }
                    }
            });
        }

        this.cfg = cfg;
        this.mgrReg = mgrReg;
        this.procReg = procReg;

        log = cfg.getGridLogger().getLogger(getClass());
    }

    /**
     * Gets wrapped SPI.
     *
     * @return Wrapped SPI.
     */
    protected final S getSpi() {
        return proxy[0];
    }

    /**
     *
     * @param name SPI name
     * @return SPI for given name. If <tt>null</tt> or empty, then 1st SPI on the list
     *      is returned.
     */
    protected final S getSpi(String name) {
        if (name == null || name.length() == 0) {
            return proxy[0];
        }

        // Loop through SPI's, not proxies, because
        // proxy.getName() is more expensive than spi.getName().
        for (int i = 0; i < spi.length; i++) {
            S s = spi[i];

            if (s.getName().equals(name) == true) {
                return proxy[i];
            }
        }

        throw (GridRuntimeException)new GridRuntimeException("Failed to find SPI for name: " + name).setData(168, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
    }

    /**
     *
     * @return Configured SPI's.
     */
    protected final S[] getSpis() {
        return spi;
    }

    /**
     * {@inheritDoc}
     */
    public final void addSpiAttributes(Map<String, Serializable> attrs) throws GridException {
        for (S spi : this.spi) {
            // Inject all spi resources.
            procReg.getResourceProcessor().inject(spi);

            try {
                Map<String, Serializable> retval = spi.getNodeAttributes();

                if (retval != null) {
                    for (Map.Entry<String, Serializable> e : retval.entrySet()) {
                        if (attrs.containsKey(e.getKey()) == true) {
                            throw (GridException)new GridException("SPI attribute collision for attribute [spi=" + spi +
                                ", attr=" + e.getKey() + ']' +
                                ". Attribute set by one SPI implementation has the same name (name collision) as " +
                                "attribute set by other SPI implementation. Such overriding is not allowed. " +
                                "Please check your GridGain configuration and/or SPI implementation to avoid " +
                                "attribute name colissions.").setData(193, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
                        }

                        attrs.put(e.getKey(), e.getValue());
                    }
                }
            }
            catch (GridSpiException e) {
                throw (GridException)new GridException("Failed to get SPI attributes.", e).setData(206, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
            }
        }
    }

    /**
     * Starts wrapped SPI.
     *
     * @throws GridException If wrapped SPI could not be started.
     */
    protected final void startSpi() throws GridException {
        assert spi != null : "ASSERTION [line=217, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";
        assert proxy != null : "ASSERTION [line=218, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";

        Set<String> names = new HashSet<String>(spi.length);

        for (int i = 0; i < spi.length; i++) {
            GridSpi spi = this.spi[i];

            // Print-out all SPI parameters only in DEBUG mode.
            if (log.isDebugEnabled() == true) {
                log.debug("Starting SPI: " + spi);
            }

            GridSpiInfo info = spi.getClass().getAnnotation(GridSpiInfo.class);

            if (info == null) {
                throw (GridException)new GridException("SPI implementation does not have @GridSpiInfo annotation: " + spi.getClass()).setData(233, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
            }

            if (names.contains(spi.getName()) == true) {
                throw (GridException)new GridException("Duplicate SPI name (need to explicitly configure 'setName()' property): " +
                    spi.getName()).setData(237, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
            }

            names.add(spi.getName());

            // Only log SPI name in INFO mode.
            if (log.isInfoEnabled() == true) {
                log.info("Starting SPI implementation: " + spi.getClass().getName());
            }

            try {
                proxy[i].spiStart(cfg.getGridName());
            }
            catch (GridSpiException e) {
                throw (GridException)new GridException("Failed to start SPI: " + spi, e).setData(252, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
            }

            if (log.isDebugEnabled() == true) {
                log.debug("SPI module started ok [spi=" + spi.getClass().getName() + ", author=" + info.author() +
                    ", version=" + info.version() + ", email=" + info.email() + ", url=" +info.url() + ']');
            }
        }
    }

    /**
     * Stops wrapped SPI.
     *
     * @throws GridException If underlying SPI could not be stopped.
     */
    protected final void stopSpi() throws GridException {
        for (int i = 0; i < spi.length; i++) {
            GridSpi spi = this.spi[i];

            if (log.isDebugEnabled() == true) {
                log.debug("Stopping SPI: " + spi);
            }

            try {
                proxy[i].spiStop();

                GridSpiInfo info = spi.getClass().getAnnotation(GridSpiInfo.class);

                assert info != null : "ASSERTION [line=280, file=src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java]";

                if (log.isDebugEnabled() == true) {
                    log.debug("SPI module stopped ok [spi=" + spi.getClass().getName() +
                        ", author=" + info.author() + ", version=" + info.version() +
                        ", email=" + info.email() + ", url=" + info.url() + ']');
                }
            }
            catch (GridSpiException e) {
                throw (GridException)new GridException("Failed to stop SPI: " + spi, e).setData(289, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
            }

            try {
                procReg.getResourceProcessor().cleanup(spi);
            }
            catch (GridException e) {
                log.error("Failed to remove injected resources from SPI (ignoring): " + spi, e);
            }
        }
    }

    /**
     *
     * @return Uniformly formatted ack string.
     */
    protected final String startInfo() {
        return "Manager started ok: " + getClass().getName();
    }

    /**
     *
     * @return Uniformly formatted ack string.
     */
    protected final String stopInfo() {
        return "Manager stopped ok: " + getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    public void onKernalStart() throws GridException {
        for (GridSpi spi : this.spi) {
            ctxRwLock.writeLock().lock();

            try {
                spi.onContextInitialized(new GridSpiContext() {
                    /**
                     * {@inheritDoc}
                     */
                    public Collection<GridNode> getRemoteNodes() {
                        return mgrReg.getDiscoveryManager().getRemoteNodes();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Collection<GridNode> getAllNodes() {
                        return mgrReg.getDiscoveryManager().getAllNodes();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public GridNode getLocalNode() {
                        return mgrReg.getDiscoveryManager().getLocalNode();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public GridNode getNode(UUID nodeId) {
                        GridArgumentCheck.checkNull(nodeId, "nodeId");

                        return mgrReg.getDiscoveryManager().getNode(nodeId);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean pingNode(UUID nodeId) {
                        GridArgumentCheck.checkNull(nodeId, "nodeId");

                        return mgrReg.getDiscoveryManager().pingNode(nodeId);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void addDiscoveryListener(GridDiscoveryListener listener) {
                        GridArgumentCheck.checkNull(listener, "listener");

                        mgrReg.getDiscoveryManager().addDiscoveryListener(listener);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean removeDiscoveryListener(GridDiscoveryListener listener) {
                        GridArgumentCheck.checkNull(listener, "listener");

                        return mgrReg.getDiscoveryManager().removeDiscoveryListener(listener);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void sendMessage(GridNode node, Serializable msg, String topic) throws GridSpiException {
                        GridArgumentCheck.checkNull(node, "node");
                        GridArgumentCheck.checkNull(msg, "msg");
                        GridArgumentCheck.checkNull(topic, "topic");

                        try {
                            mgrReg.getCommunicationManager().sendMessage(node, topic, msg, POOLED_THREAD);
                        }
                        catch (GridException e) {
                            throw unwrapException(e);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void sendMessage(Collection<GridNode> nodes, Serializable msg, String topic)
                        throws GridSpiException {
                        GridArgumentCheck.checkNull(nodes, "nodes");
                        GridArgumentCheck.checkNull(msg, "msg");
                        GridArgumentCheck.checkNull(topic, "topic");

                        try {
                            mgrReg.getCommunicationManager().sendMessage(nodes, topic, msg, POOLED_THREAD);
                        }
                        catch (GridException e) {
                            throw unwrapException(e);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void addMessageListener(GridMessageListener listener, String topic) {
                        GridArgumentCheck.checkNull(listener, "listener");
                        GridArgumentCheck.checkNull(topic, "topic");

                        mgrReg.getCommunicationManager().addMessageListener(topic, listener);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean removeMessageListener(GridMessageListener listener, String topic) {
                        GridArgumentCheck.checkNull(listener, "listener");
                        GridArgumentCheck.checkNull(topic, "topic");

                        return mgrReg.getCommunicationManager().removeMessageListener(topic, listener);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void addLocalEventListener(GridLocalEventListener listener) {
                        GridArgumentCheck.checkNull(listener, "listener");

                        mgrReg.getEventStorageManager().addGridLocalEvenListener(listener);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean removeLocalEventListener(GridLocalEventListener listener) {
                        GridArgumentCheck.checkNull(listener, "listener");

                        return mgrReg.getEventStorageManager().removeGridLocalEventListener(listener);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Collection<GridNode> getTopology(GridTaskSession taskSes, Collection<GridNode> grid)
                        throws GridSpiException {
                        try {
                            return mgrReg.getTopologyManager().getTopology((GridTaskSessionImpl)taskSes, grid);
                        }
                        catch (GridException e) {
                            throw unwrapException(e);
                        }
                    }

                    /**
                     *
                     * @param e Exception to handle.
                     * @return GridSpiException Converted exception.
                     */
                    private GridSpiException unwrapException(GridException e) {
                        // Avoid double-wrapping.
                        if (e.getCause() instanceof GridSpiException) {
                            return (GridSpiException)e.getCause();
                        }

                        return (GridSpiException)new GridSpiException("Failed to execute SPI context method.", e).setData(478, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
                    }
                });
            }
            catch (GridSpiException e) {
                throw (GridException)new GridException("Failed to initialize SPI context.", e).setData(483, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
            }
            finally {
                ctxRwLock.writeLock().unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onKernalStop() {
        for (GridSpi spi : this.spi) {
            ctxRwLock.writeLock().lock();

            try {
                spi.onContextDestroyed();
            }
            finally {
                ctxRwLock.writeLock().unlock();
            }
        }
    }

    /**
     * Throws exception with uniform error message if given parameter's assertion condition
     * is <tt>false</tt>.
     *
     * @param cond Assertion condition to check.
     * @param condDescr Description of failed condition. Note that this description should include
     *      JavaBean name of the property (<b>not</b> a variable name) as well condition in
     *      Java syntax like, for example:
     *      <pre name="code" class="java">
     *      assertParameter(dirPath != null, "dirPath != null");
     *      </pre>
     *      Note that in case when variable name is the same as JavaBean property you
     *      can just copy Java condition expression into description as a string.
     * @throws GridException Thrown if given condition is <tt>false</tt>
     */
    protected final void assertParameter(boolean cond, String condDescr) throws GridException {
        if (cond == false) {
            throw (GridException)new GridException("Grid configuration parameter failed condition check: " + condDescr).setData(524, "src/java/org/gridgain/grid/kernal/managers/GridManagerAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return GridToStringBuilder.toString(GridManagerAdapter.class, this, "name", getClass().getName());
    }
}
