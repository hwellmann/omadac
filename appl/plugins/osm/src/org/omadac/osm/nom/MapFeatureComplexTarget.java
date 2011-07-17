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

public class MapFeatureComplexTarget extends ComplexTarget
{
    private static final long serialVersionUID = 1L;
    private static final int NUM_LINKS = 5000;

    private EntityManager em;
    
    public MapFeatureComplexTarget()
    {
    }
    
    @Override
    public List<Target> split()
    {
        List<Target> subtargets = new ArrayList<Target>();
        List<NumberRange<Long>> ranges = getRanges(NUM_LINKS);
        for (NumberRange<Long> range : ranges)
        {
            Target subtarget = new MapFeatureSubtarget(range);
            subtargets.add(subtarget);
        }
        
        return subtargets;
    }
    
    
    @Override
    public void merge()
    {
        em.createNativeQuery("ALTER TABLE nom.feature " +
                "ADD CONSTRAINT pk_feature " +
                "PRIMARY KEY(feature_id)").executeUpdate();
    }    

    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        String sql = "select w.id from osm.ways w  "
            + "left join nom.link l "
            + "on w.id = l.feature_id " 
            + "where l.feature_id is null "
            + "order by w.id"; 

        em.clear();
        Query query = em.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    
    @Override
    public void clean()
    {
        String sql = "delete from nom.feature where discriminator = 'F'";
        em.createNativeQuery(sql).executeUpdate();
    }
}
