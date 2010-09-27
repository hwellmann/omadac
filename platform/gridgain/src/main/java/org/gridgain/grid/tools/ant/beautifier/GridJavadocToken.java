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

package org.gridgain.grid.tools.ant.beautifier;

/**
 * Lexical token.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJavadocToken {
    /** Token type. */
    private final GridJavadocTokenType type;

    /** Token value. */
    private String val = null;

    /**
     * Creates token.
     *
     * @param type Token type.
     * @param val Token value.
     */
    GridJavadocToken(GridJavadocTokenType type, String val) {
        assert type != null : "ASSERTION [line=44, file=src/java/org/gridgain/grid/tools/ant/beautifier/GridJavadocToken.java]";
        assert val != null : "ASSERTION [line=45, file=src/java/org/gridgain/grid/tools/ant/beautifier/GridJavadocToken.java]";

        this.type = type;
        this.val = val;
    }

    /**
     * Gets token type.
     *
     * @return Token type.
     */
    GridJavadocTokenType getType() {
        return type;
    }

    /**
     * Gets token value.
     *
     * @return Token value.
     */
    String getValue() {
        return val;
    }

    /**
     * Sets new token value.
     *
     * @param val New token value.
     */
    void setUpdatedValue(String val) {
        this.val = val;
    }
}
