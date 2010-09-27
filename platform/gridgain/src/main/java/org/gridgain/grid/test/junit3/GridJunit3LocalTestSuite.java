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

import junit.framework.*;

/**
 * Local test suites will always be executed locally within distributed test suites.
 * They will be executed locally even if grid topology does not include local node.
 * Such functionality is very useful for tests that cannot be executed remotely
 * usually due to environment reasons, but can still benefit from parallel
 * execution with other tests within the same distributed test suite.
 * <p>
 * To use local test suite within distributed test suite, simply add
 * it to distributed test suite as follows:
 * <pre name="code" class="java">
 * public class GridJunit3ExampleTestSuite {
 *     // Local test suite example.
 *     public static TestSuite suite() {
 *         TestSuite suite = new GridJunit3TestSuite("Example Grid Test Suite");
 * 
 *         // Local nested test suite to always run tests A and B 
 *         // on the local node.
 *         TestSuite nested = new GridJunit3LocalTestSuite("Example Nested Sequential Suite");
 * 
 *         nested.addTestSuite(TestA.class);
 *         nested.addTestSuite(TestB.class);
 * 
 *         // Add local tests A and B.
 *         suite.addTest(nested);
 * 
 *         // Add other tests.
 *         suite.addTestSuite(TestC.class);
 *         suite.addTestSuite(TestD.class);
 * 
 *         return suite;
 *     }
 * } 
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJunit3LocalTestSuite extends TestSuite {
    /**
     *
     */
    public GridJunit3LocalTestSuite() {
        // No-op.
    }

    /**
     *
     * @param cls FIXDOC
     */
    public GridJunit3LocalTestSuite(final Class<? extends TestCase> cls) {
        super(cls);
    }

    /**
     *
     * @param cls FIXDOC
     * @param name FIXDOC
     */
    public GridJunit3LocalTestSuite(Class<? extends TestCase> cls, String name) {
        super(cls, name);
    }

    /**
     *
     * @param name FIXDOC
     */
    public GridJunit3LocalTestSuite(String name) {
        super(name);
    }

    /**
     *
     * @param classes FIXDOC
     */
    public GridJunit3LocalTestSuite(Class<?>... classes) {
        super(classes);
    }

    /**
     *
     * @param classes FIXDOC
     * @param name FIXDOC
     */
    public GridJunit3LocalTestSuite(Class<? extends TestCase>[] classes, String name) {
        super(classes, name);
    }
}
