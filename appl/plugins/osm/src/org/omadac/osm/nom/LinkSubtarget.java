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
package org.omadac.osm.nom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.omadac.make.SimpleTarget;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.NomJunction;
import org.omadac.nom.NomLink;

import com.vividsolutions.jts.geom.Coordinate;

public class LinkSubtarget extends SimpleTarget
{
    private static final long serialVersionUID = 1L;

    private NumberRange<Long> range;

    
    transient List<NomLink> links;

    transient Map<Coordinate, NomJunction> nodeMap;

    transient List<NomJunction> newJunctions;

    transient Map<Long, NomJunction> junctionMap;
    
    transient List<Coordinate> coords;

    
    
    
    public LinkSubtarget(NumberRange<Long> range)
    {
        super(String.format("NomLinks_%d_%d", range.getMinId(), range.getMaxId()));
        this.range = range;
        coords = new ArrayList<Coordinate>();
        links = new ArrayList<NomLink>();
        nodeMap = new HashMap<Coordinate, NomJunction>();
        newJunctions = new ArrayList<NomJunction>();
        junctionMap = new HashMap<Long, NomJunction>();
        
    }

    public NumberRange<Long> getRange()
    {
        return range;
    }
}
