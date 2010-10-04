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
package org.omadac.sql;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;

import org.omadac.config.OmadacException;
import org.omadac.jpa.JpaUtil;


public class SqlSchemaCreator
{
    private SqlGenerator generator;
    
    public SqlSchemaCreator(String dialect)
    {
        this.generator = new SqlGenerator(dialect);
    }
    
    public void loadSchema(URL url)
    {
        generator.loadSchema(url);
    }
    
    public void createTables()
    {
        StringWriter writer = new StringWriter();
        generator.writeCreateTablesScript(writer);
        StringReader reader = new StringReader(writer.toString());
        executeScript(reader);
    }

    public void createPrimaryKeys()
    {
        StringWriter writer = new StringWriter();
        generator.writeCreatePrimaryKeysScript(writer);
        StringReader reader = new StringReader(writer.toString());
        executeScript(reader);
    }

    public void createPrimaryKeysForTable(String tableName)
    {
        StringWriter writer = new StringWriter();
        generator.writeCreatePrimaryKeyForTable(tableName, writer);
        StringReader reader = new StringReader(writer.toString());
        executeScript(reader);
    }

    public void createKeysAndIndexesForTable(String tableName)
    {
        StringWriter writer = new StringWriter();
        generator.writeCreateKeysAndIndexesForTable(tableName, writer);
        StringReader reader = new StringReader(writer.toString());
        executeScript(reader);
    }

    public void createIndexes()
    {
        StringWriter writer = new StringWriter();
        generator.writeCreateIndexesScript(writer);
        StringReader reader = new StringReader(writer.toString());
        executeScript(reader);
    }

    public void dropTables()
    {
        StringWriter writer = new StringWriter();
        generator.writeDropTablesScript(writer);
        StringReader reader = new StringReader(writer.toString());
        executeScript(reader);
    }
    
    private void executeScript(Reader reader)
    {
        Connection connection = JpaUtil.getConnection();
        SqlScriptRunner scriptRunner = new SqlScriptRunner(connection);
        try
        {
            scriptRunner.executeScript(reader);
            JpaUtil.commit();
        }
        catch (IOException exc)
        {
            throw new OmadacException(exc);
        }
    }
}
