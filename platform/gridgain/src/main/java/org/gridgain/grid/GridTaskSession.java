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

package org.gridgain.grid;

import java.io.*;
import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.jsr305.*;

/**
 * Defines a distributed session for particular task execution.
 * <h1 class="header">Description</h1>
 * This interface defines a distributed session that exists for particular task
 * execution. Task session is distributed across the parent task and all grid
 * jobs spawned by it, so attributes set on a task or on a job can be viewed on
 * other jobs. Correspondingly attributes set on any of the jobs can also be
 * viewed on a task.
 * <p>
 * Session has 2 main features: <tt>attribute</tt> and <tt>checkpoint</tt>
 * management. Both attributes and checkpoints can be used from task itself and
 * from the jobs belonging to this task. Session attributes and checkpoints can
 * be set from any task or job methods. Session attribute and checkpoint consistency
 * is fault tolerant and is preserved whenever a job gets failed over to
 * another node for execution. Whenever task execution ends, all checkpoints
 * saved within session with {@link GridCheckpointScope#SESSION_SCOPE} scope
 * will be removed from checkpoint storage. Checkpoints saved with
 * {@link GridCheckpointScope#GLOBAL_SCOPE} will outlive the session and
 * can be viewed by other tasks.
 * <p>
 * The sequence in which session attributes are set is consistent across
 * the task and all job siblings within it. There will never be a
 * case when one job sees attribute A before attribute B, and another job sees
 * attribute B before A. Attribute order is identical across all session
 * participants. Attribute order is also fault tolerant and is preserved
 * whenever a job gets failed over to another node.
 * <p>
 * <h1 class="header">Connected Tasks</h1>
 * Note that apart from setting and getting session attributes, tasks or
 * jobs can choose to wait for a certain attribute to be set using any of
 * the <tt>waitForAttribute(...)</tt> methods. Tasks and jobs can also
 * receive asynchronous notifications about a certain attribute being set
 * through {@link GridTaskSessionAttributeListener} listener. Such feature
 * allows grid jobs and tasks remain <u><i>connected</i></u> in order
 * to synchronize their execution with each other and opens a solution for a
 * whole new range of problems.
 * <p>
 * Imagine for example that you need to compress a very large file (let's say
 * terabytes in size). To do that in grid environment you would split such
 * file into multiple sections and assign every section to a remote job for
 * execution. Every job would have to scan its section to look for repetition
 * patterns. Once this scan is done by all jobs in parallel, jobs would need to
 * synchronize their results with their siblings so compression would happen
 * consistently across the whole file. This can be achieved by setting
 * repetition patterns discovered by every job into the session. Once all
 * patterns are synchronized, all jobs can proceed with compressing their
 * designated file sections in parallel, taking into account repetition patterns
 * found by all the jobs in the split. Grid task would then reduce (aggregate)
 * all compressed sections into one compressed file. Without session attribute
 * synchronization step this problem would be much harder to solve.
 * <p>
 * <h1 class="header">Session Injection</h1>
 * Session can be injected into a task or a job using IoC (dependency
 * injection) by attaching {@link GridTaskSessionResource @GridTaskSessionResource}
 * annotation to a field or a setter method inside of {@link GridTask} or
 * {@link GridJob} implementations as follows:
 * <pre name="code" class="java">
 * ...
 * // This field will be injected with distributed task session.
 * &#64GridTaskSessionResource
 * private GridTaskSession ses = null;
 * ...
 * </pre>
 * or from a setter method:
 * <pre name="code" class="java">
 * // This setter method will be automatically called by the system
 * // to set grid task session.
 * &#64GridTaskSessionResource
 * void setSession(GridTaskSession ses) {
 *     this.ses = ses;
 * }
 * </pre>
 * <h1 class="header">Example</h1>
 * To see example on how to use <tt>GridTaskSession</tt> refer to
 * <a href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/HelloWorld+-+Gridify+With+Session" target="_top">HelloWorld Distributed Task Session Example</a>
 * on Wiki.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridTaskSession {
    /**
     * Gets task name of the task this session belongs to.
     *
     * @return Task name of the task this session belongs to.
     */
    public String getTaskName();

    /**
     * Gets ID of the node on which task execution originated.
     *
     * @return ID of the node on which task execution originated.
     */
    public UUID getTaskNodeId();

    /**
     * Gets end of computation time for the task. No job within the task
     * will be allowed to execute passed this time.
     *
     * @return End of computation time for the task.
     */
    public long getEndTime();

    /**
     * Gets session ID of the task being executed.
     *
     * @return Session ID of the task being executed.
     */
    public UUID getId();

    /**
     * Gets class loader responsible for loading all classes within task.
     *
     * @return Class loader responsible for loading all classes within task.
     */
    public ClassLoader getClassLoader();

    /**
     * Gets a collection of all grid job siblings. Job siblings are grid jobs
     * that are executing within the same task.
     *
     * @return Collection of grid job siblings executing within this task.
     */
    public Collection<GridJobSibling> getJobSiblings();

    /**
     * Gets job sibling for a given ID.
     *
     * @param jobId Job ID to get the sibling for.
     * @return Grid job sibling for a given ID.
     */
    public GridJobSibling getJobSibling(UUID jobId);

    /**
     * Sets session attributed. Note that task session is distributed and
     * this attribute will be propagated to all other jobs within this task and task
     * itself - i.e., to all accessors of this session.
     * Other jobs then will be notified by {@link GridTaskSessionAttributeListener}
     * callback than an attribute has changed.
     * <p>
     * This method is no-op if the session has finished.
     *
     * @param key Attribute key.
     * @param val Attribute value. Can be <tt>null</tt>.
     * @throws GridException If sending of attribute message failed.
     */
    public void setAttribute(Serializable key, @Nullable Serializable val) throws GridException;

    /**
     * Gets an attribute set by {@link #setAttribute} or {@link #setAttributes(Map)}
     * method. Note that this attribute could have been set by another job on
     * another node.
     * <p>
     * This method is no-op if the session has finished.
     *
     * @param key Attribute key.
     * @return Gets task attribute for given name.
     */
    public Serializable getAttribute(Serializable key);

    /**
     * Sets task attributes. This method exists so one distributed replication
     * operation will take place for the whole group of attributes passed in.
     * Use it for performance reasons, rather than {@link #setAttribute(Serializable,Serializable)}
     * method, whenever you need to set multiple attributes.
     * <p>
     * This method is no-op if the session has finished.
     *
     * @param attrs Attributes to set.
     * @throws GridException If sending of attribute message failed.
     */
    public void setAttributes(Map<? extends Serializable, ? extends Serializable> attrs) throws GridException;

    /**
     * Gets all attributes.
     *
     * @return All session attributes.
     */
    public Map<? extends Serializable, ? extends Serializable> getAttributes();

    /**
     * Add listener for the session attributes.
     *
     * @param listener Listener to add.
     * @param rewind <tt>true</tt> value will result in calling given listener for all
     *      already received attributes, while <tt>false</tt> value will result only
     *      in new attribute notification. Settings <tt>rewind</tt> to <tt>true</tt>
     *      allows for a simple mechanism that prevents the loss of notifications for
     *      the attributes that were previously received or received while this method
     *      was executing.
     */
    public void addAttributeListener(GridTaskSessionAttributeListener listener, boolean rewind);

    /**
     * Removes given listener.
     *
     * @param listener Listener to remove.
     * @return <tt>true</tt> if listener was removed, <tt>false</tt> otherwise.
     */
    public boolean removeAttributeListener(GridTaskSessionAttributeListener listener);

    /**
     * Waits for the specified attribute to be set. If this attribute is already in session
     * this method will return immediately.
     *
     * @param key Attribute key to wait for.
     * @return Value of newly set attribute.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public Serializable waitForAttribute(Serializable key) throws InterruptedException;

    /**
     * Waits for the specified attribute to be set or updated with given value. Note that
     * this method will block even if attribute is set for as long as its value is not equal
     * to the specified.
     *
     * @param key Attribute key to wait for.
     * @param val Attribute value to wait for. Can be <tt>null</tt>.
     * @return Whether or not key/value pair has been received.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public boolean waitForAttribute(Serializable key, @Nullable Serializable val) throws InterruptedException;

    /**
     * Waits for the specified attribute to be set. If this attribute is already in session
     * this method will return immediately.
     *
     * @param key Attribute key to wait for.
     * @param timeout Timeout in milliseconds to wait for. <tt>0</tt> means indefinite wait.
     * @return Value of newly set attribute.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public Serializable waitForAttribute(Serializable key, long timeout) throws InterruptedException;

    /**
     * Waits for the specified attribute to be set or updated with given value. Note that
     * this method will block even if attribute is set for as long as its value is not equal
     * to the specified.
     *
     * @param key Attribute key to wait for.
     * @param val Attribute value to wait for. Can be <tt>null</tt>.
     * @param timeout Timeout in milliseconds to wait for. <tt>0</tt> means indefinite wait.
     * @return Whether or not specified key/value pair has been set.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public boolean waitForAttribute(Serializable key, @Nullable Serializable val, long timeout)
        throws InterruptedException;

    /**
     * Waits for the specified attributes to be set. If these attributes are already in session
     * this method will return immediately.
     *
     * @param keys Attribute keys to wait for.
     * @return Attribute values mapped by their keys.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public Map<? extends Serializable, ? extends Serializable> waitForAttributes(
        Collection<? extends Serializable> keys) throws InterruptedException;

    /**
     * Waits for the specified attributes to be set or updated with given values. Note that
     * this method will block even if attributes are set for as long as their values are not equal
     * to the specified.
     *
     * @param attrs Key/value pairs to wait for.
     * @return Whether or not key/value pairs have been set.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public boolean waitForAttributes(Map<? extends Serializable, ? extends Serializable> attrs)
        throws InterruptedException;

    /**
     * Waits for the specified attributes to be set. If these attributes are already in session
     * this method will return immediately.
     *
     * @param keys Attribute keys to wait for.
     * @param timeout Timeout in milliseconds to wait for. <tt>0</tt> means indefinite wait.
     * @return Attribute values mapped by their keys.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public Map<? extends Serializable, ? extends Serializable> waitForAttributes(
        Collection<? extends Serializable> keys, long timeout) throws InterruptedException;

    /**
     * Waits for the specified attributes to be set or updated with given values. Note that
     * this method will block even if attributes are set for as long as their values are not equal
     * to the specified.
     *
     * @param attrs Key/value pairs to wait for.
     * @param timeout Timeout in milliseconds to wait for. <tt>0</tt> means indefinite wait.
     * @return Whether or not key/value pair has been set.
     * @throws InterruptedException Thrown if wait was interrupted.
     */
    public boolean waitForAttributes(Map<? extends Serializable, ? extends Serializable> attrs, long timeout)
        throws InterruptedException;

    /**
     * Saves intermediate state of a job or task to a storage. The storage implementation is defined
     * by {@link GridCheckpointSpi} implementation used.
     * <p>
     * Long running jobs may decide to store intermediate state to protect themselves from failures.
     * This way whenever a job fails over to another node, it can load its previously saved state via
     * {@link #loadCheckpoint(String)} method and continue with execution.
     * <p>
     * This method defaults checkpoint scope to {@link GridCheckpointScope#SESSION_SCOPE} and
     * implementation will automatically remove the checkpoint at the end of the session. It is
     * analogous to calling {@link #saveCheckpoint(String, Serializable, GridCheckpointScope, long) saveCheckpoint(String, Serializable, GridCheckpointScope.SESSION_SCOPE, 0}.
     *
     * @param key Key to be used to load this checkpoint in future.
     * @param state Intermediate job state to save.
     * @throws GridException If failed to save intermediate job state.
     * @see #loadCheckpoint(String)
     * @see #removeCheckpoint(String)
     * @see GridCheckpointSpi
     */
    public void saveCheckpoint(String key, Serializable state) throws GridException;

    /**
     * Saves intermediate state of a job to a storage. The storage implementation is defined
     * by {@link GridCheckpointSpi} implementation used.
     * <p>
     * Long running jobs may decide to store intermediate state to protect themselves from failures.
     * This way whenever a job fails over to another node, it can load its previously saved state via
     * {@link #loadCheckpoint(String)} method and continue with execution.
     * <p>
     * The life time of the checkpoint is determined by its timeout and scope.
     * If {@link GridCheckpointScope#GLOBAL_SCOPE} is used, the checkpoint will outlive
     * its session, and can only be removed by calling {@link GridCheckpointSpi#removeCheckpoint(String)}
     * from {@link Grid} or another task or job.
     *
     * @param key Key to be used to load this checkpoint in future.
     * @param state Intermediate job state to save.
     * @param scope Checkpoint scope. If equal to {@link GridCheckpointScope#SESSION_SCOPE}, then
     *      state will automatically be removed at the end of task execution. Otherwise, if scope is
     *      {@link GridCheckpointScope#GLOBAL_SCOPE} then state will outlive its session and can be
     *      removed by calling {@link #removeCheckpoint(String)} from another task or whenever
     *      timeout expires.
     * @param timeout Maximum time this state should be kept by the underlying storage. Value <tt>0</tt> means that 
     *       timeout will never expire.
     * @throws GridException If failed to save intermediate job state.
     * @see #loadCheckpoint(String)
     * @see #removeCheckpoint(String)
     * @see GridCheckpointSpi
     */
    public void saveCheckpoint(String key, Serializable state, GridCheckpointScope scope, long timeout)
        throws GridException;

    /**
     * Loads job's state previously saved via {@link #saveCheckpoint(String, Serializable, GridCheckpointScope, long)}
     * method from an underlying storage for a given <tt>key</tt>. If state was not previously
     * saved, then <tt>null</tt> will be returned. The storage implementation is defined by
     * {@link GridCheckpointSpi} implementation used.
     * <p>
     * Long running jobs may decide to store intermediate state to protect themselves from failures.
     * This way whenever a job starts, it can load its previously saved state and continue
     * with execution.
     *
     * @param key Key for intermediate job state to load.
     * @return Previously saved state or <tt>null</tt> if no state was found for a given <tt>key</tt>.
     * @throws GridException If failed to load job state.
     * @see #loadCheckpoint(String)
     * @see #removeCheckpoint(String)
     * @see GridCheckpointSpi
     */
    public Serializable loadCheckpoint(String key) throws GridException;

    /**
     * Removes previously saved job's state for a given <tt>key</tt> from an underlying storage.
     * The storage implementation is defined by {@link GridCheckpointSpi} implementation used.
     * <p>
     * Long running jobs may decide to store intermediate state to protect themselves from failures.
     * This way whenever a job starts, it can load its previously saved state and continue
     * with execution.
     *
     * @param key Key for intermediate job state to load.
     * @return <tt>true</tt> if job state was removed, <tt>false</tt> if state was not found.
     * @throws GridException If failed to remove job state.
     * @see #loadCheckpoint(String)
     * @see #removeCheckpoint(String)
     * @see GridCheckpointSpi
     */
    public boolean removeCheckpoint(String key) throws GridException;
}
