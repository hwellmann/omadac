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

package org.gridgain.grid.tools.ant.preparer;

import java.io.*;

/**
 * Wrapper for input buffer reader.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridSourceInput {
    /** I/O reader. */
    private final BufferedReader reader;

    /** File name. */
    private final String fileName;

    /** Line number. */
    private int line = 0;

    /**
     * Creates new descriptor with given parameters.
     *
     * @param reader Buffered reader.
     * @param fileName File name.
     */
    GridSourceInput(BufferedReader reader, String fileName) {
        assert reader != null : "ASSERTION [line=49, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceInput.java]";
        assert fileName != null : "ASSERTION [line=50, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceInput.java]";

        this.reader = reader;
        this.fileName = fileName;
    }

    /**
     * Gets file name.
     *
     * @return File name.
     */
    String getFileName() {
        return fileName;
    }

    /**
     * Gets line number.
     *
     * @return Line number.
     */
    int getLine() {
        return line;
    }

    /**
     * Reads line.
     *
     * @return Read line.
     * @throws IOException Thrown in case of any error.
     */
    String readLine() throws IOException {
        line++;

        return reader.readLine();
    }
}
