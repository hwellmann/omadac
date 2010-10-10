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

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.TxCallable;
import org.omadac.jpa.TxRunnable;
import org.omadac.make.impl.SerializableRunnable;
import org.osgi.service.component.ComponentContext;

/**
 * A Target is the smallest identifiable entity produced by the Make Engine. A target can be a file
 * or a group of database entries.
 * <p>
 * A target is created by its compile() method. An existing target can be removed with the clean()
 * method.
 * <p>
 * Targets may depend on other targets. The make engine ensures that all prerequisites of a target
 * exist before attempting to compile a given target.
 * <p>
 * A target may have a parent. In this case, the parent is a ComplexTarget which is split into a
 * number of subtargets which can be updated in parallel. A first-level target is a target without
 * parent. A first-level target which is not complex is called simple.
 * <p>
 * Subtargets of a complex target do not have any dependencies or prerequisites of their own. All
 * dependencies are expressed in terms of first-level targets.
 * <p>
 * A subtarget cannot be split further, i.e. it cannot be a complex target.
 * <p>
 * Each target is associated to two persistence units, called engine and product persistence unit.
 * The formeris owned by the make engine for keeping track of the target status, while the latter is
 * owned by the application to support targets represented by database objects.
 * <p>
 * An entity manager for the product persistence unit may be used by derived classes to create or
 * update the target.
 * <p>
 * An entity manager for the engine persistence unit may only be used by the make engine itself to
 * update the target status.
 * <p>
 * The two persistence units may or may not be associated to the same data source.
 * 
 * @author hwellmann
 * 
 */
public abstract class Target implements Serializable
{
    /**
     * Status of a target.
     * @author hwellmann
     *
     */
    public enum Status
    {
        /** The status is unknown. It may be stored in the database but is not loaded yet. */
        UNKNOWN,
        
        /** The target does not exist. */
        MISSING,
        
        /** The target is being created. */
        CREATING,
        
        /** The target is being updated. */
        UPDATING,
        
        /** 
         * The target has been created or updated, but the new status has not yet been
         * persisted.
         */
        COMPLETED,
        
        /**
         * The target is up to date.
         */
        UPTODATE,
        
        /**
         * The target is outdated, i.e. at least one of its prerequisites is not up to date.
         */
        OUTDATED,
        
        /**
         * The target is incomplete. Some of its subtargets were updated in a previous run of
         * the make engine, but the compilation was suspended or interrupted.
         */
        INCOMPLETE,
        
        /**
         * An update of this target was forced by the user.
         */
        FORCED,
        
        /**
         * An error has occurred while updating this target.
         */
        ERROR
    }

    private static final long serialVersionUID = 1L;

    /**
     * The parent of this subtarget. (Null for simple targets or complex targets.)
     */
    private ComplexTarget parent;

    /**
     * The action to be executed for updating this target.
     */
    private transient Action action;

    /**
     * Persistent target status.
     */
    private TargetInfo info;
    
    /**
     * Execution context for updating this target.
     */
    private transient ExecutionContext executionContext;
    
    /**
     * Creates an anonymous target.
     */
    public Target()
    {
        this.info = new TargetInfo();
    }
    
    /**
     * Creates a target with a given name
     * @param name  target name
     */
    public Target(String name)
    {
        this.info = new TargetInfo(name);
    }
    
    /**
     * Sets the target name from the service component properties.
     * @param context OSGi service component context.
     */
    protected void activate(ComponentContext context)
    {
        String name = (String) context.getProperties().get("name");
        info.setName(name);
    }

    /**
     * Creates this target, assuming that it does not exist. Implementations of this method
     * need not be idempotent. If the target already exists, this method may throw an exception.
     * An existing target is usually updated by calling the clean() and compile() methods.
     */
    public abstract void compile();

    /**
     * Removes a target, if it exists. Otherwise, this method has not effect.
     */
    public void clean()
    {
    }

    /**
     * Returns the action for updating this target, based on its current status.
     * @return action
     */
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

    /**
     * Returns a runnable for creating this target, when it does not exist.
     * @return  creating action
     */
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
    
    /**
     * Returns a runnable for updating this target when it exists already.
     * @return updating action
     */
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

    /**
     * Refreshes the target status from persistent storage.
     * For internal use within the make engine. This method shall not be called from derived
     * classes or other clients.
     * @return true, unless the target is new
     */
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

    /**
     * Persists the target status.
     * For internal use within the make engine. 
     */
    public void saveStatus()
    {
        EntityManager em = getEngineEntityManager();
        saveStatus(em);
        em.getTransaction().commit();
    }

    /**
     * Persists the target status with a given entity manager.
     * For internal use within the make engine. 
     * @param em entity manager
     */
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

    /**
     * Returns the entity manager factory for the product persistence unit.
     * @return
     */
    public EntityManagerFactory getEntityManagerFactory()
    {
        return executionContext.getProductEntityManagerFactory();
    }
    
    /**
     * Returns the current entity manager for the product persistence unit with an active
     * transaction. The entity manager is tied to the current thread. The method may create 
     * a new entity manager or use an existing one. If the entity manager has no active 
     * transaction, a new transaction will be started. The caller is responsible for committing
     * the transaction.
     * 
     * @return entity manager
     */
    protected EntityManager getCurrentEntityManager()
    {
        EntityManagerFactory emf = executionContext.getProductEntityManagerFactory();
        EntityManager em = JpaUtil.getCurrentEntityManager(emf);
        return em;
    }
    
    /**
     * Returns the current entity manager for the engine persistence unit with an active
     * transaction. 
     * 
     * @return entity manager
     */
    public EntityManager getEngineEntityManager()
    {
        EntityManagerFactory emf = executionContext.getEngineEntityManagerFactory();
        EntityManager em = JpaUtil.getCurrentEntityManager(emf);
        return em;
    }
    
    /**
     * Executes the given callable within a separate transaction of the product persistence unit.
     * @param <T>  return type of the callable
     * @param work  callable to be executed
     * @return result of the callable
     */
    public <T> T executeTransaction(TxCallable<T> work)
    {
        EntityManagerFactory emf = getEntityManagerFactory();
        EntityManager em = emf.createEntityManager();
        return JpaUtil.executeTransaction(em, work);
    }

    /**
     * Executes the given runnable within a separate transaction of the product persistence unit.
     * @param work  runnable to be executed
     */
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
