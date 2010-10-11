/*
 *    Omadac - The Open Map Database Compiler
 *    http://omadac.org
 * 
 *    (C) 2010, Harald Wellmann and Contributors
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.omadac.grid.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.gridgain.grid.GridConfigurationAdapter;
import org.gridgain.grid.GridException;
import org.gridgain.grid.GridFactory;
import org.gridgain.grid.spi.collision.jobstealing.GridJobStealingCollisionSpi;
import org.gridgain.grid.spi.failover.jobstealing.GridJobStealingFailoverSpi;
import org.gridgain.grid.thread.GridThreadPoolExecutorService;
import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.OmadacSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridWorker implements Runnable
{
    private static Logger log = LoggerFactory.getLogger(GridWorker.class);
    
    private OmadacSettings config;

    
    protected void setOmadacGridNode(OmadacGridNode omadacGridNode)
    {
        // just to make sure that component OmadacGridNode has been created
    }
    
    @Override
    public void run()
    {
        start();
    }
    
    
    public void start()
    {
        GridConfigManager cm = new GridConfigManager();
        config = cm.getConfiguration();
        int numThreads = config.getJobs().getThreads();
        GridConfigurationAdapter gridCfg = new GridConfigurationAdapter();

        ExecutorService service = new GridThreadPoolExecutorService(numThreads, numThreads, Long.MAX_VALUE, new LinkedBlockingQueue<Runnable>());
        gridCfg.setExecutorService(service);
      
        
        GridJobStealingCollisionSpi collisionSpi = new GridJobStealingCollisionSpi();
        collisionSpi.setActiveJobsThreshold(2*numThreads);
        collisionSpi.setWaitJobsThreshold(numThreads);
        GridJobStealingFailoverSpi failoverSpi = new GridJobStealingFailoverSpi();
        gridCfg.setCollisionSpi(collisionSpi);
        gridCfg.setFailoverSpi(failoverSpi);
        gridCfg.setPeerClassLoadingEnabled(false);
        gridCfg.setGridGainHome(config.getTmpDir());
        try
        {
            log.info("starting grid node");
            GridFactory.start(gridCfg);
        }
        catch (GridException exc)
        {
            throw new OmadacException(exc);
        }
    }
}
