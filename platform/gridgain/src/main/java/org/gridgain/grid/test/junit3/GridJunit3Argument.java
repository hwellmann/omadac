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

package org.gridgain.grid.test.junit3;

import org.gridgain.grid.test.*;
import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit3Argument {
    /** */
    private final GridTestRouter router;

    /** */
    private final GridJunit3SerializableTest test;

    /** */
    private final boolean local;

    /**
     *
     * @param router JUnit router.
     * @param test JUnit3 test.
     * @param local Local flag.
     */
    GridJunit3Argument(GridTestRouter router, GridJunit3SerializableTest test, boolean local) {
        assert router != null : "ASSERTION [line=50, file=src/java/org/gridgain/grid/test/junit3/GridJunit3Argument.java]";
        assert test != null : "ASSERTION [line=51, file=src/java/org/gridgain/grid/test/junit3/GridJunit3Argument.java]";

        this.router = router;
        this.test = test;
        this.local = local;
    }

    /**
     *
     * @return Test router.
     */
    GridTestRouter getRouter() {
        return router;
    }

    /**
     *
     * @return Serializable test.
     */
    GridJunit3SerializableTest getTest() {
        return test;
    }

    /**
     *
     * @return <tt>True</tt> if test is local.
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJunit3Argument.class, this);
    }
}
