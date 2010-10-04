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
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.MetadataInspector;
import org.omadac.make.ComplexTarget;
import org.omadac.make.Target;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.HouseNumberRange;

public class NamedRoadComplexTarget extends ComplexTarget
{
    private static final long serialVersionUID = 1L;
    private static final int NUM_ROAD_NAMES = 200;

    public NamedRoadComplexTarget()
    {
    }
    
    @Override
    public List<Target> split()
    {
        createDefaultAddressRange();
        
        List<Target> subtargets = new ArrayList<Target>();
        List<NumberRange<Long>> ranges = getRanges(NUM_ROAD_NAMES);
        for (NumberRange<Long> range : ranges)
        {
            Target subtarget = new NamedRoadSubtarget(range);
            subtargets.add(subtarget);
        }
        
        return subtargets;
    }
    
    
    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        String jpql = "select n.id from RoadName n order by n.id"; 

        EntityManager em = getCurrentEntityManager();
        Query query = em.createQuery(jpql);
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        em.getTransaction().commit();
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    
    @Override
    public void clean()
    {
        EntityManagerFactory emf = getEntityManagerFactory();
        MetadataInspector inspector = JpaUtil.getMetadataInspector(emf);
        inspector.cleanTable("nom", "road");
        inspector.cleanTable("nom", "road_link");
        JpaUtil.commit();
    }
    
    private void createDefaultAddressRange()
    {
        EntityManager em = getCurrentEntityManager();
        HouseNumberRange range = new HouseNumberRange();
        range.setId(1);
        range.setFirst("0");
        range.setLast("0");
        range.setIncrement('0');
        em.persist(range);        

        em.getTransaction().commit();
    }
    

    
}
