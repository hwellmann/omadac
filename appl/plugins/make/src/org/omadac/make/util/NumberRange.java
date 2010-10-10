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
package org.omadac.make.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A number range represents a closed interval of integer numbers.
 * @author hwellmann
 *
 * @param <T>
 */
public class NumberRange<T extends Number> implements Serializable
{
    private static final long serialVersionUID = 1L;


    /** Lower bound of range. */
    private T minId;
    
    
    /** Upper bound of range. */
    private T maxId;
    
    /**
     * Constructs a number range with given lower and upper bounds.
     * @param minId  lower bound
     * @param maxId  upper bound
     */
    public NumberRange(T minId, T maxId)
    {
        this.minId = minId;
        this.maxId = maxId;
    }
    
    /**
     * Returns the lower bound.
     * @return lower bound.
     */
    public T getMinId()
    {
        return minId;
    }

    /**
     * Returns the upper bound.
     * @return upper bound.
     */
    public T getMaxId()
    {
        return maxId;
    }

    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("NumberRange[");
        sb.append(minId);
        sb.append(",");
        sb.append(maxId);
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Splits a <em>sorted</em> list of numbers into a list of intervals, such that each 
     * interval (possibly except the last one) contains a given number of elements from the
     * list. The union of the intervals contains all elements of the original list.
     * @param <T>   number type
     * @param ids   sorted list of numbers
     * @param rangeSize  maximum size of intersection of each interval with number list
     * @return  list of intervals
     */
    public static <T extends Number> List<NumberRange<T>> split(List<T> ids, int rangeSize)
    {
        List<NumberRange<T>> ranges = new ArrayList<NumberRange<T>>();
        T minId = null;
        T maxId = null;
        T currentId = null;
        long numIds = 0;
        for (T id : ids)
        {
            currentId = id;
            if (minId == null)
            {
                minId = id;
            }
            numIds++;
            if (numIds == rangeSize)
            {
                maxId = id;
                ranges.add(new NumberRange<T>(minId, maxId));
                numIds = 0;
                minId = null;
            }
        }
       
        if (numIds != 0)
        {
            NumberRange<T> range;
            if (numIds == 1)
            {
                range = new NumberRange<T>(minId, minId);
            }
            else 
            {
                maxId = currentId;
                range = new NumberRange<T>(minId, maxId);
            }
            ranges.add(range);            
        }
        return ranges;
    }
}
