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
package org.omadac.nom;

public class TurnRestriction
{
    public static final int ALLOWED = 0;
    public static final int INCOMING_ONEWAY = 1;
    public static final int OUTGOING_ONEWAY = 2;
    public static final int BOTH_ONEWAY = 3;
    public static final int CONDITION = 4;
    public static final int DIVIDER = 5;

    private int id;
    private NomJunction junction;
    private NomLink fromLink;
    private NomLink toLink;
    private int type;

    public TurnRestriction()
    {

    }

    public TurnRestriction(NomJunction junction, NomLink from, NomLink to, int type)
    {
        this.junction = junction;
        this.fromLink = from;
        this.toLink = to;
        this.type = type;
    }

    public NomLink getFromLink()
    {
        return fromLink;
    }

    public void setFromLink(NomLink fromLink)
    {
        this.fromLink = fromLink;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public NomJunction getJunction()
    {
        return junction;
    }

    public void setJunction(NomJunction junction)
    {
        this.junction = junction;
    }

    public NomLink getToLink()
    {
        return toLink;
    }

    public void setToLink(NomLink toLink)
    {
        this.toLink = toLink;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }
}
