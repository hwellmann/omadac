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
package org.omadac.nom.convert;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class ByteArrayPackedGeometryWriter extends PackedGeometryWriter
{
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    private DataOutputStream dos = new DataOutputStream(baos);            

    public ByteArrayPackedGeometryWriter()
    {
        setOutput(dos);        
    }
    
    public byte[] toByteArray()
    {
        byte[] blob = baos.toByteArray();
        return blob;
    }
    
    public void reset()
    {
        baos.reset();
    }
}
