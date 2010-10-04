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

import static org.omadac.sql.SqlScriptRunner.LexerStatus.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.omadac.config.OmadacException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes an SQL script on a given Connection. The script is split into statements which are
 * executed one after another. The statements may not return any results, i.e. SELECT statements are
 * not supported.
 * <p>
 * The script parser is rather dumb and does not recognize any SQL syntax, except comments. It
 * simply treats each semicolon as a statement terminator.
 * <p>
 * For this to work, make sure to terminate each statement by a semicolon and avoid using semicolons
 * in quoted strings.
 * <p>
 * The ScriptRunner supports dialect and version specific variants of a script, similar to locale
 * specific lookup of resource bundles.
 * <p>
 * If a dialect and a version are set on the ScriptRunner, given a script name /path/to/foo.sql, the
 * runner will execute the first match in the following list:
 * <ul>
 * <li>/path/to/version/dialect/foo.sql</li>
 * <li>/path/to/version/foo.sql</li>
 * <li>/path/to/dialect/foo.sql</li>
 * <li>/path/to/foo.sql</li>
 * </ul>
 * 
 * @author hwellmann
 * 
 */
public class SqlScriptRunner
{
    private static Logger log = LoggerFactory.getLogger(SqlScriptRunner.class);

    private Connection dbc;

    private boolean terminateOnError = true;

    private String version = "";

    private String dialect = "";
    
    enum LexerStatus
    {
        NORMAL,
        MINUS_SEEN,
        IN_COMMENT,
        EOF
    }

    public SqlScriptRunner(Connection dbc)
    {
        this.dbc = dbc;
    }

    /**
     * Returns the current value of the termination flag.
     * 
     * @return termination flag.
     */
    public boolean getTerminateOnError()
    {
        return terminateOnError;
    }

    /**
     * Sets the termination flag. If true, any SQLException from an executed
     * statement will be propagated. Otherwise, the exception will be logged
     * and the runner will continue with the next statement.
     * 
     * @param enabled
     *            stop on error
     */
    public void setTerminateOnError(boolean enabled)
    {
        this.terminateOnError = enabled;
    }

    /**
     * Executes a script from the given reader.
     * 
     * @param reader
     *            script reader
     * @throws IOException
     */
    public void executeScript(Reader reader) throws IOException
    {
        StringBuilder command = new StringBuilder();
        LexerStatus status = NORMAL;
        while (status != EOF)
        {
            int c = reader.read();
            if (status == MINUS_SEEN)
            {
                switch (c)
                {
                    case -1:
                        status = EOF;
                        break;

                    case '-':
                        status = IN_COMMENT;
                        break;
                    
                    default:
                        status = NORMAL;
                        command.append('-');
                        command.append((char) c);
                }
            }
            else if (status == IN_COMMENT)
            {
                switch (c)
                {
                    case -1:
                        status = EOF;
                        break;

                    case '\n':
                        status = NORMAL;
                        break;

                }
            }
            else
            {
                switch (c)
                {
                    case -1:
                        status = EOF;
                        break;

                    case '-':
                        status = MINUS_SEEN;
                        break;

                    case ';':
                        runStatement(command.toString());
                        command = new StringBuilder();
                        break;

                    default:
                        command.append((char) c);
                }
            }
        }
        reader.close();
    }

    /**
     * Executes a script obtained as a resource relative to given class with a
     * given resource name, possibly interpolated with dialect and version
     * information.
     * 
     * @param clazz
     *            class for resource lookup
     * @param resourceName
     *            resource path
     */
    public void executeScript(Class<?> clazz, String resourceName)
    {
        URL url = getVersionedUrl(clazz, resourceName);

        if (url == null)
        {
            throw new OmadacException("file " + resourceName + " not found.");
        }
        log.debug("resource name = {}, url = {}", resourceName, url);
        try
        {
            InputStream is = url.openStream();
            InputStreamReader reader = new InputStreamReader(is);
            executeScript(new BufferedReader(reader));
        }
        catch (IOException exc)
        {
            throw new OmadacException(exc);
        }
    }

    /**
     * Interpolates a resource name with dialect and version information and
     * returns the first matching resource URL.
     * 
     * @param clazz
     *            class for resource lookup
     * @param resourceName
     *            resource name
     * @return
     */
    private URL getVersionedUrl(Class<?> clazz, String resourceName)
    {
        URL url = constructUrl(clazz, resourceName, true, true);
        if (url != null)
            return url;

        url = constructUrl(clazz, resourceName, true, false);
        if (url != null)
            return url;

        url = constructUrl(clazz, resourceName, false, true);
        if (url != null)
            return url;

        return constructUrl(clazz, resourceName, false, false);
    }

    private URL constructUrl(Class<?> clazz, String resourceName,
            boolean hasVersion, boolean hasDialect)
    {
        int slash = resourceName.lastIndexOf("/");
        String root = (slash == -1) ? "" : resourceName.substring(0, slash + 1);
        StringBuilder sb = new StringBuilder(root);
        if (hasVersion)
        {
            sb.append(version).append("/");
        }
        if (hasDialect)
        {
            sb.append(dialect).append("/");
        }
        sb.append(resourceName.substring(slash + 1));
        log.debug("construced path {}", sb.toString());
        URL url = clazz.getResource(sb.toString());
        return url;
    }

    /**
     * Runs a single statement from the script.
     * 
     * @param sql
     *            SQL statement
     */
    private void runStatement(String sql)
    {
        log.info("running SQL statement\n{};", sql);
        try
        {
            Statement st = dbc.createStatement();
            st.executeUpdate(sql);
            st.close();
        }
        catch (SQLException exc)
        {
            if (terminateOnError)
            {
                throw new OmadacException("error in SQL statement:\n" + sql,
                        exc);
            }
            log.error("error in SQL statement", exc);
        }
    }

    /**
     * Sets a version string for script lookup.
     * 
     * @param version
     *            script version
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * Sets an SQL dialect string for script lookup.
     * 
     * @param dialect
     *            SQL dialect
     */
    public void setDialect(String dialect)
    {
        this.dialect = dialect;
    }
}
