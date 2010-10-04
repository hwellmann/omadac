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
package org.omadac.loader.postgresql;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.omadac.config.jaxb.ImportSettings;
import org.omadac.config.jaxb.OmadacSettings;
import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.MetadataInspector;
import org.omadac.loader.AbstractFileImporter;
import org.omadac.loader.TrivialLineFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresqlFileImporter extends AbstractFileImporter
{
    private static Logger log = LoggerFactory.getLogger(PostgresqlFileImporter.class);

    private OmadacSettings config;

    private EntityManagerFactory emf;

    private String schemaName;

    private MetadataInspector inspector;

    public PostgresqlFileImporter(OmadacSettings config, EntityManagerFactory emf)
    {
        super(new TrivialLineFilter());
        this.config = config;
        this.emf = emf;
    }

    @Override
    public void run()
    {
        ImportSettings importSettings = config.getImport();
        outputDir = config.getTmpDir();
        schemaName = importSettings.getSchema();

        List<String> inputDirs = importSettings.getInputDir();
        processInputDirs(inputDirs);
    }

    @Override
    public boolean checkLoaderFiles(File[] files)
    {
        boolean result = true;
        for (File file : files)
        {
            result = result && checkTableName(file);
        }
        return result;
    }

    @Override
    public void importRawFile(File packedFile, String tableName)
    {
        String packedFileName = packedFile.getName();
        String unpackedFileName = packedFileName.replace(".gz$", "");
        boolean unzip = !unpackedFileName.equals(packedFileName);
        File unpackedFile = new File(outputDir, unpackedFileName);

        if (!unpackedFile.exists())
        {
            log.info("unpacking {} into {}", packedFile, unpackedFile);
            filter.filter(packedFile, unpackedFile, unzip);
        }

        if (unpackedFile.length() == 0)
        {
            log.info("{} is empty", packedFile);
            return;
        }

        importFilteredFile(unpackedFile.getPath(), tableName);

        unpackedFile.delete();
    }

    @Override
    public void importFilteredFile(String fileName, String tableName)
    {
        Connection dbc = null;
        try
        {
            dbc = JpaUtil.getConnection();
            String importSql = String.format("copy %s.%s from '%s' null as ''", 
                schemaName, tableName, fileName.replaceAll("\\\\", "/"));
            log.debug(importSql);

            Statement st = dbc.createStatement();
            log.info("importing {} into table {}", fileName, tableName);
            st.executeUpdate(importSql);
            st.close();
            JpaUtil.commit(dbc);
        }
        catch (SQLException exc)
        {
            log.error("error importing " + fileName, exc);
            JpaUtil.rollback(dbc);
        }
    }

    @Override
    protected void preCheckLoaderFiles()
    {
        inspector = JpaUtil.getMetadataInspector(emf);
    }

    @Override
    protected void postCheckLoaderFiles()
    {
        JpaUtil.commit(inspector.getConnection());
    }

    private boolean checkTableName(File file)
    {
        String tableName = getTableNameForFile(file);
        String sName = schemaName.toLowerCase();
        boolean result = inspector.hasTable(sName, tableName);

        if (!result)
        {
            log.error("no table for loader file {}", file.getName());
        }

        return result;
    }
}
