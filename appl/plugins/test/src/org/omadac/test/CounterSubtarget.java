package org.omadac.test;

import org.omadac.make.Target;
import org.omadac.make.util.NumberRange;

public class CounterSubtarget extends Target
{
    private static final long serialVersionUID = 1L;

    private NumberRange<Integer> range;

    public CounterSubtarget(NumberRange<Integer> range)
    {
        this.range = range;
        setName(String.format("Counter_%d_%d", range.getMinId(), range.getMaxId()));
    }
    
    
    @Override
    public String getType()
    {
        return "Counter";
    }

    

    public NumberRange<Integer> getRange()
    {
        return range;
    }


    @Override
    public void compile()
    {
        // TODO Auto-generated method stub
        
    }
}
