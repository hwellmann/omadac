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

package org.gridgain.grid.kernal.processors;

import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridToStringExclude
public interface GridProcessor {
    /**
     * Starts grid processor.
     *
     * @throws GridException Throws in case of any errors.
     */
    public void start() throws GridException;

    /**
     * Stops grid processor.
     *
     * @param cancel If <tt>true</tt>, then all ongoing tasks or jobs for relevant
     *      processors need to be cancelled.
     * @throws GridException Thrown in case of any errors.
     */
    public void stop(boolean cancel) throws GridException;

    /**
     * Callback that notifies that kernal has successfully started,
     * including all managers and processors.
     */
    public void onKernalStart();

    /**
     * Callback to notify that kernal is about to stop.
     *
     * @param cancel Flag indicating whether jobs should be canceled.
     */
    public void onKernalStop(boolean cancel);
}
