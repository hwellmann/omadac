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

import java.io.Serializable;

import org.omadac.engine.Status;
import org.omadac.engine.TargetInfo;
import org.omadac.make.impl.SerializableRunnable;
import org.osgi.service.component.ComponentContext;
import static org.omadac.engine.Status.*;
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
    
    private transient Step step;
    
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
                if (step == null) {
                    compile();
                }
                else {
                    step.compile(Target.this);
                }
                setStatus(UPTODATE);
                //saveStatus();
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
                if (step == null) {
                    clean();
                    compile();
                }
                else {
                    step.clean(Target.this);
                    step.compile(Target.this);
                }
                setStatus(UPTODATE);
                //saveStatus();
            }
        };
        return runnable;
    }
        

    public String getName()
    {
        return info.getName();
    }
   
    public void setName(String name)
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

    public ExecutionContext getExecutionContext()
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
    
    

    public TargetInfo getInfo()
    {
        return info;
    }

    public void setInfo(TargetInfo info)
    {
        this.info = info;
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

    public Step getStep()
    {
        return step;
    }

    public void setStep(Step step)
    {
        this.step = step;
    }
    
    
    public String getType() {
        throw new UnsupportedOperationException();
    }
}
