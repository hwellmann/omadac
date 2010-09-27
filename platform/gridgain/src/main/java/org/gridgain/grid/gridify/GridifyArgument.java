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

import java.io.*;

/**
 * Gridify task argument created by the system for task execution. It contains
 * all information needed to reflectively execute a method remotely. 
 * <p>
 * Use {@link GridifyArgumentAdapter} convenience adapter for creating gridify
 * arguments when implementing custom gridify jobs.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * <tt>gridified</tt> methods.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see Gridify
 */
public interface GridifyArgument extends Serializable {
    /**
     * Gets class to which the executed method belongs.
     *
     * @return Class to which method belongs.
     */
    public Class<?> getMethodClass();

    /**
     * Gets method name.
     *
     * @return Method name.
     */
    public String getMethodName();

    /**
     * Gets method parameter types in the same order they appear in method
     * signature.
     *
     * @return Method parameter types.
     */
    public Class<?>[] getMethodParameterTypes();

    /**
     * Gets method parameters in the same order they appear in method
     * signature.
     *
     * @return Method parameters.
     */
    public Object[] getMethodParameters();

    /**
     * Gets target object to execute method on. <tt>Null</tt> for static methods.
     *
     * @return Execution state (possibly <tt>null</tt>), required for remote
     *      object creation.
     */
    public Object getTarget();
}
