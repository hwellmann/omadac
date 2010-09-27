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
import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit3SerializableTestCase implements GridJunit3SerializableTest {
    /** */
    private final String name;

    /** */
    private final Class<? extends TestCase> testCls;

    /** */
    private Throwable error = null;

    /** */
    private Throwable failure = null;

    /** */
    private byte[] stdOut = null;

    /** */
    private byte[] stdErr = null;

    /** */
    private transient TestCase test = null;

    /**
     *
     * @param test Test case.
     */
    GridJunit3SerializableTestCase(TestCase test) {
        assert test != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/test/junit3/GridJunit3SerializableTestCase.java]";

        this.test = test;

        name = test.getName();

        //noinspection CastToIncompatibleInterface
        testCls = ((GridJunit3TestCaseProxy)test).getGridGainJuni3OriginalTestCase().getClass();
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
    public Test getTest() {
        // Initialize test on deserialization.
        if (test == null) {
            test = GridJunit3Utils.createTest(name, testCls);
        }

        return test;
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends TestCase> getTestClass() {
        return testCls;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"ObjectEquality"})
    public GridJunit3SerializableTestCase findTestCase(TestCase t) {
        return test == t ? this : null;
    }

    /**
     * {@inheritDoc}
     */
    public void setResult(GridJunit3SerializableTest res) {
        GridJunit3SerializableTestCase test = (GridJunit3SerializableTestCase)res;

        setError(test.getError());
        setFailure(test.getFailure());

        //noinspection CastToIncompatibleInterface
        GridJunit3TestCaseProxy proxy = (GridJunit3TestCaseProxy)this.test;

        proxy.setGridGainJunit3Result(test.getStandardOut(), test.getStandardError(), test.getError(),
            test.getFailure());
    }

    /**
     *
     * @param failure Throwable to set.
     */
    void setFailure(Throwable failure) {
        this.failure = failure;
    }

    /**
     *
     * @param error Error to set.
     */
    void setError(Throwable error) {
        this.error = error;
    }

    /**
     *
     * @param stdOut Standard output to set.
     */
    public void setStandardOut(byte[] stdOut) {
        this.stdOut = stdOut;
    }

    /**
     *
     * @param stdErr Standard error output to set.
     */
    public void setStandardError(byte[] stdErr) {
        this.stdErr = stdErr;
    }

    /**
     *
     * @return List of failures that occurred.
     */
    Throwable getFailure() {
        return failure;
    }

    /**
     *
     * @return List of errors that occurred.
     */
    Throwable getError() {
        return error;
    }

    /**
     *
     * @return Standard output.
     */
    public byte[] getStandardOut() {
        return stdOut;
    }

    /**
     *
     * @return Error output.
     */
    public byte[] getStandardError() {
        return stdErr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJunit3SerializableTestCase.class, this);
    }
}
