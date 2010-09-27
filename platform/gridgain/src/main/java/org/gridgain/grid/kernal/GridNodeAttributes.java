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

package org.gridgain.grid.kernal;

/**
 * This class defines constants (NOT enums) for internal node attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridNodeAttributes {
    /** Prefix for internally reserved attribute names. */
    static final String ATTR_PREFIX = "org.gridgain";

    /** Internal attribute name constant. */
    public static final String ATTR_BUILD_VER = ATTR_PREFIX + ".build.ver";

    /** Internal attribute name constant. */
    public static final String ATTR_JIT_NAME = ATTR_PREFIX + ".jit.name";

    /** Internal attribute name constant. */
    public static final String ATTR_USER_NAME = ATTR_PREFIX + ".user.name";

    /** Internal attribute name constant. */
    public static final String ATTR_GRID_NAME = ATTR_PREFIX + ".grid.name";

    /** Deployment mode. */
    public static final String ATTR_DEPLOYMENT_MODE = ATTR_PREFIX + ".grid.dep.mode";

    /** Internal attribute name postfix constant. */
    public static final String ATTR_SPI_VER = ATTR_PREFIX + ".spi.ver";

    /** Internal attribute name postfix constant. */
    public static final String ATTR_SPI_CLASS = ATTR_PREFIX + ".spi.class";

    /**
     * Enforces singleton.
     */
    private GridNodeAttributes() {
        // No-op.
    }
}
