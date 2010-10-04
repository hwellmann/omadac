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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.omadac.config.OmadacException;

import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class SqlGenerator
{
    private static Logger log = LoggerFactory.getLogger(SqlGenerator.class);

    protected Configuration fmConfig = new Configuration();

    private org.dom4j.Document dom4jDoc;    
    private org.w3c.dom.Document domDoc;
    private Map<String, Object> model;


    private String dialect;

    public SqlGenerator(String dialect)
    {
        this.dialect = dialect;
        fmConfig.setClassForTemplateLoading(getClass(), "/");
        fmConfig.setObjectWrapper(new DefaultObjectWrapper());
        model = new HashMap<String, Object>();
    }

    public void process(String template, Writer writer)
    {
        try
        {
            model.put("xml", NodeModel.wrap(domDoc));
            model.put("dialect", getDialect());
            Template tpl = fmConfig.getTemplate(template);
            tpl.process(model, writer);
        }
        catch (IOException exc)
        {
            throw new OmadacException(exc);
        }
        catch (TemplateException exc)
        {
            throw new OmadacException(exc);
        }
    }

    public void process(String template, File output)
    {
        try
        {
            Writer writer = new PrintWriter(output);
            process(template, writer);
            writer.close();
        }
        catch (IOException exc)
        {
            throw new OmadacException(exc);
        }
    }

    public String getDialect()
    {
        return dialect;
    }
    
    public void loadSchema(URL url)
    {
        SAXReader sax = new SAXReader();
        try
        {
            log.debug("URL = {}", url);
            dom4jDoc = sax.read(url);
            DOMWriter domWriter = new DOMWriter();
            domDoc = domWriter.write(dom4jDoc); 
        }
        catch (DocumentException exc)
        {
            throw new OmadacException(exc);
        }
    }

    public void writeCreateTablesScript(Writer output)
    {
        process("freemarker/sql/createTables.sql.ftl", output);
    }

    public void writeCreatePrimaryKeysScript(Writer output)
    {
        process("freemarker/sql/createPrimaryKeys.sql.ftl", output);
    }

    public void writeCreateIndexesScript(Writer output)
    {
        process("freemarker/sql/createIndexes.sql.ftl", output);
    }

    public void writeDropTablesScript(Writer output)
    {
        process("freemarker/sql/dropTables.sql.ftl", output);
    }
    
    public void writeCreatePrimaryKeyForTable(String tableName, Writer output)
    {
        model.put("tableName", tableName);
        process("freemarker/sql/createPrimaryKeysForTable.sql.ftl", output);
    }

    public void writeCreateIndexesForTable(String tableName, Writer output)
    {
        model.put("tableName", tableName);
        process("freemarker/sql/createIndexesForTable.sql.ftl", output);
    }

    public void writeCreateKeysAndIndexesForTable(String tableName, Writer output)
    {
        model.put("tableName", tableName);
        process("freemarker/sql/createPrimaryKeysForTable.sql.ftl", output);
        process("freemarker/sql/createIndexesForTable.sql.ftl", output);
    }
}
