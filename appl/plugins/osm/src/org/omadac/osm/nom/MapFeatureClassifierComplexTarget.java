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
import org.omadac.nom.NomFeatureType;

public class MapFeatureClassifierComplexTarget extends ComplexTarget
{
    private static final long serialVersionUID = 1L;
    private static final int NUM_LINKS = 5000;
    
    private EntityManager em;

    public MapFeatureClassifierComplexTarget()
    {
    }
    
    @Override
    public List<Target> split()
    {
        List<Target> subtargets = new ArrayList<Target>();
        List<NumberRange<Long>> ranges = getRanges(NUM_LINKS);
        for (NumberRange<Long> range : ranges)
        {
            Target subtarget = new MapFeatureClassifierSubtarget(range);
            subtargets.add(subtarget);
        }
        
        return subtargets;
    }
    
    
    @Override
    public void merge()
    {
        String hql = "delete from Feature f where f.featureType = 0";
        em.createQuery(hql).executeUpdate();
    }    

    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        String sql = "select f.feature_id from nom.feature f " 
            + "where f.feature_type != " + NomFeatureType.LINE_ROAD.getValue();

        Query query = em.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    
    @Override
    public void clean()
    {
    }
}
