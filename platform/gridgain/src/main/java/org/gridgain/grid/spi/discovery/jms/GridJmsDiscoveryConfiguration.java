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

package org.gridgain.grid.spi.discovery.jms;

import org.gridgain.grid.util.jms.*;
import org.gridgain.grid.util.tostring.*;

/**
 * JMS discovery SPI configuration bean. Provides all necessary
 * properties to configure JMS connectivity.
 * <p>
 * Unless explicitly specified, the following properties will
 * be assigned default values.
 * <ul>
 * <li>Topic name. Default is {@link #DFLT_TOPIC_NAME}.</li>
 * <li>Heartbeat frequency. Default is {@link #DFLT_HEARTBEAT_FREQ}.</li>
 * <li>Number of missed heartbeats. Default is {@link #DFLT_MAX_MISSED_HEARTBEATS}.</li>
 * <li>Ping wait time. Default is {@link #DFLT_PING_WAIT_TIME}.</li>
 * <li>Time-To-Live value. Default is {@link #DFLT_TIME_TO_LIVE}.</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJmsDiscoveryConfiguration extends GridJmsConfiguration {
    /** Default heartbeat frequency in milliseconds (value is <tt>5000</tt>). */
    public static final long DFLT_HEARTBEAT_FREQ = 5000;

    /** Default Time-To-Live value in milliseconds (value is <tt>10000</tt>). */
    public static final long DFLT_TIME_TO_LIVE = 10000;

    /**
     * Default number of heartbeat messages that could be missed until
     * node is considered to be failed (value is <tt>3</tt>).
     */
    public static final int DFLT_MAX_MISSED_HEARTBEATS = 3;

    /**
     * Default ping timeout value in milliseconds. If there is no answer
     * after this time remote node is considered to be failed
     * (value is <tt>5000</tt>).
     */
    public static final long DFLT_PING_WAIT_TIME = 5000;

    /**
     * Default handshake timeout in milliseconds. If there is no answer
     * after this time remote node would not join to the grid.
     */
    public static final long DFLT_HANDSHAKE_WAIT_TIME = 5000;

    /**
     * Default JMS topic name that will be used by discovery process
     * (value is <tt>org.gridgain.grid.spi.discovery.jms.GridJmsDiscoveryConfiguration.jms.topic</tt>).
     */
    public static final String DFLT_TOPIC_NAME = GridJmsDiscoveryConfiguration.class.getName() + ".jms.topic";

    /** Default maximum handshake threads. */
    public static final int DFLT_MAX_HANDSHAKE_THREADS = 10;
    
    /** Delay between heartbeat requests. */
    private long beatFreq = DFLT_HEARTBEAT_FREQ;

    /** Number of heartbeat messages that could be missed before remote node is considered as failed one. */
    private long maxMissedBeats = DFLT_MAX_MISSED_HEARTBEATS;

    /** Ping wait timeout. */
    private long pingWaitTime = DFLT_PING_WAIT_TIME;

    /** Handshake wait timeout. */
    private long handshakeWaitTime = DFLT_HANDSHAKE_WAIT_TIME;
    
    /** Maximum handshake threads. */
    private int maxHandshakeThreads = DFLT_MAX_HANDSHAKE_THREADS;

    /**
     * Creates instance of configuration. Sets topic name and Time-To-Live to default values.
     *
     * @see #DFLT_TOPIC_NAME
     * @see #DFLT_TIME_TO_LIVE
     */
    GridJmsDiscoveryConfiguration() {
        setTimeToLive(DFLT_TIME_TO_LIVE);
        setTopicName(DFLT_TOPIC_NAME);
    }

    /**
     * Gets interval for heartbeat messages.
     *
     * @return Time in milliseconds.
     */
    final long getHeartbeatFrequency() {
        return beatFreq;
    }

    /**
     * Sets interval for heartbeat messages.This configuration parameter is optional.
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_HEARTBEAT_FREQ}.
     *
     * @param beatFreq Time in milliseconds.
     */
    final void setHeartbeatFrequency(long beatFreq) {
        this.beatFreq = beatFreq;
    }

    /**
     * Gets numbers of heartbeat messages that could be missed before node
     * is considered to be failed.
     *
     * @return Number of heartbeat messages.
     */
    final long getMaximumMissedHeartbeats() {
        return maxMissedBeats;
    }

    /**
     * Sets numbers of heartbeat messages that could be missed before
     * node is considered to be failed. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_MAX_MISSED_HEARTBEATS}.
     *
     * @param maxMissedBeats Number of heartbeat messages.
     */
    final void setMaximumMissedHeartbeats(long maxMissedBeats) {
        this.maxMissedBeats = maxMissedBeats;
    }

    /**
     * Gets ping timeout after which if there is no answer node is
     * considered to be failed.
     *
     * @return Time in milliseconds.
     */
    final long getPingWaitTime() {
        return pingWaitTime;
    }

    /**
     * Sets ping timeout after which if there is no answer node is
     * considered to be failed. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link GridJmsDiscoveryConfiguration#DFLT_PING_WAIT_TIME}.
     *
     * @param pingWaitTime Time in milliseconds.
     */
    final void setPingWaitTime(long pingWaitTime) {
        this.pingWaitTime = pingWaitTime;
    }

    /**
     * Gets handshake timeout.
     *
     * @return handshakeWaitTime Time in milliseconds.
     */
    final long getHandshakeWaitTime() {
        return handshakeWaitTime;
    }

    /**
     * Sets handshake timeout. When node gets heartbeat from remote node
     * it asks for the attributes. If remote node does not send them back
     * and this time is out remote node would not be added in grid.
     *
     * @param handshakeWaitTime Time in milliseconds.
     */
    final void setHandshakeWaitTime(long handshakeWaitTime) {
        this.handshakeWaitTime = handshakeWaitTime;
    }
    
    /**
     * Returns maximum number of handshake threads. This means maximum 
     * number of handshakes that can be executed in parallel.
     * 
     * @return maximum number of handshake threads.
     */
    final int getMaximumHandshakeThreads() {
        return maxHandshakeThreads;
    }
    
    /**
     * Sets maximum number of handshake threads. This means maximum 
     * number of handshakes that can be executed in parallel.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_MAX_HANDSHAKE_THREADS}.
     * 
     * @param maxHandshakeThreads maximum number of handshake threads.
     */
    final void setMaximumHandshakeThreads(int maxHandshakeThreads) {
        this.maxHandshakeThreads = maxHandshakeThreads;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJmsDiscoveryConfiguration.class, this);
    }
}
