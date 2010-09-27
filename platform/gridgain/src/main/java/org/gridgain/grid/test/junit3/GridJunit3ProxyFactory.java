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

import java.lang.reflect.*;
import java.util.*;
import javassist.util.proxy.*;
import junit.framework.*;
import org.gridgain.grid.util.test.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit3ProxyFactory {
    /** */
    private static final Map<Class<? extends TestCase>, Class<? extends TestCase>> cache =
        new HashMap<Class<? extends TestCase>, Class<? extends TestCase>>();

    /**
     *
     * @param testCase Test case.
     * @return Proxy test case that simulates local execution.
     */
    @SuppressWarnings({"unchecked", "CastToIncompatibleInterface"})
    TestCase createProxy(final TestCase testCase) {
        Class<? extends TestCase> proxyCls = null;

        // Cache proxy classes to avoid redundant class creation.
        synchronized (cache) {
            proxyCls = cache.get(testCase.getClass());

            if (proxyCls == null) {
                ProxyFactory factory = new ProxyFactory() {
                    /**
                     * @see javassist.util.proxy.ProxyFactory#getClassLoader()
                     */
                    @Override
                    protected ClassLoader getClassLoader() {
                        return getClass().getClassLoader();
                    }
                };

                factory.setSuperclass(testCase.getClass());

                factory.setInterfaces(new Class[] { GridJunit3TestCaseProxy.class });

                factory.setFilter(new MethodFilter() {
                    /**
                     * {@inheritDoc}
                     */
                    public boolean isHandled(Method m) {
                        return m.getName().equals("runBare") == true || m.getName().startsWith("getGridGain") == true ||
                            m.getName().startsWith("setGridGain") == true;
                    }
                });

                cache.put(testCase.getClass(), proxyCls = factory.createClass());
            }
        }

        MethodHandler handler = new MethodHandler() {
            /** */
            private byte[] stdOut = null;

            /** */
            private byte[] stdErr = null;

            /** */
            private Throwable error = null;

            /** */
            private Throwable failure = null;

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings({"ProhibitedExceptionThrown", "ProhibitedExceptionDeclared"})
            public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {
                if (m.getName().equals("getGridGainJuni3OriginalTestCase") == true) {
                    return testCase;
                }

                if (m.getName().equals("runBare") == true) {
                    GridTestPrintStreamFactory.getStdOut().write(stdOut);
                    GridTestPrintStreamFactory.getStdErr().write(stdErr);

                    if (error != null) {
                        throw error;
                    }

                    if (failure != null) {
                        throw failure;
                    }
                }
                else if (m.getName().equals("setGridGainJunit3Result") == true) {
                    stdOut = (byte[])args[0];
                    stdErr = (byte[])args[1];
                    error = (Throwable)args[2];
                    failure = (Throwable)args[3];
                }

                return null;
            }
        };

        TestCase test = GridJunit3Utils.createTest(testCase.getName(), proxyCls);

        ((ProxyObject)test).setHandler(handler);

        return test;
    }
}
