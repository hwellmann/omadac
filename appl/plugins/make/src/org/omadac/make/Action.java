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

import org.omadac.make.impl.SerializableRunnable;

/**
 * An Action encapsulates a target and a Runnable for updating that target.
 * 
 * @author hwellmann
 *
 */
public class Action implements SerializableRunnable
{
    private static final long serialVersionUID = 1;

    private Runnable runnable;
    private Target target;

    /**
     * Constructs an action for a given target and runnable.
     * @param target
     * @param runnable
     */
    public Action(Target target, Runnable runnable)
    {
        this.target = target;
        this.runnable = runnable;
    }
    
    @Override
    public void run()
    {
        runnable.run();
    }

    public Target getTarget()
    {
        return target;
    }
    
    public void setTarget(Target target)
    {
        this.target = target;
    }

    public Runnable getRunnable()
    {
        return runnable;
    }
}
