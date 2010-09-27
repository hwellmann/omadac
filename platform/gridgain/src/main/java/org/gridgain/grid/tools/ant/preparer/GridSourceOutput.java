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
 * Wrapper for prepared source writer.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridSourceOutput {
    /** Writer. */
    private final Writer writer;

    /** Line number. */
    private int line = 1;

    /**
     * Creates wrapper with given writer.
     *
     * @param writer Writer.
     */
    GridSourceOutput(Writer writer) {
        assert writer != null : "ASSERTION [line=45, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceOutput.java]";

        this.writer = writer;
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
     * Writes given line out.
     *
     * @param s Line to write out.
     * @throws IOException Thrown in case of any error.
     */
    void writeLine(String s) throws IOException {
        writer.write(s);
        writer.write(GridSourceAntTask.NL);

        line++;
    }
}
