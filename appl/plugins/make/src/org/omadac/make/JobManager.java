/*
 *    Omadac - The Open Map Database Compiler
 *    http://omadac.org
 * 
 *    (C) 2010, Harald Wellmann and Contributors
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.omadac.make;

/**
 * A job manager executes actions for updating targets. The actions get executed asynchronously.
 * Action listeners get notified on action completion.
 * <p>
 * The job manager starts a number of worker threads (maybe even on remote hosts) for executing
 * the actions. Actions are single-threaded.
 * @author hwellmann
 *
 */
public interface JobManager
{
    /**
     * Sets the number of worker threads used by this job manager.
     * @param numThreads
     */
    void setNumThreads(int numThreads);
    
    /**
     * Starts the job manager.
     */
    void start();
    
    /**
     * Stops the job manager and terminates all worker threads. Any runinng actions will be
     * interrupted.
     */
    void stop();
    
    /**
     * Submits an action for execution. The method returns immediately. The action will be executed
     * when the next worker thread is available.
     * @param action
     */
    void submitAction(Action action);
    
    /**
     * Adds an action listener to be notified on action completion.
     * @param listener   action listener
     */
    void addActionListener(ActionListener listener);    

    /**
     * Removes an action listener.
     * @param listener   action listener
     */
    void removeActionListener(ActionListener listener);    
}
