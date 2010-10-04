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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class LoaderFileWriter
{
    private PrintWriter writer;
    private boolean firstColumn = true;
    private long numRows;
    
    public LoaderFileWriter(File file) 
        throws FileNotFoundException, UnsupportedEncodingException
    {
        writer = new PrintWriter(file, "UTF-8");
    }
    
    public void writeColumn(Object obj)
    {
        if (firstColumn)
        {
            firstColumn = false;
        }
        else
        {
            writer.print('\t');
        }
        writer.print(obj);
    }
    
    public void terminateRow()
    {
        writer.println();
        numRows++;
        firstColumn = true;
    }
    
    public void close()
    {
        writer.close();
    }
    
    public long getNumRows()
    {
        return numRows;
    }
}
