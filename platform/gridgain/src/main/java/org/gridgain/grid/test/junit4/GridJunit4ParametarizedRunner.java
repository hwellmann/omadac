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

package org.gridgain.grid.test.junit4;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.gridgain.grid.*;
import org.junit.internal.runners.*;
import org.junit.runner.*;
import org.junit.runners.Parameterized.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit4ParametarizedRunner extends GridJunit4SuiteRunner {
    /**
     *
     * @param cls FIXDOC
     */
    GridJunit4ParametarizedRunner(Class<?> cls) {
        super(cls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<GridJunit4Runner> createChildren() {
        final Class<?> cls = getTestClass();

        final AtomicInteger childCnt = new AtomicInteger(0);

        List<GridJunit4Runner> children = new ArrayList<GridJunit4Runner>();

        try {
            for (Object param : getParametersList()) {
                if (param instanceof Object[] == true) {
                    children.add(new GridJunit4ClassRunner(cls) {
                        /** */
                        private int idx = childCnt.getAndIncrement();

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        protected Description getDescription(Method mtd) {
                            return Description.createTestDescription(cls,
                                String.format("%s[%s]", mtd.getName(), idx), mtd.getAnnotations());
                        }
                    });
                }
                else {
                    throw (GridRuntimeException)new GridRuntimeException(String.format("%s.%s() must return a Collection of arrays.",
                        cls.getName(), getParametersMethod().getName())).setData(77, "src/java/org/gridgain/grid/test/junit4/GridJunit4ParametarizedRunner.java");
                }
            }
        }
        catch (InitializationError e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to create children.", e).setData(83, "src/java/org/gridgain/grid/test/junit4/GridJunit4ParametarizedRunner.java");
        }

        return children;
    }

    /**
     *
     * @return List of parameters.
     * @throws InitializationError FIXDOC
     */
    private Collection<?> getParametersList() throws InitializationError {
        try {
            return (Collection<?>) getParametersMethod().invoke(null);
        }
        catch (IllegalAccessException e) {
            throw new InitializationError(e);
        }
        catch (InvocationTargetException e) {
            throw new InitializationError(e);
        }
    }

    /**
     *
     * @return Method annotated with {@link Parameters @Parameters} annotation.
     * @throws InitializationError FIXDOC
     */
    private Method getParametersMethod() throws InitializationError {
        for (Method mtd : GridJunit4Utils.getAnnotatedMethods(getTestClass(), Parameters.class)) {
            int modifiers = mtd.getModifiers();

            if (Modifier.isStatic(modifiers) == true && Modifier.isPublic(modifiers) == true) {
                return mtd;
            }
        }

        throw new InitializationError("No public static parameters method on class: " + getTestClass());
    }
}
