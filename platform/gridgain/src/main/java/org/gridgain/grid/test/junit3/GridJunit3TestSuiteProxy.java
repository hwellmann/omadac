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
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit3TestSuiteProxy extends TestSuite {
    /** */
    private final GridJunit3ProxyFactory factory;

    /** */
    private final TestSuite original;

    /**
     *
     * @param suite Test suite to wrap.
     * @param factory Proxy factory.
     */
    GridJunit3TestSuiteProxy(TestSuite suite, GridJunit3ProxyFactory factory) {
        assert suite != null : "ASSERTION [line=45, file=src/java/org/gridgain/grid/test/junit3/GridJunit3TestSuiteProxy.java]";
        assert factory != null : "ASSERTION [line=46, file=src/java/org/gridgain/grid/test/junit3/GridJunit3TestSuiteProxy.java]";

        original = suite;

        setName(suite.getName());

        this.factory = factory;

        for (int i = 0; i < suite.testCount(); i++) {
            addTest(suite.testAt(i));
        }
    }

    /**
     *
     * @return Original test suite.
     */
    TestSuite getOriginal() {
        return original;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTest(Test test) {
        if (test instanceof GridJunit3TestSuiteProxy == true || test instanceof GridJunit3TestCaseProxy == true) {
            super.addTest(test);
        }
        else if (test instanceof TestSuite) {
            super.addTest(new GridJunit3TestSuiteProxy((TestSuite)test, factory));
        }
        else {
            assert test instanceof TestCase : "ASSERTION [line=79, file=src/java/org/gridgain/grid/test/junit3/GridJunit3TestSuiteProxy.java]. " + "Test must be either instance of TestSute or TestCase: " + test;

            super.addTest(factory.createProxy((TestCase)test));
        }
    }
}
