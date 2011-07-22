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

import org.omadac.make.SimpleTarget;
import org.omadac.make.util.NumberRange;

public class LinkSubtarget extends SimpleTarget
{
    private static final long serialVersionUID = 1L;

    private NumberRange<Long> range;

    public LinkSubtarget(NumberRange<Long> range)
    {
        super(String.format("NomLinks_%d_%d", range.getMinId(), range.getMaxId()));
        this.range = range;
    }

    public NumberRange<Long> getRange()
    {
        return range;
    }
}
