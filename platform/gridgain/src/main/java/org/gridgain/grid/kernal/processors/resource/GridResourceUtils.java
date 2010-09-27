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

package org.gridgain.grid.kernal.processors.resource;

import java.lang.reflect.*;
import org.gridgain.grid.*;

/**
 * Collection of utility methods used in package for classes reflection.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridResourceUtils {
    /**
     * Ensure singleton.
     */
    private GridResourceUtils() {
        // No-op.
    }

    /**
     * Sets the field represented by this <tt>field</tt> object on the
     * specified object argument <tt>target</tt> to the specified new value <tt>rsrc</tt>.
     *
     * @param field Field where resource should be injected.
     * @param target Target object.
     * @param rsrc Resource object which should be injected in target object field.
     * @throws GridException Thrown if unable to inject resource.
     */
    static void inject(Field field, Object target, Object rsrc) throws GridException {
        if (rsrc != null && field.getType().isAssignableFrom(rsrc.getClass()) == false) {
            throw (GridException)new GridException("Resource field is not assignable from the resource: " + rsrc.getClass()).setData(52, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }

        //noinspection ErrorNotRethrown
        try {
            // Override default Java access check.
            field.setAccessible(true);

            field.set(target, rsrc);
        }
        catch (SecurityException e) {
            throw (GridException)new GridException("Failed to inject resource [field=" + field.getName() +
                ", target=" + target + ", rsrc=" + rsrc + ']', e).setData(63, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }
        catch (IllegalAccessException e) {
            throw (GridException)new GridException("Failed to inject resource [field=" + field.getName() +
                ", target=" + target + ", rsrc=" + rsrc + ']', e).setData(67, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }
        catch (ExceptionInInitializerError e) {
            throw (GridException)new GridException("Failed to inject resource [field=" + field.getName() +
                ", target=" + target + ", rsrc=" + rsrc + ']', e).setData(71, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }
    }

    /**
     * Invokes the underlying method <tt>mtd</tt> represented by this
     * {@link Method} object, on the specified object <tt>target</tt>
     * with the specified parameter object <tt>rsrc</tt>.
     *
     * @param mtd Method which should be invoked to injecte resource.
     * @param target Target object.
     * @param rsrc Resource object which should be injected.
     * @throws GridException Thrown if unable to inject resource.
     */
    static void inject(Method mtd, Object target, Object rsrc) throws GridException {
        if (mtd.getParameterTypes().length != 1 ||
            (rsrc != null && mtd.getParameterTypes()[0].isAssignableFrom(rsrc.getClass()) == false)) {
            throw (GridException)new GridException("Setter does not have single parameter of required type [type=" +
                rsrc.getClass().getName() + ", setter=" + mtd + ']').setData(89, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }

        //noinspection ErrorNotRethrown
        try {
            mtd.setAccessible(true);

            mtd.invoke(target, rsrc);
        }
        catch (IllegalAccessException e) {
            throw (GridException)new GridException("Failed to inject resource [method=" + mtd.getName() +
                ", target=" + target + ", rsrc=" + rsrc + ']', e).setData(100, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }
        catch (InvocationTargetException e) {
            throw (GridException)new GridException("Failed to inject resource [method=" + mtd.getName() +
                ", target=" + target + ", rsrc=" + rsrc + ']', e).setData(104, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }
        catch (ExceptionInInitializerError e) {
            throw (GridException)new GridException("Failed to inject resource [method=" + mtd.getName() +
                ", target=" + target + ", rsrc=" + rsrc + ']', e).setData(108, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceUtils.java");
        }
    }
}
