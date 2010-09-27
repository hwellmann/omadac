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
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Parent adapter for all processors.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"CatchGenericClass"})
public abstract class GridProcessorAdapter implements GridProcessor {
    /** Manager registry. */
    @GridToStringExclude
    protected final GridManagerRegistry mgrReg;

    /** Processor registry. */
    @GridToStringExclude
    protected final GridProcessorRegistry procReg;

    /** Grid configuration. */
    @GridToStringExclude
    protected final GridConfiguration cfg;

    /** Grid logger. */
    @GridToStringExclude
    protected final GridLogger log;

    /**
     *
     * @param mgrReg Manager registry.
     * @param procReg Processor registry.
     * @param cfg Grid configuration.
     */
    protected GridProcessorAdapter(GridManagerRegistry mgrReg, GridProcessorRegistry procReg, GridConfiguration cfg) {
        assert mgrReg != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/kernal/processors/GridProcessorAdapter.java]";
        assert procReg != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/kernal/processors/GridProcessorAdapter.java]";
        assert cfg != null : "ASSERTION [line=62, file=src/java/org/gridgain/grid/kernal/processors/GridProcessorAdapter.java]";

        this.mgrReg = mgrReg;
        this.procReg = procReg;
        this.cfg = cfg;

        log = cfg.getGridLogger().getLogger(getClass());
    }

    /**
     * {@inheritDoc}
     */
    public void onKernalStart() {
        // No-op.
    }

    /**
     * {@inheritDoc}
     */
    public void onKernalStop(boolean cancel) {
        // No-op.
    }

    /**
     * Throws exception with uniform error message if given parameter's assertion condition
     * is <tt>false</tt>.
     *
     * @param cond Assertion condition to check.
     * @param condDescr Description of failed condition. Note that this description should include
     *      JavaBean name of the property (<b>not</b> a variable name) as well condition in
     *      Java syntax like, for example:
     *      <pre name="code" class="java">
     *      assertParameter(dirPath != null, "dirPath != null");
     *      </pre>
     *      Note that in case when variable name is the same as JavaBean property you
     *      can just copy Java condition expression into description as a string.
     * @throws GridException Thrown if given condition is <tt>false</tt>
     */
    protected final void assertParameter(boolean cond, String condDescr) throws GridException {
        if (cond == false) {
            throw (GridException)new GridException("Grid configuration parameter failed condition check: " + condDescr).setData(102, "src/java/org/gridgain/grid/kernal/processors/GridProcessorAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridProcessorAdapter.class, this);
    }
}
