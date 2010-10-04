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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.omadac.config.OmadacException;


public abstract class AbstractLineFilter
{
    protected File inputFile;

    protected File outputFile;
    
    public AbstractLineFilter()
    {
    }

    public AbstractLineFilter(File inputFile, File outputFile)
    {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public void filter(String inputFileName, String outputFileName, boolean unzip)
    {
        inputFile = new File(inputFileName);
        outputFile = new File(outputFileName);
        filter(inputFile, outputFile, unzip);
    }
    
    public void filter(File input, File output, boolean unzip)
    {
        try
        {
            PrintWriter out;
            
            out = new PrintWriter(output, "UTF-8");        

            FileInputStream is = new FileInputStream(input);
            InputStream zis = unzip ? new GZIPInputStream(is) : is;    
            Reader reader = new InputStreamReader(zis, "UTF-8"); 
            BufferedReader br = new BufferedReader(reader);
            
            String line;
            while ((line = br.readLine()) != null)
            {
                out.println(line);
            }

            out.close();
            zis.close();
        }
        catch (UnsupportedEncodingException exc)
        {
            throw new OmadacException(exc);
        }
        catch (IOException exc)
        {
            throw new OmadacException(exc);
        }
    }
    

    protected abstract String filterLine(String line);
    
}
