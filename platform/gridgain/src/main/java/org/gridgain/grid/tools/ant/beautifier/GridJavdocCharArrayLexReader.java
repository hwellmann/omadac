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
 * Character-based lexical token reader.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJavdocCharArrayLexReader {
    /** End Of File (EOF) constant. */
    public static final char EOF = (char)-1;

    /** Character-based buffer. */
    private char[] chars = null;

    /** Index in {@link #chars}. */
    private int index = 0;

    /**
     * Creates reader with given buffer.
     *
     * @param chars Input character buffer.
     */
    GridJavdocCharArrayLexReader(char[] chars) {
        this.chars = chars;
    }

    /**
     * Gets length of the buffer.
     *
     * @return Length if the buffer.
     */
    int getLength() {
        return chars.length;
    }

    /**
     * Reads next character.
     *
     * @return Next character from the buffer.
     */
    int read() {
        return index == chars.length ? EOF : chars[index++];
    }

    /**
     * Peeks at the next character in the buffer.
     *
     * @return Next character that will be returned by next {@link #read()} call.
     */
    int peek() {
        return index == chars.length ? EOF : chars[index];
    }

    /**
     * Skips next character in the buffer.
     */
    void skip() {
        if (index < chars.length) {
            index++;
        }
    }

    /**
     * Puts back last read character.
     */
    void back() {
        if (index > 0) {
            index--;
        }
    }

    /**
     * Tests whether buffer has more characters.
     *
     * @return <tt>true</tt> if buffer has at least one more character - <tt>false</tt> otherwise.
     */
    boolean hasMore() {
        return index < chars.length;
    }
}
