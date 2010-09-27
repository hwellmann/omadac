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

/**
 * This class defines own object input stream.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJdkMarshallerObjectInputStream extends ObjectInputStream {
    /** */
    private final ClassLoader clsLoader;

    /**
     * @param in Parent input stream.
     * @param clsLoader Custom class loader.
     * @throws IOException If initialization failed.
     */
    GridJdkMarshallerObjectInputStream(InputStream in, ClassLoader clsLoader) throws IOException {
        super(in);

        assert clsLoader != null : "ASSERTION [line=44, file=src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshallerObjectInputStream.java]";

        this.clsLoader = clsLoader;

        enableResolveObject(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        // NOTE: DO NOT CHANGE TO 'clsLoader.loadClass()'
        // Must have 'Class.forName()' instead of clsLoader.loadClass()
        // due to weird ClassNotFoundExceptions for arrays of classes
        // in certain cases.
        return Class.forName(desc.getName(), true, clsLoader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object resolveObject(Object obj) throws IOException {
        if (obj != null && obj.getClass().equals(GridJdkMarshallerDummySerializable.class) == true) {
            return new Object();
        }

        return super.resolveObject(obj);
    }
}

