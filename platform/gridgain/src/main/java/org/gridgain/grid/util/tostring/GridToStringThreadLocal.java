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

package org.gridgain.grid.util.tostring;

import org.gridgain.apache.*;

/**
 * FIXDOC: provide class description here.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 */
@Apache20LicenseCompatible
class GridToStringThreadLocal {
    /** */
    private StringBuilder buf = new StringBuilder(256);
    
    /** */
    private Object[] addNames = new Object[4];
    
    /** */
    private Object[] addVals = new Object[4];

    /**
     * @return FIXDOC
     */
    StringBuilder getStringBuilder() {
        return buf;
    }

    /**
     * @return FIXDOC
     */
    Object[] getAdditionalNames() {
        return addNames;
    }

    /**
     * @return FIXDOC
     */
    Object[] getAdditionalValues() {
        return addVals;
    }
}
