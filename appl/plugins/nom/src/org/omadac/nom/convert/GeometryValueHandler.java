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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.strats.ByteArrayValueHandler;

import com.vividsolutions.jts.geom.Geometry;

public class GeometryValueHandler extends ByteArrayValueHandler
{
    private static final long serialVersionUID = 1L;

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val, JDBCStore store)
    {
        if (val == null)
            return null;
        
        Geometry geom = (Geometry) val;
        try
        {
            ByteArrayPackedGeometryWriter writer = new ByteArrayPackedGeometryWriter();
            writer.reset();
            writer.write(geom);
            byte[] blob = writer.toByteArray();
            return blob;
        }
        catch (IOException exc)
        {
            throw new RuntimeException(exc);
        }
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val)
    {
        if (val == null)
            return null;
        
        try
        {
            Geometry geom = null;
            byte[] blob = (byte[]) val;
            if (blob != null)
            {
                PackedGeometryReader reader = new PackedGeometryReader();
                ByteArrayInputStream is = new ByteArrayInputStream(blob);
                reader.setInput(new DataInputStream(is));
                geom = reader.readGeometry();
            }
            return geom;
        }
        catch (IOException exc)
        {
            throw new RuntimeException(exc);
        }
    }
}
