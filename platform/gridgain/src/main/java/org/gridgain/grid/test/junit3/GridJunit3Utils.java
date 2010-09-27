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

package org.gridgain.grid.test.junit3;

import java.io.*;
import java.lang.reflect.*;
import junit.framework.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridJunit3Utils {
    /**
     * Enforces singleton.
     */
    private GridJunit3Utils() {
        // No-op.
    }

    /**
     *
     * @param name Test name.
     * @param cls Test class.
     * @return Created test case.
     */
    @SuppressWarnings({"ProhibitedExceptionThrown"})
    static TestCase createTest(String name, Class<? extends TestCase> cls) {
        assert name != null : "ASSERTION [line=50, file=src/java/org/gridgain/grid/test/junit3/GridJunit3Utils.java]";
        assert cls != null : "ASSERTION [line=51, file=src/java/org/gridgain/grid/test/junit3/GridJunit3Utils.java]";

        Constructor<?> constructor;

        try {
            constructor = getTestConstructor(cls);
        }
        catch (NoSuchMethodException e) {
            return warning("Class "+ cls.getName() + " has no public constructor TestCase(String name)" +
                " or TestCase() [error=" + toString(e) + ']');
        }

        Object test;

        try {
            if (constructor.getParameterTypes().length == 0) {
                test = constructor.newInstance();

                if (test instanceof TestCase) {
                    ((TestCase)test).setName(name);
                }
            }
            else {
                test = constructor.newInstance(name);
            }
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Cannot instantiate class.", e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException("Exception in constructor: " +
                name + " (" + toString(e.getTargetException()) + ')', e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access test case: " + name + " (" + toString(e) + ')', e);
        }

        return (TestCase)test;
    }

    /**
     * Gets a constructor which takes a single String as
     * its argument or a no arg constructor.
     *
     * @param cls Test class.
     * @return FIXDOC
     * @throws NoSuchMethodException FIXDOC
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    static Constructor<?> getTestConstructor(Class<? extends TestCase> cls) throws NoSuchMethodException {
        try {
            return cls.getConstructor(String.class);
        }
        catch (NoSuchMethodException e) {
            // fall through
        }

        return cls.getConstructor();
    }

    /**
     * Returns a test which will fail and log a warning message.
     *
     * @param msg Warning message.
     * @return FIXDOC
     */
    static TestCase warning(final String msg) {
        return new TestCase("warning") {
            /**
             * {@inheritDoc}
             */
            @Override
            protected void runTest() {
                fail(msg);
            }
        };
    }

    /**
     * Converts the stack trace into a string.
     *
     * @param t Throwable to convert.
     * @return Stack trace as a string.
     */
    static String toString(Throwable t) {
        StringWriter stringWriter = new StringWriter();

        PrintWriter writer = new PrintWriter(stringWriter);

        t.printStackTrace(writer);

        return stringWriter.toString();
    }
}
