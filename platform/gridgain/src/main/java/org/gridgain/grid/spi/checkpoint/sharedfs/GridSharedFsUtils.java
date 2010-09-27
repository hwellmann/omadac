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

package org.gridgain.grid.spi.checkpoint.sharedfs;

import java.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.*;

/**
 * Utility class that helps to manage files. It provides read/write
 * methods that simplify file operations.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridSharedFsUtils {
    /**
     * Enforces singleton.
     */
    private GridSharedFsUtils() {
        // No-op.
    }

    /**
     * Reads all checkpoint data from given file. File is read as binary
     * data which are than deserialized.
     *
     * @param file File which contains checkpoint data.
     * @param marshaller Grid marshaller.
     * @param log Messages logger.
     * @return Checkpoint data object read from given file.
     * @throws GridException Thrown if data could not be converted
     *    to {@link GridSharedFsCheckpointData} object.
     * @throws IOException Thrown if file read error occurred.
     */
    static GridSharedFsCheckpointData read(File file, GridMarshaller marshaller, GridLogger log)
        throws IOException, GridException {
        assert file != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsUtils.java]";
        assert log != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsUtils.java]";

        FileInputStream inputStream = new FileInputStream(file);

        try {
            return (GridSharedFsCheckpointData)GridMarshalHelper.unmarshal(marshaller, inputStream,
                GridSharedFsCheckpointData.class.getClassLoader());
        }
        finally {
            GridUtils.close(inputStream, log);
        }
    }

    /**
     * Writes given checkpoint data to a given file. Data are serialized to
     * the binary tream and saved to the file.
     *
     * @param file  File data should be saved to.
     * @param data  Checkpoint data.
     * @param marshaller Grid marshaller.
     * @param log   Messages logger.
     * @throws GridException Thrown if data could not be marshalled.
     * @throws IOException Thrown if file write operation failed.
     */
    static void write(File file, GridSharedFsCheckpointData data, GridMarshaller marshaller, GridLogger log)
        throws IOException, GridException {
        assert file != null : "ASSERTION [line=86, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsUtils.java]";
        assert data != null : "ASSERTION [line=87, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsUtils.java]";
        assert log != null : "ASSERTION [line=88, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsUtils.java]";

        OutputStream out = null;

        try {
            out = new FileOutputStream(file);

            GridMarshalHelper.marshal(marshaller, data, out);
        }
        finally {
            GridUtils.close(out, log);
        }
    }
}
