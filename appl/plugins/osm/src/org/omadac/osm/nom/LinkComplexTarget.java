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
import org.omadac.jpa.TxRunnable;
import org.omadac.make.ComplexTarget;
import org.omadac.make.Target;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.RoadAttributes;

public class LinkComplexTarget extends ComplexTarget
{
    private static final long serialVersionUID = 1L;
    private static final int NUM_LINKS = 1000;

    private EntityManager em;
    
    public LinkComplexTarget()
    {
    }
    
    @Override
    public List<Target> split()
    {
        createRoadAttributes();
        
        List<Target> subtargets = new ArrayList<Target>();
        List<NumberRange<Long>> ranges = getRanges(NUM_LINKS);
        for (NumberRange<Long> range : ranges)
        {
            Target subtarget = new LinkSubtarget(range);
            subtargets.add(subtarget);
        }
        
        return subtargets;
    }
    
    
    private void createRoadAttributes()
    {
        for (int i = 0; i < 8; i++)
        {
            RoadAttributes attr = new RoadAttributes();
            attr.setId(i+1);
            attr.setFunctionalClass(i);
            attr.setTravelDirection('B');
            em.persist(attr);
        }
    }    
    
    @Override
    public void merge()
    {
        em.clear();
        em.createNativeQuery(
            "alter table nom.link "
            + "add constraint pk_link "
            + "primary key (feature_id)").executeUpdate();
    }    

    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        Query query = em
                .createNativeQuery("select distinct id from osm.way_tags wt "
                        + "where wt.k = 'highway' order by id");
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        em.getTransaction().commit();
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    
    @Override
    public void clean()
    {
        MetadataInspector inspector = JpaUtil.getMetadataInspector(em);
        inspector.cleanTable("nom", "link");        
        JpaUtil.commit();

        String sql = "delete from nom.feature where discriminator = 'L'";
        em.createNativeQuery(sql).executeUpdate();
    }
}
