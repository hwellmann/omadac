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

package org.gridgain.grid.kernal.managers.collision;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.job.*;
import org.gridgain.grid.spi.collision.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public abstract class GridCollisionJobContextAdapter implements GridCollisionJobContext {
    /** */
    private final GridJobWorker jobWorker;

    /**
     *
     * @param jobWorker Job worker instance.
     */
    public GridCollisionJobContextAdapter(GridJobWorker jobWorker) {
        assert jobWorker != null : "ASSERTION [line=45, file=src/java/org/gridgain/grid/kernal/managers/collision/GridCollisionJobContextAdapter.java]";

        this.jobWorker = jobWorker;
    }

    /**
     * {@inheritDoc}
     */
    public GridTaskSessionImpl getTaskSession() {
        return jobWorker.getSession();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public UUID getJobId() {
        return jobWorker.getJobId();
    }

    /**
     * {@inheritDoc}
     */
    public GridJobContext getJobContext() {
        return jobWorker.getJobContext();
    }

    /**
     *
     * @return Job worker.
     */
    public GridJobWorker getJobWorker() {
        return jobWorker;
    }

    /**
     * {@inheritDoc}
     */
    public GridJob getJob() {
        return jobWorker.getJob();
    }
}
