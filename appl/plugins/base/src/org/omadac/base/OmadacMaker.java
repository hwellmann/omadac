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
package org.omadac.base;

import org.omadac.config.ConfigManager;
import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.Job;
import org.omadac.config.jaxb.JobSettings;
import org.omadac.config.jaxb.OmadacSettings;
import org.omadac.make.AbstractMaker;
import org.omadac.make.JobManager;

public abstract class OmadacMaker extends AbstractMaker
{
    private Job makerJob;
    
    private ConfigManager configManager;

    protected void initialize()
    {
        OmadacSettings config = getConfiguration();
        JobSettings jobs = config.getJobs();
        for (Job job : jobs.getJob())
        {
            if (job.getName().equals(getClass().getSimpleName()))
            {
                this.makerJob = job;
                break;
            }
        }
        
        if (makerJob == null)
        {
            throw new OmadacException("job settings not found");
        }
        
        if (makerJob.getMake() == null)
        {
            throw new OmadacException("make settings not found");
        }
        
        setDotOutput(makerJob.getMake().getDotOutput());
        
        String type = jobs.getManager().value();
        int numThreads = jobs.getThreads();
        JobManager jobManager = lookupJobManager(type);
        jobManager.setNumThreads(numThreads);
        
    }
    
    protected void defineForcedTargets()
    {
        for (String forced : makerJob.getMake().getForced())
        {
            addForcedTarget(forced);
        }
    }

    protected void defineGoals()
    {
        for (String goal : makerJob.getMake().getGoal())
        {
            addGoal(goal);
        }
    }
    
    
    protected OmadacSettings getConfiguration()
    {
        return configManager.getConfiguration();
    }
    
    protected void setConfigManager(ConfigManager configManager)
    {
        this.configManager = configManager;
    }


}
