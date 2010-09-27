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

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.util.*;
import java.util.concurrent.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.runnable.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridNioServer {
    /** */
    private final Selector selector;

    /** */
    private final GridNioServerListener listener;

    /** */
    private final GridLogger log;

    /** Buffer for reading. */
    private final ByteBuffer readBuf;

    /** */
    private final String gridName;

    /** */
    private final GridRunnablePool workerPool;

    /** */
    private volatile boolean closed = false;

    /**
     *
     * @param addr FIXDOC
     * @param port FIXDOC
     * @param listener FIXDOC
     * @param log FIXDOC
     * @param exec FIXDOC
     * @param gridName FIXDOC
     * @param directBuf FIXDOC
     * @throws GridException FIXDOC
     */
    public GridNioServer(InetAddress addr, int port, GridNioServerListener listener, GridLogger log,
        Executor exec, String gridName, boolean directBuf)
        throws GridException {
        assert addr != null : "ASSERTION [line=78, file=src/java/org/gridgain/grid/util/nio/GridNioServer.java]";
        assert port > 0 && port < 0xffff : "ASSERTION [line=79, file=src/java/org/gridgain/grid/util/nio/GridNioServer.java]";
        assert listener != null : "ASSERTION [line=80, file=src/java/org/gridgain/grid/util/nio/GridNioServer.java]";
        assert log != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/util/nio/GridNioServer.java]";
        assert exec != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/util/nio/GridNioServer.java]";

        this.listener = listener;
        this.log = log;
        this.gridName = gridName;

        workerPool = new GridRunnablePool(exec, log);

        readBuf = directBuf == true ? ByteBuffer.allocateDirect(8 << 10) : ByteBuffer.allocate(8 << 10);

        selector = createSelector(addr, port);
    }

    /**
     * Creates selector.
     *
     * @param addr Local address to listen on.
     * @param port Local port to listen on.
     * @return Created selector.
     * @throws GridException If selector could not be created.
     */
    private Selector createSelector(InetAddress addr, int port) throws GridException {
        Selector selector = null;

        ServerSocketChannel srvrCh = null;

        try {
            // Create a new selector
            selector = SelectorProvider.provider().openSelector();

            // Create a new non-blocking server socket channel
            srvrCh = ServerSocketChannel.open();

            srvrCh.configureBlocking(false);

            // Bind the server socket to the specified address and port
            srvrCh.socket().bind(new InetSocketAddress(addr, port));

            // Register the server socket channel, indicating an interest in
            // accepting new connections
            srvrCh.register(selector, SelectionKey.OP_ACCEPT);

            return selector;
        }
        catch (IOException e) {
            GridUtils.close(srvrCh, log);
            GridUtils.close(selector, log);

            throw (GridException)new GridException("Failed to initialize NIO selector.", e).setData(130, "src/java/org/gridgain/grid/util/nio/GridNioServer.java");
        }
    }

    /**
     *
     */
    public void close() {
        if (closed == false) {
            closed = true;

            selector.wakeup();
        }
    }

    /**
     *
     * @throws GridException FIXDOC
     */
    public void accept() throws GridException {
        if (closed == true) {
            throw (GridException)new GridException("Attempt to use closed nio server.").setData(151, "src/java/org/gridgain/grid/util/nio/GridNioServer.java");
        }

        try {
            while (closed == false && selector.isOpen() == true) {
                // Wake up every 2 seconds to check if closed.
                if (selector.select(2000) > 0) {
                    // Walk through the ready keys collection and process date requests.
                    for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); iter.hasNext() == true;) {
                        SelectionKey key = iter.next();

                        iter.remove();

                        // Was key closed?
                        if (key.isValid() == false) {
                            continue;
                        }

                        if (key.isAcceptable() == true) {
                            // The key indexes into the selector so we
                            // can retrieve the socket that's ready for I/O
                            ServerSocketChannel srvrCh = (ServerSocketChannel)key.channel();

                            SocketChannel sockCh = srvrCh.accept();

                            sockCh.configureBlocking(false);

                            sockCh.register(selector, SelectionKey.OP_READ, new GridNioServerBuffer());

                            if (log.isDebugEnabled() == true) {
                                log.debug("Accepted new client connection: " + sockCh.socket().getRemoteSocketAddress());
                            }
                        }
                        else if (key.isReadable() == true) {
                            SocketChannel sockCh = (SocketChannel)key.channel();

                            SocketAddress rmtAddr = sockCh.socket().getRemoteSocketAddress();

                            try {
                                // Reset buffer to read bytes up to its capacity.
                                readBuf.clear();

                                // Attempt to read off the channel
                                int cnt = sockCh.read(readBuf);

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Read bytes from client socket [cnt=" + cnt + ", rmtAddr=" + rmtAddr +
                                        ']');
                                }

                                if (cnt == -1) {
                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Remote client closed connection: " + rmtAddr);
                                    }

                                   GridUtils.close(key, log);

                                    continue;
                                }
                                else if (cnt == 0) {
                                    continue;
                                }

                                // Sets limit to current position and
                                // resets position to 0.
                                readBuf.flip();

                                GridNioServerBuffer nioBuf = (GridNioServerBuffer)key.attachment();

                                // We have size let's test if we have object
                                while (readBuf.remaining() > 0) {
                                    // Always write into the buffer.
                                    nioBuf.read(readBuf);

                                    if (nioBuf.isFilled() == true) {
                                        if (log.isDebugEnabled() == true) {
                                            log.debug("Read full message from client socket: " + rmtAddr);
                                        }

                                        // Copy array so we can keep reading into the same buffer.
                                        final byte[] data = nioBuf.getMessageBytes().getArray();

                                        nioBuf.reset();

                                        workerPool.execute(new GridRunnable(gridName, "grid-nio-worker", log) {
                                            /**
                                             * {@inheritDoc}
                                             */
                                            @Override
                                            protected void body() throws InterruptedException {
                                                listener.onMessage(data);
                                            }
                                        });
                                    }
                                }
                            }
                            catch (IOException e) {
                                if (closed == false) {
                                    log.error("Failed to read data from client: " + rmtAddr, e);

                                    GridUtils.close(key, log);
                                }
                            }
                        }
                    }
                }
            }
        }
        // Ignore this exception as thread interruption is equal to 'close' call.
        catch (ClosedByInterruptException e) {
            if (log.isDebugEnabled() == true) {
                log.debug("Closing selector due to thread interruption: " + e.getMessage());
            }
        }
        catch (ClosedSelectorException e) {
            throw (GridException)new GridException("Selector got closed while active.", e).setData(266, "src/java/org/gridgain/grid/util/nio/GridNioServer.java");
        }
        catch (IOException e) {
            throw (GridException)new GridException("Failed to accept or read data.", e).setData(269, "src/java/org/gridgain/grid/util/nio/GridNioServer.java");
        }
        finally {
            closed = true;

            if (selector.isOpen() == true) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Closing all client sockets.");
                }

                workerPool.join(true);

                // Close all channels registered with selector.
                for (SelectionKey key : selector.keys()) {
                    GridUtils.close(key.channel(), log);
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Closing NIO selector.");
                }

                GridUtils.close(selector, log);
            }
        }
    }
}
