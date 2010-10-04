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
package org.omadac.jpa;


import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceManager
{

    private static Logger log = LoggerFactory.getLogger(PersistenceManager.class);

    private static String persistenceUnit;

    private static Map<?,?> properties;

    private static final PersistenceManager SINGLETON = new ScopedPersistenceManager(); 

    private EntityManagerFactory emf;

    protected PersistenceManager()
    {
    }

    public static PersistenceManager getInstance()
    {
        return SINGLETON;
    }

    public synchronized EntityManagerFactory getEntityManagerFactory()
    {
        if (emf == null)
        {
            emf = createEntityManagerFactory();
            log.info("Persistence Manager has been initialized");
        }
        return emf;
    }

    public synchronized void closeEntityManagerFactory()
    {
        if (emf != null)
        {
            emf.close();
            emf = null;
            log.info("Persistence Manager has been closed");
        }
    }

    protected EntityManagerFactory createEntityManagerFactory()
    {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        return Persistence.createEntityManagerFactory(persistenceUnit, properties);
    }

    public static String getPersistenceUnit()
    {
        return persistenceUnit;
    }

    public static void setPersistenceUnit(String persistenceUnit)
    {
        PersistenceManager.persistenceUnit = persistenceUnit;
    }
    
    public static void setProperties(Map<?,?> props)
    {
        properties = props;
    }
}