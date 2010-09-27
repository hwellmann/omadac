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

package org.gridgain.grid.spi.tracing;

import org.gridgain.apache.*;
import org.gridgain.grid.spi.*;
import org.gridgain.jsr305.*;

/**
 * SPI provides pluggable tracing facility for GridGain. System runtime intercepts main
 * implementation methods and notifies this SPI. Implementation of this SPI should provide
 * any necessary processing of interception callbacks like collecting statistics, searching
 * for patterns, passing further to external monitoring console, etc.
 * <p>
 * GridGain comes with one default implementation:
 * <ul>
 *      <li>
 *          {@link org.gridgain.grid.spi.tracing.jxinsight.GridJxinsightTracingSpi} - receives method call notifications from local grid
 *          and informs JXInsight Tracer.
 *      </li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridTracingSpi extends GridSpi {
    /**
     * This callback is called right before target method interception.
     *
     * @param cls Callee class.
     * @param methodName Callee method name.
     * @param args Callee method parameters.
     */
    public void beforeCall(Class<?> cls, String methodName, Object[] args);

    /**
     * This callback is called right after target method interception.
     *
     * @param cls Callee class.
     * @param methodName Callee method name.
     * @param args Callee method parameters.
     * @param res Call result. Might be <tt>null</tt> if call
     *      returned <tt>null</tt> or if exception happened.
     * @param e Exception thrown by given method call, if any.
     */
    public void afterCall(Class<?> cls, String methodName, Object[] args, @Nullable Object res, Throwable e);
}
