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

package org.gridgain.grid.spi.communication.tcp;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.nio.*;
import org.gridgain.grid.util.tostring.*;

/**
 * <tt>GridTcpCommunicationSpi</tt> is default communication SPI which uses
 * TCP/IP protocol and Java NIO to communicate with other nodes.
 * <p>
 * To enable communication with other nodes, this SPI adds {@link #ATTR_ADDR}
 * and {@link #ATTR_PORT} local node attributes (see {@link GridNode#getAttributes()}.
 * <p>
 * At startup, this SPI tries to start listening to local port specified by
 * {@link #setLocalPort(int)} method. If local port is occupied, then SPI will
 * automatically increment the port number until it can successfully bind for
 * listening. {@link #setLocalPortRange(int)} configuration parameter controls
 * maximum number of ports that SPI will try before it fails. Port range comes
 * very handy when starting multiple grid nodes on the same machine or even
 * in the same VM. In this case all nodes can be brought up without a single
 * change in configuration.
 * <p>
 * This SPI caches connections to remote nodes so it does not have to reconnect every
 * time a message is sent. By default, idle connections are kept active for
 * {@link #DFLT_IDLE_CONN_TIMEOUT} period and then are closed. Use
 * {@link #setIdleConnectionTimeout(long)} configuration parameter to configure
 * you own idle connection timeout.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>Node local IP address (see {@link #setLocalAddress(String)})</li>
 * <li>Node local port number (see {@link #setLocalPort(int)})</li>
 * <li>Local port range (see {@link #setLocalPortRange(int)}</li>
 * <li>Number of threads used for handling NIO messages (see {@link #setMessageThreads(int)})</li>
 * <li>Idle connection timeout (see {@link #setIdleConnectionTimeout(long)})</li>
 * <li>Direct or heap buffer allocation (see {@link #setDirectBuffer(boolean)})</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridTcpCommunicationSpi is used by default and should be explicitely configured
 * only if some SPI configuration parameters need to be overridden. Examples below
 * enable encryption which is disabled by default.
 * <pre name="code" class="java">
 * GridTcpCommunicationSpi commSpi = new GridTcpCommunicationSpi();
 *
 * // Override local port.
 * commSpi.setLocalPort(4321);
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default communication SPI.
 * cfg.setCommunicationSpi(commSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridTcpCommunicationSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="communicationSpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.communication.tcp.GridTcpCommunicationSpi"&gt;
 *                 &lt;!-- Override local port. --&gt;
 *                 &lt;property name="localPort" value="4321"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridCommunicationSpi
 */
@SuppressWarnings("NonStaticInitializer")
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridTcpCommunicationSpi extends GridSpiAdapter implements GridCommunicationSpi,
    GridTcpCommunicationSpiMBean {
    /**
     * Certificate common name
     * (value is <tt>org.gridgain.grid.spi.communication.tcp.GridTcpCommunicationSpi.tcp-communication-CA</tt>).
     */
    public static final String CERTIFICATE_SERVER_CN = GridTcpCommunicationSpi.class.getName() +
        ".tcp-communication-CA";

    /** Number of threads responsible for handling messages. */
    public static final int DFLT_MSG_THREADS = 5;

    /** Node attribute that is mapped to node IP address (value is <tt>comm.tcp.addr</tt>). */
    public static final String ATTR_ADDR = "comm.tcp.addr";

    /** Node attribute that is mapped to node port number (value is <tt>comm.tcp.port</tt>). */
    public static final String ATTR_PORT = "comm.tcp.port";

    /** Default port which node sets listener to (value is <tt>47100</tt>). */
    public static final int DFLT_PORT = 47100;

    /** Default idle connection timeout (value is <tt>30000</tt>ms). */
    public static final int DFLT_IDLE_CONN_TIMEOUT = 30000;
    
    /**
     * Default local port range (value is <tt>100</tt>).
     * See {@link #setLocalPortRange(int)} for details.
     */
    public static final int DFLT_PORT_RANGE = 100;

    /** Time, which SPI will wait before retry operation. */
    private static final long ERR_WAIT_TIME = 2000;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /** Local IP address. */
    private String localAddr = null;

    /** Complex variable that represents this node IP address. */
    private InetAddress localHost = null;

    /** Local port which node uses. */
    private int localPort = DFLT_PORT;

    /** */
    private int localPortRange = DFLT_PORT_RANGE;

    /** */
    private String gridName = null;

    /** Allocate direct buffer or heap buffer. */
    private boolean directBuf = true;

    /** */
    private long idleConnTimeout = DFLT_IDLE_CONN_TIMEOUT;

    /** */
    private GridNioServer nioSrvr = null;

    /** */
    private TcpServer tcpSrvr = null;

    /** Number of threads responsible for handling messages. */
    private int msgThreads = DFLT_MSG_THREADS;

    /** */
    private IdleClientWorker idleClientWorker = null;

    /** */
    private Map<UUID, GridNioClient> clients = null;

    /** SPI listener. */
    private volatile GridMessageListener listener = null;

    /** */
    private int boundTcpPort = -1;

    /** */
    private ThreadPoolExecutor nioExec = null;

    /** */
    private final Object mux = new Object();

    /**
     * Sets local host address for socket binding. Note that one node could have
     * additional addresses beside the loopback one. This configuration
     * parameter is optional.
     *
     * @param localAddr IP address. Default value is any available local
     *      IP address.
     */
    @GridSpiConfiguration(optional = true)
    public void setLocalAddress(String localAddr) {
        this.localAddr = localAddr;
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalAddress() {
        return localAddr;
    }

    /**
     * Number of threads used for handling messages received by NIO server.
     * This number usually should be no less than number of CPUs.
     * <p>
     * If not provided, default value is {@link #DFLT_MSG_THREADS}.
     *
     * @param msgThreads Number of threads.
     */
    @GridSpiConfiguration(optional = true)
    public void setMessageThreads(int msgThreads) {
        this.msgThreads = msgThreads;
    }

    /**
     * {@inheritDoc}
     */
    public int getMessageThreads() {
        return msgThreads;
    }

    /**
     * Sets local port for socket binding.
     * <p>
     * If not provided, default value is {@link #DFLT_PORT}.
     *
     * @param localPort Port number.
     */
    @GridSpiConfiguration(optional = true)
    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    /**
     * {@inheritDoc}
     */
    public int getLocalPort() {
        return localPort;
    }

    /**
     * Sets local port range for local host ports (value must greater than or equal to <tt>0</tt>).
     * If provided local port (see {@link #setLocalPort(int)}} is occupied,
     * implementation will try to increment the port number for as long as it is less than
     * initial value plus this range.
     * <p>
     * If port range value is <tt>0</tt>, then implementation will try bind only to the port provided by
     * {@link #setLocalPort(int)} method and fail if binding to this port did not succeed.
     * <p>
     * Local port range is very useful during development when more than one grid nodes need to run
     * on the same physical machine.
     * <p>
     * If not provided, default value is {@link #DFLT_PORT_RANGE}.
     *
     * @param localPortRange New local port range.
     */
    @GridSpiConfiguration(optional = true)
    public void setLocalPortRange(int localPortRange) {
        this.localPortRange = localPortRange;
    }

    /**
     * {@inheritDoc}
     */
    public int getLocalPortRange() {
        return localPortRange;
    }

    /**
     * Sets maximum idle connection timeout upon which a connection
     * to client will be closed.
     * <p>
     * If not provided, default value is {@link #DFLT_IDLE_CONN_TIMEOUT}.
     *
     * @param idleConnTimeout Maximum idle connection time.
     */
    @GridSpiConfiguration(optional = true)
    public void setIdleConnectionTimeout(long idleConnTimeout) {
        this.idleConnTimeout = idleConnTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public long getIdleConnectionTimeout() {
        return idleConnTimeout;
    }

    /**
     * Sets flag to allocate direct or heap buffer in SPI.
     * If value is <tt>true</tt>, then SPI will use {@link ByteBuffer#allocateDirect(int)} call.
     * Otherwise, SPI will use {@link ByteBuffer#allocate(int)} call.
     * <p>
     * If not provided, default value is <tt>true</tt>.
     *
     * @param directBuf Flag indicates to allocate direct or heap buffer in SPI.
     */
    @GridSpiConfiguration(optional = true)
    public void setDirectBuffer(boolean directBuf) {
        this.directBuf = directBuf;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirectBuffer() {
        return directBuf;
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridMessageListener listener) {
        this.listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    public int getNioActiveThreadCount() {
        assert nioExec != null : "ASSERTION [line=355, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getActiveCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getNioTotalCompletedTaskCount() {
        assert nioExec != null : "ASSERTION [line=364, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getCompletedTaskCount();
    }

    /**
     * {@inheritDoc}
     */
    public int getNioCorePoolSize() {
        assert nioExec != null : "ASSERTION [line=373, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getCorePoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getNioLargestPoolSize() {
        assert nioExec != null : "ASSERTION [line=382, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getLargestPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getNioMaximumPoolSize() {
        assert nioExec != null : "ASSERTION [line=391, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getMaximumPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getNioPoolSize() {
        assert nioExec != null : "ASSERTION [line=400, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getNioTotalScheduledTaskCount() {
        assert nioExec != null : "ASSERTION [line=409, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getTaskCount();
    }

    /**
     * {@inheritDoc}
     */
    public int getNioTaskQueueSize() {
        assert nioExec != null : "ASSERTION [line=418, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        return nioExec.getQueue().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        assertParameter(localPort > 1023, "localPort > 1023");
        assertParameter(localPort <= 0xffff, "localPort < 0xffff");
        assertParameter(localPortRange >= 0, "localPortRange >= 0");
        assertParameter(msgThreads > 0, "msgThreads > 0");

        nioExec = new ThreadPoolExecutor(msgThreads, msgThreads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new GridSpiThreadFactory(gridName, "grid-nio-msg-handler", log));

        try {
            localHost = localAddr == null || localAddr.length() == 0 ?
                GridNetworkHelper.getLocalHost() : InetAddress.getByName(localAddr);
        }
        catch (IOException e) {
            throw (GridSpiException)new GridSpiException("Failed to initialize local address: " + localAddr, e).setData(441, "src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java");
        }

        try {
            // This method potentially resets local port to the value
            // local node was bound to.
            nioSrvr = resetServer();
        }
        catch (GridException e) {
            throw (GridSpiException)new GridSpiException("Failed to initialize TCP server: " + localHost, e).setData(450, "src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java");
        }

        // Set local node attributes.
        return GridUtils.makeMap(createSpiAttributeName(ATTR_ADDR), localHost, createSpiAttributeName(ATTR_PORT),
            boundTcpPort);
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        assert localHost != null : "ASSERTION [line=462, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        // Start SPI start stopwatch.
        startStopwatch();

        this.gridName = gridName;

        assertParameter(idleConnTimeout > 0, "idleConnTimeout > 0");

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("localAddr", localAddr));
            log.info(configInfo("msgThreads", msgThreads));
            log.info(configInfo("localPort", localPort));
            log.info(configInfo("localPortRange", localPortRange));
            log.info(configInfo("idleConnTimeout", idleConnTimeout));
            log.info(configInfo("directBuf", directBuf));
        }

        registerMBean(gridName, this, GridTcpCommunicationSpiMBean.class);

        synchronized (mux) {
            clients = new HashMap<UUID, GridNioClient>();
        }

        tcpSrvr = new TcpServer(nioSrvr);

        tcpSrvr.start();

        idleClientWorker = new IdleClientWorker();

        idleClientWorker.start();

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * Recreates tpcSrvr socket instance.
     *
     * @return Server socket.
     * @throws GridException Thrown if it's not possible to create tpcSrvr socket.
     */
    private GridNioServer resetServer() throws GridException {
        int maxPort = localPort + localPortRange;

        final GridNioServerListener listener = new GridNioServerListener() {
            /** Cached class loader. */
            private final ClassLoader clsLdr = getClass().getClassLoader();

            /**
             * {@inheritDoc}
             */
            public void onMessage(byte[] data) {
                try {
                    GridTcpCommunicationMessage msg = (GridTcpCommunicationMessage)GridMarshalHelper.unmarshal(
                        marshaller, new GridByteArrayList(data, data.length), clsLdr);

                    notifyListener(msg);
                }
                catch (GridException e) {
                    log.error("Failed to deserialize TCP message.", e);
                }
            }
        };

        GridNioServer srvr = null;

        // If bound TPC port was not set yet, then find first
        // available port.
        if (boundTcpPort < 0) {
            for (int port = localPort; port < maxPort; port++) {
                try {
                    srvr = new GridNioServer(localHost, port, listener, log, nioExec, gridName, directBuf);

                    boundTcpPort = port;

                    // Ack Port the tpcSrvr was bound too.
                    if (log.isInfoEnabled() == true) {
                        log.info("Successfully bound to TCP port: " + boundTcpPort);
                    }

                    break;
                }
                catch (GridException e) {
                    if (port + 1 < maxPort) {
                        if (log.isInfoEnabled() == true) {
                            log.info("Failed to bind to local port (will try next port within range) [port=" + port +
                                ", localHost=" + localHost + ']');
                        }
                    }
                    else {
                        throw (GridException)new GridException("Failed to bind to any port within range [startPort=" + localPort +
                            ", portRange=" + localPortRange + ", localHost=" + localHost + ']', e).setData(556, "src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java");
                    }
                }
            }
        }
        // If bound TCP port is set, then always bind to it.
        else {
            srvr = new GridNioServer(localHost, boundTcpPort, listener, log, nioExec, gridName, directBuf);
        }

        return srvr;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        GridUtils.interrupt(idleClientWorker);
        GridUtils.join(idleClientWorker, log);

        List<GridNioClient> clientsCopy = null;

        // Close all client connections.
        synchronized (mux) {
            if (clients != null) {
                clientsCopy = new ArrayList<GridNioClient>(clients.values());
            }

            clients = null;
        }

        if (clientsCopy != null) {
            for (GridNioClient client : clientsCopy) {
                client.close();
            }
        }

        // Stop TCP server.
        GridUtils.interrupt(tcpSrvr);
        GridUtils.join(tcpSrvr, log);

        unregisterMBean();

        // Stop NIO thread pool.
        GridUtils.shutdownNow(getClass(), nioExec, log);

        // Clear resources.
        tcpSrvr = null;
        nioSrvr = null;
        idleClientWorker = null;

        boundTcpPort = -1;

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(GridNode destNode, Serializable msg) throws GridSpiException {
        assert destNode != null : "ASSERTION [line=620, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=621, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        send0(destNode, msg);
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(Collection<GridNode> destNodes, Serializable msg) throws GridSpiException {
        assert destNodes != null : "ASSERTION [line=630, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=631, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";
        assert destNodes.size() != 0 : "ASSERTION [line=632, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        for (GridNode node : destNodes) {
            send0(node, msg);
        }
    }

    /**
     * Sends message to certain node. This implementation uses {@link GridMarshaller}
     * as stable and fast stream implementation.
     *
     * @param node  Node message should be sent to.
     * @param msg   Message that should be sent.
     * @throws GridSpiException Thrown if any socket operation fails.
     */
    private void send0(GridNode node, Serializable msg) throws GridSpiException {
        assert node != null : "ASSERTION [line=648, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=649, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Sending message to node [node=" + node + ", msg=" + msg + ']');
        }

        //Shortcut for the local node
        if (node.getId().equals(nodeId) == true) {
            // Call listener directly. The manager will execute this
            // callback in a different thread, so there should not be
            // a deadlock.
            notifyListener(new GridTcpCommunicationMessage(nodeId, msg));
        }
        else {
            GridNioClient client = null;

            try {
                synchronized (mux) {
                    client = clients.get(node.getId());

                    if (client == null) {
                        InetAddress addr = (InetAddress)node.getAttribute(createSpiAttributeName(ATTR_ADDR));
                        Integer port = (Integer)node.getAttribute(createSpiAttributeName(ATTR_PORT));

                        if (addr == null || port == null) {
                            throw (GridSpiException)new GridSpiException("Failed to send message to the destination node. " +
                                "Node does not have IP address or port set up. Check configuration and make sure " +
                                "that you use the same communication SPI on all nodes. Remote node id: " + node.getId()).setData(674, "src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java");
                        }

                        clients.put(node.getId(), client = new GridNioClient(addr, port, localHost, log));

                        // Wake up idle connection worker.
                        mux.notifyAll();
                    }
                }

                GridByteArrayList buf =
                    GridMarshalHelper.marshal(marshaller, new GridTcpCommunicationMessage(nodeId, msg));

                client.sendMessage(buf.getInternalArray(), buf.getSize());
            }
            catch (GridException e) {
                throw (GridSpiException)new GridSpiException("Failed to send message to remote node: " + node, e).setData(692, "src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationSpi.java");
            }
        }
    }

    /**
     *
     * @param msg Communication message.
     */
    private void notifyListener(GridTcpCommunicationMessage msg) {
        final GridMessageListener listener = this.listener;

        if (listener != null) {
            // Notify listener of a new message.
            listener.onMessage(msg.getNodeId(), msg.getMessage());
        }
        else {
            log.warning("Received communication message without any registered listeners (will ignore) [senderNodeId=" +
                msg.getNodeId() + ']');
        }
    }

    /**
     *
     */
    private class TcpServer extends GridSpiThread {
        /** */
        private GridNioServer srvr = null;

        /**
         *
         * @param srvr NIO server.
         */
        TcpServer(GridNioServer srvr) {
            super(gridName, "grid-tcp-nio-srvr", log);

            this.srvr = srvr;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void interrupt() {
            super.interrupt();

            synchronized (mux) {
                if (srvr != null) {
                    srvr.close();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            while (isInterrupted() == false) {
                try {
                    final GridNioServer locSrvr;

                    synchronized (mux) {
                        if (isInterrupted() == true) {
                            return;
                        }

                        locSrvr = srvr == null ? resetServer() : srvr;
                    }

                    locSrvr.accept();
                }
                catch (GridException e) {
                    if (isInterrupted() == false) {
                        log.error("Failed to accept remote connection (will wait for " + ERR_WAIT_TIME + "ms).", e);

                        Thread.sleep(ERR_WAIT_TIME);

                        synchronized (mux) {
                            srvr.close();

                            srvr = null;
                        }
                    }
                }
            }
        }
    }

    /**
     *
     */
    private class IdleClientWorker extends GridSpiThread {
        /**
         *
         */
        IdleClientWorker() {
            super(gridName, "nio-idle-client-collector", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            while (isInterrupted() == false) {

                long nextIdleTime = System.currentTimeMillis() + idleConnTimeout;

                long now = System.currentTimeMillis();

                synchronized (mux) {
                    for (Iterator<Map.Entry<UUID, GridNioClient>> iter = clients.entrySet().iterator();
                        iter.hasNext() == true;) {
                        Map.Entry<UUID, GridNioClient> e = iter.next();

                        GridNioClient client = e.getValue();

                        long idleTime = client.getIdleTime();

                        if (idleTime >= idleConnTimeout) {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Closing idle connection to node: " + e.getKey());
                            }

                            client.close();

                            iter.remove();
                        }
                        else if (now + idleConnTimeout - idleTime < nextIdleTime) {
                            nextIdleTime = now + idleConnTimeout - idleTime;
                        }
                    }

                    if (nextIdleTime - System.currentTimeMillis() > 0) {
                        mux.wait(nextIdleTime - System.currentTimeMillis());
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(3);

        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridTcpCommunicationSpi.class, this);
    }
}
