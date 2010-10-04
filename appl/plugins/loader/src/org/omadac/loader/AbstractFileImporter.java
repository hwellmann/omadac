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
package org.omadac.loader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.omadac.config.OmadacException;


public abstract class AbstractFileImporter implements Runnable
{
    protected AbstractLineFilter filter;

    protected List<String> inputDirectories;

    protected String outputDir;

    protected String schema;
    
    public AbstractFileImporter(AbstractLineFilter filter)
    {
        this.filter = filter;    
    }
    
    public void setSchema(String schema)
    {
        this.schema = schema;
    }
    
    public String getSchema()
    {
        return schema;
    }

    public abstract void importRawFile(File file, String tableName);

    public abstract void importFilteredFile(String fileName, String tableName);

    protected void preCheckLoaderFiles()
    {        
    }

    protected void postCheckLoaderFiles()
    {        
    }

    protected abstract boolean checkLoaderFiles(File[] files);

    protected String getTableNameForFileWithSchema(File file)
    {
        return  schema + "." + getTableNameForFile(file);
    }

    protected String getTableNameForFile(File file)
    {
        String name = file.getName();
        String table = name.replaceFirst("\\.txt(\\.gz)?", "");
        if (!name.equals(table))
        {
            return table.toLowerCase();
        }
        
        throw new OmadacException("cannot derive table name for " + file );
    }

    protected void processInputDirs(List<String> inputDirs)
    {
        inputDirectories = inputDirs;
        preCheckLoaderFiles();
        checkConfig();
        postCheckLoaderFiles();
        for (String inputDir: inputDirs)
        {
            File[] files = getLoaderFiles(new File(inputDir));
            importFiles(files);
        }
    }

    private void checkConfig()
    {
        checkLoaderFiles(inputDirectories);
    }

    private void checkLoaderFiles(List<String> inputDirs)
    {
        for (String input : inputDirs)
        {
            checkLoaderFiles(input);
        }
    }

    private void checkLoaderFiles(String inputDir)
    {
        File[] files = getLoaderFiles(new File(inputDir));
        if (!checkLoaderFiles(files))
        {
            throw new OmadacException("error checking loader files, see previous log messages");
        }
    }

    private File[] getLoaderFiles(File inputDir)
    {
        if (!inputDir.exists())
        {
            return new File[0];
        }
        File[] files = inputDir.listFiles(new FilenameFilter()
        {
                public boolean accept(File file, String name)
                {
                    return name.matches(".*\\.txt(\\.gz)?");
                }
        });
        return files;
    }

    private void importFiles(File[] files)
    {
        for (File file : files)
        {
            String tableName = getTableNameForFile(file);
            importFilteredFile(file.getPath(), tableName);
        }
    }
}
