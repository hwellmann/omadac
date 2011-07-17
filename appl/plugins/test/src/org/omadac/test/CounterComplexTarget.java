package org.omadac.test;

import java.util.List;

import org.omadac.make.ComplexTarget;
import org.omadac.make.Target;

public class CounterComplexTarget extends ComplexTarget
{
    private static final long serialVersionUID = 1L;

    private int start;
    private int numTicks;

    public CounterComplexTarget()
    {
        this(0, 50);
    }
    
    public CounterComplexTarget(int start, int numTicks)
    {
        this.start = start;
        this.numTicks = numTicks;
    }
    
    
    @Override
    public List<Target> split()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getType()
    {
        return "Counter";
    }


    public int getStart()
    {
        return start;
    }


    public void setStart(int start)
    {
        this.start = start;
    }


    public int getNumTicks()
    {
        return numTicks;
    }


    public void setNumTicks(int numTicks)
    {
        this.numTicks = numTicks;
    }
    
    
}
