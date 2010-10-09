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


/**
 * A no-op target, useful in contexts where a target is expected even when there is nothing
 * to do.
 * 
 * @author hwellmann
 *
 */
public class NoOpTarget extends Target
{
    private static final long serialVersionUID = 1L;

    public NoOpTarget()
    {
        super("NoOp");
    }
    
    public NoOpTarget(String name)
    {
        super(name);
    }
    
    @Override
    public void compile()
    {
        // do nothing
    }
}
