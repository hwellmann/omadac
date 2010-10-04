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
package org.omadac.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;


public class MetadataInspector
{
    private Connection dbc;

    private DatabaseMetaData metaData;

    public MetadataInspector(Connection dbc)
    {
        this.dbc = dbc;
        try
        {
            metaData = dbc.getMetaData();
        }
        catch (SQLException e)
        {
            throw new JpaException(e);
        }
    }

    public Set<String> getTables(String schema)
    {
        try
        {
            ResultSet rs = metaData.getTables(null, schema, null, new String[] { "TABLE" });
            TreeSet<String> tableNames = new TreeSet<String>();
            while (rs.next())
            {
                String tableName = rs.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
            return tableNames;
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public Set<String> getIndices(String schema, String table)
    {
        try
        {
            TreeSet<String> indexNames = new TreeSet<String>();
            ResultSet rs = metaData.getIndexInfo(dbc.getCatalog(), schema, table, false, true);
            while (rs.next())
            {
                String name = rs.getString("INDEX_NAME");
                if (name != null)
                    indexNames.add(name);
            }
            return indexNames;
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public boolean hasTable(String schema, String table)
    {
        try
        {
            ResultSet rs = metaData.getTables(dbc.getCatalog(), schema, table, null);
            boolean result = rs.next();
            rs.close();
            return result;
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public boolean hasTables(String[] tableNames)
    {
        for (String tableName : tableNames)
        {
            String[] parts = tableName.split("\\.");
            if (parts.length != 2)
            {
                throw new JpaException("table name " + tableName + " must be qualified by schema");
            }
            if (!hasTable(parts[0], parts[1]))
            {
                return false;
            }
        }
        return true;
    }

    public boolean hasSchema(String schemaName) throws SQLException
    {
        ResultSet rs = metaData.getSchemas();
        boolean result = false;
        while (rs.next() && !result)
        {
            String tableSchema = rs.getString("TABLE_SCHEM");
            result = tableSchema.toLowerCase().equals(schemaName.toLowerCase());
        }
        rs.close();
        return result;
    }

    public boolean hasIndex(String schema, String table, String index)
    {
        try
        {
            ResultSet rs = metaData.getIndexInfo(dbc.getCatalog(), schema, table, false, true);
            while (rs.next())
            {
                String name = rs.getString("INDEX_NAME");
                if (index.equalsIgnoreCase(name))
                {
                    return true;
                }
            }
            return false;
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public boolean hasPrimaryKey(String schema, String table)
    {
        try
        {
            ResultSet rs = metaData.getPrimaryKeys(dbc.getCatalog(), schema, table);
            return rs.next();
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public boolean isTableEmpty(String schema, String table)
    {
        try
        {
            boolean result = false;
            if (!hasTable(schema, table))
                return false;

            Statement st = dbc.createStatement();
            String sql = String.format("select * from %s.%s  limit 1", schema, table);
            ResultSet rs = st.executeQuery(sql);
            result = rs.next();
            rs.close();
            st.close();
            return result;
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public void createPrimaryKey(String schema, String table, String... columns)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("alter table ").append(schema).append(".").append(table);
        sb.append(" add constraint pk_").append(table);
        sb.append(" primary key (");

        for (int i = 0; i < columns.length; i++)
        {
            sb.append(columns[i]);
            if (i < columns.length - 1 && columns.length != 1)
            {
                sb.append(",");
            }
        }
        sb.append(")");
        try
        {
            Statement st = dbc.createStatement();
            st.executeUpdate(sb.toString());
            st.close();
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }

    }

    public void cleanTable(String schema, String table)
    {
        dropPrimaryKey(schema, table);
        dropIndices(schema, table);
        truncateTable(schema, table);
    }

    public void dropIndices(String schema, String table)
    {
        for (String index : getIndices(schema, table))
        {
            dropIndex(schema, table, index);
        }
    }

    public boolean dropIndex(String schema, String table, String index)
    {
        try
        {
            if (hasIndex(schema, table, index))
            {
                String sql = String.format("drop index %s.%s ", schema, index);
                Statement st = dbc.createStatement();
                st.executeUpdate(sql);
                st.close();
                return true;
            }
            return false;
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public boolean dropPrimaryKey(String schema, String table)
    {
        boolean result = false;
        try
        {
            ResultSet rs = metaData.getPrimaryKeys(dbc.getCatalog(), schema, table);
            if (rs.next())
            {
                String key = rs.getString("PK_NAME");
                Statement st = dbc.createStatement();
                String sql = String.format("alter table %s.%s drop constraint %s", schema, table,
                    key);

                st.executeUpdate(sql);
                st.close();
                result = true;
            }
            rs.close();
            return result;
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public void truncateTable(String schema, String table)
    {
        try
        {
            Statement st = dbc.createStatement();
            String sql = String.format("truncate table %s.%s", schema, table);

            st.executeUpdate(sql);
            st.close();
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }
    }

    public void dropSchema(String schema)
    {
        try
        {
            Statement st = dbc.createStatement();
            String sql = String.format("drop schema if exists %s cascade", schema);

            st.executeUpdate(sql);
            st.close();
        }
        catch (SQLException exc)
        {
            throw new JpaException(exc);
        }

    }

    public Connection getConnection()
    {
        return dbc;
    }
}
