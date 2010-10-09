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
package org.omadac.base.impl;

import java.net.URL;

import org.omadac.sql.SqlSchemaCreator;

/**
 * Create the database schema and tables for the engine persistence unit.
 * @author hwellmann
 *
 */
public class EngineSchemaCreator implements Runnable
{
    @Override
    public void run()
    {
        // TODO do not hardcode dialect
        String dialect = "postgresql";
        SqlSchemaCreator sqlSchemaCreator = new SqlSchemaCreator(dialect);
        String schemaFile = "/xml/engine_schema.xml";
        URL schema = getClass().getResource(schemaFile);
        sqlSchemaCreator.loadSchema(schema);
        sqlSchemaCreator.createTables();
        sqlSchemaCreator.createPrimaryKeys();
        sqlSchemaCreator.createIndexes();
    }
    
    public String getSchemaName()
    {
        return "engine";
    }
}
