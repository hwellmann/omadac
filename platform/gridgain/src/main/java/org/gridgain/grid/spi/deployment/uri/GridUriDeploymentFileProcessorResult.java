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

package org.gridgain.grid.spi.deployment.uri;

import java.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentFileProcessorResult {
    /** */
    private ClassLoader clsLdr = null;

    /** */
    private List<Class<? extends GridTask<?, ?>>> taskClss = null;

    /**
     * Getter for property 'clsLdr'.
     *
     * @return Value for property 'clsLdr'.
     */
    public ClassLoader getClassLoader() {
        return clsLdr;
    }

    /**
     * Setter for property 'clsLdr'.
     *
     * @param clsLdr Value to set for property 'clsLdr'.
     */
    public void setClassLoader(ClassLoader clsLdr) {
        this.clsLdr = clsLdr;
    }

    /**
     * Getter for property 'taskClss'.
     *
     * @return Value for property 'taskClss'.
     */
    public List<Class<? extends GridTask<?, ?>>> getTaskClasses() {
        return taskClss;
    }

    /**
     * Setter for property 'taskClss'.
     *
     * @param taskClss Value to set for property 'taskClss'.
     */
    public void setTaskClasses(List<Class<? extends GridTask<?, ?>>> taskClss) {
        this.taskClss = taskClss;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentFileProcessorResult.class, this);
    }
}
