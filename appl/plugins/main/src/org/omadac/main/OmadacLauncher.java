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
package org.omadac.main;

import java.util.List;

import org.omadac.config.ConfigManager;
import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.Job;
import org.omadac.config.jaxb.OmadacSettings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OmadacLauncher implements Runnable
{
    private static Logger log = LoggerFactory.getLogger(OmadacLauncher.class);
    
    private static final long TEN_SECONDS = 10000 * 60;
    
    //@Inject
    private BundleContext bundleContext;

    //@Inject
    private ConfigManager configManager;

    private OmadacSettings config;

    public OmadacLauncher()
    {
    }
    
    public void run()
    {
        boolean shutdownOsgi = true;
        int exitCode = 0;
        try
        {
            log.info("starting Omadac Main Thread");
            
            config = configManager.getConfiguration();
            if (config.isShutdown() != null)
            {
                shutdownOsgi = config.isShutdown();
            }
                        
            List<Job> jobs = config.getJobs().getJob();
            for (Job job : jobs)
            {
                Runnable runnable = waitForService(job);
                runnable.run();
            }
        }
        // CHECKSTYLE:OFF 
        catch (Throwable exc)
        // CHECKSTYLE:ON
        {
            log.error("Omadac Main Thread caught a runtime exception and will terminate", exc);
            exitCode = 1;
        } 
        finally
        {
            if (shutdownOsgi)
            {
                log.info("end of Omadac Main Thread, OSGi framework is shutting down");
                shutdownOsgiFramework(exitCode);
            }
            else
            {
                log.info("end of Omadac Main Thread, OSGi framework keeps running");
            }
        }
    }

    public void setConfigManager(ConfigManager mgr)
    {
        this.configManager = mgr;
    }
    
    public void setBundleContext(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    public void activate()
    {     
        Thread worker = new Thread(this, "Omadac Main Thread");
        worker.setDaemon(false);
        worker.start();
    }
    
    private Runnable waitForService(Job job)
    {        
        String name = job.getName();
        log.info("waiting for service {}", name);   
        Runnable service = null;
        
        try
        {
            Filter filter = bundleContext.createFilter(String.format("(&(%s=java.lang.Runnable)(name=%s))", 
                Constants.OBJECTCLASS, name));
            ServiceTracker tracker = new ServiceTracker(bundleContext, filter, null);
            tracker.open();
            service = (Runnable) tracker.waitForService(TEN_SECONDS);
            if (service == null)
            {
                throw new OmadacException("job service not found");
            }
            tracker.close();

            return service;
        }
        catch (InvalidSyntaxException exc)
        {
            throw new OmadacException(exc);
        }
        catch (InterruptedException exc)
        {
            throw new OmadacException(exc);
        }
    }    
    
    private void shutdownOsgiFramework(int exitCode)
    {
        try
        {
            log.info("stopping OSGi system bundle");
            bundleContext.getBundle(0).stop();
        }
        catch (BundleException exc)
        {
            log.error("error stopping OSGi system bundle", exc);
        }
        
        if (exitCode != 0)
        {                
            addShutdownHook();
        }
    }

    private void addShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Omadac Java VM halting with exit code 1");
                Runtime.getRuntime().halt(1);
            }
        });
    }
}
