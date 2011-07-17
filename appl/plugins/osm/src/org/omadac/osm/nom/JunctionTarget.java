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
import org.omadac.jpa.MetadataInspector;
import org.omadac.make.Target;
import org.omadac.sql.SqlScriptRunner;

public class JunctionTarget extends Target
{
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    
    
    @Override
    public void clean()
    {
        MetadataInspector inspector = JpaUtil.getMetadataInspector(em);
        inspector.cleanTable("nom", "junction");
        JpaUtil.commit();
    }

    @Override
    public void compile()
    {
        Connection connection = JpaUtil.getConnection(em);
        SqlScriptRunner scriptRunner = new SqlScriptRunner(connection);
        scriptRunner.executeScript(getClass(), "/sql/create_junctions.sql");
    }
}
