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
 * Wrapper for {@link InputStream}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJdkMarshallerInputStreamWrapper extends InputStream {
    /** */
    private InputStream in = null;

    /**
     * Creates wrapper.
     *
     * @param in Wrapped input stream
     */
    GridJdkMarshallerInputStreamWrapper(InputStream in) {
        assert in != null : "ASSERTION [line=42, file=src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshallerInputStreamWrapper.java]";

        this.in = in;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        return in.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"NonSynchronizedMethodOverridesSynchronizedMethod"})
    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"NonSynchronizedMethodOverridesSynchronizedMethod"})
    @Override
    public void reset() throws IOException {
        in.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
