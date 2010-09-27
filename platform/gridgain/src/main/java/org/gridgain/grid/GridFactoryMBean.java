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

import org.gridgain.apache.*;
import org.gridgain.grid.util.mbean.*;

/**
 * This interface defines JMX view on {@link GridFactory}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
@GridMBeanDescription("MBean that provides access to grid life-cycle operations.")
public interface GridFactoryMBean {
    /**
     * Gets state of default grid instance.
     *
     * @return State of default grid instance.
     * @see GridFactory#getState()
     */
    @GridMBeanDescription("State of default grid instance.")
    public String getState();

    /**
     * Gets state for a given grid instance.
     *
     * @param name Name of grid instance.
     * @return State of grid instance with given name.
     * @see GridFactory#getState(String)
     */
    @GridMBeanDescription("Gets state for a given grid instance. Returns state of grid instance with given name.")
    @GridMBeanParametersNames(
        "name"
    )
    @GridMBeanParametersDescriptions(
        "Name of grid instance."
    )
    public String getState(String name);

    /**
     * Stops default grid instance.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      default grid will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution.
     * @return <tt>true</tt> if default grid instance was indeed stopped,
     *      <tt>false</tt> otherwise (if it was not started).
     * @see GridFactory#stop(boolean)
     */
    @GridMBeanDescription("Stops default grid instance. Return true if default grid instance was " +
        "indeed stopped, false otherwise (if it was not started).")
    @GridMBeanParametersNames(
        "cancel"
    )
    @GridMBeanParametersDescriptions(
        "If true then all jobs currently executing on default grid will be cancelled." 
    )    
    public boolean stop(boolean cancel);

    /**
     * Stops named grid. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted. If
     * grid name is <tt>null</tt>, then default no-name grid will be stopped.
     * It does not wait for the tasks to finish their execution.
     *
     * @param name Grid name. If <tt>null</tt>, then default no-name grid will
     *      be stopped.
     * @param cancel If <tt>true</tt> then all jobs currently will be cancelled
     *      by calling {@link GridJob#cancel()} method. Note that just like with
     *      {@link Thread#interrupt()}, it is up to the actual job to exit from
     *      execution. If <tt>false</tt>, then jobs currently running will not be
     *      canceled. In either case, grid node will wait for completion of all
     *      jobs running on it before stopping.
     * @return <tt>true</tt> if named grid instance was indeed found and stopped,
     *      <tt>false</tt> otherwise (the instance with given <tt>name</tt> was
     *      not found).
     * @see GridFactory#stop(String, boolean)
     */
    @GridMBeanDescription("Stops grid by name. Cancels running jobs if cancel is true. Returns true if named " +
        "grid instance was indeed found and stopped, false otherwise.")
    @GridMBeanParametersNames(
        {
            "name", 
            "cancel"
        })
    @GridMBeanParametersDescriptions(
        {
            "Grid instance name to stop.", 
            "Whether or not running jobs should be cancelled."
        }
    )
    public boolean stop(String name, boolean cancel);

    /**
     * Stops <b>all</b> started grids. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted.
     * It does not wait for the tasks to finish their execution.
     * <p>
     * <b>Note:</b> it is usually safer and more appropriate to stop grid instances individually
     * instead of blanket operation. In most cases, the party that started the grid instance
     * should be responsible for stopping it.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      all grids will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution
     * @see GridFactory#stopAll(boolean)
     */
    @GridMBeanDescription("Stops all started grids.")
    @GridMBeanParametersNames(
        "cancel"
    )
    @GridMBeanParametersDescriptions(
        "If true then all jobs currently executing on all grids will be cancelled."
    )
    public void stopAll(boolean cancel);

    /**
     * Stops default grid. This method is identical to <tt>GridFactory.stop(null, cancel, wait)</tt> call.
     * If wait parameter is set to <tt>true</tt> then it will wait for all
     * tasks to be finished.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      default grid will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution.
     * @param wait If <tt>true</tt> then method will wait for all task being
     *      executed until they finish their execution.
     * @return <tt>true</tt> if default grid instance was indeed stopped,
     *      <tt>false</tt> otherwise (if it was not started).
     * @see GridFactory#stop(boolean, boolean)
     */
    @GridMBeanDescription("Stops default grid. Return true if default grid instance was indeed " +
        "stopped, false otherwise (if it was not started).")
    @GridMBeanParametersNames(
        {
            "cancel", 
            "wait"
        })
    @GridMBeanParametersDescriptions(
        {
            "If true then all jobs currently executing on default grid will be cancelled.", 
            "If true then method will wait for all task being executed until they finish their execution."
        }
    )
    public boolean stop(boolean cancel, boolean wait);

    /**
     * Stops named grid. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted. If
     * grid name is <tt>null</tt>, then default no-name grid will be stopped.
     * If wait parameter is set to <tt>true</tt> then grid will wait for all
     * tasks to be finished.
     *
     * @param name Grid name. If <tt>null</tt>, then default no-name grid will
     *      be stopped.
     * @param cancel If <tt>true</tt> then all jobs currently will be cancelled
     *      by calling {@link GridJob#cancel()} method. Note that just like with
     *      {@link Thread#interrupt()}, it is up to the actual job to exit from
     *      execution. If <tt>false</tt>, then jobs currently running will not be
     *      canceled. In either case, grid node will wait for completion of all
     *      jobs running on it before stopping.
     * @param wait If <tt>true</tt> then method will wait for all task being
     *      executed until they finish their execution.
     * @return <tt>true</tt> if named grid instance was indeed found and stopped,
     *      <tt>false</tt> otherwise (the instance with given <tt>name</tt> was
     *      not found).
     * @see GridFactory#stop(String, boolean, boolean)
     */
    @GridMBeanDescription("Stops named grid.Return true  if named grid instance was indeed stopped, " +
        "false otherwise (if it was not started).")
    @GridMBeanParametersNames(
        {
            "name",
            "cancel", 
            "wait"
        })
    @GridMBeanParametersDescriptions(
        {
            "Grid name. If null, then default no-name grid will be stopped.",
            "If true then all jobs currently executing on default grid will be cancelled.", 
            "If true then method will wait for all task being executed until they finish their execution."
        }
    )
    public boolean stop(String name, boolean cancel, boolean wait);

    /**
     * Stops <b>all</b> started grids. If <tt>cancel</tt> flag is set to <tt>true</tt> then
     * all jobs currently executing on local node will be interrupted.
     * If wait parameter is set to <tt>true</tt> then grid will wait for all
     * tasks to be finished.
     * <p>
     * <b>Note:</b> it is usually safer and more appropriate to stop grid instances individually
     * instead of blanket operation. In most cases, the party that started the grid instance
     * should be responsible for stopping it.
     *
     * @param cancel If <tt>true</tt> then all jobs currently executing on
     *      all grids will be cancelled by calling {@link GridJob#cancel()}
     *      method. Note that just like with {@link Thread#interrupt()}, it is
     *      up to the actual job to exit from execution
     * @param wait If <tt>true</tt> then method will wait for all task being
     *      executed until they finish their execution.
     * @see GridFactory#stopAll(boolean, boolean)
     */
    @GridMBeanDescription("Stops all started grids.")
    @GridMBeanParametersNames(
        {
            "cancel", 
            "wait"
        })
    @GridMBeanParametersDescriptions(
        {
            "If true then all jobs currently executing on default grid will be cancelled.", 
            "If true then method will wait for all task being executed until they finish their execution."
        }
    )
    public void stopAll(boolean cancel, boolean wait);
}
