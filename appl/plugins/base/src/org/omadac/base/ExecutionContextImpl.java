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

import javax.persistence.EntityManagerFactory;

import org.omadac.config.ConfigManager;
import org.omadac.jpa.ScopedEntityManagerFactory;
import org.omadac.make.ExecutionContext;

/**
 * Execution context implementation, enriched by Omadac runtime configuration.
 */
public class ExecutionContextImpl implements ExecutionContext
{
    private EntityManagerFactory emfProduct;
    private EntityManagerFactory emfEngine;
    private ConfigManager configManager;
    
    protected void setEngineEntityManagerFactory(EntityManagerFactory emf)
    {
        this.emfEngine = new ScopedEntityManagerFactory(emf);
    }
    
    
    protected void setProductEntityManagerFactory(EntityManagerFactory emf)
    {
        this.emfProduct = new ScopedEntityManagerFactory(emf);
    }
    
    protected void setConfigManager(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public EntityManagerFactory getProductEntityManagerFactory()
    {
        return emfProduct;
    }

    public EntityManagerFactory getEngineEntityManagerFactory()
    {
        return emfEngine;
    }

    public ConfigManager getConfigManager()
    {
        return configManager;
    }
}
