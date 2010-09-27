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
import junit.framework.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.test.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit3Job extends GridJobAdapter<GridJunit3SerializableTest> {
    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /**
     *
     * @param arg JUnit3 test.
     */
    GridJunit3Job(GridJunit3SerializableTest arg) {
        super(arg);
    }

    /**
     * {@inheritDoc}
     */
    public GridJunit3SerializableTest execute() throws GridException {
        final GridJunit3SerializableTest testArg = getArgument();

        TestResult collector = new TestResult();

        collector.addListener(new TestListener() {
            /** */
            private GridTestPrintStream out = null;

            /** */
            private GridTestPrintStream err = null;

            /**
             * {@inheritDoc}
             */
            public void addError(Test test, Throwable e) {
                assert test instanceof TestCase == true : "ASSERTION [line=69, file=src/java/org/gridgain/grid/test/junit3/GridJunit3Job.java]. " + "Errors can be added only to TestCases: " + test;

                testArg.findTestCase((TestCase)test).setError(e);
            }

            /**
             * {@inheritDoc}
             */
            public void addFailure(Test test, AssertionFailedError e) {
                assert test instanceof TestCase == true : "ASSERTION [line=78, file=src/java/org/gridgain/grid/test/junit3/GridJunit3Job.java]. " + "Failures can be added only to TestCases: " + test;

                testArg.findTestCase((TestCase)test).setFailure(e);
            }

            /**
             * {@inheritDoc}
             */
            public void startTest(Test test) {
                GridTestPrintStreamFactory.getStdOut().println("Distributed test started: " + getTestName(test));

                out = GridTestPrintStreamFactory.acquireOut();
                err = GridTestPrintStreamFactory.acquireErr();
            }

            /**
             * {@inheritDoc}
             */
            public void endTest(Test test) {
                GridJunit3SerializableTestCase testCase = testArg.findTestCase((TestCase)test);

                try {
                    testCase.setStandardOut(getBytes(out));
                    testCase.setStandardError(getBytes(err));
                }
                catch (IOException e) {
                    log.error("Error resetting output.", e);

                    if (testCase.getError() == null) {
                        testCase.setError(e);
                    }
                    else if (testCase.getFailure() == null) {
                        testCase.setFailure(e);
                    }
                    else {
                        // Override initial error.
                        testCase.setError(e);
                    }
                }

                GridTestPrintStreamFactory.releaseOut();
                GridTestPrintStreamFactory.releaseErr();

                out = null;
                err = null;

                GridTestPrintStreamFactory.getStdOut().println("Distributed test finished: " + getTestName(test));
            }

            /**
             *
             * @param out FIXDOC
             * @return Output bytes.
             * @throws IOException FIXDOC
             */
            private byte[] getBytes(GridTestPrintStream out) throws IOException {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

                out.purge(byteOut);

                return byteOut.toByteArray();
            }

            /**
             *
             * @param test JUnit3 test.
             * @return Test name.
             */
            private String getTestName(Test test) {
                if (test instanceof TestSuite == true) {
                    return ((TestSuite)test).getName();
                }

                return test.getClass().getName() + '.' + ((TestCase)test).getName();
            }
        });

        // Run Junits.
        testArg.getTest().run(collector);

        return testArg;
    }
}
