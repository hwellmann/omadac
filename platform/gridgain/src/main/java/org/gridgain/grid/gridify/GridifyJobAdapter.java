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
import java.lang.reflect.*;
import org.gridgain.grid.*;

/**
 * Convenience adapter for custom <tt>gridify</tt> jobs. In addition to
 * functionality provided in {@link GridJobAdapter} adapter, this adapter
 * provides default implementation of {@link #execute()} method,
 * which reflectively executes grid-enabled method based on information provided
 * in {@link GridifyArgument} parameter.
 * <p>
 * Note this adapter is only useful when passing {@link GridifyArgument} to
 * remote jobs. In many cases, remote jobs will not require {@link GridifyArgument}
 * as they will execute their code without reflection, hence the regular
 * {@link GridJobAdapter} should be used.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * <tt>gridified</tt> methods.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see Gridify
 */
public class GridifyJobAdapter extends GridJobAdapter<GridifyArgument> {
    /**
     * Initializes job with argument.
     *
     * @param arg Job argument.
     */
    public GridifyJobAdapter(GridifyArgument arg) {
        super(arg);
    }

    /**
     * Provides default implementation for execution of grid-enabled methods.
     * This method assumes that argument passed in is of {@link GridifyArgument}
     * type. It attempts to reflectively execute a method based on information
     * provided in the argument and returns the return value of the method.
     * <p>
     * If some exception occurred during execution, then it will be thrown
     * out of this method.
     *
     * @return {@inheritDoc}
     * @throws GridException {@inheritDoc}
     */
    public Serializable execute() throws GridException {
        GridifyArgument arg = getArgument();

        try {
            // Get public, package, protected, or private method.
            Method mtd = arg.getMethodClass().getDeclaredMethod(arg.getMethodName(), arg.getMethodParameterTypes());

            // Attempt to soften access control in case we grid-enabling
            // non-accessible method. Subject to security manager setting.
            if (mtd.isAccessible() == false) {
                try {
                    mtd.setAccessible(true);
                }
                catch (SecurityException e) {
                    throw (GridException)new GridException("Got security exception when attempting to soften access control for " +
                        "@Gridify method: " + mtd, e).setData(83, "src/java/org/gridgain/grid/gridify/GridifyJobAdapter.java");
                }
            }

            Object obj = null;

            // No need to create an instance for static methods.
            if (Modifier.isStatic(mtd.getModifiers()) == false) {
                // Obtain instance to execute method on.
                obj = arg.getTarget();
            }

            return (Serializable)mtd.invoke(obj, arg.getMethodParameters());
        }
        catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof GridException) {
                throw (GridException)e.getTargetException();
            }

            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw (GridException)new GridException("Failed to invoke a method due to user exception.", e.getTargetException()).setData(104, "src/java/org/gridgain/grid/gridify/GridifyJobAdapter.java");
        }
        catch (IllegalAccessException e) {
            throw (GridException)new GridException("Failed to access method for execution.", e).setData(107, "src/java/org/gridgain/grid/gridify/GridifyJobAdapter.java");
        }
        catch (NoSuchMethodException e) {
            throw (GridException)new GridException("Failed to find method for execution.", e).setData(110, "src/java/org/gridgain/grid/gridify/GridifyJobAdapter.java");
        }
    }
}
