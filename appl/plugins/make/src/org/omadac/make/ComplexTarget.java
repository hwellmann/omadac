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

import java.util.List;

import org.omadac.make.impl.SerializableRunnable;


public abstract class ComplexTarget extends Target
{
    private static final long serialVersionUID = 1;

    public ComplexTarget()
    {
    }
    
    public ComplexTarget(String name)
    {
        super(name);
    }
    
    public abstract List<Target> split();
    
    public void merge()
    {        
    }
    
    @Override
    public final void compile()
    {
    }
    
    @Override
    protected Runnable update()
    {
        Runnable runnable = new SerializableRunnable() 
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void run()
            {
                clean();
            }

        };
        return runnable;
    }
}
