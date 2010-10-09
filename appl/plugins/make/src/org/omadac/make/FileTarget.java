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
package org.omadac.make;

import java.io.File;

/**
 * A target corresponding to a file in the file system, given by an absolute or relative 
 * path name.
 * @author hwellmann
 *
 */
public abstract class FileTarget extends Target
{
    private static final long serialVersionUID = 1;

    private String fileName;
    
    /**
     * Constructs a file target with a given target name.
     * @param targetName  target name
     */
    public FileTarget(String targetName)
    {
        super(targetName);
    }
    
    /**
     * Constructs a file target with given target and file names.
     * @param targetName  target name
     * @param fileName    file name (path)
     */
    public FileTarget(String targetName, String fileName)
    {
        super(targetName);
        this.fileName = fileName;
    }
    
    /**
     * Cleans the target by deleting the file from the file system.
     */
    @Override
    public void clean()
    {
        File file = new File(fileName);
        file.delete();
    }
}
