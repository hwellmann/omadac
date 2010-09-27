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

package org.gridgain.grid.gridify.aop.spring;

import java.lang.reflect.*;
import org.aopalliance.intercept.*;
import org.gridgain.grid.*;
import org.gridgain.grid.gridify.*;
import org.gridgain.grid.kernal.*;

/**
 * Spring aspect that cross-cuts on all methods grid-enabled with
 * {@link Gridify} annotation and potentially executes them on
 * remote node.
 * <p>
 * Note that Spring uses proxy-based AOP, so in order to be properly
 * cross-cut, all methods need to be enhanced with {@link GridifySpringEnhancer}
 * helper.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * <tt>gridified</tt> methods.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see Gridify
 */
public class GridifySpringAspect implements MethodInterceptor {
    /**
     * Aspect implementation which executes grid-enabled methods on remote
     * nodes.
     *
     * {@inheritDoc}
     */
    @SuppressWarnings({"ProhibitedExceptionDeclared", "ProhibitedExceptionThrown", "CatchGenericClass", "unchecked"})
    public Object invoke(MethodInvocation invoc) throws Throwable {
        Method mtd  = invoc.getMethod();

        Gridify ann = mtd.getAnnotation(Gridify.class);

        assert ann != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/gridify/aop/spring/GridifySpringAspect.java]. " + "Intercepted method does not have gridify annotation.";

        // Since annotations in Java don't allow 'null' as default value
        // we have accept an empty string and convert it here.
        // NOTE: there's unintended behavior when user specifies an empty
        // string as intended grid name.
        // NOTE: the 'ann.gridName() == null' check is added to mitigate
        // annotation bugs in some scripting languages (e.g. Groovy).
        String gridName = ann.gridName() == null || ann.gridName().length() == 0 ? null : ann.gridName();

        if (GridFactory.getState(gridName) != GridFactoryState.STARTED) {
            throw (GridException)new GridException("Grid is not locally started: " + gridName).setData(70, "src/java/org/gridgain/grid/gridify/aop/spring/GridifySpringAspect.java");
        }

        // Initialize defaults.
        GridifyArgument arg = new GridifyArgumentAdapter(mtd.getDeclaringClass(), mtd.getName(),
            mtd.getParameterTypes(), invoc.getArguments(), invoc.getThis());

        if (ann.interceptor().equals(GridifyInterceptor.class) == false) {
            // Check interceptor first.
            if (ann.interceptor().newInstance().isGridify(ann, arg) == false) {
                return invoc.proceed();
            }
        }

        if (ann.taskClass().equals(GridifyDefaultTask.class) == false && ann.taskName().length() > 0) {
            throw (GridException)new GridException("Gridify annotation must specify either Gridify.taskName() or " +
                "Gridify.taskClass(), but not both: " + ann).setData(85, "src/java/org/gridgain/grid/gridify/aop/spring/GridifySpringAspect.java");
        }

        try {
            Grid grid = GridFactory.getGrid(gridName);

            if (ann.taskClass().equals(GridifyDefaultTask.class) == false) {
                //noinspection unchecked
                return grid.execute((Class<? extends GridTask<GridifyArgument, Object>>)ann.taskClass(), arg,
                    ann.timeout()).get();
            }

            // If task name was not specified.
            if (ann.taskName().length() == 0) {
                return grid.execute(new GridifyDefaultTask(invoc.getMethod().getDeclaringClass()), arg,
                    ann.timeout()).get();
            }

            // If task name was specified.
            return grid.execute(ann.taskName(), arg, ann.timeout()).get();
        }
        catch (Throwable e) {
            for (Class<?> ex : invoc.getMethod().getExceptionTypes()) {
                // Descend all levels down.
                Throwable cause = e.getCause();

                while (cause != null) {
                    if (ex.isAssignableFrom(cause.getClass()) == true) {
                        throw cause;
                    }

                    cause = cause.getCause();
                }

                if (ex.isAssignableFrom(e.getClass()) == true) {
                    throw e;
                }
            }

            throw (GridifyRuntimeException)new GridifyRuntimeException("Undeclared exception thrown: " + e.getMessage(), e).setData(125, "src/java/org/gridgain/grid/gridify/aop/spring/GridifySpringAspect.java");
        }
    }
}
