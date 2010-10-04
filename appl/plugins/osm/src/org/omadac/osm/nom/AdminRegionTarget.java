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

import java.sql.Connection;

import javax.persistence.EntityManager;

import org.omadac.jpa.JpaUtil;
import org.omadac.make.Target;
import org.omadac.sql.SqlScriptRunner;

public class AdminRegionTarget extends Target
{
    private static final long serialVersionUID = 1L;

    @Override
    public void clean()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void compile()
    {
        EntityManager em = getCurrentEntityManager();
        Connection connection = JpaUtil.getConnection(em);
        SqlScriptRunner scriptRunner = new SqlScriptRunner(connection);
        scriptRunner.executeScript(getClass(), "/sql/admin_region.sql");
        em.getTransaction().commit();
    }
}
