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

package org.gridgain.grid.util.test;

import java.io.*;

/**
 * Factory that allow to acquire/release Print Stream for test logging.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridTestPrintStreamFactory {
    /** */
    private static final PrintStream sysOut = System.out;

    /** */
    private static final PrintStream sysErr = System.err;

    /** */
    private static GridTestPrintStream testOut = null;

    /** */
    private static GridTestPrintStream testErr = null;

    /** */
    private static long outCnt = 0;

    /** */
    private static long errCnt = 0;

    /**
     * Enforces singleton.
     */
    private GridTestPrintStreamFactory() {
        // No-op.
    }

    /**
     * Gets original standard out.
     *
     * @return Original standard out.
     */
    public static synchronized PrintStream getStdOut() {
        return sysOut;
    }

    /**
     * Gets original standard error.
     *
     * @return Original standard error.
     */
    public static synchronized PrintStream getStdErr() {
        return sysErr;
    }

    /**
     * Acquires output stream for loging tests.
     *
     * @return Junit out print stream.
     */
    public static synchronized GridTestPrintStream acquireOut() {
        // Lazy initialization is required here to ensure that parent
        // thread group is picked off correctly by implementation.
        if (testOut == null) {
            testOut = new GridTestPrintStream(sysOut);
        }

        if (outCnt == 0) {
            System.setOut(testOut);
        }

        outCnt++;

        return testOut;
    }

    /**
     * Acquires output stream for logging errors in tests.
     *
     * @return Junit error print stream.
     */
    public static synchronized GridTestPrintStream acquireErr() {
        // Lazy initialization is required here to ensure that parent
        // thread group is picked off correctly by implementation.
        if (testErr == null) {
            testErr = new GridTestPrintStream(sysErr);
        }

        if (errCnt == 0) {
            System.setErr(testErr);
        }

        errCnt++;

        return testErr;
    }

    /**
     * Releases standard out. If there are no more acquired standard outs,
     * then it is reset to its original value.
     */
    public static synchronized void releaseOut() {
        outCnt--;

        if (outCnt == 0) {
            System.setOut(sysOut);
        }
    }

    /**
     * Releases standard error. If there are no more acquired standard errors,
     * then it is reset to its original value.
     */
    public static synchronized void releaseErr() {
        errCnt--;

        if (errCnt == 0) {
            System.setErr(sysErr);
        }
    }
}
