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

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.resources.*;

/**
 * Task for JUnit3 grid-enabled tests.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
/*@HIDE_FROM_JAVADOC@*/public class GridJunit3Task extends GridSystemTask<GridJunit3Argument, Object> {
    /** Injected local node ID. */
    @GridLocalNodeIdResource
    private UUID locNodeId = null;

    /** Grid instance. */
    @GridInstanceResource
    private Grid grid = null;

    /**
     * @param execCls Executed class.
     * @param ldr Class loader for the class.
     */
    protected GridJunit3Task(Class<?> execCls, ClassLoader ldr) {
        super(execCls, ldr);
    }

    /**
     * {@inheritDoc}
     */
    public Map<? extends GridJob, GridNode> map(List<GridNode> subgrid, GridJunit3Argument arg) {
        GridJunit3SerializableTest test = arg.getTest();

        if (arg.isLocal() == true) {
            return Collections.singletonMap(new GridJunit3Job(test), grid.getLocalNode());
        }

        return Collections.singletonMap(new GridJunit3Job(test), arg.getRouter().route(test.getTestClass(),
            test.getName(), subgrid, locNodeId));
    }

    /**
     * {@inheritDoc}
     */
    public Object reduce(List<GridJobResult> results) throws GridException {
        assert results.size() == 1 : "ASSERTION [line=70, file=src/java/org/gridgain/grid/test/junit3/GridJunit3Task.java]";

        GridJobResult res = results.get(0);

        if (res.getException() != null) {
            throw res.getException();
        }

        return res.getData();
    }
}
