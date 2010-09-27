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

package org.gridgain.grid.gridify;

import org.gridgain.grid.*;

/**
 * This defines gridify exception. This runtime exception gets thrown out of gridified
 * methods in case if method execution resulted in undeclared exception.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridifyRuntimeException extends GridRuntimeException {
    /**
     * Creates new gridify runtime exception with specified message.
     *
     * @param msg Exception message.
     */
    public GridifyRuntimeException(String msg) {
        super(msg);
    }

    /**
     * Creates new gridify runtime exception given throwable as a cause and 
     * source of error message.
     * 
     * @param cause Non-null throwable cause.
     */
    public GridifyRuntimeException(Throwable cause) {
        this(cause.getMessage(), cause);
    } 
    
    /**
     * Creates new gridify runtime exception with specified message and cause.
     *
     * @param msg Exception message.
     * @param cause Exception cause.
     */
    public GridifyRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
