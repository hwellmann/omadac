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
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

abstract class EntityManagerProxy implements EntityManager
{

    protected final EntityManager delegate;

    public EntityManagerProxy(EntityManager delegate)
    {
        this.delegate = delegate;
    }

    public void persist(Object object)
    {
        delegate.persist(object);
    }

    public <T> T merge(T entity)
    {
        return delegate.merge(entity);
    }

    public void remove(Object object)
    {
        delegate.remove(object);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey)
    {
        return delegate.find(entityClass, primaryKey);
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey)
    {
        return delegate.getReference(entityClass, primaryKey);
    }

    public void flush()
    {
        delegate.flush();
    }

    public void setFlushMode(FlushModeType flushModeType)
    {
        delegate.setFlushMode(flushModeType);
    }

    public FlushModeType getFlushMode()
    {
        return delegate.getFlushMode();
    }

    public void lock(Object object, LockModeType lockModeType)
    {
        delegate.lock(object, lockModeType);
    }

    public void refresh(Object object)
    {
        delegate.refresh(object);
    }

    public void clear()
    {
        delegate.clear();
    }

    public boolean contains(Object object)
    {
        return delegate.contains(object);
    }

    public Query createQuery(String string)
    {
        return delegate.createQuery(string);
    }

    public Query createNamedQuery(String string)
    {
        return delegate.createNamedQuery(string);
    }

    public Query createNativeQuery(String string)
    {
        return delegate.createNativeQuery(string);
    }

    @SuppressWarnings("unchecked")
    public Query createNativeQuery(String string, Class aClass)
    {
        return delegate.createNativeQuery(string, aClass);
    }

    public Query createNativeQuery(String string, String string0)
    {
        return delegate.createNativeQuery(string, string0);
    }

    public void joinTransaction()
    {
        delegate.joinTransaction();
    }

    public Object getDelegate()
    {
        return delegate.getDelegate();
    }

    public void close()
    {
        delegate.close();
    }

    public boolean isOpen()
    {
        return delegate.isOpen();
    }

    public EntityTransaction getTransaction()
    {
       return delegate.getTransaction();
    }
    
    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass)
    {
        return delegate.createNamedQuery(name, resultClass);
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery)
    {
        return delegate.createQuery(criteriaQuery);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass)
    {
        return delegate.createQuery(qlString, resultClass);
    }

    @Override
    public void detach(Object entity)
    {
        delegate.detach(entity);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
            Map<String, Object> properties)
    {
        return delegate.find(entityClass, primaryKey, properties);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode)
    {
        return delegate.find(entityClass, primaryKey, lockMode);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
            LockModeType lockMode, Map<String, Object> properties)
    {
        return delegate.find(entityClass, primaryKey, lockMode, properties);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder()
    {
        return delegate.getCriteriaBuilder();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory()
    {
        return delegate.getEntityManagerFactory();
    }

    @Override
    public LockModeType getLockMode(Object entity)
    {
        return delegate.getLockMode(entity);
    }

    @Override
    public Metamodel getMetamodel()
    {
        return delegate.getMetamodel();
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return delegate.getProperties();
    }

    @Override
    public void lock(Object entity, LockModeType lockMode,
            Map<String, Object> properties)
    {
        delegate.lock(entity, lockMode, properties);
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties)
    {
        delegate.refresh(entity, properties);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode)
    {
        delegate.refresh(entity, lockMode);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode,
            Map<String, Object> properties)
    {
        delegate.refresh(entity, lockMode, properties);
    }

    @Override
    public void setProperty(String propertyName, Object value)
    {
        delegate.setProperty(propertyName, value);
    }

    @Override
    public <T> T unwrap(Class<T> cls)
    {
        return delegate.unwrap(cls);
    }
    
}