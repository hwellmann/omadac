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

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

public class ScopedEntityManagerFactory extends EntityManagerFactoryProxy
        implements LazyCloseListener
{

    private final ThreadLocal<LazyCloseEntityManager> threadLocal;

    public ScopedEntityManagerFactory(EntityManagerFactory emf)
    {

        super(emf);
        this.threadLocal = new ThreadLocal<LazyCloseEntityManager>();
    }

    @SuppressWarnings("unchecked")
    public EntityManager createEntityManager(Map map)
    {

        LazyCloseEntityManager em = threadLocal.get();
        if (em == null)
        {
            em = new LazyCloseEntityManager(super.createEntityManager(map));
            createEntityManager(em);
        }
        return em;
    }

    public EntityManager createEntityManager()
    {

        LazyCloseEntityManager em = threadLocal.get();
        if (em == null)
        {
            em = new LazyCloseEntityManager(super.createEntityManager());
            createEntityManager(em);
        }
        return em;
    }

    private void createEntityManager(LazyCloseEntityManager em)
    {

        threadLocal.set(em);
        em.setListener(this);
    }

    protected LazyCloseEntityManager getEntityManager()
    {

        return threadLocal.get();
    }

    public void lazilyClosed()
    {

        threadLocal.set(null);
    }

    @Override
    public Cache getCache()
    {
        return getDelegate().getCache();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder()
    {
        return getDelegate().getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel()
    {
        return getDelegate().getMetamodel();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil()
    {
        return getDelegate().getPersistenceUnitUtil();
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return getDelegate().getProperties();
    }
}