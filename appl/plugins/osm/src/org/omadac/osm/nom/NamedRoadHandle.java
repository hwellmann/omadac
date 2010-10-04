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


class NamedRoadHandle
{
    Long order8;

    Long order9;

    Long nameId;

    NamedRoadHandle(Long order8, Long order9, Long nameId)
    {
        this.order8 = order8;
        this.order9 = order9;
        this.nameId = nameId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nameId == null) ? 0 : nameId.hashCode());
        result = prime * result + ((order8 == null) ? 0 : order8.hashCode());
        result = prime * result + ((order9 == null) ? 0 : order9.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NamedRoadHandle other = (NamedRoadHandle) obj;
        if (nameId == null)
        {
            if (other.nameId != null)
                return false;
        }
        else if (!nameId.equals(other.nameId))
            return false;
        if (order8 == null)
        {
            if (other.order8 != null)
                return false;
        }
        else if (!order8.equals(other.order8))
            return false;
        if (order9 == null)
        {
            if (other.order9 != null)
                return false;
        }
        else if (!order9.equals(other.order9))
            return false;
        return true;
    }

}
