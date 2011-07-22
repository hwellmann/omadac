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
package org.omadac.osm.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.junit.Test;
import org.omadac.nom.NomJunction;
import org.omadac.nom.NomLink;


public class LinkJpaTest
{
    private static String driver = "org.postgresql.Driver";
    private static String url = "jdbc:postgresql://localhost/OmadacTest";
    private static String user = "omadac";
    private static String password = "omadac";
    private static String persistenceUnit = "org.omadac.nom";
    
    
    private Map<Long, NomJunction> junctionMap = new HashMap<Long, NomJunction>();
    
    private static EntityManager createEntityManager() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("javax.persistence.jtaDataSource", "");
        props.put("javax.persistence.nonJtaDataSource", "");
        
        props.put("javax.persistence.jdbc.driver", driver);
        props.put("javax.persistence.jdbc.url", url);
        props.put("javax.persistence.jdbc.user", user);
        props.put("javax.persistence.jdbc.password", password);

        // Properties for the alternate non-jta-data-source. We need to set these
        // to override the jta-data-source in persistence.xml
        props.put("openjpa.Connection2DriverName", driver);
        props.put("openjpa.Connection2URL", url);
        props.put("openjpa.Connection2UserName", user);
        props.put("openjpa.Connection2Password", password);

        // create the schema
        //props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=false)");
        //props.put("openjpa.Log", "DefaultLevel=TRACE");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit, props);
        EntityManager em = emf.createEntityManager();
        return em;
    }

    
    
    @Test
    public void createJunctions()
    {
        EntityManager em = createEntityManager();
        
        em.getTransaction().begin();
        int numJunctions = 1000;
        for (int i = 1; i <= numJunctions; i++) {
            NomJunction junction = new NomJunction();
            junction.setFeatureType(2);
            junction.setX(i);
            junction.setY(i);
            junction.setZ(i);
            em.persist(junction);
        }
        em.getTransaction().commit();
    }
    
    @Test
    public void createLinks() {
        EntityManager em = createEntityManager();
        
        em.getTransaction().begin();
        loadJunctions(em);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        int numLinks = 500;
        for (long i = 1; i <= numLinks; i++) {
            NomLink link = new NomLink();
            link.getJunctions().add(junctionMap.get(2*i-1));
            link.getJunctions().add(junctionMap.get(2*i));
            em.persist(link);
        }
        em.getTransaction().commit();
        
    }



    private void loadJunctions(EntityManager em)
    {
        TypedQuery<NomJunction> query = em.createQuery("select j from NomJunction j", NomJunction.class);
        List<NomJunction> junctions = query.getResultList();
        for (NomJunction junction : junctions) {
            junctionMap.put(junction.getId(), junction);
        }
    }
}
