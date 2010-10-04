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
package org.omadac.sql.postgres.test;

import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.PostgresSettings;
import org.omadac.sql.postgres.PostgresAdminTool;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PostgresAdminToolTest
{
    private static PostgresSettings settings;
    private PostgresAdminTool tool;
    private String host = "localhost";
    private String db = "OmadacUnitTest";
    private String url = "jdbc:postgresql://localhost/OmadacUnitTest";
    private String user = "omadac";
    private String password = "omadac";
    

    @BeforeClass
    public static void createAdminTool() throws ClassNotFoundException
    {
        settings = new PostgresSettings();
        settings.setBinDir("/usr/bin");
        settings.setAdminUser("postgres");
        settings.setAdminPassword("postgres");

        Class.forName("org.postgresql.Driver");
    }
    
    @Before
    public void createTool()
    {        
        tool = new PostgresAdminTool(settings);
        tool.createDatabase(host, db, user);
    }
    
    @After
    public void cleanup()
    {
        tool.dropDatabase(host, db);
    }
    
    @Test
    public void createAndDropDatabase() throws SQLException
    {        
        Connection dbc = DriverManager.getConnection(url, user, password);
        assertNotNull(dbc);
        dbc.close();
    }
    
    @Test
    public void dropNonExistingSchema()
    {
        tool.dropSchema(host, db, "foobar");
    }

    @Test(expected=OmadacException.class)
    public void dropDatabaseWithOpenConnection() throws SQLException
    {        
        Connection dbc = DriverManager.getConnection(url, user, password);
        assertNotNull(dbc);
        
        tool.dropDatabase(host, db);
    }
}
