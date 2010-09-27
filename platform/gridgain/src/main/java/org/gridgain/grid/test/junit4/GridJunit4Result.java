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
import org.gridgain.grid.util.tostring.*;

/**
 * Junit4 test result.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit4Result implements Serializable {
    /** */
    private final String name;

    /** */
    private byte[] stdOut = null;

    /** */
    private byte[] stdErr = null;

    /** */
    private boolean ignored = false;

    /** */
    private Throwable failure = null;

    /**
     * @param name Test name.
     */
    GridJunit4Result(String name) {
        assert name != null : "ASSERTION [line=52, file=src/java/org/gridgain/grid/test/junit4/GridJunit4Result.java]";

        this.name = name;
    }

    /**
     *
     * @param name Test name
     * @param stdOut Standard output from test.
     * @param stdErr Standard error from test.
     * @param failure Test failure.
     */
    GridJunit4Result(String name, byte[] stdOut, byte[] stdErr, Throwable failure) {
        assert name != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/test/junit4/GridJunit4Result.java]";

        this.name = name;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
        this.failure = failure;
    }

    /**
     *
     * @return Test name.
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return Standard out.
     */
    public byte[] getStdOut() {
        return stdOut;
    }

    /**
     *
     * @param stdOut Standard out.
     */
    public void setStdOut(byte[] stdOut) {
        this.stdOut = stdOut;
    }

    /**
     *
     * @return Standard error.
     */
    public byte[] getStdErr() {
        return stdErr;
    }

    /**
     *
     * @param stdErr Standard error.
     */
    public void setStdErr(byte[] stdErr) {
        this.stdErr = stdErr;
    }

    /**
     *
     * @return Test failure.
     */
    public Throwable getFailure() {
        return failure;
    }

    /**
     *
     * @param failure Test failure.
     */
    public void setFailure(Throwable failure) {
        this.failure = failure;
    }

    /**
     *
     * @return <tt>True</tt> if test is ignored.
     */
    public boolean isIgnored() {
        return ignored;
    }

    /**
     *
     * @param ignored Ignored test flag.
     */
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJunit4Result.class, this);
    }
}
