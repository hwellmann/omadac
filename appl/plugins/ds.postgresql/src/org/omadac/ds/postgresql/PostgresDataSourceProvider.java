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
package org.omadac.ds.postgresql;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.sql.DataSource;

import org.omadac.config.ConfigManager;
import org.omadac.config.jaxb.JdbcSettings;
import org.omadac.config.jaxb.OmadacSettings;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.postgresql.ds.PGPoolingDataSource;

/**
 * Constructs a pooling Postgres data source based on the Omadac runtime configuration and
 * registers it as an OSGi service.
 * 
 * @author hwellmann
 *
 */
public class PostgresDataSourceProvider
{
    private ConfigManager cm;

    public void setConfigManager(ConfigManager configManager)
    {
        this.cm = configManager;
    }
    
    public DataSource createDataSource()
    {
        OmadacSettings config = cm.getConfiguration();
        JdbcSettings jdbc = config.getServer().getJdbc();
        
        PGPoolingDataSource dataSource = new PGPoolingDataSource();
        dataSource.setServerName(jdbc.getServer());
        dataSource.setDatabaseName(jdbc.getDatabase());
        dataSource.setUser(jdbc.getUser());
        dataSource.setPassword(jdbc.getPassword());
        dataSource.setInitialConnections(jdbc.getInitialConnections());
        dataSource.setMaxConnections(jdbc.getMaxConnections());
        return dataSource;
    }
}
