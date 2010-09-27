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

package org.gridgain.grid;

import org.gridgain.apache.*;

/**
 * This exception is thrown when user's code throws undeclared runtime exception. By user core it is 
 * assumed the code in grid task, grid job or SPI. In most cases it should be an indication of unrecoverable
 * error condition such as assertion, {@link NullPointerException}, {@link OutOfMemoryError}, etc.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public class GridUserUndeclaredException extends GridException {
    /**
     * Creates user undeclared exception with given task execution ID and
     * error message.
     *
     * @param msg Error message.
     */
    public GridUserUndeclaredException(String msg) {
        super(msg);
    }

    /**
     * Creates new user undeclared exception given throwable as a cause and 
     * source of error message.
     * 
     * @param cause Non-null throwable cause.
     */
    public GridUserUndeclaredException(Throwable cause) {
        this(cause.getMessage(), cause);
    } 
    
    /**
     * Creates user undeclared exception with given task execution ID,
     * error message and optional nested exception.
     *
     * @param msg Error message.
     * @param cause Optional nested exception (can be <tt>null</tt>).
     */
    public GridUserUndeclaredException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
