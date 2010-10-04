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



public class RoadNetworkDumper implements Runnable
{

    @Override
    public void run()
    {
//        EntityManager em = JpaUtil.getCurrentSession();
//        
//        Query query = em.createQuery("from NomLink");
//        query.setHint(QueryHints.SCROLLABLE_CURSOR, true);
//        ScrollableCursor cursor = (ScrollableCursor) query.getSingleResult();
//        
//        try
//        {
//            PrintStream out = new PrintStream("d:/temp/network.wkt");
//            while (cursor.hasNext())
//            {                
//                NomLink link = (NomLink) cursor.next();
//                out.println(link.getGeometry());
//            }
//            out.close();
//        }
//        catch (FileNotFoundException exc)
//        {
//            throw new RuntimeException(exc);
//        }        
//        em.getTransaction().commit();
    }
}
