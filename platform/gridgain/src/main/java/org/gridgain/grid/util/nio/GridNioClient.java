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

package org.gridgain.grid.util.nio;

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;
import java.net.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridNioClient {
    /** */
    private final Socket sock;

    /** Cached byte buffer. */
    private final GridByteArrayList bytes = new GridByteArrayList(512);

    /** Grid logger. */
    private final GridLogger log;

    /** Time when this client was last used. */
    private long lastUsed = System.currentTimeMillis();

    /**
     *
     * @param addr FIXDOC
     * @param port FIXDOC
     * @param localHost FIXDOC
     * @param log FIXDOC
     * @throws GridException FIXDOC
     */
    public GridNioClient(InetAddress addr, int port, InetAddress localHost, GridLogger log) throws GridException {
        assert addr != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/util/nio/GridNioClient.java]";
        assert port > 0 && port < 0xffff : "ASSERTION [line=61, file=src/java/org/gridgain/grid/util/nio/GridNioClient.java]";
        assert localHost != null : "ASSERTION [line=62, file=src/java/org/gridgain/grid/util/nio/GridNioClient.java]";
        assert log != null : "ASSERTION [line=63, file=src/java/org/gridgain/grid/util/nio/GridNioClient.java]";

        try {
            sock = new Socket(addr, port, localHost, 0);
        }
        catch (IOException e) {
            throw (GridException)new GridException("Failed to connect to remote host [addr=" + addr + ", port=" + port +
                ", localHost=" + localHost + ']', e).setData(69, "src/java/org/gridgain/grid/util/nio/GridNioClient.java");
        }

        this.log = log;
    }

    /**
     *
     */
    public synchronized void close() {
        if (log.isDebugEnabled() == true) {
            log.debug("Closing client: " + this);
        }

        GridUtils.close(sock, log);
    }

    /**
     * Gets idle time of this client.
     *
     * @return Idle time of this client.
     */
    public synchronized long getIdleTime() {
        return System.currentTimeMillis() - lastUsed;
    }

    /**
     *
     * @param data Data to send.
     * @param len Size of data in bytes.
     * @throws GridException FIXDOC
     */
    public synchronized void sendMessage(byte[] data, int len) throws GridException {
        lastUsed = System.currentTimeMillis();

        bytes.reset();

        // Allocate 4 bytes for size.
        bytes.add(len);
        
        bytes.add(data, 0, len);

        try {
            // We assume that this call does not return until the message
            // is fully sent.
            sock.getOutputStream().write(bytes.getInternalArray(), 0, bytes.getSize());
        }
        catch (IOException e) {
            throw (GridException)new GridException("Failed to send message to remote node: " + sock.getRemoteSocketAddress(), e).setData(118, "src/java/org/gridgain/grid/util/nio/GridNioClient.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridNioClient.class, this);
    }
}
