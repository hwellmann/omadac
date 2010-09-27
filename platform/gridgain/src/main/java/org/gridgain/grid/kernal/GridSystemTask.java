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

package org.gridgain.grid.kernal;

import java.util.*;

import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.*;

/**
 * All system tasks should extend this class.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <T> Type of the task argument.
 * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
 */
public abstract class GridSystemTask<T, R> extends GridTaskAdapter<T, R> {
    /** */
    private final Class<?> execCls;

    /** */
    private final ClassLoader execLdr;

    /**
     * @param execCls Executed class.
     */
    protected GridSystemTask(Class<?> execCls) {
        assert execCls != null : "ASSERTION [line=49, file=src/java/org/gridgain/grid/kernal/GridSystemTask.java]";

        this.execCls = execCls;

        execLdr = GridUtils.detectClassLoader(execCls);
    }

    /**
     * @param execCls Executed class.
     * @param execLdr Class loader for the class.
     */
    protected GridSystemTask(Class<?> execCls, ClassLoader execLdr) {
        assert execCls != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/kernal/GridSystemTask.java]";
        assert execLdr != null : "ASSERTION [line=62, file=src/java/org/gridgain/grid/kernal/GridSystemTask.java]";

        this.execCls = execCls;
        this.execLdr = execLdr;
    }

    /**
     * Gets execution class.
     *
     * @return Execution class.
     */
    public Class<?> getExecutionClass() {
        return execCls;
    }

    /**
     * Gets execution class loader.
     *
     * @return Execution class loader.
     */
    public ClassLoader getExecutionClassLoader() {
        return execLdr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridSystemTask.class, this);
    }
}
