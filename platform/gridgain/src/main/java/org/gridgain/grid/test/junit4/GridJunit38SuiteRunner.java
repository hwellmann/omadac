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

import java.util.*;

import junit.framework.*;

import org.gridgain.grid.*;
import org.junit.internal.runners.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit38SuiteRunner extends GridJunit4SuiteRunner {
    /** */
    private transient TestSuite suite = null;

    /** */
    private final String name;

    /**
     * Constructor required by JUnit4.
     *
     * @param cls Suite class.
     */
    GridJunit38SuiteRunner(Class<?> cls) {
        super(cls);

        name = cls.getName();
    }

    /**
     *
     * @param suite Test suite.
     */
    GridJunit38SuiteRunner(TestSuite suite) {
        super(suite.getClass());

        name = suite.getName();

        this.suite = suite;
    }

    /**
     *
     * @return Test suite.
     * @throws InitializationError FIXDOC
     */
    TestSuite getSuite() throws InitializationError {
        if (suite == null) {
            suite = GridJunit4Utils.createJunit3Suite(name, getTestClass());
        }

        return suite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<GridJunit4Runner> createChildren() {
        List<GridJunit4Runner> children = new ArrayList<GridJunit4Runner>();

        try {
            TestSuite suite = getSuite();

            for (int i = 0; i < suite.testCount(); i++) {
                Test test = suite.testAt(i);

                if (test instanceof TestSuite == true) {
                    children.add(new GridJunit38SuiteRunner((TestSuite)test));
                }
                else if (test instanceof TestCase == true) {
                    children.add(new GridJunit38ClassRunner((TestCase)test));
                }
                else {
                    throw (GridRuntimeException)new GridRuntimeException("Unsupported test class: " + test).setData(100, "src/java/org/gridgain/grid/test/junit4/GridJunit38SuiteRunner.java");
                }
            }
        }
        catch (InitializationError e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to create suite children.", e).setData(105, "src/java/org/gridgain/grid/test/junit4/GridJunit38SuiteRunner.java");
        }

        return children;
    }
}
