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

public abstract class FileTarget extends Target
{
    private static final long serialVersionUID = 1;

    private String fileName;
    
    public FileTarget(String targetName)
    {
        super(targetName);
    }
    
    public FileTarget(String targetName, String fileName)
    {
        super(targetName);
        this.fileName = fileName;
    }
    
    @Override
    public void clean()
    {
        File file = new File(fileName);
        file.delete();
    }
}
