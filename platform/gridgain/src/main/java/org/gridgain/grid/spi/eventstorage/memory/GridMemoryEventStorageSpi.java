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

package org.gridgain.grid.spi.eventstorage.memory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.eventstorage.*;
import org.gridgain.grid.util.tostring.*;

/**
 * In-memory {@link GridEventStorageSpi} implementation. All events are
 * kept in the FIFO queue. If no configuration is provided a default expiration
 * {@link #DFLT_EXPIRE_AGE_MS} and default count {@link #DFLT_EXPIRE_COUNT} will
 * be used.
 * <p>
 * It's recommended not to set huge size and unlimited TTL because this might
 * lead to consuming a lot of memory and result in {@link OutOfMemoryError}.
 * Both event expiration time and maximum queue size could be changed at
 * runtime.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>Event queue size (see {@link #setExpireCount(long)})</li>
 * <li>Event time-to-live value (see {@link #setExpireAgeMs(long)})</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridMemoryEventStorageSpi is used by default and should be explicitly configured only
 * if some SPI configuration parameters need to be overridden. Examples below insert own
 * events queue size value that differs from default 10000.
 * <pre name="code" class="java">
 * GridMemoryEventStorageSpispi = new GridMemoryEventStorageSpi();
 * 
 * // Init own events size.
 * spi.setExpireCount(2000);
 * 
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 * 
 * // Override default event storage SPI.
 * cfg.setEventStorageSpi(spi);
 * 
 * // Start grid.
 * GridFactory.start(cfg); 
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridMemoryEventStorageSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.eventstorage.memory.GridMemoryEventStorageSpi"&gt;
 *                 &lt;property name="expireCount" value="2000"/&gt;
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
 * @see GridEventStorageSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridMemoryEventStorageSpi extends GridSpiAdapter implements GridEventStorageSpi,
    GridMemoryEventStorageSpiMBean {
    /** Default event time to live value in milliseconds (value is {@link Long#MAX_VALUE}). */
    public static final long DFLT_EXPIRE_AGE_MS = Long.MAX_VALUE;

    /** Default expire count (value is <tt>10000</tt>). */
    public static final int DFLT_EXPIRE_COUNT = 10000;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** Event time-to-live value in milliseconds. */
    private long expireAgeMs = DFLT_EXPIRE_AGE_MS;

    /** Maximum queue size. */
    private long expireCnt = DFLT_EXPIRE_COUNT;

    /** Lock for readers, writers. */
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    /** Events queue. */
    private Queue<GridEvent> evts = new ConcurrentLinkedQueue<GridEvent>();
    
    /** Event queue size to avoid O(n) complexity of queue size() method. */
    private AtomicInteger queueSize = new AtomicInteger(0);
    
    /** Flag indicating whether cleaning is currently taking place. */
    private AtomicBoolean cleaningFlag = new AtomicBoolean(false);
    
    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(expireCnt > 0, "expireCnt > 0");
        assertParameter(expireAgeMs > 0, "expireAgeMs > 0");

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("expireAgeMs", expireAgeMs));
            log.info(configInfo("expireCnt", expireCnt));
        }

        registerMBean(gridName, this, GridMemoryEventStorageSpiMBean.class);

        // Ack ok start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        // Reset events.
        evts.clear();

        // Ack ok stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * Sets events expiration time. All events that exceed this value
     * will be removed from the queue when next event comes.
     * <p>
     * If not provided, default value is {@link #DFLT_EXPIRE_AGE_MS}.
     *
     * @param expireAgeMs Expiration time in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setExpireAgeMs(long expireAgeMs) {
        this.expireAgeMs = expireAgeMs;
    }

    /**
     * Sets events queue size. Events will be filtered out when new request comes.
     * <p>
     * If not provided, default value {@link #DFLT_EXPIRE_COUNT} will be used.
     *
     * @param expireCnt Maximum queue size.
     */
    @GridSpiConfiguration(optional = true)
    public void setExpireCount(long expireCnt) {
        this.expireCnt = expireCnt;
    }

    /**
     * {@inheritDoc}
     */
    public long getExpireAgeMs() {
        return expireAgeMs;
    }

    /**
     * {@inheritDoc}
     */
    public long getExpireCount() {
        return expireCnt;
    }

    /**
     * {@inheritDoc}
     */
    public void clearAll() {
        rwLock.writeLock().lock();

        try {
            evts.clear();
            
            queueSize.set(0);    
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<GridEvent> queryLocalEvents(GridEventFilter filter) {
        assert filter != null : "ASSERTION [line=229, file=src/java/org/gridgain/grid/spi/eventstorage/memory/GridMemoryEventStorageSpi.java]";

        List<GridEvent> list = new ArrayList<GridEvent>();

        cleanupQueue();

        rwLock.readLock().lock();
        
        try {
            for (GridEvent evt : evts) {
                // Filter out events.
                if (filter.accept(evt) == true) {
                    list.add(evt);
                }
            }
        } 
        finally {
            rwLock.readLock().unlock();
        }

        return list;
    }

    /**
     * {@inheritDoc}
     */
    public void record(GridEvent evt) throws GridSpiException {
        assert evt != null : "ASSERTION [line=256, file=src/java/org/gridgain/grid/spi/eventstorage/memory/GridMemoryEventStorageSpi.java]";

        cleanupQueue();
        
        evts.add(evt);

        queueSize.incrementAndGet();
        
        if (log.isDebugEnabled() == true) {
            log.debug("Event recorded: " + evt);
        }
    }

    /**
     * Method cleans up all events that either outnumber queue size
     * or exceeds time-to-live value. It does nothing if someone else
     * cleans up queue (lock is locked) or if there are queue readers
     * (readersNum > 0).
     */
    private void cleanupQueue() {
        // Make sure that only one thread blocks to clean and others 
        // proceed freely.
        if (cleaningFlag.compareAndSet(false, true) == true) {
            rwLock.writeLock().lock();
            
            try {
                long now = System.currentTimeMillis();

                // We can do expiration policy checks here avoiding extra thread.
                while (queueSize.get() > expireCnt) {
                    queueSize.decrementAndGet();
                    
                    GridEvent expired = evts.poll();
    
                    if (log.isDebugEnabled() == true) {
                        log.debug("Event expired by count: " + expired);
                    }
                }
        
                while (evts.isEmpty() == false && now - evts.peek().getTimestamp() >= expireAgeMs) {
                    queueSize.decrementAndGet();

                    GridEvent expired = evts.poll();
    
                    if (log.isDebugEnabled() == true) {
                        log.debug("Event expired by age: " + expired);
                    }
                }
            }
            finally {
                rwLock.writeLock().unlock();

                // Reset cleaning flag.
                boolean expectVal = cleaningFlag.compareAndSet(true, false);
                
                assert expectVal == true : "ASSERTION [line=311, file=src/java/org/gridgain/grid/spi/eventstorage/memory/GridMemoryEventStorageSpi.java]";
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMemoryEventStorageSpi.class, this);
    }  
}
