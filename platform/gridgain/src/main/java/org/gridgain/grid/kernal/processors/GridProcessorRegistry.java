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

import java.util.*;
import org.gridgain.grid.kernal.processors.job.*;
import org.gridgain.grid.kernal.processors.jobmetrics.*;
import org.gridgain.grid.kernal.processors.resource.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides centralized registry for kernal processors.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridToStringExclude
public class GridProcessorRegistry {
    /** */
    @GridToStringInclude
    private GridTaskProcessor taskProc = null;

    /** */
    @GridToStringInclude
    private GridJobProcessor jobProc = null;

    /** */
    @GridToStringInclude
    private GridTimeoutProcessor timeProc = null;

    /** */
    @GridToStringInclude
    private GridResourceProcessor rsrcProc = null;

    /** */
    @GridToStringInclude
    private GridJobMetricsProcessor metricsProc = null;

    /** */
    private List<GridProcessor> procs = new ArrayList<GridProcessor>();

    /**
     *
     * @param proc Processor to add.
     */
    public void add(GridProcessor proc) {
        assert proc != null : "ASSERTION [line=68, file=src/java/org/gridgain/grid/kernal/processors/GridProcessorRegistry.java]";

        if (proc instanceof GridTaskProcessor) {
            taskProc = (GridTaskProcessor)proc;
        }
        else if (proc instanceof GridJobProcessor) {
            jobProc = (GridJobProcessor)proc;
        }
        else if (proc instanceof GridTimeoutProcessor) {
            timeProc = (GridTimeoutProcessor)proc;
        }
        else if (proc instanceof GridResourceProcessor) {
            rsrcProc = (GridResourceProcessor)proc;
        }
        else if (proc instanceof GridJobMetricsProcessor) {
            metricsProc = (GridJobMetricsProcessor)proc;
        }
        else {
            assert false : "ASSERTION [line=86, file=src/java/org/gridgain/grid/kernal/processors/GridProcessorRegistry.java]. " + "Unknown processor class: " + proc.getClass();
        }

        procs.add(proc);
    }

    /**
     *
     * @return Task processor
     */
    public GridTaskProcessor getTaskProcessor() {
        return taskProc;
    }

    /**
     *
     * @return Job processor
     */
    public GridJobProcessor getJobProcessor() {
        return jobProc;
    }

    /**
     *
     * @return Timeout processor.
     */
    public GridTimeoutProcessor getTimeoutProcessor() {
        return timeProc;
    }

    /**
     *
     * @return Resource processor.
     */
    public GridResourceProcessor getResourceProcessor() {
        return rsrcProc;
    }

    /**
     *
     * @return Metrics processor.
     */
    public GridJobMetricsProcessor getMetricsProcessor() {
        return metricsProc;
    }

    /**
     *
     * @return All processors.
     */
    public List<GridProcessor> getProcessors() {
        return procs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridProcessorRegistry.class, this);
    }
}
