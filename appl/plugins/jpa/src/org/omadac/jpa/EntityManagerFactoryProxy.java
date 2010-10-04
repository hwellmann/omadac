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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

abstract class EntityManagerFactoryProxy implements EntityManagerFactory
{

    protected final EntityManagerFactory delegate;

    protected EntityManagerFactoryProxy(EntityManagerFactory emf)
    {
        this.delegate = emf;
    }

    public EntityManager createEntityManager()
    {
        return delegate.createEntityManager();
    }

    
    @SuppressWarnings("unchecked")
    public EntityManager createEntityManager(Map map)
    {
        return delegate.createEntityManager(map);
    }

    public boolean isOpen()
    {
        return delegate.isOpen();
    }

    public void close()
    {
        delegate.close();
    }

    public EntityManagerFactory getDelegate()
    {
        return delegate;
    }
}