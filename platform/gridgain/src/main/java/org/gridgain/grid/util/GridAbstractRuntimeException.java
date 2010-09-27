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

package org.gridgain.grid.util;

import java.io.*;

import org.gridgain.apache.*;

/**
 * This exception provides common functionality for all Grid exceptions.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public abstract class GridAbstractRuntimeException extends RuntimeException {
    /** Constant for unknown line. */
    protected static final int UNKNOWN_LINE = -1;

    /** System line separator. */
    private static final String NL = System.getProperty("line.separator");

    /** Lie of code where exception happened. */
    private int line = UNKNOWN_LINE;

    /** File where exception happened. */
    private String file = null;

    /**
     * Creates new exception with given error message.
     *
     * @param msg Error message.
     */
    protected GridAbstractRuntimeException(String msg) {
        super(msg);
    }

    /**
     * Creates new exception with given error message and optional nested
     * exception.
     *
     * @param msg Error message.
     * @param cause Optional nested exception (can be <tt>null</tt>).
     */
    protected GridAbstractRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Sets information where exception has been thrown.
     *
     * @param line Line in file.
     * @param file File name.
     * @return Exception object.
     */
    public GridAbstractRuntimeException setData(int line, String file) {
        this.line = line;
        this.file = file;

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace() {
        System.err.println(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintStream s) {
        s.println(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintWriter s) {
        s.println(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (Throwable e = this; e != null; e = e.getCause()) {
            buf.append(NL);

            // Reference equality is intended.
            //noinspection ObjectEquality
            buf.append(e == this ? "Exception:" : "Caused By:").append(NL);
            buf.append("----------").append(NL);

            if (e instanceof AssertionError == true) {
                buf.append("************************************************************").append(NL);
                buf.append("***                                                      ***").append(NL);
                buf.append("*** PLEASE SEND THIS STACK TRACE TO SUPPORT@GRIDGAIN.COM ***").append(NL);
                buf.append("***             HELP US IMPROVE GRIDGAIN!                ***").append(NL);
                buf.append("***                                                      ***").append(NL);
                buf.append("************************************************************").append(NL);
            }

            buf.append(">>> Type: ").append(e.getClass().getName()).append(NL);
            buf.append(">>> Message: ").append(e.getMessage()).append(NL);
            buf.append(">>> Documentation: http://www.gridgain.com/product.html").append(NL);

            // Add source file and line if nested exception is ours too.
            if (e instanceof GridAbstractRuntimeException == true) {
                GridAbstractRuntimeException e1 = (GridAbstractRuntimeException)e;

                // Don't pollute output with 'null's.
                if (e1.file != null) {
                    buf.append(">>> Source file: ").append(e1.file).append(NL);
                    buf.append(">>> Line number: ").append(e1.line).append(NL);
                }
            }

            StackTraceElement[] elems = e.getStackTrace();

            if (elems != null && elems.length > 0) {
                buf.append(">>> Stack trace: ").append(NL);

                for (StackTraceElement elem : elems) {
                    buf.append(">>>     at ");
                    buf.append(elem.getClassName());
                    buf.append('.');
                    buf.append(elem.getMethodName());
                    buf.append('(');
                    buf.append(elem.getFileName());
                    buf.append(':');
                    buf.append(Math.max(1, elem.getLineNumber()));
                    buf.append(')');
                    buf.append(NL);
                }
            }
        }

        return buf.toString();
    }
}
