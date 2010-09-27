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
import org.gridgain.apache.*;

/**
 * This enumeration defines different life-time scopes for checkpoints. Checkpoints can
 * be saved through:
 * <ul>
 *      <li>{@link GridTaskSession#saveCheckpoint(String, Serializable, GridCheckpointScope, long)}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public enum GridCheckpointScope {
    /**
     * Checkpoints saved with this scope will be automatically removed
     * once the task session is completed (i.e. execution of the task is completed)
     * or when they time out. This is the most often used scope for checkpoints.
     * It provides behavior for use case when jobs can failover on other nodes
     * within the same session and thus checkpoints should be preserved for the duration
     * of the entire session.
     */
    SESSION_SCOPE,

    /**
     * Checkpoints saved with this scope will only be removed automatically
     * if they time out. User can always remove them manually.
     */
    GLOBAL_SCOPE
}
