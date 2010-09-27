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

package org.gridgain.grid.marshaller.jdk;

import java.io.*;
import org.gridgain.grid.marshaller.*;

/**
 * This class defines own object output stream.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJdkMarshallerObjectOutputStream extends ObjectOutputStream {
    /**
     *
     * @param out Output stream.
     * @throws IOException Thrown in case of any I/O errors.
     */
    GridJdkMarshallerObjectOutputStream(OutputStream out) throws IOException {
        super(out);

        enableReplaceObject(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object replaceObject(final Object obj) throws IOException {
        if (obj != null && isExcluded(obj) == true) {
            return null;
        }

        if (obj != null && obj.getClass().equals(Object.class) == true) {
            return new GridJdkMarshallerDummySerializable();
        }

        return super.replaceObject(obj);
    }

    /**
     * Checks if passed in object is excluded resource.
     *
     * @param obj Object to check.
     * @return <tt>true</tt> if object is excluded.
     */
    private boolean isExcluded(Object obj) {
        assert obj != null : "ASSERTION [line=68, file=src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshallerObjectOutputStream.java]";

        for (Class<?> cls : GridMarshaller.EXCLUDED_GRID_CLASSES) {
            if (cls.isAssignableFrom(obj.getClass()) == true) {
                return true;
            }
        }

        return false;
    }
}

