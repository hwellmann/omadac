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

import java.io.*;

import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;

/**
 * Provides functionality for marshal and unmarshal objects. This helper should
 * be used for all marshal and unmarshal operations for buffers and streams.
 * 
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridMarshalHelper {
    /** Default initial buffer size for the {@link GridByteArrayOutputStream}. */
    public static final int DFLT_BUFFER_SIZE = 512;
    
    /**
     * Enforces singleton.
     */
    private GridMarshalHelper() {
        // No-op.
    }

    /**
     * Marshals object to a {@link GridByteArrayList} using given {@link GridMarshaller}.
     * 
     * @param marshaller Marshaller.
     * @param obj Object to marshal.
     * @return Buffer that contains obtained byte array.
     * @throws GridException If marshalling failed.
     */
    public static GridByteArrayList marshal(GridMarshaller marshaller, Object obj) throws GridException {
        assert marshaller != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/util/GridMarshalHelper.java]";
        
        GridByteArrayOutputStream out = null;
        
        try {
            out = new GridByteArrayOutputStream(DFLT_BUFFER_SIZE);
        
            marshaller.marshal(obj, out);
        
            return out.toByteArrayList();
        }
        finally {
            GridUtils.close(out, null);
        }
    }
    
    /**
     * Marshals object to a output stream using given {@link GridMarshaller}.
     * 
     * @param marshaller Marshaller.
     * @param obj Object to marshal.
     * @param out Output stream to marshal into.
     * @throws GridException If marshalling failed.
     */
    public static void marshal(GridMarshaller marshaller, Object obj, OutputStream out) throws GridException {
        assert marshaller != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/util/GridMarshalHelper.java]";

        marshaller.marshal(obj, out);
    }
    
    /**
     * Unmarshals object from a {@link GridByteArrayList} using given class loader. 
     * 
     * @param <T> Type of unmarshalled object.
     * @param marshaller Marshaller.
     * @param buf Buffer that contains byte array with marshalled object.
     * @param clsLoader Class loader to use.
     * @return Unmarshalled object.
     * @throws GridException If marshalling failed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(GridMarshaller marshaller, GridByteArrayList buf, ClassLoader clsLoader) 
        throws GridException {
        assert marshaller != null : "ASSERTION [line=99, file=src/java/org/gridgain/grid/util/GridMarshalHelper.java]";
        assert buf != null : "ASSERTION [line=100, file=src/java/org/gridgain/grid/util/GridMarshalHelper.java]";

        ByteArrayInputStream in = null;
        
        try {
            in = new ByteArrayInputStream(buf.getArray(), 0, buf.getSize());
            
            return (T)marshaller.unmarshal(in, clsLoader);
        }
        finally {
            GridUtils.close(in, null);
        }
    }
    
    /**
     * Unmarshals object from a input stream using given class loader.
     * 
     * @param <T> Type of unmarshalled object.
     * @param marshaller Marshaller.
     * @param in Input stream that provides marshalled object bytes.
     * @param clsLoader Class loader to use.
     * @return Unmarshalled object.
     * @throws GridException If marshalling failed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(GridMarshaller marshaller, InputStream in, ClassLoader clsLoader) 
        throws GridException {
        assert marshaller != null : "ASSERTION [line=127, file=src/java/org/gridgain/grid/util/GridMarshalHelper.java]";

        return (T)marshaller.unmarshal(in, clsLoader);
    }
}
