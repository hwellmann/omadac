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

import java.lang.annotation.*;
import java.util.*;

import org.gridgain.apache.*;

/**
 * This annotation disables caching of task results when attached to {@link GridTask} class
 * being executed. By default all results are cached and passed into
 * {@link GridTask#result(GridJobResult,List) GridTask.result(GridJobResult, List&lt;GridJobResult&gt;)}
 * method or {@link GridTask#reduce(List) GridTask.reduce(List&lt;GridJobResult&gt;)} method.
 * When this annotation is attached to a task class, then {@link GridJobResult#getData()} always
 * will return <tt>null</tt> for all jobs cached in result list.
 * <p>
 * Use this annotation when job results are too large to hold in memory and can be discarded
 * after being processed in
 * {@link GridTask#result(GridJobResult, List) GridTask.result(GridJobResult, List&lt;GridJobResult&gt;)}
 * method.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Apache20LicenseCompatible
public @interface GridTaskNoResultCache {
    // No-op.
}
