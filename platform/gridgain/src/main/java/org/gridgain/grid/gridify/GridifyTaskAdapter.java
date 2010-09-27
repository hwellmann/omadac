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

package org.gridgain.grid.gridify;

import org.gridgain.grid.*;

/**
 * Convenience adapter for tasks that work with {@link Gridify} annotation
 * for grid-enabling methods. It enhances the regular {@link GridTaskAdapter}
 * by enforcing the argument type of {@link GridifyArgument}. All tasks 
 * that work with {@link Gridify} annotation receive an argument of this type.
 * <p>
 * Please refer to {@link GridTaskAdapter} documentation for more information
 * on additional functionality this adapter provides.
 * <p>
 * <img src="{@docRoot}/img/gg_20.png" style="padding: 0px 5px 0px 0px" align="left"><h1 class="header">Migrating to GridGain 2.0</h1>
 * In GridGain 2.0 this interface API has been updated for better static type checking. Although the change is
 * trivial and provides much better type safety during development - it introduced 
 * incompatibility with prior versions of GridGain. <a href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Migration+To+GridGain+2.0+From+Previous+Version">Follow this link</a> 
 * for easy source code migration instructions.
 * 
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <R> Return value of the task (see {@link GridTask#reduce(java.util.List)} method).
 */
public abstract class GridifyTaskAdapter<R> extends GridTaskAdapter<GridifyArgument, R> {
    // No-op.
}
