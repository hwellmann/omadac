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
package org.omadac.make;

import static org.omadac.engine.Status.MISSING;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.engine.TargetInfo;


public class TargetDao 
{
    private EntityManager em;
    
    public void setEntityManager(EntityManager em)
    {
        this.em = em;
    }
    
    /**
     * Refreshes the target status from persistent storage.
     * For internal use within the make engine. This method shall not be called from derived
     * classes or other clients.
     * @return true, unless the target is new
     */
    @SuppressWarnings("unchecked")
    public boolean refreshTargetStatus(Target target)
    {
        Query query = em.createQuery("select ti from TargetInfo ti where ti.name = :name");
        query.setParameter("name", target.getName());
        List<TargetInfo> results = query.getResultList();

        TargetInfo info;
        if (results.isEmpty())
        {
            info = new TargetInfo(target.getName());
        }
        else
        {
            info = results.get(0);
        }
        target.setInfo(info);
        return info.getStatus() != MISSING;
    }

    /**
     * Persists the target status with a given entity manager.
     * For internal use within the make engine. 
     * @param em entity manager
     */
    public void saveStatus(Target target)
    {
        TargetInfo savedInfo = em.find(TargetInfo.class, target.getName());
        if (savedInfo == null)
        {
            em.persist(target.getInfo());            
        }
        else
        {
            savedInfo.setStatus(target.getInfo().getStatus());
            target.setInfo(savedInfo);
        }
    }
    
    public void deleteAll() {
        String jpql = "delete ti from TargetInfo ti";
        em.createQuery(jpql).executeUpdate();
    }
}
