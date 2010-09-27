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

package org.gridgain.grid.util.mail.outbox;

import java.io.*;
import javax.activation.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides implementation for {@link DataSource} based
 * on byte array data.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMailOutboxDataSource implements DataSource {
    /** Data content type. */
    private static final String CONTENT_TYPE = "application/octet-stream";

    /** Data source name. */
    private final String name;

    /** Byte array presentation of data object. */
    private byte[] arr = null;

    /**
     * Create new data source based on given byte array.
     *
     * @param name Name of data source.
     * @param arr Data to get raw data.
     */
    public GridMailOutboxDataSource(String name, byte[] arr) {
        assert arr != null : "ASSERTION [line=52, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxDataSource.java]";
        assert name != null : "ASSERTION [line=53, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxDataSource.java]";

        this.name = name;
        this.arr = arr;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(arr);
    }

    /**
     * {@inheritDoc}
     */
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Unsupported operation.");
    }

    /**
     * {@inheritDoc}
     */
    public String getContentType() {
        return CONTENT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailOutboxDataSource.class, this);
    }
}
