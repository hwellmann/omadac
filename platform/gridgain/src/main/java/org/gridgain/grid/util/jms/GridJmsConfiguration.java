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

import java.util.*;
import javax.jms.*;
// Single class import is required.
import javax.jms.Queue;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJmsConfiguration {
    /** */
    private GridLogger log = null;

    /** */
    private String user = null;

    /** */
    private String pswd = null;

    /** */
    private Map<Object, Object> jndiEnv = new HashMap<Object, Object>();

    /** */
    private String connFactoryName = null;

    /** */
    private String topicName = null;

    /** */
    private String queueName = null;

    /** */
    private int deliveryMode = DeliveryMode.NON_PERSISTENT;

    /** */
    private int priority = Message.DEFAULT_PRIORITY;

    /** */
    private long ttl = Message.DEFAULT_TIME_TO_LIVE;

    /** */
    private MessageListener topicMsgListener = null;

    /** */
    private MessageListener queueMsgListener = null;

    /** */
    private String selector = null;

    /** */
    private boolean transacted = false;

    /** */
    private ConnectionFactory connFactory = null;

    /** */
    private Queue queue = null;

    /** */
    private Topic topic = null;

    /**
     * @return FIXDOC
     */
    public final String getUser() {
        return user;
    }

    /**
     * @param user FIXDOC
     */
    public final void setUser(String user) {
        this.user = user;
    }

    /**
     * @return FIXDOC
     */
    public final String getPassword() {
        return pswd;
    }

    /**
     * @param pswd FIXDOC
     */
    public final void setPassword(String pswd) {
        this.pswd = pswd;
    }

    /**
     * @return FIXDOC
     */
    public final Map<Object, Object> getJndiEnvironment() {
        return jndiEnv;
    }

    /**
     * @param jndiEnv FIXDOC
     */
    public final void setJndiEnvironment(Map<Object, Object> jndiEnv) {
        this.jndiEnv = jndiEnv;
    }

    /**
     * @return FIXDOC
     */
    public final String getConnectionFactoryName() {
        return connFactoryName;
    }

    /**
     * @param connFactoryName FIXDOC
     */
    public final void setConnectionFactoryName(String connFactoryName) {
        this.connFactoryName = connFactoryName;
    }

    /**
     * @return FIXDOC
     */
    public final String getTopicName() {
        return topicName;
    }

    /**
     * @param topicName FIXDOC
     */
    public final void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    /**
     * @return FIXDOC
     */
    public final String getQueueName() {
        return queueName;
    }

    /**
     * @param queueName FIXDOC
     */
    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     *
     * @param log FIXDOC
     */
    public final void setLogger(GridLogger log) {
        assert log != null : "ASSERTION [line=178, file=src/java/org/gridgain/grid/util/jms/GridJmsConfiguration.java]";

        this.log = log;
    }

    /**
     * @return FIXDOC
     */
    public final GridLogger getLogger() {
        return log;
    }

    /**
     * @return FIXDOC
     */
    public final MessageListener getTopicMessageListener() {
        return topicMsgListener;
    }

    /**
     * @param topicMsgListener FIXDOC
     */
    public final void setTopicMessageListener(MessageListener topicMsgListener) {
        this.topicMsgListener = topicMsgListener;
    }

    /**
     * @return FIXDOC
     */
    public final MessageListener getQueueMessageListener() {
        return queueMsgListener;
    }

    /**
     * @param queueMsgListener FIXDOC
     */
    public final void setQueueMessageListener(MessageListener queueMsgListener) {
        this.queueMsgListener = queueMsgListener;
    }

    /**
     * @return FIXDOC
     */
    public final String getSelector() {
        return selector;
    }

    /**
     * @param selector FIXDOC
     */
    public final void setSelector(String selector) {
        this.selector = selector;
    }

    /**
     * @return FIXDOC
     */
    public final int getDeliveryMode() {
        return deliveryMode;
    }

    /**
     * @param deliveryMode FIXDOC
     */
    public final void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    /**
     * @return FIXDOC
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * @param priority FIXDOC
     */
    public final void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @return FIXDOC
     */
    public final long getTimeToLive() {
        return ttl;
    }

    /**
     * @param ttl FIXDOC
     */
    public final void setTimeToLive(long ttl) {
        this.ttl = ttl;
    }

    /**
     * @return FIXDOC
     */
    public boolean isTransacted() {
        return transacted;
    }

    /**
     * @param transacted FIXDOC
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    /**
     * @return FIXDOC
     */
    public ConnectionFactory getConnectionFactory() {
        return connFactory;
    }

    /**
     * @param connFactory FIXDOC
     */
    public void setConnectionFactory(ConnectionFactory connFactory) {
        this.connFactory = connFactory;
    }

    /**
     * @return FIXDOC
     */
    public Queue getQueue() {
        return queue;
    }

    /**
     * @param queue FIXDOC
     */
    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    /**
     * @return FIXDOC
     */
    public Topic getTopic() {
        return topic;
    }

    /**
     * @param topic FIXDOC
     */
    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJmsConfiguration.class, this);
    }
}
