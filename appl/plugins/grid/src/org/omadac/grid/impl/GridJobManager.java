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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.gridgain.grid.Grid;
import org.gridgain.grid.GridConfigurationAdapter;
import org.gridgain.grid.GridException;
import org.gridgain.grid.GridFactory;
import org.gridgain.grid.GridTaskFuture;
import org.gridgain.grid.GridTaskListener;
import org.gridgain.grid.spi.collision.jobstealing.GridJobStealingCollisionSpi;
import org.gridgain.grid.spi.failover.jobstealing.GridJobStealingFailoverSpi;
import org.gridgain.grid.thread.GridThreadPoolExecutorService;
import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.OmadacSettings;
import org.omadac.make.Action;
import org.omadac.make.ActionListener;
import org.omadac.make.JobManager;
import org.omadac.make.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job manager of type @{code grid} which distributes target actions to multiple grid nodes.
 * The master node (i.e. the one running this job manager) also acts as a worker. Additional
 * worker nodes are created by running a {@link GridWorker}.
 * <p>
 * A multi-master scenario (i.e. n workers receiving tasks from m masters in parallel is not
 * currently supported).
 * 
 * @author hwellmann
 *
 */
public class GridJobManager implements JobManager, GridTaskListener
{
    private static Logger log = LoggerFactory.getLogger(GridJobManager.class);
    
    private int numThreads;
    private Grid grid;
    private Vector<ActionListener> listeners = new Vector<ActionListener>();
    private Map<String, Action> targetMap = new HashMap<String, Action>();
    
    private OmadacSettings config;
    
    public void setOmadacGridNode(OmadacGridNode omadacGridNode)
    {
        // just to make sure that component OmadacGridNode has been created
    }
    
    @Override
    public void setNumThreads(int numThreads)
    {
        this.numThreads = numThreads;
    }

    @Override
    public void start()
    {
        GridConfigManager cm = (GridConfigManager) OmadacGridNode.getExecutionContext().getConfigManager();
        config = cm.getConfiguration();        
        GridConfigurationAdapter gridCfg = new GridConfigurationAdapter();
        
        Map<String,OmadacSettings> attrMap = new HashMap<String, OmadacSettings>();
        attrMap.put(GridConfigManager.KEY_OMADAC_MASTER_CONFIG, config);
        gridCfg.setUserAttributes(attrMap);

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
            grid = GridFactory.start(gridCfg);
            cm.setGrid(grid);
        }
        catch (GridException exc)
        {
            throw new OmadacException(exc);
        }
    }

    @Override
    public void stop()
    {
        GridFactory.stop(true);
    }

    @Override
    public void submitAction(Action action)
    {
        Target target = action.getTarget();
        target.setExecutionContext(OmadacGridNode.getExecutionContext());
        targetMap.put(target.getName(), action);
        log.info("scheduling job " + target.getName());
        grid.execute(GridTargetWrapper.class, target, this);
    }

    @Override
    public void addActionListener(ActionListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeActionListener(ActionListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void onFinished(GridTaskFuture<?> taskFuture)
    {
        try
        {
            String name = (String) taskFuture.get();
            Action action = targetMap.get(name);
            if (action == null)
            {
                throw new OmadacException(name + " missing in target map");
            }
            log.info("task completed: " + name);
            Target target = action.getTarget();
            target.setStatus(Target.Status.UPTODATE);
            onCompleted(target.getAction());
        }
        catch (GridException exc)
        {
            throw new OmadacException(exc);
        }        
    }
    
    private void onCompleted(Action action)
    {
        for (ActionListener listener : listeners)
        {
            listener.onCompleted(action);
        }
    }
    
}
