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

package org.gridgain.grid.spi.checkpoint;

import java.io.*;

import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

/**
 * Checkpoint SPI provides an ability to save an intermediate job state. It can
 * be useful when long running jobs need to store some intermediate state to
 * protect from system or application failures. Grid job can save intermediate
 * state in certain points of the execution (e.g., periodically) and upon start
 * check if previously saved state exists. This allows job to restart from the last
 * save checkpoint in case of preemption or other types of failover. The only
 * requirement for job state to be a checkpoint is to implement {@link Serializable}
 * interface.
 * <p>
 * Note, that since a job can execute on different nodes, checkpoints need to
 * be accessible by all nodes.
 * <p>
 * To manipulate checkpoints from grid job the following public methods are available
 * on task session (that can be injected into grid job):
 * <ul>
 * <li>{@link GridTaskSession#loadCheckpoint(String)}</li>
 * <li>{@link GridTaskSession#removeCheckpoint(String)}</li>
 * <li>{@link GridTaskSession#saveCheckpoint(String, Serializable)}</li>
 * <li>{@link GridTaskSession#saveCheckpoint(String, Serializable, GridCheckpointScope, long)}</li>
 * </ul>
 * <p>
 * GridGain provides the following <tt>GridCheckpointSpi</tt> implementations:
 * <ul>
 * <li>{@link org.gridgain.grid.spi.checkpoint.sharedfs.GridSharedFsCheckpointSpi}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridCheckpointSpi extends GridSpi {
    /**
     * Loads checkpoint from storage by its unique key.
     *
     * @param key Checkpoint key.
     * @return Loaded data or <tt>null</tt> if there is no data for a given
     *      key.
     * @throws GridSpiException Thrown in case of any error while loading
     *      checkpoint data. Note that in case when given <tt>key</tt> is not
     *      found this method will return <tt>null</tt>.
     */
    public byte[] loadCheckpoint(String key) throws GridSpiException;

    /**
     * Saves checkpoint to the storage. If checkpoint exists it will be
     * overwritten by new one.
     *
     * @param key Checkpoint unique key.
     * @param state Saved data.
     * @param timeout Every intermediate data stored by checkpoint provider
     *      should have a timeout. Timeout allows for effective resource
     *      management by checkpoint provider by cleaning saved data that are not
     *      needed anymore. Generally, the user should choose the minimum
     *      possible timeout to avoid long-term resource acquisition by checkpoint
     *      provider. Value <tt>0</tt> means that timeout will never expire.
     * @throws GridSpiException Thrown in case of any error while saving
     *    checkpoint data.
     */
    public void saveCheckpoint(String key, byte[] state, long timeout) throws GridSpiException;

    /**
     * This method instructs the checkpoint provider to clean saved data for a
     * given <tt>key</tt>.
     *
     * @param key Key for the checkpoint to remove.
     * @return <tt>true</tt> if data has been actually removed, <tt>false</tt>
     *      otherwise.
     */
    public boolean removeCheckpoint(String key);
}
