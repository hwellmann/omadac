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

import java.util.*;
import junit.framework.*;
import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit3SerializableTestSuite implements GridJunit3SerializableTest {
    /** */
    private final String name;

    /** */
    private final Class<? extends Test> testCls;

    /** */
    private List<GridJunit3SerializableTest> tests = new ArrayList<GridJunit3SerializableTest>();

    /** */
    private transient TestSuite suite = null;

    /**
     *
     * @param suite Test suite.
     */
    GridJunit3SerializableTestSuite(TestSuite suite) {
        assert suite != null : "ASSERTION [line=52, file=src/java/org/gridgain/grid/test/junit3/GridJunit3SerializableTestSuite.java]";

        this.suite = suite;

        testCls = ((GridJunit3TestSuiteProxy)suite).getOriginal().getClass();

        name = suite.getName();

        for (Enumeration<Test> e = suite.tests(); e.hasMoreElements();) {
            Test t = e.nextElement();

            if (t instanceof TestCase) {
                tests.add(new GridJunit3SerializableTestCase((TestCase)t));
            }
            else {
                assert t instanceof TestSuite == true : "ASSERTION [line=67, file=src/java/org/gridgain/grid/test/junit3/GridJunit3SerializableTestSuite.java]. " + "Test is not instance of TestCase or TestSuite: " + t;

                tests.add(new GridJunit3SerializableTestSuite((TestSuite)t));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends Test> getTestClass() {
        return testCls;
    }

    /**
     * {@inheritDoc}
     */
    public Test getTest() {
        // Initialize suite after deserialization.
        if (suite == null) {
            suite = new TestSuite(name);

            for (GridJunit3SerializableTest test : tests) {
                suite.addTest(test.getTest());
            }
        }

        return suite;
    }

    /**
     * {@inheritDoc}
     */
    public GridJunit3SerializableTestCase findTestCase(TestCase t) {
        for (GridJunit3SerializableTest test : tests) {
            GridJunit3SerializableTestCase found = test.findTestCase(t);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setResult(GridJunit3SerializableTest res) {
        GridJunit3SerializableTestSuite suite = (GridJunit3SerializableTestSuite)res;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < tests.size(); i++) {
            GridJunit3SerializableTest empty = tests.get(i);

            GridJunit3SerializableTest full = suite.tests.get(i);

            empty.setResult(full);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJunit3SerializableTestSuite.class, this);
    }
}
