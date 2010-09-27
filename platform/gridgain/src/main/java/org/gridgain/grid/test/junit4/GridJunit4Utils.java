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

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import junit.extensions.*;
import junit.framework.*;
import org.junit.internal.runners.*;
import org.junit.runner.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridJunit4Utils {
    /**
     * Enforces singleton.
     */
    private GridJunit4Utils() {
        // No-op.
    }

    /**
     *
     * @param cls FIXDOC
     * @return Super classes for given class.
     */
    static List<Class<?>> getSuperClasses(Class<?> cls) {
        List<Class<?>> results= new ArrayList<Class<?>>();

        Class<?> cur = cls;

        while (cur != null) {
            results.add(cur);

            cur = cur.getSuperclass();
        }

        return results;
    }

    /**
     *
     * @param mtd FIXDOC
     * @param results FIXDOC
     * @return <tt>True</tt> If method is shadowed by results.
     */
    static boolean isShadowed(Method mtd, List<Method> results) {
        for (Method m : results) {
            if (isShadowed(mtd, m) == true) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param cur FIXDOC
     * @param prev FIXDOC
     * @return <tt>True</tt> if method is shadowed by previous method.
     */
    static boolean isShadowed(Method cur, Method prev) {
        if (prev.getName().equals(cur.getName()) == false) {
            return false;
        }

        if (prev.getParameterTypes().length != cur.getParameterTypes().length) {
            return false;
        }

        for (int i= 0; i < prev.getParameterTypes().length; i++) {
            if (prev.getParameterTypes()[i].equals(cur.getParameterTypes()[i]) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param cls FIXDOC
     * @param annCls FIXDOC
     * @return Methods annotated with given annotation.
     */
    static List<Method> getAnnotatedMethods(Class<?> cls, Class<? extends Annotation> annCls) {
        List<Method> mtds = new ArrayList<Method>();

        for (Class<?> parent : getSuperClasses(cls)) {
            for (Method mtd : parent.getDeclaredMethods()) {
                Annotation ann = mtd.getAnnotation(annCls);

                if (ann != null && isShadowed(mtd, mtds) == false) {
                    mtds.add(mtd);
                }
            }
        }

        return mtds;
    }

    /**
     *
     * @param cls FIXDOC
     * @return <tt>True</tt> if suite has a static suite method.
     */
    static boolean hasSuiteMethod(Class<?> cls) {
        //noinspection UnusedCatchParameter
        try {
            cls.getMethod("suite");
        }
        catch (NoSuchMethodException e) {
            return false;
        }

        return true;
    }

    /**
     *
     * @param cls FIXDOC
     * @return JUnit3 test methods.
     */
    static List<Method> getJunit3Methods(Class<?> cls) {
        List<Method> mtds = new ArrayList<Method>();

        for (Method mtd : cls.getMethods()) {
            if (mtd.getName().startsWith("test") == true) {
                mtds.add(mtd);
            }
        }

        return mtds;
    }

    /**
     *
     * @param name FIXDOC
     * @param cls FIXDOC
     * @return JUnit3 suite.
     * @throws InitializationError FIXDOC
     */
    @SuppressWarnings("unchecked")
    static TestSuite createJunit3Suite(String name, Class<?> cls) throws InitializationError {
        Method suiteMtd = null;

        try {
            suiteMtd = cls.getMethod("suite");

            if (Modifier.isStatic(suiteMtd.getModifiers()) == false) {
                throw new InitializationError(cls.getName() + ".suite() must be static");
            }

            // Invoke statis suite method.
            return (TestSuite)suiteMtd.invoke(null);
        }
        catch (InvocationTargetException e) {
            throw new InitializationError(e);
        }
        catch (IllegalAccessException e) {
            throw new InitializationError(e);
        }
        catch (NoSuchMethodException e) {
            if (TestCase.class.isAssignableFrom(cls) == true) {
                //noinspection unchecked
                return new TestSuite((Class<? extends TestCase>)cls, name);
            }

            if (TestSuite.class.isAssignableFrom(cls) == true) {
                return (TestSuite)createJunit3Test(name, cls);
            }

            throw new InitializationError(e);
        }
    }

    /**
     *
     * @param test FIXDOC
     * @return FIXDOC
     */
    public static Description createJunit3Description(Test test) {
        if (test instanceof TestCase == true) {
            TestCase testCase = (TestCase)test;

            return Description.createTestDescription(testCase.getClass(), testCase.getName());
        }
        else if (test instanceof TestSuite) {
            TestSuite suite = (TestSuite)test;

            String name = suite.getName() == null ? "" : suite.getName();

            Description desc = Description.createSuiteDescription(name);

            int n = suite.testCount();

            for (int i= 0; i < n; i++) {
                desc.addChild(createJunit3Description(suite.testAt(i)));
            }

            return desc;
        }
        else if (test instanceof JUnit4TestAdapter == true) {
            JUnit4TestAdapter adapter= (JUnit4TestAdapter) test;

            return adapter.getDescription();
        }
        else if (test instanceof TestDecorator == true) {
            return createJunit3Description(((TestDecorator)test).getTest());
        }
        else {
            // This is the best we can do in this case
            return Description.createSuiteDescription(test.getClass());
        }
    }

    /**
     *
     * @param name FIXDOC
     * @param cls FIXDOC
     * @return Created test case.
     */
    static Test createJunit3Test(String name, Class<?> cls) {
        Constructor<?> constructor;

        try {
            constructor = getJunit3TestConstructor(cls);
        }
        catch (NoSuchMethodException e) {
            return junit3Warning("Class "+ cls.getName() + " has no public constructor TestCase(String name)" +
                " or TestCase() [error=" + toString(e) + ']');
        }

        Object test;

        try {
            if (constructor.getParameterTypes().length == 0) {
                test = constructor.newInstance();

                if (test instanceof TestCase == true) {
                    ((TestCase)test).setName(name);
                }
                else if (test instanceof TestSuite == true) {
                    ((TestSuite)test).setName(name);
                }
            }
            else {
                test= constructor.newInstance(name);
            }
        }
        catch (InstantiationException e) {
            return(junit3Warning("Cannot instantiate test case: " + name + " ("+ toString(e) + ')'));
        }
        catch (InvocationTargetException e) {
            return(junit3Warning("Exception in constructor: " + name + " (" + toString(e.getTargetException()) + ')'));
        }
        catch (IllegalAccessException e) {
            return(junit3Warning("Cannot access test case: " + name + " (" + toString(e) + ')'));
        }

        return (Test)test;
    }


    /**
     * Gets a constructor which takes a single String as
     * its argument or a no arg constructor.
     *
     * @param cls FIXDOC
     * @return  FIXDOC
     * @throws NoSuchMethodException FIXDOC
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    static Constructor<?> getJunit3TestConstructor(Class<?> cls) throws NoSuchMethodException {
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
     * @param msg FIXDOC
     * @return FIXDOC
     */
    static TestCase junit3Warning(final String msg) {
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
     * @return String presentation of the throwable.
     */
    static String toString(Throwable t) {
        StringWriter stringWriter = new StringWriter();

        PrintWriter writer = new PrintWriter(stringWriter);

        t.printStackTrace(writer);

        return stringWriter.toString();
    }
}
