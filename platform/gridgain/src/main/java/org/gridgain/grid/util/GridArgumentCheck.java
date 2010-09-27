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

import org.gridgain.apache.*;

/**
 * This class encapsulates argument check (null and range) for public facing APIs.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public final class GridArgumentCheck {
    /**
     * Enforces singleton.
     */
    private GridArgumentCheck() {
        // No-op.
    }

    /**
     * Checks if given argument value is <tt>null</tt>. If so - throws {@link NullPointerException}.
     *
     * @param argVal Argument value to check.
     * @param argName Name of the argument in the code (used in error message).
     */
    @SuppressWarnings({"ProhibitedExceptionThrown"})
    public static void checkNull(Object argVal, String argName) {
        if (argVal == null) {
            throw new NullPointerException("Argument cannot be null: " + argName);
        }
    }

    /**
     * Checks if given argument's range is equal to <tt>true</tt>, otherwise
     * throws {@link IllegalArgumentException} exception.
     *
     * @param argRange Argument's value range to check.
     * @param rangeDesc Description of the range checked to be used in error message.
     */
    public static void checkRange(boolean argRange, String rangeDesc) {
        if (argRange == false) {
            throw new IllegalArgumentException("Argument value is not in valid range: " + rangeDesc);
        }
    }
}
