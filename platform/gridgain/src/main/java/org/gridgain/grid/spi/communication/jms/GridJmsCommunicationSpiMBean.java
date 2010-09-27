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

package org.gridgain.grid.spi.communication.jms;

import java.util.*;
import javax.jms.*;
import javax.jms.Queue;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean that provides read-only access to the JMS communication
 * SPI configuration. Beside connectivity this bean shows message delivery mode,
 * queue/topic transaction mode and messages priority.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean provides access to the JMS communication SPI configuration.")
public interface GridJmsCommunicationSpiMBean extends GridSpiManagementMBean {
    /**
     * Indicates whether JMS messages are transacted or not.
     *
     * @return <tt>true</tt> if session supports transactions,
     *      otherwise <tt>false</tt>.
     */
    @GridMBeanDescription("Indicates whether JMS messages are transacted or not.")
    public boolean isTransacted();

    /**
     * Gets messages delivery mode.
     *
     * @return Either {@link DeliveryMode#PERSISTENT} or
     *      {@link DeliveryMode#NON_PERSISTENT}.
     */
    @GridMBeanDescription("Messages delivery mode.")
    public int getDeliveryMode();

    /**
     * Gets messages delivery priority as defined in {@link Message}.
     * The lower the faster.
     *
     * @return Message priority.
     */
    @GridMBeanDescription("Message priority.")
    public int getPriority();

    /**
     * Gets messages lifetime. Messages stays in the queue/topic until they
     * run out of time.
     *
     * @return Time-to-live value in milliseconds.
     */
    @GridMBeanDescription("Time-to-live value in milliseconds.")
    public long getTimeToLive();

    /**
     * Gets JNDI name for JMS queue.
     * If provided, then <tt>queue</tt> will be used for node-to-node
     * communication otherwise <tt>topic</tt> will be used.
     *
     * @return Name of the queue in JNDI tree.
     */
    @GridMBeanDescription("Name of the queue in JNDI tree.")
    public String getQueueName();

    /**
     * Gets JMS queue.
     * If provided, then <tt>queue</tt> will be used for node-to-node
     * communication otherwise <tt>topic</tt> will be used.
     *
     * @return JMS queue.
     */
    @GridMBeanDescription("JMS queue.")
    public Queue getQueue();

    /**
     * Gets JNDI name of the JMS topic.
     *
     * @return Name of JMS topic in JNDI tree.
     */
    @GridMBeanDescription("Name of JMS topic in JNDI tree.")
    public String getTopicName();
    
    /**
     * Gets JMS topic.
     *
     * @return JMS topic.
     */
    @GridMBeanDescription("JMS topic.")
    public Topic getTopic();

    /**
     * Gets naming context variables which are used by node to establish JNDI
     * tree connection.
     *
     * @return Map of JNDI environment variables.
     */
    @GridMBeanDescription("Map of JNDI environment variables.")
    public Map<Object, Object> getJndiEnvironment();

    /**
     * Returns name of the JMS connection factory in JNDI tree.
     *
     * @return Connection factory name.
     */
    @GridMBeanDescription("Connection factory name.")
    public String getConnectionFactoryName();
    
    /**
     * Returns JMS connection factory.
     *
     * @return Connection factory.
     */
    @GridMBeanDescription("Connection factory.")
    public ConnectionFactory getConnectionFactory();    

    /**
     * Gets JMS connection user name for connectivity authentication.
     *
     * @return Name of the user.
     */
    @GridMBeanDescription("Name of the user.")
    public String getUser();

    /**
     * Gets JMS connection password for connectivity authentication.
     *
     * @return User password.
     */
    @GridMBeanDescription("User password.")
    public String getPassword();
}
