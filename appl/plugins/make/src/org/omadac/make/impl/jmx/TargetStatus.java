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
package org.omadac.make.impl.jmx;

import org.omadac.make.Target.Status;

/**
 * Represents the current status of a target.
 * @author hwellmann
 *
 */
public class TargetStatus
{
    private String name;

    private Status status;
    
    private boolean complex;

    private int numSubtargets;

    private int numCompletedSubtargets;
    

    public TargetStatus(String name, Status status, boolean complex, int numSubtargets,
            int numCompletedSubtargets)
    {
        this.name = name;
        this.status = status;
        this.complex = complex;
        this.numSubtargets = numSubtargets;
        this.numCompletedSubtargets = numCompletedSubtargets;
    }


    public String getName()
    {
        return name;
    }


    public void setName(String name)
    {
        this.name = name;
    }


    public Status getStatus()
    {
        return status;
    }


    public void setStatus(Status status)
    {
        this.status = status;
    }


    public boolean isComplex()
    {
        return complex;
    }


    public void setComplex(boolean complex)
    {
        this.complex = complex;
    }


    public int getNumSubtargets()
    {
        return numSubtargets;
    }


    public void setNumSubtargets(int numSubtargets)
    {
        this.numSubtargets = numSubtargets;
    }


    public int getNumCompletedSubtargets()
    {
        return numCompletedSubtargets;
    }


    public void setNumCompletedSubtargets(int numCompletedSubtargets)
    {
        this.numCompletedSubtargets = numCompletedSubtargets;
    }

}
