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

import java.net.URL;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.MetadataInspector;
import org.omadac.make.SimpleTarget;
import org.omadac.make.Step;
import org.omadac.nom.Feature;
import org.omadac.sql.SqlSchemaCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NomSchemaStep implements Step<SimpleTarget>
{
    private static Logger log = LoggerFactory.getLogger(NomSchemaStep.class);
    
    private EntityManager em;
    
    public void setEntityManager(EntityManager em)
    {
        this.em = em;
    }

    @Override
    public void clean(SimpleTarget target)
    {
        log.info("dropping NOM schema");

        MetadataInspector inspector = JpaUtil.getMetadataInspector(em);
        inspector.dropSchema("nom");
    }

    @Override
    public void compile(SimpleTarget target)
    {
        log.info("creating NOM schema");
        
        MetadataInspector inspector = JpaUtil.getMetadataInspector(em);
        inspector.dropSchema("nom");

        String dialect = "postgresql";//getConfiguration().getServer().getJdbc().getSubprotocol();
        SqlSchemaCreator schemaCreator = new SqlSchemaCreator(em, dialect);
        URL schema = Feature.class.getResource("/xml/nom_schema.xml");
        schemaCreator.loadSchema(schema);
        schemaCreator.createTables();
        
        String sql = "insert into nom.DATABASE_INFO (PROVIDER, SCHEMA_VERSION) " +
                     "values ('OSM', '0.6')";
        Query q = em.createNativeQuery(sql);
        q.executeUpdate();
    }
}
