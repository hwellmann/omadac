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

package org.gridgain.grid.util.jms;

import java.io.*;
import java.util.*;
import javax.jms.*;
import javax.naming.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJms {
    /** */
    private enum ConnectionState {
        /** */
        CONNECTED,

        /** */
        FAILED
    }

    /** */
    private Context ctx = null;

    /** */
    private final GridJmsConfiguration cfg;

    /** */
    private Connection conn = null;

    /** */
    private ConnectionState state = null;

    /** */
    private Destination topic = null;

    /** */
    private Destination queue = null;

    /** */
    private final Object stateMux = new Object();

    /** */
    private final ExceptionListener exLsr;

    /** */
    private ReconnectionThread reconnThread;

    /**
     *
     * @param gridName Grid name.
     * @param cfg JMS configuration bean.
     */
    public GridJms(String gridName, GridJmsConfiguration cfg) {
        assert cfg != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/util/jms/GridJms.java]";

        this.cfg = cfg;

        // Exception listener.
        exLsr = new ExceptionListener() {
            /**
             * {@inheritDoc}
             */
            public void onException(JMSException ex) {
                GridJms.this.cfg.getLogger().error("Got exception from JMS server. " +
                    "Assume that connection has failed.", ex);

                // Assume that connection failed and we need to re-establish connection.
                state = ConnectionState.FAILED;

                try {
                    // Wake up reconnection thread. It will try to
                    // re-establish connection.
                    ensureConnection();
                }
                catch (JMSException e) {
                    // No-op.
                }
            }
        };

        reconnThread = new ReconnectionThread(gridName, "JMS Reconnection thread", cfg.getLogger());
    }

    /**
     * Starts JMS connection wrapper and establish connection.
     *
     * @throws JMSException FIXDOC
     * @throws NamingException FIXDOC
     */
    @SuppressWarnings({"JNDIResourceOpenedButNotSafelyClosed"})
    public void start() throws JMSException, NamingException {
        connect();

        // Start reconnection thread here. Connection should be already
        // established.
        reconnThread.start();
    }

    /**
     * Establishes connection.
     *
     * @throws NamingException Thrown if there is no such lookup name.
     * @throws JMSException Thrown if connect failed.
     */
    private void connect() throws NamingException, JMSException {
        ConnectionFactory factory;

        if (cfg.getConnectionFactory() == null) {
            //noinspection UseOfObsoleteCollectionType
            ctx = new InitialContext(new Hashtable<Object, Object>(cfg.getJndiEnvironment()));

            factory = (ConnectionFactory)ctx.lookup(cfg.getConnectionFactoryName());

            if (cfg.getTopicName() != null) {
                topic = (Destination)ctx.lookup(cfg.getTopicName());
            }

            if (cfg.getQueueName() != null) {
                queue = (Destination)ctx.lookup(cfg.getQueueName());
            }
        }
        else {
            factory = cfg.getConnectionFactory();

            topic = cfg.getTopic();

            queue = cfg.getQueue();
        }

        // If connection is set try to close it silently in case of reconnection.
        // So resources associated with this connection will be closed as well.
        if (conn != null) {
            close(conn, cfg.getLogger());

            conn = null;
        }

        conn = cfg.getUser() == null ? factory.createConnection() : factory.createConnection(cfg.getUser(),
            cfg.getPassword());

        conn.setExceptionListener(exLsr);

        Session ses = null;

        if (cfg.getTopicMessageListener() != null) {
            if (topic == null) {
                throw new JMSException("Must provide topic name in order to register topic message listener.");
            }

            ses = conn.createSession(cfg.isTransacted(), Session.AUTO_ACKNOWLEDGE);

            ses.createConsumer(topic, cfg.getSelector(), false).setMessageListener(cfg.getTopicMessageListener());
        }

        if (cfg.getQueueMessageListener() != null) {
            if (queue == null) {
                throw new JMSException("Must provide queue name in order to register queue message listener.");
            }

            if (ses == null) {
                ses = conn.createSession(cfg.isTransacted(), Session.AUTO_ACKNOWLEDGE);
            }

            ses.createConsumer(queue, cfg.getSelector(), false).setMessageListener(cfg.getQueueMessageListener());
        }

        synchronized(stateMux) {
            conn.start();

            state = ConnectionState.CONNECTED;
        }
    }

    /**
     * @throws JMSException FIXDOC
     */
    private void reconnect() throws JMSException {
        // If no one reestablished it.
        if (state == ConnectionState.FAILED) {
            try {
                // Reestablish connection.
                GridJms.this.connect();

                state = ConnectionState.CONNECTED;

                if (cfg.getLogger().isInfoEnabled() == true) {
                    cfg.getLogger().info("JMS connection reestablished successfully.");
                }
            }
            catch (NamingException e) {
                state = ConnectionState.FAILED;

                JMSException e1 = new JMSException("Failed to reestablish connection.");

                e1.setLinkedException(e);

                throw e1;
            }
            catch (JMSException e) {
                state = ConnectionState.FAILED;

                throw e;
            }
        }
    }

    /**
     * Tries to re-establish connection. If first try fails
     * it throws corresponding JMS exception and thread
     * continues reestablishing connection until get it.
     *
     * @throws JMSException If connection reestablishing failed.
     */
    private void ensureConnection() throws JMSException {
        synchronized (stateMux) {
            try {
                // Try to reestablish connection once.
                reconnect();
            }
            finally {
                // Force thread to continue reestablishing
                // connection if connection failed.
                if (state == ConnectionState.FAILED) {
                    stateMux.notifyAll();
                }
            }
        }
    }

    /**
     *
     */
    public void stop() {
        reconnThread.interrupt();

        GridUtils.join(reconnThread, cfg.getLogger());

        // Closing a JMS topic connection closes all the objects associated
        // with the connection including the topic session, topic publisher,
        // and topic subscriber.
        close(conn, cfg.getLogger());

        GridUtils.close(ctx, cfg.getLogger());
    }

    /**
     * @param obj FIXDOC
     * @throws JMSException FIXDOC
     */
    public void sendToTopic(Serializable obj) throws JMSException {
        send(topic, obj, null, null);
    }

    /**
     * @param obj FIXDOC
     * @param propName FIXDOC
     * @param propVal FIXDOC
     * @throws JMSException FIXDOC
     */
    public void sendToTopic(Serializable obj, String propName, String propVal) throws JMSException {
        send(topic, obj, propName, propVal);
    }

    /**
     * @param obj FIXDOC
     * @throws JMSException FIXDOC
     */
    public void sendToQueue(Serializable obj) throws JMSException {
        send(queue, obj, null, null);
    }

    /**
     * @param obj FIXDOC
     * @param propName FIXDOC
     * @param propVal FIXDOC
     * @throws JMSException FIXDOC
     */
    public void sendToQueue(Serializable obj, String propName, String propVal) throws JMSException {
        send(queue, obj, propName, propVal);
    }

    /**
     * @param queueName FIXDOC
     * @param obj FIXDOC
     * @throws JMSException FIXDOC
     * @throws NamingException FIXDOC
     */
    public void sendToQueue(String queueName, Serializable obj) throws JMSException, NamingException {
        assert queueName != null : "ASSERTION [line=317, file=src/java/org/gridgain/grid/util/jms/GridJms.java]";

        send((Destination)ctx.lookup(queueName), obj);
    }

    /**
     * @param dest FIXDOC
     * @param obj FIXDOC
     * @throws JMSException FIXDOC
     */
    public void send(Destination dest, Serializable obj) throws JMSException {
        send(dest, obj, null, null);
    }

    /**
     * @param dest FIXDOC
     * @param obj FIXDOC
     * @param propName FIXDOC
     * @param propVal FIXDOC
     * @throws JMSException FIXDOC
     */
    public void send(Destination dest, Serializable obj, String propName, String propVal) throws JMSException {
        assert dest != null : "ASSERTION [line=339, file=src/java/org/gridgain/grid/util/jms/GridJms.java]";

        ensureConnection();

        Session ses = null;

        try {
            ses = conn.createSession(cfg.isTransacted(), Session.AUTO_ACKNOWLEDGE);

            ObjectMessage msg = ses.createObjectMessage(obj);

            if (propName != null) {
                msg.setStringProperty(propName, propVal);
            }

            ses.createProducer(dest).send(msg, cfg.getDeliveryMode(), cfg.getPriority(), cfg.getTimeToLive());
        }
        catch (JMSException e) {
            synchronized(stateMux) {
                // Force reestablishing
                state = ConnectionState.FAILED;
            }

            throw e;
        }
        finally {
            close(ses, cfg.getLogger());
        }
    }

    /**
     * @param obj FIXDOC
     * @param timeout FIXDOC
     * @return FIXDOC
     * @throws JMSException FIXDOC
     */
    public Serializable requestToTopic(Serializable obj, long timeout) throws JMSException {
        return request(topic, obj, timeout, null, null);
    }

    /**
     * @param obj FIXDOC
     * @param timeout FIXDOC
     * @param propName FIXDOC
     * @param propVal FIXDOC
     * @return FIXDOC
     * @throws JMSException FIXDOC
     */
    public Serializable requestToTopic(Serializable obj, long timeout, String propName, String propVal)
        throws JMSException {
        return request(topic, obj, timeout, propName, propVal);
    }

    /**
     * @param obj FIXDOC
     * @param timeout FIXDOC
     * @return FIXDOC
     * @throws JMSException FIXDOC
     */
    public Serializable requestToQueue(Serializable obj, long timeout) throws JMSException {
        return request(queue, obj, timeout, null, null);
    }

    /**
     * @param obj FIXDOC
     * @param timeout FIXDOC
     * @param propName FIXDOC
     * @param propVal FIXDOC
     * @return FIXDOC
     * @throws JMSException FIXDOC
     */
    public Serializable requestToQueue(Serializable obj, long timeout, String propName, String propVal)
        throws JMSException {
        return request(queue, obj, timeout, propName, propVal);
    }

    /**
     * @param dest FIXDOC
     * @param obj FIXDOC
     * @param timeout FIXDOC
     * @return FIXDOC
     * @throws JMSException FIXDOC
     */
    public Serializable request(Destination dest, Serializable obj, long timeout) throws JMSException {
        return request(dest, obj, timeout, null, null);
    }

    /**
     * @param dest FIXDOC
     * @param obj FIXDOC
     * @param timeout FIXDOC
     * @param propName FIXDOC
     * @param propVal FIXDOC
     * @return FIXDOC
     * @throws JMSException FIXDOC
     */
    public Serializable request(Destination dest, Serializable obj, long timeout, String propName, String propVal)
        throws JMSException {
        assert dest != null : "ASSERTION [line=437, file=src/java/org/gridgain/grid/util/jms/GridJms.java]";

        ensureConnection();

        Session ses = null;
        TemporaryQueue answQueue = null;
        MessageConsumer cons = null;

        try {
            ses = conn.createSession(cfg.isTransacted(), Session.AUTO_ACKNOWLEDGE);

            ObjectMessage msg = ses.createObjectMessage(obj);

            if (propName != null) {
                msg.setStringProperty(propName, propVal);
            }

            answQueue = ses.createTemporaryQueue();

            msg.setJMSReplyTo(answQueue);

            ses.createProducer(dest).send(msg, cfg.getDeliveryMode(), cfg.getPriority(), cfg.getTimeToLive());

            cons = ses.createConsumer(answQueue);

            Message res = cons.receive(timeout);

            return res == null ? null : ((ObjectMessage)res).getObject();
        }
        catch (JMSException e) {
            synchronized(stateMux) {
                // Force reestablishing
                state = ConnectionState.FAILED;
            }

            throw e;
        }
        finally {
            close(cons, cfg.getLogger());

            if (answQueue != null) {
                try {
                    answQueue.delete();
                }
                catch (JMSException e) {
                    cfg.getLogger().error("Failed to delete temporary queue: " + answQueue, e);
                }
            }

            close(ses, cfg.getLogger());
        }
    }

    /**
     * Closes JMS message consumer logging {@link JMSException}.
     *
     * @param rsrc JMS message consumer to close. If consumer is <tt>null</tt>, it's no-op.
     * @param log Logger to log errors.
     */
    public static void close(MessageConsumer rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (JMSException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes JMS message producer logging {@link JMSException}.
     *
     * @param rsrc JMS message producer to close. If producer is <tt>null</tt>, it's no-op.
     * @param log Logger to log errors.
     */
    public static void close(MessageProducer rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (JMSException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    @SuppressWarnings({"UnnecessaryFullyQualifiedName"})
    public static void close(javax.jms.Connection rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.stop();
            }
            catch (JMSException e) {
                if (log != null) {
                    log.error("Failed to stop resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }

            try {
                rsrc.close();
            }
            catch (JMSException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    @SuppressWarnings({"UnnecessaryFullyQualifiedName"})
    public static void close(javax.jms.Session rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (JMSException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     *
     * FIXDOC: add file description.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private final class ReconnectionThread extends GridSpiThread {
        /** */
        private GridLogger log = null;

        /**
         * Creates new connection reestablishing thread.
         *
         * @param gridName Grid name.
         * @param name Thread name.
         * @param log Logger.
         */
        private ReconnectionThread(String gridName, String name, GridLogger log) {
            super(gridName, name, log);

            this.log = log;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            synchronized (stateMux) {
                while (isInterrupted() == false) {
                    if (state == ConnectionState.FAILED) {
                        try {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Trying to reestablish JMS connection");
                            }

                            reconnect();
                        }
                        catch (JMSException e) {
                            // No-op.
                        }

                        if (state == ConnectionState.FAILED) {
                            if (log.isDebugEnabled() == true) {
                                log.debug("JMS connection failed again. Will retry in 3 seconds.");
                            }

                            // Connection was not established. Sleep for 3 seconds and try
                            // to reestablish then
                            stateMux.wait(3000);
                        }
                    }
                    else {
                        if (log.isDebugEnabled() == true) {
                            log.debug("JMS connection re-established successfully. Wait for the next fail.");
                        }

                        // Connection was established. Wait until it fails.
                        stateMux.wait();
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJms.class, this);
    }
}
