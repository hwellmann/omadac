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
 * This exception is used to indicate error with grid topology (e.g., crashed node, etc.).
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public class GridTopologyException extends GridException {
    /**
     * Creates new topology exception with given error message.
     *
     * @param msg Error message.
     */
    public GridTopologyException(String msg) {
        super(msg);
    }

    /**
     * Creates new topology exception with given error message and optional 
     * nested exception.
     *
     * @param msg Error message.
     * @param cause Optional nested exception (can be <tt>null</tt>).
     */
    public GridTopologyException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
