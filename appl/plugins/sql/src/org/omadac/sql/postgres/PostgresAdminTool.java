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
package org.omadac.sql.postgres;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.PostgresSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PostgresAdminTool
{
    private static Logger log = LoggerFactory.getLogger(PostgresAdminTool.class);
    
    private String user;
    private String password;
    
    private String pgdump;
    
    private String pgsql;
    
    private String createdb;
    
    private String dropdb;
    
    public PostgresAdminTool(PostgresSettings pgSettings)
    {
        String pgBinDir = pgSettings.getBinDir();
        String suffix = "";
        if ("\\".equals(System.getProperty("file.separator")))
        {
            suffix = ".exe";
        }
        this.pgdump = new File(pgBinDir, "pg_dump" + suffix).toString();
        this.pgsql = new File(pgBinDir, "psql" + suffix).toString();
        this.createdb = new File(pgBinDir, "createdb" + suffix).toString();
        this.dropdb = new File(pgBinDir, "dropdb" + suffix).toString();
        
        this.user = pgSettings.getAdminUser();
        this.password = pgSettings.getAdminPassword();
    }

    public void dump(String backupFile, String host, String database, String... schemaNames)
    {
        
        List<String> cmd = new ArrayList<String>();
        cmd.add(pgdump);

        cmd.add("-i");
        cmd.add("-v");
        
        cmd.add("-f");
        cmd.add(backupFile);
        for (String schemaName : schemaNames)
        {
            cmd.add("-n");
            cmd.add(schemaName);
            log.info("dumping " + host + ":" + database + ":" + schemaName);
        }
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add(user);
        cmd.add(database);

        executeCommand(cmd, false);
        log.info("dump completed");
    }
    
    
    public void dumpTables(String backupFile, String host, String database, 
            String... tableNames) 
    {
        
        List<String> cmd = new ArrayList<String>();
        cmd.add(pgdump);

        cmd.add("-i");
        cmd.add("-v");
        
        cmd.add("-f");
        cmd.add(backupFile);
        for (String tableName : tableNames)
        {
            cmd.add("-t");
            cmd.add(tableName);
            log.info("dumping " + host + ":" + database + ":" + tableName);
        }
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add(user);
        cmd.add(database);

        executeCommand(cmd, false);
        log.info("dump completed");
    }
    
    
    public void load(String backupFile, String host, String database)
        throws IOException, InterruptedException
    {
        log.info("loading " + backupFile + " into " + host + ":" + database);
        List<String> cmd = new ArrayList<String>();
        cmd.add(pgsql);
        cmd.add("-f");
        cmd.add(backupFile);
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add(user);
        cmd.add("-d");
        cmd.add(database);
        
        executeCommand(cmd, false);
    }

    public void alterSchema(String host, String database, String oldSchema, String newSchema)
    {
        String sql = ("alter schema " + oldSchema + " rename to " + newSchema);
        log.info(sql);
        executeSql(host, database, sql);
    }
    
    public void createSchema(String host, String database, String schema, String owner)
    {
        String sql = ("create schema " + schema + " authorization " + owner);
        log.info(sql);
        executeSql(host, database, sql);
    }
    
    public void dropSchema(String host, String database, String schema)
    {
        String sql = ("drop schema if exists " + schema + " cascade");
        log.info(sql + " on " + host + ":" + database);
        executeSql(host, database, sql);
    }
    
    public void createDatabase(String host, String database, String owner)
    {
        log.info("creating " + host + ":" + database + " ("+ owner + ")");
        
        List<String> cmd = new ArrayList<String>();
        cmd.add(createdb);
        cmd.add("-O");
        cmd.add(owner);
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add(user);
        cmd.add(database);
        
        executeCommand(cmd, false);
    }
    
    
    public void dropDatabase(String host, String database)
    {
        log.info("dropping " + host + ":" + database);
        
        List<String> cmd = new ArrayList<String>();
        cmd.add(dropdb);
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add(user);
        cmd.add(database);
        
        executeCommand(cmd, false);
    }
    

    
    public void executeSql(String host, String sql)
    {
        List<String> cmd = new ArrayList<String>();
        cmd.add(pgsql);
        cmd.add("-c");
        cmd.add(sql);
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add("postgres");
        cmd.add("-d");
        cmd.add("postgres");
        cmd.add("-e");
        
        executeCommand(cmd, false);
    }
    
    public void executeSql(String host, String database, String sql)
    {
        List<String> cmd = new ArrayList<String>();
        cmd.add(pgsql);
        cmd.add("-c");
        cmd.add(sql);
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add(user);
        cmd.add("-d");
        cmd.add(database);
        cmd.add("-e");
        
        executeCommand(cmd, false);
    }
    
    public void executeSql(String host, String database, File file)
    {
        List<String> cmd = new ArrayList<String>();
        cmd.add(pgsql);
        cmd.add("-f");
        cmd.add(file.getPath());
        cmd.add("-h");
        cmd.add(host);
        cmd.add("-U");
        cmd.add(user);
        cmd.add("-d");
        cmd.add(database);
        cmd.add("-e");
        
        executeCommand(cmd, false);
    }
    
    
    private void executeCommand(List<String> cmd, boolean failOk)
    {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmd);

        processBuilder.environment().put("PGPASSWORD", password);

        processBuilder.redirectErrorStream(true);
        try
        {
            Process process = processBuilder.start();
            InputStream istream = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(istream);
            BufferedReader br = new BufferedReader(isr);
            
            String line;
            while ((line = br.readLine()) != null)
            {
                log.info("{}", line);
            }
            
            if (process.waitFor() != 0 && !failOk)
            {
                throw new OmadacException("error running " + cmd);
            }
        }
        catch (IOException exc)
        {
            throw new OmadacException(exc);
        }
        catch (InterruptedException exc)
        {
            throw new OmadacException(exc);
        }
    }
}
