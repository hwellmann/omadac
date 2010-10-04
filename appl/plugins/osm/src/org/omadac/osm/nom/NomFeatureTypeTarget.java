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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.MetadataInspector;
import org.omadac.make.Target;
import org.omadac.nom.NomFeatureType;


public class NomFeatureTypeTarget extends Target
{
    private static final long serialVersionUID = 1;

    public NomFeatureTypeTarget()
    {
    }
    
    @Override
    public void clean()
    {
        EntityManagerFactory emf = getEntityManagerFactory();
        MetadataInspector inspector = JpaUtil.getMetadataInspector(emf);
        inspector.cleanTable("nom", "nom_feature_type");
        
        JpaUtil.commit();
    }

    @Override
    public void compile()
    {
        // insert content
        EntityManager em = getCurrentEntityManager();
        
        String sql = "insert into nom.nom_feature_type "
                + "(feature_type, seq_num, dimension, description) "
                + "values "
                + "(?1, ?2, ?3, ?4)";
        
        Query query = em.createNativeQuery(sql);
        
        for (NomFeatureType type : NomFeatureType.values())
        {
            query.setParameter(1, type.getValue());
            query.setParameter(2, type.getSeqNum());
            query.setParameter(3, type.getDimension());
            query.setParameter(4, type.getDescription());
            query.executeUpdate();
        }
        
        // primary key
        String primaryKeySql = "alter table nom.nom_feature_type "
                + "add constraint pk_nom_feature_type "
                + "primary key (feature_type)";
        query = em.createNativeQuery(primaryKeySql);
        query.executeUpdate();
        
        // index
        String indexSql = "create index nx_nomfeaturetype_seqnum "
                + "on nom.nom_feature_type (seq_num)";
        query = em.createNativeQuery(indexSql);
        query.executeUpdate();
        
        em.getTransaction().commit();
    }
}
