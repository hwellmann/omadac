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
package org.omadac.osm;

import java.io.File;
import java.net.URL;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.omadac.config.ConfigManager;
import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.JdbcSettings;
import org.omadac.config.jaxb.OmadacSettings;
import org.omadac.config.jaxb.PostgresSettings;
import org.omadac.loader.postgresql.PostgresqlFileImporter;
import org.omadac.sql.SqlSchemaCreator;
import org.omadac.sql.postgres.PostgresAdminTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OsmDatabaseImporter implements Runnable
{
    private static Logger log = LoggerFactory.getLogger(OsmDatabaseImporter.class);
    
    // TODO make this configurable
    private String scriptDir = "/usr/share/postgresql/8.4/contrib/postgis-1.5";
    
    private EntityManager em;
    private OmadacSettings config;
    
    private PostgresqlFileImporter osmImport;
    private Runnable engineSchemaCreator;
    private String targetHost;
    private String targetDb;

    public OsmDatabaseImporter()
    {
    }
    
    public void setConfigManager(ConfigManager configManager)
    {
        config = configManager.getConfiguration();
    }
    
    public void setEngineSchemaCreator(Runnable engineSchemaCreator)
    {
        this.engineSchemaCreator = engineSchemaCreator;
    }
    
    public void setEntityManager(EntityManager entityManager)
    {
        this.em = entityManager;
    }
    
    @Override
    public void run()
    {
        JdbcSettings jdbcSettings = config.getServer().getJdbc();
        targetHost = jdbcSettings.getServer();
        targetDb = jdbcSettings.getDatabase();
        String xmlFile = "/xml/osm_schema.xml";
        String dialect = config.getServer().getJdbc().getSubprotocol();
        URL xmlUrl = OsmDatabaseImporter.class.getResource(xmlFile);
        
        PostgresSettings pgSettings = config.getServer().getPostgres();
        String owner = jdbcSettings.getUser();
        PostgresAdminTool pgAdmin = new PostgresAdminTool(pgSettings);

        try
        {
            pgAdmin.dropDatabase(targetHost, targetDb);
        }
        catch (OmadacException exc)
        {
            log.error("cannot drop database", exc);
        }
        pgAdmin.createDatabase(targetHost, targetDb, owner);

        SqlSchemaCreator schemaCreator = new SqlSchemaCreator(em, dialect);
        schemaCreator.loadSchema(xmlUrl);
        schemaCreator.createTables();

        osmImport = new PostgresqlFileImporter(config, em);
        osmImport.setSchema("osm");
        osmImport.run();

        schemaCreator.createPrimaryKeys();
        schemaCreator.createIndexes();
        
        engineSchemaCreator.run();
        
        enablePostgis(pgAdmin);
    }

    private void enablePostgis(PostgresAdminTool pgadmin)
    {
        pgadmin.executeSql(targetHost, targetDb, "create language plpgsql");
        pgadmin.executeSql(targetHost, targetDb, new File(scriptDir, "postgis.sql"));
        pgadmin.executeSql(targetHost, targetDb, new File(scriptDir, "spatial_ref_sys.sql"));
    }
}
