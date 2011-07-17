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
package org.omadac.osm.nom;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.make.ComplexTarget;
import org.omadac.make.Target;
import org.omadac.make.util.NumberRange;

public class PointFeatureComplexTarget extends ComplexTarget
{
    private static final long serialVersionUID = 1L;
    private static final int NUM_NODES = 1000;

    private EntityManager em;
    
    public PointFeatureComplexTarget()
    {
    }
    
    @Override
    public List<Target> split()
    {
        List<Target> subtargets = new ArrayList<Target>();
        List<NumberRange<Long>> ranges = getRanges(NUM_NODES);
        for (NumberRange<Long> range : ranges)
        {
            Target subtarget = new PointFeatureSubtarget(range);
            subtargets.add(subtarget);
        }
        
        return subtargets;
    }
    
    
    @Override
    public void merge()
    {
        Query query = em.createNativeQuery("ALTER TABLE nom.poi " +
                "ADD CONSTRAINT pk_poi " +
                "PRIMARY KEY(feature_id)");
        query.executeUpdate();

        em.getTransaction().commit();
    }    

    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        String sql = "select distinct n.id from osm.node_tags n  "
            + "order by n.id"; 

        Query query = em.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        em.getTransaction().commit();
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    
    @Override
    public void clean()
    {
    }
}
