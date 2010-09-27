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
import java.util.concurrent.*;
import org.gridgain.apache.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.jsr305.*;

/**
 * <img id="callout_img" src="{@docRoot}/img/callout_blue.gif"><span id="callout_blue">Start Here</span>&nbsp;This
 * interface defines main GridGain API available on a local grid node.
 * <p>
 * You can obtain an instance of <tt>Grid</tt> through {@link GridFactory#getGrid()},
 * or for named grids you can use {@link GridFactory#getGrid(String)} . Note that you
 * can have multiple instances of <tt>Grid</tt> running in the same VM. For
 * information on how to start or stop Grid please refer to {@link GridFactory} class.
 * <p>
 * GridGain <tt>Grid</tt> allows you to perform the following major functions:
 * <ul>
 * <li>Programmatically deploy and undeploy grid tasks.</li>
 * <li><b>Execute tasks on grid.</b></li>
 * <li>View discovered grid nodes.</li>
 * <li>Register listeners to listen to main local grid events.</li>
 * <li>Query grid events from remote nodes.</li>
 * <li>Send messages to remote grid nodes.</li>
 * </ul>
 * <p>
 * Execution of grid tasks is the most important functionality this
 * interface provides. Task execution can be either synchronous or asynchronous.
 * Synchronous task execution is done through following methods:
 * <ul>
 * <li>{@link #execute(String, Object)}</li>
 * <li>{@link #execute(String, Object, long)}</li>
 * <li>{@link #execute(Class, Object)}</li>
 * <li>{@link #execute(Class, Object, long)}</li>
 * <li>{@link #execute(GridTask, Object)}</li>
 * <li>{@link #execute(GridTask, Object, long)}</li>
 * </ul>
 * Asynchronous task execution is done through following methods:
 * <ul>
 * <li>{@link #execute(String, Object, GridTaskListener)}</li>
 * <li>{@link #execute(String, Object, long, GridTaskListener)}</li>
 * <li>{@link #execute(Class, Object, GridTaskListener)}</li>
 * <li>{@link #execute(Class, Object, long, GridTaskListener)}</li>
 * <li>{@link #execute(GridTask, Object, GridTaskListener)}</li>
 * <li>{@link #execute(GridTask, Object, long, GridTaskListener)}</li>
 * </ul>
 * Prior to executing grid task on grid it needs to be deployed. Refer to
 * {@link GridTask} documentation for more information about implicit and explicit
 * deploying and executing grid tasks.
 * <p>
 * For more information see <a target=wiki href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Grid+Interface">Grid Interface</a>
 * on Wiki.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface Grid {
    /**
     * This method calculates hash value of the given set of nodes (a topology).
     * Topology hash can be used in applications with optimistic locking scenario
     * that relying on unchanged topology during a long operation.
     * <p>
     * Note that since GridGain topology architecture is peer-to-peer (without centralized
     * coordination) there is still a small window in which different nodes would have
     * different version for the same topology. Therefore, this version cannot be used
     * in strict ACID context. Values returned by this method are not guaranteed to be
     * sequential. Standard implementation uses CRC32 hash method.
     *
     * @param nodes Collection of grid nodes. Note that this can be either full topology or
     *      any subset of it.
     * @return 8-byte topology hash value.
     */
    public long getTopologyHash(Collection<GridNode> nodes);

    /**
     * This method calculates hash all the nodes in the topology.
     * Topology hash can be used in applications with optimistic locking scenario
     * that relying on unchanged topology during a long operation. This is a shortcut
     * method for {@link #getTopologyHash(Collection)} with all grid nodes.
     * <p>
     * Note that since GridGain topology architecture is peer-to-peer (without centralized
     * coordination) there is still a small window in which different nodes would have
     * different version for the same topology. Therefore, this version cannot be used
     * in strict ACID context. Values returned by this method are not guaranteed to be
     * sequential. Standard implementation uses CRC32 hash method.
     *
     * @return 8-byte topology hash value.
     */
    public long getAllTopologyHash();

    /**
     * Adds an event listener for local events. Refer to {@link GridEventType}
     * for a set of all possible events.
     *
     * @param listener Event listener for local events.
     * @see GridEventType
     */
    public void addLocalEventListener(GridLocalEventListener listener);

    /**
     * Removes local event listener.
     *
     * @param listener Local event listener to remove.
     * @return <tt>true</tt> if listener was removed, <tt>false</tt> otherwise.
     */
    public boolean removeLocalEventListener(GridLocalEventListener listener);

    /**
     * Adds a listener for discovery events. Refer to {@link GridDiscoveryEventType}
     * for a set of all possible discovery events.
     *
     * @param listener Listener to discovery events.
     */
    public void addDiscoveryListener(GridDiscoveryListener listener);

    /**
     * Removes discovery event listener.
     *
     * @param listener Discovery event listener to remove.
     * @return <tt>True</tt> if listener was removed, <tt>false</tt> otherwise.
     */
    public boolean removeDiscoveryListener(GridDiscoveryListener listener);

    /**
     * Sends a message to a remote node. The underlying communication mechanism is defined by
     * {@link GridCommunicationSpi} implementation used.
     * <p>
     * This method can be used by jobs to communicate with other nodes in the grid. Remote nodes
     * can receive messages by registering a listener through {@link #addMessageListener(GridMessageListener)}
     * method.
     *
     * @param node Node to send a message to.
     * @param msg Message to send.
     * @throws GridException If failed to send a message to remote node.
     * @see GridCommunicationSpi
     * @see #addMessageListener(GridMessageListener)
     * @see #removeMessageListener(GridMessageListener)
     */
    public void sendMessage(GridNode node, Serializable msg) throws GridException;

    /**
     * Sends a message to a group of remote nodes. The underlying communication mechanism is defined by
     * {@link GridCommunicationSpi} implementation used.
     * <p>
     * This method can be used by jobs to communicate with other nodes in the grid. Remote nodes
     * can receive messages by registering a listener through {@link #addMessageListener(GridMessageListener)}
     * method.
     *
     * @param nodes Group of nodes to send a message to.
     * @param msg Message to send.
     * @throws GridException If failed to send a message to any of the remote nodes.
     * @see GridCommunicationSpi
     * @see #addMessageListener(GridMessageListener)
     * @see #removeMessageListener(GridMessageListener)
     */
    public void sendMessage(Collection<GridNode> nodes, Serializable msg) throws GridException;

    /**
     * Register a message listener to receive messages sent by remote nodes. The underlying
     * communication mechanism is defined by {@link GridCommunicationSpi} implementation used.
     * <p>
     * This method can be used by jobs to communicate with other nodes in the grid. Remote nodes
     * can send messages by calling {@link #sendMessage(GridNode, Serializable)} or
     * {@link #sendMessage(Collection, Serializable)} methods.
     *
     * @param listener Message listener to register.
     * @see GridCommunicationSpi
     * @see #sendMessage(GridNode, Serializable)
     * @see #sendMessage(Collection, Serializable)
     * @see #removeMessageListener(GridMessageListener)
     */
    public void addMessageListener(GridMessageListener listener);

    /**
     * Removes a previously registered message listener.
     *
     * @param listener Message listener to remove.
     * @return <tt>true</tt> of message listener was removed, <tt>false</tt> if it was not
     *      previously registered.
     * @see #addMessageListener(GridMessageListener)
     */
    public boolean removeMessageListener(GridMessageListener listener);

    /**
     * Gets a node instance based on its ID.
     *
     * @param nodeId ID of a node to get.
     * @return Node for a given ID or <tt>null</tt> is such not has not been discovered.
     * @see GridDiscoverySpi
     */
    public GridNode getNode(UUID nodeId);

    /**
     * Gets all grid nodes accepted by the filter.
     *
     * @param filter Filter for grid nodes.
     * @return Grid nodes accepted by the filter.
     */
    public Collection<GridNode> getNodes(GridNodeFilter filter);

    /**
     * Pings a remote node. The underlying communication is provided via
     * {@link GridDiscoverySpi#pingNode(UUID)} implementation.
     * <p>
     * Discovery SPIs usually have some latency in discovering failed nodes. Hence,
     * communication to remote nodes may fail at times if an attempt was made to
     * establish communication with a failed node. This method can be used to check
     * if communication has failed due to node failure or due to some other reason.
     *
     * @param nodeId ID of a node to ping.
     * @return <tt>true</tt> if node for a given ID is alive, <tt>false</tt> otherwise.
     * @see GridDiscoverySpi
     */
    public boolean pingNode(UUID nodeId);

    /**
     * Gets a collection of remote grid nodes. Remote nodes are discovered via underlying
     * {@link GridDiscoverySpi} implementation used. Unlike {@link #getAllNodes()},
     * this method does not include local grid node.
     *
     * @return Collection of remote grid nodes.
     * @see #getLocalNode()
     * @see #getAllNodes()
     * @see GridDiscoverySpi
     */
    public Collection<GridNode> getRemoteNodes();

    /**
     * Gets a collection of all grid nodes. Remote nodes are discovered via underlying
     * {@link GridDiscoverySpi} implementation used. Unlike {@link #getRemoteNodes()},
     * this method does include local grid node.
     *
     * @return Collection of remote grid nodes.
     * @see #getLocalNode()
     * @see #getRemoteNodes()
     * @see GridDiscoverySpi
     */
    public Collection<GridNode> getAllNodes();

    /**
     * Gets local grid node. Instance of local node is provided by underlying {@link GridDiscoverySpi}
     * implementation used.
     *
     * @return Local grid node.
     * @see GridDiscoverySpi
     */
    public GridNode getLocalNode();

    /**
     * Explicitely deploys given grid task on the local node. Upon completion of this method,
     * a task can immediately be executed on the grid, considering that all participating
     * remote nodes also have this task deployed. If peer-class-loading is enabled
     * (see {@link GridConfiguration#isPeerClassLoadingEnabled()}), then other nodes
     * will automatically deploy task upon execution request from the originating node without
     * having to manually deploy it.
     * <p>
     * Another way of class deployment which is supported is deployment from local class path.
     * Class from local class path has a priority over P2P deployed.
     * Following describes task class deployment:
     * <ul>
     * <li> If peer class loading is enabled (see {@link GridConfiguration#isPeerClassLoadingEnabled()})
     * <ul> Task class loaded from local class path if it is not defined as P2P loaded
     *      (see {@link GridConfiguration#getP2PLocalClassPathExclude()}).</ul>
     * <ul> If there is no task class in local class path or task class needs to be peer loaded
     *      it is downloaded from task originating node.</ul>
     * </li>
     * <li> If peer class loading is disabled (see {@link GridConfiguration#isPeerClassLoadingEnabled()})
     * <ul> Check that task class was deployed (either as GAR or explicitly) and use it.</ul>
     * <ul> If task class was not deployed then we try to find it in local class path by task
     *      name. Task name should correspond task class name.</ul>
     * <ul> If task has custom name (that does not correspond task class name) and this
     *      task was not deployed before then exception will be thrown.</ul>
     * </li>
     * </ul>
     * <p>
     * Note that this is an alternative deployment method additionally to deployment SPI that
     * provides more formal method of deploying a task, e.g. deployment of GAR files and/or URI-based
     * deployment. See {@link GridDeploymentSpi} for detailed information about grid task deployment.
     * <p>
     * Note that class can be deployed multiple times on remote nodes, i.e. re-deployed. GridGain
     * maintains internal version of deployment for each instance of deployment (analogous to
     * class and class loader in Java). Execution happens always on the latest deployed instance
     * (latest that is on the node where execution request is originated). This allows a very
     * convenient development model when a developer can execute a task on the grid from IDE,
     * then realize that he made a mistake, stop his node in IDE, fix mistake and re-execute the
     * task. Grid will automatically detect that task got renewed and redeploy it on all remote
     * nodes upon execution.
     * <p>
     * This method has no effect if the class passed in was already deployed. Implementation
     * checks for this condition and returns immediately.
     *
     * @param taskCls Task class to deploy. If task class has {@link GridTaskName} annotation,
     *      then task will be deployed under a name specified within annotation. Otherwise, full
     *      class name will be used as task's name.
     * @throws GridException If task is invalid and cannot be deployed.
     * @see GridDeploymentSpi
     */
    @SuppressWarnings("unchecked")
    public void deployTask(Class<? extends GridTask> taskCls) throws GridException;

    /**
     * Explicitely deploys given grid task on the local node. Upon completion of this method,
     * a task can immediately be executed on the grid, considering that all participating
     * remote nodes also have this task deployed. If peer-class-loading is enabled
     * (see {@link GridConfiguration#isPeerClassLoadingEnabled()}), then other nodes
     * will automatically deploy task upon execution request from the originating node without
     * having to manually deploy it.
     * <p>
     * Another way of class deployment which is supported is deployment from local class path.
     * Class from local class path has a priority over P2P deployed.
     * Following describes task class deployment:
     * <ul>
     * <li> If peer class loading is enabled (see {@link GridConfiguration#isPeerClassLoadingEnabled()})
     * <ul> Task class loaded from local class path if it is not defined as P2P loaded
     *      (see {@link GridConfiguration#getP2PLocalClassPathExclude()}).</ul>
     * <ul> If there is no task class in local class path or task class needs to be peer loaded
     *      it is downloaded from task originating node using provided class loader.</ul>
     * </li>
     * <li> If peer class loading is disabled (see {@link GridConfiguration#isPeerClassLoadingEnabled()})
     * <ul> Check that task class was deployed (either as GAR or explicitly) and use it.</ul>
     * <ul> If task class was not deployed then we try to find it in local class path by task
     *      name. Task name should correspond task class name.</ul>
     * <ul> If task has custom name (that does not correspond task class name) and this
     *      task was not deployed before then exception will be thrown.</ul>
     * </li>
     * </ul>
     * <p>
     * Note that this is an alternative deployment method additionally to deployment SPI that
     * provides more formal method of deploying a task, e.g. deployment of GAR files and/or URI-based
     * deployment. See {@link GridDeploymentSpi} for detailed information about grid task deployment.
     * <p>
     * Note that class can be deployed multiple times on remote nodes, i.e. re-deployed. GridGain
     * maintains internal version of deployment for each instance of deployment (analogous to
     * class and class loader in Java). Execution happens always on the latest deployed instance
     * (latest that is on the node where execution request is originated). This allows a very
     * convenient development model when a developer can execute a task on the grid from IDE,
     * then realize that he made a mistake, stop his node in IDE, fix mistake and re-execute the
     * task. Grid will automatically detect that task got renewed and redeploy it on all remote
     * nodes upon execution.
     * <p>
     * This method has no effect if the class passed in was already deployed. Implementation
     * checks for this condition and returns immediately.
     *
     * @param taskCls Task class to deploy. If task class has {@link GridTaskName} annotation,
     *      then task will be deployed under a name specified within annotation. Otherwise, full
     *      class name will be used as task's name.
     * @param clsLoader Task resources/classes class loader. This class loader is in charge
     *      of loading all necessary resources.
     * @throws GridException If task is invalid and cannot be deployed.
     * @see GridDeploymentSpi
     */
    @SuppressWarnings("unchecked")
    public void deployTask(Class<? extends GridTask> taskCls, ClassLoader clsLoader) throws GridException;

    /**
     * Gets map of all locally deployed tasks keyed by their task name. If no tasks were locally
     * deployed, then empty map is returned.
     *
     * @return All locally deployed tasks.
     */
    public Map<String, Class<? extends GridTask<?, ?>>> getLocalTasks();

    /**
     * Makes the best attempt to undeploy a task from the whole grid. Note that this
     * method returns immediately and does not wait until the task will actually be
     * undeployed on every node.
     * <p>
     * Note that GridGain maintains internal versions for grid tasks in case of redeployment.
     * This method will attempt to undeploy all versions on the grid task with
     * given name.
     *
     * @param taskName Name of the task to undeploy. If task class has {@link GridTaskName} annotation,
     *      then task was deployed under a name specified within annotation. Otherwise, full
     *      class name should be used as task's name.
     * @throws GridException Thrown if undeploy failed.
     */
    public void undeployTask(String taskName) throws GridException;

    /**
     * Executes a task on the grid. For information on how task gets split into remote
     * jobs and how results are reduced back into one see {@link GridTask} documentation.
     * <p>
     * This method assumes indefinite wait for task completion. To provide a timeout, use
     * {@link #execute(String, Object, long)} method. It is always recommended to specify
     * explicit task timeout.
     * <p>
     * If task for given name has not been deployed yet, then <tt>taskName</tt> will be
     * used as task class name to auto-deploy the task (see Grid#deployTask() method
     * for deployment algorithm).
     *
     * @param taskName Name of the task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, @Nullable T arg);

    /**
     * Executes a task on the grid. For information on how task gets split into remote
     * jobs and how results are reduced back into one see {@link GridTask} documentation.
     * <p>
     * If task for given name has not been deployed yet, then <tt>taskName</tt> will be
     * used as task class name to auto-deploy the task(see Grid#deployTask() method
     * for deployment algorithm).
     *
     * @param taskName Name of the task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param timeout Optional timeout for this task execution in milliseconds.
     *      If <tt>0</tt> the system will wait indefinitely for execution completion.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, @Nullable T arg, long timeout);

    /**
     * Asynchronously executes a task on the grid. For information on how task gets
     * split into remote jobs and how results are reduced back into one see {@link GridTask}
     * documentation. Once task execution is complete, {@link GridTaskListener#onFinished(GridTaskFuture)}
     * gets called. In that case {@link GridTaskFuture#isDone()} will always return <tt>true</tt>.
     * <p>
     * This method assumes indefinite wait for task completion. To provide a timeout, use
     * {@link #execute(String, Object, long, GridTaskListener)} method. It is always
     * recommended to specify explicit task timeout.
     * <p>
     * If task for given name has not been deployed yet, then <tt>taskName</tt> will be
     * used as task class name to auto-deploy the task(see Grid#deployTask() method
     * for deployment algorithm).
     *
     * @param taskName Name of the task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param listener Grid task result listener that will be called once the execution is completed
     *      (successfully or not).
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, @Nullable T arg, GridTaskListener listener);

    /**
     * Asynchronously executes a task on the grid. For information on how task gets
     * split into remote jobs and how results are reduced back into one see {@link GridTask}
     * documentation. Once task execution is complete, {@link GridTaskListener#onFinished(GridTaskFuture)}
     * gets called. In that case {@link GridTaskFuture#isDone()} will always return <tt>true</tt>.
     * <p>
     * If task for given name has not been deployed yet, then <tt>taskName</tt> will be
     * used as task class name to auto-deploy the task(see Grid#deployTask() method
     * for deployment algorithm).
     *
     * @param taskName Name of the task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param listener Grid task result listener that will be called once the execution is completed
     *      (successfully or not).
     * @param timeout Optional timeout for this task execution in milliseconds.
     *      If <tt>0</tt>, then the system will wait indefinitely for execution completion.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, @Nullable T arg, long timeout,
        GridTaskListener listener);

    /**
     * Executes a task on the grid. For information on how task gets split into remote
     * jobs and how results are reduced back into one see {@link GridTask} documentation.
     * <p>
     * This method assumes indefinite wait for task completion. To provide a timeout, use
     * {@link #execute(String, Object, long)} method. It is always recommended to specify
     * explicit task timeout.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param taskCls Class of the task to execute. If class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, @Nullable T arg);

    /**
     * Executes a task on the grid. For information on how task gets split into remote
     * jobs and how results are reduced back into one see {@link GridTask} documentation.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param taskCls Class of the task to execute. If class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param timeout Optional timeout for this task execution in milliseconds.
     *      If <tt>0</tt> the system will wait indefinitely for execution completion.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, @Nullable T arg, long timeout);

    /**
     * Asynchronously executes a task on the grid. For information on how task gets
     * split into remote jobs and how results are reduced back into one see {@link GridTask}
     * documentation. Once task execution is complete, {@link GridTaskListener#onFinished(GridTaskFuture)}
     * gets called. In that case {@link GridTaskFuture#isDone()} will always return <tt>true</tt>.
     * <p>
     * This method assumes indefinite wait for task completion. To provide a timeout, use
     * {@link #execute(String, Object, long, GridTaskListener)} method. It is always
     * recommended to specify explicit task timeout.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param taskCls Class of the task to execute. If class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param listener Grid task result listener that will be called once the execution is completed
     *      (successfully or not).
     * @param <T> Type of the task argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, @Nullable T arg,
        GridTaskListener listener);

    /**
     * Asynchronously executes a task on the grid. For information on how task gets
     * split into remote jobs and how results are reduced back into one see {@link GridTask}
     * documentation. Once task execution is complete, {@link GridTaskListener#onFinished(GridTaskFuture)}
     * gets called. In that case {@link GridTaskFuture#isDone()} will always return <tt>true</tt>.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param taskCls Class of the task to execute. If class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param listener Grid task result listener that will be called once the execution is completed
     *      (successfully or not).
     * @param timeout Optional timeout for this task execution in milliseconds.
     *      If <tt>0</tt>, then the system will wait indefinitely for execution completion.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, @Nullable T arg, long timeout,
        GridTaskListener listener);

    /**
     * Executes a task on the grid. For information on how task gets split into remote
     * jobs and how results are reduced back into one see {@link GridTask} documentation.
     * <p>
     * This method assumes indefinite wait for task completion. To provide a timeout, use
     * {@link #execute(String, Object, long)} method. It is always recommended to specify
     * explicit task timeout.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param task Instance of task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @return Task future.
     * @see GridTask for information about task execution.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, @Nullable T arg);

    /**
     * Executes a task on the grid. For information on how task gets split into remote
     * jobs and how results are reduced back into one see {@link GridTask} documentation.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param task Instance of task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param timeout Optional timeout for this task execution in milliseconds.
     *      If <tt>0</tt> the system will wait indefinitely for execution completion.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, @Nullable T arg, long timeout);

    /**
     * Asynchronously executes a task on the grid. For information on how task gets
     * split into remote jobs and how results are reduced back into one see {@link GridTask}
     * documentation. Once task execution is complete, {@link GridTaskListener#onFinished(GridTaskFuture)}
     * gets called. In that case {@link GridTaskFuture#isDone()} will always return <tt>true</tt>.
     * <p>
     * This method assumes indefinite wait for task completion. To provide a timeout, use
     * {@link #execute(String, Object, long, GridTaskListener)} method. It is always
     * recommended to specify explicit task timeout.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param task Instance of task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param listener Grid task result listener that will be called once the execution is completed
     *      (successfully or not).
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, @Nullable T arg, GridTaskListener listener);

    /**
     * Asynchronously executes a task on the grid. For information on how task gets
     * split into remote jobs and how results are reduced back into one see {@link GridTask}
     * documentation. Once task execution is complete, {@link GridTaskListener#onFinished(GridTaskFuture)}
     * gets called. In that case {@link GridTaskFuture#isDone()} will always return <tt>true</tt>.
     * <p>
     * This method is extremely useful when task class is already loaded, for example,
     * in J2EE application server environment. Since application servers already support
     * deployment and hot-redeployment, it is convenient to deploy all task related classes
     * via standard J2EE deployment and then use task classes directly.
     * <p>
     * When using this method task will be deployed automatically, so no explicit deployment
     * step is required.
     *
     * @param task Instance of task to execute. If task class has {@link GridTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task's name.
     * @param arg Optional argument of task execution, can be <tt>null</tt>.
     * @param listener Grid task result listener that will be called once the execution is completed
     *      (successfully or not).
     * @param timeout Optional timeout for this task execution in milliseconds.
     *      If <tt>0</tt>, then the system will wait indefinitely for execution completion.
     * @param <T> Type of the task's argument.
     * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
     * @return Task future.
     * @see GridTask for information about task execution.
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, @Nullable T arg, long timeout,
        GridTaskListener listener);

    /**
     * Queries local node for events using passed-in filter for event selection.
     *
     * @param filter Filter used to query events on remote nodes.
     * @return Collection of grid events found on local node.
     */
    public List<GridEvent> queryLocalEvents(GridEventFilter filter);

    /**
     * Queries all remote nodes for events using passed in filter for event selection. This operation is
     * distributed and hence can fail on communication layer and generally should take longer
     * than local event notifications.
     * <p>
     * Starting with version <tt>2.1</tt> peer-class-loading is enabled for event filters.
     * You can simply call this method and your event filter will be automatically
     * loaded and executed on all remote nodes. You no longer need to manually add it
     * to the class path on all remote nodes.
     *
     * @param filter Filter used to query events on remote nodes (must implement {@link Serializable}.
     * @param nodes Nodes to query.
     * @param timeout Maximum time to wait for result, <tt>0</tt> to wait forever.
     * @return Collection of grid events returned from specified nodes.
     * @throws GridException If query failed to execute.
     */
    public List<GridEvent> queryEvents(GridEventFilter filter, Collection<GridNode> nodes, long timeout)
        throws GridException;

    /**
     * Gets the name of the grid this grid instance (and correspondently its local node) belongs to.
     * Note that single Java VM can have multiple grid instances all belonging to different grids. Grid
     * name allows to indicate to what grid this particular grid instance (i.e. grid runtime and its 
     * local node) belongs to.
     * <p>
     * If default grid instance is used, then
     * <tt>null</tt> is returned. Refer to {@link GridFactory} documentation
     * for information on how to start named grids.
     *
     * @return Name of the grid, or <tt>null</tt> for default grid.
     */
    public String getName();

    /**
     * Creates {@link ExecutorService} which will execute all submitted
     * {@link Callable} and {@link Runnable} tasks on the grid.
     * User may run {@link Callable} and {@link Runnable} tasks
     * just like normally with {@link ExecutorService java.util.ExecutorService},
     * but these tasks must implement {@link Serializable} interface.
     * <p>
     * The execution will happen either locally or remotely, depending on
     * configuration of {@link GridLoadBalancingSpi} and {@link GridTopologySpi}.
     * <p>
     * The typical Java example could be:
     * <pre name="code" class="java">
     * ExecutorService exec = grid.newGridExecutorService();
     *
     * Future&lt;String&gt; future = exec.submit(new MyCallable());
     * ...
     * String res = future.get();
     * </pre>
     *
     * @return <tt>ExecutorService</tt> which delegates all calls to grid.
     */
    public ExecutorService newGridExecutorService();
}
