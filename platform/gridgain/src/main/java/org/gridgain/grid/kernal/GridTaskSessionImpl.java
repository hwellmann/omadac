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

package org.gridgain.grid.kernal;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridTaskSessionImpl implements GridTaskSession {
    /** */
    private final String taskName;

    /** */
    private final String userVer;

    /** */
    private final String taskClsName;

    /** */
    private final UUID sesId;

    /** */
    private final UUID jobId;

    /** */
    private final long endTime;

    /** */
    private final UUID taskNodeId;

    /** */
    private final GridProcessorRegistry procReg;

    /** */
    private final GridManagerRegistry mgrReg;

    /** */
    private Collection<GridJobSibling> siblings = null;

    /** */
    private final Map<Serializable, Serializable> attrs = new HashMap<Serializable, Serializable>(1);

    /** */
    private List<GridTaskSessionAttributeListener> listeners = Collections.emptyList();

    /** */
    private ClassLoader clsLdr = null;

    /** */
    private boolean closed = false;

    /** */
    private String topSpi = null;

    /** */
    private String cpSpi = null;

    /** */
    private String failSpi = null;

    /** */
    private String loadSpi = null;

    /** */
    private long seqNum = 0;

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param taskNodeId Task node ID.
     * @param taskName Task name.
     * @param userVer Task code version. Might be null if deployment failed.
     * @param seqNum Task internal node version.\
     * @param taskClsName Task class name.
     * @param sesId Task session ID.
     * @param jobId Job ID.
     * @param endTime Task execution end time.
     * @param siblings Collection of siblings.
     * @param attrs Session attributes.
     * @param procReg Processor registry.
     * @param mgrReg Managers registry.
     */
    public GridTaskSessionImpl(
        UUID taskNodeId,
        String taskName,
        String userVer,
        Long seqNum,
        String taskClsName,
        UUID sesId,
        UUID jobId,
        long endTime,
        Collection<GridJobSibling> siblings,
        Map<? extends Serializable, ? extends Serializable> attrs, GridProcessorRegistry procReg,
        GridManagerRegistry mgrReg) {
        assert taskNodeId != null : "ASSERTION [line=126, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";
        assert taskName != null : "ASSERTION [line=127, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";
        assert sesId != null : "ASSERTION [line=128, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";
        assert siblings != null : "ASSERTION [line=129, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";
        assert procReg != null : "ASSERTION [line=130, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";
        assert attrs != null : "ASSERTION [line=131, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";
        assert mgrReg != null : "ASSERTION [line=132, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";
        assert seqNum != null : "ASSERTION [line=133, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";

        this.taskNodeId = taskNodeId;
        this.taskName = taskName;
        this.userVer = userVer;
        this.seqNum = seqNum;

        // Note that class name might be null here if task was not explicitely
        // deployed.
        this.taskClsName = taskClsName;
        this.sesId = sesId;
        this.jobId = jobId;
        this.endTime = endTime;
        this.siblings = Collections.unmodifiableCollection(siblings);
        this.procReg = procReg;
        this.mgrReg = mgrReg;

        this.attrs.putAll(attrs);
    }

    /**
     *
     */
    public void onClosed() {
        synchronized (mux) {
            closed = true;

            mux.notifyAll();
        }
    }

    /**
     *
     * @return <tt>True</tt> if session is closed.
     */
    public boolean isClosed() {
        synchronized (mux) {
            return closed;
        }
    }

    /**
     *
     * @return Task node ID.
     */
    public UUID getTaskNodeId() {
        return taskNodeId;
    }

    /**
     * {@inheritDoc}
     * @param key FIXDOC
     */
    public Serializable waitForAttribute(Serializable key) throws InterruptedException {
        return waitForAttribute(key, 0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean waitForAttribute(Serializable key, Serializable val) throws InterruptedException {
        return waitForAttribute(key, val, 0);
    }

    /**
     * {@inheritDoc}
     */
    public Serializable waitForAttribute(Serializable key, long timeout) throws InterruptedException {
        GridArgumentCheck.checkNull(key, "key");

        if (timeout == 0) {
            timeout = Long.MAX_VALUE;
        }

        long now = System.currentTimeMillis();

        // Prevent overflow.
        long end = now + timeout < 0 ? Long.MAX_VALUE : now + timeout;

        // Don't wait longer than session timeout.
        if (end > endTime) {
            end = endTime;
        }

        synchronized (mux) {
            while (closed == false && attrs.containsKey(key) == false && now < end) {
                mux.wait(end - now);

                now = System.currentTimeMillis();
            }

            if (closed == true) {
                throw new InterruptedException("Session was closed: " + this);
            }

            return attrs.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean waitForAttribute(Serializable key, Serializable val, long timeout) throws InterruptedException {
        GridArgumentCheck.checkNull(key, "key");

        if (timeout == 0) {
            timeout = Long.MAX_VALUE;
        }

        long now = System.currentTimeMillis();

        // Prevent overflow.
        long end = now + timeout < 0 ? Long.MAX_VALUE : now + timeout;

        // Don't wait longer than session timeout.
        if (end > endTime) {
            end = endTime;
        }

        synchronized (mux) {
            boolean isFound = false;

            while (closed == false && (isFound = isAttributeSet(key, val)) == false && now < end) {
                mux.wait(end - now);

                now = System.currentTimeMillis();
            }

            if (closed == true) {
                throw new InterruptedException("Session was closed: " + this);
            }

            return isFound;
        }
    }

    /**
     * {@inheritDoc}
     * @param keys FIXDOC
     */
    public Map<? extends Serializable, ? extends Serializable> waitForAttributes(Collection<? extends Serializable>
        keys) throws InterruptedException {
        return waitForAttributes(keys, 0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean waitForAttributes(Map<? extends Serializable, ? extends Serializable> attrs)
        throws InterruptedException {
        return waitForAttributes(attrs, 0);
    }

    /**
     * {@inheritDoc}
     */
    public Map<? extends Serializable, ? extends Serializable> waitForAttributes(
        Collection<? extends Serializable> keys, long timeout) throws InterruptedException {
        GridArgumentCheck.checkNull(keys, "keys");

        if (keys.isEmpty() == true) {
            return Collections.emptyMap();
        }

        if (timeout == 0) {
            timeout = Long.MAX_VALUE;
        }

        long now = System.currentTimeMillis();

        // Prevent overflow.
        long end = now + timeout < 0 ? Long.MAX_VALUE : now + timeout;

        // Don't wait longer than session timeout.
        if (end > endTime) {
            end = endTime;
        }

        synchronized (mux) {
            while (closed == false && attrs.keySet().containsAll(keys) == false && now < end) {
                mux.wait(end - now);

                now = System.currentTimeMillis();
            }

            if (closed == true) {
                throw new InterruptedException("Session was closed: " + this);
            }

            Map<Serializable, Serializable> retVal = new HashMap<Serializable, Serializable>(keys.size());

            for (Serializable key : keys) {
                retVal.put(key, attrs.get(key));
            }

            return retVal;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean waitForAttributes(Map<? extends Serializable, ? extends Serializable> attrs, long timeout)
        throws InterruptedException {
        GridArgumentCheck.checkNull(attrs, "attrs");

        if (attrs.isEmpty() == true) {
            return true;
        }

        if (timeout == 0) {
            timeout = Long.MAX_VALUE;
        }

        long now = System.currentTimeMillis();

        // Prevent overflow.
        long end = now + timeout < 0 ? Long.MAX_VALUE : now + timeout;

        // Don't wait longer than session timeout.
        if (end > endTime) {
            end = endTime;
        }

        synchronized (mux) {
            boolean isFound = false;

            while (closed == false && (isFound = this.attrs.entrySet().containsAll(attrs.entrySet())) == false &&
                now < end) {
                mux.wait(end - now);

                now = System.currentTimeMillis();
            }

            if (closed == true) {
                throw new InterruptedException("Session was closed: " + this);
            }

            return isFound;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Returns task class name.
     *
     * @return Task class name.
     */
    public String getTaskClassName() {
        return taskClsName;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getId() {
        return sesId;
    }

    /**
     *
     * @return Job ID.
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * {@inheritDoc}
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     *
     * @return Task version.
     */
    public String getUserVersion() {
        return userVer;
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader() {
        synchronized (mux) {
            return clsLdr;
        }
    }

    /**
     *
     * @param clsLdr FIXDOC
     */
    public void setClassLoader(ClassLoader clsLdr) {
        assert clsLdr != null : "ASSERTION [line=435, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";

        synchronized (mux) {
            this.clsLdr = clsLdr;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridJobSibling> getJobSiblings() {
        synchronized (mux) {
            return  siblings;
        }
    }

    /**
     *
     * @param siblings FIXDOC
     */
    public void setJobSiblings(Collection<GridJobSibling> siblings) {
        synchronized (mux) {
            this.siblings = Collections.unmodifiableCollection(siblings);
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridJobSibling getJobSibling(UUID jobId) {
        GridArgumentCheck.checkNull(jobId, "jobId");

        synchronized (mux) {
            for (GridJobSibling sibling : siblings) {
                if (sibling.getJobId().equals(jobId) == true) {
                    return sibling;
                }
            }

            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(Serializable key, Serializable val) throws GridException {
        GridArgumentCheck.checkNull(key, "key");

        setAttributes(Collections.singletonMap(key, val));
    }

    /**
     * {@inheritDoc}
     * @param key FIXDOC
     */
    public Serializable getAttribute(Serializable key) {
        GridArgumentCheck.checkNull(key, "key");

        synchronized (mux) {
            return attrs.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAttributes(Map<? extends Serializable, ? extends Serializable> attrs) throws GridException {
        GridArgumentCheck.checkNull(attrs, "attrs");

        if (attrs.isEmpty() == true) {
            return;
        }

        // Note that there is no mux notification in this block.
        // The reason is that we wait for ordered attributes to
        // come back from task prior to notification. The notification
        // will happen in 'setInternal(...)' method.
        synchronized (mux) {
            this.attrs.putAll(attrs);
        }

        if (jobId == null) {
            procReg.getTaskProcessor().setAttributes(this, attrs);
        }
        else {
            procReg.getJobProcessor().setAttributes(this, attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<? extends Serializable, ? extends Serializable> getAttributes() {
        synchronized (mux) {
            return Collections.unmodifiableMap(new HashMap<Serializable, Serializable>(attrs));
        }
    }

    /**
     *
     * @param attrs Attributes to set.
     */
    public void setInternal(Map<? extends Serializable, ? extends Serializable> attrs) {
        GridArgumentCheck.checkNull(attrs, "attrs");

        if (attrs.isEmpty() == true) {
            return;
        }

        List<GridTaskSessionAttributeListener> listeners;

        synchronized (mux) {
            this.attrs.putAll(attrs);

            listeners = this.listeners;

            mux.notifyAll();
        }

        for (Map.Entry<? extends Serializable, ? extends Serializable> entry : attrs.entrySet()) {
            for (GridTaskSessionAttributeListener listener : listeners) {
                listener.onAttributeSet(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addAttributeListener(GridTaskSessionAttributeListener listener, boolean rewind) {
        GridArgumentCheck.checkNull(listener, "listener");

        Map<Serializable, Serializable> attrs = null;

        List<GridTaskSessionAttributeListener> listeners = null;

        synchronized (mux) {
            listeners = new ArrayList<GridTaskSessionAttributeListener>(this.listeners.size());

            listeners.addAll(this.listeners);

            listeners.add(listener);

            listeners = Collections.unmodifiableList(listeners);

            this.listeners = listeners;

            if (rewind == true) {
                attrs = new HashMap<Serializable, Serializable>(this.attrs);
            }
        }

        if (rewind == true) {
            //noinspection ConstantConditions
            assert attrs != null : "ASSERTION [line=590, file=src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java]";

            for (Map.Entry<Serializable, Serializable> entry : attrs.entrySet()) {
                for (GridTaskSessionAttributeListener l : listeners) {
                    l.onAttributeSet(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeAttributeListener(GridTaskSessionAttributeListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        synchronized (mux) {
            List<GridTaskSessionAttributeListener> listeners =
                new ArrayList<GridTaskSessionAttributeListener>(this.listeners);

            boolean removed = listeners.remove(listener);

            this.listeners = Collections.unmodifiableList(listeners);

            return removed;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void saveCheckpoint(String key, Serializable state) throws GridException {
        saveCheckpoint(key, state, GridCheckpointScope.SESSION_SCOPE, 0);
    }

    /**
     * {@inheritDoc}
     */
    public void saveCheckpoint(String key, Serializable state, GridCheckpointScope scope, long timeout)
        throws GridException {
        GridArgumentCheck.checkNull(key, "key");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        synchronized (mux) {
            if (closed == true) {
                throw (GridException)new GridException("Failed to save checkpoint (session closed).").setData(635, "src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java");
            }
        }

        mgrReg.getCheckpointManager().storeCheckpoint(this, key, state, scope, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public Serializable loadCheckpoint(String key) throws GridException {
        GridArgumentCheck.checkNull(key, "key");

        synchronized (mux) {
            if (closed == true) {
                throw (GridException)new GridException("Failed to load checkpoint (session closed).").setData(650, "src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java");
            }
        }

        return mgrReg.getCheckpointManager().loadCheckpoint(this, key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCheckpoint(String key) throws GridException {
        GridArgumentCheck.checkNull(key, "key");

        synchronized (mux) {
            if (closed == true) {
                throw (GridException)new GridException("Failed to remove checkpoint (session closed).").setData(665, "src/java/org/gridgain/grid/kernal/GridTaskSessionImpl.java");
            }
        }

        return mgrReg.getCheckpointManager().removeCheckpoint(this, key);
    }

    /**
     *
     * @param key FIXDOC
     * @param val FIXDOC
     * @return <tt>true</tt> if key/value pair was set.
     */
    private boolean isAttributeSet(Serializable key, Serializable val) {
        if (attrs.containsKey(key) == true) {
            Serializable stored = attrs.get(key);

            if (val == null && stored == null) {
                return true;
            }

            if (val != null && stored != null) {
                return val.equals(stored) == true;
            }
        }

        return false;
    }

    /**
     *
     * @return Topology SPI name.
     */
    public String getTopologySpi() {
        return topSpi;
    }

    /**
     *
     * @param topSpi Topology SPI name.
     */
    public void setTopologySpi(String topSpi) {
        this.topSpi = topSpi;
    }

    /**
     *
     * @return Checkpoint SPI name.
     */
    public String getCheckpointSpi() {
        return cpSpi;
    }

    /**
     *
     * @param cpSpi Checkpoint SPI name.
     */
    public void setCheckpointSpi(String cpSpi) {
        this.cpSpi = cpSpi;
    }

    /**
     *
     * @return Failover SPI name.
     */
    public String getFailoverSpi() {
        return failSpi;
    }

    /**
     *
     * @param failSpi Failover SPI name.
     */
    public void setFailoverSpi(String failSpi) {
        this.failSpi = failSpi;
    }

    /**
     *
     * @return Load balancing SPI name.
     */
    public String getLoadBalancingSpi() {
        return loadSpi;
    }

    /**
     *
     * @param loadSpi Load balancing SPI name.
     */
    public void setLoadBalancingSpi(String loadSpi) {
        this.loadSpi = loadSpi;
    }

    /**
     * @return Task internal version.
     */
    public long getSequenceNumber() {
        return seqNum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridTaskSessionImpl.class, this);
    }
}
