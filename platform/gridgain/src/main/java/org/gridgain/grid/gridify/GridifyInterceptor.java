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
 * This interface defines an interceptor call for {@link Gridify} annotation. Interceptor
 * gets called in advise code to decide whether or not to grid-enable this method.
 * <p>
 * Interceptors can be used to provide fine-grain control on {@link Gridify} annotation
 * behavior. For example, an interceptor can be implemented to grid enable the method
 * only if CPU on the local node has been above 80% of utilization for the last 5 minutes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridifyInterceptor {
    /**
     * This method is called before actual grid-enabling happens. 
     * 
     * @param gridify Gridify annotation instance that caused the grid-enabling.
     * @param arg Gridify argument.
     * @return <tt>True</tt> if method should be grid-enabled, <tt>false</tt> otherwise.
     * @throws GridException Thrown in case of any errors.
     */
    public boolean isGridify(Gridify gridify, GridifyArgument arg) throws GridException;
}
