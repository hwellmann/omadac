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
package org.omadac.osm.test;

import javax.persistence.EntityManager;

import org.omadac.jpa.JpaUtil;
import org.omadac.nom.NomJunction;
import org.omadac.nom.NomLink;

import org.junit.Test;


public class JunctionTest
{
    @Test
    public void saveLink()
    {
        EntityManager em = JpaUtil.getCurrentEntityManager();
        NomJunction from = new NomJunction(1, 2, 3);
        NomJunction to = new NomJunction(4, 5, 6);
        NomLink link = new NomLink(10);
        link.setReferenceNode(from);
        link.setNonReferenceNode(to);
        em.persist(from);
        em.persist(to);
        em.persist(link);
        em.getTransaction().commit();        
    }
}
