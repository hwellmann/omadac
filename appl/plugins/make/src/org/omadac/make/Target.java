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
package org.omadac.make;

import static org.omadac.make.Target.Status.MISSING;
import static org.omadac.make.Target.Status.UPTODATE;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.TxCallable;
import org.omadac.jpa.TxRunnable;
import org.omadac.make.impl.SerializableRunnable;
import org.omadac.make.impl.TargetInfo;
import org.osgi.service.component.ComponentContext;

public abstract class Target
{
    public enum Status
    {
        UNKNOWN,
        MISSING,
        CREATING,
        UPDATING,
        COMPLETED,
        UPTODATE,
        OUTDATED,
        INCOMPLETE,
        FORCED,
        ERROR
    }

    private static final long serialVersionUID = 1L;

    private ComplexTarget parent;

    private transient Action action;

    private TargetInfo info;
    
    private transient ExecutionContext executionContext;
    
    public Target()
    {
        this.info = new TargetInfo();
    }
    
    public Target(String name)
    {
        this.info = new TargetInfo(name);
    }
    
    protected void activate(ComponentContext context)
    {
        String name = (String) context.getProperties().get("name");
        info.setName(name);
    }

    public abstract void compile();

    public void clean()
    {
    }

    public synchronized Action getAction()
    {
        if (action == null)
        {
            Runnable runnable;
            switch (info.getStatus())
            {
                case MISSING:
                case CREATING:
                    runnable = create();
                    break;

                case INCOMPLETE:
                case OUTDATED:
                case UPDATING:
                    runnable = update();
                    break;

                default:
                    String msg = String.format("target %s is %s", info.getName(), info.getStatus());
                    throw new IllegalStateException(msg);
            }
            action = new Action(this, runnable);
        }
        return action;
    }

    protected Runnable create()
    {
        Runnable runnable = new SerializableRunnable() 
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void run()
            {
                compile();
                setStatus(UPTODATE);
                saveStatus();
            }

        };
        return runnable;
    }
    
    
    protected Runnable update()
    {
        Runnable runnable = new SerializableRunnable() 
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void run()
            {
                clean();
                compile();
                setStatus(UPTODATE);
                saveStatus();
            }
        };
        return runnable;
    }
        

    public String getName()
    {
        return info.getName();
    }
   
    protected void setName(String name)
    {
        info.setName(name);
    }
    
    public Status getStatus()
    {
        return info.getStatus();
    }

    public void setStatus(Status status)
    {
        info.setStatus(status);
    }

    protected ExecutionContext getExecutionContext()
    {
        return executionContext;
    }
    
    public void setExecutionContext(ExecutionContext context)
    {
        this.executionContext = context;
    }
    
    public ComplexTarget getParent()
    {
        return parent;
    }

    public void setParent(ComplexTarget parent)
    {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    public boolean refreshTargetStatus()
    {
        EntityManager em = getEngineEntityManager();
        Query query = em.createQuery("select ti from TargetInfo ti where ti.name = :name");
        query.setParameter("name", getName());
        List<TargetInfo> results = query.getResultList();

        if (results.isEmpty())
        {
            info = new TargetInfo(getName());
        }
        else
        {
            info = results.get(0);
        }
        setStatus(info.getStatus());
        return info.getStatus() != MISSING;
    }

    public void saveStatus()
    {
        EntityManager em = getEngineEntityManager();
        saveStatus(em);
        em.getTransaction().commit();
    }

    public void saveStatus(EntityManager em)
    {
        TargetInfo savedInfo = em.find(TargetInfo.class, info.getName());
        if (savedInfo == null)
        {
            em.persist(info);            
        }
        else
        {
            savedInfo.setStatus(info.getStatus());
            info = savedInfo;
        }
    }

    public EntityManagerFactory getEntityManagerFactory()
    {
        return executionContext.getProductEntityManagerFactory();
    }
    
    protected EntityManager getCurrentEntityManager()
    {
        EntityManagerFactory emf = executionContext.getProductEntityManagerFactory();
        EntityManager em = JpaUtil.getCurrentEntityManager(emf);
        return em;
    }
    
    public EntityManager getEngineEntityManager()
    {
        EntityManagerFactory emf = executionContext.getEngineEntityManagerFactory();
        EntityManager em = JpaUtil.getCurrentEntityManager(emf);
        return em;
    }
    
    public <T> T executeTransaction(TxCallable<T> work)
    {
        EntityManagerFactory emf = getEntityManagerFactory();
        EntityManager em = emf.createEntityManager();
        return JpaUtil.executeTransaction(em, work);
    }

    public void executeTransaction(TxRunnable work)
    {
        EntityManagerFactory emf = getEntityManagerFactory();
        EntityManager em = emf.createEntityManager();
        JpaUtil.executeTransaction(em, work);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        String name = info.getName();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Target other = (Target) obj;
        
        String name = info.getName();
        String otherName = other.info.getName();
        if (name == null)
        {
            if (otherName != null)
                return false;
        }
        else if (!name.equals(otherName))
            return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        return info.getName();
    }
}
